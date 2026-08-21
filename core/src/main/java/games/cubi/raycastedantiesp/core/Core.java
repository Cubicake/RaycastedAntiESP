/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core;

import games.cubi.logs.PlatformLogger;
import games.cubi.logs.Logger;

public class Core {

    public static Core instance;

    private Core(PlatformLogger logger) {
        Logger.init(logger);
    }

    public static Core initialize(PlatformLogger logger) {
        if (instance != null) {
            return instance;
        }
        instance = new Core(logger);
        return instance;
    }

    public static Core getInstance() {
        if (instance == null) {
            Logger.error(new IllegalStateException("Core has not been initialized yet but Core#getInstance called!"),1, Core.class);
        }
        return instance;
    }

    public void intelliJStopThinkingThisIsAUtilClass() {}
}
