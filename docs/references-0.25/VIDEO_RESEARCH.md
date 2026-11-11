# User video reference research — Chao Garden 0.25

This is an implementation reference derived from the two videos attached by the user. The videos were inspected across their entire duration using 147 overview frames, original-resolution scene frames, and 6 fps action strips. Room and staff descriptions below are direct visual observations. Any proposed Minecraft adaptation is identified as such. No game assets were extracted from the recording; these are screenshots and design observations.

## Sources and integrity

The three uploaded volumes `Captures.z01`, `Captures.z02`, and `Captures.zip` were joined with `zip -s 0` and their two MP4 members extracted with Python ZIP support. The full reconstructed archive CRC test passed. Input volumes were left unchanged. Exact video hashes, sizes, durations and resolution are recorded in `source-manifest.json`; full ffprobe output is next to each MP4.

| Video | Duration | Content |
| --- | ---: | --- |
| `SONIC ADVENTURE 2 2026-11-01 13-35-12.mp4` | 7:14.112 | Chao lobby, first neutral garden, baby care and reactions |
| `SONIC ADVENTURE 2 2026-11-01 13-45-09.mp4` | 4:59.308 | Lobby/garden approach, Kindergarten exterior and all demonstrated services |

Both videos are 2560×1440, variable frame rate, about 27.5 fps average, with audio. The Kindergarten bulletin credits explicitly identify **Chao World Extended**. Therefore visible expanded charts, products and menus must not automatically be described as universal unmodded SA2 features.

## Reference files and timing

- `garden-candidate/contacts/contact-01.jpg` through `contact-06.jpg`: entire first video, one overview sample per five seconds.
- `kindergarten-candidate/contacts/contact-01.jpg` through `contact-04.jpg`: entire second video at the same interval.
- `native-keyframes/`: 35 usable original-resolution PNG scene frames in the curated source (36 in the full working set). `native-keyframe-index.json` maps every current file to its source video and exact requested second. The black 161-second transition frame is excluded from the curated source.
- `motion/<sequence>/`: 6 fps central 1600×960 native crops and timestamped contact strips. Per-folder `sequence.json` records start, duration, sampling and crop. These nominal frame targets select nearby frames from a variable-rate source, not exact game animation keyframes.
- `market-detail/`: half-second samples from 4:30–4:45 of the second video for the Sell menu and empty state.

Overview sample labels are approximate **bucket midpoints**, starting at ~00:02.5, then ~00:07.5. They are not exact cuts or action boundaries. Native `-ss` keyframes are authoritative for the specific scene at the named second. Dense strip labels use the nominal sample target.

## Main timeline

