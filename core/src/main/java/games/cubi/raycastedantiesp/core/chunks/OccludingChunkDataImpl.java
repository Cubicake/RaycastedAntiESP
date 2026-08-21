/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

import java.util.Arrays;

import static games.cubi.raycastedantiesp.core.utils.VarHandler.LONG_ARRAY_HANDLE;

/**
 * A 4096-bit bitset, where 1 indicates occluding and 0 indicates not occluding.
 * Safe for single-writer, multiple-reader setups
 */
public final class OccludingChunkDataImpl implements OccludingChunkData {
    // Plain reads/writes would probably be fine as occluding state being stale for a few seconds causes no issues, and tearing is a non-issue as this is a bitset, so only a single bit value matters at a time.
    // However, plain reads can technically be cached indefinitely, so opaque reads are more correct.

    private final long[] occlusionData;

    public OccludingChunkDataImpl() {
        this.occlusionData = new long[ChunkData.WORD_COUNT];
    }

    public OccludingChunkDataImpl(long[] occlusionData) {
        if (occlusionData.length != ChunkData.WORD_COUNT) {
            throw new IllegalArgumentException("occlusionData must contain " + ChunkData.WORD_COUNT + " longs");
        }
        this.occlusionData = Arrays.copyOf(occlusionData, occlusionData.length);
    }

    static OccludingChunkDataImpl solid() {
        // Make every bit 1, which is the same as making every long -1
        long[] solidData = new long[ChunkData.WORD_COUNT];
        Arrays.fill(solidData, -1L);
        return new OccludingChunkDataImpl(solidData);
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

    @Override
    public OccludingChunkData setOccluding(int x, int y, int z, boolean occluding) {
        int packed = ChunkData.packLocalChecked(x, y, z);
        // >>> 6 divides the packed block index by 64 to select the containing long. Avoids signed division; probably an unnecessary optimisation but why not
        int wordIndex = packed >>> 6;
        // 1L << packed creates a mask for only the packed block's bit.
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(occlusionData, wordIndex);
        // If occluding is true, OR the mask in to set the bit; otherwise AND with ~mask to clear it.
        // ~ flips all bits, so mask becomes all 1 except for the bit being set to 0. ANDing this means that anything in word which was already true stays true and already false stays false, except for the bit being set to false.
        long updated = occluding ? word | mask : word & ~mask;
        // Not an atomicity issue since writes are single-threaded
        LONG_ARRAY_HANDLE.setOpaque(occlusionData, wordIndex, updated);
        return this;
    }
}
