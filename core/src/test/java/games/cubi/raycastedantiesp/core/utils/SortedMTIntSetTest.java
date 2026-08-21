/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import games.cubi.utils.sets.SortedStripedMTIntSet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortedMTIntSetTest {
    private static final Field STATE_FIELD = declaredField(SortedStripedMTIntSet.class, "state");
    private static final Field STRIPES_FIELD = declaredField(STATE_FIELD.getType(), "stripes");
    private static final Field MASK_FIELD = declaredField(STATE_FIELD.getType(), "mask");

    @Test
    void validatesStripeCounts() {
        for (int stripeCount : List.of(1, 2, 4, 8, 16, 1024)) {
            StateSnapshot snapshot = snapshot(new SortedStripedMTIntSet(stripeCount));
            assertEquals(stripeCount, snapshot.stripes.length);
            assertEquals(stripeCount - 1, snapshot.mask);
        }

        for (int invalidStripeCount : List.of(0, -1, -2, 3, 5, 6, 12)) {
            assertThrows(IllegalArgumentException.class, () -> new SortedStripedMTIntSet(invalidStripeCount));
        }
    }

    @Test
    void routesValuesWithTheStripeMask() {
        int[] values = {
                Integer.MIN_VALUE, -8, -4, -3, -2, -1,
                0, 1, 2, 3, 4, 5, 8, Integer.MAX_VALUE
        };
        SortedStripedMTIntSet set = new SortedStripedMTIntSet(4);
        for (int value : values) {
            set.add(value);
        }

        StateSnapshot snapshot = snapshot(set);
        for (int stripeIndex = 0; stripeIndex < snapshot.stripes.length; stripeIndex++) {
            int expectedStripe = stripeIndex;
            int[] expected = Arrays.stream(values)
                    .filter(value -> (value & 3) == expectedStripe)
                    .sorted()
                    .toArray();
            assertArrayEquals(expected, snapshot.stripes[stripeIndex]);
        }
        assertValid(set, new HashSet<>(Arrays.stream(values).boxed().toList()));
    }

    @Test
    void keepsEveryStripeSortedAcrossInsertionOrdersAndRemovals() {
        int[] values = new Random(12345L).ints(241, -120, 121).distinct().toArray();
        List<int[]> insertionOrders = List.of(
                Arrays.stream(values).sorted().toArray(),
                Arrays.stream(values).boxed().sorted((left, right) -> Integer.compare(right, left)).mapToInt(Integer::intValue).toArray(),
                shuffled(values)
        );

        for (int[] insertionOrder : insertionOrders) {
            SortedStripedMTIntSet set = new SortedStripedMTIntSet();
            Set<Integer> expected = new HashSet<>();
            for (int value : insertionOrder) {
                set.add(value);
                expected.add(value);
            }
            assertValid(set, expected);

            for (int value : insertionOrder) {
                if ((value & 3) == 0) {
                    assertTrue(set.remove(value));
                    expected.remove(value);
                }
            }
            assertValid(set, expected);
        }
    }

    @Test
    void growsOnlyAtTheRequiredLiveCountThresholds() {
        SortedStripedMTIntSet set = new SortedStripedMTIntSet();

        for (int value = 0; value < 50; value++) {
            set.add(value);
        }
        assertEquals(1, stripeCount(set));
        set.add(0);
        assertEquals(1, stripeCount(set));

        set.add(50);
        assertEquals(2, stripeCount(set));
        for (int value = 51; value < 100; value++) {
            set.add(value);
        }
        assertEquals(2, stripeCount(set));
        set.add(0);
        assertEquals(2, stripeCount(set));

        set.add(100);
        assertEquals(4, stripeCount(set));
        for (int value = 101; value < 200; value++) {
            set.add(value);
        }
        assertEquals(4, stripeCount(set));
        set.add(0);
        assertEquals(4, stripeCount(set));

        set.add(200);
        assertEquals(8, stripeCount(set));
        Set<Integer> expected = new HashSet<>();
        for (int value = 0; value <= 200; value++) {
            expected.add(value);
        }
        assertValid(set, expected);
    }

    @Test
    void preservesMembershipAndRoutingDuringGrowth() {
        SortedStripedMTIntSet set = new SortedStripedMTIntSet();
        Set<Integer> expected = new HashSet<>();
        for (int count = 1; count <= 401; count++) {
            int value = (count & 1) == 0 ? -count : count;
            set.add(value);
            expected.add(value);
            if (count == 50 || count == 51 || count == 100 || count == 101 || count == 200 || count == 201 || count == 401) {
                assertValid(set, expected);
            }
        }
    }

    @Test
    void preservesSortedOrderWhenGrowthSplitsAnOldStripe() {
        SortedStripedMTIntSet set = new SortedStripedMTIntSet(2);
        Set<Integer> expected = new HashSet<>();
        for (int value = 200; value >= 0; value -= 2) {
            set.add(value);
            expected.add(value);
        }

        assertEquals(4, stripeCount(set));
        assertValid(set, expected);
    }

    @Test
    void neverShrinksAfterRemovals() {
        SortedStripedMTIntSet set = new SortedStripedMTIntSet();
        for (int value = 0; value < 201; value++) {
            set.add(value);
        }
        assertEquals(8, stripeCount(set));

        for (int value = 0; value < 201; value++) {
            assertTrue(set.remove(value));
        }
        assertEquals(8, stripeCount(set));
        assertValid(set, Set.of());
        set.add(Integer.MIN_VALUE);
        assertEquals(8, stripeCount(set));
    }

    @Test
    void ordinaryMutationsReplaceOnlyTheSelectedStripe() {
        SortedStripedMTIntSet set = new SortedStripedMTIntSet(4);
        for (int value = 0; value < 4; value++) {
            set.add(value);
        }

        StateSnapshot beforeAdd = snapshot(set);
        set.add(4);
        StateSnapshot afterAdd = snapshot(set);
        assertNotSame(beforeAdd.state, afterAdd.state);
        assertNotSame(beforeAdd.stripes[0], afterAdd.stripes[0]);
        for (int stripeIndex = 1; stripeIndex < 4; stripeIndex++) {
            assertSame(beforeAdd.stripes[stripeIndex], afterAdd.stripes[stripeIndex]);
        }

        StateSnapshot beforeDuplicate = snapshot(set);
        set.add(4);
        assertSame(beforeDuplicate.state, snapshot(set).state);
        assertFalse(set.remove(100));
        assertSame(beforeDuplicate.state, snapshot(set).state);

        StateSnapshot beforeRemove = snapshot(set);
        assertTrue(set.remove(4));
        StateSnapshot afterRemove = snapshot(set);
        assertNotSame(beforeRemove.state, afterRemove.state);
        assertNotSame(beforeRemove.stripes[0], afterRemove.stripes[0]);
        for (int stripeIndex = 1; stripeIndex < 4; stripeIndex++) {
            assertSame(beforeRemove.stripes[stripeIndex], afterRemove.stripes[stripeIndex]);
        }
    }

    @Test
    void traversesOneImmutableSnapshotAcrossStripes() {
        SortedStripedMTIntSet set = new SortedStripedMTIntSet(4);
        Set<Integer> expected = new HashSet<>(Set.of(-4, 0, 1, 2, 3, 4, 5, 8));
        expected.forEach(set::add);

        Set<Integer> observed = new HashSet<>();
        boolean[] mutated = {false};
        set.forEach(value -> {
            assertTrue(observed.add(value));
            if (!mutated[0]) {
                mutated[0] = true;
                assertTrue(set.remove(5));
                set.add(99);
            }
        });

        assertEquals(expected, observed);
        expected.remove(5);
        expected.add(99);
        assertValid(set, expected);

        SortedStripedMTIntSet empty = new SortedStripedMTIntSet(2);
        empty.forEach(value -> assertTrue(false, "An empty set must not be traversed: " + value));
    }

    private static int[] shuffled(int[] values) {
        int[] shuffled = values.clone();
        Random random = new Random(9876L);
        for (int index = shuffled.length - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            int value = shuffled[index];
            shuffled[index] = shuffled[swapIndex];
            shuffled[swapIndex] = value;
        }
        return shuffled;
    }

    private static void assertValid(SortedStripedMTIntSet set, Set<Integer> expected) {
        StateSnapshot snapshot = snapshot(set);
        assertEquals(snapshot.stripes.length - 1, snapshot.mask);

        Set<Integer> actual = new HashSet<>();
        for (int stripeIndex = 0; stripeIndex < snapshot.stripes.length; stripeIndex++) {
            int[] stripe = snapshot.stripes[stripeIndex];
            for (int valueIndex = 1; valueIndex < stripe.length; valueIndex++) {
                assertTrue(stripe[valueIndex - 1] < stripe[valueIndex]);
            }
            for (int value : stripe) {
                assertEquals(stripeIndex, value & snapshot.mask);
                assertTrue(actual.add(value), "Duplicate value in published stripes: " + value);
            }
        }
        assertEquals(expected, actual);

        for (int value : expected) {
            assertTrue(set.contains(value));
        }
    }

    private static int stripeCount(SortedStripedMTIntSet set) {
        return snapshot(set).stripes.length;
    }

    private static StateSnapshot snapshot(SortedStripedMTIntSet set) {
        try {
            Object state = STATE_FIELD.get(set);
            return new StateSnapshot(
                    state,
                    (int[][]) STRIPES_FIELD.get(state),
                    MASK_FIELD.getInt(state)
            );
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to inspect published set state", exception);
        }
    }

    private static Field declaredField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private record StateSnapshot(Object state, int[][] stripes, int mask) {
    }
}
