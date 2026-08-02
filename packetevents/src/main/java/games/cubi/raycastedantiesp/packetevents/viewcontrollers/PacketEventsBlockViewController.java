package games.cubi.raycastedantiesp.packetevents.viewcontrollers;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import games.cubi.locatables.api.BlockSpatial;
import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.implementations.ImmutableBlockSpatialImpl;
import games.cubi.locatables.implementations.MutableBlockSpatialImpl;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.raycastedantiesp.core.tracked.NettyTileEntity;
import games.cubi.raycastedantiesp.core.tracked.TrackedTileEntity;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.BlockViewTransition;
import games.cubi.raycastedantiesp.packetevents.replaydata.PacketEventsTileEntityReplayData;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser.BlockChunkParser;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser.ChunkParser;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser.NonMutatingBlockChunkParser;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser.NonMutatingOcclusionChunkParser;
import games.cubi.raycastedantiesp.packetevents.viewcontrollers.chunkparser.OcclusionChunkParser;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.IntSupplier;

public abstract class PacketEventsBlockViewController implements PacketListener {
    private final BlockInfoResolver blockInfoResolver;
    private final ChunkParser mutatingChunkParser;
    private final ChunkParser nonMutatingChunkParser;
    private final IntSupplier currentTickSupplier;
    private final PacketEventsCommonViewController common;
    private TileEntityConfig tileEntityConfig = null;
    private int hideOnSpawnDistanceSquared = 0;

    protected PacketEventsBlockViewController(BlockInfoResolver blockInfoResolver, boolean trackAllBlocks, IntSupplier currentTickSupplier) {
        this.blockInfoResolver = blockInfoResolver;
        this.currentTickSupplier = currentTickSupplier;
        common = PacketEventsCommonViewController.get(currentTickSupplier);
        if (trackAllBlocks) {
            mutatingChunkParser = new BlockChunkParser(blockInfoResolver, this::getHiddenBlockId);
            nonMutatingChunkParser = new NonMutatingBlockChunkParser(blockInfoResolver, this::getHiddenBlockId);
        } else {
            mutatingChunkParser = new OcclusionChunkParser(blockInfoResolver, this::getHiddenBlockId);
            nonMutatingChunkParser = new NonMutatingOcclusionChunkParser(blockInfoResolver, this::getHiddenBlockId);
        }
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
        boolean tileChecksEnabled = tileEntityConfig.enabled();
        BlockView blockView = playerData.blockView();
        blockView.applyTileEntityCheckMode(tileChecksEnabled, currentTick,
                tileEntity -> sendTileEntityVisibilityRepair(event.getUser(), tileEntity));

        handleBlockPackets(event, event.getUser(), playerData, world, currentTick, tileChecksEnabled);

        if (blockView.hasPendingTransitions()) {
            processTileEntityTransitions(event.getUser(), playerData);
        }
    }

