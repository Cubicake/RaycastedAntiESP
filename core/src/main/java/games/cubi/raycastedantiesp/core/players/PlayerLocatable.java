/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.api.ImmutableLocatable;
import games.cubi.locatables.api.MutableFloatingLocatable;
import games.cubi.locatables.api.Spatial;
import games.cubi.locatables.implementations.ImmutableLocatableImpl;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The location source maintains the player's feet position, rotation and pose.
 * The world is null while the position is unavailable. Reads of multiple fields may span an update.
 */
public interface PlayerLocatable extends MutableFloatingLocatable {
    float yaw();

    float pitch();

    PlayerPose pose();

    default float cameraYOffset() {
        return pose().cameraYOffset();
    }

    default double cameraY() {
        return y() + cameraYOffset();
    }

    /** Copies the camera position, or returns null while the position is unavailable. */
    default @Nullable ImmutableLocatable cameraLocation() {
        UUID world = world();
        return world == null ? null : new ImmutableLocatableImpl(world, x(), cameraY(), z());
    }

    /** Returns positive infinity when the camera is unavailable in the expected world. */
    default double cameraDistanceSquared(UUID expectedWorld, Spatial target) {
        return expectedWorld.equals(world()) ? target.distanceSquared(x(), cameraY(), z()) : Double.POSITIVE_INFINITY;
    }
}
