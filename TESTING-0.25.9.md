# Chao Garden 0.25.9 verification

The production build and all 18 core suites passed, totaling 1,695,092 checks. Fresh native NeoForge GameTest servers passed 250/250 cases with the base mod and 250/250 with the pinned real Accessories 1.1.0-beta.54+1.21.1 and owo 0.12.15.6-beta.1+1.21 dependencies. These tests run the compiled production code in the development runtime. Packaging compares the installable JAR's classes and resources byte for byte with the compiled source and current assets.

This release refines the Hero Garden pavilion with slim lavender columns, matching native collision, a two-tier shallow cap, and a lower fountain top. Authored-room revision 34 applies the changes through the existing migration path. Care, ownership and original-world return regression coverage remains in place.

The original SA2 audio importer converts local game ADX files into a Minecraft resource pack for the Battle lobby, Neutral/Hero/Dark gardens, kindergarten and up to six classroom chorus tracks. Real synthetic ADX conversion produced all 11 Vorbis files with the expected stereo/mono layouts. Missing, ambiguous and corrupt inputs were rejected, and a previous output pack survived conversion failure.

An ordinary native Minecraft client loaded 5,860 block states with zero missing models and completed seven scenes, eight internal frames and eight saves, restoring the original return state. The pavilion, fountain, wide Hero view and classroom were visually inspected. No new screenshot or video preview gallery is included.

The client used nine-second synthetic audio imported by the production tool. All four requested area routes (lobby, Hero, Dark and kindergarten) activated. The classroom completed three sequential starts across 499 active ticks, without overlap, and stopped after exit. This validates actual SoundManager streaming/timing and cleanup using null OpenAL output; it is not a listening or authenticity test of original SA2 recordings.

The complete asset generation and validation pipeline passed in an independent source copy. All 6,595 resource files matched the production source byte for byte. Existing reference inputs and earlier verification records are retained. Development tests, smoke helpers, world fixtures and synthetic audio are excluded from the installable JAR.

Original SA2 recordings are not bundled. The previously supplied Neutral Garden recording remains; its exact OST rendition remains unverified. The other original area and chorus recordings still require the user's local game files. See docs/SA2-AUDIO-IMPORT.md. Separate learned solo singing is not implemented, and Black Market audio remains separately supplied. Existing arranged phrases remain the fallback without an imported chorus pack.

The Hero refinement used inspected Battle and official SEGA still images. No additional playable walkthrough was reviewed in this continuation. Minecraft proportions remain an interpretation. Details are in docs/references-0.25.9.md.

Production artifact: `chao_garden-0.25.9.jar` (14,228,072 bytes).

SHA-256: `b8b9fcbb08040f5ffd5d2bf0286b1bed92ad2baa4680451c88fa42f52276fa5b`

Closed logs and machine-readable results are in `docs/validation-0.25.9/`. Packaging checks unique safe ZIP paths, CRC integrity, the executable Gradle launcher, source hashes and exact JAR payloads.

Reproduction: use Java 21 and Gradle with the checked-in wrapper; run `./gradlew build`, the native `runGameTestServer` tasks with the desired dependency profile, `python3 scripts/test_sa2_audio_0259.py`, and `python3 scripts/generate_all_assets.py` in a separate source copy. Audio import requires FFmpeg and ffprobe. Native client checks require a graphical/OpenGL environment; the dev-only Audio0259VisualSmoke helper records the explicit playback assertions used here.