    private void handleBlockPackets(PacketSendEvent event, User viewer, PlayerData playerData, UUID world, int currentTick, boolean tileChecksEnabled) {
        if (world == null) {
            return;
        }

        BlockView blockView = playerData.blockView();

        if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrapperPlayServerUnloadChunk packet = new WrapperPlayServerUnloadChunk(event);
            removeChunk(packet, blockView, world);
        } else if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            handleSingleBlockChange(event, viewer, playerData, world, packet, tileChecksEnabled);
        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
            handleMultiBlockChange(event, blockView, world, packet, playerData.ownLocation(), tileChecksEnabled);
        } else if (event.getPacketType() == PacketType.Play.Server.BLOCK_ENTITY_DATA) {
            WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(event);
            ImmutableBlockSpatialImpl position = new ImmutableBlockSpatialImpl(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
            TrackedTileEntity<PacketEventsTileEntityReplayData> tileEntity = getTrackedTileEntity(blockView, world, position);
            if (tileEntity == null) {
                // This can be triggered by things such as virtual signs
                Logger.warning("Received standalone block entity data for an uncached tile entity. Location: " + world + " " + position.blockX() + "," + position.blockY() + "," + position.blockZ() + ". Data:" + packet.getBlockEntityType().getName() + packet.getNBT(), 7, PacketEventsBlockViewController.class);
                return;
            }
            ensureTileReplayData(tileEntity).setBlockEntityData(packet.getBlockEntityType(), packet.getNBT());
            if (tileChecksEnabled && !blockView.isVisible(world, position, currentTick)) {
                event.setCancelled(true);
                sendHiddenBlock(viewer, position);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
            ChunkParser parser = tileChecksEnabled ? mutatingChunkParser : nonMutatingChunkParser;
            @Nullable Column result = parser.parse(blockView, world, packet.getColumn(), playerData.nettyData().getCurrentWorldMinHeight() >> 4);
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
    }

    private void handleMultiBlockChange(PacketSendEvent event, BlockView blockView, UUID world, WrapperPlayServerMultiBlockChange packet, Locatable playerLocation, boolean tileChecksEnabled) {
        MutableBlockSpatialImpl key = new MutableBlockSpatialImpl(0, 0, 0);
        for (WrapperPlayServerMultiBlockChange.EncodedBlock change : packet.getBlocks()) {
            char blockID = (char) change.getBlockId();
            boolean tileEntity = blockInfoResolver.isTileEntity(blockID);
            blockView.upsertBlock(world, change.getX(), change.getY(), change.getZ(), blockID);
            key.setBlockPosition(change.getX(), change.getY(), change.getZ());
            if (tileEntity) {
                boolean visibleIfNew = !tileChecksEnabled || visibleIfNew(key, playerLocation, world);
                TrackedTileEntity<?> state = blockView.updateOrInsertTileEntity(world, key, blockID, visibleIfNew);
                if (!tileChecksEnabled) {
                    blockView.recordOutboundTileEntityVisibility(state, true);
                } else if (state != null && !state.visible()) {
                    change.setBlockId(getHiddenBlockId(key.blockY()));
                    event.markForReEncode(true);
                }
            } else {
                blockView.removeTileEntity(world, key);
            }
        }
    }

    static class MutableVector3i extends Vector3i {
        int x,y,z;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public MutableVector3i set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
    }

    private final ThreadLocal<MutableVector3i> vector = ThreadLocal.withInitial(MutableVector3i::new);
    private final ThreadLocal<WrapperPlayServerBlockChange> blockChangeWrapper = ThreadLocal.withInitial(() -> new WrapperPlayServerBlockChange(vector.get(), 0));

    private WrapperPlayServerBlockChange getBlockChangeWith(int x, int y, int z, int blockID) {
        MutableVector3i vec = vector.get().set(x,y,z);
        WrapperPlayServerBlockChange change = blockChangeWrapper.get();
        change.setBlockID(blockID);
        change.setBlockPosition(vec);
        return change;
    }

    private void processTileEntityTransitions(User viewer, PlayerData playerData) {
        BlockView blockView = playerData.blockView();
        int worldEpoch = playerData.acquireWorldEpoch();
        blockView.drainTransitions((type, tileEntity, modeToken, transitionWorldEpoch) ->
                processTileEntityTransition(viewer, blockView, worldEpoch, type, tileEntity, modeToken, transitionWorldEpoch));
    }

    private void processTileEntityTransition(User viewer, BlockView blockView, int worldEpoch,
            BlockViewTransition.Type type, TrackedTileEntity<?> tileEntity, long modeToken, int transitionWorldEpoch) {
        TrackedTileEntity<PacketEventsTileEntityReplayData> state = resolveCurrentTransitionState(tileEntity, transitionWorldEpoch, worldEpoch);
        if (state == null || state.blockID() == 0) {
            return;
        }
        switch (type) {
            case HIDE -> {
                if (!blockView.isCurrentEnabledTileEntityMode(modeToken)) {
                    state.setVisible(true);
                    state.setLastChecked(TrackedTileEntity.NEVER_CHECKED);
                    return;
                }
                viewer.writePacketSilently(getBlockChangeWith(tileEntity.blockX(), tileEntity.blockY(), tileEntity.blockZ(), getHiddenBlockId(tileEntity.blockY())));
            }
            case SHOW -> {
                if (!state.visible()) {
                    return;
                }
                sendTileEntity(viewer, state);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sendTileEntityVisibilityRepair(User viewer, TrackedTileEntity<?> tileEntity) {
        TrackedTileEntity<PacketEventsTileEntityReplayData> state = (TrackedTileEntity<PacketEventsTileEntityReplayData>) tileEntity;
        if (state.blockID() != 0) {
            sendTileEntity(viewer, state);
        }
    }

    private void sendTileEntity(User viewer, TrackedTileEntity<PacketEventsTileEntityReplayData> tileEntity) {
        viewer.writePacketSilently(getBlockChangeWith(tileEntity.blockX(), tileEntity.blockY(), tileEntity.blockZ(), tileEntity.blockID()));

        PacketEventsTileEntityReplayData replayData = ensureTileReplayData(tileEntity);
        if (replayData.blockEntityType() != null && replayData.nbt() != null) {
            viewer.writePacketSilently(buildBlockEntityDataPacket(tileEntity, replayData));
        }
    }

    private void handleSingleBlockChange(PacketSendEvent event, User viewer, PlayerData playerData, UUID world, WrapperPlayServerBlockChange packet, boolean tileChecksEnabled) {
        char blockID = (char) packet.getBlockId();
        boolean tileEntity = blockInfoResolver.isTileEntity(blockID);
        Vector3i position = packet.getBlockPosition();
        ImmutableBlockSpatialImpl location = new ImmutableBlockSpatialImpl(position.getX(), position.getY(), position.getZ());

        playerData.blockView().upsertBlock(world, position.getX(), position.getY(), position.getZ(), blockID);
        if (tileEntity) {
            boolean visibleIfNew = !tileChecksEnabled || visibleIfNew(location, playerData.ownLocation(), world);
            TrackedTileEntity<?> state = playerData.blockView().updateOrInsertTileEntity(world, location, blockID, visibleIfNew);
            if (!tileChecksEnabled) {
                playerData.blockView().recordOutboundTileEntityVisibility(state, true);
            } else if (state != null && !state.visible()) {
                event.setCancelled(true);
                sendHiddenBlock(viewer, location);
            }
        } else {
            playerData.blockView().removeTileEntity(world, location);
        }
    }

    private void sendHiddenBlock(User viewer, BlockSpatial location) {
        viewer.writePacketSilently(new WrapperPlayServerBlockChange(
                new Vector3i(location.blockX(), location.blockY(), location.blockZ()),
                getHiddenBlockId(location.blockY())
        ));
    }

    private boolean visibleIfNew(BlockSpatial location, Locatable playerLocation, UUID packetWorld) {
        if (!tileEntityConfig.enabled()) {
            return false;
        }
        if (playerLocation == null || playerLocation.world() == null || !playerLocation.world().equals(packetWorld)) {
            return false;
        }
        return location.distanceSquared(playerLocation) <= hideOnSpawnDistanceSquared;
    }

    private WrapperPlayServerBlockEntityData buildBlockEntityDataPacket(BlockSpatial location, PacketEventsTileEntityReplayData replayData) {
        return new WrapperPlayServerBlockEntityData(
                new Vector3i(location.blockX(), location.blockY(), location.blockZ()),
                replayData.blockEntityType(),
                replayData.nbt()
        );
    }

    @SuppressWarnings("unchecked")
    private static TrackedTileEntity<PacketEventsTileEntityReplayData> getTrackedTileEntity(BlockView blockView, UUID world, BlockSpatial position) {
        return (TrackedTileEntity<PacketEventsTileEntityReplayData>) blockView.getTrackedTileEntity(world, position);
    }

    @SuppressWarnings("unchecked")
    static @Nullable TrackedTileEntity<PacketEventsTileEntityReplayData> resolveCurrentTransitionState(BlockViewTransition transition, int currentWorldEpoch) {
        return resolveCurrentTransitionState(transition.tileEntity(), transition.worldEpoch(), currentWorldEpoch);
    }

    @SuppressWarnings("unchecked")
    static @Nullable TrackedTileEntity<PacketEventsTileEntityReplayData> resolveCurrentTransitionState(
            TrackedTileEntity<?> tileEntity, int transitionWorldEpoch, int currentWorldEpoch) {
        if (transitionWorldEpoch != currentWorldEpoch
                || !(tileEntity instanceof NettyTileEntity<?> nettyTileEntity)
                || nettyTileEntity.isRemoved()) {
            return null;
        }
        return (TrackedTileEntity<PacketEventsTileEntityReplayData>) tileEntity;
    }

    private PacketEventsTileEntityReplayData ensureTileReplayData(TrackedTileEntity<PacketEventsTileEntityReplayData> tileEntity) {
        PacketEventsTileEntityReplayData replayData = tileEntity.extraData();
        if (replayData == null) {
            replayData = new PacketEventsTileEntityReplayData();
            tileEntity.setExtraData(replayData);
        }
        return replayData;
    }
}
