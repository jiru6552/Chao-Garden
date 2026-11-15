# Chao Garden 0.25.81 — garden unlocks and effects

NeoForge 1.21.1. Builds on the accepted 0.25.80 project; this report distinguishes native tests, real-client observations and remaining migration limits.

## Unlocks and lobby changes

Hero and Dark start locked and unlock independently, permanently, for the whole server. This reuses `HubWorldData` in `chao_hub_visits.dat`, adding `GardenUnlocks081.Hero` and `.Dark`; existing return journals and scenery revisions remain intact. A real owned child qualifies at the existing +0.5/-0.5 alignment thresholds. Adults use their locked alignment. Unowned creatures, unfinished construction and unidentified stored states do not invent acquisition history. Live admission excludes dead states. Retained registry/recovery records with validated ownership and creature UUID, or identified terminal carriers with a Keeper, may still prove a past acquisition; this read-only historical path never revives or republishes a Chao.

The server checks both the saved unlock and actual installed access before garden entry. A forged or stale visible portal cannot open a locked side. Old visitors can still leave either garden: when its stairs are unavailable, they return to supported central lobby footing. Neutral access and safe central returns remain available while a side update is pending.

Locked approaches have no stairs or portal, and the Dark descent opening is filled with the original solid glass footing. Opening a side changes only the finite authored access footprint. Exact current states, block-entity contents and occupied supports are checked before any write; conflicts defer the whole transition. The scene anchor publishes the applied access mask last. Its vanilla block state persists and reaches existing clients and late joiners through normal chunk/block synchronization, so scene meshes and collision use the same committed state. The lobby revision stays 38; there is no room-wide regeneration during this migration.

Existing edited blocks, inventories and actors are preserved. Keepers must clear the affected path or restore edited cells before retrying. The automatic retry interval is 200 server ticks. An older layout with revision below 38 or a missing scene anchor is preserved instead of being rebuilt. If both revision and anchor are absent, any existing block or nonspectator actor in the construction footprint also refuses initial generation. Damaged central footing/headroom refuses visitor entry; the access-status command does not diagnose every central-floor defect. Unknown old layouts require a backed-up restoration of a compatible authored lobby **together with its saved revision and scene-anchor metadata**; restoring blocks alone and running rescan is insufficient. There is no force-rebuild command.

## Migration and operator recovery

Read-only acquisition evidence comes from existing owned garden-registry appearance records, loaded owned Chao, online inventory/ender chest, equipped/accessory storage, open menus/cursor, nested vanilla containers and bundles, public item-stack item-handler storage, already-loaded Container block entities, and retained owner/UUID-validated damage-recovery snapshots. Recovery tickets and deliveries are untouched. Unopened loot chests are skipped so migration does not roll their contents. Source chunks are not force-loaded.

Offline player files, unloaded sources with no retained ownership snapshot, finished recovery records whose appearance was discarded, inaccessible private mod storage and nesting beyond the supported bounds cannot reliably prove an old acquisition. Login, later chunk loads and future acquisitions retry automatically. Traversal is bounded and rotates through accessible roots: depth 4, 512 recursive inspections, up to 4096 container slots/menu roots (plus ordinary inventory/ender/accessory roots and the cursor), 16 block entities per queued chunk pass, two chunk works per server tick, and recovery snapshots consumed in batches of 128. Both unlocked flags stop further evidence scans.

Permission-level-2 commands:

- `/chao gardens status` — saved unlocks and guarded lobby-update status.
- `/chao gardens rescan` — retry accessible acquisition evidence and the guarded installation.
- `/chao gardens unlock hero`, `/chao gardens unlock dark`, `/chao gardens unlock all` — explicitly recover missing historical evidence. These remain subject to safe physical installation and do not overwrite player builds.

The new journal pages explain unlocks and garden growth without adding information about the existing hidden equipped abilities.

## Garden effects

