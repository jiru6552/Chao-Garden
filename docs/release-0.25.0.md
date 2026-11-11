# Chao Garden 0.25.0

This release replaces the previous lobby and Kindergarten with spaces built from the two supplied SA2 recordings and adds the first Neutral Garden. Current Minecraft/reference comparisons are in the [offline gallery](release-gallery-0.25.0.html).

- The lobby has a genuinely transparent circular floor, pale radial grid, gold rings, clouds and celestial scenery below, and an ornate central exit. Its Kindergarten and Neutral Garden arches are usable; Hero/Dark entrances remain reserved.
- The first Garden has a wide grass basin, pale tiered cliffs, palms with harvestable Garden Fruit, flowers, a real pond and waterfall, cave return and ridge transporter. Release your own Chao, place eggs and toys, and use normal care activities. Its protected Survival rules permit these actions while preserving the authored landscape. No starter Chao or eggs bypass the existing acquisition rules.
- The school is rebuilt as a blue, red-roofed campus with oval windows, a courtyard, an ornate teal-rug foyer and distinct walkable classroom, Doctor, Principal, Fortune-Teller and Black Market spaces. Native furniture and architectural pieces are included; external decoration mods are optional.
- The four staff are adult Chao with profession-specific meshes and one eye pair. Doctor, Principal and Fortune-Teller follow the recording; the academic Teacher is an original fitting addition. They acknowledge authorized interactions with short gestures.
- Services use compact room-visible menus after interaction. Medical charts, treatment, names, twelve principal topics with subpages, four shared lesson seats/countdowns, bulletin information and resident shopping remain server-authoritative. The resident market also supports whole-lot selling with a single-use quote and complete inventory-space check.
- Baby presentation now distinguishes seated quiet time, movement-driven crawl, small heart/petting reactions, supported carrying, question responses, rhythmic fruit bites, upright novice water distress and curled sleep. New hatch gestures do not apply to adult-built Omochao. Existing care, evolution, genes and companion systems are retained.
- Physical toys are now treated as navigation obstacles. This fixes Chao trying to walk through a TV to reach its screen; play remains voluntary and requires the ordinary front approach, without granting a reward for merely placing the toy. See [pathfinding notes](TOY_PATHFINDING_025.md).

## Adaptations and boundaries

The reference classroom has no visible teacher; the new teacher is deliberately invented. The recording shows the Black Market Sell entry and its empty state but does not reveal prices. Buyback is therefore an explicit Minecraft rule: one quarter of the plain catalog purchase value for the whole selected lot, rounded down. Chao, eggs, robot components/hearts, custom-state stacks, unknown goods and outputs of currently loaded recipes are refused. Failed payment-space checks leave the lot intact.

Garden visiting pauses neither ordinary loaded Chao care nor their normal decision making. Held/equipped aging behavior remains unchanged. Rooms are shared, with protected staff and four globally tracked classroom seats. The original return journal and game mode restore on exit; `/chao return` is available if needed.

The supplied recordings do not clearly show the actual shell break or a complete crawl cycle. Those retain the existing mechanics and authored planted crawl, refined to fit the observed baby posture. Source-derived actions and frame timestamps are documented in [baby motion notes](BABY_ANIMATIONS_025.md). Scenery and visual details are authored Minecraft adaptations, not an exact import of the original SA2 level mesh.

See [TESTING.md](../TESTING.md) for the completed runtime checks, [environment notes](ENVIRONMENTS_025.md), [Kindergarten details](KINDERGARTEN_025.md), [adult staff](STAFF_025.md) and [decoration research](DECORATION_RESEARCH_025.md). Earlier generated previews remain in earlier saved releases; current source includes the present captures and selected native reference frames.
