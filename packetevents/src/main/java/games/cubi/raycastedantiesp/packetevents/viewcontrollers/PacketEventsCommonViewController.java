package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.function.IntSupplier;

public class PacketEventsCommonViewController {
    private static PacketEventsCommonViewController INSTANCE;
    private final  IntSupplier currentTickSupplier;
    public final boolean v_1_21_5_orAbove = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_5);

    private PacketEventsCommonViewController(IntSupplier currentTick) {
        this.currentTickSupplier = currentTick;
    }

    public static PacketEventsCommonViewController get(IntSupplier currentTick) {
        if (INSTANCE == null) {
            INSTANCE = new PacketEventsCommonViewController(currentTick);
        }
        return INSTANCE;
    }

    public void writeIfPresent(User viewer, PacketWrapper<?> packet) {
        if (viewer == null || packet == null) {
            return;
        }

        viewer.writePacketSilently(packet);
    }
}
