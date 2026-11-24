import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TestCaseLoader {

    public static List<Process> testCase1() {
        List<Process> l = new ArrayList<>();
        l.add(new Process(1, 0, 4, 2));
        l.add(new Process(2, 1, 7, 3));
        l.add(new Process(3, 3, 2, 1));
        return l;
    }

    public static List<Process> testCase2() {
        List<Process> l = new ArrayList<>();
        l.add(new Process(1, 0, 5, 4));
        l.add(new Process(2, 1, 6, 2));
        l.add(new Process(3, 4, 4, 3));
        return l;
    }

    public static List<Process> byNumber(int n) {
        if (n == 1) return testCase1();
        if (n == 2) return testCase2();
        throw new IllegalArgumentException("Unknown test case " + n);
    }

    public static SimConfig assignmentConfig(int n) {
        return new SimConfig(3, n == 1 ? 0 : 1);
    }

    public static int[] expectedAcsFinish(int n) {
        return n == 1 ? new int[]{9, 13, 8} : new int[]{18, 15, 20};
    }

    public static List<Process> copyAll(List<Process> src) {
        List<Process> out = new ArrayList<>();
        for (Process p : src) out.add(p.copy());
        return out;
    }

    // CSV 

    private static PrintWriter open(String path, boolean append) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        return append
                ? new PrintWriter(Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND))
                : new PrintWriter(Files.newBufferedWriter(p, StandardCharsets.UTF_8));
    }

    static String csv(String s) {
        if (s == null) return "";
        boolean quote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        return quote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    public static void exportMetrics(SimulationResult r, String path) throws IOException {
        try (PrintWriter w = open(path, false)) {
            w.println("Process,Arrival,Burst,Priority,Finish,Turnaround,Waiting,Response");
            for (Process p : r.processes) {
                w.printf("%s,%d,%d,%d,%d,%d,%d,%d%n", p.name(), p.arrival, p.burst, p.priority,
                        p.finish, MetricsCalculator.turnaround(p), MetricsCalculator.waiting(p),
                        MetricsCalculator.response(p));
            }
            w.printf(Locale.US, "AVERAGE,,,,,%.2f,%.2f,%.2f%n",
                    r.avgTurnaround, r.avgWaiting, r.avgResponse);
        }
    }

    public static void exportTrace(SimulationResult r, String path) throws IOException {
        try (PrintWriter w = open(path, false)) {
            w.println("Time,Running,Tier1,Tier2,Notes");
            for (TraceRow t : r.trace) {
                w.println(String.join(",", String.valueOf(t.time), csv(t.running), csv(t.tier1),
                        csv(t.tier2), csv(t.notes)));
            }
        }
    }

    public static void appendSummary(String path, String testCase, SimulationResult r) throws IOException {
        boolean isNew = !Files.exists(Paths.get(path));
        try (PrintWriter w = open(path, true)) {
            if (isNew) {
                w.println("TestCase,Scheduler,Quantum,ContextSwitch,AgingInterval,"
                        + "AvgWaiting,AvgTurnaround,AvgResponse,TotalTime");
            }
            w.printf(Locale.US, "%s,%s,%d,%d,%d,%.2f,%.2f,%.2f,%d%n", csv(testCase),
                    csv(r.schedulerName), r.config.quantum, r.config.contextSwitch,
                    r.config.agingInterval, r.avgWaiting, r.avgTurnaround, r.avgResponse, r.totalTime);
        }
    }
}
