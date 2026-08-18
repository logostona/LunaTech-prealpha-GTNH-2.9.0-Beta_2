package lunatech.data;

/**
 * One Shomate coefficient set and the temperature range it is valid over. See DATA.md.
 * <p>
 * Coefficients are range-specific and wildly wrong outside their range — evaluating iron's
 * 1100–1809 K set at 298 K returns a heat capacity roughly 87 times too large. The range is
 * therefore part of the data, not a footnote, and {@code lunatech.thermo.Shomate} refuses to
 * extrapolate.
 * <p>
 * Fields are public and non-final because Gson populates them reflectively.
 */
public final class ShomateRange {

    /** Inclusive lower bound of validity, K. */
    public double minKelvin;

    /** Inclusive upper bound of validity, K. */
    public double maxKelvin;

    public double a;
    public double b;
    public double c;
    public double d;
    public double e;

    /** Enthalpy offset, kJ/mol. Used by the integrated form. */
    public double f;

    /** Entropy constant, J/(mol*K). Carried because NIST publishes it; not yet used. */
    public double g;

    /** Enthalpy of formation offset, kJ/mol. Cancels in a difference. */
    public double h;

    public boolean covers(double kelvin) {
        return kelvin >= minKelvin && kelvin <= maxKelvin;
    }
}
