# Hero Garden first-pass reference study

This study distinguishes the original Dreamcast garden from the Battle/modern layout. The screenshots below were downloaded and directly inspected as images. They are reference material, not runtime textures or recovered level geometry.

## Viewed sources

| Local image | Source and scope | SHA-256 |
| --- | --- | --- |
| `references-0.25.3/hero/sega-hero-00.jpg` | [SEGA's official 2001 Chao World gallery](https://sonic.sega.jp/sonicadv2/0601/shots_e.html), [original image](https://sonic.sega.jp/sonicadv2/0601/shots/chaohg00.jpg). Original SA2/Dreamcast promotional screenshot, 270×202. | `708f460b70111995265f76688a27a2797d3e98d2b4a79f6ac4b9823f7ba042b0` |
| `references-0.25.3/hero/sega-hero-01.jpg` | Same official gallery, [original image](https://sonic.sega.jp/sonicadv2/0601/shots/chaohg01.jpg). Original SA2/Dreamcast promotional screenshot, 270×202. | `6d3bd3964b8c84b9d850571b4507aa64a0f261c8094012bdd27a82bd60fe08a6` |
| `references-0.25.3/hero/battle-overview.jpg` | [Chao Island's archived SA2B overview image](https://chao.hippotank.com/sa2b/images/garden-hero-big.jpg), 1355×593. Community-hosted game screenshot; capture executable/ROM and mod configuration are not supplied. | `eeeb14bef6c4d08a5e6364af29d7574d5e3efa7c37922b01714f291b7a1e8e74` |

SEGA's official text confirms the floating pink-cloud setting and fountain theme. The first image directly shows a pink cloud sea, yellow-to-blue sky, lavender classical column and a small separate bell-tower island. The second shows a rock wall, lavender/white temple-like fountain backdrop with cyan emblem, blue water and lawn. These original-version pictures support the visual theme, not Battle's detailed ground plan.

## Battle overview: direct pixel observations

The overview places a large rounded blue pool on the left and a broad green lawn on the right. A narrow pale tile curb follows the water. A white/lavender fountain backdrop stands against the left rock wall; its emblem is cyan, and a blue water curtain enters the pool. A raised pavilion sits beside the pool in the foreground: pale columns support a shallow chamfered cap with an ornamental band. The lawn contains scattered palms and a fallen pale column. Rocky flanks surround an open gap facing dense pink clouds and a pale yellow sky. A small bell tower stands on a separate floating island beyond the lawn; the transporter is at the far lawn edge. A separate pale-framed exit is visible in the right rock wall. No river or bridge is visible in this overview.

## Version distinction and implementation choice

[Chao Island's modern/Dreamcast comparison](https://chao-island.com/info-center/basics/gardens-sa2) explicitly associates the cross-garden water channel and bridge with Dreamcast. Do not copy a bridge from the original-version screenshots into a Battle-oriented reconstruction by accident. Search also returned “Enhanced Hero Garden” and CWE evening images; those modded views were excluded as layout authorities.

For 0.25.3, use the Battle overview for a compact, playable first pass: rounded fountain pool, adjacent raised classical pavilion, clear lawn, rock flanks, pale stone architecture and a pink-cloud outlook. The separate exit should remain distinct from the fountain backdrop. A distant decorative bell island is a useful landmark. Widths, heights, travel distances and Minecraft block details are authored adaptations; this study does not claim an original mesh, exact coordinates, measured scale, recovered collision or finished one-to-one reconstruction.

No alignment, evolution-speed, reward-egg or unlock mechanic is specified by this visual study.
