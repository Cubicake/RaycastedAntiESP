package games.cubi.raycastedantiesp.core.engine;

record TickTimingSnapshot(
        int scheduledTick,
        int startTick,
        int completionTick,
        int threads,
        int registeredPlayers,
        long queueNanos,
        long wallNanos,
        long maxBatchNanos,
        long entityNanos,
        long playerNanos,
        long tileNanos,
        int processedPlayers,
        int bypassSkippedPlayers,
        int nullLocationSkippedPlayers,
        int entityChecked,
        int entityRaycasts,
        int entityNullTargets,
        int playerChecked,
        int playerRaycasts,
        int playerNullTargets,
        int tileChecked,
        int tileRaycasts,
        int tileWorldSkipped,
        int tileRadiusSkipped
) {
    String toSlowTickMessage() {
        return "Tick completed slowly."
                + " scheduledTick=" + scheduledTick
                + " startTick=" + startTick
                + " completionTick=" + completionTick
                + " threads=" + threads
                + " wallTimeTaken=" + TickTimingFormatter.formatMillis(wallNanos) + "ms"
                + " schedulerWait=" + TickTimingFormatter.formatMillis(queueNanos) + "ms"
                + " slowestWorkerBatch=" + TickTimingFormatter.formatMillis(maxBatchNanos) + "ms"
                + " entityProcessingTime=" + TickTimingFormatter.formatMillis(entityNanos) + "ms"
                + " playerProcessingTime=" + TickTimingFormatter.formatMillis(playerNanos) + "ms"
                + " tileProcessingTime=" + TickTimingFormatter.formatMillis(tileNanos) + "ms"
                + " playerCount=" + registeredPlayers
                + " processedPlayers=" + processedPlayers
                + " bypassSkippedPlayers=" + bypassSkippedPlayers
                + " nullLocationSkippedPlayers=" + nullLocationSkippedPlayers
                + " entityRecheckCandidates=" + entityChecked
                + " entityRaycasts=" + entityRaycasts
                + " entityNullTargets=" + entityNullTargets
                + " playerRecheckCandidates=" + playerChecked
                + " playerRaycasts=" + playerRaycasts
                + " playerNullTargets=" + playerNullTargets
                + " tileRecheckCandidates=" + tileChecked
                + " tileRaycasts=" + tileRaycasts
                + " tileWorldSkipped=" + tileWorldSkipped
                + " tileRadiusSkipped=" + tileRadiusSkipped;
    }

    String toSlowestSummary() {
        return "scheduledTick=" + scheduledTick
                + ", startedAtTick=" + startTick
                + ", totalWallTime=" + TickTimingFormatter.formatMillis(wallNanos) + " ms"
                + ", schedulerWait=" + TickTimingFormatter.formatMillis(queueNanos) + " ms"
                + ", slowestWorkerBatch=" + TickTimingFormatter.formatMillis(maxBatchNanos) + " ms"
                + ", processingTime(entity/player/tile)=" + TickTimingFormatter.formatMillis(entityNanos) + "/" + TickTimingFormatter.formatMillis(playerNanos) + "/" + TickTimingFormatter.formatMillis(tileNanos) + " ms"
                + ", raycasts(entity/player/tile)=" + entityRaycasts + "/" + playerRaycasts + "/" + tileRaycasts;
    }
}
