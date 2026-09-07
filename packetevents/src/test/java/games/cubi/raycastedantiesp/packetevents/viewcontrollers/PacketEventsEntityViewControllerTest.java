/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import games.cubi.raycastedantiesp.core.view.TransitionType;
import games.cubi.raycastedantiesp.packetevents.tracked.PacketEventsEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static games.cubi.raycastedantiesp.core.tracked.NettyEntity.NO_LEASHER;
import static games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController.ClientTransitionAction.DESTROY;
import static games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController.ClientTransitionAction.NONE;
import static games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController.ClientTransitionAction.SPAWN_AND_SYNC;
import static games.cubi.raycastedantiesp.packetevents.viewcontrollers.PacketEventsEntityViewController.ClientTransitionAction.SYNC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsEntityViewControllerTest {
    @Test
    void retainingSeenEntitySuppressesHideWithoutDestroying() {
        assertEquals(NONE, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.HIDE, true, true));
        assertEquals(SYNC, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.SHOW, true, true));
    }

    @Test
    void disablingRetentionUsesDestroyAndRespawn() {
        assertEquals(DESTROY, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.HIDE, true, false));
        assertEquals(SPAWN_AND_SYNC, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.SHOW, false, false));
    }

    @Test
    void unseenEntityIsNeverDestroyedAndRequiresFirstSpawn() {
        assertEquals(NONE, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.HIDE, false, true));
        assertEquals(SPAWN_AND_SYNC, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.SHOW, false, true));
    }

    @Test
    void forgetTransitionDoesNotWriteClientPackets() {
        assertEquals(NONE, PacketEventsEntityViewController.resolveClientTransitionAction(
                TransitionType.Entity.FORGET, true, false));
    }

    @Test
    void staleVisibilityTransitionsAreIgnored() {
        assertFalse(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                TransitionType.Entity.SHOW, false));
        assertFalse(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                TransitionType.Entity.HIDE, true));
        assertTrue(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                TransitionType.Entity.SHOW, true));
        assertTrue(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                TransitionType.Entity.HIDE, false));
    }

    @Test
    void sharedEntityFlagsUpdateSneakingAndGlowingState() {
        PacketEventsEntity entity = entity();

        PacketEventsEntityViewController.applyTrackedMetadata(entity, List.of(
                new EntityData<>(0, null, (byte) 0x42)
        ));

        assertTrue(entity.sneaking());
        assertTrue(entity.glowing());

        PacketEventsEntityViewController.applyTrackedMetadata(entity, List.of(
                new EntityData<>(0, null, (byte) 0)
        ));

        assertFalse(entity.sneaking());
        assertFalse(entity.glowing());
    }

    @Test
    void metadataWithoutSharedFlagsPreservesTrackedState() {
        PacketEventsEntity entity = entity();
        entity.setSneaking(true);
        entity.setGlowing(true);

        PacketEventsEntityViewController.applyTrackedMetadata(entity, List.of(
                new EntityData<>(1, null, (byte) 0)
        ));

        assertTrue(entity.sneaking());
        assertTrue(entity.glowing());
    }

    private static PacketEventsEntity entity() {
        return new PacketEventsEntity(null, 0, 0, 0, 1, UUID.randomUUID(), false, 0, true);
    }

    @Test
    void normalizesVanillaAndCompatibilityUnleashSentinels() {
        assertEquals(NO_LEASHER, PacketEventsEntityViewController.normalizeLeashHolderEntityID(0));
        assertEquals(NO_LEASHER, PacketEventsEntityViewController.normalizeLeashHolderEntityID(-1));
        assertEquals(1, PacketEventsEntityViewController.normalizeLeashHolderEntityID(1));
        assertEquals(-3, PacketEventsEntityViewController.normalizeLeashHolderEntityID(-3));
    }

}
