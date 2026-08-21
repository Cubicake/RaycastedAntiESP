/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

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
