/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.logs.Logger;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

/**
 * Immutable entity-type exclusion policy resolved during plugin startup.
 */
public final class EntityTypeExclusions {
    public static volatile IntSet volatileExclusionSet = null;

    // Is this sort of an insane approach? Yes. But it works.
    static class FakeLazyConstant {
        private static final IntSet finalEntityTypeExclusions = init();

        private static IntSet init() {
            IntSet set = volatileExclusionSet;
            if (set == null) {
                Logger.error(new RuntimeException("Exclusion set was not initialised before being read"), 4, EntityTypeExclusions.class);
                return IntSets.emptySet();
            }
            return set;
        }
    }

    public static boolean excludes(int entityType) {
        return FakeLazyConstant.finalEntityTypeExclusions.contains(entityType);
    }

    public static int size() {
        return FakeLazyConstant.finalEntityTypeExclusions.size();
    }
}
