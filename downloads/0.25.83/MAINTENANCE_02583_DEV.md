# Chao Garden 0.25.83 maintenance report

This maintenance pass fixes verified problems in AI, equipped companions, care throttling and resource caches, and removes repeated work from morphology rendering and optional integration. It preserves the existing gameplay, fixed head geometry, morphology assets, saved identifiers and dependencies. No production asset or registered content was deleted.

The final **NeoForge 1.21.1** JAR is **30,688,961 bytes**, SHA256 **`1032b086242deba5c5babc346ad9286247d83f644935d1dc370a51fd33d6492e`**. A clean build executed all 80 tasks successfully; the final complete native server catalogue passed **4,884/4,884** tests.

| Theme | Survey and result | Scope of verification |
|---|---|---|
| AI and ticking | Reviewed interaction goals, priorities, fruit harvesting and social completion; fixed remote harvesting and repeated orchard path probes | Six new native goal/harvest regressions; existing toy and AI catalogue retained |
| Gardens, care and persistence | Reviewed garden data, recovery/storage, care authority, lesson state and synchronization | New care regression, full catalogue, copied older-world upgrade and actual two-client checks |
| Equipment | Reviewed actual slot authorization, ownership, energy and optional APIs; fixed exhausted innate cleanup | Original Nether regression unchanged and passing; new End regression; real Accessories and Aether runtimes |
| Rendering and attachments | Reviewed 302 client files, morphology preparation, layers, caches and resource reload | Six new cache assertions; representative production-renderer screenshots and state checks |
| Build and tests | Repaired obsolete morphology asset pins and unsafe validator output paths | Successful clean build; historical geometry preservation audit; Python fixture/CLI tests |
| Resources and content | Audited model/texture/audio links, recipes, loot, aliases, duplicates and archive boundaries | 8,336 source resource files, zero broken-reference errors; representative visual checks; 46 audio files decoded |

The survey covers these systems and their boundaries; it is not a claim that every possible gameplay state or artistic detail was exhaustively inspected.

## Fixes and bounded optimizations

**Fruit seeking and harvesting.** A shaking Chao must remain within the existing squared work distance of nine, both during continuation and when committing its harvest. Shoving or moving it away now cancels work without remote fruit or care rewards. A synchronous search checks each resolved tree root once: the tested unreachable four-pod tree makes **four failed side-path requests instead of sixteen**. Loaded-chunk checks precede adjacent stand-cell access and stale target reads. The original 2,025-position search remains; this is a measured reduction in duplicate probes, not a general fourfold pathfinding speed claim. The stale-target native test also confirms that a far unloaded target stays unloaded; the adjacent stand-cell branch was inspected separately rather than isolated in its own runtime test.

**Care and lessons.** The four-tick care throttle previously persisted a dimension game clock, so a future timestamp could prevent legitimate care after a rollback or clock mismatch. It now weakly tracks the actual server player using server ticks, rejects client calls, and leaves the historical saved tag intact but unused. Ownership, distance, station and action checks remain. Inactive lessons now return before parsing an absent remaining-time field or searching the lesson catalogue. A late review caught an intermediate reorder that caused caught parse exceptions for empty lessons; the corrected guard was included in every final rerun. Global trait parsing and the save schema remain unchanged. The native regression checks inactive, active, expired and unknown lessons, the old future timestamp, same-tick denial, acceptance after four ticks, and an actual earned-medal transaction with codec preservation.

**Exhausted equipped companions.** Authorized adult Nether/End theme companions now clear their innate fire/levitation condition before the low-energy return. The inherited Nether bug allowed immunity to work while an existing fire timer remained. The original Nether test's body and annotation are unchanged and now pass; a new End test verifies removal of existing levitation while retaining unrelated effects. Baby/preview exclusions, slot authority and energy limits on ordinary trained benefits remain. The causal audit distinguishes this production fix from a changed test expectation.

**Optional Accessories integration.** Nine reflection method handles are resolved once during Accessories-present setup. Slot/capability/stack contents are still read live; no typed optional-class linkage or dependency was added. This removes repeated metadata discovery from lookup and notification paths without caching mutable equipment. Public signatures were checked against beta54 and the Aether-bundled beta48; actual final optional-runtime execution used beta54.

**Morphology rendering and attachment caches.** Sixteen appearance adapters previously evaluated a discarded `getOrDefault` fallback even on cache hits. They now sample only on misses. The initial prepared development path avoids **29 redundant target evaluations/snapshot allocations**; preparation, interpolation, invalidation and bounded blend storage are unchanged. No FPS/TPS percentage is claimed. Head/body fitting landmark caches now clear through the existing client resource-reload listener. Six numerical regression assertions cover stale/replacement landmarks. A changed custom resource pack's actual fitted rendering remains a manual follow-up.

**Validators and journal.** Nine Java morphology suites and ten Python validators contained two obsolete protected hashes from the intentional .72 reciprocal Neutral Swim/Fly completion. Only those two pins and explanatory comments were updated. An independent .71-to-.72 audit preserves all 59 original joints, fixed head/face vertices and UVs, and seven retained target rows in each affected family. Every other geometry pin and assertion remains. Four Hero validators now support explicit, non-overwriting `--output` paths; default execution prints results without replacing private historical evidence. Five CLI tests cover real entrypoints and overwrite refusal. The journal's garden paragraphs now have proper separation; its content check recognizes the two intentional .81 garden chapters, for **60 sections** and **10/10** passing checks. Existing written-book item components are not rewritten.

## Resources, models and compatibility

