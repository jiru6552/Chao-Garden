# Held Chao and Accessories companions

Sneak-right-click a Chao you keep with an empty hand to pick it up. The selected
hand becomes the held Chao item; no empty carrier is required first. This
works even when the other inventory slots are occupied. The existing carrier
item ID remains compatible with old saves. Empty legacy carrier items disappear;
the item now represents the Chao itself.

The filled item renders its saved Chao as an animated model in either hand,
including its inherited coat, theme, animal parts, blinking and idle movement.
Use it on a block to release the Chao into clear space. Its original entity UUID,
keeper, name, genes, care and other entity data are preserved. A copied carrier
cannot release a second living entity with the same UUID. Other keepers cannot
capture or release it, and Chao currently in activities cannot be picked up.

## Optional companion slot

Install **Accessories for NeoForge 1.21.1** and its required **oωo library** on
the client and server. A **Chao Companion** slot appears in Accessories. A living baby or adult outside a cocoon can be equipped. Each
player can use one companion at a time.

An equipped Chao follows its keeper as a real world entity. The inventory item
owns the saved creature; the summoned body is temporary and does not also save
itself to the world. Unequipping, logging out or changing dimensions recalls
the body and preserves its current state in the same item. Storage pauses care and lifespan. A summoned Chao spends one fullness every 300 ticks and one energy every 600. Every 6,000 loaded equipped ticks, a companion with at least 10 fullness and more than five energy gains one bond. Below 10 fullness it loses two happiness instead. Petting and feeding still help, and harm from the keeper can lower happiness and bond.

Adult visual maturity grows at its normal rate. Equipped time does not advance lifespan, birthdays, reincarnation or Omochao breakdown. Partial care intervals survive unequipping and saving.

Companion benefits scale with **trained skill levels0–99**. Genetic ranks
E/D/C/B/A/S affect training and are separate from those levels. Adult Run and Swim assistance also requires their existing earned-point unlocks (667 Run, 100 Swim). Visual evolution type does not choose abilities.
These numerical benefits are the Minecraft adaptation:

| Skill | Benefit |
| --- | --- |
| Run | A small land speed bonus at low levels, growing smoothly to +100% at level99. Sprinting keeps its normal multiplier. |
| Swim | A water movement bonus growing to +200% at level99. Level70 grants night vision in water; level99 also grants water breathing in water. |
| Fly 0–70 | Vanilla elytra flight and steering, with a two-to-ten-second limit per airborne period. Double jump to begin. |
| Fly 71–98 | The same elytra movement; the limit grows to twelve seconds. |
| Fly 99 | Full vanilla elytra flight and steering, activated by the usual double jump. Chest armor stays equipped. |
| Power | Defends the keeper against recent combat targets. Damage grows from an ordinary punch1 to11 at level99, equal to a fully charged Netherite sword with SharpnessV before armor/other damage effects. Run also improves attack frequency. |

Every eligible adult provides normal elytra lift, steering and collisions without replacing chest armor. Below Fly 99, the flight permission expires after its skill-scaled time; the player then falls normally. The timer starts on actual flight activation. Sneaking, stopping flight, slot cycling or re-equipping midair cannot renew it: the original elapsed window continues until landing or entering water. Fly 99 and all-S Omochao have unlimited companion flight while their energy lasts. Losing the live companion also ends its flight permission. An ordinary chest elytra continues to work independently. An airborne keeper using creative or mod-granted player flight carries the equipped Chao on their back. This attachment costs no glide time. Equipped Chao take no fall damage.

The abilities pause when energy is5 or lower. Gliding/elytra flight consumes
one energy every40 ticks and trains Fly. Successful attacks consume one energy
and train Power. A tired companion below 20 energy rests beside a stationary, grounded keeper. Crouching together lets it rest to full energy. The companion avoids attacking other players,
other Chao or its keeper's tamed animals.

Water bonuses stop when leaving water, unequipping or disabling the companion.
Only Chao-owned attribute modifiers and effect instances are removed; ordinary
potion effects, including effects applied while swimming, are preserved.

## Dimension traits and back attachment

A living, currently equipped Nether Chao prevents fire, burning, lava and hot-floor damage and extinguishes both its keeper and active temporary body. Ordinary damage remains effective. A living equipped End Chao prevents and clears levitation. Endermen ignore eye contact, while normal attack retaliation remains possible.

Press **V** (rebindable in Controls) with an End companion to open your actual vanilla Ender Chest inventory. Server checks validate the real companion slot, body, owner and recovery state, limit repeated requests, and revoke access when the companion becomes unavailable. Stored items retain ordinary Ender Chest ownership, transfers and persistence. No separate inventory is created.

These innate traits remain active at low energy. Holding an item or having a nearby Chao does not grant them. Skill-scaled movement and combat abilities still follow their existing energy rules.

