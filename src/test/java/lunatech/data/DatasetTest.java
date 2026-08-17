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
 * The dataset half of the validation harness required by SCOPE.md section 3.
 * <p>
 * These are structural gates rather than physics checks: they exist so that an unsourced constant,
 * a wrong unit, or data verified against a different GregTech build fails the build instead of
 * quietly producing a plausible number.
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
            assertFalse(material.id.trim().isEmpty(), "material with blank id");
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
        Material iron = Datasets.materials().require("iron");
        assertEquals(1.134d, iron.massKilograms(144L), 1.0e-3d);
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
        assertFalse(q.source.trim().isEmpty(), where + " has a blank source");
        assertNotNull(q.method, where + " has no method");
        assertTrue(
            q.isExperimental() || q.method.startsWith("estimated:"),
            where + " has an unrecognised method: " + q.method);
        if (!q.isExperimental()) {
            assertNotNull(q.uncertainty, where + " is estimated but states no uncertainty");
        }
    }
}
