package games.cubi.raycastedantiesp.paper.engine;

import games.cubi.raycastedantiesp.core.config.ConfigManager;
import games.cubi.raycastedantiesp.core.engine.AsyncRunner;
import games.cubi.raycastedantiesp.core.engine.SimpleEngine;
import games.cubi.raycastedantiesp.paper.PaperParticleSpawner;
import games.cubi.raycastedantiesp.paper.RaycastedAntiESP;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;

import java.util.function.IntSupplier;

public class PaperSimpleEngine extends SimpleEngine {

    public PaperSimpleEngine(RaycastedAntiESP plugin, ConfigManager cfg, IntSupplier currentTickSupplier) {
        super(cfg, new PaperParticleSpawner(), currentTickSupplier, new PaperAsyncRunner(plugin.getServer().getAsyncScheduler()));
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
