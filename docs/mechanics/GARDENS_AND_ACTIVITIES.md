# Gardens, race courses and karate

The Chao Garden Transporter has six registration slots. Place the device and Activate garden to mark its chunk and the eight adjacent chunks, covering all buildable heights. No enclosure, floor or fruit tree is required. Existing saved enclosures retain their original footprints until explicitly reactivated. The built-in garden dimensions use their authored boundaries when their transporter is registered.

Owned Chao placed inside an active garden are detected automatically. A detected Chao uses a free slot in its keeper's garden and follows the existing boundary-containment behavior. Picking it up or explicitly moving it out frees that automatic slot. Unloading its chunk preserves the record until the body loads again. Unowned, equipped, boarded and busy Chao keep their existing lifecycle handling.

Dragging a carried Chao into a slot still reserves it manually, including while the Chao is carried. Existing registrations retain this behavior after upgrading. Left-clicking a roster icon dismisses that Chao until it leaves the area or is picked up and released again. With overlapping eligible gardens, use manual registration to choose a home.

The server stores one record per UUID and sends updated roster snapshots through the existing garden screen refresh. Discovery uses the saved boundaries and ownership checks. A full garden admits another detected Chao when a slot becomes available.

Garden records store UUIDs, owners and last locations, not duplicate Chao NBT. Carried and companion Chao do not become selectable copies. The lobby lists your registered Chao across gardens and dimensions; starting a contest briefly loads their saved locations and resolves the existing bodies. If a Chao is carried, missing or otherwise unavailable, the start fails safely.

## Build a race

1. Place a straight row of 2–8 **Starting Line** sections, all oriented the same way. Each section supplies one entrant slot. Place a matching straight **Ending Line** somewhere along a traversable route.
2. Hold **Race Markers** and right-click the starting line to choose the source. Place the first flag no more than **32 blocks** away. The preview line turns green or red according to range.
3. Continue placing flags. Each newly placed flag becomes the next source. Right-click an existing flag to resume or edit its route. Finish by right-clicking the ending line while holding linked flags.
4. Right-click the starting line with an ordinary/empty hand to open the lobby. Enter multiple Chao you own. Nearby multiplayer keepers may use the same lobby to fill free slots with their own registered Chao. Only the course host can start or cancel.

Courses support up to 64 links and must total at least eight blocks. Links cannot loop. Lines behave as low fences when idle and open for a contest. Flags are route controls; they do not remove terrain or create a track. Build a walkable course, using water, stairs and descents to reward different skills.

The race uses actual Chao navigation through the linked flags. Run governs land speed, Swim governs water speed, Power aids climbing and Fly aids descending. Virtual race stamina drains during the event; exhaustion greatly reduces pace. Entrants earn skill training and Race Experience through completing the course; active timed-out runners also receive modest training. Winners receive a bonus. A race times out after five minutes so blocked terrain cannot strand a participant forever.

## Build a dojo

Make exactly one connected **7×7 square of Karate Mats**. Right-click any tile to open its lobby. Up to eight Chao can enter a tournament of successive two-Chao bouts. Each bout has a 90-second limit. Power improves strikes, Swim improves defence, Fly improves dodging, Run improves attack frequency, and Stamina increases virtual bout health. Intelligence reduces zeal loss. Low zeal pauses attacks while the Chao recovers motivation.

Bouts use virtual health rather than hurting the actual pets. Chao approach, swing and push one another, with ring-out and timeout decisions. Every competitor receives skill training and Karate Experience; the champion receives a bonus.

## Safety and persistence

A Chao cannot enter two lobbies or become a companion/carried item while competing. The server validates owners, free slots, arena construction and button reach. Breaking a relevant arena/course block, disconnecting a keeper or canceling returns Chao safely. Original NoAI, invulnerability, stay and sleep flags are restored. A durable return-home journal and an entity-side copy recover interrupted activities after restart without creating duplicate Chao. Temporary chunk tickets expire or are explicitly released. Missing bodies are never manufactured from the ledger.

The race and karate stat roles follow Chao Island's [SA2 races](https://chao-island.com/info-center/training-sa2/races) and [karate guide](https://chao-island.com/info-center/training-sa2/karate.html). Course building, Minecraft movement, training rewards, garden registration and multiplayer lobbies are authored adaptations. They do not reproduce the console courses or exact hidden formulas.
