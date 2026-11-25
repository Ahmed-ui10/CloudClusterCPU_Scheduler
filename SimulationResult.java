import java.util.ArrayList;
import java.util.List;

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
