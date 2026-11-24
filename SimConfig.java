public class SimConfig {

    public static final int DEFAULT_AGING_INTERVAL = 4;

    public final int quantum;          
    public final int contextSwitch;     
    public final int agingInterval;     
    public String outputDir = "output";  

    public SimConfig(int quantum, int contextSwitch) {
        this(quantum, contextSwitch, DEFAULT_AGING_INTERVAL);
    }

    public SimConfig(int quantum, int contextSwitch, int agingInterval) {
        if (quantum <= 0 || agingInterval <= 0 || contextSwitch < 0) {
            throw new IllegalArgumentException("need quantum > 0, aging interval > 0, context switch >= 0");
        }
        this.quantum = quantum;
        this.contextSwitch = contextSwitch;
        this.agingInterval = agingInterval;
    }

    public int effectivePriority(int initialPriority, int waitingTime) {
        return initialPriority - waitingTime / agingInterval;
    }

    public boolean isHigherPriority(int a, int b) {
        return a < b;
    }

    @Override
    public String toString() {
        return "Q=" + quantum + ", CS=" + contextSwitch + ", AgingInterval=" + agingInterval;
    }
}
