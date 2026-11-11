# Stationary toy reach and continuation follow-up

Status: **implemented in working source; native build, GameTests and visual verification pending**. This follows the piano/coffin and mobile-toy chunks. It is not part of the original packaged 0.25.18 JAR.

## Scope and behavior

The free-play controller now uses explicit stationary interaction anchors for these 18 toys:

- Viewing/listening/surprise: TV, radio, jack-in-the-box and rubber duck retain at least one-block center-to-center distance. TV and jack remain front-only; a wet duck is observed from its supported dry shore cell.
- Ground-play contact: trumpet, drum, triangle, guitar, accordion, bubble wand, rattle, maracas, bell, castanets, cymbals, flute, tambourine and shovel use the approached side's actual collision extent plus half the actor width and .06 blocks clearance. This works with the collision shape's current facing instead of assuming a circular prop.

A coarse navigation endpoint is no longer enough to begin stationary play: final movement settles within .08 blocks, then aligns the actor toward the prop. During play, the actor must remain within .25 blocks of a viewing anchor or .16 blocks of a contact anchor. Larger displacement cancels rather than continuing remotely or snapping the actor back from a distance.

Anchor checks include full actor collision bounds, actual support below the feet, no liquid in those bounds, and no other living actor occupying the space. They run during selection, immediately before engagement and throughout play. Lost support or blocked contact interrupts without completion reward. Small tolerable drift is settled at the anchor.

Toy kind, sand under the shovel, waterlogging transitions for duck, and required water for lifesaver are rechecked through approach and active play. TV/jack rotations that invalidate the selected front side also cancel. Look heights are toy-specific rather than a universal .6-block point.

Keyboard, ball, coffin, horse and lifesaver positioning/animation remain on their dedicated controllers. The existing lifecycle cleanup and bounded pending mounted recovery are retained. No models, held-prop rendering, inventory semantics or collection behavior were changed in this chunk.

## Completed verification

- `scripts/test_toy_interactions_02518.sh`: **2,285 pure Java assertions passed**, including the complete stationary classification, width/collision clearances, viewing separation, reach limits, sand and water-state changes, plus prior keyboard/coffin/mobile rules.
- **Nine geometry tests passed** (four coffin, five horse/lifesaver). These remain regression checks; no assets changed in this chunk.
- Compiler parsing of `ToyContent`, `ToyInteractionRules`, `StationaryToyGameTests02518` and its pure test passed. Parsing is not Minecraft/NeoForge type checking.
- Independent source review found no blocking issue in anchor/support/environment checks or dedicated-controller separation. It specifically notes that tighter TV arrival still needs native slow-newborn routing validation.

## Native regressions added, not executed

`StationaryToyGameTests02518` contains **25 new GameTests**:

- One actual navigation/engagement/completion fixture for each of the 18 stationary toys. They verify precise contact, collision clearance and viewing separation. Jack's authored surprise may interrupt instead of requiring a completion reward.
- Displaced radio listener and trumpet player cancel without reward.
- Removing shovel sand, draining a wet duck, rotating a TV away from its selected side, removing drum support and obstructing a bell's anchor all cancel and return the block to idle.

Run those alongside the existing four-orientation slow-newborn TV navigation suite, the 18 first-chunk tests, the 15 mobile-toy tests and the original toy lifecycle tests. The new fixtures themselves use adult navigation and are not a substitute for newborn path timing tests.

## Explicit limits

- Full Gradle/NeoForge compilation and native execution remain blocked by the unavailable Gradle distribution/dependency cache and the official wrapper download failing with `java.net.SocketException: Network is unreachable`. No new JAR or native pass is claimed.
- Viewing/contact positioning is verified mathematically and by source review, not by a rendered client capture. Hand reach, head angle and variable anatomy still need visual inspection.
- Contact anchors respect the existing collision envelopes; several imported props still have approximate envelopes. This chunk does not claim perfect anatomical hand-to-mesh contact.
- Handheld-looking toys still use the existing placed-prop presentation and generic gestures. Their held-prop redesign and source object-animation issues are separate remaining work.
- School-owned scripted sessions use their unchanged authored pupil path through `beginSchoolPlay`, not this free-play controller. Their choreography is not certified by these new tests.
- For stationary play, a forced world edit that embeds an actor in solid terrain cancels the toy session but does not provide general-purpose terrain rescue. Mounted cleanup continues to provide its separate validated/deferred recovery.
