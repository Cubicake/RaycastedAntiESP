/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastConfigTest {
    @Test
    void playerAndEntityRetentionSettingsLoadIndependently() throws SerializationException {
        PlayerConfig playerConfig = PlayerConfig.load(entityNode(true), "checks.player");
        EntityConfig entityConfig = EntityConfig.load(entityNode(false), "checks.entity");

        assertTrue(playerConfig.keepClientEntityWhenHidden());
        assertFalse(playerConfig.onlyCheckSneaking());
        assertFalse(entityConfig.keepClientEntityWhenHidden());
    }

    @Test
    void playerSneakingOnlySettingLoadsIndependently() throws SerializationException {
        ConfigurationNode node = entityNode(true);
        node.node("only-check-sneaking").set(true);

        PlayerConfig playerConfig = PlayerConfig.load(node, "checks.player");

        assertTrue(playerConfig.onlyCheckSneaking());
    }

    @Test
    void tileEntityConfigDoesNotRequireOrEnableClientRetention() throws SerializationException {
        TileEntityConfig config = TileEntityConfig.load(baseNode(), "checks.tile-entity");

        assertFalse(config.keepClientEntityWhenHidden());
    }

    @Test
    void legacyConstructorKeepsDestroyBehaviorWhileNewOverloadCanRetain() {
        RaycastConfig legacy = new RaycastConfig(true, true, 3, 8, 48, 24, 5);
        RaycastConfig retaining = new RaycastConfig(true, true, 3, 8, 48, 24, 5, true);

        assertFalse(legacy.keepClientEntityWhenHidden());
        assertTrue(retaining.keepClientEntityWhenHidden());
    }

    private static ConfigurationNode entityNode(boolean keepClientEntityWhenHidden) throws SerializationException {
        ConfigurationNode node = baseNode();
        node.node("hide-sounds-when-hidden").set(true);
        node.node("keep-client-entity-when-hidden").set(keepClientEntityWhenHidden);
        node.node("only-check-sneaking").set(false);
        return node;
    }

    private static ConfigurationNode baseNode() throws SerializationException {
        ConfigurationNode node = BasicConfigurationNode.root();
        node.node("enabled").set(true);
        node.node("max-occluding-count").set(3);
        node.node("always-show-radius").set(8);
        node.node("raycast-radius").set(48);
        node.node("hide-on-spawn-distance").set(24);
        node.node("visible-recheck-interval-ticks").set(5);
        return node;
    }
}
