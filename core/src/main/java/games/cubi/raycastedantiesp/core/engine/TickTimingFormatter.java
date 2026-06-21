package games.cubi.raycastedantiesp.core.engine;

import java.util.Locale;

final class TickTimingFormatter {
    private TickTimingFormatter() {}

    static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0);
    }

    static String formatSeconds(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000_000.0);
    }
}
