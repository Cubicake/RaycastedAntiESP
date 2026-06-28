/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.locatables.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.RaycastConfig;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.NettyData;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.utils.PrimitiveIntArrayList;
import games.cubi.raycastedantiesp.core.utils.Packet;
import games.cubi.raycastedantiesp.core.view.EntityView;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.UUID;

import static games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable.NO_LEASHER;
import static games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable.NO_VEHICLE;

/**
 * @param <P> The platform's packet wrapper (PacketWrapper<?>)
 */
public abstract class PacketEntityViewController<P> {
    private static PacketEntityViewController<?> SELF; //TODO Switch to LazyConstant once out of preview (see https://openjdk.org/jeps/526)

    {
        synchronized (PacketEntityViewController.class) {
            if (SELF != null) {
                throw new IllegalStateException("Multiple instances of PacketEventsEntityViewController created.");
            }
            SELF = this;

        }
    }

    protected static PacketEntityViewController<?> get() {
        return SELF;
    }

    protected EntityConfig entityConfig = null;
    protected PlayerConfig playerConfig = null;
    protected double hideOnSpawnEntityDistanceSquared = 0;
    protected double hideOnSpawnPlayerDistanceSquared = 0;

    protected void handleWorldStatePacket(UUID player, String world, int minWorldHeight, int currentTick) {
        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player);
        if (playerData == null) {
            Logger.error("Received world state packet for unknown player, uuid=" + player, 2, PacketEntityViewController.class);
            return;
        }
        if (world == null) {
            Logger.error("Received world state packet without a world name, uuid=" + player, 2, PacketEntityViewController.class);
            return;
        }

        NettyData nettyData = playerData.nettyData();
        String previousWorld = nettyData.getCurrentWorldName();
        if (world.equals(previousWorld)) {
            nettyData.setCurrentWorldMinHeight(minWorldHeight);
            return;
        }

