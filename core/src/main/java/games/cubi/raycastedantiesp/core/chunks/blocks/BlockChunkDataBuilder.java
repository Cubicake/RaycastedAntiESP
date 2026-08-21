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

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

public final class BlockChunkDataBuilder {
    private static final int BYTE_PALETTE_LIMIT = 256;

    private BlockChunkDataBuilder() {
    }

    public static BlockChunkData copyOfPalette(char[] sourcePalette, IntUnaryOperator sourceIndexAtPacked, BlockInfoResolver blockInfoResolver) {
        if (sourcePalette.length == 0 || sourcePalette.length > BYTE_PALETTE_LIMIT) {
            throw new IllegalArgumentException("sourcePalette must contain between 1 and " + BYTE_PALETTE_LIMIT + " entries");
        }

        // First compact only the source entries actually referenced by the 4,096 blocks.
        int[] remap = new int[sourcePalette.length];
        Arrays.fill(remap, -1);
        char[] compactPalette = new char[sourcePalette.length];
        int compactSize = 0;
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            int sourceIndex = checkedPaletteIndex(sourceIndexAtPacked.applyAsInt(packed), sourcePalette.length);
            if (remap[sourceIndex] == -1) {
                remap[sourceIndex] = compactSize;
                compactPalette[compactSize++] = sourcePalette[sourceIndex];
            }
        }

        final int usedPaletteSize = compactSize;
        // Readers are intentionally invoked again below; rescanning avoids a 4,096-entry temporary index array.
        IntUnaryOperator compactIndexAtPacked = packed -> {
            int sourceIndex = checkedPaletteIndex(sourceIndexAtPacked.applyAsInt(packed), sourcePalette.length);
            return remap[sourceIndex];
        };
        char[] usedPalette = Arrays.copyOf(compactPalette, usedPaletteSize);
        if (usedPaletteSize == 1) {
            return new SingleBlockChunkData(usedPalette[0], blockInfoResolver.isOccluding(usedPalette[0]));
        }
        if (usedPaletteSize == 2) {
            long[] bitData = new long[ChunkData.WORD_COUNT];
            for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
                if (compactIndexAtPacked.applyAsInt(packed) == 1) {
                    setPackedBit(bitData, packed);
                }
            }
            return new TwoBlockChunkData(usedPalette[1], usedPalette[0], blockInfoResolver.isOccluding(usedPalette[1]), blockInfoResolver.isOccluding(usedPalette[0]), bitData);
        }
        if (usedPaletteSize <= 4) {
            return new FourBlockChunkData(usedPalette, usedPaletteSize, compactIndexAtPacked, blockInfoResolver);
        }
        if (usedPaletteSize <= 16) {
            return new SixteenBlockChunkData(usedPalette, usedPaletteSize, compactIndexAtPacked, blockInfoResolver);
        }
        return new ByteArrayBlockChunkData(usedPalette, usedPaletteSize, compactIndexAtPacked, blockInfoResolver);
    }

    public static BlockChunkData copyOfStates(IntUnaryOperator blockStateAtPacked, BlockInfoResolver blockInfoResolver) {
        char[] palette = new char[BYTE_PALETTE_LIMIT];
        int paletteSize = 0;
        boolean overflow = false;
        // Global palettes have no local index table. Discover whether a byte palette is possible without storing 4,096 indices.
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            char blockID = checkedBlockID(blockStateAtPacked.applyAsInt(packed));
            if (!overflow && indexOf(palette, paletteSize, blockID) < 0) {
                if (paletteSize == BYTE_PALETTE_LIMIT) {
                    overflow = true;
                } else {
                    palette[paletteSize++] = blockID;
                }
            }
        }

        if (!overflow) {
            // Delegate to the compact palette path; the state reader is rescanned to keep temporary allocation bounded by palette size.
            final int size = paletteSize;
            char[] sourcePalette = Arrays.copyOf(palette, size);
            return copyOfPalette(sourcePalette, packed -> indexOf(sourcePalette, size, checkedBlockID(blockStateAtPacked.applyAsInt(packed))), blockInfoResolver);
        }

        // More than 256 states require final direct storage, populated together with its occlusion mask.
        char[] blockData = new char[ChunkData.BLOCK_COUNT];
        long[] occlusionData = new long[ChunkData.WORD_COUNT];
        for (int packed = 0; packed < ChunkData.BLOCK_COUNT; packed++) {
            char blockID = checkedBlockID(blockStateAtPacked.applyAsInt(packed));
            blockData[packed] = blockID;
            if (blockInfoResolver.isOccluding(blockID)) {
                setPackedBit(occlusionData, packed);
            }
        }
        return new CharArrayBlockChunkData(blockData, occlusionData, true);
    }

    private static int indexOf(char[] palette, int paletteSize, char blockID) {
        for (int index = 0; index < paletteSize; index++) {
            if (palette[index] == blockID) {
                return index;
            }
        }
        return -1;
    }

    private static int checkedPaletteIndex(int paletteIndex, int paletteSize) {
        if (paletteIndex < 0 || paletteIndex >= paletteSize) {
            throw new IllegalArgumentException("palette index must be in [0, " + (paletteSize - 1) + "], but was " + paletteIndex);
        }
        return paletteIndex;
    }

    private static char checkedBlockID(int blockID) {
        if (blockID < 0 || blockID > Character.MAX_VALUE) {
            throw new IllegalArgumentException("blockID must fit in an unsigned 16-bit value, but was " + blockID);
        }
        return (char) blockID;
    }

    private static void setPackedBit(long[] data, int packed) {
        // packed >>> 6 selects the 64-bit word; Java masks the long shift distance to packed & 63 for the bit.
        data[packed >>> 6] |= 1L << packed;
    }
}
