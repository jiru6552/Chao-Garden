## 0.25.49 — Garden toys and retirement

- Retire the coffin from active toys, rewards, crafting, market and creative inventory. Preserve saved IDs as inert compatibility markers and non-placeable tokens, with no inventory or chunk scans.
- Give accordion, guitar, triangle, bubble wand and castanets separate flat floor models while preserving their held and inventory presentation.
- Refine rocking-horse mounting, continuous rocking, measured seat/handle/foot contact, safe exit and production previews.
- Add car, Sonic/Shadow dolls, broom, picture book, pogo stick and crayons with source-derived assets and individual Lionfish interactions. Document source geometry, authored adaptations and reference limits.
- Expand native recovery/preview/save checks and actual client quality studies. See docs/TOYS_02549.md for per-toy results and remaining limitations.

## 0.25.48 — Piano and accordion interactions

- Bring piano performers closer to the measured front keys, limit arm extension and follow the key depression with visible replacement hands. Keep the existing alternating harp phrase and gameplay rewards.
- Give the existing accordion a dedicated two-hand Lionfish performance: rigid keyboard/support ends, moving bellows, key presses and synchronized original reed notes for placed, learned and classroom use.
- Expose both toys through production once/loop/stop previews with interruption cleanup and no care or inventory changes. Add focused rig, source-asset, native-server and client verification.
- Preserve organ, guitar, Analyzer, garden, Chaos Drive, equipped-flight, carrying, running and lobby fixes. See docs/INSTRUMENTS_02548.md.

## 0.25.47 — Real shader and Accessories verification

- Blend late lobby glass color before a separate depth-only resolve. This removes Solas banding where the final tread and landing share a plane, retaining all geometry and foreground depth for shader effects.
- Verify exact Solas Shader V3.7b with official NeoForge Iris/Sodium and real Accessories/owo. Preserve all 0.25.46 toy, equipped-flight, back attachment and running changes.
- Correct stale native tests for baby equipping, earned Run/Swim milestones and head/back carrying; add actual baby release and separate-process equipped save/load probes.
- Add focused lobby view selection and require actual rendered lobby frames before captures. See docs/REAL_MOD_CHECKS_02547.md for verified versions, results and limitations.

## 0.25.46 — Toy contact, equipped flight, running and lobby rendering

- Move organ performers closer to the actual front manual; fit natural arms and preserve clear-front access, rewards, cooldowns and player collision.
- Bring the guitar closer and higher, and correct the string-face orientation and two-hand grip.
- Admit the server-authorized equipped glide at vanilla's initial double-jump gate; retain normal elytra and all server eligibility checks. Fit the baby carry anchor to the keeper's back.
- Bound and ease running cadence while retaining movement speed and Run unlocks; steady high-speed torso/head motion.
- Render lobby glass with normal shader translucency and appropriate depth, sort floor/stairs/tubes together, preserve authored backgrounds after deferred rendering, and correct stair/rail normals.
- Add focused rig/native regressions and disposable native-client studies. Exact Solas V3.7b and independent multiplayer verification remain required; see docs/FIXES_02546.md.

## 0.25.45 — Guitar interaction

- Refine the existing guitar with a torso-held CWE mesh, left-hand fretting and alternating Lionfish strums aligned to the measured string face.
- Use an original four-string chord at the actual strum crossings for placed play, learned performances, classroom practice and `/chao testtoy guitar once|loop|stop`.
- Keep lesson gates, reservations, completion rewards and cooldowns; hide the reserved source only during hand presentation and never create a real inventory prop.
- Add source-contact/rendered-hand, sound-asset and native lifecycle tests and an opt-in eight-session client study. Retain the organ, piano, Analyzer, garden and drive changes.

## 0.25.44 — Organ interaction

- Refine the existing organ with measured front-manual contact, sustained two-hand Lionfish chords, key-only animation, happy expressions and an original pipe-organ sound.
- Add `/chao testtoy organ once|loop|stop` using the real placed interaction without rewards or saved care/cooldown changes.
- Add organ mesh/timing/asset and native navigation/orientation/interruption tests, plus a disposable client study. Preserve piano, Analyzer, garden and drive behavior.
- Correct the stale held-item damage-death test to verify inventory-preserving recovery, with separate terminal-drop coverage.

