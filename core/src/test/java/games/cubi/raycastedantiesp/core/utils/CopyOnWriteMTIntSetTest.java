/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import games.cubi.utils.sets.CopyOnWriteMTIntSet;
import games.cubi.utils.sets.SortedStripedMTIntSet;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class CopyOnWriteMTIntSetTest {
    private static final List<Implementation> IMPLEMENTATIONS = List.of(
            new Implementation("sorted stripes", SortedStripedMTIntSet::new)
    );

    @TestFactory
    Stream<DynamicContainer> implementationsMeetContract() {
        return IMPLEMENTATIONS.stream().map(implementation -> dynamicContainer(
                implementation.name(),
                Stream.of(
                        dynamicTest("adds, finds, and removes all int values", () -> basicOperations(implementation.create())),
                        dynamicTest("removes beginning, middle, and end values", () -> removalPositions(implementation.create())),
                        dynamicTest("traverses one coherent snapshot", () -> snapshotTraversal(implementation.create())),
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

    private static void snapshotTraversal(CopyOnWriteMTIntSet set) {
        set.add(1);
        set.add(2);
        set.add(3);

        Set<Integer> observed = new HashSet<>();
        set.forEach(value -> {
            assertTrue(observed.add(value));
            if (value == 1) {
                assertTrue(set.remove(2));
                set.add(4);
            }
        });

        assertTrue(observed.containsAll(Set.of(1, 2, 3)));
        assertTrue(observed.size() == 3);
        assertFalse(set.contains(2));
        assertTrue(set.contains(4));

        set.remove(1);
        set.remove(3);
        set.remove(4);
        set.forEach(value -> assertTrue(false, "An empty set must not be traversed: " + value));
    }

    private static void concurrentReadersAndWriters(CopyOnWriteMTIntSet set) {
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            int transientCount = 512;
            int transientBase = 100_000;
            for (int value = 0; value < transientCount; value++) {
                set.add(transientBase + value);
            }

            int valueCount = 2_048;
            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            try {
                for (int worker = 0; worker < 2; worker++) {
                    int workerIndex = worker;
                    futures.add(executor.submit(() -> {
                        await(start);
                        if (workerIndex == 0) {
                            for (int value = 0; value < valueCount; value++) {
                                addFinalValues(set, value);
                            }
                        } else {
                            for (int value = valueCount - 1; value >= 0; value--) {
                                addFinalValues(set, value);
                            }
                        }
                    }));
                }
                for (int remover = 0; remover < 2; remover++) {
                    futures.add(executor.submit(() -> {
                        await(start);
                        for (int value = 0; value < transientCount; value++) {
                            set.remove(transientBase + value);
                        }
                    }));
                }
                for (int reader = 0; reader < 3; reader++) {
                    futures.add(executor.submit(() -> {
                        await(start);
                        for (int iteration = 0; iteration < 10_000; iteration++) {
                            int value = iteration % valueCount;
                            set.contains(value);
                            set.contains(-value - 1);
                            set.contains(Integer.MIN_VALUE);
                            set.contains(Integer.MAX_VALUE);
                            if (iteration % 100 == 0) {
                                Set<Integer> snapshot = new HashSet<>();
                                set.forEach(snapshotValue -> assertTrue(snapshot.add(snapshotValue), "Duplicate value in traversal snapshot: " + snapshotValue));
                            }
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

            Set<Integer> expected = new HashSet<>();
            for (int value = 0; value < valueCount; value++) {
                expected.add(value);
                expected.add(-value - 1);
            }
            expected.add(Integer.MIN_VALUE);
            expected.add(Integer.MAX_VALUE);

            Set<Integer> actual = new HashSet<>();
            set.forEach(value -> assertTrue(actual.add(value), "Duplicate value in final snapshot: " + value));
            assertEquals(expected, actual);
            for (int value = 0; value < transientCount; value++) {
                assertFalse(set.contains(transientBase + value));
            }
        });
    }

    private static void addFinalValues(CopyOnWriteMTIntSet set, int value) {
        set.add(value);
        set.add(-value - 1);
        set.add(value);
        set.add(-value - 1);
        if (value == 0) {
            set.add(Integer.MIN_VALUE);
            set.add(Integer.MAX_VALUE);
        }
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
