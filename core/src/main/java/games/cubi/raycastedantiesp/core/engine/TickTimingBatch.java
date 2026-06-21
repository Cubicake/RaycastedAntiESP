package games.cubi.raycastedantiesp.core.engine;

public class TickTimingBatch {
    long entityNanos;
    long playerNanos;
    long tileNanos;

    int processedPlayers;
    int bypassSkippedPlayers;
    int nullLocationSkippedPlayers;

    int entityChecked;
    int entityRaycasts;
    int entityNullTargets;

    int playerChecked;
    int playerRaycasts;
    int playerNullTargets;

    int tileChecked;
    int tileRaycasts;
    int tileWorldSkipped;
    int tileRadiusSkipped;

    TickTimingBatch() {}

    public long startBatch() {
        return System.nanoTime();
    }

    public long elapsedSince(long startNanos) {
        return System.nanoTime() - startNanos;
    }

    public long startEntitySection() {
        return System.nanoTime();
    }

    public void finishEntitySection(long startNanos) {
        entityNanos += Math.max(0, System.nanoTime() - startNanos);
    }

    public long startPlayerSection() {
        return System.nanoTime();
    }

    public void finishPlayerSection(long startNanos) {
        playerNanos += Math.max(0, System.nanoTime() - startNanos);
    }

    public long startTileSection() {
        return System.nanoTime();
    }

    public void finishTileSection(long startNanos) {
        tileNanos += Math.max(0, System.nanoTime() - startNanos);
    }

    public void incrementProcessedPlayers() {
        processedPlayers++;
    }

    public void incrementBypassSkippedPlayers() {
        bypassSkippedPlayers++;
    }

    public void incrementNullLocationSkippedPlayers() {
        nullLocationSkippedPlayers++;
    }

    public void addEntityChecked(int count) {
        entityChecked += count;
    }

    public void incrementEntityNullTargets() {
        entityNullTargets++;
    }

    public void incrementEntityRaycasts() {
        entityRaycasts++;
    }

    public void addPlayerChecked(int count) {
        playerChecked += count;
    }

    public void incrementPlayerNullTargets() {
        playerNullTargets++;
    }

    public void incrementPlayerRaycasts() {
        playerRaycasts++;
    }

    public void addTileChecked(int count) {
        tileChecked += count;
    }

    public void incrementTileWorldSkipped() {
        tileWorldSkipped++;
    }

    public void incrementTileRadiusSkipped() {
        tileRadiusSkipped++;
    }

    public void incrementTileRaycasts() {
        tileRaycasts++;
    }
}
