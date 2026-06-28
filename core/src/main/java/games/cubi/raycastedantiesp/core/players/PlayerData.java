package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.Locatable;
import games.cubi.locatables.implementations.ThreadSafeLocatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.ViewRegistry;
import games.cubi.raycastedantiesp.core.view.controller.PacketEntityViewController;

import java.util.UUID;

public class PlayerData {
    private final UUID playerUUID;
    private final int joinTick;
    private volatile boolean hasBypassPermission;
    private volatile boolean connected = true;
    private final ThreadSafeLocatable ownLocation;

    private final BlockView blockView;
    private final EntityView<?> entityView;
    private final EntityView<?> playerView;
    private final NettyData nettyData;

    PlayerData(UUID player, boolean hasBypassPermission, int joinTick, int selfEntityID, PlayerRegistry.SelfEntityCreator selfEntityCreator) {
        this.joinTick = joinTick;
        this.playerUUID = player;
        this.hasBypassPermission = hasBypassPermission;

        blockView = ViewRegistry.createBlockView();
        entityView = ViewRegistry.createEntityView();
        playerView = ViewRegistry.createPlayerEntityView();
        ownLocation = new ThreadSafeLocatable(null, 0, 0, 0);
        NettyEntityLocatable<?, ?> selfEntity = Logger.requireNonNull(
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

    public boolean isConnected() {
        return connected;
    }

    public void markDisconnected() {
        connected = false;
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
        if (nettyData.isSelfEntityID(entityID)) {
            return nettyData.getSelfEntity();
        }
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