## 0.25.37 — Item, tree and care refinements

- Bubble-wand play uses happy eyes and an open mouth while retaining hurt, care and other priority reactions.
- Raise cymbal gain from 0.24 to 0.36 and drum gain from 0.24 to 0.28; retain existing samples, pitch and rhythm.
- SA2-inspired key, bound green Keeper’s Journal, blue/yellow vanilla-style disc, and corrected held teleporter UVs.
- Journal crafts from a book and reusable Chao Key. Teleporter crafting still consumes its key.
- Fruit-log item matches the planted trunk; normal and Dark logs each craft four corresponding oak planks.
- Dark seeds grow bare garden-style trees with Dark log drops; preserve existing fruit and watering during migration and reload.
- Analyzer Lessons shows the current class or no-class message; Doctor no longer instructs placing a station.

# Changelog

## 0.25.11

- Added a low curved, grass-topped limestone bank and nine additional broad-leaf plants beside the Neutral waterfall. Existing stream, pool, palms and main walking routes remain.
- Replaced the authored palm bark with warmer brown horizontal rings and explicit full-tile UVs.
- Included five area themes and six full classroom singing recordings converted from the supplied ADX folder. Kindergarten background music fades out for classroom singing and returns outside the room.
- Authored-room revision 36 preserves care/ownership while applying the side-bank update.

## 0.25.10

- Consolidated Hero/Dark Fruit into the garden variants, including child-affinity effects, trades and saved stack/seed/orchard aliases.
- Filled the Hero pool rim gaps and checked the complete land-facing water perimeter.
- Refined the Neutral waterfall with pale limestone, nine broad-leaf plants, lighter water and low-ground flower clusters, using the supplied Battle reference frames.
- Authored-room revision 35 applies the garden changes through the existing migration path.

## 0.25.9

- Slim native lavender Hero pavilion columns, shallow pavilion cap and fountain top.
- Local SA2 ADX importer for named area themes and six singing tracks.
- Optional full-length classroom playback without overlapping phrases; stop on exit or lesson end.
- Current verification and reconstruction notes included; original SA2 audio remains user-supplied.


## 0.22.0 - Consistency, lifecycle and arithmetic audit

This is the user-defined 50% readiness checkpoint. Current execution and artifact review status is recorded in [TESTING.md](TESTING.md); earlier completed release evidence is retained in [VALIDATION_021.md](docs/VALIDATION_021.md).

- Shortened six oversized Hero/Dark ordinary head cores while preserving width, face planes, joints, crest lengths, special forms and original comparison inputs. Reanchored age-dependent markers without accumulating offsets.
- Enabled actual adult first-type and gradual-age geometry for all 19 retained themes. Added 209 native-UV atlases and fitted goggles, chest emblems, packs, crowns and markers to animated bounds. Protected inherited eye texels and cached animal-overlay descriptors.
- Restored four egg-icon pixels covered by glare and added separate evolution, rebirth and death cocoon palettes on the unchanged shell geometry. Preserved the established DNA, drive, fruit and seed artwork.
- Removed the hidden 500-point reincarnation carry cap, retaining exactly ten percent of every valid visible/hidden point total with level 1 and zero progress. Preserved E–S gain ranges, expressed-allele first-evolution upgrades and the S cap.
- Corrected Chaos evolution's happiness threshold to above 80, separately retaining above 50 for ordinary reincarnation and the robot care-heart outcome.
- Made maximum configured lifespans reachable, saturated lifetime counters, and preserved held/worn stacks and garden identity when terminal item insertion is denied or re-entered. Added actual egg/cocoon/rebirth/death/robot retry coverage.
- Prevented a knocked-out or ring-out karate fighter from counterattacking, bounded lifetime activity arithmetic and price conversion, and aligned care tooltips with the synchronized server food profile.
- Removed 1,615 unreachable generated assets and thirteen superseded capture drivers. Replaced the unused 222-bone runtime prototype with the exact two-bone cocoon, bounded render-state and held-Chao caches, and retained authoring/history/compatibility inputs.
- Completed a 465-frame actual-client study, five complete 24-frame motion reviews, individually inspected 135 PDF pages and both offline gallery checks. Publication requires hash-bound source/render/capture evidence.

