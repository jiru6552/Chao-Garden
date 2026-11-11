# Release validation - 0.16.0

Target: Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. This release replaces continuous swaying with a quiet baseline and adds movement progression based on earned stat points.

## Rules and server integration

The integrated build passed 83,480 deterministic checks, including 1,205 new movement-rule checks. All 40 server GameTests passed. Four new integration cases cover actual navigation at the four Run tiers, immediate and reloaded point-derived attributes, safe novice water behavior and separate Swim speed, and a real falling body's point-gated glide with a finite budget and no midair restart.

Walking unlocks at 50 Run points, running at 667 and dashing at 1,334. Swimming unlocks at 100 Swim points, with an improved stroke at 667. Garden ledge gliding unlocks at 135 Fly points and lasts at most 60 active gliding ticks per fall. These rules use `ChaoState.points`, independently of displayed levels or progress bars. Existing state serialization is unchanged. A reincarnated child retains 10% of points and may keep an ability if it still meets its requirement.

The newer Chao Island wiki gives modern garden flight as above 134 Fly points; the older Info Center says 100. This release follows the requested wiki's GameCube/HD threshold. See [movement research](docs/research/MOVEMENT_016.md) for sources and the distinction from Dreamcast and Chao World Extended mechanics.

Movement speeds update on the authoritative entity, including when it is busy in a competition or summoned as a companion. Novices receive emergency water safety while flailing. Flight is descending, limited and reset by landing/water; there is no powered hovering. The separate Accessories adult companion perk still requires Fly level 10.

## Animation and geometry

The original eight imported clips remain available through the raw sampler. Ordinary gameplay uses a fixed, lowered-arm baseline plus subtle breathing. A 27.5-tick sway gesture occurs once per 13-20-second cycle with an individual UUID phase and eased edges. Locomotion advances from travelled stride rather than the wall clock. The controller provides crawling, walking, running, dashing, flailing, paddling, improved swimming, gliding, falling, supported held poses and seated/resting care poses. Mode changes blend over eight render-age ticks. Baby stride cadence compensates for vanilla's extra multiplier.

The actual Minecraft pose probe passed 9,884 gameplay samples across all twenty rigs and twelve modes, plus 2,000 raw-source samples. It checked finite values, bounded gesture duty, independent phases, quiet stopped arms/torso/head, lowered crawl posture and an eased crawl-to-walk transition.

A separate cuboid diagnostic sampled eleven ordinary rigs at thirteen crawl phases. Its 572 native and 11,726 fitted-arm/head checks found no contacts deeper than 0.05 model pixels. This diagnostic excludes breathing, head tracking, reactions and transition blends; it does not establish universal clearance. Reproduce it with `art/validate_chao_crawl_clearance.py`. Custom themes, animal parts and Chaos details inherit the sampled semantic anchors.

## Client and previews

The completed development study preserved twenty-three fresh 1280x720 captures with successful screenshot callbacks: seven feature views and two eight-frame motion sequences. Feature views cover the four ground tiers, a close side comparison of crawling/walking, the three water styles, an actually gliding Chao, custom crawl overlays, a held newborn and a Run-49 Analyzer tooltip. The motion sequences preserve the same entity within each sequence, with a quiet/occasional idle group and one Chao physically crawling along a navigation path. Original PNGs and actual source game-tick intervals are retained. GIF playback uses a review frame rate and does not claim real-time timing.

The Analyzer explains current capability and the next point threshold; three guide pages explain learning to move, swim and glide. Held Chao stay upright. Optional mod identity fixtures enable the Create appearance for visual inspection; they do not establish complete third-party modpack compatibility. Development fixtures, pose probes and capture harnesses are excluded from the installable JAR.

All four PDF pages were rendered and visually reviewed for legibility, clipping and whole-frame placement. The offline gallery script passed a syntax check; packaging verifies every current image link, preserved capture hash and production class/resource. The final production comparison also confirms unchanged asset/data resources and saved-state class.

## Reproduce

```bash
./gradlew build runGameTestServer
./gradlew runClient -PchaoVisualTest=true -PchaoMotionStudy=true -PchaoVariantFixtures=true
python3 art/validate_chao_crawl_clearance.py
python3 scripts/package_chao016.py --previews-only
python3 scripts/build_preview_pdf_016.py
python3 scripts/package_chao016.py
```

The opt-in visual study requires the development `ChaoVisual` world. Earlier completed validation is preserved in [VALIDATION_0151.md](docs/VALIDATION_0151.md) and prior versioned records. Package checks compare production classes/resources to the current build and retain the exact 41 asset-only imported inputs while excluding private inspection data, worlds, caches and third-party jars.
