# Chao Garden 0.25.4 verification

The production build and all 14 core suites passed, totaling 1,603,458 checks. Fresh native GameTest servers passed 233/233 cases with the base mod and 233/233 with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

The native suite includes the new landscape and owned-world migration cases, eight-part transporter placement/removal and legacy upgrades, Hero device preservation and obstruction handling, clinic furnishings, Ionic column geometry and ceiling contacts, cornice placement, and shaped door picking. Earlier care, school, shop, garden and optional companion regressions remain enabled.

The full asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource-domain, texture, geometry and malformed-input guards passed. Production classes/resources were compared byte for byte with the tested artifact; development fixtures and test classes are excluded from the JAR.

The startup trace exposed repeated optimization of the detailed Ionic collision mesh; balanced unions now preserve its exact geometry and optimize once. An initial full run also exposed two stale landscape assertions, which were corrected while retaining the physical boundary checks. Failed-world entity data showed that the initial TV approach fixture had randomly developed hiccups, correctly disabling play while ill. That navigation fixture now receives the existing treatment immunity so unrelated random illness cannot interrupt the route. Both final full profiles passed with unchanged game AI, newborn speed, obstacles and timeout. The resident lifecycle fixture also waits for all six campus chunks to be entity-ticking before checking recovery, matching the mod's existing guard against duplicate residents; identity, position and stock assertions remain intact. Diagnostic logs are retained with the verification evidence.

No new screenshot/video gallery or native client visual review is claimed for this release. Final in-game appearance, lighting and feel can be checked directly with the supplied JAR, as requested.

Production artifact: `chao_garden-0.25.4.jar` (13,753,981 bytes).

SHA-256: `b9809c22ad1da131e60ee8f3e981cafa2d5123132c8965e37ad63159baa1aa65`

Detailed reports and closed logs are in `docs/validation-0.25.4/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads. Original feedback pictures are retained in `docs/references-0.25.4/feedback/`.
