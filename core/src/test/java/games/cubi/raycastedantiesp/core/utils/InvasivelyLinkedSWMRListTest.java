/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvasivelyLinkedSWMRListTest {
    @Test
    void readerHoldingOldHeadRemainsSafeDuringHeadReplacement() throws Exception {
        Node head = new Node(1);
        Node middle = new Node(2);
        Node tail = new Node(3);
        head.linkAfter(tail);
        head.linkAfter(middle);

        CountDownLatch holdingHead = new CountDownLatch(1);
        CountDownLatch detached = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                Iterator<Node> iterator = head.iterator();
                assertEquals(1, iterator.next().value);
                holdingHead.countDown();
                assertTrue(detached.await(5, TimeUnit.SECONDS));
                while (iterator.hasNext()) {
                    iterator.next();
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        reader.start();

        assertTrue(holdingHead.await(5, TimeUnit.SECONDS));
        assertEquals(middle, head.nextAcquire());
        head.detachHeadWriterOnly();
        detached.countDown();
        reader.join(5_000L);

        assertFalse(reader.isAlive());
        assertNull(failure.get());
        assertNull(head.nextAcquire());
        assertNull(middle.previousWriterOnly());
        assertEquals(tail, middle.nextAcquire());
    }

    private static final class Node extends InvasivelyLinkedSWMRList<Node> {
        private final int value;

        private Node(int value) {
            this.value = value;
        }
    }
}