The separate saved visual evolution-strength value is retained. The existing ordinary adult increment is `1 / (4 × configured childTicks)` per eligible loaded care tick, with the existing strength cap 1.2.

| Actual location | Adult passive visual strength | Child alignment nudge |
|---|---:|---:|
| Neutral Garden or another ordinary dimension | 1× baseline | None |
| Hero Garden | 2× baseline | +1 / configured childTicks per eligible care tick |
| Dark Garden | 0.5× baseline | −1 / configured childTicks per eligible care tick |

Dark **half speed** is the requested Minecraft adaptation, rather than the supplied SA2 reference's stopped passive growth. It remains positive. Rates use the current configuration; no wiki-derived wall-clock duration replaces it.

Fruit remains separate: a completed whole fruit adds the existing +0.005 adult or +0.02 child strength, irrespective of the passive multiplier. Normal food stat effects are unchanged. Childhood duration, stat levels, XP, points, lifespan, adult alignment and first evolution identity retain their existing rules. Adult specialization still reverses along Run–Power and Swim–Fly independently. Sleeping retains ordinary active care; cocoon and dead-state pauses remain intact. Existing lesson, activity, preview and construction care pauses also retain their rules.

The two existing care call sites resolve the Chao's actual dimension. Normal living entities use their ordinary clock; equipped bodies use their exclusive existing care path, with paused lifespan/birthdays and paused baby childhood. Equipped children can receive the environmental alignment nudge while that care runs. Stored items and unloaded Chao receive no background progress. Location changes do not re-credit old age at the new multiplier; old age-only saves backfill at the ordinary rate before new location-specific ticks.

Two optional appended codec fields store bounded double rounding residue for child alignment. All previous field indices remain unchanged; old saves initialize residue to zero. Tiny increments at the largest legal configured childhood therefore survive save/reload and float rounding without using or rerolling genes/traits.

## Verification

The available core suite passes, including 118 new garden-progression checks, existing Creative controls/commands, first-evolution selection/ties, opposing specialization axes, fixed-head morphology samples and exact historical face/DNA migrations. The focused dedicated GameTest cohort passes all 18 cases. The separate sequential fresh-server scenario passes independently; it cannot run together with tests that deliberately unlock the same permanent global flags.

The full 4,862-case native regression run completed with 4,861 passes and **one inherited failure**: `actualnetherslotprotectsfireandlavabutnotordinarydamage` — “Tired living Nether companion did not extinguish keeper and body.” It exited nonzero and is not reported as a green build. No new failure identity appeared. The final complete rerun after the orphaned-lobby guard and terminal-history correction reproduced the same 4,861/4,862 result and exact inherited failure; it also exited nonzero. The new garden cases passed in that final cohort, and the final isolated fresh-server scenario passed again.

New native coverage includes independent Hero/Dark acquisition, real locked portal admission, legacy visitor safe returns, permission denial, owner/stored/nested/recovery evidence, read-only loot preservation, saved journals, late-player access, atomic edited-block/chest/occupancy deferral, exact legacy-38 access migration and retry/idempotence. Progression fixtures exercise 15 real actors across the three garden dimensions, every ordinary adult alignment, separate fruit completion, mid-meal NBT reload, and an actual Accessories-equipped adult moving Hero→Dark→stored with one exclusive care clock and paused life counters. Core checks cover equipped babies, maximum configured timing/rounding, opposite changes, old clock backfill and strength-vs-age independence.

No fresh full-family morphology screenshot sweep was part of this garden change. Existing morphology assets, face/toy/equipment render paths and training rules are preserved; automated regression coverage does not prove every visual combination on every device.

The accepted .80 full-suite baseline had 4,844 tests and two failures: tired Nether companion fire protection and an intermittent hungry-tree fruit pickup failure in unchanged foraging code. Its unchanged 2,717-test recheck reproduced only Nether protection. The final .81 comparison will retain exact failure identities rather than treating a later recheck as erasing an earlier failure.

