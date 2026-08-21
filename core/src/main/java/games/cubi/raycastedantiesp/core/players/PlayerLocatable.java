/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.api.FloatingLocatableEquality;
import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.MutableFloatingLocatable;
import games.cubi.locatables.api.Spatial;
import games.cubi.raycastedantiesp.core.utils.VarHandler;

import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.UUID;

/**
 * A mutable, thread-safe location and view state for a player.
 *
 * <p>Mutations are serialized because platform and packet lifecycle callbacks
 * may write from different threads. World and scalar reads use relaxed
 * per-field access: a read may combine values from adjacent updates.</p>
 * <p></p>
 * Reads of several fields are not coherently published. This is acceptable
 * since players typically move in small increments, so an old x and new z
 * still closely approximate the player's actual position. In less-frequent
 * cases such as teleports or dimension switches, raycasts may use the incorrect
 * position briefly. This does not matter, as incorrect state will be fixed by next tick's raycast.
 */
public final class PlayerLocatable implements MutableFloatingLocatable, FloatingLocatableEquality {
    private final Object writerMonitor = new Object();

    private volatile UUID world; private static final VarHandle WORLD = VarHandler.get(PlayerLocatable.class, "world", UUID.class);
    private volatile double x;  private static final VarHandle X = VarHandler.get(PlayerLocatable.class, "x", double.class);
    private volatile double y; private static final VarHandle Y = VarHandler.get(PlayerLocatable.class, "y", double.class);
    private volatile double z; private static final VarHandle Z = VarHandler.get(PlayerLocatable.class, "z", double.class);
    private volatile float yaw; private static final VarHandle YAW = VarHandler.get(PlayerLocatable.class, "yaw", float.class);
    private volatile float pitch; private static final VarHandle PITCH = VarHandler.get(PlayerLocatable.class, "pitch", float.class);
    private volatile PlayerPose pose; private static final VarHandle POSE = VarHandler.get(PlayerLocatable.class, "pose", PlayerPose.class);

    public PlayerLocatable() {
    }

    /** Initialises or replaces the complete valid player state. */
    public void initialise(UUID world, double footX, double footY, double footZ, float yaw, float pitch, PlayerPose pose) {
        validateFullState(world, footX, footY, footZ, yaw, pitch, pose);
        synchronized (writerMonitor) {
            writeFullState(world, footX, footY, footZ, yaw, pitch, pose);
        }
    }

    /** Updates the complete state for a world change. */
    public void worldChange(UUID world, double footX, double footY, double footZ, float yaw, float pitch, PlayerPose pose) {
        validateFullState(world, footX, footY, footZ, yaw, pitch, pose);
        synchronized (writerMonitor) {
            writeFullState(world, footX, footY, footZ, yaw, pitch, pose);
        }
    }

    /** Updates the player's foot position while retaining rotation and pose. */
    public void updateFoot(double footX, double footY, double footZ) {
        validateFinite(footX, footY, footZ);
        synchronized (writerMonitor) {
            X.setOpaque(this, footX);
            Y.setOpaque(this, footY);
            Z.setOpaque(this, footZ);
        }
    }

    /** Updates the player's rotation while retaining position and pose. */
    public void updateRotation(float yaw, float pitch) {
        validateRotation(yaw, pitch);
        synchronized (writerMonitor) {
            YAW.setOpaque(this, yaw);
            PITCH.setOpaque(this, pitch);
        }
    }

    /** Updates the player's pose while retaining position and rotation. */
    public void updatePose(PlayerPose pose) {
        Objects.requireNonNull(pose, "pose");
        synchronized (writerMonitor) {
            POSE.setOpaque(this, pose);
        }
    }

    /** Updates position and rotation in one logical operation. */
    public void updateFootAndRotation(double footX, double footY, double footZ, float yaw, float pitch) {
        validateFinite(footX, footY, footZ);
        validateRotation(yaw, pitch);
        synchronized (writerMonitor) {
            X.setOpaque(this, footX);
            Y.setOpaque(this, footY);
            Z.setOpaque(this, footZ);
            YAW.setOpaque(this, yaw);
            PITCH.setOpaque(this, pitch);
        }
    }

    /** Invalidates and clears the complete player state. */
    public void invalidate() {
        synchronized (writerMonitor) {
            WORLD.setOpaque(this, null);
            X.setOpaque(this, 0.0);
            Y.setOpaque(this, 0.0);
            Z.setOpaque(this, 0.0);
            YAW.setOpaque(this, 0.0f);
            PITCH.setOpaque(this, 0.0f);
            POSE.setOpaque(this, null);
        }
    }

