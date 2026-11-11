# Chao Kindergarten, 0.24

The Kindergarten is a shared protected room in `chao_garden:kindergarten`. Enter through the blue arch in the hub. Right-click a staff member, carry a Chao into the room, and select it in the service journal. Nearby owned Chao are also selectable. Unowned Chao can be claimed through ordinary garden care first. Every service checks the player's ownership, dimension, distance, alive state and selected inventory stack again on the server.

| Place | Functional service |
| --- | --- |
| Classroom | Current lesson and next rotation, real drop-off, four shared places, countdown, finished-student pickup and first instrument awards |
| Doctor | Three medical charts; free treatment of all six existing illnesses; existing paid grade-care action beside eligible stats |
| Principal | Eight selectable care lessons covering needs, training, growth, gradual evolution, class, reincarnation, clothing and companions |
| Fortune-Teller | Server-selected lucky names, another suggestion, accepting the suggestion and a custom name |
| Bulletin Board | Right-click the decorated vanilla board for the actual current lesson, next rotation, occupancy and directions |
| Black Market | Persistent resident using the ordinary real stock, currency, limits and refresh system |

The charts display actual saved ability grades, levels and points, personality tendencies, preferred fruit shape, loaded age, reincarnations, illnesses and needs, earned medals and karate tournament wins. Bout losses and draws are explicitly described as unrecorded because the current contest system does not store those counters. This release does not pretend to have the original game's complete per-bout chart. The robot and genetics details remain available in the existing Analyzer.

## Class ownership and persistence

A held Chao is consumed only after its same-UUID classroom body is admitted. Class cannot be withdrawn early, captured, gifted, petted, injured or used for remote care. A normal student keeps its original movement/gravity flags for its eventual carried snapshot. Its ordinary aging and needs pause during boarding. The existing learning worker grants the actual action/instrument unlock when the countdown finishes; it grants no intelligence or ordinary stat points.

Four places are shared by all keepers. A durable overworld `SavedData` ledger owns each student; its visible body is an unsaved projection. A namespaced NeoForge chunk controller keeps exactly the two classroom chunks ticking while any student is boarded, including a completed student awaiting pickup. Lessons therefore continue while a keeper visits another dimension or logs out, while the server is running. Six independent resident-room chunks also remain active while visitors are present, so staff and saved market stock finish loading even with a short server view distance. These resident tickets reconcile on every room entry and each second, release when the room empties, and are cleared by a load-validation callback after a crash. Pre-release duplicate residents are reconciled only after their saved entity chunks load; an active customer or demonstrably purchased market stock takes priority over a fresh actor. A restart reconstitutes missing projections from the ledger. Server-offline time does not advance a lesson. Completed students stay safe until their keeper returns; they are never automatically sent to an absent player's inventory.

Pickup requires a free inventory slot. It inserts a complete filled carrier with the same UUID, removes its classroom ledger/body, then delivers separately committed instrument awards. Any overflow award drops beside the keeper. Repeated pickup requests cannot create another Chao or instrument. Names and carried state are changed directly in their original inventory item; treatment and naming do not create a second living creature.

## Sources and adaptations

Primary references are Chao Island's [Kindergarten overview](https://chao-island.com/info-center/kindergarten/kindergarten), [Classroom](https://chao-island.com/wiki/Classroom), [Health Center](https://chao-island.com/info-center/kindergarten/health-center.html), [Principal's Room](https://chao-island.com/info-center/kindergarten/principals-room.html), [Fortune-Telling House](https://chao-island.com/info-center/kindergarten/fortune-telling-house) and [Bulletin Board](https://chao-island.com/info-center/kindergarten/bulletin-board.html).

The modern Classroom wiki states ten-minute lessons, thirty-minute offers, progress within Chao World, and five Song/Drawing lessons. The older Info Center classroom text instead states thirty/sixty minutes and outside-world-only progress, and the overview also gives an older three-stage mastery description. This implementation retains the existing configurable modern 10/30-minute timing and five stages. Keepers can remain in Minecraft Kindergarten or leave it while class runs. The room's four shared places, preserved existing instrument expansion, paused needs during boarding, persistent ticking chunks and physical instrument rewards are explicit Minecraft adaptations.

The Doctor's free cure and three chart categories follow SA2 Battle. Existing paid grade care is preserved as the mod's extension rather than attributed to the original health center. The Principal uses original paraphrased guidance for this mod's actual mechanics. Fortune suggestions are a small server-side pool; custom names are limited to 24 characters and control/format codes are removed. All services work without optional decoration mods. The old Lesson Desk and Doctor Station IDs remain valid for existing saves; new recipes and creative entries are removed so new players discover these services in Kindergarten.

The protected rooms do not yet add the other hub gardens, playable downloadable bulletin posts, original game voice dialogue, or a complete original-game combat-bout history. They do not require external decoration dependencies.
