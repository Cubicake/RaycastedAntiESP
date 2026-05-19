package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.locatables.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.EntityViewTransition;
import games.cubi.raycastedantiesp.core.view.controller.PacketEntityViewController;
import games.cubi.raycastedantiesp.packetevents.locatables.PacketEventsEntity;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsEntityReplayData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntSupplier;

import static games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable.NO_LEASHER;

public abstract class PacketEventsEntityViewController extends PacketEntityViewController<PacketWrapper<?>> implements PacketListener {
    private final IntSupplier currentTickSupplier;
    private final PacketEventsCommonViewController common;

    protected PacketEventsEntityViewController(IntSupplier currentTickSupplier) {
        this.currentTickSupplier = currentTickSupplier;
        common = PacketEventsCommonViewController.get(currentTickSupplier);
    }

    protected abstract UUID resolveWorldUUID(User user);

    public void removeViewer(UUID viewerUUID) {
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        UUID viewerUUID = event.getUser().getUUID();
        if (viewerUUID == null) {
            return;
        }

        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(viewerUUID);

        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            handleLoginPhaseLoginPacket(viewerUUID, currentTickSupplier.getAsInt());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            handlePlayPhaseLoginPacket(packet.getEntityId(), viewerUUID, currentTickSupplier.getAsInt());
        }

        if (playerData == null) {
            return;
        }

        if (ConfigManager.get().getEntityConfig() != entityConfig) {
            entityConfig = ConfigManager.get().getEntityConfig();
            hideOnSpawnEntityDistanceSquared = entityConfig.hideOnSpawnDistance() * entityConfig.hideOnSpawnDistance();
        }

        if (ConfigManager.get().getPlayerConfig() != playerConfig) {
            playerConfig = ConfigManager.get().getPlayerConfig();
            hideOnSpawnPlayerDistanceSquared = playerConfig.hideOnSpawnDistance() * playerConfig.hideOnSpawnDistance();
        }

        Locatable ownLocation = playerData.ownLocation();
        UUID world = ownLocation != null ? ownLocation.world() : resolveWorldUUID(event.getUser());
        int currentTick = currentTickSupplier.getAsInt();

        playerData.runAllNettyTasks();
        handleEntityPackets(event, event.getUser(), playerData, world, currentTick);

        if (playerData.entityView().hasPendingTransitions()) {
            processEntityTransitions(viewerUUID, event.getUser(), cast(playerData.entityView()));
        }

        if (playerData.playerView().hasPendingTransitions()) {
            processEntityTransitions(viewerUUID, event.getUser(), cast(playerData.playerView()));
        }
        
