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
     * @return the first entry in the linked list of tasks which does not need to be evicted yet, or null if all tasks should be evicted.
     */
    default FutureNettyTask evictUntilThis(int currentTick) {
        if (!thisShouldBeEvicted(currentTick)) {
            return this;
        }
        Logger.warning("A task was evicted from the Netty task queue due to being too old! This should not happen under normal circumstances and may indicate a problem with the system being overloaded or tasks taking too long to execute. It was a " + getClass().getSimpleName(), 3, FutureNettyTask.class);
        if (hasNextLinkedTask()) {
            evictUntilThis(currentTick);
        }
        return null;
    }

    default void runAllLinkedTasks() {
        run();
        if (hasNextLinkedTask()) {
            assert getNext() != null;
            getNext().runAllLinkedTasks();
        }
    }

    /**
     * Allows linking of tasks into lists for each entity, so that when an entity is processed, all pending tasks for that entity can be processed at once.
     * @return the next task in the list, or null if this is the end of the list. Order of tasks is guaranteed to be in ascending order of submitted tick.
     */
    @Nullable FutureNettyTask getNext();

    boolean hasNextLinkedTask();
}
