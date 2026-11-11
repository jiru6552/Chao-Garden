# Chaos Drives 0.25.43

Minecraft 1.21.1 / NeoForge 21.1.219. The four existing base IDs, effects, Chao serialization and existing save data are retained. Sixteen combined items are added; no generic `unstable_drive` existed or is introduced.

## Recipes and effects

All recipes produce one item. Four-item recipes are shapeless and fit the inventory grid. Nine-item recipes require a crafting table.

| Item | Ingredients | Skill effect |
|---|---|---|
| Solid Swim/Fly/Run/Power Drive | Four matching base drives | Corresponding skill +1 full level |
| Chaotic Drive | One of each of the four base drives | All four skills +24 progress |
| Super Chaotic Drive | Nine Chaotic Drives | A uniform shuffle assigns +24 progress, +1 level, +5 levels and −2 levels to different skills |
| Unstable Chaotic Drive | Nine Super Chaotic Drives | One randomly chosen drive skill receives a weighted random target level/progress, with a rare rank-change roll |
| Super Solid Swim/Fly/Run/Power Drive | Nine matching Solid Drives | Corresponding skill +10 levels |
| Unstable Solid Swim/Fly/Run/Power Drive | Nine matching Super Solid Drives | 5% level 99, otherwise level 0 |
| Perfect Drive | Four Unstable Solid Drives at corners, four Unstable Chaotic Drives at edges, Nether Star at center | Uniformly chooses one non-maxed Analyzer skill, including Stamina, and reaches level 99 |

Perfect accepts any mixture of the four Unstable Solid types, including four of the same type. Other typed recipes require matching types. Perfect never changes rank and is returned through the existing item-rejection path if all five skills are maxed, even if that becomes true while it is being considered.

Solid, Super Solid, Chaotic and Super Chaotic each give +1 happiness, +40 luck training progress and +20 intelligence training progress, once per consumed item. Unstable Solid makes three independent equal-probability sign rolls: happiness ±1, luck training ±40, intelligence training ±20. Perfect has no additional care rewards.

## Percentages and boundaries

The percentages retain the existing drive convention: 100 progress is one level, so +40% means +40 progress, not multiplication of stored luck points. Luck/intelligence have existing hidden levels 0–99, progress 0–99 and points 0–4000. Ordinary skills have levels 0–99, progress 0–99 and points 0–9999. Happiness remains −100 to +100.

Full-level changes preserve progress (26.43 → 27.43, or 26.43 → 24.43). At maximum level, existing rules require progress 0. Gains use the existing grade-dependent randomized level-up points: 13 + 3 × grade plus −2…+2. Losses subtract the existing mean per lost level, as used by companion defeat; level 0 clears earned points. Partial losses borrow across level boundaries. Force-to-zero effects clear both progress and points. Hidden-stat losses use the same boundary arithmetic with their own grade/point caps.

Base-drive directional evolution influence stays unchanged. Combined drives change trained stats and the existing last-trained tie breaker through ordinary training; they do not add a separate directional-axis bonus. Neither aging, lifespan, inherited rank genes nor animal parts are changed. A changed expressed rank affects subsequent training; previously earned points are not rerolled.

## Unstable Chaotic distribution

One of Swim, Fly, Run and Power is selected uniformly. Its target is level 0 with 0.5% probability or level 99 with 0.5%. Otherwise the target total progress is `1 + uniform(0…4949) + uniform(0…4949)`: a triangular distribution from 0.01 to 98.99 levels, peaked at 49.50. This weights middle target levels above extreme targets. If an interior target equals the current value, it moves one progress point instead. The resulting change can be as small as 1% of a level. This is a distribution over the resulting level, not a percentage multiplier of the starting points.

Happiness is independently uniform −10…+10 inclusive. Intelligence training is independently uniform −100…+100 progress inclusive. Luck is unchanged. A separate 2% roll attempts one rank step on the same selected skill, equally likely up/down, bounded E…X. At E/X, an outward step stays at the boundary; there is no wraparound. Inherited rank alleles are untouched. Existing reincarnation rules still reduce X to S; this update does not alter that rule. The item's additional tooltip is only the requested italic warning.

Super Chaotic uses Fisher–Yates to choose among all 24 assignments uniformly; every use assigns each of its four effects once, before ordinary bounds are applied. All four full-level/partial effects refer only to Swim/Fly/Run/Power.

## Drops and shop

Only the original four base drives remain in the Black Market catalog, at their existing prices and unlimited stock. Existing valid merchant saves and restock schedules are retained. Chest drive loot is unchanged.

Each qualifying hostile death rolls one base drive at 5%, or 100% when the credited player has a valid Chao in a functional Accessories slot. A Chao only in ordinary inventory does not qualify. Any equipped Chao counts, including a baby or one in the existing recovery cooldown; this new item-drop rule does not alter their movement/stat benefits or recovery timing.

Attribution first uses Minecraft's `LivingEntity.getKillCredit`: recent player credit, including player-owned projectiles and vanilla tamed-wolf attribution. If credit instead belongs to an owned Chao, resolve its online owner. The equipment of another nearby player does not count. An uncredited environmental kill uses 5%. Companion attacks already use the Chao entity as their damage source; this owner resolution is necessary for those attacks.

Looting does not multiply the roll. Existing hostile eligibility/tag rules, `doMobLoot` and cancelled ordinary loot events are honored. Legacy base-drive pools are normalized to this one roll; crafted drives in actual dropped gear are left intact. Dragons bypass normal item death loot, so their XP sequence uses its attributed player and a saved one-time decision. A miss or cancelled item spawn also commits that decision and cannot reroll on later bursts or reload.

No optional mod is a required new dependency. Without Accessories installed, the normal 5% rule still operates. Install the same Chao Garden version on server and clients for the new item registry.
