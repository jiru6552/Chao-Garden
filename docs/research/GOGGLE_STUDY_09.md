# Create and Thaumcraft goggles — 0.9 reference study

Examined October 29, 2026. The requested placement is the upper forehead,
leaving the Chao's ordinary eyes visible. These are original cuboid
recreations adapted to the Chao rig. Source textures, JSON elements and
third-party class files are not imported into the mod or its editable models.

## Create: Engineer's Goggles

The primary reference is Create's own `mc1.21.1/dev` branch:

- [Wearable goggles model](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/resources/assets/create/models/block/goggles.json).
- [Wearable model texture](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/resources/assets/create/textures/item/goggles_model.png).
- [Item icon](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/resources/assets/create/textures/item/goggles.png).
- [Head-context model selection](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/content/equipment/goggles/GogglesModel.java).

The model was read, and both original texture images were enlarged with
nearest-neighbour sampling and visually inspected. The wearable frame has
seven brass pieces: paired upper/lower rim bars, two outer side bars, and a
broad centre bridge forming the inner edges of both lenses. The stepped
corners give the pair a rounded pixel outline. The worn lenses use black
stained glass; the item icon shows subdued cyan glass. A thin dark brown
strap wraps around the sides and rear.

The authored Chao interpretation preserves that seven-piece construction.
It uses two wide, smoky teal panes, warm brass rims, brighter upper rims and
a substantial central bridge. The muted teal conveys the icon's glass color
while retaining the darker worn appearance. No helmet, extra gear or raised
head-top mounting is added.

## Thaumcraft: Goggles of Revealing

The primary reference is Azanor's published
[Thaumcraft 6.1.BETA26 file](https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2629023),
the official Minecraft 1.12.2 release. Its JAR was inspected as a ZIP in memory;
only scratch reference images and the relevant item-class bytecode were
examined. Nothing was executed from the mod.

The actual resources inspected were:

- `assets/thaumcraft/textures/entity/armor/goggles.png` (128×64 armor atlas).
- `assets/thaumcraft/textures/items/goggles.png` (16×16 inventory icon).
- `assets/thaumcraft/textures/items/goggles_bauble.png` (16×16 front pair).
- The item's armor-texture reference, confirming the worn atlas above.

All three images show ochre/gold, rounded square rims around **dark purple**
lenses. The normal inventory icon and worn atlas show dark brown leather;
the atlas also includes a gold adjustment buckle. The bauble image gives a
clear front view of the two separate rings and small connecting bridge.
This is a different design from Create's wider glass apertures and broad
middle bar. The original worn appearance is principally texture-based;
the Chao recreation interprets those visible features as shallow 3D blocks.

The authored model has four stepped rim bars per near-square lens, a smaller
gold bridge, narrow purple panes, leather temple joins and a gold side buckle.
Its geometry remains distinct from Create's even when materials are ignored.
The reference establishes appearance, not a new compatible Thaumcraft port;
the existing `thaumcraft` presence gate remains in force.

## Fit and editable construction

The two designs are authored in `art/chao_goggles.py` using the generator's
native cuboid helper and four dedicated original material swatches. Create
uses 14 cuboids and Thaumcraft uses 17. Every part has a `goggles_` name so
equipment can be validated separately from the crown silhouette.

Both front pairs occupy local Y **−3.5 to −1.5**, wholly within the upper
forehead; the head top is −3.75 and ordinary eyes begin at −1.0. Frame centres
sit at Z −3.65, with the glass slightly behind at −3.55. Temple joins touch
both the frames and upper head, then connect to thin side and rear straps.
Nothing passes over the head top. The quarter-unit vertical gap above and
half-unit gap below keep the crowns and eyes separate from the goggles.

The scratch-only review sheets are `refs/goggles-09/create-textures-review.png`,
`thaumcraft-textures-review.png` and `thaumcraft-armor-atlas-review.png`.
They contain the inspected original textures for research and are not shipped
as Chao Garden assets. The final game captures verify the adapted appearance
and fit after generation.
