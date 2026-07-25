/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper;

import games.cubi.raycastedantiesp.core.utils.BuildProperties.Version;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {
    @Test
    void legacyApiEntriesAreSkipped() {
        assertNull(UpdateChecker.findComparablePaperVersion(List.of(
            entry("1.2.3", UpdateChecker.UpdateChannel.STABLE),
            entry("v1.2.4-RELEASE", UpdateChecker.UpdateChannel.STABLE)
        ), UpdateChecker.UpdateChannel.STABLE));
    }

    @Test
    void wrongPlatformLatestEntriesAreSkippedUntilPaperIsFound() {
        UpdateChecker.ApiVersion selected = UpdateChecker.findComparablePaperVersion(List.of(
            entry("17.41.264-Fabric-12.0.104", UpdateChecker.UpdateChannel.ALPHA),
            entry("malformed", UpdateChecker.UpdateChannel.ALPHA),
            entry("17.41.264-Paper-12.0.103-RELEASE", UpdateChecker.UpdateChannel.ALPHA)
        ), UpdateChecker.UpdateChannel.ALPHA);

        assertNotNull(selected);
        assertEquals("17.41.264-Paper-12.0.103-RELEASE", selected.rawVersion());
        assertEquals(version("17.41.264"), selected.coreVersion());
        assertEquals(version("12.0.103"), selected.platformVersion());
    }

    @Test
    void coreBehindTriggersOutdated() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.263"),
            version("12.0.103"),
            List.of(entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE)),
            true,
            false,
            false
        );

        assertResult(report, 0, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.STABLE);
    }

    @Test
    void platformBehindTriggersOutdated() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.264"),
            version("12.0.102"),
            List.of(entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE)),
            true,
            false,
            false
        );

        assertResult(report, 0, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.STABLE);
    }

    @Test
    void aheadInOneVersionButBehindInTheOtherTriggersOutdated() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.265"),
            version("12.0.102"),
            List.of(entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE)),
            true,
            false,
            false
        );

        assertResult(report, 0, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.STABLE);
    }

    @Test
    void reportIncludesAllEnabledChannelsInStableBetaAlphaOrder() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.264"),
            version("12.0.103"),
            List.of(
                entry("17.41.264-Paper-12.0.105", UpdateChecker.UpdateChannel.ALPHA),
                entry("17.41.264-Paper-12.0.104", UpdateChecker.UpdateChannel.BETA),
                entry("1.2.3", UpdateChecker.UpdateChannel.STABLE),
                entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE)
            )
        );

        assertEquals(3, report.results().size());
        assertResult(report, 0, UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.STABLE);
        assertResult(report, 1, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.BETA);
        assertResult(report, 2, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.ALPHA);
    }

    @Test
    void upToDateAlphaStillIncludesReleaseAndBetaLines() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.264"),
            version("12.0.103"),
            List.of(
                entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.ALPHA),
                entry("1.2.3", UpdateChecker.UpdateChannel.STABLE),
                entry("17.41.263-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE),
                entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.BETA)
            )
        );

        assertEquals(3, report.results().size());
        assertResult(report, 0, UpdateChecker.UpdateStatus.AHEAD, UpdateChecker.UpdateChannel.STABLE);
        assertResult(report, 1, UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.BETA);
        assertResult(report, 2, UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.ALPHA);

        List<String> lines = messageLines(report);
        assertEquals(3, lines.size());
        assertMessageLine(lines.get(0), UpdateChecker.UpdateStatus.AHEAD, UpdateChecker.UpdateChannel.STABLE);
        assertMessageLine(lines.get(1), UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.BETA);
        assertMessageLine(lines.get(2), UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.ALPHA);
    }

    @Test
    void legacyOnlyReleaseProducesNoComparableReleaseLine() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.264"),
            version("12.0.103"),
            List.of(entry("1.6.5", UpdateChecker.UpdateChannel.STABLE)),
            true,
            false,
            false
        );

        assertResult(report, 0, UpdateChecker.UpdateStatus.NO_COMPARABLE_VERSION, UpdateChecker.UpdateChannel.STABLE);
        assertMessageLine(UpdateChecker.formatUpdateMessage(report), UpdateChecker.UpdateStatus.NO_COMPARABLE_VERSION, UpdateChecker.UpdateChannel.STABLE);
    }

    @Test
    void buildSuffixesAndMetadataAreIgnored() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.264"),
            version("12.0.103"),
            List.of(entry("17.41.264-Paper-12.0.104-SNAPSHOT+build-2026-07-08+git-abcdef12", UpdateChecker.UpdateChannel.STABLE)),
            true,
            false,
            false
        );

        assertResult(report, 0, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.STABLE);
    }

    @Test
    void channelsCanBeDisabledIndependently() {
        List<UpdateChecker.VersionEntry> entries = List.of(
            entry("17.41.264-Paper-12.0.104", UpdateChecker.UpdateChannel.BETA),
            entry("17.41.264-Paper-12.0.105", UpdateChecker.UpdateChannel.ALPHA),
            entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE)
        );

        UpdateChecker.UpdateCheckReport alphaOnly = UpdateChecker.checkVersions(version("17.41.264"), version("12.0.103"), entries, true, false, true);
        assertEquals(2, alphaOnly.results().size());
        assertResult(alphaOnly, 0, UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.STABLE);
        assertResult(alphaOnly, 1, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.ALPHA);

        UpdateChecker.UpdateCheckReport betaOnly = UpdateChecker.checkVersions(version("17.41.264"), version("12.0.103"), entries, true, true, false);
        assertEquals(2, betaOnly.results().size());
        assertResult(betaOnly, 0, UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.STABLE);
        assertResult(betaOnly, 1, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.BETA);

        UpdateChecker.UpdateCheckReport stableOnly = UpdateChecker.checkVersions(version("17.41.264"), version("12.0.103"), entries, true, false, false);
        assertEquals(1, stableOnly.results().size());
        assertResult(stableOnly, 0, UpdateChecker.UpdateStatus.UP_TO_DATE, UpdateChecker.UpdateChannel.STABLE);

        UpdateChecker.UpdateCheckReport prereleaseOnly = UpdateChecker.checkVersions(version("17.41.264"), version("12.0.103"), entries, false, true, true);
        assertEquals(2, prereleaseOnly.results().size());
        assertResult(prereleaseOnly, 0, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.BETA);
        assertResult(prereleaseOnly, 1, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.ALPHA);

        UpdateChecker.UpdateCheckReport none = UpdateChecker.checkVersions(version("17.41.264"), version("12.0.103"), entries, false, false, false);
        assertEquals(0, none.results().size());
    }

    @Test
    void currentAheadOfLatestReleaseReportsAheadOfReleaseWhenPrereleasesAreDisabled() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            version("17.41.265"),
            version("12.0.103"),
            List.of(entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE)),
            true,
            false,
            false
        );

        assertResult(report, 0, UpdateChecker.UpdateStatus.AHEAD, UpdateChecker.UpdateChannel.STABLE);
        assertMessageLine(UpdateChecker.formatUpdateMessage(report), UpdateChecker.UpdateStatus.AHEAD, UpdateChecker.UpdateChannel.STABLE);
    }

    @Test
    void behindMessageIncludesTargetVersionAndMainModrinthPage() {
        UpdateChecker.ApiVersion beta = new UpdateChecker.ApiVersion("17.41.264-Paper-12.0.103", version("17.41.264"), version("12.0.103"));
        UpdateChecker.UpdateCheckReport report = new UpdateChecker.UpdateCheckReport(List.of(UpdateChecker.UpdateCheckResult.behind(UpdateChecker.UpdateChannel.BETA, beta)));

        String message = UpdateChecker.formatUpdateMessage(report);
        assertMessageLine(message, UpdateChecker.UpdateStatus.BEHIND, UpdateChecker.UpdateChannel.BETA);
        assertTrue(message.contains(beta.rawVersion()));
        assertTrue(message.contains("modrinth.com/project/bCjNZu0C"));
    }

    @Test
    void invalidCurrentVersionReturnsOnlyInvalidVersionMessage() {
        UpdateChecker.UpdateCheckReport report = UpdateChecker.checkVersions(
            new Version(-1, -1, -1, true),
            version("12.0.103"),
            List.of(
                entry("17.41.264-Paper-12.0.103", UpdateChecker.UpdateChannel.STABLE),
                entry("17.41.264-Paper-12.0.104", UpdateChecker.UpdateChannel.BETA),
                entry("17.41.264-Paper-12.0.105", UpdateChecker.UpdateChannel.ALPHA)
            )
        );

        assertEquals(1, report.results().size());
        assertEquals(UpdateChecker.UpdateStatus.INVALID_CURRENT_VERSION, report.results().getFirst().status());
        String message = UpdateChecker.formatUpdateMessage(report);
        assertTrue(message.startsWith("<red>"));
        assertTrue(message.contains("RaycastedAntiESP"));
        assertTrue(message.contains("invalid current version format"));
    }

    private static Version version(String version) {
        return Version.parse(version);
    }

    private static UpdateChecker.VersionEntry entry(String version, UpdateChecker.UpdateChannel channel) {
        return new UpdateChecker.VersionEntry(version, channel);
    }

    private static void assertResult(UpdateChecker.UpdateCheckReport report, int index, UpdateChecker.UpdateStatus status, UpdateChecker.UpdateChannel channel) {
        assertEquals(status, report.results().get(index).status());
        assertEquals(channel, report.results().get(index).channel());
    }

    private static List<String> messageLines(UpdateChecker.UpdateCheckReport report) {
        return UpdateChecker.formatUpdateMessage(report).lines().toList();
    }

    private static void assertMessageLine(String line, UpdateChecker.UpdateStatus status, UpdateChecker.UpdateChannel channel) {
        assertTrue(line.contains(channel.messageName()));
        assertTrue(line.contains("RaycastedAntiESP"));

        switch (status) {
            case BEHIND -> {
                assertTrue(line.startsWith("<red>"));
                assertTrue(line.contains("behind"));
                assertTrue(line.contains("Please upgrade"));
            }
            case UP_TO_DATE -> {
                assertTrue(line.startsWith("<green>"));
                assertTrue(line.contains("up to date"));
            }
            case AHEAD, NO_COMPARABLE_VERSION -> {
                assertTrue(line.startsWith("<yellow>"));
                assertTrue(line.contains("ahead"));
            }
            case INVALID_CURRENT_VERSION -> throw new IllegalArgumentException("Use the invalid-current-version assertions instead.");
        }
    }
}
