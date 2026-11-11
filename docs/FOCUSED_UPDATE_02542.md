# Piano, Creative Analyzer editing and garden setup

## Piano

The triangle was the last completed new instrument before the playable-build work. This update advances the placed SA2 piano, which still used generic keyboard gestures. The organ and other unfinished toys are unchanged.

The existing renderer fits each actual visible hand to the source piano's measured key surface, using separate Lionfish press tracks. The server sends the reserved block position and the existing interaction deadline. Sixteen alternating contacts drive one harp cue per press and the placed key frame; the former generic block chime is suppressed for the piano. Hands raise and lower through the existing presence and transition blend. A happy expression yields to hurt and other activity priorities. Natural approach, block ownership, rewards and cooldown remain in the existing PlayGoal.

Operators can use `/chao testtoy piano once`, `/chao testtoy piano loop`, and `/chao testtoy piano stop` (or `/chao testtoy stop`). Place a piano within three blocks of a dry, idle Chao with clear ground at its front. The preview uses the real placed piano, production contact/cues and normal synchronized clock. It pauses care and grants no rewards, preserves the prior AI flag and cooldown, and cleans up on interruption, logout, distance, unload or explicit stop. It never creates an inventory toy.

## Creative Analyzer controls

Click an expressed grade to cycle E, D, C, B, A, S, X and back to E. Click the stat bar to choose progress 0–99. Click a level for +1, Shift-click for −1, Control-click for +10, and Control+Shift-click for −10. Hit testing uses the rounded visible canvas edges, including reduced GUI scales.

Levels remain 0–99 and points 0–9999. A level edit adjusts points by the actual clamped level difference times `13 + 3 × current grade`, the existing mean training gain and defeat decrement. Existing points are otherwise retained: changing grade does not rewrite historical gains. Level 99 forces progress to zero; selecting progress alone never grants a level. Positive training edits update the existing training-recency tie breaker. Ordinary point-dependent abilities and future evolution selection therefore reflect the edited points. Inherited grade alleles, age, lifespan and care are not rewritten; X remains an expressed grade, not an inherited gene.

A dedicated bounded request is validated on the server against the player's actual Creative game mode, same-level entity, normal care eligibility/distance, living state, cocoon and activity checks, and valid action/stat/delta. The authoritative entity state is synchronized and any equipped carrier snapshot refreshed. Survival, Spectator, distant and malformed requests cannot edit stats. Creative retains the existing care permission to manage another keeper's Chao.

## Garden setup

The existing device location and Activate action now mark its chunk plus eight neighboring chunks. No enclosure, tree, floor or neighboring-chunk load check is required. The square covers the world's buildable height. This does not construct terrain or barriers.

Saved enclosure footprints remain unchanged on load; explicitly activating or rechecking an old device replaces its footprint with the square. Authored garden dimensions still use their established whole-garden boundaries. Ownership, six slots, automatic detection, manual reservations, dismissal, recovery associations and management remain in their existing systems.

## Test entry points

- `coreTest`: Analyzer rank/modifier/progress/limit/serialization and scaled-edge checks, plus existing core regressions.
- `pianoTimingTest`: actual Lionfish samples against every alternating contact, frame and pitch limit.
- `chao_focused42` and `chao_garden_checks040`: native authorization, persistence, bounds, eight piano age/facing combinations, damage/removal/reload, reward-free commands, and garden/portal regressions.
- `chao_garden_save42`, then `chao_garden_load42` against one test directory: separate process save/reload of new and legacy gardens, edited resident, detection, crossing, return and pickup.

Client screen and visual captures are kept in the release evidence, separately from automated checks. Actual listening and two independent clients with network latency remain manual acceptance checks.
