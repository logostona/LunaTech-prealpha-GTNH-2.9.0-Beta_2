# LunaTech — Path & Coordinate Reference

Fast lookup for everything that would otherwise be rediscovered. **Every source line below was read from the vendored tree, not remembered** — but line numbers drift if the GT5U pin changes, so re-verify after any version bump (which also invalidates [AUDIT.md](AUDIT.md)).

Conventions: `repo/` = this repository. `vendor/` = `D:\LunaTech\vendor\GT5-Unofficial`. `inst/` = `C:\Users\Computador\AppData\Roaming\PrismLauncher\instances\GTNH 2.9.0`.

---

## 1. Machine locations

| What | Path |
|---|---|
| This repository | `D:\LunaTech\repo` |
| GT5U source, tag `5.09.52.594` | `D:\LunaTech\vendor\GT5-Unofficial` (outside git; shallow clone) |
| Live GTNH instance | `C:\Users\Computador\AppData\Roaming\PrismLauncher\instances\GTNH 2.9.0` |
| Instance mods | `inst/.minecraft/mods/` |
| Instance GregTech config | `inst/.minecraft/config/GregTech/` (thin — 150 lines, no energy or temperature keys) |
| Game log | `inst/.minecraft/logs/latest.log` |
| Saves | `inst/.minecraft/saves/` |
| PrismLauncher binary | `%LOCALAPPDATA%\Programs\PrismLauncher\prismlauncher.exe` |
| JDK 17.0.15 | `%APPDATA%\PrismLauncher\java\java-runtime-beta` |
| **JDK 21.0.7 — use this, matches CI** | `%APPDATA%\PrismLauncher\java\java-runtime-delta` |
| JDK 25.0.1 — what the instance runs on | `%APPDATA%\PrismLauncher\java\java-runtime-epsilon` |
| GitHub repo | `https://github.com/logostona/LunaTech-prealpha-GTNH-2.9.0-Beta_2` |

## 2. This repository

| Path | Holds |
|---|---|
| `CLAUDE.md` | Session context: ratified decisions, traps, formatting rules |
| `PHILOSOPHY.md` | Intent only, non-normative |
| `SCOPE.md` | **Authoritative** — objectives, milestones, §9 open-decisions register |
| `UNITS.md` | Normative unit system; §7.1 is the display-conversion map |
| `AUDIT.md` | Stock GTNH defects A1–A8 with dispositions |
| `DATA.md` | Dataset schema, provenance rules, versioning |
| `REFERENCE.md` | This file |
| `src/main/java/lunatech/units/Units.java` | **Sole home of κ**; rounding rules |
| `src/main/java/lunatech/units/SiFormat.java` | SI prefix formatting, locale pinned to ROOT |
| `src/main/java/lunatech/data/` | `Datasets`, `MaterialDataset`, `Material`, `Quantity` |
| `src/main/java/lunatech/mixins/` | Five mixins — see section 5 |
| `src/main/resources/lunatech/data/materials.json` | Property dataset (iron, water) |
| `src/main/resources/mixins.lunatech.json` | **Hand-written** mixin config; the convention does not generate it |
| `src/main/resources/assets/gregtech/lang/en_US.lang` | 18 display overrides in GregTech's resource domain |
| `src/main/resources/mcmod.info` | Uses `${modVersion}` / `${minecraftVersion}`, never `${version}` |
| `.github/workflows/build.yml` | CI; republishes failures as public annotations |
| `gradle.properties` | `usesMixins = true`, `mixinsPackage = mixins` |
| `dependencies.gradle` | GT5U pin `com.github.GTNewHorizons:GT5-Unofficial:5.09.52.594:dev` |

## 3. GT5U source coordinates — verified at `5.09.52.594`

All paths relative to `vendor/src/main/java/`.

### Units and energy

| Fact | Location |
|---|---|
| Voltage tier array `V[]`; MAX is `Integer.MAX_VALUE - 7` | `gregtech/api/enums/GTValues.java:93` |
| `L = 144` — fluid per material unit, so 1 ingot = 144 mB | `gregtech/api/enums/GTValues.java:80` |
| `STEAM_PER_WATER = 160` | `gregtech/api/enums/GTValues.java:427` |
| `mEUt` is **`int`**, bounding recipe power at 2.147e9 | `gregtech/api/util/GTRecipe.java:102` |
| Coil heat = `1 + 900 x ordinal` | `gregtech/api/enums/HeatingCoilLevel.java:32` |

