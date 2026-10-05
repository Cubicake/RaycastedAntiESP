package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Owns YAML loading, default-node creation, and replacement of config.yml through a temporary file. */
final class ConfigFile {
    private final Path dataFolder;
    private final Path configPath;
    private final YamlConfigurationLoader loader;

    ConfigFile(Path dataFolder) {
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        configPath = dataFolder.resolve("config.yml");
        loader = createLoader(configPath);
    }

    private YamlConfigurationLoader createLoader(Path path) {
        return YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .headerMode(HeaderMode.PRESERVE)
                .defaultOptions(ConfigMapping.options())
                .build();
    }

    CommentedConfigurationNode createDefaultNode() {
        CommentedConfigurationNode defaults = loader.createNode();
        try {
            defaults.set(RootConfig.class, RootConfig.DEFAULT);
        } catch (SerializationException e) {
            throw new ConfigLoadException("Code-defined configuration defaults could not be serialized", e);
        }
        return defaults;
    }

    CommentedConfigurationNode load() {
        try {
            Files.createDirectories(dataFolder);
            return Files.exists(configPath) ? loader.load() : loader.createNode();
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to load config.yml", e);
        }
    }

    void save(CommentedConfigurationNode node) {
        Path temporaryPath = null;
        try {
            temporaryPath = Files.createTempFile(dataFolder, "config-", ".yml.tmp");
            createLoader(temporaryPath).save(node);
            try {
                Files.move(temporaryPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
            temporaryPath = null;
        } catch (IOException e) {
            throw new ConfigLoadException("Failed to save config.yml", e);
        } finally {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
