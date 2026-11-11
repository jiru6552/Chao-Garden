# Shiny material and placed egg mark — 0.17

The existing shiny coats contained lighter colors and a static painted sparkle,
but used the same opaque cutout renderer as an ordinary Chao. They therefore
had no actual animated material response.

`ChaoShine` supplies a silver-white reflection using a source-alpha-masked
adaptation of Minecraft's entity foil shader and animated texture transform.
A dedicated neutral reflection
texture replaces the enchanted item's purple material. A 3× reflection UV
scale lets the ribbons cross the small face patches in the imported atlas;
the built-in 0.16× entity foil scale would leave those regions nearly flat.
The layer follows the
same sampled geometry and pose as the original body; custom animal/theme
overlays receive their own matching pass. It also applies to jewel coats,
including Moon, and to the held Chao because that path reuses the entity
renderer. Invisible creatures and cocoons do not receive a reflection pass.

The pass uses equal-depth testing and writes color only. Its shader also
samples the original source texture at the untransformed UV, discarding
transparent pixels before sampling the moving reflection. This matters for
opposite sides of a zero-thickness wing with different alpha silhouettes;
equal depth alone cannot distinguish those coincident faces. Vanilla glint speed and strength settings
continue to control the effect. This is a stylized moving reflection, not a
physically based reflective environment shader.

Detached white squares/crosses in the earlier review frames were traced to
the preexisting shiny-only `END_ROD` particle emitted every 80 ticks by
`ChaoEntity`. They floated away from the body and were not a reflection
alpha-mask failure. That fake shiny sparkle emitter was removed; the moving
surface material now supplies the shine. The separate Chaos aura is retained.
Source-alpha masking additionally protects real transparent model faces.

Placed shiny/jewel eggs use the same material. The End egg keeps its existing
animated portal shader. Egg inventory items use vanilla foil, with inherited
state taking precedence over the registry item's original color.

The old egg generator painted a white cross at texture y=5–15. The placed
egg's longitude atlas maps y=0 to its lower pole, putting the cross near the
base instead of its shoulder. The old cross was removed from 108 existing
egg atlases (4,320 changed pixels), plus the four new Moon atlases. The repair
was compared pixel-by-pixel with 0.16: only exact white cross pixels changed;
spots, wrap gutters and shell UVs were retained. The generator no longer
paints the cross. `art/build_shine_assets.py` reproduces the reflection texture
and repairs old generated atlases without changing their shell patterns.

The actual client comparison shows readable moving neutral reflections on
shiny red and blue Chao and on white/red shiny eggs, while the paired ordinary
coats remain matte. The golden jewel shell also reflects, and the neighboring
End egg retains its dark portal material. Shell patterns remain recognizable
and the old fixed lower sparkle is absent. An eight-frame sequence confirms
the highlight moves across the crest, body and feet instead of being painted
into the coat. The source-alpha shader compiled and loaded successfully in
the final client run. All eight final sequence frames were visually reviewed:
the highlights remain on the visible body and shell, no detached white puffs
remain, eye shapes stay readable, and no filled transparent-face corners were
observed in these views. The paired normal coats remain matte across the same
sequence. The final egg comparison also confirms the ordinary/shiny contrast,
the retained colored spots, and the unchanged portal material on the End egg.
