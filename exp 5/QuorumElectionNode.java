import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Distributed Node Daemon implementing Quorum-Based Leader Election
 * with Strict Split-Brain Prevention.
 *
 * Mathematical Invariant:
 * Majority Quorum Q = (N / 2) + 1. For N = 5, Q = 3.
 * Two majorities in a cluster of size N MUST overlap by at least 1 node (Pigeonhole Principle).
 * Thus, no more than one leader can ever be elected simultaneously, preventing split-brain.
 */
public class QuorumElectionNode {

    public enum State {
        FOLLOWER,
        CANDIDATE,
        LEADER,
        FENCED_MINORITY
    }

    public static final Map<Integer, Integer> CLUSTER_PORTS = Map.of(
            1, 8001,
            2, 8002,
            3, 8003,
            4, 8004,
            5, 8005
    );

    public static final int DRIVER_PORT = 8000;
    public static final int TOTAL_NODES = CLUSTER_PORTS.size(); // 5
    public static final int QUORUM_THRESHOLD = (TOTAL_NODES / 2) + 1; // 3

    private static final long HEARTBEAT_INTERVAL_MS = 800;
    private static final long ELECTION_TIMEOUT_MIN_MS = 1800;
    private static final long ELECTION_TIMEOUT_MAX_MS = 2600;
    private static final long LEADER_LEASE_TIMEOUT_MS = 2200;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final int nodeId;
    private final int port;
    private final Object stateLock = new Object();
    private final Set<Integer> blockedPeers = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Long> lastSeenPeerMs = new ConcurrentHashMap<>();

    // Consensus State
    private State state = State.FOLLOWER;
    private long currentTerm = 0;
    private Integer votedFor = null;
    private Integer currentLeaderId = null;
    private long lastLeaderHeartbeatMs = System.currentTimeMillis();
    private long electionTimeoutMs;

    private volatile boolean isRunning = true;
    private DatagramSocket socket;

    private final ExecutorService workerPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public QuorumElectionNode(int nodeId, Set<Integer> initialBlocked) {
        this.nodeId = nodeId;
        this.port = CLUSTER_PORTS.getOrDefault(nodeId, 8000 + nodeId);
        if (initialBlocked != null) {
            this.blockedPeers.addAll(initialBlocked);
        }
        resetElectionTimeout();

        try {
            this.socket = new DatagramSocket(this.port);
            this.socket.setSoTimeout(1000);
        } catch (Exception e) {
            System.err.println("Fatal: Node " + nodeId + " could not bind UDP port " + this.port + ": " + e.getMessage());
        }
    }

    private void resetElectionTimeout() {
        // Randomized election timeout to prevent split votes
        long range = ELECTION_TIMEOUT_MAX_MS - ELECTION_TIMEOUT_MIN_MS;
        this.electionTimeoutMs = ELECTION_TIMEOUT_MIN_MS + (long) (Math.random() * range);
    }

    private void log(String tag, String message) {
        String ts = LocalTime.now().format(TIME_FORMAT);
        String blockedStr = blockedPeers.isEmpty() ? "none" : blockedPeers.toString();
        System.out.println(String.format("[%s] [Node %d | Port %d | Term %d | %s] [%s] %s (Blocked: %s)",
                ts, nodeId, port, currentTerm, state, tag, message, blockedStr));
    }

    public void start() {
        log("BOOT", String.format("Node %d booted. Quorum required: %d/%d nodes. Initial state: %s",
                nodeId, QUORUM_THRESHOLD, TOTAL_NODES, state));

        // 1. Packet Receiver Thread
        Thread receiver = new Thread(this::runReceiverLoop, "Node-" + nodeId + "-Receiver");
        receiver.setDaemon(true);
        receiver.start();

        // 2. Leader Heartbeat Periodic Task
        scheduler.scheduleAtFixedRate(this::sendLeaderHeartbeat, 500, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // 3. Follower Watchdog & Election Timer Loop
        scheduler.scheduleAtFixedRate(this::watchdogCheck, 800, 400, TimeUnit.MILLISECONDS);

        // Main thread wait loop
        while (isRunning) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }

        shutdown();
    }

