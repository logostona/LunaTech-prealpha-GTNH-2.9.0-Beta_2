package lunatech.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Structural and budget gates for the reactions dataset. See DATA.md and SCOPE.md section 3.1.
 * <p>
 * The reaction schema separates two kinds of number that look alike and are not. Activation energy
 * and reference temperature are physical, so they carry provenance and are held to a ratified
 * budget. Residence time and conversion are LunaTech's chosen operating point, so they carry a
 * rationale instead — giving them a source and an uncertainty would let a design decision pass as a
 * measurement.
 */
class ReactionDatasetTest {

    @Test
    @DisplayName("The dataset loads at the schema version this build understands")
    void loads() {
        ReactionDataset dataset = Datasets.reactions();
        assertEquals(Datasets.SUPPORTED_SCHEMA_VERSION, dataset.schemaVersion);
        assertFalse(dataset.reactions.isEmpty(), "dataset contains no reactions");
    }

    @Test
    @DisplayName("Reaction ids are present, lowercase and unique")
    void idsAreUsableAsJoinKeys() {
        Set<String> seen = new HashSet<String>();
        for (Reaction reaction : Datasets.reactions().reactions) {
            assertNotNull(reaction.id, "reaction with no id");
            String trimmed = reaction.id.trim();
            assertFalse(trimmed.isEmpty(), "reaction with blank id");
            assertEquals(reaction.id.toLowerCase(), reaction.id, "reaction id must be lowercase");
            assertTrue(seen.add(reaction.id), "duplicate reaction id: " + reaction.id);
            assertNotNull(reaction.equation, "no equation for " + reaction.id);
        }
    }

    @Test
    @DisplayName("Physical parameters carry provenance and sit inside their ratified budgets")
    void kineticParametersAreSourcedAndAdmissible() {
        for (Reaction reaction : Datasets.reactions().reactions) {
            check(reaction.id, "activationEnergy", reaction.activationEnergy, "J/mol");
            check(reaction.id, "referenceTemperature", reaction.referenceTemperature, "K");
        }
    }

    @Test
    @DisplayName("Operating points are usable and answerable, but claim no provenance")
    void operatingPointsAreDeclaredNotMeasured() {
        for (Reaction reaction : Datasets.reactions().reactions) {
            OperatingPoint point = reaction.referencePoint;
            assertNotNull(point, reaction.id + " declares no reference point");
            assertTrue(point.residenceTimeSeconds > 0.0d, reaction.id + " has a non-positive residence time");
            assertTrue(point.conversion > 0.0d, reaction.id + " has a non-positive reference conversion");
            assertTrue(point.conversion < 1.0d, reaction.id + " claims complete conversion, which no rate reaches");
            assertNotNull(point.rationale, reaction.id + " states no rationale for its operating point");
            String rationale = point.rationale.trim();
            assertFalse(rationale.isEmpty(), reaction.id + " has a blank operating-point rationale");
        }
    }

    @Test
    @DisplayName("A declared equilibrium ceiling, where present, is a real fraction")
    void equilibriumCeilingIsSane() {
        for (Reaction reaction : Datasets.reactions().reactions) {
            Double ceiling = reaction.maximumConversion;
            if (ceiling != null) {
                double value = ceiling.doubleValue();
                assertTrue(value > 0.0d, reaction.id + " has a non-positive maximum conversion");
                assertTrue(value <= 1.0d, reaction.id + " allows conversion above one");
            }
        }
    }

    private static void check(String id, String field, Quantity q, String expectedUnit) {
        String where = id + "." + field;
        assertNotNull(q, "missing " + where);
        assertTrue(!Double.isNaN(q.value) && !Double.isInfinite(q.value), where + " is not finite");
        assertEquals(expectedUnit, q.unit, where + " has the wrong unit");
        assertNotNull(q.source, where + " has no source");
        String trimmedSource = q.source.trim();
        assertFalse(trimmedSource.isEmpty(), where + " has a blank source");
        assertNotNull(q.method, where + " has no method");
        assertTrue(
            q.isExperimental() || q.method.startsWith("estimated:"),
            where + " has an unrecognised method: " + q.method);

        Budgets.Budget budget = Budgets.forField(field);
        assertNotNull(budget, "no ratified budget for " + field + "; see SCOPE.md section 3.1");
        if (!q.isExperimental()) {
            assertNotNull(q.uncertainty, where + " is estimated but states no uncertainty");
            double limit = budget.ceilingLimit(q.value);
            assertTrue(
                q.uncertainty.doubleValue() <= limit,
                where + " is estimated with uncertainty " + q.uncertainty + " above the ceiling " + limit);
        }
    }
}
