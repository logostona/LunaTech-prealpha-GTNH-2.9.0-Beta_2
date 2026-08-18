package lunatech.data;

/**
 * The declared mapping between moles and millibuckets for one reaction. See DATA.md.
 * <p>
 * This is a <em>declaration</em>, not a derivation, and the distinction matters. A real mapping is
 * mass divided by molar mass divided by density — but the density of a gas is meaningless without a
 * pressure, and {@link Quantity} has no pressure field yet (DATA.md, remaining limitations). Deriving
 * it anyway would mean inventing a state and presenting the result as physics.
 * <p>
 * So the basis is stated openly with a rationale, exactly as {@link OperatingPoint} is, and it is
 * the thing to replace first when pressure arrives. Stoichiometry stays exact regardless: the basis
 * scales every component equally, so ratios between reactants and products are unaffected by
 * whatever value it takes.
 */
public final class ReactionBasis {

    /** Millibuckets of fluid per mole of any component. Must be positive. */
    public double millibucketsPerMole;

    /** Why this value. Never blank — it stands in for provenance. */
    public String rationale;
}