        event.getUser().flushPackets();
    }

    private void handleEntityPackets(PacketSendEvent event, User viewer, PlayerData playerData, UUID world, int currentTick) {
        //code readability helper constant
        final boolean REQUIRE_EVENT_CANCELLATION = true;
        switch (event.getPacketType()) {
            case PacketType.Play.Server.SPAWN_LIVING_ENTITY -> {
                Logger.error("Received spawn living entity packet. This packet type should not be used in modern Minecraft versions, and its presence likely indicates a protocol mapping issue. Viewer=" + viewer.getUUID() + " tick=" + currentTick, 2, PacketEventsEntityViewController.class);
                throw new RuntimeException("Spawn Living Entity packet appeared. This shouldn't exist");
                //if (handleLivingEntitySpawn(new WrapperPlayServerSpawnLivingEntity(event), playerData, world, currentTick) == REQUIRE_EVENT_CANCELLATION)
                 //   event.setCancelled(true);
            }
            case PacketType.Play.Server.SPAWN_ENTITY -> {
                WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
                Logger.debug("Spawning entity for player " + viewer.getUUID() + " entity #" + packet.getEntityId() + " tick=" + currentTick + " type=" + packet.getEntityType().getName());
                if (handleEntitySpawn(packet, packet.getEntityType().isInstanceOf(EntityTypes.PLAYER), playerData, world, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_ANIMATION -> {
                if (handleEntityAnimation(new WrapperPlayServerEntityAnimation(event).getEntityId(), playerData) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_STATUS -> {
                if (handleEntityEvent(new WrapperPlayServerEntityStatus(event).getEntityId(), playerData) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.HURT_ANIMATION -> {
                if (handleHurtAnimation(new WrapperPlayServerHurtAnimation(event).getEntityId(), playerData) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.SPAWN_PAINTING -> {
                throw new RuntimeException("Spawn Painting packet appeared. This shouldn't exist");
                //if (handlePaintingSpawn(new WrapperPlayServerSpawnPainting(event), playerData, world, currentTick) == REQUIRE_EVENT_CANCELLATION)
                //    event.setCancelled(true);
            }
            case PacketType.Play.Server.SPAWN_PLAYER -> {
                Logger.error("Received spawn player entity packet. This packet type should not be used in modern Minecraft versions, and its presence likely indicates a protocol mapping issue. Viewer=" + viewer.getUUID() + " tick=" + currentTick, 2, PacketEventsEntityViewController.class);
                throw new RuntimeException("Spawn Player packet appeared. This shouldn't exist");
                //if (handlePlayerSpawn(new WrapperPlayServerSpawnPlayer(event), playerData, world, currentTick) == REQUIRE_EVENT_CANCELLATION)
                //    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_RELATIVE_MOVE -> {
                if (handleRelativeMove(new WrapperPlayServerEntityRelativeMove(event), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION -> {
                if (handleRelativeMoveAndRotation(new WrapperPlayServerEntityRelativeMoveAndRotation(event), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_TELEPORT -> {
                if (handleTeleport(new WrapperPlayServerEntityTeleport(event), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_POSITION_SYNC -> {
                if (handlePositionSync(new WrapperPlayServerEntityPositionSync(event), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_ROTATION -> {
                if (handleEntityRotation(new WrapperPlayServerEntityRotation(event), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_HEAD_LOOK -> {
                if (handleEntityHeadLook(new WrapperPlayServerEntityHeadLook(event), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_METADATA -> {
                WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
                if (handleEntityMetadata(packet, packet.getEntityId(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.REMOVE_ENTITY_EFFECT -> {
                WrapperPlayServerRemoveEntityEffect packet = new WrapperPlayServerRemoveEntityEffect(event);
                if (handleRemoveEntityEffect(packet, packet.getEntityId(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_EQUIPMENT -> {
                WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
                if (handleEntityEquipment(packet, packet.getEntityId(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_VELOCITY -> {
                WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(event);
                if (handleEntityVelocity(packet, packet.getEntityId(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ENTITY_EFFECT -> {
                WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(event);
                if (handleEntityEffect(packet, packet.getEntityId(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.SET_PASSENGERS -> {
                WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
                if (handleEntityPassengers(packet.getEntityId(), packet.getPassengers(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.DESTROY_ENTITIES -> {
                handleDestroyEntities(new WrapperPlayServerDestroyEntities(event).getEntityIds(), playerData, currentTick);
            }
            case PacketType.Play.Server.UPDATE_ATTRIBUTES -> {
                WrapperPlayServerUpdateAttributes wrapper = new WrapperPlayServerUpdateAttributes(event);
                if (handleAttributeUpdate(wrapper, wrapper.getEntityId(), playerData, currentTick) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            case PacketType.Play.Server.ATTACH_ENTITY -> {
                WrapperPlayServerAttachEntity wrapper = new WrapperPlayServerAttachEntity(event);
                if (handleLeashEntity(wrapper.getAttachedId(), wrapper.getHoldingId(), playerData) == REQUIRE_EVENT_CANCELLATION)
                    event.setCancelled(true);
            }
            default -> {}
        }
    }

    protected NettyEntityLocatable<?,?> createSelfEntity(PlayerData ownData, int entityID, UUID playerUUID) {
        return PacketEventsEntity.createSelfEntity(ownData, entityID, playerUUID);
    }

    /*
    private void handleEntityPackets(PacketSendEvent event, User viewer, PlayerData playerData, UUID world, int currentTick) {
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            if (world == null) {
                Logger.error(new RuntimeException("World null when handling spawn living entity packet, uuid=" + event.getUser().getUUID() + " tick=" + currentTick), 2, PacketEventsEntityViewController.class);
                return;
            }
            WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(event);
            PacketEventsEntity entity = trackEntitySpawn(playerData.entityView().cast(), packet.getEntityUUID(), packet.getEntityId(), world,
                    packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), playerData.ownLocation(), EntityLocatable.SpawnType.LIVING, packet.getEntityType());
            if (entity == null) {
                Logger.error(new RuntimeException("Entity null after attempting to track entity spawn for living entity packet, uuid=" + packet.getEntityUUID() + " id=" + packet.getEntityId() + " tick=" + currentTick), 2, PacketEventsEntityViewController.class);;
                return;
            }
            entity
                    .setYaw(packet.getYaw())
                    .setPitch(packet.getPitch())
                    .setHeadYaw(packet.getHeadPitch())
                    .setVelocity(packet.getVelocity().getX(), packet.getVelocity().getY(), packet.getVelocity().getZ())
                    .setMetadata(copyEntityMetadata(packet.getEntityMetadata()))
                    .setOnGround(true);
            if (!entity.visible()) {
                entity.setClientVisible(false);
                event.setCancelled(true);
            } else {
                entity.setClientVisible(true);
            }
        }

        else if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            if (world == null) {
                return;
            }
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
            if (packet.getUUID().isEmpty()) {
                Logger.error(new RuntimeException("Entity UUID null when handling spawn entity packet, id=" + packet.getEntityId() + " tick=" + currentTick), 2, PacketEventsEntityViewController.class);
                return;
            }
            UUID entityUUID = packet.getUUID().get();

            PacketEventsEntity entity = trackEntitySpawn(playerData.entityView().cast(), entityUUID, packet.getEntityId(), world,
                    packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), playerData.ownLocation(), EntityLocatable.SpawnType.ENTITY, packet.getEntityType());
            if (entity == null) {
                return;
            }
            Vector3d velocity = packet.getVelocity().orElseGet(Vector3d::zero);
            entity.setEntityData(packet.getData())
                    .setYaw(packet.getYaw())
                    .setPitch(packet.getPitch())
                    .setHeadYaw(packet.getHeadYaw())
                    .setVelocity(velocity.getX(), velocity.getY(), velocity.getZ())
                    .setOnGround(true);
            if (!entity.visible()) {
                entity.setClientVisible(false);
                event.setCancelled(true);
            } else {
                entity.setClientVisible(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_PAINTING) {
            if (world == null) {
                return;
            }
            WrapperPlayServerSpawnPainting packet = new WrapperPlayServerSpawnPainting(event);
            PacketEventsEntity entity = trackEntitySpawn(playerData.entityView().cast(), packet.getUUID(), packet.getEntityId(), world,
                    packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), playerData.ownLocation(), EntityLocatable.SpawnType.PAINTING, packet.getType().orElse(null), packet.getDirection());
            if (entity == null) {
                return;
            }
            entity.setYaw(0.0F)
                    .setPitch(0.0F)
                    .setHeadYaw(0.0F)
                    .setVelocity(0.0, 0.0, 0.0)
                    .setOnGround(true);
            if (!entity.visible()) {
                entity.setClientVisible(false);
                event.setCancelled(true);
            } else {
                entity.setClientVisible(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            if (world == null) {
                return;
            }
            WrapperPlayServerSpawnPlayer packet = new WrapperPlayServerSpawnPlayer(event);
            PacketEventsEntity entity = trackEntitySpawn(playerData.playerView().cast(), packet.getUUID(), packet.getEntityId(), world,
                    packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), playerData.ownLocation(), EntityLocatable.SpawnType.PLAYER, null);
            if (entity == null) {
                return;
            }
            entity.setYaw(packet.getYaw())
                    .setPitch(packet.getPitch())
                    .setHeadYaw(packet.getYaw())
                    .setVelocity(0.0, 0.0, 0.0)
                    .setMetadata(copyEntityMetadata(packet.getEntityMetadata()))
                    .setOnGround(true);
            if (!entity.visible()) {
                entity.setClientVisible(false);
                event.setCancelled(true);
            } else {
                entity.setClientVisible(true);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(event);
            handleRelativeMove(event, playerData, packet.getEntityId(), packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ(), packet.isOnGround(), currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            handleRelativeMoveAndRotation(
                    event,
                    playerData,
                    packet.getEntityId(),
                    packet.getDeltaX(),
                    packet.getDeltaY(),
                    packet.getDeltaZ(),
                    packet.getYaw(),
                    packet.getPitch(),
                    packet.isOnGround(),
                    currentTick
            );
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(event);
            handleTeleport(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            WrapperPlayServerEntityPositionSync packet = new WrapperPlayServerEntityPositionSync(event);
            handlePositionSync(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_ROTATION) {
            WrapperPlayServerEntityRotation packet = new WrapperPlayServerEntityRotation(event);
            handleRotation(event, playerData, packet.getEntityId(), packet.getYaw(), packet.getPitch(), packet.isOnGround(), currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_HEAD_LOOK) {
            WrapperPlayServerEntityHeadLook packet = new WrapperPlayServerEntityHeadLook(event);
            handleHeadLook(event, playerData, packet.getEntityId(), packet.getHeadYaw(), currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
            handleEntityMetadata(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
            handleEntityEquipment(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(event);
            handleEntityVelocity(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
            WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(event);
            handleEntityEffect(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
            handleEntityPassengers(event, playerData, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);
            handleDestroyEntities(event, viewer, playerData, packet, currentTick);
        }
    }*/
/*
    @Override
    protected NettyEntityLocatable<?,?,?,?> processLivingEntitySpawn(PlayerData playerData, PacketWrapper<?> packetWrapper, UUID world, int currentTick) {
        WrapperPlayServerSpawnLivingEntity packet = (WrapperPlayServerSpawnLivingEntity) packetWrapper;
        Logger.debug("SpawnLiv: " + packet.getEntityId());
        PacketEventsEntity entity = trackEntitySpawn(playerData.entityView().cast(), packet.getEntityUUID(), packet.getEntityId(), world,
                packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), playerData.ownLocation(), EntityLocatable.SpawnType.LIVING, packet.getEntityType());
        entity
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setHeadYaw(packet.getHeadPitch())
                .setVelocity(packet.getVelocity().getX(), packet.getVelocity().getY(), packet.getVelocity().getZ())
                .setMetadata(copyEntityMetadata(packet.getEntityMetadata()));
        return entity;
    }*/

    @Override
    protected @NotNull NettyEntityLocatable<?,?> processEntitySpawn(PlayerData playerData, PacketWrapper<?> packetWrapper, UUID world, int currentTick) {
        WrapperPlayServerSpawnEntity packet = (WrapperPlayServerSpawnEntity) packetWrapper;
        if (packet.getUUID().isEmpty()) {
            Logger.errorAndReturn(new RuntimeException("Entity UUID null when handling spawn entity packet, id=" + packet.getEntityId() + " tick=" + currentTick), 2, PacketEventsEntityViewController.class);
            return null;
        }
        UUID entityUUID = packet.getUUID().get();

        PacketEventsEntity entity = trackEntitySpawn(playerData, entityUUID, packet.getEntityId(), world,
                packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), packet.getEntityType());
        Vector3d velocity = packet.getVelocity().orElseGet(Vector3d::zero);
        entity.setEntityData(packet.getData())
                .setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setHeadYaw(packet.getHeadYaw())
                .setVelocity(velocity.getX(), velocity.getY(), velocity.getZ());
        return entity;
    }
/*
    @Override
    protected NettyEntityLocatable<?,?,?,?> processPaintingSpawn(PlayerData playerData, PacketWrapper<?> packetWrapper, UUID world, int currentTick) {
        WrapperPlayServerSpawnPainting packet = (WrapperPlayServerSpawnPainting) packetWrapper;
        PacketEventsEntity entity = new PacketEventsEntity(world, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), packet.getEntityId(), packet.getUUID(), EntityLocatable.SpawnType.PAINTING, packet.getType().orElse(null), packet.getDirection(), true);
        ensureReplayData(entity);
        return entity;
    }

    @Override
    protected NettyEntityLocatable<?,?,?,?> processPlayerSpawn(PlayerData playerData, PacketWrapper<?> packetWrapper, UUID world, int currentTick) {
        WrapperPlayServerSpawnPlayer packet = (WrapperPlayServerSpawnPlayer) packetWrapper;
        PacketEventsEntity entity = trackEntitySpawn(playerData.playerView().cast(), packet.getUUID(), packet.getEntityId(), world,
                packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), playerData.ownLocation(), EntityLocatable.SpawnType.PLAYER, null);
        entity.setYaw(packet.getYaw())
                .setPitch(packet.getPitch())
                .setHeadYaw(packet.getYaw())
                .setMetadata(copyEntityMetadata(packet.getEntityMetadata()));
        return entity;
    }*/

    @Override
    protected int processRelativeMovePacket(PacketWrapper<?> packetWrapper, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityRelativeMove packet = (WrapperPlayServerEntityRelativeMove) packetWrapper;
        int entityID = packet.getEntityId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received relative move packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        entity.add(packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
        entity.setOnGround(packet.isOnGround());

        return entityID;
    }

    @Override
    protected int processRelativeMoveAndRotationPacket(PacketWrapper<?> packet, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityRelativeMoveAndRotation packetWrapper = (WrapperPlayServerEntityRelativeMoveAndRotation) packet;
        int entityID = packetWrapper.getEntityId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received relative move and rotation packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        entity.add(packetWrapper.getDeltaX(), packetWrapper.getDeltaY(), packetWrapper.getDeltaZ());
        entity.setYaw(packetWrapper.getYaw()).setPitch(packetWrapper.getPitch()).setOnGround(packetWrapper.isOnGround());

        return entityID;
    }

    @Override
    protected int processTeleportPacket(PacketWrapper<?> packet, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityTeleport packetWrapper = (WrapperPlayServerEntityTeleport) packet;
        int entityID = packetWrapper.getEntityId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received teleport packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        Vector3d position = packetWrapper.getPosition();
        Vector3d velocity = packetWrapper.getDeltaMovement();
        entity.set(position.getX(), position.getY(), position.getZ());
        entity.setYaw(packetWrapper.getYaw()).setPitch(packetWrapper.getPitch()).setVelocity(velocity.x, velocity.y, velocity.z).setOnGround(packetWrapper.isOnGround());

        return entityID;
    }

    @Override
    protected int processPositionSyncPacket(PacketWrapper<?> packet, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityPositionSync packetWrapper = (WrapperPlayServerEntityPositionSync) packet;
        int entityID = packetWrapper.getId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received position sync packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        Vector3d position = packetWrapper.getValues().getPosition();
        Vector3d velocity = packetWrapper.getValues().getDeltaMovement();
        entity.set(position.getX(), position.getY(), position.getZ());
        entity.setYaw(packetWrapper.getValues().getYaw()).setPitch(packetWrapper.getValues().getPitch()).setVelocity(velocity.x, velocity.y, velocity.z).setOnGround(packetWrapper.isOnGround());

        return entityID;
    }



    @Override
    protected void cachePacket(PacketWrapper<?> packet, int entityID, PlayerData playerData) {
        cachePacket(packet, entityID, playerData, DELAYED_CACHE_PACKET_RETRY_COUNT);
    }

    private void cachePacket(PacketWrapper<?> packet, int entityID, PlayerData playerData, int retriesRemaining) {
        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            if (retriesRemaining > 0) {
                Logger.error("Attempted to cache packet for unknown entity, id=" + entityID + " packet=" + packet.getClass().getSimpleName() + ". Will attempt again.", 6, PacketEventsEntityViewController.class);
                delayPacketHandling(playerData, () -> cachePacket(packet, entityID, playerData, retriesRemaining - 1));
                return;
            }
            Logger.error("Failed to cache packet after retries, id=" + entityID + " packet=" + packet.getClass().getSimpleName(), 2, PacketEventsEntityViewController.class);
            return;
        }
        ensureReplayData((PacketEventsEntity) entity).addPacket(packet);
    }

    @Override
    protected int processRotationPacket(PacketWrapper<?> packet, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityRotation packetWrapper = (WrapperPlayServerEntityRotation) packet;
        int entityID = packetWrapper.getEntityId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received rotation packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        entity.setYaw(packetWrapper.getYaw()).setPitch(packetWrapper.getPitch()).setOnGround(packetWrapper.isOnGround());

        return entityID;
    }

    @Override
    protected int processHeadLookPacket(PacketWrapper<?> packet, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityHeadLook packetWrapper = (WrapperPlayServerEntityHeadLook) packet;
        int entityID = packetWrapper.getEntityId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received head look packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        entity.setHeadYaw(packetWrapper.getHeadYaw());

        return entityID;
    }

    @Override
    protected int processEntityVelocityPacket(PacketWrapper<?> packet, PlayerData playerData, int currentTick) {
        WrapperPlayServerEntityVelocity packetWrapper = (WrapperPlayServerEntityVelocity) packet;
        int entityID = packetWrapper.getEntityId();

        NettyEntityLocatable<?,?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.error("Received velocity packet for unknown entity, id=" + entityID, 2, PacketEventsEntityViewController.class);
            return entityID;
        }
        entity.setVelocity(packetWrapper.getVelocity().getX(), packetWrapper.getVelocity().getY(), packetWrapper.getVelocity().getZ());

        return entityID;
    }

    @Override
    protected void sendEntityPassengerPacket(int vehicle, ArrayList<Integer> passengers, PlayerData playerData) {
        NettyEntityLocatable<?,?> entity = playerData.entityFromID(vehicle);
        if (entity == null) {
            Logger.error("Attempted to send passenger packet for unknown entity, id=" + vehicle, 2, PacketEventsEntityViewController.class);
            return;
        }
        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(vehicle, passengers.stream().mapToInt(Integer::intValue).toArray());
        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(playerData.getPlayerUUID());
        PacketEvents.getAPI().getProtocolManager().getUser(channel).writePacketSilently(packet);
    }

    private PacketEventsEntity trackEntitySpawn(PlayerData playerData, UUID entityUUID, int entityID, UUID world, double x, double y, double z, EntityType entityType) {

        PacketEventsEntity entity = new PacketEventsEntity(playerData, world, x, y, z, entityID, entityUUID, false, entityType, true /*default value as this is handled in PacketEntityViewController*/);
        ensureReplayData(entity);
        return entity;
    }

    private void processEntityTransitions(UUID viewerUUID, User viewer, EntityView<PacketEventsEntity> entityView) {
        for (EntityViewTransition transition : entityView.drainTransitions()) {
            PacketEventsEntity entity = getTrackedEntity(entityView, transition.targetUUID());

            switch (transition.type()) {
                case HIDE -> {
                    if (entity != null && entity.clientVisible() && entity.entityID() >= 0) {
                        viewer.writePacketSilently(new WrapperPlayServerDestroyEntities(entity.entityID()));
                        entity.setClientVisible(false);
                    }
                }
                case SHOW -> {
                    if (entity == null || entity.isSelfEntity()) {
                        Logger.warning("PacketEvents.processEntityTransitions show-skipped viewer=" + viewerUUID
                                + " target=" + transition.targetUUID()
                                + " reason="
                                + (entity == null ? "missing-entity" : "self-entity"), 2, PacketEventsEntityViewController.class);
                        continue;
                    }
                    PacketEventsEntityReplayData replayData = ensureReplayData(entity);
                    sendEntityShow(viewer, PlayerRegistry.getInstance().getPlayerData(viewerUUID), entity, replayData);
                    entity.setClientVisible(true);
                }
            }
        }
    }

    private PacketWrapper<?> buildSpawnPacket(PacketEventsEntity entity) {
        if (entity.isSelfEntity()) {
            Logger.errorAndReturn(new RuntimeException("Should not build spawn packet for self entity"), 1, PacketEventsEntityViewController.class);
            return null;
        }
        return new WrapperPlayServerSpawnEntity(
                    entity.entityID(),
                    Optional.of(entity.entityUUID()),
                    entity.entityType(),
                    new Vector3d(entity.x(), entity.y(), entity.z()),
                    entity.pitch(),
                    entity.yaw(),
                    entity.headYaw(),
                    entity.entityData(),
                    Optional.of(new Vector3d(entity.velocityX(), entity.velocityY(), entity.velocityZ()))
            );
    }

    private WrapperPlayServerSetPassengers buildPassengersPacket(@Nullable NettyEntityLocatable<?,?> entity, PlayerData playerData) {
        if (entity == null) {
            return null;
        }
        int[] passengerIDs = entity.passengerIDs();
        if (passengerIDs == null || passengerIDs.length == 0) {
            return null;
        }
        ArrayList<Integer> visiblePassengerIDs = new ArrayList<>();
        for (int passengerID : passengerIDs) {
            NettyEntityLocatable<?,?> passenger = playerData.entityFromID(passengerID);
            if (passenger != null && passenger.visible()) {
                visiblePassengerIDs.add(passengerID);
            }
        }
        return new WrapperPlayServerSetPassengers(entity.entityID(), visiblePassengerIDs.stream().mapToInt(Integer::intValue).toArray());
    }

    private @Nullable WrapperPlayServerAttachEntity[] buildLeashPackets(PacketEventsEntity entity, PlayerData playerData) {
        int[] leashedIDs = entity.leashedEntityIDsOrNull();
        int leashingID = entity.leashingEntity();
        WrapperPlayServerAttachEntity leashingShow = null;
        if (leashingID != NO_LEASHER) {
            NettyEntityLocatable<?,?> leashHolder = playerData.entityFromID(leashingID);
            if (leashHolder != null && leashHolder.visible()) {
                leashingShow = new WrapperPlayServerAttachEntity(entity.entityID(), leashingID, true);
            }
        }
        if (leashedIDs == null || leashedIDs.length == 0) {
            return leashingShow == null ? null : new WrapperPlayServerAttachEntity[]{leashingShow};
        }
        WrapperPlayServerAttachEntity[] packets = new WrapperPlayServerAttachEntity[leashedIDs.length + (leashingShow == null ? 0 : 1)];
        int index = 0;
        if (leashingShow != null) {
            packets[0] = leashingShow;
            index = 1;
        }
        for (int leashedID : leashedIDs) {
            NettyEntityLocatable<?,?> leashHolder = playerData.entityFromID(leashedID);
            if (leashHolder != null && leashHolder.visible()) {
                packets[index] = new WrapperPlayServerAttachEntity(leashedID, entity.entityID(), true);
                index++;
            }
        }
        return packets;
    }

    private WrapperPlayServerEntityEffect copyEffectPacket(WrapperPlayServerEntityEffect effect) {
        WrapperPlayServerEntityEffect copy = new WrapperPlayServerEntityEffect(
                effect.getEntityId(),
                effect.getPotionType(),
                effect.getEffectAmplifier(),
                effect.getEffectDurationTicks(),
                buildEffectFlags(effect.isAmbient(), effect.isVisible(), effect.isShowIcon())
        );
        copy.setFactorData(effect.getFactorData());
        return copy;
    }

    private WrapperPlayServerEntityMetadata copyEntityMetadataPacket(WrapperPlayServerEntityMetadata packet) {
        return new WrapperPlayServerEntityMetadata(
                packet.getEntityId(),
                copyEntityMetadata(packet.getEntityMetadata())
        );
    }

    private WrapperPlayServerEntityEquipment copyEntityEquipmentPacket(WrapperPlayServerEntityEquipment packet) {
        return new WrapperPlayServerEntityEquipment(
                packet.getEntityId(),
                copyEquipment(packet.getEquipment())
        );
    }

    private WrapperPlayServerEntityVelocity copyEntityVelocityPacket(WrapperPlayServerEntityVelocity packet) {
        return new WrapperPlayServerEntityVelocity(
                packet.getEntityId(),
                new Vector3d(packet.getVelocity().getX(), packet.getVelocity().getY(), packet.getVelocity().getZ())
        );
    }

    private WrapperPlayServerRemoveEntityEffect copyRemoveEntityEffectPacket(WrapperPlayServerRemoveEntityEffect packet) {
        return new WrapperPlayServerRemoveEntityEffect(
                packet.getEntityId(),
                packet.getPotionType()
        );
    }

    private List<EntityData<?>> copyEntityMetadata(List<EntityData<?>> metadata) {
        return metadata == null ? List.of() : List.copyOf(metadata);
    }

    private List<Equipment> copyEquipment(List<Equipment> equipment) {
        return equipment == null ? List.of() : List.copyOf(equipment);
    }

    private byte buildEffectFlags(boolean ambient, boolean visible, boolean showIcon) {
        byte flags = 0;
        if (ambient) {
            flags |= 1;
        }
        if (visible) {
            flags |= 2;
        }
        if (showIcon) {
            flags |= 4;
        }
        return flags;
    }
    
    @SuppressWarnings("unchecked")
    public  <T> T cast(Object value) {
        return (T) value;
    }

    private PacketEventsEntity getTrackedEntity(EntityView<PacketEventsEntity> entityView, UUID entityUUID) {
        return entityView.getEntity(entityUUID);
    }

    private PacketEventsEntity getTrackedEntity(EntityView<PacketEventsEntity> entityView, int entityID) {
        return entityView.getEntity(entityID);
    }

    private PacketEventsEntityReplayData ensureReplayData(PacketEventsEntity entity) {
        PacketEventsEntityReplayData replayData = entity.packetReplayData();
        if (replayData == null) {
            replayData = PacketEventsEntityReplayData.create();
            entity.setPacketReplayData(replayData);
        }
        return replayData;
    }

    private void sendEntityShow(User viewer, PlayerData data, PacketEventsEntity entity, PacketEventsEntityReplayData replayData) {
        viewer.writePacketSilently(buildSpawnPacket(entity));
        sendEntityAbsoluteCorrection(viewer, entity);

        for (PacketWrapper<?> cachedPacket : replayData.getPackets()) {

            if (cachedPacket.getClass() == WrapperPlayServerEntityMetadata.class) {
                WrapperPlayServerEntityMetadata metadataPacket = copyEntityMetadataPacket((WrapperPlayServerEntityMetadata) cachedPacket);
                viewer.writePacketSilently(metadataPacket);
            } else if (cachedPacket.getClass() == WrapperPlayServerEntityEquipment.class) {
                WrapperPlayServerEntityEquipment equipmentPacket = copyEntityEquipmentPacket((WrapperPlayServerEntityEquipment) cachedPacket);
                viewer.writePacketSilently(equipmentPacket);
            } else if (cachedPacket.getClass() == WrapperPlayServerEntityVelocity.class) {
                WrapperPlayServerEntityVelocity velocityPacket = copyEntityVelocityPacket((WrapperPlayServerEntityVelocity) cachedPacket);
                viewer.writePacketSilently(velocityPacket);
            } else if (cachedPacket.getClass() == WrapperPlayServerEntityEffect.class) {
                viewer.writePacketSilently(copyEffectPacket((WrapperPlayServerEntityEffect) cachedPacket));
            } else if (cachedPacket.getClass() == WrapperPlayServerRemoveEntityEffect.class) {
                viewer.writePacketSilently(copyRemoveEntityEffectPacket((WrapperPlayServerRemoveEntityEffect) cachedPacket));
            } else if (cachedPacket.getClass() == WrapperPlayServerUpdateAttributes.class) {
                WrapperPlayServerUpdateAttributes existing = (WrapperPlayServerUpdateAttributes) cachedPacket;
                WrapperPlayServerUpdateAttributes copy = new WrapperPlayServerUpdateAttributes(existing.getEntityId(), existing.getProperties());
                viewer.writePacketSilently(copy);
            }
            else {
                Logger.warning("Unsupported cached packet type for replay: " + cachedPacket.getClass().getName(), 2, PacketEventsEntityViewController.class);
            }
        }
        common.writeIfPresent(viewer, buildPassengersPacket(entity, data));
        common.writeIfPresent(viewer, buildPassengersPacket(data.entityFromID(entity.vehicleID()), data));
        WrapperPlayServerAttachEntity[] leashPackets = buildLeashPackets(entity, data);
        if (leashPackets == null) return;
        for (WrapperPlayServerAttachEntity leashPacket : leashPackets) {
            if (leashPacket != null) {
                viewer.writePacketSilently(leashPacket);
            }
        }
    }

    private void sendEntityAbsoluteCorrection(User viewer, PacketEventsEntity entity) {
        if (entity.entityID() < 0) {
            return;
        }
        viewer.writePacketSilently(new WrapperPlayServerEntityTeleport(
                entity.entityID(),
                new Vector3d(entity.x(), entity.y(), entity.z()),
                new Vector3d(entity.velocityX(), entity.velocityY(), entity.velocityZ()),
                entity.yaw(),
                entity.pitch(),
                RelativeFlag.NONE,
                entity.onGround()
        ));
        viewer.writePacketSilently(new WrapperPlayServerEntityHeadLook(entity.entityID(), entity.headYaw()));
    }

    protected void insertEntityToPlayerView(NettyEntityLocatable<?,?> entity, PlayerData playerData) {
        playerData.playerView().insertEntity(entity.cast());
    }

    protected void insertEntityToEntityView(NettyEntityLocatable<?,?> entity, PlayerData playerData) {
        playerData.entityView().insertEntity(entity.cast()); //todo: no need to put here, move to abstract packet view controller
    }
}
