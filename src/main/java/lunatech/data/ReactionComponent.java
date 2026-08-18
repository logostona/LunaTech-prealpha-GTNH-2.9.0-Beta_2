package lunatech.data;

/**
 * One reactant or product of a reaction, in moles. See DATA.md.
 * <p>
 * {@code material} is a GregTech material name, resolved at runtime with
 * {@code Materials.getRealMaterial}. Keeping the name rather than a fluid registry id means the
 * dataset does not have to know GregTech's naming scheme, and a typo fails loudly at registration
 * instead of silently matching nothing.
 * <p>
 * Fields are public and non-final because Gson populates them reflectively.
 */
public final class ReactionComponent {

    /** GregTech material name, for example {@code CarbonMonoxide}. */
    public String material;

    /** {@code gas} or {@code liquid}. Selects which GregTech fluid accessor applies. */
    public String phase;

    /** Stoichiometric coefficient, in moles. Positive on both sides. */
    public double moles;

    public boolean isGas() {
        return "gas".equals(phase);
    }

    @Override
    public String toString() {
        return moles + " mol " + material + " (" + phase + ")";
    }
}
