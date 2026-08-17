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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.units.Units;

/**
 * Guards the display overrides described in UNITS.md section 7.1.
 * <p>
 * Three things can go wrong here and all of them are silent in game. A format string whose
 * placeholder count no longer matches its caller throws at render time; an energy key that still
 * says EU is simply unconverted; and a power key that states a unit contradicts the mixin which
 * supplies one, producing something like "80 W EU/t".
 */
class EnergyLabelTest {

    private static final String LANG = "/assets/gregtech/lang/en_US.lang";

    /**
     * Keys stating a plain energy. At κ = 1 the number is already joules, so these are relabelled
     * in the lang file itself and must say J.
     */
    private static final Set<String> ENERGY_KEYS = new HashSet<String>(
        Arrays.asList(
            "gt.item.desc.stored_eu",
            "gt.item.desc.eu_info",
            "GT5U.nei.start_eu",
            "GT5U.nei.fuel",
            "GT5U.nei.display.total",
            "GT5U.scanner.ic2_info_2",
            "GT5U.scanner.energy_info_3",
            "GT5U.infodata.battery_buffer.stored_items",
            "GT5U.infodata.energy"));

    /**
     * Keys stating a power. Watts are EU/t x 20, so the number must change and a mixin supplies
     * both value and unit. These must carry no unit of their own.
     */
    private static final Set<String> MIXIN_VALUED_KEYS = new HashSet<String>(
        Arrays.asList("GT5U.nei.display.usage", "GT5U.nei.display.voltage"));

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
        EXPECTED_PLACEHOLDERS.put("GT5U.nei.display.usage", 2);
        EXPECTED_PLACEHOLDERS.put("GT5U.nei.display.voltage", 2);
    }

    @Test
    @DisplayName("Relabelling energy is only legitimate because one EU is exactly one joule")
    void relabellingNeedsNoArithmetic() {
        assertEquals(1.0d, Units.JOULES_PER_EU, 1.0e-12d);
        assertEquals(1234567.0d, Units.joules(1234567L), 1.0e-9d);
    }

    @Test
    @DisplayName("Converting power does need arithmetic, so the lang file cannot do it alone")
    void powerNeedsConversion() {
        // 4 EU/t is 80 W, not 4 of anything. This is why the power keys carry no unit.
        assertEquals(80.0d, Units.watts(4L), 1.0e-9d);
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
            int actual = countPlaceholders(entry.getValue());
            assertEquals(expected.intValue(), actual, "placeholders in " + key);
        }
    }

    @Test
    @DisplayName("Energy keys state joules; power keys state no unit at all")
    void unitsAreStatedByExactlyOneLayer() {
        Map<String, String> overrides = readLang();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            assertFalse(value.contains("EU"), key + " still says EU");

            if (ENERGY_KEYS.contains(key)) {
                assertTrue(value.contains(" J"), key + " is an energy key but does not state joules");
            } else if (MIXIN_VALUED_KEYS.contains(key)) {
                // The mixin supplies "80 W". A unit here would duplicate or contradict it.
                assertFalse(value.contains(" J"), key + " is a power key but states joules");
                assertFalse(value.contains(" W"), key + " states a unit the mixin already supplies");
            } else {
                assertTrue(false, key + " belongs to neither the energy nor the power set");
            }
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
