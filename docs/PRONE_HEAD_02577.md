# Prone toy head correction — 0.25.77

Toy-car and crayon/marker floor play bring the back of the original head closer to the torso. Car pitch is about 5.73 degrees flatter (0.20 to 0.10 radians), with extra lift reduced from 0.40 to 0.10 model pixels. Crayon pitch is about 2.86 degrees flatter (0.25 to 0.20 radians), with lift reduced from 0.15 to zero. The gentler pitch retains chin room above the hands while reducing the neck gap. The existing presence curve blends entry and exit.

The original head box, shared neck/floor fitting, hands, paper/car positions, current faces and lowered mouths remain unchanged. A larger straight vertical drop failed the existing Amy hand/head checks. A smaller straight drop passed but still showed a gap in the first client study; the final pitch/lift adjustment improves both connection and hand clearance.

The final active-toy and prone-support suites passed in `prone077-rigs06`; the quiet-toy suite passed in `prone077-rigs04`, after which its production code remained unchanged. Active tests sampled 138,672 poses and 179,760 emitted hands; quiet tests sampled 80,892 poses and 130,968 emitted hands. Car minimum hand/head clearance was 0.099302 model pixels, and quiet minimum was 0.036825. No hand, floor, grip or transition assertion was relaxed. The NeoForge 1.21.1 / Java 21 JAR assembled successfully.

The connection check covered 588 baby/adult state and transition configurations, 191,670 production frames, 8,592 visible core-face contacts, 210 hat contacts and 575,010 head-child matrices. Maximum measured visible neck gap is 0.148472 model pixels, versus 0.440929 in 0.25.76. Prone support passed 1,516 source cases and 1,005,244 sampled frames. The controlled old-lift comparison measures a 0.30-pixel car and 0.15-pixel crayon lift reduction at the final angles; it is not the total displacement of every head point versus the old pitch. Numerical checks do not replace visual review.

The final actual-client run completed eight production-preview cases and saved 51 original 1440×900 screenshots with no skipped cases. It covered both toys on ordinary babies, shiny babies with rabbit parts, and Hero Power/Power+Fly adults, plus a Dark Run car loop/explicit stop and a Hero crayon damage interruption. Native command dispatch, production pose frames, client cue channels, saved identity fields and temporary AI cleanup checks passed. These were operator previews, not a fresh natural-approach/pathfinding test.

Personal static review covered the six matched side panels on `head-connection.png`; a separate reviewer also checked seven original front, loop, interrupted and recovered views. The sampled heads sit close to the torso, hands and props remain coherent, and stopped/interrupted play returns to a recovered pose. All 51 originals and their capture evidence are included in the delivery gallery. Screenshots and numerical continuity checks do not establish a complete every-frame motion review.

Existing production previews are:

```text
/chao testtoy toy_car once
/chao testtoy toy_car loop
/chao testtoy toy_car stop
/chao testtoy crayons once
/chao testtoy crayons loop
/chao testtoy crayons stop
```

Both require operator permissions and retain existing preview cleanup/save protection. No gameplay, persistence, network, toy timing, sound or genetics code changed. The broad server suite and remote multiplayer were not rerun for this rendering-only change; the known 0.25.76 inherited failures remain outside this fix. Physical listening, phone/touch controls, custom shaders and exhaustive equipment checks remain unverified.
