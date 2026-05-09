package games.cubi.raycastedantiesp.core.view.controller;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.RaycastConfig;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.utils.Packet;
import games.cubi.raycastedantiesp.core.view.EntityView;

import java.util.ArrayList;
import java.util.UUID;

import static games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable.NO_LEASHER;

/**
 * @param <P> The platform's packet wrapper (PacketWrapper<?>)
 */
public abstract class PacketEntityViewController<P> {
    protected EntityConfig entityConfig = null;
    protected PlayerConfig playerConfig = null;
    protected double hideOnSpawnEntityDistanceSquared = 0;
    protected double hideOnSpawnPlayerDistanceSquared = 0;

    protected void handlePlayPhaseLoginPacket(int entityID, UUID playerUUID, int currentTick) {
        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(playerUUID);
        playerData.playerView().insertEntity(createSelfEntity(playerData, entityID, playerUUID).cast());
    }

    protected PlayerData handleLoginPhaseLoginPacket(UUID playerUUID, int currentTick) {
        return PlayerRegistry.getInstance().registerAndGetPlayer(playerUUID, currentTick);
    }

    protected abstract NettyEntityLocatable<?,?> createSelfEntity(PlayerData ownData, int entityID, UUID playerUUID);

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     *//*
    protected boolean handleLivingEntitySpawn(P packet, PlayerData playerData, UUID world, int currentTick) {
        if (world == null) {
            Logger.error(new RuntimeException("World null when handling spawn living entity packet, uuid=" + playerData.getPlayerUUID() + " tick=" + currentTick), 2, PacketEntityViewController.class);
            return true;
        }
        NettyEntityLocatable<?,?> entity = processLivingEntitySpawn(playerData, packet, world, currentTick);

        if (entityConfig.enabled()) {
            double distanceSquared = playerData.ownLocation().distanceSquared(entity);
            if (distanceSquared > hideOnSpawnEntityDistanceSquared) {
                entity.setVisible(false);
                entity.setClientVisible(false);
                insertEntityToEntityView(entity, playerData);
                return true;
            }
        } else {
            entity.setClientVisible(true);
        }
        insertEntityToEntityView(entity, playerData);
        return false;
    }*/
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    @Packet(Packet.Packets.SPAWN_ENTITY)
    protected boolean handleEntitySpawn(P packet, boolean isPlayer, PlayerData playerData, UUID world, int currentTick) {
        if (world == null) {
            Logger.error(new RuntimeException("World null when handling spawn entity packet, uuid=" + playerData.getPlayerUUID() + " tick=" + currentTick), 2, PacketEntityViewController.class);
            return true;
        }

        NettyEntityLocatable<?,?> entity = processEntitySpawn(playerData, packet, world, currentTick);

        if ((!isPlayer && entityConfig.enabled()) || playerConfig.enabled()) {
            double distanceSquared = playerData.ownLocation().distanceSquared(entity);
            if (distanceSquared > hideOnSpawnEntityDistanceSquared) {
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
/*
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     *\/
    protected boolean handlePaintingSpawn(P packet, PlayerData playerData, UUID world, int currentTick) {
        if (world == null) {
            Logger.error(new RuntimeException("World null when handling spawn painting packet, uuid=" + playerData.getPlayerUUID() + " tick=" + currentTick), 2, PacketEntityViewController.class);
            return true;
        }
        NettyEntityLocatable<?,?> entity = processPaintingSpawn(playerData, packet, world, currentTick);

        if (entityConfig.enabled()) {
            double distanceSquared = playerData.ownLocation().distanceSquared(entity);
            if (distanceSquared > hideOnSpawnEntityDistanceSquared) {
                entity.setVisible(false);
                entity.setClientVisible(false);
                insertEntityToEntityView(entity, playerData);
                return true;
            }
        } else {
            entity.setClientVisible(true);
        }
        insertEntityToEntityView(entity, playerData);
        return false;
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     *\/
    protected boolean handlePlayerSpawn(P packet, PlayerData playerData, UUID world, int currentTick) {
        if (world == null) {
            Logger.error(new RuntimeException("World null when handling spawn player packet, uuid=" + playerData.getPlayerUUID() + " tick=" + currentTick), 2, PacketEntityViewController.class);
            return true;
        }
        NettyEntityLocatable<?,?> entity = processPlayerSpawn(playerData, packet, world, currentTick);

        if (playerConfig.enabled()) {
            double distanceSquared = playerData.ownLocation().distanceSquared(entity);
            if (distanceSquared > hideOnSpawnPlayerDistanceSquared) {
                entity.setVisible(false);
                entity.setClientVisible(false);
                insertEntityToPlayerView(entity, playerData);
                return true;
            }
        } else {
            entity.setClientVisible(true);
        }
        insertEntityToPlayerView(entity, playerData);
        return false;
    }*/
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
        cachePacket(packet, entityID, playerData);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleRemoveEntityEffect(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityEquipment(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityVelocity(P packet, int entityID, PlayerData playerData, int currentTick) {
        processEntityVelocityPacket(packet, playerData, currentTick);
        cachePacket(packet, entityID, playerData); //todo: may be wrong?
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityEffect(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }
    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleEntityPassengers(int entityID, int[] passengers, PlayerData playerData, int currentTick) {
        NettyEntityLocatable<?,?> entity = entityFromID(entityID, playerData);
        entity.setPassengerIDs(passengers);
        checkVehicle(entity, playerData);
        if (cancelIfEnabledAndHidden(entityID, playerData)) return true;
        boolean passengersNotVisible = false;
        ArrayList<Integer> visiblePassengers = new ArrayList<>(passengers.length);
        for (int passengerID : passengers) {
            if (cancelIfEnabledAndHidden(passengerID, playerData)) {
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

    private void checkVehicle(NettyEntityLocatable<?,?> entity, PlayerData playerData) {
        int vehicleID = entity.vehicleID();
        if (vehicleID >= 0) {
            NettyEntityLocatable<?,?> vehicle = entityFromID(vehicleID, playerData);
            if (vehicle == null) {
                Logger.error(new RuntimeException("Found null vehicle when handling entity passengers packet, vehicleID=" + vehicleID + " for player: " + playerData.getPlayerUUID()), 2, PacketEntityViewController.class);
                return;
            }
            if (cancelIfEnabledAndHidden(vehicleID, playerData)) {
                //Vehicle is hidden, so this entity should be hidden as well. No need to check passengers.
                return;
            }
            //Vehicle is visible, but this entity may not be.
            if (cancelIfEnabledAndHidden(entity.entityID(), playerData)) {
                return;
            }
            ArrayList<Integer> passengers = new ArrayList<>();
            passengers.add(entity.entityID());
            sendEntityPassengerPacket(vehicleID, passengers, playerData);
        }
    }

    protected void handleDestroyEntities(P packet, PlayerData playerData, int currentTick) {
        processDestroyEntitiesPacket(packet, playerData, currentTick);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    protected boolean handleAttributeUpdate(P packet, int entityID, PlayerData playerData, int currentTick) {
        cachePacket(packet, entityID, playerData);
        return cancelIfEnabledAndHidden(entityID, playerData);
    }

    /**
     * @return Whether or not to cancel the packet event. <code>true</code> to cancel, <code>false</code> to do nothing.
     */
    @Packet(Packet.Packets.LEASH_ENTITY)
    protected boolean handleLeashEntity(P packet, int leashedEntity, int leashingEntity, PlayerData playerData) {
        //Note, leashing entity ID will be -1 to unleash
        NettyEntityLocatable<?,?> leashed = entityFromID(leashedEntity, playerData);
        if (leashed == null) {
            Logger.error(new RuntimeException("Found null leashed entity when handling leash entity packet, leashedEntityID=" + leashedEntity + " for player: " + playerData.getPlayerUUID() + ". Packet:" + packet), 2, PacketEntityViewController.class);
            return false;
        }
        if (leashingEntity == -1) {
            int previouslyLeashingEntityID = leashed.leashingEntity();
            if (previouslyLeashingEntityID == NO_LEASHER) {
                Logger.error("Entity was already unleashing when handling leash entity packet, leashedEntityID=" + leashedEntity + " for player: " + playerData.getPlayerUUID() + ". Packet:" + packet, 2, PacketEntityViewController.class);
                return false;
            }
            entityFromID(previouslyLeashingEntityID, playerData).removeLeashedEntity(leashedEntity);
            leashed.setLeashingEntity(NO_LEASHER);
            return cancelIfEnabledAndHidden(leashedEntity, playerData);
        }
        else {
            NettyEntityLocatable<?,?> leashing = entityFromID(leashingEntity, playerData);
            if (leashing == null) {
                Logger.error("Found null leashing entity when handling leash entity packet, leashingEntityID=" + leashingEntity + " for player: " + playerData.getPlayerUUID() + ". Packet:" + packet, 2, PacketEntityViewController.class);
                return false;
            }
            leashed.setLeashingEntity(leashingEntity);
            leashing.addLeashedEntity(leashedEntity);
            return cancelIfEnabledAndHidden(leashedEntity, playerData) || cancelIfEnabledAndHidden(leashingEntity, playerData);
        }
    }

    /**
     * @return Either the entity or player view for this player, depending on the entity ID
     */
    protected EntityView<?> viewFromEntityID(int entityID, PlayerData playerData) {
        EntityView<?> entityView = playerData.entityView();
        if (entityView.exists(entityID)) {
            return entityView;
        }
        if (playerData.playerView().exists(entityID)) {
            return playerData.playerView();
        }
        Logger.warning("Could not find view for entityID=" + entityID + " uuid=" + playerData.getPlayerUUID(), 6, PacketEntityViewController.class);
        return null;
    }

    protected NettyEntityLocatable<?,?> entityFromID(int entityID, PlayerData playerData) {
        EntityView<?> entityView = viewFromEntityID(entityID, playerData);
        if (entityView == null) {
            return null;
        }
        return (NettyEntityLocatable<?, ?>) entityView.getEntity(entityID);
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
        EntityView<?> entityView = viewFromEntityID(entityID, playerData);

        if (entityView == null) {
            Logger.warning("Checked if packet for entity should be cancelled, but entity did not exist. ID: " + entityID + " for player: " + playerData.getPlayerUUID(), 6, PacketEntityViewController.class);
            return true;
        }

        if (entityView.isVisible(entityID)) {
            return false;
        }

        return getCorrectConfig(entityView).enabled(); // If this statement is reached, the entity should be hidden, so if the config is enabled it is hidden.
    }

    /**
     * @return The created entity, with a default visibility of <code>true</code>. Does not insert the entity into any views, that is the responsibility of the caller.
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

    protected abstract void cachePacket(P packet, int entityID, PlayerData playerData);

    /**   @return The entity ID of the entity   */
    protected abstract int processRotationPacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processHeadLookPacket(P packet, PlayerData playerData, int currentTick);

    /**   @return The entity ID of the entity   */
    protected abstract int processEntityVelocityPacket(P packet, PlayerData playerData, int currentTick);

    /**Silently sends the provided array of entities as passengers for the required vehicle.*/
    protected abstract void sendEntityPassengerPacket(int vehicle, ArrayList<Integer> passengers, PlayerData playerData);

    protected abstract void processDestroyEntitiesPacket(P packet, PlayerData playerData, int currentTick);

    protected abstract void insertEntityToPlayerView(NettyEntityLocatable<?,?> entity, PlayerData playerData);

    protected abstract void insertEntityToEntityView(NettyEntityLocatable<?,?> entity, PlayerData playerData);
}
