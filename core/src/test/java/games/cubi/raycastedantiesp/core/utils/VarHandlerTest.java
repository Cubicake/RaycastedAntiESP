/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import org.junit.jupiter.api.Test;

import java.lang.invoke.VarHandle;

import static games.cubi.raycastedantiesp.core.utils.VarHandler.BYTE_ARRAY_HANDLE;
import static games.cubi.raycastedantiesp.core.utils.VarHandler.CHAR_ARRAY_HANDLE;
import static games.cubi.raycastedantiesp.core.utils.VarHandler.LONG_ARRAY_HANDLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VarHandlerTest {
    private int instanceValue;
    private static String staticValue;

    @Test
    void createsInstanceAndStaticFieldHandles() {
        VarHandle instanceHandle = VarHandler.get(VarHandlerTest.class, "instanceValue", int.class);
        VarHandle staticHandle = VarHandler.$tatic(VarHandlerTest.class, "staticValue", String.class);

        instanceHandle.set(this, 14);
        staticHandle.set("value");

        assertEquals(14, instanceValue);
        assertEquals("value", staticValue);
    }

    @Test
    void exposesPrimitiveArrayHandles() {
        byte[] bytes = new byte[1];
        char[] chars = new char[1];
        long[] longs = new long[1];

        BYTE_ARRAY_HANDLE.setOpaque(bytes, 0, (byte) 3);
        CHAR_ARRAY_HANDLE.setRelease(chars, 0, 'x');
        LONG_ARRAY_HANDLE.setOpaque(longs, 0, 9L);

        assertEquals((byte) 3, BYTE_ARRAY_HANDLE.getOpaque(bytes, 0));
        assertEquals('x', CHAR_ARRAY_HANDLE.getAcquire(chars, 0));
        assertEquals(9L, LONG_ARRAY_HANDLE.getOpaque(longs, 0));
    }
}
