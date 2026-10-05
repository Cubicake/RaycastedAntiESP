package games.cubi.raycastedantiesp.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A mutable configuration under preparation, separate from the active runtime snapshot.
 * Used only during a load or edit; ConfigManager owns synchronization, persistence, and publication.
 */
final class ConfigCandidate {
    private final CommentedConfigurationNode node;
    private final CommentedConfigurationNode defaults;
    private final List<Repair> repairs = new ArrayList<>();
    private boolean dirty;

    ConfigCandidate(CommentedConfigurationNode node, CommentedConfigurationNode defaults) {
        this.node = node;
        this.defaults = defaults;
        dirty |= ConfigMigration.migrate(node);
        dirty |= ConfigMapping.removeDuplicateHeader(node);
        mergeDefaults();
    }

    CommentedConfigurationNode node() {
        return node;
    }

    List<Repair> repairs() {
        return repairs;
    }

    boolean isDirty() {
        return dirty;
    }

    void markDirty() {
        dirty = true;
    }

    boolean hasDefault(NodePath path) {
        return !defaults.node(path.array()).virtual();
    }

    void mergeDefaults() {
        dirty |= mergeMissing(defaults, node);
    }

    LoggingConfig parseLoggingConfig() {
        return deserializeWithRepairs(NodePath.path("logging"), LoggingConfig.class);
    }

    RootConfig parseReadyConfig() {
        // Hiding sounds from hidden entities is currently unsupported; persist the false defaults.
        for (String section : List.of("player", "entity")) {
            repair(NodePath.path("checks", section, "hide-sounds-when-hidden"), "hiding sounds from hidden entities is currently unsupported");
        }
        int attemptsRemaining = Math.max(2, countLeaves(defaults) + 1);
        while (attemptsRemaining-- > 0) {
            RootConfig root = deserializeWithRepairs(NodePath.path(), RootConfig.class);
            if (!RootConfig.CURRENT_VERSION.equals(root.configVersion())) {
                throw ConfigMigration.unsupportedVersion(root.configVersion());
            }
            List<AbsoluteViolation> violations = validate(root);
            if (violations.isEmpty()) {
                canonicalizeEnums(root);
                return root;
            }

            boolean repaired = false;
            for (AbsoluteViolation violation : violations) {
                repaired |= repair(violation.path, violation.message);
            }
            if (!repaired) {
                throw new ConfigLoadException("Configuration validation failed without a repairable default");
            }
        }
        throw new ConfigLoadException("Configuration could not be repaired after exhausting the available defaults");
    }

    private void canonicalizeEnums(RootConfig root) {
        canonicalizeValue(NodePath.path("engine", "mode"), root.engineConfig().mode().getName());
        canonicalizeValue(NodePath.path("block-processor", "mode"), root.blockProcessorConfig().mode().getName());
    }

    private void canonicalizeValue(NodePath path, String value) {
        ConfigurationNode target = node.node(path.array());
        if (!Objects.equals(target.raw(), value)) {
            target.raw(value);
            dirty = true;
        }
    }

    private <T> T deserializeWithRepairs(NodePath mappingPath, Class<T> type) {
        int attemptsRemaining = Math.max(1, countLeaves(defaults.node(mappingPath.array())) + 1);
        while (attemptsRemaining-- > 0) {
            try {
                T value = node.node(mappingPath.array()).get(type);
                if (value == null) {
                    throw new ConfigLoadException("Configuration mapper returned null for " + pathToString(mappingPath));
                }
                return value;
            } catch (SerializationException exception) {
                boolean repaired = false;
                for (SerializationException failure : leafFailures(exception)) {
                    NodePath failurePath = normalizePath(failure.path(), mappingPath);
                    repaired |= repair(failurePath, failure.rawMessage());
                }
                if (!repaired) {
                    throw new ConfigLoadException("Invalid configuration at " + pathToString(mappingPath), exception);
                }
            }
        }
        throw new ConfigLoadException("Configuration at " + pathToString(mappingPath) + " could not be repaired");
    }

    private List<AbsoluteViolation> validate(RootConfig root) {
        List<AbsoluteViolation> violations = new ArrayList<>();
        if (!root.blockProcessorConfig().trackAllBlocks() && root.checksConfig().chunkSectionConfig().enabled()) {
            violations.add(new AbsoluteViolation(
                    NodePath.path("checks", "chunk-section", "enabled"),
                    "must be false when block-processor.track-all-blocks is false"
            ));
        }
        return violations;
    }

    boolean repair(NodePath failedPath, String reason) {
        NodePath repairPath = findRepairPath(failedPath);
        if (repairPath == null) {
            return false;
        }

        ConfigurationNode target = node.node(repairPath.array());
        ConfigurationNode replacement = defaults.node(repairPath.array());
        if (Objects.equals(target.raw(), replacement.raw())) {
            return false;
        }

        String existingComment = target instanceof CommentedConfigurationNode commented ? commented.comment() : null;
        target.from(replacement);
        copyComments(replacement, target);
        if (existingComment != null && target instanceof CommentedConfigurationNode commented) {
            commented.comment(existingComment);
        }
        dirty = true;
        repairs.add(new Repair(repairPath, reason, replacement.raw()));
        return true;
    }

