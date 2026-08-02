/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.entity;

import games.cubi.utils.sets.CopyOnWriteMTIntSet;

/**
 * Global registry of entity IDs which RaycastedAntiESP must ignore.
 */
public final class EntityBypassRegistry {
    private static final CopyOnWriteMTIntSet BYPASSED_ENTITY_IDS = CopyOnWriteMTIntSet.get();
    // Technically, (26.2+) Minecraft only guarantees that an entity ID corresponds to one-and-only-one entity within a single world, and entities in different worlds can have the same ID.
    //              (26.1.2-) Minecraft does not guarantee that an entity ID is unique.
    // However, this requires the int id counter to overflow from Integer.MAX_VALUE all the way back to 0. This is unlikely to occur, and even if it does occur, it is unlikely that the original entities are still alive.
    private EntityBypassRegistry() {
    }

    public static void addEntity(int entityID) {
        BYPASSED_ENTITY_IDS.add(entityID);
    }

    /**
     * For use when an entity is despawned/killed, or in other words completely gone from the server.
     */
    public static boolean markEntityDespawned(int entityID) {
        return BYPASSED_ENTITY_IDS.remove(entityID);
    }

    public static boolean isBypassed(int entityID) {
        return BYPASSED_ENTITY_IDS.contains(entityID);
    }
}
