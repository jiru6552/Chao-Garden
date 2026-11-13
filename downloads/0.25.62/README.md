# Chao Garden 0.25.62

[Download JAR](chao_garden-0.25.62.jar?raw=true) · [Source changes ZIP](chao-garden-0.25.62-source-changes.zip?raw=true)

Adds the complete blocky Neutral / Run-first family: balanced, Run, Power, Fly, Swim and four compatible pairs. The original head box stays unchanged. Requested stripes now occupy the head top and central crest, leaving the forehead and sides clear. The full build, 476 dedicated-server tests and 92 actual client cases passed.

[NEUTRAL_RUN_02562](NEUTRAL_RUN_02562.md) · [VIEWER_REFERENCES_02562](VIEWER_REFERENCES_02562.md)

- [Neutral / Run first / family-stages / actual front](gallery-neutral-run-family-stages-front.png)
- [Neutral / Run first / family-stages / actual right](gallery-neutral-run-family-stages-right.png)
- [Neutral / Run first / family-stages / actual back](gallery-neutral-run-family-stages-back.png)
- [Neutral / Run first / training / actual front](gallery-neutral-run-training-front.png)
- [Neutral / Run first / training / actual right](gallery-neutral-run-training-right.png)
- [Neutral / Run first / training / actual back](gallery-neutral-run-training-back.png)
- [Neutral / Run first / opposition / actual front](gallery-neutral-run-opposition-front.png)
- [Neutral / Run first / opposition / actual right](gallery-neutral-run-opposition-right.png)
- [Neutral / Run first / opposition / actual back](gallery-neutral-run-opposition-back.png)
- [Neutral / Run first / variants / actual front](gallery-neutral-run-variants-front.png)
- [Neutral / Run first / variants / actual right](gallery-neutral-run-variants-right.png)
- [Neutral / Run first / variants / actual back](gallery-neutral-run-variants-back.png)
- [Neutral / Run first / existing-family-regressions / actual front](gallery-neutral-run-existing-family-regressions-front.png)
- [Neutral / Run first / existing-family-regressions / actual right](gallery-neutral-run-existing-family-regressions-right.png)
- [Neutral / Run first / existing-family-regressions / actual back](gallery-neutral-run-existing-family-regressions-back.png)
- [Neutral / Run first / attachments / actual front](gallery-neutral-run-attachments-front.png)
- [Neutral / Run first / attachments / actual right](gallery-neutral-run-attachments-right.png)
- [Neutral / Run first / attachments / actual back](gallery-neutral-run-attachments-back.png)
- [Neutral / Run first / ui / sequential front, left, back](gallery-neutral-run-ui.png)
- [Neutral / Run first / motion-toys-equipped / sequential front, left, back](gallery-neutral-run-motion-toys-equipped.png)
- [Neutral / Run first / gain-reversal-evolution / sequential front, left, back](gallery-neutral-run-gain-reversal-evolution.png)
- [Neutral / Run first / cross-intermediates / actual front](gallery-neutral-run-cross-intermediates-front.png)
- [Neutral / Run first / cross-intermediates / actual right](gallery-neutral-run-cross-intermediates-right.png)
- [Neutral / Run first / cross-intermediates / actual back](gallery-neutral-run-cross-intermediates-back.png)
- [Actual Analyzer controls / uniform full frame](gallery-analyzer-controls.png)

[Actual client motion recording](neutral-run-0.25.62-motion.mp4?raw=true)

- Blocky shapes, palettes and interior development timing are authored approximations. The requested marking placement intentionally differs from the viewer’s forehead droplets.
- The independent viewer rendered, responded to controls and rotated. Review covers 90 matched viewer/game pairs plus 27 opposite-side cells. The native client used development outputs with all 8,336 production entries byte-identical to the final JAR; it did not load that archive.
- One inherited journal paragraph-format assertion remains: 8/9 passed. This is separate from the passing full build, focused suites and dedicated-server tests.
- Physical touch, independent remote clients, complete world restart and historical-world migration were not tested. Entity-NBT roundtrips and in-process tracked synchronization passed.
- Water captures show a stationary pose. Upright Creative carrying has player/hair occlusion. Horizontal carrying, autonomous swimming/gliding, continuous movie playback, listening and full lifetime progression were not verified.
- Large edits in this new family ease over 30 ticks; the display lags continuously changing stats. Full passive visual maturity at 14.4 hours versus the ordinary 12-hour adult lifespan is inherited. Care boosts or Creative maturity allow endpoint testing.

Use matching client/server JARs and external Lionfish API 3.1. Source ZIP is a cumulative overlay onto the complete 0.25.43 source; this branch contains the full source. Original captures are archived unchanged; galleries crop/resize uniformly. Development-classpath/JAR byte comparison does not mean runClient loaded the packaged JAR. See reports, native-client-captures.json, required-check-evidence.json and SHA256SUMS.txt.
