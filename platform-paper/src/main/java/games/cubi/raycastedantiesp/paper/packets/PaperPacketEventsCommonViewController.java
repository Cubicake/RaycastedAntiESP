package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.protocol.player.User;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsCommonViewController;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.UUID;
import java.util.function.IntSupplier;

public final class PaperPacketEventsCommonViewController extends PacketEventsCommonViewController implements Listener {
    /** NEVER mutate a published map instance; replace it with a copied map. **/
    private volatile Object2ObjectArrayMap<String, UUID> worldIdByWorldName = new Object2ObjectArrayMap<>();

    public PaperPacketEventsCommonViewController(IntSupplier currentTickSupplier) {
        super(currentTickSupplier);
        Bukkit.getWorlds().forEach(this::registerWorld);
        Bukkit.getPluginManager().registerEvents(this, RaycastedAntiESP.get());
    }

    @Override
    public UUID resolveWorldUUID(User user) {
        if (user.getDimensionType() == null || user.getDimensionType().getName() == null) {
            return null;
        }
        return resolveWorldUUID(user.getDimensionType().getName().toString());
    }

    @Override
    public UUID resolveWorldUUID(String worldName) {
        if (worldName == null) {
            return null;
        }
        return worldIdByWorldName.get(worldName);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        registerWorld(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        unregisterWorld(event.getWorld());
    }

    private synchronized void registerWorld(World world) {
        Object2ObjectArrayMap<String, UUID> updatedWorldIds = new Object2ObjectArrayMap<>(worldIdByWorldName);
        updatedWorldIds.put(world.getKey().toString(), world.getUID());
        worldIdByWorldName = updatedWorldIds;
    }

    private synchronized void unregisterWorld(World world) {
        Object2ObjectArrayMap<String, UUID> updatedWorldIds = new Object2ObjectArrayMap<>(worldIdByWorldName);
        updatedWorldIds.remove(world.getKey().toString());
        worldIdByWorldName = updatedWorldIds;
    }
}
