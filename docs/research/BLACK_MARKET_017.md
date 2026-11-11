# Black Market and animation study - 0.17.0

This document distinguishes the reference catalog from the Minecraft implementation. The linked pages were inspected for this release; the project does not claim to reproduce every original shop condition, game currency, toy behaviour or rendering effect.

## Sources

- [Chao Island: Black Market](https://chao-island.com/info-center/kindergarten/black-market.html) supplies modern fruit, seed, coloured egg, shiny egg and hat prices. The Dreamcast section lists Black, Silver, Gold, Ruby, Sapphire, Amethyst, Emerald and Moon.
- [Chao Island: Sega Superstars](https://chao-island.com/guides/spin-off-games/sega-superstars/#black-market) supplies the requested toy hierarchy: Trumpet 5,000 rings; Drum 15,000; Jack-in-a-box 30,000; Radio 60,000; TV 100,000. Star Fruit, tree and cake are excluded as requested.
- [Chao Island: Colours](https://chao-island.com/info-center/advanced/colours.html) describes jewel coats as metallic and reflective. Moon is purple with a dimpled, golf-ball-like surface, rather than a white or silver jewel.
- The supplied `600px-Sa2-blackmarket-welcome-542734233.png` is the merchant appearance reference: dark sunglasses and a lower-face mask on a Chao body. The reference image is not bundled as a project texture.
- [Recorded Chao crawling gameplay excerpt](https://tenor.com/view/oomfie-scrungo-chao-omw-gif-22210069) was inspected frame by frame (39 frames, 80 ms each in that GIF). It shows a low forward face, alternating support, bent rear legs and a small body/head nod. Its original game's precise version and playback-rate provenance are unverified, so the implementation borrows the action rather than claiming an exact copied timeline. Full YouTube videos were located but could not be played through the available access route; no full-video inspection is claimed.

## Pricing adaptation

Modern ring costs use `ceil(rings / 100)` emeralds. Most expensive offers use emerald blocks and a remainder while preserving the exact emerald value. When more than 64 blocks are needed, the total is rounded upward to a multiple of nine so it fits the merchant's two payment slots. Radio therefore costs **67 blocks (603 emeralds)** and TV **112 blocks (1,008 emeralds)**. These are ordinary vanilla emerald blocks, not a separate currency.

The regular egg is the explicit exception to the source's N/A-cost exclusion and costs **2 emeralds** here. Dreamcast rare Chao were gated by Emblems, so their new ring-equivalent entries are authored balance values: Silver 50, Gold 75, the four gems 100 and Moon 150 emeralds. The existing ordinary Black egg supplies Black at 20 emeralds; it uses the existing mono-tone coat rather than adding a separate Dreamcast two-tone Black identity. No claim is made that these rare-egg prices were original ring costs. Moon is appended as jewel index 13, preserving all earlier coat indices.

The resulting catalog has **67 products**: one regular egg, 26 coloured/shiny eggs, seven jewel/Moon eggs, nine fruit products, seven seed products, twelve hats and five toys. Source N/A products, genes, drives, Star Fruit, tree and cake are absent from new merchant stock. Original-game menu voice themes are not translated into Minecraft menu packs. See [LOOT.md](../../LOOT.md) for the complete priced catalog.

## Rotation and saved traders

Each stock has 15 offers: regular egg; two ordinary coloured eggs; one shiny egg; one jewel/Moon egg; two shape fruits; one other fruit; Strong Seed; two other listed seeds; two hats; two toys. All eligible products can be selected across restocks, with no permanently omitted colour set or original-game Emblem gate. Stock refreshes after three to seven minutes when trading closes. Current offers and purchase counts persist; an old catalog version is discarded on load so removed drive/gene offers do not remain for sale.

Night potion goals are removed from the inherited wandering trader. Older saved invisibility effects are cleared. The finite visit and normal spawning restrictions remain.

## Toys, hats and seeds

The five new toys are reusable five-second care sessions with temporary uncollectable item-display props and a short rhythm. They are not full simulated musical instruments or game-program TVs. Saved Ball, Rattle and Maracas remain usable but are absent from new merchant stock.

The twelve priced hats are wearable Chao head equipment. A keeper right-clicks with a hat to equip or swap it, or with shears to remove it. Replaced headwear returns to inventory or drops when full. Ownership and care/activity restrictions apply. Vanilla equipment synchronization and entity data retain the hat through save/load and filled-item transport. The old Skeleton Dog `hatsUnlocked` flag does not gate this direct interaction.

Mushroom and Nutritious Mushroom remain shop foods. Their seeds have no new crafting, creative or planting route. Old seed and tree IDs remain registered so saved worlds do not lose blocks or inventory entries; existing placed mushroom trees remain compatible. Other non-mushroom fruit can still craft into seeds, and the seven listed seed products can be bought directly.

## Materials and movement

Shiny and jewel Chao/eggs receive a moving silver highlight pass using an included reflection shader, Minecraft's foil blending and its animated texture transform. It follows the player's glint settings and needs no separate shader mod. The shader samples the original surface's alpha mask as well as checking equal depth, preserving transparent eye/wing corners even for overlapping or coplanar faces. Old shiny-only END_ROD particles are removed so the moving material, rather than detached white puffs, supplies the shine; the separate Chaos aura remains. It is an animated material sheen, not ray-traced reflection. The End egg retains its actual vanilla animated portal pass.

Crawling uses a low head/body arrangement, alternating forelimb reach and plant, a lifted recovery, an opposite rear-knee push and a small torso rock/head nod. Distance moved advances the cycle, so it stops rather than continuing to paddle when stationary. The prior point milestones and occasional idle gestures remain. [CRAWL_017.md](CRAWL_017.md) records the detailed cycle and fitting limits. Actual-client capture timings and completed validation are recorded in [TESTING.md](../../TESTING.md).
