package lunatech.units;

/**
 * The single definition of LunaTech's unit system.
 * <p>
 * This class is normative. Per UNITS.md section 6 rule 2, the EU/SI conversion factor appears in
 * exactly one place in the codebase, and that place is here. No other class may contain a
 * conversion literal; {@code UnitsTest} enforces the constants, and a source check should eventually
 * enforce the absence of duplicates elsewhere.
 * <p>
 * SI is the unit of meaning. EU is a wire protocol. Recipes are authored in SI and compiled down to
 * EU at registration time, never the reverse — see UNITS.md section 1.
 */
public final class Units {

    private Units() {}

    /**
     * Joules per Energy Unit.
     * <p>
     * This is a <em>definition</em>, not a measurement. It is not derived from any GregTech value,
     * and no GregTech value can contradict it: a stock value implying a different factor is a defect
     * in that value, recorded in AUDIT.md. See UNITS.md section 2 for the numerical range argument.
     */
    public static final double JOULES_PER_EU = 1.0d;

    /** Seconds per Minecraft tick, exactly. */
    public static final double SECONDS_PER_TICK = 0.05d;

    /** Ticks per second, exactly. */
    public static final double TICKS_PER_SECOND = 20.0d;

    /**
     * Litres per millibucket: 1 mB is exactly 1 mL. UNITS.md section 4.
     * <p>
     * Every extensive quantity — mass, amount of substance — derives from this one declaration
     * combined with the material's real density and molar mass.
     */
    public static final double LITRES_PER_MILLIBUCKET = 1.0e-3d;

    /** Millibuckets per ingot. Verified against {@code GTValues.L} at GT5U 5.09.52.594. */
    public static final int MILLIBUCKETS_PER_INGOT = 144;

    /** Energy in joules represented by the given quantity of EU. */
    public static double joules(long eu) {
        return eu * JOULES_PER_EU;
    }

    /** Power in watts represented by the given EU per tick. */
    public static double watts(long euPerTick) {
        return euPerTick * JOULES_PER_EU * TICKS_PER_SECOND;
    }

    /**
     * Compile an energy demand from joules to EU, rounding <em>up</em>.
     * <p>
     * Demand always rounds up and output always rounds down, so that quantization can never
     * manufacture energy. This is what makes objective O2 hold exactly rather than on average.
     */
    public static long euDemand(double joules) {
        return (long) Math.ceil(joules / JOULES_PER_EU);
    }

    /** Compile an energy output from joules to EU, rounding <em>down</em>. See {@link #euDemand}. */
    public static long euOutput(double joules) {
        return (long) Math.floor(joules / JOULES_PER_EU);
    }

    /** Volume in litres for a quantity of millibuckets. */
    public static double litres(long millibuckets) {
        return millibuckets * LITRES_PER_MILLIBUCKET;
    }
}
