/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.Spatial;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.raycast.ParticleSpawner;
import games.cubi.raycastedantiesp.paper.locatables.LocatableAdapterUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public class PaperParticleSpawner implements ParticleSpawner {
    public void spawnParticleAt(Locatable locatable, Colour colour) {
        Objects.requireNonNull(Bukkit.getWorld(locatable.world())).spawnParticle(Particle.DUST, locatable.x(), locatable.y(), locatable.z(), 0, toBukkitDust(colour));
    }

    //While not strictly-speaking thread-safe since bukkit player data is accessed, this effectively just dispatches a packet and should not cause any issues when called async.
    public void spawnParticleAt(UUID worldUUID, Spatial spatial, Colour colour) {
        World world = Logger.requireNonNull(LocatableAdapterUtils.getWorld(worldUUID), "UUID resolved to nonexistent world", 2, PaperParticleSpawner.class);
        world.spawnParticle(Particle.DUST, spatial.x(), spatial.y(), spatial.z(), 0, toBukkitDust(colour));
    }

    public void spawnParticleAt(UUID worldUUID, double x, double y, double z, Colour colour) {
        World world = Logger.requireNonNull(LocatableAdapterUtils.getWorld(worldUUID), "UUID resolved to nonexistent world", 2, PaperParticleSpawner.class);
        world.spawnParticle(Particle.DUST, x, y, z, 0, toBukkitDust(colour));
    }

    private static final Particle.DustOptions RED_DUST = new Particle.DustOptions(Color.RED, 1);
    private static final Particle.DustOptions GREEN_DUST = new Particle.DustOptions(Color.GREEN, 1);
    private static final Particle.DustOptions BLUE_DUST = new Particle.DustOptions(Color.BLUE, 1);

    private static Particle.DustOptions toBukkitDust(Colour colour) {
        return switch (colour) {
            case RED -> RED_DUST;
            case GREEN -> GREEN_DUST;
            case BLUE -> BLUE_DUST;
        };
    }
}
