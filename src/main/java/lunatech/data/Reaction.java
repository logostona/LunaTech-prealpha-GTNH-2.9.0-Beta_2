package lunatech.data;

/**
 * Kinetic parameters for one reaction. See DATA.md.
 * <p>
 * Deliberately minimal: the ratio form of the Arrhenius equation cancels the pre-exponential
 * factor, so a reaction needs only an activation energy and the temperature its quoted rate belongs
 * to. A is unknown for most reactions, and a schema that demanded it would invite invention.
 * <p>
 * Fields are public and non-final because Gson populates them reflectively.
 */
public final class Reaction {

    /** Lowercase, unique, stable. */
    public String id;

    /** Human-readable stoichiometry, for review rather than for parsing. */
    public String equation;

    /** Activation energy in J/mol. */
    public Quantity activationEnergy;

    /** The absolute temperature at which a quoted duration or rate applies. */
    public Quantity referenceTemperature;

    @Override
    public String toString() {
        return id + " (" + equation + ")";
    }
}
