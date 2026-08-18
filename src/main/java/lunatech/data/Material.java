package lunatech.data;

/**
 * Physical properties of one material. See DATA.md for the schema.
 * <p>
 * Optional properties may be absent, but when present must be complete. Fields are public and
 * non-final because Gson populates them reflectively.
 */
public final class Material {

    /** Lowercase, unique, stable. The join key to GregTech materials; renaming it breaks data. */
    public String id;

    /** Chemical formula, for example {@code Fe} or {@code H2O}. */
    public String formula;

    public Quantity molarMass;
    public Quantity density;
    public Quantity meltingPoint;
    public Quantity boilingPoint;
    /** Single-point heat capacity. Retained alongside {@link #heatCapacity} as an independent check. */
    public Quantity specificHeat;

    /**
     * Temperature-dependent heat capacity as Shomate ranges. Optional: materials that no duty
     * calculation touches need not carry one.
     */
    public HeatCapacity heatCapacity;
    public Quantity enthalpyOfFusion;
    public Quantity enthalpyOfVaporisation;

    /**
     * Mass in kilograms of the given volume in millibuckets, using this material's density and the
     * matter basis of UNITS.md section 4, where one millibucket is exactly one millilitre.
     */
    public double massKilograms(long millibuckets) {
        return millibuckets * 1.0e-6d * density.value;
    }

    @Override
    public String toString() {
        return id + " (" + formula + ")";
    }
}
