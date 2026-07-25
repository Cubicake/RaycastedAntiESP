package games.cubi.raycastedantiesp.core.raycast;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.MutableFloatingSpatial;
import games.cubi.locatables.api.Spatial;
import games.cubi.locatables.implementations.MutableSpatialImpl;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.view.BlockView;

public class RaycastUtil {

    //True: Has line-of-sight
    //This is deliberately a ray-stepping algorithm rather than DDA as it is much faster (2x in benchmarking)
    //Missing blocks is acceptable, as it will be assumed the player can see past those corners.
    //While this uses objects, JHM and in-game profiling have both shown that all objects used here are consistently scalarised by the JVM.
    public static boolean raycast(Locatable start, Spatial end, int maxOccluding, int alwaysShowRadius, int maxRaycastRadius, boolean debug, BlockView snap, int stepSize, ParticleSpawner particleSpawner) {
        MutableFloatingSpatial clonedEnd = end.cloneAndIfBlockThenCentre();
        double total = start.distance(clonedEnd) - stepSize; //benchmarking shows that calling distance() is faster than distanceSquared() then checking distanceSquared < stepSize*stepSize every time despite the latter replacing a square root with multiplication
        if (total <= alwaysShowRadius) return true;
        if (total > maxRaycastRadius) return false;
        if (debug && particleSpawner == null) {
            Logger.errorAndReturn(new RuntimeException("raycast called with debug enabled but no ParticleSpawner supplied"), 2, RaycastUtil.class);
        }

        Spatial dir = clonedEnd.subtract(start).normalise().scalarMultiply(stepSize);

        MutableFloatingSpatial current = new MutableSpatialImpl(start.x(),start.y(),start.z());

        for (double traveled = 0; traveled < total; traveled += stepSize) { //benchmarking shows that for loop is marginally faster than while loop initially (after running for a while they are equal
            current.add(dir);

            if (snap.isBlockOccluding(current.blockX(), current.blockY(), current.blockZ())) {
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
