# Guitar interaction 0.25.45

This update refines the existing `chao_guitar` in the full 0.25.44 working source, including the preceding organ update. It targets Minecraft 1.21.1, NeoForge 21.1.219, Java 21 and Lionfish API 3.1. Use the same Chao Garden version and a separately installed Lionfish API 3.1 on clients and server. No duplicate guitar item or additional animation dependency is introduced.

## Playing and geometry

The Chao holds the existing CWE guitar across its torso, with the neck to its left and the string face pointing outward. Its left hand supports the neck and shifts between chord positions; its right hand crosses the soundboard for alternating down/up strokes. Both hands and the rendered instrument use the same measured transform. The renderer fits the visible skin, replacement or Chaos-layer palms, keeps shoulder compensation small and uses the existing forearm extension mechanism where needed. The existing renderer and expression priorities remain in place: a happy playing expression yields to damage, care and other higher-priority states.

The actual item mesh is `models/cwe02516/chao_guitar.obj`, distinct from its lawn/idle representation. Its measured bounds are X 0.0625–0.9375, Y 0.0625–0.4394682, Z 0.2861166–0.7138834. The measured bridge/string origin is approximately `(0.1603256, 0.1487173, 0.4255777)` and the upper neck center is `(0.75974105, 0.31281055, 0.62002745)`. The playing transform maps their long axis toward the Chao's left and the source face normal `(0.0257916, 0.7233867, -0.6899612)` outward.

The strumming target is the measured soundboard/string center `(0.3291302, 0.19492848, 0.48033774)`. It sweeps across the strings, not along the neck. The fretting target follows the source neck around X 0.56–0.64, beginning from `(0.60, 0.26908057, 0.56820762)`. These are guitar-specific contacts, not coordinates copied from the organ or piano. The torso-relative scale is 0.60. Its height was checked against short forms as well as adult forms so the held guitar stays above the floor.

## Timing, sound and gameplay

One phrase uses the existing 160-tick/eight-second performance clock. Lionfish supplies sixteen alternating string crossings at ticks 24, 32, 40, 48, 56, 64, 72, 80, 88, 96, 104, 112, 120, 128, 136 and 144. Each crossing emits one guitar cue. Eight chord pitches span pairs of strums. Hands raise and lower through the shared presence and transition system.

`scripts/generate_guitar_02545.py` synthesizes an original 0.52-second mono four-string chord with staggered plucked-string attacks and a damped release. It uses no sampled game audio. The sound is registered as `chao_garden:toy.guitar`; old generic guitar pulses are suppressed for the refined paths. Interruption stops new cues. An already-emitted short note retains its pitch-dependent release tail.

The same production pose and cue clock serve three ordinary paths:

- **Placed play:** the existing lesson requirement, voluntary selection, stationary approach/contact, reservation, completion reward and cooldown remain authoritative. The one reserved guitar block is hidden while the Chao presents it in its hands, then becomes visible and idle again. No guitar stack enters the Chao's inventory. Breaking the source remains a normal single-item drop.
- **Learned performances:** a Chao that knows guitar uses the shared instrument clock and held pose instead of the old generic floating prop and periodic notes. Existing behavior selection, joining and ending rules remain in place.
- **Classroom practice:** enrolled pupils use the same guitar presentation and attenuated cues. Enrollment, lesson duration, pause handling, learning and teacher rewards stay in the existing school system.

The organ, piano, Analyzer, garden and drive implementations are preserved by this focused update.

## Operator previews

With operator permission level 2, stand within 12 blocks of a dry, idle Chao:

```mcfunction
/chao testtoy list
/chao testtoy guitar once
/chao testtoy guitar loop
/chao testtoy guitar stop
/chao testtoy stop
```

Guitar previews are registered through `HandToyEffects`. Like the other hand-held toy previews, they do not require a placed guitar or a learned guitar lesson. They use the production Lionfish pose, sound and synchronized phrase clock; they do not use a separate demonstration animation.

Previews pause care/age, award no lessons or gameplay rewards, create no inventory/world guitar, and restore the prior NoAI flag. Busy Chao, real held items, school residents/students and other incompatible states are rejected. Explicit stop, interaction, damage, unload/reload, keeper logout, leaving the dimension or moving beyond the existing 64-block keeper range clean up playback. Ordinary petting/damage effects still retain their gameplay meaning. Moving away from or removing a reserved placed guitar cancels the ordinary placed interaction; a free hand-held operator preview has no placed source to reserve.

