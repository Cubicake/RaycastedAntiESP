/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.jetbrains.annotations.Nullable;

public enum BlockProcessorMode implements ConfigEnum {
    PACKETEVENTS("packetevents");

    private final String configName;

    BlockProcessorMode(String configName) {
        this.configName = configName;
    }

    public String getName() {
        return configName;
    }

    public static @Nullable BlockProcessorMode fromString(String name) {
        for (BlockProcessorMode mode : values()) {
            if (mode.configName.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return null;
    }

    @Override
    public String[] getValues() {
        return new String[] {configName};
    }
}
