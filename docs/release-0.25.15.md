# Chao Garden 0.25.15

The requested stone walls, grass sides, waterfall stone, broadleaf plant, Dark Garden bark and gravestones return to their exact pre-import textures. The source grass top remains, now area-filtered before resizing to reduce its high-frequency noise. Palm bark, Hero masonry and other accepted materials are retained.

Ten Kindergarten pictures now use decoded original drawings, photographs and clinic posters. The picture frames keep their mounts; square drawings are fitted without horizontal stretching. Hall posters retain their original proportions within the existing square block face. The hidden Black Market cabinet remains unadvertised.

Seventeen toys now use identified original SA2 models and textures: maracas, bell, drum, flute, castanets, trumpet, cymbals, tambourine, rattle, bubble wand, rocking horse, radio, TV, coffin, organ, piano and lifesaver. They have matching 32px item icons and 85 placed poses, fitted inside the existing interaction envelopes. Paired instruments are paired; the TV uses its original channel frames; the coffin lid moves independently; keyboard/speaker animation stays local to its source faces. All toy acquisition and play rules remain in place.

Twelve wearable hats use the original apple, bucket, can, box, pot, paper bag, pan, stump, watermelon and three wool-cap models. Their source meshes are adapted to the established fitted hat envelopes across 20 rig resources. Merchant materials keep their original UV positions in the enlarged atlas. Blue/pink hoodies and their sleeves have chamfered geometry and a neutral, dye-compatible fabric adapted from the original wool material. Hoodies are custom mod garments; they are not presented as vanilla SA2 clothing.

## Garden geometry

The supplied folder contains actual visual landtables for all three gardens. The offline audit reconstructs 1,245 Neutral, 1,477 Hero and 1,245 Dark triangles, including their source object transforms and texture indices. This is direct inspection of the original geometry, not an original-game executable running inside Minecraft.

The source Neutral grass has a broad low rise before its enclosing cliffs, and the Hero grass also includes shallow shoulders. Small one- and two-block terraces now soften the eastern cliff feet in those gardens. The main care lawn, existing palms, receiving pools, pavilion and return approaches remain clear. The Dark Garden's main source ground rises only four source units, less than a full Minecraft block at the current scale; its existing low ground was retained rather than inventing a larger hill. These are targeted voxel adaptations, not a complete one-to-one terrain replacement.

Scenery revision 37 runs through the existing garden migration/preflight path, which preserves supported care assemblies and safely relocates bodies when terrain occupies their previous position. No saved item or block identifiers were renamed.

## Rebuilding and provenance

`art/sa2_02515/` contains selected supplied PRS inputs, decoded textures, source meshes, exact rollback PNGs, prior attachment envelopes and SHA-256 input identities. The complete gd_PC archive is not required at runtime. `art/extract_sa2_02515.py` re-extracts the selected toy/hat geometry, and `art/audit_sa2_gardens02515.py` reconstructs the original garden visual triangles. `scripts/generate_sa2_assets_02515.py` is the final artwork/model pass in the normal asset pipeline; `scripts/validate_sa2_assets_02515.py` checks the real generated resources.

The binary formats follow the primary SA Tools implementation: https://github.com/X-Hax/sa_tools/tree/master/Libraries/SAModel . The PC Neutral landtable is at 0x10740, Hero at 0x18dd0 and Dark at 0x15ac0 after REL relocation. Hero/Dark differ by 0x300 from the GC config offsets; the actual decoded PC tables were validated before export.

Music, singing, Chao motion clips, food/seed effects, market stock and gardening mechanics are unchanged. Replace the old mod JAR on both client and server. No separate resource pack or game-file installation is needed. Completed checks and limitations are in `TESTING.md`.
