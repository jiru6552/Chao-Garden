# Garden transporter appearance - 0.15

The existing `garden_block` keeps its garden roster, enrollment and activity behavior. Its model now follows the PC SA2 Chao-shaped transporter/departure machine shown on [Chao Island](https://chao-island.com/info-center/misc/chao-transporter.html), using an authored block-built interpretation.

The recognizable features are a pale cyan Chao monitor casing, yellow marker, red-framed black display, blue cheek controls, pale lower housing, a striped golden ramp and red receiving pad. The screen says CHAO / GARDEN because this block continues to manage gardens. It does not inherit the source game's departure/deletion feature.

The entire model fits one block: its overall height is 15.78 model units. Four-way facing makes the screen face the player on placement. Rotated outline/collision shapes match its equipment housing, head and marker while retaining the existing block ID and garden metadata. Old gardens load with the default north-facing orientation.

`generate_transporter_assets.py` authors 30 cuboids and eight native pixel textures. The activity asset generator invokes it last, so full regeneration retains the new design. The item uses the placed block model. Bounds, rotated-ramp bounds, UV ranges, texture decoding and four block-state rotations were checked. Final appearance should also be reviewed in the actual client.
