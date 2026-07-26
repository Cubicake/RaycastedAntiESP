/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.config;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.EntityTypeExclusions;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSets;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Collection;

import static games.cubi.raycastedantiesp.core.config.raycast.EntityTypeExclusions.volatileExclusionSet;

public final class PaperEntityTypeExclusionResolver {
    private PaperEntityTypeExclusionResolver() {}

    public static void resolveAndInitialise(Collection<String> configuredNames) {
        ClientVersion version = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();

        IntOpenHashSet excludedTypes = new IntOpenHashSet(configuredNames.size());
        for (String configuredName : configuredNames) {
            NamespacedKey key = NamespacedKey.fromString(configuredName);
            if (key == null) {
                warn("contains invalid resource name '" + configuredName + "'");
                continue;
            }

            // Have to use FQN since we have PE EntityType imported (it's a longer FQN)
            org.bukkit.entity.EntityType paperType = Registry.ENTITY_TYPE.get(key);
            if (paperType == null) {
                warn("contains unknown entity type '" + configuredName + "'");
                continue;
            }
            if (paperType == org.bukkit.entity.EntityType.PLAYER) {
                warn("cannot exclude player entity type '" + configuredName + "'");
                continue;
            }

            EntityType packetEventsType = SpigotConversionUtil.fromBukkitEntityType(paperType);
            if (packetEventsType == null) {
                warn("contains entity type '" + configuredName + "' which is known to Paper but not PacketEvents");
                continue;
            }

            int entityType = packetEventsType.getId(version);
            if (entityType < 0) {
                warn("contains entity type '" + configuredName + "' which has no concrete PacketEvents ID for " + version);
                continue;
            }
            excludedTypes.add(entityType);
        }
        synchronized (EntityTypeExclusions.class) {
            if (volatileExclusionSet != null) throw new RuntimeException("Volatile exclusion set already initialised");
            volatileExclusionSet = IntSets.unmodifiable(excludedTypes);
        }
        Logger.info("Resolved " + EntityTypeExclusions.size() + " excluded entity types.", 5);
    }

    private static void warn(String message) {
        Logger.warning("Parsing excluded entity type failed with error: " + message + ". The entry will be ignored.", 4,
                PaperEntityTypeExclusionResolver.class);
    }
}
