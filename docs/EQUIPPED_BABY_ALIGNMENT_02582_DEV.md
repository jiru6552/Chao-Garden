# Chao Garden 0.25.82 — equipped baby alignment

NeoForge 1.21.1. Builds on the accepted 0.25.81 source and preserves the existing Chao state codec, garden effects, evolution locks, companion rewards and optional Accessories boundary.

## Behavior and configuration

The existing internal alignment range is −1…+1, displayed by the Analyzer as −100…+100. One displayed point is 0.01 internally. The server clamps every applied change to the existing range. Only actual babies qualify: adults retain both their public alignment value and locked evolved identity/first type, with no normalization side effect from a rejected influence. Dead, cocooned, unfinished construction and Omochao robot states do not receive baby effects; a genuine baby is not excluded merely for a separate color, shiny, character or immortality variant flag.

Defaults in the existing server config (`serverconfig/chao_garden-server.toml`):

| Key under `equippedBabyAlignment` | Default | Valid range |
|---|---:|---:|
| `hostileKillPoints` | +1 Hero point per credited hostile death | 0…100 |
| `peacefulKillPoints` | −1 Dark point per credited peaceful/villager/other-player death | 0…100 magnitude |

Zero disables its kill influence. These values are displayed alignment points, not internal float units or numerical training levels. This new mechanic has no effect on stat rewards, lifespan, first-evolution selection or adult specialization.

While actually equipped in the Nether, each completed 1,200 eligible server ticks gives one Dark point. Without classic Aether installed, the End gives one Hero point per 1,200 ticks. When mod ID `aether` is installed, only `aether:the_aether` supplies that passive Hero point: the End stops supplying it, even if the Aether dimension is disabled or not yet visited. Detection uses ModList and exact ordinary dimension IDs, without Aether-only class descriptors or a required dependency. At 20 TPS, 1,200 ticks is one minute; server lag slows simulation time, with no wall-clock catch-up.

The two partial minute counters pause independently outside their relevant dimension or when unequipped. Time in one dimension cannot finish the other dimension's partial minute. A minute at a clamped endpoint is consumed rather than banked for a later edit. Missing legacy fields initialize to zero; malformed counters reset safely. The counters, duplicate-tick markers and a per-server session UUID live inside the existing saved Chao entity persistent data carried by its real accessory item, not a new player/global care system. The server tick counter is stable across Pre/Post callbacks and dimensions; the session UUID scopes its duplicate markers across restarts without resetting earned partial time. Reconnects do not reset or replay elapsed wall time.

## Ownership, death handling and classification

Kill attribution reuses `EnemyDriveDrops.credited`: vanilla recent-player/projectile/tamed-pet credit, plus the existing companion-owner mapping. Thus a recent player can receive an environmental finishing death if vanilla still assigns that player kill credit. Self deaths are excluded. The responsible player must be the actual current online server-player instance, with one authorized real Accessories carrier. Ordinary inventory, ender storage, empty/stale slots, ambiguous duplicates and conflicting live creature UUIDs do not qualify.

The authoritative managed body is read before checking baby eligibility (including a temporarily hidden/non-ticking projection tracked by the existing Active record), so a stale baby item cannot override an evolved adult. Before the first summon, during recovery or when no body can be placed, the authorized actual slot can still receive these new equipped effects through its saved body data, provided no conflicting live identity exists. Existing pending-removal/defeat snapshots are resolved using the same companion transaction before writing. Point changes immediately update the body/entity tracking and/or actual accessory container. An unrelated live body or preview never becomes a fallback editing target.

A narrow observer at the return of NeoForge's `CommonHooks.onLivingDeath` reads its final cancellation result; it does not change that result, loot or XP. This handles later-priority vetoes before alignment changes, while reading the actual slot and responsible player at the accepted kill moment. A persistent per-victim death commit rejects repeated callbacks and reloaded old death sequences. A genuinely alive new/revived entity clears that old commit on join or incoming damage. The actual dragon's initial lethal hit passes through this hook before its health-1 DYING phase; delayed experience bursts must not re-credit a later-equipped baby.

Classification is intentionally narrower than the existing Chaos Drive reward policy:

