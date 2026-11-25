import java.util.List;

public interface Scheduler {
    String name();
    SimulationResult run(List<Process> processes, SimConfig config);
}
