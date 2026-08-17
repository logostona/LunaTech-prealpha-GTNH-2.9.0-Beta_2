package lunatech.data;

/**
 * A single physical value with its units and provenance.
 * <p>
 * Objective O5 requires every constant LunaTech ships to carry a source, and SCOPE.md section 3.1
 * requires estimated values to be judged against their method's stated uncertainty rather than the
 * experimental budget. A bare number can satisfy neither, so the dataset schema has no bare numbers
 * — see DATA.md.
 * <p>
 * Fields are public and non-final because Gson populates them reflectively.
 */
public final class Quantity {

    /** Magnitude in {@link #unit}. Must be finite. */
    public double value;

    /** SI unit string, for example {@code kg/m3}. The harness asserts this per field. */
    public String unit;

    /** Reference work, DOI, or named estimation model. Never blank. */
    public String source;

    /** Either {@code experimental} or {@code estimated:<model>}. */
    public String method;

    /** Absolute uncertainty in {@link #unit}. Required when {@link #method} is not experimental. */
    public Double uncertainty;

    public boolean isExperimental() {
        return "experimental".equals(method);
    }

    @Override
    public String toString() {
        return value + " " + unit + " (" + method + ", " + source + ")";
    }
}
