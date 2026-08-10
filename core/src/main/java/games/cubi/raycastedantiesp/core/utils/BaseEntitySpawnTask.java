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
