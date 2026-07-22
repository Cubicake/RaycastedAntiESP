package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.implementations.ThreadSafeLocatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.utils.VarHandler;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.ViewRegistry;
import games.cubi.raycastedantiesp.core.view.controller.PacketEntityViewController;

import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.UUID;
import java.util.function.IntSupplier;

public class PlayerData {
    public static final int INVALID_WORLD_EPOCH = -1;

    private final UUID playerUUID;
    private final int joinTick;
    private volatile boolean hasBypassPermission;
    private volatile boolean connected;
    private final ThreadSafeLocatable ownLocation;

    private final BlockView blockView;
    private final EntityView<?> entityView;
    private final EntityView<?> playerView;
    private final NettyData nettyData;
    // Even positive values are stable world sessions; odd values mean the views are being replaced.
    private volatile int worldEpoch; private static final VarHandle WORLD_EPOCH = VarHandler.get(PlayerData.class, "worldEpoch", int.class);
    private UUID viewWorld;

    PlayerData(UUID player, boolean hasBypassPermission, int joinTick, int selfEntityID, PlayerRegistry.SelfEntityCreator selfEntityCreator) {
        this.joinTick = joinTick;
        this.playerUUID = player;
        this.hasBypassPermission = hasBypassPermission;
        connected = true;

        IntSupplier worldEpochSupplier = this::acquireWorldEpoch;
        blockView = ViewRegistry.createBlockView(worldEpochSupplier);
        entityView = ViewRegistry.createEntityView(worldEpochSupplier);
        playerView = ViewRegistry.createPlayerEntityView(worldEpochSupplier);
        ownLocation = new ThreadSafeLocatable(null, 0, 0, 0);
        NettyEntity<?> selfEntity = Logger.requireNonNull(
                selfEntityCreator.createSelfEntity(this, selfEntityID, player),
                "Self entity creator returned null",
                3,
                PlayerData.class
        );
        nettyData = new NettyData(selfEntity);
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

    public BlockView blockView() {
        return blockView;
    }

    /** Acquire-reads the current player-wide view epoch. */
    public int acquireWorldEpoch() {
        return (int) WORLD_EPOCH.getAcquire(this);
    }

    /**
     * Returns a stable epoch for {@code expectedWorld}, or {@link #INVALID_WORLD_EPOCH} when the world session is not usable.
     */
    public int tryAcquireWorldEpochFor(UUID expectedWorld) {
        int before = acquireWorldEpoch();
        if (!isStableWorldEpoch(before)) {
            return INVALID_WORLD_EPOCH;
        }
        UUID currentWorld = viewWorld;
        int after = acquireWorldEpoch();
        if (before != after || !Objects.equals(currentWorld, expectedWorld)) {
            return INVALID_WORLD_EPOCH;
        }
        return before;
    }

    public static boolean isStableWorldEpoch(int epoch) {
        return epoch != 0 && (epoch & 1) == 0;
    }

    /** Structural-writer operation performed before clearing all world-scoped views. */
    public void beginWorldTransition() {
        int current = acquireWorldEpoch();
        if ((current & 1) != 0) { //current is odd
            throw new IllegalStateException("World transition already in progress");
        }
        WORLD_EPOCH.setRelease(this, current + 1);
        // Keep subsequent view-clearing writes behind the invalidating odd epoch.
        VarHandle.releaseFence();
        viewWorld = null;
    }

    /** Structural-writer operation performed after all world-scoped views have been cleared. */
    public void completeWorldTransition(UUID world) {
        int current = acquireWorldEpoch();
        if ((current & 1) == 0) { //current is even
            throw new IllegalStateException("No world transition in progress");
        }
        viewWorld = world;
        WORLD_EPOCH.setRelease(this, current + 1);
    }

    public void updateOwnLocation(UUID world, double x, double y, double z) {
        ownLocation.set(x, y, z, world);
    }

    /**
     * Returns a live, thread-safe view of the player's current location.
     * Casting the result to a mutable type and modifying it is unsupported.
     */
    public Locatable ownLocation() {
        return ownLocation;
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

    public boolean isConnected() {
        return connected;
    }

    public void markDisconnected() {
        connected = false;
        int current = acquireWorldEpoch();
        if ((current & 1) == 0) {
            WORLD_EPOCH.setRelease(this, current + 1);
        }
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

    public NettyEntity<?> entityFromID(int entityID) {
        if (nettyData.isSelfEntityID(entityID)) {
            return nettyData.getSelfEntity();
        }
        EntityView<?> entityView = viewFromEntityID(entityID);
        if (entityView == null) {
            return null;
        }
        return (NettyEntity<?>) entityView.getEntity(entityID);
    }

    public void setBypassPermission(boolean hasBypassPermission) {
        this.hasBypassPermission = hasBypassPermission;
    } //todo: need to link up

    @Override
    public String toString() {
        return "PlayerData{" +
                "playerUUID=" + playerUUID +
                ", joinTick=" + joinTick +
                ", hasBypassPermission=" + hasBypassPermission() +
                ", ownLocation=" + ownLocation +
                ", blockView=" + blockView +
                ", entityView=" + entityView +
                ", playerView=" + playerView +
                ", nettyData=" + nettyData +
                '}';
    }
}
