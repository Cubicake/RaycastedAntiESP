/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.locatables;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.BlockLocatable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
// If only this was kotlin and I could use extension functions
public class LocatableAdapterUtils {

    public static Location toCentredLocation(BlockLocatable locatable) {
        return new Location(getWorld(locatable.world()), locatable.blockX() + 0.5, locatable.blockY() + 0.5, locatable.blockZ() + 0.5);
    }

    public static Location toBukkitLocation(Locatable locatable) {
        return new Location(getWorld(locatable.world()), locatable.x(), locatable.y(), locatable.z());
    }

    public static Locatable toLocatable(Location location, Locatable.LocatableType type) {

        UUID worldUUID = location.getWorld().getUID();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return Locatable.create(worldUUID, x, y, z, type);
    }

    public static <T extends Locatable> T toLocatable(Location location, Class<T> type) {
        return toLocatable(location, 0, type);
    }

    public static <T extends Locatable> T toLocatable(Location location, double heightOffset, Class<T> type) {

        UUID worldUUID = location.getWorld().getUID();
        double x = location.getX();
        double y = location.getY() + heightOffset;
        double z = location.getZ();

        return Locatable.create(worldUUID, x, y, z, type);
    }

    private static final Map<UUID, WeakReference<World>> worldCache = new ConcurrentHashMap<>();

    //Bukkit#getWorld iterates through all worlds and compares UUIDs, this is probably not actually needed though
    public static World getWorld(UUID worldUUID) {
        World world = null;
        WeakReference<World> weakRef = worldCache.get(worldUUID);
        if (weakRef != null) {
            world = weakRef.get();
        }
        if (world == null) {
            world = Bukkit.getWorld(worldUUID);
            if (world != null) {
                worldCache.put(worldUUID, new WeakReference<>(world));
            }
        }
        return world;
    }
}
