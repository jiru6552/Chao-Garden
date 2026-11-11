# Items, DNA and gem chest eggs — Chao Garden 0.25.50

This focused update builds on the delivered 0.25.49 source. Java 21, Minecraft 1.21.1, NeoForge 21.1.219 and external Lionfish API 3.1 remain required. Use matching client and server Chao Garden versions. Earlier toy, movement, companion, garden and lobby changes are retained.

## Vanilla hats and older saves

Use **carved pumpkin** (`minecraft:carved_pumpkin`), **melon block** (`minecraft:melon`) and **skeleton skull** (`minecraft:skeleton_skull`) as Chao hats. Offer them through the existing keeper/gift interaction or Analyzer wardrobe. The Chao still considers and may refuse a gift. Shears and wardrobe removal return the actual vanilla stack, including its custom components. Ownership, busy/resting rules, item conservation and accepted-decision persistence remain in effect.

The inspected 0.25.49 registry contained only one separate item in this group: `chao_garden:watermelon_hat`. There were no registered custom pumpkin, melon or skull aliases to delete or invent. The watermelon item is removed from normal acquisition, creative inventory and market stock. Its old ID remains only as a hidden save-compatibility alias, converting to vanilla melon along supported load/use paths. Administrative commands can still resolve that alias so older saves can decode it safely.

Pumpkin and skull use Minecraft's own surfaces/models, fitted to the current animated skull bounds with a 0.3-model-pixel margin. Melon retains the existing rounded watermelon-cap appearance and fit; only its underlying equipment item changes. Closed pumpkin/skull hats conceal animal ears, horns, forehead/beaks and face/muzzles visually, without modifying saved DNA. The melon cap and unrelated hats keep their previous overlay behavior.

Conversion preserves count and explicit components, including names, custom data and opaque nested-container data. A legacy hat with a foreign damageable component also retains its old unstackable limit through an added max-stack-size override; otherwise vanilla melon's stackable default would make that combination invalid. Ordinary converted melons use the normal stack limit of 64. Conversion runs for Chao worn/held equipment on load or level join, player inventory ticks including offhand/armor, dropped item entities joining the level, direct legacy offers and worn-item returns. Carrier/companion snapshots use the normal entity-load path. An old accepted in-progress gift completes with a vanilla melon and retains its saved decision.

**Untouched storage is not scanned.** Unloaded/unopened chests, nested component inventories and private third-party inventories retain the hidden alias until extracted into a supported inventory/drop/use path. No recursive rewrite or bulk chunk conversion occurs. A failed inventory return plus cancelled world drop restores the original worn stack for retry. Old trader catalogs restock once, removing old hat offers; valid current stock retains purchases. See [full hat migration details](HATS_02550.md).

Uncarved pumpkins, jack o'lanterns, other skull/head types, melon slices and glistering melon slices are unchanged. Ordinary melon-slice food behavior and food components remain intact. None of the three vanilla hats is sold by the Black Market.

## Complete Chaos DNA requirement

Chaos eligibility now requires **all 31 registered species during the current life**. The mask derives from the complete stable species catalog, instead of the original 21-bit limit. Species order, item registration and save encoding are unchanged; Boar keeps the established `warthog_animal_gene` registry spelling and bit index.

The ten additional required species are Bee, Ladybug, Scorpion, Deer, Elephant, Kangaroo, Koala, Lion, Mole and Swallow. Previously received bits remain recorded. A saved child with the original 21 needs the remaining ten; no DNA is granted automatically. Already-evolved Chaos Chao remain immortal and are not requalified. An ordinary adult does not retroactively become Chaos by completing DNA after first evolution.

The other requirements are preserved: third life or later, happiness strictly above 80, and prospective Normal first evolution, with alignment still choosing Light/Angel/Devil. Reincarnation continues its existing current-life checklist reset; this update does not carry checklist completion into a new life. The separate Tails/Amy/Knuckles character-form gate explicitly retains its original 21-species requirement.

Analyzer has two checklist pages, dynamic totals and a missing-only filter that resets/clamps its page as the live checklist changes. Gene tooltips, journal, README and current mechanics/loot guidance match the new requirement. Opening an older journal refreshes its standard content through the existing journal mechanism while retaining unrelated stack components. Historical release/research documents remain versioned history.

## Half-fish stock rotation and economy

Each stock has three ordinary DNA selections from a 27-entry pool, and **one special selection from Dragon, Unicorn, Phoenix and Half-fish**. Each special has a one-in-four selection chance for that slot. Half-fish is absent from the ordinary pool, so it cannot appear twice through the two categories. Unicorn remains available.

| Sample | Emeralds per sample | Samples per purchase | Uses per offer | Conditional chest DNA weight |
| --- | ---: | ---: | ---: | ---: |
| Half-fish | 1, unchanged | 1 | 4, unchanged | 3/90, unchanged |
| Dragon, Unicorn, Phoenix | 2, unchanged | 1 | 2, unchanged | 2/90 each, unchanged |

Shop selection is separated from the original chest/economy mythical classification. `TrainingSupplyRules` and generic chest-training policy remain unchanged, including the 12% DNA category chance. Half-fish therefore remains approximately 0.4% per qualifying generic training chest roll, independent of its new shop category. Other DNA loot, enemy sources, training effects and quantities are unchanged.

Stock version 37 rebuilds version-36-and-older saved catalogs once, including resident traders. That ordinary catalog migration also resets their finite offer counters; it does not remove purchased player items. Valid current stock preserves used counts and deadlines. Invalid current stock containing a retired hat, duplicate or two special DNA entries is rejected too. Normal timed restocking still waits for an open trade to close. The existing actual offer count remains 42, or 43 with its rare extra gem offer; outdated documentation was corrected.

