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

### A1.1 — The recipe side, and an inverted rung

Recipe requirements are set through the `COIL_HEAT` metadata key (`GTRecipeConstants.java:86`). Distribution across the tree, in Kelvin:

| Requirement | Recipe count | Note |
|---|---|---|
| 300, 500 | 1 each | at or below ambient |
| 573–800 | ~42 | 800 K alone accounts for 35 |
| 900–1500 | ~63 | 1200 K alone accounts for 50 |
| 1688–1800 | 6 | at the Cupronickel ceiling |
| 1900–3663 | ~20 | |
| 4000–7200+ | ~30 | |

The great majority of recipes sit at or below 1800 K, inside what GregTech rates Cupronickel for.

**A separate defect — the ladder is order-inverted at one rung.** GregTech ranks Nichrome (3601) above Kanthal (2701). In reality FeCrAl (Kanthal) outperforms NiCr (Nichrome): Kanthal A-1 elements run to about 1673 K and APM to 1698 K, while Nichrome 80/20 elements are limited to roughly 1473 K. The correct resistive ordering is Cupronickel ≪ Nichrome < Kanthal, so this is not merely a magnitude error — the tiers are in the wrong sequence, and fixing magnitudes alone would preserve the mistake.

**Two recipe classes cannot survive correction:**

- **At or below ambient.** A blast furnace recipe requiring 300 K is asking for room temperature. These do not belong in a heat-gated machine at all and should be re-homed rather than re-tuned.
- **Above every crucible.** Requirements at 7200 K exceed the boiling point of every element — tungsten boils at 5828 K, rhenium at 5869 K, and carbon sublimes near 3915 K. No container holds a 7200 K condensed phase. These are only physical as plasma or containerless processing, which is the same conclusion the coil side reaches from the other direction.

So A1's coordinated change has four parts: real service temperatures at the low tiers, swapping the Kanthal/Nichrome ordering, a technology transition above roughly 1700 K (MoSi₂ reaches ~2123 K, then induction, arc and plasma), and relocating the sub-ambient recipes out of the blast furnace entirely.

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

**Disposition: correct, by one global rescale.** The ordering is already physically correct, so this is a single correction rather than eleven. The origin of the ≈ 2.3 factor is resolved in A7, and it is not benign.

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

## A7 — Generators emit more energy than their fuel contains, and efficiency scales the wrong way

**Stock:** generator efficiency is a construction argument in `LoaderMetaTileEntities.registerCombustionGenerators` / `registerGasTurbines`. Combustion: LV 95, MV 90, HV 85. Gas turbine: LV 95, MV 90, HV 85, EV 60, IV 50 (percent).

**Violation — energy creation.** Delivered energy is `FUEL_VALUE × efficiency / 100`. For methane in an LV gas turbine that is 104 × 0.95 = **98.8 kJ/L delivered from a fuel whose real lower heating value is 35.8 kJ/L**. Efficiencies of 85–95 % cannot absorb the A6 offset — they barely move it. GregTech generators therefore return roughly 2.3–2.9× the chemical energy of their input across the whole gas ladder. Under UNITS.md this is a direct breach of objective O2, and it is the single clearest "free energy" defect the audit has found.

This also rules out the two benign explanations for A6's offset. It is not efficiency headroom, since efficiency is declared near unity. It could still be a compressed-gas basis — declaring GT's gas fluids to sit at ≈ 2.5 bar rather than 1 atm would rescale every gas at once, exactly as A2 does for steam. But the A6 residuals span 1.82 to 2.90, so a single pressure declaration would leave a ±25 % per-species error, against A2's 2 %. That is wide enough that the pressure story would be concealing a real spread rather than explaining it.

**Disposition: correct.** Rebuild gas fuel energies from real lower heating values at 1 atm and let generator efficiency do only what efficiency does. The consequence is a uniform ≈ 2.3× reduction in gas generator output, which is a balance shift but a predictable and uniform one, and SCOPE §6 rule 1 permits making a step harder.

**Second violation — efficiency scales backwards.** GregTech makes larger turbines *worse*: 95 % at LV falling to 50 % at IV. Real thermal plant runs the other way, since larger machines support higher pressure ratios, reheat and combined cycles — small engines reach roughly 30 % while combined-cycle plant exceeds 60 %. Combined with A4's doubling of energy cost per overclock tier, GregTech systematically penalizes scale where reality rewards it.

**Disposition: open.** Inverting the efficiency ladder is physically correct but interacts with progression pacing across every generator tier, so it needs its own analysis rather than a quick flip.

## A8 — The steam cycle is stingy by roughly the same order that combustion is generous

