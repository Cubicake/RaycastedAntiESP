/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.engine;

import games.cubi.raycastedantiesp.core.config.ConfigEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum EngineMode implements ConfigEnum {
    ASYNC("async", "simple"),
    NETTY("netty");

    private final String configName;
    private final @Nullable String aliasName;

    EngineMode(String configName) {
        this.configName = configName;
        aliasName = null;
    }

    EngineMode(String configName, @NotNull String aliasName) {
        this.configName = configName;
        this.aliasName = aliasName;
    }

    public String getName() {
        return configName;
    }

    public static @Nullable EngineMode fromString(String name) {
        for (EngineMode mode : values()) {
            if (mode.configName.equalsIgnoreCase(name) || name.equalsIgnoreCase(mode.aliasName)) {
                return mode;
            }
        }
        return null;
    }

    @Override
    public String[] getValues() {
        return new String[] {NETTY.configName, ASYNC.configName};
    }
}
