/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper;

import de.oliver.fancyholograms.api.events.*;
import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancynpcs.api.events.NpcCreateEvent;
import de.oliver.fancynpcs.api.events.NpcModifyEvent;
import de.oliver.fancynpcs.api.events.NpcSpawnEvent;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.entity.EntityBypassRegistry;
import games.cubi.raycastedantiesp.paper.utils.PaperListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

// Entity remove events aren't listened to because the event names are a bit confusing to me and I'm unclear when each event is called.
// This should not be an issue as NPCs and Holograms are typically static entities which are not spawned or unspawned.
public class FancyCompatibility {
    FancyCompatibility() {
        try {
            new NPCs();
            Logger.info("Successfully initiated FancyNpcs compatibility layer",5);
        } catch (Exception ignored) {}

        try {
            new Holograms();
            Logger.info("Successfully initiated FancyHolograms compatibility layer",5);
        } catch (Exception ignored) {}
    }

    static class NPCs extends PaperListener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void npcLoad(NpcSpawnEvent event) {
            EntityBypassRegistry.addEntity(event.getNpc().getEntityId());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void npcLoad(NpcCreateEvent event) {
            EntityBypassRegistry.addEntity(event.getNpc().getEntityId());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void npcLoad(NpcModifyEvent event) {
            EntityBypassRegistry.addEntity(event.getNpc().getEntityId());
        }
    }

    static class Holograms extends PaperListener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onHologramCreate(HologramCreateEvent event) {
            add(event.getHologram());
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onHologramShow(HologramShowEvent event) {
            add(event.getHologram());
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onHologramsLoaded(HologramsLoadedEvent event) {
            event.getManager().forEach(this::add);
        }

        private void add(Hologram hologram) {
            EntityBypassRegistry.addEntity(hologram.getEntityId());
        }
    }
}
