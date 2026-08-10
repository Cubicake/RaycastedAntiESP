/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.bStats;

import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.config.raycast.ChunkSectionConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.SoundEffectsConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class MetricsCollector {
    private static final int PLUGIN_ID = 24553;
    private static final String ALWAYS_VISIBLE = "Always visible";

    private final Metrics metrics;
    private final ConfigManager config;

    public MetricsCollector(RaycastedAntiESP plugin, ConfigManager config) {
        this.config = config;
        metrics = new Metrics(plugin, PLUGIN_ID);
        registerCustomMetrics();
    }

    public void shutdown() {
        metrics.shutdown();
    }

    private void registerCustomMetrics() {
        /*
        // Raycast radii
        addNumericChart("v2_player_raycast_radius", () -> config.getPlayerConfig().getRaycastRadius());
        addNumericChart("v2_entity_raycast_radius", () -> config.getEntityConfig().getRaycastRadius());
        addNumericChart("v2_tile_entity_raycast_radius", () -> config.getTileEntityConfig().getRaycastRadius());
        //addNumericChart("v2_sound_effects_raycast_radius", () -> config.getSoundEffectsConfig().raycastRadius());

        // Hide-on-spawn distances
        addNumericChart("v2_player_hide_on_spawn_distance", () -> config.getPlayerConfig().hideOnSpawnDistance());
        addNumericChart("v2_entity_hide_on_spawn_distance", () -> config.getEntityConfig().hideOnSpawnDistance());
        addNumericChart("v2_tile_entity_hide_on_spawn_distance", () -> config.getTileEntityConfig().hideOnSpawnDistance());

        // Visible recheck intervals
        addNumericChart("v2_player_visible_recheck_interval_ticks", () -> config.getPlayerConfig().getVisibleRecheckIntervalTicks());
        addNumericChart("v2_entity_visible_recheck_interval_ticks", () -> config.getEntityConfig().getVisibleRecheckIntervalTicks());
        addNumericChart("v2_tile_entity_visible_recheck_interval_ticks", () -> config.getTileEntityConfig().getVisibleRecheckIntervalTicks());
        //addNumericChart("v2_chunk_section_visible_recheck_interval_ticks", () -> config.getChunkSectionConfig().visibleRecheckIntervalTicks());

        // Occlusion limits
        addNumericChart("v2_player_max_occluding_count", () -> config.getPlayerConfig().getMaxOccludingCount());
        addNumericChart("v2_entity_max_occluding_count", () -> config.getEntityConfig().getMaxOccludingCount());
        addNumericChart("v2_tile_entity_max_occluding_count", () -> config.getTileEntityConfig().getMaxOccludingCount());

        // Entity hide mode
        addBooleanChart("v2_entity_keep_client_entity_when_hidden", () -> config.getEntityConfig().keepClientEntityWhenHidden());
        addBooleanChart("v2_player_keep_client_entity_when_hidden", () -> config.getPlayerConfig().keepClientEntityWhenHidden());

        // Always-visible radii also communicate whether each check is enabled.
        metrics.addCustomChart(new SimplePie("v2_player_always_show_radius", () -> {
            PlayerConfig playerConfig = config.getPlayerConfig();
            return alwaysVisible(playerConfig.enabled(), playerConfig.getAlwaysShowRadius());
        }));
        metrics.addCustomChart(new SimplePie("v2_entity_always_show_radius", () -> {
            EntityConfig entityConfig = config.getEntityConfig();
            return alwaysVisible(entityConfig.enabled(), entityConfig.getAlwaysShowRadius());
        }));
        metrics.addCustomChart(new SimplePie("v2_tile_entity_always_show_radius", () -> {
            TileEntityConfig tileEntityConfig = config.getTileEntityConfig();
            return alwaysVisible(tileEntityConfig.enabled(), tileEntityConfig.getAlwaysShowRadius());
        }));
        metrics.addCustomChart(new SimplePie("v2_sound_effects_always_play_radius", () -> {
            SoundEffectsConfig soundEffectsConfig = config.getSoundEffectsConfig();
            return alwaysVisible(soundEffectsConfig.enabled(), soundEffectsConfig.alwaysPlayRadius());
        }));
        metrics.addCustomChart(new SimplePie("v2_chunk_section_always_show_radius_chunks", () -> {
            ChunkSectionConfig chunkSectionConfig = config.getChunkSectionConfig();
            return alwaysVisible(chunkSectionConfig.enabled(), chunkSectionConfig.alwaysShowRadiusChunks());
        }));
         */ // these charts may be added at a future date

        // Runtime metrics
        metrics.addCustomChart(new SimplePie("server_size", this::getPlayerCount));
        metrics.addCustomChart(new SimplePie("v2_median_tracked_entities_per_player", this::getMedianEntityCount));
    }

    private void addNumericChart(String chartID, IntMetric metric) {
        metrics.addCustomChart(new SimplePie(chartID, metric));
    }

    private void addBooleanChart(String chartID, BooleanMetric metric) {
        metrics.addCustomChart(new SimplePie(chartID, metric));
    }

    private String getPlayerCount() {
        return bucketPlayerCount(PlayerRegistry.getInstance().getAllPlayerData().size());
    }

    private String getMedianEntityCount() {
        return bucketMedianEntityCounts(
                PlayerRegistry.getInstance().getAllPlayerData(),
                PlayerData::isConnected,
                playerData -> playerData.entityView().size()
        );
    }

    private static String alwaysVisible(boolean enabled, int radius) {
        return enabled ? String.valueOf(radius) : ALWAYS_VISIBLE;
    }

    static String bucketPlayerCount(int playerCount) {
        if (playerCount < 0) {
            throw new IllegalArgumentException("Player count cannot be negative");
        }
        if (playerCount <= 3) return String.valueOf(playerCount);
        if (playerCount <= 6) return "4-6";
        if (playerCount <= 10) return "7-10";
        if (playerCount <= 15) return "11-15";
        if (playerCount <= 25) return "16-25";
        if (playerCount <= 40) return "26-40";
        if (playerCount <= 70) return "41-70";
        if (playerCount <= 100) return "71-100";
        if (playerCount <= 200) return "101-200";
        if (playerCount <= 300) return "201-300";
        if (playerCount <= 500) return "301-500";
        if (playerCount <= 1000) return "501-1000";
        if (playerCount <= 5000) return "1001-5000";
        return "5001+";
    }

    static String bucketMedianEntityCounts(int[] entityCounts) {
        if (entityCounts.length == 0) {
            return null;
        }

        Arrays.sort(entityCounts);
        int middle = entityCounts.length / 2;
        double median = entityCounts.length % 2 == 1
                ? entityCounts[middle]
                : ((long) entityCounts[middle - 1] + entityCounts[middle]) / 2.0;
        return bucketMedianEntityCount(median);
    }

    static <T> String bucketMedianEntityCounts(Collection<T> samples, Predicate<T> isConnected, ToIntFunction<T> entityCount) {
        int[] entityCounts = samples.stream()
                .filter(isConnected)
                .mapToInt(entityCount)
                .toArray();
        return bucketMedianEntityCounts(entityCounts);
    }

    static String bucketMedianEntityCount(double entityCount) {
        if (entityCount < 0) {
            throw new IllegalArgumentException("Entity count cannot be negative");
        }
        if (entityCount <= 20) return "0-20";
        if (entityCount <= 50) return "21-50";
        if (entityCount <= 100) return "51-100";
        if (entityCount <= 300) return "101-300";
        if (entityCount <= 500) return "301-500";
        if (entityCount <= 1000) return "501-1000";
        if (entityCount <= 2000) return "1001-2000";
        if (entityCount <= 5000) return "2001-5000";
        return "5001+";
    }

    @FunctionalInterface
    private interface IntMetric extends Callable<String> {
        int getAsInt();

        @Override
        default String call() {
            return String.valueOf(getAsInt());
        }
    }

    @FunctionalInterface
    private interface BooleanMetric extends Callable<String> {
        boolean getAsBool();

        @Override
        default String call() {
            return String.valueOf(getAsBool());
        }
    }
}
