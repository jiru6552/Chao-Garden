# Chao Garden 0.25.47: real Solas and Accessories checks

Modrinth access is now working. The previous checks that were blocked by downloads were completed using the actual libraries and exact requested shader pack. This update retains all organ, guitar, equipped-flight, back attachment and running changes from 0.25.46. Its additional gameplay-code change is limited to the lobby renderer.

## Verified runtime

| Component | Version |
| --- | --- |
| Minecraft / NeoForge | 1.21.1 / 21.1.219 |
| Java / Lionfish API | 21.0.9 / 3.1 |
| Solas Shader | Exact V3.7b ZIP, unmodified default options |
| Iris / Sodium | Official NeoForge 1.8.12 / 0.6.13 builds for Minecraft 1.21.1 |
| Accessories / owo | 1.1.0-beta.54+1.21.1 / 0.12.15.6-beta.1+1.21 |

Published SHA512, sizes and archive CRCs were checked. Accessories and owo also match the earlier pinned SHA256 values. [Exact versions, URLs and hashes](real-mod-versions-02547.json) are included for reproduction. No third-party binaries, shader code or loader compatibility checks were modified.

## Additional shader fix

The real Solas run showed that stairs, landings, guards and tubes were visible with the 0.25.46 rendering changes, but exposed horizontal stripes on one view beneath the Dark landing. Ray tests against the actual emitted geometry found the last tread and landing sharing exactly the same top, bottom and star planes. A combined color/depth draw let those overlapping transparent fragments fight for depth.

The late scene now blends all sorted color layers without depth writes, then renders the same geometry with only depth writes enabled. Solas still receives the nearest foreground depth for fog and compositing. The depth pass masks every color attachment, uses the same shader/vertex/alpha path and reuses the same frozen energy phase. The early main-scene mode remains unchanged. No stairs, floor panels, fences, landing surfaces, portals or collision were removed or rebuilt.

A valid same-angle Solas before/after capture shows that the horizontal stripes are gone while the broad landing underside, treads and guards remain visible. The final 0.25.47 client run completed all ten camera views with exact Solas V3.7b active, and all ten matching views with shaders disabled but Iris/Sodium still loaded. These include both flights, landings and undersides, views through guards, and tubes against portal/floor/sky; the final captures were visually inspected. The test helper now waits for actual lobby-rendered frames with no loading screen or overlay before capturing; loading-screen images are not counted as visual evidence.

## Actual Accessories coverage

The real-client study completed seven scenarios: baby with ordinary elytra; adult maximum companion glide; finite glide and expiry; baby denied companion flight then carried during creative flight; actual-water swimming; ground/head return; and Hero companion flight. Native jump input reached the integrated server, armor stayed equipped, ordinary elytra continued normally and the real companion layer rendered. This uses the actual Accessories slot and callback, not the earlier attachment-only fixture or synthetic permit study.

The focused real-mod dedicated-server suite passed all **36 required tests**: 24 Drive checks and 12 equipped probes. The three Drive tests that previously failed for missing Accessories now pass. Coverage includes native elytra with and without a companion, finite budgets and landing reset, packet/launch replay resistance, water/potion preservation, robot boost cooldowns, numerical combat damage, defeat/recovery, care, and a baby slot/unequip/native-release roundtrip preserving state and UUID.

Six older assertions or fixtures were updated to the current intended rules: babies may equip but do not grant adult flight; Run0 adults return to HEAD after flight; free-following ability fixtures explicitly earn the existing Run milestone, and water fixtures earn Swim; ordinary chest elytra carries the Chao on the back. The elytra probe then removes the actual accessory and checks that vanilla flight continues. Numerical damage, bonus, cooldown and budget assertions remain. No ability code, unlock threshold, budget or save schema was changed to make these tests pass.

Two further tests passed in separate server JVMs using the same disposable world. The actual player file and Accessories attachment restored the same Chao UUID, full care/genetic state, paused age, partial care clock, opaque entity data and a pending named/damaged vanilla sword. These tests require no foreign item or identity stub. The broader variant ability suite uses identity fixtures for optional mod gates; it does not establish full-modpack integration.

The earlier 106 focused native tests and mechanics/pose/piano regressions remain recorded in [the original fix report](FIXES_02546.md). Their gameplay implementations are retained.

## Reproduction

Place the verified Accessories/owo JARs in `dev-mods/accessories` and Iris/Sodium in `dev-mods/shaders`. Keep Solas in the test client's `shaderpacks` folder and select it through Iris. Use Java 21 and the existing wrapper:

```sh
bash gradlew --no-configuration-cache runGameTestServer -PchaoAccessoriesTest=true -PchaoAccessoriesDir=dev-mods/accessories -PchaoVariantFixtures=true -PchaoGameTestNamespaces=chao_drives43,chao_equipped46 -PchaoGameTestDir=run-real-mod-tests
bash gradlew --no-configuration-cache runClient -PchaoAccessoriesTest=true -PchaoAccessoriesDir=dev-mods/accessories -Pcompanion046Study=true -Pcompanion046Dir=run-real-companion
bash gradlew --no-configuration-cache runClient -PchaoShaderTest=true -PchaoShaderDir=dev-mods/shaders -PlobbyFixStudy=solas37b -PlobbyFixDir=run-real-solas
```

For separate-process persistence, run `chao_equipped46_save`, then `chao_equipped46_load`, with the actual Accessories flags and the exact same disposable game directory. Do not erase the directory between runs. Native probes, fixtures and visual helpers are excluded from the production JAR. Normal in-game previews remain `/chao testtoy organ once|loop|stop`, `/chao testtoy guitar once|loop|stop` and `/chao testcarry once|loop|stop`.

## Limits and in-game checks

The cloud client uses Mesa software rendering and a silent OpenAL sink. Captures establish actual scene rendering with Iris/Solas enabled; they do not establish physical-GPU frame rates or every shader option. The shader pack's bloom makes exit spiral centers and stars brighter; the colored outer spiral rings remain visible. Listening, independent multi-client multiplayer and a complete user modpack were not tested.

In your usual pack, check organ/guitar hand contact, baby back attachment while swimming/elytra/other flight, and high Run in motion. With Solas V3.7b, look through and below both stair flights and guards, and check the tubes against the portal, floor and sky. Use matching Chao Garden 0.25.47 JARs on client/server and Lionfish API 3.1 separately; Iris/Sodium/Solas are client-side shader components.
