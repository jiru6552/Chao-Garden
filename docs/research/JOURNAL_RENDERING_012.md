# Journal edge rendering

The panel and symbol PNGs are antialiased RGBA textures. The plain
`GuiGraphics.blit` overload used by `AdventureScreen` does not enable alpha
blending in Minecraft 1.21.1. Its `position_tex` fragment shader discards only
pixels with exactly zero alpha. Near-transparent Lanczos fringe pixels could
therefore appear as opaque dark dots or specks around circular symbols and
rounded panel corners.

`AdventureScreen` now enables the standard blend function for the panel and
symbol blits, then disables blending after each immediate draw. This applies to
the journal and the existing status, genetics and animal screens.

The UI generator also composites translucent decoration over the opaque shell
instead of overwriting the shell's alpha. Decorative rings and gloss are
clipped to the inner rounded shell. Shadow opacity remains outside the panel,
and invisible alpha values 1 and 2 produced by downsampling are cleared.

Validation: all four panel PNGs have fully opaque inset interiors, transparent
outermost corners, and the four panels plus symbol atlas contain no alpha-1/2
fringe pixels. Before/after raster inspection reproduced the old opaque fringe
and showed smooth, clean edges after blending. Real-client verification is
recorded with the release validation.