- Other players and AbstractVillager types (including wandering traders) always mean Dark; self kills are ignored.
- Verified ordinary passive vanilla types are listed explicitly, along with friendly Chao and the mod's trader/staff/pupil, plus optional verified Aether flying cow, phyg and sheepuff IDs. Friendly Chao classification is a deliberate policy; their existing limited nonlethal bullying is preserved.
- The vanilla killer rabbit is the explicit hostile rabbit variant.
- For non-player/non-villager types, an explicitly ignored tag opts out. The killer-rabbit special case then wins before ordinary tag resolution; for the remaining types, conflicting hostile/peaceful tags mean no alignment and one explicit override wins before inference.
- Untagged NeutralMob creatures, ordinary piglins, spiders, tamable creatures and NPCs are not inferred hostile. Otherwise an `Enemy` or `MONSTER` declaration is a defensible hostile declaration, including modded entities.
- Unknown modded animals, predators and other creatures are not guessed peaceful from `Animal` or `CREATURE` alone.

Modpack entity-type tags are `chao_garden:baby_alignment_hostile`, `chao_garden:baby_alignment_peaceful` and `chao_garden:baby_alignment_ignored`, under `data/chao_garden/tags/entity_type/`. Players/villagers cannot be reclassified by those tags. The existing broader `chaos_drive_enemies` reward tag and all normal kill rewards stay unchanged.

## Independent care sources

The new dimension-minute clock uses the actual accessory callback with its own saved tick guard. The existing garden alignment/lifespan/visual-strength care remains in the exclusive live-body path, with a separate shared-care same-tick guard. No garden care is replayed from the accessory callback. Hero/Dark gardens retain their .81 child nudge and adult strength rates; neither is mistaken for the Nether or End/Aether minute. Existing preview/cocoon/dead-state pauses are retained. Stored items get no active minute progress. The new equipped influences do not advance baby maturity, age, stats or adult life counters.

## Verification

The final core suite passes, including **1,320 new equipped-baby checks** and all existing core checks. New checks cover displayed units/bounds, disabled and invalid deltas, all ordinary adult alignment/first-type combinations (including the public mirror), special baby variants, independent dimension clocks, cap consumption, repeated state reloads and garden coexistence.

| Actual native runtime | New cases | Result |
|---|---:|---|
| Accessories/owo, Aether absent | 14 | 14 passed |
| Official Aether 1.5.10 installed | 14 | 14 passed |
| Final complete regression, Aether absent | 4,876 | 4,875 passed; one inherited failure |

The final full run exited **1**, not zero. Its sole failure is exactly the accepted .81 failure: `actualnetherslotprotectsfireandlavabutnotordinarydamage` — “Tired living Nether companion did not extinguish keeper and body.” All 37 prior namespaces were selected, plus the new baby-alignment namespace; no new failure identity appeared. The older intermittent tree-foraging failure did not appear and is not claimed fixed.

The 14 native cases exercise actual accessory slots and accepted engine deaths, late-priority death cancellation, repeated deaths, no-credit/unequipped/foreign-owner/conflicting-UUID rejection, adults, immediate pre-summon and recovering baby carriers, two-player recent-hit/projectile/tamed-pet attribution, normal drive rewards, the dragon's initial lethal hit and later XP bursts, a complete unaccelerated 1,210-tick Nether observation, End/Aether substitution, actual Aether Zephyr and Flying Cow deaths, serialized inventory pause/re-equip, normal player-file logout/login with the same player/Chao identities, and the existing Hero Garden child nudge. Both counter sources and ordinary care are checked against server ticks, including repeated real accessory callbacks within one tick.

Aether was actually loaded, not simulated by a boolean: loader logs select Aether 1.5.10, embedded Cumulus 2.0.7/Nitrogen 1.1.25, external Accessories beta54 rather than bundled beta48, and owo 0.12.15.6. The official JAR was checked against publisher SHA512/SHA1 and all CRCs (SHA256 `a999a9265eb550a46a0f8eedfee7c3c75371d7f6cf34b7c09ff800e48633e9f8`). The private GameTest preset first passes the project's unchanged exact-eight-definition guard, then appends the exact official Aether definition. This is a development fixture only; ordinary clients use normal mod datapacks and the release has no Aether dependency.

