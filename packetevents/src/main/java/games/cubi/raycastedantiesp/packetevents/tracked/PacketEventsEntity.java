/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.tracked;

import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsEntityReplayData;

import java.util.UUID;

public class PacketEventsEntity extends NettyEntity<PacketEventsEntityReplayData> {
    public PacketEventsEntity(PlayerData owningPlayer, double x, double y, double z, int entityID, UUID entityUUID, boolean isSelfEntity, int entityType, boolean visible) {
        super(owningPlayer, x, y, z, entityID, entityUUID, isSelfEntity, entityType, visible);
    }

    private PacketEventsEntity(PlayerData selfData, int selfEntityID, UUID selfEntityUUID) {
        super(selfData, selfEntityID, selfEntityUUID);
    }

    public static PacketEventsEntity createSelfEntity(PlayerData selfData, int selfEntityID, UUID selfEntityUUID) {
        return new PacketEventsEntity(selfData, selfEntityID, selfEntityUUID);
    }
}
