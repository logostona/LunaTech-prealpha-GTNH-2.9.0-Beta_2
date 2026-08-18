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

### 3.1 Error budgets ⚖️ ratified (D3)

**What a budget compares.** The draft left this implicit and it is the whole difficulty, because a
number LunaTech records *is* taken from the reference, so it cannot disagree with it. A budget
therefore binds one of three things:

1. **Agreement** — where a value is cross-checked against a second independent source, or derived
   two ways (e.g. an enthalpy of fusion quoted per mole and per kilogram), the two must agree within
   the agreement budget.
2. **Admissibility** — an estimated value states its method's uncertainty. That uncertainty must not
   exceed the ceiling, or the value is too poor to ship.
3. **Invariants** — balances and inequalities that must hold identically, with no reference at all.

**The correction to the draft:** it said estimated values are judged by "the method's uncertainty,
not the experimental budget". That is unenforceable — a value inherits whatever uncertainty its
method claims, so any estimate passes. Every quantity now carries a **ceiling** as well, and that
ceiling is what makes estimation admissible rather than unlimited.

| Quantity | Agreement | Max admissible uncertainty | Reference class |
|---|---|---|---|
| Element balance | **exact** (integer equality) | — | derived |
| Charge balance | **exact** | — | derived |
| Mass balance closure | ≤ 1×10⁻⁹ relative | — | derived |
| Molar mass | ± 1×10⁻³ g/mol | ± 1×10⁻² g/mol | IUPAC atomic weights |
| ΔfH° (298.15 K) | ± 5 kJ/mol | ± 15 kJ/mol | NIST / DIPPR / literature |
| ΔrG°, equilibrium constants | ± 10 kJ/mol | ± 25 kJ/mol | derived from ΔfH°, S° |
| Cp over stated range | ± 3 % | ± 10 % | correlation fit |
| ΔvapH, ΔfusH | ± 3 % | ± 10 % | experimental |
| Normal boiling / melting point | ± 2 K | ± 10 K | experimental |
| Liquid density | ± 2 % | ± 5 % | experimental |
| Activation energy | ± 20 kJ/mol | ± 40 kJ/mol | kinetics literature |
| Isotope mass | ± 1×10⁻⁶ u **or the reference's own stated uncertainty, whichever is larger** | same | AME evaluation |
| Half-life | ± 0.1 % **or the reference's own stated uncertainty, whichever is larger** | same | NUBASE / ENSDF |
| Recipe energy vs thermodynamic minimum | **EU_supplied ≥ duty**, hard inequality | — | derived |

**Why the nuclear rows changed.** Flat budgets of ± 0.1 % and ± 1×10⁻⁶ u are attainable for
well-measured nuclides and impossible for exotic ones, whose reference values are themselves
uncertain by tens of percent. A budget tighter than the reference demands precision that does not
exist, so it is expressed relative to the reference's own uncertainty.

**Activation energy is deliberately loose.** Published values for the same reaction routinely differ
by 20–30 kJ/mol depending on catalyst, support and temperature window. A tight budget here would be
false precision, and the honest response is a wide budget with the uncertainty stated.

❌ **Known schema gap.** Density and Cp are strongly temperature-dependent, but `Quantity` has no
temperature field — the conditions live in the `source` string, where no test can read them. Two
values at different temperatures could satisfy every budget and still be inconsistent. Fixing this
needs a schema change, tracked alongside the Cp(T) work in [DATA.md](DATA.md) §5.

**Enforcement status.** Admissibility (rule 2) is enforced now by `DatasetTest` for every shipped
quantity. Agreement (rule 1) and the invariants (rule 3) become enforceable when the data they
compare exists — there are no dual-sourced values, no stoichiometry and no recipes yet. A budget
that nothing checks is a comment, and this section says plainly which ones those currently are.

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

Deliverable: [UNITS.md](UNITS.md) promoted from draft to normative, plus a machine-readable constants resource and the stock-anchor audit table in [AUDIT.md](AUDIT.md).

**Method revised.** The original plan was to *derive* a J-per-EU factor from GTNH's implicit anchors. That plan is void: stock GTNH implies mutually inconsistent factors across battery capacity, NEI recipe cost, fuel energy and water heating, because each was balanced independently for gameplay. There is no single factor to discover. M0 therefore declares the factor and audits stock values against it.

