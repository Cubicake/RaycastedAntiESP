package games.cubi.raycastedantiesp.core.engine;

public class TickTimingBatchNoOp extends TickTimingBatch {
    public static final TickTimingBatchNoOp INSTANCE = new TickTimingBatchNoOp();
    public long startBatch() {return 0;}
    public long elapsedSince(long startNanos) {return 0;}
    public long startEntitySection() {return 0;}
    public void finishEntitySection(long startNanos) {}
    public long startPlayerSection() {return 0;}
    public void finishPlayerSection(long startNanos) {}
    public long startTileSection() {return 0;}
    public void finishTileSection(long startNanos) {}
    public void incrementProcessedPlayers() {}
    public void incrementBypassSkippedPlayers() {}
    public void incrementNullLocationSkippedPlayers() {}
    public void addEntityChecked(int count) {}
    public void incrementEntityNullTargets() {}
    public void incrementEntityRaycasts() {}
    public void addPlayerChecked(int count) {}
    public void incrementPlayerNullTargets() {}
    public void incrementPlayerRaycasts() {}
    public void addTileChecked(int count) {}
    public void incrementTileWorldSkipped() {}
    public void incrementTileRadiusSkipped() {}
    public void incrementTileRaycasts() {}
}
