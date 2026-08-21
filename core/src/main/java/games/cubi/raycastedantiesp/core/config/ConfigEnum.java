/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface ConfigEnum {
    String[] getValues();

    Registry REGISTRY = new Registry();

    default void register() {
        REGISTRY.add(this.getClass());
    }

    static String[] getAllValues() {
        return REGISTRY.getAllValues();
    }

    final class Registry {
        private final Set<Class<? extends ConfigEnum>> enums = new HashSet<>();

        void add(Class<? extends ConfigEnum> e) {
            enums.add(e);
        }

        String[] getAllValues() {
            return enums.stream()
                .flatMap(e -> Arrays.stream(e.getEnumConstants()[0].getValues()))
                .toArray(String[]::new);
        }
    }
}
