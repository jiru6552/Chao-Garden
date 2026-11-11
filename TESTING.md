# Chao animation and sound previews

Requires Chao Garden 0.25.28 or later, cheats/operator permission level 2, and a nearby Chao on dry solid ground. The normal renderer and external Lionfish API 3.1 are used. No toy item or daycare lesson is required for these temporary previews.

Stand within 12 blocks of the Chao you want to preview. The nearest ordinary Chao is selected; other Chao are untouched.

```mcfunction
/chao testtoy list
/chao testtoy rattle once
/chao testtoy bubble_wand once
/chao testtoy maracas once
/chao testtoy bell once
/chao testtoy tambourine once
/chao testtoy castanets once
/chao testtoy tambourine loop
/chao testtoy stop
```

Omitting `once` also plays one complete 160-tick (8-second) cycle. `loop` repeats complete cycles, including raising and lowering the hands. Run another toy command to switch. `/chao testtoy <toy> stop` also works when that toy is the nearest active preview. Tab completion lists supported toys and modes.

The preview pauses autonomous AI and the Chao's age/care simulation, grants no rewards, does not teach lessons, and creates no real held/dropped toy or placed block. It preserves the original NoAI and Stay settings. Actual damage or petting still has its normal gameplay effects after interrupting the preview.

A preview stops on right-click interaction, damage, unloading or reloading the Chao, the initiating player disconnecting or changing dimensions, or that player moving more than 64 blocks away. Explicit stop searches for the nearest active preview within 12 blocks. Other players can see the animation and hear its normal sound cues.

Busy Chao, held items, cocoons, sleeping/eating Chao, school residents/enrolled students, companions and activities are rejected with an explanation. Retrieve a daycare student first. The source-reference comparison mode must be turned off with `/chao appearance garden` before previewing gameplay animations.

For listening: set Master and Friendly Creatures volume above zero. Enable subtitles if helpful. Listen for tone, volume, clipping and tap/shake synchronization; test multiple nearby Chao for overlapping sound. These commands play the whole performance; `/playsound` plays just an individual audio cue. The bubble wand uses its existing bubble-pop sound rather than a new instrument sound.

These commands preview presentation. They do not substitute for testing natural toy selection, approach/pathfinding, placed-block restoration or daycare learning/rewards. New or revised toys and animations must include corresponding preview support; see AGENTS.md.


## 0.25.29 instrument previews

With operator permission, stand within 12 blocks of an ordinary idle Chao:

```text
/chao testtoy list
/chao testtoy tambourine once
/chao testtoy castanets once
/chao testtoy cymbals once
/chao testtoy cymbals loop
/chao testtoy stop
```

The previews use the production poses and cues. Repeat with a baby and an adult, then check side views, interruption, and a second player's view. The tambourine's decorated face should face upward; castanet open lids should remain below and ahead of the head. Listen for one cymbal clash at each paired rim contact. No items or lessons are required for previews, and previews grant no rewards. A busy, enrolled, wet, airborne, or otherwise incompatible Chao is rejected.
