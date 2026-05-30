package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;

import java.util.Arrays;

final class PassengerReconciliationTask extends BaseEntitySpawnTask {
    private final PlayerData playerData;
    private final int queuedEntityId;
    private final int vehicleEntityId;
    private final int[] passengerIds;

    PassengerReconciliationTask(PlayerData playerData, int queuedEntityId, int vehicleEntityId, int[] passengerIds, int submittedTick) {
        super(submittedTick);
        this.playerData = playerData;
        this.queuedEntityId = queuedEntityId;
        this.vehicleEntityId = vehicleEntityId;
        this.passengerIds = passengerIds.clone();
    }

    @Override
    public void run() {
        NettyEntityLocatable<?,?> vehicle = playerData.entityFromID(vehicleEntityId);
        if (vehicle == null) {
            Logger.error("Reconciliation fail: Attempted to reconcile passengers for unknown vehicle, queuedEntityId=" + queuedEntityId + " vehicleEntityId=" + vehicleEntityId, 3, this.getClass());
            return;
        }
        PacketEntityViewController.get().handleEntityPassengersNow(vehicle, passengerIds, playerData, submittedTick);
    }

    @Override
    public String toString() {
        return "PassengerReconciliationTask{" +
                "submittedTick=" + submittedTick +
                ", queuedEntityId=" + queuedEntityId +
                ", vehicleEntityId=" + vehicleEntityId +
                ", passengerIds=" + Arrays.toString(passengerIds) +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
