/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import games.cubi.locatables.implementations.ImmutableBlockSpatialImpl;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.tracked.NettyTileEntity;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.packetevents.view.PacketEventsBlockView;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsBlockViewControllerTest {
    private static final IntSupplier STABLE_WORLD_EPOCH = () -> 2;
    private static final BlockInfoResolver RESOLVER = new BlockInfoResolver() {
        @Override public boolean isOccluding(int blockStateID) { return false; }
        @Override public boolean isTileEntity(int blockStateID) { return blockStateID != 0; }
        @Override public boolean hasBlockEntityData(int blockStateID) { return blockStateID != 0; }
    };

    @Test
    void transitionCannotTargetReplacementAtSameCoordinates() {
        UUID world = UUID.randomUUID();
        ImmutableBlockSpatialImpl location = new ImmutableBlockSpatialImpl(3, 64, 5);
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        view.updateOrInsertTileEntity(world, location, (char) 1, true);
        TrackedTileEntity<?> original = view.getTrackedTileEntity(world, location);
        view.updateVisibilityForEachNeedingRecheck(0, 1, view.tileEntityCheckModeToken(), 2, ignored -> BlockView.VisibilityResolver.HIDE);
        view.flushPendingTransitions();
        AtomicReference<TrackedTileEntity<?>> transitionEntity = new AtomicReference<>();
        view.drainTransitions((type, tileEntity, modeToken, worldEpoch) -> transitionEntity.set(tileEntity));

        view.removeTileEntity(world, location);
        view.updateOrInsertTileEntity(world, location, (char) 2, true);
        TrackedTileEntity<?> replacement = view.getTrackedTileEntity(world, location);
        int replacementLastChecked = replacement.lastChecked();

        assertSame(original, transitionEntity.get());
        assertTrue(((NettyTileEntity<?>) original).isRemoved());
        original.setLastChecked(42);
        assertTrue(((NettyTileEntity<?>) original).isRemoved());
        assertNull(PacketEventsBlockViewController.resolveCurrentTransitionState(transitionEntity.get(), 2, 2));
        assertTrue(replacement.visible());
        assertEquals(replacementLastChecked, replacement.lastChecked());
    }

    @Test
    void currentTransitionStillResolvesByIdentity() {
        UUID world = UUID.randomUUID();
        ImmutableBlockSpatialImpl location = new ImmutableBlockSpatialImpl(3, 64, 5);
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        TrackedTileEntity<?> tileEntity = view.updateOrInsertTileEntity(world, location, (char) 1, true);
        view.updateVisibilityForEachNeedingRecheck(0, 1, view.tileEntityCheckModeToken(), 2, ignored -> BlockView.VisibilityResolver.HIDE);
        view.flushPendingTransitions();
        AtomicReference<TrackedTileEntity<?>> transitionEntity = new AtomicReference<>();
        view.drainTransitions((type, queuedTileEntity, modeToken, worldEpoch) -> transitionEntity.set(queuedTileEntity));

        assertSame(transitionEntity.get(), PacketEventsBlockViewController.resolveCurrentTransitionState(transitionEntity.get(), 2, 2));
    }

    @Test
    void showTransitionCannotTargetReplacementAtSameCoordinates() {
        UUID world = UUID.randomUUID();
        ImmutableBlockSpatialImpl location = new ImmutableBlockSpatialImpl(3, 64, 5);
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, STABLE_WORLD_EPOCH);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        TrackedTileEntity<?> original = view.updateOrInsertTileEntity(world, location, (char) 1, false);
        view.updateVisibilityForEachNeedingRecheck(0, 1, view.tileEntityCheckModeToken(), 2, ignored -> BlockView.VisibilityResolver.SHOW);
        view.flushPendingTransitions();
        AtomicReference<TrackedTileEntity<?>> transitionEntity = new AtomicReference<>();
        view.drainTransitions((type, tileEntity, modeToken, worldEpoch) -> transitionEntity.set(tileEntity));

        view.removeTileEntity(world, location);
        view.updateOrInsertTileEntity(world, location, (char) 2, false);
        TrackedTileEntity<?> replacement = view.getTrackedTileEntity(world, location);
        int replacementLastChecked = replacement.lastChecked();

        assertNull(PacketEventsBlockViewController.resolveCurrentTransitionState(transitionEntity.get(), 2, 2));
        assertFalse(replacement.visible());
        assertEquals(replacementLastChecked, replacement.lastChecked());
    }

    @Test
    void transitionCannotCrossWorldEpoch() {
        UUID firstWorld = UUID.randomUUID();
        UUID secondWorld = UUID.randomUUID();
        ImmutableBlockSpatialImpl position = new ImmutableBlockSpatialImpl(3, 64, 5);
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsBlockView view = new PacketEventsBlockView(RESOLVER, true, worldEpoch::getAcquire);
        view.applyTileEntityCheckMode(true, 0, unused -> {});
        TrackedTileEntity<?> original = view.updateOrInsertTileEntity(firstWorld, position, (char) 1, true);
        view.updateVisibilityForEachNeedingRecheck(0, 1, view.tileEntityCheckModeToken(), worldEpoch.getAcquire(), ignored -> BlockView.VisibilityResolver.HIDE);
        view.flushPendingTransitions();
        AtomicReference<TrackedTileEntity<?>> transitionEntity = new AtomicReference<>();
        AtomicInteger transitionWorldEpoch = new AtomicInteger();
        view.drainTransitions((type, tileEntity, modeToken, queuedWorldEpoch) -> {
            transitionEntity.set(tileEntity);
            transitionWorldEpoch.set(queuedWorldEpoch);
        });

        worldEpoch.setRelease(4);
        TrackedTileEntity<?> replacement = view.updateOrInsertTileEntity(secondWorld, position, (char) 2, true);

        assertNull(PacketEventsBlockViewController.resolveCurrentTransitionState(
                transitionEntity.get(), transitionWorldEpoch.get(), worldEpoch.getAcquire()));
        assertTrue(replacement.visible());
    }
}
