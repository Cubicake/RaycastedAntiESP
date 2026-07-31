/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.utils;

import games.cubi.raycastedantiesp.core.utils.VarHandler;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public class FoliaTicker implements IntSupplier {
    private volatile int currentTick; private static final VarHandle CURRENT_TICK = VarHandler.get(FoliaTicker.class, "currentTick", int.class);

    public FoliaTicker() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(RaycastedAntiESP.get(), this::increment, 1L, 1L); //Is this guaranteed to be a specific thread?
    }

    private void increment(ScheduledTask scheduledTask) {
        CURRENT_TICK.getAndAdd(this, 1);
    }

    @Override
    public int getAsInt() {
        return (int) CURRENT_TICK.getOpaque(this);
    }
}
