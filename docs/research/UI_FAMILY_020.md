# Inspector and family fixes for 0.20.0

## Confirmed causes

- The blue box was part of every `adventure_status.png`, `adventure_genetics.png`, and `adventure_animals.png`, baked by `art/generate_ui_assets.py` into `(211,245)-(574,302)`. The Inspector uses these backgrounds across Stats, Genes, Parts, Mind and Care. It was not a widget left alive by a tab switch.
- The old Family cards deliberately printed the first twelve characters of a stable hexadecimal lineage identifier. The ledger recorded ancestor IDs and compact DNA but no display names. Those values were not recoverable names.
- The compact DNA string is a persistence format. Printing it directly made colour, jewel, grade and personality alleles unnecessarily hard to read.

## Changes

The UI asset generator now reserves the bottom note well for `adventure_guide.png`, where it is intentional. The three Inspector card interiors are plain and opaque, preserving the rounded shell, rings, antialiasing and transparent corners. Regenerated PNGs ship with the source fix. The existing pipeline invokes this generator after other UI writers; root owns that pipeline ordering.

Family cards display two parents across the first row and four grandparents in the next two rows. Each card shows the name and readable coat alleles. Hover reveals the stable lineage ID, all saved DNA fields, and an explanation when an old record has no saved name. Ability grade digits become named skills with grade letters; colour, tone, shine, jewel and favourite fruit indices become names. Personality alleles identify each axis. The underlying genotype and lineage IDs are unchanged.

The profile and command labels also distinguish rare immortal characters from genetic Chaos. Amy, Omochao, Tails and Knuckles use their character name and actual Child/Adult stage; their immortality no longer mislabels them as Chaos, Angel, Light or Devil. Normal-character immortals retain the genetic Chaos stage and alignment phenotype.

Both actual birth paths in `ChaoEntity` now save the names obtained from the two real parent entities immediately after breeding. On a subsequent birth, `recordParentage` copies those optional name snapshots into the correct grandparent positions. The vanilla spawn-egg self call still does not invent a family.

Name snapshots use these optional existing trait keys:

| Key | Meaning |
| --- | --- |
| `family.parent.0.name`, `family.parent.1.name` | Display names of the parents at this birth |
| `family.grandparent.0.name` through `.3.name` | Saved parent-name records inherited from the two parents |

The existing trait storage accepts bounded ASCII. A canonical base64url field stores at most 96 UTF-8 bytes in at most 128 ASCII characters, retaining Unicode and whole supplementary code points. Control characters are removed; invalid stored fields return no name. This adds no save schema version and does not alter existing ID, DNA, training, care or genetics semantics.

## Existing worlds

Old records without names stay loadable. The Inspector can resolve a missing name against a currently loaded Chao with the same lineage ID within 128 blocks; this is a client presentation fallback and does not mutate the saved ledger. If that ancestor is unloaded or absent, the UI says `Name not recorded`. An entirely absent ancestor says `Unknown ancestor`. It does not present an encoded name or an identifier as a name.

Saved names are snapshots, so renaming an ancestor later does not silently rewrite descendants or require global entity scans. Already unknown grandparent names cannot be reconstructed from DNA alone.

## Validation

- Executed standalone Java21 compile and `ChaoFamilyPresentation020Test`: **54 checks passed**. Covers two generations with actual supplied names, Unicode, truncation, corrupt records, save/reload, old records, unchanged IDs/genotypes, readable allele presentation, and rare-character versus genetic Chaos identity across save/reload and all three alignment extremes.
- The same 54 checks are wired into `ChaoStateTest`, which `MechanicsTestSuite` runs.
- `FamilyNameGameTests.realNamedParentsAndGrandparentsSurviveEntityReload` exercises the real entity birth method, two generations of names, NBT reload and vanilla spawn-egg behavior.
- `FamilyNameGameTests.courtshipEggCarriesNamesThroughItemEntityReloadAndHatch` exercises actual courtship, physical egg creation, item snapshot, egg NBT reload and a real hatch. Root reported both family GameTests passing in the initial 101-test full Minecraft run and again with actual Accessories and identity fixtures. The later narrow label change adds 15 pure checks and requires root's final compile rerun.
- Executed `art/validate_inspector_ui_020.py`: checks the final PNG regions of all three Inspector themes, the retained Guide well, and all four corners of each background.
- Root owns final in-client captures of Stats/Mind/Genes/Parts/Care and named Family cards, including hover and legacy missing-name behavior. Source/pixel checks alone do not establish in-client layout quality.

For a two-generation capture fixture, breed the first parents, call `child.recordParentNames("Maple","Pine")`, mark the resulting child adult, breed it with the second parent, and call `grandchild.recordParentNames("Willow","Fern")`. The Family screen is `showMind(3)`.