## 0.21.0 - Gradual evolution, robot legacy and companions

Validated with 229,779 deterministic checks, 117/117 required server tests in each default/real-Accessories profile, a completed 340-capture actual-client study, and individual full-size review of all 78 PDF pages. See [VALIDATION_021.md](docs/VALIDATION_021.md) for evidence and scope.

- Separated first-evolution identity from age/fruit evolution strength and mutable signed secondary training. Highest-stat and last-increased ties follow the requested Minecraft rule; first alignment/type stay fixed until reincarnation.
- Replaced exaggerated ordinary head development with restrained, referenced young/fully aged targets and reversible valid secondary combinations.
- Added finite young-adult Omochao lives, all-C starts, animal/illness immunity, physical components/hearts, creator genealogy and C/B/A/S reconstruction investment.
- Scaled every adult companion's speed, water, glide/elytra and combat benefits by skill level; all-S robots gain validated five-second boosts.
- Refined Light/Angel/Devil Chaos head placement, width and prominence while retaining the icy terminal materials.
- Added actual age progression, robot reconstruction and real Accessories studies, with whole-frame previews and retained provenance.

## 0.20.0 - Refined forms, family and special Chao

Validated with 228,773 deterministic checks, 101 required server tests in each default/real-Accessories profile, a completed 247-frame actual-client study, and a fully reviewed 55-page PDF. See [VALIDATION_020.md](docs/VALIDATION_020.md) for historical evidence and scope.

- Strengthened real ordinary secondary evolution with connected stepped head shapes and morphed primary crests, preserving face planes and animation joints. Ordinary ±1 limits and opt-in Monster ±3 progression are demonstrated separately.
- Added family display-name snapshots, loaded-relative lookup for older records and explicit unknown-name fallbacks; removed the leftover inspection background well.
- Refitted accessories to animated head/body bounds across child, primary, character and Chaos rigs.
- Refined Amy and Omochao. Omochao uses a single robotic eye set and inherits the builder's expressed coat properties during real four-stage assembly.
- Refined all three Chaos families with a separate translucent tip pass that preserves inherited coat rendering.
- Retired Spartoi and Egg character acquisition and presentation while preserving safe saved-data migration and ordinary eggs.
- Added a metadata-driven actual-client study, whole-frame gallery/PDF workflow, source provenance and packaging without a size ceiling.

## 0.19.0 - Kindergarten and an expanded garden

Validated on Minecraft Java 1.21.1 / NeoForge 21.1.219 with 227,419 deterministic checks, 95/95 required server tests in both default and real Accessories/identity-fixture profiles, a completed 372-frame client study and a visually reviewed twenty-page PDF. See [TESTING.md](TESTING.md) for evidence and scope.

- Added thirteen diploid personality tendencies, eighteen emotion meters, six illness diagnoses and hidden Luck/Intelligence allele pairs. Existing saved values migrate without rerolling displayed phenotypes.
- Added actual Doctor Station and Lesson Desk blocks, medicine, secure paid grade care, timed rotating classes, saved lesson progress and all eighteen knowledge tracks. Song/drawing improve to five; all four dances unlock breakdancing. Lessons do not grant Intelligence XP.
- Added inventory-backed Chao equipment categories, actual dye consumption, component-preserving replacement/removal and earned-medal selection. First instrument lessons grant one persisted, once-only physical prop.
- Added persistent friendships and timed expressive/social actions, illness symptoms, learned performances, orchard shaking and a larger set of placeable toys.
- Added thirteen CWE-inspired crop families, ten extra animal samples, thirteen lenses, ten wearable accessories and nine other gifts. Rotating market categories preserve discovery; classroom-only N/A props are earned rather than sold.
- Added gentle egg rocking, physical egg throwing, wall-triggered rough hatching and visible end-of-life cocoons. Rebirth keeps learned knowledge, equipment and friends while resetting personality and current-life progress.
- Preserved lifetime contest wins across reincarnation by reconciling the saved state journal with the legacy entity counter; a real-race regression verifies the next victory advances the total.
- Preserved the original body UUID through rebirth eggs, carried egg items and reload, keeping the same garden entry. Hatching rejects a live UUID already present in any server dimension; actual death frees registry/recovery entries while safe capture and rebirth retain them.
- Expanded ancestry records, inherited coat/form presentation, character/rare appearances and optional CWE rules. Exact original meshes, undocumented behavior and platform-only features are distinguished in [the coverage ledger](docs/IMPLEMENTATION_COVERAGE_019.md).
- Prepared release metadata, reproducible assets and actual-client preview workflows without a size ceiling. All 372 raw captures, six actual-frame motion reviews and the 20 selected PDF pages remain bound to the final capture manifest.

