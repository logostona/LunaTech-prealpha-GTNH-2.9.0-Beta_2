# LunaTech — Unit System

**Status:** Draft 0.1 — pre-alpha. ⚖️ ratified · ❓ open (tracked in [SCOPE.md §9](SCOPE.md#9-open-decisions-register)).
**Target:** GT5-Unofficial `5.09.52.594` as shipped in GTNH 2.9.0-beta2.

This document defines LunaTech's dimensional system. It is normative: any LunaTech value that contradicts it is a defect.

---

## 1. Governing principle

⚖️ **SI is the unit of meaning. EU is a wire protocol.**

Every quantity the player sees, every quantity in a dataset, and every quantity in a recipe definition is SI. EU exists only as the integer encoding GregTech uses to move and store energy, and as the interoperability format at the boundary with mods we do not replace.

⚖️ **Direction of authority: recipes are authored in SI and compiled down to EU at registration time. Never the reverse.**

This is the load-bearing rule. If a recipe is authored in EU and displayed in SI, the SI figure is a decorative rendering of a gameplay-balance integer, and the validation harness ([SCOPE.md §3](SCOPE.md#3-reliability--validation-contract)) has nothing real to check. Authored in SI and compiled to EU, the SI figure is the truth and EU is derived. Runtime behaviour is identical; epistemic status is opposite.

---

## 2. The energy definition

⚖️ **1 EU ≡ 1 joule, exactly.**

κ = 1 J/EU is a **definition, not a measurement.** It is not derived from any stock GTNH value, and no stock value can contradict it — a stock value that implies a different κ is a defect in that value (§5).

Justification is numerical range and dimensional convenience, in that order:

| Property | Consequence of κ = 1 |
|---|---|
| Minimum machine rating (1 EU/t) | 20 W |
| Maximum recipe rating (`mEUt` is `int`) | 42.95 GW |
| Total-energy headroom (`long` EU) | ≈ 9.2 EJ |
| Quantization floor | 1 J |
| EU/t → W | multiply by 20 |
| EU → J | identity |

A smaller κ risks `int` overflow on high-tier power and `long` overflow on accumulated energy; a larger κ quantizes small chemical reactions to zero. κ = 1 sits centrally in the usable range and makes the conversion auditable by inspection.

✅ **Verified at `5.09.52.594`.** `GTRecipe.mEUt` is declared `int` (`GTRecipe.java:102`, alongside `mDuration` and `mSpecialValue`), so recipe power is bounded at 2 147 483 647 EU/t regardless of `GTValues.V[]` being `long[]`. GregTech itself is already pressed against that ceiling: the MAX tier is defined as `Integer.MAX_VALUE - 7`, not as 4× the tier below it, purely to stay inside `int`. The range argument for κ = 1 is therefore not merely comfortable — it is the same constraint GregTech already had to design around, and κ = 1 places LunaTech's usable band exactly where GregTech's own headroom ends.

**Quantization floor:** no LunaTech process may have an energy term below 1 J. Reactions at smaller scale must be aggregated to a larger batch basis rather than rounded.

## 3. Time and power

⚖️ 1 tick ≡ 0.05 s exactly. Therefore 1 EU/t ≡ 20 W exactly.

⚖️ **GT's voltage tiers are retained unmodified.** Their numeric values are treated as tier indices whose *implied power* under κ = 1 happens to fall on a realistic industrial ladder. LunaTech does not renumber them.

✅ Tier ladder **verified** against `GTValues.java:93-96` at `5.09.52.594`. The ladder is ×4 per tier from ULV through UXV, then breaks: MAX is `Integer.MAX_VALUE - 7` = 2 147 483 640, which is 8 less than 4 × UXV, because 4 × UXV = 2 147 483 648 overflows `int`. A 16th entry, 8 589 934 592, exists solely as an out-of-bounds guard and is commented in source as "not really a real tier" — it must never be treated as one.

| Tier | EU/t | Power | Industrial analogue |
|---|---|---|---|
| ULV | 8 | 160 W | hand tool |
| LV | 32 | 640 W | workshop |
| MV | 128 | 2.56 kW | machine shop |
| HV | 512 | 10.24 kW | light industrial |
| EV | 2 048 | 40.96 kW | industrial |
| IV | 8 192 | 163.8 kW | small factory |
| LuV | 32 768 | 655.4 kW | heavy industry |
| ZPM | 131 072 | 2.62 MW | process plant |
| UV | 524 288 | 10.49 MW | small power station |
| UHV | 2 097 152 | 41.94 MW | large industrial site |
| UEV | 8 388 608 | 167.8 MW | — |
| UIV | 33 554 432 | 671.1 MW | — |
| UMV | 134 217 728 | 2.68 GW | national grid scale |
| UXV | 536 870 912 | 10.74 GW | — |
| MAX | 2 147 483 640 | 42.95 GW | ≈ 40 reactors |

### 3.1 Electrical potential and current ❓

GT already computes `EU/t = Voltage × Amperage`, so the structure P = V·I is correct and only unlabelled. Two options remain open (§9 D11):

- **(a)** Assign each tier a real voltage class (LV → 400 V, MV → 3.3 kV, HV → 13.8 kV, EV → 69 kV, IV → 230 kV …) and derive real amperage from P = V·I. Realistic, but real engineering offers ~6 voltage classes against GT's ~15 tiers, so high tiers run out of physical voltages and must scale through current and parallelism instead.
- **(b)** Treat tiers as opaque indices, expose only power in W, and never claim a voltage.

Option (b) is safe and shippable; (a) is richer and needs a story for tiers above UHV.

## 4. The matter basis

⚖️ **1 mB ≡ 1 mL exactly.** One bucket is one litre.

Every other extensive quantity derives from this single declaration, using data LunaTech must ship regardless:

- **Volume:** V = (amount in mB) × 10⁻⁶ m³
- **Mass:** m = V · ρ, from the material's real density
- **Amount of substance:** n = m / M, from the material's real molar mass

Solids inherit the same basis through GT's existing unit ratios — 1 ingot = 144 mB ⇒ 144 mL. Worked example, iron: 144 mL × 7.874 g/cm³ = **1.134 kg** per ingot.

✅ **Verified.** `GTValues.L = 144`, documented in source as "Fluid per Material Unit" (`GTValues.java:80`; now deprecated in favour of `GTRecipeBuilder.INGOTS`, same ratio). The propagation from fluids to solids therefore holds, and the 1.134 kg iron ingot and its ≈ 1.05 MJ melt duty stand as computed.

⚖️ **Rationale.** This is the only basis under which one number means the same thing for a fluid and a solid, and under which volume, mass and moles all follow from one declaration. It also reproduces GTNH's existing pacing: the melt duty for one iron ingot is ≈ 1.05 MJ (772 kJ sensible from 298→1811 K, plus 280 kJ fusion), which at HV takes ≈ 103 s and at EV ≈ 26 s — the same order as stock blast furnace timings. A molar basis (1000 mB ≡ 1 mol) makes the same ingot 8 g and the same melt 0.7 s at HV, which would force artificially inflated durations to stay recognizable — the "artificial grind" PHILOSOPHY §5 forbids.

⚖️ **Accepted cost.** GT's stock chemistry is balanced by volume as though volumes were moles. Under this basis most of it is stoichiometrically wrong, making the correction surface large. This is expected under SCOPE §1 and does not modify §6's replacement rules.

## 5. Why stock EU values disagree, and what follows

Stock GTNH implies mutually inconsistent conversion factors across battery capacity, NEI recipe cost, fuel energy content and water heating, because each was balanced independently for gameplay rather than derived from a shared physical basis.

⚖️ **Consequence.** Under §2 these are not competing candidates for κ. They are defects measured against κ. The M0 audit therefore produces, for each stock anchor:

| Field | Content |
|---|---|
| Anchor | the stock mechanic or recipe |
| Stock value | as read from `5.09.52.594` source |
| Implied κ | J/EU that value asserts |
| Correction factor | implied κ ÷ 1 |
| Physical violation | the specific breach, per SCOPE §6 rule 2 |
| Disposition | correct · accept · out of scope |

That table is simultaneously the audit result and the recorded justification SCOPE §6 rule 2 demands for every replacement.

## 6. Compilation and rounding

SI → EU compilation happens once, at registration. Rules:

1. ⚖️ **Energy demand rounds up. Energy output rounds down.** Never the reverse — this is what makes O2 ("no free energy") hold under quantization rather than merely on average.
2. ⚖️ **κ appears in exactly one place in the codebase.** A single `Units` constant. A test asserts no other conversion literal exists.
3. ⚖️ **Accumulated rounding is checked across recipe chains,** not only per recipe, so a long chain cannot drift into net energy gain.
4. **Overclocking.** ✅ Verified at `OverclockCalculator.java:33-59`: `eutIncreasePerOC = 4`, `durationDecreasePerOC = 2`, so a standard overclock **doubles total energy consumed per tier**. `enablePerfectOC()` sets `durationDecreasePerOC = 4`, conserving total energy. The blast furnace path uses `durationDecreasePerHeatOC = 4` (final, energy-conserving), gated on `HEAT_OVERCLOCK_THRESHOLD = 1800` with a separate `HEAT_DISCOUNT_THRESHOLD = 900`. Consumption is computed as `ceil(recipePower × 4^overclocks)` into a `long`, while duration stays `int`.

   ⚖️ **D8 resolution — keep the mechanic, give it physics.** GregTech's standard overclock already imposes a 2× energy penalty per tier. Under κ = 1 that is a statement about efficiency, and it is *directionally correct*: driving a process faster at higher power dissipates more, because irreversibility grows with rate. LunaTech therefore retains standard overclocking and interprets it as entropy production, with perfect overclocking as the reversible ideal limit. This means the mechanic needs no override — only a physical reading and an honest UI. The exact 2× factor per tier is not itself derived from anything, so its *magnitude* remains an audit item; its *sign* is right.

## 7. Display

- Power in W with SI prefixes; energy in J with SI prefixes; temperature in K.
- Mass in kg, amount in mol, volume in L.
- ❓ Whether EU remains visible at all — hidden entirely, or behind a debug toggle (§9 D12). Recommendation: debug toggle only.
- Values shown to the player are the authored SI values, never back-converted from the compiled EU integer. Back-conversion would surface rounding artefacts as if they were physics.

## 8. Boundary with unreplaced content

LunaTech cannot remove EU as a storage type: GT stores energy as `long` EU and AE2, OpenComputers, the AFSU and other pack mods transact in it. At any interface with content LunaTech does not replace, EU is exchanged at exactly κ with no adjustment. Energy crossing that boundary is conserved by definition; whether the *stock side* then treats it physically is out of scope.

## 9. Verification status

Read from source at tag `5.09.52.594` (vendored at `vendor/GT5-Unofficial`, outside this repo). Class names dropped their `GT_` prefixes in this build — the relevant classes are `GTValues`, `GTRecipe`, `OverclockCalculator`, `HeatingCoilLevel`.

- [x] **Tier values** — `GTValues.java:93-96`. §3 ladder confirmed; MAX corrected to 2 147 483 640 and the 16th guard entry documented.
- [x] **Field widths** — `GTRecipe.java:102`, `mEUt` is `int`. §2 range argument confirmed and strengthened.
- [x] **Overclocking** — `OverclockCalculator.java:33-75`. Standard OC doubles energy per tier; perfect OC conserves it. D8 resolved in §6.
- [x] **Blast furnace heat** — `HeatingCoilLevel.java`. Heat is `1 + 900 × ordinal`, giving 901 / 1801 / 2701 … 13501. Nominally Kelvin, but a synthetic linear ladder, not a physical one. See audit finding A1.
- [x] **Steam conversion** — `GTValues.STEAM_PER_WATER = 160`. See audit finding A2.
- [x] **Battery capacity** — `MetaGeneratedItem01.java:5158`, LV lithium cell = 100 000 EU at V[1]. See audit finding A3.
- [x] **Solid unit ratios** — `GTValues.L = 144`. §4 propagation confirmed.
- [x] **Chemical fuel energy values** — extracted from `MaterialsInit.java` via `MaterialBuilder.setFuel`. Semantics confirmed at `MTEBasicGenerator.java:262-277`: `consumedFluidPerOperation` returns 1, so `FUEL_VALUE` is **EU per millibucket** — joules per mL, kJ per litre — before generator efficiency. See audit finding A6, which is the strongest evidence yet that κ = 1 is correctly scaled: eleven gaseous fuels track real lower heating values with a mean ratio of 2.3 and a spread of only 1.8–2.9.
