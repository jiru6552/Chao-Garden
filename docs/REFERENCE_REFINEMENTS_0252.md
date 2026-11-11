# Kindergarten follow-up reference brief — 0.25.2

This brief covers the first four screenshots and the later nine screenshots plus the expanded garden feedback. The attached pictures show the user's installed previous release; they are **before** evidence, not 0.25.2 captures. All thirteen screenshot originals were directly inspected. Their unchanged bytes and SHA-256 values are recorded in `references-0.25.2/reference-manifest.json`.

| Reference | Visible defect or requested change | What current verification must expose |
| --- | --- | --- |
| `2026-11-01_22.59.16.png` | A broad opening remains above the inner green stepped school arch and beneath the projecting cream facade lintel. The hallway's gold frieze and ceiling lantern are visible through it. | Exterior walking-height low upward views from **both** front corners, a centered view, and the corresponding interior underside. The reveal must be a continuous solid architectural surface, with no stretched temporary face or low collision cap. |
| `2026-11-01_23.00.04.png` | The opened green door shows the wrong-looking reverse art; opening/closing selection is reported as unreliable. This still cannot establish the interaction defect by itself. | Both broad faces of each mirrored leaf, open and closed; knobs and ornament stay readable in local face coordinates. Actual native camera rays to visible center, hinge edge, free edge and each height section must hit the rendered slab. Click the returned native hit result and observe pair prediction, authoritative acknowledgement and denied rollback. |
| `2026-11-01_23.01.15.png` | The Doctor's moustache has an asymmetric dark upper lobe in a downward view. The other upper lobe is light. | Native top-down views from both sides plus front and oblique views. Inspect the real lit model, not just the generated texture or a model-position assertion; compare equivalent upward-facing surfaces. |
| `2026-11-01_23.01.46.png` | The Fortune entrance arch is purple, but the full approach remains generic cream, brown and brightly lit until the room. The user asks for themed hallways, especially purple/dark/spooky/occult Fortune atmosphere. | The complete Fortune approach at its hub junction, middle and room end, with continuous motion along it. Each other staff approach needs a coherent destination theme and continuous walkable junctions. Dark Fortune lighting must retain legibility and staff interaction. |

The requested corridor direction is: welcoming entrance; playful classroom; clean mint/white clinic; scholarly principal; utility/storage Black Market closet; dark purple celestial/occult Fortune. This extends the existing rooms while keeping their staff, barriers, walking clearance and Adventure rules.

## Why previous passing checks did not catch these defects

The earlier `school-facade` shot was a wide/front view; it did not expose the oblique header gap shown by the user. A wall count or existence check does not prove a sealed reveal at that camera angle.

The previous native helper called `gameMode.useItemOn` with a `BlockHitResult` constructed at the selected owner's block center. Those actions genuinely tested native packets, complete-pair local prediction, acknowledgement and rollback, but they bypassed native camera selection. They therefore did **not** establish that a user's crosshair could hit every visible part of an overhanging leaf. New hitbox evidence must identify the native ray's exact block and location before dispatch; direct block-use methods or manufactured results are insufficient.

Endpoint door captures established open/closed states, but a single front view did not establish the reverse broad-face UV orientation after opening. New captures need both physical sides and all facings in a ray matrix. A UV test must inspect each face from that face's own viewing direction, rather than assume the same coordinate order for front and back.

The old Doctor front/oblique checks did not isolate the top faces under equal light. A mirrored shape can share positions while reversing winding or lighting; the current lit model must be reviewed from the newly supplied top-down angle.

The old Fortune room view established that the **room** was decorated. It did not establish a themed approach. Current evidence must follow the entire corridor, including its turn/junction, rather than only the entrance trim and destination.

## Evidence and release discipline

The machine-readable obligations are in `visual-review-contract-0.25.2.json`. They describe required evidence and do not declare a fix complete. The native runner owns a focused scene plan and genuine actions. Review reports must name exactly which unmodified current PNGs, native MP4 frame ranges, camera rays and source hashes were inspected. Contact sheets are review aids, not additional native frames or real-time playback.

Review current stills and continuous native mesh recordings. For door transitions, inspect every decoded native frame around each click/acknowledgement; report the actual frame count and time bounds. For longer walking views, state the sampling cadence and any consecutive-frame windows honestly. Do not infer visual PASS from model counts, generated assets, receipt strings or a test total.

