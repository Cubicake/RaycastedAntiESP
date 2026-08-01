package games.cubi.raycastedantiesp.paper;

import games.cubi.raycastedantiesp.core.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsTest {
    @Test
    void legacyVersionTwoConfigReceivesNewPlayerAndRetentionDefaults(@TempDir Path dataFolder) throws IOException {
        byte[] defaults = readDefaultConfig();
        String legacyConfig = new String(defaults, StandardCharsets.UTF_8)
                .replace("        keep-client-entity-when-hidden: true\r\n", "")
                .replace("        keep-client-entity-when-hidden: true\n", "")
                .replace("        only-check-sneaking: false\r\n", "")
                .replace("        only-check-sneaking: false\n", "");
        Path configPath = dataFolder.resolve("config.yml");
        Files.writeString(configPath, legacyConfig, StandardCharsets.UTF_8);

        ConfigManager manager = ConfigManager.initialiseConfigManager(
                () -> new ByteArrayInputStream(defaults), dataFolder, List.of());

        assertTrue(manager.getPlayerConfig().keepClientEntityWhenHidden());
        assertFalse(manager.getPlayerConfig().onlyCheckSneaking());
        assertTrue(manager.getEntityConfig().keepClientEntityWhenHidden());
        assertEquals("2.0", manager.getConfigFile().node("config-version").getString());

        String mergedConfig = Files.readString(configPath, StandardCharsets.UTF_8);
        assertEquals(2, mergedConfig.split("keep-client-entity-when-hidden: true", -1).length - 1);
        assertTrue(mergedConfig.contains("only-check-sneaking: false"));
    }

    private static byte[] readDefaultConfig() throws IOException {
        try (InputStream input = ConfigDefaultsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }
}
