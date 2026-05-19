package games.cubi.raycastedantiesp.packetevents;

import org.jetbrains.annotations.Contract;

public interface BlockInfoResolver {
    @Contract(pure = true)
    boolean isOccluding(int blockStateID);
    @Contract(pure = true)
    boolean isTileEntity(int blockStateID);
}
