/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import java.lang.invoke.VarHandle;
import java.util.Arrays;

public class IncrementingMTIntSet implements AppendingMTIntSet {
    private volatile int[] values = new int[0]; private static final VarHandle VALUES = VarHandler.get(IncrementingMTIntSet.class, "values", int[].class);

    public boolean contains(int value) {
        int[] valueSnapshot = (int[]) VALUES.getAcquire(this);
        return Arrays.binarySearch(valueSnapshot, value) >= 0;
    }

    public void add(int value) {
        while (true) {
            int[] oldValues = (int[]) VALUES.getAcquire(this);
            int result = Arrays.binarySearch(oldValues, value);

            if (result >= 0) return; //already in array
            int[] newValues = new int[oldValues.length + 1];
            int insertionPoint = -result - 1;

            System.arraycopy(oldValues, 0, newValues, 0, insertionPoint);
            newValues[insertionPoint] = value;
            // If the new value is larger than the old values, this will do nothing
            System.arraycopy(oldValues, insertionPoint, newValues, insertionPoint + 1, oldValues.length - insertionPoint);

            boolean success = VALUES.weakCompareAndSetRelease(this, oldValues, newValues);
            if (success) return;
        }
    }

    public boolean remove(int value) {
        while (true) {
            int[] oldValues = (int[]) VALUES.getAcquire(this);
            int result = Arrays.binarySearch(oldValues, value);

            if (result < 0) return false; //not in array
            int[] newValues = new int[oldValues.length - 1];

            System.arraycopy(oldValues, 0, newValues, 0, result);
            System.arraycopy(oldValues, result + 1, newValues, result, oldValues.length - result - 1);

            boolean success = VALUES.weakCompareAndSetRelease(this, oldValues, newValues);
            if (success) return true;
        }
    }
}
