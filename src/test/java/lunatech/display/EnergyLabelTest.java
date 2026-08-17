package lunatech.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.units.Units;

/**
 * Guards the energy relabelling described in UNITS.md section 7.
 * <p>
 * Two things can go wrong here and both are silent in-game. A format string whose placeholder
 * count no longer matches the caller throws at render time, and a power quantity relabelled as
 * energy states a falsehood in watts-sized numbers. Both are caught at build time instead.
 */
class EnergyLabelTest {

    private static final String LANG = "/assets/gregtech/lang/en_US.lang";

    /** Key to the number of format placeholders GregTech's own string uses. */
    private static final Map<String, Integer> EXPECTED_PLACEHOLDERS = new HashMap<String, Integer>();

    static {
        EXPECTED_PLACEHOLDERS.put("gt.item.desc.stored_eu", 2);
        EXPECTED_PLACEHOLDERS.put("gt.item.desc.eu_info", 3);
        EXPECTED_PLACEHOLDERS.put("GT5U.nei.start_eu", 2);
        EXPECTED_PLACEHOLDERS.put("GT5U.nei.fuel", 1);
        EXPECTED_PLACEHOLDERS.put("GT5U.nei.display.total", 1);
        EXPECTED_PLACEHOLDERS.put("GT5U.scanner.ic2_info_2", 2);
        EXPECTED_PLACEHOLDERS.put("GT5U.scanner.energy_info_3", 2);
        EXPECTED_PLACEHOLDERS.put("GT5U.infodata.battery_buffer.stored_items", 2);
        EXPECTED_PLACEHOLDERS.put("GT5U.infodata.energy", 2);
    }

    @Test
    @DisplayName("Relabelling is only legitimate because one EU is exactly one joule")
    void relabellingNeedsNoArithmetic() {
        // The whole trick: the displayed number is unchanged, so this must be an identity.
        assertEquals(1.0d, Units.JOULES_PER_EU, 1.0e-12d);
        assertEquals(1234567.0d, Units.joules(1234567L), 1.0e-9d);
    }

    @Test
    @DisplayName("Every overridden key keeps GregTech's placeholder count")
    void placeholderCountsMatch() {
        Map<String, String> overrides = readLang();
        assertEquals(EXPECTED_PLACEHOLDERS.size(), overrides.size(), "override count changed");

        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Integer expected = EXPECTED_PLACEHOLDERS.get(key);
            assertNotNull(expected, "unexpected key " + key + "; add it to EXPECTED_PLACEHOLDERS");
            assertEquals(expected.intValue(), countPlaceholders(entry.getValue()), "placeholders in " + key);
        }
    }

    @Test
    @DisplayName("No override silently relabels a power quantity as energy")
    void neverMislabelPower() {
        Map<String, String> overrides = readLang();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String value = entry.getValue();
            String where = entry.getKey();
            assertFalse(value.contains("EU/t"), where + " relabels a rate; watts need arithmetic");
            assertFalse(value.contains(" EU"), where + " still says EU");
            assertTrue(value.contains(" J"), where + " does not state joules");
        }
    }

    private static int countPlaceholders(String value) {
        int count = 0;
        int at = value.indexOf("%s");
        while (at >= 0) {
            count++;
            at = value.indexOf("%s", at + 2);
        }
        return count;
    }

    private static Map<String, String> readLang() {
        InputStream stream = EnergyLabelTest.class.getResourceAsStream(LANG);
        assertNotNull(stream, "language override resource missing: " + LANG);
        Map<String, String> entries = new LinkedHashMap<String, String>();
        try {
            Charset utf8 = Charset.forName("UTF-8");
            InputStreamReader in = new InputStreamReader(stream, utf8);
            BufferedReader reader = new BufferedReader(in);
            try {
                String line = reader.readLine();
                while (line != null) {
                    String trimmed = line.trim();
                    boolean skip = trimmed.isEmpty() || trimmed.startsWith("#");
                    int split = trimmed.indexOf('=');
                    if (!skip && split > 0) {
                        String key = trimmed.substring(0, split);
                        entries.put(key, trimmed.substring(split + 1));
                    }
                    line = reader.readLine();
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + LANG, e);
        }
        return entries;
    }
}
