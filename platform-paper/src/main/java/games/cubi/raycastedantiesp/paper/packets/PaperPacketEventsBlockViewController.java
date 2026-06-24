package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsBlockViewController;
import games.cubi.raycastedantiesp.paper.staging.PacketEventsPaperBlockInfoResolver;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;

import java.util.function.IntSupplier;

public class PaperPacketEventsBlockViewController extends PacketEventsBlockViewController {
    private final int stoneBlockId = SpigotConversionUtil.fromBukkitBlockData(Material.STONE.createBlockData()).getGlobalId();
    private final int deepslateBlockId = SpigotConversionUtil.fromBukkitBlockData(Material.DEEPSLATE.createBlockData()).getGlobalId();

    public PaperPacketEventsBlockViewController(IntSupplier currentTickSupplier) {
        super(new PacketEventsPaperBlockInfoResolver(), currentTickSupplier);
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);
    }

    @Override
    protected int getHiddenBlockId(int blockY) {
        return blockY > 0 ? stoneBlockId : deepslateBlockId;
    }
}
