import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ACSScheduler implements Scheduler {
    @Override
    public String name() {
        return "ACS";
    }

    @Override
    public SimulationResult run(List<Process> input, SimConfig config) {

        if (input == null) {
            throw new IllegalArgumentException("Process list cannot be null");
        }

        if (config == null) {
            throw new IllegalArgumentException("Simulation config cannot be null");
        }

        SimulationResult result = new SimulationResult(name(), config);
        result.processes.addAll(TestCaseLoader.copyAll(input));

        List<Process> arrivals = new ArrayList<>(result.processes);
        arrivals.sort(Process.BY_ARRIVAL_THEN_ID);
        Deque<Process> tier1 = new ArrayDeque<>();

        // Tier 2: Priority Scheduling
        List<Process> tier2 = new ArrayList<>();

        // Track Tier 2 entry time for tie breaking
        Map<Integer, Integer> tier2EntryTime = new HashMap<>();

        // Track last aging update time
        Map<Integer, Integer> lastAgingUpdate = new HashMap<>();

        int nextArrival = 0;
        int time = 0;
        int completed = 0;
        int lastRunningProcessId = -1;

        while (completed < result.processes.size()) {

            // Admit newly arrived processes
            nextArrival = admitArrivals(
                    arrivals, nextArrival, time, tier1
            );

            // Update waiting time and effective priority
            updateAging(tier2, config, time, lastAgingUpdate);

            // CPU idle
            if (tier1.isEmpty() && tier2.isEmpty()) {

                result.addTrace(
                        time,
                        "IDLE",
                        queueText(tier1),
                        queueText(tier2),
                        "CPU is idle; waiting for next arrival"
                );
                time++;
                lastRunningProcessId = -1;
                continue;
            }

            // Select next process
            Process selected = selectNext(
                    tier1, tier2, tier2EntryTime
            );

            // Context switching
            if (lastRunningProcessId != -1
                    && lastRunningProcessId != selected.id
                    && config.contextSwitch > 0) {

                for (int i = 0; i < config.contextSwitch; i++) {

                    result.addTrace(
                            time,
                            "CS",
                            queueText(tier1),
                            queueText(tier2),
                            "Context switch"
                    );

                    time++;

                    nextArrival = admitArrivals(
                            arrivals, nextArrival, time, tier1
                    );

                    updateAging(
                            tier2, config, time, lastAgingUpdate
                    );
                }
                nextArrival = admitArrivals(
                        arrivals, nextArrival, time, tier1
                );
                updateAging(
                        tier2, config, time, lastAgingUpdate
                );

                // Select again because queues may have changed
                selected = selectNext(
                        tier1, tier2, tier2EntryTime
                );
            }

            // Determine selected tier
            boolean fromTier1 = !tier1.isEmpty();

            Process current;

            if (fromTier1) {

                current = tier1.removeFirst();

            } else {

                current = selected;

                tier2.remove(current);

                tier2EntryTime.remove(current.id);
                lastAgingUpdate.remove(current.id);
            }

            current.state = Process.ProcessState.RUNNING;

            if (current.firstStart == -1) {
                current.firstStart = time;
            }
            // TIER 1: ROUND ROBIN
            if (fromTier1) {

                int slice = Math.min(
                        config.quantum,
                        current.remaining
                );

                for (int used = 0; used < slice; used++) {

                    nextArrival = admitArrivals(
                            arrivals, nextArrival, time, tier1
                    );

                    // Tier 2 processes continue aging
                    updateAging(
                            tier2, config, time, lastAgingUpdate
                    );

                    result.addTrace(
                            time,
                            current.name(),
                            queueText(tier1),
                            queueText(tier2),
                            "Tier 1 - Round Robin"
                    );

                    current.remaining--;
                    current.quantumUsed++;

                    time++;
                }

                if (current.remaining == 0) {

                    // Process completed
                    current.finish = time;
                    current.state = Process.ProcessState.TERMINATED;

                    completed++;

                } else {

                    // Quantum expired: demote to Tier 2
                    current.tier = Process.Tier.TIER2;
                    current.state = Process.ProcessState.READY;

                    current.quantumUsed = 0;
                    current.waitingInTier2 = 0;
                    current.effectivePriority = current.priority;

                    tier2.add(current);

                    tier2EntryTime.put(current.id, time);
                    lastAgingUpdate.put(current.id, time);
                }
            }
            else {

                while (current.remaining > 0) {

                    nextArrival = admitArrivals(
                            arrivals, nextArrival, time, tier1
                    );

                    // Other Tier 2 processes keep aging
                    updateAging(
                            tier2, config, time, lastAgingUpdate
                    );

                    result.addTrace(
                            time,
                            current.name(),
                            queueText(tier1),
                            queueText(tier2),
                            "Tier 2 - Non-Preemptive Priority"
                                    + " | Effective Priority: "
                                    + current.effectivePriority
                    );

                    current.remaining--;
                    time++;
                }

                // Process completed
                current.finish = time;
                current.state = Process.ProcessState.TERMINATED;

                completed++;
            }

            lastRunningProcessId = current.id;
        }

        // Calculate final metrics
        MetricsCalculator.compute(result);

        return result;
    }
    private static int admitArrivals(
            List<Process> arrivals,
            int nextArrival,
            int time,
            Deque<Process> tier1) {

        while (nextArrival < arrivals.size()
                && arrivals.get(nextArrival).arrival <= time) {

            Process arriving = arrivals.get(nextArrival++);

            arriving.state = Process.ProcessState.READY;
            arriving.tier = Process.Tier.TIER1;

            tier1.addLast(arriving);
        }

        return nextArrival;
    }
    private static void updateAging(
            List<Process> tier2,
            SimConfig config,
            int time,
            Map<Integer, Integer> lastAgingUpdate) {

        for (Process p : tier2) {

            int lastUpdate = lastAgingUpdate.getOrDefault(
                    p.id, time
            );

            int elapsed = time - lastUpdate;

            if (elapsed > 0) {

                p.waitingInTier2 += elapsed;

                lastAgingUpdate.put(p.id, time);
            }

            // Smaller priority number means higher priority
            p.effectivePriority = config.effectivePriority(
                    p.priority,
                    p.waitingInTier2
            );
        }
    }
    //  PROCESS
    private static Process selectNext(
            Deque<Process> tier1,
            List<Process> tier2,
            Map<Integer, Integer> tier2EntryTime) {

        // Tier 1 has scheduling preference
        if (!tier1.isEmpty()) {
            return tier1.peekFirst();
        }

        if (tier2.isEmpty()) {
            return null;
        }

        // Priority and tie-breaking rules
        Comparator<Process> priorityOrder =
                Comparator.comparingInt(
                        (Process p) -> p.effectivePriority
                )
                .thenComparingInt(
                        p -> tier2EntryTime.getOrDefault(
                                p.id, Integer.MAX_VALUE
                        )
                )
                .thenComparingInt(p -> p.arrival)
                .thenComparingInt(p -> p.id);

        return tier2.stream()
                .min(priorityOrder)
                .orElse(null);
    }
    // QUEUE DISPLAY
    private static String queueText(Deque<Process> queue) {
        return MetricsCalculator.formatQueue(queue);
    }
    private static String queueText(List<Process> queue) {
        return MetricsCalculator.formatQueue(queue);
    }
}
