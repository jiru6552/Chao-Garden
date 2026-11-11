# Chao Garden 0.23.0 verification

This report records fresh validation of the recovered Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21 source. Results from the working copy removed during workspace maintenance are not used. The [0.22 report](TESTING-0.22.0.md), old gallery and PDF remain historical model/lifecycle references; their acquisition descriptions are superseded by [0.23 release notes](docs/release-0.23.0.md).

## Mechanics and server integration

The current seven-suite runner passes **1,119,014 assertions**. Repeated runs in the optional profile are not added to this total.

| Suite | Passed checks |
| --- | ---: |
| State, genetics, lifecycle and theme availability | 168,905 |
| Egg distribution policy | 243 |
| Black Market stock and payment | 840,271 |
| Activities | 101,311 |
| Companion rules | 2,091 |
| Fruit | 4,984 |
| Locomotion | 1,209 |

Both actual server profiles pass **134/134 required GameTests**:

| Profile | Test duration | Build duration | Evidence |
| --- | ---: | ---: | --- |
| Base mod | 6.034 seconds | 30 seconds | [Default](docs/validation-0.23/server-default.json) |
| Real Accessories plus 17 identity fixtures | 7.131 seconds | 33 seconds | [Accessories](docs/validation-0.23/server-accessories.json) |

The [core report](docs/validation-0.23/core.json) and server reports bind the full successful logs. Accessories-dependent tests succeed without active execution in the base profile and exercise the real API in the second profile. The pinned dependencies are Accessories `1.1.0-beta.54+1.21.1` and oωo `0.12.15.6-beta.1+1.21`; exact hashes and API-registration confirmation are retained. Identity fixtures exercise mod-list gates without implementing third-party mods.

New actual integration coverage includes arbitrary chest IDs, forced independent egg categories, physical generic chest and minecart fills, witness cleanup after exceptions, rejection of non-chest contexts, all sixteen base colors repeatedly purchased through real merchant menus, exhausted saved base-offer recovery, saved Moon removal after enabling space policy, addon egg/recipe/breeding/robot restrictions, fire and real lava immersion, levitation and controlled Enderman gaze rays, actual Ender Chest transfers/save/load/foreign inventory isolation/stale clicks, and flight grip/landing/gifts/same-UUID release with ordinary physics. Additional admission cases reject a dead keeper and a nonempty inventory cursor without losing its stack.

## Resources and preserved art

Independent resource checks pass **265 acquisition invariants**, **38 themed recipe-absence checks**, and all nineteen theme asset sets. Old egg recipes, recipe advancements, six egg modifiers and six bonus pools are absent. The finalizer is idempotent, keeps other mods' modifier entries and produces canonical locale and loot ordering.

A full clean-copy asset pipeline passes. **All 5,302 source resources are byte-identical** after rebuilding, with no missing, extra or changed files. The live tree stays untouched. All 41 imported authoring inputs and the archived prototype match too. The final resource-tree SHA-256 is `9463f95fed65169deae90d8aa077d3740107e9c0f619c6d0eb2b681aa88a98b5`. [Rebuild evidence](docs/validation-0.23/asset-rebuild.json) binds the complete log and comparison.

[Preserved art](docs/validation-0.23/assets-preserved.json) independently compares **41 original authoring inputs, 4,108 texture PNGs and 751 model JSONs** with the saved 0.22 release: no differences. [Independent review](docs/validation-0.23/independent-review.json) covers alternate acquisition paths, real-slot authorization, saved Moon stock and managed-only physics cleanup. [Space identifier evidence](docs/validation-0.23/egg-distribution-policy.json) records the three exact configurable mappings; it does not claim full space-modpack testing.

## Client, packaging and scope

**12 actual 1280×720 client captures and 12 save callbacks passed**, along with **8,127 rig pose cases**. Every image was individually opened and inspected. Coverage includes finite glide, shiny/Nether/End/Chaos/Omochao/raw-reference materials, a clear prone side view, landing, unequipping and local third/first-person views. An actual simulated **10-block tracking gap** still draws the Chao on the keeper and suppresses its ordinary world draw. The derived keeper-ID index is rebuilt from currently tracked bodies and clears on world change/logout. The [runtime report](docs/validation-0.23/runtime-client.json) and [visual review](docs/validation-0.23/visual-review.json) bind logs, plan and source images. All **36 gallery links** resolve; no graphical browser run is claimed. The current [flight gallery](docs/release-gallery-0.23.0.html) preserves every reviewed frame. The historical 0.22 gallery/PDF retain their original labels.

The final post-client `build coreTest` passes in 12 seconds. The installable JAR is **byte-identical to the exact server-tested JAR**: **12,073,562 bytes**, SHA-256 `e325425aeccd419d297d93caba802228cfa4b8798c1748b6aebbab5457dab2f9`. Its **199 production classes** match current compiled output and **5,297 runtime resources** match source. Test helpers and all five fixture resources are excluded. [Production proof](docs/validation-0.23/production-jar.json) records these checks.

The production packaging gate checks version metadata, compiled class and source-resource parity, complete class coverage, development fixture exclusions, CRCs, unique safe archive paths, every source payload and both galleries' image links. Final artifact sizes and hashes are recorded in the separate verification file. The complete ZIP retains source, editable assets, current proofs and actual captures.

```bash
./gradlew build coreTest runGameTestServer
./gradlew runGameTestServer -PchaoVariantFixtures=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/compatible/dependencies
python3 scripts/validate_distribution_023.py
python3 scripts/validate_variants.py
python3 scripts/generate_all_assets.py
./gradlew runClient -PchaoVisualTest=true -PchaoAudit023Study=true -PchaoAccessoriesTest=true -PchaoAccessoriesDir=/path/to/compatible/dependencies
python3 scripts/package_chao023.py
```

The visual study uses a disposable `ChaoVisual` world and synthetic settings. Full third-party modpacks, external internet multiplayer, every custom dimension and long-duration performance benchmarks remain outside this validation. Already-generated chest contents are not rerolled.
