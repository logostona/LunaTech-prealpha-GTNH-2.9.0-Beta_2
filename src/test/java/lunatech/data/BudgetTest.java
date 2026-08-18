package lunatech.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the error budgets ratified in SCOPE.md section 3.1.
 * <p>
 * These assertions are not testing arithmetic. They exist so that loosening a budget is a visible,
 * deliberate edit to a ratified decision rather than a quiet change that lets weaker data in.
 */
class BudgetTest {

    private static final double EPS = 1.0e-12d;

    @Test
    @DisplayName("Ratified budgets match SCOPE.md section 3.1")
    void ratifiedValues() {
        assertAbsolute("molarMass", 1.0e-3d, 1.0e-2d);
        assertRelative("density", 0.02d, 0.05d);
        assertAbsolute("meltingPoint", 2.0d, 10.0d);
        assertAbsolute("boilingPoint", 2.0d, 10.0d);
        assertRelative("specificHeat", 0.03d, 0.10d);
        assertRelative("enthalpyOfFusion", 0.03d, 0.10d);
        assertRelative("enthalpyOfVaporisation", 0.03d, 0.10d);
        assertAbsolute("activationEnergy", 20_000.0d, 40_000.0d);
        assertAbsolute("referenceTemperature", 2.0d, 75.0d);
    }

    @Test
    @DisplayName("A ceiling is never tighter than the agreement budget it backstops")
    void ceilingIsTheLooserOfTheTwo() {
        String[] fields = { "molarMass", "density", "meltingPoint", "boilingPoint", "specificHeat", "enthalpyOfFusion",
            "enthalpyOfVaporisation", "activationEnergy", "referenceTemperature" };
        for (String field : fields) {
            Budgets.Budget budget = Budgets.forField(field);
            assertNotNull(budget, field);
            assertTrue(
                budget.ceiling >= budget.agreement,
                field + " has a ceiling tighter than its agreement budget, which admits nothing");
        }
    }

    @Test
    @DisplayName("Relative budgets scale with the value; absolute ones do not")
    void limitsScaleCorrectly() {
        Budgets.Budget density = Budgets.forField("density");
        assertEquals(157.48d, density.agreementLimit(7874.0d), 1.0e-6d); // 2 % of iron's density

        Budgets.Budget melting = Budgets.forField("meltingPoint");
        assertEquals(2.0d, melting.agreementLimit(1811.0d), EPS); // absolute, unaffected by value
        assertEquals(2.0d, melting.agreementLimit(273.15d), EPS);
    }

    @Test
    @DisplayName("An unbudgeted field is reported rather than silently permitted")
    void unknownFieldsHaveNoBudget() {
        assertNotNull(Budgets.forField("density"));
        assertTrue(Budgets.isBudgeted("specificHeat"));
        org.junit.jupiter.api.Assertions.assertNull(Budgets.forField("colour"));
        org.junit.jupiter.api.Assertions.assertFalse(Budgets.isBudgeted("colour"));
    }

    private static void assertAbsolute(String field, double agreement, double ceiling) {
        Budgets.Budget budget = Budgets.forField(field);
        assertNotNull(budget, "no budget declared for " + field);
        assertEquals(agreement, budget.agreement, EPS, field + " agreement");
        assertEquals(ceiling, budget.ceiling, EPS, field + " ceiling");
        assertTrue(!budget.relative, field + " should be an absolute budget");
    }

    private static void assertRelative(String field, double agreement, double ceiling) {
        Budgets.Budget budget = Budgets.forField(field);
        assertNotNull(budget, "no budget declared for " + field);
        assertEquals(agreement, budget.agreement, EPS, field + " agreement");
        assertEquals(ceiling, budget.ceiling, EPS, field + " ceiling");
        assertTrue(budget.relative, field + " should be a relative budget");
    }
}
