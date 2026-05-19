package games.cubi.raycastedantiesp.core.engine;

import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.config.DebugConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.raycast.ParticleSpawner;
import games.cubi.raycastedantiesp.core.raycast.RaycastUtil;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.EntityView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

public class SimpleEngine implements Engine {
    private final ConfigManager config;
    private final ParticleSpawner particleSpawner;
    private final IntSupplier currentTickSupplier;
    private final AtomicInteger tickThreadsRunning = new AtomicInteger(0);
    private final AtomicLong tickNanos = new AtomicLong(0);
    private final AsyncRunner asyncRunner;

    public SimpleEngine(ConfigManager config, ParticleSpawner particleSpawner, IntSupplier currentTickSupplier, AsyncRunner asyncRunner) {
        this.config = config;
        this.particleSpawner = particleSpawner;
        this.currentTickSupplier = currentTickSupplier;
        this.asyncRunner = asyncRunner;
    }

    @Override
    public void tick() {
        int threads = config.getEngineConfig().simpleConfig().asyncProcessingThreads();
        if (threads < 1) threads = 1;

        if (!tickThreadsRunning.compareAndSet(0, threads)) {
            Logger.warning("RaycastedAntiESP is still ticking from the last tick! Skipping this tick to avoid concurrent modification issues. Current tick: " + currentTickSupplier.getAsInt(), 2, SimpleEngine.class);
            return;
        }

        tickNanos.set(System.nanoTime());

        final int currentTick = currentTickSupplier.getAsInt();
        Collection<PlayerData> allPlayers = PlayerRegistry.getInstance().getAllPlayerData();

        EntityConfig entityConfig = config.getEntityConfig();
        PlayerConfig playerConfig = config.getPlayerConfig();
        TileEntityConfig tileEntityConfig = config.getTileEntityConfig();
        DebugConfig debugConfig = config.getDebugConfig();

        Logger.debug("Tick #" + currentTick);
        if (currentTick % 1200 == 0) {
            Logger.debug("Printing player data");
            for (PlayerData playerData : allPlayers) {
                Logger.debug("Player " + playerData.getPlayerUUID() + " location=" + playerData.ownLocation());
                Logger.debug("EntityView:"+ playerData.entityView().getStringDataForDebugging());
                Logger.debug("PlayerView:"+ playerData.playerView().getStringDataForDebugging());
            }
        }

        // If only one thread is configured, just use the current async thread to avoid the overhead of scheduling tasks and context switching.
        if (threads == 1) {
            subTick(new ArrayList<>(allPlayers), entityConfig, playerConfig, tileEntityConfig, debugConfig, currentTick);
            return;
        }

        List<List<PlayerData>> batches = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            batches.add(new ArrayList<>());
        }

        int index = 0;
        for (PlayerData playerData : allPlayers) {
            batches.get(index++ % threads).add(playerData);
        }

