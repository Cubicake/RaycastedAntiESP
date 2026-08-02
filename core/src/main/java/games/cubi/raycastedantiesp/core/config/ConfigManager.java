/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.engine.EngineConfig;
import games.cubi.raycastedantiesp.core.config.raycast.ChunkSectionConfig;
import games.cubi.raycastedantiesp.core.config.raycast.EntityConfig;
import games.cubi.raycastedantiesp.core.config.raycast.PlayerConfig;
import games.cubi.raycastedantiesp.core.config.raycast.SoundEffectsConfig;
import games.cubi.raycastedantiesp.core.config.raycast.TileEntityConfig;
import games.cubi.raycastedantiesp.core.utils.VarHandler;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.lang.invoke.VarHandle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigManager {
    private static final String REQUIRED_CONFIG_VERSION = "2.0";
    private static ConfigManager instance;

    private final Supplier<InputStream> resourceSupplier;
    private final Path dataFolder;
    private final Path configPath;
    private final YamlConfigurationLoader loader;
    private final List<ConfigExtension<? extends Config>> extensions;

    private ConfigurationNode config;
    private RootConfig startupConfig;
    private volatile RootConfig activeConfig; private static final VarHandle ACTIVE_CONFIG = VarHandler.get(ConfigManager.class, "activeConfig", RootConfig.class);

    private ConfigManager(Supplier<InputStream> resourceSupplier, Path dataFolder, List<ConfigExtension<? extends Config>> extensions) {
        this.resourceSupplier = resourceSupplier;
        this.dataFolder = dataFolder;
        this.extensions = List.copyOf(extensions);
        this.configPath = dataFolder.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        load();
    }

    public static ConfigManager initialiseConfigManager(Supplier<InputStream> resourceSupplier, Path dataFolder, List<ConfigExtension<? extends Config>> extensions) {
        if (instance == null) {
            instance = new ConfigManager(resourceSupplier, dataFolder, extensions);
        }
        return instance;
    }

    public static ConfigManager get() {
        if (instance == null) {
            Logger.errorAndReturn(new RuntimeException("ConfigManager accessed before being initiated. Please report this."), 2, ConfigManager.class);
        }
        return instance;
    }

    public void load() {
        ensureConfigFileExists();
        ConfigurationNode loaded = loadConfigNode();
        ConfigurationNode defaults = loadBundledDefaults();
        if (defaults != null && mergeMissing(defaults, loaded)) {
            saveConfigNode(loaded);
        }
        RootConfig parsed = parse(loaded);
        validateReload(parsed);
        config = loaded;
        if (startupConfig == null) {
            startupConfig = parsed;
        }
        ACTIVE_CONFIG.setOpaque(this, parsed);
    }

    public SetConfigResult setConfigValue(String path, String rawValue) {
        ConfigurationNode candidate = loadConfigNode();
        ConfigurationNode target = node(candidate, path);
        if (target.virtual()) {
            return SetConfigResult.invalid("Unknown config path: " + path);
        }

        ConfigReader.setRaw(target, ConfigReader.parseRawValue(rawValue));

        return applyCandidate(candidate, false);
    }

    public SetConfigResult addConfigListValue(String path, String rawValue) {
        return mutateConfigListValue(path, rawValue, ListMutation.ADD);
    }

    public SetConfigResult removeConfigListValue(String path, String rawValue) {
        return mutateConfigListValue(path, rawValue, ListMutation.REMOVE);
    }

    private SetConfigResult mutateConfigListValue(String path, String rawValue, ListMutation mutation) {
        ConfigurationNode candidate = loadConfigNode();
        ConfigurationNode target = node(candidate, path);
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

        Object parsedValue = ConfigReader.parseRawValue(rawValue);
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

        ConfigReader.setRaw(target, values);
        return applyCandidate(candidate, true);
    }

    private SetConfigResult applyCandidate(ConfigurationNode candidate, boolean allowRestartRequired) {
        RootConfig parsed;
        try {
            parsed = parse(candidate);
            validateReload(parsed);
        } catch (RestartRequiredException e) {
            if (!allowRestartRequired) {
                return SetConfigResult.invalid(e.getMessage());
            }
            config = candidate;
            saveConfigNode(candidate);
            return SetConfigResult.restartRequired(e.getMessage());
        } catch (ConfigLoadException e) {
            return SetConfigResult.invalid(e.getMessage());
        }

        config = candidate;
        ACTIVE_CONFIG.setOpaque(this, parsed);
        saveConfigNode(candidate);
        return SetConfigResult.ok();
    }

    public PlayerConfig getPlayerConfig() {
        return activeConfig().checksConfig().playerConfig();
    }

    public EntityConfig getEntityConfig() {
        return activeConfig().checksConfig().entityConfig();
    }

    public TileEntityConfig getTileEntityConfig() {
        return activeConfig().checksConfig().tileEntityConfig();
    }

    public SoundEffectsConfig getSoundEffectsConfig() {
        return activeConfig().checksConfig().soundEffectsConfig();
    }

    public ChunkSectionConfig getChunkSectionConfig() {
        return activeConfig().checksConfig().chunkSectionConfig();
    }

    public DebugConfig getDebugConfig() {
        RootConfig current = activeConfig();
        return current == null ? null : current.debugConfig();
    }

    public UpdateConfig getUpdateConfig() {
        return activeConfig().updateConfig();
    }

    public EngineConfig getEngineConfig() {
        return activeConfig().engineConfig();
    }

    public BlockProcessorConfig getBlockProcessorConfig() {
        return activeConfig().blockProcessorConfig();
    }

    public <T extends Config> T getExtensionConfig(Class<T> type) {
        return activeConfig().extensionConfig(type);
    }

    private RootConfig activeConfig() {
        return (RootConfig) ACTIVE_CONFIG.getOpaque(this);
    }

    public ConfigurationNode getConfigFile() {
        return config;
    }

    public Map<String, Object> getConfigValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        collectConfigValues(config, "", values);
        return values;
    }

    private RootConfig parse(ConfigurationNode loaded) {
        String version = ConfigReader.string(ConfigReader.node(loaded, "config-version"), "config-version");
        if (!REQUIRED_CONFIG_VERSION.equals(version)) {
            throw new ConfigLoadException("Unsupported config-version '" + version + "'. RaycastedAntiESP requires config-version '2.0'.");
        }

        ChecksConfig checksConfig = ChecksConfig.load(loaded);
        EngineConfig engineConfig = EngineConfig.load(loaded);
        BlockProcessorConfig blockProcessorConfig = BlockProcessorConfig.load(loaded);
        DebugConfig debugConfig = DebugConfig.load(loaded);
        UpdateConfig updateConfig = UpdateConfig.load(loaded);
        Map<Class<? extends Config>, Config> extensionConfigs = new LinkedHashMap<>();
        for (ConfigExtension<? extends Config> extension : extensions) {
            extensionConfigs.put(extension.type(), extension.load(loaded, blockProcessorConfig));
        }

        if (!blockProcessorConfig.trackAllBlocks() && checksConfig.chunkSectionConfig().enabled()) {
            throw new ConfigLoadException("checks.chunk-section.enabled must be false when block-processor.track-all-blocks is false");
        }

        return new RootConfig(version, checksConfig, engineConfig, blockProcessorConfig, debugConfig, updateConfig, Map.copyOf(extensionConfigs));
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
        if (next.checksConfig().hasEnabledStatusChanges(startupConfig.checksConfig())) {
            throw new RestartRequiredException("player and entity checks cannot be enabled or disabled without a restart.");
        }
        for (ConfigExtension<? extends Config> extension : extensions) {
            validateExtensionReload(extension, next);
        }
    }

    private <T extends Config> void validateExtensionReload(ConfigExtension<T> extension, RootConfig next) {
        T startupExtensionConfig = startupConfig.extensionConfig(extension.type());
        T nextExtensionConfig = next.extensionConfig(extension.type());
        if (extension.requiresRestart(startupExtensionConfig, nextExtensionConfig)) {
            throw new RestartRequiredException("block-processor." + extension.type().getSimpleName() + " cannot be changed without a restart");
        }
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
            String nextPath = path.isEmpty() ? key : path + "." + key;
            collectConfigValues(entry.getValue(), nextPath, values);
        }
    }

    private void ensureConfigFileExists() {
        try {
            Files.createDirectories(dataFolder);
            if (!Files.exists(configPath)) {
                InputStream resource = resourceSupplier.get();
                if (resource != null) {
                    try (resource) {
                        Files.copy(resource, configPath);
                    }
                } else {
                    Files.createFile(configPath);
                }
            }
            prependConfigDocumentationHeaderIfMissing();
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to create config.yml", e);
        }
    }

    private void prependConfigDocumentationHeaderIfMissing() {
        try {
            String content = Files.readString(configPath);

            if (content.startsWith("#")) {
                Logger.debug("Config starts with comment");
                return;
            }

            Logger.debug("no starting comment");
            Files.writeString(configPath,"# An explanation of this configuration file and what all the options do can be found at https://raycastedantiesp.cubi.games/config/");
        } catch (Exception e) {
            Logger.warning(e,3, ConfigManager.class);
        }
    }

    private ConfigurationNode loadConfigNode() {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load config.yml", e);
        }
    }

    private ConfigurationNode loadBundledDefaults() {
        InputStream resource = resourceSupplier.get();
        if (resource == null) {
            return null;
        }
        try (resource) {
            return YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(resource)))
                    .build()
                    .load();
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load bundled config defaults", e);
        }
    }

    private boolean mergeMissing(ConfigurationNode defaults, ConfigurationNode target) {
        if (target.virtual()) {
            target.from(defaults);
            return true;
        }

        boolean changed = false;
        if (!defaults.childrenMap().isEmpty()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : defaults.childrenMap().entrySet()) {
                changed |= mergeMissing(entry.getValue(), target.node(entry.getKey()));
            }
        }
        return changed;
    }

    private void saveConfigNode(ConfigurationNode node) {
        try {
            loader.save(node);
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to save config.yml", e);
        }
    }

    private ConfigurationNode node(ConfigurationNode root, String path) {
        String[] parts = path.split("\\.");
        return ConfigReader.node(root, parts);
    }

    private enum ListMutation {
        ADD,
        REMOVE
    }

    private static final class RestartRequiredException extends ConfigLoadException {
        private RestartRequiredException(String message) {
            super(message);
        }
    }

    public record SetConfigResult(boolean success, boolean restartRequired, String message) {
        public static SetConfigResult ok() {
            return new SetConfigResult(true, false, "Config value updated.");
        }

        public static SetConfigResult restartRequired(String message) {
            return new SetConfigResult(true, true, message);
        }

        public static SetConfigResult invalid(String message) {
            return new SetConfigResult(false, false, message);
        }
    }
}
