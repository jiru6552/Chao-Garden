# Chao Garden 0.25.84

[**Download the JAR**](chao_garden-0.25.84.jar?raw=true) · [Source changes](chao-garden-0.25.84-source-changes.zip?raw=true) · [Development report](FIXES_02584_REPORT.md) · [Verification bundle](chao-garden-0.25.84-verification.zip?raw=true) · [Checksums](SHA256SUMS.txt)

For Minecraft Java 1.21.1, NeoForge 21.1.219 and Java 21. Replace the previous Chao Garden JAR on both server and clients. Retain required external Lionfish API 3.1; adult equipped companions additionally use Accessories and its owo dependency. Aether is optional. Third-party mod binaries are not bundled.

This update fixes the duck's missing opposite eye, enables physical player bumps for duck/lifesaver floats, lays placed tambourines flat, adds gradual baby Power features around the unchanged head, and adds safe operator appearance refresh commands. Existing saves, registered content and completed forms are retained.

Operator level 2: `/chao refresh ~`, `/chao refresh "Chao Name"`, or `/chao refresh held`. Nearest means a loaded Chao within 12 blocks; names select the current dimension and reject ambiguity. Held selects the filled carrier in the main hand. Refresh clears old reference-model overrides and synchronizes appearance without resetting identity, age, training, genes or equipment.

Verification: clean build passed, 4,938/4,938 native server tests passed, with focused, minimal and actual-Aether checks recorded separately. There are 50 actual toy/baby client frames, 34 live viewer references and a ten-case actual-input study. Controlled TCP checks verified packet behavior. Extreme-speed unequip still produced native position corrections, and the natural companion-AI client fixture did not record a hit. Those are explicit limitations, not passing claims. Phone controls, shaders, listening, continuous high-speed travel and broad modpacks still need manual checks. Read the development report for scope and retained failed attempts.

[Viewer/game front](fixes084-viewer-game-front.png) · [Viewer/game side](fixes084-viewer-game-right.png) · [Viewer/game back](fixes084-viewer-game-back.png) · [Baby detail](fixes084-baby-power-late-native-detail.png) · [Toys](fixes084-toys.png)

All 15 delivery artifacts are copied byte-for-byte from the sealed tested bundle. Its manifest retains prepublication local-delivery metadata. This branch contains complete source; the separate source ZIP is a cumulative overlay onto complete 0.25.43 commit fcde2f9d012499b2e2cb1ec1d50a7f58d4dc3ca2, not a standalone checkout. Follow its included controls for 21 inherited exact deletion guards. Published README navigation is separate from the immutable tested source archive. No saved test worlds or account data are included.
