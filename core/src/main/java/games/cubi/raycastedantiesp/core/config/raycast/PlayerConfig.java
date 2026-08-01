package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.ConfigReader;
import org.spongepowered.configurate.ConfigurationNode;

public class PlayerConfig extends RaycastConfig {
    private final boolean onlyCheckSneaking;

    private PlayerConfig(RaycastConfig config, boolean onlyCheckSneaking) {
        super(config.enabled(), config.hideSoundsWhenHidden(), config.getMaxOccludingCount(), config.getAlwaysShowRadius(),
                config.getRaycastRadius(), config.hideOnSpawnDistance(), config.getVisibleRecheckIntervalTicks(),
                config.keepClientEntityWhenHidden());
        this.onlyCheckSneaking = onlyCheckSneaking;
    }

    public static PlayerConfig load(ConfigurationNode node, String path) {
        return new PlayerConfig(
                RaycastConfig.load(node, path, true, true),
                ConfigReader.bool(ConfigReader.node(node, "only-check-sneaking"), path + ".only-check-sneaking")
        );
    }

    public boolean onlyCheckSneaking() {
        return onlyCheckSneaking;
    }
}
