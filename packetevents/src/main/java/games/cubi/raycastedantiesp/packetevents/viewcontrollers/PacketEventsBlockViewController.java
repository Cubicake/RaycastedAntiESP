package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.Locatable;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.locatables.implementations.MutableBlockLocatable;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.BlockViewTransition;
import games.cubi.raycastedantiesp.core.locatables.TileEntityLocatable;
import games.cubi.raycastedantiesp.packetevents.BlockInfoResolver;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsTileEntityReplayData;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntSupplier;

import static games.cubi.raycastedantiesp.core.view.AbstractBlockView.CHUNK_SIZE;
import static games.cubi.raycastedantiesp.core.view.AbstractBlockView.pack;

public abstract class PacketEventsBlockViewController implements PacketListener {
    private final BlockInfoResolver blockInfoResolver;
    private final IntSupplier currentTickSupplier;
    private final PacketEventsCommonViewController common;
    private TileEntityConfig tileEntityConfig = null;
    private int hideOnSpawnDistanceSquared = 0;

    protected PacketEventsBlockViewController(BlockInfoResolver blockInfoResolver, IntSupplier currentTickSupplier) {
        this.blockInfoResolver = blockInfoResolver;
        this.currentTickSupplier = currentTickSupplier;
        common = PacketEventsCommonViewController.get(currentTickSupplier);
    }

    protected abstract int getHiddenBlockId(int blockY);

    public void removeViewer(UUID viewerUUID) {
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        UUID viewerUUID = event.getUser().getUUID();
        if (viewerUUID == null) {
            return;
        }

        if (!(ConfigManager.get().getTileEntityConfig() == tileEntityConfig)) {
            tileEntityConfig = ConfigManager.get().getTileEntityConfig();
            hideOnSpawnDistanceSquared = tileEntityConfig.hideOnSpawnDistance() * tileEntityConfig.hideOnSpawnDistance();
        }

        PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(viewerUUID);
        if (playerData == null) {
            return;
        }

        UUID world = common.resolvePacketWorld(playerData, event.getUser());
        int currentTick = currentTickSupplier.getAsInt();

        handleBlockPackets(event, event.getUser(), playerData, world, currentTick);

        if (playerData.blockView().hasPendingTransitions()) {
            processTileEntityTransitions(event.getUser(), playerData.blockView());
        }
    }

    private void handleBlockPackets(PacketSendEvent event, User viewer, PlayerData playerData, UUID world, int currentTick) {
        if (world == null) {
            return;
        }

        BlockView blockView = playerData.blockView();

        if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrapperPlayServerUnloadChunk packet = new WrapperPlayServerUnloadChunk(event);
            removeChunk(packet, blockView, world);
        } else if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            handleSingleBlockChange(event, viewer, playerData, world, packet, currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
            handleMultiBlockChange(event, blockView, world, packet, playerData.ownLocation(), currentTick);
        } else if (event.getPacketType() == PacketType.Play.Server.BLOCK_ENTITY_DATA) {
            WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(event);
            ImmutableBlockLocatable location = new ImmutableBlockLocatable(world, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
            TileEntityLocatable<PacketEventsTileEntityReplayData> tileEntity = getTrackedTileEntity(blockView, location);
            if (tileEntity == null) {
                // This can be triggered by things such as virtual signs
                Logger.warning("Received standalone block entity data for an uncached tile entity. Location: " + location.world() + " " + location.blockX() + "," + location.blockY() + "," + location.blockZ() + ". Data:" + packet.getBlockEntityType().getName() + packet.getNBT(), 7, PacketEventsBlockViewController.class);
                return;
            }
            ensureTileReplayData(tileEntity).setBlockEntityData(packet.getBlockEntityType(), packet.getNBT());
            if (!blockView.isVisible(location, currentTick)) {
                event.setCancelled(true);
                sendHiddenBlock(viewer, location);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
            @Nullable Column result = ingestChunkAndSetTileEntitiesToHiddenBlocks(playerData, world, packet.getColumn().getX(), packet.getColumn().getZ(), packet.getColumn(), playerData.nettyData().getCurrentWorldMinHeight() >> 4);
            if (result != null) {
                packet.setColumn(result);
                event.markForReEncode(true);
            }

        } else if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK_BULK) {
            WrapperPlayServerChunkDataBulk packet = new WrapperPlayServerChunkDataBulk(event);
            throw new RuntimeException("I didn't think this packet existed. Please report this to the developer with details on how to reproduce it so it can be implemented");
        }
    }

