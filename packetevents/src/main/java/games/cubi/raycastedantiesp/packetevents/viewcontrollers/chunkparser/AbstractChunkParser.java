/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.GlobalPalette;
import games.cubi.locatables.implementations.MutableBlockSpatialImpl;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.chunks.ChunkData;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsTileEntityReplayData;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

abstract class AbstractChunkParser<D> implements ChunkParser {
    private static final TileEntity[] EMPTY_TILE_ENTITIES = new TileEntity[0];

    protected final BlockInfoResolver blockInfoResolver;
    private final boolean mutatePackets;
    private final IntUnaryOperator hiddenBlockID;
    private final boolean modernHeightmaps;
    private final ClientVersion clientVersion;

    protected AbstractChunkParser(BlockInfoResolver blockInfoResolver, boolean mutatePackets, IntUnaryOperator hiddenBlockID) {
        this.blockInfoResolver = blockInfoResolver;
        this.mutatePackets = mutatePackets;
        this.hiddenBlockID = hiddenBlockID;
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        this.modernHeightmaps = version.isNewerThanOrEquals(ServerVersion.V_1_21_5);
        this.clientVersion = version.toClientVersion();
    }

    protected abstract @Nullable D parseSection(Chunk_v1_18 section);

    protected abstract void storeSection(BlockView blockView, UUID world, int chunkX, int sectionY, int chunkZ, D data);

    @Override
    public final @Nullable Column parse(BlockView blockView, UUID world, Column column, int minimumSectionY) {
        BaseChunk[] sections = column.getChunks();
        long[][] managedBySection = null;
        MutableBlockSpatialImpl key = null;
        int chunkX = column.getX();
        int chunkZ = column.getZ();
        // Shifting the chunk coordinate left by four reserves the low four bits for the local block coordinate.
        int blockOriginX = chunkX << 4;
        int blockOriginZ = chunkZ << 4;
        boolean mutatedBlock = false;

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            Chunk_v1_18 section = (Chunk_v1_18) sections[sectionIndex];
            int sectionY = minimumSectionY + sectionIndex;
            D data = parseSection(section);
            if (data == null) {
                blockView.removeChunkSection(world, chunkX, sectionY, chunkZ);
            } else {
                storeSection(blockView, world, chunkX, sectionY, chunkZ, data);
            }

            if (!sectionMayContainManagedTiles(section)) {
                continue;
            }
            long[] managed = null;
            for (int localY = 0; localY < ChunkData.CHUNK_SIZE; localY++) {
                for (int localZ = 0; localZ < ChunkData.CHUNK_SIZE; localZ++) {
                    for (int localX = 0; localX < ChunkData.CHUNK_SIZE; localX++) {
                        char blockID = (char) section.getBlockId(localX, localY, localZ);
                        if (!blockInfoResolver.isTileEntity(blockID)) {
                            continue;
                        }
                        if (managed == null) {
                            managed = new long[ChunkData.WORD_COUNT];
                            if (managedBySection == null) {
                                managedBySection = new long[sections.length][];
                            }
                            managedBySection[sectionIndex] = managed;
                        }
                        int packed = ChunkData.packUncheckedGuarded(localX, localY, localZ);
                        // >>> 6 selects the containing 64-bit word; Java masks the shift distance to the bit within that word.
                        managed[packed >>> 6] |= 1L << packed;
                        int blockX = blockOriginX + localX;
                        int blockY = (sectionY << 4) + localY;
                        int blockZ = blockOriginZ + localZ;
                        if (key == null) {
                            key = new MutableBlockSpatialImpl(0, 0, 0);
                        }
                        key.setBlockPosition(blockX, blockY, blockZ);
                        TrackedTileEntity<?> state = blockView.updateOrInsertTileEntity(world, key, blockID, !mutatePackets);
                        if (!mutatePackets) {
                            blockView.recordOutboundTileEntityVisibility(state, true);
                        } else if (state != null && !state.visible()) {
                            section.set(localX, localY, localZ, hiddenBlockID.applyAsInt(blockY));
                            mutatedBlock = true;
                        }
                    }
                }
            }
        }

