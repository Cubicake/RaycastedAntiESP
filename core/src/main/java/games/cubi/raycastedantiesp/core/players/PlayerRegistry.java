package games.cubi.raycastedantiesp.core.players;

import games.cubi.locatables.Locatable;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {

    @FunctionalInterface
    public interface SelfEntityCreator {
        NettyEntityLocatable<?, ?> createSelfEntity(PlayerData playerData, int selfEntityID, UUID playerUUID);
    }

    private static PlayerRegistry instance;

    private PlayerRegistry() {}

    public static PlayerRegistry getInstance() {
        if (instance == null) {
            instance = new PlayerRegistry();
        }
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
