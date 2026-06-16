package abms.common;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.node.Node;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.MasterLog;

/**
 * Runs a scenario boot string {@code runs} times in sequence, blocking on each run
 * via {@link Simulation#awaitCompletion()} and handing the completed simulation to
 * the provided {@link RunObserver} before starting the next run.
 */
public final class BatchRunner {

    public interface BootStringFactory {
        String build(int runIndex);
    }

    public interface RunObserver {
        void onRunCompleted(int runIndex, Simulation sim);

        void onAllRunsCompleted(int runs);
    }

    private BatchRunner() {
    }

    public static void run(int runs, Level logLevel, BootStringFactory bootFactory, RunObserver observer) {
        MasterLog.setLogLevel(logLevel);
        PrintStream originalOut = System.out;
        PrintStream nullOut = new PrintStream(new OutputStream() { @Override public void write(int b) { } });
        for (int i = 0; i < runs; i++) {
            String bootString = bootFactory.build(i);

            String[] args = bootString.split(" ");
            // Mute Simulation.stepCompleted()'s per-tick visualization while the run is in
            // progress so that 1000-run batches don't bury the aggregate stats in noise.
            System.setOut(nullOut);
            try {
                List<Node> nodes = Deployment.get().loadDeployment(Arrays.asList(args));
                if (nodes == null)
                    throw new IllegalStateException("Deployment failed at run " + i);
                for (Node node : nodes)
                    node.start();
                Simulation sim = Simulation.getLastInstance();
                if (sim == null)
                    throw new IllegalStateException("No Simulation registered at run " + i);
                sim.awaitCompletion();
                System.setOut(originalOut);
                observer.onRunCompleted(i, sim);
            } finally {
                System.setOut(originalOut);
            }
        }
        observer.onAllRunsCompleted(runs);
    }
}