        for (List<PlayerData> batch : batches) {
            asyncRunner.runNow(() -> subTick(batch, entityConfig, playerConfig, tileEntityConfig, debugConfig, currentTick));
        }
    }

    private void subTick(List<PlayerData> batch, EntityConfig entityConfig, PlayerConfig playerConfig, TileEntityConfig tileEntityConfig, DebugConfig debugConfig, int currentTick) {
        try {
            processTickForPlayers(batch, entityConfig, playerConfig, tileEntityConfig, debugConfig.showDebugParticles(), currentTick);
        }
        finally {
            int threadsRemaining = tickThreadsRunning.decrementAndGet();
            if (threadsRemaining < 0) {
                Logger.warning("tickThreadsRunning went below 0! This should never happen. Resetting to 0 to avoid further issues.", 2, SimpleEngine.class);
                tickThreadsRunning.set(0);
            }
            if (threadsRemaining == 0) {
                long elapsedNanos = System.nanoTime() - tickNanos.get();
                if (elapsedNanos > 40 * 1000000) {//40 ms
                    Logger.warning("Tick completed in " + (elapsedNanos / 1_000_000.0) + " ms. If you see this warning frequently, consider reducing the raycasting load by adjusting the configuration.", 5, SimpleEngine.class);
                }
            }
        }
    }

    private void processTickForPlayers(List<PlayerData> playerDataList, EntityConfig entityConfig, PlayerConfig playerConfig, TileEntityConfig tileEntityConfig,
                                       boolean debugParticles, int currentTick) {

        for (PlayerData playerData : playerDataList) {
            if (playerData.hasBypassPermission()) continue;

            BlockView blockView = playerData.blockView();

            Locatable playerLocation = playerData.ownLocation();
            if (playerLocation == null) {
                continue;
            }

            if (entityConfig.enabled()) checkEntities(playerData, playerLocation, entityConfig, debugParticles, blockView, currentTick);
            if (playerConfig.enabled()) checkPlayers(playerData, playerLocation, playerConfig, debugParticles, blockView, currentTick);
            if (tileEntityConfig.enabled()) checkTileEntities(playerData, playerLocation, tileEntityConfig, debugParticles, blockView, currentTick);
        }
    }

    private void checkEntities(PlayerData player, Locatable playerLocation, EntityConfig entityConfig, boolean debugParticles, BlockView blockView, int currentTick) {
        EntityView<?> entityView = player.entityView();

        for (UUID entityUUID : entityView.getNeedingRecheck(entityConfig.getVisibleRecheckIntervalTicks(), currentTick)) {
            boolean wasVisible = entityView.isVisible(entityUUID, currentTick);
            Locatable entityLocation = entityView.getLocation(entityUUID);
            if (entityLocation == null) {
                Logger.debug("SimpleEngine.checkEntities skipped-null-location viewer=" + player.getPlayerUUID()
                        + " target=" + entityUUID
                        + " wasVisible=" + wasVisible
                        + " tick=" + currentTick);
                continue;
            }
            boolean canSee = RaycastUtil.raycast(player, playerLocation, entityLocation, entityConfig.getMaxOccludingCount(), entityConfig.getAlwaysShowRadius(), entityConfig.getRaycastRadius(), debugParticles, blockView, 1, particleSpawner);
            entityView.setVisibility(entityUUID, canSee, currentTick);
        }
    }

    private void checkPlayers(PlayerData player, Locatable playerLocation, PlayerConfig playerConfig, boolean debugParticles, BlockView blockView, int currentTick) {
        EntityView<?> playerView = player.playerView();

        for (UUID otherPlayerUUID : playerView.getNeedingRecheck(playerConfig.getVisibleRecheckIntervalTicks(), currentTick)) {
            boolean wasVisible = playerView.isVisible(otherPlayerUUID, currentTick);
            Locatable otherPlayerLocation = playerView.getLocation(otherPlayerUUID);
            if (otherPlayerLocation == null) {
                Logger.debug("SimpleEngine.checkPlayers skipped-null-location viewer=" + player.getPlayerUUID()
                        + " target=" + otherPlayerUUID
                        + " wasVisible=" + wasVisible
                        + " tick=" + currentTick);
                continue;
            }
            boolean canSee = RaycastUtil.raycast(player, playerLocation, otherPlayerLocation, playerConfig.getMaxOccludingCount(), playerConfig.getAlwaysShowRadius(), playerConfig.getRaycastRadius(), debugParticles, blockView, 1, particleSpawner);
            playerView.setVisibility(otherPlayerUUID, canSee, currentTick);
        }
    }

    private void checkTileEntities(PlayerData player, Locatable playerLocation, TileEntityConfig tileEntityConfig, boolean debugParticles, BlockView blockView, int currentTick) {
        for (BlockLocatable tileEntityLocation : blockView.getNeedingRecheck(tileEntityConfig.getVisibleRecheckIntervalTicks(), currentTick)) {
            if (tileEntityLocation.world() == null || !tileEntityLocation.world().equals(playerLocation.world())) {
                continue;
            }

            if (playerLocation.distanceSquared(tileEntityLocation) > (double) tileEntityConfig.getRaycastRadius() * tileEntityConfig.getRaycastRadius()) {
                blockView.setVisibility(tileEntityLocation, false, currentTick);
                continue;
            }
            boolean canSee = RaycastUtil.raycast(player, playerLocation, tileEntityLocation, tileEntityConfig.getMaxOccludingCount() + 1, tileEntityConfig.getAlwaysShowRadius(), tileEntityConfig.getRaycastRadius(), debugParticles, blockView, 1, particleSpawner);
            blockView.setVisibility(tileEntityLocation, canSee, currentTick);
        }
    }
}
