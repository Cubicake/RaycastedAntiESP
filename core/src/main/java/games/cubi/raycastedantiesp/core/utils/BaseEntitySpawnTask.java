package games.cubi.raycastedantiesp.core.utils;

import games.cubi.logs.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class BaseEntitySpawnTask implements EntitySpawnTask {
    protected final int submittedTick;
    private @Nullable EntitySpawnTask next;

    protected BaseEntitySpawnTask(int submittedTick) {
        this.submittedTick = submittedTick;
    }

    @Override
    public final int getSubmittedTick() {
        return submittedTick;
    }

    @Override
    public final @Nullable EntitySpawnTask getNext() {
        return next;
    }

    @Override
    public final void setNext(@Nullable EntitySpawnTask next) {
        if (next == this) {
            Logger.errorAndReturn(new IllegalArgumentException("A FutureNettyTask cannot link to itself: " + this), 1, BaseEntitySpawnTask.class);
        }
        if (this.next != null && next != null) {
            Logger.errorAndReturn(new IllegalStateException("FutureNettyTask already has a next task. Existing next=" + this.next + ", attempted next=" + next + ", task=" + this), 1, BaseEntitySpawnTask.class);
        }
        this.next = next;
    }
}
