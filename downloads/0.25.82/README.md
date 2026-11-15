# Chao Garden 0.25.82

[**Download the JAR**](chao_garden-0.25.82.jar?raw=true) | [Source changes](chao-garden-0.25.82-source-changes.zip?raw=true) | [Development report](EQUIPPED_BABY_ALIGNMENT_02582_DEV.md) | [Verification bundle](chao-garden-0.25.82-verification.zip?raw=true) | [Checksums](SHA256SUMS)

Minecraft Java 1.21.1 / NeoForge / Java 21. Replace the previous Chao Garden JAR on both server and clients. Retain external Lionfish API; equipped companions require Accessories and its owo dependency. Aether is optional.

Baby-only server-side alignment: default +1 displayed point for credited hostile kills and -1 for villagers, known peaceful mobs and other players. Both kill magnitudes are configurable. Nether gives -1 per 1,200 equipped ticks; End gives +1, replaced by the classic Aether dimension when that mod is installed. Ambiguous neutral mobs are not guessed; explicit modpack classification tags are supported. Adults are unchanged.

All core tests passed, including 1,320 new checks. Focused native cases passed 14/14 both without and with real Aether 1.5.10; actual two-client replication and seeded Aether minute-boundary tests passed. The full regression passed 4,875/4,876; its sole failure is the inherited Nether companion extinguishing issue. Nine inherited morphology SHA preflights remain stale. Manual phone/PvP/modpack testing and a fresh-process restart of partly elapsed .82 counters remain.

[Actual-client comparison](client-comparison.png) | [Aether view](final-aether-view.png) | [Delivery manifest](DELIVERY_MANIFEST.json)

The source ZIP is a cumulative overlay onto complete 0.25.43 commit fcde2f9d012499b2e2cb1ec1d50a7f58d4dc3ca2, not a standalone checkout; follow its included application controls. This branch contains the complete source. Published README navigation is added separately from the immutable tested source ZIP. No third-party mod binaries or saved test worlds are bundled.
