# Dimension egg surfaces — 0.15.1

The Nether shell now uses irregular small red-stone clusters, dark porous cracks and restrained lighter flecks. Its material reference was the actual `assets/minecraft/textures/block/netherrack.png` inside the local vanilla Minecraft Java 1.21.1 client JAR. The palette and cluster scale were inspected against that primary game asset; the mod's five-color motif is authored separately in `art/dimension_egg_art.py`.

The previous 16px motif was enlarged four times onto the 64px shell, creating broad brick-like patches. The new motif repeats at native texel density around the shell. The inventory egg carries the same material through its edge with quiet curved shading, rather than an unvarying dark border. Its rounded silhouette is unchanged.

The End egg no longer has purple side bands, polar caps or an inventory outline. Its item and static fallback atlas use a continuous dark teal/black starfield with cyan and pale mint points. The existing ordinary egg geometry is retained. The placed egg's live portal effect is handled by the renderer; this authored atlas remains useful for the inventory image and static fallback.

The two 16×16 item sprites retain their exact earlier alpha masks and binary transparency. Both 64×64 fallback shell textures are fully opaque, with equal first/last columns on all 64 rows. The End outputs contain zero purple pixels under the review predicate (red and blue both over 1.12 times green). The Nether atlas has 2,416 horizontal color transitions, compared with 364 in the enlarged previous pattern; this records the denser material grain rather than claiming an automatic aesthetic score.

Full asset rebuilding uses the existing `scripts/generate_all_assets.py` pipeline. A targeted rebuild may call `surface(id, 64)` and `egg_sprite(id, EGG_ROWS)` for `nether` and `end`, then run `art/refine_egg_shell.py`. No Chao creature texture, badge, theme availability rule, egg incubation or save identifier changes are required for these surfaces.
