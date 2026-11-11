# Isolated production-method checks

These checks require **Python 3 and Java 21**, but do not require Gradle, downloaded dependencies, Minecraft, or a running server. Java is selected from `JAVA_HOME/bin/java` when `JAVA_HOME` is set, otherwise from `PATH`.

From the repository's `source` directory:

```sh
python3 -B scripts/tests/check_tatami_isolated.py
python3 -B scripts/tests/check_daycare_isolated.py
python3 -B scripts/tests/check_breeding_isolated.py
```

They can also be launched by absolute script path from any working directory. Each run reads the current production Java methods, combines them with small test doubles, and executes the resulting Java source in an automatically cleaned temporary directory. A failed assertion, Java compilation error, missing runtime, or missing/changed method signature produces a nonzero exit status. No production source, generated game asset, or historical validation report is rewritten.

## Coverage

- **Tatami: 205 checks.** Extracts `ActivityManager.mat` and `KarateMatBlock.getDrops`. Covers independent adjacent assemblies, all 49 clicked tiles, controller ordering, legacy adjacency, malformed/unloaded assemblies, center-derived loot, preserved loot parameters, and orphan/legacy loot behavior.
- **Daycare: 135 checks.** Extracts `KindergartenContent.isBoarded`, `GardenRegistry.managed`, and `ActivityManager.callHome`. Covers the boarding/owner/roster/marker truth table, protected recall, reconstructed student state, release followed by ordinary recall, and existing companion/NPC/activity/construction guards.

The original pre-fix daycare methods fail the isolated boarding-management check; the corrected methods pass. These counts describe the current harnesses, not native-server coverage.

- **Breeding: 63 checks.** Extracts `ChaoEntity.canMate` and `spawnChildFromBreeding`. Covers null creation, rejected and throwing publication, retry, same/reversed/third-parent reentry during creation/publication/XP callbacks, single commit, rejected XP without offspring replay, and ineligible/self breeding. Failed attempts retain love/cooldown, but may advance the inheritance RNG.

## Limits and native regressions

**These are isolated checks, not native GameTests or a full build.** The lightweight API stubs cannot prove compatibility with the actual Minecraft/NeoForge APIs, native loot/explosion behavior, event ordering, dimension transfer, disk persistence, server restart, or real classroom rewards. Source extraction uses balanced braces for these known methods, not a general Java parser; substantial changes to their signatures or syntax may require updating the harness.

The corresponding native regressions are in:

- `src/main/java/com/chaogarden/test/BreedingPublicationGameTests02518.java`, including real join cancellation, callback reentry, cooldown, XP and particle packet assertions.
- `src/main/java/com/chaogarden/test/TatamiAssemblyGameTests02518.java`, including deterministic native explosion finalization with a fixed edge-impact list.
- `src/main/java/com/chaogarden/test/KindergartenServicesGameTests.java`, method `registeredGardenStudentsStayInClassAcrossReloadAndRetrieveWithRewards`.

The daycare native regression includes an in-memory saved-data round trip and projection republication, not a full server restart. Its contest check exercises remote selection rejection, not the separate stale-selection-before-enrollment case.

At the time these portable scripts were added, all three isolated checks passed. The native build and new GameTests remained **unrun** because the Gradle/dependency download was unavailable. Run the project's documented native verification workflow in a configured Java 21/NeoForge environment before claiming a native pass.
