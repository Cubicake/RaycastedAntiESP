/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.tracked;

import games.cubi.utils.Clearable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NettyEntityTrackedStateTest {
    @Test
    void trackedFlagsUpdateIndependentlyAndReportChanges() {
        TestEntity entity = new TestEntity();

        assertTrue(entity.setSneaking(true));
        assertTrue(entity.sneaking());
        assertFalse(entity.glowing());
        assertEquals(NettyEntity.NEVER_CHECKED, entity.lastChecked());

        entity.setLastChecked(10);
        assertFalse(entity.setSneaking(true));
        assertEquals(10, entity.lastChecked());

        assertTrue(entity.setGlowing(true));
        assertTrue(entity.sneaking());
        assertTrue(entity.glowing());
        assertEquals(NettyEntity.NEVER_CHECKED, entity.lastChecked());

        entity.clear();

        assertFalse(entity.sneaking());
        assertFalse(entity.glowing());
    }

    @Test
    void concurrentUpdatesToDifferentFlagsDoNotOverwriteEachOther() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            TestEntity entity = new TestEntity();
            int iterations = 2_000;
            Phaser phases = new Phaser(3);
            AtomicBoolean lostUpdate = new AtomicBoolean();

            try (var executor = Executors.newFixedThreadPool(2)) {
                var sneakingTask = executor.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        phases.arriveAndAwaitAdvance();
                        entity.setSneaking(true);
                        phases.arriveAndAwaitAdvance();
                    }
                });
                var glowingTask = executor.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        phases.arriveAndAwaitAdvance();
                        entity.setGlowing(true);
                        phases.arriveAndAwaitAdvance();
                    }
                });

                for (int i = 0; i < iterations; i++) {
                    entity.clear();
                    phases.arriveAndAwaitAdvance();
                    phases.arriveAndAwaitAdvance();
                    if (!entity.sneaking() || !entity.glowing()) {
                        lostUpdate.set(true);
                    }
                }
                sneakingTask.get();
                glowingTask.get();
            }

            assertFalse(lostUpdate.get());
        });
    }

    @Test
    void relationshipSnapshotsRemainDefensiveWhileInternalAccessorsDoNotCopy() {
        TestEntity entity = new TestEntity();
        entity.setPassengerIDs(new int[]{2, 3});
        entity.addLeashedEntity(4);

        int[] passengerSnapshot = entity.passengerIDs();
        int[] passengerInternal = entity.passengerIDsNoAlloc();
        assertNotSame(passengerSnapshot, passengerInternal);
        passengerSnapshot[0] = 99;
        assertEquals(2, passengerInternal[0]);
        assertSame(passengerInternal, entity.passengerIDsNoAlloc());

        int[] leashSnapshot = entity.leashedEntityIDsOrNull();
        int[] leashInternal = entity.leashedEntityIDsOrNullNoAlloc();
        assertNotSame(leashSnapshot, leashInternal);
        leashSnapshot[0] = 99;
        assertEquals(4, leashInternal[0]);
        assertSame(leashInternal, entity.leashedEntityIDsOrNullNoAlloc());
    }

    private static final class TestEntity extends NettyEntity<Clearable> {
        private TestEntity() {
            super(null, 0, 0, 0, 1, UUID.randomUUID(), false, 0, true);
        }
    }
}
