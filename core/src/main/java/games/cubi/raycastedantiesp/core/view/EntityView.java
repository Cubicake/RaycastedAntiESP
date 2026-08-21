/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view;

import games.cubi.raycastedantiesp.core.tracked.TrackedEntity;
import games.cubi.locatables.api.Spatial;
import games.cubi.raycastedantiesp.core.tracked.NettyEntity;
import games.cubi.raycastedantiesp.core.utils.Clearable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public interface EntityView<T extends TrackedEntity<?>>  extends Clearable {
    void insertEntity(UUID world, T entity);

    void removeEntity(int entityID, int currentTick);

    void removeEntity(int entityID);

    void removeEntity(UUID entityUUID, int currentTick);

    T getEntity(UUID entityUUID);

    T getEntity(int entityID);

    Spatial getPosition(UUID entityUUID);

    int getEntityID(UUID entityUUID);

    boolean exists(UUID entityUUID);

    boolean exists(int entityID);

    /**
     * Returns a weakly consistent count of the entities currently tracked by this view.
     * This operation is safe to call from threads other than the structural writer.
     */
    int size();

    @Deprecated
    boolean isVisible(UUID entityUUID, int currentTick);

    boolean isVisible(UUID entityUUID);

    boolean isVisible(int entityID);

    void setVisibility(NettyEntity<?> entity, boolean visible, int currentTick, int expectedWorldEpoch);

    /**
     * Applies visibility from the Netty structural writer without publishing an engine transition.
     *
     * @return whether the entity and epoch were current and the visibility was applied
     */
    boolean recordDirectVisibility(NettyEntity<?> entity, boolean visible, int currentTick, int expectedWorldEpoch);

    Collection<UUID> getKnownEntities();

    int[] getKnownEntityIDs();

    /**
     * Iterates currently tracked entities that should be visibility-checked.
     *
     * @return number of entities passed to {@code action}.
     */
    int forEachNeedingRecheck(int visibleRecheckTicks, int currentTick, Consumer<UUID> action);

    /**
     * Iterates currently tracked entities that should be visibility-checked.
     *
     * @return number of entities passed to {@code action}, or 0 if {@code countingActuallyNeeded} is false.
     */
    int forEachNeedingRecheckEntity(int visibleRecheckTicks, int currentTick, boolean countingActuallyNeeded, int expectedWorldEpoch, Consumer<NettyEntity<?>> action);

    boolean hasPendingTransitions();

    /** Publishes the partial transition batch owned by the current engine producer. */
    void flushPendingTransitions();

    @FunctionalInterface
    interface TransitionConsumer {
        void accept(EntityViewTransition.Type type, TrackedEntity<?> entity, int worldEpoch);
    }

    void drainTransitions(TransitionConsumer consumer);

    boolean isPlayerView();

    default <T> T cast() {
        return (T) this;
    }

    String getStringDataForDebugging();

    interface Factory {
        EntityView<?> createEntityView(IntSupplier worldEpochSupplier);
    }
}
