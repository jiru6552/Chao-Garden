# Idle variety — 0.25.78

Seven optional garden gestures now use the existing synchronized behavior clock and shared Lionfish rig. The fixed head box, morphology assets, inherited personality and faces remain intact.

| ID | Action | Duration | Natural weighting or gate |
| --- | --- | --- | --- |
| `look_around` | Paused glances to each side | 72 ticks | More likely with curiosity |
| `look_up` | Look up, linger and settle | 64 ticks | More likely with curiosity |
| `think` | Head tilt and small thoughtful paw gesture | 80 ticks | Skillful and solitary tendencies increase weight |
| `bashful` | Small inward gesture with a lowered, tilted gaze | 68 ticks | Quieter, less energetic or solitary tendencies increase weight |
| `chuckle` | Two small, uneven laugh accents | 68 ticks | Requires moderate joy or happiness and no substantial fear/anger/crying; chatty/energetic tendencies add weight |
| `tired` | Gentle sleepy dips and a brief yawn | 84 ticks | Requires actual moderate fatigue, sleepiness or reduced energy; urgent tiredness/sleep takes priority |
| `ground_play` | Seated, near-ground paw fidget | 104 ticks | Carefree/easily bored tendencies add weight; requires a supported stationary moment |

The personality weights are soft preferences: a personality does not select one fixed animation. Baby posture and the existing form determine presentation; alignment does not rewrite personality. The selector uses individual random 400–900-tick gaps and only admits eligible stationary Chao. Hunger, illness, sleep, strong moods, lessons, movement, held items, care, toys and managed activities take priority. Idles give no care, friendship, training or lesson rewards. Original gameplay simulation continues during natural idles.

Both older ambient loops are replaced: the imported torso twist/low arm sweep and the separate repeating personality overlay. Raw source/reference animation inspection remains unchanged. The separate rash-triggered `SCRATCH` animation and underlying illness behavior are retained unchanged. Rash treatment, illness selection and symptom face precedence remain existing gameplay. Commanded-stay Chao retain their existing priority-one stay lock and calm pose.

Temporary idle faces run through the current resolver below native character exclusions, damage, danger, sleep, symptoms, illness and active care. Inherited eyes/mouths and lens colors restore when the gesture ends. The ordinary visible emotion ball gets a tiny phase accent; existing halos, themed/flame markers and hidden geometry keep their appearances. No audio loop was added.

Natural idles use appended `Behavior.IDLE` and the existing tracked detail/start/duration fields. Existing enum ordinals and the Chao state schema are preserved. A saved transient idle is canceled safely on reload and waits for a new interval; no inherited face or personality reroll occurs. Existing operator preview sessions pause permanent simulation and restore temporary AI flags using their established cleanup.

