package games.cubi.raycastedantiesp.core.utils;

/**
 * Classes where specific methods must be called from a single thread can extend this class and call {@link #guardThread()}
 * at the start of those methods to ensure that they are only called from the permitted thread.
 */
public abstract class SingleThreadedGuard {
    private final Thread permittedThread;

    protected SingleThreadedGuard(Thread thread) {
        permittedThread = thread;
    }

    protected void guardThread() {
        if (Thread.currentThread() != permittedThread) {
            throw new IllegalStateException("Method called from wrong thread. Expected: " + permittedThread + ", actual: " + Thread.currentThread());
        }
    }
}
