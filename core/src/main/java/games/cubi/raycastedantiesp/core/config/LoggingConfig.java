package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record LoggingConfig(LogLevelConfig info, LogLevelConfig warn, LogLevelConfig error) implements Config {
    public static final LoggingConfig DEFAULT = new LoggingConfig(LogLevelConfig.DEFAULT, LogLevelConfig.DEFAULT, LogLevelConfig.DEFAULT);

    public LogLevelConfig forSeverity(Severity severity) {
        return switch (severity) {
            case INFO -> info;
            case WARN -> warn;
            case ERROR -> error;
        };
    }

    public enum Severity {
        INFO,
        WARN,
        ERROR
    }
}
