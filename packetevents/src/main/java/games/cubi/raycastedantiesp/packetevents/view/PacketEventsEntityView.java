package games.cubi.raycastedantiesp.packetevents.view;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;
import ca.spottedleaf.concurrentutil.map.SWMRHashTable;
import games.cubi.locatables.Locatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.locatables.NettyEntityLocatable;
import games.cubi.raycastedantiesp.core.utils.SingleThreadedGuard;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.core.view.EntityViewTransition;
import games.cubi.raycastedantiesp.packetevents.locatables.PacketEventsEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class PacketEventsEntityView extends SingleThreadedGuard implements EntityView<PacketEventsEntity> {
    private final SWMRHashTable<UUID, PacketEventsEntity> entitiesByUUID = new SWMRHashTable<>();
    private final Int2ObjectOpenHashMap<UUID> entityUUIDsByID = new Int2ObjectOpenHashMap<>();
    private final MultiThreadedQueue<EntityViewTransition> transitions = new MultiThreadedQueue<>();
    private final boolean isPlayerView;

    public PacketEventsEntityView(boolean isPlayerView) {
        super(Thread.currentThread()); // Should be player's netty thread
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

        UUID entityUUID = entity.entityUUID();
        int entityID = entity.entityID();
        guardThread();
        UUID previousUUIDForID = entityUUIDsByID.put(entityID, entityUUID);
        if (previousUUIDForID != null && !previousUUIDForID.equals(entityUUID)) {
            PacketEventsEntity previousEntityForID = entitiesByUUID.get(previousUUIDForID);
            if (previousEntityForID != null && previousEntityForID.entityID() == entityID && entitiesByUUID.remove(previousUUIDForID, previousEntityForID)) {
                previousEntityForID.clear();
            }
        }

        PacketEventsEntity previousEntityForUUID = entitiesByUUID.put(entityUUID, entity);
        if (previousEntityForUUID != null && previousEntityForUUID.entityID() != entityID) {
            entityUUIDsByID.remove(previousEntityForUUID.entityID(), entityUUID);
        }
        if (previousEntityForUUID != null && previousEntityForUUID != entity) {
            previousEntityForUUID.clear();
        }
    }

    @Override
    public void removeEntity(int entityID, int currentTick) {
        removeEntity(entityID);
    }

    @Override
    public void removeEntity(int entityID) {
        guardThread();
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
        guardThread();
        return entityUUIDsByID.containsKey(entityID);
    }

    @Override
    public boolean isVisible(int entityID) {
        PacketEventsEntity entity = Logger.requireNonNull(getTrackedEntity(entityID), "Entity with ID " + entityID + " does not exist in EntityView", 3, PacketEventsEntityView.class);
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
        PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
        if (entity == null) {
            Logger.debug("EntityView.setVisibility missing uuid=" + entityUUID
                    + " requestedVisible=" + visible
                    + " tick=" + currentTick);
            return;
        }
        if (entity.isSelfEntity()) return;
        setVisibility(entity, visible, currentTick);
    }

    @Override
    public void setVisibility(@NotNull NettyEntityLocatable<?,?> entity, boolean visible, int currentTick) {
        if (entity.visible() != visible) {
            transitions.add(new EntityViewTransition(
                    visible ? EntityViewTransition.Type.SHOW : EntityViewTransition.Type.HIDE,
                    entity.entityUUID(),
                    entity.entityID()
            ));
        }
        entity.setVisible(visible);
        entity.setLastChecked(currentTick);
    }

    @Override
    public Collection<UUID> getKnownEntities() {
        List<UUID> known = new ArrayList<>(entitiesByUUID.size());
        entitiesByUUID.forEachKey(known::add);
        return known;
    }

    @Override
    public int[] getKnownEntityIDs() {
        /*
        guardThread();
        // This is only called while clearing on the Netty thread, so size()
        // is stable and can be used as the exact output array length.
        int[] entityIDs = new int[entityUUIDsByID.size()];
        int[] count = new int[1];
        entityUUIDsByID.forEachKey(entityID -> {
            entityIDs[count[0]++] = entityID;
        });*/
        guardThread();
        return entityUUIDsByID.keySet().toIntArray();
        //return entityIDs;
    }

    @Override
    public int forEachNeedingRecheck(int recheckTicks, int currentTick, Consumer<UUID> action) {
        int processed = 0;
        for (PacketEventsEntity state : entitiesByUUID.values()) {
            if (state.visible() && (recheckTicks < 0 || currentTick - state.lastChecked() < recheckTicks)) {
                continue;
            }
            action.accept(state.entityUUID());
            processed++;
        }
        return processed;
    }

    @Override
    public int forEachNeedingRecheckEntity(int recheckTicks, int currentTick, boolean countingActuallyNeeded, Consumer<NettyEntityLocatable<?,?>> action) {
        if (countingActuallyNeeded) {
            return entitiesByUUID.forEachValueCounted( (entity) -> {
                if (entity.visible() && (recheckTicks < 0 || currentTick - entity.lastChecked() < recheckTicks)) {
                    return false;
                }
                action.accept(entity);
                return true;
            });
        }
        entitiesByUUID.forEachValue( (entity) -> {
            if (entity.visible() && (recheckTicks < 0 || currentTick - entity.lastChecked() < recheckTicks)) {
                return;
            }
            action.accept(entity);
        });
        return 0;
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
        guardThread();
        entitiesByUUID.clear();
        entityUUIDsByID.clear();
        transitions.clear();
    }

    private PacketEventsEntity getTrackedEntity(int entityID) {
        guardThread();
        UUID entityUUID = entityUUIDsByID.get(entityID);
        return entityUUID == null ? null : entitiesByUUID.get(entityUUID);
    }

    public String getStringDataForDebugging() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityView isPlayerView=").append(isPlayerView).append("\n");
        guardThread();
        entityUUIDsByID.forEach((entityID, entityUUID) -> {
            PacketEventsEntity entity = entitiesByUUID.get(entityUUID);
            builder.append("EntityID=").append(entityID)
                    .append(" UUID=").append(entityUUID)
                    .append(" Entity=").append(entity)
                    .append("\n");
        });
        return builder.toString();
    }
}
