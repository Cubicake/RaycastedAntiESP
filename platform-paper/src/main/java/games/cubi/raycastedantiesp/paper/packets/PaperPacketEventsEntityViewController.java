package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.player.User;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;


public class PaperPacketEventsEntityViewController extends PacketEventsEntityViewController implements Listener {
    private final Map<NamespacedKey, UUID> worldIdByWorldKey = new ConcurrentHashMap<>();
    private final int stoneBlockId = SpigotConversionUtil.fromBukkitBlockData(Material.STONE.createBlockData()).getGlobalId();
    private final int deepslateBlockId = SpigotConversionUtil.fromBukkitBlockData(Material.DEEPSLATE.createBlockData()).getGlobalId();

    public PaperPacketEventsEntityViewController(IntSupplier currentTickSupplier) {
        super(currentTickSupplier);
        Bukkit.getPluginManager().registerEvents(this, RaycastedAntiESP.get());
        Bukkit.getWorlds().forEach(this::registerWorld);
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    protected UUID resolveWorldUUID(User user) {
        if (user.getDimensionType() == null || user.getDimensionType().getName() == null) {
            return null;
        }
        NamespacedKey worldKey = NamespacedKey.fromString(user.getDimensionType().getName().toString());
        return worldKey == null ? null : worldIdByWorldKey.get(worldKey);
    }

    @Override
    protected int getHiddenBlockId(int blockY) {
        return blockY > 0 ? stoneBlockId : deepslateBlockId;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        registerWorld(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        worldIdByWorldKey.remove(event.getWorld().getKey());
    }

    private void registerWorld(World world) {
        worldIdByWorldKey.put(world.getKey(), world.getUID());
    }
}
