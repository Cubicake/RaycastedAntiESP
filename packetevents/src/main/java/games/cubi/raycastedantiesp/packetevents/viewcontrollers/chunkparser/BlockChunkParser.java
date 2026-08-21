/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser;

import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.GlobalPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.Palette;
import games.cubi.raycastedantiesp.core.chunks.BlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.chunks.ChunkData;
import games.cubi.raycastedantiesp.core.view.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.IntUnaryOperator;

public class BlockChunkParser extends AbstractChunkParser<BlockChunkData> {
    public BlockChunkParser(BlockInfoResolver blockInfoResolver, IntUnaryOperator hiddenBlockID) {
        this(blockInfoResolver, true, hiddenBlockID);
    }

    protected BlockChunkParser(BlockInfoResolver blockInfoResolver, boolean mutatePackets, IntUnaryOperator hiddenBlockID) {
        super(blockInfoResolver, mutatePackets, hiddenBlockID);
    }

    @Override
    protected @Nullable BlockChunkData parseSection(Chunk_v1_18 section) {
        if (section.isEmpty()) {
            return null;
        }
        DataPalette data = section.getChunkData();
        Palette palette = data.palette;
        if (!(palette instanceof GlobalPalette)) {
            int paletteSize = palette.size();
            char[] states = new char[paletteSize];
            for (int index = 0; index < paletteSize; index++) {
                states[index] = checkedBlockID(palette.idToState(index));
            }
            if (paletteSize == 1) {
                return states[0] == 0 ? null : BlockChunkData.filled(states[0], blockInfoResolver);
            }
            return BlockChunkData.copyOfPalette(states, packed -> data.storage.get(toPacketEventsIndex(packed)), blockInfoResolver);
        }
        return BlockChunkData.copyOfStates(packed -> {
            int x = ChunkData.unpackX(packed);
            int y = ChunkData.unpackY(packed);
            int z = ChunkData.unpackZ(packed);
            return section.getBlockId(x, y, z);
        }, blockInfoResolver);
    }

    @Override
    protected void storeSection(BlockView blockView, UUID world, int chunkX, int sectionY, int chunkZ, BlockChunkData data) {
        blockView.replaceChunkSection(world, chunkX, sectionY, chunkZ, data);
    }

    private static int toPacketEventsIndex(int packed) {
        int x = packed & ChunkData.LOCAL_MASK;
        int y = packed >> 4 & ChunkData.LOCAL_MASK;
        int z = packed >> 8 & ChunkData.LOCAL_MASK;
        // PacketEvents packs y in the high nibble group, z in the middle, and x in the low nibble.
        return y << 8 | z << 4 | x;
    }

    private static char checkedBlockID(int blockID) {
        if (blockID < 0 || blockID > Character.MAX_VALUE) {
            throw new IllegalArgumentException("blockID must fit in an unsigned 16-bit value, but was " + blockID);
        }
        return (char) blockID;
    }
}
