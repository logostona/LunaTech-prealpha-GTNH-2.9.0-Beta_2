package lunatech.kinetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.data.Datasets;
import lunatech.data.Reaction;

/** Pins the reactor model that objective O4 rests on. */
class ContinuousReactorTest {

    @Test
    @DisplayName("At its own reference point a reaction reproduces its declared conversion")
    void referencePointIsSelfConsistent() {
        Reaction shift = Datasets.reaction("water_gas_shift");
        double conversion = ContinuousReactor.conversion(shift, 673.0d, 60.0d);
        assertEquals(0.80d, conversion, 1.0e-9d);
    }

    @Test
    @DisplayName("Doubling residence time raises conversion, without ever reaching one")
    void longerResidenceConvertsMore() {
        // Reforming rather than shift: shift declares a 0.95 ceiling that masks the rate law here.
        Reaction reforming = Datasets.reaction("steam_methane_reforming");
        double once = ContinuousReactor.conversion(reforming, 1123.0d, 30.0d);
        double twice = ContinuousReactor.conversion(reforming, 1123.0d, 60.0d);
        assertEquals(0.70d, once, 1.0e-9d);
        assertEquals(0.91d, twice, 1.0e-3d);
        assertTrue(twice > once);
        assertTrue(twice < 1.0d);
    }

    @Test
    @DisplayName("Temperature acts exponentially: 100 K lifts shift conversion from 0.80 to near completion")
    void temperatureDominates() {
        Reaction shift = Datasets.reaction("water_gas_shift");
        // Uncapped this is ~0.99996; the reaction declares an equilibrium ceiling of 0.95.
        double hot = ContinuousReactor.conversion(shift, 773.0d, 60.0d);
        assertEquals(0.95d, hot, 1.0e-9d);
    }

    @Test
    @DisplayName("A declared equilibrium ceiling caps conversion however long the hold")
    void equilibriumCeilingHolds() {
        Reaction shift = Datasets.reaction("water_gas_shift");
        double veryLong = ContinuousReactor.conversion(shift, 673.0d, 100_000.0d);
        assertEquals(0.95d, veryLong, 1.0e-9d);
    }

    @Test
    @DisplayName("Without a ceiling, conversion stays below one until double precision runs out")
    void uncappedApproachesUnity() {
        Reaction reforming = Datasets.reaction("steam_methane_reforming");

        // At a realistic hold the asymptote is visible: high, and strictly short of complete.
        double held = ContinuousReactor.conversion(reforming, 1123.0d, 300.0d);
        assertTrue(held > 0.99d, "expected near-complete conversion, got " + held);
        assertTrue(held < 1.0d, "conversion should not reach one at a realistic residence time");

        // At an absurd hold, exp(-Da) underflows and the result rounds to exactly 1.0. Documented
        // in ContinuousReactor rather than clamped, since the unconverted fraction there is far
        // below anything representable.
        double absurd = ContinuousReactor.conversion(reforming, 1123.0d, 100_000.0d);
        assertEquals(1.0d, absurd, 0.0d);
    }

    @Test
    @DisplayName("A steeper activation energy gives a sharper temperature response")
    void activationEnergyShapesSensitivity() {
        Reaction shift = Datasets.reaction("water_gas_shift");
        Reaction reforming = Datasets.reaction("steam_methane_reforming");

        double shiftRate = Arrhenius
            .rateMultiplier(shift.activationEnergy.value, shift.referenceTemperature.value, 723.0d);
        double reformingRate = Arrhenius
            .rateMultiplier(reforming.activationEnergy.value, reforming.referenceTemperature.value, 1173.0d);

        // Both are 50 K above their reference. Reforming's larger Ea makes it respond more strongly,
        // which is exactly the behaviour GregTech's fixed 1800 K threshold cannot express.
        assertTrue(
            reformingRate > shiftRate,
            "expected the steeper activation energy to respond more, " + reformingRate + " vs " + shiftRate);
    }

    @Test
    @DisplayName("Residence time follows from vessel volume and throughput")
    void residenceTimeFromFlow() {
        assertEquals(30.0d, ContinuousReactor.residenceTimeSeconds(60.0d, 2.0d), 1.0e-9d);

        Reaction shift = Datasets.reaction("water_gas_shift");
        double byFlow = ContinuousReactor.conversion(shift, 673.0d, 120.0d, 2.0d);
        double byTime = ContinuousReactor.conversion(shift, 673.0d, 60.0d);
        assertEquals(byTime, byFlow, 1.0e-12d);
    }

    @Test
    @DisplayName("Impossible operating conditions are rejected")
    void rejectsNonsense() {
        Reaction shift = Datasets.reaction("water_gas_shift");
        assertThrows(IllegalArgumentException.class, () -> ContinuousReactor.conversion(shift, 673.0d, 0.0d));
        assertThrows(IllegalArgumentException.class, () -> ContinuousReactor.conversion(shift, -1.0d, 60.0d));
        assertThrows(IllegalArgumentException.class, () -> ContinuousReactor.residenceTimeSeconds(60.0d, 0.0d));
        assertThrows(IllegalArgumentException.class, () -> ContinuousReactor.conversion(null, 673.0d, 60.0d));
    }
}
