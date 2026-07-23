/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.lang.invoke.VarHandle;

/**
 * An append-only multithreaded int set backed by an open-addressing int hash set.
 * Designed for highly multithreaded reads and rare writes.
 */
public class CopyOnWriteIntSet implements AppendingMTIntSet {
    private volatile IntOpenHashSet backingSet; private static final VarHandle BACKING_SET = VarHandler.get(CopyOnWriteIntSet.class, "backingSet", IntOpenHashSet.class);

    public CopyOnWriteIntSet() {
        backingSet = new IntOpenHashSet();
    }

    public boolean contains(int value) {
        return ((IntOpenHashSet) BACKING_SET.getAcquire(this)).contains(value);
    }

    public void add(int value) {
        while (true) {
            IntOpenHashSet oldSet = (IntOpenHashSet) BACKING_SET.getAcquire(this);
            if (oldSet.contains(value)) return;

            IntOpenHashSet newSet = oldSet.clone();
            newSet.add(value);
            boolean success = BACKING_SET.weakCompareAndSetRelease(this, oldSet, newSet);
            if (success) return;
        }
    }
}
