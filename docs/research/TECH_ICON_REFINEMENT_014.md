# Technology icon refinements - 0.14

These are original pixel-art reconstructions of the requested objects. Official item textures were inspected as visual references, but no source-mod pixels are copied into the deliverable art.

| Theme | Revised motif | Primary reference |
| --- | --- | --- |
| Create | The accepted 16px wooden cogwheel is retained. The native 8px design has separate top, bottom, side and diagonal teeth, a two-pixel dark axle socket, and a contrasting wooden hub. | [Create source repository](https://github.com/Creators-of-Create/Create) |
| Refined Storage | Advanced Processor: a raised cyan/teal crystal chip on a diagonal gray circuit substrate. This replaces the pale disk and blue rectangular center. Both native sizes preserve the chip's cyan upper section and gray lower substrate. | [Official Advanced Processor documentation](https://refinedmods.com/refined-storage/utility/advanced-processor.html) and [official item texture](https://github.com/refinedmods/refinedstorage2/blob/develop/refinedstorage-common/src/main/resources/assets/refinedstorage/textures/item/advanced_processor.png) |
| ProjectE | Philosopher's Stone: a round burgundy/magenta orb, pale upper-left highlight and broken red/orange/yellow sparks around its edge. The previous square gold bezel is removed. | [Official 1.21.1 item texture](https://github.com/sinkillerj/ProjectE/blob/mc1.21.1/src/main/resources/assets/projecte/textures/item/philosophers_stone.png) |

The 8px and 16px plans are independent artwork, rather than a reduction that discards narrow features. The tiny art is composited onto the egg by the shared generator, while the 16px art is written to the existing Chao badge material. Neither path is tinted with the Chao coat palette, preserving the item's identifying colors.

QA reviewed all three symbols enlarged and at actual dimensions, on light and dark backgrounds, and composited onto the final egg shell. Outputs are deterministic, fresh RGBA images with only fully transparent or fully opaque pixels. The Create 16px artwork remains byte-identical to 0.13; the other five requested-size outputs are changed.
