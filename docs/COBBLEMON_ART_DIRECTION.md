# Cobblemon-inspired Chao art direction

The visual target is a Chao translated into Minecraft's shape language. The 0.2 model made the silhouette smoother, but its ellipsoids, interpolated normals, and glossy three-dimensional facial details read more like a small conventional 3D character. This pass uses deliberate cuboid masses and visible pixel painting instead.

## References actually inspected

The official Cobblemon source geometry definitions and 64×64 PNG textures for Jigglypuff, Rowlet, and Cleffa were inspected. Front and three-quarter illustrations were rendered from those source cubes for internal comparison. These illustrations are not Minecraft screenshots, and no reference geometry or textures are included in the Chao mod.

| Official reference | Main geometry | Facial treatment |
| --- | --- | --- |
| Jigglypuff | One 12×12×12 cube for the combined head/body | Flat 4×4 eye and 3×3 iris layers; tiny highlight detail |
| Rowlet | 13×11×12 head and 12×5×11 waist | Flat 2×3 eyes, 1×1 pupils, and a textured face plane |
| Cleffa | 9×6×9 head above a broader 10×6×9 torso | Flat 1×2 eyes and small pixel mouth layers |

These round source characters are translated into broad, unbroken box faces. Small appendages and selected rotations establish character; the core is not approximated with a stack of many overlapping corner boxes.

## Applied direction

- Give the Chao a clear main head volume and a compact torso. Shape changes should have a purpose in the silhouette.
- Use a few rotated cuboid sections for the distinctive swept crest, short arms, flat projecting feet, and thin textured wings.
- Paint the eyes and expression details on small front-facing planes. A single pixel highlight can supply eye sparkle without an additional glossy eyeball mesh.
- Keep visible texels proportionate to geometry. The reference head faces commonly use roughly one texture pixel per model unit, rather than stretching an entire high-resolution material swatch over each small face.
- Use restrained, coherent color clusters, rim highlights, and darker lower surfaces to imply softness. Avoid both smooth gradients and indiscriminate one-pixel noise.
- Keep animation expressive through poses, timing, eye states, and the floating emotion symbol. More geometry is not a substitute for character.

The inspiration is an art approach. The Chao's geometry and textures are original; Cobblemon is not a required dependency.

## Primary sources

- [Official Cobblemon custom model guide](https://www.cobblemon.com/en/guides)
- [Official model-creation tutorial](https://wiki.cobblemon.com/index.php/Tutorials/Creating_A_Model?veaction=edit)
- [Jigglypuff source assets](https://gitlab.com/cable-mc/cobblemon-assets/-/tree/master/blockbench/pokemon/gen1/0039_jigglypuff)
- [Rowlet source assets](https://gitlab.com/cable-mc/cobblemon-assets/-/tree/master/blockbench/pokemon/gen7/0722_rowlet)
- [Cleffa source assets](https://gitlab.com/cable-mc/cobblemon-assets/-/tree/master/blockbench/pokemon/gen2/0173_cleffa)
