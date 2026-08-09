/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.raycast;

import games.cubi.locatables.api.*;
import games.cubi.locatables.implementations.MutableChunkSection;
import games.cubi.locatables.implementations.MutableSpatialImpl;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.chunks.ChunkOcclusionView;
import games.cubi.raycastedantiesp.core.view.BlockView;

public class RaycastUtil {

    //True: Has line-of-sight
    //This is deliberately a ray-stepping algorithm rather than DDA as it is much faster (2x in benchmarking)
    //Missing blocks is acceptable, as it will be assumed the player can see past those corners.
    //While this uses objects, JHM and in-game profiling have both shown that all objects used here are consistently scalarised by the JVM.
    public static boolean raycast(Locatable start, Spatial end, int maxOccluding, int alwaysShowRadius, int maxRaycastRadius, boolean debug, BlockView snap, float stepSize, ParticleSpawner particleSpawner) {
        //return raycastCubiNoAllocUnrolledLongBitsAccumulated(maxOccluding, alwaysShowRadius, maxRaycastRadius, debug, 1, snap, start.x(), start.y(), start.z(), end.x(), end.y(), end.z());

        //return raycastUnrolledAccumulated(maxOccluding, alwaysShowRadius, maxRaycastRadius, debug, 1, snap, start, end);
        return raycast(start, end, maxOccluding, alwaysShowRadius, maxRaycastRadius, debug, snap, 0f, stepSize, particleSpawner);
    }

    public static boolean raycast(Locatable start, Spatial end, int maxOccluding, int alwaysShowRadius, int maxRaycastRadius, boolean debug, BlockView snap, float yOffsetEnd, float stepSize, ParticleSpawner particleSpawner) {
        double endOffset = end instanceof BlockSpatial ? 0.5 : 0.0;
        MutableFloatingSpatial clonedEnd = new MutableSpatialImpl(end.x() + endOffset, end.y() + endOffset + yOffsetEnd, end.z() + endOffset);
        //Equivalent to end.cloneAndIfBlockThenCentre(); but not used since the JVM was not reliably scalarising that method (probably due to the polymorphic overriding?). This causes 0 object allocations.
        float total = (float) (start.distance(clonedEnd) - stepSize); //benchmarking shows that calling distance() is faster than distanceSquared() then checking distanceSquared < stepSize*stepSize every time despite the latter replacing a square root with multiplication
        if (total <= alwaysShowRadius) return true;
        if (total > maxRaycastRadius) return false;
        if (debug && particleSpawner == null) {
            Logger.errorAndReturn(new RuntimeException("raycast called with debug enabled but no ParticleSpawner supplied"), 2, RaycastUtil.class);
        }

        Spatial dir = clonedEnd.subtract(start).normalise().scalarMultiply(stepSize);

        MutableFloatingSpatial current = new MutableSpatialImpl(start.x(),start.y(),start.z());
        MutableChunkSection currentChunk = new MutableChunkSection().updateFrom(current);
        ChunkOcclusionView currentOcclusionView = snap.getChunkOcclusionView(currentChunk);

        for (float traveled = 0; traveled < total; traveled += stepSize) {
            current.add(dir);
            if (!ChunkSectionEquality.equals(currentChunk, current)) {
                currentChunk.updateFrom(current);
                currentOcclusionView = snap.getChunkOcclusionView(currentChunk);
            }

            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(current.blockX(), current.blockY(), current.blockZ())) {
                maxOccluding--;
                if (debug) particleSpawner.spawnParticleAt(start.world(), current, ParticleSpawner.Colour.RED);
                if (maxOccluding < 1) return false;
                continue;
            }

            if (debug) particleSpawner.spawnParticleAt(start.world(), current, ParticleSpawner.Colour.GREEN);
        }
        return true;
    }
