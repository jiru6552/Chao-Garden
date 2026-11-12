# Baby Run revision and Analyzer alignment — 0.25.53

This update corrects the baby Run target from 0.25.52 and adds a Creative alignment control to the existing Chao Analyzer. The intended prototype remains a **Hero-aligned Run baby**; Run anatomy and alignment are separate influences. Adult morphology is preserved for evaluation in a later update. Use matching 0.25.53 client/server JARs with Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1.

## Baby shape and colors

The revised baby has a tapered forehead and five closed, swept-back pointed quills. At maximum Run, the head widens from approximately 7 to 10.4 model pixels and extends from 6 to 10.8 pixels in depth. The eye plane stays registered at the front while the silhouette changes most strongly from the side and back. This is a Minecraft interpretation of the supplied pointed Run reference rather than an unchanged square head with a stronger deformation multiplier.

The new baby targets are authored analytically from the canonical vertices, retaining their index correspondence, face winding and original limb UVs. Equal vertex counts do not make unrelated donor cuboids interchangeable. Hands and feet are compact closed solids, with fixed sole planes; the old narrow decorative strokes and baby stripe substitution are removed from this target. The head, torso and limbs blend together through the same progress value. Hidden quills are excluded from the bounds used to fit closed hats, preventing invisible geometry from inflating the hat.

Run and Hero use independent baby target weights. Neutral Run takes the green/lime body and darker quill accents from the supplied photograph. Hero Run uses the white, blue and violet Hero family instead of inheriting the Neutral reference's identity. Dark alignment retains the existing baby Dark palette while allowing the Run silhouette. Alignment now interpolates across its signed range, so dragging through Neutral does not jump directly between the end colors. Natural two-tone markings are distinct from inherited named colors, monocolor, shine and jewel identity.

[Reference notes](BABY_RUN_REFERENCES_02553.md) identify the small photograph as a Neutral Run first form, distinguish it from the advanced blue Neutral Run/Run photograph, and describe the authored palette approximations. A matching public-source copy of that photograph is included. The user's Minecraft comparison was reviewed inline, but its original file bytes were unavailable; it is **not** falsely represented as an included or reconstructed PNG.

The 0.25.52 adult model, schema, source assets and adult material method are preserved. A regression fingerprint compares emitted vertices, UVs, normals and joint matrices across 150 adult profiles against the delivered 0.25.52 output. This revision does not redesign the adult endpoint or change its training/maturity rates.

## Development rules and saves

The existing baby Run target remains the provisional prototype rate from 0.25.52, not a verified original-game threshold:

```text
clamp((Run total - Power total) / 480, 0, 1)
training total = bounded level * 100 + current progress
```

That is twenty net ordinary 24-progress Run drive units from zero; 5, 10 and 15 net units correspond to 25%, 50% and 75%. Power opposes Run. Current-value decreases and Creative stat edits can reverse this target without reducing biological age. Hero anatomy reaches its endpoint at alignment +0.5, while alignment itself can range from −1 to +1. An alignment edit does not alter training, age, care, genes or first-evolution identity. The client retains the existing 20-tick interpolation for larger changes.

