package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.Locatable;
import games.cubi.locatables.implementations.ThreadSafeLocatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.ViewRegistry;
import games.cubi.raycastedantiesp.core.view.controller.PacketEntityViewController;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class PlayerData {
    private final UUID playerUUID;
    private final int joinTick;
    private volatile boolean hasBypassPermission;
    private final ThreadSafeLocatable ownLocation;

    private final BlockView blockView;
    private final EntityView<?> entityView;
    private final EntityView<?> playerView;
    private final NettyData nettyData;

    public PlayerData(UUID player, boolean hasBypassPermission, int joinTick) {
        this(player, joinTick);
        this.hasBypassPermission = hasBypassPermission;
    }

    public PlayerData(UUID player, int joinTick) {
        this.joinTick = joinTick;
        this.playerUUID = player;

        blockView = ViewRegistry.createBlockView();
        entityView = ViewRegistry.createEntityView();
        playerView = ViewRegistry.createPlayerEntityView();
        nettyData = new NettyData();
        ownLocation = new ThreadSafeLocatable(null, 0, 0, 0);
    }

    public EntityView<?> entityView() {
        return entityView;
    }

    public EntityView<?> playerView() {
        return playerView;
    }

    public NettyData nettyData() {
        return nettyData;
    }

    private final Queue<Runnable> nettyTasks = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /**
     * Schedules a task to run as soon as possible on the Netty thread for this player. The task will be run immediately when the next packet is sent to this player. Safe to call from any thread.
     * @param task The task to run on the Netty thread for this player.
     * @Appropriate_Calling_Threads All
     */
    public void runNettyTaskASAP(Runnable task) {
        nettyTasks.add(task);
    }

    /**
      * @Appropriate_Calling_Threads Netty thread associated with this player
     */
    public void runAllNettyTasks() {
        List<Runnable> tasksToRun = new ArrayList<>();
        for (Runnable task; (task = nettyTasks.poll()) != null; ) {
            tasksToRun.add(task);
        }
        for (Runnable task : tasksToRun) {
            try {
                task.run();
            } catch (Exception e) {
                Logger.error("Error while running netty task for player " + playerUUID, e, 3, PlayerData.class);
            }
        }
    }

    public BlockView blockView() {
        return blockView;
    }

    public void updateOwnLocation(UUID world, double x, double y, double z) {
        ownLocation.set(x, y, z, world);
    }

    public Locatable ownLocation() {
        ThreadSafeLocatable existing = ownLocation;
        return existing == null ? null : existing.clonePlainAndCentreIfBlockLocation();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean hasBypassPermission() {
        return hasBypassPermission;
    }

    public int getJoinTick() {
        return joinTick;
    }

    /**
     * @return Either the entity or player view for this player, depending on the entity ID
     */
    public EntityView<?> viewFromEntityID(int entityID) {
        if (entityView.exists(entityID)) {
            return entityView;
        }
        if (playerView.exists(entityID)) {
            return playerView;
        }
        Logger.warning("Could not find view for entityID=" + entityID + " uuid=" + playerUUID, 6, PacketEntityViewController.class);
        return null;
    }

    public NettyEntityLocatable<?,?> entityFromID(int entityID) {
        EntityView<?> entityView = viewFromEntityID(entityID);
        if (entityView == null) {
            return null;
        }
        return (NettyEntityLocatable<?, ?>) entityView.getEntity(entityID);
    }

    public void setBypassPermission(boolean hasBypassPermission) {
        this.hasBypassPermission = hasBypassPermission;
    } //todo: need to link up

    @Override
    public String toString() {
        return "PlayerData{" +
                "playerUUID=" + playerUUID +
                ", joinTick=" + joinTick +
                ", hasBypassPermission=" + hasBypassPermission +
                ", ownLocation=" + ownLocation +
                ", blockView=" + blockView +
                ", entityView=" + entityView +
                ", playerView=" + playerView +
                ", nettyData=" + nettyData +
                '}';
    }
}
