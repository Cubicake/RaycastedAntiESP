package games.cubi.raycastedantiesp.core.engine;

final class TickTimings {
    private final int scheduledTick;
    private final long scheduledNanos;
    private final int startTick;
    private final long startNanos;
    private final int threads;
    private final int registeredPlayers;

    private long maxBatchNanos;
    private long entityNanos;
    private long playerNanos;
    private long tileNanos;

    private int processedPlayers;
    private int bypassSkippedPlayers;
    private int nullLocationSkippedPlayers;

    private int entityChecked;
    private int entityRaycasts;
    private int entityNullTargets;

    private int playerChecked;
    private int playerRaycasts;
    private int playerNullTargets;

    private int tileChecked;
    private int tileRaycasts;
    private int tileWorldSkipped;
    private int tileRadiusSkipped;

    TickTimings(int scheduledTick, long scheduledNanos, int startTick, long startNanos, int threads, int registeredPlayers) {
        this.scheduledTick = scheduledTick;
        this.scheduledNanos = scheduledNanos;
        this.startTick = startTick;
        this.startNanos = startNanos;
        this.threads = threads;
        this.registeredPlayers = registeredPlayers;
    }

    synchronized void recordBatch(TickTimingBatch batch, long batchNanos) {
        // Batch-local primitive counters avoid atomic writes in the raycast loop, where the work being measured is sub-microsecond.
        maxBatchNanos = Math.max(maxBatchNanos, Math.max(0, batchNanos));
        entityNanos += batch.entityNanos;
        playerNanos += batch.playerNanos;
        tileNanos += batch.tileNanos;

        processedPlayers += batch.processedPlayers;
        bypassSkippedPlayers += batch.bypassSkippedPlayers;
        nullLocationSkippedPlayers += batch.nullLocationSkippedPlayers;

        entityChecked += batch.entityChecked;
        entityRaycasts += batch.entityRaycasts;
        entityNullTargets += batch.entityNullTargets;

        playerChecked += batch.playerChecked;
        playerRaycasts += batch.playerRaycasts;
        playerNullTargets += batch.playerNullTargets;

        tileChecked += batch.tileChecked;
        tileRaycasts += batch.tileRaycasts;
        tileWorldSkipped += batch.tileWorldSkipped;
        tileRadiusSkipped += batch.tileRadiusSkipped;
    }

    synchronized TickTimingSnapshot snapshot(int completionTick, long completionNanos) {
        return new TickTimingSnapshot(
                scheduledTick,
                startTick,
                completionTick,
                threads,
                registeredPlayers,
                Math.max(0, startNanos - scheduledNanos),
                Math.max(0, completionNanos - scheduledNanos),
                maxBatchNanos,
                entityNanos,
                playerNanos,
                tileNanos,
                processedPlayers,
                bypassSkippedPlayers,
                nullLocationSkippedPlayers,
                entityChecked,
                entityRaycasts,
                entityNullTargets,
                playerChecked,
                playerRaycasts,
                playerNullTargets,
                tileChecked,
                tileRaycasts,
                tileWorldSkipped,
                tileRadiusSkipped
        );
    }
}
