# LunaTech — Philosophy

*This document states intent. [SCOPE.md](SCOPE.md) is authoritative on what gets built, in what order, and how correctness is measured. Where the two appear to disagree, SCOPE.md governs and this document is wrong.*

## 1. What LunaTech Is

LunaTech is a Java addon mod for GregTech 5-Unofficial, targeting GregTech: New Horizons 2.9.0-beta2 on Minecraft 1.7.10. It is not a fork, not a modpack, and not a script layer.

## 2. Core Vision

GregTech New Horizons is widely recognized as one of the most sophisticated and complex modpacks available. While it is celebrated for its deep industrial progression, many of its core mechanics still rely on simplified approximations.

The vision for **LunaTech** is to bridge the gap between gameplay and real-world science by anchoring process design, kinetics, and energy dynamics in empirical data and low-error simulation. Beyond refining current systems, LunaTech aims to significantly expand GTNH's chemical, physical, nuclear, and computational processing lines.

"Low-error" is not a sentiment here. It is a set of numeric tolerances, enforced on every shipped value by an automated harness that fails the build on violation. Those tolerances are defined in [SCOPE.md §3](SCOPE.md#3-reliability--validation-contract); this document does not restate them.

## 3. Guiding Pillars

### I. Data-Driven Chemical & Physical Realism

- **Empirical Foundation:** Process parameters — enthalpy, reaction kinetics, phase transitions, mass balances — are derived from validated experimental data or near-DFT semi-empirical models rather than arbitrary values. Every constant carries its provenance.

- **Continuous Reaction Dynamics:** Move away from rigid, batch-only operations where applicable. Chemical systems should simulate continuous flow, equilibrium conditions, and rate-limiting factors.

- **Stoichiometric Rigor:** Every reaction preserves elemental mass and energy balance, accounting for side products, catalytic degradation, and thermal dissipation.

### II. Grounded Energy Dynamics

- **Unit Standardization:** Resolve the abstraction of GregTech Energy Units by establishing a mathematically sound, documented mapping to real physical units (Joules, Watts, Kelvin) — and publishing the residual error against the anchors GTNH already implies, rather than concealing the anchors that disagree.

- **Thermal & Pressure Integration:** Energy requirements are tied directly to thermodynamic states. Heating curves, endothermic and exothermic thresholds, and pressure requirements dictate machine efficiency and operational safety.

- **No Free Energy:** No process returns more energy than its reactants permit, nor demands less than its thermodynamic minimum duty. This is an invariant, checked by test rather than by judgement.

### III. Scalable System Architecture

- **Modular Processing:** Complex industrial synthesis relies on dynamic, parameter-driven multiblock units rather than thousands of static, single-purpose machines.

- **Intelligent Indexing:** High-throughput compound generation is handled via dynamic data structures and filtering to maintain performance, avoiding registry bloat in recipe browsers like NEI.

- **Algorithmic Extensibility:** Property estimation models (QSPR, group contribution, molecular simulation) generate data offline or via external pipelines, feeding structured parameters into the game engine cleanly. Nothing expensive runs at tick time — no DFT, no MD, no CFD in the game loop. This is the assumption every performance claim rests on.

## 4. Domains of Interest

The long-term territory. Sequencing is decided in [SCOPE.md §8](SCOPE.md#8-milestones), not here; listing a domain is not a commitment to build it next.

- **Organic Chemistry:** Comprehensive hydrocarbon processing, functional group transformations, polymer synthesis, and dynamic catalysis based on molecular properties.

- **Inorganic & Materials Science:** High-purity metal refining, crystal growth kinetics, advanced ceramic synthesis, and structural alloy dynamics.

- **Nuclear Physics & Engineering:** Isotopic separation, realistic decay chains, neutron flux dynamics, and thermodynamic heat-exchange loops.

- **Computation & Industrial Automation:** Advanced process control, sensor integration, data-logging, and deep compatibility with programmatic automation platforms such as OpenComputers.

## 5. Implementation Principles

- **Realism Without Tedium:** Complex science must translate into engaging, meaningful gameplay challenges. Difficulty should stem from system design, resource allocation, and process optimization — not artificial grind.

- **Correction Over Deference:** Where a stock GTNH process contradicts physical reality, LunaTech corrects it rather than working around it. Correction is never taken on taste; it requires a recorded, specific physical violation.

- **GTNH Harmony:** LunaTech builds upon GTNH's established progression curve and respects its milestones. It expands and refines existing tiers (LV through MAX) without creating shortcuts — no LunaTech path reaches a tier earlier than stock GTNH allows. Harmony constrains *when* a player arrives, not *how much rigour* the journey demands: a corrected process may be more involved than the one it replaces. It may never be cheaper.

- **Honest Costs:** Realism has prices — coupling to GregTech internals, and the fact that a pack cannot safely add or remove LunaTech mid-playthrough. These are stated plainly to users, never discovered by them.

- **Performance First:** All calculations, data structures, and multiblock logic are optimized for tick-rate stability and low memory overhead, against declared and measured budgets.

## 6. What LunaTech Is Not

A philosophy that excludes nothing constrains nothing. The binding non-goals — no runtime heavy simulation, no general-purpose chemistry engine, no support outside GTNH, no worldgen or questbook ambitions — are enumerated in [SCOPE.md §5](SCOPE.md#5-out-of-scope-non-goals).
