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

public class FourBlockChunkData extends AbstractPalettedBlockChunkData {
    private static final int ENTRIES_PER_BYTE = 4;
    private static final int BITS_PER_ENTRY = 2;
    private static final int PALETTE_MASK = 0b11;

    private final byte[] blockData = new byte[ChunkData.BLOCK_COUNT / ENTRIES_PER_BYTE];

    public FourBlockChunkData(char blockID, BlockInfoResolver blockInfoResolver) {
        super(blockID, 4, blockInfoResolver);
    }

    FourBlockChunkData(char[] palette, int paletteSize, int[] indices, long[] occlusionData) {
        super(palette, paletteSize, 4, occlusionData);
        if (indices.length != ChunkData.BLOCK_COUNT) {
            throw new IllegalArgumentException("indices must contain " + ChunkData.BLOCK_COUNT + " entries");
        }
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            setPaletteIndex(packed, indices[packed]);
        }
    }

    FourBlockChunkData(char[] palette, int paletteSize, IntUnaryOperator indexAtPacked, BlockInfoResolver blockInfoResolver) {
        super(palette, paletteSize, 4, new long[ChunkData.WORD_COUNT]);
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            int paletteIndex = indexAtPacked.applyAsInt(packed);
            setPaletteIndex(packed, paletteIndex);
            setOccludingPacked(packed, blockInfoResolver.isOccluding(palette[paletteIndex]));
        }
    }

    static FourBlockChunkData upgradeFromTwoBlock(TwoBlockChunkData old) {
        int[] indices = new int[ChunkData.BLOCK_COUNT];
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            indices[packed] = old.bitIsSet(packed) ? 1 : 0;
        }
        return new FourBlockChunkData(new char[]{old.falseBitBlock(), old.trueBitBlock()}, 2, indices, old.copyOcclusionData());
    }

    @Override
    protected int getPaletteIndex(int packed) {
        // >>> 2 divides the packed block index by 4 to select the byte containing this block's 2-bit palette index.
        int byteIndex = packed >>> 2;
        // packed & 0b11 gets the block's position inside that byte, then * 2 converts it to a bit offset.
        int shift = (packed & 0b11) * BITS_PER_ENTRY;
        int packedByte = Byte.toUnsignedInt((byte) BYTE_ARRAY_HANDLE.getOpaque(blockData, byteIndex));
        // >>> moves the target 2-bit index to the bottom, and & keeps only those 2 bits.
        return (packedByte >>> shift) & PALETTE_MASK;
    }

    @Override
    protected void setPaletteIndex(int packed, int paletteIndex) {
        // >>> 2 divides the packed block index by 4 to select the byte containing this block's 2-bit palette index.
        int byteIndex = packed >>> 2;
        // packed & 0b11 gets the block's position inside that byte, then * 2 converts it to a bit offset.
        int shift = (packed & 0b11) * BITS_PER_ENTRY;
        // PALETTE_MASK << shift creates 1s over the old 2-bit index; ~ flips that to 0s over only the target index.
        int clearMask = ~(PALETTE_MASK << shift);
        int packedByte = Byte.toUnsignedInt((byte) BYTE_ARRAY_HANDLE.getOpaque(blockData, byteIndex));
        // & clearMask clears the old index, then | writes the new 2-bit palette index into the cleared slot.
        int updated = (packedByte & clearMask) | ((paletteIndex & PALETTE_MASK) << shift);
        BYTE_ARRAY_HANDLE.setOpaque(blockData, byteIndex, (byte) updated);
    }

    @Override
    protected BlockChunkData upgrade() {
        return SixteenBlockChunkData.upgradeFromFourBlock(this);
    }
}
