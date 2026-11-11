# Chao Garden 0.25.2 verification

All seven current release gates passed, including the completed native visual review.

The final target is Minecraft Java 1.21.1, NeoForge 21.1.219 and Java 21.0.13+9, built with Gradle 9.2.1. The production JAR is 13,613,761 bytes with SHA-256:

`32b535d0ec165adab99d842bf6c20efb7ac15f64be927d8de918ebdda391de9f`

| Gate | Current result | Evidence |
| --- | --- | --- |
| Core rules | 14 suites; 1,603,445 checks passed | `docs/validation-0.25.2/core.json` |
| Fresh default server | 208/208 passed; 47.73 seconds of GameTests | `server-default.json` and closed log |
| Fresh Accessories server | 208/208 passed; 52.16 seconds of GameTests | `server-accessories.json` and closed log |
| Independent asset rebuild | All 6,375 resource files identical; no missing, extra or changed files | `asset-rebuild.json` |
| Production artifact | 282 production classes and 6,370 runtime resources match compiled/source payloads; CRC and exact domains passed | `production-jar.json` |
| Native client | One successful fresh v6 run; 88 scenes, 124 original PNGs, 36 contiguous clips | `runtime-client.json` |
| Visual review | 124/124 original screenshots and all 36 clips reviewed; no known blocking defects | `visual-review.json` |

The JSON reports named without a directory above are in `docs/validation-0.25.2/`. Both server profiles tested the exact JAR identified above. The Accessories profile used the actual Accessories 1.1.0-beta.54+1.21.1 and owo-lib 0.12.15.6-beta.1+1.21 dependencies, with API registration confirmed. Its 17 additional identity fixtures exercise configured identities; they are not full third-party mod implementations. No furnishing mod is required.

## Native evidence and its scope

The final capture used a fresh Minecraft client and integrated world on Xvfb/Mesa llvmpipe at 1280×720. Its development capture harness observes real game rendering and input. All 282 production class payloads and 6,370 runtime resource payloads match the tested release JAR; capture helpers and development dimension fixtures are excluded from that JAR.

The closed run prepared all three scenery revisions from zero to 27 through genuine initial hub entry. It verified 288 real door-selection rays, 36 phantom-hit cases and 18 actual door actions, including the cabinet and a server-denied action. Prediction, acknowledgement and rollback cover all fifteen door parts. Both ordinary merchant interactions selected the same existing resident through the crosshair and opened the native Market UI, before and after the hidden hallway walk. No merchant fixture actor or manufactured target was used. Ordinary keyboard travel completed the Fortune hallway, hidden shop approach, both compact Garden paths and both hub/Garden portal journeys. The original visit journal was restored on completion.

The full recording, `docs/motion-0.25.2/video/world-actual-final.mp4`, contains 16,351 native frames. Capture was requested at 30 Hz; its measured average is about 30.00145 Hz. The 36 clips retain 4,957 contiguous source frames, with measured local averages of about 27.4704–30.1590 Hz. All native presentation-time intervals are preserved; the maximum measured interval error is about 9.2×10⁻¹⁴ seconds. There is no interpolation, resampling, slow motion or montage. Audio was not captured. Capture cadence is not a game-performance benchmark.

The review aids identify exact decoded frame indices, native timestamps and RGB frame hashes. Door actions, controlled camera paths, lobby machine views and merchant UI sequences include every decoded frame. Other walks and water views use every fifth frame, the final frame and declared consecutive event windows. Scaled contacts and enlarged crops support visual review; they are separate from the original screenshots and are not additional native captures.

Completed owner reports account for 4,370 uniquely inspected frames out of the 4,957 recorded clip frames. All Door, School, Staff and Lobby clip frames were inspected. Garden review honestly samples 394 of its 981 frames across the complete six-clip span. This is declared frame-based inspection, not a claim of uninterrupted real-time playback or inspection of every Garden frame.

The supplied feedback originals and selected original-game frames remain labelled as references. The compact Garden has an authored 49×41-block bounding box and 1,745 terrain columns, compared with 101×91 and 8,279 before. Its scale follows visual relationships in the supplied footage; no original game mesh dimensions were recovered. The cave-side arrival and the raised 135-block overview are explicitly different views.

The full recording linked by this gallery contains the current 0.25.2 world review only. Nine retained 0.25.0 baby clips, timed frames and provenance are historical evidence in the source archive; their earlier full recording and duplicated derived previews remain in the prior complete release. See [release notes](docs/release-0.25.2.md) for exact retention scope and [prior verification](TESTING-0.25.1.md) for historical checks.

## Reproducibility and publication

The final native source cohort freezes 6,907 code, resource, build, validation, capture-plan and reference inputs. Its SHA-256 is `d026d0bdb4867cd79e94a95696c5f4b4df929efb34fce251d884d5e03697d0b1`; the scene plan SHA-256 is `aa5c208037b315bea6d1507bf3d43ec9723420b407964417bb2b9678ff52d34d`. Generated reports, galleries and release prose are outputs, excluded from the cohort to avoid hash cycles.

The independent rebuild ran the full asset pipeline in a disposable copy and compared every main resource byte, retained raw input and separate GameTest fixture. Migration cases include registered providers, Bells, linked activities, supported care assemblies, water toys, real grown fruit trees and live bodies. Oversized or unsupported migrations stop before terrain writes.

Publication requires all seven current gates above, matching tested JAR hashes, current screenshot/video hashes and recursive review-document closure. Nested review snapshots stay frozen through archive construction. The complete ZIP and verified source checkpoint compare their archived payload bytes against the captured source, check CRCs and unsafe/duplicate paths, and preserve the executable Gradle launcher. Earlier failed or focused diagnostic runs do not count as final release evidence.

Use `python PUBLISH-0.25.2.py` to reproduce the full proof bundle, or add `--source-checkpoint` for the verified source archive. This post-capture publication variant retains every integrity check in the original frozen writers, includes exact declared verification logs omitted by their generic source filter, and raises the earlier local archive-size budget for owned-file delivery. All captured inputs remain unchanged and the original frozen writers are retained.
