package games.cubi.raycastedantiesp.core.utils;

import games.cubi.logs.Logger;
import org.jetbrains.annotations.Nullable;

public interface FutureNettyTask extends Runnable {
    int TICKS_BEFORE_EVICTION = 3;

    int getSubmittedTick();

    default boolean thisShouldBeEvicted(int currentTick) {
        return ( currentTick - getSubmittedTick() ) >= TICKS_BEFORE_EVICTION;
    }

    /**
     * Appends the provided task to the end of this linked task chain.
     */
    default void appendLinkedTask(FutureNettyTask task) {
        FutureNettyTask current = this;
        while (current.getNext() != null) {
            current = current.getNext();
        }
        current.setNext(task);
    }

    /**
     * @return the first task in this chain that should not be evicted yet, or null if all tasks should be evicted.
     */
    @Nullable
    default FutureNettyTask trimExpiredTasks(int currentTick) {
        FutureNettyTask current = this;
        while (current != null && current.thisShouldBeEvicted(currentTick)) {
            Logger.warning("A task was evicted from the Netty task queue due to being too old! This should not happen under normal circumstances and may indicate a problem with the system being overloaded or tasks taking too long to execute. Current tick=" + currentTick + " Task=" + current, 3, FutureNettyTask.class);
            FutureNettyTask next = current.getNext();
            current.setNext(null);
            current = next;
        }
        return current;
    }

    default void runLinkedTasks() {
        FutureNettyTask current = this;
        while (current != null) {
            FutureNettyTask next = current.getNext();
            current.setNext(null);
            try {
                current.run();
            } catch (Exception e) {
                Logger.error("Error while running future netty task " + current, e, 3, FutureNettyTask.class);
            }
            current = next;
        }
    }

    /**
     * Allows linking of tasks into lists for each entity, so that when an entity is processed, all pending tasks for that entity can be processed at once.
     * @return the next task in the list, or null if this is the end of the list. Order of tasks is guaranteed to be in ascending order of submitted tick.
     */
    @Nullable FutureNettyTask getNext();

    /**
     * Sets the next task in the linked task chain.
     * Passing null is allowed and is used when detaching already-linked tasks before eviction or execution.
     * <p>
     * This should generally not be called directly, use {@link #appendLinkedTask} to link tasks together and let the system handle detaching when necessary.
     */
    void setNext(@Nullable FutureNettyTask next);
}
