/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import games.cubi.raycastedantiesp.core.config.raycast.EntityTypeExclusions;
import games.cubi.raycastedantiesp.core.entity.EntityBypassRegistry;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController;

import java.util.function.IntSupplier;

public class PaperPacketEventsEntityViewController extends PacketEventsEntityViewController {

    public PaperPacketEventsEntityViewController(IntSupplier currentTickSupplier) {
        super(currentTickSupplier);
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (shouldBypass(event)) {
            return;
        }
        super.onPacketSend(event);
    }

    private boolean shouldBypass(PacketSendEvent event) {
        return switch (event.getPacketType()) {
            case PacketType.Play.Server.SPAWN_ENTITY -> shouldBypassSpawn(new WrapperPlayServerSpawnEntity(event));
            case PacketType.Play.Server.ENTITY_ANIMATION -> isBypassed(new WrapperPlayServerEntityAnimation(event).getEntityId());
            case PacketType.Play.Server.ENTITY_STATUS -> isBypassed(new WrapperPlayServerEntityStatus(event).getEntityId());
            case PacketType.Play.Server.HURT_ANIMATION -> isBypassed(new WrapperPlayServerHurtAnimation(event).getEntityId());
            case PacketType.Play.Server.ENTITY_RELATIVE_MOVE -> isBypassed(new WrapperPlayServerEntityRelativeMove(event).getEntityId());
            case PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION -> isBypassed(new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId());
            case PacketType.Play.Server.ENTITY_TELEPORT -> isBypassed(new WrapperPlayServerEntityTeleport(event).getEntityId());
            case PacketType.Play.Server.ENTITY_POSITION_SYNC -> isBypassed(new WrapperPlayServerEntityPositionSync(event).getId());
            case PacketType.Play.Server.ENTITY_ROTATION -> isBypassed(new WrapperPlayServerEntityRotation(event).getEntityId());
            case PacketType.Play.Server.ENTITY_HEAD_LOOK -> isBypassed(new WrapperPlayServerEntityHeadLook(event).getEntityId());
            case PacketType.Play.Server.ENTITY_METADATA -> isBypassed(new WrapperPlayServerEntityMetadata(event).getEntityId());
            case PacketType.Play.Server.REMOVE_ENTITY_EFFECT -> isBypassed(new WrapperPlayServerRemoveEntityEffect(event).getEntityId());
            case PacketType.Play.Server.ENTITY_EQUIPMENT -> isBypassed(new WrapperPlayServerEntityEquipment(event).getEntityId());
            case PacketType.Play.Server.ENTITY_VELOCITY -> isBypassed(new WrapperPlayServerEntityVelocity(event).getEntityId());
            case PacketType.Play.Server.ENTITY_EFFECT -> isBypassed(new WrapperPlayServerEntityEffect(event).getEntityId());
            case PacketType.Play.Server.UPDATE_ATTRIBUTES -> isBypassed(new WrapperPlayServerUpdateAttributes(event).getEntityId());
            case PacketType.Play.Server.SET_PASSENGERS -> {
                WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
                yield isBypassed(packet.getEntityId()) || containsBypassed(packet.getPassengers());
            }
            case PacketType.Play.Server.ATTACH_ENTITY -> {
                WrapperPlayServerAttachEntity packet = new WrapperPlayServerAttachEntity(event);
                yield isBypassed(packet.getAttachedId()) || isBypassed(packet.getHoldingId());
            }
            case PacketType.Play.Server.DESTROY_ENTITIES -> containsBypassed(new WrapperPlayServerDestroyEntities(event).getEntityIds());
            default -> false;
        };
    }

    private boolean shouldBypassSpawn(WrapperPlayServerSpawnEntity packet) {
        int entityID = packet.getEntityId();
        if (isBypassed(entityID)) {
            return true;
        }
        if (packet.getEntityType().isInstanceOf(EntityTypes.PLAYER)) {
            return false;
        }
        int entityType = packet.getEntityType().getId(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()
        );
        if (!EntityTypeExclusions.excludes(entityType)) {
            return false;
        }
        EntityBypassRegistry.bypassedEntityIDs().add(entityID);
        return true;
    }

    private static boolean isBypassed(int entityID) {
        return EntityBypassRegistry.isBypassed(entityID);
    }

    private static boolean containsBypassed(int[] entityIDs) {
        for (int entityID : entityIDs) {
            if (isBypassed(entityID)) {
                return true;
            }
        }
        return false;
    }
}
