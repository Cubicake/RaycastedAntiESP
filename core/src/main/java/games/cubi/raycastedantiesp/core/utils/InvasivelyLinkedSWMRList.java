/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An intrusive doubly linked node designed for a single-writer,
 * multiple-reader access pattern.
 *
 * <h2>Concurrency model</h2>
 * <ul>
 *     <li>Exactly one writer may modify a list.</li>
 *     <li>Readers may traverse forward concurrently.</li>
 *     <li>Readers must not inspect {@code previousWriterOnly()}.</li>
 *     <li>The writer must not reuse a removed node until all readers that
 *         could still hold that node have quiesced.</li>
 * </ul>
 *
 * <p>Forward links are published with release semantics and read with
 * acquire semantics. The backward link is plain because only the writer
 * may access it.</p>
 *
 * <p>A common layout uses a permanent sentinel node. Data nodes are placed
 * after the sentinel, and the sentinel itself is never unlinked.</p>
 *
 * @param <T> the concrete node type
 */
public abstract class InvasivelyLinkedSWMRList<T extends InvasivelyLinkedSWMRList<T>> implements Iterable<T> {

    private T next; private static final VarHandle NEXT = VarHandler.get(InvasivelyLinkedSWMRList.class, "next", InvasivelyLinkedSWMRList.class);
    private T previous;

    /**
     * Returns this object with its concrete node type.
     *
     * <p>An implementation must return {@code this}:</p>
     *
     * <pre>{@code
     * @Override
     * protected MyNode self() {
     *     return this;
     * }
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    /**
     * Provides access to private fields {@code next} and {@code previous}. They are only accessible from within this class, not subclasses.
     */
    private static <N extends InvasivelyLinkedSWMRList<N>> InvasivelyLinkedSWMRList<N> linksOf(N node) {
        return node;
    }

    /**
     * Returns the next node using acquire semantics.
     *
     * <p>This is the forward-link operation intended for concurrent
     * readers.</p>
     */
    @SuppressWarnings("unchecked")
    public final T nextAcquire() {
        return (T) NEXT.getAcquire(this);
    }

    /**
     * Returns the next node without memory ordering.
     *
     * <p>Only the single writer may call this method.</p>
     */
    private T nextWriterOnly() {
        return next;
    }

    private void setNextRelease(T value) {
        NEXT.setRelease(this, value);
    }

    /**
     * Returns the preceding node.
     *
     * <p>Only the single writer may call this method. Concurrent readers
     * must not use the backward link.</p>
     */
    public final T previousWriterOnly() {
        return previous;
    }

    /**
     * Reports whether this node has a predecessor.
     *
     * <p>Only the single writer may call this method. A permanent sentinel
     * normally has no predecessor and therefore returns {@code false}.</p>
     */
    public final boolean isLinkedWriterOnly() {
        return previous != null;
    }

    /**
     * Reports whether both intrusive links are clear.
     *
     * <p>This does not prove that no reader still holds the node. Node
     * lifetime and reader quiescence must be managed separately.</p>
     */
    public final boolean isDetachedWriterOnly() {
        return previous == null && nextWriterOnly() == null;
    }

