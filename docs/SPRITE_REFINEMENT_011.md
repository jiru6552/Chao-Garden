# Inventory art refinement — 0.11

The drive, gene and egg item IDs and gameplay effects are retained. Their item
models still use Minecraft's generated-item renderer, so the art works in the
inventory, on the ground and in either hand without an additional renderer.

## Chaos Drives

The four attached Sonic Adventure reference photographs show long colored
crystals between striped gray caps. The new original 16×16 sprites follow
that construction: an angled elongated prism, three broad colored facets,
gray metal ridges at both ends and a narrow colored seal. They replace the
previous short diamond with a thick band through its middle. The familiar
yellow Swim, purple Fly, green Run and red Power mapping is unchanged.

## Animal Gene tubes

Each of the 21 canonical DNA samples and its legacy item alias shares a native
32×32 sprite. A rounded glass tube encloses colored liquid and an opposing
two-strand helix, with narrow rungs, a bright meniscus, pale glass reflections
and a crimped silver cap. The sample color is also visible in the cap seal.
The 21 palettes and small sample notches distinguish the animals; the tooltip
continues to name the animal and its exact effects.

The outer glass walls, major highlights and helix strokes remain broad enough
to read in a normal inventory footprint. The visual index also includes tiny
16-pixel examples so the large previews do not conceal the small-scale result.
Only opaque foreground pixels and a transparent background are used; there
are no blurred edges, translucent halos or photographic pixels.

## Egg families

All 44 base/color/shiny/jewel egg icons and all 48 theme egg icons share a new
native 16×16 shell. Stepped light bands and a lit upper-left rim give the shell
volume. Three irregular patches wrap around ordinary colored shells; the
normal egg uses the pale shell and turquoise markings associated with Chao.
Shiny shells have stronger contrast and a separate glint. Jewel eggs use broad
cut facets rather than a dense wireframe. Themed eggs keep their individual
symbols, now clipped to the shaded shell with a small relief shadow.

The shared art lives in `art/item_sprite_art.py`. The original palette and
recipe generators call that helper; there is one shell implementation for
the entire egg catalog. All 92 registered egg appearances remain distinct.
The spawn-egg icon uses the same normal shell as the ordinary egg.

## Rebuilding and checking

Run `python3 scripts/generate_all_assets.py` to rebuild the project in its
normal resource order. The art helpers encode PNGs to memory, verify the
encoded image, flush it and publish it with an atomic replacement.

`python3 scripts/preview_item_sprites.py` renders the source art into
`docs/screenshots/item-sprites-0.11.png` without modifying data or resources.
It is an authored-art contact sheet, not a simulated Minecraft screenshot.

The existing gene validator checks canonical/legacy equality, unique samples,
native dimensions, transparent cutouts and preserved additive generation.
The variant validator checks unique themed icons and their recipes. Neither
the gameplay item registrations nor the theme availability rules change.