    private NodePath findRepairPath(NodePath failedPath) {
        Object[] parts = failedPath.array();
        for (int index = 0; index < parts.length; index++) {
            if (parts[index] instanceof Number) {
                parts = Arrays.copyOf(parts, index);
                break;
            }
        }

        for (int length = parts.length; length >= 0; length--) {
            NodePath candidate = NodePath.path(Arrays.copyOf(parts, length));
            if (!defaults.node(candidate.array()).virtual()) {
                return candidate;
            }
        }
        return null;
    }

    private List<SerializationException> leafFailures(SerializationException root) {
        List<SerializationException> failures = new ArrayList<>();
        ArrayDeque<SerializationException> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            SerializationException current = pending.removeFirst();
            boolean hasNested = false;
            for (Throwable suppressed : current.getSuppressed()) {
                if (suppressed instanceof SerializationException nested) {
                    pending.addLast(nested);
                    hasNested = true;
                }
            }
            if (!hasNested) {
                failures.add(current);
            }
        }
        return failures;
    }

    private NodePath normalizePath(NodePath failurePath, NodePath mappingPath) {
        if (failurePath == null || failurePath.size() == 0) {
            return mappingPath;
        }
        if (startsWith(failurePath, mappingPath)) {
            return failurePath;
        }
        return mappingPath.plus(failurePath);
    }

    private boolean startsWith(NodePath path, NodePath prefix) {
        if (prefix.size() > path.size()) {
            return false;
        }
        for (int index = 0; index < prefix.size(); index++) {
            if (!Objects.equals(path.get(index), prefix.get(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean mergeMissing(ConfigurationNode defaults, ConfigurationNode target) {
        if (target.virtual() || target.raw() == null && target.childrenMap().isEmpty() && target.childrenList().isEmpty()) {
            target.from(defaults);
            copyComments(defaults, target);
            return true;
        }

        boolean changed = mergeComment(defaults, target);
        if (!defaults.childrenMap().isEmpty()) {
            Object raw = target.raw();
            if (target.childrenMap().isEmpty() && !(raw instanceof Map<?, ?>)) {
                return repair(target.path(), "expected a mapping");
            }
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : defaults.childrenMap().entrySet()) {
                changed |= mergeMissing(entry.getValue(), target.node(entry.getKey()));
            }
        } else if (!defaults.childrenList().isEmpty() && target.isList()) {
            // Entries can be reordered or removed, so default comments must follow values rather than indices.
            Map<String, ConfigurationNode> commentedDefaults = new LinkedHashMap<>();
            for (ConfigurationNode child : defaults.childrenList()) {
                if (child.rawScalar() instanceof String value && child instanceof CommentedConfigurationNode commented && commented.comment() != null) {
                    commentedDefaults.put(value, child);
                }
            }
            for (ConfigurationNode child : target.childrenList()) {
                if (child.rawScalar() instanceof String value) {
                    ConfigurationNode defaultChild = commentedDefaults.get(value);
                    if (defaultChild != null) {
                        changed |= mergeComment(defaultChild, child);
                    }
                }
            }
        }
        return changed;
    }

    private boolean mergeComment(ConfigurationNode defaults, ConfigurationNode target) {
        if (!(defaults instanceof CommentedConfigurationNode defaultCommented) || !(target instanceof CommentedConfigurationNode targetCommented)) {
            return false;
        }
        if (targetCommented.comment() == null && defaultCommented.comment() != null) {
            targetCommented.comment(defaultCommented.comment());
            return true;
        }
        return false;
    }

    private void copyComments(ConfigurationNode source, ConfigurationNode target) {
        if (source instanceof CommentedConfigurationNode sourceCommented
                && target instanceof CommentedConfigurationNode targetCommented
                && sourceCommented.comment() != null) {
            targetCommented.commentIfAbsent(sourceCommented.comment());
        }
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : source.childrenMap().entrySet()) {
            copyComments(entry.getValue(), target.node(entry.getKey()));
        }
        List<? extends ConfigurationNode> sourceChildren = source.childrenList();
        List<? extends ConfigurationNode> targetChildren = target.childrenList();
        for (int index = 0; index < Math.min(sourceChildren.size(), targetChildren.size()); index++) {
            copyComments(sourceChildren.get(index), targetChildren.get(index));
        }
    }

    private int countLeaves(ConfigurationNode node) {
        if (node.childrenMap().isEmpty() && node.childrenList().isEmpty()) {
            return 1;
        }
        int count = 0;
        for (ConfigurationNode child : node.childrenMap().values()) {
            count += countLeaves(child);
        }
        for (ConfigurationNode child : node.childrenList()) {
            count += countLeaves(child);
        }
        return Math.max(1, count);
    }

    private static String pathToString(NodePath path) {
        if (path.size() == 0) {
            return "<root>";
        }
        StringBuilder result = new StringBuilder();
        for (Object part : path) {
            if (part instanceof Number) {
                result.append('[').append(part).append(']');
            } else {
                if (!result.isEmpty()) {
                    result.append('.');
                }
                result.append(part);
            }
        }
        return result.toString();
    }

    record Repair(NodePath path, String reason, Object defaultValue) {
        Repair {
            path = path.copy();
        }

        String pathString() {
            return pathToString(path);
        }
    }

    private record AbsoluteViolation(NodePath path, String message) {
    }
}
