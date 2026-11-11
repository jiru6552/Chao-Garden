# Chao Garden 0.25.6 verification

The production build and all 16 core suites passed, totaling 1,693,183 checks. Fresh native GameTest servers passed 245/245 cases with the base mod and 245/245 with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release moves both garden stairways along the lobby wall, joins their railings at turns and landings, removes the obsolete stair support blocks, and rebuilds the Hero and Dark gates from the supplied game pictures. It also attaches Dark fruit to visible branches, restricts tombstone engravings to the front and back, and corrects the Ionic capital and shaft clearances. Existing IDs, garden care data and original-world return journals are retained. The native suite checks placement, movement, interactions and ownership alongside the asset and geometry guards.

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource-domain, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the changed scenery internally. No screenshot/video previews are included, as requested. This is focused visual and automated coverage; it does not claim exhaustive manual gameplay or a perfect reconstruction of the original game.

The attached Hero and Dark gate pictures are the primary visual references for this update. Dark Garden remains a first pass: its native-water waterfall shares the red pool tint. Its custom storm sky hides the vanilla sun and moon while logical daytime skylight preserves ordinary crop and fruit growth.

Production artifact: `chao_garden-0.25.6.jar` (14,132,814 bytes).

SHA-256: `a3b455b5cf73aa1408dce5cc0298df0f70aeb9c73729eeb425f0a8279bd47565`

Detailed reports and closed logs are in `docs/validation-0.25.6/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads. The user-supplied feedback and gate reference pictures are retained in `docs/references-0.25.6/`.
