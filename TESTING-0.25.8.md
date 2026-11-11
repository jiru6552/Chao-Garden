# Chao Garden 0.25.8 verification

The production build and all 18 core suites passed, totaling 1,695,092 checks. Fresh native GameTest servers passed 249/249 cases with the base mod and 249/249 with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release raises the Dark Garden fruit trees by one block, keeps all ambient baby Chao inside the classroom, rebuilds the classroom play-city props with separated geometry, and reverses the ornate school doors while closing their frame gaps. The door leaves retain their requested frame-edge placement. Existing care, ownership and player-student separation remain part of the native regression coverage.

Automatic music routing is available for six areas. Bundled area recording: Neutral Garden (existing supplied Sonic Marketplace recording). The exact SA2 OST rendition of the supplied recording remains unverified. Original themes still requiring supplied audio: Chao Lobby, Hero Garden, Dark Garden, Chao Kindergarten, Black Market. The companion resourcepack template is in docs/music-pack0257/. The existing jukebox disc retains its supplied recording and ordinary controls.

Song lessons use original pitched Chao-vocal phrases arranged from the supplied voice timbre, with notes and rests. These are a newly arranged adaptation rather than the original SA2 singing recording.

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the changed scenery internally. No screenshot or video previews are included, as requested. This provides focused visual and automated coverage; the detailed reports state the specific checks and their limits.

Existing garden care, ownership and original-world return data are retained. Dark Garden remains a first pass, including its red-tinted native-water waterfall. Original reference inputs from earlier updates remain in the complete project.

Production artifact: `chao_garden-0.25.8.jar` (14,218,021 bytes).

SHA-256: `e6fce99f54c3fe77345048bc6222107c61bde44b0a75928a6207964e2dc92fdb`

Detailed reports and closed logs are in `docs/validation-0.25.8/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads.