| Source | Time | Observed content | Best reference |
| --- | --- | --- | --- |
| First | 0:00–0:19 | Circular glossy lobby; central transporter; garden gate | `garden-007-lobby-floor.png`, `garden-015-lobby-garden-gate.png` |
| First | 0:20–0:55 | Neutral garden entry; eggs; seated newborn and shells | `garden-022-garden-entry.png`, `garden-050-hatch-sit.png` |
| First | 1:13–1:24 | Child notices/inspects; question orb and head turns | `motion/question/` |
| First | 2:30–3:00 | Seated fruit holding and eating | `motion/eat/`, `garden-160-eat-fruit.png` |
| First | 3:03–3:38 | Low-skill child distressed in water; kicking and flailing | `motion/water/`, `garden-190-water-distress.png` |
| First | 3:40–3:50 | Petting followed by pickup/carry, not continuous petting | `motion/pet/` |
| First | 4:00–4:20 | Repeated petting and heart reaction | `motion/pet-true/` |
| First | 5:10 | Tiered waterfall/cliff ledges | `garden-310-waterfall-ledge.png` |
| First | 5:25–5:34 | Child carried facing carrier; closed eyes and small limb movements | `motion/carry/` |
| First | 5:33–5:56 | Child by transporter, mostly seated/head turning; not a verified uninterrupted crawl | `motion/crawl/`, `garden-350-garden-wide.png` |
| First | 6:20 | Waterfall and garden placement | `garden-380-garden-waterfall.png` |
| First | 6:22–6:30 | Sleepy child, pickup, throw/landing, then side-curled sleep | `motion/sleep-confirmed/` |
| First | 6:27–6:30 | Clearest sleep posture, gold spiral orb then round orb | `motion/sleep-confirmed/contacts/strip-03.jpg` |
| First | 6:30 onward | Cave entrance, flowers, lawn and cliffs | `garden-390-garden-cave.png` |
| Second | 0:35–0:41 | Hub Kindergarten gate, then school exterior/entrance and lobby | `kindergarten-039-lobby-angle.png`, `kindergarten-041-school-lobby.png` |
| Second | 0:41–1:00 | Lobby door tour and bulletin board | Native 042, 044, 047, 050, 051, 054, 058, 059 frames |
| Second | About 1:04–1:55 | Fortune teller and naming flow | `kindergarten-069-fortune-room.png` |
| Second | About 2:00–2:39 | Doctor and chart pages | `kindergarten-134-doctor-room.png` |
| Second | About 2:42–3:28 | Principal, care advice categories and pages | `kindergarten-167-principal-room.png` |
| Second | About 3:31–3:56 | Classroom lesson, leave/take interaction | `kindergarten-216-classroom.png`, `kindergarten-220-classroom-confirm.png` |
| Second | About 4:00–4:29 | Bulletin/market shop list and merchandise | `kindergarten-251-black-market-room.png`, overview sheets |
| Second | 4:33.75–4:38 | Sell selected; no eligible item empty state | `market-detail/contact-02.jpg` and `contact-03.jpg` |
| Second | 4:47–4:50 | Ideal native exterior views | `kindergarten-289-exit-reference.png` |

Approximate service boundaries are navigation aids rather than frame-accurate cuts. The listed native room frames are precise.

## Circular lobby

The lobby is a broad circle with a dark smoky purple, highly glossy/transparent-looking floor. Thin pale blue/white radial and grid lines cross it, interrupted by broad concentric gold rings. The seven-second native frame clearly shows inverted wood rim/posts and yellow stars/cloud-purple sky reflected below the walking surface. This is a specific visual target: a plain opaque dark floor or transparent pane over empty black void misses it.

The perimeter has wood posts and rim/panelling, dark blue dense starfields below the rim, and clouded purple/blue sky with scattered yellow stars above. The central transporter is a gold/turquoise gazebo: four slender columns, purple energy strands, a domed/crowned gold top, and a rainbow/purple circular active portal on the top and base. Garden portals sit around the outer rim.

Suggested Minecraft treatment: a thin collision/walk plane rendered as translucent glass, pale radial/grid detail and gold rings, with a decorative mirrored/stellar underside visible through it. This approximates the observed reflection without claiming a real planar-reflection shader.

## First neutral garden

The garden is an open, rolling lawn rather than a square flat arena. High grey/pale cliff walls have irregular stepped grassy caps and ledges. A dark cave entrance in one cliff is framed by flowers. The opposite side opens toward a cloudy ocean/sky horizon. A raised outer grassy ridge holds the Chao transporter.

A pond occupies the right side in the entry/wide view, with a pale stone/sand bank and shallow edge. A waterfall descends several cliff terraces into it. The waterfalls, cliff walls, and raised ridge provide the major height changes. Palms have gently bent diamond-textured trunks, about five long broad drooping leaves, and yellow coconuts. Pink, lavender and yellow flowers form small clusters near the cave, cliffs, and trees. Fruit, shells, toys and animals appear scattered around the lawn.

The recording is useful for scenery and behaviour, but prior explicit design rules still apply: do not silently reintroduce free egg sources or new animal mobs merely because they appear in SA2. Protected garden interaction must still permit releasing held Chao, offering food and placing intended toys; otherwise the newly built garden cannot support the demonstrated care loop.

