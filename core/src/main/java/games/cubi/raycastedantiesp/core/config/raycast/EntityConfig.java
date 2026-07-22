package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.ConfigReader;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Set;

public class EntityConfig extends RaycastConfig {
    private final Set<String> excludedTypes;

    private EntityConfig(RaycastConfig config, List<String> excludedTypes) {
        super(config.enabled(), config.hideSoundsWhenHidden(), config.getMaxOccludingCount(), config.getAlwaysShowRadius(),
                config.getRaycastRadius(), config.hideOnSpawnDistance(), config.getVisibleRecheckIntervalTicks(),
                config.keepClientEntityWhenHidden());
        this.excludedTypes = Set.copyOf(excludedTypes);
    }

    public static EntityConfig load(ConfigurationNode node, String path) {
        RaycastConfig config = RaycastConfig.load(node, path, true, true);
        List<String> excludedTypes = ConfigReader.stringList(
                ConfigReader.node(node, "excluded-types"),
                path + ".excluded-types"
        );
        return new EntityConfig(config, excludedTypes);
    }

    public Set<String> excludedTypes() {
        return excludedTypes;
    }
}
