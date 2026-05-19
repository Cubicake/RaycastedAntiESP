package games.cubi.raycastedantiesp.paper;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.paper.engine.PaperSimpleEngine;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.paper.utils.PaperListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.function.IntSupplier;

import static games.cubi.raycastedantiesp.paper.UpdateChecker.checkForUpdates;

public class EventListener extends PaperListener {
    private final RaycastedAntiESP plugin;
    private final PaperSimpleEngine engine;
    private final IntSupplier currentTickSupplier;

    private static EventListener instance = null;

    private EventListener(RaycastedAntiESP plugin, PaperSimpleEngine engine, IntSupplier currentTickSupplier) {
        this.plugin = plugin;
        this.engine = engine;
        this.currentTickSupplier = currentTickSupplier;
    }

    public static EventListener initialise(RaycastedAntiESP plugin, PaperSimpleEngine engine, IntSupplier currentTickSupplier) {
        if (instance == null) {
            instance = new EventListener(plugin, engine, currentTickSupplier);
        }
        return instance;
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        PlayerRegistry.getInstance().unregisterPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST) //Runs first
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (player.hasPermission("raycastedantiesp.updatecheck")) { //todo: centralise permission strings to prevent issues when perm names are changed
            checkForUpdates(plugin, player);
        }

        boolean hasBypassPermission = player.hasPermission("raycastedantiesp.bypass");
        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());

        if (playerData == null) {
            Logger.warning("Failed to load player data for " + player.getName() + " (" + player.getUniqueId() + "). Attempting to reconstruct.", 3, EventListener.class);
            playerData = PlayerRegistry.getInstance().registerAndGetPlayerIfAbsent(player.getUniqueId(), hasBypassPermission, currentTickSupplier.getAsInt());
        }
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
        Bukkit.getAsyncScheduler().runNow(plugin, task -> engine.tick());
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
