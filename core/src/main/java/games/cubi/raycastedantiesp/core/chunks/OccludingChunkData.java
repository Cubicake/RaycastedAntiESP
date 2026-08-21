/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

import org.jetbrains.annotations.Range;

import java.util.BitSet;

public sealed interface OccludingChunkData extends ChunkOcclusionView permits OccludingChunkData.Solid, OccludingChunkDataImpl {
    /**
     * The parameters here are local coordinates within the chunk, so they should be in the range [0, 15].
     * @return The object to be stored. Mutations may require upgrading the object to a more complex implementation, so the returned object may not be the same as the current one.
     */
    OccludingChunkData setOccluding(@Range(from = 0, to = 15) int x, @Range(from = 0, to = 15) int y, @Range(from = 0, to = 15) int z, boolean occluding);

    static OccludingChunkDataImpl empty() {
        return new OccludingChunkDataImpl();
    }

    static OccludingChunkData filled() {
        return OccludingChunkDataImpl.solid();
    }

    OccludingChunkData SOLID = new Solid();

    static OccludingChunkData solid() {
        return SOLID;
    }

    static OccludingChunkData copyOf(BitSet bitSet) {
        long[] words = bitSet.toLongArray();
        if (words.length != ChunkData.WORD_COUNT) {
            long[] resized = new long[ChunkData.WORD_COUNT];
            System.arraycopy(words, 0, resized, 0, Math.min(words.length, resized.length));
            words = resized;
        }
        return new OccludingChunkDataImpl(words);
    }

    final class Solid implements OccludingChunkData {
        @Override
        public boolean isOccludingLocal(int x, int y, int z) {
            return true;
        }

        @Override
        public OccludingChunkData setOccluding(int x, int y, int z, boolean occluding) {
            return filled().setOccluding(x, y, z, occluding);
        }

        @Override
        public boolean isSolid() {
            return true;
        }
    }
}
