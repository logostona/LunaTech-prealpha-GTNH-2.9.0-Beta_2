# LunaTech — working context

A GregTech 5 Unofficial **addon** for GTNH 2.9.0-beta2 (Minecraft 1.7.10). Not a fork, not a modpack, not a script layer. It grounds GTNH's process design, kinetics and energy dynamics in real physical data.

## Document map

| File | Role |
|---|---|
| `PHILOSOPHY.md` | Intent only. Non-normative. |
| `SCOPE.md` | **Authoritative**: what gets built, milestones, open-decisions register. |
| `UNITS.md` | Normative unit system. Any value contradicting it is a defect. |
| `AUDIT.md` | Stock GTNH defects, findings A1–A7, each with a disposition. |

Where PHILOSOPHY and SCOPE appear to disagree, SCOPE governs.

## Ratified — do not re-litigate

- **1 EU ≡ 1 J exactly.** A definition, not a measurement. No GregTech value can contradict it; one that implies otherwise is a defect belonging in AUDIT.md.
- **1 mB ≡ 1 mL exactly.** Mass and moles derive from this plus real density and molar mass. This single missing quantity — density — explains most of GTNH's cross-phase inconsistency.
- **SI is authored; EU is compiled** at registration. Never the reverse, or the harness has nothing real to check.
- **Demand rounds up, output rounds down**, so quantization cannot manufacture energy.
- **κ lives only in `lunatech.units.Units`.** No conversion literal anywhere else.
- **Replacement of stock GTNH content is permitted**, but requires an AUDIT.md entry naming the *specific physical violation* first. "Feels wrong" is not a justification.
- **Progression may be made harder, never earlier or cheaper.**
- Standard overclocking is retained and read as entropy production (D8).
- Mixins deferred (`usesMixins = false`) until a behavioural change needs them (D2).

## Reference source — grep, don't guess

GT5U source is vendored **outside git** at `D:\LunaTech\vendor\GT5-Unofficial`, shallow-cloned at tag `5.09.52.594` — the exact build GTNH 2.9.0-beta2 ships.

Class names dropped their `GT_` prefixes in this build: `GTValues`, `GTRecipe`, `OverclockCalculator`, `HeatingCoilLevel`, `MTEBasicGenerator`.

This build has **absorbed many former addons into the single GregTech jar**, all therefore in scope: bartworks (+ its cross-mod modules), tectech, GT++ (`miscutils`), goodgenerator, ggfab, kekztech, kubatech, gtnhlanth, galacticgreg, gtnhintergalactic, gtneioreplugin and detravscannermod. The full list is the `modList` in `src/main/resources/mcmod.info` of the vendored source.

Resource files are token-expanded by the convention plugin. The valid tokens are `${modVersion}`, `${minecraftVersion}` and `${modName}` — *not* `${version}` or `${mcversion}`, which fail `:processResources` with an unhelpful "could not copy file" error.

## Formatting (Spotless, enforced by CI)

Import order is `java, javax, net, org, com`, with unmatched packages (`cpw`, `lunatech`) **last**, blank line between groups. Static imports first.

The Eclipse 4.19 profile **wraps any chain of two or more method calls onto separate lines**, regardless of line length. `Datasets.materials().require("iron")` becomes two lines. Prefer writing a single-call accessor or a local variable over accepting the wrap — `Datasets.material("iron")` and `String t = s.trim(); t.isEmpty()` both read better than what the formatter produces.

Live instance for reference: `C:\Users\Computador\AppData\Roaming\PrismLauncher\instances\GTNH 2.9.0`

**Never cite a GregTech value that has not been read from that tree.** Cite as `file:line`.

## Build

Gradle 9.4.0 with GTNH convention plugins, requiring **JDK 17+**.

`java` on PATH is only a Java 8 JRE, but **PrismLauncher has already downloaded full JDKs** and they work for building:

| Path under `%APPDATA%\PrismLauncher\java\` | Version |
|---|---|
| `java-runtime-beta` | JDK 17.0.15 |
| `java-runtime-delta` | JDK 21.0.7 — matches CI, prefer this |
| `java-runtime-epsilon` | JDK 25.0.1 — what the GTNH instance runs on |

Local build: set `JAVA_HOME` to `java-runtime-delta`, then `.\gradlew.bat build`.

A build emits **four** jars into `build/libs/`. Only the unclassified one goes into a real instance:

| Jar | Use |
|---|---|
| `lunatech-<ver>.jar` | **the mod** — multi-release, Java 8 bytecode |
| `lunatech-<ver>-dev.jar` | development classpath only |
| `lunatech-<ver>-sources.jar` | sources |
| `lunatech-<ver>-predowngrade.jar` | pre-downgrade bytecode; **never** ship or install it |

Copying `-predowngrade` into `mods/` puts a second jar declaring the same mod id on the classpath, with class files the 1.7.10 runtime cannot load. Filter on all three classifiers, not just `dev` and `sources`.

The version comes from `git describe`, so **every commit produces a differently-named jar and `build/libs` accumulates them.** A classifier-only filter then matches several versions at once and installs all of them. Use `clean build`, or take only the newest, and clear `mods/lunatech-*.jar` before copying.

Note also that Gradle cannot run inside the agent's sandbox — it needs a loopback connection for its daemon and fails with `Unable to establish loopback connection` even with `--no-daemon`. Local builds have to be run by the user; CI is the agent's build path.

CI also builds on JDK 21 and is the authority when they disagree. CI emits its own errors as annotations because job logs need repo admin auth to read, even on a public repo.

Branches: `main` holds documentation only; `m1-toolchain` holds the scaffold and stays off `main` until it compiles.

GT5U is pinned as `com.github.GTNewHorizons:GT5-Unofficial:5.09.52.594:dev`. Bumping it invalidates AUDIT.md until re-verified.

## Working conventions

- Verify before asserting. A plausible number is not a source-read number.
- Findings that turn out to be *reinterpretations* rather than errors are more valuable than corrections — see A2, where GT's steam constant is within 2 % of saturated steam at 1.2 MPa and needed relabelling, not fixing.
- Open decisions live in SCOPE.md §9. Closing one means editing that table.
