package games.cubi.locatables.implementations;

import games.cubi.locatables.Locatable;
import games.cubi.locatables.MutableLocatable;

import java.util.UUID;
import java.util.concurrent.locks.StampedLock;

public class ThreadSafeLocatable implements MutableLocatable {
    private volatile UUID world;

    private double x, y, z;

    private final StampedLock lock = new StampedLock();

    public ThreadSafeLocatable(UUID world, double x, double y, double z) {
        this.world = world;
        this.x = x; this.y = y; this.z = z;
    }

    /**
     * Runs a write operation on x, y, z fields under a write lock.
     */
    private void withWriteLock(Runnable body) {
        long stamp = lock.writeLock();
        try {
            body.run();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Locatable that)) return false;

        long stamp = lock.tryOptimisticRead();

        UUID thatWorld = that.world();
        double[] thatPos = that.getAtomicPositionArray();

        boolean equal = this.world.equals(thatWorld) &&
                    Double.doubleToLongBits(this.x) == Double.doubleToLongBits(thatPos[0]) &&
                    Double.doubleToLongBits(this.y) == Double.doubleToLongBits(thatPos[1]) &&
                    Double.doubleToLongBits(this.z) == Double.doubleToLongBits(thatPos[2]);
        if (!lock.validate(stamp)) {
            // If the lock was invalid, we can't trust the result, so we have to retry with locks again
            stamp = lock.readLock();
            thatWorld = that.world();
            thatPos = that.getAtomicPositionArray();
            try {
                equal = this.world.equals(thatWorld) &&
                        Double.doubleToLongBits(this.x) == Double.doubleToLongBits(thatPos[0]) &&
                        Double.doubleToLongBits(this.y) == Double.doubleToLongBits(thatPos[1]) &&
                        Double.doubleToLongBits(this.z) == Double.doubleToLongBits(thatPos[2]);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return equal;
    }

    @Override
    public int hashCode() {
        long stamp = lock.tryOptimisticRead();
        int hash = 3;
        // while we could just grab the values, release the lock, and then calculate the hash, that would marginally increase ram use for a marginal performance gain and this looks neater
        hash = 19 * hash + this.world.hashCode();
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.x));
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.y));
        hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.z));
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                hash = 3;
                hash = 19 * hash + this.world.hashCode();
                hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.x));
                hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.y));
                hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.z));
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return hash;
    }

    @Override
    public String toString() {
        // It's a string, who cares if it's not thread safe?
        return toStringForm();
    }

    @Override
    public LocatableType getType() {
        return LocatableType.ThreadSafe;
    }

    @Override
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    @Override
    public double lengthSquared() {
        long stamp = lock.tryOptimisticRead();
        double result = x*x + y*y + z*z;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = x*x + y*y + z*z;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * @return Normalised internal vectors, may busy-wait if write access is locked
     */
    @Override
    public MutableLocatable normalize() {
        withWriteLock(() -> {
            double len = Math.sqrt(x*x + y*y + z*z);
            x /= len; y /= len; z /= len;
        });
        return this;
    }

    @Override
    public MutableLocatable add(Locatable locatable) {
        double[] otherPosition = locatable.getAtomicPositionArray();
        withWriteLock(() -> {
            x += otherPosition[0];
            y += otherPosition[1];
            z += otherPosition[2];
        });
        return this;
    }

    @Override
    public MutableLocatable add(double x, double y, double z) {
        withWriteLock(() -> { this.x += x; this.y += y; this.z += z; });
        return this;
    }

    @Override
    public MutableLocatable subtract(Locatable locatable) {
        double[] otherPosition = locatable.getAtomicPositionArray();
        withWriteLock(() -> {
            x -= otherPosition[0];
            y -= otherPosition[1];
            z -= otherPosition[2];
        });
        return this;
    }

    @Override
    public MutableLocatable scalarMultiply(double factor) {
        withWriteLock(() -> { x *= factor; y *= factor; z *= factor; });
        return this;
    }

    @Override
    public UUID world() {
        long stamp = lock.tryOptimisticRead();
        UUID result = world;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = world;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public int blockX() {
        return (int) Math.floor(x());
    }

    @Override
    public int blockY() {
        return (int) Math.floor(y());
    }

    @Override
    public int blockZ() {
        return (int) Math.floor(z());
    }

    @Override
    public double x() {
        long stamp = lock.tryOptimisticRead();
        double result = x;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = x;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public double y() {
        long stamp = lock.tryOptimisticRead();
        double result = y;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public double z() {
        long stamp = lock.tryOptimisticRead();
        double result = z;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = z;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public double[] getAtomicPositionArray() {
        long stamp = lock.tryOptimisticRead();
        double[] result = new double[]{x, y, z};
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = new double[]{x, y, z};
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public MutableLocatable setX(double x) {
        withWriteLock(() -> this.x = x);
        return this;
    }

    @Override
    public MutableLocatable setY(double y) {
        withWriteLock(() -> this.y = y);
        return this;
    }

    @Override
    public MutableLocatable setZ(double z) {
        withWriteLock(() -> this.z = z);
        return this;
    }

    @Override
    public MutableLocatable setX(int x) {
        withWriteLock(() -> this.x = x);
        return this;
    }

    @Override
    public MutableLocatable setY(int y) {
        withWriteLock(() -> this.y = y);
        return this;
    }

    @Override
    public MutableLocatable setZ(int z) {
        withWriteLock(() -> this.z = z);
        return this;
    }

    @Override
    public MutableLocatable setWorld(UUID world) {
        this.world = world;
        return this;
    }

    @Override
    public MutableLocatable set(Locatable locatable) {
        double[] otherPosition = locatable.getAtomicPositionArray();
        withWriteLock(() -> {
            this.world = locatable.world(); // technically this could be racy, but the world shouldn't be changing often
            this.x = otherPosition[0];
            this.y = otherPosition[1];
            this.z = otherPosition[2];
        });
        return this;
    }

    @Override
    public MutableLocatable set(double x, double y, double z, UUID world) {
        withWriteLock(() -> {
            this.world = world;
            this.x = x; this.y = y; this.z = z;
        });
        return this;
    }

    @Override
    public MutableLocatable set(double x, double y, double z) {
        withWriteLock(() -> {
            this.x = x; this.y = y; this.z = z;
        });
        return this;
    }

    @Override
    public MutableLocatable set(int x, int y, int z) {
        withWriteLock(() -> {
            this.x = x; this.y = y; this.z = z;
        });
        return this;
    }

    @Override
    public boolean strictlyEquals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThreadSafeLocatable that)) return false;

        long thisStamp = lock.tryOptimisticRead();
        long thatStamp = that.lock.tryOptimisticRead();
        boolean equal = this.world.equals(that.world) &&
                    Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
                    Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y) &&
                    Double.doubleToLongBits(this.z) == Double.doubleToLongBits(that.z);
        if (!lock.validate(thisStamp) || !that.lock.validate(thatStamp)) {
            // If either lock was invalid, we can't trust the result, so we have to retry with locks again
            thisStamp = lock.readLock();
            thatStamp = that.lock.readLock();
            try {
                equal = this.world.equals(that.world) &&
                        Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
                        Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y) &&
                        Double.doubleToLongBits(this.z) == Double.doubleToLongBits(that.z);
            } finally {
                lock.unlockRead(thisStamp);
                that.lock.unlockRead(thatStamp);
            }
        }
        return equal;
    }
}

