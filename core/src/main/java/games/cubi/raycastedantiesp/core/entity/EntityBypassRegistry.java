/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.entity;

import games.cubi.raycastedantiesp.core.utils.CopyOnWriteMTIntSet;

/**
 * Global registry of entity IDs which RaycastedAntiESP must ignore.
 */
public final class EntityBypassRegistry {
    private static final CopyOnWriteMTIntSet BYPASSED_ENTITY_IDS = CopyOnWriteMTIntSet.get();

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
