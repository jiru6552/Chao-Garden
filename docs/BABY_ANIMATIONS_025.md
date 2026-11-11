# Baby Chao reference and runtime animation pass

The supplied first video was reviewed over its full 434.112-second timeline,
then at six frames per second for the visible care actions. Its original file is
`SONIC ADVENTURE 2 2026-11-01 13-35-12.mp4`, 2560 × 1440.
SHA-256: `0fb69bf0caad1ebb3222fa4db255cc0b90d08b343cb59b2b5aa0e1d61f2aced4`.

The imported official cuboids, textures, eight original clips and exact
comparison sampler are preserved. These are new authored gameplay poses and
actual behavior triggers applied to the existing native skeleton.

| Source time | Visible reference | Gameplay implementation |
| --- | --- | --- |
| 00:44–00:56 | Seated newborn beside its shell, looking around | Gentle/natural hatch begins a saved 64-tick settling action. A stopped untrained baby sits; moving babies retain the planted, distance-driven hands-and-knees crawl. |
| 01:15–01:18 | Small head tilts and a question marker over an offered egg | Real held-item THINK/INSPECT phases use restrained progress-driven head turns and the native question marker. |
| 02:40–02:43, within 02:30–03:00 | Seated baby holding fruit at its face; closed content eyes while nibbling | One real 60-tick EAT transaction raises the fruit three times, holds it at the mouth while chewing, then lowers it. The held item, hands, mouth and head share the synced action progress. |
| 03:05–03:08, within 03:03–03:38 | Upright distressed untrained swimmer, alternating scoops/kicks and squeezed eyes | Actual water membership plus the point-based Swim gate selects FLAIL, small server splashes, `> <` eyes and open mouth. Existing escape AI and drowning safety remain active. |
| 04:02–04:05, within 04:00–04:20 | Feet planted, low quiet hands, little head dip; heart enlarges/shrinks | Actual petting stops the navigation path and gives a quiet 60-tick PET pose with a gently pulsing native heart. Repeated clicks during the existing care cooldown grant no extra care and do not restart the gesture. |
| 05:27–05:30 | Suspended baby with low hands, tucked feet and small kicks | Actual held-Chao item rendering gets a dedicated HELD pose, gentle torso swing and alternating soft kicks. Companion back grip remains separate. |
| 06:27–06:29 | Low curled sleeping child, closed eyes, gold spiral above it | Actual saved sleep selects a lower curled REST pose, slow breathing/head settling, closed eyes and a small gold spiral indicator. |

The camera looks away during the shell break; that transition cannot be
reconstructed frame-for-frame from this recording. Likewise the 05:33–05:56
candidate interval mostly shows sitting and looking, without a clean continuous
crawl cycle. This pass preserves the existing planted crawl mechanics instead
of claiming an unseen cycle was directly traced. The low native skeleton and
distance-based alternating contact/recovery remain a Minecraft adaptation.

Eye expressions use skull-local landmarks derived from each gameplay mesh's
native eye panels. The open white eye panels and irises are both hidden for a
closed expression, preventing overlapping eyes. Ordinary blinks last three
ticks every 86–148 ticks, with an individual UUID phase. Robot and Chaos faces
keep their own native appearance. The one white texel used by expression
strokes is reproducibly authored by `scripts/generate_animation_assets025.py`.

## Verification and capture

`ChaoAnimationRules025Test` checks all three meal lifts, mouth-only chewing,
bounded fractional timing, short individually phased blinks, and the actual
low-speed crawl/idle boundary. `BabyAnimationGameTests025` checks a real hatch
reaction through save/reload and expiry, unchanged inherited stats, actual
petting path cancellation, finite cooldown and duplicate-click care protection.
The existing item/wardrobe/lifecycle/companion regression suite still owns
transaction correctness.

The opt-in `Baby025VisualSmoke` study uses real server admission paths, not
forced renderer poses. Enable only this study with
`-PchaoAnimation025Study=true -PchaoVisualTest=true`. It captures twelve
five-tick-spaced client images for each sequence:

`newborn`, `quiet`, `crawl`, `pet`, `question`, `meal`, `water`, `sleep`, `held`.

Files are named `chao025-baby-<sequence>-00.png` through `-11.png`. The
`CHAO_BABY025_TRIGGER` and `CHAO_BABY025_FRAME` log records state the server
trigger tick, client frame tick, elapsed ticks, item phase/progress, behavior,
actual renderer motion and locomotion amount. Gift captures wait for the real
INSPECT/EAT phase; the preceding consideration is not skipped. The carried
sequence uses an actual captured Chao stack in the player's selected hand.
The other studies must be disabled to avoid competing camera control.

Continuous 30-fps proof recording also retains forty ticks after each sequence's
last still, so real hatch/pet/item completion and their interpolation back to a
quiet pose remain visible. The fixture's crawl navigation stops after sixty-two
ticks; its body slows and settles through the ordinary production controller.
`CHAO_BABY025_SEQUENCE_END` binds the clip endpoint to a wall clock time. Trigger,
frame and completion records also include wall clock milliseconds, and each
saved PNG has a SHA-256 callback record.

`Baby025VisualSmokePoseProbe` also checks every current gameplay rig/motion,
fractional pet continuity, quiet planted pet limbs, suspended foot motion,
upright flailing and closed-eye landmark preservation in the live client.
