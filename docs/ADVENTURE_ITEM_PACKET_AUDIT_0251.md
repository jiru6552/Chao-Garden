# Adventure care-item packet audit — 0.25.1

The pinned Minecraft 1.21.1 / NeoForge 21.1.219 sources confirm that an ordinary Adventure client with `mayBuild=false` and no `CanPlaceOn` component can send care-item block-use packets and reach this mod's scoped permission bridge. There is no earlier Adventure check suppressing those packets. This is a source audit, not a claim that a new native-client care click was recorded.

## Pinned evidence

The locally cached, patched source/compiled bundle used for this review is:

`/workspace/tools/gradle-home/caches/neoformruntime/intermediate_results/sourcesAndCompiledWithNeoForge_ca7fc4496430cc387d97324f667f01662e2e9ae9_output.jar`

Size: 31,766,858 bytes. SHA256: `5f596379f9178c1f8ecfd4aa29de4da57d1d0dcda06e06a20b29797413b5067c`.

| Source entry inside that JAR | Relevant method line | Source-entry SHA256 |
| --- | --- | --- |
| `net/minecraft/client/Minecraft.java` | `startUseItem`, 1700 | `f9fe34d178746c3e999aa8e2bb0f0903ec6af58640fbfeb778d3e39dc7be96b2` |
| `net/minecraft/client/multiplayer/MultiPlayerGameMode.java` | `startPrediction`, 261; `useItemOn`, 291; `performUseItemOn`, 305 | `f92ae7b5357b1ac7dc5d5e2e0293d9ff754b929db9b1c79f5febd21db09ea863` |
| `net/minecraft/world/item/ItemStack.java` | `useOn`, 357; `onItemUse`, 370 | `b35770a757f0c62a88276a38c36763d26e204d1a6210ea3fcac71a53526bc389` |
| `net/minecraft/server/network/ServerGamePacketListenerImpl.java` | `handleUseItemOn`, 1102 | `ec075b7115af7e755f209d1b459f80a61deb89f0069fdfffd8e31a2782064754` |
| `net/neoforged/neoforge/common/CommonHooks.java` | `onPlaceItemIntoWorld`, 594 | `1c01b3c4aabdfeb9b0bc74ddbe3f481bc1762ce8e5e4cfb550417fd7e0f0b929` |

Line numbers describe this exact cached bundle. The release's version pins are in [gradle.properties](../gradle.properties); this development source bundle is not redistributed in the mod.

## Actual order

1. `Minecraft.startUseItem` handles a block hit by calling `MultiPlayerGameMode.useItemOn`. It checks ordinary input/feature state, but has no Adventure/CanPlaceOn precheck.
2. `useItemOn` checks the world border and starts prediction. Its prediction callback runs `performUseItemOn`, stores that result, and unconditionally constructs a `ServerboundUseItemOnPacket` afterward. `startPrediction` sends that packet. A normal prediction result of PASS or FAIL does not suppress it.
3. `performUseItemOn` runs the normal NeoForge right-click and block/item phases. `ItemStack.useOn` posts `UseItemOnBlockEvent` with `ITEM_AFTER_BLOCK` **before** the client `onItemUse` Adventure permission gate. On the server, the same event is posted before `CommonHooks.onPlaceItemIntoWorld` performs its Adventure gate.
4. `HubContent.gardenAdventureUse` therefore receives the real client and server event. For the bounded Garden whitelist it grants `mayBuild` only around the synchronous nested native `ItemStack.useOn`, restores the previous value in `finally`, and returns the native result through the outer event. The recursion guard and the already-temporary permission prevent a second bridge invocation. No item component or game mode is rewritten.
5. The server packet handler validates reach, the hit location, feature flags and world interaction permission, then invokes `ServerPlayerGameMode.useItemOn`. These checks remain authoritative; the client does not grant itself server permission.

The whitelist covers Chao eggs, held Chao carriers, allowed garden BlockItems such as toys, and the Garden Bell only when clicking an actual garden provider. Other blocks, out-of-bounds uses, cooldowns, spectator use and other cancellation hooks retain their ordinary handling.

## Separate runtime evidence and limits

The actual server GameTests exercise native `gameMode.useItemOn` for egg placement, same-UUID carrier release and toy placement in Adventure, with no persistent build permission. They also exercise item/placement cancellation, protected terrain and native Garden Bell linking. Relevant sources are `GardenWorldGameTests025`, `GardenAdventureGameTests0251` and `GardenRegistryMigrationGameTests0251`; their current results are recorded in the final release's server reports.

This note closes only the suspected client packet-suppression gap. It does not invent a recorded native care click, certify third-party input/interaction mods, or claim that intentionally cancelled or out-of-reach actions should succeed. No application change or extra runtime invocation was required for this audit.
