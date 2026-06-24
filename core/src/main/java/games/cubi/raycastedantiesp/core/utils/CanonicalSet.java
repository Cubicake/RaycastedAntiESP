package games.cubi.raycastedantiesp.core.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * A Set-like data structure that stores canonical instances of key values.
 * Any compatible key type with {@code equals} and {@code hashcode} implemented identically to the value type may be used for lookup,
 * but only canonical values of type {@code V} may be stored.
 *
 * @param <K> key type used for lookup
 * @param <V> stored canonical value type
 */
public interface CanonicalSet<K, V extends K> {
    int size();

    boolean isEmpty();

    boolean contains(K keyValue);

    @Nullable V get(K keyValue);

    @Nullable V add(V keyValue);

    void addAll(Set<? extends V> keyValues);

    @Nullable V computeIfAbsent(K keyValue, @NotNull Function<? super K, ? extends V> mappingFunction);

    @Nullable V remove(K keyValue);

    void clear();

    @NotNull Set<V> keySet();

    @NotNull Collection<V> values();
}