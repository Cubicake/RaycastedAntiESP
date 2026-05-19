package games.cubi.raycastedantiesp.packetevents.view;

import games.cubi.locatables.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.EntityViewTransition;
import games.cubi.raycastedantiesp.packetevents.locatables.PacketEventsEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketEventsEntityView implements EntityView<PacketEventsEntity> {
    private final Map<UUID, PacketEventsEntity> entitiesByUUID = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityUUIDsByID = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<EntityViewTransition> transitions = new ConcurrentLinkedQueue<>();
    private final boolean isPlayerView;

    public PacketEventsEntityView(boolean isPlayerView) {
        this.isPlayerView = isPlayerView;
    }

    public static PacketEventsEntityView createPlayerView() {
        return new PacketEventsEntityView(true);
    }

    public static PacketEventsEntityView createEntityView() {
        return new PacketEventsEntityView(false);
    }

    @Override
    public void insertEntity(PacketEventsEntity entity) {
        if (entity == null || entity.entityUUID() == null) {
            Logger.error(new RuntimeException("Attempted to insert null entity or entity with null UUID into EntityView"), 2, PacketEventsEntityView.class);
            return;
        }
        entitiesByUUID.put(entity.entityUUID(), entity);
        entityUUIDsByID.put(entity.entityID(), entity.entityUUID());
    }

    @Override
    public void removeEntity(int entityID, int currentTick) {
        UUID entityUUID = entityUUIDsByID.remove(entityID);
        if (entityUUID == null) {
            return;
        }
        PacketEventsEntity removed = entitiesByUUID.remove(entityUUID);
        if (removed == null) {
            return;
        }
        removed.clear();
    }

    @Override
    public void removeEntity(UUID entityUUID, int currentTick) {
        int entityID = getEntityID(entityUUID);

        removeEntity(entityID, currentTick);
    }

    @Override
    public PacketEventsEntity getEntity(UUID entityUUID) {
        return entitiesByUUID.get(entityUUID);
    }

    @Override
    public PacketEventsEntity getEntity(int entityID) {
        return getTrackedEntity(entityID);
    }

    @Override
    public boolean exists(UUID entityUUID) {
        return entitiesByUUID.containsKey(entityUUID);
    }

    @Override
    public boolean exists(int entityID) {
        return entityUUIDsByID.containsKey(entityID);
    }

    @Override
    public boolean isVisible(int entityID) {
        PacketEventsEntity entity = getTrackedEntity(entityID);
        assert entity != null;
        return entity.visible();
    }

    @Override
    public Locatable getLocation(UUID entityUUID) {
        PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
        if (entity == null) {
            return null;
        }
        return entity.clonePlainAndCentreIfBlockLocation().set(entity.x(), entity.y() + 0.5, entity.z(), entity.world());
    }

    @Override
    public int getEntityID(UUID entityUUID) {
        PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
        return entity == null ? -1 : entity.entityID();
    }

    @Override
    public boolean isVisible(UUID entityUUID, int currentTick) {
        return isVisible(entityUUID);
    }

    @Override
    public boolean isVisible(UUID entityUUID) {
        PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
        return entity == null || entity.visible();
    }

    @Override
    public void setVisibility(UUID entityUUID, boolean visible, int currentTick) {
        PacketEventsEntity existing = entitiesByUUID.get(entityUUID);
        if (existing == null) {
            Logger.debug("EntityView.setVisibility missing uuid=" + entityUUID
                    + " requestedVisible=" + visible
                    + " tick=" + currentTick);
            return;
        }
        if (existing.isSelfEntity()) return;
        if (existing.visible() != visible) {
            transitions.add(new EntityViewTransition(
                    visible ? EntityViewTransition.Type.SHOW : EntityViewTransition.Type.HIDE,
                    existing.entityUUID(),
                    existing.entityID()
            ));
        }
        existing.setVisible(visible);
        existing.setLastChecked(currentTick);
    }

    @Override
    public Collection<UUID> getKnownEntities() {
        return List.copyOf(entitiesByUUID.keySet());
    }

    @Override
    public Collection<UUID> getNeedingRecheck(int recheckTicks, int currentTick) {
        List<UUID> needingRecheck = new ArrayList<>();
        for (PacketEventsEntity state : entitiesByUUID.values()) {
            if (state.visible() && (currentTick - state.lastChecked()) < recheckTicks) {
                continue;
            }
            needingRecheck.add(state.entityUUID());
        }
        return needingRecheck;
    }

    @Override
    public boolean hasPendingTransitions() {
        return !transitions.isEmpty();
    }

    @Override
    public List<EntityViewTransition> drainTransitions() {
        List<EntityViewTransition> drained = new ArrayList<>();
        EntityViewTransition transition;
        while ((transition = transitions.poll()) != null) {
            drained.add(transition);
        }
        return drained;
    }

    @Override
    public boolean isPlayerView() {
        return isPlayerView;
    }

    @Override
    public void clear() {
        entitiesByUUID.clear();
        entityUUIDsByID.clear();
        transitions.clear();
    }

    private PacketEventsEntity getTrackedEntity(int entityID) {
        UUID entityUUID = entityUUIDsByID.get(entityID);
        return entityUUID == null ? null : entitiesByUUID.get(entityUUID);
    }

    public String getStringDataForDebugging() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityView isPlayerView=").append(isPlayerView).append("\n");
        Set<Map.Entry<Integer, UUID>> entries = new HashSet<>(entityUUIDsByID.entrySet());
        for (Map.Entry<Integer, UUID> entry : entries) {
            PacketEventsEntity entity = entitiesByUUID.get(entry.getValue());
            builder.append("EntityID=").append(entry.getKey())
                    .append(" UUID=").append(entry.getValue())
                    .append(" Entity=").append(entity.toString())
                    .append("\n");
        }
        return builder.toString();
    }
}