    private void removeChunk(WrapperPlayServerUnloadChunk packet, BlockView blockView, UUID world) {
        blockView.removeChunk(world, packet.getChunkX(), packet.getChunkZ());
        removeChunkTileEntities(blockView, world, packet.getChunkX(), packet.getChunkZ());
    }

    private void handleMultiBlockChange(PacketSendEvent event, BlockView blockView, UUID world, WrapperPlayServerMultiBlockChange packet, Locatable playerLocation, int currentTick) {
        MutableBlockLocatable key = new MutableBlockLocatable(world);
        for (WrapperPlayServerMultiBlockChange.EncodedBlock change : packet.getBlocks()) {
            int blockID = change.getBlockId();
            boolean occluding = blockID != 0 && blockInfoResolver.isOccluding(blockID);
            boolean tileEntity = blockInfoResolver.isTileEntity(blockID);
            blockView.upsertBlock(world, change.getX(), change.getY(), change.getZ(), occluding);
            key.set(change.getX(), change.getY(), change.getZ());
            if (tileEntity) {
                TileEntityLocatable<?> existing = blockView.getTrackedTileEntity(key);
                boolean visibleIfNew = existing == null && visibleIfNew(key, playerLocation, world);
                blockView.updateOrInsertTileEntity(key, blockID, visibleIfNew);
                if (!blockView.isVisible(key, currentTick)) {
                    change.setBlockId(getHiddenBlockId(key.blockY()));
                    event.markForReEncode(true);
                }
            } else {
                blockView.removeTileEntity(key);
            }
        }
    }

    private void processTileEntityTransitions(User viewer, BlockView blockView) {
        for (BlockViewTransition transition : blockView.drainTransitions()) {
            BlockLocatable location = transition.location();
            TileEntityLocatable<PacketEventsTileEntityReplayData> state = getTrackedTileEntity(blockView, location);

            switch (transition.type()) {
                case HIDE -> viewer.writePacketSilently(new WrapperPlayServerBlockChange(
                        new Vector3i(location.blockX(), location.blockY(), location.blockZ()),
                        getHiddenBlockId(location.blockY())
                ));
                case SHOW -> {
                    if (state == null || state.blockID() == 0) {
                        continue;
                    }
                    viewer.writePacketSilently(new WrapperPlayServerBlockChange(
                            new Vector3i(location.blockX(), location.blockY(), location.blockZ()),
                            state.blockID()
                    ));
                    PacketEventsTileEntityReplayData replayData = ensureTileReplayData(state);
                    if (replayData.blockEntityType() != null && replayData.nbt() != null) {
                        viewer.writePacketSilently(buildBlockEntityDataPacket(location, replayData));
                    }
                }
            }
        }
    }

    private void handleSingleBlockChange(PacketSendEvent event, User viewer, PlayerData playerData, UUID world, WrapperPlayServerBlockChange packet, int currentTick) {
        int blockID = packet.getBlockId();
        boolean occluding = blockID != 0 && blockInfoResolver.isOccluding(blockID);
        boolean tileEntity = blockInfoResolver.isTileEntity(blockID);
        Vector3i position = packet.getBlockPosition();
        ImmutableBlockLocatable location = new ImmutableBlockLocatable(world, position.getX(), position.getY(), position.getZ());

        playerData.blockView().upsertBlock(world, position.getX(), position.getY(), position.getZ(), occluding);
        if (tileEntity) {
            TileEntityLocatable<?> existing = playerData.blockView().getTrackedTileEntity(location);
            boolean visibleIfNew = existing == null && visibleIfNew(location, playerData.ownLocation(), world);
            playerData.blockView().updateOrInsertTileEntity(location, blockID, visibleIfNew);
            if (!playerData.blockView().isVisible(location, currentTick)) {
                event.setCancelled(true);
                sendHiddenBlock(viewer, location);
            }
        } else {
            playerData.blockView().removeTileEntity(location);
        }
    }

    private static final TileEntity[] TILE_ENTITY_ARRAY_TYPE_MARKER = new TileEntity[0];

