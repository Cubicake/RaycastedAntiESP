/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.utils;

import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public final class PaperScheduler {
    private PaperScheduler() {}

    public static void runAsync(RaycastedAntiESP plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, ignored -> task.run());
    }

    public static void runGlobal(RaycastedAntiESP plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
    }

    /**
     * Runs audience work on the owning entity region where one exists, otherwise on the global region.
     */
    public static void runForAudience(RaycastedAntiESP plugin, CommandSender audience, Runnable task) {
        if (audience instanceof Entity entity) {
            entity.getScheduler().run(plugin, ignored -> task.run(), null);
            return;
        }
        runGlobal(plugin, task);
    }
}
