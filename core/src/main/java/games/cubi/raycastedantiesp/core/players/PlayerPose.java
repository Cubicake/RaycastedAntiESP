/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

/** Player body poses and the corresponding camera height above the feet. */
public enum PlayerPose {
    UNKNOWN(1.62f),
    STANDING(1.62f),
    SNEAKING(1.27f),
    SWIMMING(0.40f),
    FALL_FLYING(0.40f),
    SPIN_ATTACK(0.40f),
    SLEEPING(0.20f),
    DYING(0.20f);

    private final float cameraYOffset;

    PlayerPose(float cameraYOffset) {
        this.cameraYOffset = cameraYOffset;
    }

    public float cameraYOffset() {
        return cameraYOffset;
    }
}
