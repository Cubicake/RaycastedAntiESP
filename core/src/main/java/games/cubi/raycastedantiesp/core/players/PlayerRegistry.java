package games.cubi.raycastedantiesp.core.players;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {

    private static PlayerRegistry instance;

    private PlayerRegistry() {}

    public static PlayerRegistry getInstance() {
        if (instance == null) {
            instance = new PlayerRegistry();
        }
        return instance;
    }

    private final ConcurrentHashMap<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public void registerPlayerIfAbsent(UUID playerUUID, boolean hasBypassPermission, int joinTick) {
        playerDataMap.putIfAbsent(playerUUID, new PlayerData(playerUUID, hasBypassPermission, joinTick));
    }

    /** Forcefully registers a player and returns the new PlayerData, even if they were already registered.**/
    public PlayerData registerAndGetPlayer(UUID playerUUID, int joinTick) {
        PlayerData newData = new PlayerData(playerUUID, joinTick);
        playerDataMap.put(playerUUID, newData);
        return newData;
    }

    public PlayerData registerAndGetPlayerIfAbsent(UUID playerUUID, boolean hasBypassPermission, int joinTick) {
        PlayerData newData = new PlayerData(playerUUID, hasBypassPermission, joinTick);
        PlayerData existingData = playerDataMap.putIfAbsent(playerUUID, newData);
        return existingData != null ? existingData : newData;
    }

    public void unregisterPlayer(UUID playerUUID) {
        PlayerData unregisteredPlayer = playerDataMap.remove(playerUUID);
        if (unregisteredPlayer == null) {
            return;
        }
        unregisteredPlayer.blockView().clear();
        unregisteredPlayer.entityView().clear();
        unregisteredPlayer.playerView().clear();
        unregisteredPlayer.nettyData().clear();
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
