/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.packets;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.chunks.BlockInfoResolver;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class PacketEventsPaperBlockInfoResolver implements BlockInfoResolver {
    private final boolean[] occlusionArray;
    /** Raw Bukkit TileState capability, before plugin config overrides. */
    private final boolean[] rawTileEntityArray;
    /** Anti-ESP managed tile entity state, after configured exclusions. */
    private final boolean[] tileEntityArray;

    public PacketEventsPaperBlockInfoResolver(Predicate<BlockData> excludedBlocks) {
        BlockInfoArrays result = iterateBlockStates(Objects.requireNonNull(excludedBlocks, "excludedBlocks"));
        occlusionArray = result.occluding;
        rawTileEntityArray = result.rawTileEntity;
        tileEntityArray = result.managedTileEntity;
    }

    private BlockInfoArrays iterateBlockStates(Predicate<BlockData> excludedBlocks) {
        boolean run = true;
        int airs = 0;
        int lastNonAirID = 0;
        Map<Integer, Boolean> occlusion = new HashMap<>(111000); //Tests show 30,000 block IDs in 1.21.11, and we scan forwards for 80k air ids just in case, so 111k is enough. This is a pointless micro optimization but why not
        Map<Integer, Boolean> rawTileEntity = new HashMap<>(111000);
        Map<Integer, Boolean> managedTileEntity = new HashMap<>(111000);
        int iterator = 0;
        while (run) {
            BlockData blockData = SpigotConversionUtil.toBukkitBlockData(WrappedBlockState.getByGlobalId(iterator));
            if (blockData == null) {
                Logger.warning("Material for block state ID " + iterator + " is null, stopping iteration. This is not expected to happen.", 5, PacketEventsPaperBlockInfoResolver.class);
                run = false;
                continue;
            }
            Material material = blockData.getMaterial();
            if (material == Material.AIR) {
                airs++;
                if (airs > 80000) { // There is a sequence of ~40 air blocks around ID 100, and another of several hundred at ~3000. We scan forwards 80k to future-proof any mojank. Since it runs once at startup, perf is irrelevant here
                    run = false;
                    continue;
                }
            }
            else {
                airs = 0;
                lastNonAirID = iterator;
            }
            boolean occluding = material != Material.BARRIER && material.isOccluding();
            occlusion.put(iterator, occluding);
            try {
                boolean isTileEntity = blockData.createBlockState() instanceof TileState;
                rawTileEntity.put(iterator, isTileEntity);
                managedTileEntity.put(iterator, isTileEntity && !excludedBlocks.test(blockData));
            } catch (Exception a) {
                rawTileEntity.put(iterator, false);
                managedTileEntity.put(iterator, false);
                // will sometimes inconsistently happen, just ignore it ig?
            }
            iterator++;
        }
        boolean[] occlusionArray = new boolean[lastNonAirID + 1];
        boolean[] rawTileEntityArray = new boolean[lastNonAirID + 1];
        boolean[] managedTileEntityArray = new boolean[lastNonAirID + 1];
        for (int i = 0; i < (lastNonAirID + 1) /*Ignore the trailing airs*/; i++) {
            occlusionArray[i] = occlusion.get(i);
            rawTileEntityArray[i] = rawTileEntity.get(i);
            managedTileEntityArray[i] = managedTileEntity.get(i);
        }
        return new BlockInfoArrays(occlusionArray, rawTileEntityArray, managedTileEntityArray);
    }

    @Override
    public boolean isOccluding(int blockStateID) {
        if (blockStateID < 0 || blockStateID >= occlusionArray.length) {
            return false; // Default to non-occluding for invalid IDs, should be safe since invalid IDs shouldn't exist in the world
        }
        return occlusionArray[blockStateID];
    }

    @Override
    public boolean isTileEntity(int blockStateID) {
        if (blockStateID < 0 || blockStateID >= tileEntityArray.length) {
            return false; // Default to non-tile-entity for invalid IDs, should be safe since invalid IDs shouldn't exist in the world
        }
        return tileEntityArray[blockStateID];
    }

    @Override
    public boolean hasBlockEntityData(int blockStateID) {
        if (blockStateID < 0 || blockStateID >= rawTileEntityArray.length) {
            return false; // Default to non-block-entity for invalid IDs, should be safe since invalid IDs shouldn't exist in the world
        }
        return rawTileEntityArray[blockStateID];
    }

    public boolean[] dumpOcclusionArray() {
        return occlusionArray;
    }

    private boolean[] dumpTileEntityArray() {
        return tileEntityArray;
    }

    private record BlockInfoArrays(boolean[] occluding, boolean[] rawTileEntity, boolean[] managedTileEntity) {
    }
}
