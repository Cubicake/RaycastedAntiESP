/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.raycast;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.Spatial;
import games.cubi.locatables.api.BlockSpatial;
import games.cubi.raycastedantiesp.core.chunks.ChunkOcclusionView;
import games.cubi.raycastedantiesp.core.view.BlockView;

import java.util.UUID;

public class RaycastUtil {

    //True: Has line-of-sight
    //This is deliberately a ray-stepping algorithm rather than DDA as it is much faster (2x in benchmarking)
    //Missing blocks is acceptable, as it will be assumed the player can see past those corners.
    //While this uses objects, JHM and in-game profiling have both shown that all objects used here are consistently scalarised by the JVM.
    /**
     * Mutable position and chunk-section state is flattened into the advancer so C2 only
     * needs to scalarise one state holder while retaining a readable ray-stepping method.
     */
    public static boolean raycastUnrolledAccumulated(int maxOccluding, final int alwaysShowRadiusSquared, final int maxRaycastRadiusSquared, boolean debug,
                                                     float yOffsetEnd, final BlockView snap, final Locatable start, final Spatial end, ParticleSpawner spawner) {
        double startX = start.x();
        double startY = start.y();
        double startZ = start.z();
        double endOffset = end instanceof BlockSpatial ? 0.5 : 0.0;
        RayDirection direction = RayDirection.from(end.x() + endOffset, end.y() + endOffset + yOffsetEnd, end.z() + endOffset, startX, startY, startZ);
        double lengthSquared = direction.getLengthSquared();
        if (lengthSquared <= alwaysShowRadiusSquared) return true;
        if (lengthSquared > maxRaycastRadiusSquared) return false;
        double total = direction.normaliseAndGetLength(lengthSquared);
        float finalDist = (float) (total - 1);

        final RayAdvancer rayAdvancer = debug ? new DebugRayAdvancer(startX, startY, startZ, direction, snap, start.world(), spawner) : new RayAdvancer(startX, startY, startZ, direction, snap);

        int remainingSteps = (int) Math.floor(finalDist);
        while (remainingSteps >= 4) {
            rayAdvancer.advance();
            rayAdvancer.advance();
            rayAdvancer.advance();
            rayAdvancer.advance();
            if (rayAdvancer.occluded > maxOccluding) return false;
            remainingSteps -= 4;
        }
        while (remainingSteps-- > 0) rayAdvancer.advance();
        return rayAdvancer.occluded <= maxOccluding;
    }

    private static sealed class RayAdvancer extends RayPosition permits DebugRayAdvancer {
        private final RayDirection direction;
        private final BlockView blockView;

        private int chunkX, chunkY, chunkZ;
        private ChunkOcclusionView currentOcclusionView;
        private int occluded = 0;

        private RayAdvancer(double startX, double startY, double startZ, RayDirection direction, BlockView blockView) {
            super(startX, startY, startZ);
            this.direction = direction;
            chunkX = blockX() >> 4;
            chunkY = blockY() >> 4;
            chunkZ = blockZ() >> 4;
            currentOcclusionView = blockView.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            this.blockView = blockView;
        }

        private void advance() {
            add(direction);
            int blockX = blockX();
            int blockY = blockY();
            int blockZ = blockZ();
            if (!isCurrentChunk(blockX, blockY, blockZ)) {
                moveToChunk(blockX, blockY, blockZ);
                currentOcclusionView = blockView.getChunkOcclusionView(chunkX, chunkY, chunkZ);
            }
            if (currentOcclusionView != null && currentOcclusionView.isOccludingGlobal(blockX, blockY, blockZ)) {
                occluded++;
                particleRed();
            } else particleGreen();
        }

        private boolean isCurrentChunk(int blockX, int blockY, int blockZ) {
            return blockX >> 4 == chunkX && blockY >> 4 == chunkY && blockZ >> 4 == chunkZ;
            // ((nextChunkX ^ chunkX) | (nextChunkY ^ chunkY) | (nextChunkZ ^ chunkZ)) != 0 is slower than this
        }

        private void moveToChunk(int blockX, int blockY, int blockZ) {
            chunkX = blockX >> 4;
            chunkY = blockY >> 4;
            chunkZ = blockZ >> 4;
        }

        void particleGreen() {}
        void particleRed() {}
    }

    private static final class DebugRayAdvancer extends RayAdvancer {
        private final UUID world;
        private final ParticleSpawner particleSpawner;
        private DebugRayAdvancer(double startX, double startY, double startZ, RayDirection direction, BlockView blockView, UUID world, ParticleSpawner particleSpawner) {
            super(startX, startY, startZ, direction, blockView);
            this.world = world;
            this.particleSpawner = particleSpawner;
        }
        void particleGreen() {
            particleSpawner.spawnParticleAt(world, x, y, z, ParticleSpawner.Colour.GREEN);
        }
        void particleRed() {
            particleSpawner.spawnParticleAt(world, x, y, z, ParticleSpawner.Colour.RED);
        }
    }

    private static sealed class RayPosition permits RayAdvancer {
        double x, y, z;

        private RayPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        final void add(RayDirection direction) {
            x += direction.x;
            y += direction.y;
            z += direction.z;
        }

        final int blockX() {
            return (int) Math.floor(x);
        }

        final int blockY() {
            return (int) Math.floor(y);
        }

        final int blockZ() {
            return (int) Math.floor(z);
        }
    }

    private static final class RayDirection {
        private double x, y, z;

        private RayDirection(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static RayDirection from(double endX, double endY, double endZ, double startX, double startY, double startZ) {
            return new RayDirection(endX - startX, endY - startY, endZ - startZ);
        }

        private double getLengthSquared() {
            return x * x + y * y + z * z;
        }

        private double normaliseAndGetLength(double lengthSquared) {
            double len = Math.sqrt(lengthSquared);
            double scale = 1 / len;
            x *= scale;
            y *= scale;
            z *= scale;
            return len;
        }
    }
}
