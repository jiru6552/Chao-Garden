# Chao reference study and art direction — 0.10.0

The rig and reference study below are retained from 0.10. For the current incremental acquisition and child/adult part proportions, see [Animal Parts 0.11](research/ANIMAL_PARTS_011.md).

The geometry and textures in this mod are newly authored. No Marketplace model, texture, or animation source was extracted. The proportions below describe this reconstruction, not verified dimensions from the Bedrock pack.

Version 0.9 keeps the broad head and relaxed arm rig while rebuilding the 41 refined themed crowns as chunky blocks on the intact main structure. The official reference images use both stepped rectangular pieces and deliberately angled cuboids; their terminal faces stay flat. Each crown uses a few substantial parts rather than a faceted skull or many tiny voxel steps.

Create's Engineer's Goggles and Thaumcraft's Goggles of Revealing are original recreations of the actual mod equipment, fitted across the upper forehead. Source models/textures were reference material; none are bundled. The themed collection remains 48 original mod/dimension tributes, with 42 designs updated in this pass. See [the 0.9 block-head study](research/OFFICIAL_BLOCK_HEAD_STUDY_09.md) and [goggle study](research/GOGGLE_STUDY_09.md).

Version 0.10 adds visible animal parts through 21 Animal Gene samples while retaining this block construction and the existing main forms. The seven Chao projects now share **309 bones and 721 native cubes**, including **109 animal-part bones and 232 animal cuboids**. The egg remains a separate eighth project. The [animal-gene notes](research/ANIMAL_GENES_010.md) document the Info Center training profile, supplied slots and special effects.

## References actually examined

- Every frame of Gamemode One's native **720×720** Chao variant showcase, including the baby, Hero, Dark, Swim, Fly, Run, Power and character variants. Duplicate frames are not counted as separate forms.
- The official **1600×1600** garden image, which adds Knuckles, Emerald and Creeper Chao in other poses.
- The official achievement-spawn GIF and garden-entry GIF. The variant showcase is a slideshow; neither it nor the small spawn clip establishes walk/pet animation timing.
- An original **Sonic Adventure DX cutscene Chao** OBJ and texture, examined from orthographic front, side, rear and three-quarter views. It is a cutscene reference, not a claim that it is the exact SA2 garden mesh.
- Neutral-child and fifteen evolution head shapes from **Chao Island's 3D viewer**, examined from front and side. These are supplemental viewer assets with their own color system and documented minor differences, not verified byte-for-byte SA2 model exports.
- Additional Chao Island child-gallery pictures used as visual cross-checks.
- Light, Angel and Devil head silhouettes from the public Chao Island viewer
  data, rendered at a common scale from the front and side, plus its Chaos
  appearance descriptions. The SA2 Battle archive catalog pages identify
  original Light/Angel/Devil model assets; their archive downloads returned
  HTTP 403, so those original archive bytes were not examined.

Several relevant gameplay videos were located, including the supplied video and its Chao Garden follow-up. Their players remained black/buffering here, and timeline previews did not yield readable frames. Those videos are listed as references, not as inspected footage or timing evidence.

## What the broader comparison changed

The 0.4 head used an 8×8×8 cube. A near-square projected front does not establish equal depth, and the old steep camera exposed a large top plane. The original-game and viewer studies also show that the broad head bulb must be considered separately from its tapered crest. The modern child reference's broad head is approximately 1.29 times wider than its face height; its head/torso width ratio is about 1.68. These support a compact, broad face instead of a tall block.

| Feature | 0.4 reconstruction | 0.5 reconstruction | 0.6 reconstruction |
| --- | --- | --- | --- |
| Main head, width × height × depth | 8×8×8 | 8×6.5×6 | **8×6.5×6.5** |
| Torso | 5×5×4 | 5×4.5×3.5 | **5×4.5×3.5** |
| Primary eye planes | 2×3 | 2.5×3 | **2.5×3**, white:pupil:white widths of 1:2:1 |
| Common crest width | 3 | 4 | **4**, about half the head width |
| Feet | 2×2×3 | 2.5×2×3 | **2.5×2×3**, distinct toes and heels |
| Hands | Low beside torso | Raised close beneath the head | **Lower shoulders and downward hanging hands** |
| Tail | No default tail | Small colored-cap tail | Small colored-cap tail; forked Devil Chaos tail |

