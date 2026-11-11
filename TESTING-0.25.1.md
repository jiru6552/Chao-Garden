# Chao Garden 0.25.1 verification

All seven current validation reports pass.

Tested on Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21.0.13+9 and Gradle 9.2.1. Native images and recordings came from the actual integrated client at 1280×720 under Xvfb/Mesa llvmpipe. The client used the ordinary vanilla three-dimension flat preset plus the mod's three registered realms; the separate GameTest preset was absent from ordinary resources.

| Check | Current result |
|---|---|
| Core mechanics | 1,584,936 checks across 13 suites |
| Fresh default NeoForge server | 193/193 required cases; 56.52 s; Gradle 2m 9s |
| Fresh real Accessories server | 193/193 required cases; 47.53 s; Gradle 1m 38s |
| Independent full asset rebuild | 6,057/6,057 resources byte-identical; 390.859 s |
| Fresh native client | 47 scenes, 67 SHA-bound PNGs, 20 continuous clips |
| Final production artifact | 268 classes, 6,052 runtime resources, exact 6,050-entry assets/data domain |

The real optional profile loaded accessories-neoforge-1.1.0-beta.54+1.21.1.jar; owo-lib-neoforge-0.12.15.6-beta.1+1.21.jar. Accessories API registration was observed. Its 17 identity fixtures exercise identifier-dependent branches; they are not complete third-party mod implementations.

The actual server regressions cover all seven door pairs and their 42 segments, placement/break/cancellation behavior, window collision in four facings, Fortune's body barrier and real interaction ray, the full approach enclosure, wildlife admission, filled pool cells and supported plants, cliff containment, Adventure care actions, side-of-tube return landing, and saved garden/provider/Bell migrations. Migration cases preserve owner/member identities and physical last-seen positions; initial and late-published bodies find distinct collision-free dry land without changing their payloads or moving an ordinary swimmer.

The fresh native run observed all three scenery revisions at zero before genuine initial Hub entry, then revision 26 immediately after entry and before any gate scene. Four portal journeys used ordinary local-player forward input and normal networking. Both outbound Hub journeys kept observed feet at Y80.0, on the physical floor. Normal Minecraft terrain-loading screens and brief destination chunk-mesh streaming remain visible in the recordings. Original Survival mode and the return journal were restored after the study.

Seven native door actions include normal opening/closing and one explicit server-denied click. The study observes actual outgoing action sequences, whole-pair client prediction, incoming vanilla acknowledgements, every-render six-part state coherence, and complete rollback after cancellation. The denied action uses ordinary acknowledgement reconciliation, with no forced client-state writes. Dense native-frame inspection separately checks the rendered meshes.

Fortune's actor-bound probe observed 504 submitted mesh frames across render age 2028.06–2524.44, with both lid scales fixed at 1.0 and 44 genuine service-gesture frames after a real UI interaction. The Doctor's moustache, artwork/columns, badge clearance, counter/curtain surfaces, windows, classroom, storage closet, portals and garden are covered by the native views.

Recordings preserve the actual nominal-30-Hz X11 timestamps; clips retain contiguous source-frame indices and PTS without interpolation or retiming. Their microsecond timebase can make ffprobe's inferred r_frame_rate differ from nominal capture cadence; source metadata, actual average cadence and frame timestamps are checked. Visual review uses individually opened native PNGs, full-timeline frame contacts and specified consecutive/native-resolution windows; it does not imply uninterrupted real-time playback or inspection of every full video frame. All 957 frames of the seven door-action clips are inspected in the final door review.

The full asset CLI ran in an isolated disposable source copy. It reproduced all 6,057 main resources exactly, retained the physically separate two-file GameTest fixture, and preserved all 41 imported authoring inputs and the archived prototype. Five source-only loot/NBT fixtures, all study/test classes and the development flat-preset override are excluded from the production JAR. Both tested server payloads and all observed production client payloads match the final JAR.

Nine retained baby clips are explicitly historical 0.25.0 captures. Their selected 3,792 animation/model/render inputs remain byte-identical. ChaoEntity is not claimed byte-identical: the exact Garden Bell tool-interaction guard is a documented, source-checked exception. The current room/door/staff capture is fresh 0.25.1 footage.

The room geometry is an authored Minecraft adaptation of the supplied SA2/Battle recordings and 25 screenshots. Camera axes and absolute distances are inferred from those views. The low cave-side arrival angle partly hides the waterfall faces; dedicated overview/pool/waterfall views show the full cascade. Hero and Dark gates remain reserved.

| Pure suite | Passed checks |
|---|---:|
| ChaoState | 168,905 |
| Egg distribution policy | 243 |
| Training supplies | 248 |
| Wardrobe choices | 6,644 |
| Baby animation timing | 1,489 |
| Portal geometry | 1,210 |
| Staff face timing0251 | 177,195 |
| Black Market stock | 899,355 |
| Kindergarten buyback | 220,052 |
| Activity rules | 101,311 |
| Companion rules | 2,091 |
| SA2 fruit effects | 4,984 |
| Locomotion rules | 1,209 |

Final artifact: `chao_garden-0.25.1.jar`, 13,327,710 bytes, 6,456 unique ZIP entries; full CRC passes.

SHA-256: `7955e9209e352232355413bddd1fd887a3359dc4a7d0dab1b5d67d2cbdf83a54`

Machine-readable reports and exact source ledgers are in [docs/validation-0.25.1](docs/validation-0.25.1/). The [current offline gallery](docs/release-gallery-0.25.1.html) and [release notes](docs/release-0.25.1.md) accompany the editable source. Historical results remain in TESTING-0.25.0.md and earlier validation directories.
