package lunatech.data;

/**
 * Kinetic parameters for one reaction. See DATA.md.
 * <p>
 * Deliberately minimal. The ratio form of the Arrhenius equation cancels the pre-exponential
 * factor, so a reaction needs only an activation energy, the temperature its quoted rate belongs
 * to, and one reference operating point. A is unknown for most reactions, and a schema demanding it
 * would invite invention.
 * <p>
 * Fields are public and non-final because Gson populates them reflectively.
 */
public final class Reaction {

    /** Lowercase, unique, stable. */
    public String id;

    /** Human-readable stoichiometry, for review rather than for parsing. */
    public String equation;

    /** Activation energy in J/mol. Physical, and therefore sourced. */
    public Quantity activationEnergy;

    /** The absolute temperature the reference operating point applies at. Sourced. */
    public Quantity referenceTemperature;

    /** LunaTech's chosen anchor point. A design decision, not a measurement. */
    public OperatingPoint referencePoint;

    /**
     * Optional ceiling on conversion, standing in for a real equilibrium calculation.
     * <p>
     * A reaction with a small or negative ΔrG cannot run to completion however long it is held, and
     * a kinetic model alone will happily predict otherwise. Until ΔrG(T) is in the dataset this is
     * a declared cap rather than a computed one, and DATA.md says so.
     */
    public Double maximumConversion;

    @Override
    public String toString() {
        return id + " (" + equation + ")";
    }
}
