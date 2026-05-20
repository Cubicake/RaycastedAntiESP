package games.cubi.raycastedantiesp.core.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Long2ObjectMTHashMapTest {
    @Test
    void basicOperationsWork() {
        Long2ObjectMTHashMap<String> map = new Long2ObjectMTHashMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(1L));
        assertEquals("fallback", map.getOrDefault(1L, "fallback"));

        assertNull(map.put(1L, "one"));
        assertEquals("one", map.get(1L));
        assertTrue(map.containsKey(1L));
        assertEquals(1, map.size());

        assertEquals("one", map.replace(1L, "uno"));
        assertEquals("uno", map.get(1L));
        assertTrue(map.replace(1L, "uno", "eins"));
        assertEquals("eins", map.get(1L));
        assertFalse(map.replace(1L, "uno", "one"));

        assertFalse(map.remove(1L, "uno"));
        assertTrue(map.remove(1L, "eins"));
        assertFalse(map.containsKey(1L));
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void putIfAbsentAndComputeIfAbsentWork() {
        Long2ObjectMTHashMap<String> map = new Long2ObjectMTHashMap<>();
        AtomicInteger computeCalls = new AtomicInteger();

        assertNull(map.putIfAbsent(1L, "one"));
        assertEquals("one", map.putIfAbsent(1L, "uno"));
        assertEquals("one", map.get(1L));

        assertEquals("two", map.computeIfAbsent(2L, ignored -> {
            computeCalls.incrementAndGet();
            return "two";
        }));
        assertEquals("two", map.computeIfAbsent(2L, ignored -> {
            computeCalls.incrementAndGet();
            return "dos";
        }));
        assertEquals(1, computeCalls.get());
        assertEquals("two", map.get(2L));
    }

    @Test
    void forEachAndRemoveIfKeyWork() {
        Long2ObjectMTHashMap<String> map = new Long2ObjectMTHashMap<>();
        Set<Long> seenKeys = ConcurrentHashMap.newKeySet();

        for (long i = 0; i < 10; i++) {
            map.put(i, "value-" + i);
        }

        map.forEach((key, value) -> {
            seenKeys.add(key);
            assertEquals("value-" + key, value);
        });

        assertEquals(10, seenKeys.size());
        for (long i = 0; i < 10; i++) {
            assertTrue(seenKeys.contains(i));
        }

        assertEquals(5, map.removeIfKey(key -> key % 2 == 0));
        assertEquals(5, map.size());
        for (long i = 0; i < 10; i++) {
            assertEquals(i % 2 != 0, map.containsKey(i));
        }
    }

    @Test
    void writeLockedCallbacksRejectReentry() {
        Long2ObjectMTHashMap<String> map = new Long2ObjectMTHashMap<>();
        map.put(1L, "one");

        assertThrows(IllegalStateException.class, () -> map.computeIfAbsent(2L, ignored -> map.get(1L)));
        assertThrows(IllegalStateException.class, () -> map.forEach((key, value) -> map.size()));
        assertThrows(IllegalStateException.class, () -> map.removeIfKey(map::containsKey));
    }

    @Test
    void mutableValuesAreStoredByReference() {
        Long2ObjectMTHashMap<BitSet> map = new Long2ObjectMTHashMap<>();
        BitSet bits = new BitSet();

        map.put(1L, bits);
        bits.set(3);

        assertTrue(map.get(1L).get(3));
    }

    @Test
    void computeIfAbsentOnlyComputesOnceUnderContention() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Long2ObjectMTHashMap<String> map = new Long2ObjectMTHashMap<>();
            AtomicInteger computeCalls = new AtomicInteger();
            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<String>> futures = new ArrayList<>();

            try {
                for (int i = 0; i < 32; i++) {
                    futures.add(executor.submit(() -> {
                        await(startLatch);
                        return map.computeIfAbsent(42L, ignored -> {
                            computeCalls.incrementAndGet();
                            try {
                                TimeUnit.MILLISECONDS.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(e);
                            }
                            return "value";
                        });
                    }));
                }

                startLatch.countDown();

                for (Future<String> future : futures) {
                    assertEquals("value", future.get(5, TimeUnit.SECONDS));
                }
                assertEquals(1, computeCalls.get());
                assertEquals(1, map.size());
                assertEquals("value", map.get(42L));
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    void supportsConcurrentReadersAndWriters() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Long2ObjectMTHashMap<Integer> map = new Long2ObjectMTHashMap<>(128);
            ExecutorService executor = Executors.newFixedThreadPool(6);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Future<?>> futures = new ArrayList<>();

            try {
                for (int writer = 0; writer < 2; writer++) {
                    final int writerOffset = writer * 10_000;
                    futures.add(executor.submit(() -> {
                        await(startLatch);
                        try {
                            for (int i = 0; i < 5_000; i++) {
                                long key = (writerOffset + i) % 256L;
                                map.put(key, i);
                                map.putIfAbsent(key + 1, i);
                                map.replace(key, i + 1);
                                map.remove(key - 1);
                            }
                        } catch (Throwable t) {
                            failure.compareAndSet(null, t);
                        }
                    }));
                }

                for (int reader = 0; reader < 4; reader++) {
                    futures.add(executor.submit(() -> {
                        await(startLatch);
                        try {
                            for (int i = 0; i < 20_000; i++) {
                                long key = i % 256L;
                                map.get(key);
                                map.getOrDefault(key, -1);
                                map.containsKey(key);
                                map.size();
                                map.isEmpty();
                            }
                        } catch (Throwable t) {
                            failure.compareAndSet(null, t);
                        }
                    }));
                }

                startLatch.countDown();

                for (Future<?> future : futures) {
                    future.get(5, TimeUnit.SECONDS);
                }
                assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    void forEachAndRemoveIfKeyWorkUnderMultithreadedContention() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Long2ObjectMTHashMap<Integer> map = new Long2ObjectMTHashMap<>(256);
            for (long i = 0; i < 256; i++) {
                map.put(i, (int) i);
            }

            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Future<?>> futures = new ArrayList<>();

            try {
                futures.add(executor.submit(() -> {
                    await(startLatch);
                    try {
                        for (int i = 0; i < 50; i++) {
                            map.forEach((key, value) -> assertNotNull(value));
                        }
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }));

                futures.add(executor.submit(() -> {
                    await(startLatch);
                    try {
                        for (int i = 0; i < 50; i++) {
                            map.removeIfKey(key -> key % 11 == 0);
                        }
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }));

                futures.add(executor.submit(() -> {
                    await(startLatch);
                    try {
                        for (int i = 0; i < 5_000; i++) {
                            map.put(i % 256L, i);
                        }
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }));

                futures.add(executor.submit(() -> {
                    await(startLatch);
                    try {
                        for (int i = 0; i < 20_000; i++) {
                            map.get(i % 256L);
                            map.containsKey(i % 256L);
                        }
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }));

                startLatch.countDown();

                for (Future<?> future : futures) {
                    future.get(5, TimeUnit.SECONDS);
                }
                assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    final BitSet returnValueOne = BitSet.valueOf(new long[] {0b111111});
    final BitSet returnValueTwo = BitSet.valueOf(new long[] {0b101010});
    final BitSet initialValue = BitSet.valueOf(new long[] {0b101010, 0b010111, 0b111000});

    @Test
    void multipleThreadsReadingAndWriting() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            Long2ObjectMTHashMap<BitSet> map = new Long2ObjectMTHashMap<>(256);
            ExecutorService executor = Executors.newFixedThreadPool(6);
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Future<?>> futures = new ArrayList<>();

            // Pre-populate the map with all keys from 0 to 127
            for (long i = 0; i < 256; i++) {
                map.put(i, (BitSet) initialValue.clone());
            }

            try {
                futures.add(executor.submit(() -> {
                    await(startLatch);
                    try {
                        for (int repetitions = 0; repetitions < 44; repetitions++) {
                            for (int i = 0; i < 64; i++) {
                                long key = i;
                                map.put(key, (BitSet) returnValueOne.clone());
                            }
                        }
                        System.out.println("Writer 1 done, current successful optimistic reads: " + map.successfulOptimisticReads.get());
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }));
                futures.add(executor.submit(() -> {
                    await(startLatch);
                    try {
                        for (int repetitions = 0; repetitions < 44; repetitions++) {
                            for (int i = 0; i < 64; i++) {
                                long key = i * 2;
                                map.put(key, (BitSet) returnValueTwo.clone());
                            }
                        }
                        System.out.println("Writer 2 done, current successful optimistic reads: " + map.successfulOptimisticReads.get());
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                }));


                for (int reader = 0; reader < 4; reader++) {
                    futures.add(executor.submit(() -> {
                        await(startLatch);
                        try {
                            for (int i = 0; i < 800; i++) {
                                long key = i % 256L;
                                map.get(key);
                                map.getOrDefault(key, new BitSet());
                                map.containsKey(key);
                                map.size();
                                map.isEmpty();
                            }
                        } catch (Throwable t) {
                            failure.compareAndSet(null, t);
                        }
                    }));
                }
                startLatch.countDown();

                for (Future<?> future : futures) {
                    future.get(5, TimeUnit.SECONDS);
                }
                assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());

                int sharedKeysWithReturnValueOne = 0;
                int sharedKeysWithReturnValueTwo = 0;

                for (long i = 0; i < 256; i++) {
                    BitSet value = map.get(i);
                    assertNotNull(value, "Value for key " + i + " should not be null");

                    if (i <= 62 && i % 2 == 0) {
                        boolean matchesOne = value.equals(returnValueOne);
                        boolean matchesTwo = value.equals(returnValueTwo);

                        assertTrue(matchesOne || matchesTwo, "Shared key " + i + " should be either returnValueOne or returnValueTwo, but was "
                                        + Arrays.toString(value.toLongArray()));
                        if (matchesOne) {
                            sharedKeysWithReturnValueOne++;
                        } else {
                            sharedKeysWithReturnValueTwo++;
                        }
                    } else if (i <= 63) {
                        assertEquals(returnValueOne, value, "Key " + i + " should contain returnValueOne, but was "
                                        + Arrays.toString(value.toLongArray()));
                    } else if (i <= 127 && i % 2 == 0) {
                        assertEquals(returnValueTwo, value, "Key " + i + " should contain returnValueTwo, but was "
                                        + Arrays.toString(value.toLongArray()));
                    } else {
                        assertEquals(initialValue, value, "Key " + i + " should still contain the initial value, but was "
                                        + Arrays.toString(value.toLongArray()));
                    }
                }
                System.out.println("Results from testing Long2ObjectMTHashMap with multiple threads:");
                System.out.println("Shared keys with returnValueOne: " + sharedKeysWithReturnValueOne);
                System.out.println("Shared keys with returnValueTwo: " + sharedKeysWithReturnValueTwo);
                System.out.println("Successful atomic reads: " + map.successfulOptimisticReads.get());
                System.out.println("Failed optimistic reads (due to concurrent writes): " + map.failedOptimisticReads.get());
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        });
        int rerunTest = 2;
    }
}