1. ⚖️ **Declare the definition.** 1 EU ≡ 1 J exactly; 1 tick ≡ 0.05 s; 1 mB ≡ 1 mL. See [UNITS.md](UNITS.md) §2–§4 for the range and pacing arguments behind these.
2. **Read the source at `5.09.52.594`** and complete the UNITS.md §9 verification checklist. Nothing is citable before this.
3. **Enumerate every stock anchor** — voltage tiers, blast furnace heat requirements (already in Kelvin), steam conversion, battery capacities, and every fuel whose real lower heating value is known.
4. **Produce the audit table** (UNITS.md §5): stock value, implied κ, correction factor, specific physical violation, disposition. The spread across anchors is expected output, not a problem to resolve. This table doubles as the recorded justification §6 rule 2 requires.
5. **Resolve the overclocking question.** GT5U's standard overclock is understood to be ×4 EU/t for ÷2 time — total energy *not* conserved, roughly 2× cost per tier — while "perfect" overclock conserves it. If confirmed, decide whether LunaTech reads standard OC as a genuine thermodynamic efficiency loss or overrides it. This interacts directly with the rounding rules in UNITS.md §6 and constrains every later energy claim.

**Verification requirement:** every numeric claim in step 2 and the OC behaviour in step 5 must be read out of the actual GT5U/GTNH 2.9.0-beta2 source in this repo's recorded dependency, not from memory or wiki. Tier values and conversion constants are stated nowhere in this document for exactly that reason.

Exit criteria: UNITS.md normative and source-verified; audit table complete with a disposition on every anchor; O1 satisfied; OC policy decided.

### M1 — Toolchain & validation skeleton *(in progress)*

⚖️ **Toolchain settled (D1).** GTNH's own convention plugins, matching GT5U's build exactly: `com.gtnewhorizons.gtnhsettingsconvention` 2.0.24 in settings, `com.gtnewhorizons.gtnhconvention` in the build script, resolving from `nexus.gtnewhorizons.com`. Minecraft 1.7.10, Forge 10.13.4.1614, MCP `stable`/12, Gradle 9.4.0. Modern Java syntax via JVM Downgrader targeting Java 8 bytecode, with GTNHLib supplying stubs — identical to GT5U, so we compile the same way as the mod we extend. GT5U is pinned as `com.github.GTNewHorizons:GT5-Unofficial:5.09.52.594:dev`; bumping it invalidates [AUDIT.md](AUDIT.md) until re-verified.

⚖️ **D2 deferred, deliberately.** `usesMixins = false` until the first behavioural change actually requires it. Enabling it later is a properties change plus a mixin source set. Deferring avoids a UniMixins dependency and mixin-debug build overhead that M1 does not use, and nothing in M1 changes machine behaviour.

**Scaffolded:** build scripts, wrapper, mod entry point, `Units` (the single home of κ per UNITS.md §6 rule 2), and the first harness tests pinning the ratified constants.

✅ **Build verified in CI.** The dev machine has only a Java 8 JRE and Gradle 9.4.0 requires JDK 17+, so builds run in GitHub Actions on JDK 21 rather than locally. Compile, the validation harness, and the full build (including Spotless and Checkstyle) all pass on `m1-toolchain`. This confirms in practice what D1 settled on paper: the GT5U coordinate resolves, LunaTech compiles against it, and JVM Downgrader produces Java 8 bytecode.

The five `UnitsTest` assertions passing means κ, the matter basis, the tier ladder, the no-free-energy rounding invariant, and the iron-melt pacing figure are now machine-enforced rather than prose — §3's harness requirement is live, not aspirational.

**Note on CI diagnostics.** GitHub gates Actions job logs behind repository admin auth even for public repos, so the workflow republishes failures through the public annotations API. Annotations are capped at 10 per step, so the whole diff goes into a single annotation; emitting one per line truncates silently and produces confidently wrong conclusions.

✅ **Dataset loader done (D4).** JSON, loaded from the classpath via Gson, specified in [DATA.md](DATA.md). Every value carries units and provenance because the schema admits no bare numbers, and the harness asserts unit strings per field, rejects unsourced constants, and refuses data verified against a GregTech build other than the pinned one. The iron figures behind the pacing argument now come from the dataset rather than being hardcoded in the test.

✅ **Loads into a live instance.** Built locally on JDK 21 and installed into the GTNH 2.9.0-beta2 instance; the mod appears in the in-game mod list, confirming `required-after:gregtech` resolves against the real pack and not merely against the maven artifact.