The four-stat first-evolution selector, recency ties, simultaneous combined-drive policy, biological clock and locked adult identity from [0.25.52](https://github.com/jiru6552/Chao-Garden/blob/codex/download-0.25.53/docs/HERO_RUN_02552.md) remain unchanged. No persistent fields or save-format version are added. The new appearance is derived from the existing saved state, so older ordinary babies acquire the corrected presentation without rewriting their identity. Existing adults keep their locked alignment. Eleven actual older-save records are covered by native round-trip regressions; no user world was modified for testing.

## Analyzer control

Open **Mind → Profile**. The bottom-right bar shows Dark, Neutral and Hero, including the Neutral center and the ±0.5 identity thresholds. The current value is visible in Survival too. It uses the free lower-right area below the existing profile traits, with no information or navigation hidden behind it.

In Creative, hold an Analyzer and drag the bar for an available ordinary living baby. Drag samples are throttled, with the final release submitted as well. The thumb previews the pending drag; the Chao state and appearance update from the synchronized server result, not a local authoritative edit. The bounded integer payload represents −1.000 through +1.000.

The server checks the player's **actual Creative game mode**, non-spectator status, a real Analyzer in either hand, valid range/UUID, a living editable baby in the current dimension, the existing care permission and eight-block distance, and absence of a cocoon or busy activity. Survival clients cannot gain access by forging the request or setting a local Creative/instabuild flag. Existing Creative administrative ownership override is retained, as for other Analyzer edits. Accepted edits synchronize the entity and companion-item representation and persist through the normal encoded state/NBT path.

**Adults are read-only, including in Creative.** The bar displays their locked evolution alignment and says `LOCKED`; the server rejects edits toward either side, including Hero-to-Neutral. It does not silently change a hidden alignment mirror or let a Hero adult become Dark. Dead and immortal forms are also unavailable for editing. Ordinary food/care alignment behavior is unchanged.

## Actual client review

A single completed Minecraft client cohort covered **26 scenes and 581 saved original PNGs**, using real Accessories/owo for the equipped scenes. The delivered capture acceptance manifest binds every PNG to its literal native/render completion and checksum.

- Neutral and Hero babies at 0%, 25%, 50%, 75% and 100% Run were captured from matched front, side and back angles, with fixed age/alignment within each series. A separate equal-depth normal/max pair was captured in all three views. Back captures use a matched rear three-quarter camera, slightly elevated to expose the quill depth.
- The actual Analyzer was opened and dragged through Dark, Neutral and Hero. The same baby was subsequently captured in the world. The final actual UI drag produced alignment **+0.786**, synchronized successfully and survived an exact save/reload check; it was not a direct fixture assignment presented as a mouse test.
- Survival read-only interaction and a forged request left alignment unchanged. A player in Creative interacting with a Hero adult also remained locked after a forged edit; both saved and reloaded unchanged.
- Training increases, decreases and expressions were captured while walking. The three training scenes recorded 392, 394 and 404 moving frames, respectively. The movie labels accelerated fixture training; playback uses actual normal-time client frames.
- Rabbit parts with a skull hat, phoenix wings with glasses, red-shiny and ruby-jewel coats, a two-hand broom, a raised picture book, and actual equipped ground/flight poses were reviewed. Ground carry preserves the existing head-perch pose; flight uses the existing back attachment. No new carry layout was introduced. Broom and book each passed 378 measured contact checks, with maximum errors below 0.000001 model units.

Representative actual views were visually inspected for the swept head, closed limbs, eyes, expressions and attachment continuity. No blocking defect was found in that review. This does not claim inspection of every saved frame, every accessory, every toy or every third-party model. The galleries include all requested quarter stages; original full-resolution frames are archived separately from resized navigation sheets. A 92.600-second silent motion recording is supplied separately: 1,852 original-time frames at 20 fps, with captions below the unscaled client image and frame/timing provenance. It includes the actual alignment drag, training gains and decreases, expressions, toy grips and both equipped poses.

## Verification

| Check | Observed result |
| --- | --- |
| Java 21 compilation and core suite | Passed; 171,316 ChaoState checks and 785 new alignment checks, plus the existing mechanics suites. |
| Alignment bar input/layout math | 1,835 checks passed. |
| Baby mesh, interpolation and attachment rig | 12,541,764 checks passed over 4,210 emitted-geometry/motion frames and 8,020 compact closed limb solids. Maximum small-progress vertex step: 0.02703608 model pixels. |
| Delivered adult preservation | Exact 0.25.52 fingerprint retained: `da472d55f399960b560f03b95b6df96f3de76b26bd6af78f01c60a0492dc8782`. |
| Native dedicated server | All **486 required tests passed**: 61 new baby/alignment cases plus the previous 425. Real Accessories/owo and Lootr were installed. |
| Actual client | All 26 scenes passed; 581/581 original PNGs saved, including eight actual Analyzer screens. |
| Final broad build | `build --continue` completed in 10m25s; production JAR assembled. Only triangle/trumpet transition tasks failed, with byte-identical 0.25.52 results; all other executed checks passed. |

Native coverage includes Creative authority, Survival and spoofed requests, missing Analyzer, spectator, eight-block boundary, stale/wrong targets, malformed/truncated packets, busy/cocoon/dead/immortal cases, administrative ownership behavior, all adult alignments, synchronization, NBT/carrier persistence, independent Run/alignment and older saves. These are actual server API tests with simulated players; they are not two independent networked clients.

The final current broad build retains exactly the two instrument-transition failures from the delivered 0.25.52 baseline: triangle has 25 failures in 267,696 samples (minimum −0.54432297 pixels) and trumpet has 37 in 267,696 (minimum −0.58569264 pixels). Their assertions remain intact, and both current result files match the prior SHA-256 values byte for byte. The standalone journal suite also remains 8/9 passing with its single pre-existing `Your gardens` paragraph-format assertion. An isolated run against delivered 0.25.52 sources reproduced the identical assertion; the guide and test bytes are unchanged. Current comparison evidence is included with the downloads. No new failing check was observed; the broad build is not fully green.

## Recheck in your game

1. Compare the normal and maximum-Run baby's front, side and back with the reference, then inspect 25/50/75% stages. Check the hand/foot solids while walking and turning, not only while standing still.
2. In Creative, open Mind → Profile and drag alignment through Dark, Neutral and Hero. The baby should retain its Run development while its alignment appearance changes. Save/reload, then check the same Chao again.
3. Confirm a Survival player's bar is read-only and a Creative adult's bar remains locked. The existing adult should retain its first evolution's alignment.
4. Check your preferred hat, animal parts, glasses, toys and equipped pose. Existing previews remain `/chao testtoy broom once`, `/chao testtoy picture_book loop` and `/chao testtoy stop`; they use production animations, grant no rewards and preserve normal preview cleanup.

This environment has no audio output hardware, and no sound changes are part of this update. Shaders, resource packs, a full modpack, a real user's existing world, remote-player views and network latency were not independently verified. The source ZIP is a cumulative overlay onto full 0.25.43 source, including prior updates and explicit source deletions; use its deletion instructions or the complete delivery branch. The source helper does not touch Minecraft saves.
