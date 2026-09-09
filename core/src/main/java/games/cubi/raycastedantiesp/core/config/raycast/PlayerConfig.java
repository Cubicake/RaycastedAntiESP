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

@ConfigSerializable
public record PlayerConfig(
        @Setting(nodeFromParent = true)
        RaycastSettings raycastSettings,
        @Comment("Suppress sounds produced by hidden players.")
        boolean hideSoundsWhenHidden,
        @Comment("Retain hidden players client-side instead of destroying their entities. This will cause a non-moving \"ghost player\" to persist until the real player is visible again.")
        boolean keepClientEntityWhenHidden,
        @Comment("Only raycast other players while the viewing player is sneaking.")
        boolean onlyCheckSneaking
) implements RaycastConfig {
    public static final PlayerConfig DEFAULT = new PlayerConfig(
            new RaycastSettings(true, (byte) 3, (short) 8, (short) 128, (short) 24, (short) 5),
            true,
            false,
            true
    );
}
