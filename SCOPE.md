# LunaTech — Scope, Objectives & Reliability

**Status:** Draft 0.1 — pre-alpha. Decisions marked ⚖️ are ratified; items marked ❓ are open and listed in §9.
**Target platform:** GregTech: New Horizons 2.9.0-beta2 (Minecraft 1.7.10 / Forge).
**Companion document:** [PHILOSOPHY.md](PHILOSOPHY.md) states the vision. This document states what we will and will not build, how we will know it is correct, and in what order.

---

## 1. Product definition

⚖️ **LunaTech ships as a standalone Java addon mod for GT5-Unofficial**, installed alongside a stock GTNH 2.9.0-beta2 instance. It is not a fork of GregTech and not a script pack.

⚖️ **LunaTech may replace stock GTNH content where that content contradicts the realism pillars.** This is an accepted departure from a purely additive addon, with the following consequences recorded up front:

| Capability | Mechanism from an addon | Cost |
|---|---|---|
| Register new materials, multiblocks, recipes | GT5U / GTNH public API | None — normal addon work |
| Remove or rewrite stock recipes | Mutate GT `RecipeMap` entries at post-init, after GTNH registration | Load-order sensitivity; must run last |
| Change machine *behaviour* (EU draw, overclock rules, heat handling) | **Mixins via UniMixins** (shipped with GTNH 2.9) | Hard coupling to GT5U internals; breaks on GT5U refactors |
| Change progression gating | Recipe + material tier assignment | Bounded by PHILOSOPHY §5 "GTNH Harmony"; see §6 |

**Accepted cost:** mixin-level coupling to GT5U internals is a permanent maintenance burden and pins us to specific GT5U builds. We take it on knowingly because behavioural realism is unreachable without it.

**Accepted cost:** replacing stock recipes means LunaTech is not save-safe to add or remove mid-playthrough. This must be stated in user-facing docs from the first release.

---

## 2. Objectives

Numbered for reference. Each is stated so that it can be judged done or not done.

**O1 — Grounded energy units.** Publish a documented, defensible mapping between GregTech EU and SI (J, W, K), including the residual error against every anchor GTNH already provides, and a stated policy for the anchors that disagree.

**O2 — Thermodynamic honesty.** No LunaTech process may return more energy than its reactants' enthalpy permits, or require less energy than its thermodynamic minimum duty. Enforced by test, not by review.

**O3 — Stoichiometric closure.** Every LunaTech reaction balances exactly by element and by charge. Side products, catalyst consumption, and thermal dissipation are represented explicitly rather than discarded.

**O4 — Continuous-flow processing.** At least one parameterized reactor multiblock whose output depends on residence time, temperature, pressure, and equilibrium conversion — not on a fixed recipe duration.

**O5 — Sourced parameters.** Every physical constant shipped in LunaTech data carries provenance (reference database, DOI, or named estimation method with its stated uncertainty). No anonymous magic numbers.

**O6 — Registry restraint.** High-throughput compound coverage does not degrade NEI or recipe lookup. Measured, with a declared budget (§7).

**O7 — TPS stability.** LunaTech's per-tick cost stays inside a declared budget under a defined stress scenario (§7).

**O8 — Domain expansion.** Meaningful content in the four claimed domains: organic chemistry, inorganic/materials, nuclear, computation/automation. Sequenced, not simultaneous (§8).

---

## 3. Reliability & validation contract

⚖️ **Reliability is defined as numeric tolerances enforced by an automated validation harness.** A dataset entry or recipe definition that violates its budget fails the build. Provenance (O5) is required alongside, but tolerances are the gate.

### 3.1 Draft error budgets ❓ (numbers to ratify — §9 D3)

