/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view;

import games.cubi.raycastedantiesp.core.utils.Clearable;
import games.cubi.utils.IntrusiveSPSCQueue;
import games.cubi.raycastedantiesp.core.tracked.TrackedEntity;

import java.util.Objects;

/**
 * Queues entity visibility transitions without allocating a record for every transition.
 *
 * <p>Exactly one engine thread at a time owns the pending batch and publishes it to the intrusive SPSC queue. The
 * producer role may migrate between engine threads, while the player's Netty thread exclusively drains published
 * entries. Producer and consumer methods must not be called from the opposite role.</p>
 */
public final class PackedEntityTransitionQueue { //todo: potentially merge this with PackedBlockTransitionQueue, and add a mode token to entities
    private static final int BATCH_MIN_SIZE = 3;
    private static final int BATCH_SIZE = 8;

    private final IntrusiveSPSCQueue<QueueEntry> publishedEntries = new IntrusiveSPSCQueue<>();
    private final PendingBatch pending = new PendingBatch();

    public void add(EntityViewTransition.Type type, TrackedEntity<?> entity, int worldEpoch) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(entity, "entity");

        if (pending.count != 0 && pending.worldEpoch != worldEpoch) {
            flushPending();
        }
        if (pending.count == 0) {
            pending.worldEpoch = worldEpoch;
        }
        pending.add(type, entity);
        if (pending.count == BATCH_SIZE) {
            flushPending();
        }
    }

    /** Publishes the producer-owned partial batch. Only the engine producer may call this method. */
    public void flushPendingTransitions() {
        flushPending();
    }

    public boolean hasPendingTransitions() {
        return !publishedEntries.isEmpty();
    }

    /**
     * Drains published entries in FIFO order. A claimed queue entry is consumed at most once if a callback throws while
     * processing it, and any unvisited transitions packed into that entry are discarded.
     */
    public void drainTransitions(EntityView.TransitionConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        QueueEntry entry;
        while ((entry = publishedEntries.poll()) != null) {
            entry.drain(consumer);
        }
    }

    /**
     * Discards transitions already published to the consumer. The producer-owned pending batch is intentionally left
     * untouched: a structural clear can race an engine flush, and that in-flight batch must retain its old epoch so
     * the consumer can reject it normally after it is published.
     */
    public void clearPublishedTransitions() {
        publishedEntries.clear();
    }

    private void flushPending() {
        switch (pending.count) {
            case 0 -> {
                return;
            }
            case 1 -> publishedEntries.offer(new SingleEntry(pending.t1, pending.e1, pending.worldEpoch));
            case 2 -> {
                publishedEntries.offer(new SingleEntry(pending.t1, pending.e1, pending.worldEpoch));
                publishedEntries.offer(new SingleEntry(pending.t2, pending.e2, pending.worldEpoch));
            }
            case BATCH_MIN_SIZE, 4, 5, 6, 7, BATCH_SIZE -> publishedEntries.offer(new BatchEntry(pending));
            default -> throw new IllegalStateException("Entity transition batch exceeded eight entries");
        }
        pending.clear();
    }

    private abstract static class QueueEntry extends IntrusiveSPSCQueue.Node {
        abstract void drain(EntityView.TransitionConsumer consumer);
    }

    private static final class SingleEntry extends QueueEntry {
        private final EntityViewTransition.Type type;
        private final TrackedEntity<?> entity;
        private final int worldEpoch;

        private SingleEntry(EntityViewTransition.Type type, TrackedEntity<?> entity, int worldEpoch) {
            this.type = type;
            this.entity = entity;
            this.worldEpoch = worldEpoch;
        }

        @Override
        public void drain(EntityView.TransitionConsumer consumer) {
            consumer.accept(type, entity, worldEpoch);
        }
    }

    private static final class PendingBatch implements Clearable {
        private EntityViewTransition.Type t1;
        private EntityViewTransition.Type t2;
        private EntityViewTransition.Type t3;
        private EntityViewTransition.Type t4;
        private EntityViewTransition.Type t5;
        private EntityViewTransition.Type t6;
        private EntityViewTransition.Type t7;
        private EntityViewTransition.Type t8;
        private TrackedEntity<?> e1;
        private TrackedEntity<?> e2;
        private TrackedEntity<?> e3;
        private TrackedEntity<?> e4;
        private TrackedEntity<?> e5;
        private TrackedEntity<?> e6;
        private TrackedEntity<?> e7;
        private TrackedEntity<?> e8;
        private int worldEpoch;
        private int count;

        private void add(EntityViewTransition.Type type, TrackedEntity<?> entity) {
            switch (++count) {
                case 1 -> { t1 = type; e1 = entity; }
                case 2 -> { t2 = type; e2 = entity; }
                case 3 -> { t3 = type; e3 = entity; }
                case 4 -> { t4 = type; e4 = entity; }
                case 5 -> { t5 = type; e5 = entity; }
                case 6 -> { t6 = type; e6 = entity; }
                case 7 -> { t7 = type; e7 = entity; }
                case 8 -> { t8 = type; e8 = entity; }
                default -> throw new IllegalStateException("Entity transition batch exceeded eight entries");
            }
        }

        public void clear() {
            t1 = null;
            t2 = null;
            t3 = null;
            t4 = null;
            t5 = null;
            t6 = null;
            t7 = null;
            t8 = null;
            e1 = null;
            e2 = null;
            e3 = null;
            e4 = null;
            e5 = null;
            e6 = null;
            e7 = null;
            e8 = null;
            worldEpoch = 0;
            count = 0;
        }
    }

    private static final class BatchEntry extends QueueEntry {
        private final EntityViewTransition.Type t1;
        private final EntityViewTransition.Type t2;
        private final EntityViewTransition.Type t3;
        private final EntityViewTransition.Type t4;
        private final EntityViewTransition.Type t5;
        private final EntityViewTransition.Type t6;
        private final EntityViewTransition.Type t7;
        private final EntityViewTransition.Type t8;
        private final TrackedEntity<?> e1;
        private final TrackedEntity<?> e2;
        private final TrackedEntity<?> e3;
        private final TrackedEntity<?> e4;
        private final TrackedEntity<?> e5;
        private final TrackedEntity<?> e6;
        private final TrackedEntity<?> e7;
        private final TrackedEntity<?> e8;
        private final int worldEpoch;
        private final int count;

        private BatchEntry(PendingBatch pending) {
            t1 = pending.t1;
            t2 = pending.t2;
            t3 = pending.t3;
            t4 = pending.t4;
            t5 = pending.t5;
            t6 = pending.t6;
            t7 = pending.t7;
            t8 = pending.t8;
            e1 = pending.e1;
            e2 = pending.e2;
            e3 = pending.e3;
            e4 = pending.e4;
            e5 = pending.e5;
            e6 = pending.e6;
            e7 = pending.e7;
            e8 = pending.e8;
            worldEpoch = pending.worldEpoch;
            count = pending.count;
        }

        @Override
        public void drain(EntityView.TransitionConsumer consumer) {
            consumer.accept(t1, e1, worldEpoch);
            if (count >= 2) consumer.accept(t2, e2, worldEpoch);
            if (count >= 3) consumer.accept(t3, e3, worldEpoch);
            if (count >= 4) consumer.accept(t4, e4, worldEpoch);
            if (count >= 5) consumer.accept(t5, e5, worldEpoch);
            if (count >= 6) consumer.accept(t6, e6, worldEpoch);
            if (count >= 7) consumer.accept(t7, e7, worldEpoch);
            if (count >= 8) consumer.accept(t8, e8, worldEpoch);
        }
    }
}
