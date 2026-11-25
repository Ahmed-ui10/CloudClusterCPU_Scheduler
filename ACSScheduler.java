import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * ACS implementation for Student 3.
 * Tier 1 uses Round Robin. A process that consumes its full quantum and still
 * has work remaining is demoted to Tier 2.
 *
 * IMPORTANT: Tier 2 is intentionally a temporary FIFO placeholder. Student 4
 * is expected to replace this policy with the Priority Queue + Aging policy.
 */
public class ACSScheduler implements Scheduler {

    @Override
    public String name() {
        return "ACS";
    }

    @Override
    public SimulationResult run(List<Process> input, SimConfig config) {
        if (input == null) throw new IllegalArgumentException("Process list cannot be null");
        if (config == null) throw new IllegalArgumentException("Simulation config cannot be null");

        SimulationResult result = new SimulationResult(name(), config);
        result.processes.addAll(TestCaseLoader.copyAll(input));

        // Keep the result list in the original input order, but admit processes
        // according to arrival time (and then process ID).
        List<Process> arrivals = new ArrayList<>(result.processes);
        arrivals.sort(Process.BY_ARRIVAL_THEN_ID);

        Deque<Process> tier1 = new ArrayDeque<>();
        Deque<Process> tier2 = new ArrayDeque<>();
        int nextArrival = 0;
        int time = 0;
        int completed = 0;
        int lastRunningProcessId = -1;

        while (completed < result.processes.size()) {
            nextArrival = admitArrivals(arrivals, nextArrival, time, tier1);

            // If both queues are empty, advance time until the next arrival.
            if (tier1.isEmpty() && tier2.isEmpty()) {
                if (nextArrival < arrivals.size()) {
                    result.addTrace(time, "IDLE", queueText(tier1), queueText(tier2),
                            "CPU is idle; waiting for the next arrival");
                    time++;
                    lastRunningProcessId = -1; // no context switch from idle to a process
                    continue;
                }
                break;
            }

            boolean selectedFromTier1 = !tier1.isEmpty();
            Process current = selectedFromTier1 ? tier1.removeFirst() : tier2.removeFirst();

            // Charge context-switch time only when moving directly from one
            // process to a different process. Initial dispatch and idle->CPU
            // transitions do not incur a context switch.
            if (lastRunningProcessId != -1
                    && lastRunningProcessId != current.id
                    && config.contextSwitch > 0) {
                for (int cs = 0; cs < config.contextSwitch; cs++) {
                    nextArrival = admitArrivals(arrivals, nextArrival, time, tier1);
                    result.addTrace(time, "CS", queueText(tier1), queueText(tier2),
                            "Context switch before " + current.name());
                    time++;
                }
                // Processes that arrived during CS are now in the appropriate
                // ready queue. The selected process keeps its dispatch slot.
                nextArrival = admitArrivals(arrivals, nextArrival, time, tier1);
            }

            current.state = Process.ProcessState.RUNNING;
            if (current.firstStart < 0) current.firstStart = time;

            if (selectedFromTier1) {
                int slice = Math.min(config.quantum, current.remaining);
                for (int used = 0; used < slice; used++) {
                    nextArrival = admitArrivals(arrivals, nextArrival, time, tier1);
                    String note = "Tier 1 / Round Robin";
                    if (used == slice - 1) {
                        if (current.remaining == 1) {
                            note += " | process completes after this unit";
                        } else if (slice == config.quantum) {
                            note += " | quantum expires; demote after this unit";
                        }
                    }
                    result.addTrace(time, current.name(), queueText(tier1), queueText(tier2), note);
                    current.remaining--;
                    current.quantumUsed++;
                    time++;
                }

                if (current.remaining == 0) {
                    current.finish = time;
                    current.state = Process.ProcessState.TERMINATED;
                    completed++;
                } else {
                    // A process is demoted only after it has consumed the full
                    // quantum and still has remaining CPU burst.
                    current.tier = Process.Tier.TIER2;
                    current.state = Process.ProcessState.READY;
                    current.quantumUsed = 0;
                    tier2.addLast(current);
                }
            } else {
                // Temporary Tier 2 placeholder: FIFO, non-preemptive execution.
                // Student 4 will replace this with Priority Queue + Aging.
                while (current.remaining > 0) {
                    nextArrival = admitArrivals(arrivals, nextArrival, time, tier1);
                    String note = "Tier 2 / temporary FIFO placeholder (non-preemptive)";
                    if (current.remaining == 1) note += " | process completes after this unit";
                    result.addTrace(time, current.name(), queueText(tier1), queueText(tier2), note);
                    current.remaining--;
                    time++;
                }
                current.finish = time;
                current.state = Process.ProcessState.TERMINATED;
                completed++;
            }

            lastRunningProcessId = current.id;
        }

        MetricsCalculator.compute(result);
        return result;
    }

    private static int admitArrivals(List<Process> arrivals, int nextArrival,
                                     int time, Deque<Process> tier1) {
        while (nextArrival < arrivals.size()
                && arrivals.get(nextArrival).arrival <= time) {
            Process arriving = arrivals.get(nextArrival++);
            arriving.state = Process.ProcessState.READY;
            arriving.tier = Process.Tier.TIER1;
            tier1.addLast(arriving);
        }
        return nextArrival;
    }

    private static String queueText(Deque<Process> queue) {
        return MetricsCalculator.formatQueue(queue);
    }
}
