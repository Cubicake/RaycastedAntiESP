/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;

public record DebugConfig(byte infoLevel, List<String> infoExemptedClasses, byte warnLevel, List<String> warnExemptedClasses,
                          byte errorLevel, List<String> errorExemptedClasses, boolean debugParticles, boolean timings) implements Config {

    public static DebugConfig load(ConfigurationNode root) {
        ConfigurationNode node = ConfigReader.node(root, "debug");
        return new DebugConfig(
                level(ConfigReader.node(node, "info-level"), "debug.info-level"),
                ConfigReader.stringList(ConfigReader.node(node, "info-exempted-classes"), "debug.info-exempted-classes"),
                level(ConfigReader.node(node, "warn-level"), "debug.warn-level"),
                ConfigReader.stringList(ConfigReader.node(node, "warn-exempted-classes"), "debug.warn-exempted-classes"),
                level(ConfigReader.node(node, "error-level"), "debug.error-level"),
                ConfigReader.stringList(ConfigReader.node(node, "error-exempted-classes"), "debug.error-exempted-classes"),
                ConfigReader.bool(ConfigReader.node(node, "particles"), "debug.particles"),
                ConfigReader.bool(ConfigReader.node(node, "timings"), "debug.timings")
        );
    }

    public byte getInfoLevel() {
        return infoLevel;
    }

    public byte getWarnLevel() {
        return warnLevel;
    }

    public byte getErrorLevel() {
        return errorLevel;
    }

    public boolean showDebugParticles() {
        return debugParticles;
    }

    public boolean recordTimings() {
        return timings;
    }

    public boolean isExempted(Severity severity, Class<?>... sources) {
        List<String> exempted = switch (severity) {
            case INFO -> infoExemptedClasses;
            case WARN -> warnExemptedClasses;
            case ERROR -> errorExemptedClasses;
        };
        if (sources == null || exempted.isEmpty()) {
            return false;
        }
        for (Class<?> source : sources) {
            if (source == null) {
                continue;
            }
            if (exempted.contains(source.getSimpleName()) || exempted.contains(source.getName())) {
                return true;
            }
        }
        return false;
    }

    private static byte level(ConfigurationNode node, String path) {
        int level = ConfigReader.integer(node, path);
        if (level < 0 || level > 10) {
            throw new ConfigLoadException(path + " must be between 0 and 10");
        }
        return (byte) level;
    }

    public enum Severity {
        INFO,
        WARN,
        ERROR
    }
}
