# Movement milestones and idle refinement - 0.16

Primary references: [Chao Island Wiki - Stats](https://chao-island.com/wiki/Stats), its [Level redirect](https://chao-island.com/wiki/Level), [SA2 races](https://chao-island.com/wiki/Chao_Races_%28SA2%29), and the older [Info Center - Stats](https://chao-island.com/info-center/basics/stats.html). This release follows the GameCube/HD SA2 rules rather than the Dreamcast thresholds or Chao World Extended's optional behavior.

| Original-game milestone used | Points |
| --- | ---: |
| Crawl | Run below 50 |
| Walk | Run 50 |
| Run | Run 667 |
| Dash | Run 1334 |
| Novice flailing | Swim below 100 |
| Swim | Swim 100 |
| Improved swim stroke | Swim 667 |
| Garden ledge flight | Fly above 134; integer threshold 135 |

Points determine capability. The displayed level is a count of level-ups and the progress bar advances toward the next one; stat grades change how many points a level-up awards. The existing mod already stores grade-dependent points separately, so no fixed level conversion is used. Newborns have zero points. Reincarnation retains ten percent of points, allowing a reborn child to keep an ability when enough points survive. Adulthood alone does not override these SA2 gates.

The Fly sources disagree. The older Info Center says garden flight begins at 100 Fly points; the newer wiki explicitly gives above 134 for the GC/HD gardens. The implementation uses 135 to follow the requested newer wiki. Chao World Extended's 200 Fly/700 Swim animation rules and its unrestricted flat-ground flight are mod features, not the vanilla thresholds used here.

The source garden flight is short and ledge-based. Minecraft adapts it to a bounded descending glide with actual horizontal motion, a sixty-tick allowance per fall, an energy eligibility check and no upward lift. Ground and water speed formulas are Minecraft balance choices. Novices flail and seek safety rather than gaining a new drowning death condition. The equipped Accessories player-glide perk remains the earlier adult Fly-level 10 feature and is distinct from a garden Chao's point-based ledge ability.

The imported full idle clip previously played continuously, producing the reported repetitive swing. The new gameplay layer samples a quiet lowered-arm anchor pose and adds restrained breathing. It schedules one 27.5-tick sway gesture per UUID-phased 260-399-tick interval, with five-tick easing and no gesture during movement or reactions. Locomotion changes blend over eight ticks. The held pose uses a reduced-strength gesture while supported. Ground locomotion follows actual movement; crawl, trained gaits, novice water motion, swimming, improved swimming and gliding use their own pose logic. The existing raw source clip sampler remains available for reference validation. Themes, animal parts and Chaos additions follow the same semantic poses, while a held Chao uses a supported carrying pose.

Validation scope and actual captured behavior are recorded in the current `TESTING.md`; the preview manifest records original frames and source game-tick intervals. Review GIFs use a fixed inspection frame rate and are not presented as real-time capture recordings.
