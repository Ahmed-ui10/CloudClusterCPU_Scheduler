import java.util.Comparator;

public class Process {

    public enum ProcessState { NEW, READY, RUNNING, TERMINATED }

    public enum Tier { TIER1, TIER2 }

    public static final Comparator<Process> BY_ARRIVAL_THEN_ID =
            Comparator.<Process>comparingInt(p -> p.arrival).thenComparingInt(p -> p.id);

    public final int id;
    public final int arrival;
    public final int burst;
    public final int priority;          

    public int remaining;
    public int effectivePriority;      
    public Tier tier = Tier.TIER1;
    public ProcessState state = ProcessState.NEW;
    public int firstStart = -1;         
    public int finish = -1;           
    public int quantumUsed = 0; 
    public int waitingInTier2 = 0;      

    public Process(int id, int arrival, int burst, int priority) {
        if (arrival < 0 || burst <= 0) {
            throw new IllegalArgumentException("P" + id + ": need arrival >= 0 and burst > 0");
        }
        this.id = id;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.remaining = burst;
        this.effectivePriority = priority;
    }

    public Process copy() {
        return new Process(id, arrival, burst, priority);
    }

    public String name() {
        return "P" + id;
    }

    public boolean isFinished() {
        return remaining == 0;
    }

    @Override
    public String toString() {
        return name();
    }
}
