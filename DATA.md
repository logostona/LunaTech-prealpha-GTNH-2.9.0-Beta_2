# LunaTech — Dataset Format

**Status:** Draft 0.1. Resolves [SCOPE.md](SCOPE.md) §9 D4.
**Parsed with:** Gson, which Minecraft 1.7.10 already provides — no new runtime dependency.

## 1. Decisions

⚖️ **JSON.** Datasets are JSON resources under `src/main/resources/lunatech/data/`, loaded from the classpath at runtime and validated by the harness at build time.

⚖️ **Every value is an object, never a bare number.** A bare `7874` records a magnitude and nothing else. Objective O5 requires provenance on every constant, and [SCOPE.md §3.1](SCOPE.md#31-draft-error-budgets--numbers-to-ratify--9-d3) requires estimated values to inherit their method's uncertainty rather than the experimental budget. Neither survives a bare number, so the schema makes the disciplined form the only form.

⚖️ **Units are explicit strings on every quantity, in SI.** The harness asserts the unit string per field. A silent unit error is the single most likely way to corrupt this project's numbers, and this makes it a build failure rather than a plausible-looking result.

⚖️ **Curated in this repository for now.** There is no generation pipeline yet, so datasets are hand-curated here. When property estimation (QSPR, group contribution) arrives it belongs in a sibling repository per PHILOSOPHY's "Algorithmic Extensibility", emitting files in this same schema. The schema is the contract between them, which is why it is specified independently of how values are produced.

## 2. Versioning

| Field | Meaning |
|---|---|
| `schemaVersion` | Integer. Bumped only on a breaking shape change; the loader rejects versions it does not know. |
| `datasetVersion` | ISO date of last content change. Advisory. |
| `gt5uVersion` | The GregTech build the data was verified against. Must match the pin in `dependencies.gradle`. |

The `gt5uVersion` check is deliberate: [AUDIT.md](AUDIT.md) is only valid against `5.09.52.594`, so a dataset claiming a different build is a defect the harness should catch rather than something to discover later.

## 3. Quantity shape

```json
{
  "value": 7874.0,
  "unit": "kg/m3",
  "source": "CRC Handbook of Chemistry and Physics, 97th ed.",
  "method": "experimental",
  "uncertainty": 1.0
}
```

| Field | Required | Notes |
|---|---|---|
| `value` | yes | Finite. NaN and infinity are rejected. |
| `unit` | yes | SI, and must match the expected unit for that field. |
| `source` | yes | Reference work, DOI, or named estimation method. Never blank. |
| `method` | yes | `experimental`, or `estimated:<model>` — e.g. `estimated:joback`. |
| `uncertainty` | no | Absolute, in the same unit. Required when `method` is not `experimental`. |

## 4. Material record

```json
{
  "id": "iron",
  "formula": "Fe",
  "molarMass":        { "value": 55.845,  "unit": "g/mol", ... },
  "density":          { "value": 7874.0,  "unit": "kg/m3", ... },
  "meltingPoint":     { "value": 1811.0,  "unit": "K",     ... },
  "specificHeat":     { "value": 449.0,   "unit": "J/(kg*K)", ... },
  "enthalpyOfFusion": { "value": 247290.0,"unit": "J/kg",  ... }
}
```

`id` is lowercase, unique, and stable — it is the join key to GregTech materials and must not be renamed casually. Optional properties may be omitted; when present they must be complete.

Expected units, asserted by the harness:

| Field | Unit |
|---|---|
| `molarMass` | `g/mol` |
| `density` | `kg/m3` |
| `meltingPoint`, `boilingPoint` | `K` |
| `specificHeat` | `J/(kg*K)` |
| `enthalpyOfFusion`, `enthalpyOfVaporisation` | `J/kg` |

## 5. Reaction records

```json
{
  "id": "water_gas_shift",
  "equation": "CO + H2O -> CO2 + H2",
  "activationEnergy":     { "value": 80000.0, "unit": "J/mol", ... },
  "referenceTemperature": { "value": 673.0,   "unit": "K",     ... },
  "referencePoint": { "residenceTimeSeconds": 60.0, "conversion": 0.8, "rationale": "..." },
  "maximumConversion": 0.95
}
```

⚖️ **Two kinds of number that look alike and are not.** `activationEnergy` and `referenceTemperature` are physical, so they are `Quantity` objects carrying provenance and held to the budgets in [SCOPE.md §3.1](SCOPE.md#31-error-budgets-⚖%EF%B8%8F-ratified-d3). `referencePoint` is LunaTech's chosen anchor — a design decision, not a measurement — so it is deliberately **not** a `Quantity`. Giving it a source and an uncertainty would let an invented number wear a citation. It carries a `rationale` instead, so the choice stays answerable.

The pair fixes the Damköhler number without any absolute rate constant: a reaction reaching conversion X in time τ has kτ = −ln(1 − X) there, and every other condition follows by ratio.

`maximumConversion` is an optional declared ceiling standing in for a real equilibrium calculation. A kinetic model alone drives an equilibrium-limited reaction to completion given enough time, which is wrong. Until ΔrG(T) is in the dataset this cap is declared rather than computed.

**The seeded values are provisional.** Both reactions carry `estimated:literature-range` activation energies with wide uncertainties, because published values for one reaction differ by 20–30 kJ/mol with catalyst, support and temperature window. They are honest placeholders that satisfy the ratified ceiling, not citations.

Reaction datasets carry no `gt5uVersion`: activation energies are chemistry, not values read out of GregTech, so a GregTech bump does not invalidate them.

## 6. Temperature-dependent heat capacity ✅ resolved

`heatCapacity` holds Shomate coefficient sets in the molar form NIST publishes, each with the temperature range it is valid over:

```json
"heatCapacity": {
  "source": "NIST Chemistry WebBook, Shomate coefficients for the alpha-delta phase of iron",
  "method": "experimental",
  "ranges": [ { "minKelvin": 298.0, "maxKelvin": 700.0, "a": 18.42868, "b": 24.64301, ... } ]
}
```

⚖️ **The range is data, not a footnote.** Shomate coefficients are wildly wrong outside their band — iron's 1100–1809 K set evaluated at 298 K returns about 87 times the true heat capacity. `lunatech.thermo.Shomate` therefore refuses to extrapolate rather than approximating, and the harness requires ranges to be ordered and contiguous: a gap makes some temperatures unevaluable, an overlap makes the answer depend on iteration order.

⚖️ **Stored molar, converted on read.** NIST publishes molar values; converting to a mass basis by hand at data-entry time is where transcription errors hide.

**Duty is integrated, not multiplied.** The Shomate form was chosen because its integral is analytic, so heating enthalpy is exact rather than Cp × ΔT. That distinction is worth 40 % on the one number the matter basis rests on — see [UNITS.md §4](UNITS.md#4-the-matter-basis).

**The single-point `specificHeat` is retained deliberately**, not superseded. It comes from a different source (CRC) than the correlation (NIST), so the two cross-check: `DatasetTest` asserts they agree within the ratified ±3 % agreement budget. They land 0.1 % apart. This is the first thing in the project to exercise the *agreement* rule of [SCOPE.md §3.1](SCOPE.md#31-error-budgets-⚖%EF%B8%8F-ratified-d3), which until now had nothing to check.

`heatCapacity` is optional. A material no duty calculation touches need not carry one, and inventing coefficients to fill the schema would be worse than leaving it absent.

## 7. Conditions on state-dependent values ✅ resolved

`Quantity` carries an optional `temperatureKelvin`. Density and single-point heat capacities are strongly temperature-dependent, and their conditions previously lived only in the `source` string where no test could read them — so two values at different temperatures could satisfy every budget and still be inconsistent. This closes the gap recorded in SCOPE.md §3.1.

## 8. Remaining limitations

- **Pressure is not represented.** Density of a gas is meaningless without it, and no `pressurePascals` field exists yet.
- **Iron's correlation stops at 1809 K** against a melting point of 1811 K. The 2 K gap sits inside the ratified ±2 K agreement budget and contributes about 0.1 % of the melt duty, so it is accepted rather than papered over.
- **Liquid and gas phases have no correlations yet.** Only solid iron carries Cp(T); everything above melting is still unmodelled.
