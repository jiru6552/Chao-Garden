# Chao Garden 0.25.83

[**Download the JAR**](chao_garden-0.25.83.jar?raw=true) · [Source changes](chao-garden-0.25.83-source-changes.zip?raw=true) · [Maintenance report](MAINTENANCE_02583_DEV.md) · [Verification bundle](chao-garden-0.25.83-verification.zip?raw=true) · [Checksums](SHA256SUMS)

For Minecraft Java 1.21.1, NeoForge 21.1.219 and Java 21. Replace the previous Chao Garden JAR on both server and clients. Install external Lionfish API 3.1; adult equipped companions additionally use Accessories and its owo dependency. Aether is optional. Third-party mod binaries are not bundled.

Maintenance fixes: displaced fruit harvesting, stale care throttles, exhausted Nether/End companion cleanup and resource-reload attachment caches. Rendering and optional integration avoid repeated calculations and metadata lookups. Existing registered content and all 8,335 inherited resource files are preserved.

Final verification: clean build with 80 executed tasks, 4,884/4,884 native server tests, 7/7 minimal-runtime tests and 14/14 with real Aether. Actual client and two-player TCP checks, a copied older garden world and representative visuals passed. Phone/manual controls, shaders/custom packs, listening and authentic player-world/modpack upgrades still need manual checks. The report distinguishes those limits and preserves unsuccessful/superseded attempts.

[Accessories and poses](maintenance-visuals-final.png) · [Toy lighting](maintenance-lighting-final.png) · [Multiplayer and older garden](maintenance-network-world-final.png) · [Delivery manifest](DELIVERY_MANIFEST.json)

The JAR, source ZIP, verification ZIP, report, images and checksum files are copied byte-for-byte from the sealed delivery. This branch contains the complete source. The separate source ZIP is a cumulative overlay onto complete 0.25.43 commit fcde2f9d012499b2e2cb1ec1d50a7f58d4dc3ca2, not a standalone checkout; follow its included controls. Published README navigation is separate from the immutable tested source archive. No saved test worlds or account data are included.
