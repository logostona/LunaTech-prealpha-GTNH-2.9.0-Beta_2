package lunatech.data;

import java.util.HashMap;
import java.util.Map;

/**
 * The error budgets ratified in SCOPE.md section 3.1, in executable form.
 * <p>
 * Each quantity carries two limits. <em>Agreement</em> bounds how far a value may sit from an
 * independent cross-check. <em>Ceiling</em> bounds the uncertainty an estimated value may itself
 * declare, and it is the one that makes estimation admissible rather than unlimited: without it, a
 * value judged against "its method's uncertainty" passes whatever that method claims.
 */
public final class Budgets {

    private Budgets() {}

    /** A limit, either absolute in the field's own unit or relative to the value. */
    public static final class Budget {

        public final double agreement;

        public final double ceiling;

        public final boolean relative;

        Budget(double agreement, double ceiling, boolean relative) {
            this.agreement = agreement;
            this.ceiling = ceiling;
            this.relative = relative;
        }

        /** Largest deviation from a cross-check this quantity may show. */
        public double agreementLimit(double value) {
            return relative ? Math.abs(value) * agreement : agreement;
        }

        /** Largest uncertainty an estimated value of this quantity may declare. */
        public double ceilingLimit(double value) {
            return relative ? Math.abs(value) * ceiling : ceiling;
        }
    }

    private static final Map<String, Budget> BY_FIELD = new HashMap<String, Budget>();

    private static void absolute(String field, double agreement, double ceiling) {
        BY_FIELD.put(field, new Budget(agreement, ceiling, false));
    }

    private static void relative(String field, double agreement, double ceiling) {
        BY_FIELD.put(field, new Budget(agreement, ceiling, true));
    }

    static {
        absolute("molarMass", 1.0e-3d, 1.0e-2d); // g/mol
        relative("density", 0.02d, 0.05d);
        absolute("meltingPoint", 2.0d, 10.0d); // K
        absolute("boilingPoint", 2.0d, 10.0d); // K
        relative("specificHeat", 0.03d, 0.10d);
        relative("enthalpyOfFusion", 0.03d, 0.10d);
        relative("enthalpyOfVaporisation", 0.03d, 0.10d);

        // Kinetics. Deliberately wide: published activation energies for one reaction routinely
        // differ by 20-30 kJ/mol with catalyst, support and temperature window. A tight budget here
        // would be false precision.
        absolute("activationEnergy", 20_000.0d, 40_000.0d); // J/mol
        absolute("referenceTemperature", 2.0d, 75.0d); // K
    }

    /** @return the budget for this dataset field, or null if none is declared. */
    public static Budget forField(String field) {
        return BY_FIELD.get(field);
    }

    /** @return true if SCOPE.md section 3.1 declares a budget for this field. */
    public static boolean isBudgeted(String field) {
        return BY_FIELD.containsKey(field);
    }
}
