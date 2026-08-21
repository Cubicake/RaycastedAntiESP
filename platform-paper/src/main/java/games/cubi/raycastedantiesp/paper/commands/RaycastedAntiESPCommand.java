/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.commands;

import games.cubi.locatables.api.Locatable;
import games.cubi.locatables.api.MutableFloatingSpatial;
import games.cubi.locatables.api.Spatial;
import games.cubi.locatables.implementations.MutableLocatableImpl;
import games.cubi.locatables.implementations.MutableSpatialImpl;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.tracked.TrackedEntity;
import games.cubi.raycastedantiesp.core.players.PlayerData;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.core.raycast.RaycastUtil;
import games.cubi.raycastedantiesp.core.view.AbstractBlockView;
import games.cubi.raycastedantiesp.core.view.BlockView;
import games.cubi.raycastedantiesp.core.view.EntityView;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import games.cubi.raycastedantiesp.paper.UpdateChecker;
import games.cubi.raycastedantiesp.paper.packets.PacketEventsPaperBlockInfoResolver;

import games.cubi.raycastedantiesp.paper.utils.PaperScheduler;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import net.strokkur.commands.*;
import net.strokkur.commands.arguments.IntArg;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Description;
import net.strokkur.commands.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.UUID;

// Credit to Strokkur for making StrokkCommands, a non-hideous way to use the power of brigadier.

@Command("raycastedantiesp")
@Aliases({"raesp", "antiesp", "reo"})
@Description("Command for management of the RaycastedAntiESP plugin")
@Permission("raycastedantiesp.command")
public class RaycastedAntiESPCommand {
    @DefaultExecutes
    public void helpCommand(CommandSender sender) {
        sender.sendRichMessage("<white>RaycastedAntiESP <yellow>v" + RaycastedAntiESP.get().getDescription().getVersion());
        sender.sendRichMessage("<white>Commands:");
        sender.sendRichMessage("<green>/raycastedantiesp reload <gray>- Reloads the config");
        sender.sendRichMessage("<green>/raycastedantiesp config-values <gray>- Shows all config values");
        sender.sendRichMessage("<green>/raycastedantiesp set <key> <value> <gray>- Sets a config value");
        sender.sendRichMessage("<green>/raycastedantiesp add <key> <value> <gray>- Adds a value to a list config");
        sender.sendRichMessage("<green>/raycastedantiesp remove <key> <value> <gray>- Removes a value from a list config");
        sender.sendRichMessage(Attribution.attributionCommandDescription); //Using constant from Attribution class to ensure that it cannot be deleted without the developer noticing that they are obligated to replace it with an equivalent notice.
    }

    @Executes("reload")
    void reloadCommand(CommandSender sender) {
        try {
            ConfigManager.get().load();
            sender.sendMessage("[RaycastedAntiESP] Config reloaded.");
        } catch (RuntimeException e) {
            sender.sendRichMessage("<red>[RaycastedAntiESP] Config reload rejected: <white>" + e.getMessage());
        }
    }

