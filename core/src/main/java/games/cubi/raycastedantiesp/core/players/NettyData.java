package games.cubi.raycastedantiesp.core.players;

import games.cubi.raycastedantiesp.core.utils.IntArrayList;
import games.cubi.raycastedantiesp.core.utils.IntArrayListMarker;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;


/**
 * Per-player mutable state intended for Netty-side packet tracking and deferred reconciliation.
 */
public class NettyData {
    private final Int2ObjectMap<int[]> unresolvedLeashedEntityIDsByHolderID = new Int2ObjectArrayMap<>(16); // shot in the dark guess at capacity here

    public void addUnresolvedLeash(int holderEntityID, int leashedEntityID) {
        unresolvedLeashedEntityIDsByHolderID.compute(holderEntityID, (ignored, existing) -> {
            if (IntArrayList.contains(existing, leashedEntityID)) {
                return existing;
            }
            return IntArrayList.add(existing, leashedEntityID);
        });
    }

    public boolean removeUnresolvedLeash(int holderEntityID, int leashedEntityID) {
        final boolean[] removed = new boolean[1];
        unresolvedLeashedEntityIDsByHolderID.computeIfPresent(holderEntityID, (ignored, existing) -> {
            if (!IntArrayList.contains(existing, leashedEntityID)) {
                return existing;
            }
            removed[0] = true;
            int[] updated = IntArrayList.remove(existing, leashedEntityID);
            return IntArrayList.isEmpty(updated) ? null : updated;
        });
        return removed[0];
    }

    public int[] consumeUnresolvedLeashes(int holderEntityID) {
        return unresolvedLeashedEntityIDsByHolderID.remove(holderEntityID);
    }

    public int[] removePendingHolder(int holderEntityID) {
        return unresolvedLeashedEntityIDsByHolderID.remove(holderEntityID);
    }

    public void removeUnresolvedLeashedEntityFromAll(int leashedEntityID) {
        ObjectIterator<Int2ObjectMap.Entry<int @IntArrayListMarker []>> iterator = unresolvedLeashedEntityIDsByHolderID.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<int @IntArrayListMarker []> entry = iterator.next();
            int[] existing = entry.getValue();
            if (!IntArrayList.contains(existing, leashedEntityID)) {
                continue;
            }
            int[] updated = IntArrayList.remove(existing, leashedEntityID);
            if (IntArrayList.isEmpty(updated)) {
                iterator.remove();
                continue;
            }
            entry.setValue(updated);
        }
    }

    public void clear() {
        unresolvedLeashedEntityIDsByHolderID.clear();
    }
}
