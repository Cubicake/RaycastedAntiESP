package games.cubi.raycastedantiesp.core.utils.fastutils;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Long2ObjectMap<V> {
    /**
     * An entry set providing fast iteration.
     *
     * <p>
     * In some cases (e.g., hash-based classes) iteration over an entry set requires the creation of a
     * large number of {@link java.util.Map.Entry} objects. Some {@code fastutil} maps might return
     * {@linkplain Map#entrySet() entry set} objects of type {@code FastEntrySet}: in this case,
     * {@link #fastIterator() fastIterator()} will return an iterator that is guaranteed not to create a
     * large number of objects, <em>possibly by returning always the same entry</em> (of course,
     * mutated), and {@link #fastForEach(Consumer)} will apply the provided consumer to all elements of
     * the entry set, <em>which might be represented always by the same entry</em> (of course, mutated).
     */
    interface FastEntrySet<V> extends ObjectSet<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>> {
        /**
         * Returns a fast iterator over this entry set; the iterator might return always the same entry
         * instance, suitably mutated.
         *
         * @return a fast iterator over this entry set; the iterator might return always the same
         *         {@link java.util.Map.Entry} instance, suitably mutated.
         */
        ObjectIterator<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>> fastIterator();

        /**
         * Iterates quickly over this entry set; the iteration might happen always on the same entry
         * instance, suitably mutated.
         *
         * <p>
         * This default implementation just delegates to {@link #forEach(Consumer)}.
         *
         * @param consumer a consumer that will by applied to the entries of this set; the entries might be
         *            represented by the same entry instance, suitably mutated.
         * @since 8.1.0
         */
        default void fastForEach(final Consumer<? super it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>> consumer) {
            forEach(consumer);
        }
    }

    /**
     * Returns the number of key/value mappings in this map. If the map contains more than
     * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
     *
     * @return the number of key-value mappings in this map.
     * @see it.unimi.dsi.fastutil.Size64
     */
    @Override
    int size();

    /**
     * Removes all of the mappings from this map (optional operation). The map will be empty after this
     * call returns.
     *
     * @throws UnsupportedOperationException if the {@link #clear()} operation is not supported by this
     *             map
     */
    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the default return value (optional operation).
     *
     * This value must be returned by type-specific versions of {@code get()}, {@code put()} and
     * {@code remove()} to denote that the map does not contain the specified key. It must be
     * {@code null} by default.
     *
     * <p>
     * <strong>Warning</strong>: Changing this to a non-null value can have unforeseen consequences.
     * Especially, it breaks compatibility with the specifications of Java's {@link java.util.Map}
     * interface. It has to be used with great care and thorough study of all method comments is
     * recommended.
     *
     * @param rv the new default return value.
     * @see #defaultReturnValue()
     */
    @Override
    void defaultReturnValue(V rv);

    /**
     * Gets the default return value.
     *
     * @return the current default return value.
     */
    @Override
    V defaultReturnValue();

    /**
     * Returns a type-specific set view of the mappings contained in this map.
     *
     * <p>
     * This method is necessary because there is no inheritance along type parameters: it is thus
     * impossible to strengthen {@link Map#entrySet()} so that it returns an
     * {@link it.unimi.dsi.fastutil.objects.ObjectSet} of type-specific entries (the latter makes it
     * possible to access keys and values with type-specific methods).
     *
     * @return a type-specific set view of the mappings contained in this map.
     * @see Map#entrySet()
     */
    ObjectSet<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>> long2ObjectEntrySet();

    /**
     * Returns a set view of the mappings contained in this map.
     *
     * @apiNote Note that this specification strengthens the one given in {@link Map#entrySet()}.
     *
     * @return a set view of the mappings contained in this map.
     * @see Map#entrySet()
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    default ObjectSet<Map.Entry<Long, V>> entrySet() {
        return (ObjectSet)long2ObjectEntrySet();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation just delegates to the corresponding
     * type-specific&ndash;{@linkplain it.unimi.dsi.fastutil.Function function} method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    default V put(final Long key, final V value) {
        return Long2ObjectFunction.super.put(key, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation just delegates to the corresponding
     * type-specific&ndash;{@linkplain it.unimi.dsi.fastutil.Function function} method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    default V get(final Object key) {
        return Long2ObjectFunction.super.get(key);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation just delegates to the corresponding
     * type-specific&ndash;{@linkplain it.unimi.dsi.fastutil.Function function} method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    default V remove(final Object key) {
        return Long2ObjectFunction.super.remove(key);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote Note that this specification strengthens the one given in {@link Map#keySet()}.
     * @return a set view of the keys contained in this map.
     * @see Map#keySet()
     */
    @Override
    LongSet keySet();

    /**
     * {@inheritDoc}
     *
     * @apiNote Note that this specification strengthens the one given in {@link Map#values()}.
     * @return a set view of the values contained in this map.
     * @see Map#values()
     */
    @Override
    ObjectCollection<V> values();

    /**
     * Returns true if this function contains a mapping for the specified key.
     *
     * @param key the key.
     * @return true if this function associates a value to {@code key}.
     * @see Map#containsKey(Object)
     */
    @Override
    boolean containsKey(long key);

    /**
     * Returns true if this function contains a mapping for the specified key.
     * <p>
     * This default implementation just delegates to the corresponding
     * type-specific&ndash;{@linkplain it.unimi.dsi.fastutil.Function function} method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    default boolean containsKey(final Object key) {
        return Long2ObjectFunction.super.containsKey(key);
    }

    // Defaultable methods
    /**
     * Performs the given action for each entry in this map until all entries have been processed or the
     * action throws an exception.
     *
     * <p>
     * This default implementation just delegates to {@code forEach} on the type-specific entry set,
     * taking care of using {@code fastForEach} if available.
     *
     * @see Map#forEach(java.util.function.BiConsumer)
     */
    default void forEach(final it.unimi.dsi.fastutil.longs.LongObjectBiConsumer<? super V> consumer) {
        final ObjectSet<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>> entrySet = long2ObjectEntrySet();
        final Consumer<it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>> wrappingConsumer = (entry) -> consumer.accept(entry.getLongKey(), entry.getValue());
        if (entrySet instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap.FastEntrySet) {
            ((it.unimi.dsi.fastutil.longs.Long2ObjectMap.FastEntrySet<V>)entrySet).fastForEach(wrappingConsumer);
        } else {
            entrySet.forEach(wrappingConsumer);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation just delegates to the corresponding {@link Map} method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    default void forEach(final BiConsumer<? super Long, ? super V> action) {
        Map.super.forEach(action);
    }

    /**
     * Returns the value to which the specified key is mapped, or the {@code defaultValue} if this map
     * contains no mapping for the key.
     *
     * @param key the key.
     * @param defaultValue the default mapping of the key.
     *
     * @return the value to which the specified key is mapped, or the {@code defaultValue} if this map
     *         contains no mapping for the key.
     *
     * @see java.util.Map#getOrDefault(Object, Object)
     * @since 8.0.0
     */
    @Override
    default V getOrDefault(final long key, final V defaultValue) {
        final V v;
        return ((v = get(key)) != defaultReturnValue() || containsKey(key)) ? v : defaultValue;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation just delegates to the corresponding {@link Map} method.
     *
     * @deprecated Please use the corresponding type-specific method instead.
     */
    @Deprecated
    @Override
    default V getOrDefault(final Object key, final V defaultValue) {
        return Map.super.getOrDefault(key, defaultValue);
    }

    /**
     * If the specified key is not already associated with a value, associates it with the given value
     * and returns the {@linkplain #defaultReturnValue() default return value}, else returns the current
     * value.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     *
     * @return the previous value associated with the specified key, or the
     *         {@linkplain #defaultReturnValue() default return value} if there was no mapping for the
     *         key.
     *
     * @see java.util.Map#putIfAbsent(Object, Object)
     * @since 8.0.0
     */
    default V putIfAbsent(final long key, final V value) {
        final V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        put(key, value);
        return drv;
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to the specified value.
     *
     * @param key key with which the specified value is associated.
     * @param value value expected to be associated with the specified key.
     *
     * @return {@code true} if the value was removed.
     *
     * @see java.util.Map#remove(Object, Object)
     * @since 8.0.0
     */
    default boolean remove(final long key, final Object value) {
        final V curValue = get(key);
        if (!java.util.Objects.equals(curValue, value) || (curValue == defaultReturnValue() && !containsKey(key))) return false;
        remove(key);
        return true;
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param key key with which the specified value is associated.
     * @param oldValue value expected to be associated with the specified key.
     * @param newValue value to be associated with the specified key.
     *
     * @return {@code true} if the value was replaced.
     *
     * @see java.util.Map#replace(Object, Object, Object)
     * @since 8.0.0
     */
    default boolean replace(final long key, final V oldValue, final V newValue) {
        final V curValue = get(key);
        if (!java.util.Objects.equals(curValue, oldValue) || (curValue == defaultReturnValue() && !containsKey(key))) return false;
        put(key, newValue);
        return true;
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     *
     * @param key key with which the specified value is associated.
     * @param value value to be associated with the specified key.
     *
     * @return the previous value associated with the specified key, or the
     *         {@linkplain #defaultReturnValue() default return value} if there was no mapping for the
     *         key.
     *
     * @see java.util.Map#replace(Object, Object)
     * @since 8.0.0
     */
    default V replace(final long key, final V value) {
        return containsKey(key) ? put(key, value) : defaultReturnValue();
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute its value using
     * the given mapping function and enters it into this map.
     *
     * <p>
     * Note that contrarily to the default
     * {@linkplain java.util.Map#computeIfAbsent(Object, java.util.function.Function)
     * computeIfAbsent()}, it is not possible to not add a value for a given key, since the
     * {@code mappingFunction} cannot return {@code null}. If such a behavior is needed, please use the
     * corresponding <em>nullable</em> version.
     *
     * @apiNote all {@code computeIfAbsent()} methods have a different logic based on the argument; no
     *          delegation is performed, contrarily to other superficially similar methods such as
     *          {@link java.util.Iterator#forEachRemaining} or {@link java.util.List#replaceAll}.
     *
     * @param key key with which the specified value is to be associated.
     * @param mappingFunction the function to compute a value.
     *
     * @return the current (existing or computed) value associated with the specified key.
     *
     * @see java.util.Map#computeIfAbsent(Object, java.util.function.Function)
     * @since 8.0.0
     */
    default V computeIfAbsent(final long key, final java.util.function.LongFunction<? extends V> mappingFunction) {
        java.util.Objects.requireNonNull(mappingFunction);
        final V v = get(key);
        if (v != defaultReturnValue() || containsKey(key)) return v;
        V newValue = mappingFunction.apply(key);
        put(key, newValue);
        return newValue;
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute its value using
     * the given mapping function and enters it into this map, unless the key is not present in the
     * given mapping function.
     *
     * <p>
     * This version of {@linkplain java.util.Map#computeIfAbsent(Object, java.util.function.Function)
     * computeIfAbsent()} uses a type-specific version of {@code fastutil}'s
     * {@link it.unimi.dsi.fastutil.Function Function}. Since {@link it.unimi.dsi.fastutil.Function
     * Function} has a {@link it.unimi.dsi.fastutil.Function#containsKey(Object) containsKey()} method,
     * it is possible to avoid adding a key by having {@code containsKey()} return {@code false} for
     * that key.
     *
     * @apiNote all {@code computeIfAbsent()} methods have a different logic based on the argument; no
     *          delegation is performed, contrarily to other superficially similar methods such as
     *          {@link java.util.Iterator#forEachRemaining} or {@link java.util.List#replaceAll}.
     *
     * @param key key with which the specified value is to be associated.
     * @param mappingFunction the function to compute a value.
     *
     * @return the current (existing or computed) value associated with the specified key.
     *
     * @see java.util.Map#computeIfAbsent(Object, java.util.function.Function)
     * @since 8.0.0
     */
    default V computeIfAbsent(final long key, final Long2ObjectFunction<? extends V> mappingFunction) {
        java.util.Objects.requireNonNull(mappingFunction);
        final V v = get(key), drv = defaultReturnValue();
        if (v != drv || containsKey(key)) return v;
        if (!mappingFunction.containsKey(key)) return drv;
        V newValue = mappingFunction.get(key);
        put(key, newValue);
        return newValue;
    }

    /**
     * @deprecated Please use {@code computeIfAbsent()} instead.
     */
    @Deprecated
    default V computeIfAbsentPartial(final long key, final Long2ObjectFunction<? extends V> mappingFunction) {
        return computeIfAbsent(key, mappingFunction);
    }

    /**
     * If the value for the specified key is present, attempts to compute a new mapping given the key
     * and its current mapped value.
     *
     * @apiNote The JDK specification for this method equates not being associated with a value with
     *          being associated with {code null}. This is not the case for this method.
     *
     * @param key key with which the specified value is to be associated.
     * @param remappingFunction the function to compute a value.
     *
     * @return the new value associated with the specified key, or the {@linkplain #defaultReturnValue()
     *         default return value} if none.
     *
     * @see java.util.Map#computeIfPresent(Object, java.util.function.BiFunction)
     * @since 8.0.0
     */
    default V computeIfPresent(final long key, final java.util.function.BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        java.util.Objects.requireNonNull(remappingFunction);
        final V oldValue = get(key), drv = defaultReturnValue();
        if (oldValue == drv && !containsKey(key)) return drv;
        final V newValue = remappingFunction.apply(Long.valueOf(key), (oldValue));
        if (newValue == null) {
            remove(key);
            return drv;
        }
        put(key, newValue);
        return newValue;
    }

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value (or {@code null}
     * if there is no current mapping).
     *
     * <p>
     * If the function returns {@code null}, the mapping is removed (or remains absent if initially
     * absent). If the function itself throws an (unchecked) exception, the exception is rethrown, and
     * the current mapping is left unchanged.
     *
     * @param key key with which the specified value is to be associated.
     * @param remappingFunction the function to compute a value.
     *
     * @return the new value associated with the specified key, or the {@linkplain #defaultReturnValue()
     *         default return value} if none.
     *
     * @see java.util.Map#compute(Object, java.util.function.BiFunction)
     * @since 8.0.0
     */
    default V compute(final long key, final java.util.function.BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        java.util.Objects.requireNonNull(remappingFunction);
        final V oldValue = get(key), drv = defaultReturnValue();
        final boolean contained = oldValue != drv || containsKey(key);
        final V newValue = remappingFunction.apply(Long.valueOf(key), contained ? (oldValue) : null);
        if (newValue == null) {
            if (contained) remove(key);
            return drv;
        }
        put(key, newValue);
        return newValue;
    }

    /**
     * If the specified key is not already associated with a value, associates it with the given
     * {@code value}. Otherwise, replaces the associated value with the results of the given remapping
     * function, or removes if the result is {@code null}.
     *
     * @apiNote The JDK specification for this method equates not being associated with a value with
     *          being associated with {code null}. This is not the case for this method.
     *
     * @param key key with which the resulting value is to be associated.
     * @param value the value to be merged with the existing value associated with the key or, if no
     *            existing value is associated with the key, to be associated with the key.
     * @param remappingFunction the function to recompute a value if present.
     *
     * @return the new value associated with the specified key, or the {@linkplain #defaultReturnValue()
     *         default return value} if no value is associated with the key.
     *
     * @see java.util.Map#merge(Object, Object, java.util.function.BiFunction)
     * @since 8.0.0
     */
    default V merge(final long key, final V value, final java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        java.util.Objects.requireNonNull(remappingFunction);
        java.util.Objects.requireNonNull(value);
        final V oldValue = get(key), drv = defaultReturnValue();
        final V newValue;
        if (oldValue != drv || containsKey(key)) {
            final V mergedValue = remappingFunction.apply((oldValue), (value));
            if (mergedValue == null) {
                remove(key);
                return drv;
            }
            newValue = (mergedValue);
        } else {
            newValue = value;
        }
        put(key, newValue);
        return newValue;
    }

    /**
     * Returns an unmodifiable, type-specific map with the specified entries. See
     * {@link java.util.Map#ofEntries(Map.Entry[])}
     */
    @SafeVarargs
    @SuppressWarnings({ "varargs", "unchecked" })
    static <V> it.unimi.dsi.fastutil.longs.Long2ObjectMap<V> ofEntries(it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V>... entries) {
        if (entries.length == 0) return Long2ObjectMaps.EMPTY_MAP;
        if (entries.length == 1) return Long2ObjectMaps.singleton(entries[0].getLongKey(), entries[0].getValue());
        if (entries.length <= 8) {
            long[] keys = new long[entries.length];
            V[] vals = (V[])new Object[entries.length];
            for (int i = 0; i < entries.length; ++i) {
                it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V> entry = entries[i];
                keys[i] = entry.getLongKey();
                // Prevent duplicate keys
                for (int j = 0; j < i; ++j) if (((keys[j]) == (keys[i]))) throw new IllegalArgumentException("duplicate key: " + keys[i]);
                vals[i] = entry.getValue();
            }
            return Long2ObjectMaps.unmodifiable(new Long2ObjectArrayMap<>(keys, vals, entries.length));
        } else {
            it.unimi.dsi.fastutil.longs.Long2ObjectMap<V> newMap = new Long2ObjectOpenHashMap<>(entries.length, 0.75f);
            for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V> entry : entries) {
                if (!((newMap.put(entry.getLongKey(), entry.getValue())) == null)) throw new IllegalArgumentException("duplicate key: " + entry.getLongKey());
            }
            return Long2ObjectMaps.unmodifiable(newMap);
        }
    }

    /**
     * Returns an unmodifiable, type-specific entry. See {@link java.util.Map#entry(Object, Object)}
     */
    static <V> it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<V> entry(long key, V value) {
        return new AbstractLong2ObjectMap.BasicEntry<>(key, value);
    }

    /**
     * A type-specific {@link java.util.Map.Entry}; provides some additional methods that use
     * polymorphism to avoid (un)boxing.
     *
     * @see java.util.Map.Entry
     */
    interface Entry<V> extends Map.Entry<Long, V> {
        /**
         * Returns the key corresponding to this entry.
         *
         * @see java.util.Map.Entry#getKey()
         */
        long getLongKey();

        /**
         * {@inheritDoc}
         *
         * @deprecated Please use the corresponding type-specific method instead.
         */
        @Deprecated
        @Override
        default Long getKey() {
            return Long.valueOf(getLongKey());
        }
    }
}
