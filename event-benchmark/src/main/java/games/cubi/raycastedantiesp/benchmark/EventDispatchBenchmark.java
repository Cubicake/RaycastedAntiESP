/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.benchmark;

import games.cubi.utils.events.CancellableEventRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Level;

@State(Scope.Benchmark)
public class EventDispatchBenchmark {
    private static final CancellableEventRegistry.Order[] CUBI_PRIORITIES = {
            CancellableEventRegistry.Order.EARLY,
            CancellableEventRegistry.Order.NORMAL,
            CancellableEventRegistry.Order.LATE,
            CancellableEventRegistry.Order.MONITOR,
    };
    private static final EventPriority[] BUKKIT_PRIORITIES = {
            EventPriority.LOWEST,
            EventPriority.NORMAL,
            EventPriority.HIGHEST,
            EventPriority.MONITOR,
    };

    @Param({"0", "1", "4", "16", "32", "64"})
    public int listenerCount;

    private CancellableEventRegistry<CubiBenchmarkEvent> cubiRegistry;
    private CubiBenchmarkEvent cubiEvent;
    private BukkitBenchmarkEvent bukkitEvent;

    @Setup(Level.Trial)
    public void setup() {
        EventBenchmarkPlugin plugin = EventBenchmarkPlugin.instance();
        BukkitBenchmarkEvent.getHandlerList().unregister(plugin);

        cubiRegistry = new CancellableEventRegistry<>();
        cubiEvent = new CubiBenchmarkEvent();
        bukkitEvent = new BukkitBenchmarkEvent();

        for (int index = 0; index < listenerCount; index++) {
            CancellableEventRegistry.Order cubiPriority = listenerCount < 16
                    ? CancellableEventRegistry.Order.NORMAL
                    : CUBI_PRIORITIES[index % CUBI_PRIORITIES.length];
            EventPriority bukkitPriority = listenerCount < 16
                    ? EventPriority.NORMAL
                    : BUKKIT_PRIORITIES[index % BUKKIT_PRIORITIES.length];

            cubiRegistry.register(cubiPriority, CubiBenchmarkEvent::recordHandling);
            Bukkit.getPluginManager().registerEvent(
                    BukkitBenchmarkEvent.class,
                    new Listener() {},
                    bukkitPriority,
                    (ignored, event) -> ((BukkitBenchmarkEvent) event).recordHandling(),
                    plugin,
                    true
            );
        }

        BukkitBenchmarkEvent.getHandlerList().bake();
        verifySetup();
    }

    private void verifySetup() {
        int registeredBukkitListeners = BukkitBenchmarkEvent.getHandlerList().getRegisteredListeners().length;
        if (registeredBukkitListeners != listenerCount) {
            throw new IllegalStateException("Expected " + listenerCount + " Bukkit listeners, found " + registeredBukkitListeners);
        }
        if (!cubiRegistry.dispatch(cubiEvent) || !bukkitEvent.callEvent()) {
            throw new IllegalStateException("An uncancelled verification dispatch returned cancelled");
        }
        if (cubiEvent.handledCount() != listenerCount || bukkitEvent.handledCount() != listenerCount) {
            throw new IllegalStateException(
                    "Verification dispatch count mismatch: cubi=" + cubiEvent.handledCount()
                            + ", bukkit=" + bukkitEvent.handledCount()
                            + ", expected=" + listenerCount
            );
        }
        cubiEvent.resetHandledCount();
        bukkitEvent.resetHandledCount();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        BukkitBenchmarkEvent.getHandlerList().unregister(EventBenchmarkPlugin.instance());
        BukkitBenchmarkEvent.getHandlerList().bake();
    }

    @Benchmark
    public long cubi() {
        boolean dispatched = cubiRegistry.dispatch(cubiEvent);
        long handled = cubiEvent.handledCount();
        return dispatched ? handled : ~handled;
    }

    @Benchmark
    public long bukkit() {/*
        boolean dispatched = bukkitEvent.callEvent();
        long handled = bukkitEvent.handledCount();
        return dispatched ? handled : ~handled;
        */
        return 0;
    }

    public static final class CubiBenchmarkEvent extends games.cubi.utils.events.CancellableEvent {
        private long handledCount;

        void recordHandling() {
            handledCount++;
        }

        long handledCount() {
            return handledCount;
        }

        void resetHandledCount() {
            handledCount = 0L;
        }
    }

    public static final class BukkitBenchmarkEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();

        private boolean cancelled;
        private long handledCount;

        BukkitBenchmarkEvent() {
            super(true);
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        void recordHandling() {
            handledCount++;
        }

        long handledCount() {
            return handledCount;
        }

        void resetHandledCount() {
            handledCount = 0L;
        }
    }
}
