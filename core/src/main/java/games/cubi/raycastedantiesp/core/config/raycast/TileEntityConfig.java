/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import org.spongepowered.configurate.ConfigurationNode;

public class TileEntityConfig extends RaycastConfig {
    private TileEntityConfig(RaycastConfig config) {
        super(config.enabled(), false, config.getMaxOccludingCount(), config.getAlwaysShowRadius(),
                config.getRaycastRadius(), config.hideOnSpawnDistance(), config.getVisibleRecheckIntervalTicks());
    }

    public static TileEntityConfig load(ConfigurationNode node, String path) {
        return new TileEntityConfig(RaycastConfig.load(node, path, false));
    }
}
