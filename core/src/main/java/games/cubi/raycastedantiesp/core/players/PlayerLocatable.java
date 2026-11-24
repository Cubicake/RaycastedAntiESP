/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.api.MutableFloatingLocatable;

/**
 * The supplier of this object promises to keep it up-to-date at all times
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
}
