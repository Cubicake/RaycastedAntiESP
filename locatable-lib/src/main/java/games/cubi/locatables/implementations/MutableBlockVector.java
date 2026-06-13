package games.cubi.locatables.implementations;

import games.cubi.locatables.Locatable;
import games.cubi.locatables.MutableLocatable;
import games.cubi.locatables.BlockLocatable;

import java.util.UUID;

/**
 * A mutable locatable representing a floating-point vector in a given world, akin to a MutableLocatableImpl. However, Object#equals and Object#hashCode are implemented as per BlockLocatable, meaning that they are equal if their blockX, blockY, blockZ and world are equal.
 * <p>
 *     This allows them to be used as keys in hashmaps where they will be considered equal to AbstractBlockLocations with the same block coordinates and world, but they can also be mutated and have vector-like operations performed on them.
 * <p>
 *      The object is named "Vector" to reflect the fact that it supports floating-point coordinates under the hood.
 */
public class MutableBlockVector implements BlockLocatable, MutableLocatable {
    private UUID world;
    private double mutableX;
    private double mutableY;
    private double mutableZ;

    public MutableBlockVector(UUID world, double x, double y, double z) {
        this.world = world;
        this.mutableX = x;
        this.mutableY = y;
        this.mutableZ = z;
    }

    public MutableBlockVector(UUID world, int x, int y, int z) {
        this.world = world;
        this.mutableX = x;
        this.mutableY = y;
        this.mutableZ = z;
    }

    @Override
    public LocatableType getType() {
        return LocatableType.MutableBlockVector;
    }

    @Override
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    @Override
    public double lengthSquared() {
        return mutableX*mutableX + mutableY*mutableY + mutableZ*mutableZ;
    }

    @Override
    public MutableLocatable add(Locatable locatable) {
        this.mutableX += locatable.x();
        this.mutableY +=  locatable.y();
        this.mutableZ += locatable.z();
        return this;
    }

    @Override
    public MutableLocatable subtract(Locatable locatable) {
        this.mutableX -= locatable.x();
        this.mutableY -=  locatable.y();
        this.mutableZ -= locatable.z();
        return this;
    }

    @Override
    public MutableLocatable scalarMultiply(double factor) {
        this.mutableX *= factor;
        this.mutableY *= factor;
        this.mutableZ *= factor;
        return this;
    }

    @Override
    public UUID world() {
        return world;
    }

    @Override
    public int blockX() {
        return (int) Math.floor(mutableX);
    }

    @Override
    public int blockY() {
        return (int) Math.floor(mutableY);
    }

    @Override
    public int blockZ() {
        return (int) Math.floor(mutableZ);
    }

    @Override
    public double x() {
        return mutableX;
    }

    @Override
    public double y() {
        return mutableY;
    }

    @Override
    public double z() {
        return mutableZ;
    }

    public void add(int dx, int dy, int dz) {
        this.mutableX += dx;
        this.mutableY += dy;
        this.mutableZ += dz;
    }

    @Override
    public MutableLocatable setX(double x) {
        this.mutableX = x;
        return this;
    }

    @Override
    public MutableLocatable setY(double y) {
        this.mutableY = y;
        return this;
    }

    @Override
    public MutableLocatable setZ(double z) {
        this.mutableZ = z;
        return this;
    }

    @Override
    public MutableLocatable setX(int x) {
        this.mutableX = x;
        return this;
    }

    @Override
    public MutableLocatable setY(int y) {
        this.mutableY = y;
        return this;
    }

    @Override
    public MutableLocatable setZ(int z) {
        this.mutableZ = z;
        return this;
    }

    @Override
    public MutableLocatable setWorld(UUID world) {
        this.world = world;
        return this;
    }

    /**
     * This checks equality with AbstractBlockLocations, not Locatables. Use Locatable#isEqualTo for that. Thus, hashmap lookups are compatible with only AbstractBlockLocations, not Locatables.
     */
    @Override
    public boolean equals(Object o) {
        return isEqual(o);
    }

    @Override
    public int hashCode() {
        return blockHash();
    }

    @Override
    public String toString() {
        return toStringForm();
    }

    @Override
    public boolean strictlyEquals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MutableBlockVector that)) return false;
        if (!(this.world().equals(that.world()))) return false;

        if (Double.doubleToLongBits(this.x()) != Double.doubleToLongBits(that.x())) {
            return false;
        }
        if (Double.doubleToLongBits(this.y()) != Double.doubleToLongBits(that.y())) {
            return false;
        }
        return Double.doubleToLongBits(this.z()) == Double.doubleToLongBits(that.z());
    }
}
