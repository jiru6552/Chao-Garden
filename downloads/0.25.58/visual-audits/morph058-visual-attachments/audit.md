# Final .58 motion / toy / carry visual audit

**PASS WITH LIMITATIONS.** Current post-depth cohort; reviewer `/root/accept_motion058`. No new visual blocker found in this scoped review.

## Provenance

- Final log: `/workspace/.setup/morph058-client-final.log`
- Log SHA-256: `cc4e0990a8afbe7bf2cea29498c1a3ff51f8677e28b95372a0e34b845e09eb12`
- Current .58 JAR SHA-256: `ed3f32e0f256a513bdc83732ad1c746834bce9840dfaabe3223b9c01d193eaa3`
- Source index SHA-256: `8534915c535536471d05345907f67934fefd18a0ee5abb8a69c1e52f452349b5`
- Case catalog SHA-256: `3ffdb99bca883e229c1247cc4d2604648151660be49e6a9748737b087a110b28`
- Verified all 184 original PNG hashes and literal capture markers, all ten review-sheet hashes, and all ten native/render proof pairs against this exact log.
- Personally inspected all ten sheets (90 selected original frames) and reopened 19 originals at full resolution, including every five current book-front frame. The two JSON indices enumerate paths, hashes and literal proof lines.

```text
[20:33:43] [Render thread/INFO] [co.ch.cl.MorphRefinementVisualSmoke058/]: MORPH058_VISUAL_COMPLETE requestedCases=77 renderedCases=77 skippedCases=0 start=0 limit=77 baseline=false captures=593 saved=593 epochMillis=1794515623107 normal20TPS=true audioListening=false output=/workspace/.setup/morph058-client-final
BUILD SUCCESSFUL in 8m 15s
```

## Observations

- **Case 48 / hero_normal_run_fly_gain_walk:** Hero Run+Fly gain: sampled actual strides and changing accents/tips remain coherent; box head and eyes stay fixed, compact extremities and attached wing forms remain intact. Early frames include the mobility threshold transition.
- **Case 49 / hero_normal_run_fly_loss_walk:** Hero Run+Fly loss: specialized accents/tips recede with reductions. Later low-Run locomotion/sitting transition differs from gain as expected; no visible stretched or detached limb in reviewed samples.
- **Case 50 / hero_normal_power_swim_gain_walk:** Hero Power+Swim gain: sampled actual strides show coherent tips, halo, limbs and gold wings while compatible influences develop; no apparent root/cap interference.
- **Case 51 / hero_normal_power_swim_loss_walk:** Hero Power+Swim loss: sampled actual strides preserve boxed head and attached wing/limb construction while influences recede; no apparent root/cap interference.
- **Case 52 / hero_normal_run_fly_broom:** One-shot broom preview: side original shows hands enclosing handle in front of skull, bristles beside ground; front samples show happy expression. Rear t180 original shows broom absent and ordinary pose after cleanup. No apparent detached hand or toy in reviewed samples.
- **Case 53 / hero_normal_power_swim_book:** One-shot picture-book preview: all five front originals t012/t024/t036/t048/t060 reopened full resolution. Colored upper caps meet white lower roots cleanly with no interleaved stripe artifact, including t036 which failed in the superseded cohort. Happy face and page-turn snapshots change while hands remain at book edges. Side t096 shows book held in front; rear t180 shows book absent after cleanup.
- **Case 54 / hero_normal_fly_equipped_flying:** Actual Accessories Hero Fly adult in upright Creative flight: front/side/rear-oblique samples keep one coherent attached Chao body at player back. Full-size side t072 and rear t120 show compact folded limbs, separated halo and intact tips; player occludes the inward-facing face. No evidence of separation or hollow wing outline in available views.
- **Case 55 / hero_baby_equipped_flying_regression:** Actual Accessories Hero baby in upright Creative flight: front/side/rear-oblique samples retain compact head/body/limbs at back mount. Full-size side t072 and rear t120 show stable attachment and separate orb; face is substantially occluded by player as mounted. No evidence of detached or stretched geometry in available views.
- **Case 74 / neutral_normal_power_swim_gain_walk:** Neutral Power+Swim gain: sampled actual strides and evolving stepped top retain original head/eyes, compact hands/feet, and attached pink wings; side t180 checked full size.
- **Case 75 / neutral_normal_power_swim_loss_walk:** Neutral Power+Swim loss: sampled actual strides show top receding toward basic tip, without skull deformation or detached limb/wing; side t180 checked full size.

The prior book-root/cap horizontal stripe interference is absent in current front t012, t024, t036, t048 and t060 originals. This verdict uses the new complete77 cohort, not the archived pre-depth screenshots.

## Runtime evidence

| Case | Elapsed ticks | Walked blocks | Captures | Moving frames | Toy contacts | Max grip error (blocks) |
|---|---:|---:|---:|---:|---:|---:|
| 48 | 280 | 6.792856 | 23 | 396 | 0 | 0 |
| 49 | 280 | 6.009938 | 23 | 405 | 0 | 0 |
| 50 | 280 | 9.739358 | 23 | 404 | 0 | 0 |
| 51 | 280 | 9.739358 | 23 | 408 | 0 | 0 |
| 52 | 180 | 0.000000 | 13 | 0 | 370 | 3.60411e-07 |
| 53 | 180 | 0.000000 | 14 | 0 | 372 | 3.5793818e-07 |
| 54 | 140 | 0.000000 | 10 | 16 | 0 | 0 |
| 55 | 140 | 0.000000 | 10 | 16 | 0 | 0 |
| 74 | 280 | 9.739358 | 22 | 396 | 0 | 0 |
| 75 | 280 | 9.739358 | 23 | 403 | 0 | 0 |

Broom and book each used the real `chao testtoy ... once` dispatcher. Native completion follows assertions that the preview is inactive, toy ticks are zero and target is cleared; both t180 original images visibly show the prop gone. Grip measurements require actual transformed palms against production toy contacts, rather than assuming that a model value implies a usable grip.

Carry cases require actual Accessories runtime/container, a managed Chao projection and flying attachment selection; renderer completion also requires the real player carry layer to render more than 20 times. The side and rear originals show stable mounted forms, with expected inward-facing facial occlusion by the player.

## Limits

- Visual review is 90 selected first/middle/last captured originals through ten uniformly cropped/resized sheets, plus 19 reopened 1440x900 original PNGs. All 184 source PNG hashes were checked; this does not mean all 184 were visually opened.
- Front, left and rear-oblique views are sequential points in each live scene, not simultaneous matched captures of one animation instant. This is snapshot sequence inspection, not full video playback or an interactive manual playthrough.
- Runtime helpers exercised production actors/renderer, navigation, training, Analyzer reductions, once toy commands and actual Accessories slot at normal 20 TPS. Fixture training was deliberately accelerated; this does not establish natural lifetime training speed.
- Toy coverage is broom and picture book once previews and their natural completion/cleanup. No autonomous approach, repeated use, forced interruption, every toy, audio cue or listening claim.
- Carry coverage is actual Accessories slot during upright Creative flight only. No elytra/horizontal flight, swimming, takeoff/landing, all custom player rigs/skins or exhaustive accessory combinations.
- No physical phone or touch interaction, shader packs, remote multiplayer, dedicated-server session or full audio playback was tested by this audit.
- This audit does not independently redo first-evolution rules, Analyzer permission/persistence tests, static all-form/viewer comparison or source build; those belong to the broader release evidence.

Only audit artifacts were written. No production/source/assets/build/client/package/commit/push action was performed.
