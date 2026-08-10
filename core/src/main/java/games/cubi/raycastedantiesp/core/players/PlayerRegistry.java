/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.raycastedantiesp.core.tracked.NettyEntity;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {

    @FunctionalInterface
    public interface SelfEntityCreator {
        NettyEntity<?> createSelfEntity(PlayerData playerData, int selfEntityID, UUID playerUUID);
    }

    private static final PlayerRegistry instance = new PlayerRegistry();

    private PlayerRegistry() {}

    public static PlayerRegistry getInstance() {
        return instance;
    }

    private final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    /** Forcefully registers a player and returns the new PlayerData, even if they were already registered.**/
    public PlayerData registerAndGetPlayer(UUID playerUUID, int joinTick, int selfEntityID, SelfEntityCreator selfEntityCreator) {
        PlayerData newData = new PlayerData(playerUUID, false, joinTick, selfEntityID, selfEntityCreator);
        PlayerData old = playerDataMap.put(playerUUID, newData);
        if (old != null) old.markDisconnected();
        return newData;
    }

    public void unregisterPlayer(UUID playerUUID) {
        PlayerData unregisteredPlayer = playerDataMap.remove(playerUUID);
        if (unregisteredPlayer == null) {
            return;
        }
        unregisteredPlayer.markDisconnected();
    }

    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    public boolean isPlayerRegistered(UUID playerUUID) {
        return playerDataMap.containsKey(playerUUID);
    }

    /**
     * @return Live, mutable collection of all PlayerData instances.
     * **/
    public Collection<PlayerData> getAllPlayerData() {
        return playerDataMap.values();
    }
}
