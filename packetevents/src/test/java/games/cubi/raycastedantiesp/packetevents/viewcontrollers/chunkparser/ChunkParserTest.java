/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.HeightmapType;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.locatables.implementations.ImmutableBlockSpatialImpl;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.packetevents.view.PacketEventsBlockView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkParserTest {
    private static final IntSupplier STABLE_WORLD_EPOCH = () -> 2;
    private static final TestPacketEventsAPI PACKET_EVENTS_API = new TestPacketEventsAPI();
    private static final BlockInfoResolver RESOLVER = new BlockInfoResolver() {
        @Override
        public boolean isOccluding(int blockStateID) {
            return blockStateID == 1 || blockStateID == 2;
        }

        @Override
        public boolean isTileEntity(int blockStateID) {
            return blockStateID == 99;
        }

        @Override
        public boolean hasBlockEntityData(int blockStateID) {
            return blockStateID == 99 || blockStateID == 100;
        }
    };

    @BeforeAll
    static void installPacketEventsAPI() {
        PacketEvents.setAPI(PACKET_EVENTS_API);
    }

    @BeforeEach
    void resetServerVersion() {
        PACKET_EVENTS_API.serverVersion = ServerVersion.V_1_21_5;
    }

    @Test
    void nonMutatingBlockParserTracksTilesWithoutChangingPacket() {
        UUID world = UUID.randomUUID();
        Chunk_v1_18 section = airSection();
        section.set(3, 2, 1, 99);
        TileEntity tileEntity = new TileEntity((byte) (3 << 4 | 1), (short) 2, 0, null);
        Column column = new Column(0, 0, true, new BaseChunk[]{section}, new TileEntity[]{tileEntity});
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);

        Column replacement = new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(view, world, column, 0);

        assertNull(replacement);
        assertEquals(99, section.getBlockId(3, 2, 1));
        var tracked = view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(3, 2, 1));
        assertNotNull(tracked);
        assertTrue(tracked.visible());
        assertNotNull(tracked.extraData());
    }

    @Test
    void mutatingBlockParserHidesNewTilesAndReturnsReplacement() {
        UUID world = UUID.randomUUID();
        Chunk_v1_18 section = airSection();
        section.set(3, 2, 1, 99);
        TileEntity tileEntity = new TileEntity((byte) (3 << 4 | 1), (short) 2, 0, null);
        Column column = new Column(0, 0, true, new BaseChunk[]{section}, new TileEntity[]{tileEntity});
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        view.applyTileEntityCheckMode(true, 0, unused -> {});

        Column replacement = new BlockChunkParser(RESOLVER, ignored -> 1).parse(view, world, column, 0);

        assertNotNull(replacement);
        assertEquals(1, section.getBlockId(3, 2, 1));
        assertEquals(0, replacement.getTileEntities().length);
        assertFalse(view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(3, 2, 1)).visible());
    }

    @Test
    void packetEventsAndCorePackingKeepYAndZDistinct() {
        UUID world = UUID.randomUUID();
        Chunk_v1_18 section = airSection();
        section.set(3, 2, 1, 1);
        section.set(3, 1, 2, 0);
        Column column = new Column(0, 0, true, new BaseChunk[]{section}, new TileEntity[0]);
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);

        new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(view, world, column, 0);

        assertTrue(view.isBlockOccluding(new ImmutableBlockLocatable(world, 3, 2, 1)));
        assertFalse(view.isBlockOccluding(new ImmutableBlockLocatable(world, 3, 1, 2)));
    }

    @Test
    void bothOcclusionParserModesPopulateOcclusionWithoutUnnecessaryMutation() {
        UUID world = UUID.randomUUID();
        Chunk_v1_18 nonMutatingSection = airSection();
        nonMutatingSection.set(1, 2, 3, 1);
        PacketEventsBlockView nonMutatingView = new PacketEventsBlockView(RESOLVER, false, STABLE_WORLD_EPOCH);
        Column nonMutatingColumn = new Column(0, 0, true, new BaseChunk[]{nonMutatingSection}, new TileEntity[0]);

        assertNull(new NonMutatingOcclusionChunkParser(RESOLVER, ignored -> 2).parse(nonMutatingView, world, nonMutatingColumn, 0));
        assertTrue(nonMutatingView.isBlockOccluding(new ImmutableBlockLocatable(world, 1, 2, 3)));
        assertEquals(1, nonMutatingSection.getBlockId(1, 2, 3));

        Chunk_v1_18 mutatingSection = airSection();
        mutatingSection.set(1, 2, 3, 1);
        PacketEventsBlockView mutatingView = new PacketEventsBlockView(RESOLVER, false, STABLE_WORLD_EPOCH);
        Column mutatingColumn = new Column(0, 0, true, new BaseChunk[]{mutatingSection}, new TileEntity[0]);
        assertNull(new OcclusionChunkParser(RESOLVER, ignored -> 2).parse(mutatingView, world, mutatingColumn, 0));
        assertTrue(mutatingView.isBlockOccluding(new ImmutableBlockLocatable(world, 1, 2, 3)));
    }

    @Test
    void mutatingReingestionKeepsExistingVisibleTileVisible() {
        UUID world = UUID.randomUUID();
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        Chunk_v1_18 initial = airSection();
        initial.set(3, 2, 1, 99);
        TileEntity tile = new TileEntity((byte) (3 << 4 | 1), (short) 2, 0, null);
        Column initialColumn = new Column(0, 0, true, new BaseChunk[]{initial}, new TileEntity[]{tile});
        new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(view, world, initialColumn, 0);

        view.applyTileEntityCheckMode(true, 1, unused -> {});
        Chunk_v1_18 resend = airSection();
        resend.set(3, 2, 1, 99);
        Column resendColumn = new Column(0, 0, true, new BaseChunk[]{resend}, new TileEntity[]{tile});
        Column replacement = new BlockChunkParser(RESOLVER, ignored -> 1).parse(view, world, resendColumn, 0);

        assertNull(replacement);
        assertEquals(99, resend.getBlockId(3, 2, 1));
        assertTrue(view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(3, 2, 1)).visible());
    }

    @Test
    void authoritativeEmptySectionRemovesPreviouslyTrackedTile() {
        UUID world = UUID.randomUUID();
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        Chunk_v1_18 lower = new Chunk_v1_18();
        Chunk_v1_18 upper = airSection();
        upper.set(3, 1, 2, 99);
        TileEntity tile = new TileEntity((byte) (3 << 4 | 2), (short) 17, 0, null);
        new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(
                view, world, new Column(0, 0, true, new BaseChunk[]{lower, upper}, new TileEntity[]{tile}), 0
        );
        ImmutableBlockSpatialImpl location = new ImmutableBlockSpatialImpl(3, 17, 2);
        assertNotNull(view.getTrackedTileEntity(world, location));

        new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(
                view, world, new Column(0, 0, true, new BaseChunk[]{new Chunk_v1_18(), new Chunk_v1_18()}, new TileEntity[0]), 0
        );

        assertNull(view.getTrackedTileEntity(world, location));
        assertFalse(view.isBlockOccluding(new ImmutableBlockLocatable(world, location.blockX(), location.blockY(), location.blockZ())));
    }

    @Test
    void nonMutatingParserRecordsExistingHiddenTileAsOutboundVisibleWithoutChangingColumn() {
        UUID world = UUID.randomUUID();
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        ImmutableBlockSpatialImpl location = new ImmutableBlockSpatialImpl(3, 2, 1);
        TrackedTileEntity<?> tracked = view.updateOrInsertTileEntity(world, location, (char) 99, false);
        tracked.setLastChecked(42);
        Chunk_v1_18 section = airSection();
        section.set(3, 2, 1, 99);
        TileEntity[] tileEntities = {
                new TileEntity((byte) (3 << 4 | 1), (short) 2, 0, null),
                new TileEntity((byte) (4 << 4 | 1), (short) 2, 0, null)
        };
        Column column = new Column(0, 0, true, new BaseChunk[]{section}, tileEntities);
        BaseChunk[] originalSections = column.getChunks();

        Column replacement = new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(view, world, column, 0);

        assertNull(replacement);
        assertSame(originalSections, column.getChunks());
        assertSame(section, column.getChunks()[0]);
        assertSame(tileEntities, column.getTileEntities());
        assertTrue(tracked.visible());
        assertEquals(TrackedTileEntity.NEVER_CHECKED, tracked.lastChecked());
        assertEquals(99, section.getBlockId(3, 2, 1));
        assertNull(view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(4, 2, 1)));
    }

    @Test
    void bothOcclusionParserModesTrackAndApplyTileVisibility() {
        UUID world = UUID.randomUUID();
        TileEntity tile = new TileEntity((byte) (3 << 4 | 1), (short) 2, 0, null);

        Chunk_v1_18 nonMutatingSection = airSection();
        nonMutatingSection.set(3, 2, 1, 99);
        PacketEventsBlockView nonMutatingView = new PacketEventsBlockView(RESOLVER, false, STABLE_WORLD_EPOCH);
        Column nonMutatingReplacement = new NonMutatingOcclusionChunkParser(RESOLVER, ignored -> 1).parse(
                nonMutatingView, world, new Column(0, 0, true, new BaseChunk[]{nonMutatingSection}, new TileEntity[]{tile}), 0
        );
        assertNull(nonMutatingReplacement);
        assertTrue(nonMutatingView.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(3, 2, 1)).visible());

        Chunk_v1_18 mutatingSection = airSection();
        mutatingSection.set(3, 2, 1, 99);
        PacketEventsBlockView mutatingView = new PacketEventsBlockView(RESOLVER, false, STABLE_WORLD_EPOCH);
        mutatingView.applyTileEntityCheckMode(true, 0, unused -> {});
        Column mutatingReplacement = new OcclusionChunkParser(RESOLVER, ignored -> 1).parse(
                mutatingView, world, new Column(0, 0, true, new BaseChunk[]{mutatingSection}, new TileEntity[]{tile}), 0
        );
        assertNotNull(mutatingReplacement);
        assertEquals(1, mutatingSection.getBlockId(3, 2, 1));
        assertFalse(mutatingView.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(3, 2, 1)).visible());
    }

    @Test
    void modernPackedTileCoordinatesSupportNegativeChunksAndY() {
        UUID world = UUID.randomUUID();
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        Chunk_v1_18 section = airSection();
        section.set(15, 15, 14, 99);
        TileEntity tile = new TileEntity((byte) (15 << 4 | 14), (short) -17, 0, null);

        new NonMutatingBlockChunkParser(RESOLVER, ignored -> 1).parse(
                view, world, new Column(-2, -3, true, new BaseChunk[]{section}, new TileEntity[]{tile}), -2
        );

        var tracked = view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(-17, -17, -34));
        assertNotNull(tracked);
        assertNotNull(tracked.extraData());
    }

    @Test
    void mutatingParserCompactsManagedAndInvalidEntriesWhileRetainingUnmanagedData() {
        UUID world = UUID.randomUUID();
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        Chunk_v1_18 section = airSection();
        section.set(1, 2, 1, 99);
        section.set(2, 2, 2, 100);
        TileEntity managed = new TileEntity((byte) (1 << 4 | 1), (short) 2, 0, new NBTCompound());
        TileEntity unmanaged = new TileEntity((byte) (2 << 4 | 2), (short) 2, 0, new NBTCompound());
        TileEntity invalid = new TileEntity((byte) (3 << 4 | 3), (short) 2, 0, new NBTCompound());
        assertEquals(0, section.getBlockId(3, 2, 3));

        Column replacement = new BlockChunkParser(RESOLVER, ignored -> 1).parse(
                view, world, new Column(0, 0, true, new BaseChunk[]{section}, new TileEntity[]{managed, unmanaged, invalid}), 0
        );

        assertNotNull(replacement);
        assertEquals(1, replacement.getTileEntities().length);
        assertSame(unmanaged, replacement.getTileEntities()[0]);
        assertEquals(0, section.getBlockId(3, 2, 3));
        assertNotNull(view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(1, 2, 1)).extraData());
        assertNull(view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(2, 2, 2)));
        assertNull(view.getTrackedTileEntity(world, new ImmutableBlockSpatialImpl(3, 2, 3)));
    }

    @Test
    void columnReplacementPreservesBothSupportedHeightmapFormats() {
        UUID world = UUID.randomUUID();
        NBTCompound legacyHeightmaps = new NBTCompound();
        PACKET_EVENTS_API.serverVersion = ServerVersion.V_1_21_4;
        Column legacy = hiddenTileColumn(legacyHeightmaps);
        Column legacyReplacement = new BlockChunkParser(RESOLVER, ignored -> 1).parse(
                enabledView(), world, legacy, 0
        );
        assertNotNull(legacyReplacement);
        assertSame(legacyHeightmaps, legacyReplacement.getHeightMaps());

        Map<HeightmapType, long[]> modernHeightmaps = Map.of(HeightmapType.WORLD_SURFACE, new long[]{1L});
        PACKET_EVENTS_API.serverVersion = ServerVersion.V_1_21_5;
        Column modern = hiddenTileColumn(modernHeightmaps);
        Column modernReplacement = new BlockChunkParser(RESOLVER, ignored -> 1).parse(
                enabledView(), world, modern, 0
        );
        assertNotNull(modernReplacement);
        assertSame(modernHeightmaps, modernReplacement.getHeightmaps());
    }

    private static PacketEventsBlockView enabledView() {
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        return view;
    }

    private static Column hiddenTileColumn(NBTCompound heightmaps) {
        Chunk_v1_18 section = airSection();
        section.set(1, 2, 1, 99);
        TileEntity tile = new TileEntity((byte) (1 << 4 | 1), (short) 2, 0, null);
        return new Column(0, 0, true, new BaseChunk[]{section}, new TileEntity[]{tile}, heightmaps);
    }

    private static Column hiddenTileColumn(Map<HeightmapType, long[]> heightmaps) {
        Chunk_v1_18 section = airSection();
        section.set(1, 2, 1, 99);
        TileEntity tile = new TileEntity((byte) (1 << 4 | 1), (short) 2, 0, null);
        return new Column(0, 0, true, new BaseChunk[]{section}, new TileEntity[]{tile}, heightmaps);
    }

    private static Chunk_v1_18 airSection() {
        Chunk_v1_18 section = new Chunk_v1_18();
        // The empty test constructor has no palette entry, so seed palette index zero with air before setting blocks.
        section.set(0, 0, 0, 0);
        return section;
    }

    private static final class TestPacketEventsAPI extends PacketEventsAPI<Object> {
        private ServerVersion serverVersion = ServerVersion.V_1_21_5;
        private final ServerManager serverManager = () -> serverVersion;

        @Override public void load() {}
        @Override public boolean isLoaded() { return true; }
        @Override public void init() {}
        @Override public boolean isInitialized() { return true; }
        @Override public void terminate() {}
        @Override public boolean isTerminated() { return false; }
        @Override public Object getPlugin() { return this; }
        @Override public ServerManager getServerManager() { return serverManager; }
        @Override public com.github.retrooper.packetevents.manager.protocol.ProtocolManager getProtocolManager() { return null; }
        @Override public com.github.retrooper.packetevents.manager.player.PlayerManager getPlayerManager() { return null; }
        @Override public com.github.retrooper.packetevents.netty.NettyManager getNettyManager() { return null; }
        @Override public com.github.retrooper.packetevents.injector.ChannelInjector getInjector() { return null; }
    }
}
