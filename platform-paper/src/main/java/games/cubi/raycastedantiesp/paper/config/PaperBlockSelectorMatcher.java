package games.cubi.raycastedantiesp.paper.config;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.config.raycast.BlockSelector;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Resolves core block selectors through the block registry without exposing processor state IDs. */
public final class PaperBlockSelectorMatcher implements Predicate<BlockData> {
    private final List<BlockData> parsedSelectors;

    public PaperBlockSelectorMatcher(List<BlockSelector> selectors) {
        Objects.requireNonNull(selectors, "selectors");
        List<BlockData> resolvedSelectors = new ArrayList<>();
        for (BlockSelector selector : selectors) {
            NamespacedKey key = NamespacedKey.fromString(selector.blockKey());
            BlockType blockType = key == null ? null : Registry.BLOCK.get(key);
            if (blockType == null) {
                warnUnresolved(selector);
                continue;
            }
            try {
                resolvedSelectors.add(blockType.createBlockData(selector.toString().substring(selector.blockKey().length())));
            } catch (IllegalArgumentException exception) {
                warnUnresolved(selector);
            }
        }
        parsedSelectors = List.copyOf(resolvedSelectors);
    }

    @Override
    public boolean test(BlockData blockData) {
        for (BlockData selector : parsedSelectors) {
            // The parsed selector must be the argument so omitted properties remain wildcards.
            if (blockData.matches(selector)) {
                return true;
            }
        }
        return false;
    }

    private void warnUnresolved(BlockSelector selector) {
        Logger.warning("Unknown block selector '" + selector + "' in checks.tile-entity.excluded-blocks; ignoring it on this server.", 4,
                PaperBlockSelectorMatcher.class);
    }
}
