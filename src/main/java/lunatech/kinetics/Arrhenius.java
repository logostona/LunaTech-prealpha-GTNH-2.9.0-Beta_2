package lunatech.kinetics;

/**
 * Temperature dependence of reaction rate, replacing GregTech's fixed-threshold heat overclock.
 * See AUDIT.md finding A5.
 * <p>
 * GregTech grants a fixed x4 speed-up for every 1800 K a furnace exceeds a recipe's requirement
 * (`OverclockCalculator.java:57,75`). That is wrong in <em>form</em>, not merely in magnitude: rate
 * follows k = A exp(-Ea/RT), so the same temperature excess means completely different things at
 * 1000 K and at 10 000 K. A linear-threshold model can be tuned to agree at exactly one temperature
 * and is wrong everywhere else.
 * <p>
 * This class expresses the ratio form, which needs no pre-exponential factor:
 * <pre>
 *     r(T) = exp( -(Ea/R) * (1/T - 1/T_ref) )
 * </pre>
 * A is unknown for most reactions and cancels in the ratio, so a reaction needs only an activation
 * energy and the reference temperature its quoted duration belongs to.
 */
public final class Arrhenius {

    private Arrhenius() {}

    /**
     * Molar gas constant, J/(mol*K). Exact by definition since the 2019 SI redefinition fixed both
     * the Avogadro and Boltzmann constants, so this carries no uncertainty.
     */
    public static final double GAS_CONSTANT = 8.314462618d;

    /**
     * Default ceiling on the rate multiplier, and by symmetry a floor of its reciprocal.
     * <p>
     * A cap is required for two independent reasons. Numerically, the exponential overflows to
     * infinity and would produce zero-length or infinite operations. Physically, an unbounded
     * kinetic speed-up is wrong: once a reaction is fast enough, the process stops being limited by
     * chemistry and becomes limited by heat transfer, mass transfer or equilibrium, and no further
     * temperature buys proportional throughput. The cap is where LunaTech declares that the regime
     * has changed.
     * <p>
     * 64 is chosen as 4^3 so the ceiling lands on GregTech's own progression grid: exactly three
     * heat overclocks. That keeps the mechanic recognizable while changing its shape.
     */
    public static final double DEFAULT_MAX_MULTIPLIER = 64.0d;

    /**
     * Rate multiplier relative to the reaction's reference temperature, using
     * {@link #DEFAULT_MAX_MULTIPLIER}.
     *
     * @param activationEnergy J/mol, must be finite and non-negative
     * @param referenceKelvin  absolute temperature the reference duration belongs to
     * @param actualKelvin     absolute temperature the process actually runs at
     * @return > 1 above the reference temperature, < 1 below it, exactly 1 at it
     */
    public static double rateMultiplier(double activationEnergy, double referenceKelvin, double actualKelvin) {
        return rateMultiplier(activationEnergy, referenceKelvin, actualKelvin, DEFAULT_MAX_MULTIPLIER);
    }

    /** As {@link #rateMultiplier(double, double, double)}, with an explicit ceiling. */
    public static double rateMultiplier(double activationEnergy, double referenceKelvin, double actualKelvin,
        double maxMultiplier) {
        requireFinite(activationEnergy, "activation energy");
        requireAbsoluteTemperature(referenceKelvin, "reference temperature");
        requireAbsoluteTemperature(actualKelvin, "actual temperature");
        if (activationEnergy < 0.0d) {
            throw new IllegalArgumentException("Activation energy cannot be negative: " + activationEnergy);
        }
        if (!(maxMultiplier >= 1.0d) || Double.isInfinite(maxMultiplier)) {
            throw new IllegalArgumentException("Max multiplier must be finite and at least 1: " + maxMultiplier);
        }

        // Compared directly rather than via the exponential, so the identity is exact at T = T_ref
        // instead of merely very close to 1.
        if (actualKelvin == referenceKelvin) {
            return 1.0d;
        }

        double exponent = -(activationEnergy / GAS_CONSTANT) * (1.0d / actualKelvin - 1.0d / referenceKelvin);
        double multiplier = Math.exp(exponent);

        double floor = 1.0d / maxMultiplier;
        if (multiplier > maxMultiplier) {
            return maxMultiplier;
        }
        if (multiplier < floor) {
            return floor;
        }
        return multiplier;
    }

    /**
     * Duration of an operation at {@code actualKelvin}, given the duration quoted at the reaction's
     * reference temperature. Faster reaction, shorter operation.
     */
    public static double durationSeconds(double referenceDurationSeconds, double activationEnergy,
        double referenceKelvin, double actualKelvin) {
        requireFinite(referenceDurationSeconds, "reference duration");
        if (referenceDurationSeconds <= 0.0d) {
            throw new IllegalArgumentException("Reference duration must be positive: " + referenceDurationSeconds);
        }
        double multiplier = rateMultiplier(activationEnergy, referenceKelvin, actualKelvin);
        return referenceDurationSeconds / multiplier;
    }

    private static void requireAbsoluteTemperature(double kelvin, String what) {
        requireFinite(kelvin, what);
        if (kelvin <= 0.0d) {
            throw new IllegalArgumentException(what + " must be above absolute zero: " + kelvin + " K");
        }
    }

    private static void requireFinite(double value, String what) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(what + " must be finite: " + value);
        }
    }
}