During actual Chao-supported finite glide or elytra flight, its real body holds onto the keeper's back and follows the torso pose. The usual separate follower draw is suppressed. The attachment is synchronized for other players, clears on landing or unequip, and leaves normal movement physics when the creature is released into a garden.

## S-rank Omochao flight

An Omochao with **all five ranks at S** grants elytra flight even if its trained
Fly level is still0. While actually flying, press its **Flight Boost** key
(default **R**, rebindable in Controls) to accelerate along the look direction.
Each accepted boost costs two energy and has a **100-tick/five-second cooldown**.
The server checks the equipped robot, ownership, live state, energy and deadline;
the client sends only the keypress.

Both the player and the robot retain the cooldown. Switching between robots,
slot copies, unequipping, save/reload and release/recapture do not reset it. The
same server clock is used across dimensions, and respawn preserves the player's
deadline.

## Defeat and recovery

A defeated equipped companion is recalled instead of dropping its creature or
dying permanently. It remains disabled for **1,200 server ticks (60 seconds at
20 TPS)**, then returns while still equipped. Each defeat loses **10 happiness**
and **one level from a randomly chosen positive-level skill**; skill levels and
points never fall below zero. If every skill is already level zero, happiness
is still lost. Recovery itself applies no additional penalty.

Recovery time is stored in the item and cannot be skipped by unequipping and
placing it. It advances with the world's server time. The custom companion
slot retains its Chao item on player death. Logout/server-stop hooks take a
final snapshot before player saves. Old temporary companion bodies found on
disk are rejected rather than becoming another creature.

## API and source verification

This integration uses the publisher's actual Accessories API through a small
optional reflection boundary, so the base mod can load without Accessories.
The adapter calls `AccessoriesAPI.registerAccessory`, the `Accessory`
equipment/tick callbacks, and the actual slot container's
`markChanged(false)` after changing an equipped stack.

Primary references:

- [Wisp Forest API fundamentals](https://docs.wispforest.io/accessories/developer/api_fundamentals)
- [Slot data format](https://docs.wispforest.io/accessories/general/slot_types)
- [Accessories 1.21.x source](https://github.com/wisp-forest/accessories/tree/1.21.x)
- [Publisher releases](https://github.com/wisp-forest/accessories/releases)

The inspected runtime artifacts are Accessories
`1.1.0-beta.54+1.21.1` and oωo
`0.12.15.6-beta.1+1.21`, obtained from the publisher's Modrinth metadata. Their
JAR manifests declare the matching NeoForge/Minecraft line and the oωo
dependency. These third-party JARs are development/runtime dependencies and are
not embedded in Chao Garden. See the release validation notes for the tests that
were actually run with and without those mods.

## Carrying and damage recovery — 0.25.41

Babies grant no combat, speed, water, variant, flight or boost benefits. They sit on the keeper’s head on land and grip the back in water or flight. Adults without the earned Run unlock use the head perch; adults without Swim grip the back in water. Trained adults retain their existing following and benefits. Native elytra and player-ability flight both attach to the back. The existing renderer carries the real tracked body, including its coat and parts; head/back transitions follow an outward arc.

Baby care continues while equipped but childhood age and maturity stay paused. Adults keep the existing maturity/paused-lifespan split. Both use the existing 1,200-tick equipped recovery; baby defeat preserves stats, while existing adult defeat penalties remain.

Unequipped damage deaths publish one recovery snapshot before removing the body, preventing both vanilla equipment drops and the remaining hurt handler from returning the same item again. The destination order is the last marked active garden, the online owner’s inventory, compatible carried or Accessories storage, then a dropped Chao item by the owner. Existing garden capacity and ownership remain authoritative. Pickup retains a separate home mark; explicit unregistration clears it. Existing garden entries seed that mark on load. Associations already erased before this update cannot be inferred.

An unavailable/full/blocked garden falls back to item delivery without erasing its mark. An offline owner receives an available garden return immediately and a message on login; otherwise the saved snapshot waits for login. Cancelled final spawns retry. Completed receipts retain only identity and notification metadata. Obsolete body snapshots are rejected. Natural death, rebirth and Omochao outcomes keep their existing lifecycle and retire the damage-recovery receipt when complete.

Storage uses its public item-handler capability and verifies the inserted item remains observable. A handler error with an unobservable or ambiguous result holds the full snapshot for operator review rather than retrying a possibly committed write. No arbitrary third-party storage is assumed compatible. An ownerless Chao drops at the death position when it is inside world bounds; an out-of-bounds destination remains saved for review. Abrupt machine/power failure between Minecraft’s separate save files is not an atomic database transaction.

Operator preview: `/chao testcarry once`, `/chao testcarry loop`, `/chao testcarry stop`, with a Chao equipped on dry ground outside flight. The eight-second ground/flight/swim/ground cycle uses its real unlock state and production attachment/pose. It freezes Chao care, grants no rewards, and ends on interaction, damage, actual flight/water, unequip, unload or logout. No preview flags are saved.
