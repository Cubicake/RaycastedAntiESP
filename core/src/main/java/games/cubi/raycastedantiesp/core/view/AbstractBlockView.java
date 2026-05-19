package games.cubi.raycastedantiesp.core.view;

import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.ChunkSectionLocatable;
import games.cubi.locatables.Locatable;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.raycastedantiesp.core.locatables.TileEntityLocatable;
import games.cubi.raycastedantiesp.core.utils.CanonicalSet;
import games.cubi.raycastedantiesp.core.utils.ConcurrentSelfMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractBlockView<T extends TileEntityLocatable<?>> implements BlockView {
    public static final int CHUNK_SIZE = 16;
    public static final int LOCAL_MASK = CHUNK_SIZE - 1;

    private final Map<ChunkSectionLocatable, BitSet /*true if that position is occluding. Positions are 0-15 for x,y,z*/> chunks = new ConcurrentHashMap<>();
    private final CanonicalSet<Locatable, T> knownTileEntities = new ConcurrentSelfMap<>();
    private final ConcurrentLinkedQueue<BlockViewTransition> transitions = new ConcurrentLinkedQueue<>();

    @Deprecated
    protected abstract T createTrackedTileEntity(BlockLocatable location, int blockID, boolean visible);

    protected abstract T createTrackedTileEntity(UUID world, int x, int y, int z, int blockID);


    public boolean isBlockOccluding(UUID world, int x, int y, int z) {
        final BitSet chunk = getChunk(world, x >> 4, y >> 4, z >> 4);
        if (chunk == null) {
            // chunks may be empty if there are no occluding blocks in that chunk
            return false;
        }

        return chunk.get(maskAndPack(x, y, z));
    }

    @Override
    public boolean isBlockOccluding(BlockLocatable location) {
        return location != null
                && location.world() != null
                && isBlockOccluding(location.world(), location.blockX(), location.blockY(), location.blockZ());
    }

    public int loadedChunkCount() {
        return chunks.size();
    }

    @Override
    public void insertTileEntityIfAbsent(BlockLocatable location, int blockID, boolean visible) {
        knownTileEntities.computeIfAbsent(location, ignored -> createTrackedTileEntity(location, blockID, visible));
    }

    @Override
    public void insertTileEntity(BlockLocatable location, int blockID, boolean visible) {
        knownTileEntities.remove(location);
        knownTileEntities.add(createTrackedTileEntity(location, blockID, visible));
    }

    @Override
    public void removeTileEntity(BlockLocatable location) {
        T tileEntity = knownTileEntities.remove(location);
        if (tileEntity != null) {
            tileEntity.clear();
        }
    }

    @Override
    public T getTrackedTileEntity(BlockLocatable location) {
        return knownTileEntities.get(location);
    }

    @Override
    public T getTrackedTileEntity(ImmutableBlockLocatable location) {
        return knownTileEntities.get(location);
    }

    @Override
    public boolean isVisible(BlockLocatable location, int currentTick) {
        T state = knownTileEntities.get(location);
        return state == null || state.visible();
    }

    @Override
    public void setVisibility(BlockLocatable location, boolean visible, int currentTick) {
        T existing = knownTileEntities.get(location);
        if (existing == null) {
            return;
        }
        if (existing.visible() != visible) {
            transitions.add(new BlockViewTransition(
                    visible ? BlockViewTransition.Type.SHOW : BlockViewTransition.Type.HIDE,
                    location
            ));
        }
        existing.setVisible(visible);
        existing.setLastChecked(currentTick);
    }

    @Override
    public Collection<BlockLocatable> getKnownTileEntities() {
        return List.copyOf(knownTileEntities.keySet());
    }

    @Override
    public Collection<BlockLocatable> getNeedingRecheck(int recheckTicks, int currentTick) {
        List<BlockLocatable> needingRecheck = new ArrayList<>();
        for (T tileEntity : knownTileEntities.values()) {
            if (tileEntity.visible() && currentTick - tileEntity.lastChecked() < recheckTicks) {
                continue;
            }
            needingRecheck.add(tileEntity);
        }
        return needingRecheck;
    }

    @Override
    public boolean hasPendingTransitions() {
        return !transitions.isEmpty();
    }

    @Override
    public List<BlockViewTransition> drainTransitions() {
        List<BlockViewTransition> drained = new ArrayList<>();
        BlockViewTransition transition;
        while ((transition = transitions.poll()) != null) {
            drained.add(transition);
        }
        return drained;
    }

    @Override
    public void upsertBlock(UUID world, int x, int y, int z, boolean occluding) {
        final BitSet chunk = getOrCreateChunk(world, x >> 4, y >> 4, z >> 4);
        chunk.set(maskAndPack(x, y, z), occluding);
    }

    @Override
    public void removeChunk(UUID world, int chunkX, int chunkZ) {
        chunks.entrySet().removeIf(entry ->
                entry.getKey().world().equals(world)
                        && entry.getKey().chunkX() == chunkX
                        && entry.getKey().chunkZ() == chunkZ
        );
    }

    @Override
    public void removeChunkSection(UUID world, int chunkX, int chunkY, int chunkZ) {
        chunks.remove(new ChunkSectionLocatable.ImmutableChunkSectionLocatable(world, chunkX, chunkY, chunkZ));
    }

    @Override
    public void replaceChunkSection(UUID world, int chunkX, int chunkY, int chunkZ, BitSet occludingBlocks) {
        chunks.put(new ChunkSectionLocatable.ImmutableChunkSectionLocatable(world, chunkX, chunkY, chunkZ), occludingBlocks
        );
    }

    @Override
    public void clear() {
        chunks.clear();
        knownTileEntities.clear();
        transitions.clear();
    }

    public static int maskAndPack(int x, int y, int z) {
        return pack(x & LOCAL_MASK, y & LOCAL_MASK, z & LOCAL_MASK);
    }

    public static int pack(int x, int y, int z) {
        return (x & LOCAL_MASK) | ((y & LOCAL_MASK) << 4) | ((z & LOCAL_MASK) << 8);
    }

    public static int unpackX(int packed) {
        return packed & LOCAL_MASK;
    }

    public static int unpackY(int packed) {
        return (packed >> 4) & LOCAL_MASK;
    }

    public static int unpackZ(int packed) {
        return (packed >> 8) & LOCAL_MASK;
    }

    private BitSet getChunk(UUID world, int chunkX, int chunkY, int chunkZ) {
        return chunks.get(new ChunkSectionLocatable.ImmutableChunkSectionLocatable(world, chunkX, chunkY, chunkZ));
    }

    private BitSet getOrCreateChunk(UUID world, int chunkX, int chunkY, int chunkZ) {
        return chunks.computeIfAbsent(
                new ChunkSectionLocatable.ImmutableChunkSectionLocatable(world, chunkX, chunkY, chunkZ),
                ignored -> new BitSet(CHUNK_SIZE*CHUNK_SIZE*CHUNK_SIZE)
        );
    }
}