        blockView.pruneTileEntitiesAbsentFromChunkSections(world, chunkX, chunkZ, minimumSectionY, sections.length, managedBySection);
        TileEntity[] sourceTileEntities = column.getTileEntities();
        TileEntity[] filtered = sourceTileEntities;
        boolean stripped = false;
        int retainedCount = 0;
        TileEntity[] retained = null;
        if (sourceTileEntities.length != 0 && key == null) {
            key = new MutableBlockSpatialImpl(0, 0, 0);
        }
        for (int index = 0; index < sourceTileEntities.length; index++) {
            TileEntity tileEntity = sourceTileEntities[index];
            boolean strip = processTileEntity(blockView, world, blockOriginX, blockOriginZ, sections, minimumSectionY, managedBySection, tileEntity, key);
            if (mutatePackets && strip) {
                if (!stripped) {
                    stripped = true;
                    if (index > 0) {
                        // At least one entry is gone, so sourceLength - 1 is the largest final array we can need.
                        retained = new TileEntity[sourceTileEntities.length - 1];
                        System.arraycopy(sourceTileEntities, 0, retained, 0, index);
                        retainedCount = index;
                    }
                }
            } else if (stripped) {
                if (retained == null) {
                    retained = new TileEntity[sourceTileEntities.length - 1];
                }
                retained[retainedCount++] = tileEntity;
            }
        }
        if (stripped) {
            filtered = retained == null
                    ? EMPTY_TILE_ENTITIES
                    : retainedCount == retained.length ? retained : Arrays.copyOf(retained, retainedCount);
        }

        return mutatePackets && (mutatedBlock || stripped) ? copyColumn(column, filtered) : null;
    }

    private boolean sectionMayContainManagedTiles(Chunk_v1_18 section) {
        var palette = section.getChunkData().palette;
        if (!(palette instanceof GlobalPalette)) {
            for (int index = 0; index < palette.size(); index++) {
                if (blockInfoResolver.isTileEntity(palette.idToState(index))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean processTileEntity(BlockView blockView, UUID world, int blockOriginX, int blockOriginZ, BaseChunk[] sections, int minimumSectionY,
                                      long[][] managedBySection, TileEntity tileEntity, MutableBlockSpatialImpl key) {
        int packedXZ = Byte.toUnsignedInt(tileEntity.getPackedByte());
        // Modern block entities store local x in the high nibble and local z in the low nibble.
        int localX = packedXZ >>> 4;
        int localZ = packedXZ & ChunkData.LOCAL_MASK;
        int blockX = blockOriginX + localX;
        int blockY = tileEntity.getYShort();
        int blockZ = blockOriginZ + localZ;
        // Arithmetic shifting divides by 16 with the required floor behavior for negative block Y coordinates.
        int sectionIndex = (blockY >> 4) - minimumSectionY;
        key.setBlockPosition(blockX, blockY, blockZ);
        if (sectionIndex >= 0 && sectionIndex < sections.length) {
            long[] managed = managedBySection == null ? null : managedBySection[sectionIndex];
            int packed = ChunkData.packUncheckedGuarded(localX, blockY, localZ);
            if (managed != null && (managed[packed >>> 6] & 1L << packed) != 0L) {
                TrackedTileEntity<PacketEventsTileEntityReplayData> state = tracked(blockView, world, key);
                if (state != null) {
                    replayData(state).setBlockEntityData(BlockEntityTypes.getById(clientVersion, tileEntity.getType()), tileEntity.getNBT());
                    return !state.visible();
                }
                Logger.warning("Managed chunk block entity was missing from tracked state at " + blockX + "," + blockY + "," + blockZ, 3, AbstractChunkParser.class);
                return true;
            }
        }

        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            Logger.warning("Received chunk block entity outside authoritative section data at " + blockX + "," + blockY + "," + blockZ, 3, AbstractChunkParser.class);
            return true;
        }
        int blockID = sections[sectionIndex].getBlockId(localX, blockY & ChunkData.LOCAL_MASK, localZ);
        if (blockInfoResolver.hasBlockEntityData(blockID) && !blockInfoResolver.isTileEntity(blockID)) {
            return false;
        }
        Logger.warning("Received invalid or uncached chunk block entity at " + blockX + "," + blockY + "," + blockZ, 3, AbstractChunkParser.class);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static TrackedTileEntity<PacketEventsTileEntityReplayData> tracked(BlockView blockView, UUID world, MutableBlockSpatialImpl position) {
        return (TrackedTileEntity<PacketEventsTileEntityReplayData>) blockView.getTrackedTileEntity(world, position);
    }

    private static PacketEventsTileEntityReplayData replayData(TrackedTileEntity<PacketEventsTileEntityReplayData> tileEntity) {
        PacketEventsTileEntityReplayData replayData = tileEntity.extraData();
        if (replayData == null) {
            replayData = new PacketEventsTileEntityReplayData();
            tileEntity.setExtraData(replayData);
        }
        return replayData;
    }

    @SuppressWarnings("deprecation")
    private Column copyColumn(Column column, TileEntity[] tileEntities) {
        if (modernHeightmaps) {
            return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getHeightmaps());
        }
        return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), tileEntities, column.getHeightMaps());
    }
}
