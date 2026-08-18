package lunatech.kinetics;

/**
 * One fluid quantity in a reactor batch: what to take from a hatch, or what to put back.
 * <p>
 * Deliberately free of any GregTech type. Keeping the batch arithmetic in plain data means it can
 * be tested without Minecraft on the classpath, and the metatile is left with nothing but the
 * translation from a material name to a fluid.
 */
public final class BatchAmount {

    /** GregTech material name. */
    public final String material;

    /** {@code gas} or {@code liquid}. */
    public final String phase;

    /** Millibuckets, already rounded down to a whole amount. */
    public final long millibuckets;

    BatchAmount(String material, String phase, long millibuckets) {
        this.material = material;
        this.phase = phase;
        this.millibuckets = millibuckets;
    }

    public boolean isGas() {
        return "gas".equals(phase);
    }

    @Override
    public String toString() {
        return millibuckets + " mB " + material + " (" + phase + ")";
    }
}
