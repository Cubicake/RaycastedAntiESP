package games.cubi.raycastedantiesp.packetevents.locatables;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsEntityReplayData;

import java.util.UUID;

public class PacketEventsEntity extends NettyEntityLocatable<EntityType, PacketEventsEntityReplayData> {
    public PacketEventsEntity(UUID world, double x, double y, double z, int entityID, UUID entityUUID, boolean isSelfEntity, EntityType entityType, boolean visible) {
        super(world, x, y, z, entityID, entityUUID, isSelfEntity, entityType, visible);
    }

    private PacketEventsEntity(int selfEntityID, UUID selfEntityUUID) {
        super(selfEntityID, selfEntityUUID);
    }

    public static PacketEventsEntity createSelfEntity(int selfEntityID, UUID selfEntityUUID) {
        return new PacketEventsEntity(selfEntityID, selfEntityUUID);
    }

    @Override
    public boolean strictlyEquals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PacketEventsEntity that)) return false;
        if (!this.equals(other)) return false;

        if (entityID() != that.entityID()) return false;
        if (!entityUUID().equals(that.entityUUID())) return false;
        if (isSelfEntity() != that.isSelfEntity()) return false;
        if (entityType() != that.entityType()) return false;
        if (visible() != that.visible()) return false;
        if (yaw() != that.yaw()) return false;
        if (pitch() != that.pitch()) return false;
        if (headYaw() != that.headYaw()) return false;
        if (velocityX() != that.velocityX()) return false;
        if (velocityY() != that.velocityY()) return false;
        if (velocityZ() != that.velocityZ()) return false;
        if (onGround() != that.onGround()) return false;
        return entityData() == that.entityData();
    }
}
