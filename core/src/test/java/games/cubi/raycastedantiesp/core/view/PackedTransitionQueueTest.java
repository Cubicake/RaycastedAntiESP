/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view;

import games.cubi.raycastedantiesp.core.tracked.TrackedEntity;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackedTransitionQueueTest {
    @Test
    void entitySequencesPreserveOrderIdentityAndEpoch() {
        for (int count : new int[]{1, 2, 3, 8, 9}) {
            PackedEntityTransitionQueue queue = new PackedEntityTransitionQueue();
            List<TrackedEntity<?>> entities = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                TrackedEntity<?> entity = entity();
                entities.add(entity);
                queue.add(index % 2 == 0 ? EntityViewTransition.Type.SHOW : EntityViewTransition.Type.HIDE, entity, 7);
            }

            queue.flushPendingTransitions();
            assertTrue(queue.hasPendingTransitions());
            List<EntityObservation> observed = new ArrayList<>();
            queue.drainTransitions((type, entity, worldEpoch) -> observed.add(new EntityObservation(type, entity, worldEpoch)));

            assertEquals(count, observed.size());
            for (int index = 0; index < count; index++) {
                EntityObservation observation = observed.get(index);
                assertSame(entities.get(index), observation.entity());
                assertEquals(index % 2 == 0 ? EntityViewTransition.Type.SHOW : EntityViewTransition.Type.HIDE, observation.type());
                assertEquals(7, observation.worldEpoch());
            }
            assertFalse(queue.hasPendingTransitions());
        }
    }

    @Test
    void entityMetadataChangeSplitsPendingBatch() {
        PackedEntityTransitionQueue queue = new PackedEntityTransitionQueue();
        TrackedEntity<?> first = entity();
        TrackedEntity<?> second = entity();
        TrackedEntity<?> third = entity();
        queue.add(EntityViewTransition.Type.HIDE, first, 2);
        queue.add(EntityViewTransition.Type.SHOW, second, 2);
        queue.add(EntityViewTransition.Type.HIDE, third, 4);
        queue.flushPendingTransitions();

        List<EntityObservation> observed = new ArrayList<>();
        queue.drainTransitions((type, entity, worldEpoch) -> observed.add(new EntityObservation(type, entity, worldEpoch)));

        assertEquals(List.of(2, 2, 4), observed.stream().map(EntityObservation::worldEpoch).toList());
        assertSame(first, observed.get(0).entity());
        assertSame(second, observed.get(1).entity());
        assertSame(third, observed.get(2).entity());
    }

    @Test
    void blockSequencesPreserveOrderIdentityAndSharedMetadata() {
        PackedBlockTransitionQueue queue = new PackedBlockTransitionQueue();
        List<TrackedTileEntity<?>> tiles = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            TrackedTileEntity<?> tileEntity = tileEntity();
            tiles.add(tileEntity);
            queue.add(index % 2 == 0 ? BlockViewTransition.Type.SHOW : BlockViewTransition.Type.HIDE, tileEntity, 13L, 5);
        }

        queue.flushPendingTransitions();
        List<BlockObservation> observed = new ArrayList<>();
        queue.drainTransitions((type, tileEntity, modeToken, worldEpoch) ->
                observed.add(new BlockObservation(type, tileEntity, modeToken, worldEpoch)));

        assertEquals(9, observed.size());
        for (int index = 0; index < observed.size(); index++) {
            BlockObservation observation = observed.get(index);
            assertSame(tiles.get(index), observation.tileEntity());
            assertEquals(index % 2 == 0 ? BlockViewTransition.Type.SHOW : BlockViewTransition.Type.HIDE, observation.type());
            assertEquals(13L, observation.modeToken());
            assertEquals(5, observation.worldEpoch());
        }
    }

    @Test
    void blockMetadataChangeSplitsPendingBatch() {
        PackedBlockTransitionQueue queue = new PackedBlockTransitionQueue();
        TrackedTileEntity<?> first = tileEntity();
        TrackedTileEntity<?> second = tileEntity();
        TrackedTileEntity<?> third = tileEntity();
        queue.add(BlockViewTransition.Type.HIDE, first, 3L, 2);
        queue.add(BlockViewTransition.Type.SHOW, second, 3L, 2);
        queue.add(BlockViewTransition.Type.SHOW, third, 5L, 2);
        queue.flushPendingTransitions();

        List<BlockObservation> observed = new ArrayList<>();
        queue.drainTransitions((type, tileEntity, modeToken, worldEpoch) ->
                observed.add(new BlockObservation(type, tileEntity, modeToken, worldEpoch)));

        assertEquals(List.of(3L, 3L, 5L), observed.stream().map(BlockObservation::modeToken).toList());
        assertEquals(List.of(2, 2, 2), observed.stream().map(BlockObservation::worldEpoch).toList());
    }

    @Test
    void clearDiscardsPublishedEntries() {
        PackedEntityTransitionQueue entityQueue = new PackedEntityTransitionQueue();
        entityQueue.add(EntityViewTransition.Type.SHOW, entity(), 1);
        entityQueue.flushPendingTransitions();
        entityQueue.clearPublishedTransitions();
        assertFalse(entityQueue.hasPendingTransitions());
        entityQueue.drainTransitions((type, entity, worldEpoch) -> {
            throw new AssertionError("cleared entity transition was drained");
        });

        for (int index = 0; index < 8; index++) {
            entityQueue.add(EntityViewTransition.Type.SHOW, entity(), 1);
        }
        entityQueue.clearPublishedTransitions();
        assertFalse(entityQueue.hasPendingTransitions());

        PackedBlockTransitionQueue blockQueue = new PackedBlockTransitionQueue();
        blockQueue.add(BlockViewTransition.Type.HIDE, tileEntity(), 1L, 1);
        blockQueue.flushPendingTransitions();
        blockQueue.clearPublishedTransitions();
        assertFalse(blockQueue.hasPendingTransitions());
        blockQueue.drainTransitions((type, tileEntity, modeToken, worldEpoch) -> {
            throw new AssertionError("cleared block transition was drained");
        });
    }

    @Test
    void entityConsumerClearPreservesPendingBatchMetadata() {
        PackedEntityTransitionQueue queue = new PackedEntityTransitionQueue();
        TrackedEntity<?> published = entity();
        TrackedEntity<?> pending = entity();
        queue.add(EntityViewTransition.Type.SHOW, published, 3);
        queue.flushPendingTransitions();
        queue.add(EntityViewTransition.Type.HIDE, pending, 17);

        queue.clearPublishedTransitions();
        assertFalse(queue.hasPendingTransitions());

        queue.flushPendingTransitions();
        List<EntityObservation> observed = new ArrayList<>();
        queue.drainTransitions((type, entity, worldEpoch) -> observed.add(new EntityObservation(type, entity, worldEpoch)));

        assertEquals(List.of(pending), observed.stream().map(EntityObservation::entity).toList());
        assertEquals(List.of(17), observed.stream().map(EntityObservation::worldEpoch).toList());
    }

    @Test
    void blockConsumerClearPreservesPendingBatchMetadata() {
        PackedBlockTransitionQueue queue = new PackedBlockTransitionQueue();
        TrackedTileEntity<?> published = tileEntity();
        TrackedTileEntity<?> pending = tileEntity();
        queue.add(BlockViewTransition.Type.SHOW, published, 2L, 3);
        queue.flushPendingTransitions();
        queue.add(BlockViewTransition.Type.HIDE, pending, 19L, 23);

        queue.clearPublishedTransitions();
        assertFalse(queue.hasPendingTransitions());

        queue.flushPendingTransitions();
        List<BlockObservation> observed = new ArrayList<>();
        queue.drainTransitions((type, tileEntity, modeToken, worldEpoch) ->
                observed.add(new BlockObservation(type, tileEntity, modeToken, worldEpoch)));

        assertEquals(List.of(pending), observed.stream().map(BlockObservation::tileEntity).toList());
        assertEquals(List.of(19L), observed.stream().map(BlockObservation::modeToken).toList());
        assertEquals(List.of(23), observed.stream().map(BlockObservation::worldEpoch).toList());
    }

    @Test
    void entityAppendDoesNotWaitForConsumerCallback() throws Exception {
        PackedEntityTransitionQueue queue = new PackedEntityTransitionQueue();
        queue.add(EntityViewTransition.Type.SHOW, entity(), 1);
        queue.flushPendingTransitions();
        CountDownLatch callbackStarted = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        CountDownLatch appendCompleted = new CountDownLatch(1);
        CountDownLatch drainCompleted = new CountDownLatch(1);

        Thread drainer = Thread.startVirtualThread(() -> {
            queue.drainTransitions((type, queuedEntity, worldEpoch) -> {
                callbackStarted.countDown();
                try {
                    releaseCallback.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    throw new AssertionError(exception);
                }
            });
            drainCompleted.countDown();
        });

        assertTrue(callbackStarted.await(5, TimeUnit.SECONDS));
        Thread producer = Thread.startVirtualThread(() -> {
            queue.add(EntityViewTransition.Type.HIDE, entity(), 1);
            queue.flushPendingTransitions();
            appendCompleted.countDown();
        });
        assertTrue(appendCompleted.await(5, TimeUnit.SECONDS));
        releaseCallback.countDown();
        assertTrue(drainCompleted.await(5, TimeUnit.SECONDS));
        producer.join();
        drainer.join();
    }

    @Test
    void callbackFailureHasAtMostOnceClaimedEntrySemantics() {
        PackedEntityTransitionQueue queue = new PackedEntityTransitionQueue();
        for (int index = 0; index < 3; index++) {
            queue.add(EntityViewTransition.Type.SHOW, entity(), 1);
        }
        queue.flushPendingTransitions();

        int[] callbacks = {0};
        assertThrows(IllegalStateException.class, () -> queue.drainTransitions((type, entity, worldEpoch) -> {
            callbacks[0]++;
            throw new IllegalStateException("test callback failure");
        }));
        assertEquals(1, callbacks[0]);

        queue.add(EntityViewTransition.Type.HIDE, entity(), 1);
        queue.flushPendingTransitions();
        assertTrue(queue.hasPendingTransitions());
    }

    @Test
    void entityProducerCanMigrateBetweenThreads() throws Exception {
        PackedEntityTransitionQueue queue = new PackedEntityTransitionQueue();
        TrackedEntity<?> first = entity();
        TrackedEntity<?> second = entity();

        Thread firstProducer = Thread.startVirtualThread(() -> {
            queue.add(EntityViewTransition.Type.SHOW, first, 1);
            queue.flushPendingTransitions();
        });
        firstProducer.join();

        Thread secondProducer = Thread.startVirtualThread(() -> {
            queue.add(EntityViewTransition.Type.HIDE, second, 1);
            queue.flushPendingTransitions();
        });
        secondProducer.join();

        List<TrackedEntity<?>> observed = new ArrayList<>();
        queue.drainTransitions((type, entity, worldEpoch) -> observed.add(entity));
        assertEquals(List.of(first, second), observed);
    }

    private static TrackedEntity<?> entity() {
        return proxy(TrackedEntity.class);
    }

    private static TrackedTileEntity<?> tileEntity() {
        return proxy(TrackedTileEntity.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object, method, args) -> {
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == byte.class) return (byte) 0;
            if (method.getReturnType() == short.class) return (short) 0;
            if (method.getReturnType() == int.class) return 0;
            if (method.getReturnType() == long.class) return 0L;
            if (method.getReturnType() == float.class) return 0.0f;
            if (method.getReturnType() == double.class) return 0.0d;
            if (method.getReturnType() == char.class) return (char) 0;
            return null;
        });
    }

    private record EntityObservation(EntityViewTransition.Type type, TrackedEntity<?> entity, int worldEpoch) {
    }

    private record BlockObservation(BlockViewTransition.Type type, TrackedTileEntity<?> tileEntity, long modeToken,
            int worldEpoch) {
    }
}
