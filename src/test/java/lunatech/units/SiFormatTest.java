package lunatech.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The mixin that uses these strings cannot be exercised without a running game, so the formatting
 * it depends on is pinned here instead.
 */
class SiFormatTest {

    @Test
    @DisplayName("Energy formats with SI prefixes")
    void energyPrefixes() {
        assertEquals("0 J", SiFormat.energy(0.0d));
        assertEquals("400 J", SiFormat.energy(400.0d));
        assertEquals("2.05 kJ", SiFormat.energy(2048.0d));
        assertEquals("1.05 MJ", SiFormat.energy(1.0506e6d));
    }

    @Test
    @DisplayName("Power formats with SI prefixes, and the tier ladder reads as expected")
    void powerPrefixes() {
        assertEquals("80 W", SiFormat.power(Units.watts(4L)));
        assertEquals("640 W", SiFormat.power(Units.watts(32L))); // LV
        assertEquals("10.24 kW", SiFormat.power(Units.watts(512L))); // HV
        assertEquals("42.95 GW", SiFormat.power(Units.watts(2_147_483_640L))); // MAX
    }

    @Test
    @DisplayName("Formatting does not follow the client's locale")
    void localeIndependent() {
        Locale original = Locale.getDefault();
        try {
            // pt-BR and many others use a comma as the decimal separator. Two players must not
            // see different numbers for the same machine.
            Locale.setDefault(new Locale("pt", "BR"));
            assertEquals("10.24 kW", SiFormat.power(Units.watts(512L)));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("Extreme and invalid values stay printable")
    void doesNotBlowUp() {
        String notANumber = SiFormat.energy(Double.NaN);
        assertTrue(notANumber.endsWith("J"));

        String infinite = SiFormat.power(Double.POSITIVE_INFINITY);
        assertTrue(infinite.endsWith("W"));

        assertEquals("-640 W", SiFormat.power(-640.0d));

        // Beyond the largest prefix the number simply grows rather than throwing.
        String beyondPrefixes = SiFormat.energy(1.0e30d);
        assertTrue(beyondPrefixes.endsWith("EJ"));
    }
}
