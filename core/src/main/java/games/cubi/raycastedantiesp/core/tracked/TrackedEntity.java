/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.tracked;

import games.cubi.locatables.api.MutableFloatingSpatial;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Per-player platform-independent representation of an entity
 */
public interface TrackedEntity<PacketReplayData> extends MutableFloatingSpatial {

    int entityID();

    UUID entityUUID();

    boolean visible();
    TrackedEntity<?> setVisible(boolean visible);

    boolean glowing();

    boolean sneaking();

    int lastChecked();
    TrackedEntity<?> setLastChecked(int lastChecked);

    /** Packet-thread-only client state. */
    boolean clientVisible();
    TrackedEntity<?> setClientVisible(boolean clientVisible);

    boolean isSelfEntity();

    /** Packet-thread-only replay state. */
    float yaw();
    TrackedEntity<?> setYaw(float yaw);

    /** Packet-thread-only replay state. */
    float pitch();
    TrackedEntity<?> setPitch(float pitch);

    /** Packet-thread-only replay state. */
    float headYaw();
    TrackedEntity<?> setHeadYaw(float headYaw);

    /** Packet-thread-only replay state. */
    double velocityX();
    double velocityY();
    double velocityZ();
    TrackedEntity<?> setVelocity(double velocityX, double velocityY, double velocityZ);

    /** Packet-thread-only replay state. */
    boolean onGround();
    TrackedEntity<?> setOnGround(boolean onGround);

    /** Packet-thread-only replay state. */
    int entityData();
    TrackedEntity<?> setEntityData(int entityData);

    int[] passengerIDs();
    TrackedEntity<?> setPassengerIDs(int[] passengerIDs);

    void setVehicleID(int vehicleID);
    int vehicleID();

    /** Packet-thread-only holder-side relationship state. */
    void addLeashedEntity(int leashedEntityID);
    void removeLeashedEntity(int leashedEntityID);
    int@Nullable[] leashedEntityIDsOrNull();
    int leashingEntity();
    void setLeashingEntity(int leashingEntityID);

    /** Packet-thread-only cached packet state. */
    PacketReplayData packetReplayData();
    TrackedEntity<?> setPacketReplayData(PacketReplayData packetReplayData);

    float getYOffset();

    /**
     * For use when the player disconnects, clears all data.
     */
    void clear();

    default <T> T cast() {
        return (T) this;
    }
}
