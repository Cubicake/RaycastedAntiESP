/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;

import java.lang.invoke.VarHandle;

/** Just a shorter alias of useful methods from ConcurrentUtil**/
public final class VarHandler {
    public static final VarHandle BYTE_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(byte[].class);
    public static final VarHandle CHAR_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(char[].class);
    public static final VarHandle LONG_ARRAY_HANDLE = ConcurrentUtil.getArrayHandle(long[].class);

    private VarHandler() {}

    public static VarHandle get(Class<?> classOf, String fieldName, Class<?> fieldClass) {
        return ConcurrentUtil.getVarHandle(classOf, fieldName, fieldClass);
    }

    public static VarHandle $tatic(Class<?> classOf, String fieldName, Class<?> fieldClass) {
        return ConcurrentUtil.getStaticVarHandle(classOf, fieldName, fieldClass);
    }
}
