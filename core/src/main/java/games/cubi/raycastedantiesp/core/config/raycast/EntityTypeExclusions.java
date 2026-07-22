package games.cubi.raycastedantiesp.core.config.raycast;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

/**
 * Immutable entity-type exclusion policy resolved during plugin startup.
 */
public final class EntityTypeExclusions {
    private static final EntityTypeExclusions EMPTY = new EntityTypeExclusions(IntSets.emptySet());
    private static volatile EntityTypeExclusions active = EMPTY;

    private final IntSet excludedTypes;

    private EntityTypeExclusions(IntCollection excludedTypes) {
        this.excludedTypes = IntSets.unmodifiable(new IntOpenHashSet(excludedTypes));
    }

    public static void initialise(IntCollection excludedTypes) {
        active = excludedTypes.isEmpty() ? EMPTY : new EntityTypeExclusions(excludedTypes);
    }

    public static boolean excludes(int entityType) {
        return active.excludedTypes.contains(entityType);
    }

    public static int size() {
        return active.excludedTypes.size();
    }

    private EntityTypeExclusions() {
        this.excludedTypes = IntSets.emptySet();
    }
}
