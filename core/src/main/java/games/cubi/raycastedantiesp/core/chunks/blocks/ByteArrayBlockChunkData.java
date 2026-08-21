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
import java.util.function.IntUnaryOperator;

import static games.cubi.raycastedantiesp.core.utils.VarHandler.BYTE_ARRAY_HANDLE;

public class ByteArrayBlockChunkData extends AbstractPalettedBlockChunkData {
    private final byte[] blockData = new byte[ChunkData.BLOCK_COUNT];

    public ByteArrayBlockChunkData(char blockID, BlockInfoResolver blockInfoResolver) {
        super(blockID, 256, blockInfoResolver);
    }

    ByteArrayBlockChunkData(char[] palette, int paletteSize, IntUnaryOperator indexAtPacked, BlockInfoResolver blockInfoResolver) {
        super(palette, paletteSize, 256, new long[ChunkData.WORD_COUNT]);
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            int paletteIndex = indexAtPacked.applyAsInt(packed);
            setPaletteIndex(packed, paletteIndex);
            setOccludingPacked(packed, blockInfoResolver.isOccluding(palette[paletteIndex]));
        }
    }

    private ByteArrayBlockChunkData(SixteenBlockChunkData old) {
        super(old, 256);
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            setPaletteIndex(packed, old.getPaletteIndex(packed));
        }
    }

    static ByteArrayBlockChunkData upgradeFromSixteenBlock(SixteenBlockChunkData old) {
        return new ByteArrayBlockChunkData(old);
    }

    @Override
    protected int getPaletteIndex(int packed) {
        // Each block gets one byte, so the packed block index directly selects the byte containing its palette index.
        // Byte.toUnsignedInt converts Java's signed byte back to the intended 0-255 palette index.
        return Byte.toUnsignedInt((byte) BYTE_ARRAY_HANDLE.getOpaque(blockData, packed));
    }

    @Override
    protected void setPaletteIndex(int packed, int paletteIndex) {
        // Each block gets one byte, so the packed block index directly selects where to store the 0-255 palette index.
        BYTE_ARRAY_HANDLE.setOpaque(blockData, packed, (byte) paletteIndex);
    }

    @Override
    protected BlockChunkData upgrade() {
        return CharArrayBlockChunkData.upgradeFromByteArrayBlock(this);
    }
}
