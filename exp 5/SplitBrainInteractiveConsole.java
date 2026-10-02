import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * Interactive Console for Quorum-based Split-Brain Preventing Election.
 * Allows testers and evaluators to manually inject partitions, trigger healing,
 * query node states, and attempt client workloads.
 */
public class SplitBrainInteractiveConsole {

    private static final int CONSOLE_PORT = 8099;
    private final Map<Integer, Integer> clusterPorts = QuorumElectionNode.CLUSTER_PORTS;
    private DatagramSocket socket;

    public SplitBrainInteractiveConsole() {
        try {
            this.socket = new DatagramSocket(CONSOLE_PORT);
            this.socket.setSoTimeout(1500);
        } catch (Exception e) {
            System.err.println("Could not bind console port " + CONSOLE_PORT + ": " + e.getMessage());
        }
    }

    private void send(int targetPort, ElectionMessage msg) {
        try {
            byte[] bytes = msg.serialize().getBytes();
            DatagramPacket packet = new DatagramPacket(
                    bytes, bytes.length, InetAddress.getByName("127.0.0.1"), targetPort
            );
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }

    private ElectionMessage receiveMessage(int timeoutMs) {
        try {
            socket.setSoTimeout(timeoutMs);
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            return ElectionMessage.deserialize(new String(packet.getData(), 0, packet.getLength()));
        } catch (SocketTimeoutException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("===========================================================================");
        System.out.println("   QUORUM-BASED SPLIT-BRAIN PREVENTING ELECTION: INTERACTIVE CLI");
        System.out.println("   Nodes: 1, 2, 3, 4, 5 (Ports: 8001 - 8005) | Majority Quorum: 3/5");
        System.out.println("   Type 'help' for available commands or 'exit' to quit.");
        System.out.println("===========================================================================");

        while (true) {
            System.out.print("\nSplitBrain-CLI> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String cmd = tokens[0].toLowerCase();

            if (cmd.equals("exit") || cmd.equals("quit")) {
                System.out.println("Exiting console. Goodbye!");
                break;
            } else if (cmd.equals("help")) {
                printHelp();
            } else if (cmd.equals("status")) {
                showStatus();
            } else if (cmd.equals("partition")) {
                if (tokens.length < 3) {
                    System.out.println("Usage: partition <group1> <group2>");
                    System.out.println("Example: partition 1,2 3,4,5");
                    continue;
                }
                injectPartition(tokens[1], tokens[2]);
            } else if (cmd.equals("heal")) {
                healPartition();
            } else if (cmd.equals("workload")) {
                if (tokens.length < 3) {
                    System.out.println("Usage: workload <nodeId> <taskName>");
                    System.out.println("Example: workload 5 write_record_101");
                    continue;
                }
                int targetNode = Integer.parseInt(tokens[1]);
                String task = tokens[2];
                submitWorkload(targetNode, task);
            } else if (cmd.equals("kill")) {
                if (tokens.length < 2) {
                    System.out.println("Usage: kill <nodeId>");
                    continue;
                }
                int targetNode = Integer.parseInt(tokens[1]);
                killNode(targetNode);
            } else {
                System.out.println("Unknown command: '" + cmd + "'. Type 'help' for instructions.");
            }
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void printHelp() {
        System.out.println("\nSupported Commands:");
        System.out.println("  status                          - Display state, term, leader, and partitions of all 5 nodes");
        System.out.println("  partition <grp1> <grp2>         - Partition cluster (e.g., 'partition 1,2 3,4,5')");
        System.out.println("  heal                            - Clear all partitions and restore full network connectivity");
        System.out.println("  workload <nodeId> <taskName>    - Submit write task to node (verifies split-brain guard)");
        System.out.println("  kill <nodeId>                   - Shut down a node to simulate node crash");
        System.out.println("  help                            - Show this manual");
        System.out.println("  exit                            - Exit console");
    }

    private void showStatus() {
        System.out.println("\n--- CLUSTER RUNTIME TOPOLOGY & STATE ---");
        for (int id = 1; id <= 5; id++) {
            ElectionMessage req = new ElectionMessage(
                    ElectionMessage.Type.STATUS_REQ, 99, id, 0, "", ""
            );
            send(clusterPorts.get(id), req);
            ElectionMessage resp = receiveMessage(800);
            if (resp != null) {
                System.out.println(String.format("  Node %d (Port %d): %s", id, clusterPorts.get(id), resp.getPayload()));
            } else {
                System.out.println(String.format("  Node %d (Port %d): UNREACHABLE / DOWN", id, clusterPorts.get(id)));
            }
        }
    }

    private void injectPartition(String grp1, String grp2) {
        System.out.println(String.format("Cutting network between Partition A {%s} and Partition B {%s}...", grp1, grp2));

        String[] nodes1 = grp1.split(",");
        String[] nodes2 = grp2.split(",");

        for (String n1 : nodes1) {
            int id1 = Integer.parseInt(n1.trim());
            ElectionMessage msg = new ElectionMessage(
                    ElectionMessage.Type.SET_PARTITION, 99, id1, 0, grp2, ""
            );
            send(clusterPorts.get(id1), msg);
        }

        for (String n2 : nodes2) {
            int id2 = Integer.parseInt(n2.trim());
            ElectionMessage msg = new ElectionMessage(
                    ElectionMessage.Type.SET_PARTITION, 99, id2, 0, grp1, ""
            );
            send(clusterPorts.get(id2), msg);
        }

        System.out.println("Partition applied successfully.");
    }

    private void healPartition() {
        System.out.println("Sending HEAL command to all 5 nodes...");
        for (int id = 1; id <= 5; id++) {
            ElectionMessage msg = new ElectionMessage(
                    ElectionMessage.Type.HEAL_PARTITION, 99, id, 0, "", ""
            );
            send(clusterPorts.get(id), msg);
        }
        System.out.println("Full cluster connectivity restored.");
    }

    private void submitWorkload(int targetNode, String task) {
        System.out.println(String.format("Submitting task '%s' to Node %d...", task, targetNode));
        ElectionMessage req = new ElectionMessage(
                ElectionMessage.Type.WORKLOAD_REQ, 99, targetNode, 0, task, ""
        );
        send(clusterPorts.get(targetNode), req);
        ElectionMessage resp = receiveMessage(1500);

        if (resp != null) {
            System.out.println(String.format("Node %d Response: Result='%s', Status='%s'",
                    targetNode, resp.getPayload(), resp.getExtra()));
        } else {
            System.out.println("No response from Node " + targetNode + " (timed out).");
        }
    }

    private void killNode(int targetNode) {
        System.out.println("Stopping Node " + targetNode + "...");
        ElectionMessage msg = new ElectionMessage(
                ElectionMessage.Type.SHUTDOWN, 99, targetNode, 0, "", ""
        );
        send(clusterPorts.get(targetNode), msg);
    }

    public static void main(String[] args) {
        SplitBrainInteractiveConsole console = new SplitBrainInteractiveConsole();
        console.run();
    }
}
