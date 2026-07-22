package games.cubi.raycastedantiesp.paper.config;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.EntityTypeExclusions;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PaperEntityTypeExclusionResolver {
    private static final String CONFIG_PATH = "checks.entity.excluded-types";

    private PaperEntityTypeExclusionResolver() {}

    public static void resolveAndInitialise(Collection<String> configuredNames) {
        ClientVersion version = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String configuredName : configuredNames) {
            String normalized = normalize(configuredName);
            if (normalized != null) {
                normalizedNames.add(normalized);
            }
        }

        IntOpenHashSet excludedTypes = new IntOpenHashSet(normalizedNames.size());
        for (String normalizedName : normalizedNames) {
            NamespacedKey key = NamespacedKey.fromString(normalizedName);
            if (key == null) {
                warn("contains invalid resource name '" + normalizedName + "'");
                continue;
            }

            org.bukkit.entity.EntityType paperType = Registry.ENTITY_TYPE.get(key);
            if (paperType == null) {
                warn("contains unknown entity type '" + normalizedName + "'");
                continue;
            }
            if (paperType == org.bukkit.entity.EntityType.PLAYER) {
                warn("cannot exclude player entity type '" + normalizedName + "'");
                continue;
            }

            EntityType packetEventsType = SpigotConversionUtil.fromBukkitEntityType(paperType);
            if (packetEventsType == null) {
                warn("contains entity type '" + normalizedName + "' which is known to Paper but not PacketEvents");
                continue;
            }

            int entityType = packetEventsType.getId(version);
            if (entityType < 0) {
                warn("contains entity type '" + normalizedName + "' which has no concrete PacketEvents ID for " + version);
                continue;
            }
            excludedTypes.add(entityType);
        }

        EntityTypeExclusions.initialise(excludedTypes);
        Logger.info("Resolved " + EntityTypeExclusions.size() + " excluded entity types.", 5);
    }

    private static String normalize(String configuredName) {
        String trimmed = configuredName.trim();
        if (trimmed.isEmpty()) {
            warn("contains an empty entry");
            return null;
        }
        return trimmed.indexOf(':') < 0 ? "minecraft:" + trimmed : trimmed;
    }

    private static void warn(String message) {
        Logger.warning(CONFIG_PATH + " " + message + ". The entry will be ignored.", 4,
                PaperEntityTypeExclusionResolver.class);
    }
}
