/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.entity.EntityBypassRegistry;
import games.cubi.raycastedantiesp.core.utils.EntitySpawnTask;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.ViewRegistry;
import games.cubi.raycastedantiesp.packetevents.tracked.PacketEventsEntity;
import games.cubi.raycastedantiesp.packetevents.view.PacketEventsEntityView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PEEntityStateReconciliationTaskTest {
    private static final UUID WORLD = UUID.randomUUID();
    private final List<UUID> registeredPlayers = new ArrayList<>();

    @BeforeAll
    static void initialiseViews() {
        ViewRegistry.initialise(
                ignored -> null,
                PacketEventsEntityView::createEntityView,
                PacketEventsEntityView::createPlayerView
        );
    }

    @AfterEach
    void unregisterPlayers() {
        for (UUID player : registeredPlayers) {
            PlayerRegistry.getInstance().unregisterPlayer(player);
        }
        registeredPlayers.clear();
    }

    @Test
    void deferredStateUpdatesApplyInSubmissionOrder() {
        PlayerData playerData = playerData();
        int entityID = 42;

        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.relativeMove(
                playerData, entityID, 1, 2, 3, false, 1));
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.relativeMoveAndRotation(
                playerData, entityID, 4, 5, 6, 10, 20, true, 2));
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.teleport(
                playerData, entityID, 100, 200, 300, 30, 40, 1, 2, 3, false, 3));
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.positionSync(
                playerData, entityID, 400, 500, 600, 50, 60, 4, 5, 6, true, 4));
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.rotation(
                playerData, entityID, 70, 80, false, 5));
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.headLook(
                playerData, entityID, 90, 6));
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.velocity(
                playerData, entityID, 7, 8, 9, 7));

        PacketEventsEntity entity = insertEntity(playerData, entityID);
        playerData.nettyData().runPendingPostSpawnTaskForEntity(entityID);

        assertEquals(400, entity.x());
        assertEquals(500, entity.y());
        assertEquals(600, entity.z());
        assertEquals(70, entity.yaw());
        assertEquals(80, entity.pitch());
        assertEquals(90, entity.headYaw());
        assertEquals(7, entity.velocityX());
        assertEquals(8, entity.velocityY());
        assertEquals(9, entity.velocityZ());
        assertEquals(false, entity.onGround());
        assertNull(playerData.nettyData().consumePendingPostSpawnTasksForEntity(entityID));
    }

    @Test
    void missingEntityTaskIsConsumedWithoutRequeueing() {
        PlayerData playerData = playerData();
        int entityID = 43;
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.velocity(
                playerData, entityID, 1, 2, 3, 10));

        playerData.nettyData().runPendingPostSpawnTaskForEntity(entityID);

        assertNull(playerData.nettyData().consumePendingPostSpawnTasksForEntity(entityID));
    }

    @Test
    void deferredStateUpdateIsSkippedWhenEntityBecomesBypassed() {
        PlayerData playerData = playerData();
        int entityID = 45;
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.velocity(
                playerData, entityID, 1, 2, 3, 10));
        PacketEventsEntity entity = insertEntity(playerData, entityID);

        EntityBypassRegistry.addEntity(entityID);
        try {
            playerData.nettyData().runPendingPostSpawnTaskForEntity(entityID);
        } finally {
            EntityBypassRegistry.markEntityDespawned(entityID);
        }

        assertEquals(0, entity.velocityX());
        assertEquals(0, entity.velocityY());
        assertEquals(0, entity.velocityZ());
    }

    @Test
    void deferredStateTaskUsesExistingEvictionBoundary() {
        PlayerData playerData = playerData();
        int entityID = 44;
        playerData.nettyData().addPostEntitySpawnTask(entityID, PEEntityStateReconciliationTask.headLook(
                playerData, entityID, 15, 0));

        playerData.nettyData().evictOldPendingPostSpawnTasks(EntitySpawnTask.TICKS_BEFORE_EVICTION);

        assertNull(playerData.nettyData().consumePendingPostSpawnTasksForEntity(entityID));
    }

    private PlayerData playerData() {
        UUID playerUUID = UUID.randomUUID();
        registeredPlayers.add(playerUUID);
        return PlayerRegistry.getInstance().registerAndGetPlayer(
                playerUUID,
                0,
                1,
                PacketEventsEntity::createSelfEntity
        );
    }

    private static PacketEventsEntity insertEntity(PlayerData playerData, int entityID) {
        PacketEventsEntity entity = new PacketEventsEntity(
                playerData,
                0,
                0,
                0,
                entityID,
                UUID.randomUUID(),
                false,
                0,
                true
        );
        @SuppressWarnings("unchecked")
        EntityView<PacketEventsEntity> entityView = (EntityView<PacketEventsEntity>) (EntityView<?>) playerData.entityView();
        entityView.insertEntity(WORLD, entity);
        return entity;
    }
}
