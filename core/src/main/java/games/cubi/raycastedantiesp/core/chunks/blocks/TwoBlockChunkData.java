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

import static games.cubi.raycastedantiesp.core.chunks.ChunkData.packLocalChecked;
import static games.cubi.raycastedantiesp.core.chunks.ChunkData.packUncheckedGuarded;
import static games.cubi.raycastedantiesp.core.utils.VarHandler.LONG_ARRAY_HANDLE;

//functionally a bitset
public class TwoBlockChunkData implements BlockChunkData {
    // Plain reads/writes would probably be fine as occluding state being stale for a few seconds causes no issues, and tearing is a non-issue as this is a bitset, so only a single bit value matters at a time.
    // However, plain reads can technically be cached indefinitely, so opaque reads are more correct.

    private final char trueBitBlock;
    private final char falseBitBlock;
    private final boolean trueBitBlockIsOccluding;
    private final boolean falseBitBlockIsOccluding;
    private final long[] blockData;

    TwoBlockChunkData(char trueBitBlock, char falseBitBlock, boolean trueBitBlockIsOccluding, boolean falseBitBlockIsOccluding, long[] blockData) {
        this.trueBitBlock = trueBitBlock;
        this.falseBitBlock = falseBitBlock;
        this.trueBitBlockIsOccluding = trueBitBlockIsOccluding;
        this.falseBitBlockIsOccluding = falseBitBlockIsOccluding;
        if (blockData.length != ChunkData.WORD_COUNT) {
            throw new IllegalArgumentException("blockData must have length " + ChunkData.WORD_COUNT + ", but has length " + blockData.length);
        }
        this.blockData = Arrays.copyOf(blockData, blockData.length);
    }

    static TwoBlockChunkData upgradeFromSingleBlock(SingleBlockChunkData old, char newBlockID, int newX, int newY, int newZ, BlockInfoResolver blockInfoResolver) {
        long[] blockData = new long[ChunkData.WORD_COUNT];
        TwoBlockChunkData upgraded = new TwoBlockChunkData(newBlockID, old.blockID, blockInfoResolver.isOccluding(newBlockID), blockInfoResolver.isOccluding(old.blockID), blockData);
        upgraded.setBit(packLocalChecked(newX, newY, newZ), true);
        return upgraded;
    }

    public static TwoBlockChunkData upgradeFromAir(char newBlockID, int newXGlobal, int newYGlobal, int newZGlobal, BlockInfoResolver blockInfoResolver) {
        long[] blockData = new long[ChunkData.WORD_COUNT];
        TwoBlockChunkData upgraded = new TwoBlockChunkData(newBlockID, (char) 0, blockInfoResolver.isOccluding(newBlockID), blockInfoResolver.isOccluding(0), blockData);
        upgraded.setBit(packUncheckedGuarded(newXGlobal, newYGlobal, newZGlobal), true);
        return upgraded;
    }

    @Override
    public char getBlockID(int x, int y, int z) {
        return bitIsSet(packLocalChecked(x, y, z)) ? trueBitBlock : falseBitBlock;
    }

    @Override
    public boolean isOccludingLocal(int x, int y, int z) {
        return bitIsSet(packLocalChecked(x, y, z)) ? trueBitBlockIsOccluding : falseBitBlockIsOccluding;
    }

    @Override
    public BlockChunkData setBlockID(int x, int y, int z, char blockID, BlockInfoResolver blockInfoResolver) {
        int packed = packLocalChecked(x, y, z);
        if (blockID == trueBitBlock) {
            setBit(packed, true);
            return this;
        }
        if (blockID == falseBitBlock) {
            setBit(packed, false);
            return this;
        }
        return FourBlockChunkData.upgradeFromTwoBlock(this).setBlockID(x, y, z, blockID, blockInfoResolver);
    }

    char trueBitBlock() {
        return trueBitBlock;
    }

    char falseBitBlock() {
        return falseBitBlock;
    }

    long[] copyOcclusionData() {
        long[] copied = new long[ChunkData.WORD_COUNT];
        if (trueBitBlockIsOccluding == falseBitBlockIsOccluding) {
            if (trueBitBlockIsOccluding) {
                Arrays.fill(copied, -1L);
            }
            return copied;
        }

        for (int wordIndex = 0; wordIndex < blockData.length; wordIndex++) {
            long word = (long) LONG_ARRAY_HANDLE.getOpaque(blockData, wordIndex);
            copied[wordIndex] = trueBitBlockIsOccluding ? word : ~word;
        }
        return copied;
    }

    boolean bitIsSet(int packed) {
        // >>> 6 divides the packed block index by 64 to select the containing long.
        int wordIndex = packed >>> 6;
        // 1L << packed creates a mask with only the packed block's bit set.
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(blockData, wordIndex);
        // & keeps only the target bit; non-zero means that bit is set.
        return (word & mask) != 0;
    }

    private void setBit(int packed, boolean value) {
        // >>> 6 divides the packed block index by 64 to select the containing long. Avoids signed division; probably an unnecessary optimisation but why not
        int wordIndex = packed >>> 6;
        // 1L << packed creates a mask for only the packed block's bit.
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(blockData, wordIndex);
        long updated = value ? word | mask : word & ~mask;
        LONG_ARRAY_HANDLE.setOpaque(blockData, wordIndex, updated);
    }
}
