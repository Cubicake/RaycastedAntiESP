/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.view;

import ca.spottedleaf.concurrentutil.map.SWMRHashTable;
import games.cubi.locatables.api.Spatial;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.utils.SingleThreadedGuard;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.EntityViewTransition;
import games.cubi.raycastedantiesp.core.view.PackedEntityTransitionQueue;
import games.cubi.raycastedantiesp.packetevents.tracked.PacketEventsEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class PacketEventsEntityView extends SingleThreadedGuard implements EntityView<PacketEventsEntity> {
    private final SWMRHashTable<UUID, PacketEventsEntity> entitiesByUUID = new SWMRHashTable<>();
    private final Int2ObjectOpenHashMap<UUID> entityUUIDsByID = new Int2ObjectOpenHashMap<>();
    private final PackedEntityTransitionQueue transitions = new PackedEntityTransitionQueue();
    private final boolean isPlayerView;
    private final IntSupplier worldEpochSupplier;
    private UUID trackedWorld;

    private PacketEventsEntityView(boolean isPlayerView, IntSupplier worldEpochSupplier) {
        super(Thread.currentThread()); // Should be player's netty thread
        this.isPlayerView = isPlayerView;
        this.worldEpochSupplier = Logger.requireNonNull(worldEpochSupplier, "worldEpochSupplier was null", 1, this.getClass());
    }

    public static PacketEventsEntityView createPlayerView(IntSupplier worldEpochSupplier) {
        return new PacketEventsEntityView(true, worldEpochSupplier);
    }

    public static PacketEventsEntityView createEntityView(IntSupplier worldEpochSupplier) {
        return new PacketEventsEntityView(false, worldEpochSupplier);
    }

    @Override
    public void insertEntity(UUID world, PacketEventsEntity entity) {
        if (world == null || entity == null || entity.entityUUID() == null) {
            Logger.error(new RuntimeException("Attempted to insert null entity or entity with null UUID into EntityView"), 2, PacketEventsEntityView.class);
            return;
        }

        UUID entityUUID = entity.entityUUID();
        int entityID = entity.entityID();
        guardThread();
        ensureTrackedWorld(world);
        UUID previousUUIDForID = entityUUIDsByID.put(entityID, entityUUID);
        if (previousUUIDForID != null && !previousUUIDForID.equals(entityUUID)) {
            PacketEventsEntity previousEntityForID = entitiesByUUID.get(previousUUIDForID);
            if (previousEntityForID != null && previousEntityForID.entityID() == entityID && entitiesByUUID.remove(previousUUIDForID, previousEntityForID)) {
                previousEntityForID.clear();
            }
        }

        PacketEventsEntity previousEntityForUUID = entitiesByUUID.put(entityUUID, entity);
        if (previousEntityForUUID != null && previousEntityForUUID.entityID() != entityID) {
            entityUUIDsByID.remove(previousEntityForUUID.entityID(), entityUUID);
        }
        if (previousEntityForUUID != null && previousEntityForUUID != entity) {
            previousEntityForUUID.clear();
        }
    }

    @Override
    public void removeEntity(int entityID, int currentTick) {
        removeEntity(entityID);
    }

    @Override
    public void removeEntity(int entityID) {
        guardThread();
        UUID entityUUID = entityUUIDsByID.remove(entityID);
        if (entityUUID == null) {
            return;
        }
        PacketEventsEntity removed = entitiesByUUID.remove(entityUUID);
        if (removed == null) {
            return;
        }
        removed.clear();
    }

    @Override
    public void removeEntity(UUID entityUUID, int currentTick) {
        int entityID = getEntityID(entityUUID);

        removeEntity(entityID, currentTick);
    }

    @Override
    public PacketEventsEntity getEntity(UUID entityUUID) {
        return entitiesByUUID.get(entityUUID);
    }

    @Override
    public PacketEventsEntity getEntity(int entityID) {
        return getTrackedEntity(entityID);
    }

    @Override
    public boolean exists(UUID entityUUID) {
        return entitiesByUUID.containsKey(entityUUID);
    }

    @Override
    public boolean exists(int entityID) {
        guardThread();
        return entityUUIDsByID.containsKey(entityID);
    }

    @Override
    public int size() {
        return entitiesByUUID.size();
    }

    @Override
    public boolean isVisible(int entityID) {
        PacketEventsEntity entity = getTrackedEntity(entityID);
        if (entity == null) {
            Logger.errorAndReturn(new RuntimeException("Entity with ID " + entityID + " does not exist in EntityView"), 3, PacketEventsEntityView.class);
        }
        return entity.visible();
    }

    @Override
    public Spatial getPosition(UUID entityUUID) {
        return entitiesByUUID.get(entityUUID);
    }

    @Override
    public int getEntityID(UUID entityUUID) {
        PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
        return entity == null ? -1 : entity.entityID();
    }

    @Override
    public boolean isVisible(UUID entityUUID, int currentTick) {
        return isVisible(entityUUID);
    }

    @Override
    public boolean isVisible(UUID entityUUID) {
        PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
        return entity == null || entity.visible();
    }

    @Override
    public void setVisibility(@NotNull NettyEntity<?> entity, boolean visible, int currentTick, int expectedWorldEpoch) {
        boolean visibilityChanged = entity.visible() != visible;
        if (!recordDirectVisibility(entity, visible, currentTick, expectedWorldEpoch)) {
            return;
        }
        if (visibilityChanged) {
            transitions.add(
                    visible ? EntityViewTransition.Type.SHOW : EntityViewTransition.Type.HIDE,
                    entity,
                    expectedWorldEpoch
            );
        }
    }

    @Override
    public boolean recordDirectVisibility(@NotNull NettyEntity<?> entity, boolean visible, int currentTick, int expectedWorldEpoch) {
        if (!isCurrentWorldEpoch(expectedWorldEpoch)) {
            return false;
        }
        if (entitiesByUUID.get(entity.entityUUID()) != entity) {
            return false;
        }
        if (entity.isSelfEntity()) {
            return false;
        }
        entity.setVisible(visible);
        entity.setLastChecked(currentTick);
        return true;
    }

    @Override
    public Collection<UUID> getKnownEntities() {
        List<UUID> known = new ArrayList<>(entitiesByUUID.size());
        entitiesByUUID.forEachKey(known::add);
        return known;
    }

    @Override
    public int[] getKnownEntityIDs() {
        guardThread();
        return entityUUIDsByID.keySet().toIntArray();
    }

    @Override
    public int forEachNeedingRecheck(int visibleRecheckTicks, int currentTick, Consumer<UUID> action) {
        int processed = 0;
        for (PacketEventsEntity state : entitiesByUUID.values()) {
            if (shouldSkipCheck(state.visible(), visibleRecheckTicks, state.lastChecked(), currentTick)) {
                continue;
            }
            action.accept(state.entityUUID());
            processed++;
        }
        return processed;
    }

    @Override
    public int forEachNeedingRecheckEntity(int visibleRecheckTicks, int currentTick, boolean countingActuallyNeeded, int expectedWorldEpoch, Consumer<NettyEntity<?>> action) {
        if (!isCurrentWorldEpoch(expectedWorldEpoch)) {
            return 0;
        }
        if (countingActuallyNeeded) {
            return entitiesByUUID.forEachValueCounted( (entity) -> {
                if (shouldSkipCheck(entity.visible(), visibleRecheckTicks, entity.lastChecked(), currentTick)) {
                    return false;
                }
                action.accept(entity);
                return true;
            });
        }
        entitiesByUUID.forEachValue( (entity) -> {
            if (shouldSkipCheck(entity.visible(), visibleRecheckTicks, entity.lastChecked(), currentTick)) {
                return;
            }
            action.accept(entity);
        });
        return 0;
    }

    private boolean shouldSkipCheck(boolean currentlyVisible, int visibleRecheckTicks, int lastChecked, int currentTick) {
        return  currentlyVisible // If not currently visible checks always run
                && ((visibleRecheckTicks < 0) // If recheck is disabled and entity is visible, skip
                    || (lastChecked > 0 && currentTick - lastChecked < visibleRecheckTicks)); // If last checked is set and the difference between last checked and now is less than visible recheck ticks, skip.
    }

    @Override
    public boolean hasPendingTransitions() {
        return transitions.hasPendingTransitions();
    }

    @Override
    public void flushPendingTransitions() {
        transitions.flushPendingTransitions();
    }

    @Override
    public void drainTransitions(EntityView.TransitionConsumer consumer) {
        transitions.drainTransitions(consumer);
    }

    @Override
    public boolean isPlayerView() {
        return isPlayerView;
    }

    @Override
    public void clear() {
        guardThread();
        trackedWorld = null;
        clearTrackedState();
    }

    private void ensureTrackedWorld(UUID world) {
        if (world.equals(trackedWorld)) {
            return;
        }
        trackedWorld = null;
        clearTrackedState();
        trackedWorld = world;
    }

    private void clearTrackedState() {
        entitiesByUUID.clear();
        entityUUIDsByID.clear();
        transitions.clearPublishedTransitions();
    }

    private boolean isCurrentWorldEpoch(int expectedWorldEpoch) {
        return PlayerData.isStableWorldEpoch(expectedWorldEpoch) && worldEpochSupplier.getAsInt() == expectedWorldEpoch;
    }

    private PacketEventsEntity getTrackedEntity(int entityID) {
        guardThread();
        UUID entityUUID = entityUUIDsByID.get(entityID);
        return entityUUID == null ? null : entitiesByUUID.get(entityUUID);
    }

    public String getStringDataForDebugging() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityView isPlayerView=").append(isPlayerView).append("\n");
        guardThread();
        entityUUIDsByID.forEach((entityID, entityUUID) -> {
            PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
            builder.append("EntityID=").append(entityID)
                    .append(" UUID=").append(entityUUID)
                    .append(" Entity=").append(entity)
                    .append("\n");
        });
        return builder.toString();
    }
}
