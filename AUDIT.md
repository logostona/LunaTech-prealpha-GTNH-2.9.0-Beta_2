# LunaTech — Stock Anchor Audit

**Status:** Draft 0.1, in progress. Deliverable of [SCOPE.md](SCOPE.md) M0.
**Source of truth:** GT5-Unofficial at tag `5.09.52.594`, as shipped in GTNH 2.9.0-beta2.
**Measured against:** [UNITS.md](UNITS.md) — 1 EU ≡ 1 J, 1 tick ≡ 0.05 s, 1 mB ≡ 1 mL.

Each finding records the specific physical violation and a disposition. Per [SCOPE.md §6](SCOPE.md#6-gtnh-compatibility-policy) rule 2, this table *is* the justification for any replacement; a stock value LunaTech changes must appear here first.

Dispositions: **correct** (replace) · **accept** (physically defensible as-is) · **reinterpret** (keep the number, restate what it means) · **open** (needs more work).

---

## A1 — Heating coil temperatures exceed the melting points of their own alloys

**Stock:** `HeatingCoilLevel.getHeat()` returns `1 + 900 × ordinal`, yielding Cupronickel 1801, Kanthal 2701, Nichrome 3601, TPV alloy 4501, HSS-G 5401 … Eternal 13501. Nominally Kelvin.

**Violation:** a resistive heating element cannot sustain a temperature above its own melting point. GT assigns Kanthal 2701 K against a real melting point near 1780 K and a maximum service temperature near 1670 K. Nichrome is assigned 3601 K against a melting point near 1670 K — an excess of over 1900 K. Cupronickel is assigned 1801 K against roughly 1450 K.

**Second-order problem:** the ladder is linear by construction (900 K per tier) rather than reflecting any material property, and it extends to 13501 K. No solid resistive element exists above roughly 3700 K (tungsten). Everything past about tier 4 is not a coil in any physical sense.

**Disposition: correct — large, and progression-coupled.** Real coil temperatures compress the low ladder and cap the resistive regime near 3000 K. High tiers must become a different heating technology (induction, arc, plasma, electron beam), which is a content opportunity rather than a loss. Because coil tier gates blast furnace recipes, correcting temperatures without simultaneously revisiting recipe temperature requirements would make existing recipes unreachable. Under SCOPE §6 rule 1 shifting them *later* is permitted, but this must be done as one coordinated change, not piecemeal.

## A2 — Steam expansion ratio is not atmospheric steam, but is almost exactly 1.2 MPa steam

**Stock:** `GTValues.STEAM_PER_WATER = 160`. One mB of water yields 160 mB of steam.

**Naive violation:** at 373.15 K and 101.325 kPa, saturated steam has a specific volume near 1.673 m³/kg. Since 1 mL of water is 1 g, atmospheric steam should expand about 1673-fold. GT's 160 is low by a factor of 10.5.

**But:** invert the question. A specific volume of 160 mL/g is 0.160 m³/kg, and saturated steam reaches 0.16333 m³/kg at **1.2 MPa, T_sat ≈ 461 K (188 °C)**. GT's constant is within **2 %** of saturated steam at 1.2 MPa — a completely ordinary industrial steam condition.

**Disposition: reinterpret.** GT's steam is not wrong, it is *unlabelled*. Declare GT steam to be saturated steam at 1.2 MPa and 461 K rather than atmospheric steam, and the existing constant becomes correct to within 2 % at zero gameplay cost. This also gives steam a real pressure, which UNITS.md §3.1 and the thermodynamic pillar both need. ✅ Strongly recommended — one of the cheapest realism wins available.

## A3 — Battery capacity is physically plausible as-is

**Stock:** `MetaGeneratedItem01.java:5158` — LV lithium battery, capacity 100 000 EU at V[1] = 32.

**Check:** 100 000 EU = 100 kJ = **27.8 Wh**. A single 18650 lithium-ion cell holds roughly 10–12 Wh; a small two- or three-cell pack holds 25–35 Wh.

**Disposition: accept.** This value is already right to within the tolerance of "which pack are we talking about." It is worth recording that not everything in GregTech needs correcting — this is direct evidence that κ = 1 J/EU is well-scaled, since a value chosen purely for gameplay lands on the correct physical magnitude under it.

## A4 — Standard overclocking: correct in sign, unjustified in magnitude

**Stock:** `OverclockCalculator.java:33-35` — `eutIncreasePerOC = 4`, `durationDecreasePerOC = 2`. Total energy per operation therefore **doubles per overclock tier**. `enablePerfectOC()` sets the divisor to 4, conserving energy.

**Assessment:** under κ = 1 this is a claim about efficiency, and the direction is physically correct — driving a process faster dissipates more, because irreversible losses grow with rate. Perfect overclocking is the reversible ideal.

**Disposition: reinterpret, magnitude open.** Retain the mechanic and read it as entropy production (see UNITS.md §6). The factor of exactly 2 per tier is not derived from anything and should eventually be grounded in the specific process; the sign is right, the number is arbitrary.

## A5 — Heat overclocking uses a linear temperature threshold where kinetics are exponential

**Stock:** `OverclockCalculator.java:74-75` — `HEAT_DISCOUNT_THRESHOLD = 900`, `HEAT_OVERCLOCK_THRESHOLD = 1800`, with `durationDecreasePerHeatOC = 4`. Every 1800 K of furnace temperature above the recipe requirement quadruples speed at constant energy; every 900 K grants an energy discount.

**Violation:** this is wrong in *form*, not merely in magnitude. Reaction rates follow Arrhenius behaviour, k ∝ exp(−Eₐ/RT), so a fixed temperature excess cannot produce a fixed rate multiplier — the same ΔT of 1800 K yields wildly different acceleration at 1000 K than at 10 000 K. A linear-threshold model can be tuned to match at exactly one temperature and is wrong everywhere else.

**Disposition: correct.** Replace with an Arrhenius rate model parameterized by per-reaction activation energy. This is directly downstream of the "reaction kinetics" commitment in PHILOSOPHY §3 Pillar I, and it is the natural bridge from M0 into M2's continuous-flow reactor, where rate laws are the core mechanic.

## A6 — Gas fuel values track real chemistry to ±20 %; liquid fuel values are meaningless because GregTech has no density

**Stock:** fuel energy is set by `MaterialBuilder.setFuel(FuelType, fuelPower)` in `MaterialsInit.java` and auto-registered through the `FUEL_VALUE` metadata key. Semantics verified at `MTEBasicGenerator.java:262-277`: `consumedFluidPerOperation` returns **1**, and fuel energy is `FUEL_VALUE × efficiency × 1 / 100`. So **`FUEL_VALUE` is EU per millibucket** — under UNITS.md, joules per mL, i.e. **kJ per litre** at 100 % efficiency.

### Gases — compared against real lower heating value as an ideal gas at STP (22.414 L/mol)

| Fuel | GT (kJ/L) | Real LHV (kJ/L) | GT ÷ real |
|---|---|---|---|
| Hydrogen | 20 | 10.8 | 1.85 |
| Carbon monoxide | 24 | 12.6 | 1.90 |
| Methane | 104 | 35.8 | 2.90 |
| Ethylene | 128 | 59.0 | 2.17 |
| Ethane | 168 | 63.7 | 2.64 |
| Propene | 192 | 85.9 | 2.23 |
| Butadiene | 206 | 113.4 | 1.82 |
| Propane | 232 | 91.2 | 2.54 |
| Butane | 296 | 118.6 | 2.50 |
| Toluene | 328 | 168.3 | 1.95 |
| Benzene | 360 | 141.4 | 2.55 |

**Eleven independent gases, ratios spanning 1.82 to 2.90, mean ≈ 2.3.** This is not a coincidence and not arbitrary balance: GregTech's gas fuel ladder is *ordered and scaled by real combustion enthalpy*, offset by a near-constant factor. Note that benzene and toluene — liquids at room temperature — fit the same cluster only when treated as ideal gases at STP, which confirms the phase convention rather than contradicting it.

**Disposition: reinterpret, then rescale by one constant.** The ordering is already physically correct. The residual factor of ≈ 2.3 is a single global correction, not 11 separate ones, and its origin is a follow-up question (a compressed-gas basis, or a deliberate generator-efficiency headroom, would both produce a constant offset of roughly this size).

### Liquids — compared against real LHV as a liquid

| Fuel | GT (kJ/L) | Real LHV (kJ/L) | GT ÷ real |
|---|---|---|---|
| Methanol | 84 | ≈ 15 800 | 1/188 |
| Ethanol | 192 | ≈ 21 100 | 1/110 |
| Light Fuel | 305 | ≈ 35 300 | 1/116 |
| Biodiesel | 320 | ≈ 33 000 | 1/103 |
| Fuel (diesel) | 480 | ≈ 35 800 | 1/75 |
| Gasoline | 576 | ≈ 32 300 | 1/56 |
| Octane | 80 | ≈ 31 200 | 1/390 |

**Violation:** liquid fuels sit on roughly the same numeric scale as gases despite holding some 600–1000× more mass per litre. The cause is structural, not a balance error — **GregTech has no density, so energy-per-volume cannot be consistent across phases.** This is the same missing quantity identified in UNITS.md §4, now visible as a concrete defect.

**Internal contradiction, independent of any real-world comparison:** octane is rated 80 while gasoline is rated 576, though octane is essentially the principal component of gasoline — a 7× disagreement inside GregTech's own data.

**Also noted:** `NatruralGas` (spelled thus in source) is rated 20 kJ/L against methane's 104, though natural gas is predominantly methane. It falls far outside the gas cluster and looks like a straightforward error.

**Disposition: correct.** Liquid fuel energies must be rebuilt from density × mass-specific LHV once the matter basis is in force. This is the single largest quantitative correction the audit has identified, and it is a direct consequence of adopting 1 mB ≡ 1 mL.

### Why this matters beyond fuels

A6 answers the question that opened this work. The observed inconsistency between battery filling, NEI costs and water heating is not evidence that GregTech was built without physics — the gas ladder shows real chemistry underneath. It is evidence that **GregTech's physics is correct within a phase and breaks across phases**, because the one quantity that relates volume to energy — density — was never modelled. Supplying it is the highest-leverage correction available.

---

## Outstanding

- **Recipe temperature requirements** — must be enumerated before A1 can be actioned, since the two changes are coupled.
- **NEI displayed recipe costs** and **water-heating energy** — the two other inconsistencies observed in play; not yet traced to source.
- **The ≈ 2.3 gas offset** — determine whether it is a compressed-gas basis, generator-efficiency headroom, or an arbitrary constant.
- **Generator efficiencies per tier** — needed to convert `FUEL_VALUE` into delivered energy, since the raw value is pre-efficiency.
- **Recipe temperature requirements** — must be enumerated before A1 can be actioned, since the two changes are coupled.
- **NEI displayed recipe costs** and **water-heating energy** — the two other inconsistencies observed in play; not yet traced to source.
