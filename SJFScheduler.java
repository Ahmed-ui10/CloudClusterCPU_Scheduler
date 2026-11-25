import java.util.ArrayList;
import java.util.List;

public class SJFScheduler implements Scheduler {

    @Override
    public String name() {
        return "Preemptive SJF";
    }

    @Override
    public SimulationResult run(List<Process> input, SimConfig config) {
        SimulationResult result = new SimulationResult(name(), config);
        result.processes.addAll(TestCaseLoader.copyAll(input));

        List<Process> arrivals = new ArrayList<>(result.processes);
        arrivals.sort(Process.BY_ARRIVAL_THEN_ID);
        
        List<Process> ready = new ArrayList<>();
        Process current = null;
        int nextArrival = 0;
        int time = 0;
        int completed = 0;
        int csRemaining = 0;
        int lastRunningProcessId = -1;

        while (completed < result.processes.size()) {
            
            while (nextArrival < arrivals.size() && arrivals.get(nextArrival).arrival <= time) {
                ready.add(arrivals.get(nextArrival++));
            }

            if (csRemaining > 0) {
                result.addTrace(time, "CS", queueText(ready), "", "Context Switch");
                time++;
                csRemaining--;
                continue;
            }

            Process selected = selectShortest(current, ready);

            if (selected == null) {
                result.addTrace(time, "IDLE", "[Empty]", "", "CPU idle");
                time++;
                continue;
            }

            if (selected != current) {
                if (current != null) {
                    ready.add(current); 
                }
                current = selected;
                ready.remove(current);
                
                if (lastRunningProcessId != -1 && lastRunningProcessId != current.id && config.contextSwitch > 0) {
                    csRemaining = config.contextSwitch;
                    continue;
                }
                
                if (current.firstStart == -1) {
                    current.firstStart = time;
                }
            }

            result.addTrace(time, current.name(), queueText(ready), "", "");
            current.remaining--;
            time++;

            if (current.remaining == 0) {
                current.finish = time;
                current.state = Process.ProcessState.TERMINATED;
                completed++;
                current = null;
            }
            lastRunningProcessId = (current != null) ? current.id : -1;
        }

        MetricsCalculator.compute(result);
        return result;
    }

    private Process selectShortest(Process current, List<Process> ready) {
        Process best = current;
        for (Process p : ready) {
            if (best == null) {
                best = p;
            } else {
                if (p.remaining < best.remaining) {
                    best = p;
                } else if (p.remaining == best.remaining) {
                    if (p.arrival < best.arrival || (p.arrival == best.arrival && p.id < best.id)) {
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    private String queueText(List<Process> queue) {
        return MetricsCalculator.formatQueue(queue);
    }
}