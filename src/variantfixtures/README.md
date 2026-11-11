# Identity-only integration gate fixture

Use a clean development run with:

```bash
./gradlew runGameTestServer -PchaoVariantFixtures=true
```

The opt-in profile generates a separate source set containing one trivial
`@Mod` entrypoint for every mod ID in the Chao theme catalog. This makes the
real NeoForge `ModList` and conditional recipe loader take their mod-present
branches. The fixture marker is `chao_variant_fixtures`.

The ordinary profile has no synthetic IDs, so only Nether and End are enabled
without optional integration mods. The server test compares creative visibility,
recipe presence, egg placement and custom-data bypass rejection against the
actual loaded mod list in both profiles.

These fixtures supply identities only. They do not implement third-party mods,
and a passing fixture run is not a compatibility test with those mods' actual
code. Keep real integration jars out of this synthetic run to avoid duplicate
mod IDs. Production jars contain only the main source set; fixture classes and
metadata are never included in an installable Chao Garden jar.
