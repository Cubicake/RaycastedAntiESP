package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.engine.EngineConfig;

import java.util.Map;

public record RootConfig(String configVersion, ChecksConfig checksConfig, EngineConfig engineConfig, BlockProcessorConfig blockProcessorConfig, DebugConfig debugConfig, UpdateConfig updateConfig, Map<Class<? extends Config>, Config> extensionConfigs) implements Config {
    public <T extends Config> T extensionConfig(Class<T> type) {
        Config config = extensionConfigs.get(type);
        return type.cast(config);
    }
}
