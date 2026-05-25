package games.cubi.raycastedantiesp.core.players;

import games.cubi.raycastedantiesp.core.utils.Clearable;
import games.cubi.raycastedantiesp.core.utils.FutureNettyTask;
import games.cubi.raycastedantiesp.core.utils.IntArrayList;
import games.cubi.raycastedantiesp.core.utils.IntArrayListMarker;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;


/**
 * Per-player mutable state intended for Netty-side packet tracking and deferred reconciliation.
 */
public class NettyData implements Clearable {
    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START Leash tracking:
    //
    private final Int2ObjectArrayMap<int[]> unresolvedLeashedEntityIDsByHolderID = new Int2ObjectArrayMap<>(16); // shot in the dark guess at capacity here. Can't be the more generic Int2ObjectMap because that doesn't expose a fast iterator.

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
    //
    // END Leash tracking.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    //
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // START Netty task queue:
    //
    private final Int2ObjectArrayMap<FutureNettyTask> pendingNettyTasksByEntityID = new Int2ObjectArrayMap<>(16); // shot in the dark guess at capacity here. Can't be the more generic Int2ObjectMap because that doesn't expose a fast iterator.

    public void addPendingNettyTask(int entityID, FutureNettyTask task) {
        pendingNettyTasksByEntityID.put(entityID, task);
    }

    public void runPendingNettyTaskForEntity(int entityID) {
        FutureNettyTask task = pendingNettyTasksByEntityID.remove(entityID);
        if (task != null) {
            task.run();
        }
    }

    public void evictOldPendingNettyTasks(int currentTick) {
        ObjectIterator<Int2ObjectMap.Entry<FutureNettyTask>> iterator = pendingNettyTasksByEntityID.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<FutureNettyTask> entry = iterator.next();
            FutureNettyTask task = entry.getValue();
            FutureNettyTask evictUntil = task.evictUntilThis(currentTick);
            if (evictUntil == task) continue;
            if (evictUntil == null) {
                iterator.remove();
            } else {
                entry.setValue(evictUntil);
            }
        }
    }

    //
    // END Netty task queue.
    // ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    //

    @Override
    public void clear() {
        unresolvedLeashedEntityIDsByHolderID.clear();
    }
}
