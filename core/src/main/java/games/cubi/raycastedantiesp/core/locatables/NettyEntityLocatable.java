package games.cubi.raycastedantiesp.core.locatables;

import games.cubi.locatables.ImmutableLocatable;
import games.cubi.locatables.MutableLocatable;
import games.cubi.locatables.implementations.ImmutableLocatableImpl;
import games.cubi.locatables.implementations.MutableLocatableImpl;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.Clearable;
import games.cubi.raycastedantiesp.core.utils.PrimitiveIntArrayList;
import games.cubi.raycastedantiesp.core.utils.IntArrayListMarker;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Designed for use with netty-based systems, where entity data updates only ever come from one thread, but reads may come from multiple threads. This is however not enforced, and must be kept in mind when using this class.
 * A representation of an entity for a specific player.
 */
public abstract class NettyEntityLocatable<EntityType, PacketReplayData extends Clearable> implements EntityLocatable<EntityType, PacketReplayData> {
    // immutable fields
    private final int entityID;
    private final UUID entityUUID;
    private final boolean isSelfEntity;
    private final EntityType entityType;
    private final PlayerData owningPlayer;

    // Netty mutatable fields. Should NEVER be mutated from the engine thread, but reads are fine.
    private volatile UUID world;
    private volatile double x, y, z;
    private volatile float yaw;
    private volatile float pitch;
    private volatile float headYaw;
    private volatile double velocityX;
    private volatile double velocityY;
    private volatile double velocityZ;
    private volatile boolean onGround = true;
    private volatile int@IntArrayListMarker[] leashedIDs;
    private volatile int leasherID = NO_LEASHER;
    private volatile int[] passengerIDs;
    private volatile int vehicleID = NO_VEHICLE;


    private volatile int entityData;
    private volatile PacketReplayData packetReplayData; // For use storing the packets we can't be bothered to directly store, with all cached packets being sent back out to the client when entity is visible again.

    // mutatable by several threads (engine and netty), may need to investigate atomic updates for thread safety
    private volatile int lastChecked;
    private volatile boolean clientVisible = true;

    // engine thread mutable, reads from netty and engine.
    private volatile boolean visible;

    public NettyEntityLocatable(PlayerData owningPlayer, UUID world, double x, double y, double z, int entityID, UUID entityUUID, boolean isSelfEntity, EntityType entityType, boolean visible) {
        this.world = world;
        this.x = x; this.y = y; this.z = z;

        this.entityID = entityID;
        this.entityUUID = entityUUID;
        this.isSelfEntity = isSelfEntity;
        this.entityType = entityType;

        this.visible = visible;

        this.owningPlayer = owningPlayer;
    }

    // For creating the self entity, where we don't have access to the world or position. Always visible.
    protected NettyEntityLocatable(PlayerData selfData, int selfPlayerID, UUID ownUUID) {
        entityID = selfPlayerID;
        entityUUID = ownUUID;
        owningPlayer = selfData;
        isSelfEntity = true;
        entityType = null;
        clientVisible = true;
        visible = true;
    }

    @Override
    public LocatableType getType() {
        return LocatableType.NettyEntity;
    }

    @Override
    public int entityID() {
        return entityID;
    }

    @Override
    public UUID entityUUID() {
        return entityUUID;
    }

    @Override
    public boolean visible() {
        return visible;
    }

    /**
     * Wherever possible, effort should be made to only call this from the engine thread, for thread safety reasons.
     */
    @Override
    public EntityLocatable<?, ?> setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public int lastChecked() {
        return lastChecked;
    }
// todo it may be that this is only ever set by the engine thread? If so, just volatile annotation may be safe enough, as no two engine threads should update a single player at the same time (add guard lock for this)
    @Override
    public EntityLocatable<?, ?> setLastChecked(int lastChecked) {
        this.lastChecked = lastChecked;
        return this;
    }

    @Override
    public boolean clientVisible() {
        return clientVisible;
    }

    @Override
    public EntityLocatable<?, ?> setClientVisible(boolean clientVisible) {
        this.clientVisible = clientVisible;
        return this;
    }

    @Override
    public boolean isSelfEntity() {
        return isSelfEntity;
    }

    @Override
    public float yaw() {
        return yaw;
    }

    /**
     * THIS METHOD IS ONLY SAFE TO CALL FROM THE PLAYER's NETTY THREAD
     */
    @Override
    public EntityLocatable<?, ?> setYaw(float yaw) {
        this.yaw = yaw;
        return this;
    }

    @Override
    public float pitch() {
        return pitch;
    }

    /**
     * THIS METHOD IS ONLY SAFE TO CALL FROM THE PLAYER's NETTY THREAD
     */
    @Override
    public EntityLocatable<?, ?> setPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    @Override
    public float headYaw() {
        return headYaw;
    }

    /**
     * THIS METHOD IS ONLY SAFE TO CALL FROM THE PLAYER's NETTY THREAD
     */
    @Override
    public EntityLocatable<?, ?> setHeadYaw(float headYaw) {
        this.headYaw = headYaw;
        return this;
    }

    @Override
    public double velocityX() {
        return velocityX;
    }

    @Override
    public double velocityY() {
        return velocityY;
    }

    @Override
    public double velocityZ() {
        return velocityZ;
    }

