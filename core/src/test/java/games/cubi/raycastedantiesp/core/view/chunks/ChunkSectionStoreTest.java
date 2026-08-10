/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view.chunks;

import games.cubi.locatables.api.BlockLocatable;
import games.cubi.locatables.api.BlockSpatial;
import games.cubi.locatables.implementations.ImmutableBlockSpatialImpl;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.chunks.ChunkData;
import games.cubi.raycastedantiesp.core.chunks.BlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.OccludingChunkData;
import games.cubi.raycastedantiesp.core.chunks.OccludingChunkDataImpl;
import games.cubi.raycastedantiesp.core.chunks.blocks.CharArrayBlockChunkData;
import games.cubi.raycastedantiesp.core.tracked.NettyTileEntity;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.core.utils.Clearable;
import games.cubi.raycastedantiesp.core.view.AbstractBlockView;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.BlockViewTransition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSectionStoreTest {
    private static final IntSupplier STABLE_WORLD_EPOCH = () -> 2;
    private static final BlockInfoResolver ODD_OCCLUDING = new BlockInfoResolver() {
        @Override
        public boolean isOccluding(int blockStateID) {
            return (blockStateID & 1) == 1;
        }

        @Override
        public boolean isTileEntity(int blockStateID) {
            return blockStateID == 99;
        }

        @Override
        public boolean hasBlockEntityData(int blockStateID) {
            return isTileEntity(blockStateID);
        }
    };

    @Test
    void occlusionOnlyAndBlockStoresAgreeForIdenticalBlockIds() {
        char[] section = new char[ChunkData.BLOCK_COUNT];
        section[ChunkData.packUncheckedGuarded(0, 0, 0)] = 1;
        section[ChunkData.packUncheckedGuarded(15, 15, 15)] = 2;
        section[ChunkData.packUncheckedGuarded(3, 2, 1)] = 3;

        ChunkSectionStore occlusionOnly = new OccludingChunkSectionStore(ODD_OCCLUDING);
        ChunkSectionStore blockAware = new BlockChunkSectionStore(ODD_OCCLUDING);
        occlusionOnly.replaceSectionOcclusion(-2, -1, 3, occlusionOf(section));
        blockAware.replaceSection(-2, -1, 3, BlockChunkData.copyOfStates(packed -> section[packed], ODD_OCCLUDING));

        assertTrue(occlusionOnly.isOccluding((-2 << 4), (-1 << 4), (3 << 4)));
        assertTrue(blockAware.isOccluding((-2 << 4), (-1 << 4), (3 << 4)));
        assertFalse(occlusionOnly.isOccluding((-2 << 4) + 15, (-1 << 4) + 15, (3 << 4) + 15));
        assertFalse(blockAware.isOccluding((-2 << 4) + 15, (-1 << 4) + 15, (3 << 4) + 15));
        assertTrue(occlusionOnly.isOccluding((-2 << 4) + 3, (-1 << 4) + 2, (3 << 4) + 1));
        assertTrue(blockAware.isOccluding((-2 << 4) + 3, (-1 << 4) + 2, (3 << 4) + 1));
    }

    @Test
    void blockStoreStoresRepresentationUpgradesFromMutation() {
        BlockChunkSectionStore store = new BlockChunkSectionStore(ODD_OCCLUDING);
        long key = ChunkSectionStore.packChunkCoords(0, 0, 0);

        for (int packed = 1; packed <= 256; packed++) {
            store.setBlockID(ChunkData.unpackX(packed), ChunkData.unpackY(packed), ChunkData.unpackZ(packed), packed);
        }

        assertInstanceOf(CharArrayBlockChunkData.class, store.get(key));
        assertTrue(store.isOccluding(ChunkData.unpackX(255), ChunkData.unpackY(255), ChunkData.unpackZ(255)));
        assertFalse(store.isOccluding(ChunkData.unpackX(256), ChunkData.unpackY(256), ChunkData.unpackZ(256)));
    }

    @Test
    void storesReplaceRemoveColumnAndClearSignedSectionCoordinates() {
        ChunkSectionStore store = new BlockChunkSectionStore(ODD_OCCLUDING);
        char[] section = new char[ChunkData.BLOCK_COUNT];
        section[ChunkData.packUncheckedGuarded(1, 2, 3)] = 1;

        BlockChunkData data = BlockChunkData.copyOfStates(packed -> section[packed], ODD_OCCLUDING);
        store.replaceSection(-1, ChunkSectionStore.SECTION_Y_MIN, 3, data);
        store.replaceSection(-1, ChunkSectionStore.SECTION_Y_MAX, 3, data);
        store.replaceSection(-1, -2, 3, data);
        store.replaceSection(4, 5, -6, data);

        assertEquals(4, store.loadedSectionCount());
        assertTrue(store.isOccluding((-1 << 4) + 1, (-2 << 4) + 2, (3 << 4) + 3));
        assertTrue(store.isOccluding((4 << 4) + 1, (5 << 4) + 2, (-6 << 4) + 3));

        store.removeSection(-1, -2, 3);
        assertEquals(3, store.loadedSectionCount());
        assertFalse(store.isOccluding((-1 << 4) + 1, (-2 << 4) + 2, (3 << 4) + 3));

        store.removeColumn(-1, 3);
        assertEquals(1, store.loadedSectionCount());

        store.removeColumn(4, -6);
        assertEquals(0, store.loadedSectionCount());

        store.replaceSection(-1, -2, 3, data);
        store.clear();
        assertEquals(0, store.loadedSectionCount());
    }

    @Test
    void blockViewWorldSwitchClearsOldStateAndDifferentWorldReadsMiss() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, true);
        UUID worldA = UUID.randomUUID();
        UUID worldB = UUID.randomUUID();
        ImmutableBlockLocatable locationA = new ImmutableBlockLocatable(worldA, 1, 2, 3);

        view.applyTileEntityCheckMode(true, 0, unused -> {});
        view.upsertBlock(worldA, 1, 2, 3, 1);
        view.updateOrInsertTileEntity(locationA, 99, true);
        view.updateVisibilityForEachNeedingRecheck(0, 1, view.tileEntityCheckModeToken(), ignored -> BlockView.VisibilityResolver.HIDE);
        view.flushPendingTransitions();

        Assertions.assertTrue(view.isBlockOccluding(worldA, 1, 2, 3));
        Assertions.assertFalse(view.isBlockOccluding(worldB, 1, 2, 3));
        Assertions.assertTrue(view.hasPendingTransitions());

        view.removeChunk(worldB, 0, 0);
        view.removeChunkSection(worldB, 0, 0, 0);
        view.removeTileEntity(new ImmutableBlockLocatable(worldB, 1, 2, 3));

        Assertions.assertTrue(view.isBlockOccluding(worldA, 1, 2, 3));
        assertNotNull(view.getTrackedTileEntity(locationA));
        Assertions.assertTrue(view.hasPendingTransitions());

        view.upsertBlock(worldB, 17, 2, 3, 1);

        Assertions.assertFalse(view.isBlockOccluding(worldA, 1, 2, 3));
        assertNull(view.getTrackedTileEntity(locationA));
        Assertions.assertFalse(view.hasPendingTransitions());
        Assertions.assertTrue(view.isBlockOccluding(worldB, 17, 2, 3));
    }

    @Test
    void blockViewTileMapRepopulatesAfterClear() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID firstWorld = UUID.randomUUID();
        UUID secondWorld = UUID.randomUUID();
        ImmutableBlockLocatable first = new ImmutableBlockLocatable(firstWorld, 1, 64, 2);
        ImmutableBlockLocatable second = new ImmutableBlockLocatable(secondWorld, 17, 65, 18);

        view.updateOrInsertTileEntity(first, 99, true);
        assertNotNull(view.getTrackedTileEntity(first));

        view.clear();
        assertNull(view.getTrackedTileEntity(first));
        assertEquals(0, view.forEachNeedingRecheck(-1, 0, ignored -> {}));

        view.updateOrInsertTileEntity(second, 99, true);
        assertNotNull(view.getTrackedTileEntity(second));
        assertEquals(1, view.forEachNeedingRecheck(-1, 0, ignored -> {}));
    }

    @Test
    void occlusionOnlyReplacementStoresAndRemovesBitsetSections() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        long[] occlusionData = new long[ChunkData.WORD_COUNT];
        int packed = ChunkData.packUncheckedGuarded(1, 2, 3);
        occlusionData[packed >>> 6] |= 1L << packed;

        view.replaceChunkSectionOcclusion(world, -1, -2, 3, new OccludingChunkDataImpl(occlusionData));

        Assertions.assertEquals(1, view.loadedChunkCount());
        Assertions.assertTrue(view.isBlockOccluding(world, (-1 << 4) + 1, (-2 << 4) + 2, (3 << 4) + 3));
        Assertions.assertFalse(view.isBlockOccluding(world, (-1 << 4) + 2, (-2 << 4) + 2, (3 << 4) + 3));

        view.removeChunkSection(world, -1, -2, 3);

        Assertions.assertEquals(0, view.loadedChunkCount());
        Assertions.assertFalse(view.isBlockOccluding(world, (-1 << 4) + 1, (-2 << 4) + 2, (3 << 4) + 3));
    }

    @Test
    void blockStorageRejectsOcclusionOnlyReplacement() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, true);

        assertThrows(UnsupportedOperationException.class, () ->
                view.replaceChunkSectionOcclusion(UUID.randomUUID(), 0, 0, 0, OccludingChunkData.empty())
        );
    }

    @Test
    void wrappedColumnCollisionsRemainSemanticallyIndependent() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        ImmutableBlockLocatable first = new ImmutableBlockLocatable(world, 1, 64, 2);
        ImmutableBlockLocatable sameChunk = new ImmutableBlockLocatable(world, 2, 68, 3);
        ImmutableBlockLocatable collidingX = new ImmutableBlockLocatable(world, (65_536 << 4) + 1, 65, 2);
        ImmutableBlockLocatable collidingZ = new ImmutableBlockLocatable(world, 1, 66, (65_536 << 4) + 2);
        ImmutableBlockLocatable otherBucket = new ImmutableBlockLocatable(world, 17, 67, 2);

        view.updateOrInsertTileEntity(first, 99, true);
        view.updateOrInsertTileEntity(collidingX, 99, true);
        view.updateOrInsertTileEntity(sameChunk, 99, true);
        view.updateOrInsertTileEntity(collidingZ, 99, true);
        view.updateOrInsertTileEntity(otherBucket, 99, true);

        AtomicInteger visited = new AtomicInteger();
        assertEquals(5, view.forEachNeedingRecheck(-1, 0, ignored -> visited.incrementAndGet()));
        assertEquals(5, visited.get());

        assertNotNull(view.getTrackedTileEntity(first));
        assertNotNull(view.getTrackedTileEntity(sameChunk));
        assertNotNull(view.getTrackedTileEntity(collidingX));
        assertNotNull(view.getTrackedTileEntity(collidingZ));
        assertNotNull(view.getTrackedTileEntity(otherBucket));

        view.removeChunk(world, first.chunkX(), first.chunkZ());
        assertNull(view.getTrackedTileEntity(first));
        assertNull(view.getTrackedTileEntity(sameChunk));
        assertNotNull(view.getTrackedTileEntity(collidingX));
        assertNotNull(view.getTrackedTileEntity(collidingZ));

        view.removeTileEntity(collidingX);
        assertNull(view.getTrackedTileEntity(collidingX));
        assertNotNull(view.getTrackedTileEntity(collidingZ));
    }

    @Test
    void homogeneousChunkRemovalKeepsAcquiredChainTraversable() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        ImmutableBlockLocatable first = new ImmutableBlockLocatable(world, 1, 64, 2);
        ImmutableBlockLocatable second = new ImmutableBlockLocatable(world, 2, 65, 3);
        ImmutableBlockLocatable third = new ImmutableBlockLocatable(world, 3, 66, 4);

        TestTileEntity firstEntity = view.updateOrInsertTileEntity(first, 99, true);
        TestTileEntity secondEntity = view.updateOrInsertTileEntity(second, 99, true);
        TestTileEntity thirdEntity = view.updateOrInsertTileEntity(third, 99, true);
        TestTileEntity acquiredHead = view.getTrackedTileEntity(first);

        view.removeChunk(world, first.chunkX(), first.chunkZ());

        assertNull(view.getTrackedTileEntity(first));
        assertNull(view.getTrackedTileEntity(second));
        assertNull(view.getTrackedTileEntity(third));
        assertTrue(firstEntity.isRemoved());
        assertTrue(secondEntity.isRemoved());
        assertTrue(thirdEntity.isRemoved());
        assertEquals(0, view.forEachNeedingRecheck(-1, 0, ignored -> {}));
        int acquiredCount = 0;
        for (NettyTileEntity<TestExtraData> current = acquiredHead; current != null; current = current.nextAcquire()) {
            acquiredCount++;
        }
        assertEquals(3, acquiredCount);
    }

    @Test
    void disablingTileChecksConsumesVisibilityRepairs() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        ImmutableBlockSpatialImpl position = new ImmutableBlockSpatialImpl(1, 64, 2);
        ArrayList<TrackedTileEntity<?>> visibilityRepairs = new ArrayList<>();

        view.applyTileEntityCheckMode(true, 0, visibilityRepairs::add);
        assertTrue(visibilityRepairs.isEmpty());
        TestTileEntity tileEntity = view.updateOrInsertTileEntity(world, position, 99, false);
        view.applyTileEntityCheckMode(true, 1, visibilityRepairs::add);
        assertTrue(visibilityRepairs.isEmpty());

        view.applyTileEntityCheckMode(false, 1, visibilityRepairs::add);

        assertEquals(1, visibilityRepairs.size());
        assertSame(tileEntity, visibilityRepairs.getFirst());
        assertTrue(tileEntity.visible());
        assertFalse(view.hasPendingTransitions());
    }

    @Test
    void modeGenerationRejectsInFlightHideAndForcesFirstEnabledCheck() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        ImmutableBlockLocatable location = new ImmutableBlockLocatable(world, 1, 64, 2);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        view.updateOrInsertTileEntity(location, 99, true);
        long staleToken = view.tileEntityCheckModeToken();

        int processed = view.updateVisibilityForEachNeedingRecheck(-1, 1, staleToken, ignored -> {
            view.applyTileEntityCheckMode(false, 1, unused -> {});
            return BlockView.VisibilityResolver.HIDE;
        });

        assertEquals(1, processed);
        assertTrue(view.isVisible(location, 1));

        view.applyTileEntityCheckMode(true, 2, unused -> {});
        long enabledToken = view.tileEntityCheckModeToken();
        AtomicInteger checks = new AtomicInteger();
        view.updateVisibilityForEachNeedingRecheck(-1, 2, enabledToken, ignored -> {
            checks.incrementAndGet();
            return BlockView.VisibilityResolver.SHOW;
        });
        assertEquals(1, checks.get());
    }

    @Test
    void worldEpochRejectsVisibilityResultCapturedBeforeClear() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false, worldEpoch::getAcquire);
        UUID firstWorld = UUID.randomUUID();
        UUID secondWorld = UUID.randomUUID();
        ImmutableBlockSpatialImpl position = new ImmutableBlockSpatialImpl(1, 64, 2);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        TestTileEntity original = view.updateOrInsertTileEntity(firstWorld, position, 99, true);
        long modeToken = view.tileEntityCheckModeToken();
        int staleEpoch = worldEpoch.getAcquire();

        int processed = view.updateVisibilityForEachNeedingRecheck(-1, 1, modeToken, staleEpoch, ignored -> {
            worldEpoch.setRelease(3);
            view.clear();
            worldEpoch.setRelease(4);
            view.updateOrInsertTileEntity(secondWorld, position, 100, true);
            return BlockView.VisibilityResolver.HIDE;
        });

        assertEquals(1, processed);
        assertTrue(original.visible());
        assertTrue(view.getTrackedTileEntity(secondWorld, position).visible());
        assertTrue(worldEpoch.getAcquire() > staleEpoch);
        assertFalse(view.hasPendingTransitions());
    }

    @Test
    void transitionPublicationIncludesCommittedTileState() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
            UUID world = UUID.randomUUID();
            ImmutableBlockLocatable location = new ImmutableBlockLocatable(world, 1, 64, 2);
            view.applyTileEntityCheckMode(true, 0, unused -> {});
            TestTileEntity tileEntity = view.updateOrInsertTileEntity(location, 99, true);
            long modeToken = view.tileEntityCheckModeToken();
            AtomicInteger acknowledgedTick = new AtomicInteger();
            int transitions = 1_000;

            Thread producer = Thread.startVirtualThread(() -> {
                for (int tick = 1; tick <= transitions; tick++) {
                    byte visibility = (tick & 1) == 0 ? BlockView.VisibilityResolver.SHOW : BlockView.VisibilityResolver.HIDE;
                    view.updateVisibilityForEachNeedingRecheck(0, tick, modeToken, ignored -> visibility);
                    view.flushPendingTransitions();
                    while (acknowledgedTick.getAcquire() != tick) {
                        Thread.onSpinWait();
                    }
                }
            });

            AtomicReference<BlockViewTransition.Type> transitionType = new AtomicReference<>();
            AtomicReference<TrackedTileEntity<?>> transitionTileEntity = new AtomicReference<>();
            boolean[] drained = {false};
            for (int tick = 1; tick <= transitions; tick++) {
                do {
                    drained[0] = false;
                    view.drainTransitions((type, queuedTileEntity, modeTokenValue, worldEpoch) -> {
                        transitionType.set(type);
                        transitionTileEntity.set(queuedTileEntity);
                        drained[0] = true;
                    });
                } while (!drained[0]);

                boolean expectedVisible = (tick & 1) == 0;
                assertEquals(expectedVisible ? BlockViewTransition.Type.SHOW : BlockViewTransition.Type.HIDE, transitionType.get());
                assertSame(tileEntity, transitionTileEntity.get());
                assertEquals(expectedVisible, tileEntity.visible());
                assertEquals(tick, tileEntity.lastChecked());
                acknowledgedTick.setRelease(tick);
            }
            producer.join();
        });
    }

    @Test
    void observingReplacementWorldDataNeverExposesOldWorldState() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            TestBlockView view = new TestBlockView(ODD_OCCLUDING, true);
            UUID firstWorld = UUID.randomUUID();
            UUID secondWorld = UUID.randomUUID();
            view.upsertBlock(firstWorld, 1, 2, 3, 1);

            Thread writer = Thread.startVirtualThread(() -> view.upsertBlock(secondWorld, 17, 2, 3, 1));
            while (!view.isBlockOccluding(secondWorld, 17, 2, 3)) {
                Thread.onSpinWait();
            }

            assertFalse(view.isBlockOccluding(secondWorld, 1, 2, 3));
            assertFalse(view.isBlockOccluding(firstWorld, 1, 2, 3));
            writer.join();
        });
    }

    @Test
    void authoritativeSectionPruningRemovesAbsentAndOutOfRangeTilesWhilePreservingCollisions() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        ImmutableBlockLocatable retained = new ImmutableBlockLocatable(world, 1, 1, 1);
        ImmutableBlockLocatable removed = new ImmutableBlockLocatable(world, 2, 2, 2);
        ImmutableBlockLocatable emptySection = new ImmutableBlockLocatable(world, 3, 17, 3);
        ImmutableBlockLocatable outsideColumn = new ImmutableBlockLocatable(world, 4, 33, 4);
        ImmutableBlockLocatable collision = new ImmutableBlockLocatable(world, (65_536 << 4) + 1, 2, 1);
        view.updateOrInsertTileEntity(retained, 99, true);
        view.updateOrInsertTileEntity(removed, 99, true);
        view.updateOrInsertTileEntity(emptySection, 99, true);
        view.updateOrInsertTileEntity(outsideColumn, 99, true);
        view.updateOrInsertTileEntity(collision, 99, true);

        long[][] present = new long[2][];
        present[0] = new long[ChunkData.WORD_COUNT];
        int packed = ChunkData.packUncheckedGuarded(retained.blockX(), retained.blockY(), retained.blockZ());
        present[0][packed >>> 6] |= 1L << packed;
        view.pruneTileEntitiesAbsentFromChunkSections(world, 0, 0, 0, 2, present);

        assertNotNull(view.getTrackedTileEntity(retained));
        assertNull(view.getTrackedTileEntity(removed));
        assertNull(view.getTrackedTileEntity(emptySection));
        assertNull(view.getTrackedTileEntity(outsideColumn));
        assertNotNull(view.getTrackedTileEntity(collision));
    }

    @Test
    void nullSectionPresenceRemovesAllTilesInTheAuthoritativeColumn() {
        TestBlockView view = new TestBlockView(ODD_OCCLUDING, false);
        UUID world = UUID.randomUUID();
        ImmutableBlockLocatable first = new ImmutableBlockLocatable(world, 1, 1, 1);
        ImmutableBlockLocatable second = new ImmutableBlockLocatable(world, 2, 17, 2);
        view.updateOrInsertTileEntity(first, 99, true);
        view.updateOrInsertTileEntity(second, 99, true);

        view.pruneTileEntitiesAbsentFromChunkSections(world, 0, 0, 0, 2, null);

        assertNull(view.getTrackedTileEntity(first));
        assertNull(view.getTrackedTileEntity(second));
    }

    private static OccludingChunkData occlusionOf(char[] section) {
        long[] words = new long[ChunkData.WORD_COUNT];
        for (int packed = 0; packed < section.length; packed++) {
            if (ODD_OCCLUDING.isOccluding(section[packed])) {
                words[packed >>> 6] |= 1L << packed;
            }
        }
        return new OccludingChunkDataImpl(words);
    }

    private static final class TestBlockView extends AbstractBlockView<TestExtraData, TestTileEntity> {
        private final IntSupplier worldEpochSupplier;

        private TestBlockView(BlockInfoResolver blockInfoResolver, boolean trackAllBlocks) {
            this(blockInfoResolver, trackAllBlocks, STABLE_WORLD_EPOCH);
        }

        private TestBlockView(BlockInfoResolver blockInfoResolver, boolean trackAllBlocks, IntSupplier worldEpochSupplier) {
            super(blockInfoResolver, trackAllBlocks, worldEpochSupplier);
            this.worldEpochSupplier = worldEpochSupplier;
        }

        @Override
        protected TestTileEntity createTrackedTileEntity(BlockSpatial position, char blockID, boolean visible) {
            return new TestTileEntity(position, visible, blockID);
        }

        private TestTileEntity updateOrInsertTileEntity(UUID world, BlockSpatial position, int blockID, boolean visibleIfNew) {
            return super.updateOrInsertTileEntity(world, position, (char) blockID, visibleIfNew);
        }

        private TestTileEntity updateOrInsertTileEntity(BlockLocatable location, int blockID, boolean visibleIfNew) {
            return updateOrInsertTileEntity(location.world(), location, blockID, visibleIfNew);
        }

        private void removeTileEntity(BlockLocatable location) {
            removeTileEntity(location.world(), location);
        }

        private TestTileEntity getTrackedTileEntity(BlockLocatable location) {
            return getTrackedTileEntity(location.world(), location);
        }

        private boolean isVisible(BlockLocatable location, int currentTick) {
            return isVisible(location.world(), location, currentTick);
        }

        private boolean isBlockOccluding(UUID world, int x, int y, int z) {
            return isBlockOccluding(new ImmutableBlockLocatable(world, x, y, z));
        }

        private int updateVisibilityForEachNeedingRecheck(int recheckTicks, int currentTick, long modeToken, BlockView.VisibilityResolver action) {
            return updateVisibilityForEachNeedingRecheck(recheckTicks, currentTick, modeToken, worldEpochSupplier.getAsInt(), action);
        }
    }

    private static final class TestTileEntity extends NettyTileEntity<TestExtraData> {
        private TestTileEntity(BlockSpatial position, boolean visible, char blockID) {
            super(position, visible, TrackedTileEntity.NEVER_CHECKED, blockID);
        }
    }

    private static final class TestExtraData implements Clearable {
        @Override
        public void clear() {
        }
    }
}
