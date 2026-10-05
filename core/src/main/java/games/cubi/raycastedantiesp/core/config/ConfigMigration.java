package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.MoveStrategy;

/** Upgrades supported on-disk schemas before defaults are merged or objects are mapped. */
final class ConfigMigration {
    private static final String PREVIOUS_CONFIG_VERSION = "2.0";
    private static final ConfigurationTransformation VERSION_2_1_TRANSFORMATION = createVersion21Transformation();

    private ConfigMigration() {
    }

    static boolean migrate(CommentedConfigurationNode node) {
        ConfigurationNode versionNode = node.node("config-version");
        Object rawVersion = versionNode.raw();
        if (rawVersion == null) {
            versionNode.raw(RootConfig.CURRENT_VERSION);
            return true;
        }
        if (!(rawVersion instanceof String version)) {
            throw new ConfigLoadException("config-version must be a string");
        }
        if (RootConfig.CURRENT_VERSION.equals(version)) {
            return false;
        }
        if (!PREVIOUS_CONFIG_VERSION.equals(version)) {
            throw unsupportedVersion(version);
        }

        try {
            VERSION_2_1_TRANSFORMATION.apply(node);
        } catch (ConfigurateException e) {
            throw new ConfigLoadException("Failed to migrate config-version '" + PREVIOUS_CONFIG_VERSION + "' to '" + RootConfig.CURRENT_VERSION + "'", e);
        }
        versionNode.raw(RootConfig.CURRENT_VERSION);
        return true;
    }

    static ConfigLoadException unsupportedVersion(String version) {
        return new ConfigLoadException(
                "Unsupported config-version '" + version + "'. RaycastedAntiESP requires config-version '"
                        + RootConfig.CURRENT_VERSION + "'."
        );
    }

    private static ConfigurationTransformation createVersion21Transformation() {
        ConfigurationTransformation.Builder builder = ConfigurationTransformation.builder().moveStrategy(MoveStrategy.MERGE);
        addMove(builder, "info-level", "info", "level");
        addMove(builder, "info-exempted-classes", "info", "exempted-classes");
        addMove(builder, "warn-level", "warn", "level");
        addMove(builder, "warn-exempted-classes", "warn", "exempted-classes");
        addMove(builder, "error-level", "error", "level");
        addMove(builder, "error-exempted-classes", "error", "exempted-classes");
        builder.addAction(NodePath.path("block-processor", "packetevents"), (path, value) -> {
            value.raw(null);
            return null;
        });
        return builder.build();
    }

    private static void addMove(ConfigurationTransformation.Builder builder, String oldKey, String severity, String newKey) {
        builder.addAction(NodePath.path("debug", oldKey), (path, value) -> new Object[] {"logging", severity, newKey});
    }
}
