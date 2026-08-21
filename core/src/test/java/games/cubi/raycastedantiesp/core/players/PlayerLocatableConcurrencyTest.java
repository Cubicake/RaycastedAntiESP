/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.implementations.ImmutableLocatableImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLocatableConcurrencyTest {
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_WORLD = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void concurrentReadersAndWritersObserveFiniteSamples() throws Exception {
        PlayerLocatable location = new PlayerLocatable();
        location.initialise(WORLD, 0.0, 1.0, 2.0, 0.0f, 0.0f, PlayerPose.STANDING);
        ImmutableLocatableImpl target = new ImmutableLocatableImpl(WORLD, 10.0, 11.0, 12.0);

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch readersStarted = new CountDownLatch(4);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<?>> readers = new ArrayList<>();

        try {
            Future<?> lifecycleWriter = executor.submit(() -> runLifecycleWriter(location));
            Future<?> partialWriter = executor.submit(() -> runPartialWriter(location));
            for (int i = 0; i < 4; i++) {
                readers.add(executor.submit(() -> runReader(location, target, running, readersStarted, failure)));
            }

            assertTrue(readersStarted.await(5, TimeUnit.SECONDS));
            lifecycleWriter.get(10, TimeUnit.SECONDS);
            partialWriter.get(10, TimeUnit.SECONDS);
            running.set(false);
            for (Future<?> reader : readers) {
                reader.get(10, TimeUnit.SECONDS);
            }

            assertNull(failure.get(), () -> "Concurrent reader failed: " + failure.get());
        } finally {
            running.set(false);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static void runLifecycleWriter(PlayerLocatable location) {
        for (int i = 0; i < 20_000; i++) {
            UUID world = (i & 1) == 0 ? WORLD : OTHER_WORLD;
            location.worldChange(world, i, i + 1.0, i + 2.0, i % 360.0f, (i % 180.0f) - 90.0f, poseFor(i));
            if ((i & 255) == 0) {
                location.invalidate();
                location.initialise(world, i, i + 1.0, i + 2.0, 0.0f, 0.0f, PlayerPose.UNKNOWN);
            }
        }
    }

    private static void runPartialWriter(PlayerLocatable location) {
        for (int i = 0; i < 20_000; i++) {
            try {
                location.updateFoot(i + 0.25, i + 1.25, i + 2.25);
                location.updateRotation(i % 360.0f, (i % 180.0f) - 90.0f);
                location.updatePose(poseFor(i + 1));
            } catch (IllegalStateException ignored) {
                // The lifecycle writer is allowed to invalidate between partial updates.
            }
        }
    }

    private static void runReader(PlayerLocatable location, ImmutableLocatableImpl target, AtomicBoolean running,
                                  CountDownLatch readersStarted, AtomicReference<Throwable> failure) {
        readersStarted.countDown();
        try {
            while (running.get()) {
                location.world();
                double x = location.x();
                double y = location.y();
                double z = location.z();
                float yaw = location.yaw();
                float pitch = location.pitch();
                location.pose();
                float cameraYOffset = location.cameraYOffset();
                double cameraX = location.cameraX();
                double cameraY = location.cameraY();
                double cameraZ = location.cameraZ();
                double distanceSquared = location.cameraDistanceSquared(target);

                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                        || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                        || !Float.isFinite(cameraYOffset)
                        || !Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)
                        || !Double.isFinite(distanceSquared)) {
                    throw new AssertionError("Observed a non-finite player sample");
                }
            }
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
        }
    }

    private static PlayerPose poseFor(int value) {
        return switch (Math.floorMod(value, 4)) {
            case 0 -> PlayerPose.STANDING;
            case 1 -> PlayerPose.SNEAKING;
            case 2 -> PlayerPose.SWIMMING;
            default -> PlayerPose.SLEEPING;
        };
    }
}