Initial .81 attempts found two test/helper compilation mistakes, old strict face/Chaos fixture lengths after the append-only codec addition, a geometry-fixture chest drop correctly triggering the occupancy guard, and a test-only access to a package-private revision map in the added orphaned-lobby case. The revision assertion now reads the real SavedData serialization. These were corrected without changing acquisition/progression semantics or weakening item-preservation and collision checks. The legacy face/DNA projection still compares the complete original saved bytes, with the three face fields and two new zero residue fields checked explicitly.

The final frozen runtime was exercised by an actual integrated-server host and a distinct TCP-connected observer who joined after Hero unlocked. All 28 native framebuffer captures and the live renderer/critical-block-state checks pass across fresh locked, Hero-only, protected-chest obstruction and both-open stages. The server denies permission-0 recovery, preserves all seven fixture diamonds while blocked, verifies their exact transfer before the controlled retry, and receives actual client dimension acknowledgments for Hero/return/Dark/return travel.

A separate Java process reopens that **same** normally saved world. Compressed `chao_hub_visits.dat` is read before opening, proving both flags already existed on disk before acquisition observers could run. The same two saved Chao UUIDs and installed access remain. Four matched native views and the restart audit are included. The root continued Minecraft's native experimental-dimension warning through ordinary mouse input; no world copying or SavedData editing substituted for restart testing.

I visually inspected the two final four-panel screenshot sheets (eight original native frames), plus the reopened overview. They show the correct absent locked approaches, solid closed layout, Hero-only access, unchanged blocked Dark approach, and the opened Hero/Dark routes. The first client cohort exposed stray Dark flame particles from an older ticker; the ticker now checks committed access and the actual portal block, and the final locked close view is clear. Other recorded views have automated framebuffer/hash/renderer checks, rather than a claim that every frame received manual judgment.

See `lobby-stages.png`, `lobby-details.png` and `restart-comparison.png`. These are composites of actual screenshots with outside labels, not authoring projections. Full frame ledgers and input hashes are in the verification bundle.

Progression's native NBT save/load and equipped dimension moves were tested; physical chunk-unload/restart growth-rate measurement and direct two-client strength-delta measurement were not. The multiplayer capture run verifies lobby state/appearance and portal behavior. Equipped-baby environmental alignment is covered by core tests, rather than an actual equipped-client slot case.

Real client captures use the declared development classpath. They are distinct from an installed-JAR playthrough. The delivered JAR is checked against recorded compiled production bytes. Software rendering and null audio are used; phone controls, hardware rendering and public-server latency remain user testing areas.

## Delivery

Files are supplied together as `chao_garden-0.25.81.jar`, `chao-garden-0.25.81-source-changes.zip`, `chao-garden-0.25.81-verification.zip`, this report, native screenshot sheets and `SHA256SUMS`.

Final JAR: 30,673,620 bytes, SHA256 `9e13c8650502c2631b0f5c60e9ebbbd3de4ec826d697bc1184f8ab7e69635adb`. All 8,995 recorded production class/resource payloads (including 709 production classes) match the final tested development runtime.

The final production archive is audited for unique entries, CRCs, NeoForge/Minecraft versions, excluded test/capture helpers, and exact production-class/resource binding to the recorded client runtime. The cumulative source contains 996 source paths plus four controls; it preserves all 968 accepted .80 source paths, 21 exact deletion guards and both inherited deletion controls. ZIP payload hashes and path/symlink safety are checked before delivery. Private verification includes actual process exits, test identities, selected numbered log excerpts, failed attempts, runtime/source freezes, native frame ledgers, restart evidence and root-inspected screenshot provenance. Saved worlds, account caches and runtime installations are omitted.

The cumulative source overlay retains every accepted .80 source path and all 21 inherited exact resource-deletion guards, onto complete .43 commit `fcde2f9d012499b2e2cb1ec1d50a7f58d4dc3ca2`. No publication or Git-history mutation is part of this delivery.
