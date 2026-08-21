/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

public final class ChunkData {
    public static final int CHUNK_SIZE = 16;
    public static final int LOCAL_MASK = CHUNK_SIZE - 1;
    public static final int BLOCK_COUNT = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;
    public static final int WORD_COUNT = BLOCK_COUNT / Long.SIZE;

    private ChunkData() {
    }

    public static int packLocalChecked(int x, int y, int z) {
        checkLocalCoordinate(x, "x");
        checkLocalCoordinate(y, "y");
        checkLocalCoordinate(z, "z");
        return packUncheckedGuarded(x, y, z);
    }

    /**
     * Packs the local coordinates into a single integer without checking bounds. Coordinates outside the range will be correctly packed.
     */
    public static int packUncheckedGuarded(int x, int y, int z) {
        return (z & LOCAL_MASK) << 8 | (y & LOCAL_MASK) << 4 | (x & LOCAL_MASK);
    }

    public static int unpackX(int packed) {
        checkPacked(packed);
        return packed & LOCAL_MASK;
    }

    public static int unpackY(int packed) {
        checkPacked(packed);
        return packed >> 4 & LOCAL_MASK;
    }

    public static int unpackZ(int packed) {
        checkPacked(packed);
        return packed >> 8 & LOCAL_MASK;
    }

    public static void checkPacked(int packed) {
        if (packed < 0 || packed >= BLOCK_COUNT) {
            throw new IllegalArgumentException("packed local block index must be in [0, " + (BLOCK_COUNT - 1) + "], but was " + packed);
        }
    }

    private static void checkLocalCoordinate(int coordinate, String name) {
        if (coordinate < 0 || coordinate >= CHUNK_SIZE) {
            throw new IllegalArgumentException(name + " local coordinate must be in [0, " + LOCAL_MASK + "], but was " + coordinate);
        }
    }
}
