# Blood drop and scroll refinement - 0.14.1

This patch refines two existing theme symbols at their native 8px and 16px sizes. The small symbols appear on the egg items; the larger symbols appear on the corresponding Chao chest badges.

| Theme | Refinement |
| --- | --- |
| Neo Vitae | Sharper upper tip and a rounded lower bowl, retaining the red blood-drop palette. |
| Iron's Spells | A cleaner unfurled scroll with straight parchment, balanced curled ends and simple ink. |

The collection remains nineteen themes: seventeen optional mod themes plus Nether and End. Theme gates, save IDs and the retired-theme compatibility behavior introduced in 0.14.0 are retained.

The updated images are authored through the editable native pixel-art source in `art/theme_icons_magic.py`. The generated item sprites and Chao texture atlases use those same definitions. Reference research from 0.14.0 remains in `docs/research/MAGIC_ICON_REFINEMENT_014.md`.

The current PDF focuses on the two changes. `docs/icon-touchup-gallery.html` includes six current actual Minecraft frames plus the complete authored theme reference. Captures use development identity fixtures to enable the optional appearances; they do not establish full compatibility with each third-party mod. `TESTING.md` records the completed validation.
