/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.engine;

public class TickTimingBatch {
    long entityNanos;
    long playerNanos;
    long tileNanos;

    int processedPlayers;
    int bypassSkippedPlayers;
    int nullLocationSkippedPlayers;

    int entityChecked;
    int entityRaycasts;
    int entityNullTargets;

    int playerChecked;
    int playerRaycasts;
    int playerNullTargets;

    int tileChecked;
    int tileRaycasts;
    int tileWorldSkipped;
    int tileRadiusSkipped;

    TickTimingBatch() {}

    public long startBatch() {
        return System.nanoTime();
    }

    public long elapsedSince(long startNanos) {
        return System.nanoTime() - startNanos;
    }

    public long startEntitySection() {
        return System.nanoTime();
    }

    public void finishEntitySection(long startNanos) {
        entityNanos += Math.max(0, System.nanoTime() - startNanos);
    }

    public long startPlayerSection() {
        return System.nanoTime();
    }

    public void finishPlayerSection(long startNanos) {
        playerNanos += Math.max(0, System.nanoTime() - startNanos);
    }

    public long startTileSection() {
        return System.nanoTime();
    }

    public void finishTileSection(long startNanos) {
        tileNanos += Math.max(0, System.nanoTime() - startNanos);
    }

    public void incrementProcessedPlayers() {
        processedPlayers++;
    }

    public void incrementBypassSkippedPlayers() {
        bypassSkippedPlayers++;
    }

    public void incrementNullLocationSkippedPlayers() {
        nullLocationSkippedPlayers++;
    }

    public void addEntityChecked(int count) {
        entityChecked += count;
    }

    public void incrementEntityRaycasts() {
        entityRaycasts++;
    }

    public void addPlayerChecked(int count) {
        playerChecked += count;
    }

    public void incrementPlayerRaycasts() {
        playerRaycasts++;
    }

    public void addTileChecked(int count) {
        tileChecked += count;
    }

    public void incrementTileWorldSkipped() {
        tileWorldSkipped++;
    }

    public void incrementTileRadiusSkipped() {
        tileRadiusSkipped++;
    }

    public void incrementTileRaycasts() {
        tileRaycasts++;
    }
}
