package games.cubi.raycastedantiesp.core.engine;

import games.cubi.locatables.ImmutableLocatable;
import games.cubi.locatables.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.config.DebugConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.raycast.ParticleSpawner;
import games.cubi.raycastedantiesp.core.raycast.RaycastUtil;
import games.cubi.raycastedantiesp.core.utils.PrimitiveIntArrayList;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.EntityView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

public abstract class SimpleEngine implements Engine {
    private static final long SLOW_TICK_NANOS = 40 * 1_000_000L;
    //Literally just magic numbers I made by keyboard mashing
    private static final int TICK_IDLE = 1872;
    private static final int TICK_PENDING = -129;
    private static final int TICK_RUNNING = 34892;

    private final ConfigManager config;
    private final ParticleSpawner particleSpawner;
    private final IntSupplier currentTickSupplier;
    private final AtomicInteger tickThreadsRunning = new AtomicInteger(0);
    private final AtomicInteger runningTick = new AtomicInteger(-1);
    private final AtomicInteger tickState = new AtomicInteger(TICK_IDLE);
    private final AtomicLong tickNanos = new AtomicLong(0);
    private final AsyncRunner asyncRunner;
    private volatile TimingStats timingStats = TimingStatsNoOp.INSTANCE;

    public SimpleEngine(ConfigManager config, ParticleSpawner particleSpawner, IntSupplier currentTickSupplier, AsyncRunner asyncRunner) {
        this.config = config;
        this.particleSpawner = particleSpawner;
        this.currentTickSupplier = currentTickSupplier;
        this.asyncRunner = asyncRunner;
    }

    /**
     * Reserves the next engine tick before it is handed to the async scheduler.
     *
     * @return true if the caller now owns the only pending tick slot; false if an existing pending
     * or running tick should cover this attempt.
     */
    public boolean markTickRunning() {
        if (tickState.compareAndSet(TICK_IDLE, TICK_PENDING)) {
            return true;
        }
        recordSkippedTick(-1, System.nanoTime());
        return false;
    }

    /**
     * Releases a pending tick reservation when async scheduling fails before {@link #tick(int, long)}
     * can claim it.
     */
    public void cancelPendingTickReservation() {
        if (!tickState.compareAndSet(TICK_PENDING, TICK_IDLE)) {
            Logger.warning("Attempted to cancel a pending tick reservation, but the tick was no longer pending.", 5, SimpleEngine.class);
        }
    }

    /**
     * Runs the tick which was reserved using {@link #markTickRunning} and, if the engine falls behind the server clock, keeps the
     * same worker on the latest tick instead of yielding back to the scheduler.
     *
     * @param scheduledTick the server tick captured before async handoff.
     * @param scheduledNanos the time captured before async handoff, used to separate queue delay from engine work.
     */
    @Override
    public void tick(int scheduledTick, long scheduledNanos) {
        boolean runTick = true;
        boolean expectPending = true;
        int startTick = currentTickSupplier.getAsInt();
        while (runTick) {
            if (!startTickState(scheduledTick, expectPending)) {
                return;
            }
            if (!dispatchTick(scheduledTick, startTick, scheduledNanos)) {
                return;
            }

            int latestTick = currentTickSupplier.getAsInt();
            if (latestTick > startTick) {
                // If running behind, don't yield the thread, just run the next tick immediately
                Logger.warning("Tick thread completed tick #" + startTick + " after tick #" + latestTick + " had already begun. Starting next tick immediately instead of yielding thread to scheduler. This is probably safe to ignore but may suggest that your server is overloaded.", 5);
                startTick = latestTick;
                scheduledNanos = System.nanoTime();
                scheduledTick = latestTick;

                if (tickState.get() == TICK_RUNNING) {
                    // This means that there is already a tick running, so we should exit
                    Logger.info("Tick finished behind but another thread had already begun ticking the next tick.", 5, SimpleEngine.class);
                    return;
                }
                expectPending = false; //Next call to startTickState should not expect to see status set to pending
            }
            else runTick = false;
        }
    }

