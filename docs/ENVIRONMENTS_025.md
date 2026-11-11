# Chao World 0.25 environment implementation

Primary visual source: the two supplied 2560×1440 recordings, extracted without rescaling. `SONIC ADVENTURE 2 2026-11-01 13-35-12.mp4` supplies the lobby at 0:00–0:17 and first Neutral Garden throughout 0:20–7:05. The floor close-up at 0:07 shows the cloud/star underside and inverted wooden posts. Garden entry/cave, palms and flower beds are clear at 0:22 and 5:50; waterfall shelves are clear at 5:10–5:20 and 6:20. The other recording supplies the Kindergarten and is documented by that module's owner.

The lobby is a collision-supported translucent circular mesh, with thin pale blue radial/grid lines and three broad gold rings. There is no opaque checkerboard foundation or enclosing roof. A custom unlit material draws the celestial backdrop, reflected wooden rim/posts and star panels below the glass. The material does not alter global fog, projection, blending, culling or depth state outside its RenderType, and geometry is cached. The gold/cyan gazebo, animated energy columns, blue/cream/red Kindergarten arch and natural stone Garden arch are authored native renderer geometry. Only their collision footprint is stored as invisible blocks. The Hero stairs are walkable; Hero/Dark gates remain closed for future work.

The first Garden is a separate daylight dimension with a broad grassy basin, pale tiered cliffs, a real cave, eleven fruit-bearing palms, blue/pink flower clusters, a sandy bank, an actual water-filled pond and connected six-tier waterfall. The existing functional Garden Transporter sits on the left ridge. The central care lawn, x −6..6 and z 0..11, is level at ground y79. No Chao or eggs are granted or spawned by scenery generation.

Palm fruit uses the existing Garden Fruit pod, ripening stage, harvest drop and item. A narrowly tagged crown support and adjacent trunk satisfy its survival rules. Right-clicking a palm trunk shakes its ripe pods; clicking a ripe pod also harvests normally. Player fruit trees, toys and activity blocks survive future bounded scenery revisions.

## Coordinates and routes

| Realm | Authored bounds | Arrival | Return route |
|---|---|---|---|
| Lobby | x −27..28, y56..108, z −27..28 | (0.5,80.1,0.5) | Step out of, then back into, center gazebo |
| Neutral Garden | x −53..54, y66..119, z −51..48 | (−26.5,80.1,−24.5) | Walk into rear cave, x −29.8..−23.2, z −39.5..−35.5 |
| Kindergarten | x −37..38, y76..103, z −34..52 | (0.5,80.1,40.5) | Courtyard gate x ±2, z47..50.5 |

The Lobby's north Kindergarten arch is z−24.5..−21, x ±2 about x0.5. Its south natural Garden arch is z21..24.5. Portal arrivals retain the existing forty-tick latch, require stepping out before retriggering, and preserve the original persistent return journal. `/chao return` remains an emergency route to the original dimension/location/mode. Canceled travel restores the source mode and retains the journal.

Hub and Kindergarten visits use Adventure mode. Garden admission uses protected Survival so actual egg/carrier use, seeds, toys, Garden Blocks, race components and karate mats work through Minecraft's normal server interaction paths. Original creative administrators retain Creative only in the Garden; ordinary original Creative/Adventure visitors receive no general build privileges. Only Chao activity/toy/fruit blocks can be placed or removed by ordinary visitors. Fixed terrain, scenery palms, authored pods and the ridge Transporter are protected. Buckets, flint-and-steel, fire charges, explosions, hostile spawns and ordinary damage cannot alter the shared authored space. Original game mode and flight state restore on returning home. No server-wide gamerules are changed.

## Assets and verification

`LobbyDecor025.register(bus)` registers eight internal blocks and one scenery block-entity type; none has a survival item or recipe. `LobbySceneRenderer025` self-registers on the client event bus. Opaque terrain uses normal face culling and native shading; collision glass, fruit support and scenery anchors do not occlude.

`generate_lobby_garden_assets_025.py` reproducibly writes textures, block models, the scoped shader, support tags, a scene manifest and a locale fragment. The manifest marks all scenery blocks `item:false`. `generate_hub_dimensions_025.py` writes the three ordinary dimension resources and a separate dedicated GameTest preset preserving the three vanilla dimensions. That preset and visual bootstrap structures are excluded from release JARs.

`GardenWorldGameTests025` exercises the actual server `gameMode.useItemOn` paths for egg placement, carrier release preserving UUID/keeper and toy placement/removal; unrelated placement and terrain destruction rejection; walking through the Garden arch and back through the cave; original Adventure mode/location/journal restoration; denied travel rollback; and real palm pod support/harvest. Runtime results and actual-client scene review belong in the release validation report; this document does not claim those checks passed before they are run.
