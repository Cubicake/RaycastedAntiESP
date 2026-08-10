/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.replaydata;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class PacketEventsTileEntityReplayDataTest {
    @Test
    void nbtIsCopiedOnWriteAndReadAndClearedWithReplayState() {
        PacketEventsTileEntityReplayData replayData = new PacketEventsTileEntityReplayData();
        NBTCompound source = new NBTCompound();

        replayData.setBlockEntityData(null, source);

        NBTCompound firstRead = replayData.nbt();
        assertNotNull(firstRead);
        assertNotSame(source, firstRead);
        assertNotSame(firstRead, replayData.nbt());

        replayData.clear();
        assertNull(replayData.blockEntityType());
        assertNull(replayData.nbt());
    }
}
