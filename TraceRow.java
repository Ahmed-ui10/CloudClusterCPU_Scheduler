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
