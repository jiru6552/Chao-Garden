# Black Market and toys — 0.10

The requested merchant is a Minecraft adaptation of the Chao Black Market.
The item stock and visual references were read from the
[Chao Island Info Center Black Market guide](https://chao-island.com/info-center/kindergarten/black-market.html)
and its [Black Market wiki entry](https://chao-island.com/wiki/Black_Market).
The publisher's Black Market screenshot linked from the Info Center was
inspected: cyan Chao, dark glasses, pale mouth cover, yellow hands and floating
yellow marker. The new model keeps those recognizable cues and adds an olive
traveling hood, robe and satchel for its wandering Minecraft role. All geometry
and textures are newly authored; no reference image is included as a mod asset.

The guide documents changing selections of rare eggs and seeds. The game's
Black Market uses Rings, while this adaptation explicitly uses emeralds as
requested. Exact emerald prices, natural-spawn timing, visit length and toy
effects below are Minecraft balance choices, not claimed Sonic values.

## Merchant behavior

- A real wandering merchant opens the standard merchant interface. Every offer
  uses emeralds, with normal vanilla payment and finite product stock.
- Each stock has four fruit seeds, all four drives, six animal genes, one shiny
  egg, one jewel egg and three reusable toys. Seeds replace direct fruit sales.
  Different visits/restocks can expose every seed and all 21 animal genes.
- Seeds cost 4–8 emeralds; drives 3; ordinary genes 8 and exotic genes 16;
  shiny eggs 32, jewel eggs 48; ball 8, rattle 6 and maracas 10. Eggs have one
  purchase per stock. Their normal appearance/genetics initialization is retained.
- Stock refreshes after a random three to seven minutes, matching the stock
  interval documented in the [SA2B Black Market article](https://chao-island.com/wiki/Black_Market_%28SA2B%29).
  A merchant waits until the player closes trading before changing stock.
  Offers, sold quantities and the refresh deadline survive save/load.
- Natural visits are attempted at most once per 20 minutes of world play, with
  a 60% visit roll, near a living overworld player. Both trader-spawning and
  mob-spawning game rules are respected. The world saves its attempt/occupied
  budget, so restarting cannot reroll it. Visits last up to 20 minutes and expire
  by absolute world time, including when the trader was unloaded.

## Toys

The [Classroom guide](https://chao-island.com/info-center/kindergarten/classroom.html)
describes instruments and Chao performing together; it includes maracas. The
ball, rattle and playable maracas here are reusable Minecraft care tools.

Right-click a Chao you keep. A play session lasts five seconds and has a
30-second per-Chao cooldown. It adds four happiness and two bond, costing one
energy; a child gets seven happiness from the rattle. The Chao chases/bounces
after the ball or bounces and plays a musical rhythm with maracas/rattle.
Resting, cocooned and activity-controlled Chao cannot be interrupted. Another
keeper cannot use a toy to claim a Chao.

The ball visible during play is a temporary bouncing item-display entity,
not an obtainable dropped-item stack. Players, hoppers and item pipes cannot
collect it. It expires after the session. The inventory
toy is not consumed. No toy grants stat levels or animal appearance parts.

## Validation

`MarketStockTest` exercises deterministic stock, category coverage, distinct
products, price bounds, limited rare egg stock and all-source reachability.
`MarketGameTests` verifies registered merchant products and emerald payment,
saved sold quantities, toy ownership/cooldown/reuse, and persisted visit budgets.
