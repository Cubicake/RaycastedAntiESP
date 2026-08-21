/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.raycast;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.Spatial;
import games.cubi.locatables.implementations.ImmutableBlockSpatialImpl;
import games.cubi.locatables.implementations.ImmutableLocatableImpl;
import games.cubi.locatables.implementations.ImmutableSpatialImpl;
import games.cubi.raycastedantiesp.core.view.BlockView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastUtilTest {
    @Test
    void spatialTargetUsesStartWorldForDebugParticles() {
        UUID world = UUID.randomUUID();
        Locatable start = new ImmutableLocatableImpl(world, 0, 0, 0);
        RecordingParticleSpawner particles = new RecordingParticleSpawner();

        assertTrue(RaycastUtil.raycast(start, new ImmutableSpatialImpl(2, 0, 0), 1, 0, 10, true, emptyBlockView(), 1, particles));
        assertEquals(List.of(world), particles.worlds);
    }

    @Test
    void blockSpatialTargetIsCentredBeforeRaycast() {
        UUID world = UUID.randomUUID();
        Locatable start = new ImmutableLocatableImpl(world, 0.5, 0.5, 0.5);
        RecordingParticleSpawner particles = new RecordingParticleSpawner();

        assertTrue(RaycastUtil.raycast(start, new ImmutableBlockSpatialImpl(3, 0, 0), 1, 0, 10, true, emptyBlockView(), 1, particles));
        assertEquals(0.5, particles.positions.getFirst().y());
        assertEquals(0.5, particles.positions.getFirst().z());
    }

    private static BlockView emptyBlockView() {
        return (BlockView) Proxy.newProxyInstance(
                BlockView.class.getClassLoader(),
                new Class<?>[]{BlockView.class},
                (proxy, method, args) -> method.getReturnType() == boolean.class ? false : null
        );
    }

    private static final class RecordingParticleSpawner implements ParticleSpawner {
        private final List<UUID> worlds = new ArrayList<>();
        private final List<Spatial> positions = new ArrayList<>();

        @Override
        public void spawnParticleAt(Locatable locatable, Colour colour) {
            spawnParticleAt(locatable.world(), locatable, colour);
        }

        @Override
        public void spawnParticleAt(UUID world, Spatial spatial, Colour colour) {
            worlds.add(world);
            positions.add(new ImmutableSpatialImpl(spatial.x(), spatial.y(), spatial.z()));
        }
    }
}
