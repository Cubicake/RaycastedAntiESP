/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.ConfigurationNode;

public record BlockProcessorConfig(BlockProcessorMode mode, boolean trackAllBlocks) implements Config {
    public static BlockProcessorConfig load(ConfigurationNode root) {
        ConfigurationNode node = ConfigReader.node(root, "block-processor");
        String modeName = ConfigReader.string(ConfigReader.node(node, "mode"), "block-processor.mode");
        BlockProcessorMode mode = BlockProcessorMode.fromString(modeName);
        if (mode == null) {
            throw new ConfigLoadException("block-processor.mode has unsupported value '" + modeName + "'");
        }
        return new BlockProcessorConfig(
                mode,
                ConfigReader.bool(ConfigReader.node(node, "track-all-blocks"), "block-processor.track-all-blocks")
        );
    }
}