| Quantity | Budget | Reference class |
|---|---|---|
| Element balance | **exact** (integer equality) | derived |
| Charge balance | **exact** | derived |
| Mass balance closure | ≤ 1×10⁻⁹ relative | derived |
| ΔfH° (298.15 K) | ± 5 kJ/mol | NIST / DIPPR / literature |
| ΔrG°, equilibrium constants | ± 10 kJ/mol | derived from ΔfH°, S° |
| Cp(T) over stated range | ± 3 % | correlation fit |
| ΔvapH, ΔfusH | ± 3 % | experimental |
| Normal boiling / melting point | ± 2 K | experimental |
| Liquid density | ± 2 % | experimental |
| Isotope mass | ± 1×10⁻⁶ u | AME evaluation |
| Half-life | ± 0.1 % | NUBASE / ENSDF |
| Recipe energy vs thermodynamic minimum | **EU_supplied ≥ duty**, hard inequality | derived |

**Estimated values are permitted** where no experimental data exists, but must be tagged with the method and its published uncertainty, and the budget applied is the method's uncertainty — not the experimental budget above. A value with no source and no method is a build failure.

### 3.2 Harness

- Datasets ship as versioned structured resources in the mod ❓ (format — §9 D4), never as inline Java literals.
- A JUnit suite loads every dataset at build time and asserts §3.1 for every entry.
- A separate invariant suite asserts cross-cutting properties: no energy-positive loop across the full recipe graph, no unreachable recipe, no duplicate registration, no NEI page-count regression.
- CI (GitHub Actions) runs both on every push. Red build = not shippable.
- Tolerance violations are never waived silently. A waiver is a committed, dated entry with a reason.

---

## 4. In scope

- New GT machines and parameterized multiblocks implementing continuous chemical, thermal, and nuclear processes.
- New materials, compounds, isotopes, and fluids with sourced properties.
- Replacement of specific stock GTNH recipes and machine behaviours that contradict the realism pillars, behind explicit configuration where feasible.
- The EU ↔ SI mapping and the audit of existing GTNH values against it.
- Offline-computed property data (QSPR, group contribution, semi-empirical) delivered as validated static resources.
- Automation surface: sensor readouts, process state exposure, OpenComputers integration ❓ (release phasing — §9 D6).

## 5. Out of scope (non-goals)

Stated so we can decline cleanly later.

- **Not** a new modpack, questbook, or worldgen overhaul.
- **Not** a general-purpose chemistry engine or teaching tool. Fidelity serves gameplay.
- **No runtime DFT, MD, or CFD.** All expensive computation happens offline; the game consumes results. Non-negotiable — it is the load-bearing assumption behind O7.
- **No support for non-GTNH environments.** Stock GregTech, other packs, and other Minecraft versions are unsupported.
- **No multiplayer-competitive balance guarantees.**
- **No save migration tooling** for adding/removing LunaTech mid-playthrough (§1 accepted cost).
- **No cosmetic/rendering ambitions** beyond what the machines require to be readable.

---

## 6. GTNH compatibility policy

Because we allow replacement (§1), the boundary must be written down rather than judged case by case:

1. **Progression order is preserved.** A player must not be able to reach a tier earlier than stock GTNH allows via a LunaTech path. Replacement may make a step *harder or more involved*; it may not make it *earlier*.
2. **Replacement requires a recorded justification** naming the specific physical violation in the stock recipe. "Feels wrong" is not a justification; "violates mass balance by 40 %" is.
3. **Replacements are config-gated where mechanically possible.** Where a mixin makes gating impractical, that is documented as unconditional.
4. **Terminal outputs stay compatible.** A LunaTech-replaced chain still yields the item stock GTNH downstream recipes expect.

⚖️ **D5 resolved.** PHILOSOPHY §5 "GTNH Harmony" now rules that harmony constrains *when* a player arrives at a tier, not how much rigour the journey demands. A corrected process may be more involved than the one it replaces; it may never be cheaper or earlier. Rule 1 above is the operative test.

---

## 7. Performance budgets ❓ (to be set by measurement — §9 D7)

Placeholders pending a baseline harness; these are the quantities we will bind, not yet the values.

- **Tick cost:** LunaTech server-tick time under a defined stress scenario (N active multiblocks, all running) — budget TBD after baseline.
- **Recipe count:** net added entries per `RecipeMap`, and NEI page count delta — budget TBD.
- **Memory:** heap delta at post-init versus stock GTNH — budget TBD.
- **Load time:** post-init duration delta — budget TBD.

