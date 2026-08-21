/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.utils.BaseEntitySpawnTask;

import java.util.function.Consumer;

final class PEEntityStateReconciliationTask extends BaseEntitySpawnTask {
    private final PlayerData playerData;
    private final int entityID;
    private final String packetDescription;
    private final Consumer<NettyEntity<?>> update;

    private PEEntityStateReconciliationTask(PlayerData playerData, int entityID, String packetDescription, Consumer<NettyEntity<?>> update, int submittedTick) {
        super(submittedTick);
        this.playerData = playerData;
        this.entityID = entityID;
        this.packetDescription = packetDescription;
        this.update = update;
    }

    static PEEntityStateReconciliationTask relativeMove(PlayerData playerData, int entityID, double deltaX, double deltaY, double deltaZ, boolean onGround, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "relative move", entity ->
                PacketEventsEntityViewController.applyRelativeMove(entity, deltaX, deltaY, deltaZ, onGround), submittedTick);
    }

    static PEEntityStateReconciliationTask relativeMoveAndRotation(PlayerData playerData, int entityID, double deltaX, double deltaY, double deltaZ,
                                                                   float yaw, float pitch, boolean onGround, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "relative move and rotation", entity ->
                PacketEventsEntityViewController.applyRelativeMoveAndRotation(entity, deltaX, deltaY, deltaZ, yaw, pitch, onGround), submittedTick);
    }

    static PEEntityStateReconciliationTask teleport(PlayerData playerData, int entityID, double x, double y, double z, float yaw, float pitch,
                                                    double velocityX, double velocityY, double velocityZ, boolean onGround, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "teleport", entity ->
                PacketEventsEntityViewController.applyTeleport(entity, x, y, z, yaw, pitch, velocityX, velocityY, velocityZ, onGround), submittedTick);
    }

    static PEEntityStateReconciliationTask positionSync(PlayerData playerData, int entityID, double x, double y, double z, float yaw, float pitch,
                                                        double velocityX, double velocityY, double velocityZ, boolean onGround, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "position sync", entity ->
                PacketEventsEntityViewController.applyPositionSync(entity, x, y, z, yaw, pitch, velocityX, velocityY, velocityZ, onGround), submittedTick);
    }

    static PEEntityStateReconciliationTask rotation(PlayerData playerData, int entityID, float yaw, float pitch, boolean onGround, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "rotation", entity ->
                PacketEventsEntityViewController.applyRotation(entity, yaw, pitch, onGround), submittedTick);
    }

    static PEEntityStateReconciliationTask headLook(PlayerData playerData, int entityID, float headYaw, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "head look", entity ->
                PacketEventsEntityViewController.applyHeadLook(entity, headYaw), submittedTick);
    }

    static PEEntityStateReconciliationTask velocity(PlayerData playerData, int entityID, double velocityX, double velocityY, double velocityZ, int submittedTick) {
        return new PEEntityStateReconciliationTask(playerData, entityID, "velocity", entity ->
                PacketEventsEntityViewController.applyVelocity(entity, velocityX, velocityY, velocityZ), submittedTick);
    }

    @Override
    public void run() {
        if (PacketEventsEntityViewController.isBypassed(entityID)) {
            return;
        }
        NettyEntity<?> entity = playerData.entityFromID(entityID);
        if (entity == null) {
            Logger.warning("Deferred " + packetDescription + " packet still has no tracked entity, id=" + entityID + ".", 6, this.getClass());
            return;
        }
        update.accept(entity);
    }

    @Override
    public String toString() {
        return "PEEntityStateReconciliationTask{" +
                "submittedTick=" + submittedTick +
                ", entityID=" + entityID +
                ", packetDescription='" + packetDescription + '\'' +
                ", playerUUID=" + playerData.getPlayerUUID() +
                '}';
    }
}