/*
    public static boolean raycastCubiNoAllocUnrolledLongBitsAccumulated(int maxOccluding, final int alwaysShowRadius, final int maxRaycastRadius, boolean debug, final int stepSize, BlockView snap,
                                                                        final double startX, final double startY, final double startZ, final double endX, final double endY, final double endZ) {
        if (maxOccluding <= 0) return false;

        double total = Math.sqrt(((endX - startX) * (endX - startX)) + ((endY - startY) * (endY - startY)) + ((endZ - startZ) * (endZ - startZ)));
        if (total <= alwaysShowRadius) return true;
        if (total > maxRaycastRadius) return false;
        double finalDist = total - stepSize;

        final double dirX = (endX - startX) / total * stepSize;
        final double dirY = (endY - startY) / total * stepSize;
        final double dirZ = (endZ - startZ) / total * stepSize;

        double currentX = startX;
        double currentY = startY;
        double currentZ = startZ;

        int chunkX = ((int) currentX) >> 4;
        int chunkY = ((int) currentY) >> 4;
        int chunkZ = ((int) currentZ) >> 4;
        ChunkOcclusionView currentOcclusionView = snap.getChunkOcclusionView(chunkX, chunkY, chunkZ);

        int occluded = 0;

        double traveled = 0;
        final double fourSteps = stepSize * 4.0;
        while (traveled + fourSteps <= finalDist) {
            currentX += dirX;
            currentY += dirY;
            currentZ += dirZ;

            int blockX = (int) Math.floor(currentX);
            int blockY = (int) Math.floor(currentY);
            int blockZ = (int) Math.floor(currentZ);
            if (blockX >> 4 != chunkX || blockY >> 4 != chunkY || blockZ >> 4 != chunkZ) {
                chunkX = blockX >> 4;
                chunkY = blockY >> 4;
                chunkZ = blockZ >> 4;
                currentOcclusionView = snap.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded ++;

            currentX += dirX;
            currentY += dirY;
            currentZ += dirZ;

            blockX = (int) Math.floor(currentX);
            blockY = (int) Math.floor(currentY);
            blockZ = (int) Math.floor(currentZ);
            if (blockX >> 4 != chunkX || blockY >> 4 != chunkY || blockZ >> 4 != chunkZ) {
                chunkX = blockX >> 4;
                chunkY = blockY >> 4;
                chunkZ = blockZ >> 4;
                currentOcclusionView = snap.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded ++;

            currentX += dirX;
            currentY += dirY;
            currentZ += dirZ;

            blockX = (int) Math.floor(currentX);
            blockY = (int) Math.floor(currentY);
            blockZ = (int) Math.floor(currentZ);
            if (blockX >> 4 != chunkX || blockY >> 4 != chunkY || blockZ >> 4 != chunkZ) {
                chunkX = blockX >> 4;
                chunkY = blockY >> 4;
                chunkZ = blockZ >> 4;
                currentOcclusionView = snap.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded ++;

            currentX += dirX;
            currentY += dirY;
            currentZ += dirZ;

            blockX = (int) Math.floor(currentX);
            blockY = (int) Math.floor(currentY);
            blockZ = (int) Math.floor(currentZ);
            if (blockX >> 4 != chunkX || blockY >> 4 != chunkY || blockZ >> 4 != chunkZ) {
                chunkX = blockX >> 4;
                chunkY = blockY >> 4;
                chunkZ = blockZ >> 4;
                currentOcclusionView = snap.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded ++;

            if (occluded >= maxOccluding) return false;

            traveled += fourSteps;
        }

        for (; traveled < finalDist; traveled += stepSize) {
            currentX += dirX;
            currentY += dirY;
            currentZ += dirZ;

            int blockX = (int) Math.floor(currentX);
            int blockY = (int) Math.floor(currentY);
            int blockZ = (int) Math.floor(currentZ);
            if (blockX >> 4 != chunkX || blockY >> 4 != chunkY || blockZ >> 4 != chunkZ) {
                chunkX = blockX >> 4;
                chunkY = blockY >> 4;
                chunkZ = blockZ >> 4;
                currentOcclusionView = snap.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded ++;
        }

        return occluded < maxOccluding;
    }*/

    /**
     * The local spatial and chunk-section objects do not escape this method, so the JVM
     * should be able to scalarise them while keeping the ray-stepping code easier to follow.
     */
    public static boolean raycastUnrolledAccumulated(int maxOccluding, final int alwaysShowRadius, final int maxRaycastRadius, boolean debug,
                                                     final float stepSize, final BlockView snap, final Spatial start, final Spatial end) {
        RayPosition current = RayPosition.from(start);
        RayDirection direction = RayDirection.from(end, current);
        double total = direction.getLengthAndNormalise(stepSize);
        if (total <= alwaysShowRadius) return true;
        if (total > maxRaycastRadius) return false;
        float finalDist = (float) (total - stepSize);

        RayChunkSection currentChunk = new RayChunkSection().updateFrom(current);
        ChunkOcclusionView currentOcclusionView = snap.getChunkOcclusionView(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);

        int occluded = 0;

        float traveled = 0;
        final float fourSteps = stepSize * 4.0f;
        while (traveled + fourSteps <= finalDist) {
            current.advance(direction);
            int blockX = current.blockX();
            int blockY = current.blockY();
            int blockZ = current.blockZ();
            if (blockX >> 4 != currentChunk.chunkX || blockY >> 4 != currentChunk.chunkY || blockZ >> 4 != currentChunk.chunkZ) {
                currentChunk.updateFrom(current);
                currentOcclusionView = snap.getChunkOcclusionView(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded++;

            current.advance(direction);
            blockX = current.blockX();
            blockY = current.blockY();
            blockZ = current.blockZ();
            if (blockX >> 4 != currentChunk.chunkX || blockY >> 4 != currentChunk.chunkY || blockZ >> 4 != currentChunk.chunkZ) {
                currentChunk.updateFrom(current);
                currentOcclusionView = snap.getChunkOcclusionView(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded++;

            current.advance(direction);
            blockX = current.blockX();
            blockY = current.blockY();
            blockZ = current.blockZ();
            if (blockX >> 4 != currentChunk.chunkX || blockY >> 4 != currentChunk.chunkY || blockZ >> 4 != currentChunk.chunkZ) {
                currentChunk.updateFrom(current);
                currentOcclusionView = snap.getChunkOcclusionView(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded++;

            current.advance(direction);
            blockX = current.blockX();
            blockY = current.blockY();
            blockZ = current.blockZ();
            if (blockX >> 4 != currentChunk.chunkX || blockY >> 4 != currentChunk.chunkY || blockZ >> 4 != currentChunk.chunkZ) {
                currentChunk.updateFrom(current);
                currentOcclusionView = snap.getChunkOcclusionView(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded++;

            if (occluded >= maxOccluding) return false;

            traveled += fourSteps;
        }

        for (; traveled < finalDist; traveled += stepSize) {
            current.advance(direction);
            int blockX = current.blockX();
            int blockY = current.blockY();
            int blockZ = current.blockZ();
            if (blockX >> 4 != currentChunk.chunkX || blockY >> 4 != currentChunk.chunkY || blockZ >> 4 != currentChunk.chunkZ) {
                currentChunk.updateFrom(current);
                currentOcclusionView = snap.getChunkOcclusionView(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) occluded++;
        }

        return occluded < maxOccluding;
    }

    private static final class RayPosition {
        private double x, y, z;

        private void advance(RayDirection direction) {
            x += direction.x;
            y += direction.y;
            z += direction.z;
        }

        private int blockX() {
            return (int) Math.floor(x);
        }

        private int blockY() {
            return (int) Math.floor(y);
        }

        private int blockZ() {
            return (int) Math.floor(z);
        }

        private int chunkX() {
            return blockX() >> 4;
        }

        private int chunkY() {
            return blockY() >> 4;
        }

        private int chunkZ() {
            return blockZ() >> 4;
        }

        private RayPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static RayPosition from(Spatial spatial) {
            return new RayPosition(spatial.x(), spatial.y(), spatial.z());
        }
    }

    private static final class RayDirection {
        private double x, y, z;

        private RayDirection(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static RayDirection from(Spatial end, RayPosition start) {
            return new RayDirection(end.x() - start.x, end.y() - start.y, end.z() - start.z);
        }

        private double getLengthAndNormalise(float stepSize) {
            double len = Math.sqrt(x * x + y * y + z * z);
            x = (x / len) * stepSize;
            y = (y / len) * stepSize;
            z = (z / len) * stepSize;
            return len;
        }
    }

    private static final class RayChunkSection {
        private int chunkX, chunkY, chunkZ;

        private RayChunkSection updateFrom(RayPosition other) {
            this.chunkX = other.chunkX();
            this.chunkY = other.chunkY();
            this.chunkZ = other.chunkZ();

            return this;
        }
    }
}