## Kindergarten exterior

Use native 4:49 (`kindergarten-289-exit-reference.png`) as the best full facade reference.

- Symmetrical **blue clapboard** building, red scalloped/shingled pitched roof.
- **Three front gables**: large central sign/emblem gable and two smaller dormers.
- **Two small stone chimneys** with cream caps.
- **Four oval front windows**, two per side; the inner pair are taller. Green frames and white glazing.
- Two dormer windows with green flowerboxes. More raised flowerboxes below the front windows.
- **Six twisted/bulbous blue columns** across the facade, cream capitals/bases.
- Central green arched double entrance with Chao symbols.
- Yellow **Chao-in-laurel emblem**, not a clock, above the KINDERGARTEN sign.
- Cream cornice/dentil trim and brick/cream base.
- Rounded thick hedge rows border a yellow-brick approach, with steps at the doors and forest/sky backdrop.

## Kindergarten lobby

One open central hall with angled wall segments and doors around it is more faithful than a generic narrow corridor with cubbies. Warm cream plaster walls sit above rich brown vertical wood wainscot, with horizontal caps and skirting. A repeating peach/Chao ornamental frieze follows the wall top. The ceiling is high and inset/coffered, with small cream round lights.

An ornate teal rug has dense green vines, gold/orange flowers, and a patterned edge. A tan irregular/polygon tile walkway surrounds it. The walls carry colourful child drawings/Chao portraits, and narrow wood-framed arched windows with multicolour Chao stained-glass top panes. A red freestanding bulletin board stands near the classroom, on angled pale supports, with pinned white notices and star corners.

Distinct arch/chamfer-topped service doors:

| Service | Door design | Confirmed local adjacency |
| --- | --- | --- |
| Classroom | Blue, red trim, Chao drop emblem | Bulletin board to its left; Principal to its right |
| Principal | Brown, green trim, spectacles/moustache emblem | Classroom left; Health right on next angled wall |
| Health center | Teal/cyan, white/cream trim, cross | Principal left |
| Fortune teller | Vivid magenta, star | Near arched window/drawing |
| Exit | Large green double door, Chao drop emblems | Arched window then Black Market to its right |
| Black Market | Small dark teal cabinet/double doors, black/yellow signs | Right of Exit with window between; brushes and bucket beside it |

These adjacency observations do not establish exact compass bearings or a complete scaled plan. Native 042/044/047/051/054/059 are the evidence.

## Fortune teller

Native 1:09 is the visual reference. Deep purple folded drapes, gold patterned trim/coins/tassels, and waist ties surround a dim stone room with dark wood floor. A red high-backed chair has nested red/gold geometric framing. The table has red top cloth and grey draped front, a green/blue crystal ball on a patterned square coaster, and a three-candle gold candelabra at the left. Books, scrolls, cards, decorated urns/pots, potion jars, hanging masks, posters/calligraphy, a fishbowl and a Chao-skull-like ornament create a dense fortune-telling study.

The staff Chao is a distinct **adult** pale cyan Chao with a **gold circlet and purple forehead gem**, heavy purple eyelids, and pale/cream shoulder/neck accents. It is not a generic baby in a purple hood.

Observed service flow: greet → proposed lucky name → Yes accepts; rejecting proposals leads to more proposals and then a custom naming keyboard. A name visibly persists in later UI. The recording does not establish the maximum name length, and expanded naming UI may be CWE-specific.

## Doctor

Native 2:14 shows near-white upper walls with a green diamond border, dark teal lower wall tiles, and mint/green checkerboard floor. A cobalt medicine cabinet has glass-front doors, jars, and yellow handles. Blue shelves/cabinets contain cross-labelled supplies. A white tubular bed has mint mattress, yellow pillow and vivid magenta quilt. The matching magenta rolling stool/chair, blue exam table, medical/Chao posters, and blue-curtained window make the room recognisable.

