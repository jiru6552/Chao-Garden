# Dark Garden: first playable foundation

This is a compact, playable interpretation of the **Sonic Adventure 2: Battle** Dark Garden. The [reference study](references-0.25.5/REFERENCE_STUDY.md) and its five primary screenshot originals distinguish Battle's sparse black fruit trunks from the large tree/cage visible in some Dreamcast images. No game mesh or screenshot pixels are used as runtime art.

The first pass uses pale ashen ground, jagged pale rock, a large irregular red pool, sparse bare fruit trees, small engraved gravestones, broken iron-fence fragments, a far-bank grave seat, a purple storm-cloud sphere and a separate distant horned crag with one carved Chao mask. It deliberately avoids treating the Dark Garden as a dark-grass recolor of Hero Garden or placing a giant tree in the middle of the Battle-style care space.

## Layout and scale

| Feature | Native location |
| --- | --- |
| Care field | An oval within 49×41 blocks, with a small west cave apron; 1,510 ground columns. |
| Arrival | `(-18.5, 80.1, 8.5)`, yaw −125°. The first five metres east are level, dry and clear. |
| Return portal | `(-22, 80, 8)`, facing east. A real 3×4 animated portal sits in the plain pale-rock west cave without a quartz rim. |
| Fixed garden device | `(-15, 80, 12)`, facing north; the same eight-cell 2×2×2 provider used by the other gardens. |
| Red native-water pool | Irregular footprint centered near `(6, -3)`, 294 full source-water columns at Y79 over a continuous rock basin at Y77. |
| Back-cliff waterfall | Three blocks wide near `(10, 88, -16)`, feeding a genuine water channel into the pool. |
| Bare fruit trees | Near `(-11, 5)`, `(16, 10)` and `(-12, -8)`. Their twelve pods use actual Dark Garden Fruit, with supported ripening and one-item harvests. |
| Distant carved crag | Beyond the playable north cliff. Nine finite blockstate tiles form one complete mask near `(0, 106, -32)`; the backdrop remains outside the care field. |

Scale and exact placement are authored Minecraft adaptations, not recovered original game measurements. The reference's blue waterfall and red pool use different visual colors. This first native-fluid pass uses the pool's red biome tint for the waterfall too; it does not install a global water handler or introduce damaging lava. Further contour, vegetation and color refinements remain possible after direct in-game feedback.

## Native care and protection

The new `chao_garden:dark_garden` realm participates in the existing Chao World Adventure rules. Player care objects, eggs, carried Chao, fruit seeds, garden providers and Bell links retain their normal mechanisms. The authored device and native fruit pods cannot be recovered as purchased furnishings. Pale ash is added to the ordinary dirt tag so saplings can survive. Bare fruit uses the existing Dark Garden Fruit item and a narrow survival rule requiring a real horizontal deadwood arm joined to the central branched trunk. In 0.25.6 the arm is visible above each pod; every Dark fruit growth stage has a stem that reaches its underside. The twelve pod cells remain unchanged so a harvested growth stage survives scene updates.

The server uses logical daylight to preserve the existing brightness-dependent fruit and sapling growth rules. The client independently displays a purple storm-cloud sphere with `SkyType.NONE`, so no vanilla sun or moon appears. The biome has no precipitation, world features or wildlife spawn lists. No alignment bonus, new evolution rule or progression unlock is inferred from the reference imagery.

Invisible barriers follow the irregular field edge through the full usable height, with a high invisible cap. They protect Chao, eggs and thrown goods while the distant crag remains scenery. The cloud sphere is cached geometry drawn in actual world/camera space, rather than a screen-fixed sky image or a solid roof.

Scene preparation preflights the entire rewrite before changing terrain. Purchased providers retain all eight physical cells, canonical registry keys, owner, roster and Bell links. Preserved care blocks keep their support and waterlogged toys require source water. Unsupported block entities defer the whole rewrite. Chao, eggs and goods are preserved by object/UUID and payload; displaced bodies receive separate collision-free shore landings, and busy Chao defer an unsafe rewrite. Harvested native fruit retains its current growth stage through repeated scene preparation.

## Editable assets and checks

`scripts/generate_dark_garden_0255.py` authors the six native materials, finite wood/grave/mask models, inventory models, loot, strict tag merges, purple cloud texture, biome and `art/dark_garden_lang_fragment_0255.json`. `art/dark_resource_contract_0255.py` binds the exact six BlockItems to their Java registrations and catalog; individual mask tiles are not extra item IDs.

`art/validate_dark_garden_0255.py --self-test` checks finite domains, opacity, material texel density, cropped-member UVs, ash color, red native-water tint, sky seams/poles, cached atmosphere geometry, strict fruit support, dirt-tag preservation and preflight ordering. It also checks that the headstone engraving is confined to its broad front/back faces, with unmarked matching stone on sides and horizontal steps. Its seven negative fixtures reject wrong water color, wildlife, stretched wood, an uncapped headstone, an incomplete distant mask, and engraving repeated on a side or top.

Native cases are grouped in `dark0255_geometry`, `dark0255_preserve` and `dark0255_care`. They cover ticking native water, all twelve visible fruit attachments, rejected missing/perpendicular arms, supported fruit harvest and regrowth, complete mask backing, return aperture, invisible containment, purchased-provider and actor preservation, refusal before block-entity overwrites, and real Adventure care/return integration. Completed run results belong to the release verification, rather than being implied by this source note.
