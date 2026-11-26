import java.util.ArrayList;
import java.util.List;

public class SJFScheduler implements Scheduler {

    public String name() {
        return "Preemptive SJF";
    }

    public SimulationResult run(List<Process> input, SimConfig config) {
        SimulationResult result = new SimulationResult(name(), config);
        result.processes.addAll(TestCaseLoader.copyAll(input));
        List<Process> arrivals = new ArrayList<>(result.processes);
        arrivals.sort(Process.BY_ARRIVAL_THEN_ID);
        List<Process> ready = new ArrayList<>();
        Process current = null;
        int time = 0;
        int nextArrival = 0;
        int completed = 0;
        int csRemaining = 0;
        int lastRunningProcessId = -1;
        boolean csFinished = false;

        while (completed < result.processes.size()) {

            while (nextArrival < arrivals.size() && arrivals.get(nextArrival).arrival <= time) {
                ready.add(arrivals.get(nextArrival));
                nextArrival++;
            }

            if (csRemaining > 0) {
                result.addTrace(time, "CS", queueText(ready), "", "Context Switch");
                time++;
                csRemaining--;
                if (csRemaining == 0) {
                    csFinished = true;
                }
                continue;
            }

            Process selected = selectShortest(current, ready);

            if (selected == null) {
                result.addTrace(time, "IDLE", "[Empty]", "", "CPU idle");
                time++;
                lastRunningProcessId = -1;
                csFinished = false;
                continue;
            }

            if (selected != current) {
                if (current != null) {
                    ready.add(current);
                }
                current = selected;
                ready.remove(current);

                if (lastRunningProcessId != -1 && lastRunningProcessId != current.id && config.contextSwitch > 0 && !csFinished) {
                    csRemaining = config.contextSwitch;
                    csFinished = false;
                    continue;
                }
                csFinished = false;
            }

            if (current.firstStart == -1) {current.firstStart = time;}

            result.addTrace(time, current.name(), queueText(ready), "", "");
            current.remaining--;
            time++;

            lastRunningProcessId = current.id;

            if (current.remaining == 0) {
                current.finish = time;
                current.state = Process.ProcessState.TERMINATED;
                completed++;
                current = null;
            }
        }
        MetricsCalculator.compute(result);
        return result;
    }

    private Process selectShortest(Process current, List<Process> ready) {
        Process shortest = current;
        for (Process p : ready) {
            if (shortest == null) {
                shortest = p;
            }
            else if (p.remaining < shortest.remaining) {
                shortest = p;
            }
            else if (p.remaining == shortest.remaining) {

                if (p.arrival < shortest.arrival || (p.arrival == shortest.arrival && p.id < shortest.id)) {
                    shortest = p;
                }
            }
        }
        return shortest;
    }
    private String queueText(List<Process> queue) {
        return MetricsCalculator.formatQueue(queue);
    }
}
