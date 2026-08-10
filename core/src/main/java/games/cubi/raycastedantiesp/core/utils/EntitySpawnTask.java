/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import games.cubi.logs.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * For runnable tasks which should be run immediately after an entity is spawned.
 */
public interface EntitySpawnTask extends Runnable {
    int TICKS_BEFORE_EVICTION = 16;

    int getSubmittedTick();

    default boolean thisShouldBeEvicted(int currentTick) {
        return ( currentTick - getSubmittedTick() ) >= TICKS_BEFORE_EVICTION;
    }

    /**
     * Appends the provided task to the end of this linked task chain.
     */
    default void appendLinkedTask(EntitySpawnTask task) {
        EntitySpawnTask current = this;
        while (current.getNext() != null) {
            current = current.getNext();
        }
        current.setNext(task);
    }

    /**
     * @return the first task in this chain that should not be evicted yet, or null if all tasks should be evicted.
     */
    @Nullable
    default EntitySpawnTask trimExpiredTasks(int currentTick) {
        EntitySpawnTask current = this;
        while (current != null && current.thisShouldBeEvicted(currentTick)) {
            Logger.warning("A task was evicted from the Netty task queue due to being too old! This should not happen under normal circumstances and may indicate a problem with the system being overloaded or tasks taking too long to execute. Current tick=" + currentTick + " Task=" + current, 3, EntitySpawnTask.class);
            EntitySpawnTask next = current.getNext();
            current.setNext(null);
            try {
                current.run(); // perhaps the task is salvageable even if we missed the correct caller
            } catch (Exception e) {
                Logger.error("Error while running evicted future netty task " + current, e, 3, EntitySpawnTask.class);
            }
            current = next;
        }
        return current;
    }

    default void runLinkedTasks() {
        EntitySpawnTask current = this;
        while (current != null) {
            EntitySpawnTask next = current.getNext();
            current.setNext(null);
            try {
                current.run();
            } catch (Exception e) {
                Logger.error("Error while running future netty task " + current, e, 3, EntitySpawnTask.class);
            }
            current = next;
        }
    }

    /**
     * Allows linking of tasks into lists for each entity, so that when an entity is processed, all pending tasks for that entity can be processed at once.
     * @return the next task in the list, or null if this is the end of the list. Order of tasks is guaranteed to be in ascending order of submitted tick.
     */
    @Nullable EntitySpawnTask getNext();

    /**
     * Sets the next task in the linked task chain.
     * Passing null is allowed and is used when detaching already-linked tasks before eviction or execution.
     * <p>
     * This should generally not be called directly, use {@link #appendLinkedTask} to link tasks together and let the system handle detaching when necessary.
     */
    void setNext(@Nullable EntitySpawnTask next);
}
