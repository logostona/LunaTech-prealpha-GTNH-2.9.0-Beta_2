package lunatech.kinetics;

import lunatech.data.OperatingPoint;
import lunatech.data.Reaction;

/**
 * Conversion in a continuous-flow reactor, driven by Arrhenius kinetics. Objective O4, milestone M2.
 * <p>
 * This is what makes a LunaTech reactor something other than a recipe with a timer. Output depends
 * on temperature and residence time rather than on a fixed duration, so running hotter or feeding
 * slower changes what comes out, continuously.
 * <p>
 * <b>Model and its assumptions, stated because they bound what the numbers mean.</b> A first-order,
 * isothermal plug-flow reactor with no change in volumetric flow across the reaction. Conversion is
 * X = 1 − exp(−Da), with the Damköhler number Da = k·τ. The absolute rate constant never appears:
 * the reaction's reference point fixes Da_ref = −ln(1 − X_ref), and every other condition follows
 * by ratio, Da = Da_ref · r(T) · (τ / τ_ref).
 * <p>
 * Real reactions of other orders, non-isothermal operation, and mass-transfer limitation are all
 * outside this model. It is a first-order approximation in both senses of the phrase.
 */
public final class ContinuousReactor {

    private ContinuousReactor() {}

    /** Residence time of a vessel: its volume divided by the volumetric flow through it. */
    public static double residenceTimeSeconds(double volumeLitres, double flowLitresPerSecond) {
        if (!(volumeLitres > 0.0d) || Double.isInfinite(volumeLitres)) {
            throw new IllegalArgumentException("Reactor volume must be finite and positive: " + volumeLitres);
        }
        if (!(flowLitresPerSecond > 0.0d) || Double.isInfinite(flowLitresPerSecond)) {
            throw new IllegalArgumentException("Flow must be finite and positive: " + flowLitresPerSecond);
        }
        return volumeLitres / flowLitresPerSecond;
    }

    /**
     * Damköhler number implied by a reaction's reference point: the dimensionless ratio of residence
     * time to reaction time at the reference temperature.
     */
    public static double referenceDamkohler(Reaction reaction) {
        OperatingPoint point = requirePoint(reaction);
        double conversion = point.conversion;
        if (!(conversion > 0.0d) || !(conversion < 1.0d)) {
            throw new IllegalArgumentException(
                "Reference conversion must lie strictly between 0 and 1, was " + conversion);
        }
        return -Math.log(1.0d - conversion);
    }

    /**
     * Fractional conversion at the given temperature and residence time.
     *
     * @return a value in (0, 1), capped by the reaction's {@code maximumConversion} when it declares
     *         one — a kinetic model alone will otherwise drive an equilibrium-limited reaction to
     *         completion given enough time, which is wrong.
     */
    public static double conversion(Reaction reaction, double kelvin, double residenceTimeSeconds) {
        OperatingPoint point = requirePoint(reaction);
        if (!(residenceTimeSeconds > 0.0d) || Double.isInfinite(residenceTimeSeconds)) {
            throw new IllegalArgumentException(
                "Residence time must be finite and positive: " + residenceTimeSeconds);
        }
        if (!(point.residenceTimeSeconds > 0.0d)) {
            throw new IllegalArgumentException(
                "Reference residence time must be positive: " + point.residenceTimeSeconds);
        }

        double rate = Arrhenius
            .rateMultiplier(reaction.activationEnergy.value, reaction.referenceTemperature.value, kelvin);
        double damkohler = referenceDamkohler(reaction) * rate * (residenceTimeSeconds / point.residenceTimeSeconds);
        double conversion = 1.0d - Math.exp(-damkohler);

        Double ceiling = reaction.maximumConversion;
        if (ceiling != null && conversion > ceiling.doubleValue()) {
            return ceiling.doubleValue();
        }
        return conversion;
    }

    /** Conversion for a vessel of the given volume and throughput. */
    public static double conversion(Reaction reaction, double kelvin, double volumeLitres,
        double flowLitresPerSecond) {
        double residence = residenceTimeSeconds(volumeLitres, flowLitresPerSecond);
        return conversion(reaction, kelvin, residence);
    }

    private static OperatingPoint requirePoint(Reaction reaction) {
        if (reaction == null) {
            throw new IllegalArgumentException("Reaction must not be null");
        }
        if (reaction.referencePoint == null) {
            throw new IllegalArgumentException("Reaction " + reaction.id + " declares no reference point");
        }
        if (reaction.activationEnergy == null || reaction.referenceTemperature == null) {
            throw new IllegalArgumentException("Reaction " + reaction.id + " is missing kinetic parameters");
        }
        return reaction.referencePoint;
    }
}
