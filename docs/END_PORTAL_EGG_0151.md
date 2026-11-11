# End portal egg surface - 0.15.1

The placed End Chao egg uses Minecraft Java 1.21.1's actual `RenderType.endPortal()` over the existing closed, rounded egg shell. It has no separately painted outline, border or frame. Ordinary, Nether, colored, shiny and other themed eggs retain their existing textured rendering and corrected shell mapping.

The vanilla End portal renderer supplies the implementation reference: `TheEndPortalRenderer`, `RenderType.END_PORTAL`, `BufferBuilder.beginElement`, and the `rendertype_end_portal` vertex/fragment shader files in the local official 1.21.1 game artifacts. The shader binds `minecraft:textures/environment/end_sky.png` and `minecraft:textures/entity/end_portal.png`, projects the texture from vertex positions, and animates fifteen differently transformed star layers using Minecraft's `GameTime` uniform. No copied shader or additional texture is required. Resource packs that replace the vanilla End portal appearance also affect this surface.

The shell geometry is reused without adding an overlapping pass. The shader's opaque output and normal depth test/write keep it occluded by nearby blocks. Its default back-face culling is valid for the outward-wound closed shell; the shell's positive signed volume is approximately 274.647 model-pixel cubed. The existing renderer transform flips two axes and therefore preserves winding. Shared mesh emission includes entity attributes, but the vanilla position-only buffer explicitly ignores attributes absent from its format. Because portal projection uses positions rather than shell UVs, the longitude join cannot stretch the portal pattern.

The egg continues its existing incubation wobble and shadow. The animated portal is visual; placement, recovery, incubation, hatching, ownership and saved heritage retain their existing behavior. Inventory artwork remains an item sprite, authored separately from the world shader surface.

Release validation should include actual client front/rear views plus two fixed-camera frames separated in time to distinguish shader animation from the shell's wobble. Compile and client outcomes are recorded by the coordinated release in `TESTING.md`.
