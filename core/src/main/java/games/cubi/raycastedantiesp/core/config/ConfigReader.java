/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class ConfigReader {
    private ConfigReader() {}

    public static ConfigurationNode node(ConfigurationNode parent, String... path) {
        return parent.node((Object[]) path);
    }

    public static String string(ConfigurationNode node, String path) {
        Object raw = node.raw();
        if (raw instanceof String value) {
            return value;
        }
        throw invalid(path, "string", raw);
    }

    public static int integer(ConfigurationNode node, String path) {
        Object raw = node.raw();
        if (raw instanceof Number value) {
            return exactInteger(value, path, "integer");
        }
        throw invalid(path, "integer", raw);
    }

    public static boolean bool(ConfigurationNode node, String path) {
        Object raw = node.raw();
        if (raw instanceof Boolean value) {
            return value;
        }
        throw invalid(path, "boolean", raw);
    }

    public static List<String> stringList(ConfigurationNode node, String path) {
        if (node.virtual() || node.raw() == null) {
            return List.of();
        }
        if (node.childrenList().isEmpty()) {
            Object raw = node.raw();
            if (raw instanceof List<?> list && list.isEmpty()) {
                return List.of();
            }
            throw invalid(path, "list of strings", raw);
        }
        List<String> values = new ArrayList<>();
        for (ConfigurationNode child : node.childrenList()) {
            Object raw = child.raw();
            if (!(raw instanceof String value)) {
                throw invalid(path, "list of strings", raw);
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    public static List<Integer> integerList(ConfigurationNode node, String path) {
        if (node.virtual() || node.raw() == null) {
            return List.of();
        }
        if (node.childrenList().isEmpty()) {
            Object raw = node.raw();
            if (raw instanceof List<?> list && list.isEmpty()) {
                return List.of();
            }
            throw invalid(path, "list of integers", raw);
        }
        List<Integer> values = new ArrayList<>();
        for (ConfigurationNode child : node.childrenList()) {
            Object raw = child.raw();
            if (!(raw instanceof Number value)) {
                throw invalid(path, "list of integers", raw);
            }
            int id = exactInteger(value, path, "list of integers");
            if (id < 0) {
                throw new ConfigLoadException(path + " cannot contain negative block state IDs: " + id);
            }
            values.add(id);
        }
        return List.copyOf(values);
    }

    public static Object parseRawValue(String rawValue) {
        try {
            ConfigurationNode parsed = org.spongepowered.configurate.yaml.YamlConfigurationLoader.builder()
                    .source(() -> new java.io.BufferedReader(new java.io.StringReader("value: " + rawValue)))
                    .build()
                    .load();
            return parsed.node("value").raw();
        } catch (java.io.IOException e) {
            throw new ConfigLoadException("Invalid YAML value: " + rawValue, e);
        }
    }

    public static void setRaw(ConfigurationNode node, Object value) {
        try {
            node.set(value);
        } catch (SerializationException e) {
            throw new ConfigLoadException("Failed to set config value", e);
        }
    }

    private static ConfigLoadException invalid(String path, String expected, Object raw) {
        String actual = raw == null ? "null" : raw.getClass().getSimpleName();
        return new ConfigLoadException(path + " must be a " + expected + " but was " + actual);
    }

    private static int exactInteger(Number value, String path, String expected) {
        try {
            return switch (value) {
                case Byte ignored -> value.intValue();
                case Short ignored -> value.intValue();
                case Integer ignored -> value.intValue();
                case Long longValue -> {
                    if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                        throw invalid(path, expected, value);
                    }
                    yield longValue.intValue();
                }
                case BigInteger bigInteger -> bigInteger.intValueExact();
                case BigDecimal bigDecimal -> bigDecimal.intValueExact();
                default -> exactIntegerFromFloatingPoint(value, path, expected);
            };
        } catch (ArithmeticException e) {
            throw invalid(path, expected, value);
        }
    }

    private static int exactIntegerFromFloatingPoint(Number value, String path, String expected) {
        double doubleValue = value.doubleValue();
        if (!Double.isFinite(doubleValue)
                || Math.rint(doubleValue) != doubleValue
                || doubleValue < Integer.MIN_VALUE
                || doubleValue > Integer.MAX_VALUE) {
            throw invalid(path, expected, value);
        }
        return (int) doubleValue;
    }
}
