/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.replaydata;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import games.cubi.utils.Clearable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;

/** Replay state owned by the viewer's Netty thread. */
public final class PacketEventsEntityReplayData implements Clearable {
    private final Queue<PacketWrapper<?>> packets = new ArrayDeque<>();
    private final Metadata metadata;

    private PacketEventsEntityReplayData(int metadataFieldCount) {
        metadata = new Metadata(metadataFieldCount);
    }

    public static PacketEventsEntityReplayData create() {
        return create(0);
    }

    public static PacketEventsEntityReplayData create(int metadataFieldCount) {
        return new PacketEventsEntityReplayData(metadataFieldCount);
    }

    public void addPacket(PacketWrapper<?> packet) {
        if (packet instanceof WrapperPlayServerEntityMetadata metadataPacket) {
            mergeMetadata(metadataPacket.getEntityMetadata());
            return;
        }
        packets.add(packet);
    }

    void mergeMetadata(@Nullable List<EntityData<?>> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        for (EntityData<?> field : updates) {
            metadata.update(field);
        }
    }

    public List<EntityData<?>> metadataForReplay() {
        return metadata;
    }

    /** Treat the packet queue as read-only. */
    public Queue<PacketWrapper<?>> getPackets() {
        return packets;
    }

    @Override
    public void clear() {
        packets.clear();
        metadata.reset();
    }

    // This is a List because PE wants a list
    private static final class Metadata extends AbstractList<EntityData<?>> {
        private static final int METADATA_TERMINATOR = 0xFF;
        private static final EntityData<?>[] EMPTY_FIELDS = new EntityData<?>[0];

        private EntityData<?>[] fields;
        private int size;

        private Metadata(int fieldCount) {
            fields = fieldCount == 0 ? EMPTY_FIELDS : new EntityData<?>[fieldCount]; // self-entities do not have metadata storage
        }

        private <T> void update(EntityData<T> field) {
            int index = field.getIndex();
            if (index < 0 || index >= METADATA_TERMINATOR) {
                return;
            }
            if (index >= fields.length) {
                int capacity = Math.max(index + 1, fields.length * 2);
                fields = Arrays.copyOf(fields, Math.min(capacity, METADATA_TERMINATOR));
            }

            EntityData<?> cached = fields[index];
            if (cached != null && cached.getType() == field.getType()) {
                // Both entries use the same serializer and therefore the same value type.
                @SuppressWarnings("unchecked")
                EntityData<T> typedEntry = (EntityData<T>) cached;
                typedEntry.setValue(field.getValue());
                return;
            }

            fields[index] = new EntityData<>(index, field.getType(), field.getValue());
            if (cached == null) {
                size++;
                modCount++;
            }
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public EntityData<?> get(int index) {
            Objects.checkIndex(index, size);
            int fieldIndex = nextPopulatedIndex(0);
            for (int i = 0; i < index; i++) {
                fieldIndex = nextPopulatedIndex(fieldIndex + 1);
            }
            return fields[fieldIndex];
        }

        @Override
        public @NotNull Iterator<EntityData<?>> iterator() {
            return new Iterator<>() {
                private int nextIndex;
                private int remaining = size;

                @Override
                public boolean hasNext() {
                    return remaining > 0;
                }

                @Override
                public EntityData<?> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    nextIndex = nextPopulatedIndex(nextIndex);
                    remaining--;
                    return fields[nextIndex++];
                }
            };
        }

        private int nextPopulatedIndex(int start) {
            while (fields[start] == null) {
                start++;
            }
            return start;
        }

        private void reset() {
            Arrays.fill(fields, null);
            size = 0;
            modCount++;
        }
    }
}
