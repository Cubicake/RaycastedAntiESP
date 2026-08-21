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

public class SixteenBlockChunkData extends AbstractPalettedBlockChunkData {
    private static final int ENTRIES_PER_BYTE = 2;
    private static final int BITS_PER_ENTRY = 4;
    private static final int PALETTE_MASK = 0b1111;

    private final byte[] blockData = new byte[ChunkData.BLOCK_COUNT / ENTRIES_PER_BYTE];

    public SixteenBlockChunkData(char blockID, BlockInfoResolver blockInfoResolver) {
        super(blockID, 16, blockInfoResolver);
    }

    SixteenBlockChunkData(char[] palette, int paletteSize, IntUnaryOperator indexAtPacked, BlockInfoResolver blockInfoResolver) {
        super(palette, paletteSize, 16, new long[ChunkData.WORD_COUNT]);
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            int paletteIndex = indexAtPacked.applyAsInt(packed);
            setPaletteIndex(packed, paletteIndex);
            setOccludingPacked(packed, blockInfoResolver.isOccluding(palette[paletteIndex]));
        }
    }

    private SixteenBlockChunkData(FourBlockChunkData old) {
        super(old, 16);
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            setPaletteIndex(packed, old.getPaletteIndex(packed));
        }
    }

    static SixteenBlockChunkData upgradeFromFourBlock(FourBlockChunkData old) {
        return new SixteenBlockChunkData(old);
    }

    @Override
    protected int getPaletteIndex(int packed) {
        // >>> 1 divides the packed block index by 2 to select the byte containing this block's 4-bit palette index.
        int byteIndex = packed >>> 1;
        // packed & 1 selects the low or high nibble, then * 4 converts it to a bit offset.
        int shift = (packed & 1) * BITS_PER_ENTRY;
        int packedByte = Byte.toUnsignedInt((byte) BYTE_ARRAY_HANDLE.getOpaque(blockData, byteIndex));
        // >>> moves the target nibble to the bottom, and & keeps only those 4 bits.
        return (packedByte >>> shift) & PALETTE_MASK;
    }

    @Override
    protected void setPaletteIndex(int packed, int paletteIndex) {
        // >>> 1 divides the packed block index by 2 to select the byte containing this block's 4-bit palette index.
        int byteIndex = packed >>> 1;
        // packed & 1 selects the low or high nibble, then * 4 converts it to a bit offset.
        int shift = (packed & 1) * BITS_PER_ENTRY;
        // PALETTE_MASK << shift creates 1s over the old nibble; ~ flips that to 0s over only the target nibble.
        int clearMask = ~(PALETTE_MASK << shift);
        int packedByte = Byte.toUnsignedInt((byte) BYTE_ARRAY_HANDLE.getOpaque(blockData, byteIndex));
        // & clearMask clears the old nibble, then | writes the new 4-bit palette index into the cleared slot.
        int updated = (packedByte & clearMask) | ((paletteIndex & PALETTE_MASK) << shift);
        BYTE_ARRAY_HANDLE.setOpaque(blockData, byteIndex, (byte) updated);
    }

    @Override
    protected BlockChunkData upgrade() {
        return ByteArrayBlockChunkData.upgradeFromSixteenBlock(this);
    }
}