**Stock, verified end to end:**

- `GTValues.STEAM_PER_WATER = 160` (`GTValues.java:427`), applied in `MTEBoiler.produceSteam` (`MTEBoiler.java:219-221`) and `MTELargeBoiler.java:391-393`. One mB of water yields 160 mB of steam.
- `MTESteamTurbine` states its own rate in `getDescription`: **"Base rate: 2L of Steam → 1 EU"**, with `getEfficiency() = 6 + mTier` and `consumedFluidPerOperation` returning that efficiency (`MTESteamTurbine.java:64-97`).

Therefore **1 mB water → 160 mB steam → 80 EU**.

**Check against reality.** Adopting A2's finding that GregTech's steam is saturated steam at 1.2 MPa, raising 1 g of water from 298 K to that state requires h_g − h_f ≈ 2784 − 105 = **2679 J**. GregTech returns **80 J** of electricity from it.

That is a **thermal efficiency of 3.0 %**. A modern Rankine plant reaches 35–40 %. Watt-era beam engines managed 3–5 %. GregTech's steam cycle is, quantitatively, an eighteenth-century engine — thematically defensible for early game, except that the same conversion persists unchanged into the late tiers.

**Efficiency scales backwards here too.** Fuel efficiency is `600 / (6 + tier)` percent: 85.7 % at LV, 75 % at MV, 66.7 % at HV. This is the third machine family showing A7's inversion, which makes it a systemic design choice rather than an oversight in one class.

### The cross-system spread, quantified

This closes the question that started the project. Two subsystems, measured against the same physics:

| Path | GregTech versus reality |
|---|---|
| Chemical fuel in a gas turbine (A6, A7) | **≈ 2.3× generous** — returns more than the fuel's chemical energy |
| Water → steam → turbine (A8) | **≈ 12× stingy** — 3.0 % thermal efficiency against ~35 % |

End to end the two disagree by a factor of roughly **27**. The same joule is worth about 27× more depending on whether it arrives by combustion or by steam. That is why battery filling, NEI recipe costs and water heating each implied a different EU-to-joule factor: they were never measuring one quantity.

**On NEI specifically:** the recipe browser is not an independent anchor. It displays EU/t and duration and multiplies them, so it is arithmetically consistent with whatever the recipe declares. The inconsistency observed there is this cross-subsystem spread showing through, not a separate defect.

**Disposition: correct, jointly with A6 and A7.** These must move together. Fixing combustion alone widens the gap to ~27× in the other direction; fixing steam alone does the same. The correct sequence is to set both against real thermodynamics in one change, then re-check progression pacing across every generator tier.

### Why this matters beyond fuels

A6 answers the question that opened this work. The observed inconsistency between battery filling, NEI costs and water heating is not evidence that GregTech was built without physics — the gas ladder shows real chemistry underneath. It is evidence that **GregTech's physics is correct within a phase and breaks across phases**, because the one quantity that relates volume to energy — density — was never modelled. Supplying it is the highest-leverage correction available.

---

## Outstanding

M0's audit questions are now closed. What remains is design work, not investigation.

- ~~Recipe temperature requirements~~ — enumerated; see A1.1, which also found the inverted Kanthal/Nichrome rung.
- ~~NEI displayed recipe costs~~ — not an independent anchor; see A8. NEI multiplies EU/t by duration and is internally consistent.
- ~~Water-heating energy~~ — traced end to end; see A8.
- ~~The ≈ 2.3 gas offset~~ — resolved in A7. Not efficiency headroom; a compressed-gas reinterpretation was considered and rejected on residual spread.
- ~~Generator efficiencies per tier~~ — extracted; see A7.

**Open design questions, each needing its own analysis before any code:**

1. **The joint energy correction (A6 + A7 + A8).** Combustion and steam must be corrected together or the 27× gap simply moves. Requires a pacing pass over every generator tier.
2. **Inverting the efficiency ladder (A7, A8).** Real plant grows more efficient with scale; GregTech does the reverse in at least three machine families. Physically clear, but it touches progression everywhere.
3. **The A1 coordinated change.** Real service temperatures, the Kanthal/Nichrome swap, a heating-technology transition above ~1700 K, and re-homing the sub-ambient recipes.
4. **Arrhenius kinetics (A5)** — the natural bridge from M0 into M2's continuous-flow reactor.
- **Recipe temperature requirements** — must be enumerated before A1 can be actioned, since the two changes are coupled.
- **NEI displayed recipe costs** and **water-heating energy** — the two other inconsistencies observed in play; not yet traced to source.
