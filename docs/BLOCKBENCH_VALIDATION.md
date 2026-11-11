# Editable Chao model validation — 0.9.0

The projects in `art/blockbench/` are real **Blockbench Generic Model (`free`)**
projects. Anatomy, classic evolution crests, body details and all 41 refined
themed crowns use editable **Cube primitives**. The main rectangular head
remains visible; a few chunky stepped or rotated crown blocks attach to it.
Create and Thaumcraft goggles are also cuboids, positioned on the upper forehead.
Thin expression/wing/glyph details and small emotion symbols also use meshes. Rig groups, embedded PNG textures, and the Chao animation
preview clips are included. These are the same shapes exported to Minecraft.

The runtime geometry is exported by `art/blockbench_to_runtime.py`. It converts
both native Cube primitives and mesh elements into the mod's native renderer;
no external model or animation mod is required.

## Open and export

Open a `.bbmodel` through **File → Open Model** in Blockbench. Textures are
embedded, so the project does not depend on an absolute development-machine
path. Hidden rig groups hold alternate evolutions, expressions, the three
Chaos forms and all 48 themed sets. Animal-gift variation geometry is absent. Keep the existing rig names
when changing the shape of a runtime variant.

After editing, save the Generic project and export its native runtime geometry:

```bash
python3 art/blockbench_to_runtime.py \
  art/blockbench/chao_adventure_master.bbmodel \
  src/main/resources/assets/chao_garden/models/entity/chao_mesh.json
```

The egg counterpart is `chao_egg.bbmodel` → `egg_mesh.json`. Add
`--texture-output OUTPUT.png` when exporting an edited embedded texture.

The renderer selects one inherited atlas for an ordinary Chao, one canonical
theme atlas for active themed heritage, or a Light/Angel/Devil atlas for a Chaos Chao. Keep edited geometry mapped
to the first embedded texture; additional texture pages are not imported as
separate rendering passes.

`chao_mod_variants.bbmodel` previews Create by default and embeds its atlas.
To inspect another theme, hide the previous theme and enable its
`variant_<id>_head`, `variant_<id>_body` and `variant_<id>_marker` groups, then
replace the first texture with `textures/entity/chao/variant_<id>.png` from the
mod assets. The seven Chao previews share the same full rig; hidden groups
remain part of the geometry export. Keep the ordinary `head` cube element
and head rig group visible for every theme. Its selected crown/goggle blocks
attach to the main head; keep eye and expression groups enabled. The six
designs outside the 0.9 art pass retain their earlier head details.

Cube primitives can be edited normally through their **From/To, Size, origin,
rotation, inflation, stretch, and UV** controls. There is no need to convert a
new cube to a mesh. Per-face UV rectangles, 90-degree face-UV rotations, mirrored
UV endpoints, and standard Box UV layouts with `uv_offset`/`mirror_uv` are
supported. Null-texture or disabled faces are omitted. Zero-depth panes are
valid: zero-area edge faces are skipped, and coincident opposite faces become
one two-sided native face to avoid z-fighting.

Blockbench uses Y-up coordinates. The bridge converts positions, group pivots,
rest rotations, and winding to Minecraft's model coordinates. Both the 4.x
embedded-group outliner and 5.x flat `groups[]` representation are supported.
The six Chao animation clips remain editable **Blockbench previews**. Gameplay
animation is procedural Java code driven by the named rig; changing an editor
preview clip does not automatically change its gameplay animation.

## Run the checks

Python 3 and Pillow are required:

```bash
python3 -m pip install Pillow
python3 scripts/validate_models.py
python3 scripts/validate_head_morphs.py
```

Use `--no-roundtrip` while an edited project has not yet been exported into its
runtime counterpart. Use `--no-style-checks` when deliberately experimenting
outside this release's Chao art contract. Structural validity and the exporter
checks remain available for those projects.

The checks cover:

- Generic/free metadata, actual Cube/mesh elements, unique UUIDs, complete
  outliner references, and a non-cyclic rig hierarchy.
- Finite cube bounds, origins, rotations, inflation/stretch, active faces,
  valid texture indices, UV rotations, per-face rectangles, and Box UV bounds.
- Finite mesh coordinates, valid vertex references, per-vertex UV maps, and
  embedded PNG decoding.
