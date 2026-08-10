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
import org.bukkit.event.Listener;

public abstract class PaperListener implements Listener {
    public PaperListener() {
        Bukkit.getPluginManager().registerEvents(this, RaycastedAntiESP.get());
    }
}
