TrumpetTransitionRigTest definitive 0.25.48 / 0.25.49 comparison

Both isolated JVMs exit 1 with exactly the same report bytes:
Trumpet transition samples=267696 failures=37 minHandHeadPixels=-0.58569264
Report SHA-256: b64266310e866db7a3628192f974bcd9abf1eabfd4b36b797de53dfc37fa0bfa
Failure tick distribution: {0=11, 1=8, 2=6, 3=4, 4=3, 6=5}

Baseline uses the delivered 0.25.48 JAR for all production classes. Its test and four helper sources were compiled from exact parent git sources against that JAR, without any current build classes/resources on its classpath. Current uses hash-stable private snapshots of 49 main/test classes. Each run uses its own copied model/animation assets and working directory under /tmp. 48 and 49 asset inputs match byte-for-byte. The current resource classpath also snapshots the source resources.

See evidence.json for source, loaded class, dependency JAR and report hashes; resource-hashes.json for all 124 model/animation/catalog hashes; snapshot-class-hashes.json for stable current class snapshots; *-report.txt / *-run.log / *-class-load.log / *-classpath.txt for raw evidence. The baseline run was completed earlier in the same task and reused while the current private resource classpath was completed; elapsed duration is intentionally unspecified. No source edits, Gradle, client launches, or shared build/report writes were performed.
