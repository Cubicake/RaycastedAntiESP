/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view.chunks;

import games.cubi.raycastedantiesp.core.chunks.BlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.ChunkOcclusionView;
import games.cubi.raycastedantiesp.core.chunks.OccludingChunkData;

public interface ChunkSectionStore {
    /**
     * Coordinates are chunk coordinates
     */
    ChunkOcclusionView getSection(int chunkX, int chunkY, int chunkZ);

    boolean isOccluding(int x, int y, int z);

    void setBlockID(int x, int y, int z, int blockID);

    void replaceSection(int chunkX, int sectionY, int chunkZ, BlockChunkData data);

    void replaceSectionOcclusion(int chunkX, int sectionY, int chunkZ, OccludingChunkData data);

    void removeSection(int chunkX, int sectionY, int chunkZ);

    void removeColumn(int chunkX, int chunkZ);

    int loadedSectionCount();

    void clear();

    static final int HORIZONTAL_BITS = 28;
    static final int SECTION_Y_BITS = 8;
    static final int HORIZONTAL_MIN = -(1 << (HORIZONTAL_BITS - 1));
    static final int HORIZONTAL_MAX = (1 << (HORIZONTAL_BITS - 1)) - 1;
    static final int SECTION_Y_MIN = -(1 << (SECTION_Y_BITS - 1));
    static final int SECTION_Y_MAX = (1 << (SECTION_Y_BITS - 1)) - 1;
    static final int SECTION_Y_COUNT = 1 << SECTION_Y_BITS;
    static final long SECTION_Y_INCREMENT = 1L << HORIZONTAL_BITS;
    static final long HORIZONTAL_MASK = (1L << HORIZONTAL_BITS) - 1L;
    static final long SECTION_Y_MASK = (1L << SECTION_Y_BITS) - 1L;

    static long packChunkCoords(int chunkX, int sectionY, int chunkZ) {
        checkHorizontal(chunkX, "chunkX");
        checkSectionY(sectionY);
        checkHorizontal(chunkZ, "chunkZ");
        return ((long) chunkX & HORIZONTAL_MASK) << (SECTION_Y_BITS + HORIZONTAL_BITS)
                | ((long) sectionY & SECTION_Y_MASK) << HORIZONTAL_BITS
                | ((long) chunkZ & HORIZONTAL_MASK);
    }

    static long packGlobalCoords(int x, int y, int z) {
        return packChunkCoords(x >> 4, y >> 4, z >> 4);
    }

    static int unpackChunkX(long key) {
        return (int) (key >> (SECTION_Y_BITS + HORIZONTAL_BITS));
    }

    static int unpackSectionY(long key) {
        return (int) (key << HORIZONTAL_BITS >> (HORIZONTAL_BITS + HORIZONTAL_BITS));
    }

    static int unpackChunkZ(long key) {
        return (int) (key << (Long.SIZE - HORIZONTAL_BITS) >> (Long.SIZE - HORIZONTAL_BITS));
    }

    private static void checkHorizontal(int value, String name) {
        if (value < HORIZONTAL_MIN || value > HORIZONTAL_MAX) {
            throw new IllegalArgumentException(name + " must be in [" + HORIZONTAL_MIN + ", " + HORIZONTAL_MAX + "], but was " + value);
        }
    }

    private static void checkSectionY(int sectionY) {
        if (sectionY < SECTION_Y_MIN || sectionY > SECTION_Y_MAX) {
            throw new IllegalArgumentException("sectionY must be in [" + SECTION_Y_MIN + ", " + SECTION_Y_MAX + "], but was " + sectionY);
        }
    }
}
