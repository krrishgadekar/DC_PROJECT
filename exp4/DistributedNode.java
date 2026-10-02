import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedNode {
    private static final Map<Integer, Integer> CLUSTER_PORTS = Map.of(
        1, 5001,
        2, 5002,
        3, 5003,
        4, 5004,
        5, 5005
    );
    private static final int TOTAL_NODES = CLUSTER_PORTS.size();
    private static final int QUORUM_THRESHOLD = (TOTAL_NODES / 2) + 1; // 3 for N=5
    private static final long HEARTBEAT_INTERVAL_MS = 1000;
    private static final long TIMEOUT_THRESHOLD_MS = 3000;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final int nodeId;
    private final int port;
    private final String algorithm; // "bully" or "ring"
    private final Set<Integer> blockedPeers;
    private final Map<Integer, Long> aliveTable = new ConcurrentHashMap<>();
    private final Object stateLock = new Object();
    private final List<Integer> ringOrder;

    private int lamportClock = 0;
    private Integer leaderId = null;
    private boolean isLeader = false;
    private boolean electionInProgress = false;
    private volatile boolean isRunning = true;
    private DatagramSocket socket;

    public DistributedNode(int nodeId, String algorithm, Set<Integer> blockedPeers) {
        this.nodeId = nodeId;
        this.port = CLUSTER_PORTS.get(nodeId);
        this.algorithm = algorithm.toLowerCase();
        this.blockedPeers = blockedPeers;

        this.ringOrder = new ArrayList<>(CLUSTER_PORTS.keySet());
        Collections.sort(this.ringOrder);

        long now = System.currentTimeMillis();
        for (int peerId : CLUSTER_PORTS.keySet()) {
            if (peerId != nodeId) {
                aliveTable.put(peerId, now);
            }
        }

        try {
            this.socket = new DatagramSocket(this.port);
            this.socket.setSoTimeout(1000);
        } catch (Exception e) {
            System.err.println("Failed to bind socket on port " + this.port + ": " + e.getMessage());
        }
    }

    private void log(String tag, String message) {
        String timestamp = LocalTime.now().format(TIME_FORMAT);
        System.out.println(String.format("[%s] [Node %d | L=%d | %s] [%s] %s",
                timestamp, nodeId, lamportClock, algorithm.toUpperCase(), tag, message));
    }

    private int tickClock(int receivedTime) {
        synchronized (stateLock) {
            lamportClock = Math.max(lamportClock, receivedTime) + 1;
            return lamportClock;
        }
    }

    private void sendPacket(int targetId, String payload) {
        if (targetId == nodeId || blockedPeers.contains(targetId)) return;
        try {
            byte[] buffer = payload.getBytes();
            DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length, InetAddress.getByName("127.0.0.1"), CLUSTER_PORTS.get(targetId)
            );
            socket.send(packet);
        } catch (IOException ignored) {}
    }

    // Thread 1: Heartbeat Sender Thread
    private void runSenderThread() {
        while (isRunning) {
            int clk = tickClock(0);
            String payload = String.format("HEARTBEAT|%d|%d|%b", nodeId, clk, isLeader);

            for (int peerId : CLUSTER_PORTS.keySet()) {
                sendPacket(peerId, payload);
            }

            try {
                Thread.sleep(HEARTBEAT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Thread 2: Heartbeat & Election Protocol Listener
    private void runListenerThread() {
        byte[] buffer = new byte[2048];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String[] parts = msg.split("\\|");

                String type = parts[0];
                int senderId = Integer.parseInt(parts[1]);

                if (blockedPeers.contains(senderId)) continue;

                int receivedClock = Integer.parseInt(parts[2]);
                tickClock(receivedClock);

                if ("HEARTBEAT".equals(type)) {
                    boolean senderIsLeader = Boolean.parseBoolean(parts[3]);
                    synchronized (stateLock) {
                        aliveTable.put(senderId, System.currentTimeMillis());
                        if (senderIsLeader) {
                            leaderId = senderId;
                        }
                    }
                } else if ("BULLY_ELECTION".equals(type)) {
                    log("BULLY", "Received ELECTION message from Node " + senderId);
                    int clk = tickClock(0);
                    sendPacket(senderId, String.format("BULLY_OK|%d|%d", nodeId, clk));
                    triggerElection();
                } else if ("BULLY_OK".equals(type)) {
                    log("BULLY", "Received OK from higher Node " + senderId + ". Standing down.");
                    synchronized (stateLock) {
                        electionInProgress = false;
                    }
                } else if ("COORDINATOR".equals(type)) {
                    int newLeader = Integer.parseInt(parts[3]);
                    synchronized (stateLock) {
                        leaderId = newLeader;
                        isLeader = (leaderId == nodeId);
                        electionInProgress = false;
                    }
                    log("ELECTION", "Recognized Coordinator: Node " + leaderId);
                } else if ("RING_ELECTION".equals(type)) {
                    int originId = Integer.parseInt(parts[3]);
                    String candidates = parts[4];
                    handleRingElection(originId, candidates);
                } else if ("RING_COORD".equals(type)) {
                    int originId = Integer.parseInt(parts[3]);
                    int electedLeader = Integer.parseInt(parts[4]);
                    handleRingCoordinator(originId, electedLeader);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (isRunning) log("ERROR", "Listener error: " + e.getMessage());
            }
        }
    }

    // Thread 3: Timeout Watchdog & Quorum Evaluator
    private void runWatchdogThread() {
        while (isRunning) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            long now = System.currentTimeMillis();
            List<Integer> reachable = new ArrayList<>();
            List<Integer> silent = new ArrayList<>();
            reachable.add(nodeId);

            synchronized (stateLock) {
                for (Map.Entry<Integer, Long> entry : aliveTable.entrySet()) {
                    if (now - entry.getValue() > TIMEOUT_THRESHOLD_MS) {
                        silent.add(entry.getKey());
                    } else {
                        reachable.add(entry.getKey());
                    }
                }

                boolean leaderDead = (leaderId == null) || silent.contains(leaderId);
                boolean hasQuorum = reachable.size() >= QUORUM_THRESHOLD;

                if (leaderDead && !electionInProgress) {
                    if (hasQuorum) {
                        log("WATCHDOG", String.format("Leader absent. Quorum satisfied (%d/%d). Initiating %s Election...",
                                reachable.size(), TOTAL_NODES, algorithm.toUpperCase()));
                        triggerElection();
                    } else {
                        log("WATCHDOG", String.format("Leader absent. MINORITY PARTITION (%d/%d nodes). Refusing election to prevent Split-Brain.",
                                reachable.size(), TOTAL_NODES));
                    }
                }
            }
        }
    }

    private void triggerElection() {
        if ("ring".equals(algorithm)) {
            startRingElection();
        } else {
            startBullyElection();
        }
    }

    // ==========================================
    // 1. BULLY ELECTION ENGINE
    // ==========================================
    private void startBullyElection() {
        List<Integer> higherNodes = new ArrayList<>();
        synchronized (stateLock) {
            if (electionInProgress) return;
            electionInProgress = true;
            for (int peerId : CLUSTER_PORTS.keySet()) {
                if (peerId > nodeId && !blockedPeers.contains(peerId)) {
                    higherNodes.add(peerId);
                }
            }
        }

        if (higherNodes.isEmpty()) {
            declareLeadership();
            return;
        }

        int clk = tickClock(0);
        for (int target : higherNodes) {
            sendPacket(target, String.format("BULLY_ELECTION|%d|%d", nodeId, clk));
        }

        // Wait window: If no higher node replies OK, declare leadership
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

            synchronized (stateLock) {
                if (electionInProgress && !isLeader) {
                    log("BULLY", "No higher nodes responded with OK. Declaring leadership.");
                    declareLeadership();
                }
            }
        }).start();
    }

    private void declareLeadership() {
        synchronized (stateLock) {
            isLeader = true;
            leaderId = nodeId;
            electionInProgress = false;
        }
        log("ELECTION", "Node " + nodeId + " declared as Leader. Broadcasting COORDINATOR...");
        int clk = tickClock(0);
        String coordMsg = String.format("COORDINATOR|%d|%d|%d", nodeId, clk, nodeId);
        for (int peerId : CLUSTER_PORTS.keySet()) {
            sendPacket(peerId, coordMsg);
        }
    }

    // ==========================================
    // 2. RING ELECTION ENGINE
    // ==========================================
    private int getNextRingNeighbor() {
        int idx = ringOrder.indexOf(nodeId);
        int n = ringOrder.size();
        for (int i = 1; i < n; i++) {
            int candidate = ringOrder.get((idx + i) % n);
            if (!blockedPeers.contains(candidate)) {
                return candidate;
            }
        }
        return nodeId;
    }

    private void startRingElection() {
        synchronized (stateLock) {
            if (electionInProgress) return;
            electionInProgress = true;
        }
        int next = getNextRingNeighbor();
        int clk = tickClock(0);
        log("RING", "Passing election token to neighbor: Node " + next);
        String payload = String.format("RING_ELECTION|%d|%d|%d|%d", nodeId, clk, nodeId, nodeId);
        sendPacket(next, payload);
    }

    private void handleRingElection(int originId, String candidateListStr) {
        List<String> candidates = new ArrayList<>(Arrays.asList(candidateListStr.split(",")));

        if (originId == nodeId) {
            // Circuit complete: Elect highest ID
            int maxId = candidates.stream().mapToInt(s -> Integer.parseInt(s)).max().orElse(nodeId);
            log("RING", "Token completed circuit. Highest ID elected: Node " + maxId);

            synchronized (stateLock) {
                leaderId = maxId;
                isLeader = (maxId == nodeId);
                electionInProgress = false;
            }

            int next = getNextRingNeighbor();
            int clk = tickClock(0);
            sendPacket(next, String.format("RING_COORD|%d|%d|%d|%d", nodeId, clk, nodeId, maxId));
        } else {
            // Append self if absent, forward around ring
            if (!candidates.contains(String.valueOf(nodeId))) {
                candidates.add(String.valueOf(nodeId));
            }
            int next = getNextRingNeighbor();
            int clk = tickClock(0);
            log("RING", "Forwarding token to Node " + next + " with candidates: " + candidates);
            sendPacket(next, String.format("RING_ELECTION|%d|%d|%d|%s", nodeId, clk, originId, String.join(",", candidates)));
        }
    }

    private void handleRingCoordinator(int originId, int electedLeader) {
        synchronized (stateLock) {
            leaderId = electedLeader;
            isLeader = (electedLeader == nodeId);
            electionInProgress = false;
        }
        log("COORDINATOR", "Acknowledged Ring Leader: Node " + electedLeader);

        if (originId != nodeId) {
            int next = getNextRingNeighbor();
            int clk = tickClock(0);
            sendPacket(next, String.format("RING_COORD|%d|%d|%d|%d", nodeId, clk, originId, electedLeader));
        }
    }

    // Thread 4: Background Workload Worker
    private void runWorkerThread() {
        int taskId = 0;
        while (isRunning) {
            taskId++;
            String role;
            synchronized (stateLock) {
                role = isLeader ? "LEADER" : "FOLLOWER";
            }
            log("WORKLOAD", String.format("Executing task #%d as [%s]", taskId, role));

            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void start() {
        log("INIT", "Booting Node " + nodeId + " on port " + port + ". Blocked: " + blockedPeers);

        Thread sender = new Thread(this::runSenderThread, "Sender-Thread");
        Thread listener = new Thread(this::runListenerThread, "Listener-Thread");
        Thread watchdog = new Thread(this::runWatchdogThread, "Watchdog-Thread");
        Thread worker = new Thread(this::runWorkerThread, "Worker-Thread");

        sender.setDaemon(true);
        listener.setDaemon(true);
        watchdog.setDaemon(true);
        worker.setDaemon(true);

        sender.start();
        listener.start();
        watchdog.start();
        worker.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isRunning = false;
            if (socket != null && !socket.isClosed()) socket.close();
        }));

        try {
            while (isRunning) Thread.sleep(1000);
        } catch (InterruptedException e) {
            isRunning = false;
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DistributedNode <node_id> <bully|ring> [blocked_peer_ids...]");
            return;
        }

        int id = Integer.parseInt(args[0]);
        String algo = args[1];
        Set<Integer> blocked = new HashSet<>();
        for (int i = 2; i < args.length; i++) {
            blocked.add(Integer.parseInt(args[i]));
        }

        DistributedNode node = new DistributedNode(id, algo, blocked);
        node.start();
    }
}