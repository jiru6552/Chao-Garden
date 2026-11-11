# Adventure icon refinements - 0.14.0

The current symbols are original pixel art, drawn on native 8px and 16px grids. Referenced mod images were inspected as visual guides and are not copied into the output assets.

| Theme | Refined reference | Small/large treatment |
| --- | --- | --- |
| Cataclysm | Existing Ignis helmet | The two yellow center dots are joined into a continuous bar at 8px. The 16px image remains byte-identical to 0.13. |
| Born in Chaos | Seed of Chaos | Dark burgundy, irregular seed/bean silhouette with a scarlet core; replaces the previous skull at both sizes. |
| Nether | Netherrack egg | Rounded egg silhouette with pitted dark-red stone and warmer flecks. Matches the full egg surface motif. |
| End | End-portal egg | Purple rim surrounding a dark teal/black starfield, with sparse cyan/pale stars. Matches the full egg surface motif. |

## Seed of Chaos source verification

The publisher is mongoose_artist. The [official CurseForge project](https://www.curseforge.com/minecraft/mc-mods/born-in-chaos) and [official Modrinth project](https://modrinth.com/mod/1kcHy4de) provide the current 1.21.1 release. The [publisher version API](https://api.modrinth.com/v2/project/1kcHy4de/version?game_versions=%5B%221.21.1%22%5D) resolved `born_in_chaos_[Neoforge]_1.21.1_1.7.6.jar`, SHA1 `73704f38ac368c03716f9cc8f537470d3b352fa2`.

The actual item model `assets/born_in_chaos_v1/models/item/seedof_chaos.json` references `born_in_chaos_v1:item/chaossemt`. The corresponding 16px texture was visually inspected on a light background. It is a red, irregular seed shape with a vivid center and dark burgundy exterior, not a skull. Reference bytes and the publisher JAR were kept outside the source project and are not distributed.

The initially suggested [FoxyFox909 repository](https://github.com/FoxyFox909/Born-in-Chaos) has only a minimal source skeleton and no item textures; it was not used to infer the item appearance.

## Dimension palette contract

Nether uses outline `#47282c`, netherrack body `#854449`, dark pits `#5c3238`, warm patches `#b06662` and pale flecks `#ca8680`. End uses rim `#713f9c` with highlight `#b77be5`, portal interior `#101b29`/`#182c35`, and stars `#70c4bc`/`#d0eee1`. Both the chest badges and full item art share these materials and egg silhouettes.

## Validation

The affected 8px and 16px images are nonempty RGBA with binary transparency. The Cataclysm 8px middle bar has four adjacent gold pixels; its 16px image is unchanged. Native and enlarged output was inspected on muted and egg-shell backgrounds. All module icons remain distinct at each native size. Complete Nether/End item surfaces are authored by the shared egg generator, not by layering the tiny badge on an ordinary shell.
