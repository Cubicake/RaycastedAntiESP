/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view.chunks;

import ca.spottedleaf.concurrentutil.map.SWMRLong2ObjectHashTable;
import games.cubi.raycastedantiesp.core.chunks.BlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.chunks.ChunkData;
import games.cubi.raycastedantiesp.core.chunks.ChunkOcclusionView;
import games.cubi.raycastedantiesp.core.chunks.blocks.TwoBlockChunkData;

import static games.cubi.raycastedantiesp.core.view.chunks.ChunkSectionStore.packChunkCoords;
import static games.cubi.raycastedantiesp.core.view.chunks.ChunkSectionStore.packGlobalCoords;

public final class BlockChunkSectionStore extends SWMRLong2ObjectHashTable<BlockChunkData> implements ChunkSectionStore {
    private final BlockInfoResolver blockInfoResolver;

    public BlockChunkSectionStore(BlockInfoResolver blockInfoResolver) {
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
        BlockChunkData chunk = get(key);
        return chunk != null && chunk.isOccludingLocal(x & ChunkData.LOCAL_MASK, y & ChunkData.LOCAL_MASK, z & ChunkData.LOCAL_MASK);
    }

    /**
     * This method is not thread-safe and should only be called from the netty thread.
     */
    @Override
    public void setBlockID(int x, int y, int z, int blockID) {
        long key = packGlobalCoords(x, y, z);
        char storedBlockID = toStoredBlockID(blockID);
        BlockChunkData existing = get(key);
        if (existing == null) { // this means the chunk is currently air (or something's gone horribly wrong)
            if (storedBlockID == 0) {
                return;
            }
            TwoBlockChunkData newChunk = TwoBlockChunkData.upgradeFromAir(storedBlockID, x, y, z, blockInfoResolver);
            put(key, newChunk);
            return;
        }

        BlockChunkData updated = existing.setBlockID(x & ChunkData.LOCAL_MASK, y & ChunkData.LOCAL_MASK, z & ChunkData.LOCAL_MASK, storedBlockID, blockInfoResolver);
        if (updated != existing) {
            put(key, updated);
        }
    }

    @Override
    public void replaceSection(int chunkX, int sectionY, int chunkZ, BlockChunkData data) {
        put(packChunkCoords(chunkX, sectionY, chunkZ), data);
    }

    @Override
    public void replaceSectionOcclusion(int chunkX, int sectionY, int chunkZ, games.cubi.raycastedantiesp.core.chunks.OccludingChunkData data) {
        throw new UnsupportedOperationException("Block chunk storage requires full block IDs for section replacement");
    }

    @Override
    public void removeSection(int chunkX, int sectionY, int chunkZ) {
        remove(packChunkCoords(chunkX, sectionY, chunkZ));
    }

    @Override
    public void removeColumn(int chunkX, int chunkZ) {
        // Start at the raw 8-bit Y value 0. Incrementing visits 0..255, which represents signed section Y values 0..127 then -128..-1.
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

    private static char toStoredBlockID(int blockID) {
        if (blockID < 0 || blockID > Character.MAX_VALUE) {
            throw new IllegalArgumentException("blockID must fit in an unsigned 16-bit value, but was " + blockID);
        }
        return (char) blockID;
    }

}