    /**
     * Inserts {@code node} directly after this node.
     *
     * <p>Only the single writer may call this method.</p>
     *
     * <p>The inserted node must:</p>
     * <ul>
     *     <li>not be this node;</li>
     *     <li>have both intrusive links clear;</li>
     *     <li>not be visible to any reader;</li>
     *     <li>not belong to another list.</li>
     * </ul>
     *
     * <p>The release store to this node's forward link is the insertion
     * linearisation point. A reader that observes the inserted node through
     * {@link #nextAcquire()} also observes writes made to the node before
     * insertion.</p>
     *
     * @param node detached node to insert
     * @throws NullPointerException if {@code node} is null
     * @throws IllegalArgumentException if asked to insert this node
     * @throws IllegalStateException if {@code node} appears linked
     */
    public final void linkAfter(T node) {
        Objects.requireNonNull(node, "node");

        T self = self();

        if (node == self) {
            throw new IllegalArgumentException("A node cannot be linked after itself");
        }

        InvasivelyLinkedSWMRList<T> nodeLinks = linksOf(node);

        if (nodeLinks.previous != null || nodeLinks.nextWriterOnly() != null) {
            throw new IllegalStateException("The node is already linked or is not detached");
        }

        T oldNext = nextWriterOnly();

        if (node == oldNext) {
            throw new IllegalStateException("The node is already the current successor");
        }

        /*
         * Fully initialise the inserted node before publishing it.
         */
        nodeLinks.previous = self;
        nodeLinks.next = oldNext;

        if (oldNext != null) {
            InvasivelyLinkedSWMRList<T> oldNextLinks = linksOf(oldNext);
            oldNextLinks.previous = node;
        }

        /*
         * Publication and insertion linearisation point.
         */
        setNextRelease(node);
    }

    /**
     * Removes this node from its current list.
     *
     * <p>Only the single writer may call this method.</p>
     *
     * <p>The node must have a predecessor. This rule prevents accidental
     * removal of a permanent sentinel and avoids the need to update an
     * external head reference.</p>
     *
     * <p>A concurrent reader may still:</p>
     * <ul>
     *     <li>return this node after it has been removed;</li>
     *     <li>continue from this node to its former successor;</li>
     *     <li>observe the cleared forward link and stop early.</li>
     * </ul>
     *
     * <p>The removed node must not be modified, reused, or inserted again
     * until every reader that could hold it has quiesced.</p>
     *
     * @return the node that followed this node, or {@code null}
     * @throws IllegalStateException if this node has no predecessor
     */
    public final T unlink() {
        T oldPrevious = previous;

        if (oldPrevious == null) {
            throw new IllegalStateException(
                    "Cannot unlink a detached node or sentinel node"
            );
        }

        T oldNext = nextWriterOnly();

        /*
         * Update the writer-only backward link first. The following release
         * store also publishes this change, though readers must not depend
         * on it.
         */
        if (oldNext != null) {
            InvasivelyLinkedSWMRList<T> oldNextLinks = linksOf(oldNext);
            oldNextLinks.previous = oldPrevious;
        }

        /*
         * Bypass this node. This is the removal linearisation point.
         */
        InvasivelyLinkedSWMRList<T> previousLinks = linksOf(oldPrevious);
        previousLinks.setNextRelease(oldNext);

        /*
         * Clear this node after it has been bypassed. Release semantics are
         * stronger than strictly required here, but they keep the ordering
         * rules simple and explicit.
         */
        previous = null;
        setNextRelease(null);

        return oldNext;
    }

    /**
     * Detaches a list head after its owner has published a replacement head.
     *
     * <p>Only the single writer may call this method. Concurrent readers that already hold the old head may either
     * continue to the former successor or stop at the cleared link, matching the weakly-consistent iterator contract.</p>
     */
    public final T detachHeadWriterOnly() {
        if (previous != null) {
            throw new IllegalStateException("Only a list head may be detached with this method");
        }
        T oldNext = nextWriterOnly();
        if (oldNext != null) {
            linksOf(oldNext).previous = null;
        }
        setNextRelease(null);
        return oldNext;
    }

    /**
     * Returns a weakly consistent forward iterator beginning with this node.
     *
     * <p>The iterator does not create a snapshot. During concurrent writes
     * it may miss inserted nodes, return removed nodes, or stop early after
     * reaching a removed node. It does not throw
     * {@link java.util.ConcurrentModificationException}.</p>
     */
    @Override
    public final @NotNull Iterator<T> iterator() {
        return new Iterator<>() {
            private T next = self();

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public T next() {
                T result = next;

                if (result == null) {
                    throw new NoSuchElementException();
                }

                next = result.nextAcquire();
                return result;
            }
        };
    }
}
