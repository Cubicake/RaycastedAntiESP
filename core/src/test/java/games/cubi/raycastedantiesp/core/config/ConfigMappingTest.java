package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.engine.EngineMode;
import games.cubi.raycastedantiesp.core.config.raycast.BlockSelector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMappingTest {
    @Test
    void codeDefaultsRoundTripThroughObjectMapper() throws SerializationException {
        CommentedConfigurationNode node = mappedDefaults();

        assertEquals(RootConfig.DEFAULT, node.get(RootConfig.class));
        assertEquals(RootConfig.DEFAULT.engineConfig().mode().getName(), node.node("engine", "mode").raw());
        assertInstanceOf(String.class, node.node("engine", "mode").raw());
        assertNotNull(node.node("checks", "player", "enabled").comment());
        assertEquals(
                List.copyOf(RootConfig.DEFAULT.checksConfig().entityConfig().excludedTypes()),
                node.node("checks", "entity", "excluded-types").getList(String.class)
        );
    }

    @Test
    void excludedTypesIncludesDetailedGuidance() throws SerializationException {
        CommentedConfigurationNode list = mappedDefaults().node("checks", "entity", "excluded-types");
        String comment = list.comment();

        assertNotNull(comment);
        assertTrue(comment.contains("minecraft:falling_block"));
        assertTrue(comment.contains("/summon command"));
        Map<String, String> guidance = Map.of(
                "minecraft:arrow", "Projectiles move very quickly",
                "minecraft:area_effect_cloud", "Potions and area-effect clouds",
                "minecraft:potion", "minecraft:potion entity was removed in 1.21.5",
                "minecraft:lightning_bolt", "Lightning is short-lived",
                "minecraft:ender_dragon", "Boss entities",
                "minecraft:block_display", "hologram or NPC plugins"
        );
        for (CommentedConfigurationNode child : list.childrenList()) {
            String expected = guidance.get(child.getString());
            if (expected == null) {
                assertNull(child.comment());
            } else {
                assertNotNull(child.comment());
                assertTrue(child.comment().contains(expected));
            }
        }
    }

    @Test
    void entryCommentsWorkOnAnotherListByValue(@TempDir Path directory) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(directory.resolve("list-comments.yml"))
                .defaultOptions(ConfigMapping.options())
                .build();
        CommentedConfigurationNode node = loader.createNode();
        CommentedList value = new CommentedList(List.of("second", "custom", "first"));
        node.set(CommentedList.class, value);

        CommentedConfigurationNode list = node.node("values");
        assertEquals(value.values(), list.getList(String.class));
        assertEquals("Second entry", list.node(0).comment());
        assertNull(list.node(1).comment());
        assertEquals("First entry", list.node(2).comment());
        list.node(1).comment("User-authored entry comment");

        loader.save(node);
        CommentedConfigurationNode loaded = loader.load();

        assertEquals(value, loaded.get(CommentedList.class));
        assertEquals("Second entry", loaded.node("values", 0).comment());
        assertEquals("User-authored entry comment", loaded.node("values", 1).comment());
        assertEquals("First entry", loaded.node("values", 2).comment());
    }

    @Test
    void engineAliasReadsButCanonicalNameWrites() throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigMapping.options());
        node.raw("simple");

        assertEquals(EngineMode.ASYNC, node.get(EngineMode.class));

        node.set(EngineMode.class, EngineMode.ASYNC);
        assertEquals(EngineMode.ASYNC.getName(), node.raw());
    }

    @Test
    void blockSelectorsRoundTripWithoutFillingOmittedProperties() throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigMapping.options());
        for (String value : List.of("minecraft:chest", "minecraft:chest[waterlogged=true]", "minecraft:chest[facing=north,type=single,waterlogged=false]", "example:block[type=left,facing=north]")) {
            node.raw(value);

            BlockSelector selector = node.get(BlockSelector.class);
            node.set(BlockSelector.class, selector);

            assertEquals(value, node.raw());
        }
    }

    @Test
    void annotationConstraintReportsTheMappedFieldPath() throws SerializationException {
        CommentedConfigurationNode node = mappedDefaults();
        NodePath expectedPath = NodePath.path("engine", "async", "processing-threads");
        node.node(expectedPath.array()).raw(0);

        SerializationException exception = assertThrows(SerializationException.class, () -> node.get(RootConfig.class));

        assertEquals(expectedPath, exception.path());
    }

    @Test
    void yamlLoaderRoundTripsMappedAndUserComments(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("comments.yml");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(path)
                .defaultOptions(ConfigMapping.options())
                .build();
        CommentedConfigurationNode node = loader.createNode();
        node.set(CommentedValue.class, new CommentedValue("mapped"));
        node.node("unknown").comment("User-authored comment").raw("preserved");

        loader.save(node);
        CommentedConfigurationNode loaded = loader.load();

        assertEquals("Mapped comment", loaded.node("value").comment());
        assertEquals("User-authored comment", loaded.node("unknown").comment());
    }

    private CommentedConfigurationNode mappedDefaults() throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigMapping.options());
        node.set(RootConfig.class, RootConfig.DEFAULT);
        return node;
    }

    @ConfigSerializable
    private record CommentedValue(@Comment("Mapped comment") String value) {
    }

    @ConfigSerializable
    private record CommentedList(
            @ListEntryComments({
                    @ListEntryComments.Entry(value = "first", comment = "First entry"),
                    @ListEntryComments.Entry(value = "second", comment = "Second entry"),
                    @ListEntryComments.Entry(value = "absent", comment = "Absent entry")
            })
            List<String> values
    ) {
    }
}
