# Coffin retirement — 0.25.49

The coffin is no longer an active toy. It is absent from market stock, victory rewards, toy registration/selection, creative inventory and recipes. Its active model variants, textures, loot table, animation paths and asset-regeneration command are removed. Historical source extraction metadata and previous release notes remain as provenance, not runtime content.

The two old registry names are intentionally retained as inert compatibility entries. Removing them outright can make Minecraft discard a placed block palette entry or a saved item before the mod can inspect its custom components.

- An already placed coffin becomes a small neutral smooth-stone marker. It retains the old `facing`, `playing`, `frame` and `waterlogged` properties so every old saved palette combination decodes. The marker has no collision, toy behavior, animation or loot. It can be broken manually. Existing waterlogged data remains water; no chunk scan or automatic block replacement occurs.
- A stored coffin stack becomes a hidden, non-placeable ordinary item with a neutral paper icon and a retirement tooltip. Count, custom name, custom data, lore and other item components are left to the original Minecraft stack codec without conversion or inventory sweeping. It can remain inside nested containers without rewriting surrounding items. Players may discard it normally.
- A previously saved Chao mount is cancelled through the existing generic safe dismount/recovery path. Saved care/genetics and unrelated entity data are retained, and no completion reward is granted. The original gravity and AI settings are restored. If every exit is obstructed, recovery remains pending until clear supported ground becomes available; this prevents suffocation or a forced teleport into a wall.
- Existing market offers that contain the removed catalog ID fail the existing saved-stock validation and restock on load. A historical `reward.coffin` trait remains ignored save history; it does not regrant an item or overwrite other traits.

The compatibility IDs can still be referenced explicitly by operators or external datapacks. They resolve to inert legacy data, not an obtainable or usable Chao toy. This update does not scan unopened containers, force-load chunks, erase stored stacks or migrate worlds backward to an older JAR.

## Verification supplied to the release coordinator

Private Java 21 compilation passed for the compatibility classes and native tests. The focused activity reward checks passed 174 assertions; 500 market selections contained no coffin. Three asset checks verify absent old meshes/textures/loot/recipes/tags and neutral compatibility models. All remaining OBJ files were checked before removing seven material definitions used only by the old coffin.

Eight native GameTests under `chao_retired49` cover hidden/non-placeable registration, component-preserving item and nested-container codec round trips, all 64 legacy block-state combinations, no natural goal selection or rewards, saved mount recovery, deliberate gravity/AI flags, sealed recovery across a second reload, and saved merchant offer invalidation. All eight tests ran and passed on the dedicated NeoForge GameTest server during the 407-test release run. These are actual codec, registry, world-block, recovery and merchant checks; an existing user world was not opened or bulk-converted. See TOYS_02549.md for final client/build results and limits.
