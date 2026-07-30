/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class CopyOnWriteMTIntSetTest {
    private static final List<Implementation> IMPLEMENTATIONS = List.of(
            new Implementation("sorted array", SortedMTIntSet::new),
            new Implementation("fastutil wrapper", MTWrappedFastIntSet::new)
    );

    @TestFactory
    Stream<DynamicContainer> implementationsMeetContract() {
        return IMPLEMENTATIONS.stream().map(implementation -> dynamicContainer(
                implementation.name(),
                Stream.of(
                        dynamicTest("adds, finds, and removes all int values", () -> basicOperations(implementation.create())),
                        dynamicTest("removes beginning, middle, and end values", () -> removalPositions(implementation.create())),
                        dynamicTest("supports concurrent readers and writers", () -> concurrentReadersAndWriters(implementation.create()))
                )
        ));
    }

    private static void basicOperations(CopyOnWriteMTIntSet set) {
        int[] values = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};
        for (int value : values) {
            assertFalse(set.contains(value));
            set.add(value);
            set.add(value);
            assertTrue(set.contains(value));
        }
        for (int value : values) {
            assertTrue(set.remove(value));
            assertFalse(set.contains(value));
            assertFalse(set.remove(value));
        }
    }

    private static void removalPositions(CopyOnWriteMTIntSet set) {
        for (int value = 1; value <= 5; value++) {
            set.add(value);
        }

        assertTrue(set.remove(1));
        assertTrue(set.remove(3));
        assertTrue(set.remove(5));

        assertFalse(set.contains(1));
        assertTrue(set.contains(2));
        assertFalse(set.contains(3));
        assertTrue(set.contains(4));
        assertFalse(set.contains(5));
    }

    private static void concurrentReadersAndWriters(CopyOnWriteMTIntSet set) {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            int valueCount = 512;
            int addedBase = 10_000;
            for (int value = 0; value < valueCount; value++) {
                set.add(value);
            }

            ExecutorService executor = Executors.newFixedThreadPool(6);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            try {
                for (int worker = 0; worker < 2; worker++) {
                    int from = worker * valueCount / 2;
                    int to = (worker + 1) * valueCount / 2;
                    futures.add(executor.submit(() -> {
                        await(start);
                        for (int value = from; value < to; value++) {
                            assertTrue(set.remove(value));
                        }
                    }));
                    futures.add(executor.submit(() -> {
                        await(start);
                        for (int value = from; value < to; value++) {
                            set.add(addedBase + value);
                        }
                    }));
                }
                for (int reader = 0; reader < 2; reader++) {
                    futures.add(executor.submit(() -> {
                        await(start);
                        for (int iteration = 0; iteration < 10_000; iteration++) {
                            int value = iteration % valueCount;
                            set.contains(value);
                            set.contains(addedBase + value);
                        }
                    }));
                }

                start.countDown();
                for (Future<?> future : futures) {
                    future.get(5, TimeUnit.SECONDS);
                }
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }

            for (int value = 0; value < valueCount; value++) {
                assertFalse(set.contains(value));
                assertTrue(set.contains(addedBase + value));
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while awaiting concurrent test start", exception);
        }
    }

    private record Implementation(String name, Supplier<CopyOnWriteMTIntSet> factory) {
        private CopyOnWriteMTIntSet create() {
            return factory.get();
        }
    }
}