## 0.18.0 - Official comparison and garden choices

Validated on Minecraft Java 1.21.1 / NeoForge 21.1.219 with 197,095 deterministic checks, 58/58 required server tests in each default and actual Accessories profile, and a completed 665-frame client study. See TESTING.md for scope and artifact checks.

- Added an unmodified official model/texture/source-clip comparison appearance alongside the customized garden appearance, preserving the creature's saved identity and care data.
- Corrected source rotation/UV handling, limb orientation and inherited coat mapping; improved walking and reaction transitions.
- Changed shiny reflection coordinates from the coat atlas to a continuous geometry projection, retaining source-alpha masking and the End egg's portal material.
- Joined the trader's lens frames to their temples and the mask to its connected cheek/rear straps. Original source anatomy remains the basis.
- Made garden toys placeable and available for autonomous Chao play instead of requiring a direct care-item interaction.
- Added held item-decision stages, delayed care commits, food acceptance based on needs, and returning unwanted or unfamiliar dropped items.
- Extended the food catalog with Chao Adventure 2 fruit and retained the mushroom seed retirement.
- Added genetics-aware Chaos coats and corrected Moon's material using the inspected visual references.
- Expanded the capture study and release artifacts without a package-size ceiling. The gallery and PDF are driven by scene metadata rather than a fixed capture count.

## 0.17.0 - Crawling, shine and the Black Market

- Replaced the weak crawling pose with an alternating reach, plant, push and recovery cycle, including a lifted returning hand, bent rear legs and a small torso/head nod. The cycle follows actual travelled stride and stops while stationary.
- Added a moving silver reflection pass to shiny/jewel Chao and placed eggs using an included source-masked reflection material and Minecraft foil blending. Removed the old shiny-only sparkle particles so detached white puffs do not interrupt the sheen. Removed the unwanted white piece under placed eggs and kept the live End-portal shell.
- Refitted the merchant with reference sunglasses and a mask, preserving the supplied Chao rig/walk cycle. Removed its night invisibility, including clearing an old saved potion effect.
- Rebuilt the rotating emerald catalog around the supplied Black Market and Sega Superstars lists: regular/coloured/shiny/rare eggs, priced fruit and seeds, twelve wearable hats and five reusable toys. Removed drive/gene sales and new stock of old toys. Added purple dimpled Moon coat support without changing older jewel indices. Existing-generation traders migrate away from obsolete offers.
- Removed new Mushroom/Nutritious Mushroom seed recipes, planting and creative entries while retaining old IDs and existing orchards for save compatibility.
- Restored inherited Run-form coat textures and corrected source packaging so the legitimate `coats/run/` art folder is retained while the root game runtime remains excluded.
- Added a fresh offline gallery, a four-page PDF and validation records for this release. See [testing](TESTING.md) and [source/pricing notes](docs/research/BLACK_MARKET_017.md).

## 0.16.0 - Movement progression and quieter idle

- Added quiet breathing and brief UUID-phased idle gestures, point-based crawl/walk/run/dash, novice/competent/advanced swimming, and finite descending ledge glides.
- Added Analyzer unlock tooltips and three movement guide pages while preserving the separate adult Accessories player-glide perk.
- Preserved the completed [0.16 validation record](docs/VALIDATION_016.md).


