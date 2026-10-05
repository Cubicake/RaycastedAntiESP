/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP. RaycastedAntiESP is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at
 * https://www.gnu.org/licenses/agpl-3.0.html. See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.engine.EngineConfig;
import games.cubi.raycastedantiesp.core.config.raycast.ChunkSectionConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.SoundEffectsConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.utils.VarHandler;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates startup, runtime edits, and publication of configuration snapshots.
 * ConfigCandidate prepares values and repairs; ConfigFile handles persistence.
 */
public class ConfigManager {
    private static volatile ConfigManager instance;
    private static final VarHandle INSTANCE = VarHandler.$tatic(ConfigManager.class, "instance", ConfigManager.class);

    private final ConfigFile configFile;

    private volatile ActiveState activeState = new ActiveState(LoggingConfig.DEFAULT, null);
    private static final VarHandle ACTIVE_STATE = VarHandler.get(ConfigManager.class, "activeState", ActiveState.class);
    private RootConfig startupConfig;
    private CommentedConfigurationNode configNode;

    ConfigManager(Path dataFolder) {
        configFile = new ConfigFile(dataFolder);
    }

    public static synchronized ConfigManager initialiseConfigManager(Path dataFolder) {
        ConfigManager current = (ConfigManager) INSTANCE.getAcquire();
        if (current == null) {
            ConfigManager manager = new ConfigManager(dataFolder);
            INSTANCE.setRelease(manager);
            try {
                manager.loadLoggingConfig();
            } catch (RuntimeException exception) {
                INSTANCE.setRelease(null);
                throw exception;
            }
            current = manager;
        }
        return current;
    }

    public static ConfigManager get() {
        ConfigManager current = (ConfigManager) INSTANCE.getAcquire();
        if (current == null) {
            Logger.errorAndReturn(new RuntimeException("ConfigManager accessed before being initiated. Please report this."), 2, ConfigManager.class);
        }
        return current;
    }

    public static LoggingConfig getLoggingConfigOrDefault() {
        ConfigManager current = (ConfigManager) INSTANCE.getAcquire();
        return current == null ? LoggingConfig.DEFAULT : current.getLoggingConfig();
    }

    public synchronized void completeInitialLoad() {
        if (startupConfig != null) {
            throw new IllegalStateException("Configuration loading has already been completed");
        }

        ConfigCandidate candidate = prepare();
        RootConfig ready = candidate.parseReadyConfig();
        saveIfChanged(candidate);

        startupConfig = ready;
        configNode = candidate.node();
        ACTIVE_STATE.setRelease(this, new ActiveState(ready.loggingConfig(), ready));
        logRepairs(candidate.repairs());
    }

    /**
     * Reloads the complete configuration. The active snapshot is replaced only after parsing and validation succeed.
     */
    public synchronized void load() {
        requireReady();
        ConfigCandidate candidate = prepare();
        RootConfig ready = candidate.parseReadyConfig();
        saveIfChanged(candidate);
        configNode = candidate.node();
        logRepairs(candidate.repairs());
        validateReload(ready);
        ACTIVE_STATE.setRelease(this, new ActiveState(ready.loggingConfig(), ready));
    }

    public synchronized SetConfigResult setConfigValue(String path, String rawValue) {
        requireReady();
        ConfigCandidate candidate = prepare();
        ConfigurationNode target = node(candidate.node(), path);
        if (target.virtual()) {
            return SetConfigResult.invalid("Unknown config path: " + path);
        }

        try {
            Object parsedValue = parseRawValue(rawValue);
            target.set(parsedValue);
            candidate.markDirty();
            NodePath targetPath = NodePath.path((Object[]) path.split("\\."));
            if (parsedValue == null && candidate.hasDefault(targetPath)) {
                candidate.repair(targetPath, "value cannot be null");
            }
        } catch (ConfigurateException e) {
            return SetConfigResult.invalid("Invalid YAML value: " + rawValue);
        }
        return applyCandidate(candidate, false);
    }

    public synchronized SetConfigResult addConfigListValue(String path, String rawValue) {
        requireReady();
        return mutateConfigListValue(path, rawValue, ListMutation.ADD);
    }

    public synchronized SetConfigResult removeConfigListValue(String path, String rawValue) {
        requireReady();
        return mutateConfigListValue(path, rawValue, ListMutation.REMOVE);
    }

