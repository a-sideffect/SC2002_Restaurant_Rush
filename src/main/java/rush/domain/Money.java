package rush.domain;

import java.util.Locale;

/** Formatting for money, which is always stored as integer cents. */
public final class Money {
    private Money() { }

    /** Formats cents as a dollar string, e.g. 1830 -> "$18.30", 5 -> "$0.05". */
    public static String format(int cents) {
        String sign = cents < 0 ? "-" : "";
        int abs = Math.abs(cents);
        return String.format(Locale.ROOT, "%s$%d.%02d", sign, abs / 100, abs % 100);
    }
}