    /**
     * THIS METHOD IS ONLY SAFE TO CALL FROM THE PLAYER's NETTY THREAD
     */
    @Override
    public EntityLocatable<?, ?> setVelocity(double velocityX, double velocityY, double velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        return this;
    }

    @Override
    public boolean onGround() {
        return onGround;
    }

    /**
     * THIS METHOD IS ONLY SAFE TO CALL FROM THE PLAYER's NETTY THREAD
     */
    @Override
    public EntityLocatable<?, ?> setOnGround(boolean onGround) {
        this.onGround = onGround;
        return this;
    }

    @Override
    public EntityType entityType() {
        return entityType;
    }

    @Override
    public int entityData() {
        return entityData;
    }

    @Override
    public EntityLocatable<?, ?> setEntityData(int entityData) {
        this.entityData = entityData;
        return this;
    }

    @Override
    public int[] passengerIDs() {
        return passengerIDs == null ? null : passengerIDs.clone();
    }

    @Override
    public EntityLocatable<?, ?> setPassengerIDs(int[] passengerIDs) {
        this.passengerIDs = passengerIDs == null ? new int[0] : passengerIDs.clone();
        return this;
    }

    @Override
    public void setVehicleID(int vehicleID) {
        this.vehicleID = vehicleID;
    }

    @Override
    public int vehicleID() {
        return vehicleID;
    }

    @Override
    public void addLeashedEntity(int leashedEntityID) {
        leashedIDs = PrimitiveIntArrayList.add(leashedIDs, leashedEntityID);
    }

    @Override
    public void removeLeashedEntity(int leashedEntityID) {
        leashedIDs = PrimitiveIntArrayList.remove(leashedIDs, leashedEntityID);
    }

    public int@Nullable[] leashedEntityIDsOrNull() {
        return PrimitiveIntArrayList.getCopyOrNull(leashedIDs);
    }

    public static final int NO_VEHICLE = -1;
    public static final int NO_LEASHER = -2;

    @Override
    public int leashingEntity() {
        return leasherID;
    }

    @Override
    public void setLeashingEntity(int leashingEntityID) {
        this.leasherID = leashingEntityID;
    }

    @Override
    public PacketReplayData packetReplayData() {
        return packetReplayData;
    }

    @Override
    public EntityLocatable<?, ?> setPacketReplayData(PacketReplayData packetReplayData) {
        this.packetReplayData = packetReplayData;
        return this;
    }

    @Override
    public ImmutableLocatable getOffsetEntityLocation() {
        return new ImmutableLocatableImpl(world, x, y + 0.5, z); //todo: move away from hardcoded offset
    }

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double z() {
        return z;
    }

    @Override
    public UUID world() {
        return world;
    }

    @Override
    public MutableLocatable clonePlainAndCentreIfBlockLocation() {
        return new MutableLocatableImpl(world, x, y, z);
    }

    @Override
    public MutableLocatable setX(double x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableLocatable setY(double y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableLocatable setZ(double z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableLocatable setX(int x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableLocatable setY(int y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableLocatable setZ(int z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableLocatable setWorld(UUID world) {
        this.world = world;
        return this;
    }

    @Override
    public MutableLocatable set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updatePassengerPositions();
        return this;
    }

    @Override
    public MutableLocatable set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updatePassengerPositions();
        return this;
    }

    @Override
    public MutableLocatable add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        updatePassengerPositions();
        return this;
    }

    // This does not correctly set the passenger position, as the passenger is above the vehicle. It is however close enough to minimise annoying interpolation on show.
    private void updatePassengerPositions() {
        if (passengerIDs == null || passengerIDs.length == 0) return;
        for (int passengerID : passengerIDs) {
            NettyEntityLocatable<?, ?> passenger = owningPlayer.entityFromID(passengerID);
            if (passenger != null) {
                passenger.setVehicleID(entityID);
                passenger.setWorld(world);
                passenger.setX(x);
                passenger.setY(y);
                passenger.setZ(z);
                passenger.updatePassengerPositions();
            }
        }
    }

    @Override
    public void clear() {
        world = null;
        if (packetReplayData != null) packetReplayData.clear();
        packetReplayData = null;

        vehicleID = NO_VEHICLE;
        passengerIDs = null;
        leashedIDs = null;
        leasherID = NO_LEASHER;
    }

    @Override
    public boolean equals(Object other) {
        return isEqualTo(other);
    }

    @Override
    public int hashCode() {
        return makeHash();
    }

    @Override
    public String toString() {
        return "NettyEntityLocatable{" +
                "entityID=" + entityID +
                ", entityUUID=" + entityUUID +
                ", isSelfEntity=" + isSelfEntity +
                ", entityType=" + entityType +
                ", world=" + world +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", headYaw=" + headYaw +
                ", velocityX=" + velocityX +
                ", velocityY=" + velocityY +
                ", velocityZ=" + velocityZ +
                ", onGround=" + onGround +
                ", leashedIDs=" + (leashedIDs == null ? null : PrimitiveIntArrayList.toString(leashedIDs)) +
                ", leasherID=" + leasherID +
                ", passengerIDs=" + (passengerIDs == null ? null : PrimitiveIntArrayList.toString(passengerIDs)) +
                ", vehicleID=" + vehicleID +
                ", entityData=" + entityData +
                '}';
    }
}
