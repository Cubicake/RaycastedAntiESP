/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.EntityTypeExclusions;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController;

import java.util.UUID;
import java.util.function.IntSupplier;

public class PaperPacketEventsEntityViewController extends PacketEventsEntityViewController {

    public PaperPacketEventsEntityViewController(IntSupplier currentTickSupplier) {
        super(currentTickSupplier);
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);
    }

    @Override
    protected boolean handleEntitySpawn(PacketWrapper<?> packet, int entityID, boolean isPlayer,
                                        PlayerData playerData, UUID world, int currentTick) {
        if (isPlayer || world == null) {
            return super.handleEntitySpawn(packet, entityID, isPlayer, playerData, world, currentTick);
        }

        WrapperPlayServerSpawnEntity spawnPacket = (WrapperPlayServerSpawnEntity) packet;
        int entityType = spawnPacket.getEntityType().getId(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()
        );
        if (!EntityTypeExclusions.excludes(entityType)) {
            return super.handleEntitySpawn(packet, entityID, false, playerData, world, currentTick);
        }

        NettyEntity<?> entity = Logger.requireNonNull(
                processEntitySpawn(playerData, packet, world, currentTick),
                "processEntitySpawn returned null",
                3,
                PaperPacketEventsEntityViewController.class
        );
        entity.setVisible(true);
        entity.setClientVisible(true);
        entity.setLastChecked(currentTick);
        insertEntityToEntityView(entity, playerData, world);
        playerData.nettyData().runPendingPostSpawnTaskForEntity(entityID);
        return false;
    }
}
