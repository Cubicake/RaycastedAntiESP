package games.cubi.raycastedantiesp.core.view;

import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.raycastedantiesp.core.locatables.TileEntityLocatable;
import games.cubi.raycastedantiesp.core.utils.Clearable;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface BlockView extends Clearable {
    boolean isBlockOccluding(BlockLocatable location);

    /**
     * Constructs a new tile entity with the provided id and visibility, or updates the block id for existing tile entities.
     */
    void updateOrInsertTileEntity(BlockLocatable location, int blockID, boolean visibleIfNew);

    void removeTileEntity(BlockLocatable location);

    TileEntityLocatable<?> getTrackedTileEntity(BlockLocatable location);

    TileEntityLocatable<?> getTrackedTileEntity(ImmutableBlockLocatable location);

    boolean isVisible(BlockLocatable location, int currentTick);

    void setVisibility(BlockLocatable location, boolean visible, int currentTick);

    Collection<BlockLocatable> getKnownTileEntities();

    /**
     * Iterates currently tracked tile entities that should be visibility-checked.
     *
     * @return number of tile entities passed to {@code action}.
     */
    int forEachNeedingRecheck(int recheckTicks, int currentTick, Consumer<BlockLocatable> action);

    @FunctionalInterface
    interface VisibilityResolver {
        byte SKIPPED = 78;
        byte HIDE = -23;
        byte SHOW = 42;

        byte setVisible(BlockLocatable location);
    }

    /**
     * Iterates currently tracked tile entities that should be visibility-checked.
     *
     * @return number of tile entities passed to {@code action}.
     */
    int updateVisibilityForEachNeedingRecheck(int recheckTicks, int currentTick, VisibilityResolver action);

    boolean hasPendingTransitions();

    List<BlockViewTransition> drainTransitions();

    void upsertBlock(UUID world, int x, int y, int z, boolean occluding);

    void removeChunk(UUID world, int chunkX, int chunkZ);

    void removeChunkSection(UUID world, int chunkX, int chunkY, int chunkZ);

    void replaceChunkSection(UUID world, int chunkX, int chunkY, int chunkZ, BitSet occludingBlocks);

    default <T> T cast() {
        return (T) this;
    }

    interface Factory {
        BlockView createBlockView();
    }
}