    /**
     * Claims worker capacity and either runs work immediately or schedules worker batches.
     *
     * @return true if at least one sub-tick worker accepted responsibility for completing the tick
     * lifecycle; false if this attempt was skipped or cleaned up during setup.
     */
    private boolean dispatchTick(int scheduledTick, int startTick, long scheduledNanos) {
        int threads = config.getEngineConfig().simpleConfig().asyncProcessingThreads();
        if (threads < 1) threads = 1;

        DebugConfig debugConfig = config.getDebugConfig();
        TimingStats tickTimingStats = timingStats(debugConfig);
        boolean recordTimings = tickTimingStats != TimingStatsNoOp.INSTANCE;

        long startNanos = System.nanoTime();

        int tickAlreadyRunning = runningTick.get();
        // First guard for the current tick already being processed (can happen if the previous tick ran overtime and thus didn't yield the thread)
        if (scheduledTick == tickAlreadyRunning) {
            logAggregateReport(tickTimingStats.recordSkipped(threads, startNanos));
            Logger.info("RaycastedAntiESP is already processing this tick; skipping duplicate same-tick attempt."
                    + " scheduledTick=" + scheduledTick
                    + " currentServerTick=" + startTick
                    + " currentRunningTick=" + tickAlreadyRunning, 6, SimpleEngine.class);
            finishTickState();
            return false;
        }

        int runningThreads = tickThreadsRunning.compareAndExchange(0, threads);
        // Now guard for previous tick still running
        if (runningThreads != 0) {
            long queueNanos = Math.max(0, startNanos - scheduledNanos);
            Logger.warning("RaycastedAntiESP is still ticking from the last tick! Skipping this tick to avoid concurrent modification issues."
                    + " scheduledTick=" + scheduledTick
                    + " currentServerTick=" + currentTickSupplier.getAsInt()
                    + " currentRunningTick=" + tickAlreadyRunning
                    + " runningThreads=" + runningThreads
                    + " timeSpentInQueue=" + TickTimingFormatter.formatMillis(queueNanos) + "ms", 5, SimpleEngine.class);
            logAggregateReport(tickTimingStats.recordSkipped(threads, startNanos));
            finishTickState();
            return false;
        }

        TickTimings timings = null;
        boolean handedOffToSubTick = false;
        boolean claimedRunningTick = false;
        try {
            tickNanos.set(startNanos);

            final int currentTick = startTick;
            runningTick.set(currentTick);
            claimedRunningTick = true;
            Collection<PlayerData> allPlayers = PlayerRegistry.getInstance().getAllPlayerData();

            EntityConfig entityConfig = config.getEntityConfig();
            PlayerConfig playerConfig = config.getPlayerConfig();
            TileEntityConfig tileEntityConfig = config.getTileEntityConfig();
            if (recordTimings) {
                timings = new TickTimings(scheduledTick, scheduledNanos, currentTick, startNanos, threads, allPlayers.size());
            }
            /*
            Logger.debug("Tick #" + currentTick);
            if (currentTick % 1200 == 0) {
                Logger.debug("Printing player data");
                for (PlayerData playerData : allPlayers) {
                    Logger.debug("Player " + playerData.getPlayerUUID() + " location=" + playerData.ownLocation());
                    Logger.debug("EntityView:" + playerData.entityView().getStringDataForDebugging());
                    Logger.debug("PlayerView:" + playerData.playerView().getStringDataForDebugging());
                }
            }*/

            // If only one thread is configured, just use the current async thread to avoid the overhead of scheduling tasks and context switching.
            if (threads == 1) {
                handedOffToSubTick = true;
                subTick(new ArrayList<>(allPlayers), entityConfig, playerConfig, tileEntityConfig, debugConfig, currentTick, timings, tickTimingStats);
                return true;
            }

            List<List<PlayerData>> batches = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                batches.add(new ArrayList<>());
            }

            int index = 0;
            for (PlayerData playerData : allPlayers) {
                batches.get(index++ % threads).add(playerData);
            }

            TickTimings tickTimings = timings; // needs to be effectively-final for lambda
            int scheduledBatches = 0;
            try {
                for (List<PlayerData> batch : batches) {
                    asyncRunner.runNow(() -> subTick(batch, entityConfig, playerConfig, tileEntityConfig, debugConfig, currentTick, tickTimings, tickTimingStats));
                    scheduledBatches++;
                }
                handedOffToSubTick = true;
            }
            finally {
                if (scheduledBatches < threads) {
                    tickThreadsRunning.addAndGet(-(threads - scheduledBatches));
                    handedOffToSubTick = scheduledBatches > 0;
                }
            }
            return handedOffToSubTick;
        }
        finally {
            if (!handedOffToSubTick) {
                tickThreadsRunning.set(0);
                if (claimedRunningTick) {
                    finaliseTick(startTick);
                } else {
                    finishTickState();
                }
                Logger.error("An error occurred during tick scheduling before handing off to sub-tick processing. Resetting tickThreadsRunning to 0 to avoid deadlock. Current tick: " + currentTickSupplier.getAsInt(), 2, SimpleEngine.class);
            }
        }
    }

    /**
     * Processes one worker batch and lets the final batch publish timing data and release the tick reservation.
     *
     * @param timingStats the timing sink selected when this tick started, so config changes during
     * worker execution do not split one tick across sinks.
     */
    private void subTick(List<PlayerData> batch, EntityConfig entityConfig, PlayerConfig playerConfig, TileEntityConfig tileEntityConfig, DebugConfig debugConfig, int currentTick, TickTimings timings, TimingStats timingStats) {
        TickTimingBatch batchTimings = timings == null ? TickTimingBatchNoOp.INSTANCE : new TickTimingBatch();
        long batchStartNanos = batchTimings.startBatch();
        try {
            processTickForPlayers(batch, entityConfig, playerConfig, tileEntityConfig, debugConfig.showDebugParticles(), currentTick, batchTimings);
        }
        finally {
            if (timings != null) {
                timings.recordBatch(batchTimings, batchTimings.elapsedSince(batchStartNanos));
            }
            int threadsRemaining = tickThreadsRunning.decrementAndGet();
            if (threadsRemaining < 0) {
                Logger.error("tickThreadsRunning went below 0! This should never happen. Resetting to 0 to avoid further issues.", 2, SimpleEngine.class);
                tickThreadsRunning.set(0);
                finaliseTick(currentTick);
            }
            if (threadsRemaining == 0) {
                try {
                    long completionNanos = System.nanoTime();
                    if (timings != null) {
                        TickTimingSnapshot snapshot = timings.snapshot(currentTickSupplier.getAsInt(), completionNanos);
                        String aggregateReport = timingStats.recordCompleted(snapshot, completionNanos);
                        if (snapshot.wallNanos() > SLOW_TICK_NANOS) {
                            Logger.warning(snapshot.toSlowTickMessage(), 5, SimpleEngine.class);
                        }
                        logAggregateReport(aggregateReport);
                    } else {
                        long elapsedNanos = completionNanos - tickNanos.get();
                        if (elapsedNanos > SLOW_TICK_NANOS) {
                            Logger.warning("Tick completed in " + (elapsedNanos / 1_000_000.0) + " ms. If you see this warning frequently, consider reducing the raycasting load by adjusting the configuration.", 5, SimpleEngine.class);
                        }
                    }
                } finally {
                    finaliseTick(currentTick);
                }
            }
        }
    }

    /**
     * Moves the reserved tick into the running state. Catch-up iterations may start from idle
     * because they are created by the current worker instead of by the server tick event.
     *
     * @return true if this thread now owns the running state; false if another pending or running
     * tick should take precedence.
     */
    private boolean startTickState(int scheduledTick, boolean expectPending) {
        if (expectPending) {
            if (tickState.compareAndSet(TICK_PENDING, TICK_RUNNING)) {
                return true;
            }
            if (tickState.compareAndSet(TICK_IDLE, TICK_RUNNING)) {
                // This means that this tick was dispatched but not marked as such, or was unmarked at some point.
                Logger.warning("Tick " + scheduledTick + " was dispatched but not marked as pending. This suggests a race condition. Please report this on our GitHub or Discord.", 5, SimpleEngine.class);
                return true;
            }
        } else if (tickState.compareAndSet(TICK_IDLE, TICK_RUNNING)) {
            return true;
        }

        // This means there is already a tick pending or running, so we should exit.
        recordSkippedTick(-1, System.nanoTime());
        Logger.info("Tick " + scheduledTick + " is pending but another tick is already pending or running. Exiting now.", 6, SimpleEngine.class);
        return false;
    }

    /**
     * Releases ownership for the tick that completed, without clearing a newer running tick that
     * may have already taken over.
     */
    private void finaliseTick(int currentTick) {
        runningTick.compareAndSet(currentTick, -1);
        finishTickState();
    }

    /**
     * Returns the reservation state to idle after the active tick has stopped touching shared player visibility data.
     */
    private void finishTickState() {
        if (!tickState.compareAndSet(TICK_RUNNING, TICK_IDLE)) {
            // This should never happen as it means the tick was marked as completed before processing was completed
            Logger.warning("tickState was not running when completing tick! This should never happen. Please report this on our GitHub or Discord.", 5, SimpleEngine.class);
        }
    }

    private void processTickForPlayers(List<PlayerData> playerDataList, EntityConfig entityConfig, PlayerConfig playerConfig, TileEntityConfig tileEntityConfig,
                                       boolean debugParticles, int currentTick, TickTimingBatch timings) {

        for (PlayerData playerData : playerDataList) {
            if (!playerData.isConnected()) {
                continue;
            }
            playerData.nettyData().markPendingPostSpawnTasksForEviction();
            if (playerData.hasBypassPermission()) {
                timings.incrementBypassSkippedPlayers();
                continue;
            }

            BlockView blockView = playerData.blockView();

            Locatable playerLocation = playerData.ownLocation();
            if (playerLocation == null || playerLocation.world() == null) {
                timings.incrementNullLocationSkippedPlayers();
                continue;
            }
            timings.incrementProcessedPlayers();

            if (entityConfig.enabled()) {
                long sectionStartNanos = timings.startEntitySection();
                checkEntities(playerData, playerLocation, entityConfig, debugParticles, blockView, currentTick, timings);
                timings.finishEntitySection(sectionStartNanos);
            }
            if (playerConfig.enabled()) {
                long sectionStartNanos = timings.startPlayerSection();
                checkPlayers(playerData, playerLocation, playerConfig, debugParticles, blockView, currentTick, timings);
                timings.finishPlayerSection(sectionStartNanos);
            }
            if (tileEntityConfig.enabled()) {
                long sectionStartNanos = timings.startTileSection();
                checkTileEntities(playerData, playerLocation, tileEntityConfig, debugParticles, blockView, currentTick, timings);
                timings.finishTileSection(sectionStartNanos);
            }
        }
    }

    private void checkEntities(PlayerData player, Locatable playerLocation, EntityConfig entityConfig, boolean debugParticles, BlockView blockView, int currentTick, TickTimingBatch timings) {
        EntityView<?> entityView = player.entityView();

        int checked = entityView.forEachNeedingRecheckEntity(entityConfig.getVisibleRecheckIntervalTicks(), currentTick, !(timings instanceof TickTimingBatchNoOp), entity -> {
            boolean wasVisible = entity.visible();
            if (attachedToSelf(player, entityView, entity, currentTick)) {
                return;
            }
            ImmutableLocatable entityLocation = entity.getOffsetEntityLocation();
            if (entityLocation == null) {
                timings.incrementEntityNullTargets();
                Logger.debug("SimpleEngine.checkEntities skipped-null-location viewer=" + player.getPlayerUUID()
                        + " target=" + entity.entityUUID()
                        + " wasVisible=" + wasVisible
                        + " tick=" + currentTick);
                return;
            }
            timings.incrementEntityRaycasts();
            boolean canSee = RaycastUtil.raycast(player, playerLocation, entityLocation, entityConfig.getMaxOccludingCount(), entityConfig.getAlwaysShowRadius(), entityConfig.getRaycastRadius(), debugParticles, blockView, 1, particleSpawner);
            entityView.setVisibility(entity, canSee, currentTick);
        });
        timings.addEntityChecked(checked);
    }

    private void checkPlayers(PlayerData player, Locatable playerLocation, PlayerConfig playerConfig, boolean debugParticles, BlockView blockView, int currentTick, TickTimingBatch timings) {
        EntityView<?> playerView = player.playerView();

        int checked = playerView.forEachNeedingRecheckEntity(playerConfig.getVisibleRecheckIntervalTicks(), currentTick, !(timings instanceof TickTimingBatchNoOp), otherPlayer -> {
            boolean wasVisible = otherPlayer.visible();
            if (attachedToSelf(player, playerView, otherPlayer, currentTick)) {
                return;
            }
            ImmutableLocatable otherPlayerLocation = otherPlayer.getOffsetEntityLocation();
            if (otherPlayerLocation == null) {
                timings.incrementPlayerNullTargets();
                Logger.debug("SimpleEngine.checkPlayers skipped-null-location viewer=" + player.getPlayerUUID()
                        + " target=" + otherPlayer.entityUUID()
                        + " wasVisible=" + wasVisible
                        + " tick=" + currentTick);
                return;
            }
            timings.incrementPlayerRaycasts();
            boolean canSee = RaycastUtil.raycast(player, playerLocation, otherPlayerLocation, playerConfig.getMaxOccludingCount(), playerConfig.getAlwaysShowRadius(), playerConfig.getRaycastRadius(), debugParticles, blockView, 1, particleSpawner);
            playerView.setVisibility(otherPlayer, canSee, currentTick);
        });
        timings.addPlayerChecked(checked);
    }

    private boolean attachedToSelf(PlayerData player, EntityView<?> view, NettyEntityLocatable<?,?> entity, int currentTick) {
        int selfEntityID = player.nettyData().getSelfEntityID();
        if (!player.nettyData().isSelfEntityID(entity.leashingEntity())
                && !player.nettyData().isSelfEntityID(entity.vehicleID())
                && !PrimitiveIntArrayList.contains(entity.passengerIDs(), selfEntityID)) {
            return false;
        }
        view.setVisibility(entity, true, currentTick);
        return true;
    }

    private void checkTileEntities(PlayerData player, Locatable playerLocation, TileEntityConfig tileEntityConfig, boolean debugParticles, BlockView blockView, int currentTick, TickTimingBatch timings) {
        int checked = blockView.updateVisibilityForEachNeedingRecheck(tileEntityConfig.getVisibleRecheckIntervalTicks(), currentTick, tileEntityLocation -> {
            if (tileEntityLocation.world() == null || !tileEntityLocation.world().equals(playerLocation.world())) {
                timings.incrementTileWorldSkipped();
                return BlockView.VisibilityResolver.SKIPPED;
            }

            if (playerLocation.distanceSquared(tileEntityLocation) > (double) tileEntityConfig.getRaycastRadius() * tileEntityConfig.getRaycastRadius()) {
                timings.incrementTileRadiusSkipped();
                return BlockView.VisibilityResolver.HIDE;
            }
            timings.incrementTileRaycasts();
            boolean canSee = RaycastUtil.raycast(player, playerLocation, tileEntityLocation, tileEntityConfig.getMaxOccludingCount() + 1, tileEntityConfig.getAlwaysShowRadius(), tileEntityConfig.getRaycastRadius(), debugParticles, blockView, 1, particleSpawner);
            return canSee ? BlockView.VisibilityResolver.SHOW : BlockView.VisibilityResolver.HIDE;
        });
        timings.addTileChecked(checked);
    }

    private static void logAggregateReport(String aggregateReport) {
        if (aggregateReport != null) {
            Logger.info(aggregateReport, 4, SimpleEngine.class);
        }
    }

    /**
     * Counts a skipped tick against the currently configured timing sink.
     */
    private void recordSkippedTick(int threads, long nowNanos) {
        logAggregateReport(timingStats().recordSkipped(threads, nowNanos));
    }

    /**
     * Selects the timing sink from the current debug config.
     *
     * @return a collecting {@link TimingStats} while timing diagnostics are enabled, otherwise the
     * no-op singleton.
     */
    private TimingStats timingStats() {
        return timingStats(config.getDebugConfig());
    }

    /**
     * Keeps disabled timing diagnostics allocation-free and starts a fresh collection window when
     * diagnostics are re-enabled.
     *
     * @return the timing sink that should be used for a newly starting tick or skip record.
     */
    private TimingStats timingStats(DebugConfig debugConfig) {
        if (!debugConfig.recordTimings()) {
            timingStats = TimingStatsNoOp.INSTANCE;
            return timingStats;
        }

        if (timingStats == TimingStatsNoOp.INSTANCE) {
            timingStats = new TimingStats();
        }
        return timingStats;
    }
}
