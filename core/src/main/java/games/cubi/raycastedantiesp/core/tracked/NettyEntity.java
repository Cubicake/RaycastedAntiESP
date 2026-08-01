/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.tracked;

import games.cubi.locatables.api.MutableFloatingSpatial;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.Clearable;
import games.cubi.raycastedantiesp.core.utils.PrimitiveIntArrayList;
import games.cubi.raycastedantiesp.core.utils.IntArrayListMarker;
import games.cubi.raycastedantiesp.core.utils.VarHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.VarHandle;
import java.util.UUID;

/**
 * Designed for use with netty-based systems, where entity data updates only ever come from one thread, but reads may come from multiple threads. This is however not enforced, and must be kept in mind when using this class.
 * A representation of an entity for a specific player.
 */
public abstract class NettyEntity<PacketReplayData extends Clearable> implements TrackedEntity<PacketReplayData> {
    // immutable fields
    private final int entityID; // Note that, while rare, entity ids will be negative once the entity id counter overflows.
    private final UUID entityUUID;
    private final boolean isSelfEntity;
    private final int entityType;
    private final PlayerData owningPlayer;

    // Netty mutatable fields. Should NEVER be mutated from the engine thread, but reads are fine.
    private volatile double x; private static final VarHandle X = VarHandler.get(NettyEntity.class, "x", double.class);
    private volatile double y; private static final VarHandle Y = VarHandler.get(NettyEntity.class, "y", double.class);
    private volatile double z; private static final VarHandle Z = VarHandler.get(NettyEntity.class, "z", double.class);

    // Packet-derived state shared with the engine.
    private static final byte FLAG_SNEAKING = 1;
    private static final byte FLAG_GLOWING = 1 << 1;
    private volatile byte trackedFlags; private static final VarHandle TRACKED_FLAGS = VarHandler.get(NettyEntity.class, "trackedFlags", byte.class);

    // Packet-thread-only replay and client state.
    private float yaw;
    private float pitch;
    private float headYaw;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private boolean onGround = true;
    private int@IntArrayListMarker[] leashedIDs;

    // Netty-written attachment state shared with the engine thread.
    private volatile int leasherID = NO_LEASHER; private static final VarHandle LEASHER_ID = VarHandler.get(NettyEntity.class, "leasherID", int.class);
    private volatile int[] passengerIDs; private static final VarHandle PASSENGER_IDS = VarHandler.get(NettyEntity.class, "passengerIDs", int[].class);
    private volatile int vehicleID = NO_VEHICLE; private static final VarHandle VEHICLE_ID = VarHandler.get(NettyEntity.class, "vehicleID", int.class);


    private int entityData;
    private PacketReplayData packetReplayData; // For use storing the packets we can't be bothered to directly store, with all cached packets being sent back out to the client when entity is visible again.

    // Mutable by the engine and packet threads.
    private volatile int lastChecked; private static final VarHandle LAST_CHECKED = VarHandler.get(NettyEntity.class, "lastChecked", int.class);
    private boolean clientVisible = true;

    // engine thread mutable, reads from netty and engine.
    private volatile boolean visible;

    // Constants
    public static final int NEVER_CHECKED = Integer.MIN_VALUE;
    public static final int NO_VEHICLE = -1;
    public static final int NO_LEASHER = -2;
    public static final int SELF_ENTITY_TYPE = -29;

    public NettyEntity(PlayerData owningPlayer, double x, double y, double z, int entityID, UUID entityUUID, boolean isSelfEntity, int entityType, boolean visible) {
        X.set(this, x); Y.set(this, y); Z.set(this, z);

        this.entityID = entityID;
        this.entityUUID = entityUUID;
        this.isSelfEntity = isSelfEntity;
        this.entityType = entityType;

        this.visible = visible;

        this.owningPlayer = owningPlayer;
    }