The doctor is a distinct **adult** pale cyan Chao with **grey swept temple hair**, heavy white/grey brows, tired half-lidded eyes and a toothy grin. **Blue stethoscope/cross-like earpieces and tubing** are visible. A white coat and head reflector are not visible in the native frame.

Observed menu: See the doctor / Medical chart / Cancel. Chart pages include ability grades/stat bars plus Lifespan, personality/favourite/age/times transformed/condition/birthday, and records. Lifespan and expanded pages are visibly part of this CWE session. The demonstrated patient is healthy: there is no observed paid medicine or completed sickness cure transaction. Existing mod-tailored free health checks/care can remain, without inventing video evidence for treatment prices.

## Principal

Native 2:47 shows warm cream walls, wood and green-inset lower cabinetry, purple carpet with blue/teal Chao motifs, and a carved burgundy desk with repeated ovals. The tall chair is green. Desk props include books, open paper/book, green quill/inkwell, purple crystal, blue nameplate, and a blue/green globe on a gold stand. Trophies, certificate, medal, coloured books, sepia portraits, a Chao bust on green-marble pedestal, green curtains and a warm chandelier finish the study.

The principal is a distinct **adult** pale cyan Chao with **round silver/grey spectacles, bushy white moustache, pointed white goatee**, and yellow orb. Do not replace this with a generic topknot or gold glasses.

The service is category-based care advice followed by readable pages and Back/Next/Exit controls. Visible topics include What’s Chao?, Getting to Chao Garden, Raising Chao, Chao Food, and Getting Tree Fruits. This is appropriate to adapt to the Minecraft controls and mechanics already implemented.

## Classroom

Native 3:36 and 3:40 are the main references. Vivid sky blue walls **and ceiling** carry fluffy white clouds. Pale yellow/cream floor tiles alternate printed Chao heads, stars and hearts. A large toy-block town has red houses, green/blue pointed roofs, yellow towers, a green apple tree with red fruit, and blue block steps/sofa. Pink/teal toddler chairs, coloured blocks/balls, a toy-piano-like structure and hanging C H A O letters fill the scene. Several baby Chao sit together singing/gesturing.

The source has **no teacher**; the user explicitly asks for an invented matching adult teacher. Its appearance should be labelled a new design, not an observed staff recreation.

Observed UI: Today’s lesson: Song, with an offer-rotation countdown (10:28 visible). Leave Chao / Take Chao / Exit. The confirmation explicitly allows **up to four Chao** and says they cannot be taken home until lessons finish. After leaving the held Chao, Lesson in progress: Song shows 09:56, supporting a ten-minute lesson, and the student name appears. The full rotation period cannot be inferred from a remaining-time display alone. For the requested walkable adaptation, room entry should not force an interface; interaction with the invented teacher opens lesson UI.

## Black Market

Native 4:11 shows a small, dark, narrow grey-stone closet/stall. A red/pink patterned rug lies before a brown/red counter. The adult pale cyan merchant wears round dark sunglasses and a face mask. Clutter includes a red/gold chest with pearls, medicine bottles, yellow/red hanging toys/clothing, black/yellow signs, huge brushes, a framed portrait, small green mask/head, a snow globe/green-hat jar, shelves and cobwebs. This should not become a large generic wooden shop.

The UI has a burgundy heading, ring counter, Shopping / Sell items / Back. Shopping has left-hand tabs/categories and item lists; the right side shows a large product, price and short description. Examples visible during this recording are Hero Fruit 120 rings, Strong Seed 500, Jewel Silver Lens 2000, Headphones 2500, and a CWE Spartoi Lens 6000. Prior user-requested removals and the Minecraft emerald economy take precedence over automatically adding every recorded CWE product.

