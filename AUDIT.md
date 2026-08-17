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

---

## Outstanding

- **Chemical fuel energy values** — the last M0 item. Mechanism located (`Materials.mFuelPower` / `mFuelType`, auto-registered through `GTItemIterator` into the `Fuel` map via the `FUEL_VALUE` key), values not yet extracted. Requires walking the `Materials` enum. These are the anchors with genuine real-world lower heating values to compare against, so they are the highest-value remaining audit input.
- **Recipe temperature requirements** — must be enumerated before A1 can be actioned, since the two changes are coupled.
- **NEI displayed recipe costs** and **water-heating energy** — the two other inconsistencies observed in play; not yet traced to source.
