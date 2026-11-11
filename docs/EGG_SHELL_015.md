# Placed egg shell repair — 0.15

In 0.15.1 this UV repair remains in place, while dimension egg artwork changes again: the Nether shell gets finer netherrack detail and the painted End rim is removed. Placed End eggs now use Minecraft's live End portal shader instead of their fallback shell atlas. See [dimension egg art](DIMENSION_EGG_ART_0151.md) and [End portal rendering](END_PORTAL_EGG_0151.md). The purple-rim comparison below records the original 0.15 repair.

The closing longitude panel of every ring previously interpolated from about U=59.5 back to U=0. That stretched almost the entire 64px shell texture across one narrow wedge. The End egg made the error particularly visible because its dark portal interior and purple rim have strong contrast.

`art/refine_egg_shell.py` keeps the existing closed shell geometry and gives each panel adjacent, unwrapped longitude coordinates. Closing panels finish at U=63.5; the first panel starts at U=0.5. Matching edge texels form a longitude gutter, and the flat polar caps sample a single cap color. In 0.15 the two painted End portal panels and purple rim were left intact, as was the existing interior artwork of Nether, normal, colored, shiny and jewel coats.

The postprocessor also updates the editable egg Blockbench project and its embedded preview texture. Run it after the ordinary asset generators. `scripts/validate_egg_shell.py` checks closed topology, bounded panel UV spans, six closing panels, every shell atlas edge and the exact Blockbench roundtrip. It deliberately restores the original UV stretch and a mismatching atlas edge to prove both regressions are rejected.

Egg textures have an independent coat selector in `ChaoEggRenderer`, so the official Chao body geometry/texture integration does not change which shell atlas a textured egg uses. The 0.15.1 End shader path bypasses that painted atlas when rendering the placed End shell.

The uploaded Sonic world pack contains no Chao egg geometry or hatching animation. Its `minecraft:egg` definition is the vanilla projectile, and its small egg item texture is used for the world’s leave-act item. Eggman/Eggrobo/eggmobile files are unrelated. Consequently the mod retains its own rounded egg and incubation wobble rather than importing an unrelated asset.
