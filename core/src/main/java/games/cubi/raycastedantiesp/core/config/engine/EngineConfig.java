/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.engine;

import games.cubi.raycastedantiesp.core.config.Config;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record EngineConfig(
        EngineMode mode,
        @Setting("async") AsyncEngineConfig asyncConfig
) implements Config {
    public static final EngineConfig DEFAULT = new EngineConfig(EngineMode.ASYNC, AsyncEngineConfig.DEFAULT);

    public EngineMode getMode() {
        return mode;
    }
}