Note on JDKs: `java` on PATH is a Java 8 JRE, but PrismLauncher had already downloaded full JDKs 17, 21 and 25 — see [CLAUDE.md](CLAUDE.md) for paths and for which of the four build jars is the shippable one.

**Remaining: the §7 performance baseline.** Deliberately split, because a tick-cost budget is meaningless while the mod ticks nothing:

- *Measurable now:* load-time delta, heap delta at post-init, and NEI page delta — all of which should be indistinguishable from zero for a mod that registers nothing. Establishing the method and confirming zero is worth doing before content exists, since it is the only time the answer is known in advance.
- *Deferred to M2:* the tick-cost budget, which needs running multiblocks to measure.

### M2 — Continuous-flow reactor (O4) *(model done; shell remaining)*

✅ **Process model built and tested.** `Arrhenius` and `ContinuousReactor` compute conversion from temperature and residence time, anchored on a per-reaction operating point, with a declared equilibrium ceiling. This is the part that makes a reactor more than a recipe with a timer, and it is pure logic, so it is fully covered by the harness without a running game.

**Remaining: the in-game shell.** A GregTech multiblock is a substantial piece of Minecraft work in its own right — StructureLib definition, metatile class, GUI, recipe map registration, textures — and none of it can be verified by CI. It is deliberately separated from the physics rather than bundled with it.

**Still unmodelled from the original description:** pressure, side products, catalyst degradation and thermal dissipation. The model is first-order, isothermal plug flow with no volume change, and DATA.md and the class javadoc both say so.
One parameterized reactor. Residence time, T, p, equilibrium conversion, side products, catalyst degradation, thermal dissipation. Proves the hardest architectural claim on a small number of reactions.

### M3 — First chemistry vertical
A real production chain built on M2, with full provenance and enforced budgets.

### M4+ — Domain expansion (O8)
Inorganic/materials, nuclear, automation. Sequenced after M2's architecture is proven, not before.

---

## 9. Open decisions register

| # | Decision | Why it matters | Owner |
|---|---|---|---|
| ~~D1~~ | ~~Build toolchain and Java target~~ **Resolved:** GTNH convention plugins, Gradle 9.4.0 on JDK 17+, JVM Downgrader to Java 8 bytecode (M1) | Blocks M1 entirely | closed |
| ~~D2~~ | ~~Adopt UniMixins from M1 or defer~~ **Resolved:** deferred; `usesMixins = false` until a behavioural change needs it (M1) | Defines how invasive v1 can be | closed |
| ~~D3~~ | ~~Ratify the §3.1 error budgets~~ **Resolved.** Ratified with a correction: every quantity now carries a ceiling on declared uncertainty, because judging an estimate against its own method's uncertainty is circular. Enforced by `DatasetTest` and pinned by `BudgetTest` | They are the reliability contract | closed |
| ~~D4~~ | ~~Dataset format and versioning~~ **Resolved: JSON.** Schema, provenance and versioning rules in [DATA.md](DATA.md). Curated in this repo until a generation pipeline exists, then a sibling repo emits the same schema | Affects harness design and review ergonomics | closed |
| ~~D5~~ | ~~Does "harder than stock" count as invalidating a progression milestone?~~ **Resolved:** harder is permitted, earlier and cheaper are not (§6) | Governs every replacement decision | closed |
| D6 | Is OpenComputers integration in v1 or deferred? | §4 currently claims it without a phase | — |
| D7 | Set §7 performance budgets from the M1 baseline | Turns O6/O7 from aspiration into gates | — |
| D8 | Standard-overclock energy policy: real efficiency loss, or override? | Interacts with UNITS.md §6 rounding; constrains every energy claim | — |
| D9 | Versioning and release scheme for a pre-alpha pinned to a GTNH beta | Users need to know what pairs with what | — |
| D11 | Expose real voltage classes per tier, or power-only? (UNITS.md §3.1) | Option (a) needs a story for tiers above UHV | — |
| ~~D12~~ | ~~Does EU stay visible in the UI?~~ **Resolved: no.** GregTech's text is relabelled to SI. Energy keys done (UNITS.md §7.1); power keys await D2 | Player-facing consequence of the SI-display decision | closed |

**Closed by [UNITS.md](UNITS.md):** the J-per-EU value (declared, not derived — §2), the matter basis (1 mB ≡ 1 mL — §4), and the SI-authoring direction (§1). The former D8 "chosen anchor" question is void: there is no anchor to choose, because stock GTNH is self-inconsistent (§5).
