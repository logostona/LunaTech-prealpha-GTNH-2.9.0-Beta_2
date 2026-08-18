package lunatech.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.data.Datasets;
import lunatech.data.Material;
import lunatech.thermo.Shomate;

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
            assertTrue(Units.euOutput(joules) <= Units.euDemand(joules), "output exceeded demand at " + joules + " J");
        }
    }

    @Test
    @DisplayName("An iron ingot melt is about 1.47 MJ, integrated over Cp(T) rather than approximated")
    void ironIngotMeltDuty() {
        Material iron = Datasets.material("iron");

        double massKg = iron.massKilograms(Units.MILLIBUCKETS_PER_INGOT);

        // Iron's Shomate data stops at 1809 K against a melting point of 1811 K. The 2 K gap sits
        // inside the +/- 2 K agreement budget ratified for melting points, and contributes about
        // 0.1 percent of the duty.
        double sensible = Shomate.enthalpyChangeMass(iron.heatCapacity, iron.molarMass.value, 298.15d, 1809.0d);
        double dutyJoules = massKg * (sensible + iron.enthalpyOfFusion.value);

        assertEquals(1.134d, massKg, 1.0e-3d);
        assertEquals(1.471e6d, dutyJoules, 5.0e3d);

        // About 144 s at HV. The constant-Cp approximation this replaces gave 103 s, understating
        // the duty by 40 percent -- still the same order as stock blast furnace timings, so the
        // matter basis survives, but the number it rests on has moved.
        assertEquals(144.0d, Units.euDemand(dutyJoules) / Units.watts(512L), 2.0d);
        assertTrue(iron.heatCapacity.isExperimental(), "the pacing argument rests on measured Cp(T)");
    }
}
