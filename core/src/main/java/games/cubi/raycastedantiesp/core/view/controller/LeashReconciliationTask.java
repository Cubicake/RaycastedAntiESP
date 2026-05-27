package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;

final class LeashReconciliationTask extends BaseEntitySpawnTask {
    private final PacketEntityViewController<?> controller;
    private final PlayerData playerData;
    private final int leashedEntityId;
    private final int leashingEntityId;

    LeashReconciliationTask(PacketEntityViewController<?> controller, PlayerData playerData, int leashedEntityId, int leashingEntityId, int submittedTick) {
        super(submittedTick);
        this.controller = controller;
        this.playerData = playerData;
        this.leashedEntityId = leashedEntityId;
        this.leashingEntityId = leashingEntityId;
    }

    @Override
    public void run() {
        controller.handleLeashEntityNow(leashedEntityId, leashingEntityId, playerData, getSubmittedTick(), getSubmittedTick());
    }

    @Override
    public String toString() {
        return "LeashReconciliationTask{" +
                "submittedTick=" + getSubmittedTick() +
                ", queuedEntityId=" + leashedEntityId +
                ", leashedEntityId=" + leashedEntityId +
                ", leashingEntityId=" + leashingEntityId +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