The 0.6 depth adds half a unit front-to-back to the shorter 0.5 head, following
the user's visual feedback. It remains an authored blend of the DLC's block
language and the original Chao silhouette: no true orthographic side view of the
DLC source model was available. Real Minecraft front, side, back, three-quarter
and normal-player-height views check the result. The historical 0.4 comparison
uses the same low camera, FOV, yaw and entity scale.

The 0.5 hands overlapped the bottom of the head and looked extended sideways.
The 0.6 rig lowers the shoulders and puts each hand beneath its shoulder in the
unanimated geometry. Care poses keep smaller arm lifts, eating moves the hands
forward and lower, and the head nod range is restrained to leave shoulder room.
Editor preview clips reflect this hanging-arm pose; gameplay movement still
comes from Java animation code.

The 0.9 themed collection preserves these proportions and resting-arm pivots.
Its 41 block-built crowns attach to the unchanged head cube. Broad roots and
short stepped or rotated sections carry each type silhouette, with rectangular
ends and color caps. Create and Thaumcraft goggles sit on the upper forehead,
with the lenses and straps above the Chao eye planes. Compact packs, clasps
and glyphs attach to the torso. Each theme has an independent floating marker with a silhouette
such as a gear, gauge, rune, storage device, lantern or crystal.

## Evolution silhouettes and colors

Fly forms need broad head lobes or thin Hero forks, not only larger back wings. Run forms need rearward and lateral quills; Hero Run has a separate central prong. Neutral Power uses broad outward/backward temple fins, while Hero Power uses short crown prongs. Swim forms use low rear fins, jaw coloration and flat paddle feet. These shapes have independent visibility rules. In 0.10, Animal Genes replace only the slots supplied by each animal, preserving other acquired parts. Ears, low forehead crests, muzzles and horns are cuboid additions around the intact rectangular head. Acquired ears, forehead crests or horns replace the ordinary evolution crest; replacement limbs use the original low shoulder and foot pivots.

Normal-coat palettes now separate **crest tip, hand, toe, heel, collar, belly, wing and orb** colors. This permits Hero's blue tips and extremities with a yellow belly, neutral Fly's pink/blue/orange regions, and Dark Run's charcoal/green/cyan pattern. Some alignment/type combinations are not shown in the DLC showcase; their palettes are clearly marked as authored extensions in `art/chao_palettes.py`. Colored and jewel coats retain their inherited base colors.

## Chaos silhouettes and colors

Chaos progression already existed before this pass, but its visuals used one
shared crown and enlarged wings over ordinary alignment coats. The three forms
now have their own geometry groups and canonical materials:

| Form | Authored silhouette | Material and eyes | Marker |
| --- | --- | --- | --- |
| Light | Tall droplet crest, low side lobes and downturned rear head | Blue coat and tips, solid green eyes | Yellow light cube |
| Angel | Two waved head arches, descending outer lobes and a downturned rear spike | White/pink coat, solid blue eyes | Yellow ring halo |
| Devil | Bent upper horns, lower temple points, arm spikes and a forked tail | Maroon coat with red/orange/yellow flame tips, solid yellow eyes | Purple flame |

All three have tiny triangular wing remnants and no visible mouth or ordinary
pupils. The shapes are cuboid interpretations of the references; the viewer
meshes and study renders are reference material rather than shipped mod assets.
Inherited coat genes stay saved, while the displayed Chaos texture is fixed by
the form. The runtime has 168 inherited body atlases, three Chaos atlases and
48 theme atlases: **219 body materials**, plus **216 egg textures** consisting
of 168 inherited coats and 48 themed shells.

The original Minecraft-style renderer remains in use. The eight
embedded-texture Blockbench projects contain the actual exported geometry:
seven Chao form previews and one egg. Their six named Chao animation clips are
editing previews; `ChaoModel.java` implements in-game movement and synchronized
care reactions. The exporter imports geometry and rest poses, not animation
keyframes. No exact DLC keyframe-timing claim is made.