### Overclocking

| Fact | Location |
|---|---|
| `eutIncreasePerOC = 4` | `gregtech/api/util/OverclockCalculator.java:33` |
| `durationDecreasePerOC = 2` — **energy doubles per tier** | `OverclockCalculator.java:35` |
| `enablePerfectOC()` sets divisor 4 — energy conserved | `OverclockCalculator.java:125` |
| `durationDecreasePerHeatOC = 4` | `OverclockCalculator.java:57` |
| `HEAT_DISCOUNT_THRESHOLD = 900`, `HEAT_OVERCLOCK_THRESHOLD = 1800` | `OverclockCalculator.java:74-75` |

### Fuels, generators, steam

| Fact | Location |
|---|---|
| `consumedFluidPerOperation` returns **1**, so `FUEL_VALUE` is EU per mB | `gregtech/api/metatileentity/implementations/MTEBasicGenerator.java:262` |
| Fuel energy = `mSpecialValue x efficiency x consumed / 100` | `MTEBasicGenerator.java:277` |
| 74 `setFuel(FuelType, value)` calls — the fuel table | `gregtech/loaders/materials/MaterialsInit.java` |
| `FUEL_VALUE` and `COIL_HEAT` metadata keys | `gregtech/api/util/GTRecipeConstants.java:81,86` |
| Combustion generator efficiencies 95/90/85 | `gregtech/loaders/preload/LoaderMetaTileEntities.java:9880` |
| Gas turbine efficiencies 95/90/85/60/50 | `LoaderMetaTileEntities.java:9904` |
| Steam turbine "2 L steam to 1 EU", efficiency `6 + tier` | `gregtech/common/tileentities/generators/MTESteamTurbine.java:64-97` |
| Boiler water consumption via `STEAM_PER_WATER` | `gregtech/common/tileentities/boilers/MTEBoiler.java:219-221` |
| Large boiler water/EU relation | `gregtech/common/tileentities/machines/multi/MTELargeBoiler.java:391-393` |
| LV lithium battery = 100 000 EU | `gregtech/common/items/MetaGeneratedItem01.java:5158` |

### Display surfaces — all four

| Surface | Location |
|---|---|
| GregTech lang file, 239 EU mentions | `vendor/src/main/resources/assets/gregtech/lang/en_US.lang` |
| Tooltips: `euText:168`, `euRateText:175`, `euCapacityText:182`, `voltageText:189` | `gregtech/api/util/tooltip/TooltipHelper.java` |
| NEI: `getEUtDisplay:82`, `getVoltageString:94`, `computeVoltageForEURate:111` | `gregtech/api/objects/overclockdescriber/EUNoOverclockDescriber.java` |
| Waila, battery buffer | `implementations/MTEBasicBatteryBuffer.java:297` |
| Waila, single machine | `implementations/MTEBasicMachine.java:1173` |
| Waila, multiblock | `implementations/MTEMultiBlockBase.java:2485` |
| GregTech's own mixin configs, proof they are hand-written | `vendor/src/main/resources/mixins.gregtech{,.early,.late}.json` |

## 4. External coordinates

| Fact | Value |
|---|---|
| GTNH maven | `https://nexus.gtnewhorizons.com/repository/public/` |
| GT5U artifact | `com.github.GTNewHorizons:GT5-Unofficial` |
| Spotless config source | `GTNewHorizons/ExampleMod1.7.10` tag `0.2.2`, `gtnhShared/spotless.importorder` and `spotless.gradle` |
| gtnhlib formatter signature | `formatNumber(java.lang.Number)` — **not** `(long)`; longs autobox |

## 5. LunaTech mixins — target and ordinal map

Ordinals are position-sensitive; each is bounded to calls that really are powers.

