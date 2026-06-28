/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.utils.*;
import games.cubi.raycastedantiesp.core.utils.Packet.Packets;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable.NO_VEHICLE;

/**
 * Per-player mutable state intended for Netty-side packet tracking and deferred reconciliation.
 */
public class NettyData implements Clearable {
    private static final int DEFAULT_MAP_SIZE = 16;
    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START Leash tracking:
    //
    private final Int2ObjectArrayMap<int[]> unresolvedLeashedEntityIDsByHolderID = new Int2ObjectArrayMap<>(DEFAULT_MAP_SIZE);

    public void addUnresolvedLeash(int holderEntityID, int leashedEntityID) {
        unresolvedLeashedEntityIDsByHolderID.compute(holderEntityID, (ignored, existing) -> {
            if (PrimitiveIntArrayList.contains(existing, leashedEntityID)) {
                return existing;
            }
            return PrimitiveIntArrayList.add(existing, leashedEntityID);
        });
    }

    public boolean removeUnresolvedLeash(int holderEntityID, int leashedEntityID) {
        final boolean[] removed = new boolean[1];
        unresolvedLeashedEntityIDsByHolderID.computeIfPresent(holderEntityID, (ignored, existing) -> {
            if (!PrimitiveIntArrayList.contains(existing, leashedEntityID)) {
                return existing;
            }
            removed[0] = true;
            int[] updated = PrimitiveIntArrayList.remove(existing, leashedEntityID);
            return PrimitiveIntArrayList.isEmpty(updated) ? null : updated;
        });
        return removed[0];
    }

    public int[] consumeUnresolvedLeashes(int holderEntityID) {
        return unresolvedLeashedEntityIDsByHolderID.remove(holderEntityID);
    }