The final resource survey covered **8,336 files**: 8,335 inherited files plus a 112-byte development orchard fixture. It inspected 2,688 JSON/mcmeta documents, 5,046 PNGs, 140 native entity models, 503 OBJs, 46 OGGs, 12,444 own-namespace reference edges and 552 recipe/loot item references. All checked links resolve. The portable audit reported **zero errors and 60 contextual warnings**: 58 imported question/dizzy symbol UVs intentionally remapped by the loader, and two historical collapsed prototype additions. These were retained and documented rather than treated as new rendering defects.

All 46 OGGs decoded successfully through ffmpeg. This verifies decodability, not listening quality. PNG container/CRC checks likewise do not establish artistic correctness. The 1,329 duplicate-byte groups represent about 10.9 MB before compression; dynamic material paths, aliases, animation endpoints, tests, references, save compatibility and attribution prevent treating equal bytes as proof of dead content. They remain intact.

No registered items, blocks, entities, components, IDs, models, textures, animations, sounds, dependencies or saved content were removed. The development fixture and capture/test helpers are excluded from the installable JAR. The clean archive omits one orphan **empty directory entry**, `assets/chao_garden/textures/block/sa2_02515/chao_coffin/`; it contains no file or saved content. Every nonempty archive payload matches the final tested runtime assembly. The archive audit binds 718 production classes and production assets/data, checks unique entries and CRCs, and confirms all 19,103 frozen source/class/resource paths remain unchanged.

## Actual final verification

| Check | Result and practical limit |
|---|---|
| Clean build | `--no-configuration-cache --no-build-cache --continue clean build`: **80 tasks executed, successful**. Includes all nine previously blocked suites and accessory cache assertions. Exact source copy avoided concurrent mutation of the client/server runtime. |
| Full native dedicated-server catalogue | **4,884/4,884**, process exit zero; all 38 retained namespaces plus the new maintenance namespace. Includes evolution, opposing influences, persistence, controls, commands, toys, gardens, equipment, baby alignment and authorization regressions. |
| Minimal runtime | **7/7** focused maintenance cases, with Minecraft, NeoForge, Chao Garden and Lionfish only; no Accessories or Aether. |
| Optional Aether runtime | **14/14**, real Aether 1.5.10, Accessories beta54 and required dependencies; actual loader and dependency hashes recorded. Optional dependencies are not bundled into the release. |
| Python validators/utilities | Ten geometry validators passed; the four CLI-modified validators then passed full final runs. **8/8** resource fixtures, **5/5** output-CLI tests and **10/10** journal checks passed. |
| Wearables/poses client | **30 actual captures**, five baby/adult Neutral/Hero/Dark cases: look-around, thinking, ground-play, crayons and toy car, then recovered fronts. Fixed 7×6×6 heads, finite poses, codec/equipment retention and cleanup checked. |
| Toy lighting client | **32 actual captures**, held/placed pogo and Shadow doll in daylight and darkness. Actual zero-light scenes dim correctly; baked UV2 is zero with shading enabled and the engine receives the scene light. This is not GPU-buffer interception. |
| Older-world upgrade client | A separate copy of the .81 fixture world reopened successfully: four captures, both garden unlocks, two Chao identities/keepers/names and player inventory preserved; **3,492 critical cells** unchanged. Original 99 world files and seven settings were untouched. |
| Two connected clients with Aether | Actual host/observer TCP connection and **10 captures**. Real kill pipeline changed baby alignment +20→+21→+20; the adult Hero/Run identity and +80 alignment remained intact. Actual Aether travel and client acknowledgements verified a seeded 1199→1200 equipped-tick boundary. |

The two-client check used separate real processes and production synchronization/rendering. It was not a manual PvP session or a full wall-clock minute soak. Native dimension tests exercise 1,210 actual server ticks, but GameTests can tick faster than real time. Save tests include serialization and actual player-file logout/login; a partly elapsed .82 counter across fresh processes remains untested.

The selected visual sheets contain actual final client frames, uniformly resized with labels outside them. Representative equipment stayed attached during active/recovered poses; selected crayon/car views retained connected heads and limbs. Pogo/Shadow visibly dimmed in darkness. Matched network frames retained the equipped baby, and the older garden overview retained its unlocked stairs/portals. These are timed stills, not continuous motion or a review of every mature form/accessory combination. No new artwork or animation redesign was needed.

The evidence bundle retains unsuccessful and superseded attempts: nine inherited stale-pin baseline failures, three inherited journal assertions, an invalid template namespace, an exit-zero run with **no tests**, five navigation-fixture failures, an unsupported client phase, and two explicitly cancelled clean builds. Corrected final reruns above passed. Intermediate captures/results remain labelled intermediate. No final test failures remain; existing Gradle/FML deprecation warnings remain.

## Remaining focused follow-ups

- Test manual phone controls, public-latency multiplayer, manual combat and extended equipment/dimension sessions.
- Check custom resource-pack reload fitting, shader packs and hardware rendering. This pass used software graphics and default resources.
- Listen to sounds and overlapping toy interactions. Audio decoding and cue assertions do not substitute for listening.
- Review more morphology/accessory combinations in motion. The visual sample is representative, not an exhaustive artistic pass or a new viewer comparison.
- Back up and test an authentic player world before upgrading a live server. The successful controlled fixture upgrade is useful evidence, not a guarantee for every modpack/world.
- Larger audit findings remain intact for focused work: activity-roster pagination near the 64-slot/200,000-character packet boundary, guarded migrations of non-lobby authored realms, ambiguous third-party reward callbacks, and profiling broader empty-trait parsing. Their gameplay/save consequences did not justify speculative changes here.

The source ZIP is a cumulative overlay onto the complete .43 base, retaining all accepted .82 paths and the 21 unchanged inherited deletion guards; it is not a standalone checkout. The verification ZIP contains receipts, bounded log excerpts, audits and selected actual screenshots, excluding saved worlds, account data and third-party binaries. Use matching client/server versions and the existing external Lionfish requirement.
