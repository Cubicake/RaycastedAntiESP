package games.cubi.raycastedantiesp.paper.engine;

import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.engine.AsyncRunner;
import games.cubi.raycastedantiesp.core.engine.Engine;
import games.cubi.raycastedantiesp.core.engine.SimpleEngine;
import games.cubi.raycastedantiesp.core.players.PlayerRegistry;
import games.cubi.raycastedantiesp.paper.PaperParticleSpawner;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.function.IntSupplier;

public class PaperSimpleEngine implements Engine {
    private final AsyncScheduler asyncScheduler;
    private final BukkitScheduler bukkitScheduler;
    private final RaycastedAntiESP plugin;
    //private final BukkitESM entitySnapshotManager;
    private final SimpleEngine delegate;

    public PaperSimpleEngine(RaycastedAntiESP plugin, ConfigManager cfg, IntSupplier currentTickSupplier) {
        this.plugin = plugin;
        asyncScheduler = plugin.getServer().getAsyncScheduler();
        bukkitScheduler = plugin.getServer().getScheduler();
        delegate = new SimpleEngine(cfg, new PaperParticleSpawner(), currentTickSupplier, new PaperAsyncRunner(asyncScheduler));

        //forceEntityLocationUpdate();
    }

    public void tick(int scheduledTick, long scheduledNanos) {
        delegate.tick(scheduledTick, scheduledNanos);
    }

    //should be folia compatible too
    public static class PaperAsyncRunner implements AsyncRunner {
        private final AsyncScheduler asyncScheduler;

        public PaperAsyncRunner(AsyncScheduler asyncScheduler) {
            this.asyncScheduler = asyncScheduler;
        }

        public void runNow(Runnable task) {
            asyncScheduler.runNow(RaycastedAntiESP.get(), (ignored) -> task.run());
        }
    }
}
