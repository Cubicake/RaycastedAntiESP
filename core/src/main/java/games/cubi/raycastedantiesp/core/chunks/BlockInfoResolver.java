/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

import org.jetbrains.annotations.Contract;

public interface BlockInfoResolver {
    @Contract(pure = true)
    boolean isOccluding(int blockStateID);

    /**
     * Anti-ESP managed tile entity state after config exemptions and force-includes.
     */
    @Contract(pure = true)
    boolean isTileEntity(int blockStateID);

    /**
     * Raw block entity capability before anti-ESP config overrides.
     */
    @Contract(pure = true)
    boolean hasBlockEntityData(int blockStateID);
}
