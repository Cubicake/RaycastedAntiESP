package games.cubi.raycastedantiesp.core.config.raycast;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A namespaced block key, optionally restricted by property values. Omitted properties match any value.
 */
public record BlockSelector(String value) {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");
    private static final Pattern PROPERTY = Pattern.compile("[a-z0-9_]+");

    public BlockSelector {
        validate(value);
    }

    public static BlockSelector parse(String input) {
        return new BlockSelector(input);
    }

    public String blockKey() {
        int openingBracket = value.indexOf('[');
        return openingBracket < 0 ? value : value.substring(0, openingBracket);
    }

    public boolean matchesAllStates() {
        return value.indexOf('[') < 0;
    }

    @Override
    public @NotNull String toString() {
        return value;
    }

    private static void validate(String input) {
        Objects.requireNonNull(input, "value");
        int openingBracket = input.indexOf('[');
        if (openingBracket < 0) {
            if (input.indexOf(']') >= 0) {
                throw new IllegalArgumentException("Unexpected closing bracket in block selector");
            }
            validateBlockKey(input);
            return;
        }
        if (!input.endsWith("]") || input.indexOf('[', openingBracket + 1) >= 0
                || input.indexOf(']', openingBracket) != input.length() - 1) {
            throw new IllegalArgumentException("Malformed block-state properties");
        }

        String propertyText = input.substring(openingBracket + 1, input.length() - 1);
        if (propertyText.isEmpty()) {
            throw new IllegalArgumentException("Block-state properties cannot be empty");
        }

        validateBlockKey(input.substring(0, openingBracket));
        Set<String> properties = new HashSet<>();
        for (String assignment : propertyText.split(",", -1)) {
            int equals = assignment.indexOf('=');
            if (equals <= 0 || equals != assignment.lastIndexOf('=') || equals == assignment.length() - 1) {
                throw new IllegalArgumentException("Invalid block-state property '" + assignment + "'");
            }
            String name = validateProperty(assignment.substring(0, equals), "property name");
            validateProperty(assignment.substring(equals + 1), "property value");
            if (!properties.add(name)) {
                throw new IllegalArgumentException("Duplicate block property '" + name + "'");
            }
        }
    }

    private static String validateBlockKey(String blockKey) {
        Objects.requireNonNull(blockKey, "blockKey");
        int separator = blockKey.indexOf(':');
        if (separator <= 0 || separator != blockKey.lastIndexOf(':') || separator == blockKey.length() - 1
                || !NAMESPACE.matcher(blockKey.substring(0, separator)).matches()
                || !PATH.matcher(blockKey.substring(separator + 1)).matches()) {
            throw new IllegalArgumentException("Invalid namespaced block key '" + blockKey + "'");
        }
        return blockKey;
    }

    private static String validateProperty(String value, String description) {
        Objects.requireNonNull(value, description);
        if (!PROPERTY.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid block-state " + description + " '" + value + "'");
        }
        return value;
    }
}
