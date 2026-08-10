/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.tracked;

import games.cubi.locatables.api.BlockSpatial;
import games.cubi.raycastedantiesp.core.utils.Clearable;
import games.cubi.raycastedantiesp.core.utils.InvasivelyLinkedSWMRList;
import games.cubi.raycastedantiesp.core.utils.VarHandler;

import java.lang.invoke.VarHandle;

/** Tile visibility and lifecycle are shared with the engine; block and replay data are packet-thread-only. */
public abstract class NettyTileEntity<PacketReplayData extends Clearable> extends InvasivelyLinkedSWMRList<NettyTileEntity<PacketReplayData>> implements TrackedTileEntity<PacketReplayData> {
    private volatile boolean visible;
    private volatile int lastChecked; private static final VarHandle LAST_CHECKED = VarHandler.get(NettyTileEntity.class, "lastChecked", int.class);
    private char blockID;
    private PacketReplayData extraData;

    private static final int REMOVED = Integer.MIN_VALUE + 11;

    private final int x, y, z;
    public NettyTileEntity(BlockSpatial position, boolean visible, int lastChecked, char blockID) {
        x = position.blockX();
        y = position.blockY();
        z = position.blockZ();

        this.blockID = blockID;
        this.visible = visible;
        LAST_CHECKED.set(this, lastChecked);
    }

    public NettyTileEntity(int x, int y, int z, boolean visible, char blockID) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.visible = visible;
        this.blockID = blockID;
        LAST_CHECKED.set(this, NEVER_CHECKED);
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public TrackedTileEntity<PacketReplayData> setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public int lastChecked() {
        return (int) LAST_CHECKED.getAcquire(this);
    }

    @Override
    public TrackedTileEntity<PacketReplayData> setLastChecked(int checkedTick) {
        int current = (int) LAST_CHECKED.getAcquire(this);
        while (current != REMOVED) {
            if (LAST_CHECKED.weakCompareAndSetRelease(this, current, checkedTick)) {
                break;
            }
            current = (int) LAST_CHECKED.getAcquire(this);
        }
        return this;
    }

    /** Marks this allocation as detached. This state is absorbing because removed nodes are never reused. */
    public final void markRemoved() {
        LAST_CHECKED.setRelease(this, REMOVED);
    }

    /** Consumer-side lifecycle check performed immediately before emitting a queued transition. */
    public final boolean isRemoved() {
        return (int) LAST_CHECKED.getAcquire(this) == REMOVED;
    }

    @Override
    public int blockID() {
        return blockID;
    }

    @Override
    public TrackedTileEntity<PacketReplayData> setBlockID(char blockID) {
        this.blockID = blockID;
        return this;
    }

    @Override
    public PacketReplayData extraData() {
        return extraData;
    }

    @Override
    public TrackedTileEntity<PacketReplayData> setExtraData(PacketReplayData extraData) {
        this.extraData = extraData;
        return this;
    }

    @Override
    public void clearExtraData() {
        if (extraData != null) {
            //extraData.clear(); probs not needed
            extraData = null;
        }
    }

    @Override
    public int blockX() {
        return x;
    }

    @Override
    public int blockY() {
        return y;
    }

    @Override
    public int blockZ() {
        return z;
    }

    @Override
    public void clear() {
        if (extraData != null) {
            extraData.clear();
        }
    }

    @Override
    public String toString() {
        return toStringForm();
    }
}
