/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TimingStatsSelectorTest {
    @Test
    void concurrentEnableConvergesOnOneCollector() throws InterruptedException {
        TimingStatsSelector selector = new TimingStatsSelector();
        int threadCount = 32;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        Set<TimingStats> selected = ConcurrentHashMap.newKeySet();
        ArrayList<Thread> threads = new ArrayList<>(threadCount);

        for (int index = 0; index < threadCount; index++) {
            Thread thread = Thread.startVirtualThread(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
                selected.add(selector.select(true));
            });
            threads.add(thread);
        }

        ready.await();
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(1, selected.size());
    }

    @Test
    void disableAndReenableStartsFreshCollector() {
        TimingStatsSelector selector = new TimingStatsSelector();

        TimingStats first = selector.select(true);

        assertSame(first, selector.select(true));
        assertSame(TimingStatsNoOp.INSTANCE, selector.select(false));
        TimingStats second = selector.select(true);
        assertNotSame(first, second);
        assertSame(second, selector.select(true));
    }
}
