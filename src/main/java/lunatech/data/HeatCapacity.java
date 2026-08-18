package lunatech.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Temperature-dependent molar heat capacity as a set of Shomate ranges. See DATA.md.
 * <p>
 * Stored in the molar form NIST publishes rather than converted to a mass basis at data-entry time.
 * Conversion is one division by molar mass and is done on read; doing it by hand on the way in is
 * where transcription errors hide.
 */
public final class HeatCapacity {

    /** Reference work for the whole coefficient set. Never blank. */
    public String source;

    /** {@code experimental} or {@code estimated:<model>}, as elsewhere in the schema. */
    public String method;

    /** Contiguous, ascending, non-overlapping. Enforced by the harness. */
    public List<ShomateRange> ranges = new ArrayList<ShomateRange>();

    public boolean isExperimental() {
        return "experimental".equals(method);
    }

    /** @return the range covering this temperature, or null if none does. */
    public ShomateRange rangeFor(double kelvin) {
        for (ShomateRange range : ranges) {
            if (range.covers(kelvin)) {
                return range;
            }
        }
        return null;
    }

    /** @return the lowest temperature any range covers. */
    public double minKelvin() {
        double lowest = Double.POSITIVE_INFINITY;
        for (ShomateRange range : ranges) {
            lowest = Math.min(lowest, range.minKelvin);
        }
        return lowest;
    }

    /** @return the highest temperature any range covers. */
    public double maxKelvin() {
        double highest = Double.NEGATIVE_INFINITY;
        for (ShomateRange range : ranges) {
            highest = Math.max(highest, range.maxKelvin);
        }
        return highest;
    }
}