## 0.11.0 — Leafy orchards and progressive animal parts

- Rebuilt all twelve orchard trees at seven blocks tall with five-by-five, layered canopies of real biome-tinted leaf blocks, full wood branches and four hanging fruit pods. Added vanilla-style foliage support, decay, shears, waterlogging and player placement. Bone meal can expand saved small trees without duplicating their crop; obstructed upgrades preserve the old tree.
- Redrew all 92 ordinary/shiny/jewel/themed egg sprites, four elongated capped Chaos Drives and 21 colored glass DNA vials. Canonical and legacy gene items keep matching art; item IDs, training effects and theme gates are retained.
- Replaced rectangular race-marker boards with thin red triangular pennants in both placed and held models. Linking controls, the visible distance preview and the 32-block limit are retained; the recipe now uses red wool.
- Ordinary gene samples now select one supported part not already expressed by that animal. Repeated samples progressively complete the set and preserve unrelated parts for mixed appearances. Same-animal ears and horns can coexist; different-animal ears/horns replace their incompatible opposite slot. This guaranteed one-part selection is a Minecraft adaptation, not an asserted original-game probability.
- Made child animal details smaller than adult details while retaining the existing cuboid rig, connected limbs, broad main head and Chaos appearance protection. Saved partial or complete appearances remain valid without a state-schema change.
- Updated the care guide, acquisition tooltips and documentation. See [orchards](docs/FRUIT_TREES.md), [progressive parts](docs/research/ANIMAL_PARTS_011.md), [sprite art](docs/SPRITE_REFINEMENT_011.md) and [validation](TESTING.md).

## 0.10.0 — Gardens, activities and companions

- Added 21 colored DNA-vial Animal Genes with exact benefit tooltips, modern Chao Island Info Center training profiles, mixed visible animal parts and Half Fish/Bat/Skeleton Dog effects. Chaos appearances remain protected. Previous animal-item IDs stay usable aliases; existing saved history is retained.
- Added independent drive and gene chances to conventional `chests/` loot tables across namespaces, preserving vanilla and earlier bonus pools.
- Added renewable branch trees for 12 fruits, hanging pods with visible ripening and regrowth, fruit-to-seed crafting, bone meal support and farmer seed trades.
- Added a naturally visiting Chao Black Market Trader with an emerald merchant interface, rotating seeds, drives, genes, shiny/jewel eggs and reusable ball/rattle/maracas toys. Stock, purchases and visit limits persist.
- Added Garden Blocks and Garden Bells for up to eight Chao per garden. Added player-built race courses with straight starting/ending lines, linked flags and 32-block previews, plus complete 7×7 karate mats. Shared lobbies let multiple keepers enter their own Chao, earn training and return home after contests or interruptions.
- Added empty-hand sneak pickup and animated held Chao. Capture/release preserves the original entity UUID and rejects a copied carrier while that Chao is already present.
- Added optional Accessories adult companions with Power-based combat, Fly-based glide and a persistent 60-second defeat recovery. Defeat costs 10 happiness and one randomly chosen trained skill level. Holding, care, gardens and competitions work without Accessories.
- Expanded the in-game care guide and release documentation. See [orchards](docs/FRUIT_TREES.md), [genes](docs/research/ANIMAL_GENES_010.md), [market](docs/research/BLACK_MARKET_MECHANICS_010.md), [activities](docs/mechanics/GARDENS_AND_ACTIVITIES.md), [companions](docs/COMPANIONS.md) and [validation](TESTING.md).

## 0.9.0 — Cuboid crowns and forehead goggles

- Rebuilt the 41 refined mod-theme crowns as a small authored set of native cuboid parts on the intact rectangular head, following the official Minecraft Sonic add-on reference images. Stepped sections, angled blocks, flat ends and color caps replace the 0.8 polygon slopes.
- Recreated Create’s Engineer’s Goggles and Thaumcraft’s Goggles of Revealing with original geometry and materials, fitted across the upper forehead with the eyes clear. Thaumcraft retains its existing availability gate.
- Updated the editable Blockbench models, crown manifest, preview gallery and model-style validation. The complete collection remains 48 themes; saved heritage, source-mod gates, recipes, genes, names and incubation behavior remain.
- Continued to defer animal-gift appearance variations.

