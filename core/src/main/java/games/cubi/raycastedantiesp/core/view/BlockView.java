package games.cubi.raycastedantiesp.core.view;

import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.raycastedantiesp.core.locatables.TileEntityLocatable;
import games.cubi.raycastedantiesp.core.utils.Clearable;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BlockView extends Clearable {
    boolean isBlockOccluding(BlockLocatable location);

    void insertTileEntityIfAbsent(BlockLocatable location, int blockID, boolean visible);

    void insertTileEntity(BlockLocatable location, int blockID, boolean visible);

    void removeTileEntity(BlockLocatable location);

    TileEntityLocatable<?> getTrackedTileEntity(BlockLocatable location);

    TileEntityLocatable<?> getTrackedTileEntity(ImmutableBlockLocatable location);

    boolean isVisible(BlockLocatable location, int currentTick);

    void setVisibility(BlockLocatable location, boolean visible, int currentTick);

    Collection<BlockLocatable> getKnownTileEntities();

    Collection<BlockLocatable> getNeedingRecheck(int recheckTicks, int currentTick);

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
