/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.tracked;

import games.cubi.locatables.api.ImmutableBlockSpatial;
import games.cubi.raycastedantiesp.core.utils.Clearable;

public interface TrackedTileEntity<T> extends ImmutableBlockSpatial, Clearable {
    int NEVER_CHECKED = Integer.MIN_VALUE;

    boolean visible();
    TrackedTileEntity<T> setVisible(boolean visible);

    int lastChecked();
    TrackedTileEntity<T> setLastChecked(int lastChecked);

    /** Packet-thread-only structural state. */
    int blockID();
    TrackedTileEntity<T> setBlockID(char blockID);

    /** Packet-thread-only replay state. */
    T extraData();
    TrackedTileEntity<T> setExtraData(T extraData);
    void clearExtraData();
}
