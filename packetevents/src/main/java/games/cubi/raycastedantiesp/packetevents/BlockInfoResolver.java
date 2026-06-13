package games.cubi.raycastedantiesp.packetevents;

import org.jetbrains.annotations.Contract;

public interface BlockInfoResolver {
    @Contract(pure = true)
    boolean isOccluding(int blockStateID);

    /**
     * Anti-ESP managed tile entity state after config exemptions and force-includes.
     */
    @Contract(pure = true)
    boolean isTileEntity(int blockStateID);

    /**
     * Raw block entity capability before anti-ESP config overrides.
     */
    @Contract(pure = true)
    boolean hasBlockEntityData(int blockStateID);
}
