/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.view;

import games.cubi.locatables.api.BlockLocatable;
import games.cubi.locatables.api.BlockSpatial;
import games.cubi.locatables.api.SpatialChunkSection;
import games.cubi.raycastedantiesp.core.chunks.ChunkOcclusionView;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.core.chunks.BlockChunkData;
import games.cubi.raycastedantiesp.core.chunks.OccludingChunkData;
import games.cubi.raycastedantiesp.core.utils.Clearable;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * A per-player block view with one structural writer and concurrent weakly-consistent readers.
 *
 * <p>Packet-driven structural mutations, including chunk and tile membership changes, must all be performed by the
 * same outbound Netty thread. At most one engine worker concurrently traverses a player's view and commits visibility
 * decisions; visibility fields and the transition queue are safe for that Netty/engine overlap.</p>
 */
public interface BlockView extends Clearable {
    /**
     * Parameters are chunk coordinates
     */
    ChunkOcclusionView getChunkOcclusionView(int chunkX, int chunkY, int chunkZ);

    default ChunkOcclusionView getChunkOcclusionView(SpatialChunkSection chunkSection) {
        return getChunkOcclusionView(chunkSection.chunkX(), chunkSection.chunkY(), chunkSection.chunkZ());
    }

    boolean isBlockOccluding(BlockLocatable location);

    boolean isBlockOccluding(int x, int y, int z);

    /**
     * Constructs a new tile entity with the provided id and visibility, or updates the block id for an existing tile
     * entity. This is a structural-writer operation.
     *
     * @return the current tracked tile entity, or null when the location cannot be tracked.
     */
    TrackedTileEntity<?> updateOrInsertTileEntity(UUID world, BlockSpatial position, char blockID, boolean visibleIfNew);

    /** Structural-writer operation. */
    void removeTileEntity(UUID world, BlockSpatial position);

    TrackedTileEntity<?> getTrackedTileEntity(UUID world, BlockSpatial position);

    boolean isVisible(UUID world, BlockSpatial position, int currentTick);

    /**
     * Records visibility established by the current outbound packet without queuing another packet. This also resets
     * the check timestamp because the transmitted state supersedes the result of any previous visibility check. The
     * tile entity must be the current state returned by this view's structural-writer operations.
     */
    void recordOutboundTileEntityVisibility(TrackedTileEntity<?> tileEntity, boolean visible);

    /**
     * Applies a mode change from the structural writer.
     *
     * Invokes {@code visibilityRepairConsumer} for each tile entity made visible when checks are disabled. The
     * structural writer must send their current state directly rather than publishing them to the engine transition
     * queue.
     */
    void applyTileEntityCheckMode(boolean enabled, int currentTick, Consumer<TrackedTileEntity<?>> visibilityRepairConsumer);

    /** Returns an opaque enabled-state/generation snapshot for rejecting results that cross a mode change. */
    long tileEntityCheckModeToken();

    /** Returns whether {@code modeToken} is still the current enabled generation. */
    boolean isCurrentEnabledTileEntityMode(long modeToken);

    Collection<TrackedTileEntity<?>> getKnownTileEntities();

    /**
     * Iterates currently tracked tile entities that should be visibility-checked.
     *
     * @return number of tile entities passed to {@code action}.
     */
    int forEachNeedingRecheck(int recheckTicks, int currentTick, Consumer<TrackedTileEntity<?>> action);

    @FunctionalInterface
    interface VisibilityResolver {
        byte SKIPPED = 78;
        byte HIDE = -23;
        byte SHOW = 42;

        byte setVisible(TrackedTileEntity<?> tileEntity);
    }

    /**
     * Iterates currently tracked tile entities that should be visibility-checked.
     *
     * @return number of tile entities passed to {@code action}.
     */
    int updateVisibilityForEachNeedingRecheck(int recheckTicks, int currentTick, long modeToken, int expectedWorldEpoch, VisibilityResolver action);

    boolean hasPendingTransitions();

    /** Publishes the partial transition batch owned by the current engine producer. */
    void flushPendingTransitions();

    @FunctionalInterface
    interface TransitionConsumer {
        void accept(BlockViewTransition.Type type, TrackedTileEntity<?> tileEntity, long modeToken, int worldEpoch);
    }

    void drainTransitions(TransitionConsumer consumer);

    /** Structural-writer operation. */
    void upsertBlock(UUID world, int x, int y, int z, int blockID);

    /** Structural-writer operation. */
    void removeChunk(UUID world, int chunkX, int chunkZ);

    /** Structural-writer operation. */
    void removeChunkSection(UUID world, int chunkX, int chunkY, int chunkZ);

    /**
     * Removes tracked tile entities absent from an authoritative chunk column. A null {@code presentBySection} means
     * the entire column has no managed tiles; a null entry means that section has none. This is a structural-writer
     * operation.
     */
    void pruneTileEntitiesAbsentFromChunkSections(UUID world, int chunkX, int chunkZ, int minimumSectionY, int sectionCount, long[][] presentBySection);

    /** Structural-writer operation. */
    void replaceChunkSection(UUID world, int chunkX, int chunkY, int chunkZ, BlockChunkData data);

    /** Structural-writer operation. */
    void replaceChunkSectionOcclusion(UUID world, int chunkX, int chunkY, int chunkZ, OccludingChunkData data);

    default <T> T cast() {
        return (T) this;
    }

    interface Factory {
        BlockView createBlockView(IntSupplier worldEpochSupplier);
    }
}
