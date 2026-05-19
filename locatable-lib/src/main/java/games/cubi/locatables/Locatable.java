package games.cubi.locatables;

import games.cubi.locatables.implementations.*;

import java.util.UUID;

// A vector-like interface representing a location in a 3D space within a specific world.
public sealed interface Locatable extends ChunkSectionLocatable, StrictEquality permits MutableLocatable, ImmutableLocatable, BlockLocatable {

    double x();
    double y();
    double z();
    UUID world();

    default int blockX() {
        return (int) Math.floor(x());
    }
    default int blockY() {
        return (int) Math.floor(y());
    }
    default int blockZ() {
        return (int) Math.floor(z());
    }

    @Override
    default int chunkX() {
        return blockX() >> 4;
    }
    @Override
    default int chunkZ() {
        return blockZ() >> 4;
    }
    @Override
    default int chunkY() {
        return blockY() >> 4;
    }

    default double length() {
        return Math.sqrt(lengthSquared());
    }

    default double lengthSquared() {
        return x() * x() +  y() * y() + z() * z();
    }

    default double distance(Locatable locatable) {
        return Math.sqrt(distanceSquared(locatable));
    }

    default double distanceSquared(Locatable locatable) {
        double dx = x() - locatable.x();
        double dy = y() - locatable.y();
        double dz = z() - locatable.z();
        return dx * dx + dy * dy + dz * dz;
    }

    default double distanceSquared(double x, double y, double z) {
        double dx = this.x() - x;
        double dy = this.y() - y;
        double dz = this.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    default MutableLocatable clonePlainAndCentreIfBlockLocation() {
        return new MutableLocatableImpl(world(), x(), y(), z());
    }

    /**@return An array of the 3 coordinates, in the order [x,y,z]. This is used for atomic updates of all 3 coordinates.*/
    default double[] getAtomicPositionArray() {
        synchronized (this) {
            return new double[]{x(), y(), z()};
        }
    }

    LocatableType getType();

    boolean isMutable();

    MutableLocatable castToMutableOrNull(); // There shouldn't be a need for an equivalent of this method for immutable locatables as all of their methods are common to all Locatables

    default boolean isEqualTo(Object thatOne) {
        if (this == thatOne) return true;
        if (!(thatOne instanceof Locatable that)) return false;
        if (!(this.world().equals(that.world()))) return false;

        if (Double.doubleToLongBits(this.x()) != Double.doubleToLongBits(that.x())) {
            return false;
        }
        if (Double.doubleToLongBits(this.y()) != Double.doubleToLongBits(that.y())) {
            return false;
        }
        if (Double.doubleToLongBits(this.z()) != Double.doubleToLongBits(that.z())) {
            return false;
        }

        return true;
    }

    default int makeHash() {
        int hash = 3;

        hash = 19 * hash + this.world().hashCode();
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(x()));
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(y()));
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(z()));
        return hash;
    }

    default String toStringForm() {
        return getType()+
                "{" +
                "world=" + world() +
                ", x=" + x() +
                ", y=" + y() +
                ", z=" + z() +
                '}';
    }

    enum LocatableType {
        ThreadSafe,
        NettyEntity,
        NettyTileEntity,
        MutableBlockVector,
        ImmutableBlockLocation,
        Immutable,
        Mutable,
        // implementations from other modules/projects should use the following
        ExternalMutable,
        ExternalImmutable,
        ExternalMutableBlock,
        ExternalImmutableBlock,
    }

    static Locatable convertLocatable(Locatable from, LocatableType to, boolean clone) {
        switch (to) {
            case ThreadSafe -> {
                if ((from instanceof ThreadSafeLocatable) && !clone) return from;
                return new ThreadSafeLocatable(from.world(), from.x(), from.y(), from.z());
            }
            case MutableBlockVector -> {
                if ((from instanceof MutableBlockVector) && !clone) return from;
                return new MutableBlockVector(from.world(), from.x(), from.y(), from.z());
            }
            case ImmutableBlockLocation -> {
                if ((from instanceof ImmutableBlockLocatable) && !clone) return from;
                return new ImmutableBlockLocatable(from.world(), from.x(), from.y(), from.z());
            }
            case Mutable -> {
                if ((from instanceof MutableLocatableImpl) && !clone) return from;
                return new MutableLocatableImpl(from.world(), from.x(), from.y(), from.z());
            }
            default -> {
                return new MutableLocatableImpl(from.world(), from.x(), from.y(), from.z());
            }
        }
    }

    static Locatable copyOf(Locatable locatable) {
        return convertLocatable(locatable, locatable.getType(), true);
    }

    static Locatable create(UUID world, double x, double y, double z, LocatableType type) {
        switch (type) {
            case ThreadSafe -> {
                return new ThreadSafeLocatable(world, x, y, z);
            }
            case MutableBlockVector, ExternalMutableBlock -> {
                return new MutableBlockVector(world, x, y, z);
            }
            case Mutable, ExternalMutable -> {
                return new MutableLocatableImpl(world, x, y, z);
            }
            case ImmutableBlockLocation, ExternalImmutableBlock -> {
                return new ImmutableBlockLocatable(world, x, y, z);
            }
            case Immutable, ExternalImmutable -> {
                return new ImmutableLocatableImpl(world, x, y, z);
            }

        }
        return null;
    }

    static <T extends Locatable> T create(UUID world, double x, double y, double z, Class<T> type) {
        if (type == ThreadSafeLocatable.class) {
            return (T) new ThreadSafeLocatable(world, x, y, z);
        } else if (type == MutableBlockVector.class) {
            return (T) new MutableBlockVector(world, x, y, z);
        } else if (type == ImmutableBlockLocatable.class) {
            return (T) new ImmutableBlockLocatable(world, x, y, z);
        } else if (type == MutableLocatableImpl.class) {
            return (T) new MutableLocatableImpl(world, x, y, z);
        } else if (type == ImmutableLocatableImpl.class) {
            return (T) new ImmutableLocatableImpl(world, x, y, z);
        } else {
            throw new IllegalArgumentException("Unsupported Locatable type: " + type.getName());
        }
    }

    static Locatable create(UUID world, double x, double y, double z) {
        return create(world, x, y, z, LocatableType.Mutable);
    }
}
