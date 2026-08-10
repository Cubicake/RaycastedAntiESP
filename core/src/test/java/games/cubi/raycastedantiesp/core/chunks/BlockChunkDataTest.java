/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

import games.cubi.raycastedantiesp.core.chunks.blocks.ByteArrayBlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.blocks.CharArrayBlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.blocks.FourBlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.blocks.SingleBlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.blocks.SixteenBlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.blocks.TwoBlockChunkData;
import org.junit.jupiter.api.Test;

import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockChunkDataTest {
    private static final BlockInfoResolver FIRST_255_OCCLUDING = resolver(blockStateID -> blockStateID >= 0 && blockStateID < 255);
    private static final BlockInfoResolver ALL_SMALL_OCCLUDING = resolver(blockStateID -> blockStateID >= 0 && blockStateID < 512);

    @Test
    void factorySelectsSmallestRepresentations() {
        assertInstanceOf(SingleBlockChunkData.class, fromStates(repeating(0), FIRST_255_OCCLUDING));
        assertInstanceOf(TwoBlockChunkData.class, fromStates(repeating(0, 1), FIRST_255_OCCLUDING));
        assertInstanceOf(FourBlockChunkData.class, fromStates(repeating(0, 1, 2, 3), FIRST_255_OCCLUDING));
        assertInstanceOf(SixteenBlockChunkData.class, fromStates(repeating(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), FIRST_255_OCCLUDING));
        assertInstanceOf(ByteArrayBlockChunkData.class, fromStates(repeating(uniqueWithNonOccludingTail(256)), FIRST_255_OCCLUDING));
        assertInstanceOf(CharArrayBlockChunkData.class, fromStates(repeating(uniqueWithNonOccludingTail(257)), FIRST_255_OCCLUDING));
    }

    @Test
    void upgradesPreserveBlockIdsAndOcclusion() {
        char[] expected = new char[ChunkData.BLOCK_COUNT];
        BlockChunkData data = BlockChunkData.filled((char) 0, FIRST_255_OCCLUDING);

        for (int packed = 1; packed <= 256; packed++) {
            data = setPacked(data, packed, (char) packed);
            expected[packed] = (char) packed;

            if (packed == 1) {
                assertInstanceOf(TwoBlockChunkData.class, data);
            } else if (packed == 2 || packed == 3) {
                assertInstanceOf(FourBlockChunkData.class, data);
            } else if (packed >= 4 && packed <= 15) {
                assertInstanceOf(SixteenBlockChunkData.class, data);
            } else if (packed >= 16 && packed <= 255) {
                assertInstanceOf(ByteArrayBlockChunkData.class, data);
            } else {
                assertInstanceOf(CharArrayBlockChunkData.class, data);
            }
        }

        assertMatches(expected, data, FIRST_255_OCCLUDING);
    }

    @Test
    void quarterByteWritesDoNotCorruptNeighboringCells() {
        BlockChunkData data = fromStates(repeating(0, 1, 2, 3), FIRST_255_OCCLUDING);
        assertInstanceOf(FourBlockChunkData.class, data);

        data = setPacked(data, 0, (char) 2);
        assertEquals(2, getPacked(data, 0));
        assertEquals(1, getPacked(data, 1));
        assertEquals(2, getPacked(data, 2));
        assertEquals(3, getPacked(data, 3));

        data = setPacked(data, 3, (char) 0);
        assertEquals(2, getPacked(data, 0));
        assertEquals(1, getPacked(data, 1));
        assertEquals(2, getPacked(data, 2));
        assertEquals(0, getPacked(data, 3));
    }

    @Test
    void halfByteWritesDoNotCorruptNeighboringCells() {
        BlockChunkData data = fromStates(repeating(0, 1, 2, 3, 4), FIRST_255_OCCLUDING);
        assertInstanceOf(SixteenBlockChunkData.class, data);

        data = setPacked(data, 0, (char) 4);
        assertEquals(4, getPacked(data, 0));
        assertEquals(1, getPacked(data, 1));

        data = setPacked(data, 1, (char) 3);
        assertEquals(4, getPacked(data, 0));
        assertEquals(3, getPacked(data, 1));
    }

    @Test
    void bytePaletteIndex255Works() {
        char[] blockIDs = new char[ChunkData.BLOCK_COUNT];
        for (int packed = 0; packed < 255; packed++) {
            blockIDs[packed] = (char) packed;
        }
        blockIDs[255] = 1000;

        BlockChunkData data = fromStates(blockIDs, FIRST_255_OCCLUDING);

        assertInstanceOf(ByteArrayBlockChunkData.class, data);
        assertEquals(254, getPacked(data, 254));
        assertEquals(1000, getPacked(data, 255));
        assertTrue(isOccludingPacked(data, 254));
        assertFalse(isOccludingPacked(data, 255));
    }

    @Test
    void allOccludingByteBoundaryStaysByteArray() {
        char[] blockIDs = new char[ChunkData.BLOCK_COUNT];
        for (int packed = 0; packed < 256; packed++) {
            blockIDs[packed] = (char) packed;
        }

        BlockChunkData data = fromStates(blockIDs, ALL_SMALL_OCCLUDING);

        assertInstanceOf(ByteArrayBlockChunkData.class, data);
        for (int packed = 0; packed < 256; packed++) {
            assertEquals(packed, getPacked(data, packed));
            assertTrue(isOccludingPacked(data, packed));
        }
    }

    @Test
    void palettedOcclusionDoesNotDependOnPaletteAppendOrder() {
        BlockInfoResolver oddOccluding = resolver(blockStateID -> (blockStateID & 1) == 1);
        char[] blockIDs = repeating(2, 1, 4, 3);

        BlockChunkData data = fromStates(blockIDs, oddOccluding);

        assertInstanceOf(FourBlockChunkData.class, data);
        assertMatches(blockIDs, data, oddOccluding);
    }

    @Test
    void invalidLocalCoordinatesThrow() {
        BlockChunkData single = BlockChunkData.filled((char) 0, FIRST_255_OCCLUDING);
        assertThrows(IllegalArgumentException.class, () -> single.getBlockID(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> single.setBlockID(0, 16, 0, (char) 0, FIRST_255_OCCLUDING));
        assertThrows(IllegalArgumentException.class, () -> single.isOccludingLocal(0, 0, 16));

        BlockChunkData paletted = fromStates(repeating(0, 1, 2, 3), FIRST_255_OCCLUDING);
        assertThrows(IllegalArgumentException.class, () -> paletted.getBlockID(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> paletted.setBlockID(0, 16, 0, (char) 2, FIRST_255_OCCLUDING));
        assertThrows(IllegalArgumentException.class, () -> paletted.isOccludingLocal(0, 0, 16));
    }

    @Test
    void paletteFactoryDiscardsUnusedEntriesAndUsesCorePackedPositions() {
        char[] sourcePalette = {(char) 0, (char) 7, (char) 8, (char) 9};
        int target = ChunkData.packUncheckedGuarded(3, 2, 1);
        BlockChunkData data = BlockChunkData.copyOfPalette(sourcePalette, packed -> packed == target ? 2 : 1, FIRST_255_OCCLUDING);

        assertInstanceOf(TwoBlockChunkData.class, data);
        assertEquals(8, data.getBlockID(3, 2, 1));
        assertEquals(7, data.getBlockID(1, 2, 3));
    }

    private static BlockInfoResolver resolver(IntPredicate occluding) {
        return new BlockInfoResolver() {
            @Override
            public boolean isOccluding(int blockStateID) {
                return occluding.test(blockStateID);
            }

            @Override
            public boolean isTileEntity(int blockStateID) {
                return false;
            }

            @Override
            public boolean hasBlockEntityData(int blockStateID) {
                return false;
            }
        };
    }

    private static BlockChunkData fromStates(char[] blockIDs, BlockInfoResolver blockInfoResolver) {
        return BlockChunkData.copyOfStates(packed -> blockIDs[packed], blockInfoResolver);
    }

    private static char[] repeating(int... blockIDs) {
        char[] converted = new char[blockIDs.length];
        for (int i = 0; i < blockIDs.length; i++) {
            converted[i] = (char) blockIDs[i];
        }
        return repeating(converted);
    }

    private static char[] repeating(char[] blockIDs) {
        char[] data = new char[ChunkData.BLOCK_COUNT];
        for (int packed = 0; packed < data.length; packed++) {
            data[packed] = blockIDs[packed % blockIDs.length];
        }
        return data;
    }

    private static char[] uniqueWithNonOccludingTail(int uniqueCount) {
        char[] blockIDs = new char[uniqueCount];
        int occludingCount = Math.min(uniqueCount, 255);
        for (int i = 0; i < occludingCount; i++) {
            blockIDs[i] = (char) i;
        }
        for (int i = occludingCount; i < uniqueCount; i++) {
            blockIDs[i] = (char) (1000 + i);
        }
        return blockIDs;
    }

    private static BlockChunkData setPacked(BlockChunkData data, int packed, char blockID) {
        return data.setBlockID(ChunkData.unpackX(packed), ChunkData.unpackY(packed), ChunkData.unpackZ(packed), blockID, FIRST_255_OCCLUDING);
    }

    private static char getPacked(BlockChunkData data, int packed) {
        return data.getBlockID(ChunkData.unpackX(packed), ChunkData.unpackY(packed), ChunkData.unpackZ(packed));
    }

    private static boolean isOccludingPacked(BlockChunkData data, int packed) {
        return data.isOccludingLocal(ChunkData.unpackX(packed), ChunkData.unpackY(packed), ChunkData.unpackZ(packed));
    }

    private static void assertMatches(char[] expected, BlockChunkData data, BlockInfoResolver blockInfoResolver) {
        for (int packed = 0; packed < expected.length; packed++) {
            int packedIndex = packed;
            char expectedBlockID = expected[packed];
            assertEquals(expectedBlockID, getPacked(data, packed), () -> "block ID mismatch at packed index " + packedIndex);
            assertEquals(blockInfoResolver.isOccluding(expectedBlockID), isOccludingPacked(data, packed), () -> "occlusion mismatch at packed index " + packedIndex);
        }
    }
}
