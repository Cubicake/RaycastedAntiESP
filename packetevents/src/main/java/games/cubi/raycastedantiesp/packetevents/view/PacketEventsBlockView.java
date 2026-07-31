/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.view;

import games.cubi.locatables.api.BlockSpatial;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.core.view.AbstractBlockView;
import games.cubi.raycastedantiesp.packetevents.tracked.PacketEventsTileEntity;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsTileEntityReplayData;

import java.util.function.IntSupplier;

public class PacketEventsBlockView extends AbstractBlockView<PacketEventsTileEntityReplayData, PacketEventsTileEntity> {
    public PacketEventsBlockView(BlockInfoResolver blockInfoResolver, boolean trackAllBlocks, IntSupplier worldEpochSupplier) {
        super(blockInfoResolver, trackAllBlocks, worldEpochSupplier);
    }

    @Override
    protected PacketEventsTileEntity createTrackedTileEntity(BlockSpatial position, char blockID, boolean visible) {
        return new PacketEventsTileEntity(position, visible, TrackedTileEntity.NEVER_CHECKED, blockID);
    }
}
