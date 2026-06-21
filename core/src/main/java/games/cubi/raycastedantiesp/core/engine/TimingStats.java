package games.cubi.raycastedantiesp.core.engine;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;

public class TimingStats {
    private static final long REPORT_INTERVAL_NANOS = 30_000_000_000L;

    private final LongArrayList wallSamples = new LongArrayList();
    private final LongArrayList schedulerWaitSamples = new LongArrayList();
    private final LongArrayList slowestWorkerBatchSamples = new LongArrayList();
    private final LongArrayList entityWorkerSamples = new LongArrayList();
    private final LongArrayList playerWorkerSamples = new LongArrayList();
    private final LongArrayList tileWorkerSamples = new LongArrayList();

    private long intervalStartNanos = 0;
    private long nextReportNanos = 0;
    private int completedTicks = 0;
    private int skippedTicks = 0;
    private int lastThreads = 0;

    private long entityRaycastsTotal = 0;
    private long playerRaycastsTotal = 0;
    private long tileRaycastsTotal = 0;

    private TickTimingSnapshot slowestTick = null;

    /**
     * Drops the current reporting window so later samples do not mix with data collected under a previous timing configuration.
     */
    synchronized void reset() {
        wallSamples.clear();
        schedulerWaitSamples.clear();
        slowestWorkerBatchSamples.clear();
        entityWorkerSamples.clear();
        playerWorkerSamples.clear();
        tileWorkerSamples.clear();
        intervalStartNanos = 0;
        nextReportNanos = 0;
        completedTicks = 0;
        skippedTicks = 0;
        lastThreads = 0;
        entityRaycastsTotal = 0;
        playerRaycastsTotal = 0;
        tileRaycastsTotal = 0;
        slowestTick = null;
    }

    /**
     * Adds a completed tick to the active reporting window.
     *
     * @return a formatted aggregate report when the reporting interval has elapsed; otherwise null.
     */
    synchronized String recordCompleted(TickTimingSnapshot snapshot, long nowNanos) {
        ensureInterval(nowNanos);
        completedTicks++;
        lastThreads = snapshot.threads();
        wallSamples.add(snapshot.wallNanos());
        schedulerWaitSamples.add(snapshot.queueNanos());
        slowestWorkerBatchSamples.add(snapshot.maxBatchNanos());
        entityWorkerSamples.add(snapshot.entityNanos());
        playerWorkerSamples.add(snapshot.playerNanos());
        tileWorkerSamples.add(snapshot.tileNanos());

        entityRaycastsTotal += snapshot.entityRaycasts();
        playerRaycastsTotal += snapshot.playerRaycasts();
        tileRaycastsTotal += snapshot.tileRaycasts();

        if (slowestTick == null || snapshot.wallNanos() > slowestTick.wallNanos()) {
            slowestTick = snapshot;
        }
        return reportIfDue(nowNanos);
    }

    /**
     * Adds a skipped tick attempt to the active reporting window without creating worker timing samples.
     *
     * @return a formatted aggregate report when the reporting interval has elapsed; otherwise null.
     */
    synchronized String recordSkipped(int threads, long nowNanos) {
        ensureInterval(nowNanos);
        skippedTicks++;
        if (threads != -1) lastThreads = threads;
        return reportIfDue(nowNanos);
    }

    private void ensureInterval(long nowNanos) {
        if (nextReportNanos == 0) {
            intervalStartNanos = nowNanos;
            nextReportNanos = nowNanos + REPORT_INTERVAL_NANOS;
        }
    }

    private String reportIfDue(long nowNanos) {
        if (nextReportNanos == 0 || nowNanos < nextReportNanos) {
            return null;
        }

        String report = buildReport(nowNanos);
        reset();
        intervalStartNanos = nowNanos;
        nextReportNanos = nowNanos + REPORT_INTERVAL_NANOS;
        return report;
    }

    private String buildReport(long nowNanos) {
        long intervalNanos = Math.max(1, nowNanos - intervalStartNanos);
        String slowestSummary = slowestTick == null ? "n/a" : slowestTick.toSlowestSummary();
        return "Raycasted Anti-ESP tick timing summary for the last " + TickTimingFormatter.formatSeconds(intervalNanos) + "s:\n"
                + "Completed Anti-ESP ticks: " + completedTicks + "\n"
                + "Skipped tick attempts: " + skippedTicks + "\n"
                + "Configured worker threads: " + lastThreads + "\n\n"
                + "Total time from server tick event to Anti-ESP completion (avg / p50 / p95 / max): " + formatStats(wallSamples) + "\n"
                + "Time spent waiting in the async scheduler before Anti-ESP work started (avg / p50 / p95 / max): " + formatStats(schedulerWaitSamples) + "\n"
                + "Slowest worker batch processing time after work started (avg / p50 / p95 / max): " + formatStats(slowestWorkerBatchSamples) + "\n\n"
                + "Worker section times are summed across worker batches for multi-threaded ticks, not wall-clock critical-path time.\n"
                + "Worker time spent checking entities per completed tick (avg / p50 / p95 / max): " + formatStats(entityWorkerSamples) + "\n"
                + "Worker time spent checking players per completed tick (avg / p50 / p95 / max): " + formatStats(playerWorkerSamples) + "\n"
                + "Worker time spent checking tile entities per completed tick (avg / p50 / p95 / max): " + formatStats(tileWorkerSamples) + "\n\n"
                + "Total raycasts performed, entity / player / tile: " + entityRaycastsTotal + " / " + playerRaycastsTotal + " / " + tileRaycastsTotal + "\n"
                + "Slowest tick in this interval: " + slowestSummary;
    }

    private String formatStats(LongList samples) {
        if (samples.isEmpty()) {
            return "n/a";
        }
        LongArrayList sorted = new LongArrayList(samples);
        sorted.sort(LongComparators.NATURAL_COMPARATOR);
        long total = 0;
        LongIterator iterator = sorted.longIterator();
        while (iterator.hasNext()) {
            total += iterator.nextLong();
        }
        int sampleCount = sorted.size();
        return TickTimingFormatter.formatMillis(total / sorted.size())
                + " / " + TickTimingFormatter.formatMillis(percentile(sorted, 0.50))
                + " / " + TickTimingFormatter.formatMillis(percentile(sorted, 0.95))
                + " / " + TickTimingFormatter.formatMillis(sorted.getLong(sampleCount - 1))
                + " ms";
    }

    private long percentile(LongList sorted, double percentile) {
        int index = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil(sorted.size() * percentile) - 1));
        return sorted.getLong(index);
    }
}
