# Handheld toy presentation: blocked quality handoff

## Outcome

No runtime code, collectible ownership, animation, or assets changed in this chunk. The existing ground-prop presentation remains visible. A generic client ItemStack attached to the torso would not satisfy prop-to-hand contact, so it was deliberately not shipped. Independent read-only review reached the same conclusion.

This is an implementation/validation blocker, not a claim that the feature is impossible. The next implementation needs measured rig/prop sockets and a working native renderer to validate them. Gradle/NeoForge and native rendering remain unavailable in this environment: the earlier writable-Gradle-home attempt could not download the uncached distribution because network access was blocked. No new native build was attempted here.

## Concrete evidence

- `ChaoHeldItemLayer.java:21-30`: handles actual saved items, learned INSTRUMENT and WATER behavior, but not placed toy sessions. Its synthetic learned props never enter inventory. This existing precedent is safe for collectible accounting, not evidence of correct hand contact.
- `ChaoModel.java:61-64`: `transformHeldItem` attaches to body plus fixed offsets, not a palm or grip. `holdHands` at 243 onward poses both arms but does not constrain either hand to a prop.
- `ChaoHeldItemLayer.java:52-58`: most toy items are BlockItems and use scale .105; shovel uses .30. Imported items are commonly fitted to a .875-block longest dimension, so the default toy extent is only about .092 blocks before actor scaling. Reusing this frame is not a reliable instrument size policy.
- `ChaoRenderer.java:141-144`: global scale is .64 for children, .80 adults and .86 immortals; development also changes rig geometry. Applying age scale again in a new layer would double-scale it.
- `ChaoMeshAsset.java:372-387`: arm attachment bounds intentionally select upper-arm cube0, excluding the hand at cube1. They are wardrobe bounds, not grip sockets. `transformBone` follows the hierarchy but cannot supply an authored grip position by itself.
- A full JSON resource scan found eight models with `attachment_landmarks`, all with only `eyes`. There are no authored grip, tool, hand or mouth landmarks in this resource schema. Form-specific hand geometry differs; Chaos has separate hand bones and empty ordinary hand geometry. `ChaoModel.java:174` hides normal arms when animal arms replace them.
- `scripts/generate_sa2_assets_02515.py:83-96`: item axes differ by category. Maracas and cymbals are duplicated into one baked paired mesh; each half needs separate transforms for genuine two-hand movement. Flute, rattle, bell and wand item orientations differ from the placed form.
- `scripts/generate_cwe_assets_02516.py:196-210`: guitar/accordion have a separate asset pipeline. Accordion deformation currently scales the complete mesh rather than independently articulating bellows and rigid end pieces.
- `models/item/chao_triangle.json`: inherits the placed idle block model, unlike the imported handheld-item meshes. Shovel uses the fruit02512 asset pipeline.
- `ChaoEntity.java:54-59,220-232`: toy kind/end time are synchronized, but no session target position or visual-owner identity is synchronized. Persistent `ToyContent` target tags are server-side ownership data, not a renderer contract.
- `ToyContent.java:293-336`: the placed block remains authoritative through a session. Completion creates no replacement toy. `toyRemoved` cancels owners. These semantics should remain unchanged.

## Category-specific work

- Rattle and bell: measured handle socket, one palm attachment, gentle wrist motion. Verify child and adult palms rather than anchoring by body height.
- Bubble wand: separate grip and mouth-facing loop locations; blowing motion must bring the loop near the mouth without putting the handle through the face.
- Shovel: handle grip plus blade/ground contact, two-hand support where appropriate. Keep the existing sand requirement and reward behavior. Reuse its source asset, not a second collectible.
- Trumpet and flute: mouthpiece anchor and secondary hand support; distinct source-axis corrections. They cannot use the same generic rotation as a rattle.
- Guitar: neck hand and strumming hand with a torso-rest plane; source neck/body must retain fixed proportions.
- Accordion: separate rigid end grips and bellows-only deformation, with hand separation matching the bellows phase.
- Maracas, cymbals and castanets: separately addressable left/right pieces where required, synchronized to each hand. Do not render the whole paired mesh once per hand.
- Triangle: suspended triangle plus independently animated striker. Do not attach its whole ground model to a hand as a substitute.
- Tambourine: one supporting grip and one striking hand.
- Drum: keep the drum placed. Implement hand or stick contact at its playing surface, not wholesale pickup.

## Proposed ownership/rendering contract

1. Keep the original placed block as the sole collectible and server reservation. Never move it into a Chao inventory and never spawn a second collectible entity for presentation.
2. Add an explicit synchronized session target/owner and presentation phase. A client-only prop renderer must verify the matching block, kind and active owner before rendering.
3. Suppress the placed visual only when a valid replacement visual is available. Unsupported anatomy, real held item precedence, missing asset, unload or canceled session must fall back to the visible placed toy. Do not leave a saved invisible block after crash/reload.
4. Use one renderer-owned pickup/use/put-down transform path, or an equivalently tested block/entity handoff. Preserve a single visual across block/entity update ordering and chunk/render-distance differences.
5. Keep server collision, recovery, reward and reservation semantics unchanged unless an explicit test justifies a change. Clean up on break/recovery, displacement, new held item, death, unload/reload and simultaneous claim.

## Required regression/visual evidence before enabling

- Ownership GameTests: exactly one recoverable toy before/during/after completion, cancellation, removal, actor death and reload; no held stack or dropped item created; second actor cannot acquire it.
- Presentation tests: valid target/kind/owner, stale/missing session and block, real-item precedence, unsupported rig fallback, pickup and put-down endpoint continuity, and no persistent hidden state.
- Asset tests: grip landmarks lie on handle geometry; rigid piece lengths/scales unchanged across frames; paired instruments have two distinct rendered parts, not four; bellows deformation excludes rigid ends.
- Native captures: all categories at pickup/use/put-down, four facings, child/adult/immortal, relevant animal arms and Chaos/special forms. Check palm/mouth contact, ground clearance, animation timing, occlusion, texture/light, shadows, reload and chunk-boundary visibility.

## Verification performed here

- Read-only renderer, ownership, generator and JSON resource inspection, independently reviewed.
- Existing isolated suite rerun: **2,285 Java rule assertions plus 9 geometry tests pass**.
- No new regression test claims a handheld fix because no such fix was enabled. No native compilation, native GameTests, screenshots, visual signoff or new JAR.
- Jack timing and unrelated geometry fixes are outside this handoff and belong to the next focused chunk.