    public void shutdown() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        scheduler.shutdownNow();
        workerPool.shutdownNow();
        log("HALT", "Node " + nodeId + " cleanly halted.");
    }

    private void sendPacket(int targetPort, ElectionMessage msg) {
        try {
            byte[] bytes = msg.serialize().getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bytes, bytes.length, InetAddress.getByName("127.0.0.1"), targetPort
            );
            socket.send(packet);
        } catch (IOException ignored) {}
    }

    private void sendToNode(int targetId, ElectionMessage msg) {
        if (blockedPeers.contains(targetId)) {
            // Simulated network partition: packet dropped
            return;
        }
        int targetPort = (targetId == 0) ? DRIVER_PORT : CLUSTER_PORTS.getOrDefault(targetId, 8000 + targetId);
        sendPacket(targetPort, msg);
    }

    private void runReceiverLoop() {
        byte[] buffer = new byte[4096];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String raw = new String(packet.getData(), 0, packet.getLength());
                ElectionMessage msg = ElectionMessage.deserialize(raw);

                if (msg != null) {
                    if (blockedPeers.contains(msg.getSenderId())) {
                        // Drop packet due to simulated network partition
                        continue;
                    }
                    workerPool.submit(() -> handleMessage(msg));
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (isRunning) {
                    log("ERR", "Receiver error: " + e.getMessage());
                }
            }
        }
    }

    private void handleMessage(ElectionMessage msg) {
        lastSeenPeerMs.put(msg.getSenderId(), System.currentTimeMillis());

        synchronized (stateLock) {
            // Rule 1: Term Update. If incoming message has higher term, step down to Follower immediately
            if (msg.getTerm() > currentTerm) {
                log("TERM_ADVANCE", String.format("Observed higher Term %d from Node %d (local Term was %d). Demoting to FOLLOWER.",
                        msg.getTerm(), msg.getSenderId(), currentTerm));
                currentTerm = msg.getTerm();
                state = State.FOLLOWER;
                votedFor = null;
                currentLeaderId = null;
            }

            switch (msg.getType()) {
                case HEARTBEAT:
                    handleHeartbeat(msg);
                    break;
                case HEARTBEAT_ACK:
                    handleHeartbeatAck(msg);
                    break;
                case REQUEST_VOTE:
                    handleRequestVote(msg);
                    break;
                case VOTE_GRANT:
                case VOTE_REJECT:
                    // Processed synchronously in Candidate election loop
                    break;
                case COORDINATOR:
                    handleCoordinator(msg);
                    break;
                case WORKLOAD_REQ:
                    handleWorkload(msg);
                    break;
                case SET_PARTITION:
                    handleSetPartition(msg);
                    break;
                case HEAL_PARTITION:
                    handleHealPartition(msg);
                    break;
                case STATUS_REQ:
                    handleStatusRequest(msg);
                    break;
                case SHUTDOWN:
                    shutdown();
                    break;
                default:
                    break;
            }
        }
    }

    // ----------------------------------------------------------------------
    // Watchdog Timer & Quorum-Guarded Election Trigger
    // ----------------------------------------------------------------------
    private void watchdogCheck() {
        synchronized (stateLock) {
            long now = System.currentTimeMillis();

            // 1. Leader Quorum Check: Leader must maintain active majority heartbeats
            if (state == State.LEADER) {
                int reachableCount = countReachablePeers(now, LEADER_LEASE_TIMEOUT_MS);
                if (reachableCount < QUORUM_THRESHOLD) {
                    log("LEADER_FENCE", String.format("SPLIT-BRAIN GUARD: Leader lost majority quorum! Only %d/%d nodes reachable (Quorum requires %d). Stepping down immediately!",
                            reachableCount, TOTAL_NODES, QUORUM_THRESHOLD));
                    state = State.FENCED_MINORITY;
                    currentLeaderId = null;
                }
                return;
            }

            // 2. Follower / Fenced Watchdog: Check if leader has gone silent
            long elapsedSinceHeartbeat = now - lastLeaderHeartbeatMs;
            if (elapsedSinceHeartbeat > electionTimeoutMs) {
                int reachable = countReachablePeers(now, 4000);
                log("WATCHDOG_EXPIRY", String.format("Leader timeout elapsed (%dms > %dms). Visible cluster size: %d/%d nodes.",
                        elapsedSinceHeartbeat, electionTimeoutMs, reachable, TOTAL_NODES));

                // PRE-ELECTION QUORUM VERIFICATION (Split-Brain Prevention)
                if (reachable < QUORUM_THRESHOLD) {
                    log("SPLIT_BRAIN_PREVENTED", String.format("Node %d is in a MINORITY PARTITION (%d/%d reachable < Quorum %d). REFUSING to start election. State: FENCED_MINORITY.",
                            nodeId, reachable, TOTAL_NODES, QUORUM_THRESHOLD));
                    state = State.FENCED_MINORITY;
                    currentLeaderId = null;
                    resetElectionTimeout();
                    lastLeaderHeartbeatMs = now; // Suppress continuous election triggers
                } else {
                    log("QUORUM_OK", String.format("Majority reachable (%d/%d >= %d). Initiating Quorum-Based Election...",
                            reachable, TOTAL_NODES, QUORUM_THRESHOLD));
                    startQuorumElection();
                }
            }
        }
    }

    private int countReachablePeers(long now, long timeoutMs) {
        int count = 1; // Count self
        for (int peerId : CLUSTER_PORTS.keySet()) {
            if (peerId != nodeId && !blockedPeers.contains(peerId)) {
                Long lastSeen = lastSeenPeerMs.get(peerId);
                // If peer is alive or not partitioned
                if (lastSeen != null && (now - lastSeen < timeoutMs)) {
                    count++;
                } else if (lastSeen == null && !blockedPeers.contains(peerId)) {
                    // Node just booted and not blocked
                    count++;
                }
            }
        }
        return count;
    }

    // ----------------------------------------------------------------------
    // Quorum-Based Election Engine
    // ----------------------------------------------------------------------
    private void startQuorumElection() {
        currentTerm++;
        state = State.CANDIDATE;
        votedFor = nodeId;
        currentLeaderId = null;
        resetElectionTimeout();
        lastLeaderHeartbeatMs = System.currentTimeMillis();

        long electionTerm = currentTerm;
        log("ELECTION_START", String.format("Starting Election for Term %d. Self-voted (1 vote). Soliciting majority quorum (%d votes needed)...",
                electionTerm, QUORUM_THRESHOLD));

        // Launch election gathering in worker thread
        workerPool.submit(() -> runElectionVotesCollection(electionTerm));
    }

    private void runElectionVotesCollection(long electionTerm) {
        // Collect votes
        Set<Integer> voters = new HashSet<>();
        voters.add(nodeId); // Self vote

        DatagramSocket voteSocket = null;
        int votePort = port + 100;

        try {
            voteSocket = new DatagramSocket(votePort);
            voteSocket.setSoTimeout(1200);

            // Broadcast REQUEST_VOTE to all non-blocked peers
            for (int peerId : CLUSTER_PORTS.keySet()) {
                if (peerId != nodeId && !blockedPeers.contains(peerId)) {
                    ElectionMessage voteReq = new ElectionMessage(
                            ElectionMessage.Type.REQUEST_VOTE, nodeId, peerId, electionTerm,
                            String.valueOf(votePort), ""
                    );
                    sendToNode(peerId, voteReq);
                }
            }

            // Wait for vote responses
            byte[] buf = new byte[2048];
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < 1200) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    voteSocket.receive(pkt);
                    String raw = new String(pkt.getData(), 0, pkt.getLength());
                    ElectionMessage resp = ElectionMessage.deserialize(raw);

                    if (resp != null && resp.getTerm() == electionTerm && resp.getType() == ElectionMessage.Type.VOTE_GRANT) {
                        voters.add(resp.getSenderId());
                        log("VOTE_RECEIVED", String.format("Vote GRANTED by Node %d. Tally: %d/%d (Needed: %d)",
                                resp.getSenderId(), voters.size(), TOTAL_NODES, QUORUM_THRESHOLD));

                        if (voters.size() >= QUORUM_THRESHOLD) {
                            break; // Quorum satisfied!
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                    break;
                }
            }
        } catch (Exception e) {
            log("VOTE_ERR", "Election vote socket error: " + e.getMessage());
        } finally {
            if (voteSocket != null && !voteSocket.isClosed()) {
                voteSocket.close();
            }
        }

        // Evaluate Quorum Consensus
        synchronized (stateLock) {
            if (state != State.CANDIDATE || currentTerm != electionTerm) {
                return; // Stale election
            }

            if (voters.size() >= QUORUM_THRESHOLD) {
                // SUCCESS: Strict majority quorum obtained
                state = State.LEADER;
                currentLeaderId = nodeId;
                log("LEADER_PROCLAIM", String.format("MAJORITY CONSENSUS ATTAINED! Votes: %s (%d/%d >= Quorum %d). Declaring Node %d as Cluster LEADER for Term %d!",
                        voters, voters.size(), TOTAL_NODES, QUORUM_THRESHOLD, nodeId, electionTerm));

                // Broadcast COORDINATOR to all nodes
                ElectionMessage coordMsg = new ElectionMessage(
                        ElectionMessage.Type.COORDINATOR, nodeId, 0, electionTerm,
                        voters.toString(), "MAJORITY_ELECTED"
                );
                for (int peerId : CLUSTER_PORTS.keySet()) {
                    if (peerId != nodeId) {
                        sendToNode(peerId, coordMsg);
                    }
                }
                sendLeaderHeartbeat();
            } else {
                // FAILURE: Split-Brain Prevention Triggered
                log("SPLIT_BRAIN_PREVENTED", String.format("ELECTION FAILED: Gathered only %d/%d votes (Quorum threshold is %d). REFUSING leadership to prevent split-brain. Transitioning to FENCED_MINORITY.",
                        voters.size(), TOTAL_NODES, QUORUM_THRESHOLD));
                state = State.FENCED_MINORITY;
                currentLeaderId = null;
                resetElectionTimeout();
            }
        }
    }

    private void handleRequestVote(ElectionMessage msg) {
        int candidateId = msg.getSenderId();
        long candidateTerm = msg.getTerm();
        int replyPort = CLUSTER_PORTS.get(candidateId);
        try {
            if (!msg.getPayload().isEmpty()) {
                replyPort = Integer.parseInt(msg.getPayload());
            }
        } catch (Exception ignored) {}

        boolean grant = false;
        String reason = "";

        // Vote grant rules:
        // 1. Candidate's term must be >= currentTerm
        // 2. Node hasn't voted for anyone else in this term
        // 3. Node must not be in a partition that blocks candidate
        if (candidateTerm >= currentTerm && (votedFor == null || votedFor == candidateId)) {
            grant = true;
            votedFor = candidateId;
            currentTerm = candidateTerm;
            lastLeaderHeartbeatMs = System.currentTimeMillis(); // Reset timer on granting vote
            reason = "TERM_VALID_NOT_VOTED";
            log("VOTE_GRANT", String.format("Voted FOR Candidate Node %d in Term %d", candidateId, candidateTerm));
        } else {
            reason = String.format("ALREADY_VOTED_FOR_%s_OR_LOWER_TERM", votedFor);
            log("VOTE_DENY", String.format("DENIED vote to Node %d for Term %d (votedFor=%s, localTerm=%d)",
                    candidateId, candidateTerm, votedFor, currentTerm));
        }

        ElectionMessage resp = new ElectionMessage(
                grant ? ElectionMessage.Type.VOTE_GRANT : ElectionMessage.Type.VOTE_REJECT,
                nodeId, candidateId, currentTerm, reason, ""
        );
        sendPacket(replyPort, resp);
    }

    private void handleCoordinator(ElectionMessage msg) {
        if (msg.getSenderId() == nodeId) return;
        int newLeader = msg.getSenderId();
        long term = msg.getTerm();

        log("COORDINATOR_RECOGNIZE", String.format("Recognized Node %d as Cluster Leader for Term %d. Electing Quorum: %s",
                newLeader, term, msg.getPayload()));

        currentLeaderId = newLeader;
        currentTerm = term;
        state = State.FOLLOWER;
        votedFor = null;
        lastLeaderHeartbeatMs = System.currentTimeMillis();
    }

    // ----------------------------------------------------------------------
    // Heartbeat & Leases
    // ----------------------------------------------------------------------
    private void sendLeaderHeartbeat() {
        synchronized (stateLock) {
            if (state != State.LEADER) return;

            ElectionMessage hb = new ElectionMessage(
                    ElectionMessage.Type.HEARTBEAT, nodeId, 0, currentTerm,
                    "HEARTBEAT", String.valueOf(QUORUM_THRESHOLD)
            );
            for (int peerId : CLUSTER_PORTS.keySet()) {
                if (peerId != nodeId) {
                    sendToNode(peerId, hb);
                }
            }
        }
    }

    private void handleHeartbeat(ElectionMessage msg) {
        if (msg.getSenderId() == nodeId) return;
        lastLeaderHeartbeatMs = System.currentTimeMillis();
        currentLeaderId = msg.getSenderId();
        if (state == State.FENCED_MINORITY || state == State.CANDIDATE) {
            log("FENCE_LIFTED", String.format("Valid heartbeat received from Leader Node %d in Term %d. Restoring state to FOLLOWER.",
                    msg.getSenderId(), msg.getTerm()));
            state = State.FOLLOWER;
        }

        // Send Heartbeat ACK back to leader
        ElectionMessage ack = new ElectionMessage(
                ElectionMessage.Type.HEARTBEAT_ACK, nodeId, msg.getSenderId(), currentTerm, "ACK", ""
        );
        sendToNode(msg.getSenderId(), ack);
    }

    private void handleHeartbeatAck(ElectionMessage msg) {
        lastSeenPeerMs.put(msg.getSenderId(), System.currentTimeMillis());
    }

    // ----------------------------------------------------------------------
    // Workload Processing (Split-Brain Guarded)
    // ----------------------------------------------------------------------
    private void handleWorkload(ElectionMessage msg) {
        String task = msg.getPayload();
        int clientPort = (msg.getSenderId() == 0) ? DRIVER_PORT : 8099;

        if (state == State.LEADER) {
            log("WORKLOAD_EXEC", String.format("Authorized Leader executing task: '%s'. Quorum lease valid.", task));
            ElectionMessage resp = new ElectionMessage(
                    ElectionMessage.Type.WORKLOAD_RESP, nodeId, msg.getSenderId(), currentTerm,
                    "SUCCESS_EXECUTED_BY_LEADER_N" + nodeId, "OK"
            );
            sendPacket(clientPort, resp);
        } else if (state == State.FENCED_MINORITY) {
            log("SPLIT_BRAIN_REJECT", String.format("SPLIT-BRAIN GUARD: Rejected task '%s'! Node %d is FENCED in a minority partition.", task, nodeId));
            ElectionMessage resp = new ElectionMessage(
                    ElectionMessage.Type.WORKLOAD_RESP, nodeId, msg.getSenderId(), currentTerm,
                    "REJECTED_SPLIT_BRAIN_FENCED_MINORITY", "FAIL_NO_QUORUM"
            );
            sendPacket(clientPort, resp);
        } else {
            log("WORKLOAD_REDIRECT", String.format("Follower Node %d cannot execute task '%s'. Current leader is Node %s.",
                    nodeId, task, currentLeaderId));
            ElectionMessage resp = new ElectionMessage(
                    ElectionMessage.Type.WORKLOAD_RESP, nodeId, msg.getSenderId(), currentTerm,
                    "NOT_LEADER_REDIRECT_TO_N" + currentLeaderId, "REDIRECT"
            );
            sendPacket(clientPort, resp);
        }
    }

    // ----------------------------------------------------------------------
    // Dynamic Partition Manipulation & Status
    // ----------------------------------------------------------------------
    private void handleSetPartition(ElectionMessage msg) {
        blockedPeers.clear();
        if (!msg.getPayload().isEmpty()) {
            String[] tokens = msg.getPayload().split(",");
            for (String tok : tokens) {
                try {
                    blockedPeers.add(Integer.parseInt(tok.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        log("PARTITION_APPLIED", "Network partition updated. Blocked peer set: " + blockedPeers);
    }

    private void handleHealPartition(ElectionMessage msg) {
        blockedPeers.clear();
        log("PARTITION_HEALED", "Network partition healed. Full connectivity restored.");
        lastLeaderHeartbeatMs = System.currentTimeMillis();
        if (state == State.FENCED_MINORITY) {
            state = State.FOLLOWER;
        }
    }

    private void handleStatusRequest(ElectionMessage msg) {
        String info = String.format("State=%s;Term=%d;Leader=%s;Blocked=%s;Reachable=%d",
                state, currentTerm, currentLeaderId, blockedPeers, countReachablePeers(System.currentTimeMillis(), 4000));
        ElectionMessage resp = new ElectionMessage(
                ElectionMessage.Type.STATUS_RESP, nodeId, msg.getSenderId(), currentTerm,
                info, state.name()
        );
        int targetPort = (msg.getSenderId() == 0) ? DRIVER_PORT : 8099;
        sendPacket(targetPort, resp);
    }

    // ----------------------------------------------------------------------
    // Main Entry Point
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java QuorumElectionNode <nodeId> [blockedPeer1,blockedPeer2,...]");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        Set<Integer> blocked = new HashSet<>();
        if (args.length > 1 && !args[1].isEmpty() && !args[1].equalsIgnoreCase("none")) {
            String[] parts = args[1].split(",");
            for (String p : parts) {
                try {
                    blocked.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        QuorumElectionNode node = new QuorumElectionNode(nodeId, blocked);
        node.start();
    }
}
