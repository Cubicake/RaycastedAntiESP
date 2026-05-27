package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;

final class PECacheablePacketReconciliationTask extends BaseEntitySpawnTask {
    private final PacketEventsEntityViewController controller;
    private final PlayerData playerData;
    private final int entityId;
    private final PacketWrapper<?> packet;

    PECacheablePacketReconciliationTask(PacketEventsEntityViewController controller, PlayerData playerData, int entityId, PacketWrapper<?> packet, int submittedTick) {
        super(submittedTick);
        this.controller = controller;
        this.playerData = playerData;
        this.entityId = entityId;
        this.packet = packet;
    }

    @Override
    public void run() {
        controller.cachePacketNow(packet, entityId, playerData, getSubmittedTick());
    }

    @Override
    public String toString() {
        return "PECacheablePacketReconciliationTask{" +
                "submittedTick=" + getSubmittedTick() +
                ", entityId=" + entityId +
                ", packetType=" + packet.getClass().getSimpleName() +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
