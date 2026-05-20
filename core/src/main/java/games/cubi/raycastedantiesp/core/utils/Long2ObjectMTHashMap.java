package games.cubi.raycastedantiesp.core.utils;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongObjectBiConsumer;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;

/**
 * A small thread-safe utility map for primitive {@code long} keys.
 *
 * <p>Reads use optimistic locking where possible. Mutations use the write lock. Callback-based
 * methods such as {@link #computeIfAbsent(long, LongFunction)}, {@link #forEach(LongObjectBiConsumer)},
 * and {@link #removeIfKey(LongPredicate)} run their callbacks while holding the write lock, so those
 * callbacks must not call back into this same map instance.
 *
 * <p>This class only synchronizes access to the map structure itself. Stored values are returned by
 * reference and are not made thread-safe by this map.
 *
 * @param <V> the type of mapped values
 */
public class Long2ObjectMTHashMap<V> {
    private final Long2ObjectOpenHashMap<V> backingMap;
    private final StampedLock lock = new StampedLock();
    private final ThreadLocal<Integer> writeLockedCallbackDepth = ThreadLocal.withInitial(() -> 0);

    public Long2ObjectMTHashMap(final int expected, final float loadFactor) {
        this.backingMap = new Long2ObjectOpenHashMap<>(expected, loadFactor);
    }

    public Long2ObjectMTHashMap(final int expected) {
        this(expected, Hash.DEFAULT_LOAD_FACTOR);
    }

    public Long2ObjectMTHashMap() {
        this(Hash.DEFAULT_INITIAL_SIZE, Hash.DEFAULT_LOAD_FACTOR);
    }

    public V get(final long key) {
        long stamp = lock.tryOptimisticRead();
        V value = backingMap.get(key);
        if (!lock.validate(stamp)) {
            assertNoWriteLockedCallbackReentry();
            stamp = lock.readLock();
            try {
                value = backingMap.get(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return value;
    }

    public V getOrDefault(final long key, final V defaultValue) {
        long stamp = lock.tryOptimisticRead();
        V value = getOrDefaultInternal(key, defaultValue);
        if (!lock.validate(stamp)) {
            assertNoWriteLockedCallbackReentry();
            stamp = lock.readLock();
            try {
                value = getOrDefaultInternal(key, defaultValue);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return value;
    }

    public boolean containsKey(final long key) {
        long stamp = lock.tryOptimisticRead();
        boolean contains = backingMap.containsKey(key);
        if (!lock.validate(stamp)) {
            assertNoWriteLockedCallbackReentry();
            stamp = lock.readLock();
            try {
                contains = backingMap.containsKey(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return contains;
    }

    public int size() {
        long stamp = lock.tryOptimisticRead();
        int size = backingMap.size();
        if (!lock.validate(stamp)) {
            assertNoWriteLockedCallbackReentry();
            stamp = lock.readLock();
            try {
                size = backingMap.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return size;
    }

    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean empty = backingMap.isEmpty();
        if (!lock.validate(stamp)) {
            assertNoWriteLockedCallbackReentry();
            stamp = lock.readLock();
            try {
                empty = backingMap.isEmpty();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return empty;
    }

    public V put(final long key, final V value) {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            return backingMap.put(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public V putIfAbsent(final long key, final V value) {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            if (backingMap.containsKey(key)) {
                return backingMap.get(key);
            }
            backingMap.put(key, value);
            return null;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public V replace(final long key, final V value) {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            if (!backingMap.containsKey(key)) {
                return null;
            }
            return backingMap.put(key, value);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public boolean replace(final long key, final V oldValue, final V value) {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            if (!backingMap.containsKey(key) || !Objects.equals(backingMap.get(key), oldValue)) {
                return false;
            }
            backingMap.put(key, value);
            return true;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public V remove(final long key) {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            return backingMap.remove(key);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public boolean remove(final long key, final Object value) {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            if (!backingMap.containsKey(key) || !Objects.equals(backingMap.get(key), value)) {
                return false;
            }
            backingMap.remove(key);
            return true;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void clear() {
        assertNoWriteLockedCallbackReentry();

        long stamp = lock.writeLock();
        try {
            backingMap.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public V computeIfAbsent(final long key, final LongFunction<? extends V> mappingFunction) {
        assertNoWriteLockedCallbackReentry();
        Objects.requireNonNull(mappingFunction, "mappingFunction");

        long stamp = lock.writeLock();
        try {
            if (backingMap.containsKey(key)) {
                return backingMap.get(key);
            }

            V value = invokeWriteLockedCallback(() -> mappingFunction.apply(key));
            backingMap.put(key, value);
            return value;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void forEach(final LongObjectBiConsumer<? super V> consumer) {
        assertNoWriteLockedCallbackReentry();
        Objects.requireNonNull(consumer, "consumer");

        long stamp = lock.writeLock();
        try {
            ObjectIterator<Long2ObjectMap.Entry<V>> iterator = backingMap.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry<V> entry = iterator.next();
                invokeWriteLockedCallback(() -> {
                    consumer.accept(entry.getLongKey(), entry.getValue());
                    return null;
                });
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public int removeIfKey(final LongPredicate predicate) {
        assertNoWriteLockedCallbackReentry();
        Objects.requireNonNull(predicate, "predicate");

        long stamp = lock.writeLock();
        try {
            int removed = 0;
            ObjectIterator<Long2ObjectMap.Entry<V>> iterator = backingMap.long2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                Long2ObjectMap.Entry<V> entry = iterator.next();
                if (invokeWriteLockedCallback(() -> predicate.test(entry.getLongKey()))) {
                    iterator.remove();
                    removed++;
                }
            }
            return removed;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private V getOrDefaultInternal(final long key, final V defaultValue) {
        V value = backingMap.get(key);
        return value != null || backingMap.containsKey(key) ? value : defaultValue;
    }

    private void assertNoWriteLockedCallbackReentry() {
        if (writeLockedCallbackDepth.get() > 0) {
            throw new IllegalStateException(
                    "Callbacks executed under the write lock must not reenter this Long2ObjectMTHashMap instance."
            );
        }
    }

    private <T> T invokeWriteLockedCallback(final Long2ObjectMTHashMapCallback<T> callback) {
        writeLockedCallbackDepth.set(writeLockedCallbackDepth.get() + 1);
        try {
            return callback.invoke();
        } finally {
            int depth = writeLockedCallbackDepth.get() - 1;
            if (depth == 0) {
                writeLockedCallbackDepth.remove();
            } else {
                writeLockedCallbackDepth.set(depth);
            }
        }
    }

    @FunctionalInterface
    private interface Long2ObjectMTHashMapCallback<T> {
        T invoke();
    }
}