Each becomes a CI-asserted regression gate once measured.

---

## 8. Milestones

### M0 — EU ↔ SI mapping ⚖️ *(first slice; everything else is scoped against it)*

Deliverable: a document plus a machine-readable constants resource defining the mapping, with residuals.

Method:
1. **Fix the trivial anchors.** 1 tick = 0.05 s, so 1 EU/t = 20 EU/s exactly; a J-per-EU scale factor therefore fixes W directly.
2. **Enumerate GTNH's existing implicit anchors** from the 2.9.0-beta2 source — voltage tiers, EBF heat requirements (GTNH already specifies these in Kelvin, which is a genuine gift), steam-to-EU conversion, and the EU yield of every fuel whose real lower heating value is known.
3. **Derive a candidate J/EU from each fuel independently.** Compare GT EU output against real LHV per unit. These will disagree; the spread is the finding.
4. **Choose one anchor, publish the residuals** for all others rather than hiding them. Document which stock values we intend to correct (§6 rule 2) and which we accept as unfixable.
5. **Investigate the overclocking energy question.** GT5U's standard overclock is understood to be ×4 EU/t for ÷2 time — i.e. total energy is *not* conserved, ~2× cost per tier — while "perfect" overclock conserves it. If confirmed against source, decide whether LunaTech interprets standard OC as a real thermodynamic efficiency loss or overrides it. This decision constrains every later energy claim.

**Verification requirement:** every numeric claim in step 2 and the OC behaviour in step 5 must be read out of the actual GT5U/GTNH 2.9.0-beta2 source in this repo's recorded dependency, not from memory or wiki. Tier values and conversion constants are stated nowhere in this document for exactly that reason.

Exit criteria: mapping published; residual table complete; O1 satisfied; OC policy decided.

### M1 — Toolchain & validation skeleton
Buildable addon against GTNH 2.9.0-beta2 ❓ (toolchain — §9 D1), loading into a live instance, doing nothing but registering itself. Dataset loader, JUnit tolerance harness, and CI green. Performance baseline harness (§7) measured and budgets set.

### M2 — Continuous-flow reactor (O4)
One parameterized reactor. Residence time, T, p, equilibrium conversion, side products, catalyst degradation, thermal dissipation. Proves the hardest architectural claim on a small number of reactions.

### M3 — First chemistry vertical
A real production chain built on M2, with full provenance and enforced budgets.

### M4+ — Domain expansion (O8)
Inorganic/materials, nuclear, automation. Sequenced after M2's architecture is proven, not before.

---

## 9. Open decisions register

| # | Decision | Why it matters | Owner |
|---|---|---|---|
| D1 | Build toolchain and Java target for a GTNH 2.9.0-beta2 addon (GTNHGradle / RetroFuturaGradle; Java 8 bytecode vs Java 17 toolchain) | Blocks M1 entirely | — |
| D2 | Adopt UniMixins as a declared dependency from M1, or defer until a behavioural change actually needs it | Defines how invasive v1 can be | — |
| D3 | Ratify the §3.1 error budgets | They are the reliability contract; drafted, not agreed | — |
| D4 | Dataset format and versioning (JSON / CSV / other), and whether generation code lives in this repo or a sibling | Affects harness design and review ergonomics | — |
| ~~D5~~ | ~~Does "harder than stock" count as invalidating a progression milestone?~~ **Resolved:** harder is permitted, earlier and cheaper are not (§6) | Governs every replacement decision | closed |
| D6 | Is OpenComputers integration in v1 or deferred? | §4 currently claims it without a phase | — |
| D7 | Set §7 performance budgets from the M1 baseline | Turns O6/O7 from aspiration into gates | — |
| D8 | Chosen J-per-EU anchor, and the standard-overclock energy policy | Output of M0; every later energy number depends on it | — |
| D9 | Versioning and release scheme for a pre-alpha pinned to a GTNH beta | Users need to know what pairs with what | — |
