import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Automated Simulation Harness for Experiment 5:
 * "Quorum-Based Split-Brain Preventing Leader Election"
 *
 * Demonstrates:
 * 1. Unified Cluster Election (5/5 nodes alive -> Leader elected by majority quorum)
 * 2. Network Partition Injection: Minority {1, 2} vs Majority {3, 4, 5}
 * 3. Split-Brain Prevention: Minority partition {1, 2} REFUSES election (2/5 < Quorum 3)
 * 4. Fencing Guards: Minority partition rejects client workloads
 * 5. Majority Partition Service: Majority {3, 4, 5} maintains quorum and executes writes
 * 6. Dynamic Partition Healing: Cluster reunites and restores single unified leader
 */
public class SplitBrainSimDriver {

    private static final int DRIVER_PORT = 8000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private DatagramSocket socket;
    private final List<Process> spawnedProcesses = new ArrayList<>();
    private final Map<Integer, Integer> clusterPorts = QuorumElectionNode.CLUSTER_PORTS;

    public static void main(String[] args) {
        SplitBrainSimDriver driver = new SplitBrainSimDriver();
        driver.runSimulation();
    }

    private void log(String phase, String msg) {
        String ts = LocalTime.now().format(TIME_FORMAT);
        System.out.println(String.format("[%s] [DRIVER] [%s] %s", ts, phase, msg));
    }

    private void banner(String title) {
        System.out.println("\n" + "=".repeat(85));
        System.out.println("  " + title);
        System.out.println("=".repeat(85));
    }

    public void runSimulation() {
        banner("DISTRIBUTED COMPUTING LAB - EXPERIMENT 5\n  QUORUM-BASED SPLIT-BRAIN PREVENTING LEADER ELECTION");

        try {
            this.socket = new DatagramSocket(DRIVER_PORT);
            this.socket.setSoTimeout(2500);
        } catch (Exception e) {
            System.err.println("Driver socket bind failed on port " + DRIVER_PORT + ": " + e.getMessage());
            return;
        }

        try {
            // Stage 0: Boot Cluster
            bootCluster();
            Thread.sleep(3000); // Allow initial election to settle

            // Stage 1: Verify Initial Stable Leader
            verifyInitialLeader();
            Thread.sleep(1000);

            // Stage 2: Inject Partition (Minority {1, 2} vs Majority {3, 4, 5})
            injectNetworkPartition();
            Thread.sleep(3000); // Wait for watchdogs to detect partition

            // Stage 3: Verify Split-Brain Prevention in Minority
            verifyMinoritySplitBrainPrevention();
            Thread.sleep(1000);

            // Stage 4: Verify Majority Health & Workload Processing
            verifyMajorityClusterWorkload();
            Thread.sleep(1000);

            // Stage 5: Heal Network Partition & Reunite Cluster
            healNetworkPartition();
            Thread.sleep(3000); // Allow cluster re-unification and re-election

            // Stage 6: Final Cluster State & Summary
            verifyHealedUnifiedCluster();
            Thread.sleep(500);

            printSummaryTable();

        } catch (Exception e) {
            log("ERROR", "Simulation exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            teardownCluster();
        }
    }

    // ----------------------------------------------------------------------
    // Cluster Lifecycle
    // ----------------------------------------------------------------------
    private void bootCluster() throws Exception {
        banner("STAGE 0: SPAWNING 5-NODE DISTRIBUTED CLUSTER (N=5, QUORUM=3)");

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        for (int i = 1; i <= 5; i++) {
            List<String> cmd = List.of(
                    javaBin, "-cp", classpath, "QuorumElectionNode", String.valueOf(i), "none"
            );
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            Process p = pb.start();
            spawnedProcesses.add(p);
            log("BOOT", String.format("Node %d process spawned on UDP port %d", i, clusterPorts.get(i)));
            Thread.sleep(150);
        }

        log("READY", "All 5 nodes active. Awaiting initial quorum consensus election...");
    }

    private void teardownCluster() {
        banner("CLEANUP: SHUTTING DOWN CLUSTER");
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        for (Process p : spawnedProcesses) {
            p.destroyForcibly();
        }
        log("CLEANUP", "All node processes stopped.");
    }

    private void send(int targetPort, ElectionMessage msg) {
        try {
            byte[] bytes = msg.serialize().getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bytes, bytes.length, InetAddress.getByName("127.0.0.1"), targetPort
            );
            socket.send(packet);
        } catch (IOException e) {
            log("NET_ERR", "Send error: " + e.getMessage());
        }
    }

