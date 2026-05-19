package games.cubi.raycastedantiesp.core.utils;

public class ThreadGuard {
    public static final boolean ASSERTIONS_ENABLED = areAssertionsEnabled();

    @SuppressWarnings("PointlessBooleanExpression")
    /**
     * @return true if assertions are enabled, false otherwise
     */
    private static boolean areAssertionsEnabled() {
        try {
            assert true == false; // can be simplified to assert false but this reads more clearly to me as a statement which will fail.
            return false;
        } catch (AssertionError e) {
            return true;
        }
    }

}
