# Garden behavior in 0.19

Reviewed 30 October 2026. Main references: [Chao World Extended](https://chao-island.com/wiki/Chao_World_Extended), its More Chao Animations, Birthday Event, Monster Evolution and More Accessories sections, [Personality & Emotion](https://chao-island.com/wiki/Personality_%26_Emotion), [Death & Reincarnation](https://chao-island.com/wiki/Death_%26_Reincarnation), and [Stats](https://chao-island.com/wiki/Stats). When the live Wiki denied direct retrieval, the indexed primary-page text was used. These are authored Minecraft behaviors informed by those references, not execution of SA2 scripts.

## Decisions rather than labels

The core stores thirteen inherited personality sliders, eighteen emotion/need meters and six independent illness meters. The entity selects real, timed actions and navigates to actual partners/objects. One server owns the decision and completion; clients render synchronized action, remaining time, partner and instrument/dance detail.

| Need or event | Observable behavior | Boundary |
| --- | --- | --- |
| Joy | Cheer and small jumps | Other needs interrupt it |
| Anger | Tantrum; an angry Dark Chao that dislikes its keeper may strike once | Half a health point; never strikes a target already at one health |
| Fear, crying, surprise | Cower, flee using navigation, cry with tears, recover from dizziness | Recovery lowers the appropriate emotion only on completion |
| Fatigue or disturbed sleep | Groggy recovery; natural sleep/wake | Disturbance adds tiredness and prevents immediate return to sleep |
| Relax | Sit quietly; Fickle changes the rest duration | Requires sufficient food and energy |
| Loneliness | Approach a keeper to greet/nuzzle, or another Chao to converse | Solitude suppresses keeper attention; Chatty increases social attempts |
| Boredom and learned skills | Song, drawing, five dance/exercise routines, break dancing, eleven instruments | Four dance lessons unlock break dancing; performers can approach nearby musicians to join a band |
| Hunger near an orchard | Walk to a real trunk, shake it, release ripe fruit | Harvest changes each real pod to age zero; normal pickup/consider/eat then consumes the actual dropped item |
| Optional bullying | Angry, low-Kindness Chao may bully; high-Kindness Chao approach and intervene | `bullyChao=false` by default; friendly social behavior always runs |

The legacy Doctor labels remain projections of the underlying sliders. Carefree affects abuse response, boredom and animation speed; Naive affects sleep; Skillful affects training gestures; Regain affects recovery; Vitality affects activity/mating; Fickle affects relaxation. Charm, unused Total/Stress meters and the unknown purpose of Nourish are preserved without inventing benefits or a stress-death system. Condition changes with actual garden movement/stationary time and suppresses visible illness reactions when high.

The world-side action durations, attempt frequencies, thresholds, path radii and sound substitutions are Minecraft adaptations. Typical actions last 36–180 ticks, a tree shake lasts 80 ticks and a normal social conversation lasts 120 ticks. Hunger scans seven blocks around a Chao and does not load chunks. Numeric source meter ranges are retained; exact original decision thresholds are not claimed.

## Two-body friendships

Conversations require loneliness and available, awake, healthy partners. Both bodies approach, turn toward each other and share a timer. Five completed conversations unlock gossip; ten unlock hugs/holding hands. Hugs close the distance; handholding follows an actual short walk. Only the lower runtime UUID commits a shared completion, once, after verifying both action clocks, partners and distance. Interruption cancels both actors without completion benefits.

Each creature has its own persistent friend identity independent of mutable simulation randomness and its runtime entity UUID. At most 32 friendship records are retained. Capture, normal saves, companion snapshots and rebirth eggs preserve identity and completed counts. An unfinished shared session cancels on load without replaying a result. There is no unbounded world friendship registry.

## Illness, medicine and food transactions

All six illnesses have distinct actions: cough, sneeze/cold, scratch/rash, runny nose, hiccup and stomach ache. A visible symptom never cures its meter. Natural rare onset, documented form/personality protections and treatment immunity live in the pure core. The Doctor reports the actual active conditions.

Medicine offered by a keeper transfers exactly one real stack to the Chao. It follows THINK → TRAIN → commit, rechecking eligibility at each decision. Only after clearing the held slot does the core cure the active illnesses and begin treatment immunity. Saving during treatment preserves the pending dose; saving after treatment cannot replay it. The Doctor station has its own secured inventory-consuming treatment transaction.

Food, drives, DNA and CWE gifts retain the same ownership/consideration pipeline. Fullness, fruit restrictions and Heart Fruit breeding eligibility are checked again at commit. A refused stack retains its components and returns once; unknown items are held, examined and tossed with a no-repickup grace period. Hungry pickup gives real usable food priority over unfamiliar clutter. The optional CWE stronger-fruit table is selected once at the actual consume point; it never stacks with the ordinary table. Monster and rare-parts settings similarly enter the authoritative training calls.

## Physical toys

All 24 registered toys are recoverable placed blocks. The original eight IDs are retained. Selection is voluntary and requires enough food/energy, no active care/lesson/activity and no illness. Learned instruments require their lessons. The Shovel requires the race reward and actual sand below it. A block is reserved by one nearby Chao; completion has one reward and a personal cooldown. Reload or block removal cancels an unfinished session without inventing copies or rewards.

| Toy family | Actual interaction |
| --- | --- |
| Ball | Kick moves the one real block into a supported empty cell; the Chao navigates after it. No ball items/display copies are spawned |
| Rocking horse, coffin | Chao occupies the prop during its session, then returns to the saved adjacent standing cell; breaking/reload restores normal gravity |
| Lifesaver | Waterlogged ring, real floating Chao, clear shore approach and safe dismount |
| Rubber duck | Waterlogged floating prop, shore approach and splashing |
| Radio, TV, jack-in-box | Placed animated frames, sounds and watching/surprise gestures; jack-in-box has the source 30% fright chance, with Crybaby crying instead |
| Piano, organ, eleven learned instruments | Animated play, distinct instrument sounds and notes; learned handheld performances can form bands |
| Bubble wand, shovel, rattle | Bubbles, visible sandcastle/play frames, and child-weighted rattle enjoyment |

The ring/duck need water placed below a clear bank; they do not magically fill a dry garden pool. Mounted actor and gravity recovery are persisted, including cancellation on reload.

## Annual birthdays

CWE describes an adult age-year birthday, nearby awake guests, a party hat/cake and a 25-second host wait. The year length is not specified there. `birthdayYearTicks=24000` therefore deliberately maps one loaded Minecraft day to one adult year; zero disables birthdays. This is independent of configurable life duration.

A host spends its annual saved credit before starting a 500-tick party. Nearby awake Chao navigate in, clap/cheer and receive a single social completion. The party hat is temporary render geometry; paid equipment is untouched. At the end of the wait one server-tagged vanilla cake serving enters the ordinary held-food THINK → EAT pipeline. A hungry Chao receives 25 fullness with no skill XP; a full Chao returns the real serving. The cake presentation and food amount are explicit Minecraft adaptations, not a claimed source nutrition table. The bounded once-per-year credit survives reload; interruption cannot repeatedly create cakes. Rebirth resets the adult calendar and guest token.

## Lifecycle ownership

First evolution keeps its cocoon. Rebirth and natural death now also have real 200-tick cocoons before replacing/removing the body. Rebirth creates one egg and transfers exact component-preserving stacks from all five paid cosmetic slots plus friendship history. Only a successful world spawn clears/discards the old actor. A failed egg creation retains the actor/items and retries later. Natural death returns equipment once, including when ordinary mob loot is disabled. Core rebirth keeps a random subset of acquired parts and unlocked headwear eligibility; egg hatching clears the old cocoon timer.

Incomplete Omochao stages are actual visible, owned prototypes with care/activity/capture blocked and simulation frozen. They cannot age, become hungry, accept another player's gifts or enter ordinary garden AI before construction finishes.

## Server verification

Behavior GameTests exercise actual two-Chao navigation and talk completion, the 5/10 friendship gates, interruption/reload non-replay, real medicine consumption across a save, illness remaining active during symptoms, waking disturbances/natural wake, actual orchard shake → drop → pickup → held eating, full cocoon → egg → hatching equipment retention, and birthday wait/guest/cake non-duplication. Toy tests cover all 24 real BlockItems, animation/voluntary navigation, hunger refusal, break/reload cancellation, actual moving-ball chase, mounted dismount, and a waterlogged lifesaver session. Existing exact gene-effect, ownership and capped-drive tests remain in the server suite; lifecycle assertions now observe the full cocoon and retained rebirth equipment.

The root release validation record contains actual build and GameTest results. This implementation note is not a substitute for those results.
