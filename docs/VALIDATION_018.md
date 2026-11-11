# Release validation - 0.18.0

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This release changes source comparison rendering, model/UV handling, inherited coats, shiny material projection, merchant attachments, item decisions, placed toys and food content.

## Checks completed during implementation

The shared Java build and deterministic rules passed **197,095 checks**: 70,466 state, 123,010 Black Market stock, 1,099 activity, 1,260 companion, 55 fruit effects and 1,205 locomotion checks. The default dedicated GameTest server passed **58/58 required tests**. A separate dedicated run with actual Accessories **1.1.0-beta.54+1.21.1** and oωo **0.12.15.6-beta.1+1.21** also passed **58/58 required tests**. Both runs completed successfully on Java 21 and NeoForge 21.1.219. These are dedicated-server integration checks, not a claim of complete compatibility with every optional mod or modpack.

The added server cases exercise delayed care commits, acceptance/rejection and saved item ownership, hat gifts, placed toy use/interruption, and comparison appearance persistence alongside the existing garden, activity and companion checks. Development fixture and third-party dependency JARs are excluded from distribution. [Runtime dependency inputs](docs/validation-0.18/accessories-runtime-inputs.json) record the tested dependency hashes.

The full asset pipeline completed successfully and reproduced all **5,727 final source resources byte for byte**, including the fruit, toy and heritage finalizers. The two obsolete mushroom seed recipes remained absent. See [pipeline evidence](docs/validation-0.18/full-asset-pipeline.json).

The surface diagnostic verifies continuity across the actual egg shell and twenty supplied meshes. It also checks that the spectacle pieces and mask pieces form connected solids. This diagnostic is bounded to its sampled geometry and views; it does not substitute for the final client review. See [surface evidence](docs/validation-0.18/surfaces-and-accessory-joins.json).

## Actual client and preview review

The final client study completed successfully with **665 whole-frame screenshots and all 665 asynchronous save callbacks**. It includes 45 feature views, the twenty supplied reference rigs and six motion sequences. Client assertions passed 16,795 gameplay-pose checks and 2,000 raw-source pose checks. The completed log contains no Chao runtime or shader-loading failure. [Client evidence](docs/validation-0.18/runtime-client.json) and the [capture manifest](docs/release-capture-manifest-0.18.0.json) preserve the completed run's log hash, frame hashes and source game ticks.

Front, side and rear merchant views show connected lens frames, temples, cheek cloth and rear mask straps. Shiny Chao and both sides of the egg shell were reviewed with the continuous material; the End egg retains its animated portal and no white pedestal. The gallery also records source comparisons, limb/wing orientation, plain and two-tone coats, Moon and Chaos heritage, placed toys, the 24-food collection, item-decision phases and the journal. These views establish the inspected scenes, not every possible animation, shader pack or modpack.

The six GIF reviews use actual captures spaced **two game ticks apart**, with **100 ms per frame** at nominal 20 ticks per second. Walk/pet/accept/reject/toy/shine use 32/80/160/100/200/48 source frames and last 3.2/8/16/10/20/4.8 seconds, including the final 100 ms hold. Whole frames are fitted within 640x360 without cropping or stretching. Identical adjacent GIF frames may coalesce while their total duration is retained. Every original PNG remains available. No generated in-between frames are added; these are game-clock reviews, not wall-clock recordings.

The metadata-selected **eight-page PDF** was rendered with Poppler and every page visually inspected for readable text, complete frames, clipped content and overlap. Its source provenance binds the PDF to this exact capture study. See [PDF source provenance](docs/pdf-source-manifest-0.18.0.json) and [render review](docs/validation-0.18/pdf-review.json).

## Packaging protections

The production JAR passed exact current compiled-class/resource parity and ZIP CRC checks: **12,867,188 bytes**, SHA-256 `3b653326763184a237aef21bd9d9809cf8d72eefdf2a13619010e0a9860a5d22`. Fixture mods, test structures and visual capture classes are excluded. The release packager rechecks this parity and retains the exact 41-file official asset-only input allowlist and all twenty inherited-coat folders, including the legitimate nested `official/coats/run` art directory. Nineteen active themes remain available, while the 29 retired theme IDs use ordinary-egg fallback models without reintroduced theme art/recipes.

The archive checks capture hashes, current PDF source provenance, offline image links, source snapshots, private-input exclusions and CRCs. There is no 75 MiB limit: the user explicitly prioritized complete working content and thorough review over file size. The eight established editable projects remain, with any new editable projects retained as well.

## Reproduce

Before the opt-in capture command, create a blank disposable development world named **ChaoVisual**, then close the client. The source archive excludes game runtime worlds; the capture harness constructs its temporary study scenes in this world. Do not use a valued survival world for development captures.

```bash
./gradlew build runGameTestServer
./gradlew runClient -PchaoVisualTest=true -PchaoExperienceStudy=true -PchaoVariantFixtures=true
python3 art/validate_surface_018.py
python3 scripts/package_chao018.py --previews-only
python3 scripts/build_preview_pdf_018.py
python3 scripts/package_chao018.py
```

The opt-in capture study requires the development ChaoVisual world and `docs/release-scenes-0.18.0.json`. Python asset diagnostics use Pillow and NumPy. Previous completed evidence is preserved in [VALIDATION_017.md](docs/VALIDATION_017.md).
