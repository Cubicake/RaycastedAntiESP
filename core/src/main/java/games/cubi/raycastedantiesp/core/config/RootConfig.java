/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.engine.EngineConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record RootConfig(
        String configVersion,
        @Setting("logging") LoggingConfig loggingConfig,
        @Setting("checks") ChecksConfig checksConfig,
        @Setting("engine") EngineConfig engineConfig,
        @Setting("block-processor") BlockProcessorConfig blockProcessorConfig,
        @Setting("debug") DebugConfig debugConfig,
        @Setting("updates") UpdateConfig updateConfig
) implements Config {
    public static final String CURRENT_VERSION = "2.1";
    public static final RootConfig DEFAULT = new RootConfig(
            CURRENT_VERSION,
            LoggingConfig.DEFAULT,
            ChecksConfig.DEFAULT,
            EngineConfig.DEFAULT,
            BlockProcessorConfig.DEFAULT,
            DebugConfig.DEFAULT,
            UpdateConfig.DEFAULT
    );
}