| Mixin | Target | Converted | Deliberately skipped |
|---|---|---|---|
| `MixinTooltipHelper` | `TooltipHelper` | `euCapacityText`, `euRateText` | — |
| `MixinEUNoOverclockDescriber` | `EUNoOverclockDescriber` | `getEUtDisplay`, `getVoltageString` | — |
| `MixinBatteryBufferWaila` | `MTEBasicBatteryBuffer` | `formatNumber` ordinals 2-3 | 0-1: stored and max **energy** |
| `MixinBasicMachineWaila` | `MTEBasicMachine` | ordinals 0-1 | 2-3: `mEUt x 40`, a **steam flow in L/s** |
| `MixinMultiBlockWaila` | `MTEMultiBlockBase` | ordinals 0-3 | 4+: item counts, fluid counts, avg tick time |

## 6. Commands

Build, install and launch. `clean` matters: `build/libs` accumulates one jar per commit, and a classifier-only filter would copy several versions at once.

```powershell
Remove-Item "C:\Users\Computador\AppData\Roaming\PrismLauncher\instances\GTNH 2.9.0\.minecraft\mods\lunatech-*.jar" -Force -ErrorAction SilentlyContinue; $env:JAVA_HOME = "C:\Users\Computador\AppData\Roaming\PrismLauncher\java\java-runtime-delta"; cd D:\LunaTech\repo; .\gradlew.bat clean build; Get-ChildItem D:\LunaTech\repo\build\libs\*.jar | Where-Object { $_.Name -notmatch '-(dev|sources|predowngrade)\.jar$' } | Copy-Item -Destination "C:\Users\Computador\AppData\Roaming\PrismLauncher\instances\GTNH 2.9.0\.minecraft\mods\"
```

```powershell
& "$env:LOCALAPPDATA\Programs\PrismLauncher\prismlauncher.exe" --launch "GTNH 2.9.0"
```

```powershell
Select-String -Path "C:\Users\Computador\AppData\Roaming\PrismLauncher\instances\GTNH 2.9.0\.minecraft\logs\latest.log" -Pattern "lunatech|mixin"
```

CI status, then failure text. Job logs need repository admin auth; annotations are public, which is why the workflow republishes failures through them.

```bash
R=https://api.github.com/repos/logostona/LunaTech-prealpha-GTNH-2.9.0-Beta_2
curl -s "$R/actions/runs?per_page=1" | grep -E '"(id|status|conclusion)"' | head -3
```

```bash
R=https://api.github.com/repos/logostona/LunaTech-prealpha-GTNH-2.9.0-Beta_2
SHA=$(git -C /d/LunaTech/repo rev-parse HEAD)
ID=$(curl -s "$R/commits/$SHA/check-runs" | grep -m1 '"id"' | sed 's/[^0-9]//g')
curl -s "$R/check-runs/$ID/annotations"
```

Verify a method descriptor before writing a mixin against it:

```bash
"$APPDATA/PrismLauncher/java/java-runtime-delta/bin/javap.exe" -cp "<jar>" <fully.qualified.Class>
```

## 7. Traps, by symptom

| Symptom | Cause | Fix |
|---|---|---|
| Game will not start: resource `mixins.lunatech.json` invalid or could not be read | The manifest names a config the convention does not generate | Hand-write `src/main/resources/mixins.lunatech.json` |
| `:processResources` fails, "could not copy file `mcmod.info`" | Wrong token names | `${modVersion}`, `${minecraftVersion}`, `${modName}` |
| Spotless fails locally while **CI is green and `git diff` is empty** | CRLF in the working copy; `.gitattributes` normalises on commit | Normalise the working copy to LF |
| Spotless expands a one-line statement into four | Eclipse profile splits any chain of 2+ method calls | Use a local variable or a single-call accessor |
| Mixin compiles but never applies | Wrong descriptor, or `remap` not `false` on a mod target | Confirm with `javap` first |
| Two mods claiming id `lunatech` | `-predowngrade` installed, or several versions copied | Filter all three classifiers and `clean build` |
| Agent cannot run Gradle at all | Sandbox blocks the daemon's loopback connection | The user runs local builds; CI is the agent's build path |
| A GregTech number looks wrong | It may be unlabelled rather than incorrect | Check AUDIT.md A2 before "correcting" anything |
