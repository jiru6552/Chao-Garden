# Chao Garden 0.25.5 verification

The production build and all 15 core suites passed, totaling 1,606,618 checks. Fresh native GameTest servers passed 245/245 cases with the base mod and 245/245 with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release adds a first playable Dark Garden and its protected Adventure care/return route, celestial lobby stairs and distinct Hero/Dark gateways, smooth curved school door profiles and thinner fitted frames, the Fortune-Teller curtain correction, and bulletin-board/corner-pillar relocation. Existing IDs, paired-door prediction/rollback, owned garden data and original-world return journals are retained. The native suite checks real scene placement, movement, picking, interactions and ownership rather than only matching generated files.

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource-domain, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the curved closed/open doors and both lobby approaches internally. No screenshot/video previews are included, as requested. This is automated and focused visual coverage; it does not claim exhaustive manual gameplay or a perfect reconstruction of the original game.

The supplied YouTube pages were reachable, but their video streams were unavailable. Reference notes identify the actual SA2 game imagery/model previews used instead, without claiming unseen video timestamps. Dark Garden is a first pass: its native-water waterfall shares the red pool tint; the original reference waterfall is blue. The custom storm sky hides the vanilla sun/moon while daytime skylight preserves ordinary crop and fruit growth.

Production artifact: `chao_garden-0.25.5.jar` (14,112,694 bytes).

SHA-256: `2fc63b58c29a572074b7ae59a65c3d852a5056eee4313bb242593743626b9529`

Detailed reports and closed logs are in `docs/validation-0.25.5/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads. The original annotated door sketch and attributed reference inputs are retained in `docs/references-0.25.5/`.
