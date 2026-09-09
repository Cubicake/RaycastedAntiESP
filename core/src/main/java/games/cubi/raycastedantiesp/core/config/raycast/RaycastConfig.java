/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.Config;

public interface RaycastConfig extends Config {
    RaycastSettings raycastSettings();

    default boolean enabled() {
        return raycastSettings().enabled();
    }

    default boolean hideSoundsWhenHidden() {
        return false;
    }

    default byte getMaxOccludingCount() {
        return raycastSettings().maxOccludingCount();
    }

    default short getAlwaysShowRadius() {
        return raycastSettings().alwaysShowRadius();
    }

    default short getRaycastRadius() {
        return raycastSettings().raycastRadius();
    }

    default short hideOnSpawnDistance() {
        return raycastSettings().hideOnSpawnDistance();
    }

    default short getVisibleRecheckIntervalTicks() {
        return raycastSettings().visibleRecheckIntervalTicks();
    }

    default boolean keepClientEntityWhenHidden() {
        return false;
    }

    default int alwaysShowRadiusSquared() {
        int radius = getAlwaysShowRadius();
        return radius * radius;
    }

    default int raycastRadiusSquared() {
        int radius = getRaycastRadius();
        return radius * radius;
    }

    default int hideOnSpawnDistanceSquared() {
        int distance = hideOnSpawnDistance();
        return distance * distance;
    }
}
