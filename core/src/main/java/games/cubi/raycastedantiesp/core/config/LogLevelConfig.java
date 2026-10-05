package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

@ConfigSerializable
public record LogLevelConfig(
        @Comment("Messages above this level are suppressed. Set to 0 to suppress this severity.")
        @ConfigRange(min = 0, max = 10)
        byte level,
        @Comment("Simple or fully qualified class names whose messages should be suppressed.")
        List<String> exemptedClasses
) implements Config {
    public static final LogLevelConfig DEFAULT = new LogLevelConfig((byte) 5, List.of());

    public LogLevelConfig {
        exemptedClasses = List.copyOf(exemptedClasses);
    }

    public boolean isExempted(Class<?>... sources) {
        if (sources == null || exemptedClasses.isEmpty()) {
            return false;
        }
        for (Class<?> source : sources) {
            if (source != null && (exemptedClasses.contains(source.getSimpleName()) || exemptedClasses.contains(source.getName()))) {
                return true;
            }
        }
        return false;
    }
}
