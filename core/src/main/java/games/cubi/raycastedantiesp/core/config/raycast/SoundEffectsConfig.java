/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.Config;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public record SoundEffectsConfig(
        @Comment("Whether sounds should be hidden when their source cannot be seen. This feature is not implemented yet.")
        boolean enabled,
        @Comment("Maximum number of occluding blocks permitted by a successful sound raycast.")
        int maxOccludingCount,
        @Comment("Distance in blocks within which sounds are always played.")
        int alwaysPlayRadius,
        @Comment("Maximum distance in blocks at which sounds are raycasted.")
        int raycastRadius
) implements Config {
    public static final SoundEffectsConfig DEFAULT = new SoundEffectsConfig(false, 3, 8, 48);
}
