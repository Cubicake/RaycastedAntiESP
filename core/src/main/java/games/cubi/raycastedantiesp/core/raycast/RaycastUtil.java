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
}