## 0.8.0 — Connected themed head evolutions

- Refined 41 mod themes with confirmed downloadable Minecraft 1.21.1 NeoForge builds: 14 technical, 15 magic and 12 adventure designs. Each now uses one connected head shell with broad roots, a distinct crown outline and swept tapered tips in place of the previous head accessories.
- Preserved the readable Chao face, broad cheeks, relaxed resting arms, body details, floating markers and the three Chaos appearances. Five historical mod themes and the Nether/End designs keep their previous shapes; the collection remains 48 themes.
- Rechecked publisher releases, alphas and community ports. Documented Botania Revived, the Blood Magic port and Hex Casting development builds explicitly. Added the verified modern `galacticraft` alias alongside `galacticraftcore`.
- Added editable head profiles, a generated morph manifest, and head-shell validation. Existing heritage, recipes, genes, names, incubation and source-mod availability rules remain intact. Animal-gift appearance variations remain deferred.

## 0.7.0 — Mod-history Chao heritage

- Added 48 original themed Chao: 46 mod-inspired designs and Nether/End forms, with separate accessories, coat palettes, chest glyphs and floating markers.
- Expanded the requested collection across technical, magic, creature and dimension mods, including classic Minecraft mod families. Documented actual mod IDs, community-port distinctions and dormant historical gates.
- Added shapeless themed-egg recipes gated by their source mod, preserving inherited genes, names and incubation from the input egg. Added filtered creative-tab entries, `/chao variants` and `/chao variantegg <id>`. Egg placement also checks availability and rejects embedded-data bypasses.
- Added persistent themed heritage with parent inheritance and reincarnation retention. Version 3 saves accept previous version 2 data without losing existing traits.
- Kept removed-mod heritage dormant with an ordinary appearance, retaining its saved identity for restoration. Stable egg item IDs preserve inventory data even while a source mod is absent.
- Preserved the main Chao proportions, relaxed arm rig, normal evolution mechanics and the three Chaos forms. Chaos visuals take precedence over the saved theme. Small-animal gift appearance variants remain deferred.

## 0.6.0 — Relaxed arms and the main Chaos forms

- Slightly deepened the head from 8×6.5×6 to 8×6.5×6.5 while preserving the shorter face.
- Lowered the shoulders and hands into a downward resting pose, with adjusted care animations and head movement to prevent arm/head clipping.
- Added distinct Light, Angel and Devil Chaos appearances to the existing Chaos evolution system, with canonical form colors and their own editable Blockbench projects. Inherited coat genes remain saved.
- Deferred animal appearance variations and omitted their geometry from the active models. Animal gifts still train stats and fill the current-life Chaos checklist; existing animal data remains saved.
- Updated the care guide, journal text and reference fixtures to reflect the focus on main forms.

## 0.5.0 — Broader proportions and distinct evolution silhouettes

- Replaced the 8×8×8 head with an authored 8×6.5×6 head after examining native-aspect DLC references, an original SADX model and Chao Island's supplemental viewer geometry.
- Widened the eyes to 2.5×3 and the low common crest to four units; shortened the torso, raised the hands and broadened the feet while keeping a compact stance.
- Added a small tail and repositioned facial overlays and animal attachments for the shallower head and torso.
- Rebuilt Swim/Fly/Run/Power head fins and swept shapes, including separate Hero Fly, Hero Run, Hero Power and Dark Power silhouettes.
- Separated crest-tip, hand, toe, heel, collar and belly colors. Added Swim jaw bands and Dark chest/forehead markings to normal coats. Genetic coats retain their inherited base material.
- Separated Swim paddle-foot visibility from arm gifts, and kept evolved head fins visible when animal wings or arms replace other parts.
- Added low-camera front/side/back/three-quarter checks and a matched-camera 0.4 comparison. Geometry sizes now support half units without silent integer rounding.
- Kept the existing saved-data schema and original Chao voice recordings.