    private SetConfigResult mutateConfigListValue(String path, String rawValue, ListMutation mutation) {
        ConfigCandidate candidate = prepare();
        ConfigurationNode target = node(candidate.node(), path);
        if (target.virtual()) {
            return SetConfigResult.invalid("Unknown config path: " + path);
        }
        if (!target.childrenMap().isEmpty()) {
            return SetConfigResult.invalid(path + " is not a list path");
        }

        List<Object> values = new ArrayList<>(target.childrenList().stream().map(ConfigurationNode::raw).toList());
        if (values.isEmpty() && target.raw() != null && !(target.raw() instanceof List<?>)) {
            return SetConfigResult.invalid(path + " is not a list path");
        }

        Object parsedValue;
        try {
            parsedValue = parseRawValue(rawValue);
        } catch (ConfigurateException e) {
            return SetConfigResult.invalid("Invalid YAML value: " + rawValue);
        }

        boolean changed = switch (mutation) {
            case ADD -> {
                if (values.contains(parsedValue)) {
                    yield false;
                }
                values.add(parsedValue);
                yield true;
            }
            case REMOVE -> values.remove(parsedValue);
        };
        if (!changed) {
            return SetConfigResult.invalid("No change made for " + path);
        }

        try {
            target.set(values);
            candidate.markDirty();
        } catch (SerializationException e) {
            return SetConfigResult.invalid("Failed to update " + path);
        }
        return applyCandidate(candidate, true);
    }

    private SetConfigResult applyCandidate(ConfigCandidate candidate, boolean allowRestartRequired) {
        candidate.mergeDefaults();
        RootConfig ready;
        try {
            ready = candidate.parseReadyConfig();
            validateReload(ready);
        } catch (RestartRequiredException e) {
            if (!allowRestartRequired) {
                return SetConfigResult.invalid(e.getMessage());
            }
            saveIfChanged(candidate);
            configNode = candidate.node();
            logRepairs(candidate.repairs());
            return SetConfigResult.restartRequired(e.getMessage(), candidate.repairs());
        } catch (ConfigLoadException e) {
            return SetConfigResult.invalid(e.getMessage());
        }

        saveIfChanged(candidate);
        configNode = candidate.node();
        ACTIVE_STATE.setRelease(this, new ActiveState(ready.loggingConfig(), ready));
        logRepairs(candidate.repairs());
        return SetConfigResult.ok(candidate.repairs());
    }

    public LoggingConfig getLoggingConfig() {
        return ((ActiveState) ACTIVE_STATE.getAcquire(this)).loggingConfig;
    }

    public PlayerConfig getPlayerConfig() {
        return requireReady().checksConfig().playerConfig();
    }

    public EntityConfig getEntityConfig() {
        return requireReady().checksConfig().entityConfig();
    }

    public TileEntityConfig getTileEntityConfig() {
        return requireReady().checksConfig().tileEntityConfig();
    }

    public SoundEffectsConfig getSoundEffectsConfig() {
        return requireReady().checksConfig().soundEffectsConfig();
    }

    public ChunkSectionConfig getChunkSectionConfig() {
        return requireReady().checksConfig().chunkSectionConfig();
    }

    public DebugConfig getDebugConfig() {
        return requireReady().debugConfig();
    }

    public UpdateConfig getUpdateConfig() {
        return requireReady().updateConfig();
    }

    public EngineConfig getEngineConfig() {
        return requireReady().engineConfig();
    }

    public BlockProcessorConfig getBlockProcessorConfig() {
        return requireReady().blockProcessorConfig();
    }

    public synchronized Map<String, Object> getConfigValues() {
        requireReady();
        Map<String, Object> values = new LinkedHashMap<>();
        collectConfigValues(configNode, "", values);
        return Collections.unmodifiableMap(values);
    }

    void loadLoggingConfig() {
        ConfigCandidate candidate = prepare();
        LoggingConfig logging = candidate.parseLoggingConfig();
        saveIfChanged(candidate);
        configNode = candidate.node();
        ACTIVE_STATE.setRelease(this, new ActiveState(logging, null));
        logRepairs(candidate.repairs());
    }

    private ConfigCandidate prepare() {
        return new ConfigCandidate(configFile.load(), configFile.createDefaultNode());
    }

    private void saveIfChanged(ConfigCandidate candidate) {
        if (candidate.isDirty()) {
            configFile.save(candidate.node());
        }
    }

