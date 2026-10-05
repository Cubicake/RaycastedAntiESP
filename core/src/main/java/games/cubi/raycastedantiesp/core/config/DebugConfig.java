/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public record DebugConfig(
        @Comment("Show raycast debug particles.")
        boolean particles,
        @Comment("Record and report engine timing information.")
        boolean timings
) implements Config {
    public static final DebugConfig DEFAULT = new DebugConfig(false, false);

    public boolean showDebugParticles() {
        return particles;
    }

    public boolean recordTimings() {
        return timings;
    }
}
