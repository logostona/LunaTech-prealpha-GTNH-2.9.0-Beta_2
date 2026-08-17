package lunatech.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * First member of the validation harness described in SCOPE.md section 3.
 * <p>
 * These assertions are not testing arithmetic — they are pinning ratified decisions so that a
 * casual edit to {@link Units} fails the build instead of silently invalidating UNITS.md, AUDIT.md
 * and every energy figure derived from them.
 */
class UnitsTest {

    private static final double EPS = 1.0e-9d;

    @Test
    @DisplayName("1 EU is exactly 1 joule (UNITS.md section 2)")
    void kappaIsUnity() {
        assertEquals(1.0d, Units.JOULES_PER_EU, EPS);
    }

    @Test
    @DisplayName("1 mB is exactly 1 mL, and an ingot is 144 mB (GTValues.L)")
    void matterBasis() {
        assertEquals(144, Units.MILLIBUCKETS_PER_INGOT);
        // One iron ingot occupies 144 mL. With a density of 7.874 g/cm3 this is 1.134 kg,
        // the figure UNITS.md section 4 and the blast furnace pacing argument both rest on.
        assertEquals(0.144d, Units.litres(Units.MILLIBUCKETS_PER_INGOT), EPS);
    }

    @Test
    @DisplayName("Tier power lands on the ladder recorded in UNITS.md section 3")
    void tierPower() {
        assertEquals(160.0d, Units.watts(8L), EPS); // ULV
        assertEquals(640.0d, Units.watts(32L), EPS); // LV
        assertEquals(10_240.0d, Units.watts(512L), EPS); // HV
        // MAX is Integer.MAX_VALUE - 7, not 4x the tier below, because 4x UXV overflows int.
        assertEquals(42_949_672_800.0d, Units.watts(2_147_483_640L), EPS);
    }

    @Test
    @DisplayName("Demand rounds up and output rounds down, so quantization cannot create energy")
    void roundingCannotManufactureEnergy() {
        assertEquals(1L, Units.euDemand(0.4d));
        assertEquals(0L, Units.euOutput(0.9d));

        // The invariant that matters: for any energy, the compiled output never exceeds the
        // compiled demand. This is objective O2 stated in code.
        double[] samples = { 0.0d, 0.1d, 0.5d, 1.0d, 1.5d, 999.999d, 1.05e6d };
        for (double joules : samples) {
            assertTrue(
                Units.euOutput(joules) <= Units.euDemand(joules),
                "output exceeded demand at " + joules + " J");
        }
    }

    @Test
    @DisplayName("An iron ingot melt is about 1.05 MJ, the figure the pacing argument rests on")
    void ironIngotMeltDuty() {
        final double massKg = 1.134d; // 144 mL at 7.874 g/cm3
        final double cpKjPerKgK = 0.45d;
        final double deltaT = 1811.0d - 298.0d;
        final double fusionKjPerKg = 247.0d;

        double dutyJoules = massKg * (cpKjPerKgK * deltaT + fusionKjPerKg) * 1000.0d;

        // Roughly 1.05 MJ. At HV (10.24 kW) that is about 103 s, the same order as stock GTNH
        // blast furnace timings -- the agreement that settled the matter basis.
        assertEquals(1.05e6d, dutyJoules, 2.0e4d);
        assertEquals(103.0d, Units.euDemand(dutyJoules) / Units.watts(512L), 2.0d);
    }
}
