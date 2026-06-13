package games.cubi.locatables.implementations;

import games.cubi.locatables.BlockLocatable;
import games.cubi.locatables.MutableLocatable;

import java.util.UUID;

public class MutableBlockLocatable implements BlockLocatable, MutableLocatable {
    private int x;
    private int y;
    private int z;
    private UUID world;

    public MutableBlockLocatable(UUID world) {
        this.world = world;
    }

    public MutableBlockLocatable(UUID world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public UUID world() {
        return world;
    }

    @Override
    public int blockX() {
        return x;
    }

    @Override
    public int blockY() {
        return y;
    }

    @Override
    public int blockZ() {
        return z;
    }

    @Override
    public LocatableType getType() {
        return LocatableType.MutableBlock;
    }

    @Override
    public MutableLocatable setX(double x) {
        this.x = (int) x;
        return this;
    }

    @Override
    public MutableLocatable setY(double y) {
        this.y = (int) y;
        return this;
    }

    @Override
    public MutableLocatable setZ(double z) {
        this.z = (int) z;
        return this;
    }

    @Override
    public MutableLocatable setX(int x) {
        this.x = x;
        return this;
    }

    @Override
    public MutableLocatable setY(int y) {
        this.y = y;
        return this;
    }

    @Override
    public MutableLocatable setZ(int z) {
        this.z = z;
        return this;
    }

    @Override
    public MutableLocatable setWorld(UUID world) {
        this.world = world;
        return this;
    }

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
        if (!(other instanceof MutableBlockLocatable that)) return false;
        if (!(this.world.equals(that.world))) return false;

        if (this.x != that.x) {
            return false;
        }
        if (this.y != that.y) {
            return false;
        }
        return this.z == that.z;
    }
}