    /**
     * Scans chunk sections once, hiding only anti-ESP managed tile entities.
     * @return the modified column with hidden blocks and filtered tile entity list if changes were made, or null to skip re-encoding when no changes were necessary
     */
    private Column ingestChunkAndSetTileEntitiesToHiddenBlocks(PlayerData playerData, UUID worldID, int chunkX, int chunkZ, Column column, int minimumChunkSectionY) {
        BlockView blockView = playerData.blockView();

        BaseChunk[] sections = column.getChunks();
        TileEntity[] chunkTileEntitiesData = column.getTileEntities();
        boolean[] includedSections = new boolean[sections.length];
        BitSet[] occludingBySection = new BitSet[sections.length];
        BitSet[] managedTileEntitiesBySection = new BitSet[sections.length];
        boolean hiddenManagedBlock = false;
        final MutableBlockLocatable key = new MutableBlockLocatable(worldID);

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            BaseChunk section = sections[sectionIndex];
            if (section == null) {
                continue;
            }

            includedSections[sectionIndex] = true;
            int sectionY = minimumChunkSectionY + sectionIndex;
            BitSet occluding = null;
            BitSet managedTileEntities = null;

            boolean chunkSectionHasOccluding = false;

            for (int localX = 0; localX < 16; localX++) {
                for (int localY = 0; localY < 16; localY++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int blockID = section.getBlockId(localX, localY, localZ);
                        if (blockID == 0) {
                            continue;
                        }

                        if (blockInfoResolver.isOccluding(blockID)) {
                            if (occluding == null) {
                                occluding = new BitSet(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);
                            }
                            occluding.set(pack(localX, localY, localZ));
                            chunkSectionHasOccluding = true;
                        }
                        if (blockInfoResolver.isTileEntity(blockID)) {
                            if (managedTileEntities == null) {
                                managedTileEntities = new BitSet(CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE);
                                managedTileEntitiesBySection[sectionIndex] = managedTileEntities;
                            }
                            managedTileEntities.set(pack(localX, localY, localZ));
                            int blockX = (chunkX << 4) + localX;
                            int blockY = (sectionY << 4) + localY;
                            int blockZ = (chunkZ << 4) + localZ;
                            key.set(blockX, blockY, blockZ);
                            blockView.updateOrInsertTileEntity(key, blockID, false);
                            section.set(localX, localY, localZ, getHiddenBlockId(blockY));
                            hiddenManagedBlock = true;
                        }
                    }
                }
            }
            if (chunkSectionHasOccluding) {
                // skip empty sections to save memory
                occludingBySection[sectionIndex] = occluding;
            }
        }

        TileEntity[] filteredTileEntitiesData = chunkTileEntitiesData;
        boolean strippedTileEntity = false;

        if (chunkTileEntitiesData != null && chunkTileEntitiesData.length > 0) {
            TileEntity[] notStrippedTileEntities = null;
            int notStrippedTileEntityCount = 0;
            for (int i = 0; i < chunkTileEntitiesData.length; i++) {
                TileEntity tileEntity = chunkTileEntitiesData[i];
                boolean strip = shouldStripChunkTileEntity(blockView, chunkX, chunkZ, sections, minimumChunkSectionY, includedSections, managedTileEntitiesBySection, tileEntity, key);

                if (strip) {
                    strippedTileEntity = true;
                    if (notStrippedTileEntities == null && i > 0) {
                        notStrippedTileEntities = new TileEntity[chunkTileEntitiesData.length];
                        System.arraycopy(chunkTileEntitiesData, 0, notStrippedTileEntities, 0, i);
                        notStrippedTileEntityCount = i;
                    }
                }
                else if (strippedTileEntity) {
                    if (notStrippedTileEntities == null) {
                        notStrippedTileEntities = new TileEntity[chunkTileEntitiesData.length];
                    }
                    notStrippedTileEntities[notStrippedTileEntityCount++] = tileEntity;
                }
            }

            if (strippedTileEntity) {
                filteredTileEntitiesData = notStrippedTileEntities == null
                        ? TILE_ENTITY_ARRAY_TYPE_MARKER
                        : Arrays.copyOf(notStrippedTileEntities, notStrippedTileEntityCount);
            }
        }

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            if (!includedSections[sectionIndex]) {
                continue;
            }
            int sectionY = minimumChunkSectionY + sectionIndex;
            BitSet occluding = occludingBySection[sectionIndex];
            if (occluding != null) {
                blockView.replaceChunkSection(worldID, chunkX, sectionY, chunkZ, occluding);
            }
            else {
                blockView.removeChunkSection(worldID, chunkX, sectionY, chunkZ);
            }
        }

        // Re-encode only when the outgoing chunk packet was actually changed.
        boolean changed = hiddenManagedBlock || strippedTileEntity;
        return changed ? copyColumnWithTileEntities(column, filteredTileEntitiesData) : null;
    }

    /**
     * Returns true when this packet NBT entry belongs to a managed or invalid tile entity and should be stripped.
     */
    private boolean shouldStripChunkTileEntity(BlockView blockView, int chunkX, int chunkZ, BaseChunk[] sections, int minimumChunkSectionY, boolean[] includedSections,
                                               BitSet[] managedTileEntitiesBySection, TileEntity tileEntity, MutableBlockLocatable key) {
        int localX = tileEntity.getX();
        int blockY = tileEntity.getY();
        int localZ = tileEntity.getZ();
        int blockX = (chunkX << 4) + localX;
        int blockZ = (chunkZ << 4) + localZ;
        int sectionIndex = (blockY >> 4) - minimumChunkSectionY;
        key.set(blockX, blockY, blockZ); // reused mutable key to save memory. The world UUID is pre-set, xyz coords must be updated. This object is used for all lookups while handling this chunk column.

        if (sectionIndex >= 0 && sectionIndex < includedSections.length && includedSections[sectionIndex]) {
            BitSet managedTileEntities = managedTileEntitiesBySection[sectionIndex];
            if (managedTileEntities != null && managedTileEntities.get(pack(localX, blockY & 15, localZ))) {
                cacheChunkTileEntity(blockView, key, tileEntity);
                return true;
            }
        }

        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            warnUncachedChunkBlockEntity(key);
            Logger.warning("Skipping uncached chunk block entity with out-of-bounds section index. Location: " + key.world() + " " + blockX + "," + blockY + "," + blockZ, 3, PacketEventsBlockViewController.class);
            return true;
        }

        BaseChunk sourceSection = sections[sectionIndex];
        if (sourceSection == null) {
            warnUncachedChunkBlockEntity(key);
            Logger.warning("Skipping uncached chunk block entity because its section data is missing. Location: " + key.world() + " " + blockX + "," + blockY + "," + blockZ, 3, PacketEventsBlockViewController.class);
            return true;
        }

        int blockID = sourceSection.getBlockId(localX, blockY & 15, localZ);
        if (blockID <= 0) {
            warnUncachedChunkBlockEntity(key);
            Logger.warning("Skipping uncached chunk block entity because the recovered block state was air or invalid (" + blockID + "). Location: " + key.world() + " " + blockX + "," + blockY + "," + blockZ, 3, PacketEventsBlockViewController.class);
            return true;
        }

        boolean managedTileEntity = blockInfoResolver.isTileEntity(blockID);
        if (blockInfoResolver.hasBlockEntityData(blockID) && !managedTileEntity) {
            // Raw-but-unmanaged block entities are normal Minecraft data; keep the packet entry untouched.
            return false;
        }

        warnUncachedChunkBlockEntity(key);
        if (managedTileEntity) {
            blockView.updateOrInsertTileEntity(key, blockID, false);
            cacheChunkTileEntity(blockView, key, tileEntity);
            return true;
        }

        Logger.warning("Recovered uncached chunk block entity from chunk sections with a non-tile-entity block state ID (" + blockID + "). Location: " + key.world() + " " + blockX + "," + blockY + "," + blockZ, 3, PacketEventsBlockViewController.class);
        return true;
    }

    private void cacheChunkTileEntity(BlockView blockView, BlockLocatable location, TileEntity tileEntity) {
        TileEntityLocatable<PacketEventsTileEntityReplayData> state = getTrackedTileEntity(blockView, location);
        if (state == null) {
            Logger.warning("Skipping chunk block entity because cached tile entity state is missing. Location: " + location.world() + " " + location.blockX() + "," + location.blockY() + "," + location.blockZ(), 3, PacketEventsBlockViewController.class);
            return;
        }
        ensureTileReplayData(state).setBlockEntityData(packetTileEntityType(tileEntity), tileEntity.getNBT());
    }

    private void warnUncachedChunkBlockEntity(BlockLocatable location) {
        Logger.warning("Received block entity data for a tile entity that wasn't in the chunk's tile entity list. Location: " + location.world() + " " + location.blockX() + "," + location.blockY() + "," + location.blockZ(), 3, PacketEventsBlockViewController.class);
    }

    private Column copyColumnWithTileEntities(Column column, TileEntity[] tileEntities) {
        TileEntity[] replacementTileEntities = tileEntities == null ? TILE_ENTITY_ARRAY_TYPE_MARKER : tileEntities;
        if (column.hasBiomeData()) {
            int[] biomeInts = column.getBiomeDataInts();
            byte[] biomeBytes = column.getBiomeDataBytes();
            if (biomeInts.length >= biomeBytes.length) {
                return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), replacementTileEntities, column.getHeightMaps(), biomeInts);
            }
            return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), replacementTileEntities, column.getHeightMaps(), biomeBytes);
        }

        if (common.v_1_21_5_orAbove) {
            return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), replacementTileEntities, column.getHeightmaps());
        }
        return new Column(column.getX(), column.getZ(), column.isFullChunk(), column.getChunks(), replacementTileEntities, column.getHeightMaps());
    }

    private void removeChunkTileEntities(BlockView blockView, UUID worldID, int chunkX, int chunkZ) {
        for (BlockLocatable known : blockView.getKnownTileEntities()) {
            if (!sameChunk(known, worldID, chunkX, chunkZ)) {
                continue;
            }
            blockView.removeTileEntity(known);
        }
    }

    private void sendHiddenBlock(User viewer, BlockLocatable location) {
        viewer.writePacketSilently(new WrapperPlayServerBlockChange(
                new Vector3i(location.blockX(), location.blockY(), location.blockZ()),
                getHiddenBlockId(location.blockY())
        ));
    }

    private boolean visibleIfNew(BlockLocatable location, Locatable playerLocation, UUID packetWorld) {
        if (!tileEntityConfig.enabled()) {
            return false;
        }
        if (playerLocation == null || playerLocation.world() == null || !playerLocation.world().equals(packetWorld)) {
            return false;
        }
        return location.distanceSquared(playerLocation) <= hideOnSpawnDistanceSquared;
    }

    private boolean sameChunk(BlockLocatable location, UUID worldID, int chunkX, int chunkZ) {
        return location.world().equals(worldID) && location.chunkX() == chunkX && location.chunkZ() == chunkZ;
    }

    private boolean sameChunkSection(BlockLocatable location, UUID worldID, int chunkX, int chunkY, int chunkZ) {
        return sameChunk(location, worldID, chunkX, chunkZ) && location.chunkY() == chunkY;
    }

    private WrapperPlayServerBlockEntityData copyBlockEntityDataPacket(WrapperPlayServerBlockEntityData packet) {
        return new WrapperPlayServerBlockEntityData(
                copyBlockVector(packet.getPosition()),
                packet.getBlockEntityType(),
                packet.getNBT()
        );
    }

    private WrapperPlayServerBlockEntityData buildBlockEntityDataPacket(BlockLocatable location, PacketEventsTileEntityReplayData replayData) {
        return new WrapperPlayServerBlockEntityData(
                new Vector3i(location.blockX(), location.blockY(), location.blockZ()),
                replayData.blockEntityType(),
                replayData.nbt()
        );
    }

    private BlockEntityType packetTileEntityType(TileEntity tileEntity) {
        return BlockEntityTypes.getById(
                PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                tileEntity.getType()
        );
    }

    private Vector3i copyBlockVector(Vector3i vector) {
        return new Vector3i(vector.getX(), vector.getY(), vector.getZ());
    }

    @SuppressWarnings("unchecked")
    private TileEntityLocatable<PacketEventsTileEntityReplayData> getTrackedTileEntity(BlockView blockView, BlockLocatable location) {
        return (TileEntityLocatable<PacketEventsTileEntityReplayData>) blockView.getTrackedTileEntity(location);
    }

    private PacketEventsTileEntityReplayData ensureTileReplayData(TileEntityLocatable<PacketEventsTileEntityReplayData> tileEntity) {
        PacketEventsTileEntityReplayData replayData = tileEntity.extraData();
        if (replayData == null) {
            replayData = new PacketEventsTileEntityReplayData();
            tileEntity.setExtraData(replayData);
        }
        return replayData;
    }
}
