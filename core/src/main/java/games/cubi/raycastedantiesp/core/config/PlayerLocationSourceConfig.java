package games.cubi.raycastedantiesp.core.config;

import games.cubi.utils.events.CubiKey;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public record PlayerLocationSourceConfig(
        @Comment("Source used to get player locations, in namespace:key format. Prefer the provider's fully qualified plugin main class as the namespace. Changing this setting requires a restart.")
        CubiKey key
) implements Config {
    public static final PlayerLocationSourceConfig DEFAULT = new PlayerLocationSourceConfig(CubiKey.from(CubiKey.class, "bukkit-api"));
}