## Reproduce checks

Use Java 21 and the checked-in Gradle 9.2.1 wrapper. The current build script needs `--no-configuration-cache` because its existing resource task accesses Project during execution. On Windows, use `gradlew.bat` instead of `bash gradlew`.

```sh
bash gradlew --no-configuration-cache assemble coreTest guitarTimingTest guitarRigTest
bash gradlew --no-configuration-cache organTimingTest organRigTest pianoTimingTest pianoRigTest
bash gradlew --no-configuration-cache runGameTestServer -PchaoGameTestNamespaces=chao_guitar45 -PchaoGameTestDir=/tmp/chao-guitar45-fresh
```

Use a fresh GameTest directory for each acceptance run. The guitar-native suite covers baby/adult placed sessions in all four orientations, lesson gating, source restoration and interruption, learned performance lifecycle, once/loop/stop previews, real dispatched sound counts, special forms/replacement hands, and actual enrolled classroom practice, pause and retrieval. Test fixtures and the visual study are excluded from the installable JAR.

For an opt-in disposable native-client study:

```sh
bash gradlew --no-configuration-cache runClient -PguitarVisualStudy=true
# Optional: -PguitarStudyDir=/path/to/disposable-client
```

Its default working directory is `run-guitar045`; this cloud run uses an external disposable directory. The study frames the production renderer and native operator commands from front and side angles. It includes a baby, adult, rabbit-handed baby and Chaos form, plus damage, explicit stop, actual placed completion and placed-source removal. It stores time-labelled screenshots and checks cleanup and hurt-expression priority.

## Recorded validation

- Lionfish timing check: passed 641 quarter-tick samples, all sixteen contact crossings, alternating direction, bounded fret/pitch values and phrase endpoints.
- Strict source-mesh/rig check: passed 302,382 pose samples and 506,796 independently emitted-hand samples across shipped forms, baby/adult sizes, development endpoints, replacement hands and Chaos palms. It checks guitar-side orientation, hand contact, skull/floor clearance and bounded shoulder movement.
- Native-client study: passed eight sessions, with 313 screenshots and fourteen verified hurt-render frames. The study used the actual Minecraft client and renderer, not just mathematical assertions.
- Final native guitar suite: **all 41 tests passed** in a fresh world, including enrolled baby/adult classroom practice, pause, completion and exactly one reward guitar per student. Reload fixtures now use canonical replacement-hand masks and matching isolated AI goals; exact saved-state and happiness assertions remain intact.
- Independent guitar asset checks: **3 passed** (source-model linkage, registered sound/subtitle, decoded waveform/duration/peak/endpoints).
- `assemble coreTest organTimingTest pianoTimingTest`: **passed**.
- Existing organ + focused piano/Analyzer/garden-square native suite: **all 51 tests passed** in a fresh world. Combined with the dedicated guitar suite, all 92 focused native tests passed.
- Independent organ asset regressions: **3 passed**.
- Packaged 0.25.45 JAR passed CRC/version/resource/fixture-exclusion checks. Organ animation/rules/geometry/sound, piano animation and drive-rules implementation bytes match the delivered 0.25.44 JAR.

The cloud client used a null audio device. Cue dispatch/timing can be verified here, but no listening approval is claimed. Two independently connected clients, network-latency behavior and a full modpack/resource-pack combination have not been tested. Existing optional-mod and unrelated broader-suite limitations remain separate from this guitar-focused acceptance run.

## In-game acceptance

1. Let a guitar-trained baby and adult approach a placed guitar from different directions and all four block orientations. Confirm one source is reserved, its held presentation follows the Chao, and the original block returns idle afterward.
2. Watch the fretting and strumming hands from the front, side and above. Check a replacement-handed Chao and a Chaos form, including the first raise and final lowering.
3. Listen at normal Friendly Creatures volume for a guitar-like attack, chord changes, timing, gain, release tails and overlap with nearby performers. This listening check remains necessary.
4. Try `once`, `loop` and `stop`, petting, damage, unload/reload and keeper logout. For placed play, move the Chao away or remove the source. Confirm cleanup and no duplicate guitar items or preview rewards.
5. Repeat with another connected player and the intended modpack. Confirm that both players see and hear the same performance.
