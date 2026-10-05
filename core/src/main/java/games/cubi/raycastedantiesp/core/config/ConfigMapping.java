package games.cubi.raycastedantiesp.core.config;

import games.cubi.raycastedantiesp.core.config.engine.EngineMode;
import games.cubi.raycastedantiesp.core.config.raycast.BlockSelector;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.NamingSchemes;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.LongFunction;

final class ConfigMapping {
    private static final String HEADER = "An explanation of this configuration file and what all the options do can be found at https://raycastedantiesp.cubi.games/config/";
    private static final String LEGACY_HEADER = "An explanation of this configuration file and what the options do can be found at https://raycastedantiesp.cubi.games/config/";
    private static final ConfigurationOptions OPTIONS = createOptions();

    private ConfigMapping() {
    }

    static ConfigurationOptions options() {
        return OPTIONS;
    }

    static boolean removeDuplicateHeader(CommentedConfigurationNode node) {
        // Without a blank line, the legacy YAML header is loaded as a comment on config-version.
        CommentedConfigurationNode version = node.node("config-version");
        String comment = version.comment();
        if (comment == null) {
            return false;
        }
        var lines = comment.lines().toList();
        var remaining = lines.stream().filter(line -> {
            String normalized = line.strip().replace("https\\://", "https://");
            return !LEGACY_HEADER.equals(normalized) && !HEADER.equals(normalized);
        }).toList();
        if (remaining.size() == lines.size()) {
            return false;
        }
        String retainedComment = String.join("\n", remaining);
        version.comment(retainedComment.isBlank() ? null : retainedComment);
        return true;
    }

    private static ConfigurationOptions createOptions() {
        ObjectMapper.Factory mapper = ObjectMapper.factoryBuilder()
                .defaultNamingScheme(NamingSchemes.LOWER_CASE_DASHED)
                .addProcessor(ListEntryComments.class, (annotation, type) -> {
                    Map<String, String> comments = new HashMap<>();
                    for (ListEntryComments.Entry entry : annotation.value()) {
                        comments.put(entry.value(), entry.comment());
                    }
                    return (value, node) -> {
                        for (ConfigurationNode child : node.childrenList()) {
                            if (child.rawScalar() instanceof String entry && child instanceof CommentedConfigurationNode commented) {
                                String comment = comments.get(entry);
                                if (comment != null) {
                                    commented.commentIfAbsent(comment);
                                }
                            }
                        }
                    };
                })
                .addConstraint(ConfigRange.class, (annotation, type) -> value -> {
                    if (!(value instanceof Number number)) {
                        throw new SerializationException(type, "Expected a numeric value");
                    }
                    long numericValue = number.longValue();
                    if (numericValue < annotation.min() || numericValue > annotation.max()) {
                        throw new SerializationException(type, "Value must be between " + annotation.min() + " and " + annotation.max());
                    }
                })
                .build();

        TypeSerializer<String> stringSerializer = strictScalarSerializer(
                String.class,
                "a string",
                String.class::cast,
                value -> value
        );
        TypeSerializer<Boolean> booleanSerializer = strictScalarSerializer(
                Boolean.class,
                "a boolean",
                Boolean.class::cast,
                value -> value
        );
        TypeSerializer<Byte> byteSerializer = strictIntegerSerializer("a byte", Byte.MIN_VALUE, Byte.MAX_VALUE, value -> (byte) value);
        TypeSerializer<Short> shortSerializer = strictIntegerSerializer("a short", Short.MIN_VALUE, Short.MAX_VALUE, value -> (short) value);
        TypeSerializer<Integer> integerSerializer = strictIntegerSerializer("an integer", Integer.MIN_VALUE, Integer.MAX_VALUE, value -> (int) value);

        TypeSerializerCollection serializers = TypeSerializerCollection.defaults().childBuilder()
                .registerExact(String.class, stringSerializer)
                .registerExact(boolean.class, booleanSerializer)
                .registerExact(Boolean.class, booleanSerializer)
                .registerExact(byte.class, byteSerializer)
                .registerExact(Byte.class, byteSerializer)
                .registerExact(short.class, shortSerializer)
                .registerExact(Short.class, shortSerializer)
                .registerExact(int.class, integerSerializer)
                .registerExact(Integer.class, integerSerializer)
                .registerExact(BlockSelector.class, blockSelectorSerializer())
                .registerExact(EngineMode.class, enumSerializer(EngineMode::fromString, EngineMode::getName))
                .registerExact(BlockProcessorMode.class, enumSerializer(BlockProcessorMode::fromString, BlockProcessorMode::getName))
                .registerAnnotatedObjects(mapper)
                .build();

        return ConfigurationOptions.defaults()
                .header(HEADER)
                .serializers(serializers)
                .shouldCopyDefaults(true);
    }

