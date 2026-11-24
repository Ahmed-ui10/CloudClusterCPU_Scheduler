import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class MetricsCalculator {

    public static int turnaround(Process p) {
        if (p.finish < 0) throw new IllegalStateException(p.name() + " has not finished");
        return p.finish - p.arrival;
    }

    public static int waiting(Process p) {
        return turnaround(p) - p.burst;
    }

    public static int response(Process p) {
        if (p.firstStart < 0) throw new IllegalStateException(p.name() + " never started");
        return p.firstStart - p.arrival;
    }

    public static void compute(SimulationResult r) {
        r.avgWaiting = r.processes.stream().mapToInt(MetricsCalculator::waiting).average().orElse(0);
        r.avgTurnaround = r.processes.stream().mapToInt(MetricsCalculator::turnaround).average().orElse(0);
        r.avgResponse = r.processes.stream().mapToInt(MetricsCalculator::response).average().orElse(0);
        r.totalTime = r.processes.stream().mapToInt(p -> p.finish).max().orElse(0);
    }

    public static String formatQueue(Collection<Process> queue) {
        if (queue == null || queue.isEmpty()) return "[Empty]";
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (Process p : queue) {
            sj.add(p.name() + " (remaining: " + p.remaining + ", Priority: " + p.effectivePriority + ")");
        }
        return sj.toString();
    }

    public static String formatMetrics(SimulationResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(r.schedulerName);
        if (r.testCaseName != null) sb.append(" | ").append(r.testCaseName);
        sb.append(" | ").append(r.config).append(" ===\n");
        sb.append(String.format("%-8s %-8s %-7s %-8s %-11s %-8s %-8s%n",
                "Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"));
        for (Process p : r.processes) {
            sb.append(String.format("%-8s %-8d %-7d %-8d %-11d %-8d %-8d%n", p.name(), p.arrival,
                    p.burst, p.finish, turnaround(p), waiting(p), response(p)));
        }
        sb.append(String.format(Locale.US, "Average Turnaround Time: %.2f%n", r.avgTurnaround));
        sb.append(String.format(Locale.US, "Average Waiting Time:    %.2f%n", r.avgWaiting));
        sb.append(String.format(Locale.US, "Average Response Time:   %.2f%n", r.avgResponse));
        return sb.toString();
    }

    public static String formatTrace(SimulationResult r) {
        StringBuilder sb = new StringBuilder("Time | Running | Tier 1 | Tier 2 | Notes\n");
        for (TraceRow t : r.trace) {
            sb.append(String.join(" | ", String.valueOf(t.time), t.running, t.tier1, t.tier2, t.notes))
              .append('\n');
        }
        return sb.toString();
    }
}

class TraceRow {
    final int time;
    final String running, tier1, tier2, notes;

    TraceRow(int time, String running, String tier1, String tier2, String notes) {
        this.time = time;
        this.running = running == null ? "" : running;
        this.tier1 = tier1 == null ? "" : tier1;
        this.tier2 = tier2 == null ? "" : tier2;
        this.notes = notes == null ? "" : notes;
    }
}

class SimulationResult {
    final String schedulerName;
    final SimConfig config;
    String testCaseName;                                 
    final List<Process> processes = new ArrayList<>();   
    final List<TraceRow> trace = new ArrayList<>();
    double avgWaiting, avgTurnaround, avgResponse;
    int totalTime;

    SimulationResult(String schedulerName, SimConfig config) {
        this.schedulerName = schedulerName;
        this.config = config;
    }

    void addTrace(int time, String running, String tier1, String tier2, String notes) {
        trace.add(new TraceRow(time, running, tier1, tier2, notes));
    }
}
