import java.io.File;
import java.util.*;

public class ClockSimDriver {
    public static void main(String[] args) {
        System.out.println("===========================================================================");
        System.out.println("DISTRIBUTED CLOCK SYNCHRONIZATION SIMULATION: LAMPORT + BERKELEY");
        System.out.println("Cluster Nodes: Node 1 (Master, 0ms drift), Node 2 (+1200ms), Node 3 (-800ms)");
        System.out.println("===========================================================================");

        List<Process> processes = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        // Configuration: {NodeID, isMaster, initialClockDriftMs}
        String[][] nodeConfigs = {
            {"1", "true", "0"},
            {"2", "false", "1200"},
            {"3", "false", "-800"}
        };

        try {
            for (String[] conf : nodeConfigs) {
                List<String> cmd = List.of(
                    javaBin, "-cp", classpath, "ClockNode", conf[0], conf[1], conf[2]
                );
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.inheritIO();
                processes.add(pb.start());
                Thread.sleep(200);
            }

            System.out.println("\nAll clock nodes successfully spawned. Simulating execution for 14 seconds...\n");
            Thread.sleep(14000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            for (Process p : processes) {
                p.destroyForcibly();
            }
            System.out.println("\nClock synchronization simulation shutdown complete.");
        }
    }
}