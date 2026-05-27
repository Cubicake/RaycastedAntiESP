package games.cubi.raycastedantiesp.core.players;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.core.utils.*;
import games.cubi.raycastedantiesp.core.utils.Packet.Packets;
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
    private final Int2ObjectArrayMap<EntitySpawnTask> pendingPostEntitySpawnTasksByEntityID = new Int2ObjectArrayMap<>(16); // shot in the dark guess at capacity here. Can't be the more generic Int2ObjectMap because that doesn't expose a fast iterator.

    /**
     * This is intended for reconciliation tasks due to Minecraft sending packets out of order. For example, {@link Packets#ENTITY_EQUIPMENT} is sent before the corresponding {@link Packets#SPAWN_ENTITY} packet, so caching of the equipment packet must await the spawn packet.
     * @param entityID The entity ID to associate the task with. Immediately after a {@link Packets#SPAWN_ENTITY} packet is processed for this entity ID, the task will be consumed and run.
     * @param task The task to run.
     */
    public void addPostEntitySpawnTask(int entityID, EntitySpawnTask task) {
        if (task.getNext() != null) {
            Logger.errorAndReturn(new IllegalArgumentException("Pending netty task was chained before queueing. Task=" + task), 4, NettyData.class);
        }
        EntitySpawnTask existing = pendingPostEntitySpawnTasksByEntityID.get(entityID);
        if (existing == null) {
            pendingPostEntitySpawnTasksByEntityID.put(entityID, task);
            return;
        }
        existing.appendLinkedTask(task);
    }

    public void runPendingPostSpawnTaskForEntity(int entityID) {
        EntitySpawnTask pendingTasks = consumePendingPostSpawnTasksForEntity(entityID);
        if (pendingTasks != null) {
            pendingTasks.runLinkedTasks();
        }
    }

    public EntitySpawnTask consumePendingPostSpawnTasksForEntity(int entityID) {
        return pendingPostEntitySpawnTasksByEntityID.remove(entityID);
    }

    public void clearPendingPostSpawnTasksForEntity(int entityID) {
        pendingPostEntitySpawnTasksByEntityID.remove(entityID);
    }

    public void evictOldPendingPostSpawnTasks(int currentTick) {
        ObjectIterator<Int2ObjectMap.Entry<EntitySpawnTask>> iterator = pendingPostEntitySpawnTasksByEntityID.int2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            Int2ObjectMap.Entry<EntitySpawnTask> entry = iterator.next();
            EntitySpawnTask survivingHead = entry.getValue().trimExpiredTasks(currentTick);
            if (survivingHead == null) {
                iterator.remove();
            } else {
                entry.setValue(survivingHead);
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
        pendingPostEntitySpawnTasksByEntityID.clear();
    }
}
