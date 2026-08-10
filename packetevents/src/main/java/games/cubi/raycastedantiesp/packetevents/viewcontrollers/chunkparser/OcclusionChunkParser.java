/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser;

import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.GlobalPalette;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.chunks.ChunkData;
import games.cubi.raycastedantiesp.core.chunks.OccludingChunkData;
import games.cubi.raycastedantiesp.core.chunks.OccludingChunkDataImpl;
import games.cubi.raycastedantiesp.core.view.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.IntUnaryOperator;

public class OcclusionChunkParser extends AbstractChunkParser<OccludingChunkData> {
    public OcclusionChunkParser(BlockInfoResolver blockInfoResolver, IntUnaryOperator hiddenBlockID) {
        this(blockInfoResolver, true, hiddenBlockID);
    }

    protected OcclusionChunkParser(BlockInfoResolver blockInfoResolver, boolean mutatePackets, IntUnaryOperator hiddenBlockID) {
        super(blockInfoResolver, mutatePackets, hiddenBlockID);
    }

    @Override
    protected @Nullable OccludingChunkData parseSection(Chunk_v1_18 section) {
        if (section.isEmpty()) {
            return null;
        }
        var palette = section.getChunkData().palette;
        if (!(palette instanceof GlobalPalette)) {
            boolean any = false;
            boolean all = true;
            for (int index = 0; index < palette.size(); index++) {
                boolean occluding = blockInfoResolver.isOccluding(palette.idToState(index));
                any |= occluding;
                all &= occluding;
            }
            if (!any) {
                return null;
            }
            if (all) {
                return OccludingChunkData.solid();
            }
        }

        OccludingChunkDataImpl data = OccludingChunkData.empty();
        int occludingCount = 0;
        for (int localY = 0; localY < ChunkData.CHUNK_SIZE; localY++) {
            for (int localZ = 0; localZ < ChunkData.CHUNK_SIZE; localZ++) {
                for (int localX = 0; localX < ChunkData.CHUNK_SIZE; localX++) {
                    if (blockInfoResolver.isOccluding(section.getBlockId(localX, localY, localZ))) {
                        data.setOccluding(localX, localY, localZ, true);
                        occludingCount++;
                    }
                }
            }
        }
        if (occludingCount == 0) {
            return null;
        }
        return occludingCount == ChunkData.BLOCK_COUNT ? OccludingChunkData.solid() : data;
    }

    @Override
    protected void storeSection(BlockView blockView, UUID world, int chunkX, int sectionY, int chunkZ, OccludingChunkData data) {
        blockView.replaceChunkSectionOcclusion(world, chunkX, sectionY, chunkZ, data);
    }
}
