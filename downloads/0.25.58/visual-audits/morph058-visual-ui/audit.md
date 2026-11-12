# Final post-depth Analyzer UI audit — 0.25.58

**PASS_WITH_LIMITATIONS — no blocking finding.**

This audit uses only the final successful post-depth 77-scene cohort: zero skips, 593 unchanged PNGs, `BUILD SUCCESSFUL in 8m 15s`. The 25 inspected originals comprise 16 Analyzer captures and 9 world views for cases 45–47. The full log SHA is `cc4e0990a8afbe7bf2cea29498c1a3ff51f8677e28b95372a0e34b845e09eb12`; the frozen production/JAR SHA is `ed3f32e0f256a513bdc83732ad1c746834bce9840dfaabe3223b9c01d193eaa3`.

I verified all 25 source-image hashes against their exact current log lines and pixel-compared every review-sheet cell with its prescribed original crop/resize. I personally viewed all four sheets and reopened both raw held-drag captures at full resolution. No old pre-depth review or percentages were reused.

The actual held values are **59.4% for the adult** and **73.0% for the baby**. Both values and thumbs remain visible without tooltip overlap. Adult alignment stays locked; Survival edits remain denied. The Creative baby changes from Neutral to Hero+80.2, reaches the safe 99% baby maturity endpoint and remains a child. Adult safe endpoint is 100%. All three current native verification lines assert exact NBT reload and preserved stage/identity. Final front, side and back views retain intact faces, compact limbs and separate halo/orb. One Survival preview blink is an ordinary expression, not a missing eye.

Physical phone touch, independent remote modified-client/multiplayer or latency testing, shaders/full modpacks, every animation/contact, full movie playback and listening were not performed by this reviewer. Static views and recorded native assertions are distinguished in the accompanying JSON.

No production source, build or game state was modified or run by this audit. See `audit.json` and `source-index.json` for the 25 original paths/hashes, four inspected sheets and literal case proofs.