    @Executes("config-values")
    void configValuesCommand(CommandSender sender) {
        ConfigManager config = ConfigManager.get();
        //dynamic config values
        sender.sendMessage("[RaycastedAntiESP] Config values: ");

        for (var entry : config.getConfigValues().entrySet()) {
            String path = entry.getKey();
            Object val = entry.getValue();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + path + "<gray> = <white>" + val));
        }
    }

    @Executes("set")
    void setCommand(@StringArg(StringArgType.STRING) String key, @StringArg(StringArgType.GREEDY) String value, CommandSender sender) {
        ConfigManager config = ConfigManager.get();
        ConfigManager.SetConfigResult result = config.setConfigValue(key, value);
        sendConfigMutationResult(sender, result, "Set", key, value);
    }

    @Executes("add")
    void addCommand(@StringArg(StringArgType.STRING) String key, @StringArg(StringArgType.GREEDY) String value, CommandSender sender) {
        ConfigManager config = ConfigManager.get();
        ConfigManager.SetConfigResult result = config.addConfigListValue(key, value);
        sendConfigMutationResult(sender, result, "Added", key, value);
    }

    @Executes("remove")
    void removeCommand(@StringArg(StringArgType.STRING) String key, @StringArg(StringArgType.GREEDY) String value, CommandSender sender) {
        ConfigManager config = ConfigManager.get();
        ConfigManager.SetConfigResult result = config.removeConfigListValue(key, value);
        sendConfigMutationResult(sender, result, "Removed", key, value);
    }

    @Executes("check-for-updates")
    void checkForUpdatesCommand(CommandSender sender) {
        UpdateChecker.checkForUpdates(RaycastedAntiESP.get(), sender);
    }

    @Executes("print-block-ids")
    void printBlockIDsCommand() {
        PacketEventsPaperBlockInfoResolver.get.iterateBlockIDs(true);
    }

    private void sendConfigMutationResult(CommandSender sender, ConfigManager.SetConfigResult result, String action, String key, String value) {
        if (!result.success()) {
            sender.sendRichMessage("<red>Invalid config change: <white>" + result.message());
            return;
        }
        sender.sendRichMessage("<white>" + action + " <green>" + value + "<white> for <green>" + key);
        if (result.restartRequired()) {
            sender.sendRichMessage("<yellow>This change was saved but requires a restart: <white>" + result.message());
        }
    }

    @Subcommand("test")
    static class TestCommands {
        @Executes("log")
        void logString(@StringArg(StringArgType.GREEDY) String message) {
            Logger.info(message, 1, RaycastedAntiESPCommand.class);
        }

        @Executes("location-drift")
        void testCommand(CommandSender sender) {
            assert Attribution.class == Attribution.class;
            assert Attribution.READ_COMMENTS_BEFORE_EDITING_OR_DELETING_CLASS_OR_FACE_LEGAL_ACTION == 0; //Using constant from Attribution class to ensure that it cannot be deleted without the developer noticing that they are obligated to replace it with an equivalent notice.
            Player player = (Player) sender;
            PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());
            Entity closestEntity = player.getNearbyEntities(10,10,10).getFirst();
            if (closestEntity == null) return;
            player.sendRichMessage("Closest entity is "+closestEntity.getName());
            Spatial entityPosition = playerData.entityView().getPosition(closestEntity.getUniqueId());
            Location bukkitLoc = closestEntity.getLocation().clone();
            player.sendRichMessage("Entity location according to PacketEvents is "+entityPosition);
            player.sendRichMessage("Entity location according to Bukkit is "+bukkitLoc);
            double driftX = Math.abs(entityPosition.x() - bukkitLoc.getX());
            double driftZ = Math.abs(entityPosition.z() - bukkitLoc.getZ());
            if (driftX < 0.0005) driftX = 0;
            if (driftZ < 0.0005) driftZ = 0;
            Logger.debug("Drift is X: "+driftX+" Z: "+driftZ);
            sender.sendRichMessage("Drift is X: "+driftX+" Z: "+driftZ);
        }

        @Subcommand("benchmark")
        static class BenchmarkCommand {
            private static final int BENCHMARK_CORPUS_SIZE = 8192;
            private static final int BENCHMARK_CORPUS_MASK = BENCHMARK_CORPUS_SIZE - 1;
            private static final int BENCHMARK_BATCH_SIZE = 2048;
            private static final int BENCHMARK_WARMUP_BATCHES = 1250;
            private static final int BENCHMARK_MEASUREMENT_BATCHES = 512;
            private static final int BENCHMARK_ROUNDS = 5;
            private static final int DEFAULT_RAYCAST_DISTANCE = 50;
            private static final long BENCHMARK_SEED = 0x6a09e667f3bcc909L;
            private static volatile long benchmarkSink;

            @DefaultExecutes
            void benchmark(Player player) {
                benchmark(player, DEFAULT_RAYCAST_DISTANCE);
            }

            @DefaultExecutes
            void benchmark(Player player, @IntArg(min = 1, max = Short.MAX_VALUE - 1) int raycastDistance) {
                Locatable[] locatables = new Locatable[BENCHMARK_CORPUS_SIZE];
                PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());
                Locatable currentPlayerLocation = playerData.ownLocation();
                Locatable playerLocatable = new MutableLocatableImpl(currentPlayerLocation.world(), currentPlayerLocation.x(), currentPlayerLocation.y(), currentPlayerLocation.z());
                MutableFloatingSpatial unitDirection = new MutableSpatialImpl(0, 0, 0);
                SplittableRandom random = new SplittableRandom(BENCHMARK_SEED);
                for (int i = 0; i < locatables.length; i++) {
                    unitDirection.setX(random.nextDouble(-0.5, 0.5));
                    unitDirection.setY(random.nextDouble(-0.5, 0.5));
                    unitDirection.setZ(random.nextDouble(-0.5, 0.5));
                    unitDirection.normalise();
                    unitDirection.scalarMultiply(raycastDistance);
                    locatables[i] = new MutableLocatableImpl(playerLocatable.world(), playerLocatable.x(), playerLocatable.y(), playerLocatable.z()).add(unitDirection);
                }
                BlockView blockView = playerData.blockView();
                int maxRaycastRadius = Math.max(100, raycastDistance + 1);
                int maxRaycastRadiusSquared = maxRaycastRadius * maxRaycastRadius;
                player.sendRichMessage("Running deterministic raycast benchmark at distance " + raycastDistance + "...");
                Bukkit.getAsyncScheduler().runNow(RaycastedAntiESP.get(), (ignored) -> {
                    java.lang.management.ThreadMXBean managementBean = ManagementFactory.getThreadMXBean();
                    com.sun.management.ThreadMXBean allocationBean = managementBean instanceof com.sun.management.ThreadMXBean bean ? bean : null;
                    if (allocationBean != null && allocationBean.isThreadAllocatedMemorySupported() && !allocationBean.isThreadAllocatedMemoryEnabled()) {
                        allocationBean.setThreadAllocatedMemoryEnabled(true);
                    }
                    long threadId = Thread.currentThread().threadId();
                    boolean measureAllocations = allocationBean != null && allocationBean.isThreadAllocatedMemoryEnabled();

                    long warmupChecksum = 0;
                    int corpusIndex = 0;
                    for (int batch = 0; batch < BENCHMARK_WARMUP_BATCHES; batch++) {
                        warmupChecksum += runRaycastBatch(playerLocatable, locatables, corpusIndex, maxRaycastRadiusSquared, blockView);
                        corpusIndex = (corpusIndex + BENCHMARK_BATCH_SIZE) & BENCHMARK_CORPUS_MASK;
                    }
                    benchmarkSink = warmupChecksum;

                    double[] nanosecondsPerRay = new double[BENCHMARK_ROUNDS];
                    double[] bytesPerRay = new double[BENCHMARK_ROUNDS];
                    long[] checksums = new long[BENCHMARK_ROUNDS];
                    double[] batchNanosecondsPerRay = new double[BENCHMARK_ROUNDS * BENCHMARK_MEASUREMENT_BATCHES];
                    int invocationsPerRound = BENCHMARK_BATCH_SIZE * BENCHMARK_MEASUREMENT_BATCHES;
                    int batchSampleIndex = 0;
                    for (int round = 0; round < BENCHMARK_ROUNDS; round++) {
                        long allocatedBefore = measureAllocations ? allocationBean.getThreadAllocatedBytes(threadId) : 0;
                        long startTime = System.nanoTime();
                        long checksum = 0;
                        corpusIndex = 0;
                        for (int batch = 0; batch < BENCHMARK_MEASUREMENT_BATCHES; batch++) {
                            long batchStartTime = System.nanoTime();
                            int batchChecksum = runRaycastBatch(playerLocatable, locatables, corpusIndex, maxRaycastRadiusSquared, blockView);
                            long batchDuration = System.nanoTime() - batchStartTime;
                            checksum += batchChecksum;
                            batchNanosecondsPerRay[batchSampleIndex++] = batchDuration / (double) BENCHMARK_BATCH_SIZE;
                            corpusIndex = (corpusIndex + BENCHMARK_BATCH_SIZE) & BENCHMARK_CORPUS_MASK;
                        }
                        long duration = System.nanoTime() - startTime;
                        long allocated = measureAllocations ? allocationBean.getThreadAllocatedBytes(threadId) - allocatedBefore : 0;
                        benchmarkSink = checksum;
                        nanosecondsPerRay[round] = duration / (double) invocationsPerRound;
                        bytesPerRay[round] = measureAllocations ? allocated / (double) invocationsPerRound : Double.NaN;
                        checksums[round] = checksum;
                    }

                    double[] sortedTimes = nanosecondsPerRay.clone();
                    Arrays.sort(sortedTimes);
                    double medianTime = sortedTimes[sortedTimes.length / 2];
                    double meanTime = mean(nanosecondsPerRay);
                    double standardDeviation = standardDeviation(nanosecondsPerRay, meanTime);
                    double coefficientOfVariation = standardDeviation / meanTime * 100.0;
                    double[] sortedBatchTimes = batchNanosecondsPerRay.clone();
                    Arrays.sort(sortedBatchTimes);
                    double batchP50 = percentile(sortedBatchTimes, 0.50);
                    double batchP90 = percentile(sortedBatchTimes, 0.90);
                    double batchP95 = percentile(sortedBatchTimes, 0.95);
                    double batchP99 = percentile(sortedBatchTimes, 0.99);
                    double batchMax = sortedBatchTimes[sortedBatchTimes.length - 1];
                    String benchmarkThread = Thread.currentThread().getName() + " (id " + threadId + ")";
                    PaperScheduler.runForAudience(RaycastedAntiESP.get(), player, () -> {
                        player.sendRichMessage("Benchmark thread: " + benchmarkThread);
                        player.sendRichMessage("Warmup rays: " + (BENCHMARK_BATCH_SIZE * BENCHMARK_WARMUP_BATCHES));
                        for (int round = 0; round < BENCHMARK_ROUNDS; round++) {
                            String allocationResult = measureAllocations ? String.format(Locale.ROOT, "%.3f B/ray", bytesPerRay[round]) : "allocation unavailable";
                            player.sendRichMessage(String.format(Locale.ROOT, "Round %d: %.3f ns/ray, %s, checksum %d", round + 1, nanosecondsPerRay[round], allocationResult, checksums[round]));
                        }
                        player.sendRichMessage(String.format(Locale.ROOT, "Rounds: median %.3f, mean %.3f, min %.3f, max %.3f ns/ray", medianTime, meanTime, sortedTimes[0], sortedTimes[sortedTimes.length - 1]));
                        player.sendRichMessage(String.format(Locale.ROOT, "Round deviation: %.3f ns/ray (CV %.2f%%)", standardDeviation, coefficientOfVariation));
                        player.sendRichMessage(String.format(Locale.ROOT, "Batches (%d rays, %d samples): p50 %.3f, p90 %.3f, p95 %.3f, p99 %.3f, max %.3f ns/ray",
                                BENCHMARK_BATCH_SIZE, sortedBatchTimes.length, batchP50, batchP90, batchP95, batchP99, batchMax));
                    });
                });
            }

            private static double mean(double[] values) {
                double total = 0;
                for (double value : values) total += value;
                return total / values.length;
            }

            private static double standardDeviation(double[] values, double mean) {
                double squaredDifferenceTotal = 0;
                for (double value : values) {
                    double difference = value - mean;
                    squaredDifferenceTotal += difference * difference;
                }
                return Math.sqrt(squaredDifferenceTotal / values.length);
            }

            private static double percentile(double[] sortedValues, double percentile) {
                double index = (sortedValues.length - 1) * percentile;
                int lowerIndex = (int) index;
                int upperIndex = Math.min(lowerIndex + 1, sortedValues.length - 1);
                double fraction = index - lowerIndex;
                return sortedValues[lowerIndex] + (sortedValues[upperIndex] - sortedValues[lowerIndex]) * fraction;
            }

            private static int runRaycastBatch(Locatable start, Locatable[] targets, int startIndex, int maxRaycastRadiusSquared, BlockView blockView) {
                int successfulRays = 0;
                for (int offset = 0; offset < BENCHMARK_BATCH_SIZE; offset++) {
                    if (RaycastUtil.raycastUnrolledAccumulated(3, 0, maxRaycastRadiusSquared, false, 0f, blockView, start, targets[startIndex + offset], null)) successfulRays++;
                }
                return successfulRays;
            }
        }

        @Executes("loaded-chunks")
        void loadedChunksCommand(Player player) {
            PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());
            AbstractBlockView<?, ?> pbsm = (AbstractBlockView<?, ?>) playerData.blockView();
            player.sendMessage(pbsm.loadedChunkCount() +"chunks loaded");
        }

        @Executes("entity-id")
        void getFromEntityID(int entityID, Player player) {
            PlayerData playerData = PlayerRegistry.getInstance().getPlayerData(player.getUniqueId());
            if (playerData == null) {
                player.sendRichMessage("<red>No player data is registered for " + describeViewer(player.getUniqueId()) + ".");
                return;
            }

            Entity bukkitEntity = SpigotConversionUtil.getEntityById(player.getWorld(), entityID);
            player.sendRichMessage("<white>Entity with ID " + entityID + " for viewer " + describeViewer(playerData.getPlayerUUID()) + ":");
            if (bukkitEntity == null) {
                player.sendRichMessage("<gray>According to Bukkit: <red>not found");
            } else {
                sendBukkitEntityData(player, bukkitEntity);
            }

            int matches = reportEntityIDMatches(player, playerData, entityID);
            if (matches == 0) {
                player.sendRichMessage("<gray>According to PacketEvents: <red>not found in either tracked view");
            }
        }

        @Executes("entity-id")
        void getFromEntityID(int entityID, CommandSender sender) {
            sender.sendRichMessage("<white>Searching all connected player views for entity ID " + entityID + ":");
            int matches = 0;
            for (PlayerData playerData : PlayerRegistry.getInstance().getAllPlayerData()) {
                if (!playerData.isConnected()) {
                    continue;
                }
                matches += reportEntityIDMatches(sender, playerData, entityID);
            }
            if (matches == 0) {
                sender.sendRichMessage("<red>No tracked entity with ID " + entityID + " was found.");
            }
        }

        @Executes("entity-uuid-raw")
        void getFromRawUUID(String entityUUIDraw, CommandSender sender) {
            sender.sendRichMessage("<white>Searching all connected player views for entity UUID " + entityUUIDraw + ":");
            UUID entityUUID = UUID.fromString(entityUUIDraw);
            getFromUUID(entityUUID, sender);
        }
        void getFromUUID(UUID entityUUID, CommandSender sender) {
            int matches = 0;
            for (PlayerData playerData : PlayerRegistry.getInstance().getAllPlayerData()) {
                if (!playerData.isConnected()) {
                    continue;
                }
                matches += reportEntityUUIDMatches(sender, playerData, entityUUID);
            }
            if (matches == 0) {
                sender.sendRichMessage("<red>No tracked entity with UUID " + entityUUID + " was found.");
            }
        }

        @Executes("entity-uuid")
        void getFromEntityUUID(Entity entity, CommandSender sender) {
            UUID entityUUID = entity.getUniqueId();
            sender.sendRichMessage("<white>Bukkit entity data:");
            sendBukkitEntityData(sender, entity);
            getFromUUID(entityUUID, sender);
        }

        private int reportEntityIDMatches(CommandSender sender, PlayerData playerData, int entityID) {
            return reportEntityIDMatches(sender, playerData, playerData.entityView(), "entity view", entityID)
                    + reportEntityIDMatches(sender, playerData, playerData.playerView(), "player view", entityID);
        }

        private int reportEntityIDMatches(CommandSender sender, PlayerData playerData, EntityView<?> view, String viewName, int entityID) {
            int matches = 0;
            for (UUID entityUUID : view.getKnownEntities()) {
                TrackedEntity<?> entity = view.getEntity(entityUUID);
                if (entity == null || entity.entityID() != entityID) {
                    continue;
                }
                sendTrackedEntityMatch(sender, playerData, viewName, entity);
                matches++;
            }
            return matches;
        }

        private int reportEntityUUIDMatches(CommandSender sender, PlayerData playerData, UUID entityUUID) {
            int matches = reportEntityUUIDMatch(sender, playerData, playerData.entityView(), "entity view", entityUUID);
            return matches + reportEntityUUIDMatch(sender, playerData, playerData.playerView(), "player view", entityUUID);
        }

        private int reportEntityUUIDMatch(CommandSender sender, PlayerData playerData, EntityView<?> view, String viewName, UUID entityUUID) {
            TrackedEntity<?> entity = view.getEntity(entityUUID);
            if (entity == null) {
                return 0;
            }
            sendTrackedEntityMatch(sender, playerData, viewName, entity);
            return 1;
        }

        private void sendTrackedEntityMatch(CommandSender sender, PlayerData playerData, String viewName, TrackedEntity<?> entity) {
            sender.sendRichMessage("<green>Match for viewer <white>" + describeViewer(playerData.getPlayerUUID()) + "<green> in <white>" + viewName + "<green>:");
            sender.sendRichMessage("<gray>According to PacketEvents: <white>" + entity);
        }

        private void sendBukkitEntityData(CommandSender sender, Entity entity) {
            sender.sendRichMessage("<gray>According to Bukkit: <white>" + entity);
            sender.sendRichMessage("<gray>Entity ID: <white>" + entity.getEntityId());
            sender.sendRichMessage("<gray>Entity UUID: <white>" + entity.getUniqueId());
            sender.sendRichMessage("<gray>Entity type: <white>" + entity.getType());
            sender.sendRichMessage("<gray>Entity name: <white>" + entity.getName());
            sender.sendRichMessage("<gray>Entity string: <white>" + entity.getAsString());
        }

        private String describeViewer(UUID playerUUID) {
            Player player = Bukkit.getPlayer(playerUUID);
            return player == null ? playerUUID.toString() : player.getName() + " (" + playerUUID + ")";
        }

        @DefaultExecutes
        public void helpCommand(@NotNull CommandSender sender) {
            sender.sendRichMessage("<white>Test subcommands:");
            sender.sendRichMessage("<green>/raycastedantiesp test location-drift <gray>- Tests the drift between Bukkit and PacketEvents entity locations");
            sender.sendRichMessage("<green>/raycastedantiesp test benchmark <player> [distance] <gray>- Benchmarks deterministic rays at the requested distance (default 50)");
            sender.sendRichMessage("<green>/raycastedantiesp test loaded-chunks <gray>- Shows the number of chunks currently loaded in the player's block view");
            sender.sendRichMessage("<green>/raycastedantiesp test entity-id <entity ID> [player] <gray>- Finds an entity by ID in one player's views, or in all player views when no player is supplied");
            sender.sendRichMessage("<green>/raycastedantiesp test entity-uuid <entity> <gray>- Shows Bukkit data and all tracked view data for a native entity selection or UUID");
        }
    }
}
