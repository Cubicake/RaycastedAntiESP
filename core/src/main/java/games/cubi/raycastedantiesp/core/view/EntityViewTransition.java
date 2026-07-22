package games.cubi.raycastedantiesp.core.view;

import games.cubi.raycastedantiesp.core.tracked.TrackedEntity;

// Used to cache visibility changes until the player's netty thread next processes
public record EntityViewTransition(Type type, TrackedEntity<?> entity, int worldEpoch) {
    public enum Type {
        SHOW,
        HIDE,
        FORGET,
    }
}
