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
    private final Set<Integer> blockedPeers;
    private final Map<Integer, Long> aliveTable = new ConcurrentHashMap<>();
    private final Object stateLock = new Object();

    private int lamportClock = 0;
    private Integer leaderId = null;
    private boolean isLeader = false;
    private volatile boolean isRunning = true;
    private DatagramSocket socket;

    public DistributedNode(int nodeId, Set<Integer> blockedPeers) {
        this.nodeId = nodeId;
        this.port = CLUSTER_PORTS.get(nodeId);
        this.blockedPeers = blockedPeers;

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
        System.out.println(String.format("[%s] [Node %d | L=%d] [%s] %s",
                timestamp, nodeId, lamportClock, tag, message));
    }

    private int tickClock(int receivedTime) {
        synchronized (stateLock) {
            lamportClock = Math.max(lamportClock, receivedTime) + 1;
            return lamportClock;
        }
    }

    // Thread 1: Heartbeat Sender Thread
    private void runSenderThread() {
        while (isRunning) {
            int clk = tickClock(0);
            String payload = String.format("HEARTBEAT|%d|%d|%b", nodeId, clk, isLeader);
            byte[] buffer = payload.getBytes();

            for (Map.Entry<Integer, Integer> entry : CLUSTER_PORTS.entrySet()) {
                int peerId = entry.getKey();
                int peerPort = entry.getValue();

                if (peerId == nodeId || blockedPeers.contains(peerId)) {
                    continue;
                }

                try {
                    DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, InetAddress.getByName("127.0.0.1"), peerPort
                    );
                    socket.send(packet);
                } catch (IOException ignored) {}
            }

            try {
                Thread.sleep(HEARTBEAT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Thread 2: Heartbeat Listener Thread
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

                if (blockedPeers.contains(senderId)) {
                    continue;
                }

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
                } else if ("COORDINATOR".equals(type)) {
                    int newLeader = Integer.parseInt(parts[3]);
                    synchronized (stateLock) {
                        leaderId = newLeader;
                        isLeader = (leaderId == nodeId);
                    }
                    log("ELECTION", "Recognized new cluster Coordinator: Node " + leaderId);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (isRunning) {
                    log("ERROR", "Listener error: " + e.getMessage());
                }
            }
        }
    }

    // Thread 3: Timeout Watchdog & Quorum Detector Thread
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

                if (leaderDead) {
                    if (hasQuorum) {
                        log("WATCHDOG", String.format("Leader absent. Quorum satisfied (%d/%d). Triggering Quorum Election...",
                                reachable.size(), TOTAL_NODES));
                        startQuorumElection(reachable);
                    } else {
                        log("WATCHDOG", String.format("Leader absent. MINORITY PARTITION (%d/%d nodes). Refusing election to prevent Split-Brain.",
                                reachable.size(), TOTAL_NODES));
                    }
                }
            }
        }
    }

    private void startQuorumElection(List<Integer> reachableNodes) {
        List<Integer> higherNodes = new ArrayList<>();
        for (int nid : reachableNodes) {
            if (nid > nodeId) {
                higherNodes.add(nid);
            }
        }

        if (higherNodes.isEmpty()) {
            log("ELECTION", "Node " + nodeId + " has highest reachable ID in majority partition. Declaring leadership.");
            synchronized (stateLock) {
                isLeader = true;
                leaderId = nodeId;
            }

            int clk = tickClock(0);
            String coordMsg = String.format("COORDINATOR|%d|%d|%d", nodeId, clk, nodeId);
            byte[] buffer = coordMsg.getBytes();

            for (int peerId : reachableNodes) {
                if (peerId != nodeId && !blockedPeers.contains(peerId)) {
                    try {
                        DatagramPacket packet = new DatagramPacket(
                            buffer, buffer.length, InetAddress.getByName("127.0.0.1"), CLUSTER_PORTS.get(peerId)
                        );
                        socket.send(packet);
                    } catch (Exception ignored) {}
                }
            }
        } else {
            log("ELECTION", "Yielding leadership to higher reachable candidates: " + higherNodes);
        }
    }

    // Thread 4: Background Workload Worker Thread
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
        log("INIT", "Booting Node " + nodeId + " on port " + port + ". Blocked peers: " + blockedPeers);

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
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }));

        try {
            while (isRunning) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            isRunning = false;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java DistributedNode <node_id> [blocked_peer_ids...]");
            return;
        }

        int id = Integer.parseInt(args[0]);
        Set<Integer> blocked = new HashSet<>();
        for (int i = 1; i < args.length; i++) {
            blocked.add(Integer.parseInt(args[i]));
        }

        DistributedNode node = new DistributedNode(id, blocked);
        node.start();
    }
}