## Gem eggs and Lootr

The shipped global loot modifier and codec default now use **0.03: 3%**, up from 0.0002: 0.02%. There remains one gem opportunity per qualifying loot generation, adding one egg on success. Base, shiny and dimension egg categories retain their independent existing behavior. No second modifier, event handler or Lootr-specific compensation roll was added.

Qualifiers remain the existing Minecraft ancient-city, desert-pyramid, stronghold-prefix, bastion-prefix and end-city-treasure tables, plus the existing configured Moon exception. Gem choice, Moon exclusivity and dimension policy are unchanged. **Black Market gem odds remain 1 in 1,000 restocks.** An explicit data-pack override can still replace the shipped rate.

Existing generated vanilla chests retain their contents. Unopened loot-table chests use the new rate when they generate. Lootr generates separate saved inventories for independent player keys, or a shared key when its supported team integration supplies one. The 3% opportunity applies once when each such inventory first generates. Reopening and reloading its cached inventory do not roll again. Lootr's own refresh/reset intentionally permits another generation; this mod does not change that policy.

The actual tested optional Lootr release is NeoForge **1.11.38.127**. It is not bundled or required. See [binary/source audit and compatibility details](GEM_EGGS_02550.md), including the distinction between vanilla generation with Lootr conversion temporarily disabled in the fixture and the separate enabled real-Lootr inventory test.

## Actual verification

- Core mechanics passed, including 651 focused DNA assertions, literal 0.25.49-JAR-generated save roundtrips, every missing species across alignments/reloads, other evolution boundaries, existing Chaos and reincarnation. The existing stock runner passed 922,385 assertions, including its repeated rotations and unchanged gem denominator.
- **119 required native dedicated-server GameTests passed:** 19 new hat checks, 39 DNA checks, nine stock checks, five gem/Lootr checks and 47 existing item, wardrobe, DNA, market, training and egg regressions. Real Accessories/owo and Lootr were loaded. New trader fixtures include 128 visitor/resident creations, 64 timed restocks, saved-stock migration and all four special purchases/exhaustion/reload.
- Exact gem probability boundaries and draw counts passed. There were **35 gem eggs across 1,024 seeded ancient-chest generations**, with at most one per generation and byte-identical contents on reopening. The enabled real-Lootr test created two independent player inventories, observed two initial fills, then 40 cached opens and a saved-data reload with zero additional fills.
- The actual Analyzer client workflow passed seven rendered captures: both pages, missing filtering, live page clamping, all-31 completion and the final missing species. Native button clicks and displayed species lists were checked. The page layout and completion screens were visually inspected.
- **Forty final actual-client hat cases passed with 120 captures**, covering babies/adults, alignment/Chaos/character forms and animal heads, with front, diagonal and rear views and a save/load before rendering. Actual rendered overlay poses verify that closed hats conceal ears/horns/forehead/face additions, unrelated limbs stay visible and melon retains its previous presentation. The complete saved state remains unchanged on every checked rendered frame. Pumpkin/Skull/Melon and repaired Dragon/Sheep/parrot/boar frames were visually inspected. This was a static fit study, not a claim of full movement or remote-player acceptance.
- The full `build --continue` invocation assembled the production JAR and ran every check. Core, all new and other existing rig/timing checks, and all 264 visible-transition checks passed. **The full build is not green:** only the unchanged triangle and trumpet transition tests failed, reproducing byte-identical 0.25.49 baseline reports. Neither test was skipped or relaxed. Triangle: 267,696 samples / 25 failures / minimum −0.54432297 pixels, report SHA-256 `99bce5827d20d9d3aba51fef07728eb609dda4bc14e2c0534e903aacba50b6f8`. Trumpet: 267,696 samples / 37 failures / minimum −0.58569264 pixels, SHA-256 `b64266310e866db7a3628192f974bcd9abf1eabfd4b36b797de53dfc37fa0bfa`. Current result files and a comparison manifest are included with the delivery; historical baseline evidence remains in the 0.25.49 delivery. These are inherited instrument-transition limitations, not passes.

The first native launch needed a template-namespace fixture correction. A later chest fixture correctly exposed Lootr interception before vanilla generation, so the vanilla-only fixture now scopes and restores Lootr's disable flag; the separate enabled Lootr cache test remains intact. The initial animal-head client fixture omitted its derived part mask; that fixture was corrected before the final save-invariance/render study. A real visual pass found closed-hat overlay protrusions and led to the targeted visual mask described above. These corrections did not relax the production eligibility, probability or conservation assertions.

## Remaining in-game verification

Check the three hats on your preferred Chao and resource pack, including head tracking, crawling/swimming/flying transitions and a second player's view. Try gift acceptance/refusal, swapping and removal, then save/reload. In a copy of your old save, extract an old melon hat from storage and check its name/data and return behavior. Untouched nested/private storage may keep the compatibility alias until exposed to a supported path.

Check your existing trader's one-time migration and Half-fish availability over restocks. The two-player Lootr test uses real server APIs and simulated server players; separate physical multiplayer clients, every Lootr version, team mods, server refresh settings, third-party loot modifiers and full modpacks were not tested. A real user world was not opened or bulk converted. No downgrade guarantee is made.

Client captures use Mesa software rendering with shaders disabled. No new sounds were added; existing sound assets and clocks remain unchanged. There is no audio hardware here, and listening was not performed.

The source ZIP is a cumulative overlay on the full 0.25.43 source, including previous updates and explicit source deletions. Follow its deletion manifest/helper, or use the complete delivery branch. The deletion helper edits source files only and never scans Minecraft saves.
