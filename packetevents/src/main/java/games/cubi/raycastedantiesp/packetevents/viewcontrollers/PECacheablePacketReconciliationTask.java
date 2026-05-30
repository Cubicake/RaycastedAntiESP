package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;
import games.cubi.raycastedantiesp.packetevents.locatables.PacketEventsEntity;

final class PECacheablePacketReconciliationTask extends BaseEntitySpawnTask {
    private final PlayerData playerData;
    private final int entityID;
    private final PacketWrapper<?> packet;

    PECacheablePacketReconciliationTask(PacketEventsEntityViewController controller, PlayerData playerData, int entityID, PacketWrapper<?> packet, int submittedTick) {
        super(submittedTick);
        this.playerData = playerData;
        this.entityID = entityID;
        this.packet = packet;
    }

    @Override
    public void run() {
        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Reconciliation fail: Attempted to cache packet for unknown entity, id=" + entityID + " packet=" + packet.getClass().getSimpleName() + ".", 3, this.getClass());
            return;
        }
        PacketEventsEntityViewController.get().ensureReplayData((PacketEventsEntity) entity).addPacket(packet);
    }

    @Override
    public String toString() {
        return "PECacheablePacketReconciliationTask{" +
                "submittedTick=" + submittedTick +
                ", entityID=" + entityID +
                ", packetType=" + packet.getClass().getSimpleName() +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
