package games.cubi.raycastedantiesp.core.players;

/** Player body poses and the corresponding camera height above the feet. */
public enum PlayerPose {
    UNKNOWN(1.62f),
    STANDING(1.62f),
    SNEAKING(1.27f),
    SWIMMING(0.40f),
    FALL_FLYING(0.40f),
    SPIN_ATTACK(0.40f),
    SLEEPING(0.20f),
    DYING(0.20f);

    private final float cameraYOffset;

    PlayerPose(float cameraYOffset) {
        this.cameraYOffset = cameraYOffset;
    }

    public float cameraYOffset() {
        return cameraYOffset;
    }
}
