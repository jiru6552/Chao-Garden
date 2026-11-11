# Visual refinements - 0.12.0

Animal parts have clearer species silhouettes, retaining the established cuboid model style. Rabbit ears have stepped outer shapes and inset pink fronts. Other animal ears, crests, feathers, tails, feet and paws receive more pronounced shapes and readable details. Children retain smaller parts than adults. Sample acquisition and independent mixed slots use the existing rules in [animal parts](research/ANIMAL_PARTS_011.md).

All four Chaos Drive icons now stand vertically, with centered colored crystals between striped metal endcaps. All 92 egg icons share a broader, rounded upper outline. The 21 colored DNA vials retain their glass tube and helix while reducing reflections and small cap details; legacy animal item aliases use the same artwork. These are native pixel-art generator edits, not rescaled game photographs.

The journal previously drew antialiased panel and symbol textures without alpha blending. Almost transparent fringe pixels became opaque dots. Its decorative rings also extended beyond the rounded frame and replaced underlying alpha. The shared screen now blends both texture blits, and the generator composites decorations through the inner shell mask. This repairs the guide and the related inspection panels, including all ten circular symbols.

Existing Chao, eggs, carriers and animal mixtures retain their data. This release changes presentation without changing animal acquisition, training effects or item IDs.

The current preview study includes real Minecraft captures and a separately labelled authored sprite sheet. Read the [animal geometry notes](ANIMAL_VISUAL_REFINEMENT_012.md) and [sprite notes](SPRITE_REFINEMENT_012.md) for authoring details.

See [release validation](../TESTING.md) for checks and [detail gallery](detail-refinement-gallery.html) for full frames.
