package games.cubi.raycastedantiesp.core.utils;

import games.cubi.logs.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFutureNettyTask implements FutureNettyTask {
    private final int submittedTick;
    private @Nullable FutureNettyTask next;

    protected AbstractFutureNettyTask(int submittedTick) {
        this.submittedTick = submittedTick;
    }

    @Override
    public final int getSubmittedTick() {
        return submittedTick;
    }

    @Override
    public final @Nullable FutureNettyTask getNext() {
        return next;
    }

    @Override
    public final void setNext(@Nullable FutureNettyTask next) {
        if (next == this) {
            Logger.errorAndReturn(new IllegalArgumentException("A FutureNettyTask cannot link to itself: " + this), 1, AbstractFutureNettyTask.class);
        }
        if (this.next != null && next != null) {
            Logger.errorAndReturn(new IllegalStateException("FutureNettyTask already has a next task. Existing next=" + this.next + ", attempted next=" + next + ", task=" + this), 1, AbstractFutureNettyTask.class);
        }
        this.next = next;
    }
}
