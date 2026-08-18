package lunatech.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.thermo.Shomate;

/**
 * The dataset half of the validation harness required by SCOPE.md section 3.
 * <p>
 * These are structural gates rather than physics checks: they exist so that an unsourced constant,
 * a wrong unit, or data verified against a different GregTech build fails the build instead of
 * quietly producing a plausible number.
 * <p>
 * They also enforce the admissibility half of the ratified error budgets (SCOPE.md section 3.1): an
 * estimated value must declare an uncertainty, and that uncertainty must sit under the ceiling for
 * its quantity. Without the ceiling, judging an estimate against "its own method's uncertainty" is
 * circular and every estimate passes.
 */
class DatasetTest {

    @Test
    @DisplayName("The dataset loads and declares the schema and GregTech build we expect")
    void loadsAndVerifies() {
        MaterialDataset dataset = Datasets.materials();

        assertEquals(Datasets.SUPPORTED_SCHEMA_VERSION, dataset.schemaVersion);
        assertEquals(Datasets.PINNED_GT5U_VERSION, dataset.gt5uVersion);
        assertFalse(dataset.materials.isEmpty(), "dataset contains no materials");
    }

    @Test
    @DisplayName("Material ids are present and unique")
    void idsAreUsableAsJoinKeys() {
        Set<String> seen = new HashSet<String>();
        for (Material material : Datasets.materials().materials) {
            assertNotNull(material.id, "material with no id");
            String trimmedId = material.id.trim();
            assertFalse(trimmedId.isEmpty(), "material with blank id");
            assertEquals(material.id.toLowerCase(), material.id, "material id must be lowercase");
            assertTrue(seen.add(material.id), "duplicate material id: " + material.id);
            assertNotNull(material.formula, "no formula for " + material.id);
        }
    }

    @Test
    @DisplayName("Every quantity carries provenance and the unit its field requires")
    void everyQuantityIsSourcedAndCorrectlyUnited() {
        for (Material m : Datasets.materials().materials) {
            // Required of every material.
            check(m.id, "molarMass", m.molarMass, "g/mol", true);
            check(m.id, "density", m.density, "kg/m3", true);

            // Optional, but complete when present.
            check(m.id, "meltingPoint", m.meltingPoint, "K", false);
            check(m.id, "boilingPoint", m.boilingPoint, "K", false);
            check(m.id, "specificHeat", m.specificHeat, "J/(kg*K)", false);
            check(m.id, "enthalpyOfFusion", m.enthalpyOfFusion, "J/kg", false);
            check(m.id, "enthalpyOfVaporisation", m.enthalpyOfVaporisation, "J/kg", false);
        }
    }

    @Test
    @DisplayName("Mass derives from the matter basis: an iron ingot is 1.134 kg")
    void massFollowsFromTheMatterBasis() {
        Material iron = Datasets.material("iron");
        assertEquals(1.134d, iron.massKilograms(144L), 1.0e-3d);
    }

    @Test
    @DisplayName("Shomate ranges are ordered, contiguous and sourced")
    void heatCapacityRangesAreWellFormed() {
        for (Material m : Datasets.materials().materials) {
            HeatCapacity capacity = m.heatCapacity;
            if (capacity == null) {
                continue;
            }
            assertNotNull(capacity.source, m.id + " heat capacity has no source");
            String trimmed = capacity.source.trim();
            assertFalse(trimmed.isEmpty(), m.id + " heat capacity has a blank source");
            assertNotNull(capacity.method, m.id + " heat capacity has no method");
            assertFalse(capacity.ranges.isEmpty(), m.id + " declares heat capacity with no ranges");

            double previousMax = Double.NaN;
            for (ShomateRange range : capacity.ranges) {
                assertTrue(
                    range.maxKelvin > range.minKelvin,
                    m.id + " has a range that does not ascend: " + range.minKelvin + " to " + range.maxKelvin);
                if (!Double.isNaN(previousMax)) {
                    // A gap would make some temperatures unevaluable; an overlap would make the
                    // answer depend on iteration order. Both are defects.
                    assertEquals(
                        previousMax,
                        range.minKelvin,
                        1.0e-9d,
                        m.id + " has a gap or overlap at " + range.minKelvin + " K");
                }
                previousMax = range.maxKelvin;
            }
        }
    }

    @Test
    @DisplayName("Cp(T) agrees with the independent single-point value, exercising the agreement budget")
    void correlationAgreesWithSinglePoint() {
        for (Material m : Datasets.materials().materials) {
            if (m.heatCapacity == null || m.specificHeat == null) {
                continue;
            }
            Double at = m.specificHeat.temperatureKelvin;
            assertNotNull(at, m.id + " has a single-point Cp with no stated temperature");

            double fromCorrelation = Shomate.heatCapacityMass(m.heatCapacity, m.molarMass.value, at.doubleValue());
            Budgets.Budget budget = Budgets.forField("specificHeat");
            double limit = budget.agreementLimit(m.specificHeat.value);
            assertEquals(
                m.specificHeat.value,
                fromCorrelation,
                limit,
                m.id + " single-point Cp and Cp(T) disagree beyond the ratified agreement budget");
        }
    }

    private static void check(String id, String field, Quantity q, String expectedUnit, boolean required) {
        String where = id + "." + field;
        if (q == null) {
            assertFalse(required, "missing required quantity " + where);
            return;
        }
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

        if (q.isExperimental()) {
            // An experimental value need not state an uncertainty, but if it does, one larger than
            // the agreement budget means the value cannot satisfy that budget even in principle.
            if (q.uncertainty != null) {
                double limit = budget.agreementLimit(q.value);
                assertTrue(
                    q.uncertainty.doubleValue() <= limit,
                    where + " states uncertainty " + q.uncertainty + " beyond its agreement budget " + limit);
            }
        } else {
            assertNotNull(q.uncertainty, where + " is estimated but states no uncertainty");
            double limit = budget.ceilingLimit(q.value);
            assertTrue(
                q.uncertainty.doubleValue() <= limit,
                where + " is estimated with uncertainty " + q.uncertainty + " above the ceiling " + limit);
        }
    }
}
