/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastConfigTest {
    @Test
    void variantSettingsRemainIndependent() {
        PlayerConfig defaults = PlayerConfig.DEFAULT;
        PlayerConfig changed = new PlayerConfig(
                defaults.raycastSettings(),
                !defaults.hideSoundsWhenHidden(),
                !defaults.keepClientEntityWhenHidden(),
                !defaults.onlyCheckSneaking()
        );

        assertNotEquals(defaults.hideSoundsWhenHidden(), changed.hideSoundsWhenHidden());
        assertNotEquals(defaults.keepClientEntityWhenHidden(), changed.keepClientEntityWhenHidden());
        assertNotEquals(defaults.onlyCheckSneaking(), changed.onlyCheckSneaking());
        assertEquals(defaults.raycastSettings(), changed.raycastSettings());
    }

    @Test
    void tileEntityUsesInterfaceDefaultsForUnsupportedSettings() {
        TileEntityConfig config = TileEntityConfig.DEFAULT;

        assertFalse(config.hideSoundsWhenHidden());
        assertFalse(config.keepClientEntityWhenHidden());
    }

    @Test
    void squaredDistancesAreDerivedFromConfiguredDistances() {
        RaycastConfig config = EntityConfig.DEFAULT;

        assertEquals((int) config.getAlwaysShowRadius() * config.getAlwaysShowRadius(), config.alwaysShowRadiusSquared());
        assertEquals((int) config.getRaycastRadius() * config.getRaycastRadius(), config.raycastRadiusSquared());
        assertEquals((int) config.hideOnSpawnDistance() * config.hideOnSpawnDistance(), config.hideOnSpawnDistanceSquared());
        assertTrue(config.raycastRadiusSquared() >= 0);
    }
}
