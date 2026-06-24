package games.cubi.raycastedantiesp.paper.utils;

import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public final class PaperScheduler {
    private PaperScheduler() {}

    public static void runAsync(RaycastedAntiESP plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, ignored -> task.run());
    }

    public static void runGlobal(RaycastedAntiESP plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
    }

    /**
     * Runs audience work on the owning entity region where one exists, otherwise on the global region.
     */
    public static void runForAudience(RaycastedAntiESP plugin, CommandSender audience, Runnable task) {
        if (audience instanceof Entity entity) {
            entity.getScheduler().run(plugin, ignored -> task.run(), null);
            return;
        }
        runGlobal(plugin, task);
    }
}
