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
public record ChunkSectionConfig(
        @Comment("Whether chunk sections are hidden when they cannot be seen. This feature is not implemented yet.")
        boolean enabled,
        @Comment("Maximum number of occluding blocks permitted by a successful raycast.")
        int maxOccludingCount,
        @Comment("Radius in chunks within which chunk sections are always visible.")
        int alwaysShowRadiusChunks,
        @Comment("Ticks between checks of visible chunk sections. -1 disables visible rechecks.")
        int visibleRecheckIntervalTicks
) implements Config {
    public static final ChunkSectionConfig DEFAULT = new ChunkSectionConfig(false, 6, 2, -1);
}