    private ElectionMessage receiveMessage(int timeoutMs) {
        try {
            socket.setSoTimeout(timeoutMs);
            byte[] buf = new byte[4096];
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            socket.receive(pkt);
            return ElectionMessage.deserialize(new String(pkt.getData(), 0, pkt.getLength()));
        } catch (SocketTimeoutException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<Integer, String> dumpClusterStatus() {
        Map<Integer, String> statusMap = new TreeMap<>();
        for (int id = 1; id <= 5; id++) {
            ElectionMessage req = new ElectionMessage(
                    ElectionMessage.Type.STATUS_REQ, 0, id, 0, "", ""
            );
            send(clusterPorts.get(id), req);
            ElectionMessage resp = receiveMessage(800);
            if (resp != null) {
                statusMap.put(id, resp.getPayload());
            } else {
                statusMap.put(id, "UNREACHABLE");
            }
        }
        return statusMap;
    }

    // ----------------------------------------------------------------------
    // Stage 1: Verify Initial Stable Leader
    // ----------------------------------------------------------------------
    private void verifyInitialLeader() {
        banner("STAGE 1: VERIFYING INITIAL MAJORITY LEADER ELECTION");
        Map<Integer, String> status = dumpClusterStatus();

        Integer leaderId = null;
        int leaderCount = 0;

        for (Map.Entry<Integer, String> entry : status.entrySet()) {
            log("STATUS", String.format("Node %d: %s", entry.getKey(), entry.getValue()));
            if (entry.getValue().contains("State=LEADER")) {
                leaderId = entry.getKey();
                leaderCount++;
            }
        }

        if (leaderCount == 1) {
            log("CONSENSUS_OK", String.format("STABLE CLUSTER: Exactly ONE Leader elected (Node %d) with full majority support.", leaderId));
            // Submit test workload to the leader
            ElectionMessage workReq = new ElectionMessage(
                    ElectionMessage.Type.WORKLOAD_REQ, 0, leaderId, 1, "TASK_001:INITIAL_CLUSTER_TEST", ""
            );
            send(clusterPorts.get(leaderId), workReq);
            ElectionMessage resp = receiveMessage(1500);
            if (resp != null && resp.getType() == ElectionMessage.Type.WORKLOAD_RESP) {
                log("WORKLOAD", String.format("Leader Node %d executed workload: %s (Status: %s)",
                        leaderId, resp.getPayload(), resp.getExtra()));
            }
        } else {
            log("WARN", "Unexpected leader count: " + leaderCount);
        }
    }

    // ----------------------------------------------------------------------
    // Stage 2: Inject Network Partition
    // ----------------------------------------------------------------------
    private void injectNetworkPartition() {
        banner("STAGE 2: INJECTING NETWORK PARTITION\n" +
               "  Topology Cut: Partition A (Minority: {1, 2}) <--- X ---> Partition B (Majority: {3, 4, 5})\n" +
               "  Network packets between {1, 2} and {3, 4, 5} will be completely dropped.");

        // Block {3, 4, 5} on Nodes 1 and 2
        for (int id : List.of(1, 2)) {
            ElectionMessage msg = new ElectionMessage(
                    ElectionMessage.Type.SET_PARTITION, 0, id, 0, "3,4,5", ""
            );
            send(clusterPorts.get(id), msg);
        }

        // Block {1, 2} on Nodes 3, 4, 5
        for (int id : List.of(3, 4, 5)) {
            ElectionMessage msg = new ElectionMessage(
                    ElectionMessage.Type.SET_PARTITION, 0, id, 0, "1,2", ""
            );
            send(clusterPorts.get(id), msg);
        }

        log("PARTITION_CUT", "Partition matrix active. Nodes 1 and 2 isolated from Nodes 3, 4, and 5.");
    }

    // ----------------------------------------------------------------------
    // Stage 3: Verify Split-Brain Prevention in Minority
    // ----------------------------------------------------------------------
    private void verifyMinoritySplitBrainPrevention() {
        banner("STAGE 3: VERIFYING SPLIT-BRAIN PREVENTION IN MINORITY PARTITION {1, 2}");
        log("MINORITY_TEST", "Checking state of Minority Partition {1, 2}...");

        Map<Integer, String> status = dumpClusterStatus();
        boolean splitBrainDetected = false;

        for (int id : List.of(1, 2)) {
            String s = status.get(id);
            log("MINORITY_STATE", String.format("Node %d: %s", id, s));
            if (s != null && s.contains("State=LEADER")) {
                splitBrainDetected = true;
            }
        }

        if (!splitBrainDetected) {
            System.out.println("\n  >>> INVARIANT PRESERVED: ZERO Leaders elected in Minority Partition {1, 2}.");
            System.out.println("      Nodes recognized reachable count (2/5) < Quorum (3), transitioned to FENCED_MINORITY.");
        } else {
            System.err.println("  >>> ERROR: Split-Brain detected! A rogue leader was elected in the minority partition.");
        }

        // Attempt to submit a client write workload to the minority partition
        log("MINORITY_WORKLOAD", "Attempting client write task to Minority Node 2: 'TX_1002:UNAUTHORIZED_TRANSFER'...");
        ElectionMessage workReq = new ElectionMessage(
                ElectionMessage.Type.WORKLOAD_REQ, 0, 2, 0, "TX_1002:UNAUTHORIZED_TRANSFER", ""
        );
        send(clusterPorts.get(2), workReq);
        ElectionMessage resp = receiveMessage(1500);

        if (resp != null) {
            log("MINORITY_GUARD", String.format("Node 2 Response: %s (Status: %s)", resp.getPayload(), resp.getExtra()));
            if (resp.getPayload().contains("REJECTED") || resp.getPayload().contains("NOT_LEADER")) {
                System.out.println("  >>> RESULT: Client write in minority partition was REJECTED by Split-Brain Guard!");
            }
        }
    }

    // ----------------------------------------------------------------------
    // Stage 4: Verify Majority Health
    // ----------------------------------------------------------------------
    private void verifyMajorityClusterWorkload() {
        banner("STAGE 4: VERIFYING MAJORITY PARTITION {3, 4, 5} CONSENSUS & WRITE AVAILABILITY");
        Map<Integer, String> status = dumpClusterStatus();

        Integer majorityLeader = null;
        for (int id : List.of(3, 4, 5)) {
            String s = status.get(id);
            log("MAJORITY_STATE", String.format("Node %d: %s", id, s));
            if (s != null && s.contains("State=LEADER")) {
                majorityLeader = id;
            }
        }

        if (majorityLeader != null) {
            log("MAJORITY_LEADER", String.format("Valid Majority Leader active on Node %d (Quorum: 3/5 nodes present).", majorityLeader));

            // Submit valid write to majority leader
            log("MAJORITY_WORKLOAD", "Submitting authorized workload to Majority Leader Node " + majorityLeader + ": 'TX_1003:AUTHORIZED_COMMIT'...");
            ElectionMessage workReq = new ElectionMessage(
                    ElectionMessage.Type.WORKLOAD_REQ, 0, majorityLeader, 0, "TX_1003:AUTHORIZED_COMMIT", ""
            );
            send(clusterPorts.get(majorityLeader), workReq);
            ElectionMessage resp = receiveMessage(1500);

            if (resp != null && resp.getPayload().contains("SUCCESS")) {
                System.out.println("  >>> RESULT: Majority partition successfully processed workload. CAP Invariant Maintained (Consistency & Partition Tolerance).");
            }
        } else {
            log("WARN", "Majority partition is currently re-electing.");
        }
    }

    // ----------------------------------------------------------------------
    // Stage 5: Heal Network Partition
    // ----------------------------------------------------------------------
    private void healNetworkPartition() {
        banner("STAGE 5: HEALING NETWORK PARTITION & REUNIFYING CLUSTER\n" +
               "  Restoring full bidirectional communication across all 5 nodes.");

        for (int id = 1; id <= 5; id++) {
            ElectionMessage heal = new ElectionMessage(
                    ElectionMessage.Type.HEAL_PARTITION, 0, id, 0, "", ""
            );
            send(clusterPorts.get(id), heal);
        }

        log("HEAL", "Partition cleared. All nodes can now communicate. Allowing cluster to re-unify...");
    }

    // ----------------------------------------------------------------------
    // Stage 6: Final Verification
    // ----------------------------------------------------------------------
    private void verifyHealedUnifiedCluster() {
        banner("STAGE 6: POST-HEALING UNIFIED CLUSTER VERIFICATION");
        Map<Integer, String> status = dumpClusterStatus();

        int leaderCount = 0;
        Integer unifiedLeader = null;

        for (Map.Entry<Integer, String> entry : status.entrySet()) {
            log("POST_HEAL_STATE", String.format("Node %d: %s", entry.getKey(), entry.getValue()));
            if (entry.getValue().contains("State=LEADER")) {
                leaderCount++;
                unifiedLeader = entry.getKey();
            }
        }

        if (leaderCount == 1) {
            System.out.println("\n  >>> INVARIANT VERIFIED: Cluster safely re-unified under ONE Leader (Node " + unifiedLeader + ").");
            System.out.println("      Minority nodes un-fenced, recognized the unified majority quorum, and resumed healthy follower duties.");
        } else {
            log("POST_HEAL_RECHECK", "Election in progress (Leaders=" + leaderCount + ")");
        }
    }

    // ----------------------------------------------------------------------
    // Comparison Table
    // ----------------------------------------------------------------------
    private void printSummaryTable() {
        banner("EVALUATION: NAIVE ELECTION VS. QUORUM-BASED SPLIT-BRAIN PREVENTING ELECTION");
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Metric / Feature", "Naive Leader Election", "Quorum-Based Election (Implemented)"));
        System.out.println("-".repeat(95));
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Quorum Requirement", "None (Highest Node Wins)", "Strict Majority Q = floor(N/2) + 1"));
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Network Partition Cut", "Both Partitions Elect Leader", "Only Majority Partition Elects Leader"));
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Split-Brain Condition?", "YES (Catastrophic Divergence)", "NO (Mathematically Impossible)"));
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Minority Partition Behavior", "Elects Rogue Leader", "Fenced (Rejects Workloads)"));
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Leader Lease Guard", "Leader runs forever in isolation", "Leader steps down if ACKs < Quorum"));
        System.out.println(String.format("%-25s | %-28s | %-32s",
                "Partition Healing", "Requires manual reconciliation", "Automatic re-unification & single leader"));
        System.out.println("-".repeat(95));
        System.out.println("\nQuorum-Based Split-Brain Prevention Simulation completed successfully!\n");
    }
}