- Nonzero face area, consistent edge winding, and outward orientation for
  closed connected surfaces. Open face patches have no global inside/outside;
  zero-depth Cube planes are explicitly exercised without weakening the
  closed-mesh checks.
- No duplicate coplanar native Cube faces that would cause z-fighting.
- Valid animation targets and keyframe timing; static egg projects can have
  zero animation clips.
- Runtime parents, pivots, rest rotations, flat/smooth shading, geometry, face
  order, winding, UV pairing, and emissive material behavior.
- Exact unmodified export parity, equivalent 5.x saved-file layouts, and
  export after custom helper metadata is removed by an ordinary editor save.
- Real edits to Cube bounds or mesh vertices, UVs, bone pivots, and rest
  rotations reaching the native runtime format.
- The public command-line exporter and byte-for-byte preservation of the
  extracted embedded PNG.
- Presence of all 48 named theme sets, independent head/body/marker groups,
  canonical texture resources and matching catalog entries.
- Native rectangular cuboid construction for each refined crown, its attachment
  to the intact head, and the generated 41-profile manifest. Forehead goggles
  are checked separately for placement and expression clearance.

An independent golden-coordinate Cube fixture also checks all three rotation
axes, a changed object origin, rescaled 45-degree rotation, inflation/stretch,
Box UV and project-inherited Box UV, UV mirroring, reversed UV rectangles,
90-degree UV corner association, active/null/disabled faces, zero-depth planes,
and inflated planes becoming real closed cuboids. Deliberately corrupt editable
and native data must be rejected.

The authored Chao release art contract is checked separately from generic file
validity: half-unit Cube **Size** dimensions, one texture texel per cube model
unit, a broad shallow head, compact cuboid torso and limbs, low swept crest,
and the ordinary Chao's cube emotion marker. The refined crowns are authored
block assemblies, with their profiles recorded in `chao_head_morphs.json`.
Their flat ends and a few stepped or angled sections retain the add-on style. Floating gears, crystals, gauges, runes and lanterns
remain separate emotion-marker silhouettes. Most expression planes use two texels per model unit;
the broader primary eyes deliberately map 2.5×3 units onto 4×6 texels. Positions
and pivots may use fractional units. This is an authored art contract informed
by the DLC and original-game studies, not a source-format restriction.

Temporary roundtrip files are created outside the source tree. The checks do not
overwrite the editable projects or shipped runtime assets.

## Validation record

The release contains eight projects: seven Chao form previews sharing the full
rig and one separate egg project. Cube density checks apply to the anatomy,
crown and goggle cuboids. Both primary eye
planes remain 2.5×3 model units mapped to 4×6 texture texels.

The dedicated head-style validator checks the generated manifest, exported
rectangular crown blocks, main-head visibility and forehead equipment. The production-animation pose sweep checks the
actual `ChaoModel.setupAnim` transforms against the exported anatomy, including
refined head geometry and the lowered hands. Floating emotion markers are
excluded from head anatomy. Saved-model validation, animation clearance and
Minecraft rendering are separate checks; their current results and reproduction
commands are recorded in [TESTING.md](../TESTING.md).

## Scope of the evidence

This is saved-file, geometry, and exporter validation. It does **not** claim that
the Blockbench desktop application was automated or its full parser ran here.
Minecraft client rendering is checked separately in the release smoke test.
Official DLC images were inspected as visual references; no DLC source geometry,
textures or animation keyframes were extracted. Earlier Cobblemon references are
historical background. Audio provenance is documented separately in `AUDIO_CREDITS.md`.

Primary technical references:

- [Blockbench Generic format capabilities](https://github.com/JannisX11/blockbench/blob/master/js/formats/generic.ts)
- [Project codec and 4.x/5.x compatibility](https://github.com/JannisX11/blockbench/blob/master/js/formats/bbmodel.js)
- [Cube and CubeFace serialization/UV mapping](https://github.com/JannisX11/blockbench/blob/master/js/outliner/types/cube.js)
- [Mesh and MeshFace serialization](https://github.com/JannisX11/blockbench/blob/master/js/outliner/types/mesh.js)
- [Mesh API types and winding](https://github.com/JannisX11/blockbench/blob/master/types/custom/mesh.d.ts)
- [Official Cobblemon custom-model guides](https://www.cobblemon.com/en/guides)
