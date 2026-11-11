# Amy and Omochao refinement — 0.20.0

## Reference and authored geometry

The Amy Chao reference is https://sonic.fandom.com/wiki/Amy_Chao and the Chao Island gallery's Special Chao → Character Chao game screenshots. The Omochao reference is Chao Island's Hacked Chao → Omochao screenshots, the SA2 Battle original model preview at https://models.spriters-resource.com/gamecube/sonicadventure2battle/asset/292922/, and the assembly description at https://chao-island.com/wiki/Chao_World_Extended#Building_an_Omochao. Reference photographs are not shipped as textures.

`art/refine_characters_020.py` authors only the gameplay adaptations on the supplied semantic baby skeleton. It leaves all 20 original rigs/textures and all original animation clips unchanged. Run it after `build_forms_019.py` and the ordinary evolution020 finalizer. The retirement pruner runs after all generators.

Amy has a compact, connected stepped cheek/crown, shorter forehead fringe with a small curl, paired triangular ears with inset light faces, and swept bob-like rear/side quills. Her smaller red torso has a white neck marking, with pink hands and feet, a red floating dot, and the original small wings/tail. The Chao-specific game reference does not show a red Amy Rose headband, so one is not added. The old generic immortal branch incorrectly hid rare characters' wings/tails/dots; that branch now applies only to actual normal-character Chaos forms. Her eyes retain one original raised eye pair and their expression overlays.

Omochao has a plated, bevelled head and torso with dark center seams, metal rivets, exactly one pair of stepped yellow spiral lenses and sockets, a mechanical smile, segmented cable arms and articulated hands, orange soles, an orange two-blade rotor, and a gray looped back key. Native organic raised eye cubes are empty and no generic eye overlays are generated for the robot. Rotor and key animate on their respective axes. It retains the source skeleton's animated arm/foot/head joints. It has no organic wings, tail, head tuft, or default emotion orb; genuine temporary reaction glyphs remain available.

## How assembly works

A Chao with the earned race wrench reward may accept Parts Boxes through the normal item thinking/use flow. Four consumed boxes build one persistent, owned actor: body; head; arms/key/rotor; completed feet and released behavior. The initial body-only prototype freezes, cannot take damage, receive care, be captured, enter a garden, or be transported. The same UUID, owner, and builder token persist across all stages. Another builder cannot finish it. If its saved prototype is unloaded or missing, assembly waits; it never spawns a replacement or duplicates a robot. A fifth box cannot create another completed actor.

The prototype snapshots its builder's appearance when construction starts, then refreshes that snapshot from the builder when the fourth box completes. Subsequent changes to the builder do not recolor a finished robot. No trained stats, acquired animal parts, equipment, reincarnation history, medals, or currency are cloned.

## Appearance inheritance that actually renders

Version019 already copied expressed coat fields into saved state, but its renderer always selected a fixed blue `character_omochao.png`. Thus the data existed without the matching visual result. Version020 uses `OmochaoMaterial` through the actual `OfficialChaoRig.texture()` path.

Large chassis, hands, and feet keep native baby UV islands. The material samples the selected builder coat into those islands, including genetic color, mono/two-tone markings, jewel texture, alignment/type coat, and active mod/dimension theme. Shiny and jewel states use the existing continuous reflection render layer. Negative, Bright, and eligible Hero brush effects apply to inherited skin pixels once. Extra mechanical plate swatches take the inherited main color, with a darker contrasting central stripe for two-tone robots and a close matching stripe for monotone robots. Fixed yellow eyes, gray cables/rivets/key, and orange rotor/soles remain mechanical colors. No inherited theme horns, animal parts, or organic spikes are added to the robot.

Amy builders use a semantic native-UV remap of their pink head and red torso; Tails/Knuckles builders map their canonical orange/red and light accent palette onto the mechanical skin. Their character-specific organic geometry is not copied. Actual Chaos builders retain their family coat/texture under the same source selection precedence. A robot building another robot preserves the original appearance snapshot hints.

The robot's generated genetic alleles agree with its expressed color, tone, jewel, and shiny state. Appearance copying reads from the builder and does not mutate its DNA. Themes preserve normal optional-mod availability gates. A removed/unloaded theme falls back to the ordinary genetic coat without erasing its saved identity.

## Validation

`RobotAppearanceGameTests` exercises the real four-box persistent assembly, changes the builder's appearance midway, and verifies the completion snapshot, same actor/owner, coherent coat alleles, serialization, absence of cloned progress/animal parts, unchanged builder DNA, and no fifth-box duplicate. Existing paid-item, ownership, unloaded-prototype, construction, and save/reload tests remain in place.

`OmochaoVisualSmokePoseProbe` runs only in the development refinement020 study. It asks the actual `OfficialChaoRig` renderer path for its registered `DynamicTexture`, checks every visible skin texel against the selected ordinary builder coat, verifies different color/tone/jewel/dimension/effect cases produce different pixels, protects every fixed hardware swatch, and checks actual shine-layer eligibility and stable cache identity. It is excluded from the installable JAR. The material implements a bounded cache, disposal deferred until the frame completes, and a resource-reload clear handler; repeated resource reloads have not been stress-tested by this probe.

`art/validate_characters_020.py` independently checks authored hierarchy, finite geometry, one robot eye pair, unchanged native coat UV topology, separate variable/hardware swatches outside original UV islands, connected Amy head/hair elements, semantic attachment landmarks, and real inherited image differences. Actual Minecraft captures are the final visual check; the offline geometry checks do not substitute for them.
