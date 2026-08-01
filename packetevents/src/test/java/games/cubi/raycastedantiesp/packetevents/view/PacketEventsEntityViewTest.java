/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.view;

import games.cubi.raycastedantiesp.core.view.EntityViewTransition;
import games.cubi.raycastedantiesp.packetevents.tracked.PacketEventsEntity;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsEntityReplayData;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsEntityViewTest {
    @Test
    void sizeTracksMembershipButNotVisibility() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        UUID world = UUID.randomUUID();
        PacketEventsEntity visible = entity(1, UUID.randomUUID());
        PacketEventsEntity hidden = entity(2, UUID.randomUUID());
        hidden.setVisible(false);

        assertEquals(0, view.size());
        view.insertEntity(world, visible);
        view.insertEntity(world, hidden);
        assertEquals(2, view.size());

        view.setVisibility(visible, false, 1, worldEpoch.getAcquire());
        view.setVisibility(hidden, true, 1, worldEpoch.getAcquire());
        assertEquals(2, view.size());

        view.removeEntity(visible.entityID());
        assertEquals(1, view.size());
        view.removeEntity(hidden.entityID());
        assertEquals(0, view.size());
    }

    @Test
    void negativeEntityIDsSupportLookupAndRemoval() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        UUID world = UUID.randomUUID();
        PacketEventsEntity negative = entity(-3, UUID.randomUUID());
        PacketEventsEntity minimum = entity(Integer.MIN_VALUE, UUID.randomUUID());

        view.insertEntity(world, negative);
        view.insertEntity(world, minimum);

        assertSame(negative, view.getEntity(-3));
        assertSame(minimum, view.getEntity(Integer.MIN_VALUE));
        assertTrue(view.exists(-3));
        assertTrue(view.exists(Integer.MIN_VALUE));

        view.removeEntity(-3);
        view.removeEntity(Integer.MIN_VALUE);

        assertFalse(view.exists(-3));
        assertFalse(view.exists(Integer.MIN_VALUE));
        assertEquals(0, view.size());
    }

    @Test
    void sizeCanBeReadFromAnotherThread() throws Exception {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        view.insertEntity(UUID.randomUUID(), entity(1, UUID.randomUUID()));

        int size = CompletableFuture.supplyAsync(view::size).get(5, TimeUnit.SECONDS);

        assertEquals(1, size);
    }

    @Test
    void staleWorldEpochCannotMutateReplacement() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        UUID firstWorld = UUID.randomUUID();
        UUID secondWorld = UUID.randomUUID();
        UUID entityUUID = UUID.randomUUID();
        PacketEventsEntity original = entity(1, entityUUID);
        view.insertEntity(firstWorld, original);
        int staleEpoch = worldEpoch.getAcquire();

        PacketEventsEntity replacement = entity(1, entityUUID);
        worldEpoch.setRelease(4);
        view.insertEntity(secondWorld, replacement);
        view.setVisibility(original, false, 1, staleEpoch);

        assertTrue(replacement.visible());
        assertFalse(view.hasPendingTransitions());
        assertTrue(worldEpoch.getAcquire() > staleEpoch);
    }

    @Test
    void replacedObjectCannotCommitWithinSameWorld() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        UUID world = UUID.randomUUID();
        UUID entityUUID = UUID.randomUUID();
        PacketEventsEntity original = entity(1, entityUUID);
        view.insertEntity(world, original);
        int epoch = worldEpoch.getAcquire();

        PacketEventsEntity replacement = entity(1, entityUUID);
        view.insertEntity(world, replacement);
        view.setVisibility(original, false, 1, epoch);

        assertTrue(replacement.visible());
        assertFalse(view.hasPendingTransitions());
        assertEquals(epoch, worldEpoch.getAcquire());
    }

    @Test
    void directVisibilityDoesNotPublishEngineTransition() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        PacketEventsEntity entity = entity(1, UUID.randomUUID());
        entity.setVisible(false);
        view.insertEntity(UUID.randomUUID(), entity);

        assertTrue(view.recordDirectVisibility(entity, true, 1, worldEpoch.getAcquire()));

        assertTrue(entity.visible());
        assertEquals(1, entity.lastChecked());
        assertFalse(view.hasPendingTransitions());
    }

    @Test
    void currentEntityTransitionRetainsIdentityAndEpoch() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        UUID world = UUID.randomUUID();
        PacketEventsEntity entity = entity(1, UUID.randomUUID());
        view.insertEntity(world, entity);
        int epoch = worldEpoch.getAcquire();

        view.setVisibility(entity, false, 1, epoch);
        view.flushPendingTransitions();
        AtomicReference<EntityViewTransition.Type> transitionType = new AtomicReference<>();
        AtomicReference<PacketEventsEntity> transitionEntity = new AtomicReference<>();
        AtomicInteger transitionWorldEpoch = new AtomicInteger();
        view.drainTransitions((type, queuedEntity, queuedWorldEpoch) -> {
            transitionType.set(type);
            transitionEntity.set((PacketEventsEntity) queuedEntity);
            transitionWorldEpoch.set(queuedWorldEpoch);
        });

        assertSame(entity, transitionEntity.get());
        assertEquals(epoch, transitionWorldEpoch.get());
        assertEquals(EntityViewTransition.Type.HIDE, transitionType.get());
        assertFalse(entity.visible());
        assertEquals(1, entity.lastChecked());
    }

    @Test
    void transitionPublicationIncludesCommittedEntityState() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            AtomicInteger worldEpoch = new AtomicInteger(2);
            PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
            UUID world = UUID.randomUUID();
            PacketEventsEntity entity = entity(1, UUID.randomUUID());
            view.insertEntity(world, entity);
            AtomicInteger acknowledgedTick = new AtomicInteger();
            int transitions = 1_000;

            Thread producer = Thread.startVirtualThread(() -> {
                for (int tick = 1; tick <= transitions; tick++) {
                    view.setVisibility(entity, (tick & 1) == 0, tick, worldEpoch.getAcquire());
                    view.flushPendingTransitions();
                    while (acknowledgedTick.getAcquire() != tick) {
                        Thread.onSpinWait();
                    }
                }
            });

            AtomicReference<EntityViewTransition.Type> transitionType = new AtomicReference<>();
            boolean[] drained = {false};
            for (int tick = 1; tick <= transitions; tick++) {
                do {
                    drained[0] = false;
                    view.drainTransitions((type, queuedEntity, worldEpochValue) -> {
                        transitionType.set(type);
                        drained[0] = true;
                    });
                    if (!drained[0]) Thread.onSpinWait();
                } while (!drained[0]);

                boolean expectedVisible = (tick & 1) == 0;
                assertEquals(expectedVisible ? EntityViewTransition.Type.SHOW : EntityViewTransition.Type.HIDE, transitionType.get());
                assertEquals(expectedVisible, entity.visible());
                assertEquals(tick, entity.lastChecked());
                acknowledgedTick.setRelease(tick);
            }
            producer.join();
        });
    }

    @Test
    void clearingPersistentSelfEntityResetsWorldScopedState() {
        PacketEventsEntity self = PacketEventsEntity.createSelfEntity(null, 1, UUID.randomUUID());
        self.setPassengerIDs(new int[]{2});
        self.setVehicleID(3);
        self.addLeashedEntity(4);
        self.setLeashingEntity(5);
        self.setPacketReplayData(PacketEventsEntityReplayData.create());

        self.clear();

        assertNull(self.passengerIDs());
        assertEquals(-1, self.vehicleID());
        assertNull(self.leashedEntityIDsOrNull());
        assertEquals(-2, self.leashingEntity());
        assertNull(self.packetReplayData());
    }

    @Test
    void packetThreadOnlyStateRoundTripsAndProtectsLeashStorage() {
        PacketEventsEntity entity = entity(1, UUID.randomUUID());
        entity.setYaw(1.25f);
        entity.setPitch(2.5f);
        entity.setHeadYaw(3.75f);
        entity.setVelocity(4.0, 5.0, 6.0);
        entity.setOnGround(false);
        entity.setEntityData(7);
        entity.setClientVisible(false);
        entity.addLeashedEntity(8);

        assertEquals(1.25f, entity.yaw());
        assertEquals(2.5f, entity.pitch());
        assertEquals(3.75f, entity.headYaw());
        assertEquals(4.0, entity.velocityX());
        assertEquals(5.0, entity.velocityY());
        assertEquals(6.0, entity.velocityZ());
        assertFalse(entity.onGround());
        assertEquals(7, entity.entityData());
        assertFalse(entity.clientVisible());

        int[] leashSnapshot = entity.leashedEntityIDsOrNull();
        assertArrayEquals(new int[]{8}, leashSnapshot);
        leashSnapshot[0] = 9;
        assertArrayEquals(new int[]{8}, entity.leashedEntityIDsOrNull());
    }

    @Test
    void trackedStateChangeDoesNotOverrideDisabledVisibleRechecks() {
        AtomicInteger worldEpoch = new AtomicInteger(2);
        PacketEventsEntityView view = PacketEventsEntityView.createEntityView(worldEpoch::getAcquire);
        PacketEventsEntity entity = entity(1, UUID.randomUUID());
        view.insertEntity(UUID.randomUUID(), entity);
        view.setVisibility(entity, true, 10, worldEpoch.getAcquire());

        assertEquals(0, view.forEachNeedingRecheckEntity(-1, 11, true, worldEpoch.getAcquire(), ignored -> {}));

        assertTrue(entity.setGlowing(true));
        assertEquals(0, view.forEachNeedingRecheckEntity(-1, 12, true, worldEpoch.getAcquire(), ignored -> {}));
        assertEquals(0, view.forEachNeedingRecheckEntity(-1, 13, true, worldEpoch.getAcquire(), ignored -> {}));
    }

    private static PacketEventsEntity entity(int entityID, UUID entityUUID) {
        return new PacketEventsEntity(null, 1, 2, 3, entityID, entityUUID, false, 0, true);
    }
}
