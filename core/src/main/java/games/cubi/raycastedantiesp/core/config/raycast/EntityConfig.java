/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.ListEntryComments;
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
        @Comment("""
                Entity types that must always remain visible.

                Falling blocks are also often used in custom mechanics, especially by plugins predating display entities. However, they occur in regular gameplay and can reveal information about hidden players, so they are not excluded by default. To exclude them, add minecraft:falling_block.

                Add further exclusions using the namespaced entity name accepted by the /summon command.
                """)
        @ListEntryComments({
                @ListEntryComments.Entry(value = "minecraft:arrow", comment =
                        "Projectiles move very quickly, giving the raycast engine limited time to process them. "
                        + "This could leave players without enough time to dodge, so projectiles are always visible by default."),
                @ListEntryComments.Entry(value = "minecraft:area_effect_cloud", comment =
                        "Potions and area-effect clouds can damage players even when not visible. "
                        + "A reasonable always-show-radius mitigates this, so exempting them is not always necessary. "
                        + "Servers where knowing the exact number of potions another player has used is an unfair advantage may want to remove these exclusions."),
                @ListEntryComments.Entry(value = "minecraft:potion", comment =
                        "The minecraft:potion entity was removed in 1.21.5 and replaced by minecraft:splash_potion and minecraft:lingering_potion. "
                        + "On 1.21.4, remove the latter two entries; on 1.21.5 and newer, remove minecraft:potion instead."),
                @ListEntryComments.Entry(value = "minecraft:lightning_bolt", comment = "Lightning is short-lived and expected to be visible to players who cannot see the block being struck."),
                @ListEntryComments.Entry(value = "minecraft:ender_dragon", comment = "Boss entities are also always visible by default."),
                @ListEntryComments.Entry(value = "minecraft:block_display", comment =
                        "Display, interaction, and mannequin entities are not normally seen in vanilla gameplay and are commonly used for custom mechanics. "
                        + "Removing these exclusions is likely to break plugins such as hologram or NPC plugins.")
        })
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
