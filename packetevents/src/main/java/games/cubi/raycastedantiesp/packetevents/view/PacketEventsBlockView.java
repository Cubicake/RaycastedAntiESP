package games.cubi.raycastedantiesp.packetevents.view;

import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.implementations.ImmutableBlockLocatable;
import games.cubi.raycastedantiesp.core.view.AbstractBlockView;
import games.cubi.raycastedantiesp.packetevents.locatables.PacketEventsTileEntity;

import java.util.UUID;

public class PacketEventsBlockView extends AbstractBlockView<PacketEventsTileEntity> {
    @Override
    protected PacketEventsTileEntity createTrackedTileEntity(BlockLocatable location, int blockID, boolean visible) {
        return new PacketEventsTileEntity(location, visible, 0, blockID);
    }

    @Override
    protected PacketEventsTileEntity createTrackedTileEntity(UUID world, int x, int y, int z, int blockID) {
        return new PacketEventsTileEntity(world, x, y, z, false, blockID);
    }
}