    private Object parseRawValue(String rawValue) throws ConfigurateException {
        ConfigurationNode parsed = YamlConfigurationLoader.builder()
                .defaultOptions(ConfigMapping.options())
                .buildAndLoadString("value: " + rawValue);
        return parsed.node("value").raw();
    }

    private void validateReload(RootConfig next) {
        if (startupConfig == null) {
            return;
        }
        if (next.engineConfig().mode() != startupConfig.engineConfig().mode()) {
            throw new RestartRequiredException("engine.mode cannot be changed without a restart.");
        }
        if (!next.blockProcessorConfig().equals(startupConfig.blockProcessorConfig())) {
            throw new RestartRequiredException("block-processor cannot be changed without a restart.");
        }
        if (!next.checksConfig().entityConfig().excludedTypes().equals(startupConfig.checksConfig().entityConfig().excludedTypes())) {
            throw new RestartRequiredException("excluded entity types cannot be changed without a restart.");
        }
        if (!next.checksConfig().tileEntityConfig().excludedBlocks().equals(startupConfig.checksConfig().tileEntityConfig().excludedBlocks())) {
            throw new RestartRequiredException("excluded tile-entity blocks cannot be changed without a restart.");
        }
        if (next.checksConfig().hasEnabledStatusChanges(startupConfig.checksConfig())) {
            throw new RestartRequiredException("player and entity checks cannot be enabled or disabled without a restart.");
        }
    }

    private RootConfig requireReady() {
        RootConfig ready = ((ActiveState) ACTIVE_STATE.getAcquire(this)).readyConfig;
        if (ready == null) {
            throw new IllegalStateException("Configuration is not available until completeInitialLoad has finished");
        }
        return ready;
    }

    private void logRepairs(List<ConfigCandidate.Repair> repairs) {
        for (ConfigCandidate.Repair repair : repairs) {
            String reason = repair.reason() == null || repair.reason().isBlank() ? "invalid value" : repair.reason();
            String message = "Invalid config value at " + repair.pathString() + ": " + reason
                    + ". Replaced it with the default " + displayValue(repair.defaultValue()) + ".";
            Logger.warning(message, 4, ConfigManager.class);
        }
    }

    private String displayValue(Object value) {
        String displayed = String.valueOf(value);
        return displayed.length() <= 160 ? displayed : displayed.substring(0, 157) + "...";
    }

    private void collectConfigValues(ConfigurationNode node, String path, Map<String, Object> values) {
        if (node.childrenMap().isEmpty() && node.childrenList().isEmpty()) {
            if (!path.isEmpty() && !node.virtual()) {
                values.put(path, node.raw());
            }
            return;
        }
        if (!node.childrenList().isEmpty()) {
            values.put(path, node.childrenList().stream().map(ConfigurationNode::raw).toList());
            return;
        }
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String key = String.valueOf(entry.getKey());
            collectConfigValues(entry.getValue(), path.isEmpty() ? key : path + "." + key, values);
        }
    }

    private ConfigurationNode node(ConfigurationNode root, String path) {
        return root.node((Object[]) path.split("\\."));
    }

    private enum ListMutation {
        ADD,
        REMOVE
    }

    private record ActiveState(LoggingConfig loggingConfig, RootConfig readyConfig) {
    }

    private static final class RestartRequiredException extends ConfigLoadException {
        private RestartRequiredException(String message) {
            super(message);
        }
    }

    public record SetConfigResult(boolean success, boolean restartRequired, boolean repaired, String message) {
        private static SetConfigResult ok(List<ConfigCandidate.Repair> repairs) {
            if (repairs.isEmpty()) {
                return new SetConfigResult(true, false, false, "Config value updated.");
            }
            return new SetConfigResult(true, false, true, repairMessage(repairs));
        }

        private static SetConfigResult restartRequired(String message, List<ConfigCandidate.Repair> repairs) {
            String result = repairs.isEmpty() ? message : message + " " + repairMessage(repairs);
            return new SetConfigResult(true, true, !repairs.isEmpty(), result);
        }

        public static SetConfigResult invalid(String message) {
            return new SetConfigResult(false, false, false, message);
        }

        private static String repairMessage(List<ConfigCandidate.Repair> repairs) {
            Set<String> paths = new LinkedHashSet<>();
            for (ConfigCandidate.Repair repair : repairs) {
                paths.add(repair.pathString());
            }
            return "Invalid values at " + String.join(", ", paths) + " were replaced with defaults.";
        }
    }
}
