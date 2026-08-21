/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;
import games.cubi.raycastedantiesp.packetevents.tracked.PacketEventsEntity;

final class PECacheablePacketReconciliationTask extends BaseEntitySpawnTask {
    private final PlayerData playerData;
    private final int entityID;
    private final PacketWrapper<?> packet;

    PECacheablePacketReconciliationTask(PlayerData playerData, int entityID, PacketWrapper<?> packet, int submittedTick) {
        super(submittedTick);
        this.playerData = playerData;
        this.entityID = entityID;
        this.packet = packet;
    }

    @Override
    public void run() {
        NettyEntity<?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Reconciliation fail: Attempted to cache packet for unknown entity, id=" + entityID + " packet=" + packet.getClass().getSimpleName() + ".", 3, this.getClass());
            if (packet instanceof WrapperPlayServerEntityMetadata metadataPacket) {
                var data = metadataPacket.getEntityMetadata();
                for (EntityData<?> eData : data) {
                    Logger.debug("Index:"+eData.getIndex()+"Type:"+eData.getType()+"Value:"+eData.getValue());
                }
            }
            return;
        }
        if (packet instanceof WrapperPlayServerEntityMetadata metadataPacket) {
            PacketEventsEntityViewController.applyTrackedMetadata(entity, metadataPacket.getEntityMetadata());
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
