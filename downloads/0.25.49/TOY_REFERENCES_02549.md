# Toy reference inventory — 0.25.49

This inventory distinguishes source assets, code names and directly observed motion. Reusing an official model does not establish that an interaction was obtainable in unmodified SA2, and an animation label does not supply its original timing or joint curves.

## Available source models

The full supplied SA2 Battle data already contains most of the requested objects. No game executable was run to extract them. The existing offline decoder in `art/extract_sa2_02515.py` reads the relocated `ChaoMain.prs` Ninja models and `al_toy.prs` texture table. This update builds on that decoder rather than constructing replacements for available source meshes.

| Toy | Supplied source geometry | Source texture names / notes |
| --- | --- | --- |
| Rocking horse | Existing `art/sa2_02515/toys.json`, object `0x307950` | `horse01`–`horse04`; existing original-mesh adaptation points forward along local +Z and rocks about local X. |
| Toy car | `ChaoMain.prs`, object `0x6e0ac`, 80 triangles | `car01`–`car03`. |
| Sonic doll | `ChaoMain.prs`, object `0x68870`, 120 triangles | `soni01`–`soni04`. |
| Shadow doll | Original `al_toy.prs` contains `sh_doll01` and `sh_doll02` | An independently identifiable Shadow geometry root was not found in the inspected toy region. Any adaptation of the Sonic doll shape must be disclosed as an adaptation, rather than called an extracted Shadow model. |
| Toy broom | `ChaoMain.prs`, object `0x6c37c`, 72 triangles | The original mesh references `houki01`, `piano02`, `johro01` and `hanabi02`; the names do not make these unrelated props. |
| Picture book | Roots `0x679fc` and `0x676b8`, 22 triangles each; page `0x67370`, 2 triangles | `book01`–`book03`; 46 triangles together. The second root has the first as its sibling, and the first has the page as its child. |
| Box of crayons | Box `0x6d464`, 22 triangles; separate crayon `0x6d724`, 26 triangles | Box uses `sabon`, `kureyon01`, `kureyon03`; crayon uses `kureyon02`. An additional posed crayon exists at `0x6c63c`. |
| Pogo stick | `ChaoMain.prs`, object `0x6bb48`, 100 triangles | `hop01`–`hop03`, `silver`, `fue02`, `star01`. |

The source files are:

- `art/sa2_02514/original/ChaoMain.prs`: SHA-256 `d296ebbb14a8386e86eba3035b9fb35c6e1add0e563ef233ef67c8830be17dc8`.
- `art/sa2_02514/original/al_toy.prs`: SHA-256 `8d34e34e4d69e011cf02c2e297b4d4f83053bcb8a367bea17a3ffd9f89b076b7`.
- The original texture table includes the book, car, Sonic/Shadow doll, broom, crayon, paper and pogo texture names. Their presence is not merely inferred from the later CWE archive.

The supplied `art/cwe02516/original/al_toy.pak` and extracted `art/cwe02516/textures/toy/` also retain the corresponding textures. Its provenance records CWE 9.6.2.2. The previously refined guitar and accordion use named CWE objects from the supplied CWE DLL; this distinction remains in their earlier provenance.

For drawing, `kami01` is an existing colored-stroke texture and `kami02` a small color strip. No separate paper model using those textures was identified by the inspected toy-region scan. A new blank paper quad or dynamically drawn marks should be described as presentation geometry added for Minecraft, even when it accompanies the genuine source crayon and box.

## Available behavioral reference

The readable [CWE source at commit `0aea72e06c7eb213769b7ee5fd53ef5db57b7e37`](https://github.com/Exant64/CWE/tree/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37) provides useful labels and approach sequencing. It is secondary reference code, not official SEGA source.

[`CWE/imgui_debug_motionstrings.h`](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/imgui_debug_motionstrings.h) lists:

- `ALM_RAKUGAKI_JYUNBI`, `ALM_RAKUGAKI_JYUNBI2`, `ALM_RAKUGAKI`: drawing preparation and drawing.
- `ALM_DOLL_YOSHIYOSHI`, `ALM_DOLL_TAKAITAKAI`: soothing/petting a doll and raising it.
- `ALM_HOPPING`, `ALM_HOPPING_LOOP`: pogo entry and repeated hopping.
- `ALM_HOUKI`: broom action.
- `ALM_MINICAR`, `ALM_MINICAR_MIAGE`: minicar action and looking upward. These labels alone do not establish whether the original action pushes or rides the car.
- `ALM_BOOK_START`, `ALM_BOOK_YOMU`, `ALM_BOOK_MEKURI`: book entry, reading and page turning.
- `ALM_HORSE_JUMP`, `ALM_HORSE_AIR`, `ALM_HORSE_START`, `ALM_HORSE_YURAYURA`, `ALM_HORSE_FINISH`: horse mounting, airborne transition, starting, rocking and finishing.

[`CWE/al_behavior/albhv_ltoy.cpp`](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_behavior/albhv_ltoy.cpp) contains `ALBHV_GoToHorse`: check exclusive attention, stand, notice the object, approach its aim position, turn to it and enter `RideHorse`. The actual base-game ride routine is referenced by address, so this code does not supply exact rocking amplitudes, seat coordinates or motion timings.

[`CWE/Chao.h`](https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/Chao.h) includes the small-toy identities CAR, BOOK, DOLL, HOUKI, HOPPING and CRAYON. This supports treating them as distinct activities rather than instrument variations.

The corresponding Minecraft poses, 160-tick session timing, sound synthesis, entry/exit fitting and collision/navigation rules are authored adaptations. They should not be described as direct imports of original game animation curves or recordings.

## Footage availability and limits

The retained user-video research is documented in [the source manifest](references-0.25/video-source-manifest.json) and [the reviewed timeline](references-0.25/VIDEO_RESEARCH.md). The two supplied recordings depict garden/lobby/care and school services. Their documented motion strips cover inspection, eating, water distress, petting, carrying and sleeping; they do not document the requested car, dolls, broom, book, pogo, crayon or horse interactions. Native frames and source hashes are retained, but the original MP4s were not found in the inspected current workspace.

The recordings visibly identify Chao World Extended, so expanded menus or unused-toy activation cannot automatically be attributed to unmodified SA2. No new original-game action footage was watched in this reference pass. Claims of exact original-game animation matching, sounds listened to, multiplayer verification or in-client acceptance must therefore come from the separate actual verification work and its stated limits.
