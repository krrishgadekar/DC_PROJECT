import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClockNode {
    private static final Map<Integer, Integer> NODE_PORTS = Map.of(
        1, 6001,
        2, 6002,
        3, 6003
    );

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final int nodeId;
    private final int port;
    private final boolean isMaster;
    private final Object clockLock = new Object();

    // Logical Clock State
    private int lamportClock = 0;

    // Simulated Physical Clock (with artificial initial drift in ms)
    private long physicalClockOffsetMs;
    private volatile boolean isRunning = true;
    private DatagramSocket socket;

    public ClockNode(int nodeId, boolean isMaster, long initialDriftMs) {
        this.nodeId = nodeId;
        this.port = NODE_PORTS.get(nodeId);
        this.isMaster = isMaster;
        this.physicalClockOffsetMs = initialDriftMs;

        try {
            this.socket = new DatagramSocket(this.port);
            this.socket.setSoTimeout(800);
        } catch (Exception e) {
            System.err.println("Error initializing socket on port " + this.port + ": " + e.getMessage());
        }
    }

    private long getPhysicalTime() {
        return System.currentTimeMillis() + physicalClockOffsetMs;
    }

    private void log(String tag, String message) {
        String ts = LocalTime.now().format(TIME_FORMAT);
        long pTime = getPhysicalTime();
        System.out.println(String.format("[%s] [Node %d | Lamport: %d | PhysTime: %dms] [%s] %s",
                ts, nodeId, lamportClock, pTime, tag, message));
    }

    // Lamport Rule 1: Local tick
    private int tickLogicalClock() {
        synchronized (clockLock) {
            lamportClock++;
            return lamportClock;
        }
    }

    // Lamport Rule 2: Message receipt update
    private int updateLogicalClockOnReceive(int receivedClock) {
        synchronized (clockLock) {
            lamportClock = Math.max(lamportClock, receivedClock) + 1;
            return lamportClock;
        }
    }

    // Thread 1: Message Listener (Logical + Berkeley Handling)
    private void runListenerThread() {
        byte[] buffer = new byte[2048];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String rawMsg = new String(packet.getData(), 0, packet.getLength());
                String[] parts = rawMsg.split("\\|");

                String msgType = parts[0];
                int senderId = Integer.parseInt(parts[1]);
                int receivedLamport = Integer.parseInt(parts[2]);

                // Update Lamport Clock on any incoming message
                updateLogicalClockOnReceive(receivedLamport);

                if ("APP_MSG".equals(msgType)) {
                    String data = parts[3];
                    log("RECV_MSG", String.format("Received '%s' from Node %d (Sender L=%d)",
                            data, senderId, receivedLamport));
                } else if ("TIME_REQ".equals(msgType)) {
                    // Berkeley Master requesting physical time
                    long localPhysTime = getPhysicalTime();
                    int clk = tickLogicalClock();
                    String reply = String.format("TIME_RESP|%d|%d|%d", nodeId, clk, localPhysTime);
                    byte[] replyBytes = reply.getBytes();
                    DatagramPacket replyPacket = new DatagramPacket(
                        replyBytes, replyBytes.length, InetAddress.getByName("127.0.0.1"), NODE_PORTS.get(senderId)
                    );
                    socket.send(replyPacket);
                } else if ("TIME_ADJ".equals(msgType)) {
                    // Berkeley Master sending offset adjustment
                    long offsetAdjustment = Long.parseLong(parts[3]);
                    synchronized (clockLock) {
                        physicalClockOffsetMs += offsetAdjustment;
                    }
                    log("BERKELEY_SYNC", String.format("Applied Clock Offset: %+d ms. New PhysTime: %d ms",
                            offsetAdjustment, getPhysicalTime()));
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (isRunning) {
                    log("ERROR", "Listener error: " + e.getMessage());
                }
            }
        }
    }

    // Thread 2: Application Traffic Generator (Generates Causal Events)
    private void runWorkerThread() {
        int eventCount = 0;
        while (isRunning) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                break;
            }

            eventCount++;
            int targetNode = (nodeId % NODE_PORTS.size()) + 1;
            int clk = tickLogicalClock();
            String payload = String.format("APP_MSG|%d|%d|Transaction_%d_from_N%d",
                    nodeId, clk, eventCount, nodeId);
            byte[] bytes = payload.getBytes();

            try {
                DatagramPacket packet = new DatagramPacket(
                    bytes, bytes.length, InetAddress.getByName("127.0.0.1"), NODE_PORTS.get(targetNode)
                );
                socket.send(packet);
                log("SEND_MSG", String.format("Sent Transaction_%d to Node %d (Local L=%d)",
                        eventCount, targetNode, clk));
            } catch (IOException ignored) {}
        }
    }

    // Thread 3: Berkeley Master Coordinator Thread (Only active on Master Node)
    private void runMasterBerkeleyCoordinator() {
        if (!isMaster) return;

        while (isRunning) {
            try {
                Thread.sleep(6000); // Trigger physical sync round every 6 seconds
            } catch (InterruptedException e) {
                break;
            }

            log("BERKELEY_MASTER", "Initiating Berkeley Physical Clock Synchronization Round...");
            Map<Integer, Long> nodeTimes = new HashMap<>();
            nodeTimes.put(nodeId, getPhysicalTime());

            // Poll worker nodes for physical timestamps
            for (int peerId : NODE_PORTS.keySet()) {
                if (peerId == nodeId) continue;
                int clk = tickLogicalClock();
                String req = String.format("TIME_REQ|%d|%d|0", nodeId, clk);
                byte[] bytes = req.getBytes();
                try {
                    DatagramPacket packet = new DatagramPacket(
                        bytes, bytes.length, InetAddress.getByName("127.0.0.1"), NODE_PORTS.get(peerId)
                    );
                    socket.send(packet);
                } catch (Exception ignored) {}
            }

            // Collect responses (simplified collection window)
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {}

            // Compute Average Physical Time
            long sum = 0;
            for (long t : nodeTimes.values()) {
                sum += t;
            }
            long avgTime = sum / nodeTimes.size();
            log("BERKELEY_MASTER", String.format("Cluster Avg Physical Time: %d ms", avgTime));

            // Broadcast adjustments
            for (int peerId : NODE_PORTS.keySet()) {
                long currentT = nodeTimes.getOrDefault(peerId, getPhysicalTime());
                long adjustment = avgTime - currentT;

                if (peerId == nodeId) {
                    synchronized (clockLock) {
                        physicalClockOffsetMs += adjustment;
                    }
                } else {
                    int clk = tickLogicalClock();
                    String adjMsg = String.format("TIME_ADJ|%d|%d|%d", nodeId, clk, adjustment);
                    byte[] bytes = adjMsg.getBytes();
                    try {
                        DatagramPacket packet = new DatagramPacket(
                            bytes, bytes.length, InetAddress.getByName("127.0.0.1"), NODE_PORTS.get(peerId)
                        );
                        socket.send(packet);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public void start() {
        log("INIT", String.format("Booting Node %d (Master=%b) on port %d with initial clock drift: %+d ms",
                nodeId, isMaster, port, physicalClockOffsetMs));

        Thread listener = new Thread(this::runListenerThread, "Listener-Thread");
        Thread worker = new Thread(this::runWorkerThread, "Worker-Thread");
        Thread masterSync = new Thread(this::runMasterBerkeleyCoordinator, "Berkeley-Thread");

        listener.setDaemon(true);
        worker.setDaemon(true);
        masterSync.setDaemon(true);

        listener.start();
        worker.start();
        masterSync.start();

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
        if (args.length < 3) {
            System.out.println("Usage: java ClockNode <node_id> <is_master:true/false> <drift_ms>");
            return;
        }

        int id = Integer.parseInt(args[0]);
        boolean master = Boolean.parseBoolean(args[1]);
        long drift = Long.parseLong(args[2]);

        ClockNode node = new ClockNode(id, master, drift);
        node.start();
    }
}