# Chao Radio toy — 0.25.75

Chao now walk around the radio to a close front seat, face its speakers, settle
into a grounded listening pose, and return to idle. A finite, quiet note phrase
replaces the old frequent loud pulses. The existing radio model, item,
placement, acquisition and saved identities remain compatible.

The delivery directory contains the [NeoForge 1.21.1 JAR](chao_garden-0.25.75.jar),
[cumulative source changes](chao-garden-0.25.75-source-changes.zip), and
[actual-client gallery and silent recordings](GALLERY.md).

## Model and reference

The existing imported SA2 radio mesh remains: a red faceted cabinet, green
speakers and handle, tuner and controls, with 88 triangles and twelve material
bindings. Its fitted item model, icon and textures are unchanged. Existing
active frames move a small horizontal top-grille detail; this is not a verified
front-speaker membrane pulse. No player manual on/off toggle existed, and none
was added.

Independent reference inspection rendered the live
[Chao Island SA2 race guide](https://chao-island.com/info-center/training-sa2/races.html)
and its original
[Radio prize icon](https://chao-island.com/assets/images/info-center/races-sa2-radio.png),
with TLS verification enabled. That establishes an appearance reference.
The SA1 guide supplied no radio reference, and the Radio Wiki remained behind
a security-verification page. Original listening choreography, timing and music
were not verified. This update's motion and notes are authored Minecraft
adaptations.

## Interaction and audio

Natural play retains the ordinary 160-tick session and care reward/cooldown
policy. Navigation finds a supported, clear seat **0.60 blocks from the radio
center on its front side**, including when starting behind the cabinet. A
radio-specific path classification prevents attempting to climb through it.
The final small seat adjustment is checked for support and clearance. The Chao
faces the speakers; bounded gaze follows their low measured center without
forcing the head through its body.

Entry settles over 20 ticks, listening ends at tick 140, and recovery occupies
the final 20 ticks. Gentle rigid head and arm motion and brief happy expressions
keep the established sitting construction. The original head box, limb shapes,
morphology and scales remain intact. Baby travel retains its existing slow
crawl: the recorded rear route took 1,035 ticks over 4.69370 blocks; the adult
rear route took 77 ticks over 4.88385 blocks. No travel-speed shortcut was added.

The former 16-tick, volume-0.35 harp pulse and session-start shout are replaced
by four vanilla harp notes at ticks **20, 52, 84 and 116**, volume **0.06**,
with a small fixed pitch sequence. A single block-driven emission path avoids
duplicate listener sounds. Transient server admission allows at most one cue
from nearby sources within **12 blocks per 32-tick interval**. Duplicate and
late clock calls do not create catch-up bursts; stopping cancels future cues.
No saved audio station or continuous background loop was introduced.

The actual vanilla event resolves to `note/harp2`: the cached mono Vorbis
sample measures **0.576146 seconds at 48 kHz**. Pitches of 1 or higher shorten
that duration, leaving a gap within the normal-20-TPS 1.6-second interval.
An already started note can finish its remaining tail after cancellation.
Vanilla attenuation still reaches 16 blocks; sources farther than 12 blocks
apart can both be audible to a listener between them. Physical listening is
still needed to judge loudness against garden music. Enable **Master and
Blocks** volume for that check; radio cues use the Blocks sound category.

## Operator previews

```text
/chao testtoy radio once
/chao testtoy radio loop
/chao testtoy radio stop
/chao testtoy stop
/chao testtoy list
```

Permission level 2 is required. Selection uses the nearest eligible Chao within
12 blocks, a real placed radio, and a clear, dry front seat already within
0.4 blocks of the Chao. Previews reuse production pose, clock and cues. Their
small checked seat adjustment remains in the world; position is not rewound.
Temporary AI, goals, orientation and cooldown state are restored, with no
preview care/training rewards or item consumption. Real damage still takes
effect. Stop, interaction, damage, radio removal/rotation, obstruction,
displacement, unloading, logout, range loss and reload terminate playback.
Loops occur only when explicitly requested for testing.

## Verification

| Check | Actual result |
| --- | --- |
| Compilation, core checks, radio rig and assembly | Passed; closed exit 0 in 49.64 seconds |
| Focused native radio tests | **71/71 passed**; 9.717-second test clock |
| Natural navigation | 32 baby/adult × facing × start-side routes and four rear alcoves passed |
| Ownership, cues and cleanup | Single-radio ownership, nearby simultaneous/staggered sources, distant sources, skipped/duplicate clocks, interruption/rejection and operator permissions passed |
| Radio numerical rig | **19,948,836 checks passed** across 419 anatomy/stage/axis cases, 871 episodes, 286,291 frames and 14,464 exit samples |
| Retained animation/timing suites | All eight passed: ball, TV, prone car/crayons, active/quiet toy rigs, sitting and active/quiet timing |
| Broad native suite | **4,461/4,490 passed; 29 failed**, exit 1; every failure matches the frozen .74 name and assertion reason |
| Save and synchronization | Entity NBT and in-process tracked-observer tests passed; independent remote multiplayer remains unverified |
| Final actual client | **22 scenes, 157 original PNGs, 79 resolved sound channels**, closed exit 0 in 430.138 seconds at normal 20 TPS; no skipped scenes |

The final client covered ten natural radio approaches, baby/adult and
Neutral/Hero/Dark morphologies, intermediate development, colored/shiny coats,
a pumpkin hat and Rabbit parts, once/loop/interruptions, two nearby radios,
and TV/car/crayon/ball regressions. Its 79 actual started channels comprise
70 radio, one TV, four car and four ball sounds; crayons stayed silent.
All captured cleanup, state, pose and audio assertions passed. The sound
callbacks establish real resource resolution and channel starts through silent
OpenAL output, not physical listening.

The coordinator personally inspected **all fifteen final review sheets**, four
selected full-resolution originals, and nine decoded rear-route movie frames.
Earlier preflight review included twelve decoded frames, bringing the motion
review to **21 selected frames across three silent recordings**. All three
recordings fully decoded; none was continuously reviewed in full. An independent
review additionally inspected seven final sheets and five originals. The
sampled views show grounded seating, speaker-facing arrival, intact cuboid
anatomy, attached equipment and clean recovery, with no blocking visual issue.
The taller TV naturally occludes its actor from some front views; side/rear
views are supplied. All 157 originals are available, but were not each opened
individually at full resolution.

The rig measured maximum sampled joint movement of **0.08003 model pixels**,
seated-floor error below **0.02501 pixels**, head-floor clearance above
**3.6353 pixels**, and hand-head clearance above **0.4319 pixels**. It checked
72,630 head-child matrices and 1,008 attachment copies, with maximum anchor
error below **0.00000385 pixels**. These are numerical bounds, not every-frame
visual acceptance of every equipment combination.

Two initial native-test private API calls were corrected to exercise public
behavior without widening production APIs. One overlapping reservation fixture
was then corrected to test physical clearance separately from ownership;
production code did not change for that rerun. Preflight review found an
adult-approach camera crop; the excluded capture helper was corrected before
the complete final client run. Diagnostic results remain in the evidence.

The broad suite's **29 inherited failures** comprise sixteen stale quiet-toy
cue expectations, four stale car sound/count assertions, eight mounted-preview
fixtures missing their required placed toys, and one tired Nether companion
extinguish bug. There are no new, changed or missing failure records. Of 176
prior native-test sources, 175 remain byte-identical. The sole intentional
update strengthens the old radio-anchor check to **0.60 ± 0.015 blocks** and
precise heading; other toys' criteria remain unchanged. It passed.

## Delivery and remaining checks

The ordinary-runtime pin binds **645 production classes and 8,254 resources**
to the actual client and production JAR. Test templates, native tests and
capture helpers are excluded from that JAR. Independent preservation checks
matched all 10,420 protected prior source/asset paths and all 358 immutable .74
artifacts, including the completed morphology and ball/TV resources. The source
archive is a cumulative overlay retaining the accepted earlier changes and
their exact deletion guards, rather than a radio-only patch.

The JAR contains **9,152 unique CRC-valid entries**, totals **31,593,111 bytes**,
and has SHA256:

```text
faf72a0b0329627641e37c99eefc64702117d23e337f40d4bcc2c3f08481fafc
```

Use Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21 and the documented
Lionfish dependency; install matching mod versions on server and clients.
Physical sound balance, phone/touch controls, independent remote multiplayer,
an authentic old-world restart, and every equipment/shader combination still
need hands-on verification. The existing Nether companion failure remains
outside this radio update.
