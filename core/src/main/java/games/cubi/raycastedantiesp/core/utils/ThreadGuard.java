/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

public class ThreadGuard {
    public static final boolean ASSERTIONS_ENABLED = areAssertionsEnabled();

    @SuppressWarnings("PointlessBooleanExpression")
    /**
     * @return true if assertions are enabled, false otherwise
     */
    private static boolean areAssertionsEnabled() {
        try {
            assert true == false; // can be simplified to assert false but this reads more clearly to me as a statement which will fail.
            return false;
        } catch (AssertionError e) {
            return true;
        }
    }

}
