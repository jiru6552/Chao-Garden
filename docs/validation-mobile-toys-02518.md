# Ball, rocking horse and lifesaver follow-up

Status: **implemented in working source; native compilation, GameTests and visual signoff pending**. This is the second focused toy chunk following `validation-toy-interactions-02518.md`. It is not included in the old packaged 0.25.18 JAR.

## Runtime changes

- Ball: precise width-aware contact point; an explicitly synchronized 10-tick kick, with physical block movement at contact tick 4; no kick from 1.7 blocks away. Chase uses the actor's normal age/stat gait instead of sitting with locomotion amplitude suppressed. Kick animation does not reuse a combat swing. Destination blocks must be clear and cannot contain another living actor. Forced displacement cancels the session. The ball remains one real, recoverable grid-positioned block; this is not a continuous rigid-body physics rewrite.
- Rocking horse: rider faces the source muzzle, which is local +Z (opposite block FACING). Position and whole-root pitch follow the same 0/+6/0/-6-degree ground-pivot motion as the mesh, using a shared 32-tick session clock. The rider's root uses a measured saddle region with a provisional pelvis offset; final rendered fit for each anatomy still requires client review.
- Lifesaver: dedicated supported-floating motion takes priority over swim/flail behavior for all swim skill levels; untrained distress splashes are suppressed during supported play. The waterlogged ring is raised .80 blocks to the water surface and bobs by only +/- .025 blocks without shrinking. Actor base follows at .78 plus the same bob; dry models remain at ground height. Draining the ring cancels play safely.
- Horse/ring entry and exit interpolate over 12 ticks. The ring's short entry/exit lift clears shore collision before lowering into water. Both mounts check full actor clearance against surrounding blocks and living actors, excluding only their owned prop. Low roofs prevent selection; new obstructions interrupt safely. Existing validated/deferred mounted cleanup is retained.

## Geometry and source changes

- `scripts/generate_sa2_assets_02515.py --mounted-toy-poses` regenerates only horse/ring poses and waterlogged variants.
- Horse uses one fixed rest fitting transform and X-axis pivot (.5,.005,.5). It stays above the ground without per-frame recentering or scale changes.
- Lifesaver uses rigid translations only. Five floating OBJ models and five JSON model definitions are new. All 64 facing/frame/playing/waterlogged blockstate combinations are explicit.
- Relevant collision envelopes and legacy toy-envelope validation were adjusted. No handheld instrument, rattle, drum, garment, analyzer or coffin choreography changes were included in this chunk.
- `ChaoEntity` adds a synchronized kick-start timestamp, reset on session start/end; existing reload cancellation remains authoritative. Client and server must use matching rebuilt versions.

## Completed checks

Run `scripts/test_toy_interactions_02518.sh`:

- **1,768 pure Java rule assertions passed**: contact reach, kick timing, chase/supported-float selection, shared frame clocks, rigid rider pivot radius, gradual mount endpoints, shoreline clearance, and prior coffin rules.
- **5 mounted geometry tests passed**: actual source horse facing/saddle, rigid pivot/floor/transforms, constant-size centered ring, shipped/generator agreement, and waterlogged surface variants.
- **4 prior coffin geometry tests passed**.
- Touched Java source parses without syntax errors using the installed compiler module. This is not NeoForge type checking.
- Python compilation and deterministic targeted regeneration checks passed. Geometry worker compared hashes of all 37 targeted files before/after regeneration.
- Independent source review found missing neighboring-block headroom checks, living-entity ball-destination checks, and the lingering splash branch. Those findings were addressed and native regressions added. Final re-review found no remaining high-confidence blocker; its goal-arbitration coverage caveat was addressed by retaining the production FloatGoal in the new test fixture.

The broad legacy SA2 asset validator passes its toy section, then fails at the existing unrelated `Missing garment bevel` check. No aggregate validator pass is claimed.

## Native tests added, not executed

`MobileToyGameTests02518` contains 15 new GameTests:

- Adult and newborn ball contact/chase with one real block.
- Remote displacement cancels without a remote kick/reward.
- Ball cannot roll into another actor; resumes after the destination clears.
- Horse saddle/facing/frame agreement in all four orientations.
- Swimmer and non-swimmer water-surface ring support with eyes above water; the production FloatGoal is retained and checked for non-interference.
- Draining ring restores supported shore placement/gravity without reward.
- Horse/ring low-roof rejection and interruption by newly placed roofs.

These supplement the 18 piano/organ/coffin/recovery tests from the first chunk and the pre-existing toy tests. Server tests cannot certify renderer poses or the absence of visual clipping.

## Remaining verification and exact blocker

The official Gradle 9.2.1 wrapper distribution and dependency cache are unavailable. Using a writable Gradle home, wrapper download failed with `java.net.SocketException: Network is unreachable`. No full compile, native GameTest run, current client capture, or new installable JAR has been produced here.

In a dependency-equipped authorized environment, run normal build/core tests, both new native test classes and prior toy tests; then capture whole adult/newborn ball pursuits, four-orientation horse rides, and swimmer/non-swimmer ring entry/rest/exit. Inspect hand/foot/saddle contact, the direction of root pitch, shoreline transitions, low-roof interruption, water surface placement, anatomy/accessories, and networked phase alignment. Reconcile release resource counts for the ten new floating model assets before packaging.