    private static <T> TypeSerializer<T> strictScalarSerializer(
            Class<T> type,
            String expected,
            Function<Object, T> reader,
            Function<T, Object> writer
    ) {
        return new TypeSerializer<>() {
            @Override
            public T deserialize(@NotNull Type expectedType, @NotNull ConfigurationNode node) throws SerializationException {
                Object raw = node.rawScalar();
                if (!type.isInstance(raw)) {
                    throw new SerializationException(expectedType, "Expected " + expected);
                }
                return reader.apply(raw);
            }

            @Override
            public void serialize(@NotNull Type expectedType, T value, @NotNull ConfigurationNode node) {
                node.raw(value == null ? null : writer.apply(value));
            }
        };
    }

    private static <T extends Number> TypeSerializer<T> strictIntegerSerializer(String expected, long minimum, long maximum, LongFunction<T> converter) {
        return new TypeSerializer<>() {
            @Override
            public T deserialize(@NotNull Type expectedType, @NotNull ConfigurationNode node) throws SerializationException {
                Object raw = node.rawScalar();
                if (!(raw instanceof Number number)) {
                    throw new SerializationException(expectedType, "Expected " + expected);
                }
                long value = exactLong(expectedType, number);
                if (value < minimum || value > maximum) {
                    throw new SerializationException(expectedType, "Value is outside the supported range for " + expected);
                }
                return converter.apply(value);
            }

            @Override
            public void serialize(@NotNull Type expectedType, T value, @NotNull ConfigurationNode node) {
                node.raw(value == null ? null : value.intValue());
            }
        };
    }

    private static long exactLong(Type expectedType, Number number) throws SerializationException {
        try {
            return switch (number) {
                case Byte ignored -> number.longValue();
                case Short ignored -> number.longValue();
                case Integer ignored -> number.longValue();
                case Long ignored -> number.longValue();
                case BigInteger value -> value.longValueExact();
                case BigDecimal value -> value.longValueExact();
                default -> exactLongFromFloatingPoint(expectedType, number.doubleValue());
            };
        } catch (ArithmeticException exception) {
            throw new SerializationException(expectedType, "Expected an integer", exception);
        }
    }

    private static long exactLongFromFloatingPoint(Type expectedType, double value) throws SerializationException {
        if (!Double.isFinite(value) || Math.rint(value) != value || value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
            throw new SerializationException(expectedType, "Expected an integer");
        }
        return (long) value;
    }

    private static <T> TypeSerializer<T> enumSerializer(Function<String, T> parser, Function<T, String> writer) {
        return new TypeSerializer<>() {
            @Override
            public T deserialize(@NotNull Type expectedType, @NotNull ConfigurationNode node) throws SerializationException {
                Object raw = node.rawScalar();
                if (!(raw instanceof String string)) {
                    throw new SerializationException(expectedType, "Expected a string");
                }
                T parsed = parser.apply(string);
                if (parsed == null) {
                    throw new SerializationException(expectedType, "Unsupported value '" + string + "'");
                }
                return parsed;
            }

            @Override
            public void serialize(@NotNull Type expectedType, T value, @NotNull ConfigurationNode node) {
                node.raw(value == null ? null : writer.apply(value));
            }
        };
    }

    private static TypeSerializer<BlockSelector> blockSelectorSerializer() {
        return new TypeSerializer<>() {
            @Override
            public BlockSelector deserialize(@NotNull Type expectedType, @NotNull ConfigurationNode node) throws SerializationException {
                Object raw = node.rawScalar();
                if (!(raw instanceof String value)) {
                    throw new SerializationException(expectedType, "Expected a namespaced block selector");
                }
                try {
                    return BlockSelector.parse(value);
                } catch (IllegalArgumentException exception) {
                    throw new SerializationException(expectedType, exception.getMessage(), exception);
                }
            }

            @Override
            public void serialize(@NotNull Type expectedType, BlockSelector value, @NotNull ConfigurationNode node) {
                node.raw(value == null ? null : value.toString());
            }
        };
    }
}
