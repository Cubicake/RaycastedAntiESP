package games.cubi.raycastedantiesp.core.locatables;

import games.cubi.locatables.ImmutableLocatable;
import games.cubi.locatables.MutableLocatable;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Per-player platform-independent representation of an entity
 */
public interface EntityLocatable<EntityType, PacketReplayData> extends MutableLocatable {

    int entityID();

    UUID entityUUID();

    boolean visible();
    EntityLocatable<?, ?> setVisible(boolean visible);

    int lastChecked();
    EntityLocatable<?, ?> setLastChecked(int lastChecked);

    boolean clientVisible();
    EntityLocatable<?, ?> setClientVisible(boolean clientVisible);

    boolean isSelfEntity();

    float yaw();
    EntityLocatable<?, ?> setYaw(float yaw);

    float pitch();
    EntityLocatable<?, ?> setPitch(float pitch);

    float headYaw();
    EntityLocatable<?, ?> setHeadYaw(float headYaw);

    double velocityX();
    double velocityY();
    double velocityZ();
    EntityLocatable<?, ?> setVelocity(double velocityX, double velocityY, double velocityZ);

    boolean onGround();
    EntityLocatable<?, ?> setOnGround(boolean onGround);

    EntityType entityType();

    int entityData();
    EntityLocatable<?, ?> setEntityData(int entityData);

    int[] passengerIDs();
    EntityLocatable<?, ?> setPassengerIDs(int[] passengerIDs);

    void setVehicleID(int vehicleID);
    int vehicleID();

    void addLeashedEntity(int leashedEntityID);
    void removeLeashedEntity(int leashedEntityID);
    int@Nullable[] leashedEntityIDsOrNull();
    int leashingEntity();
    void setLeashingEntity(int leashingEntityID);

    PacketReplayData packetReplayData();
    EntityLocatable<?, ?> setPacketReplayData(PacketReplayData packetReplayData);

    ImmutableLocatable getOffsetEntityLocation();

    /**
     * For use when the player disconnects, clears all data.
     */
    void clear();

    default <T> T cast() {
        return (T) this;
    }
}