    // For creating the self entity, where we don't have access to its position. Always visible.
    protected NettyEntity(PlayerData selfData, int selfPlayerID, UUID ownUUID) {
        entityID = selfPlayerID;
        entityUUID = ownUUID;
        owningPlayer = selfData;
        isSelfEntity = true;
        entityType = SELF_ENTITY_TYPE;
        visible = true;
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
    public TrackedEntity<?> setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public boolean glowing() {
        return (((byte) TRACKED_FLAGS.getOpaque(this)) & FLAG_GLOWING) != 0;
    }

    @Override
    public boolean sneaking() {
        return (((byte) TRACKED_FLAGS.getOpaque(this)) & FLAG_SNEAKING) != 0;
    }

    public boolean setGlowing(boolean glowing) {
        return setTrackedFlag(FLAG_GLOWING, glowing);
    }

    public boolean setSneaking(boolean sneaking) {
        return setTrackedFlag(FLAG_SNEAKING, sneaking);
    }

    private boolean setTrackedFlag(byte mask, boolean enabled) {
        byte previous = enabled
                ? (byte) TRACKED_FLAGS.getAndBitwiseOrRelease(this, mask)
                : (byte) TRACKED_FLAGS.getAndBitwiseAndRelease(this, (byte) ~mask);
        boolean changed = (previous & mask) != (enabled ? mask : 0);
        if (changed) {
            // A concurrent engine check may overwrite this marker. Hidden entities retry next tick; visible entities
            // may wait for their configured recheck or remain visible if rechecks are disabled.
            // This is acceptable and not considered an issue.
            LAST_CHECKED.setOpaque(this, NEVER_CHECKED);
        }
        return changed;
    }

    @Override
    public int lastChecked() {
        return (int) LAST_CHECKED.getOpaque(this);
    }

    @Override
    public TrackedEntity<?> setLastChecked(int lastChecked) {
        LAST_CHECKED.setOpaque(this, lastChecked);
        return this;
    }

    @Override
    public boolean clientVisible() {
        return clientVisible;
    }

    @Override
    public TrackedEntity<?> setClientVisible(boolean clientVisible) {
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
    public TrackedEntity<?> setYaw(float yaw) {
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
    public TrackedEntity<?> setPitch(float pitch) {
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
    public TrackedEntity<?> setHeadYaw(float headYaw) {
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
    public TrackedEntity<?> setVelocity(double velocityX, double velocityY, double velocityZ) {
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
    public TrackedEntity<?> setOnGround(boolean onGround) {
        this.onGround = onGround;
        return this;
    }

    public int entityType() {
        return entityType;
    }

    @Override
    public int entityData() {
        return entityData;
    }

    @Override
    public TrackedEntity<?> setEntityData(int entityData) {
        this.entityData = entityData;
        return this;
    }

    @Override
    public int[] passengerIDs() {
        int[] current = passengerIDsAcquire();
        return current == null ? null : current.clone();
    }

    @Override
    public TrackedEntity<?> setPassengerIDs(int[] passengerIDs) {
        PASSENGER_IDS.setRelease(this, passengerIDs == null ? new int[0] : passengerIDs.clone());
        return this;
    }

    @Override
    public void setVehicleID(int vehicleID) {
        VEHICLE_ID.setOpaque(this, vehicleID);
    }

    @Override
    public int vehicleID() {
        return (int) VEHICLE_ID.getOpaque(this);
    }

    @Override
    public void addLeashedEntity(int leashedEntityID) {
        if (PrimitiveIntArrayList.contains(leashedIDs, leashedEntityID)) {
            return;
        }
        leashedIDs = PrimitiveIntArrayList.add(leashedIDs, leashedEntityID);
    }

    @Override
    public void removeLeashedEntity(int leashedEntityID) {
        leashedIDs = PrimitiveIntArrayList.remove(leashedIDs, leashedEntityID);
    }

    public int@Nullable[] leashedEntityIDsOrNull() {
        return PrimitiveIntArrayList.getCopyOrNull(leashedIDs);
    }

    @Override
    public int leashingEntity() {
        return (int) LEASHER_ID.getOpaque(this);
    }

    @Override
    public void setLeashingEntity(int leashingEntityID) {
        LEASHER_ID.setOpaque(this, leashingEntityID);
    }

    @Override
    public PacketReplayData packetReplayData() {
        return packetReplayData;
    }

    @Override
    public TrackedEntity<?> setPacketReplayData(PacketReplayData packetReplayData) {
        this.packetReplayData = packetReplayData;
        return this;
    }

    @Override
    public float getYOffset() {
        return 0.5f; //todo: move away from hardcoded offset
    }

    @Override
    public double x() {
        return (double) X.getOpaque(this);
    }

    @Override
    public double y() {
        return (double) Y.getOpaque(this);
    }

    @Override
    public double z() {
        return (double) Z.getOpaque(this);
    }

    @Override
    public MutableFloatingSpatial setX(double x) {
        X.setOpaque(this, x);
        return this;
    }

    @Override
    public MutableFloatingSpatial setY(double y) {
        Y.setOpaque(this, y);
        return this;
    }

    @Override
    public MutableFloatingSpatial setZ(double z) {
        Z.setOpaque(this, z);
        return this;
    }

    @Override
    public MutableFloatingSpatial setPosition(double x, double y, double z) {
        X.setOpaque(this, x);
        Y.setOpaque(this, y);
        Z.setOpaque(this, z);
        updatePassengerPositions();
        return this;
    }

    @Override
    public MutableFloatingSpatial add(double x, double y, double z) {
        X.setOpaque(this, x() + x);
        Y.setOpaque(this, y() + y);
        Z.setOpaque(this, z() + z);
        updatePassengerPositions();
        return this;
    }

    // This does not correctly set the passenger position, as the passenger is above the vehicle. It is however close enough to minimise annoying interpolation on show.
    private void updatePassengerPositions() {
        int[] currentPassengerIDs = passengerIDsAcquire();
        if (currentPassengerIDs == null || currentPassengerIDs.length == 0) return;
        double currentX = x();
        double currentY = y();
        double currentZ = z();
        for (int passengerID : currentPassengerIDs) {
            NettyEntity<?> passenger = owningPlayer.entityFromID(passengerID);
            if (passenger != null) {
                passenger.setVehicleID(entityID);
                passenger.setX(currentX);
                passenger.setY(currentY);
                passenger.setZ(currentZ);
                passenger.updatePassengerPositions();
            }
        }
    }

    @Override
    public void clear() {
        if (packetReplayData != null) packetReplayData.clear();
        packetReplayData = null;

        TRACKED_FLAGS.setOpaque(this, (byte) 0);
        VEHICLE_ID.setOpaque(this, NO_VEHICLE);
        PASSENGER_IDS.setRelease(this, null);
        leashedIDs = null;
        LEASHER_ID.setOpaque(this, NO_LEASHER);
    }

    @Override
    public String toString() {
        return "NettyEntity{" +
                "entityID=" + entityID +
                ", entityUUID=" + entityUUID +
                ", isSelfEntity=" + isSelfEntity +
                ", entityType=" + entityType +
                ", x=" + x() +
                ", y=" + y() +
                ", z=" + z() +
                ", yaw=" + yaw() +
                ", pitch=" + pitch() +
                ", headYaw=" + headYaw() +
                ", velocityX=" + velocityX() +
                ", velocityY=" + velocityY() +
                ", velocityZ=" + velocityZ() +
                ", onGround=" + onGround() +
                ", leashedIDs=" + nullableArrayToString(leashedIDs) +
                ", leasherID=" + leashingEntity() +
                ", passengerIDs=" + nullableArrayToString(passengerIDsAcquire()) +
                ", vehicleID=" + vehicleID() +
                ", entityData=" + entityData() +
                '}';
    }

    private int[] passengerIDsAcquire() {
        return (int[]) PASSENGER_IDS.getAcquire(this);
    }

    private static String nullableArrayToString(int[] values) {
        return values == null ? null : PrimitiveIntArrayList.toString(values);
    }

}
