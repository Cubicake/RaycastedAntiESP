/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.locatables.api.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.RaycastConfig;
import games.cubi.raycastedantiesp.core.entity.EntityBypassRegistry;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.players.NettyData;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.utils.PrimitiveIntArrayList;
import games.cubi.raycastedantiesp.core.utils.Packet;
import games.cubi.raycastedantiesp.core.view.EntityView;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.UUID;

import static games.cubi.raycastedantiesp.core.tracked.NettyEntity.NO_LEASHER;
import static games.cubi.raycastedantiesp.core.tracked.NettyEntity.NO_VEHICLE;

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

    protected void handleWorldStatePacket(UUID player, String world, UUID worldUUID, int minWorldHeight, int currentTick) {
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

        int[] remainingEntityIDs = previousWorld == null ? null : drainRemainingEntityIDs(playerData);
        playerData.beginWorldTransition();
        try {
            if (previousWorld != null) {
                nettyData.setExpectedWorldTransitionDestroyEntityIDs(remainingEntityIDs);
                playerData.blockView().clear();
                playerData.entityView().clear();
                playerData.playerView().clear();
                nettyData.clearPendingReconciliationState();
                nettyData.getSelfEntity().clear();
            }
            nettyData.setCurrentWorldName(world).setCurrentWorldMinHeight(minWorldHeight);
        } catch (Exception e) {
            Logger.error(e, 1, PacketEntityViewController.class);
        } finally {
            playerData.completeWorldTransition(worldUUID); //Don't leave the player stuck on an odd world epoch
        }
    }

    protected PlayerData handlePlayPhaseLoginPacket(int entityID, UUID playerUUID, int currentTick) {
        return PlayerRegistry.getInstance().registerAndGetPlayer(playerUUID, currentTick, entityID, this::createSelfEntity);
    }

    protected abstract NettyEntity<?> createSelfEntity(PlayerData ownData, int entityID, UUID playerUUID);

    /** Applies a SHOW transition immediately on the Netty thread without publishing it to the engine SPSC queue. */
    protected abstract void processDirectEntityShow(PlayerData playerData, EntityView<?> view, NettyEntity<?> entity, int worldEpoch);

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

        NettyEntity<?> entity = Logger.requireNonNull(processEntitySpawn(playerData, packet, world, currentTick), "processEntitySpawn returned null", 3, PacketEntityViewController.class);

        if ((!isPlayer && entityConfig.enabled()) || isPlayer && playerConfig.enabled()) {
            Locatable ownLocation = playerData.ownLocation();
            boolean staleOwnLocation = ownLocation == null || ownLocation.world() == null || !ownLocation.world().equals(world);
            double distanceSquared = staleOwnLocation ? Double.POSITIVE_INFINITY : ownLocation.distanceSquared(entity);
            if (distanceSquared > (isPlayer ? hideOnSpawnPlayerDistanceSquared : hideOnSpawnEntityDistanceSquared)) {
                entity.setVisible(false);
                entity.setClientVisible(false);
                if (isPlayer) {
                    insertEntityToPlayerView(entity, playerData, world);
                    return true;
                }
                insertEntityToEntityView(entity, playerData, world);
                return true;
            }
        } else {
            entity.setClientVisible(true);
        }
        if (isPlayer) {
            insertEntityToPlayerView(entity, playerData, world);
            return false;
        }
        insertEntityToEntityView(entity, playerData, world);
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
        boolean bypassedVehicle = EntityBypassRegistry.isBypassed(entityID);
        NettyEntity<?> vehicle = bypassedVehicle ? null : playerData.entityFromID(entityID);
        int[] previousPassengers = getPreviousPassengerState(entityID, vehicle, playerData);
        playerData.nettyData().consumeUnresolvedPassengers(entityID); // throw away any unresolved passengers as this new packet will have the correct passenger list
        // "Stale" passengers are passengers that the previous authoritative state said were mounted,
        // but the new full replacement passenger list no longer contains.
        clearStalePassengerReferences(entityID, previousPassengers, passengers, playerData);
        if (vehicle == null) {
            playerData.nettyData().setUnresolvedPassengers(entityID, passengers);
            if (bypassedVehicle) {
                for (int passengerID : passengers) {
                    NettyEntity<?> passenger = playerData.entityFromID(passengerID);
                    if (passenger == null) {
                        continue;
                    }
                    passenger.setVehicleID(entityID);
                    forceVisibleBecauseAttached(passenger, playerData, currentTick, "bypassed vehicle passenger");
                }
            }
            return false;
        }
        return handleEntityPassengersNow(vehicle, passengers, playerData, currentTick);
    }

    //This (and leash handling) leaks some info to the client, as it will receive the passenger packet even if the passengers are auto-hidden once parsed, but as the packet doesn't include any location or type info, this shouldn't be too incriminating.
    boolean handleEntityPassengersNow(NettyEntity<?> entity, int[] passengers, PlayerData playerData, int currentTick) {
        int entityID = entity.entityID();
        entity.setPassengerIDs(passengers);
        int[] unresolvedPassengers = null;
        boolean selfIsPassenger = false;
        for (int passengerID : passengers) {
            NettyEntity<?> passenger = playerData.entityFromID(passengerID);
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
        boolean cancelForForcedVehicleShow = selfIsPassenger && forceVisibleBecauseAttached(entity, playerData, currentTick, "self-passenger vehicle");
        if (cancelForForcedVehicleShow) {
            return true;
        }
        if (cancelIfEnabledAndHidden(entityID, playerData)) return true;
        boolean passengersNotVisible = false;
        IntArrayList visiblePassengers = new IntArrayList(passengers.length);
        for (int passengerID : passengers) {
            if (EntityBypassRegistry.isBypassed(passengerID)) {
                visiblePassengers.add(passengerID);
            }
            else {
                NettyEntity<?> passenger = playerData.entityFromID(passengerID);
                if (passenger == null) {
                    visiblePassengers.add(passengerID);
                }
                else if (entity.isSelfEntity() && forceVisibleBecauseAttached(passenger, playerData, currentTick, "self-vehicle passenger")) {
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
        }
        if (passengersNotVisible) {
            //some passengers are hidden but others aren't. Cancel this packet and send another silently with just the visible passengers.
            sendEntityPassengerPacket(entityID, visiblePassengers, playerData);
        }
        return passengersNotVisible;
    }

    private int[] getPreviousPassengerState(int vehicleID, NettyEntity<?> vehicle, PlayerData playerData) {
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
            NettyEntity<?> previousPassenger = playerData.entityFromID(previousPassengerID);
            if (previousPassenger != null && previousPassenger.vehicleID() == vehicleID) {
                previousPassenger.setVehicleID(NO_VEHICLE);
            }
        }
    }

    private void checkVehicle(NettyEntity<?> entity, PlayerData playerData) {
        int vehicleID = entity.vehicleID();
        if (vehicleID == NO_VEHICLE) {
            return;
        }
        NettyEntity<?> vehicle = playerData.entityFromID(vehicleID);
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
            clearLeashReferencesForDestroyedEntity(entityID, playerData);
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
                NettyEntity<?> passenger = playerData.entityFromID(passengerID);
                if (passenger != null && passenger.vehicleID() == entityID) {
                    passenger.setVehicleID(NO_VEHICLE);
                }
            }
        }

        int unresolvedVehicleID = playerData.nettyData().consumeUnresolvedVehicleForPassenger(entityID);
        if (unresolvedVehicleID != NO_VEHICLE) {
            NettyEntity<?> unresolvedVehicle = playerData.entityFromID(unresolvedVehicleID);
            if (unresolvedVehicle != null) {
                // this can occur if the vehicle existed at the time of the passenger packet but not the passenger, and somehow the passenger never got resolved to the vehicle (missing spawn packets etc). In reality this should never happen.
                unresolvedVehicle.setPassengerIDs(PrimitiveIntArrayList.remove(unresolvedVehicle.passengerIDs(), entityID));
            }
        }

        NettyEntity<?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            return;
        }

        int[] currentPassengerIDs = entity.passengerIDs();
        if (!PrimitiveIntArrayList.isEmpty(currentPassengerIDs)) {
            for (int passengerID : currentPassengerIDs) {
                NettyEntity<?> passenger = playerData.entityFromID(passengerID);
                if (passenger != null && passenger.vehicleID() == entityID) {
                    passenger.setVehicleID(NO_VEHICLE);
                }
            }
        }

        int vehicleID = entity.vehicleID();
        if (vehicleID == NO_VEHICLE) {
            return;
        }
        NettyEntity<?> vehicle = playerData.entityFromID(vehicleID);
        if (vehicle != null) {
            vehicle.setPassengerIDs(PrimitiveIntArrayList.remove(vehicle.passengerIDs(), entityID));
        }
        entity.setVehicleID(NO_VEHICLE);
    }

    /**
     * Clears both sides of resolved and unresolved leash relationships so a reused entity ID
     * cannot inherit relationships belonging to the destroyed entity.
     */
    private void clearLeashReferencesForDestroyedEntity(int entityID, PlayerData playerData) {
        // Deferred relationships are indexed by both endpoints, so clear the cases where this ID
        // was the holder and where it was the leashed entity.
        int[] pendingLeashedEntityIDs = playerData.nettyData().consumeUnresolvedLeashes(entityID);
        if (!PrimitiveIntArrayList.isEmpty(pendingLeashedEntityIDs)) {
            for (int leashedEntityID : pendingLeashedEntityIDs) {
                NettyEntity<?> leashedEntity = playerData.entityFromID(leashedEntityID);
                if (leashedEntity != null && leashedEntity.leashingEntity() == entityID) {
                    leashedEntity.setLeashingEntity(NO_LEASHER);
                }
            }
        }

        int pendingHolderEntityID = playerData.nettyData().consumeUnresolvedHolderForLeashedEntity(entityID);
        if (pendingHolderEntityID != NO_LEASHER) {
            NettyEntity<?> pendingHolder = playerData.entityFromID(pendingHolderEntityID);
            if (pendingHolder != null) {
                pendingHolder.removeLeashedEntity(entityID);
            }
        }

        NettyEntity<?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            return;
        }

        // A tracked entity owns the same two relationship directions in its resolved state.
        int[] currentLeashedEntityIDs = entity.leashedEntityIDsOrNull();
        if (!PrimitiveIntArrayList.isEmpty(currentLeashedEntityIDs)) {
            for (int leashedEntityID : currentLeashedEntityIDs) {
                NettyEntity<?> leashedEntity = playerData.entityFromID(leashedEntityID);
                if (leashedEntity != null && leashedEntity.leashingEntity() == entityID) {
                    leashedEntity.setLeashingEntity(NO_LEASHER);
                }
            }
        }
        int holderEntityID = entity.leashingEntity();
        if (holderEntityID != NO_LEASHER) {
            NettyEntity<?> holder = playerData.entityFromID(holderEntityID);
            if (holder != null) {
                holder.removeLeashedEntity(entityID);
            }
            entity.setLeashingEntity(NO_LEASHER);
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
    protected boolean handleLeashEntity(int leashedEntityID, int holderEntityID, PlayerData playerData, int currentTick) {
        if (playerData.nettyData().isSelfEntityID(leashedEntityID) && playerData.nettyData().isSelfEntityID(holderEntityID)) {
            return false;
        }
        NettyEntity<?> leashedEntity = playerData.entityFromID(leashedEntityID);
        removeExistingLeashReference(leashedEntityID, leashedEntity, playerData);

        if (holderEntityID == NO_LEASHER) {
            return leashedEntity != null && cancelIfEnabledAndHidden(leashedEntity, playerData);
        }

        NettyEntity<?> holderEntity = playerData.entityFromID(holderEntityID);
        if (leashedEntity != null) {
            leashedEntity.setLeashingEntity(holderEntityID);
        }
        if (holderEntity != null) {
            holderEntity.addLeashedEntity(leashedEntityID);
        }
        if (leashedEntity == null || holderEntity == null) {
            playerData.nettyData().addUnresolvedLeash(holderEntityID, leashedEntityID);
        }

        if (holderEntity != null && holderEntity.isSelfEntity() && leashedEntity != null
                && forceVisibleBecauseAttached(leashedEntity, playerData, currentTick, "self-held leash")) {
            return true; // The visibility update repairs leash state, so suppress the packet to avoid sending it twice and before the client is aware of the entity.
        }
        return leashedEntity != null && cancelIfEnabledAndHidden(leashedEntity, playerData)
                || holderEntity != null && cancelIfEnabledAndHidden(holderEntity, playerData);
    }

    private boolean forceVisibleBecauseAttached(NettyEntity<?> entity, PlayerData self, int currentTick, String reason) {
        if (entity.isSelfEntity()) {
            return false;
        }
        boolean wasClientVisible = entity.clientVisible();
        EntityView<?> view = self.viewFromEntityID(entity.entityID());
        if (view == null) {
            Logger.warning("Could not find owning view while forcing attached entity visible, id=" + entity.entityID() + " player=" + self.getPlayerUUID() + " reason=" + reason, 6, PacketEntityViewController.class);
            // Note that this path can fire when a vehicle is bypassed, so its log level must be higher than default.
            return false;
        }
        view.setVisibility(entity, true, currentTick, self.acquireWorldEpoch());
        return !wasClientVisible;
    }

    private void removeExistingLeashReference(int leashedEntityID, NettyEntity<?> leashedEntity, PlayerData playerData) {
        int previousLeashingEntityID = leashedEntity == null
                ? playerData.nettyData().getUnresolvedHolderForLeashedEntity(leashedEntityID)
                : leashedEntity.leashingEntity();
        if (previousLeashingEntityID == NO_LEASHER) {
            return;
        }
        playerData.nettyData().removeUnresolvedLeash(previousLeashingEntityID, leashedEntityID);
        NettyEntity<?> previousHolder = playerData.entityFromID(previousLeashingEntityID);
        if (previousHolder != null) {
            previousHolder.removeLeashedEntity(leashedEntityID);
        }
        if (leashedEntity != null) {
            leashedEntity.setLeashingEntity(NO_LEASHER);
        }
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
    protected boolean cancelIfEnabledAndHidden(NettyEntity<?> entity, PlayerData playerData) {
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
    protected void reconcileUnresolvedPassengers(NettyEntity<?> insertedEntity, PlayerData playerData) {
        int[] pendingPassengers = playerData.nettyData().getUnresolvedPassengers(insertedEntity.entityID());
        if (!PrimitiveIntArrayList.isEmpty(pendingPassengers)) {
            playerData.nettyData().consumeUnresolvedPassengers(insertedEntity.entityID());
            handleEntityPassengersNow(insertedEntity, pendingPassengers, playerData, insertedEntity.lastChecked());
        }

        int unresolvedVehicleID = playerData.nettyData().getUnresolvedVehicleForPassenger(insertedEntity.entityID());
        if (unresolvedVehicleID == NO_VEHICLE) {
            return;
        }
        if (EntityBypassRegistry.isBypassed(unresolvedVehicleID)) {
            insertedEntity.setVehicleID(unresolvedVehicleID);
            forceVisibleBecauseAttached(insertedEntity, playerData, insertedEntity.lastChecked(), "bypassed vehicle unresolved passenger");
            return;
        }
        NettyEntity<?> vehicle = playerData.entityFromID(unresolvedVehicleID);
        if (vehicle == null) {
            return;
        }
        insertedEntity.setVehicleID(unresolvedVehicleID);
        playerData.nettyData().removeUnresolvedPassengerLink(insertedEntity.entityID(), unresolvedVehicleID);
        if (vehicle.isSelfEntity() && forceVisibleBecauseAttached(insertedEntity, playerData, insertedEntity.lastChecked(), "self-vehicle unresolved passenger")) {
            return;
        }
    }

    /**
     * Reconciles relationship state which arrived before this entity was identified as bypassed.
     * The pending records are retained because a bypassed entity will never have a tracked entity
     * to own the authoritative relationship state.
     */
    protected void handleBypassedEntitySpawn(int entityID, PlayerData playerData, int currentTick) {
        // Vanilla may send entity state before its spawn packet, and those packets were already
        // forwarded to the client. Discard deferred tracking work rather than replaying duplicates.
        playerData.nettyData().clearPendingPostSpawnTasksForEntity(entityID);
        int[] pendingPassengers = playerData.nettyData().getUnresolvedPassengers(entityID);
        if (!PrimitiveIntArrayList.isEmpty(pendingPassengers)) {
            for (int passengerID : pendingPassengers) {
                NettyEntity<?> passenger = playerData.entityFromID(passengerID);
                if (passenger == null) {
                    continue;
                }
                passenger.setVehicleID(entityID);
                forceVisibleBecauseAttached(passenger, playerData, currentTick, "bypassed vehicle passenger");
            }
        }

        int holderEntityID = playerData.nettyData().getUnresolvedHolderForLeashedEntity(entityID);
        if (holderEntityID != NO_LEASHER) {
            NettyEntity<?> holder = playerData.entityFromID(holderEntityID);
            if (holder != null) {
                holder.addLeashedEntity(entityID);
            }
        }
        int[] pendingLeashedEntityIDs = playerData.nettyData().getUnresolvedLeashes(entityID);
        if (!PrimitiveIntArrayList.isEmpty(pendingLeashedEntityIDs)) {
            for (int leashedEntityID : pendingLeashedEntityIDs) {
                NettyEntity<?> leashedEntity = playerData.entityFromID(leashedEntityID);
                if (leashedEntity != null) {
                    leashedEntity.setLeashingEntity(entityID);
                }
            }
        }
    }

    /**
     * Moves deferred leash state into a newly tracked entity while retaining relationships whose
     * other endpoint is still untracked or permanently bypassed.
     */
    protected void reconcileUnresolvedLeashes(NettyEntity<?> insertedEntity, PlayerData playerData) {
        int entityID = insertedEntity.entityID();
        int pendingHolderEntityID = playerData.nettyData().getUnresolvedHolderForLeashedEntity(entityID);
        if (pendingHolderEntityID != NO_LEASHER) {
            insertedEntity.setLeashingEntity(pendingHolderEntityID);
            NettyEntity<?> holder = playerData.entityFromID(pendingHolderEntityID);
            if (holder != null) {
                holder.addLeashedEntity(entityID);
                playerData.nettyData().removeUnresolvedLeash(pendingHolderEntityID, entityID);
                if (holder.isSelfEntity()) {
                    forceVisibleBecauseAttached(insertedEntity, playerData, insertedEntity.lastChecked(), "self-held unresolved leash");
                }
            }
        }

        int[] pendingLeashedEntityIDs = playerData.nettyData().getUnresolvedLeashes(entityID);
        if (PrimitiveIntArrayList.isEmpty(pendingLeashedEntityIDs)) {
            return;
        }
        for (int leashedEntityID : pendingLeashedEntityIDs) {
            NettyEntity<?> leashedEntity = playerData.entityFromID(leashedEntityID);
            if (leashedEntity == null) {
                if (EntityBypassRegistry.isBypassed(leashedEntityID)) {
                    insertedEntity.addLeashedEntity(leashedEntityID);
                }
                continue;
            }
            leashedEntity.setLeashingEntity(entityID);
            insertedEntity.addLeashedEntity(leashedEntityID);
            playerData.nettyData().removeUnresolvedLeash(entityID, leashedEntityID);
            if (insertedEntity.isSelfEntity()) {
                forceVisibleBecauseAttached(leashedEntity, playerData, leashedEntity.lastChecked(), "self-held unresolved leash");
            }
        }
    }

    private boolean keepingClientEntityWhenHidden(NettyEntity<?> entity, PlayerData data) {
        if (entityConfig.keepClientEntityWhenHidden() == playerConfig.keepClientEntityWhenHidden()) {
            return entityConfig.keepClientEntityWhenHidden();
        }
        EntityView<?> view = data.viewFromEntityID(entity.entityID());
        return getCorrectConfig(view).keepClientEntityWhenHidden();
    }

    protected void resendPassengerStateIfClientVisible(NettyEntity<?> vehicle, PlayerData playerData) {
        if (!vehicle.clientVisible() || (keepingClientEntityWhenHidden(vehicle, playerData) && !vehicle.visible())) {
            return;
        }
        sendEntityPassengerPacket(vehicle.entityID(), collectClientVisiblePassengers(vehicle.passengerIDs(), playerData), playerData);
    }

    protected IntArrayList collectClientVisiblePassengers(int[] passengerIDs, PlayerData playerData) {
        // HackyEntityIDGuard reserves -1, so it cannot accidentally match an entity being shown.
        return collectClientVisiblePassengers(passengerIDs, playerData, -1);
    }

    protected IntArrayList collectClientVisiblePassengers(int[] passengerIDs, PlayerData playerData, int entityBeingShownID) {
        int size = passengerIDs == null ? 0 : passengerIDs.length;
        IntArrayList visiblePassengers = new IntArrayList(size);
        if (passengerIDs == null) {
            return visiblePassengers;
        }
        for (int passengerID : passengerIDs) {
            if (EntityBypassRegistry.isBypassed(passengerID)) {
                visiblePassengers.add(passengerID);
                continue;
            }
            NettyEntity<?> passenger = playerData.entityFromID(passengerID);
            if (passenger != null && ((passenger.clientVisible() && (!keepingClientEntityWhenHidden(passenger, playerData) || passenger.visible())) || passenger.entityID() == entityBeingShownID)) {
                visiblePassengers.add(passengerID);
            }
        }
        return visiblePassengers;
    }

    /**
     * @return The created entity, with a default visibility of <code>true</code>. Must not return null. Does not insert the entity into any views, that is the responsibility of the caller.
     */
    protected abstract NettyEntity<?> processEntitySpawn(PlayerData playerData, P packet, UUID world, int currentTick);

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

    protected abstract void insertEntityToPlayerView(NettyEntity<?> entity, PlayerData playerData, UUID world);

    protected abstract void insertEntityToEntityView(NettyEntity<?> entity, PlayerData playerData, UUID world);
}
