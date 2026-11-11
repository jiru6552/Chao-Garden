# Voluntary wardrobe and original eggshells

Giving a hat or physical accessory, using the Wardrobe inventory list, and picking a dropped outfit up all enter the same server-owned held-item sequence. The Chao holds the actual transferred item, considers it, and either wears it or throws that same item back with its components intact. The ordinary outfit UI no longer equips immediately. The wardrobe blocks new offers while a lesson, competition, cocoon, rest, or another held-item interaction is in progress.

## Mood choice

For an awake, eligible Chao, the Minecraft adaptation uses this acceptance percentage:

```
clamp(15, 90,
  50 + happiness * 3 / 10 + (energy - 50) / 10
     + joy / 20 - anger / 20 - cry / 25 - fear / 25)
```

Happiness is bounded to -100 through 100, energy to 0 through 100, and the existing emotion accessors bound their own values. Integer divisions follow Java arithmetic. Sleeping, dead, and cocooned Chao cannot accept. A sad Chao can occasionally accept; a happy Chao can occasionally decline. Hats, clothes, face and neck accessories, lenses, and cosmetic wings use the mood choice. Food, medicines, animal genes, and drives retain their existing needs and eligibility checks.

One roll is made after considering the item. The result and pending item survive saving, carrying, and reloading; an interrupted equipment return does not reroll the decision. The old item leaves its equipment slot before inventory or entity callbacks run, and capture, care, damage, recursive offers, and recursive item processing are guarded while the replacement commits. A denied item insertion restores the old equipment and keeps the accepted new item pending for retry. Foreign stacked equipment is returned as one physical stack, preserving its count without a partial-inventory rollback.

## Eggshell masks

An egg snapshots its complete native shell appearance before gentle or rough hatch treatment can change the newborn. The Chao stores a dedicated original-egg snapshot separately from its evolving body coat. The snapshot survives entity NBT, a carried Chao, a physical rebirth egg, that egg's inventory form, and the next hatch.

Each dropped shell hat independently stores the coat of the physical egg that produced it. A different Chao can wear that shell without recoloring it to its own body. The item tooltip identifies the shell's original coat. Colored, two-tone, shiny, jewel, Moon, Nether, and End coats use the existing continuous egg atlases. Shiny and jewel shells use the continuous reflection material, and End shells use the animated End portal material.

The worn model is a complete rounded shell around the skull with a broken, open neck rim. Two real openings expose the wearer's native eyes; it does not draw an extra set of eyes. The model follows the current skull joint and fits its native bounds, including ordinary development, retained mod themes, and special adult families. Mesh and decoded-coat caches are bounded to 64 entries each. The previous extra tiny-cap render is disabled.

## Verification

`WardrobeChoiceTest` checks every percentage roll across the happiness and energy ranges, emotional effects, clamping, and ineligible states (6,644 pure checks). `WardrobeGameTests` exercises real inventory offers, saved choices, mood changes after a choice, real rejected item entities, component preservation, denied/reentrant replacement returns, foreign stacked equipment, six native hatch coats, carrying, and physical reincarnation. Existing market, item-handling, and wardrobe component tests use deliberate saved decision fixtures when their subject is a specific accepted or rejected outcome.

The excluded `Wardrobe024VisualSmokeSupport` supplies actual client scenes for accepted clothing, rejected headwear, and normal, shiny, jewel, Nether, End, and Moon shells. Its client geometry probe checks runtime skull coverage, finite vertices, bounded UVs, and unobstructed native pupil rays across the current retained themes and development families. These fixture classes are excluded from the distributed mod JAR.

The 0.24 shared actual-client study completed the wardrobe geometry audit for 8,108 runtime cases. All seven wardrobe screenshots were inspected: accepted outfit, actual rejected hat throw, ordinary shell, shiny shell, ruby jewel shell, Nether shell, and End portal shell. They show the native eyes through the mask openings, whole-head coverage, preserved coat materials, and the continuous reflection surface. See `run/screenshots/chao024-wardrobe-*.png` and `run/screenshots/chao024-shell-*.png` in the development workspace, or the packaged validation gallery.