The requested [CWE animation list](https://chao-island.com/wiki/Chao_World_Extended_%28More_Chao_Animations%29) returned a Cloudflare challenge/HTTP 403 through direct and alternate article endpoints. These are mod-authored adaptations of the ideas supplied in the request and the current rig, not verified copies of CWE timings or original-game motions. Raw reference responses are retained in the delivery evidence.

Operator examples (permission level 2):

```text
/chao testidle list
/chao testidle look_around once
/chao testidle ground_play loop
/chao testidle ground_play stop
/chao testidle stop
```

All seven IDs support once/loop/matching stop, and `/chao testtoy idle_<id>` is an alias. Each 160-tick preview plays the short production clip then returns to calm until the session ends or loops. Forced `tired` previews do not change real fatigue. Selection uses the nearest Chao within 12 blocks and reports a busy nearest target rather than choosing a different Chao silently. Damage, player interaction, unload, logout, range/dimension changes and explicit stop retain existing cleanup.

## Verification

The final Java 21 / NeoForge 1.21.1 `coreTest idleRigTest078 assemble prepareClientRun` invocation passed. Selection tests passed 38,675 checks and idle-face tests passed 13,688 checks, including fatigue gates, personality variety, non-mutating state reads, inherited faces/lenses, precedence and restoration. Existing core evolution, genetics, save and morphology tests also passed.

The new registered-rig suite passed 44,455,271 checks across 54 baby/adult alignment, first-type and maturity profiles, 406 full/interrupted episodes and 299,222 quarter-tick frames. It checked the original 7×6×6 head, unchanged scale/reach/limb proportions, 897,666 attached head-child matrices, head/palm clearance, floor clearance and transitions. Twenty-four additional large-gaze admission/cancellation/release traces sampled 8,248 quarter frames: the first admitted/released head-surface step was zero and the maximum quarter-step was 0.24244 model pixels.

The inherited resting rig already has palm/torso envelope overlap (up to 1.49078 model pixels in these profiles) and tiny foot-floor penetration (up to 0.04500 pixels). Paired current-resting baselines showed zero added penetration and exactly unchanged foot transforms. The new head/hand minimum separation was positive at 0.04111 pixels. These are numerical source-hand-envelope checks, not proof of every replacement animal paw or hat collision.

A broad `--continue build` diagnostic completed 75 tasks with nine inherited failures: `darkFlyMorphRigTest069`, `darkPowerMorphRigTest071`, `darkRunMorphRigTest070`, `darkSwimMorphRigTest068`, `heroFlyMorphRigTest065`, `heroPowerMorphRigTest067`, `heroRunMorphRigTest066`, `heroSwimMorphRigTest064` and `neutralPowerMorphRigTest063`. Each stops at an obsolete protected-model SHA preflight before its remaining geometry checks. Both those test sources and the referenced models match accepted 0.25.77 bytes; the guards were not weakened. All 8,251 packaged asset/data files also remain identical to 0.25.77. The other invoked checks completed, including facial glyphs/hat/animal anchors, car/crayon/prone support, radio, TV, ball, instruments, carry, locomotion and sitting. The final preview-gaze admission adjustment and its new continuity tests were verified separately in the final successful focused invocation.

The dedicated-server run completed 4,588 cases: 4,559 passed and 29 failed. All 44 new idle cases passed. The 29 failure names exactly match the accepted earlier broad run: 20 existing hand-toy preview cases, eight mounted-preview fixture cases, and `actualnetherslotprotectsfireandlavabutnotordinarydamage`. This includes a real existing Nether-companion issue; it is not all cosmetic test noise. The exact names and provenance are in `verification/idle078-native-inheritance-audit.json`. Native idle tests exercised actual delayed goal selection, expiry/cooldowns, rested exclusion of tired, need and movement takeover, real placed-toy takeover, sleep, item transfer, petting/damage, safe NBT reload and early/late/rejoining metadata observers. Metadata tests are not independent remote clients.

The final actual client completed 14 cases, no skips, and 84 original 1440×900 screenshots at normal 20 TPS. Seven baby previews cover all IDs, inherited eye/mouth combinations and special lenses; adult cases cover Neutral Swim/Fly, Hero Power/Fly with rabbit parts, Dark Fly/Swim, Dark Run/Fly loop-stop and Hero Swim/Run damage interruption at 65% biological maturity and 75% signed specialization. Natural controlled-goal fixtures selected adult `look_around` after 440 ticks and moderately fatigued baby `tired` after 784 ticks. They retain the production idle goal but remove unrelated goals, so they are not a full unrestricted-garden soak. Preview state preservation, identity/equipment, temporary AI restoration, actual rendered expressions and recovery assertions passed.

Personal visual review covered all seven baby action fronts, baby look-up/ground-play sides, and 16 consecutive recorded motion frames. An independent reviewer inspected 32 original-PNG views covering adult front/side/back, second-loop and stopped views, real damage/recovery, natural tired/recovery and the seven baby fronts. No clear static visual blocker was found in those samples. `idle-examples.png` is a labeled, uniformly resized crop sheet; `GALLERY.md` links all unchanged originals. `idle-motion.mp4` is an approximately ten-second excerpt of the actual natural look-around recording, with authored camera cuts between views; the full silent 55-second recording is also included. Sampled-frame review and numerical continuity do not establish a complete every-frame visual audit of all forms.

The seated ground-play action is a gentle near-ground paw fidget; literal dirt contact is an approximation. The reference page remained inaccessible. Independent remote multiplayer, prolonged crowd behavior, existing-world restarts, exhaustive hats/replacement limbs/player-attachment combinations, phone controls, custom shaders and physical audio listening were not verified. No new sounds were added. Useful follow-up checks are a live garden group, your usual hats/animal parts, and a second player's view during interruptions; historical SHA baselines and the inherited server failures need their own cleanup.
