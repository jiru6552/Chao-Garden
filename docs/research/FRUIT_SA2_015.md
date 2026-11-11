# SA2 fruit catalog and Minecraft adaptation — 0.15

Primary source: [Chao Island Fruit, Sonic Adventure 2 section](https://chao-island.com/wiki/Fruit). Visual references were inspected from the original SA2 renders on [Chao Island's Black Market page](https://chao-island.com/info-center/kindergarten/black-market.html). The uploaded Marketplace pack contains no matching Chao fruit models; the new sprites and stepped hanging solids are authored for this mod.

| Initial source in this mod | Fruits |
| --- | --- |
| Overworld trees | Chao Garden, Hero Garden, Dark Garden, Strong, Tasty |
| Rotating Black Market fruit offers | Hero, Dark, Round, Triangle, Square, Heart, Chao, Mushroom, Nutritious Mushroom |

This follows the requested adaptation: the three garden crops and two seed-only crops grow naturally. A market crop must first be obtained as fruit; none of its seeds appears in farmer or trader stock, world generation, or bonus loot. Crafting the obtained fruit yields its seed, permitting a renewable orchard afterward. Traders carry three randomly selected market fruit products per stock, alongside two natural seed products. All nine market products can appear over successive stocks. Farmers sell only the five natural seeds.

Smart Fruit and Mint Candy are unused in modern SA2. The earlier invented Stamina Fruit and those unused foods remain consumable only for saved-item compatibility. Existing Smart/Stamina orchard blocks remain registered, but their seed recipes and new acquisition paths are removed.

Whole-item feeding converts a four-bite fruit into one Minecraft interaction: Garden/shape/alignment/Heart give 40 Stamina progress, Strong 50, Tasty 30, and Chao 50 in each visible ability. Mushroom gives 50 Stamina and 50 Luck; its identical-looking nutritious variation gives 80 and 100. Alignment fruit also changes child alignment by 0.15. Heart Fruit retains the existing adult/breeding eligibility checks. One item is consumed and one fruit care event is recorded. Hero Garden Fruit uses the intended 10%-per-bite rate, rather than reproducing the PC port's stamina bug. These totals do not implement bite-sized held fruit, spoilage, original ring prices, finite tree lifetimes, or the port's texture bugs.

The source renders determine visual identity: yellow garden ovals, pale ribbed Hero Garden fruit, a red scaled Dark Garden fruit, an orange/red pointed Strong fruit, a yellow oblong Tasty fruit, yellow Hero, curved red Dark, cyan pear-shaped Chao, magenta Heart, green white-spotted mushrooms, blue Round, green Square, and orange Triangle. Native 16px sprites and hanging fruit geometry use those motifs consistently. All four pod ripening stages retain the same silhouette while their color and size mature.

The existing seven-block trees and five-wide real-leaf canopies are retained. New overworld chunks receive a sparse fruit-tree feature (one attempt per eight chunks); suitable soil and unobstructed space are required, so not every attempt succeeds. Naturally generated trees have ripe fruit. Existing worlds gain this generation in new chunks; old orchards continue to work.
