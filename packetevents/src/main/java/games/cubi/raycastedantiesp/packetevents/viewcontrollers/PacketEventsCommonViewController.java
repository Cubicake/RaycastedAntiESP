package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import games.cubi.locatables.Locatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;

import java.util.Objects;
import java.util.UUID;
import java.util.function.IntSupplier;

public abstract class PacketEventsCommonViewController {
    private static PacketEventsCommonViewController INSTANCE;
    private final  IntSupplier currentTickSupplier;
    public final boolean v_1_21_5_orAbove = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_5);

    protected PacketEventsCommonViewController(IntSupplier currentTick) {
        this.currentTickSupplier = currentTick;
    }

    public static void initialise(PacketEventsCommonViewController instance) {
        INSTANCE = Objects.requireNonNull(instance);
    }

    public static PacketEventsCommonViewController get(IntSupplier currentTick) {
        if (INSTANCE == null) {
            throw new IllegalStateException("PacketEventsCommonViewController has not been initialised.");
        }
        return INSTANCE;
    }

    public abstract UUID resolveWorldUUID(User user);

    public abstract UUID resolveWorldUUID(String worldName);

    public UUID resolvePacketWorld(PlayerData playerData, User user) {
        String trackedWorldName = playerData.nettyData().getCurrentWorldName();
        if (trackedWorldName != null) {
            return resolveWorldUUID(trackedWorldName);
        }

        Locatable ownLocation = playerData.ownLocation();
        if (ownLocation != null && ownLocation.world() != null) {
            return ownLocation.world();
        }
        return resolveWorldUUID(user);
    }

    public void writeIfPresent(User viewer, PacketWrapper<?> packet) {
        if (viewer == null || packet == null) {
            return;
        }

        viewer.writePacketSilently(packet);
    }
}
