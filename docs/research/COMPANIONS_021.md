# Companion skill progression - 0.21

Companion strength uses trained levels0–99, independently of genetic grade. Any living, energetic adult in the actual Accessories slot can defend with a basic punch and provide a short finite glide. Its first-evolution body type does not restrict which skills help the player.

| Skill | Curve and cap |
| --- | --- |
| Run | Additional land movement speed `(level/99)^1.2`; level1 is less than1%, level99 is +100%. |
| Swim | Additional NeoForge swim speed `2*(level/99)^1.15`, in water only; level99 is +200%. |
| Fly0–70 | One airborne budget of40–200 ticks: two to ten seconds. |
| Fly71–98 | Budget grows to240 ticks/twelve seconds. |
| Fly99 | Vanilla elytra flight, activated by the normal double jump. |
| Swim70+ | Night vision while in water. |
| Swim99 | Water breathing while in water. |
| Power | Damage rises continuously from1 at level0 to11 at level99. |
| All-S Omochao | Vanilla elytra regardless of fresh Fly level, plus a rebindable boost key, defaultR; five-second/100-tick server cooldown. |

The ordinary adult's two-axis evolution appearance is separate from all these numeric benefits. Flying and combat retain the existing energetic-companion rule: energy must exceed5. Flight consumes one energy every40 ticks; an accepted robot boost costs two energy. Existing defeat penalties and the one-minute item-owned recovery are retained.

## Native flight and networking

NeoForge1.21.1's actual `Player.tryToStartFallFlying` and `LivingEntity.updateFallFlying` check the chest stack directly. Two narrowly conditional Mixins add a validated companion permit to those decisions. The actual vanilla elytra travel equations, steering, pose, collision handling and normal chest-elytra behavior continue to run. Chest armor is never moved, replaced or hidden in a temporary item. Without a valid live companion, the original chest checks run unchanged.

Only a server-issued short-lived status permits client prediction. The server resolves the actual active owner/body/slot token, adult state, energy and levels/rank itself. The boost request contains a keypress boolean; it contains no rank, velocity, timestamp or companion identity. Its deadline is stored on the player, carrier and saved entity; the maximum applies. Recall, item serialization, release/recapture and alternating twoS robots cannot reset it. Player cloning preserves the deadline through respawn. The overworld game clock supplies the shared deadline across dimensions.

Finite glide uses a player-owned airborne budget. Its elapsed server-clock window starts when either velocity or actual received position shows falling, then continues even when the reported velocity is zero/clamped, sneaking pauses support, or the slot is unequipped. Unequipping, slot copies and re-equipping do not reset it in midair. Client prediction cannot renew its own budget from repeated stale status packets. A real landing or water entry restores the next airborne budget. Reaching the duration limit resumes ordinary falling. Sneaking opts out of finite glide.

## Effect and attribute ownership

Land and water modifiers use separate Chao Garden IDs and are transient; cleanup removes only those IDs. Existing potion effects are left untouched. When another source applies night vision or water breathing during Chao support, `MobEffectEvent.Applicable` hands ownership over before vanilla merges the new effect, even if its duration is shorter. Exit, unequip, disable and recall remove only the exact companion-owned effect instances. Persistent ownership markers provide login cleanup after an interrupted session; external-effect handover clears those markers. Night vision has a long enough refreshed duration to avoid vanilla's short-duration flashing.

## Reference and verification scope

The damage endpoint was checked against the local official Minecraft1.21.1/NeoForge21.1.219 sources: player base damage1, Netherite tier bonus4 and sword bonus3 total8; SharpnessV adds `1 + (5-1)*0.5 =3`, giving11 before armor or other damage events.

`CompanionRulesTest` supplies1,975 deterministic assertions including every level, monotonicity, bounds, grade/level separation, robot eligibility and defeat conservation. Eight real server GameTests cover native elytra fallback, actual-slot flight and armor preservation, water effects and external potions, actual inbound boost payload replay/cooldown and item serialization, two-robot cooldown cycling, finite-glide exhaustion/landing, real inbound falling-position packets with zero server velocity, and actual companion attacks at both damage endpoints. Tests that require Accessories explicitly skip when that optional mod is absent. The final clean integrated build passed all 229,779 deterministic checks. Both required-test server profiles passed 117/117: default in 6.116 seconds and real Accessories/oωo plus identity fixtures in 7.272 seconds. The latter executes all eight companion cases through the actual API. Source-log hashes and completion evidence are retained in `docs/validation-0.21/server-default.json` and `server-accessories.json`; the final actual client completed all 340 captures, including six live companion scenes and successful/replay-rejected robot boosts, as recorded in `TESTING.md` and `docs/validation-0.21/runtime-client.json`.
