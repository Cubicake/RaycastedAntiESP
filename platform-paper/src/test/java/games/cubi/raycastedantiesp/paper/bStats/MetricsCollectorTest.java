/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.bStats;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricsCollectorTest {
    @Test
    void playerCountBucketsCoverEveryBoundary() {
        Map<Integer, String> expectedBuckets = Map.ofEntries(
                Map.entry(0, "0"),
                Map.entry(1, "1"),
                Map.entry(2, "2"),
                Map.entry(3, "3"),
                Map.entry(4, "4-6"),
                Map.entry(6, "4-6"),
                Map.entry(7, "7-10"),
                Map.entry(10, "7-10"),
                Map.entry(11, "11-15"),
                Map.entry(15, "11-15"),
                Map.entry(16, "16-25"),
                Map.entry(25, "16-25"),
                Map.entry(26, "26-40"),
                Map.entry(40, "26-40"),
                Map.entry(41, "41-70"),
                Map.entry(70, "41-70"),
                Map.entry(71, "71-100"),
                Map.entry(100, "71-100"),
                Map.entry(101, "101-200"),
                Map.entry(200, "101-200"),
                Map.entry(201, "201-300"),
                Map.entry(300, "201-300"),
                Map.entry(301, "301-500"),
                Map.entry(500, "301-500"),
                Map.entry(501, "501-1000"),
                Map.entry(1000, "501-1000"),
                Map.entry(1001, "1001-5000"),
                Map.entry(5000, "1001-5000"),
                Map.entry(5001, "5001+")
        );

        expectedBuckets.forEach((count, bucket) ->
                assertEquals(bucket, MetricsCollector.bucketPlayerCount(count), "count=" + count));
        assertThrows(IllegalArgumentException.class, () -> MetricsCollector.bucketPlayerCount(-1));
    }

    @Test
    void medianEntityBucketsCoverEveryBoundary() {
        Map<Double, String> expectedBuckets = Map.ofEntries(
                Map.entry(0.0, "0-20"),
                Map.entry(20.0, "0-20"),
                Map.entry(20.5, "21-50"),
                Map.entry(50.0, "21-50"),
                Map.entry(50.5, "51-100"),
                Map.entry(100.0, "51-100"),
                Map.entry(100.5, "101-300"),
                Map.entry(300.0, "101-300"),
                Map.entry(300.5, "301-500"),
                Map.entry(500.0, "301-500"),
                Map.entry(500.5, "501-1000"),
                Map.entry(1000.0, "501-1000"),
                Map.entry(1000.5, "1001-2000"),
                Map.entry(2000.0, "1001-2000"),
                Map.entry(2000.5, "2001-5000"),
                Map.entry(5000.0, "2001-5000"),
                Map.entry(5000.5, "5001+")
        );

        expectedBuckets.forEach((count, bucket) ->
                assertEquals(bucket, MetricsCollector.bucketMedianEntityCount(count), "count=" + count));
        assertThrows(IllegalArgumentException.class, () -> MetricsCollector.bucketMedianEntityCount(-0.5));
    }

    @Test
    void medianHandlesOddEvenUnsortedAndEmptySamples() {
        assertEquals("21-50", MetricsCollector.bucketMedianEntityCounts(new int[]{50, 10, 30}));
        assertEquals("21-50", MetricsCollector.bucketMedianEntityCounts(new int[]{21, 20}));
        assertNull(MetricsCollector.bucketMedianEntityCounts(new int[0]));
    }

    @Test
    void medianExcludesDisconnectedPlayersAndSkipsEmptyPopulations() {
        List<EntityCountSample> samples = List.of(
                new EntityCountSample(true, 10),
                new EntityCountSample(false, 5001),
                new EntityCountSample(true, 30)
        );

        assertEquals("0-20", MetricsCollector.bucketMedianEntityCounts(
                samples,
                EntityCountSample::connected,
                EntityCountSample::entityCount
        ));
        assertNull(MetricsCollector.bucketMedianEntityCounts(
                List.of(new EntityCountSample(false, 10)),
                EntityCountSample::connected,
                EntityCountSample::entityCount
        ));
    }

    private record EntityCountSample(boolean connected, int entityCount) {}
}
