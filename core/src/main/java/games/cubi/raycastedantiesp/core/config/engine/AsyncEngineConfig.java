/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.engine;

import games.cubi.raycastedantiesp.core.config.Config;
import games.cubi.raycastedantiesp.core.config.ConfigRange;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record AsyncEngineConfig(
        @Setting("processing-threads")
        @Comment("Number of async processing threads. One is sufficient for hundreds of players.")
        @ConfigRange(min = 1, max = 64)
        int asyncProcessingThreads
) implements Config {
    public static final AsyncEngineConfig DEFAULT = new AsyncEngineConfig(1);
}
