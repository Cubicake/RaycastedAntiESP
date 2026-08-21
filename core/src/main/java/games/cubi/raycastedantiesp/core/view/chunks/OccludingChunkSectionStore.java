/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view.chunks;

import ca.spottedleaf.concurrentutil.map.SWMRLong2ObjectHashTable;
import games.cubi.raycastedantiesp.core.chunks.*;

import static games.cubi.raycastedantiesp.core.view.chunks.ChunkSectionStore.packChunkCoords;
import static games.cubi.raycastedantiesp.core.view.chunks.ChunkSectionStore.packGlobalCoords;

public final class OccludingChunkSectionStore extends SWMRLong2ObjectHashTable<OccludingChunkData> implements ChunkSectionStore {
    private final BlockInfoResolver blockInfoResolver;

    public OccludingChunkSectionStore(BlockInfoResolver blockInfoResolver) {
        this.blockInfoResolver = blockInfoResolver;
    }

    @Override
    public ChunkOcclusionView getSection(int chunkX, int chunkY, int chunkZ) {
        long key = packChunkCoords(chunkX, chunkY, chunkZ);
        return get(key);
    }

    @Override
    public boolean isOccluding(int x, int y, int z) {
        long key = packGlobalCoords(x, y, z);
        OccludingChunkData chunk = get(key);
        return chunk != null && chunk.isOccludingLocal(x & ChunkData.LOCAL_MASK, y & ChunkData.LOCAL_MASK, z & ChunkData.LOCAL_MASK);
    }

    @Override
    public void setBlockID(int x, int y, int z, int blockID) {
        long key = packGlobalCoords(x, y, z);
        boolean occluding = blockID != 0 && blockInfoResolver.isOccluding(blockID);
        OccludingChunkData existing = get(key);
        if (existing == null) {
            if (!occluding) {
                return;
            }
            existing = OccludingChunkData.empty();
            put(key, existing);
        }

        OccludingChunkData updated = existing.setOccluding(x & ChunkData.LOCAL_MASK, y & ChunkData.LOCAL_MASK, z & ChunkData.LOCAL_MASK, occluding);
        if (updated != existing) {
            put(key, updated);
        }
    }

    @Override
    public void replaceSection(int chunkX, int sectionY, int chunkZ, BlockChunkData data) {
        throw new UnsupportedOperationException("Occlusion chunk storage requires occlusion data for section replacement");
    }

    @Override
    public void replaceSectionOcclusion(int chunkX, int sectionY, int chunkZ, OccludingChunkData data) {
        put(packChunkCoords(chunkX, sectionY, chunkZ), data);
    }

    @Override
    public void removeSection(int chunkX, int sectionY, int chunkZ) {
        remove(packChunkCoords(chunkX, sectionY, chunkZ));
    }

    @Override
    public void removeColumn(int chunkX, int chunkZ) {
        // Start at raw Y 0 so the increment covers every possible 8-bit signed section coordinate exactly once.
        long key = packChunkCoords(chunkX, 0, chunkZ);
        for (int i = 0; i < SECTION_Y_COUNT; i++) {
            remove(key);
            key += SECTION_Y_INCREMENT;
        }
    }

    @Override
    public int loadedSectionCount() {
        return size();
    }

    @Override
    public void clear() {
        super.clear();
    }
}