    @Override
    public UUID world() {
        return (UUID) WORLD.getOpaque(this);
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

    public float yaw() {
        return (float) YAW.getOpaque(this);
    }

    public float pitch() {
        return (float) PITCH.getOpaque(this);
    }

    public PlayerPose pose() {
        return (PlayerPose) POSE.getOpaque(this);
    }

    public float cameraYOffset() {
        PlayerPose currentPose = (PlayerPose) POSE.getOpaque(this);
        return currentPose == null ? 0.0f : currentPose.cameraYOffset();
    }

    public double cameraX() {
        return (double) X.getOpaque(this);
    }

    public double cameraY() {
        double currentY = (double) Y.getOpaque(this);
        PlayerPose currentPose = (PlayerPose) POSE.getOpaque(this);
        return currentY + (currentPose == null ? 0.0 : (double) currentPose.cameraYOffset());
    }

    public double cameraZ() {
        return (double) Z.getOpaque(this);
    }

    public double cameraDistanceSquared(Spatial spatial) {
        Objects.requireNonNull(spatial, "spatial");
        double targetX = spatial.x();
        double targetY = spatial.y();
        double targetZ = spatial.z();
        double currentX = (double) X.getOpaque(this);
        double currentY = (double) Y.getOpaque(this);
        double currentZ = (double) Z.getOpaque(this);
        PlayerPose currentPose = (PlayerPose) POSE.getOpaque(this);
        double dx = currentX - targetX;
        double dy = currentY + (currentPose == null ? 0.0 : (double) currentPose.cameraYOffset()) - targetY;
        double dz = currentZ - targetZ;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public PlayerLocatable setX(double x) {
        validateFinite(x);
        synchronized (writerMonitor) {
            X.setOpaque(this, x);
            return this;
        }
    }

    @Override
    public PlayerLocatable setY(double y) {
        validateFinite(y);
        synchronized (writerMonitor) {
            Y.setOpaque(this, y);
            return this;
        }
    }

    @Override
    public PlayerLocatable setZ(double z) {
        validateFinite(z);
        synchronized (writerMonitor) {
            Z.setOpaque(this, z);
            return this;
        }
    }

    @Override
    public PlayerLocatable setPosition(double x, double y, double z) {
        validateFinite(x, y, z);
        synchronized (writerMonitor) {
            X.setOpaque(this, x);
            Y.setOpaque(this, y);
            Z.setOpaque(this, z);
            return this;
        }
    }

    @Override
    public PlayerLocatable setPosition(Spatial spatial) {
        Objects.requireNonNull(spatial, "spatial");
        return setPosition(spatial.x(), spatial.y(), spatial.z());
    }

    @Override
    public PlayerLocatable setWorld(UUID world) {
        if (world == null) {
            invalidate();
            return this;
        }
        synchronized (writerMonitor) {
            WORLD.setOpaque(this, world);
            return this;
        }
    }

    @Override
    public PlayerLocatable setLocation(Locatable locatable) {
        Objects.requireNonNull(locatable, "locatable");
        return setLocation(locatable.world(), locatable.x(), locatable.y(), locatable.z());
    }

    @Override
    public PlayerLocatable setLocation(UUID world, double x, double y, double z) {
        validateFinite(x, y, z);
        if (world == null) {
            invalidate();
            return this;
        }
        synchronized (writerMonitor) {
            X.setOpaque(this, x);
            Y.setOpaque(this, y);
            Z.setOpaque(this, z);
            WORLD.setOpaque(this, world);
            return this;
        }
    }

    @Override
    public PlayerLocatable set(Locatable locatable) {
        return setLocation(locatable);
    }

    @Override
    public PlayerLocatable set(double x, double y, double z) {
        return setPosition(x, y, z);
    }

    @Override
    public PlayerLocatable set(int x, int y, int z) {
        return setPosition(x, y, z);
    }

    @Override
    public PlayerLocatable set(double x, double y, double z, UUID world) {
        return setLocation(world, x, y, z);
    }

    @Override
    public LocatableType getType() {
        return LocatableType.ThreadSafe;
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
    public boolean strictlyEquals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlayerLocatable that)) {
            return false;
        }
        UUID thisWorld = world();
        double thisX = x();
        double thisY = y();
        double thisZ = z();
        return Objects.equals(thisWorld, that.world())
                && Double.doubleToLongBits(thisX) == Double.doubleToLongBits(that.x())
                && Double.doubleToLongBits(thisY) == Double.doubleToLongBits(that.y())
                && Double.doubleToLongBits(thisZ) == Double.doubleToLongBits(that.z());
    }

    @Override
    public String toString() {
        return toStringForm();
    }

    /** This method must only be called while holding {@link #writerMonitor}. */
    private void writeFullState(UUID world, double footX, double footY, double footZ, float yaw, float pitch, PlayerPose pose) {
        WORLD.setOpaque(this, null);
        X.setOpaque(this, footX);
        Y.setOpaque(this, footY);
        Z.setOpaque(this, footZ);
        YAW.setOpaque(this, yaw);
        PITCH.setOpaque(this, pitch);
        POSE.setOpaque(this, pose);
        WORLD.setOpaque(this, world);
    }

    private static void validateFullState(UUID world, double footX, double footY, double footZ, float yaw, float pitch, PlayerPose pose) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pose, "pose");
        validateFinite(footX, footY, footZ);
        validateRotation(yaw, pitch);
    }

    private static void validateFinite(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Coordinates must be finite");
        }
    }

    private static void validateFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Coordinate must be finite");
        }
    }

    private static void validateRotation(float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0f || pitch > 90.0f) {
            throw new IllegalArgumentException("Rotation must be finite and pitch must be between -90 and 90 degrees");
        }
    }
}
