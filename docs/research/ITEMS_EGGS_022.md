# Item and egg audit — 0.22.0

The restored 0.21 resource tree is the visual control. The native artwork was
already coherent: 65 egg icons, 52 DNA icons (including compatibility aliases),
four upright drives, 76 fruit/seed icons and 62 other sprites. All 259 incoming
sprites were inspected on whole native-pixel contact sheets. This review did
not justify another redesign of the fruit silhouettes, DNA detail, drive
channels, seed kernels, rounded egg outline, journal or small item symbols.

The only inventory corrections are four pixels: the final shell glare at
`(5,5)` overwrote canonical icon ink on the Aether, Ars Nouveau, Iron's Spells
and Draconic Evolution egg items. `item_sprite_art.themed_egg` now protects
every opaque icon pixel before applying the shell's highlights. The finalizer
restores those four pixels; all other item pixels remain identical to 0.21.
The inventory glyph and the Chao's emblem use the same canonical icon source.
Placed themed shells retain their existing theme palettes/spots; the gallery
must describe those as palette consistency rather than claiming a placed-shell
glyph feature.

Three distinct lifecycle cocoon atlases use the original model's material tile
at `[64,32,32,32]` inside its 256×256 atlas. Evolution uses yellow through aqua
to blue, rebirth uses peach/orange through pink, and death uses ivory through
gray. Every two-pixel latitude band is constant across all 32 longitude pixels;
both poles are uniform and the entire sampled surface is opaque. The remaining
atlas is transparent. The original cocoon's 208 vertices, 220 faces and UVs are
unchanged. The actual renderer selects the lifecycle palette and excludes coat
or shiny layers from cocoons. The [Cocoon reference](https://chao-island.com/wiki/Cocoon)
informs these stage colors; the atlas pixels are original procedural artwork.

## Recovered verification

An isolated copy of the actual resources was finalized and independently
validated. The first build changes exactly seven resources: four egg PNGs and
the three new cocoon PNGs. All unrelated resources remain byte-identical. A
second build reproduces every resource byte, with no changed or extra files.
No live resource writer was run by the item audit owner.

The independent validator passed **25,839 checks** before obsolete-sprite
pruning. It measured 314 item models, 389 resolved own texture bindings, 25
additional block textures used by item parents, 683 canonical glyph pixels,
191 placed egg atlases, 86 identical shiny/base albedo pairs, 468 shell UV
corners and 3,216 cocoon UV/corner-centre samples. Nine deliberate corruptions
were rejected: icon repainting, missing binding, fractional-alpha fringe,
shell UV overflow, longitude seam, painted shiny stripe, cocoon alpha hole,
cocoon longitude discontinuity and wrong lifecycle palette. The final pipeline
removes nine superseded sprites; its exact resulting assertion count is
recorded by the final validator rather than copied from this isolated run.

The capture helper enumerates the real `chao_garden` item registry, distributes
main-inventory 27 slots followed by hotbar 9 slots, and waits for all real client
slots to match. A short last page keeps its unused slots empty. Legacy bare
carriers legitimately remove themselves, so their capture fixture uses a real,
unspawned Chao serialized through the production `ChaoCarrierItem.snapshot`
method. This exercises the actual filled held-Chao appearance; no production
inventory rule is bypassed. Egg scenes place actual `ChaoEggEntity` instances,
including all colors/jewels, both theme groups, both shiny theme groups and
close views of Nether, End, shiny and Moon shells.

Current resource/fixture evidence is in `ITEMS_EGGS_022_FIXTURE.json`. Final
actual-client materials, all registered inventory pages, placed shell views
and rendered PDF pages must be reviewed after the centralized release capture;
the restored old run's visual claims are not reused as new proof.
