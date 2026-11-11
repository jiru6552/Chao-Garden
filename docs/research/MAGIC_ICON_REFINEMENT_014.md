# Requested magic icon refinements - 0.14.0

These six themes use matching motifs in two independently authored sizes: an 8px egg mark and a 16px Chao chest badge. The artwork is new code-authored pixel art. Official texture files were viewed as references in scratch and are not copied into the mod.

| Theme | Revised symbol | Primary reference / direction |
| --- | --- | --- |
| Astral Sorcery | Shifting Star: long white/cyan rays and a blue central jewel | [Official Shifting Star texture](https://github.com/HellFirePvP/AstralSorcery/blob/1.21.1/src/main/resources/assets/astralsorcery/textures/item/shifting_star.png) and [its base layer](https://github.com/HellFirePvP/AstralSorcery/blob/1.21.1/src/main/resources/assets/astralsorcery/textures/item/shifting_star_base.png) |
| Neo Vitae | Pointed dark-red blood drop with a small lighter highlight | The keeper requested a blood drop instead of the previous orb; [official Neo Vitae repository](https://github.com/breakinblocks/NeoVitae) supplies the current project identity |
| Ars Nouveau | Plain purple spellbook with a gold binding and cream pages; no central cross | The keeper requested simplification and centering of the established spellbook motif; [official Ars Nouveau repository](https://github.com/baileyholl/Ars-Nouveau) |
| Occultism | One simple circle with a five-point star; the tiny mark retains a continuous ring, one upper point, side points and two separated diagonal lower tips for a five-point silhouette | The keeper requested a simpler 16px mark while retaining the ring/star at 8px; [official Occultism repository](https://github.com/klikli-dev/occultism) |
| Iron's Spells 'n Spellbooks | Open parchment scroll, diagonal opposite curled ends, small dark ink strokes | [Official scroll texture](https://github.com/iron431/irons-spells-n-spellbooks/blob/1.21/src/main/resources/assets/irons_spellbooks/textures/item/scroll.png) and [official getting-started guide](https://github.com/iron431/Irons-Spellbooks-Docs/blob/main/index.md) |
| Draconic Evolution | Dragon Heart: glowing orange/yellow lobes and a long lower point, replacing the former core | [Official Dragon Heart texture](https://github.com/Draconic-Inc/Draconic-Evolution/blob/1.21/src/main/resources/assets/draconicevolution/textures/item/components/dragon_heart.png) |

The Shifting Star reference is a 16x128 animated strip; Dragon Heart is a 16x192 animated strip. The first native frame of each and the 16x16 Iron's scroll were inspected enlarged with nearest-neighbor sampling before authoring the new symbols.

## Validation and visual review

All six requested themes produce nonempty RGBA images with binary transparency at native 8px and 16px sizes. Their final item composites were inspected both at native size and enlarged, alongside the separate chest badges. Ars Nouveau's nontransparent 8px bounds are exactly `(1, 1, 7, 7)`, centered on both axes in the mark canvas. The ten unrequested magic-module designs retain byte-identical 8px/16px output; retirement of inactive themes is managed by the active catalog rather than this art module.

The actual egg and Chao texture generators route the same theme ID to these paired designs. Shared resource generation and in-game rendering checks are performed by the release owner after integration.