Freeze the source, scene plan, capture receipts and owner reports before aggregation. Use the existing stable JSON snapshot and recursive report-reference guard, including each clip's `review_report` and `review_report_sha256`. If any closed input changes, regenerate dependent reports and validate the whole reference chain again. Historical 0.25/0.25.1 garden and baby recordings remain qualified historical evidence; they must not be relabelled as new 0.25.2 captures.

The full 288-ray geometry matrix may use a clearly labelled temporary isolated native door fixture in the real Kindergarten level, restored afterward. This exposes opened wall-side faces that are legitimately occluded by the authored corridor walls. Separate actual-school actions must target reachable front/back/open inward faces through native camera selection. Reports distinguish fixture coverage from authored-scene interaction; no claim that all fixture targets are user-reachable in the school is permitted.

## Expanded feedback: nine more screenshots and garden scale

The user's later notes extend this same 0.25.2 work. The following originals are also unchanged before evidence, not current captures.

| Reference | Directly observed / requested correction | Required current view or behavior |
| --- | --- | --- |
| `2026-11-01_23.01.53.png` | Generic cream/brown bent approach to clinic; user wants a waiting room. | Show seating/registration context at the turn and clinic threshold, with clear walking and interaction space. |
| `2026-11-01_23.02.03.png` | Generic long approach to Principal; user asks for awards/certificates. | Show deliberate awards/certificate displays along the approach and into the office. |
| `2026-11-01_23.02.08.png` | Generic bent approach to classroom; user asks for student work/class pictures. | Show student work and class photos on the actual approach, with educational artwork leading into the mountain/cloud room. |
| `2026-11-01_23.02.24.png` | Broad labelled `BLACK MARKET` entry remains; merchant is reported missing and not visible in this still. | A literal unlabelled cabinet/closet conceals the hallway. Opening it reveals a dark stocked passage. Confirm the actual merchant and normal shop interaction in the room; absence cannot be diagnosed from a scenery still alone. |
| `2026-11-01_23.02.32.png` | Upper patterned block/ring band is visibly discontinuous at the turn and meets neutral ceiling faces awkwardly. | Upward facing and oblique corner views of a complete aligned side band and appropriate top/bottom texture; tile-phase proof complements native views. |
| `2026-11-01_23.02.50.png` | Oblique machine view draws translucent tubing across the spiral surfaces in an incorrect-looking order. | A continuous native orbit showing near/far tubes and both portal disks; single front views cannot validate alpha ordering. |
| `2026-11-01_23.03.03.png` | Round statue plinths visibly float above the glass floor. | Both bases from a low oblique angle visibly contact floor/support, including underside. |
| `2026-11-01_23.04.16.png` | Statue's Chao head point appears blunt and squared under its floating orb. | Front/side close-up of a pointed head silhouette, attached coherent material and separate orb. |
| `2026-11-01_23.05.00.png` | Flat green leaves meet through the bowl rim; the flowers look like disconnected spheres with no visible stalks. | Top/side urn close-ups and a short native pass must show rooted foliage inside the opening and connected bloom stems. |

The first garden is still reported unlike the first Sonic Adventure 2/Battle garden and **much too large**. The latest message adds no new garden PNG, so use the original first video and its qualified primary reference frames. Current evidence needs matched player-height arrival views, overview, pool/cascade/exit landmarks and an actual walk between them. Report authored distances and the scale reduction; do not invent recovered original measurements. Preserve filled water, sealed unused waterfall cave, open cliff fall barriers, compact framed exits and Adventure admission.

Current visual owners now cover school, staff, lobby and garden. Prior garden/baby evidence remains historical; new 0.25.2 garden/lobby claims require new exact current captures. The new complete archive retains original 0.25 baby payload and old 0.25.1 textual records; duplicate 0.25.1 native media and heavy galleries are omitted. The current gallery embeds only new corrected views/clips and selected primary/before references.

For the hidden cabinet and merchant comparison, the verified original second video was revisited directly. A native nearest-decodable frame at the requested 04:07 target (`references-0.25.2/native-keyframes/kindergarten-247-market-cabinet.png`) shows the complete teal cabinet with brooms and bucket. The original 04:07 cabinet includes market labels; removing those labels is an explicit latest-user adaptation. Retained 04:38 counter and 00:47 child-art frames provide original merchant/stock and school-art context. Their exact source hashes, timing qualification and frame hashes are in `references-0.25.2/primary-video-additions.json`. Walkable destination-themed corridors are requested additions, not corridors recovered from the original game.
