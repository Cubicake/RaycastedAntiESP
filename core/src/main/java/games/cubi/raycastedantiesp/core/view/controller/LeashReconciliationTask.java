package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;

final class LeashReconciliationTask extends BaseEntitySpawnTask {
    private final PlayerData playerData;
    private final int leashedEntityID;
    private final int leashingEntityID;

    LeashReconciliationTask(PlayerData playerData, int leashedEntityID, int leashingEntityID, int submittedTick) {
        super(submittedTick);
        this.playerData = playerData;
        this.leashedEntityID = leashedEntityID;
        this.leashingEntityID = leashingEntityID;
    }

    @Override
    public void run() {
        NettyEntityLocatable<?,?> leashed = playerData.entityFromID(leashedEntityID);
        if (leashed == null) {
            Logger.error("Reconciliation fail: Attempted to reconcile leash for unknown leashed entity, leashedEntityId=" + leashedEntityID, 3, this.getClass());
            return;
        }
        PacketEntityViewController.get().handleLeashEntityNow(leashed, leashingEntityID, playerData);
    }

    @Override
    public String toString() {
        return "LeashReconciliationTask{" +
                "submittedTick=" + submittedTick +
                ", leashedEntityId=" + leashedEntityID +
                ", leashingEntityId=" + leashingEntityID +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
