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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigSerializable
public record EntityConfig(
        @Setting(nodeFromParent = true)
        RaycastSettings raycastSettings,
        @Comment("Suppress sounds produced by hidden entities.")
        boolean hideSoundsWhenHidden,
        @Comment("Retain hidden entities client-side instead of destroying them. This will cause a non-moving \"ghost player\" to persist until the real player is visible again, but can reduce network load around large mob farms or bases.")
        boolean keepClientEntityWhenHidden,
        @Comment("Entity types that must always remain visible. Use names accepted by the summon command. By default, projectiles, bosses, potions, and custom entities are exempted.")
        Set<String> excludedTypes
) implements RaycastConfig {
    public static final EntityConfig DEFAULT = new EntityConfig(
            new RaycastSettings(true, (byte) 3, (short) 8, (short) 92, (short) 24, (short) 5),
            true,
            false,
            new LinkedHashSet<>(List.of(
                    "minecraft:arrow",
                    "minecraft:spectral_arrow",
                    "minecraft:fireball",
                    "minecraft:dragon_fireball",
                    "minecraft:small_fireball",
                    "minecraft:firework_rocket",
                    "minecraft:wither_skull",
                    "minecraft:breeze_wind_charge",
                    "minecraft:wind_charge",
                    "minecraft:ender_pearl",
                    "minecraft:evoker_fangs",
                    "minecraft:shulker_bullet",
                    "minecraft:area_effect_cloud",
                    "minecraft:lingering_potion",
                    "minecraft:splash_potion",
                    "minecraft:potion",
                    "minecraft:lightning_bolt",
                    "minecraft:ender_dragon",
                    "minecraft:wither",
                    "minecraft:warden",
                    "minecraft:block_display",
                    "minecraft:text_display",
                    "minecraft:item_display",
                    "minecraft:interaction",
                    "minecraft:mannequin"
            ))
    );

    public EntityConfig {
        excludedTypes = Collections.unmodifiableSet(new LinkedHashSet<>(excludedTypes));
    }
}
