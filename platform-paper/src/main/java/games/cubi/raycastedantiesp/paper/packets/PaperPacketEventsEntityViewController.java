/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ClassTreeIdRegistry;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;


public class PaperPacketEventsEntityViewController extends PacketEventsEntityViewController {
    private final int[] metadataFieldCounts;
    private final AtomicBoolean warnedAboutUnknownType = new AtomicBoolean();

    public PaperPacketEventsEntityViewController(IntSupplier currentTickSupplier) {
        super(currentTickSupplier);
        metadataFieldCounts = loadMetadataFieldCounts();
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);
    }

    @Override
    protected int metadataFieldCount(int entityTypeID) {
        if (entityTypeID >= 0 && entityTypeID < metadataFieldCounts.length) {
            return metadataFieldCounts[entityTypeID];
        }
        if (metadataFieldCounts.length > 0 && warnedAboutUnknownType.compareAndSet(false, true)) {
            Logger.warning("Unknown entity type ID " + entityTypeID + ". Using " + DEFAULT_METADATA_FIELD_COUNT + " metadata fields for unknown types.", 4, PaperPacketEventsEntityViewController.class);
        }
        return DEFAULT_METADATA_FIELD_COUNT;
    }

    private static int[] loadMetadataFieldCounts() {
        try {
            Field registryField = SynchedEntityData.class.getDeclaredField("ID_REGISTRY");
            registryField.setAccessible(true);
            ClassTreeIdRegistry metadataIDs = (ClassTreeIdRegistry) registryField.get(null);
            var entityTypes = BuiltInRegistries.ENTITY_TYPE;
            int[] counts = new int[entityTypes.size()];
            for (Field field : entityTypeFields()) {
                if (field.getType() != EntityType.class || !Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    if (!(field.getGenericType() instanceof ParameterizedType type)
                            || !(type.getActualTypeArguments()[0] instanceof Class<?> entityClass)) {
                        continue;
                    }
                    int typeID = entityTypes.getId((EntityType<?>) field.get(null));
                    if (typeID >= 0 && typeID < counts.length) {
                        // Entity classes register their metadata IDs during static initialization.
                        Class.forName(entityClass.getName(), true, entityClass.getClassLoader());
                        counts[typeID] = metadataIDs.getCount(entityClass);
                    }
                } catch (ReflectiveOperationException | TypeNotPresentException | LinkageError e) {
                    Logger.debug("Couldn't read metadata count for " + field.getName() + ": " + e);
                }
            }

            int unresolvedTypes = 0;
            for (int typeID = 0; typeID < counts.length; typeID++) {
                if (counts[typeID] == 0) {
                    counts[typeID] = DEFAULT_METADATA_FIELD_COUNT;
                    unresolvedTypes++;
                }
            }
            if (unresolvedTypes > 0) {
                Logger.warning("Using " + DEFAULT_METADATA_FIELD_COUNT + " metadata fields for " + unresolvedTypes
                        + " entity types with unavailable schemas.", 4, PaperPacketEventsEntityViewController.class);
            }
            return counts;
        } catch (ReflectiveOperationException | InaccessibleObjectException | LinkageError e) {
            Logger.warning("Using " + DEFAULT_METADATA_FIELD_COUNT + " metadata fields per type because the entity metadata registry could not be read: "
                    + e, 3, PaperPacketEventsEntityViewController.class);
            return new int[0];
        }
    }

    private static Field[] entityTypeFields() {
        // Minecraft 26.2 moved the typed constants to EntityTypes.
        try {
            return Class.forName("net.minecraft.world.entity.EntityTypes", false, EntityType.class.getClassLoader()).getFields();
        } catch (ClassNotFoundException ignored) {
            return EntityType.class.getFields();
        }
    }
}
