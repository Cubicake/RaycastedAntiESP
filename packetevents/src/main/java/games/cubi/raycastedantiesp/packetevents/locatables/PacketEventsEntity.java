package games.cubi.raycastedantiesp.packetevents.locatables;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsEntityReplayData;

import java.util.UUID;

public class PacketEventsEntity extends NettyEntity<PacketEventsEntityReplayData> {
    public PacketEventsEntity(PlayerData owningPlayer, double x, double y, double z, int entityID, UUID entityUUID, boolean isSelfEntity, int entityType, boolean visible) {
        super(owningPlayer, x, y, z, entityID, entityUUID, isSelfEntity, entityType, visible);
    }

    private PacketEventsEntity(PlayerData selfData, int selfEntityID, UUID selfEntityUUID) {
        super(selfData, selfEntityID, selfEntityUUID);
    }

    public static PacketEventsEntity createSelfEntity(PlayerData selfData, int selfEntityID, UUID selfEntityUUID) {
        return new PacketEventsEntity(selfData, selfEntityID, selfEntityUUID);
    }
}
