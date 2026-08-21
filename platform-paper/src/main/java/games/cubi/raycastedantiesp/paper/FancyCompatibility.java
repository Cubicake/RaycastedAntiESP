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
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

// Entity remove events aren't listened to because the event names are a bit confusing to me and I'm unclear when each event is called.
// This should not be an issue as NPCs and Holograms are typically static entities which are not spawned or unspawned.
public class FancyCompatibility {
    FancyCompatibility() {
        Logger.info("Attempting to initiate FancyNPCs and FancyHolograms compatibility layer. If you are not using those plugins, you will see an error. This is safe to ignore.", 5);
        new NPCs();
        new Holograms();
    }

    static class NPCs extends PaperListener {

        @EventHandler(priority = EventPriority.LOWEST)
        public void npcLoad(NpcSpawnEvent event) {
            int id = event.getNpc().getEntityId();
            Logger.debug("NpcSpawnEvent called, adding entity: " + id);
            EntityBypassRegistry.addEntity(id);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void npcLoad(NpcCreateEvent event) {
            int id = event.getNpc().getEntityId();
            Logger.debug("NpcCreateEvent called, adding entity: " + id);
            EntityBypassRegistry.addEntity(id);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void npcLoad(NpcModifyEvent event) {
            int id = event.getNpc().getEntityId();
            Logger.debug("NpcModifyEvent called, adding entity: " + id);
            EntityBypassRegistry.addEntity(id);
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
            int id = hologram.getEntityId();
            Logger.debug("Hologram event called, adding entity: " + id);
            EntityBypassRegistry.addEntity(id);
        }
    }
}
