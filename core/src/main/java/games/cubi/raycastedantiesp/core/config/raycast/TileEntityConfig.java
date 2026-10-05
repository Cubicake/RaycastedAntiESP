/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public record TileEntityConfig(
        @Setting(nodeFromParent = true)
        RaycastSettings raycastSettings,
        @Comment("Blocks that should not be managed as tile entities. A block key (\"minecraft:chest\") matches all states; properties (\"chest[facing=north,type=single,waterlogged=false]\") select one exact state.")
        List<BlockSelector> excludedBlocks
) implements RaycastConfig {
    public static final TileEntityConfig DEFAULT = new TileEntityConfig(
            new RaycastSettings(true, (byte) 3, (short) 8, (short) 64, (short) 24, (short) -1),
            List.of(BlockSelector.parse("minecraft:beacon"))
    );

    public TileEntityConfig {
        excludedBlocks = List.copyOf(excludedBlocks);
    }
}
