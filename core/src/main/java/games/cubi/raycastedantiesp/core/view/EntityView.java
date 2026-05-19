package games.cubi.raycastedantiesp.core.view;

import games.cubi.raycastedantiesp.core.locatables.EntityLocatable;
import games.cubi.locatables.Locatable;
import games.cubi.raycastedantiesp.core.utils.Clearable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EntityView<T extends EntityLocatable<?, ?>>  extends Clearable {
    void insertEntity(T entity);

    void removeEntity(int entityID, int currentTick);

    void removeEntity(UUID entityUUID, int currentTick);

    T getEntity(UUID entityUUID);

    T getEntity(int entityID);

    Locatable getLocation(UUID entityUUID);

    int getEntityID(UUID entityUUID);

    boolean exists(UUID entityUUID);

    boolean exists(int entityID);

    @Deprecated
    boolean isVisible(UUID entityUUID, int currentTick);

    boolean isVisible(UUID entityUUID);

    boolean isVisible(int entityID);

    void setVisibility(UUID entityUUID, boolean visible, int currentTick);

    Collection<UUID> getKnownEntities();

    Collection<UUID> getNeedingRecheck(int recheckTicks, int currentTick);

    boolean hasPendingTransitions();

    List<EntityViewTransition> drainTransitions();

    boolean isPlayerView();

    default <T> T cast() {
        return (T) this;
    }

    String getStringDataForDebugging();

    interface Factory {
        EntityView<?> createEntityView();
    }
}
