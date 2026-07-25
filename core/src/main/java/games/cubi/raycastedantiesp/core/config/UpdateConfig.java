package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.ConfigurationNode;

public record UpdateConfig(boolean checkRelease, boolean checkBeta, boolean checkAlpha, boolean notifyIngame) implements Config {

    public static UpdateConfig load(ConfigurationNode root) {
        ConfigurationNode node = ConfigReader.node(root, "updates");
        return new UpdateConfig(
                ConfigReader.bool(ConfigReader.node(node, "check-release"), "updates.check-release"),
                ConfigReader.bool(ConfigReader.node(node, "check-beta"), "updates.check-beta"),
                ConfigReader.bool(ConfigReader.node(node, "check-alpha"), "updates.check-alpha"),
                ConfigReader.bool(ConfigReader.node(node, "notify-ingame"), "updates.notify-ingame")
        );
    }

    public boolean anyChannelEnabled() {
        return checkRelease || checkBeta || checkAlpha;
    }
}