## 0.4.0 — Bedrock DLC reference reconstruction

- Reconstructed the reference Chao's square head, clean cyan materials, tall white-bordered eye columns, broad hooked crest, small yellow hands, colored heels, and tilted cube emotion marker.
- Added Hero and Dark crest silhouettes, Dark brows and toothy expression, and adult chest marks while keeping a plain neutral baby front.
- Reworked the gait with alternating short steps and subtle body sway; added a real synchronized petting reaction with closed happy eyes, raised arms, and a heart.
- Replaced Allay voice effects with isolated Sonic Adventure-series Chao samples. Audio provenance and cue mappings are documented separately.
- Preserved existing Chao, egg, carrier, breeding, inheritance, and lifecycle data.
- Updated embedded-texture Blockbench projects and the roundtrip geometry checks. The model is an original reference reconstruction, not extracted Marketplace geometry; Blockbench animation clips remain editing previews.

## 0.3.0 — Cobblemon-inspired cuboid refinement

- Rebuilt the Chao anatomy around broad, unbroken cube volumes, a compact body, short arms, and separated feet.
- Replaced glossy 3D eyes with flat pixel expressions and a smaller smile.
- Reworked the crest into tapered, angled cube sections and added restrained hand-painted pixel clusters.
- Applied consistent one-texel-per-model-unit mapping to all character cubes and adapted the evolved/animal parts to match.
- Added ordinary editable Cube primitives to the Blockbench projects and expanded the exporter for Box UV, face rotation, inflation, and zero-depth planes.
- Added cube/plane export checks and release art-contract validation.
- Studied the actual official Cobblemon Jigglypuff, Rowlet, and Cleffa geometry and textures; all Chao art remains original.

## 0.2.0 — Adventure visual refinement

- Rebuilt Chao and egg geometry as rounded, rigged low-poly meshes with smooth lighting and pixel materials.
- Added swept gradient crests, oval eyes and expression changes, pear bodies, soft limbs, curved wings, bean feet, and spherical emotion markers.
- Refined Hero/Dark/Neutral presentation, evolved silhouettes, animal textures, shiny coats, and jewel materials.
- Replaced the dark journal with an Adventure-inspired medical chart: blue/cyan shell, white cards, orange headers, segmented meters, grade badges, and an animated Chao portrait.
- Added the matching 17-chapter field-guide reader, chapter selection, mouse/keyboard controls, and responsive GUI scaling.
- Added four editable Generic Blockbench projects with embedded textures, six Chao preview animation clips, a native mesh exporter, and structural/roundtrip validation.
- Retained the same saved Chao, egg, carrier, and genetics format for updates from 0.1.0.

## 0.1.0 — First standalone release

- Added persistent Chao companions and placeable, recoverable eggs for Minecraft 1.21.1 and NeoForge.
- Added hunger, energy, resting, affection, keeper ownership, following, staying, naming, and safe carrier transport.
- Added five graded ability stats, training progress, ability points, and practical Minecraft benefits.
- Added diploid inheritance for colors, tone, shiny traits, jewel coats, and stat grades.
- Added Hero, Neutral, and Dark alignments, first evolution, secondary forms, reincarnation, and immortal Chaos evolution.
- Added adult breeding through Heart Fruit, inherited offspring eggs, and a breeding cooldown.
- Added 21 animal gifts with training, slot-based appearance features, and a per-life collection checklist.
- Added original Minecraft-style Chao and egg models, pixel textures, shiny/jewel coats, and procedural animations.
- Added fruit, Chaos Drives, an in-game care guide, the Analyzer journal, and the Carrier.
- Added additive enemy/chest supplies and rare structure-specific egg pools.
- Added configurable lifecycle timing and natural death, operator commands, deterministic mechanics checks, and Minecraft GameTests.

This is a Minecraft adaptation informed by Sonic Adventure's Chao systems. Its player-built gardens, race courses and karate rules are authored adaptations rather than reproductions of the console stages or every original behavior.
