/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.ConfigReader;
import org.spongepowered.configurate.ConfigurationNode;

public class PlayerConfig extends RaycastConfig {
    private final boolean onlyCheckSneaking;

    private PlayerConfig(RaycastConfig config, boolean onlyCheckSneaking) {
        super(config.enabled(), config.hideSoundsWhenHidden(), config.getMaxOccludingCount(), config.getAlwaysShowRadius(),
                config.getRaycastRadius(), config.hideOnSpawnDistance(), config.getVisibleRecheckIntervalTicks(),
                config.keepClientEntityWhenHidden());
        this.onlyCheckSneaking = onlyCheckSneaking;
    }

    public static PlayerConfig load(ConfigurationNode node, String path) {
        return new PlayerConfig(
                RaycastConfig.load(node, path, true, true),
                ConfigReader.bool(ConfigReader.node(node, "only-check-sneaking"), path + ".only-check-sneaking")
        );
    }

    public boolean onlyCheckSneaking() {
        return onlyCheckSneaking;
    }
}
