/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.entity.EntityBypassRegistry;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.paper.engine.PaperAsyncEngine;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.paper.utils.HackyEntityIDGuard;
import games.cubi.raycastedantiesp.paper.utils.PaperListener;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.function.IntSupplier;

import static games.cubi.raycastedantiesp.paper.UpdateChecker.checkForUpdates;

public class EventListener extends PaperListener {
    private final RaycastedAntiESP plugin;
    private final PaperAsyncEngine engine;
    private final IntSupplier currentTickSupplier;
    private final HackyEntityIDGuard entityIDGuard = new HackyEntityIDGuard();

    private static EventListener instance = null;

    private EventListener(RaycastedAntiESP plugin, PaperAsyncEngine engine, IntSupplier currentTickSupplier) {
        this.plugin = plugin;
        this.engine = engine;
        this.currentTickSupplier = currentTickSupplier;
    }

    public static EventListener initialise(RaycastedAntiESP plugin, PaperAsyncEngine engine, IntSupplier currentTickSupplier) {
        if (instance == null) {
            instance = new EventListener(plugin, engine, currentTickSupplier);
        }
        return instance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) //Paper ignoreCancelled means "do not run this method if the event is cancelled", not "run even if cancelled"
    public void onEntityRemove(EntityRemoveEvent event) {
        EntityBypassRegistry.markEntityDespawned(event.getEntity().getEntityId());
    }

    @EventHandler(priority = EventPriority.LOWEST) //Runs first
    public void onPlayerJoin(PlayerClientLoadedWorldEvent e) {
        Player player = e.getPlayer();

        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            Logger.error("Player joined before packet state was registered. Kicking player=" + player.getName() + " uuid=" + player.getUniqueId(), 1, EventListener.class);
            player.kick(MiniMessage.miniMessage().deserialize("RaycastedAntiESP failed to initialise your packet state. Please reconnect. Report this issue to the server you are playing on if you are still unable to join."));
            return;
        }

        if (player.hasPermission("raycastedantiesp.updatecheck") && (playerData.getJoinTick() - currentTickSupplier.getAsInt() < 10)) { //todo: centralise permission strings to prevent issues when perm names are changed
            checkForUpdates(plugin, player);
        }

        boolean hasBypassPermission = player.hasPermission("raycastedantiesp.bypass");
        playerData.setBypassPermission(hasBypassPermission);
        updateOwnLocation(playerData, player.getEyeLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(event.getPlayer().getUniqueId());
        if (playerData == null) return;
        updateOwnLocation(playerData, event.getPlayer().getEyeLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        updateOwnLocation(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updateOwnLocation(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        updateOwnLocation(event.getPlayer(), event.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST) //Runs first
    public void serverTickStartEvent(ServerTickStartEvent event) {
        // Capture this before async handoff so timing diagnostics can separate scheduler queueing from engine work.
        int scheduledTick = currentTickSupplier.getAsInt();
        entityIDGuard.tick(scheduledTick);
        if (!engine.markTickRunning()) {
            Logger.info("Skipped starting tick because previous tick is still running. This likely means the server is overloaded.", 6, EventListener.class);
            return;
        }
        long scheduledNanos = System.nanoTime();
        try {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> engine.tick(scheduledTick, scheduledNanos));
        } catch (RuntimeException exception) {
            engine.cancelPendingTickReservation();
            Logger.error("Failed to schedule engine tick after reserving it. Cleared the pending reservation so future ticks can continue.", exception, 2, EventListener.class);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) //Runs last
    public void serverTickStopEvent(ServerTickEndEvent event) {
    }

    private void updateOwnLocation(PlayerData playerData, Location location) {
        if (playerData == null || location == null || location.getWorld() == null) {
            return;
        }
        playerData.updateOwnLocation(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ());
    }

    private void updateOwnLocation(Player player, Location location) {
        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());
        if (playerData == null || location == null) {
            return;
        }

        Location eyeLocation = location.clone().add(0, player.getEyeHeight(), 0);
        updateOwnLocation(playerData, eyeLocation);
    }

}
