# First Neutral Garden: spatial reference for 0.25.1

The original first recording is the primary evidence: `SONIC ADVENTURE 2 2026-11-01 13-35-12.mp4`. I reopened the earlier overview contacts and 47 newly extracted native 2560×1440 frames. Twenty-one especially useful frames and all nine new user PNGs are preserved here with exact source targets and SHA256 values in `reference-index.json`. These are reference images, not current Minecraft views.

The supplied recording has no overhead map or measured coordinates. The relationships below are observed; absolute Minecraft coordinates and block dimensions are a reconstruction. The old file named `garden-350-garden-wide.png` is misleading: at **05:50** the camera is close to the cave/wall/ridge, rather than showing an overall garden panorama.

## Spatial relationships

| Evidence | Observation | Reconstruction consequence |
|---|---|---|
| **00:22** / `first-022.png` | Arrival view has the stepped waterfall on the right and broad open cloud horizon on the left. | The previous garden was mirrored relative to this view. Face into the lawn from the cave with the waterfall to the player's right. |
| **05:09**, **05:48** / `first-309.png`, `first-348.png` | From near the transporter, the waterfall is at the distant left end; the long cave wall runs toward the right. | Put the waterfall at one end of the enclosing rock wall, and the transporter at the opposite end near the open edge. |
| **00:43**, **05:07**, **05:39**, **06:50** / `first-043.png`, `first-307.png`, `first-339.png` outside the curated set, `first-410.png` | The transporter stands on a gently raised grassy outer ridge beside a tapering rock wall and an unobstructed cloud/ocean horizon. | The transporter ridge is low and near the open edge, not a machine in the center of a bowl. Do not enclose this side with a tall cliff. |
| **01:01**, **05:34**, **05:50**, **06:30** / `first-061.png`, `first-334.png`, `first-350.png`, `first-390.png` | Main exit is a narrow, tall, bevelled recess in the continuous crag wall, with grass to the mouth and flowers outside. There is no external flat awning, giant landing shelf, or sign. | Use a compact mouth about 2–3 blocks wide and 3–4 high as a player-scale interpretation, with a short dark throat and a bevel in the wall. |
| **03:10**, **03:24**, **03:31** / `first-190.png`, `first-204.png`, `first-211.png` | Broad filled pond sits immediately beneath the waterfall; shallow edges expose a submerged stone bed, and the opposite bank returns to the lawn/main cliff. | Fill the volume below a level surface, rather than only drawing a surface sheet over hollow stepped cavities. Pond should take roughly a quarter to a third of the garden's width in these views. |
| **05:11**, **05:15**, **05:21**, **06:20** / `first-311.png`, `first-315.png`, `first-321.png`, `first-380.png` | Water descends a few broad pale rock terraces; foliage is chiefly small broad-leaf plants beside the water. A dark-looking recess is distinct from the main garden exit. | Use three or four broad ledges and a fully solid recessed wall behind the waterfall, per the user's explicit change. No palm trunk inside the stream. |
| **05:48**, **06:30** / `first-348.png`, `first-390.png` | Main wall is high, irregular, fissured crag. Grass occurs at the upper crest/ground slopes; it does not wrap every level in evenly spaced green stripes. | Replace the old regular turf staircase/ring treatment with irregular crags and a thin grassy crest. |

Looking from the open lawn toward the enclosing main wall, the visual order is **waterfall → cave exit → transporter ridge** from left to right. The open cloud/ocean side connects the outer end of the waterfall area to the transporter side. The central lawn stays comparatively open and only gently undulates.

## Agreed provisional Minecraft placement

These are implementation anchors agreed with the garden owner, not coordinates recovered from SA2.

| Feature | Minecraft placement | Scale and shape |
|---|---|---|
| Cave return | `(12,80,-36)`, facing south | Narrow 3-block mouth, 3–4 high, short throat; no protruding square awning or sign. |
| Arrival | `(12.5,80.1,-29.5)`, looking toward `+Z` | Waterfall west of the player appears on screen-right. |
| Main pond | Center `(-28,-2)` | Approximately 13×19 radii; fully filled below level 79. |
| Waterfall | Channel `x=-34..-30` | Four broad terraces at front `z=-17,-23,-29,-35`, levels 83,87,91,95; solid rock behind. |
| Transporter | `(34,82,15)` | Low two-block grassy outer ridge beside the open edge. |
| Main crags | North/back wall continuing east | Irregular roughly Y93–99 rock, thin turf crest, not a regular staircase. |
| Open cliff side | Front/south side and tapering transporter end | Cloud/ocean horizon visible; invisible collision boundary supplies safety. |

See `garden-layout.svg` for the schematic reconstruction. The drawing deliberately labels uncertain scale and inferred axes.

## New attachment findings

| Attachment | Visible issue to verify in actual 0.25.1 capture |
|---|---|
| `18.14.40` | Very low Kindergarten gateway angle; close all visible underside/interior roof faces and replace the flat black plane with a live portal. |
| `18.15.04` | Current garden gateway is a plain rectangular stone/concrete frame; compare directly with `wrea.png`. |
| `18.15.30` | Pond bank reveals its shallow stepped construction; actual new proof must inspect below the surface and confirm filled water columns. |
| `18.16.00` | Main cave has an oversized projecting square shelf/flat awning and upper rectangular void. |
| `18.16.10` | Waterfall source has a hollow/spilling water construction and a nearby palm intruding into the fall. |
| `18.16.19` | Flowers appear planted/floating in water; each plant must have valid dry support and no water cell. |
| `18.16.41` | Cave throat is much larger than source and contains a wooden sign. |
| `18.17.12` | School emblem is an incomplete gold C-shaped loop and title is partly behind the front cream band. Artwork correction should preserve a full gold disk/laurel and complete cyan silhouette. |
| `wrea.png` | Target garden hub gateway: jagged heavy stone arch, two slender white twisted/diamond shafts with white bowl planters and broad green leaves, small white Chao statues on irregular low stone plinths, and flower bowls. A starry/open portal fills the arch. |

The user's explicit Adventure-mode request supersedes the previous 0.25 protected-Survival implementation. Care, fruit and toy interactions still need normal authorized production admission in Adventure mode; the visual fixture must not switch to Creative or bypass permissions to obtain a prettier screenshot.
