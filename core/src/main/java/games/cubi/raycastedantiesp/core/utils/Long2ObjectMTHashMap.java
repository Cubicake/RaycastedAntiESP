package games.cubi.raycastedantiesp.core.utils;


import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectCollection;

import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.LongFunction;

/**
 * A type-specific hash map with a fast, small-footprint implementation. The map is thread-safe with a backing StampedLock.
 *
 * @param <V> the type of mapped values
 */
public class Long2ObjectMTHashMap<V> extends Long2ObjectOpenHashMap<V> {
    private final StampedLock lock = new StampedLock();

    public Long2ObjectMTHashMap(final int expected, final float f) {
        super(expected, f);
    }

    /**
     * Creates a new hash map with {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
     *
     * @param expected the expected number of elements in the hash map.
     */
    public Long2ObjectMTHashMap(final int expected) {
        this(expected, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new hash map with initial expected {@link Hash#DEFAULT_INITIAL_SIZE} entries and
     * {@link Hash#DEFAULT_LOAD_FACTOR} as load factor.
     */
    public Long2ObjectMTHashMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    @Override
    public V get(long k) {
        long stamp = lock.tryOptimisticRead();
        V v = super.get(k);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                v = super.get(k);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return v;
    }

    @Override
    public V getOrDefault(long k, V defaultValue) {
        long stamp = lock.tryOptimisticRead();
        V v = super.getOrDefault(k, defaultValue);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                v = super.getOrDefault(k, defaultValue);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return v;
    }

    @Override
    public boolean containsKey(long k) {
        long stamp = lock.tryOptimisticRead();
        boolean contains = super.containsKey(k);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                contains = super.containsKey(k);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return contains;
    }

    @Override
    public boolean containsValue(Object v) {
        long stamp = lock.tryOptimisticRead();
        boolean contains = super.containsValue(v);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                contains = super.containsValue(v);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return contains;
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int size = super.size();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                size = super.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean empty = super.isEmpty();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                empty = super.isEmpty();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return empty;
    }

    @Override
    public void forEach(LongObjectBiConsumer<? super V> consumer) {
        long stamp = lock.tryOptimisticRead();
        super.forEach(consumer);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                super.forEach(consumer);
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }

    @Override
    public FastEntrySet<V> long2ObjectEntrySet() {
        long stamp = lock.tryOptimisticRead();
        FastEntrySet<V> entrySet = super.long2ObjectEntrySet();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                entrySet = super.long2ObjectEntrySet();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return entrySet;
    }

    @Override
    public LongSet keySet() {
        long stamp = lock.tryOptimisticRead();
        LongSet keySet = super.keySet();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                keySet = super.keySet();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return keySet;
    }

    @Override
    public ObjectCollection<V> values() {
        long stamp = lock.tryOptimisticRead();
        ObjectCollection<V> values = super.values();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                values = super.values();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return values;
    }

    @Override
    public int hashCode() {
        long stamp = lock.tryOptimisticRead();
        int hash = super.hashCode();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                hash = super.hashCode();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return hash;
    }

    // Write operations

    @Override
    public V put(long k, V v) {
        long stamp = lock.writeLock();
        try {
            return super.put(k, v);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void putAll(Map<? extends Long, ? extends V> m) {
        long stamp = lock.writeLock();
        try {
            super.putAll(m);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V remove(long k) {
        long stamp = lock.writeLock();
        try {
            return super.remove(k);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean remove(long k, Object v) {
        long stamp = lock.writeLock();
        try {
            return super.remove(k, v);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V putIfAbsent(long k, V v) {
        long stamp = lock.writeLock();
        try {
            return super.putIfAbsent(k, v);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean replace(long k, V oldValue, V v) {
        long stamp = lock.writeLock();
        try {
            return super.replace(k, oldValue, v);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V replace(long k, V v) {
        long stamp = lock.writeLock();
        try {
            return super.replace(k, v);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V computeIfAbsent(long k, LongFunction<? extends V> mappingFunction) {
        long stamp = lock.writeLock();
        try {
            return super.computeIfAbsent(k, mappingFunction);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V computeIfAbsent(long key, Long2ObjectFunction<? extends V> mappingFunction) {
        long stamp = lock.writeLock();
        try {
            return super.computeIfAbsent(key, mappingFunction);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V computeIfPresent(long k, BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        long stamp = lock.writeLock();
        try {
            return super.computeIfPresent(k, remappingFunction);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V compute(long k, BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        long stamp = lock.writeLock();
        try {
            return super.compute(k, remappingFunction);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V merge(long k, V v, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        long stamp = lock.writeLock();
        try {
            return super.merge(k, v, remappingFunction);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void clear() {
        long stamp = lock.writeLock();
        try {
            super.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}