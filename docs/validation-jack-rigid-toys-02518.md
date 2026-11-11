# Jack timing, rigid prop motion and measured collision envelopes

## Scope and behavior

- Jack-in-the-box uses one shared session clock: closed anticipation through tick39, short emergence40–43, full reveal44–127, retreat128–143, closed from144. The existing four authored meshes are reused; no new crank/hand contact or lid interpolation is claimed.
- The client expression/brief hand-and-head reaction follows the reveal at40–64, not entity age. The existing 30% fear/cry decision is made once at48, after the full reveal is visible. Existing personality choice and cooldown/reward semantics remain intact. The block emits its existing pop sound once on opening rather than on every generic four-frame loop.
- Rattle and drum use one original idle-size fit and rigid +/-8-degree rotations. Per-frame vertical support correction keeps the lowest point at .08 model units; it does not refit, rescale or recenter X/Z. Item assets and collectible behavior are unchanged.
- Collision envelopes are measured from the actual shipped idle/play vertex union, with small outward rounding and intentional floor support. Radio/guitar no longer have oversized tall boxes; triangle/duck/jack cover previously omitted geometry. Rattle/drum match their corrected fixed-size motion.
- All measured props use stable all-frame envelopes, including the jack's raised lid. This intentionally leaves empty space above a closed jack but prevents its collision and interaction anchor from growing into an actor mid-session. It is a conservative envelope, not an exact concave mesh collider.
- Asymmetric envelopes now rotate about the cell center for every cardinal facing rather than merely exchanging X/Z. Existing keyboard/coffin/horse/ring/ball controllers retain their code paths and geometry.
- No handheld ownership or rendering redesign, duck floating-height change, or unrelated source-art change.

## Changed paths

- `src/main/java/com/chaogarden/toys/ToyInteractionRules.java`: shared jack timeline and measured envelopes/rotation.
- `src/main/java/com/chaogarden/toys/ChaoToyBlock.java`: jack scheduler and measured rotated shapes.
- `src/main/java/com/chaogarden/toys/ToyContent.java`: one fear decision after reveal.
- `src/main/java/com/chaogarden/client/ChaoModel.java`: session-timed reaction.
- `scripts/generate_sa2_assets_02515.py`, `scripts/validate_sa2_assets_02515.py`: targeted `--rigid-toy-poses` generation/validation, preserving other assets.
- Ten rattle/drum block OBJ poses regenerated under `models/sa2_02515`; the two idle OBJs and generated block JSONs remain byte-identical.
- New `scripts/test_rigid_toy_geometry_02518.py` and `scripts/test_stationary_toy_geometry_02518.py`.
- `src/test/java/com/chaogarden/toys/ToyInteractionRulesTest.java`, aggregate `scripts/test_toy_interactions_02518.sh`.
- New `src/main/java/com/chaogarden/test/ToyPresentationGameTests02518.java` (five native fixtures).

## Verification

- Aggregate isolated suite: 3,089 Java rule assertions; 22 Python geometry tests (coffin4, horse/ring5, rattle/drum5, stationary envelopes8).
- The stationary geometry test compiles and executes the real production envelope helpers. It checks seven toys,35 shipped meshes,224 blockstate variants and28 cardinal envelopes, with independent rotation landmarks and negative controls for the previous asymmetric-swap bug.
- Rigid tests check mesh edge/long-distance preservation, exact generator/shipped agreement, floor contact and targeted-generation scope.
- Focused rigid asset validator: 4,378 checks passed.
- Five edited/new production Java files plus the isolated test parse successfully. Parsing is not type checking.
- Independent read-only review checked timeline ordering, all seven geometry envelopes and four-facing rotation. Its idle-hover observation was addressed with per-frame vertical support correction; final review is recorded in the task handoff.

## Native checks still required

None of the new native fixtures has run. The free-play fixture permits ordinary completion and therefore does not force the 30% fear branch or prove exactly one random draw; the once-only guard is source-reviewed. The fixtures cover the scheduled single reveal, cancellation returning to idle, real free-play reaction occurring after reveal, and child/adult native collision/anchor compatibility across four facings and all frames.

Full Gradle/NeoForge compilation, all previous native toy/TV navigation tests, resource loading and actual client rendering remain unverified. The environment has no cached Gradle distribution/dependencies; the earlier writable-home distribution download failed under network restrictions. No JAR, native screenshots or visual signoff is claimed.

The broad legacy asset validator still fails later on the existing unrelated `Missing garment bevel` assertion after its toy section. Focused checks are not a full release validation pass. Native capture should inspect jack pop/retreat readability and actor reaction, rigid drum/rattle ground contact, newly closer contact anchors, and duck/jack asymmetric facing clearance.
