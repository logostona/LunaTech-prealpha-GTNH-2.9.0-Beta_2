package lunatech.thermo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lunatech.data.Datasets;
import lunatech.data.HeatCapacity;
import lunatech.data.Material;

/** Pins the Shomate evaluation that every heating duty now depends on. */
class ShomateTest {

    private static HeatCapacity ironCapacity() {
        Material iron = Datasets.material("iron");
        return iron.heatCapacity;
    }

    @Test
    @DisplayName("Heat capacity at 298 K agrees with the independently sourced single point")
    void agreesWithTheSinglePointValue() {
        Material iron = Datasets.material("iron");
        double mass = Shomate.heatCapacityMass(iron.heatCapacity, iron.molarMass.value, 298.15d);

        // NIST Shomate against CRC's single measured value: two independent sources, 0.1 percent
        // apart. This is the agreement rule of SCOPE.md 3.1 actually being exercised.
        assertEquals(449.4d, mass, 0.5d);
        assertEquals(iron.specificHeat.value, mass, iron.specificHeat.value * 0.03d);
    }

    @Test
    @DisplayName("The integrated form is referenced to 298.15 K, so it vanishes there")
    void enthalpyVanishesAtReference() {
        double joules = Shomate.enthalpyFromReferenceMolar(ironCapacity(), 298.15d);
        assertEquals(0.0d, joules, 1.0d);
    }

    @Test
    @DisplayName("Heating iron to melting takes 58.6 kJ/mol, not the 45 kJ/mol constant Cp implies")
    void integratedEnthalpyToMelting() {
        double joules = Shomate.enthalpyChangeMolar(ironCapacity(), 298.15d, 1809.0d);
        assertEquals(58_642.0d, joules, 100.0d);

        // What the discarded approximation would have given: 449 J/(kg K) x 55.845 g/mol x 1511 K.
        double constantCp = 449.0d * (55.845d / 1000.0d) * (1809.0d - 298.15d);
        assertTrue(
            joules > constantCp * 1.4d,
            "integrated duty should exceed the constant-Cp estimate by ~55 percent, got " + joules
                + " vs "
                + constantCp);
    }

    @Test
    @DisplayName("Heat capacity rises with temperature, which a single value cannot express")
    void capacityRisesWithTemperature() {
        double cold = Shomate.heatCapacityMolar(ironCapacity(), 298.15d);
        double warm = Shomate.heatCapacityMolar(ironCapacity(), 600.0d);
        assertTrue(warm > cold, "expected Cp to rise, got " + cold + " then " + warm);
    }

    @Test
    @DisplayName("Extrapolation outside a coefficient set's range is refused, not approximated")
    void refusesToExtrapolate() {
        // Iron's data stops at 1809 K. Evaluating the top set at 298 K returns about 87 times the
        // true capacity, so silently picking the nearest range would be worse than failing.
        assertThrows(IllegalArgumentException.class, () -> Shomate.heatCapacityMolar(ironCapacity(), 2500.0d));
        assertThrows(IllegalArgumentException.class, () -> Shomate.heatCapacityMolar(ironCapacity(), 200.0d));
        assertThrows(IllegalArgumentException.class, () -> Shomate.heatCapacityMolar(ironCapacity(), -1.0d));
    }

    @Test
    @DisplayName("Range selection crosses boundaries without a discontinuity in enthalpy")
    void rangesJoinContinuously() {
        // 1042 K is a range boundary and the magnetic transition. Enthalpy either side must agree.
        double below = Shomate.enthalpyFromReferenceMolar(ironCapacity(), 1041.9d);
        double above = Shomate.enthalpyFromReferenceMolar(ironCapacity(), 1042.1d);
        assertEquals(below, above, 500.0d, "enthalpy jumps across the 1042 K boundary");
    }
}
