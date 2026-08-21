/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.ConfigurationNode;

public record UpdateConfig(boolean checkRelease, boolean checkBeta, boolean checkAlpha, boolean notifyInGame) implements Config {

    public static UpdateConfig load(ConfigurationNode root) {
        ConfigurationNode node = ConfigReader.node(root, "updates");
        return new UpdateConfig(
                ConfigReader.bool(ConfigReader.node(node, "check-release"), "updates.check-release"),
                ConfigReader.bool(ConfigReader.node(node, "check-beta"), "updates.check-beta"),
                ConfigReader.bool(ConfigReader.node(node, "check-alpha"), "updates.check-alpha"),
                ConfigReader.bool(ConfigReader.node(node, "notify-in-game"), "updates.notify-in-game")
        );
    }

    public boolean anyChannelEnabled() {
        return checkRelease || checkBeta || checkAlpha;
    }
}
