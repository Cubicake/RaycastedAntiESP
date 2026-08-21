/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.implementations.ImmutableLocatableImpl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLocatableTest {
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_WORLD = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void poseCameraOffsetsAreExposed() {
        assertEquals(1.62f, PlayerPose.UNKNOWN.cameraYOffset());
        assertEquals(1.62f, PlayerPose.STANDING.cameraYOffset());
        assertEquals(1.27f, PlayerPose.SNEAKING.cameraYOffset());
        assertEquals(0.40f, PlayerPose.SWIMMING.cameraYOffset());
        assertEquals(0.40f, PlayerPose.FALL_FLYING.cameraYOffset());
        assertEquals(0.40f, PlayerPose.SPIN_ATTACK.cameraYOffset());
        assertEquals(0.20f, PlayerPose.SLEEPING.cameraYOffset());
        assertEquals(0.20f, PlayerPose.DYING.cameraYOffset());
    }

    @Test
    void startsInvalidAndInitializesCompleteCameraState() {
        PlayerLocatable location = new PlayerLocatable();

        assertNull(location.world());
        assertNull(location.pose());
        assertEquals(0.0f, location.cameraYOffset());

        location.initialise(WORLD, 10.0, 20.0, 30.0, 90.0f, -25.0f, PlayerPose.SNEAKING);

        assertEquals(WORLD, location.world());
        assertEquals(10.0, location.x());
        assertEquals(20.0, location.y());
        assertEquals(30.0, location.z());
        assertEquals(90.0f, location.yaw());
        assertEquals(-25.0f, location.pitch());
        assertEquals(PlayerPose.SNEAKING, location.pose());
        assertEquals(1.27f, location.cameraYOffset());
        assertEquals(10.0, location.cameraX());
        assertEquals(21.27, location.cameraY(), 1.0e-6);
        assertEquals(30.0, location.cameraZ());
        assertEquals(1.27 * 1.27, location.cameraDistanceSquared(new ImmutableLocatableImpl(WORLD, 10.0, 20.0, 30.0)), 1.0e-6);
    }

    @Test
    void partialUpdatesPreserveOmittedStateAndRequireInitialization() {
        PlayerLocatable location = new PlayerLocatable();
        assertThrows(IllegalStateException.class, () -> location.updateFoot(1.0, 2.0, 3.0));
        assertThrows(IllegalStateException.class, () -> location.updateRotation(1.0f, 2.0f));
        assertThrows(IllegalStateException.class, () -> location.updatePose(PlayerPose.STANDING));

        location.initialise(WORLD, 1.0, 2.0, 3.0, 4.0f, 5.0f, PlayerPose.STANDING);
        location.updateFoot(6.0, 7.0, 8.0);
        location.updateRotation(9.0f, 10.0f);
        location.updatePose(PlayerPose.SNEAKING);

        assertEquals(6.0, location.x());
        assertEquals(7.0, location.y());
        assertEquals(8.0, location.z());
        assertEquals(9.0f, location.yaw());
        assertEquals(10.0f, location.pitch());
        assertEquals(PlayerPose.SNEAKING, location.pose());
    }

    @Test
    void locatableMutationsPreserveViewStateAndSetWorldNullInvalidates() {
        PlayerLocatable location = new PlayerLocatable();
        location.initialise(WORLD, 1.0, 2.0, 3.0, 4.0f, 5.0f, PlayerPose.SLEEPING);

        assertSame(location, location.setPosition(6.0, 7.0, 8.0));
        assertSame(location, location.setLocation(OTHER_WORLD, 9.0, 10.0, 11.0));
        assertEquals(4.0f, location.yaw());
        assertEquals(5.0f, location.pitch());
        assertEquals(PlayerPose.SLEEPING, location.pose());
        assertEquals(OTHER_WORLD, location.world());
        assertEquals(9.0, location.x());
        assertEquals(10.0, location.y());
        assertEquals(11.0, location.z());

        location.setWorld(null);

        assertNull(location.world());
        assertNull(location.pose());
        assertEquals(0.0, location.x());
        assertEquals(0.0, location.y());
        assertEquals(0.0, location.z());
        assertThrows(IllegalStateException.class, () -> location.setWorld(WORLD));
    }

    @Test
    void equalityUsesWorldAndFootCoordinatesOnly() {
        PlayerLocatable first = new PlayerLocatable();
        PlayerLocatable second = new PlayerLocatable();
        first.initialise(WORLD, 1.0, 2.0, 3.0, 4.0f, 5.0f, PlayerPose.STANDING);
        second.initialise(WORLD, 1.0, 2.0, 3.0, 90.0f, -5.0f, PlayerPose.SLEEPING);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.strictlyEquals(second));
    }

    @Test
    void fullStateRejectsInvalidValues() {
        PlayerLocatable location = new PlayerLocatable();

        assertThrows(NullPointerException.class, () -> location.initialise(null, 0.0, 0.0, 0.0, 0.0f, 0.0f, PlayerPose.STANDING));
        assertThrows(NullPointerException.class, () -> location.initialise(WORLD, 0.0, 0.0, 0.0, 0.0f, 0.0f, null));
        assertThrows(IllegalArgumentException.class, () -> location.initialise(WORLD, Double.NaN, 0.0, 0.0, 0.0f, 0.0f, PlayerPose.STANDING));
        assertThrows(IllegalArgumentException.class, () -> location.initialise(WORLD, 0.0, 0.0, 0.0, Float.NaN, 0.0f, PlayerPose.STANDING));
        assertThrows(IllegalArgumentException.class, () -> location.initialise(WORLD, 0.0, 0.0, 0.0, 0.0f, 91.0f, PlayerPose.STANDING));
        assertNull(location.world());
    }
}
