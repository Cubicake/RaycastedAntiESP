/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.replaydata;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;

import games.cubi.raycastedantiesp.core.utils.Clearable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public sealed interface PacketEventsEntityReplayData extends Clearable permits PacketEventsEntityReplayData.Impl {

    void addPacket(PacketWrapper<?> packet);

    /**
     * @return The actual queue, do not modify.
     */
    Queue<PacketWrapper<?>> getPackets();

    static PacketEventsEntityReplayData create() {
        return new Impl();
    }

    final class Impl implements PacketEventsEntityReplayData {
        private final Queue<PacketWrapper<?>> packets = new ArrayDeque<>();

        public Impl() {
        }

        public void addPacket(PacketWrapper<?> packet) {
            packets.add(packet);
        }

        @Override
        public Queue<PacketWrapper<?>> getPackets() {
            return packets;
        }

        @Override
        public void clear() {
            packets.clear();
        }
    }

}
