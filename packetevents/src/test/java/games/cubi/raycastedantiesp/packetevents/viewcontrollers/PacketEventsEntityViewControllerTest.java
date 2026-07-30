/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import games.cubi.raycastedantiesp.core.view.EntityViewTransition;
import org.junit.jupiter.api.Test;

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
                EntityViewTransition.Type.HIDE, true, true));
        assertEquals(SYNC, PacketEventsEntityViewController.resolveClientTransitionAction(
                EntityViewTransition.Type.SHOW, true, true));
    }

    @Test
    void disablingRetentionUsesDestroyAndRespawn() {
        assertEquals(DESTROY, PacketEventsEntityViewController.resolveClientTransitionAction(
                EntityViewTransition.Type.HIDE, true, false));
        assertEquals(SPAWN_AND_SYNC, PacketEventsEntityViewController.resolveClientTransitionAction(
                EntityViewTransition.Type.SHOW, false, false));
    }

    @Test
    void unseenEntityIsNeverDestroyedAndRequiresFirstSpawn() {
        assertEquals(NONE, PacketEventsEntityViewController.resolveClientTransitionAction(
                EntityViewTransition.Type.HIDE, false, true));
        assertEquals(SPAWN_AND_SYNC, PacketEventsEntityViewController.resolveClientTransitionAction(
                EntityViewTransition.Type.SHOW, false, true));
    }

    @Test
    void forgetTransitionDoesNotWriteClientPackets() {
        assertEquals(NONE, PacketEventsEntityViewController.resolveClientTransitionAction(
                EntityViewTransition.Type.FORGET, true, false));
    }

    @Test
    void staleVisibilityTransitionsAreIgnored() {
        assertFalse(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                EntityViewTransition.Type.SHOW, false));
        assertFalse(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                EntityViewTransition.Type.HIDE, true));
        assertTrue(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                EntityViewTransition.Type.SHOW, true));
        assertTrue(PacketEventsEntityViewController.transitionMatchesCurrentVisibility(
                EntityViewTransition.Type.HIDE, false));
    }

    @Test
    void normalizesVanillaAndCompatibilityUnleashSentinels() {
        assertEquals(NO_LEASHER, PacketEventsEntityViewController.normalizeLeashHolderEntityID(0));
        assertEquals(NO_LEASHER, PacketEventsEntityViewController.normalizeLeashHolderEntityID(-1));
        assertEquals(1, PacketEventsEntityViewController.normalizeLeashHolderEntityID(1));
        assertEquals(-3, PacketEventsEntityViewController.normalizeLeashHolderEntityID(-3));
    }

}
