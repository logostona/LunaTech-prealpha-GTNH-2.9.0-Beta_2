package lunatech.data;

/**
 * The reference operating point a reaction's kinetics are anchored to. See DATA.md.
 * <p>
 * This is deliberately <em>not</em> a {@link Quantity}. Residence time and conversion here are
 * LunaTech design choices, not measurements, and dressing them in a source and an uncertainty would
 * let invented numbers wear a citation. Objective O5 governs physical constants; a chosen operating
 * point is not one. It carries a {@code rationale} instead, so the choice is still answerable.
 * <p>
 * The pair fixes the Damköhler number without ever needing an absolute rate constant: a reaction
 * that reaches conversion X in time τ has kτ = −ln(1 − X) at that temperature, and every other
 * condition follows by ratio.
 */
public final class OperatingPoint {

    /** Residence time in seconds at which {@link #conversion} is reached. Must be positive. */
    public double residenceTimeSeconds;

    /** Fractional conversion at the reference temperature and residence time, in (0, 1). */
    public double conversion;

    /** Why this point was chosen. Never blank — it is what stands in for provenance. */
    public String rationale;
}