## Mod-history themes

The 48 designs expand the requested technical and magic mods into a wider
cross-section of Minecraft history, including BuildCraft, IndustrialCraft,
Railcraft, Galacticraft, Tinkers' Construct, modern machinery and storage mods,
arcane systems, creature mods and additional dimensions. Each theme combines
an authored coat, chest glyph and head/body/marker geometry rather than only a
palette swap. The complete designs, recipes, accepted mod IDs and credited
project pages are in [MOD_VARIANTS.md](MOD_VARIANTS.md).

The themes supply canonical crowns, body details and materials while active. Their
ordinary color genes, alignment, training and primary/secondary stat evolution
remain saved and working. Chaos evolution takes visual precedence without
erasing heritage. Acquired animal ears, forehead crests or horns replace the
themed crown and its goggles while keeping the themed coat, body and floating
marker. The base head and face remain visible. Half Fish can replace the marker
with a green flame; Bat hides feet, tail and wings until those slots are replaced
or cleared; Skeleton Dog clears animal parts and restores hidden feet. Chaos
forms retain their own appearance ahead of all animal effects while still
receiving gene training and checklist gains.

Each theme's three groups are named `variant_<id>_head`,
`variant_<id>_body` and `variant_<id>_marker`. The Create-themed
`chao_mod_variants.bbmodel` provides one embedded atlas and the complete shared
rig with the other themes initially hidden. Authors can select another theme's
groups and load its corresponding PNG into the first texture slot. The full
generator also emits themed egg icons, conditional data-preserving crafting
recipes and recipe advancements from the shared catalog.

The research distinguishes present NeoForge builds, separate community ports
and historical themes that remain dormant unless a compatible mod is actually
loaded. Recognizing an ID does not make an old Forge or Fabric jar compatible.
CurseForge project/file pages and primary mod metadata were inspected; the
third-party mods were not all launched together as part of that research.
Detailed source notes are preserved in
[technical history](research/MOD_HISTORY_TECH.md),
[magic history](research/MOD_HISTORY_MAGIC.md) and
[adventure history](research/MOD_HISTORY_ADVENTURE.md). The later [head-refinement research](research/HEAD_REFINEMENT_MAGIC.md)
records confirmed community ports and publisher development builds, including
Hex Casting's official 1.21.1 CI alpha. Availability distinctions remain
separate from the geometry style.

## Sources

- [Gamemode One: The ultimate guide to Chao](https://support.gamemodeone.com/hc/en-us/articles/4402606440347-The-ultimate-guide-to-Chao)
- [Official variant GIF](https://support.gamemodeone.com/hc/article_attachments/4402923261595/Sonic_ChaoCharacter.gif)
- [Official garden image](https://support.gamemodeone.com/hc/article_attachments/5709130352155/ChaoGarden.png)
- [SADX cutscene Chao model reference](https://models.spriters-resource.com/gamecube/sonicadventuredxdirectorscut/asset/308057/)
- [Chao Island 3D viewer](https://chao-island.com/viewer/) and [viewer FAQ](https://chao-island.com/viewer/faq.html)
- [Chao Island Chaos evolution and appearance research](https://chao-island.com/info-center/advanced/chaos-chao.html)
- SA2 Battle archive catalog entries: [Light](https://models.spriters-resource.com/gamecube/sonicadventure2battle/asset/320059/), [Angel](https://models.spriters-resource.com/gamecube/sonicadventure2battle/asset/320058/), [Devil](https://models.spriters-resource.com/gamecube/sonicadventure2battle/asset/320057/). Catalog identification only; archive bytes were unavailable here.
- [User-supplied gameplay reference](https://www.youtube.com/watch?v=5h5Rfx7Q5T8)
- [Follow-up with the garden at 21:20](https://www.youtube.com/watch?v=5SJ7T5n28sg&t=1280s)

The previous Cobblemon study remains historical background in `COBBLEMON_ART_DIRECTION.md`. Audio sources and processing are documented separately in `AUDIO_CREDITS.md`.