    public void removeUnresolvedLeashedEntityFromAll(int leashedEntityID) {
        ObjectIterator<Int2ObjectMap.Entry<int @IntArrayListMarker []>> iterator = unresolvedLeashedEntityIDsByHolderID.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<int @IntArrayListMarker []> entry = iterator.next();
            int[] existing = entry.getValue();
            if (!PrimitiveIntArrayList.contains(existing, leashedEntityID)) {
                continue;
            }
            int[] updated = PrimitiveIntArrayList.remove(existing, leashedEntityID);
            if (PrimitiveIntArrayList.isEmpty(updated)) {
                iterator.remove();
                continue;
            }
            entry.setValue(updated);
        }
    }
    //
    // END Leash tracking.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START Passenger tracking:
    //
    /**
     * Latest unresolved full passenger list for a vehicle.
     * This is only needed while at least one referenced passenger or the vehicle-side reconciliation has still not been spawned in for the client. The actual client deals with this somehow but we need the entity to have been spawned in before we can register the passenger relationship.
     */
    private final Int2ObjectArrayMap<int[]> unresolvedPassengerIDsByVehicleID = new Int2ObjectArrayMap<>(DEFAULT_MAP_SIZE);
    /**
     * Reverse lookup for unresolved passenger relationships.
     * Lets a later passenger spawn discover which vehicle most recently claimed it as a passenger.
     * This may grow larger than {@link #unresolvedPassengerIDsByVehicleID} if vehicles have several passengers, so it's an open hash map.
     */
    private final Int2IntOpenHashMap unresolvedVehicleIDsByPassengerID = new Int2IntOpenHashMap(DEFAULT_MAP_SIZE);

    {
        unresolvedVehicleIDsByPassengerID.defaultReturnValue(NO_VEHICLE);
    }

    public int[] getUnresolvedPassengers(int vehicleEntityID) {
        return PrimitiveIntArrayList.getCopyOrNull(unresolvedPassengerIDsByVehicleID.get(vehicleEntityID));
    }

    /**
     * Replaces any previous unresolved passenger state for this vehicle.
     * The passenger packet is authoritative, so the latest packet wins.
     */
    public void setUnresolvedPassengers(int vehicleEntityID, int[] passengerIDs) {
        consumeUnresolvedPassengers(vehicleEntityID);
        if (PrimitiveIntArrayList.isEmpty(passengerIDs)) {
            return;
        }
        int[] copiedPassengerIDs = passengerIDs.clone();
        unresolvedPassengerIDsByVehicleID.put(vehicleEntityID, copiedPassengerIDs);
        for (int passengerID : copiedPassengerIDs) {
            int previousVehicleID = unresolvedVehicleIDsByPassengerID.put(passengerID, vehicleEntityID);
            if (previousVehicleID != NO_VEHICLE && previousVehicleID != vehicleEntityID) {
                removePassengerFromUnresolvedVehicle(previousVehicleID, passengerID);
            }
        }
    }

    public int[] consumeUnresolvedPassengers(int vehicleEntityID) {
        int[] existing = unresolvedPassengerIDsByVehicleID.remove(vehicleEntityID);
        if (PrimitiveIntArrayList.isEmpty(existing)) {
            return existing;
        }
        for (int passengerID : existing) {
            if (unresolvedVehicleIDsByPassengerID.get(passengerID) == vehicleEntityID) {
                unresolvedVehicleIDsByPassengerID.remove(passengerID);
            }
        }
        return existing;
    }

    public int getUnresolvedVehicleForPassenger(int passengerEntityID) {
        return unresolvedVehicleIDsByPassengerID.get(passengerEntityID);
    }

    public boolean removeUnresolvedPassengerLink(int passengerEntityID, int vehicleEntityID) {
        if (unresolvedVehicleIDsByPassengerID.get(passengerEntityID) != vehicleEntityID) {
            return false;
        }
        unresolvedVehicleIDsByPassengerID.remove(passengerEntityID);
        removePassengerFromUnresolvedVehicle(vehicleEntityID, passengerEntityID);
        return true;
    }

    public int consumeUnresolvedVehicleForPassenger(int passengerEntityID) {
        int vehicleEntityID = unresolvedVehicleIDsByPassengerID.remove(passengerEntityID);
        if (vehicleEntityID == NO_VEHICLE) {
            return NO_VEHICLE;
        }
        removePassengerFromUnresolvedVehicle(vehicleEntityID, passengerEntityID);
        return vehicleEntityID;
    }

    private void removePassengerFromUnresolvedVehicle(int vehicleEntityID, int passengerEntityID) {
        unresolvedPassengerIDsByVehicleID.computeIfPresent(vehicleEntityID, (ignored, existing) -> {
            if (!PrimitiveIntArrayList.contains(existing, passengerEntityID)) {
                return existing;
            }
            int[] updated = PrimitiveIntArrayList.remove(existing, passengerEntityID);
            return PrimitiveIntArrayList.isEmpty(updated) ? null : updated;
        });
    }
    //
    // END Passenger tracking.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START Netty entity spawn task queue:
    //
    private final Int2ObjectArrayMap<EntitySpawnTask> pendingPostEntitySpawnTasksByEntityID = new Int2ObjectArrayMap<>(16); // shot in the dark guess at capacity here. Can't be the more generic Int2ObjectMap because that doesn't expose a fast iterator.
    private volatile boolean evictPendingPostSpawnTasksOnNextPacket = false;
    private static final VarHandle EVICT_PENDING_POST_SPAWN_TASKS_ON_NEXT_PACKET_HANDLE; //TBH there is minimal advantage to using a VarHandle over an AtomicBoolean here, just felt like it.
    static {
        try {
            EVICT_PENDING_POST_SPAWN_TASKS_ON_NEXT_PACKET_HANDLE = MethodHandles.lookup().findVarHandle( NettyData.class, "evictPendingPostSpawnTasksOnNextPacket", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    /**
     * This is intended for reconciliation tasks due to Minecraft sending packets out of order. For example, {@link Packets#ENTITY_EQUIPMENT} is sent before the corresponding {@link Packets#SPAWN_ENTITY} packet, so caching of the equipment packet must await the spawn packet.
     * @param entityID The entity ID to associate the task with. Immediately after a {@link Packets#SPAWN_ENTITY} packet is processed for this entity ID, the task will be consumed and run.
     * @param task The task to run.
     */
    public void addPostEntitySpawnTask(int entityID, EntitySpawnTask task) {
        if (task.getNext() != null) {
            Logger.errorAndReturn(new IllegalArgumentException("Pending netty task was chained before queueing. Task=" + task), 4, NettyData.class);
        }
        EntitySpawnTask existing = pendingPostEntitySpawnTasksByEntityID.get(entityID);
        if (existing == null) {
            pendingPostEntitySpawnTasksByEntityID.put(entityID, task);
            return;
        }
        existing.appendLinkedTask(task);
    }

    public void runPendingPostSpawnTaskForEntity(int entityID) {
        EntitySpawnTask pendingTasks = consumePendingPostSpawnTasksForEntity(entityID);
        if (pendingTasks != null) {
            pendingTasks.runLinkedTasks();
        }
    }

    public void evictPendingPostSpawnTasksIfRequired(int currentTick) {
        if (EVICT_PENDING_POST_SPAWN_TASKS_ON_NEXT_PACKET_HANDLE.compareAndSet(this, true, false)) evictOldPendingPostSpawnTasks(currentTick);
    }

    public void markPendingPostSpawnTasksForEviction() {
        evictPendingPostSpawnTasksOnNextPacket = true;
    }

    public EntitySpawnTask consumePendingPostSpawnTasksForEntity(int entityID) {
        return pendingPostEntitySpawnTasksByEntityID.remove(entityID);
    }

    public void clearPendingPostSpawnTasksForEntity(int entityID) {
        pendingPostEntitySpawnTasksByEntityID.remove(entityID);
    }

    public void evictOldPendingPostSpawnTasks(int currentTick) {
        ObjectIterator<Int2ObjectMap.Entry<EntitySpawnTask>> iterator = pendingPostEntitySpawnTasksByEntityID.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<EntitySpawnTask> entry = iterator.next();
            EntitySpawnTask survivingHead = entry.getValue().trimExpiredTasks(currentTick);
            if (survivingHead == null) {
                iterator.remove();
            } else {
                entry.setValue(survivingHead);
            }
        }
    }

    //
    // END Netty entity spawn task queue.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START Self entity tracking:
    //
    private final NettyEntityLocatable<?, ?> selfEntity;
    private final int selfEntityID;

    public NettyData(NettyEntityLocatable<?, ?> selfEntity) {
        this.selfEntity = selfEntity;
        this.selfEntityID = selfEntity.entityID();
    }

    public NettyEntityLocatable<?, ?> getSelfEntity() {
        return selfEntity;
    }

    public int getSelfEntityID() {
        return selfEntityID;
    }

    public boolean isSelfEntityID(int entityID) {
        return entityID == selfEntityID;
    }
    //
    // END Self entity tracking.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START World-transition destroy tracking:
    //
    private int @IntArrayListMarker [] expectedWorldTransitionDestroyEntityIDs;

    /**
     * When changing worlds, the server sends destroy packets for all the entities in the old world, but we want to clear the view. Tracking them here allows those destroy packets to be handled correctly.
     */
    public void setExpectedWorldTransitionDestroyEntityIDs(int[] expectedEntityIDs) {
        expectedWorldTransitionDestroyEntityIDs = PrimitiveIntArrayList.isEmpty(expectedEntityIDs) ? null : expectedEntityIDs.clone();
    }

    public boolean consumeExpectedWorldTransitionDestroyEntityID(int entityID) {
        if (!PrimitiveIntArrayList.contains(expectedWorldTransitionDestroyEntityIDs, entityID)) {
            return false;
        }
        expectedWorldTransitionDestroyEntityIDs = PrimitiveIntArrayList.remove(expectedWorldTransitionDestroyEntityIDs, entityID);
        if (PrimitiveIntArrayList.isEmpty(expectedWorldTransitionDestroyEntityIDs)) {
            expectedWorldTransitionDestroyEntityIDs = null;
        }
        return true;
    }
    //
    // END World-transition destroy tracking.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START World tracking:
    //

    private int currentWorldMinHeight = Integer.MIN_VALUE; // Netty thread access only
    private String currentWorldName = null; // Netty thread access only

    public int getCurrentWorldMinHeight() {
        if (currentWorldMinHeight == Integer.MIN_VALUE) {
            Logger.error(new IllegalStateException("Current world min height was requested before it was set"), 3, NettyData.class);
            return -64;
        }
        return currentWorldMinHeight;
    }

    public NettyData setCurrentWorldMinHeight(int currentWorldMinHeight) {
        this.currentWorldMinHeight = currentWorldMinHeight;
        return this;
    }

    /**
     *
     * @return The current world name, or null if the player is still in the process of joining the server.
     */
    public @Nullable String getCurrentWorldName() {
        return currentWorldName;
    }

    public NettyData setCurrentWorldName(String currentWorldName) {
        this.currentWorldName = currentWorldName;
        return this;
    }

    //
    // END World tracking.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    public void clearPendingReconciliationState() {
        unresolvedLeashedEntityIDsByHolderID.clear();
        unresolvedPassengerIDsByVehicleID.clear();
        unresolvedVehicleIDsByPassengerID.clear();
        pendingPostEntitySpawnTasksByEntityID.clear();
        evictPendingPostSpawnTasksOnNextPacket = false;
    }

    @Override
    public void clear() {
        clearPendingReconciliationState();
        expectedWorldTransitionDestroyEntityIDs = null;
        if (selfEntity != null) {
            selfEntity.clear();
        }
        currentWorldMinHeight = Integer.MIN_VALUE;
        currentWorldName = null;
    }
}
