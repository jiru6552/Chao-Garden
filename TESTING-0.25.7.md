# Chao Garden 0.25.7 verification

The production build and all 17 core suites passed, totaling 1,692,620 checks. Fresh native GameTest servers passed 248/248 cases with the base mod and 248/248 with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release reverses the Hero staircase along the same lobby wall, replaces the rigid Dark gate flame geometry with animated fire effects, and places the school door leaves at the requested frame edges with consistent hinges and inward opening. A protected ambient group of baby Chao wanders, plays and joins the current lesson, including singing animations and effects. The lobby scene also appears on fresh arrivals without first turning toward the center. The native checks cover the affected placement, movement, interactions and actor separation alongside the retained care and ownership rules.

Automatic music routing is available for six areas. Bundled area recording: Neutral Garden (existing supplied Sonic Marketplace recording). The exact SA2 OST rendition of the supplied recording remains unverified. Original themes still requiring supplied audio: Chao Lobby, Hero Garden, Dark Garden, Chao Kindergarten, Black Market. The companion resourcepack template is in docs/music-pack0257/. The existing jukebox disc retains its supplied recording and ordinary controls.

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the changed scenery internally. No screenshot or video previews are included, as requested. This provides focused visual and automated coverage; the detailed reports state the specific checks and their limits.

Existing garden care, ownership and original-world return data are retained. Dark Garden remains a first pass, including its red-tinted native-water waterfall. Original reference inputs from earlier updates remain in the complete project.

Production artifact: `chao_garden-0.25.7.jar` (14,134,842 bytes).

SHA-256: `b5a195213f1c9335185db3fd6be2414c5f625b66552d1af8a5a1fe8b7d14705e`

Detailed reports and closed logs are in `docs/validation-0.25.7/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads.
