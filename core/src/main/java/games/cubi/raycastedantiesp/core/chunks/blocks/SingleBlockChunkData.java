/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks.blocks;

import games.cubi.raycastedantiesp.core.chunks.BlockChunkData;

import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.chunks.ChunkData;

// If it is air, just use a null reference
public class SingleBlockChunkData implements BlockChunkData {
    final char blockID;
    private final boolean occluding;

    public SingleBlockChunkData(char blockID, boolean occluding) {
        this.blockID = blockID;
        this.occluding = occluding;
    }

    @Override
    public char getBlockID(int x, int y, int z) {
        ChunkData.packLocalChecked(x, y, z);
        return blockID;
    }

    @Override
    public BlockChunkData setBlockID(int x, int y, int z, char newBlockID, BlockInfoResolver blockInfoResolver) {
        ChunkData.packLocalChecked(x, y, z);
        if (newBlockID == this.blockID) {
            return this; // Chunk data did not change, no need to resize
        }
        return TwoBlockChunkData.upgradeFromSingleBlock(this, newBlockID, x, y, z, blockInfoResolver);
    }

    @Override
    public boolean isOccludingLocal(int x, int y, int z) {
        ChunkData.packLocalChecked(x, y, z);
        return occluding;
    }
}
