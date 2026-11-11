# Inventory sprite refinement in 0.12.0

The four Chaos Drives now stand upright, with both striped metal caps on the
same vertical axis and long crystal facets between them. Every egg item uses
a wider, curved crown: the 44 inherited shell variants, 48 themed eggs and the
spawn egg share the revised silhouette. World egg geometry is unchanged.

Animal-gene vials keep their glass outline, animal color and double helix.
The cap speckles, nested reflections, extra glass rims and one DNA link were
removed. All 21 colors use eleven colors including transparency, making the
small inventory image calmer while retaining a recognizable tube of DNA.
Canonical samples and the 21 legacy item aliases use identical artwork.

These are native, code-authored pixel assets: 16px eggs and drives, 32px vials.
`art/item_sprite_art.py` and `scripts/generate_gene_assets.py` remain the
editable sources. To refresh only these item PNGs and their preview sheets:

```sh
python3 scripts/preview_item_sprites.py --write-resources
```

This command leaves models, loot, recipes and language files alone.
`docs/screenshots/item-sprites-0.12.png` shows all four drives, representative
shell families and all 21 gene samples, with enlarged and 16px examples.
`docs/screenshots/item-sprites-comparison-0.12.png` compares 0.11.0 with 0.12.0.
