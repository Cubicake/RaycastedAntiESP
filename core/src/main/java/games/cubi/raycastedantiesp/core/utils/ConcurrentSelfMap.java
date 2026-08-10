/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConcurrentSelfMap<K, V extends K> implements CanonicalSet<K, V> {
    private final ConcurrentHashMap<V, V> backingMap = new ConcurrentHashMap<>();

    public ConcurrentSelfMap() {}

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public boolean contains(K keyValue) {
        return backingMap.containsKey(keyValue);
    }

    @Override
    public @Nullable V get(K keyValue) {
        return backingMap.get(keyValue);
    }

    @Override
    public @Nullable V add(V keyValue) {
        return backingMap.put(keyValue, keyValue);
    }

    @Override
    public void addAll(Set<? extends V> keyValues) {
        for (V keyValue : keyValues) {
            backingMap.put(keyValue, keyValue);
        }
    }

    @Override
    public @Nullable V computeIfAbsent(
            K keyValue,
            @NotNull Function<? super K, ? extends V> mappingFunction
    ) {
        V existing = backingMap.get(keyValue);
        if (existing != null) {
            return existing;
        }

        V canonicalValue = mappingFunction.apply(keyValue);
        if (canonicalValue == null) {
            return null;
        }

        if (!canonicalValue.equals(keyValue)) {
            throw new IllegalArgumentException(
                    "Computed canonical value must be equal to the key value."
            );
        }

        V racedValue = backingMap.putIfAbsent(canonicalValue, canonicalValue);
        return racedValue != null ? racedValue : canonicalValue;
    }

    @Override
    public @Nullable V remove(K keyValue) {
        return backingMap.remove(keyValue);
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public @NotNull Set<V> keySet() {
        return backingMap.keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        return backingMap.values();
    }
}