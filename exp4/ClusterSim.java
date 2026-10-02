import java.io.File;
import java.util.*;

public class ClusterSim {
    public static void main(String[] args) {
        String algo = (args.length > 0) ? args[0].toLowerCase() : "bully";

        System.out.println("===========================================================================");
        System.out.println("DISTRIBUTED SYSTEMS CLUSTER: QUORUM + " + algo.toUpperCase() + " ELECTION");
        System.out.println("Partition Topology: Minority Group {1, 2} vs. Majority Group {3, 4, 5}");
        System.out.println("===========================================================================");

        Map<Integer, List<String>> partitionMatrix = Map.of(
            1, List.of("3", "4", "5"),
            2, List.of("3", "4", "5"),
            3, List.of("1", "2"),
            4, List.of("1", "2"),
            5, List.of("1", "2")
        );

        List<Process> processes = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        try {
            for (Map.Entry<Integer, List<String>> entry : partitionMatrix.entrySet()) {
                int nodeId = entry.getKey();
                List<String> blocked = entry.getValue();

                List<String> command = new ArrayList<>();
                command.add(javaBin);
                command.add("-cp");
                command.add(classpath);
                command.add("DistributedNode");
                command.add(String.valueOf(nodeId));
                command.add(algo);
                command.addAll(blocked);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.inheritIO();
                processes.add(pb.start());
                Thread.sleep(150);
            }

            System.out.println("\nAll 5 node processes successfully spawned. Running for 15 seconds...\n");
            Thread.sleep(15000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            for (Process p : processes) {
                p.destroyForcibly();
            }
            System.out.println("\nCluster simulation shutdown complete.");
        }
    }
}