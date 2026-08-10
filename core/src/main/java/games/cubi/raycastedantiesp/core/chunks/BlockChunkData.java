/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

import games.cubi.raycastedantiesp.core.chunks.blocks.BlockChunkDataBuilder;
import games.cubi.raycastedantiesp.core.chunks.blocks.SingleBlockChunkData;

import java.util.function.IntUnaryOperator;

public interface BlockChunkData extends ChunkOcclusionView {
    /**
     * char is used to denote an unsigned 16-bit integer.
     */
    char getBlockID(int x, int y, int z);

    /**
     * char is used to denote an unsigned 16-bit integer.
     */
    BlockChunkData setBlockID(int x, int y, int z, char blockID, BlockInfoResolver blockInfoResolver);

    static BlockChunkData filled(char blockID, BlockInfoResolver blockInfoResolver) {
        return new SingleBlockChunkData(blockID, blockInfoResolver.isOccluding(blockID));
    }

    /**
     * Builds chunk data from a source palette. The index reader receives core-packed positions and returns an index into {@code sourcePalette}.
     * Unused source entries are discarded before choosing the smallest representation.
     */
    static BlockChunkData copyOfPalette(char[] sourcePalette, IntUnaryOperator paletteIndexAtPacked, BlockInfoResolver blockInfoResolver) {
        return BlockChunkDataBuilder.copyOfPalette(sourcePalette, paletteIndexAtPacked, blockInfoResolver);
    }

    /**
     * Builds chunk data from a block-state reader. The reader receives core-packed positions.
     */
    static BlockChunkData copyOfStates(IntUnaryOperator blockStateAtPacked, BlockInfoResolver blockInfoResolver) {
        return BlockChunkDataBuilder.copyOfStates(blockStateAtPacked, blockInfoResolver);
    }
}
