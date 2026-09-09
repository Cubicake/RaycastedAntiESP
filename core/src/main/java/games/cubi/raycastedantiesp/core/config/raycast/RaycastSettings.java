package games.cubi.raycastedantiesp.core.config.raycast;

import games.cubi.raycastedantiesp.core.config.ConfigRange;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public record RaycastSettings(
        @Comment("Whether this category is checked.")
        boolean enabled,
        @Comment("Maximum number of occluding blocks permitted by a successful raycast.")
        @ConfigRange(min = 0, max = Byte.MAX_VALUE)
        byte maxOccludingCount,
        @Comment("Distance in blocks within which targets are always visible.")
        @ConfigRange(min = 0, max = Short.MAX_VALUE)
        short alwaysShowRadius,
        @Comment("Maximum distance in blocks at which targets are raycasted.")
        @ConfigRange(min = 0, max = Short.MAX_VALUE)
        short raycastRadius,
        @Comment("Distance in blocks beyond which a newly spawned target starts hidden.")
        @ConfigRange(min = 0, max = Short.MAX_VALUE)
        short hideOnSpawnDistance,
        @Comment("Ticks between checks of visible targets. -1 keeps a shown target visible permanently.")
        @ConfigRange(min = -1, max = Short.MAX_VALUE)
        short visibleRecheckIntervalTicks
) {
}
