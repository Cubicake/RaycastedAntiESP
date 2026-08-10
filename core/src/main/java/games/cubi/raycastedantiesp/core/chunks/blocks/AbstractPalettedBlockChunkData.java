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

import static games.cubi.raycastedantiesp.core.utils.VarHandler.CHAR_ARRAY_HANDLE;
import static games.cubi.raycastedantiesp.core.utils.VarHandler.LONG_ARRAY_HANDLE;

abstract class AbstractPalettedBlockChunkData implements BlockChunkData {
    // Each concrete palette store allocates its maximum palette size up front. Adding a block only fills another slot in this array.
    // When the palette is full, the chunk upgrades to the next implementation.
    protected final char[] palette;
    protected final long[] occlusionData;
    private int paletteSize; //netty reads only

    protected AbstractPalettedBlockChunkData(char blockID, int paletteCapacity, BlockInfoResolver blockInfoResolver) {
        if (paletteCapacity < 1 || paletteCapacity > 256) {
            throw new IllegalArgumentException("paletteCapacity must be in [1, 256]");
        }
        this.palette = new char[paletteCapacity];
        this.occlusionData = new long[ChunkData.WORD_COUNT];
        setPaletteBlock(0, blockID);
        paletteSize = 1;
        if (blockInfoResolver.isOccluding(blockID)) {
            Arrays.fill(this.occlusionData, -1L);
        }
    }

    protected AbstractPalettedBlockChunkData(char[] palette, int paletteSize, int paletteCapacity, long[] occlusionData) {
        if (paletteCapacity < 1 || paletteCapacity > 256) {
            throw new IllegalArgumentException("paletteCapacity must be in [1, 256]");
        }
        if (paletteSize < 1 || paletteSize > paletteCapacity || palette.length < paletteSize) {
            throw new IllegalArgumentException("paletteSize must be in [1, paletteCapacity]");
        }
        if (occlusionData.length != ChunkData.WORD_COUNT) {
            throw new IllegalArgumentException("occlusionData must contain " + ChunkData.WORD_COUNT + " longs");
        }
        this.palette = new char[paletteCapacity];
        for (int i = 0; i < paletteSize; i++) {
            setPaletteBlock(i, palette[i]);
        }
        this.occlusionData = Arrays.copyOf(occlusionData, occlusionData.length);
        this.paletteSize = paletteSize;
    }

    protected AbstractPalettedBlockChunkData(AbstractPalettedBlockChunkData source, int paletteCapacity) {
        this(source.palette, source.paletteSize, paletteCapacity, source.occlusionData);
    }

    @Override
    public final char getBlockID(int x, int y, int z) {
        return getBlockIDPackedUnchecked(ChunkData.packLocalChecked(x, y, z));
    }

    protected final char getBlockIDPacked(int packed) {
        ChunkData.checkPacked(packed);
        return paletteBlock(getPaletteIndex(packed));
    }

    private char getBlockIDPackedUnchecked(int packed) {
        return paletteBlock(getPaletteIndex(packed));
    }

    @Override
    public final boolean isOccludingLocal(int x, int y, int z) {
        int packed = ChunkData.packLocalChecked(x, y, z);
        return isOccludingPacked(packed);
    }

    @Override
    public final BlockChunkData setBlockID(int x, int y, int z, char blockID, BlockInfoResolver blockInfoResolver) {
        int packed = ChunkData.packLocalChecked(x, y, z);
        int paletteIndex = indexOf(blockID);
        if (paletteIndex >= 0) {
            setPaletteIndex(packed, paletteIndex);
            setOccludingPacked(packed, blockInfoResolver.isOccluding(blockID));
            return this;
        }

        if (!canAddPaletteEntry()) {
            return upgrade().setBlockID(x, y, z, blockID, blockInfoResolver);
        }

        paletteIndex = addPaletteEntry(blockID);
        setPaletteIndex(packed, paletteIndex);
        setOccludingPacked(packed, blockInfoResolver.isOccluding(blockID));
        return this;
    }

    protected abstract int getPaletteIndex(int packed);

    protected abstract void setPaletteIndex(int packed, int paletteIndex);

    protected abstract BlockChunkData upgrade();

    private int indexOf(char blockID) {
        int size = paletteSize;
        for (int i = 0; i < size; i++) {
            if (paletteBlock(i) == blockID) {
                return i;
            }
        }
        return -1;
    }

    private boolean canAddPaletteEntry() {
        return paletteSize < palette.length;
    }

    private int addPaletteEntry(char blockID) {
        int size = paletteSize;
        setPaletteBlock(size, blockID);
        paletteSize = size + 1;
        return size;
    }

    protected final boolean isOccludingPacked(int packed) {
        ChunkData.checkPacked(packed);
        int wordIndex = packed >>> 6;
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(occlusionData, wordIndex);
        return (word & mask) != 0;
    }

    protected final void setOccludingPacked(int packed, boolean occluding) {
        ChunkData.checkPacked(packed);
        int wordIndex = packed >>> 6;
        long mask = 1L << packed;
        long word = (long) LONG_ARRAY_HANDLE.getOpaque(occlusionData, wordIndex);
        long updated = occluding ? word | mask : word & ~mask;
        LONG_ARRAY_HANDLE.setOpaque(occlusionData, wordIndex, updated);
    }

    protected final char paletteBlock(int index) {
        return (char) CHAR_ARRAY_HANDLE.getAcquire(palette, index);
    }

    protected final void setPaletteBlock(int index, char blockID) {
        CHAR_ARRAY_HANDLE.setRelease(palette, index, blockID);
    }
}
