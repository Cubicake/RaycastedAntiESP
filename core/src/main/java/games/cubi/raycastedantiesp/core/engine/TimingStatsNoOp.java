package games.cubi.raycastedantiesp.core.engine;

public class TimingStatsNoOp extends TimingStats {
    static final TimingStatsNoOp INSTANCE = new TimingStatsNoOp();

    private TimingStatsNoOp() {}

    @Override
    void reset() {}

    @Override
    String recordCompleted(TickTimingSnapshot snapshot, long nowNanos) {
        return null;
    }

    @Override
    String recordSkipped(int threads, long nowNanos) {
        return null;
    }
}
