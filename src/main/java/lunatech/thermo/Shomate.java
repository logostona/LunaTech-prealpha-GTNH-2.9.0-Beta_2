package lunatech.thermo;

import lunatech.data.HeatCapacity;
import lunatech.data.ShomateRange;

/**
 * The Shomate equation, as published by the NIST Chemistry WebBook.
 * <p>
 * Heat capacity, with t = T / 1000 and Cp in J/(mol*K):
 * Cp = A + B*t + C*t^2 + D*t^3 + E/t^2
 * <p>
 * Its analytic integral, which is why this form is used rather than a plain polynomial. NIST
 * publishes it referenced to 298.15 K, in kJ/mol:
 * H(T) - H(298.15) = A*t + B*t^2/2 + C*t^3/3 + D*t^4/4 - E/t + F - H
 * <p>
 * Heating duty is therefore an exact integral rather than Cp x deltaT. That distinction is not
 * academic: for iron heated from 298 K to melting, the constant-Cp approximation understates the
 * duty by about 55 percent, because Cp rises steeply and the magnetic transition near 1042 K is
 * invisible to a single value.
 * <p>
 * Extrapolation outside a coefficient set's range is refused rather than approximated. Iron's
 * 1100-1809 K coefficients evaluated at 298 K return roughly 87 times the true heat capacity, so a
 * silent extrapolation would be worse than no answer at all.
 */
public final class Shomate {

    private Shomate() {}

    /** NIST references the integrated form to this temperature. */
    public static final double REFERENCE_KELVIN = 298.15d;

    /** Molar heat capacity in J/(mol*K). */
    public static double heatCapacityMolar(HeatCapacity capacity, double kelvin) {
        ShomateRange range = require(capacity, kelvin);
        double t = kelvin / 1000.0d;
        return range.a + range.b * t + range.c * t * t + range.d * t * t * t + range.e / (t * t);
    }

    /** Specific heat capacity in J/(kg*K), given molar mass in g/mol. */
    public static double heatCapacityMass(HeatCapacity capacity, double molarMassGramsPerMole, double kelvin) {
        requirePositive(molarMassGramsPerMole, "molar mass");
        double molar = heatCapacityMolar(capacity, kelvin);
        return molar / (molarMassGramsPerMole / 1000.0d);
    }

    /** Enthalpy relative to {@link #REFERENCE_KELVIN}, in J/mol. */
    public static double enthalpyFromReferenceMolar(HeatCapacity capacity, double kelvin) {
        ShomateRange range = require(capacity, kelvin);
        double t = kelvin / 1000.0d;
        double kilojoules = range.a * t + range.b * t * t / 2.0d
            + range.c * t * t * t / 3.0d
            + range.d * t * t * t * t / 4.0d
            - range.e / t
            + range.f
            - range.h;
        return kilojoules * 1000.0d;
    }

    /**
     * Enthalpy change between two temperatures, in J/mol. Both must lie inside the covered range;
     * the reference offsets cancel, so this works across range boundaries.
     */
    public static double enthalpyChangeMolar(HeatCapacity capacity, double fromKelvin, double toKelvin) {
        double from = enthalpyFromReferenceMolar(capacity, fromKelvin);
        double to = enthalpyFromReferenceMolar(capacity, toKelvin);
        return to - from;
    }

    /** Enthalpy change per kilogram, in J/kg, given molar mass in g/mol. */
    public static double enthalpyChangeMass(HeatCapacity capacity, double molarMassGramsPerMole, double fromKelvin,
        double toKelvin) {
        requirePositive(molarMassGramsPerMole, "molar mass");
        double molar = enthalpyChangeMolar(capacity, fromKelvin, toKelvin);
        return molar / (molarMassGramsPerMole / 1000.0d);
    }

    private static ShomateRange require(HeatCapacity capacity, double kelvin) {
        if (capacity == null) {
            throw new IllegalArgumentException("No heat capacity data supplied");
        }
        requirePositive(kelvin, "temperature");
        ShomateRange range = capacity.rangeFor(kelvin);
        if (range == null) {
            throw new IllegalArgumentException(
                "No Shomate range covers " + kelvin
                    + " K; data spans "
                    + capacity.minKelvin()
                    + " to "
                    + capacity.maxKelvin()
                    + " K and extrapolation is refused");
        }
        return range;
    }

    private static void requirePositive(double value, String what) {
        boolean usable = value > 0.0d && !Double.isInfinite(value) && !Double.isNaN(value);
        if (!usable) {
            throw new IllegalArgumentException(what + " must be finite and positive: " + value);
        }
    }
}
