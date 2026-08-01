package games.cubi.raycastedantiesp.core.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncEngineVisibilityRuleTest {
    @Test
    void glowingAlwaysBypassesRaycasting() {
        assertTrue(AsyncEngine.bypassesRaycast(true, true, false));
        assertTrue(AsyncEngine.bypassesRaycast(true, false, true));
    }

    @Test
    void sneakingOnlyModeBypassesRaycastingOnlyForNonSneakingPlayers() {
        assertTrue(AsyncEngine.bypassesRaycast(false, false, true));
        assertFalse(AsyncEngine.bypassesRaycast(false, true, true));
        assertFalse(AsyncEngine.bypassesRaycast(false, false, false));
    }
}
