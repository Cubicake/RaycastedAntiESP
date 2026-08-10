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
import java.util.Arrays;

import static games.cubi.raycastedantiesp.core.utils.VarHandler.CHAR_ARRAY_HANDLE;
import static games.cubi.raycastedantiesp.core.utils.VarHandler.LONG_ARRAY_HANDLE;

public class CharArrayBlockChunkData implements BlockChunkData {
    private final char[] blockData;
    private final long[] occlusionData;

    public CharArrayBlockChunkData(char blockID, BlockInfoResolver blockInfoResolver) {
        this.blockData = new char[ChunkData.BLOCK_COUNT];
        Arrays.fill(this.blockData, blockID);
        this.occlusionData = new long[ChunkData.WORD_COUNT];
        if (blockInfoResolver.isOccluding(blockID)) {
            Arrays.fill(this.occlusionData, -1L);
        }
    }

    CharArrayBlockChunkData(char[] blockData, long[] occlusionData) {
        this(blockData, occlusionData, false);
    }

    CharArrayBlockChunkData(char[] blockData, long[] occlusionData, boolean takeOwnership) {
        if (blockData.length != ChunkData.BLOCK_COUNT) {
            throw new IllegalArgumentException("blockData must contain " + ChunkData.BLOCK_COUNT + " entries");
        }
        if (occlusionData.length != ChunkData.WORD_COUNT) {
            throw new IllegalArgumentException("occlusionData must contain " + ChunkData.WORD_COUNT + " longs");
        }
        this.blockData = takeOwnership ? blockData : Arrays.copyOf(blockData, blockData.length);
        this.occlusionData = takeOwnership ? occlusionData : Arrays.copyOf(occlusionData, occlusionData.length);
    }

    static CharArrayBlockChunkData upgradeFromByteArrayBlock(ByteArrayBlockChunkData old) {
        char[] blockData = new char[ChunkData.BLOCK_COUNT];
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            blockData[packed] = old.getBlockIDPacked(packed);
        }
        return new CharArrayBlockChunkData(blockData, old.occlusionData);
    }

    @Override
    public char getBlockID(int x, int y, int z) {
        return (char) CHAR_ARRAY_HANDLE.getOpaque(blockData, ChunkData.packLocalChecked(x, y, z));
    }

    @Override
    public BlockChunkData setBlockID(int x, int y, int z, char blockID, BlockInfoResolver blockInfoResolver) {
        int packed = ChunkData.packLocalChecked(x, y, z);
        CHAR_ARRAY_HANDLE.setOpaque(blockData, packed, blockID);
        setOccludingPacked(packed, blockInfoResolver.isOccluding(blockID));
        return this;
    }

    @Override
    public boolean isOccludingLocal(int x, int y, int z) {
        int packed = ChunkData.packLocalChecked(x, y, z);
        // >>> 6 divides the packed block index by 64 to select the containing long.
        int wordIndex = packed >>> 6;
        // 1L << packed creates a mask with only the packed block's bit set.
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(occlusionData, wordIndex);
        // & keeps only the target bit; non-zero means that bit is set.
        return (word & mask) != 0;
    }

    private void setOccludingPacked(int packed, boolean occluding) {
        // >>> 6 divides the packed block index by 64 to select the containing long.
        int wordIndex = packed >>> 6;
        // 1L << packed creates a mask for only the packed block's bit.
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(occlusionData, wordIndex);
        // If occluding is true, OR the mask in to set the bit; otherwise AND with ~mask to clear it.
        long updated = occluding ? word | mask : word & ~mask;
        LONG_ARRAY_HANDLE.setOpaque(occlusionData, wordIndex, updated);
    }
}
