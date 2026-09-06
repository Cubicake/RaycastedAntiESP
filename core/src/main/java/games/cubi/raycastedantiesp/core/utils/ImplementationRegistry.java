/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import games.cubi.utils.events.CubiKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ImplementationRegistry<I> {
    private final ConcurrentHashMap<CubiKey, Supplier<I>> implementations;
    private volatile boolean frozen = false;

    public ImplementationRegistry() {
        implementations = new ConcurrentHashMap<>();
    }

    public ImplementationRegistry(int initialCapacity, float loadFactor) {
        implementations = new ConcurrentHashMap<>(initialCapacity,loadFactor);
    }

    /**
     * Attempts to register the provided supplier under the provided key. An exception is thrown
     * if the supplier is registered after the registry is frozen. An exception being thrown
     * suggests but does not guarantee that your implementation was not registered.
     */
    public void register(CubiKey key, Supplier<I> supplier) {
        if (frozen) throw new IllegalStateException("Cannot register implementation "+key.toString()+" after registry was frozen");
        implementations.put(key, supplier);
        if (frozen) throw new IllegalStateException("Implementation "+key.toString()+" registered after registry was frozen");
    }

    /**
     * Attempts to register the provided supplier under the provided key.
     * @return true if the supplier was registered before the registry was frozen
     */
    public boolean registerSilently(CubiKey key, Supplier<I> supplier) {
        if (frozen) return false;
        implementations.put(key, supplier);
        return !frozen;
    }

    /**
     * Registers the provided supplier under the provided key,
     * even if the registry has already been frozen.
     * <p>
     * Do not use this unless you are deliberately adding implementations
     * at runtime, as it will fail to inform you that you missed
     * the correct lifecycle stage to register an implementation.
     * </p>
     */
    public void forceRegister(CubiKey key, Supplier<I> supplier) {
        implementations.put(key, supplier);
    }

    /**
     * Marks that the implementation may already have been chosen, and
     * future registrations may not be detected.
     */
    public void freeze() {
        frozen = true;
    }

    public I getFrom(CubiKey key) {
        return implementations.get(key).get();
    }
}
