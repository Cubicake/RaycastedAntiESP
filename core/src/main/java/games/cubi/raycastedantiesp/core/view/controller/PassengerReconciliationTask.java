package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;

import java.util.Arrays;

final class PassengerReconciliationTask extends BaseEntitySpawnTask {
    private final PacketEntityViewController<?> controller;
    private final PlayerData playerData;
    private final int vehicleEntityId;
    private final int[] passengerIds;

    PassengerReconciliationTask(PacketEntityViewController<?> controller, PlayerData playerData, int vehicleEntityId, int[] passengerIds, int submittedTick) {
        super(submittedTick);
        this.controller = controller;
        this.playerData = playerData;
        this.vehicleEntityId = vehicleEntityId;
        this.passengerIds = passengerIds.clone();
    }

    @Override
    public void run() {
        controller.handleEntityPassengersNow(vehicleEntityId, passengerIds, playerData, getSubmittedTick(), getSubmittedTick());
    }

    @Override
    public String toString() {
        return "PassengerReconciliationTask{" +
                "submittedTick=" + getSubmittedTick() +
                ", vehicleEntityId=" + vehicleEntityId +
                ", passengerIds=" + Arrays.toString(passengerIds) +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