Early attempts are retained in the evidence bundle: incomplete fixture API/type compilation, the world-time Pre/Post duplicate-clock issue corrected to server ticks plus a session scope, initial dragon sitting-phase setup, asynchronous destination chunk/entity readiness, and the first private optional preset rejected before game launch. Early Aether runs recorded 13/14 because the valid living projection was hidden/non-ticking during the short headless readiness window. The final test permits bounded readiness time and verifies all legitimate whole points and residual ticks earned while waiting; it keeps the real body-registration/ticking/care assertions. Those failed attempts are not relabeled as passing.

Nine inherited morphology asset-SHA preflights were already stale before this update. A full Gradle `check` pass is not claimed. No morphology/model/texture changes were made; the applicable core and native regression suites and the release assembly ran.

Two distinct actual TCP clients ran with the official Aether installed in an ordinary development world, with no GameTest preset or forced chunks. All **10 native framebuffer captures** and relevant tracked-entity identity/alignment/adult/first-type comparisons pass. The server used the production death pipeline and actual accessory container. The same original baby was preserved in inventory during the separate adult fixture, then restored for the Aether boundary check.

| Stage | Replicated alignment on both clients |
|---|---:|
| Original baby | +20.00 |
| Credited hostile death | +21.00 |
| Credited peaceful death | +20.00 |
| Separate adult Hero/Run identity after both kill signs | +80.00, unchanged |
| Original baby after one Aether passive boundary | +21.00 |

After actual dimension acknowledgments, a registered and entity-ticking baby was seeded with a 1,199-tick Hero remainder. The next genuine production accessory tick (498→499 in this cohort) added one point and immediately committed the real carrier. Both clients observed the original UUID, +21.00 alignment, unchanged baby/adult-age counters and no numerical stat training. This is a **seeded boundary test**, not a claim that the clients waited an unaccelerated full minute. The native Nether test supplies the full-minute observation; the core and real Aether native tests check the shared timing rule and actual dimension selection.

The root inspected the six-panel `client-comparison.png` and the original Aether-frame thumbnail. Values in the outside labels come from each actual tracked-state receipt. Baby Chao are visible in their established head-carry position; this is an equipped-state/synchronization check, not a full new morphology screenshot sweep. All ten original captures have independent image hashes and production-renderer evidence. A normal world/player save completed. The native reconnect case uses the real saved player file; a separate-process restart of the new .82 counters was not run. No installed-JAR phone/controller or public-latency/PvP input playthrough is claimed; the player-death native fixture uses real vanilla kill credit and its native death pipeline because GameTest disables PvP.

## Delivery and limits

Release JAR: `chao_garden-0.25.82.jar`, **30,687,849 bytes**, SHA256 `233efbc642c6e40c46542c0a11f76c17d9322d7ebb851771167f6b162d6c7132`. It is byte-identical to the production JAR from the passing installed-Aether native cohort. CRCs, unique entries, Minecraft/NeoForge metadata, the new death observer and all existing mixins are checked; test namespaces and capture helpers are excluded. All **9,007 production entries**, including **718 production classes**, match the recorded tested development runtime. The 18,819 recorded source/class/resource paths stayed unchanged through the final client and regression runs.

Source changes, this report, an actual-client screenshot sheet, a verification ZIP, a delivery manifest and SHA256SUMS accompany the JAR. The cumulative source contains **1,008 source paths plus four controls**, preserving all 996 accepted .81 source paths and all 21 exact deletion guards.

Recommended remaining checks: normal mouse/touch combat and PvP in your own server/modpack; an uninterrupted wall-clock minute on your phone at its actual TPS; any custom neutral/predator/NPC taxonomy that needs explicit tag overrides; and a fresh-process restart of a partly elapsed .82 minute. The known Nether companion fire-protection failure remains a separate follow-up. No broad third-party modpack, hardware/shaderpack or mixed-version-client compatibility claim is made.

The source archive is a cumulative overlay onto the complete .43 commit `fcde2f9d012499b2e2cb1ec1d50a7f58d4dc3ca2`, retaining every accepted .81 source path, four application controls and the 21 inherited exact deletion guards. It is not a standalone checkout. Third-party Aether/Accessories/owo/Lionfish JARs and saved test worlds are not included in the source or evidence exports. No publication or Git-history mutation is part of this update.
