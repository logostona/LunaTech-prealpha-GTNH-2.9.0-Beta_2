package lunatech.kinetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pins the rate model that replaces GregTech's fixed-threshold heat overclock. See AUDIT.md A5. */
class ArrheniusTest {

    /** Representative of a catalysed industrial reaction, in J/mol. */
    private static final double EA = 150_000.0d;

    @Test
    @DisplayName("At the reference temperature the multiplier is exactly one")
    void identityAtReference() {
        assertEquals(1.0d, Arrhenius.rateMultiplier(EA, 1000.0d, 1000.0d), 0.0d);
    }

    @Test
    @DisplayName("A 200 K excess multiplies the rate about twentyfold, not fourfold")
    void exponentialNotLinear() {
        // exp(-(150000/8.314462618) * (1/1200 - 1/1000)) = 20.2
        double multiplier = Arrhenius.rateMultiplier(EA, 1000.0d, 1200.0d);
        assertEquals(20.22d, multiplier, 0.05d);

        // GregTech grants x4 for a 1800 K excess. At 1200 K this model already gives 20x for 200 K,
        // and that divergence is the whole point of A5: the shape is wrong, not just the constant.
        assertTrue(multiplier > 4.0d);
    }

    @Test
    @DisplayName("Rate rises monotonically with temperature")
    void monotonic() {
        double previous = 0.0d;
        for (double kelvin = 900.0d; kelvin <= 1300.0d; kelvin += 50.0d) {
            double multiplier = Arrhenius.rateMultiplier(EA, 1000.0d, kelvin);
            assertTrue(multiplier > previous, "not monotonic at " + kelvin + " K");
            previous = multiplier;
        }
    }

    @Test
    @DisplayName("The multiplier is capped, and floored at its reciprocal")
    void clampedBothWays() {
        // Uncapped this would be ~8253x, which is both unphysical and a numerical hazard.
        assertEquals(64.0d, Arrhenius.rateMultiplier(EA, 1000.0d, 2000.0d), 1.0e-9d);
        // Uncapped ~0.011; the floor keeps a cold process slow rather than infinitely slow.
        assertEquals(1.0d / 64.0d, Arrhenius.rateMultiplier(EA, 1000.0d, 800.0d), 1.0e-9d);
    }

    @Test
    @DisplayName("Duration is the reference duration divided by the rate multiplier")
    void durationFollowsRate() {
        double seconds = Arrhenius.durationSeconds(100.0d, EA, 1000.0d, 1200.0d);
        assertEquals(4.94d, seconds, 0.02d);
        assertEquals(100.0d, Arrhenius.durationSeconds(100.0d, EA, 1000.0d, 1000.0d), 1.0e-9d);
    }

    @Test
    @DisplayName("Physically impossible inputs are rejected rather than silently producing a number")
    void rejectsNonsense() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Arrhenius.rateMultiplier(-1.0d, 1000.0d, 1200.0d));
        assertThrows(
            IllegalArgumentException.class,
            () -> Arrhenius.rateMultiplier(EA, 0.0d, 1200.0d));
        assertThrows(
            IllegalArgumentException.class,
            () -> Arrhenius.rateMultiplier(EA, 1000.0d, -5.0d));
        assertThrows(
            IllegalArgumentException.class,
            () -> Arrhenius.rateMultiplier(Double.NaN, 1000.0d, 1200.0d));
        assertThrows(
            IllegalArgumentException.class,
            () -> Arrhenius.durationSeconds(0.0d, EA, 1000.0d, 1200.0d));
    }
}