        if (previousWorld != null) {
            nettyData.setExpectedWorldTransitionDestroyEntityIDs(drainRemainingEntityIDs(playerData));
            playerData.blockView().clear();
            playerData.entityView().clear();
            playerData.playerView().clear();
            nettyData.clearPendingReconciliationState();
        }
        nettyData.setCurrentWorldName(world).setCurrentWorldMinHeight(minWorldHeight);
    }

    protected PlayerData handlePlayPhaseLoginPacket(int entityID, UUID playerUUID, int currentTick) {
        return PlayerRegistry.getInstance().registerAndGetPlayer(playerUUID, currentTick, entityID, this::createSelfEntity);
    }

    protected abstract NettyEntityLocatable<?,?> createSelfEntity(PlayerData ownData, int entityID, UUID playerUUID);

    protected void handlePlayerDisconnect(UUID player) {
        if (player == null) {
            return;
        }
        PlayerRegistry.getInstance().unregisterPlayer(player);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    @Packet(Packet.Packets.SPAWN_ENTITY)
    protected boolean handleEntitySpawn(P packet, int entityID, boolean isPlayer, PlayerData playerData, UUID world, int currentTick) {
        boolean returnValue = handleEntitySpawn0(packet, isPlayer, playerData, world, currentTick);
        playerData.nettyData().runPendingPostSpawnTaskForEntity(entityID);
        return returnValue;
    }

    protected boolean handleEntitySpawn0(P packet, boolean isPlayer, PlayerData playerData, UUID world, int currentTick) {
        if (world == null) {
            Logger.error(new RuntimeException("World null when handling spawn entity packet, uuid=" + playerData.getPlayerUUID() + " tick=" + currentTick), 2, PacketEntityViewController.class);
            return false;
        }

        NettyEntityLocatable<?,?> entity = Logger.requireNonNull(processEntitySpawn(playerData, packet, world, currentTick), "processEntitySpawn returned null", 3, PacketEntityViewController.class);

        if ((!isPlayer && entityConfig.enabled()) || isPlayer && playerConfig.enabled()) {
            Locatable ownLocation = playerData.ownLocation();
            boolean staleOwnLocation = ownLocation == null || ownLocation.world() == null || !ownLocation.world().equals(world);
            double distanceSquared = staleOwnLocation ? Double.POSITIVE_INFINITY : ownLocation.distanceSquared(entity);
            if (distanceSquared > (isPlayer ? hideOnSpawnPlayerDistanceSquared : hideOnSpawnEntityDistanceSquared)) {
                entity.setVisible(false);
                entity.setClientVisible(false);
                if (isPlayer) {
                    insertEntityToPlayerView(entity, playerData);
                    return true;
                }
                insertEntityToEntityView(entity, playerData);
                return true;
            }
        } else {
            entity.setClientVisible(true);
        }
        if (isPlayer) {
            insertEntityToPlayerView(entity, playerData);
            return false;
        }
        insertEntityToEntityView(entity, playerData);
        return false;
    }

    @Packet(Packet.Packets.ENTITY_ANIMATION)
    protected boolean handleEntityAnimation(int entityID, PlayerData playerData) {
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    @Packet(Packet.Packets.ENTITY_EVENT)
    protected boolean handleEntityEvent(int entityID, PlayerData playerData) {
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    @Packet(Packet.Packets.HURT_ANIMATION)
    protected boolean handleHurtAnimation(int entityID, PlayerData playerData) {
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleRelativeMove(P packet, PlayerData playerData, int currentTick) {
        int entityID = processRelativeMovePacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleRelativeMoveAndRotation(P packet, PlayerData playerData, int currentTick) {
        int entityID = processRelativeMoveAndRotationPacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleTeleport(P packet, PlayerData playerData, int currentTick) {
        int entityID = processTeleportPacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handlePositionSync(P packet, PlayerData playerData, int currentTick) {
        int entityID = processPositionSyncPacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityRotation(P packet, PlayerData playerData, int currentTick) {
        int entityID = processRotationPacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityHeadLook(P packet, PlayerData playerData, int currentTick) {
        int entityID = processHeadLookPacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityMetadata(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleRemoveEntityEffect(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    @Packet(Packet.Packets.ENTITY_EQUIPMENT)
    protected boolean handleEntityEquipment(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityVelocity(P packet, int entityID, PlayerData playerData, int currentTick) {
        processEntityVelocityPacket(packet, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityEffect(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityPassengers(int entityID, int[] passengers, PlayerData playerData, int currentTick) {
        NettyEntityLocatable<?,?> vehicle = playerData.entityFromID(entityID);
        int[] previousPassengers = getPreviousPassengerState(entityID, vehicle, playerData);
        playerData.nettyData().consumeUnresolvedPassengers(entityID); // throw away any unresolved passengers as this new packet will have the correct passenger list
        // "Stale" passengers are passengers that the previous authoritative state said were mounted,
        // but the new full replacement passenger list no longer contains.
        clearStalePassengerReferences(entityID, previousPassengers, passengers, playerData);
        if (vehicle == null) {
            playerData.nettyData().setUnresolvedPassengers(entityID, passengers);
            return false;
        }
        return handleEntityPassengersNow(vehicle, passengers, playerData, currentTick);
    }

    //This (and leash handling) leaks some info to the client, as it will receive the passenger packet even if the passengers are auto-hidden once parsed, but as the packet doesn't include any location or type info, this shouldn't be too incriminating.
    boolean handleEntityPassengersNow(NettyEntityLocatable<?,?> entity, int[] passengers, PlayerData playerData, int currentTick) {
        int entityID = entity.entityID();
        entity.setPassengerIDs(passengers);
        int[] unresolvedPassengers = null;
        boolean selfIsPassenger = false;
        for (int passengerID : passengers) {
            NettyEntityLocatable<?,?> passenger = playerData.entityFromID(passengerID);
            if (passenger == null) {
                unresolvedPassengers = PrimitiveIntArrayList.add(unresolvedPassengers, passengerID);
                continue;
            }
            if (passenger.isSelfEntity()) {
                selfIsPassenger = true;
            }
            passenger.setVehicleID(entityID);
        }
        playerData.nettyData().setUnresolvedPassengers(entityID, unresolvedPassengers);
        checkVehicle(entity, playerData);
        boolean cancelForForcedVehicleShow = selfIsPassenger && forceVisibleBecauseAttachedToSelf(entity, playerData, currentTick, "self-passenger vehicle");
        if (cancelForForcedVehicleShow) {
            return true;
        }
        if (cancelIfEnabledAndHidden(entityID, playerData)) return true;
        boolean passengersNotVisible = false;
        IntArrayList visiblePassengers = new IntArrayList(passengers.length);
        for (int passengerID : passengers) {
            NettyEntityLocatable<?,?> passenger = playerData.entityFromID(passengerID);
            if (passenger == null) {
                visiblePassengers.add(passengerID);
            }
            else if (entity.isSelfEntity() && forceVisibleBecauseAttachedToSelf(passenger, playerData, currentTick, "self-vehicle passenger")) {
                passengersNotVisible = true;
            }
            else if (entity.isSelfEntity() || passenger.isSelfEntity()) {
                visiblePassengers.add(passengerID);
            }
            else if (cancelIfEnabledAndHidden(passenger, playerData)) {
                passengersNotVisible = true;
            }
            else {
                visiblePassengers.add(passengerID);
            }
        }
        if (passengersNotVisible) {
            //some passengers are hidden but others aren't. Cancel this packet and send another silently with just the visible passengers.
            sendEntityPassengerPacket(entityID, visiblePassengers, playerData);
        }
        return passengersNotVisible;
    }

    private int[] getPreviousPassengerState(int vehicleID, NettyEntityLocatable<?,?> vehicle, PlayerData playerData) {
        if (vehicle != null) {
            int[] previousPassengerIDs = vehicle.passengerIDs();
            if (!PrimitiveIntArrayList.isEmpty(previousPassengerIDs)) {
                return previousPassengerIDs;
            }
        }
        return playerData.nettyData().getUnresolvedPassengers(vehicleID);
    }

    /**
     * Clears reverse vehicle links for passengers that were part of the previous vehicle state,
     * but are absent from the new authoritative replacement list.
     */
    private void clearStalePassengerReferences(int vehicleID, int[] previousPassengers, int[] newPassengers, PlayerData playerData) {
        if (PrimitiveIntArrayList.isEmpty(previousPassengers)) {
            return;
        }
        for (int previousPassengerID : previousPassengers) {
            if (PrimitiveIntArrayList.contains(newPassengers, previousPassengerID)) {
                continue;
            }
            NettyEntityLocatable<?,?> previousPassenger = playerData.entityFromID(previousPassengerID);
            if (previousPassenger != null && previousPassenger.vehicleID() == vehicleID) {
                previousPassenger.setVehicleID(NO_VEHICLE);
            }
        }
    }

    private void checkVehicle(NettyEntityLocatable<?,?> entity, PlayerData playerData) {
        int vehicleID = entity.vehicleID();
        if (vehicleID < 0) {
            return;
        }
        NettyEntityLocatable<?,?> vehicle = playerData.entityFromID(vehicleID);
        if (vehicle == null) {
            return;
        }
        resendPassengerStateIfClientVisible(vehicle, playerData);
    }

    protected void handleDestroyEntities(int[] entityIDs, PlayerData playerData, int currentTick) {
        for (int entityID : entityIDs) {
            if (playerData.nettyData().consumeExpectedWorldTransitionDestroyEntityID(entityID)) {
                playerData.nettyData().clearPendingPostSpawnTasksForEntity(entityID);
                playerData.entityView().removeEntity(entityID);
                playerData.playerView().removeEntity(entityID); // If not present, this fails silently, so no need to check for correct view first.
                continue;
            }
            clearPassengerReferencesForDestroyedEntity(entityID, playerData);
            clearPendingHolderReference(entityID, playerData);
            playerData.nettyData().removeUnresolvedLeashedEntityFromAll(entityID);
            playerData.nettyData().clearPendingPostSpawnTasksForEntity(entityID);
            EntityView<?> view = playerData.viewFromEntityID(entityID);
            if (view == null) continue;
            Logger.debug("Removing entity from view due to destroy packet, entityID=" + entityID + " player=" + playerData.getPlayerUUID() + " tick=" + currentTick);
            view.removeEntity(entityID, currentTick);
        }
    }

    private int[] drainRemainingEntityIDs(PlayerData playerData) {
        int[] entityIDs = playerData.entityView().getKnownEntityIDs();
        int[] playerEntityIDs = playerData.playerView().getKnownEntityIDs();
        int[] remainingEntityIDs = new int[entityIDs.length + playerEntityIDs.length];
        int offset = 0;
        System.arraycopy(entityIDs, 0, remainingEntityIDs, offset, entityIDs.length);
        offset += entityIDs.length;
        System.arraycopy(playerEntityIDs, 0, remainingEntityIDs, offset, playerEntityIDs.length);
        return remainingEntityIDs;
    }

    private void clearPassengerReferencesForDestroyedEntity(int entityID, PlayerData playerData) {
        int[] unresolvedPassengers = playerData.nettyData().consumeUnresolvedPassengers(entityID);
        if (!PrimitiveIntArrayList.isEmpty(unresolvedPassengers)) {
            for (int passengerID : unresolvedPassengers) {
                NettyEntityLocatable<?,?> passenger = playerData.entityFromID(passengerID);
                if (passenger != null && passenger.vehicleID() == entityID) {
                    passenger.setVehicleID(NO_VEHICLE);
                }
            }
        }

        int unresolvedVehicleID = playerData.nettyData().consumeUnresolvedVehicleForPassenger(entityID);
        if (unresolvedVehicleID != NO_VEHICLE) {
            NettyEntityLocatable<?,?> unresolvedVehicle = playerData.entityFromID(unresolvedVehicleID);
            if (unresolvedVehicle != null) {
                // this can occur if the vehicle existed at the time of the passenger packet but not the passenger, and somehow the passenger never got resolved to the vehicle (missing spawn packets etc). In reality this should never happen.
                unresolvedVehicle.setPassengerIDs(PrimitiveIntArrayList.remove(unresolvedVehicle.passengerIDs(), entityID));
            }
        }

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            return;
        }

        int[] currentPassengerIDs = entity.passengerIDs();
        if (!PrimitiveIntArrayList.isEmpty(currentPassengerIDs)) {
            for (int passengerID : currentPassengerIDs) {
                NettyEntityLocatable<?,?> passenger = playerData.entityFromID(passengerID);
                if (passenger != null && passenger.vehicleID() == entityID) {
                    passenger.setVehicleID(NO_VEHICLE);
                }
            }
        }

        int vehicleID = entity.vehicleID();
        if (vehicleID == NO_VEHICLE) {
            return;
        }
        NettyEntityLocatable<?,?> vehicle = playerData.entityFromID(vehicleID);
        if (vehicle != null) {
            vehicle.setPassengerIDs(PrimitiveIntArrayList.remove(vehicle.passengerIDs(), entityID));
        }
        entity.setVehicleID(NO_VEHICLE);
    }

    private void clearPendingHolderReference(int holderEntityID, PlayerData playerData) {
        int[] pendingLeashedEntityIDs = playerData.nettyData().consumeUnresolvedLeashes(holderEntityID);
        if (PrimitiveIntArrayList.isEmpty(pendingLeashedEntityIDs)) {
            return;
        }
        for (int leashedEntityID : pendingLeashedEntityIDs) {
            NettyEntityLocatable<?,?> leashedEntity = playerData.entityFromID(leashedEntityID);
            if (leashedEntity != null && leashedEntity.leashingEntity() == holderEntityID) {
                leashedEntity.setLeashingEntity(NO_LEASHER);
            }
        }
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleAttributeUpdate(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData, currentTick);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    @Packet(Packet.Packets.LEASH_ENTITY)
    protected boolean handleLeashEntity(int leashedEntity, int leashingEntity, PlayerData playerData, int currentTick) {
        if (playerData.nettyData().isSelfEntityID(leashedEntity) && playerData.nettyData().isSelfEntityID(leashingEntity)) {
            return false;
        }
        NettyEntityLocatable<?,?> leashed = playerData.entityFromID(leashedEntity);
        if (leashed == null) {
            playerData.nettyData().addPostEntitySpawnTask(leashedEntity, new LeashReconciliationTask(playerData, leashedEntity, leashingEntity, currentTick));
            return false;
        }
        return handleLeashEntityNow(leashed, leashingEntity, playerData, currentTick);
    }

    boolean handleLeashEntityNow(NettyEntityLocatable<?,?> leashedEntity, int leashingEntity, PlayerData playerData, int currentTick) {
        //Note, leashing entity ID will be -1 to unleash. From testing it sometimes seems to be 0?
        removeExistingLeashReference(leashedEntity.entityID(), leashedEntity, playerData);
        if (leashingEntity == -1 || leashingEntity == 0) {
            if (leashedEntity.leashingEntity() == NO_LEASHER) {
                Logger.warning("Entity was already unleashing when handling leash entity packet, leashedEntityID=" + leashedEntity + " for player: " + playerData.getPlayerUUID(), 4, PacketEntityViewController.class);
                return false;
            }
            leashedEntity.setLeashingEntity(NO_LEASHER);
            return cancelIfEnabledAndHidden(leashedEntity, playerData);
        }
        else {
            leashedEntity.setLeashingEntity(leashingEntity);
            NettyEntityLocatable<?,?> leashing = playerData.entityFromID(leashingEntity);
            if (leashing == null) {
                playerData.nettyData().addUnresolvedLeash(leashingEntity, leashedEntity.entityID());
                return cancelIfEnabledAndHidden(leashedEntity, playerData);
            }
            else {
                leashing.addLeashedEntity(leashedEntity.entityID());
                if (leashing.isSelfEntity()) {
                    return forceVisibleBecauseAttachedToSelf(leashedEntity, playerData, currentTick, "self-held leash"); // This schedules a visibility update, which repairs leash state, so suppressing the packet avoids sending it twice (and before the client is aware of the entity)
                }
                return cancelIfEnabledAndHidden(leashedEntity, playerData) || cancelIfEnabledAndHidden(leashingEntity, playerData);
            }
        }
    }

    private boolean forceVisibleBecauseAttachedToSelf(NettyEntityLocatable<?,?> entity, PlayerData self, int currentTick, String reason) {
        if (entity.isSelfEntity()) {
            return false;
        }
        boolean wasClientVisible = entity.clientVisible();
        EntityView<?> view = self.viewFromEntityID(entity.entityID());
        if (view == null) {
            Logger.warning("Could not find owning view while forcing self-attached entity visible, id=" + entity.entityID() + " player=" + self.getPlayerUUID() + " reason=" + reason, 4, PacketEntityViewController.class);
            return false;
        }
        view.setVisibility(entity.entityUUID(), true, currentTick);
        return !wasClientVisible;
    }

    private void removeExistingLeashReference(int leashedEntityID, NettyEntityLocatable<?,?> leashed, PlayerData playerData) {
        int previousLeashingEntityID = leashed.leashingEntity();
        if (previousLeashingEntityID == NO_LEASHER) {
            return;
        }
        if (playerData.nettyData().removeUnresolvedLeash(previousLeashingEntityID, leashedEntityID)) {
            return;
        }
        NettyEntityLocatable<?,?> previouslyLeashing = playerData.entityFromID(previousLeashingEntityID);
        if (previouslyLeashing == null) {
            Logger.warning("Found null previously leashing entity when handling leash entity packet, previouslyLeashingEntityID=" + previousLeashingEntityID + " for player: " + playerData.getPlayerUUID(), 5, PacketEntityViewController.class);
            return;
        }
        previouslyLeashing.removeLeashedEntity(leashedEntityID);
    }

    protected RaycastConfig getCorrectConfig(EntityView<?> entityView) {
        if (entityView.isPlayerView()) {
            return playerConfig;
        } else {
            return entityConfig;
        }
    }

    /**
     * @return True if the packet should be suppressed
     */
    protected boolean cancelIfEnabledAndHidden(int entityID, PlayerData playerData) {
        if (playerData.nettyData().isSelfEntityID(entityID)) {
            return false;
        }
        EntityView<?> entityView = playerData.viewFromEntityID(entityID);

        if (entityView == null) {
            Logger.warning("Checked if packet for entity should be cancelled, but entity did not exist. ID: " + entityID + " for player: " + playerData.getPlayerUUID(), 6, PacketEntityViewController.class);
            return false;
        }

        if (entityView.isVisible(entityID)) {
            return false;
        }

        return getCorrectConfig(entityView).enabled(); // If this statement is reached, the entity should be hidden, so if the config is enabled it is hidden.
    }

    /**
     * @return True if the packet should be suppressed
     */
    protected boolean cancelIfEnabledAndHidden(NettyEntityLocatable<?,?> entity, PlayerData playerData) {
        if (entity.isSelfEntity()) {
            return false;
        }
        if (entity.visible()) {
            return false;
        }

        return getCorrectConfig(playerData.viewFromEntityID(entity.entityID())).enabled(); // If this statement is reached, the entity should be hidden, so if the config is enabled it is hidden.
    }

    /**
     * Replays any passenger relationship that was blocked earlier because either:
     * 1. this entity is the vehicle and one or more passengers were missing, or
     * 2. this entity is the passenger and the vehicle was already known.
     */
    protected void reconcileUnresolvedPassengers(NettyEntityLocatable<?,?> insertedEntity, PlayerData playerData) {
        int[] pendingPassengers = playerData.nettyData().getUnresolvedPassengers(insertedEntity.entityID());
        if (!PrimitiveIntArrayList.isEmpty(pendingPassengers)) {
            playerData.nettyData().consumeUnresolvedPassengers(insertedEntity.entityID());
            handleEntityPassengersNow(insertedEntity, pendingPassengers, playerData, insertedEntity.lastChecked());
            resendPassengerStateIfClientVisible(insertedEntity, playerData);
        }

        int unresolvedVehicleID = playerData.nettyData().getUnresolvedVehicleForPassenger(insertedEntity.entityID());
        if (unresolvedVehicleID == NO_VEHICLE) {
            return;
        }
        NettyEntityLocatable<?,?> vehicle = playerData.entityFromID(unresolvedVehicleID);
        if (vehicle == null) {
            return;
        }
        insertedEntity.setVehicleID(unresolvedVehicleID);
        playerData.nettyData().removeUnresolvedPassengerLink(insertedEntity.entityID(), unresolvedVehicleID);
        if (vehicle.isSelfEntity() && forceVisibleBecauseAttachedToSelf(insertedEntity, playerData, insertedEntity.lastChecked(), "self-vehicle unresolved passenger")) {
            return;
        }
        resendPassengerStateIfClientVisible(vehicle, playerData);
    }

    protected void resendPassengerStateIfClientVisible(NettyEntityLocatable<?,?> vehicle, PlayerData playerData) {
        if (!vehicle.clientVisible()) {
            return;
        }
        sendEntityPassengerPacket(vehicle.entityID(), collectVisiblePassengers(vehicle.passengerIDs(), playerData), playerData);
    }

    private IntArrayList collectVisiblePassengers(int[] passengerIDs, PlayerData playerData) {
        int size = passengerIDs == null ? 0 : passengerIDs.length;
        IntArrayList visiblePassengers = new IntArrayList(size);
        if (passengerIDs == null) {
            return visiblePassengers;
        }
        for (int passengerID : passengerIDs) {
            NettyEntityLocatable<?,?> passenger = playerData.entityFromID(passengerID);
            if (passenger != null && passenger.visible()) {
                visiblePassengers.add(passengerID);
            }
        }
        return visiblePassengers;
    }

    /**
     * @return The created entity, with a default visibility of <code>true</code>. Must not return null. Does not insert the entity into any views, that is the responsibility of the caller.
     */
    protected abstract NettyEntityLocatable<?,?> processEntitySpawn(PlayerData playerData, P packet, UUID world, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processRelativeMovePacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processRelativeMoveAndRotationPacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processTeleportPacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processPositionSyncPacket(P packet, PlayerData playerData, int currentTick);

    protected abstract void cachePacket(P packet, int entityID, PlayerData playerData, int currentTick);
    /**   @return The entity ID of the entity   */
    protected abstract int processRotationPacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processHeadLookPacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processEntityVelocityPacket(P packet, PlayerData playerData, int currentTick);

    /**Silently sends the provided array of entities as passengers for the required vehicle.*/
    protected abstract void sendEntityPassengerPacket(int vehicle, IntArrayList passengers, PlayerData playerData);

    protected abstract void insertEntityToPlayerView(NettyEntityLocatable<?,?> entity, PlayerData playerData);

    protected abstract void insertEntityToEntityView(NettyEntityLocatable<?,?> entity, PlayerData playerData);
}
