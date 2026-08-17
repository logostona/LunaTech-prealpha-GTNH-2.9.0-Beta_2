package lunatech.units;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formats SI quantities for display, with prefixes. See UNITS.md section 7.
 * <p>
 * Locale is pinned to ROOT deliberately. A machine tooltip reading "10,24 kW" on one player's
 * client and "10.24 kW" on another's is a bug, and the default locale would produce exactly that.
 */
public final class SiFormat {

    private SiFormat() {}

    private static final String[] PREFIXES = { "", "k", "M", "G", "T", "P", "E" };

    private static final double STEP = 1000.0d;

    /** Energy in joules, e.g. {@code 400 J}, {@code 2.05 kJ}, {@code 1.05 MJ}. */
    public static String energy(double joules) {
        return format(joules, "J");
    }

    /** Power in watts, e.g. {@code 80 W}, {@code 10.24 kW}, {@code 42.95 GW}. */
    public static String power(double watts) {
        return format(watts, "W");
    }

    private static String format(double value, String unit) {
        if (value == 0.0d) {
            return "0 " + unit;
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "? " + unit;
        }

        double scaled = value;
        int prefix = 0;
        while (Math.abs(scaled) >= STEP && prefix < PREFIXES.length - 1) {
            scaled /= STEP;
            prefix++;
        }

        // DecimalFormat is not thread safe, so it is built per call rather than shared.
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat format = new DecimalFormat("0.##", symbols);
        return format.format(scaled) + " " + PREFIXES[prefix] + unit;
    }
}
