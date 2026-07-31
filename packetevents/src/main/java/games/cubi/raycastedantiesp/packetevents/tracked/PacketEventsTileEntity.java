/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.tracked;

import games.cubi.locatables.api.BlockSpatial;
import games.cubi.raycastedantiesp.core.tracked.NettyTileEntity;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsTileEntityReplayData;

public class PacketEventsTileEntity extends NettyTileEntity<PacketEventsTileEntityReplayData> {
    public PacketEventsTileEntity(BlockSpatial position, boolean visible, int lastChecked, char blockID) {
        super(position, visible, lastChecked, blockID);
    }
}
