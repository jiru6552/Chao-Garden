# Imported Chao rigs in 0.15

> Historical 0.15 implementation record. Native conversion, surface behavior and pose fitting were revised in 0.18 and 0.19; current source and [MECHANICS.md](../MECHANICS.md) take precedence over older formulas or limitations below.


The base anatomy, texture maps and eight clips use the owner-supplied Gamemode One Sonic resource pack. The asset-only authoring directory `art/imported_chao/` contains 20 Bedrock geometry files, 20 matching PNGs and the Chao animation JSON. Account files, keys, entitlement files and content manifests are excluded.

Run `python3 art/import_official_chao.py` to generate runtime meshes, original textures and UV-matched inherited/shiny coats. Add `--skip-coats` for a quick geometry/animation rebuild. Its optional `--source` accepts an already readable resource-pack root. After geometry changes, run `python3 art/retarget_chao_overlays.py` to rebuild our fitted additions.

## Conversion

Positions convert as X = −X, Y = 24 − Y, Z = Z; child pivots become parent-relative. Bedrock's angle convention converts to Java rotations (+X, +Y, −Z). Each source cube has its own child bone, preserving cube pivots, rotations, mirrored box UVs, inflation and the nondegenerate faces of zero-thickness planes. UV dimensions remain the authored logical 64 × 32, including when the PNG is 128 × 64. The source's unused question/dizzy glyphs include coordinates beyond U = 64; these are preserved and those glyphs remain hidden by the controller.

Source hierarchy matters: `body` and `skull` are siblings under `root`; arms, feet, wings and tail follow the body. Custom goggles, crowns, animal parts, markers and Chaos details follow these sampled semantic anchors. Replaced head decoration/limb meshes are hidden while the official head core and face panels remain. The cocoon alone retains our earlier geometry and procedural motion.

## Selection and clips

Children, mod themes and all three Chaos appearances use the official Baby anatomy with fitted additions. Ordinary adults select the closest available ability/alignment anatomy. Hero Swim/Run and Dark Fly/Power have no corresponding source rig; those combinations use the relevant Swim/Run/Fly/Power rig with a derived alignment coat. Saved care state, genes, colors, shiny/jewel status and companion behavior remain the mod's own systems. Source Minecraft novelty forms are imported for reference/runtime validation without adding new collectible IDs.

| Source clip | Runtime use |
| --- | --- |
| `idle`, `walk` | Blended by movement amount; Black Market trader shares both |
| `happy` | Petting/eating reaction |
| `sad` | Hurt reaction |
| `alert` | Attack reaction |
| `no_emotion` | Ordinary floating marker |
| `chao_ender.carry` | Retained Ender special pose and held-block visibility; not selected by the active catalog |
| `chao_creeper.swell` | Retained Creeper swelling pose; not selected by the active catalog |

The runtime samples original keyframe lengths and linear interpolation, including the degree-based sine/cosine, animation time, `this`, carrying and swelling expressions present in those files. Eating, swimming, resting and attacks add care-system motion after the source pose.

## Validation

`python3 scripts/validate_official_chao.py` passes 19,746 source-parity checks across 20 rigs: 366 source bones, 809 cuboids, 63 mirrored cuboids, 72 inflated cuboids, 69 zero-thickness cuboids and 4,576 nondegenerate faces. Original textures and the eight-clip animation file match their authoring bytes. Checks cover parent-relative pivots, cube bounds and rotations, mirrored winding, UV extents, inflation and plane faces.

The excluded development class `ReleaseVisualSmokeOfficialProbe` ran inside Minecraft and logged `CHAO_OFFICIAL_POSE_PROBE_COMPLETE`: 2,000 finite samples across all 20 rigs and four emotion states, including idle/walk and both specials. It also checks the lowered idle-arm signs and idle-loop endpoints. Front client captures show upright facial UVs, lowered arms and fitted theme overlays. These checks do not establish universal animation clearance or cross-mod compatibility; the release's `TESTING.md` records the actual client study scope.
