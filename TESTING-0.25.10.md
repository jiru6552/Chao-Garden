# Chao Garden 0.25.10 verification

The production build and all 18 core suites passed, totaling 1,694,873 checks. Fresh native NeoForge GameTest servers passed 254/254 required cases with the base mod and 254/254 with real Accessories 1.1.0-beta.54+1.21.1 and owo 0.12.15.6-beta.1+1.21. These runs use the compiled production source in the development runtime. Packaging verifies that every included production class and resource matches the installable JAR byte for byte, and that no expected production classes are missing.

Hero Garden Fruit and Dark Garden Fruit now provide the child's +0.15 / −0.15 affinity shift, retaining +40 Stamina in both food profiles. Separate alignment fruit/seed/orchard registry entries and acquisition resources are removed. Native NBT tests load old stacks with counts and custom components intact, and old block palettes with pod age/facing, expanded crowns and seedling stage intact. Actual Chao meals apply the benefit only after eating and exactly once. Core cases also check adult affinity locking, bounds, profiles and tooltips.

The Hero pool rim now covers every land cell directly adjacent to pool water. A native regression deliberately restores the two lawn-facing gaps, rebuilds the garden and checks the complete outline for marble and full collision. The Neutral Garden has pale exposed cascade limestone, nine broad-leaf plants on dry solid supports, lighter blue-green water, and flower clusters kept near ground level. Native checks retain the existing water sources, geometry, palm count and migration/care protections. Authored-room revision 35 applies the scenery changes.

An ordinary native Minecraft client loaded 5,822 block states with zero missing models and completed five scenes, five internal frames and five saves, restoring the original return state. Entry, waterfall, wide lawn, transporter and Hero pool-border views were individually inspected and compared with the 0.25.9 baseline and retained reference frames. The final food tag was subsequently refreshed from the generator output and loaded in both final server profiles; production code, models and textures were unchanged after visual review. No new screenshot or video preview gallery is included.

The complete asset generation and validation pipeline passed in an independent source copy. All 6,559 resource files and 276 Java source files matched the production source byte for byte. Original reference inputs and prior validation records are retained. Development tests, smoke helpers and disposable world fixtures are excluded from the installable JAR.

The visual review uses five native frames from the user's supplied SA2 Battle recording and the attached Hero pool screenshot. Earlier requested layout changes remain, including the solid wall behind the waterfall and compact exit. This is a Minecraft reconstruction, not a claim of exact source geometry. The scenic water palette is shared with kindergarten decorative water. See docs/references-0.25.10.md. Audio functionality is unchanged from 0.25.9 and was not re-audited for original-recording authenticity in this release.

Production artifact: `chao_garden-0.25.10.jar` (14,217,739 bytes).

SHA-256: `4c14a8b1bd744d3bea20cbe8122f5eee09e706153d50cb1765c98db98571d065`

Closed logs, source changes and machine-readable results are in `docs/validation-0.25.10/`. Packaging checks unique safe ZIP paths, CRC integrity, the executable Gradle launcher, source hashes, the complete production class set and exact JAR payloads.

Reproduction: use Java 21 and the checked-in Gradle wrapper; run `./gradlew build`, native `runGameTestServer` with the desired dependency profile, and `python3 scripts/generate_all_assets.py` in a separate source copy with the JDK available. Native client checks require a graphical/OpenGL environment and the dev-only World0253VisualSmoke harness; fixture worlds and internal captures are not release assets.
