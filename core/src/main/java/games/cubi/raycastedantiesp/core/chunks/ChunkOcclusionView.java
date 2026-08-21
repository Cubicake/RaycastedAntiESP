/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.chunks;

import org.jetbrains.annotations.Range;

import static games.cubi.raycastedantiesp.core.chunks.ChunkData.LOCAL_MASK;

public interface ChunkOcclusionView {
    /**
     * The parameters here are local coordinates within the chunk section, so they must be in the range [0, 15].
     *
     * @return true if the block at the given local coordinates is occluding, false otherwise.
     */
    boolean isOccludingLocal(@Range(from = 0, to = 15) int x, @Range(from = 0, to = 15) int y, @Range(from = 0, to = 15) int z);

    default boolean isOccludingGlobal(int x, int y, int z) {
        return isOccludingLocal(x & LOCAL_MASK, y & LOCAL_MASK, z & LOCAL_MASK);
    }

    default boolean isSolid() {
        return false;
    }
}
