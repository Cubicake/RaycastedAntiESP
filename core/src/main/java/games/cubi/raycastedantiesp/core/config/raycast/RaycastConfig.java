/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.Config;
import games.cubi.raycastedantiesp.core.config.ConfigReader;
import org.spongepowered.configurate.ConfigurationNode;

public class RaycastConfig implements Config {
    private final boolean enabled;
    private final boolean hideSoundsWhenHidden;
    private final byte maxOccludingCount;
    private final short alwaysShowRadius;
    private final short raycastRadius;
    private final short hideOnSpawnDistance;
    private final short visibleRecheckIntervalTicks;
    private final boolean keepClientEntityWhenHidden;

    public final int alwaysShowRadiusSquared;
    public final int raycastRadiusSquared;

    public RaycastConfig(boolean enabled, boolean hideSoundsWhenHidden, int maxOccludingCount, int alwaysShowRadius,
                         int raycastRadius, int hideOnSpawnDistance, int visibleRecheckIntervalTicks) {
        this(enabled, hideSoundsWhenHidden, maxOccludingCount, alwaysShowRadius, raycastRadius, hideOnSpawnDistance,
                visibleRecheckIntervalTicks, false);
    }

    public RaycastConfig(boolean enabled, boolean hideSoundsWhenHidden, int maxOccludingCount, int alwaysShowRadius,
                         int raycastRadius, int hideOnSpawnDistance, int visibleRecheckIntervalTicks,
                         boolean keepClientEntityWhenHidden) {
        this.enabled = enabled;
        this.hideSoundsWhenHidden = hideSoundsWhenHidden;
        this.maxOccludingCount = (byte) maxOccludingCount;
        this.alwaysShowRadius = (short) alwaysShowRadius;
        this.raycastRadius = (short) raycastRadius;
        this.hideOnSpawnDistance = (short) hideOnSpawnDistance;
        this.visibleRecheckIntervalTicks = (short) visibleRecheckIntervalTicks;
        this.keepClientEntityWhenHidden = keepClientEntityWhenHidden;

        alwaysShowRadiusSquared = alwaysShowRadius * alwaysShowRadius;
        raycastRadiusSquared = raycastRadius * raycastRadius;
    }

    protected static RaycastConfig load(ConfigurationNode node, String path, boolean hasHideSoundsWhenHidden) {
        return load(node, path, hasHideSoundsWhenHidden, false);
    }

    protected static RaycastConfig load(ConfigurationNode node, String path, boolean hasHideSoundsWhenHidden,
                                        boolean hasKeepClientEntityWhenHidden) {
        int maxOccludingCount = ConfigReader.integer(ConfigReader.node(node, "max-occluding-count"), path + ".max-occluding-count");
        if (maxOccludingCount < 0 || maxOccludingCount > Byte.MAX_VALUE) {
            Logger.warning(path + ".max-occluding-count must be between 0 and " + Byte.MAX_VALUE + " but was " + maxOccludingCount +". Defaulting to 3.", 4, RaycastConfig.class);
            maxOccludingCount = 3;
        }
        int alwaysShowRadius = ConfigReader.integer(ConfigReader.node(node, "always-show-radius"), path + ".always-show-radius");
        if (alwaysShowRadius < 0 || alwaysShowRadius > Short.MAX_VALUE) {
            Logger.warning(path + ".always-show-radius must be between 0 and " + Short.MAX_VALUE + " but was " + alwaysShowRadius +". Defaulting to 8.", 4, RaycastConfig.class);
            alwaysShowRadius = 8;
        }
        int raycastRadius = ConfigReader.integer(ConfigReader.node(node, "raycast-radius"), path + ".raycast-radius");
        if (raycastRadius < 0 || raycastRadius > Short.MAX_VALUE) {
            Logger.warning(path + ".raycast-radius must be between 0 and " + Short.MAX_VALUE + " but was " + raycastRadius +". Defaulting to 48.", 4, RaycastConfig.class);
            raycastRadius = 48;
        }
        int hideOnSpawnDistance = ConfigReader.integer(ConfigReader.node(node, "hide-on-spawn-distance"), path + ".hide-on-spawn-distance");
        if (hideOnSpawnDistance < 0 || hideOnSpawnDistance > Short.MAX_VALUE) {
            Logger.warning(path + ".hide-on-spawn-distance must be between 0 and " + Short.MAX_VALUE + " but was " + hideOnSpawnDistance +". Defaulting to 32.", 4, RaycastConfig.class);
            hideOnSpawnDistance = 32;
        }
        int visibleRecheckIntervalTicks = ConfigReader.integer(ConfigReader.node(node, "visible-recheck-interval-ticks"), path + ".visible-recheck-interval-ticks");
        if (visibleRecheckIntervalTicks < -1 || visibleRecheckIntervalTicks > Short.MAX_VALUE) {
            Logger.warning(path + ".visible-recheck-interval-ticks must be between -1 and " + Short.MAX_VALUE + " but was " + visibleRecheckIntervalTicks +". Defaulting to 5.", 4, RaycastConfig.class);
            visibleRecheckIntervalTicks = 5;
        }
        return new RaycastConfig(
                ConfigReader.bool(ConfigReader.node(node, "enabled"), path + ".enabled"),
                hasHideSoundsWhenHidden && ConfigReader.bool(ConfigReader.node(node, "hide-sounds-when-hidden"), path + ".hide-sounds-when-hidden"),
                maxOccludingCount,
                alwaysShowRadius,
                raycastRadius,
                hideOnSpawnDistance,
                visibleRecheckIntervalTicks,
                hasKeepClientEntityWhenHidden && ConfigReader.bool(ConfigReader.node(node, "keep-client-entity-when-hidden"), path + ".keep-client-entity-when-hidden")
        );
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hideSoundsWhenHidden() {
        return hideSoundsWhenHidden;
    }

    public byte getMaxOccludingCount() {
        return maxOccludingCount;
    }

    public short getAlwaysShowRadius() {
        return alwaysShowRadius;
    }

    public short getRaycastRadius() {
        return raycastRadius;
    }

    public short hideOnSpawnDistance() {
        return hideOnSpawnDistance;
    }

    public short getVisibleRecheckIntervalTicks() {
        return visibleRecheckIntervalTicks;
    }

    public boolean keepClientEntityWhenHidden() {
        return keepClientEntityWhenHidden;
    }
}
