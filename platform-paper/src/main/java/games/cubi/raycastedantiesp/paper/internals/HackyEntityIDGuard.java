/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.internals;

import games.cubi.logs.Logger;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entity IDs -1 and -2 are used as sentinel values, however they can be created by the Minecraft server if the entity id counter overflows.
 * Minecraft uses IDs -1 and 0 as sentinel values, but does not guard against them existing naturally.
 * This class guards against that by jumping from ID -2000 to 1.
 * Yes, this is unhinged.
 */
public class HackyEntityIDGuard {
    private final AtomicInteger minecraftInternalEntityIDCounter;
    private int previous;
    private PollRate pollRate = PollRate.INFREQUENT;

    public HackyEntityIDGuard() {
        this.minecraftInternalEntityIDCounter = getEntityIncrementer();
    }

    public enum PollRate {
        NEVER(0),
        INFREQUENT(10_000), // for use before overflow
        SEMI_FREQUENT(2_000), // for use after overflow
        FREQUENT(100), // for use within 100,000 ids of re-entering positive IDs
        EXTREMELY_FREQUENT(1); // for use within 10,000 ids of re-entering positive IDs

        private final int intervalTicks;

        PollRate(int intervalTicks) {
            this.intervalTicks = intervalTicks;
        }

        public int intervalTicks() {
            return intervalTicks;
        }
    }

    public void tick(int currentTick) {
        if (pollRate == PollRate.NEVER) {
            return;
        }
        if (currentTick % pollRate.intervalTicks != 0) {
            return;
        }
        pollRate = guard();
    }

    // As long as no group of players can spawn 10k entities within 5 seconds, or 2k entities within 1 tick, this will work.
    public PollRate guard() {
        if (minecraftInternalEntityIDCounter == null) return PollRate.NEVER;
        int current = minecraftInternalEntityIDCounter.get();
        try {
            if (previous > current) {
                Logger.warning("Your server's entity ID counter has overflowed! Activating mitigations.", 1, HackyEntityIDGuard.class);
            }
            if (current > 0) return PollRate.INFREQUENT;
            if (current < -100_000) return PollRate.SEMI_FREQUENT;
            if (current < -10_000) return PollRate.FREQUENT;
            if (current < -2_000) return PollRate.EXTREMELY_FREQUENT;

            minecraftInternalEntityIDCounter.set(1);
            current = 1;
            return PollRate.INFREQUENT;
        } finally {
            previous = current;
        }
    }

    private AtomicInteger getEntityIncrementer() {
        try {
            Class<?> clazz = Class.forName("net.minecraft.server.level.ServerLevel");

            Field field = clazz.getDeclaredField("ENTITY_COUNTER");
            field.setAccessible(true);

            return (AtomicInteger) field.get(null);
        } catch (Exception e) {
            Logger.debug("NMS ServerLevel did not contain ENTITY_COUNTER (Expected for 26.1.2 and below):" + e);
        }
        try {
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");

            Field counterField = entityClass.getDeclaredField("ENTITY_COUNTER");

            counterField.setAccessible(true);

            return (AtomicInteger) counterField.get(null);
        } catch (Exception e) {
            Logger.error("Entity ID counter could not be found anywhere. If you are running RaycastedAntiESP on a version which has not yet been listed as supported, you need to wait for an update. If this occured on a supported version, please report it on Discord or Github! (discord.cubi.games)", 1, HackyEntityIDGuard.class);
            return null;
        }
    }
}
