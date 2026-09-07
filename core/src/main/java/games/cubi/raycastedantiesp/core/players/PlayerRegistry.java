/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.players;

import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.utils.VarHandler;

import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerRegistry {

    @FunctionalInterface
    public interface SelfEntityCreator {
        NettyEntity<?> createSelfEntity(PlayerData playerData, int selfEntityID, UUID playerUUID);
    }

    private static final PlayerRegistry instance = new PlayerRegistry();

    private PlayerRegistry() {}

    public static PlayerRegistry get() {
        return instance;
    }

    private final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Collection<PlayerData> unmodifiablePlayerData = Collections.unmodifiableCollection(playerDataMap.values());
    private volatile PlayerData[] playerDataArray = new PlayerData[0]; private static final VarHandle PLAYER_DATA_ARRAY = VarHandler.get(PlayerRegistry.class, "playerDataArray", PlayerData[].class);

    /** Forcefully registers a player and returns the new PlayerData, even if they were already registered.**/
    public synchronized PlayerData registerAndGetPlayer(UUID playerUUID, int joinTick, int selfEntityID, SelfEntityCreator selfEntityCreator) {
        PlayerData newData = new PlayerData(playerUUID, false, joinTick, selfEntityID, selfEntityCreator);
        PlayerData old = playerDataMap.put(playerUUID, newData);
        if (old != null) old.markDisconnected();
        updatePlayerDataArray();
        return newData;
    }

    public synchronized void unregisterPlayer(UUID playerUUID) {
        PlayerData unregisteredPlayer = playerDataMap.remove(playerUUID);
        if (unregisteredPlayer == null) {
            return;
        }
        unregisteredPlayer.markDisconnected();
        updatePlayerDataArray();
    }

    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    public boolean isPlayerRegistered(UUID playerUUID) {
        return playerDataMap.containsKey(playerUUID);
    }

    /**
     * @return Live, unmodifiable view of all PlayerData instances.
     * **/
    public Collection<PlayerData> getAllPlayerData() {
        return unmodifiablePlayerData;
    }

    private void updatePlayerDataArray() {
        PLAYER_DATA_ARRAY.setRelease(this, playerDataMap.values().toArray(PlayerData[]::new));
    }

    /**
     * Do not mutate this array, all changes must go through {@link #registerAndGetPlayer(UUID, int, int, SelfEntityCreator)} /
     * {@link #unregisterPlayer(UUID)}.
     * <p></p>
     * Changes to this array will be lost on next player join/leave, and will not apply everywhere.
     */
    public PlayerData[] getPlayerDataArray() {
        return (PlayerData[]) PLAYER_DATA_ARRAY.getAcquire(this);
    }

    public void forEachPlayer(Consumer<PlayerData> consumer) {
        PlayerData[] data = getPlayerDataArray();
        for (PlayerData player : data) {
            consumer.accept(player);
        }
    }

    public int getPlayerCount() {
        return playerDataMap.size();
    }
}