**Sell evidence is limited and precise:** Sell items is selected about 4:33.75. At about 4:34.75–4:35.5 it says “You don’t have anything to sell.” About 4:36–4:38 it asks the player to bring items they want to sell. No eligible-item list, sale value, confirmation, completed sale or buyback percentage appears. Therefore any bounded sell catalogue and emerald sale rates in the Minecraft mod are a deliberate adaptation and must not be attributed to the video.

## Baby action reference and limits

| Sequence folder | Interval | Direct observation | Limit |
| --- | --- | --- | --- |
| `hatch` | 0:44–0:56 | Seated newborn near two shell halves; low torso, forward feet, small arms | The egg cracking itself is obscured/offscreen; no full hatch cycle established |
| `question` | 1:13–1:24 | Yellow/green question orb, slow head turn toward attention/offered object | Object acceptance mechanics cannot be determined from one pose |
| `eat` | 2:30–3:00 | Seated, two hands near front, head/neck lean toward fruit then upright chew; staged fruit reduction | No hunger threshold formula established |
| `water` | 3:03–3:38 | Floating low body, upright head; alternate wide/squeezed eyes, crying/wavy mouth, arm/foot kicks | Do not grant swimming skill merely because the child remains afloat |
| `pet` | 3:40–3:50 | Petting followed by pickup/carry | Folder name does not mean an uninterrupted pet cycle |
| `pet-true` | 4:00–4:20 | Child standing/squatting with planted feet, arms near sides/front; small body/head dips during rubbing; growing/shrinking heart orb | No evidence for a large constant all-limb swing |
| `carry` | 5:25–5:34 | Faces carrier, feet forward, arms bent/lifted toward hands, small pendulum bob and occasional eyes squeezed shut | Carrier movement also moves the camera |
| `crawl` | 5:33–5:56 | Mostly seated/head turns near transporter | A clean uninterrupted crawling cycle was **not** verified; use existing progression rules plus other references |
| `sleepy` | 6:03–6:19 | Camera traverses lawn and sleepy child vicinity | Child often outside central crop |
| `sleep-confirmed` | 6:22–6:30 | Sleepy seated pose → pickup → throw/landing → curled low side-sleep at 6:27.3–6:29.8, closed eyes, gold spiral orb then sphere | Not one uninterrupted sleep cycle |

All dense action folders together contain **1,044 frames** at 6 fps. The central crop is designed for animation review, so use native keyframes and full-width overviews for off-centre scenery/characters. No original animation timings, interpolation data or exact skeletal pivots can be recovered from these screenshots. Implementations can reproduce visible posture/rhythm but should not claim frame-for-frame source animation import.

## Implementation checks derived from this review

1. Lobby surface visibly see-through/glossy, with an intentional underside/reflection treatment, pale grid and broad gold rings.
2. Neutral garden has rolling topography, cliff/cave, multi-tier waterfall and pond, palms, flowers and an open-sky side, and remains usable for Chao care in its protected Survival mode.
3. Kindergarten has the observed blue/red/cream exterior and six-column facade, open warm lobby with distinct doors, and individually themed accessible rooms.
4. Each existing staff role is an adult with the specific observed face/accessories. Add the requested adult teacher as an original design. Do not duplicate eyes through overlapping face layers.
5. Staff interaction opens services; merely walking into a room does not force UI.
6. Naming, health/chart review, principal advice, four-seat timed lessons, purchase and bounded adapted sell actions are functional. Explicitly distinguish recorded mechanics from Minecraft adaptations.
7. Baby motions have restrained pose changes and clear situational orb/eye/mouth states, with carry, fruit, distress and sleep distinct from ordinary idle. Do not replace missing crawl/hatch footage with an invented claim of direct observation.
8. Preserve prior acquisition restrictions, removed Chao variants, no-new-animal-mobs decision, and prior economy/companion rules unless the current user explicitly changes them.

Native second-video 00:35 (`kindergarten-035-school-exterior.png`) is the hub Kindergarten gate, despite its original filename. It is an excellent glass-floor and inverted wood/star reflection reference. The corrected native index uses `hub-kindergarten-gate`.
