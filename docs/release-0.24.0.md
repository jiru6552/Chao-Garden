# Chao Garden 0.24.0

Minecraft Java 1.21.1, NeoForge 21.1.219, Java 21. Install this version on the server and every client, replacing the previous Chao Garden JAR. Accessories plus its matching oωo dependency are optional and required for equipped companion abilities. No decoration mod is required.

## Chao World and Kindergarten

Craft a Chao Teleporter with four glass, four copper ingots and an emerald. Its two-block tube activates when walked into. It sends the visitor to the central hub and changes them to Adventure mode. The circular room uses a dark radial-grid floor, gold rings, stars and clouds, a central gold/cyan transporter and a blue-column, red-roof Kindergarten entrance, based on the provided SA2 images. The other garden entrances are decorative for now.

The Kindergarten is a separate shared dimension with furnished rooms. Right-click the Teacher, Doctor, Principal, Fortune-Teller or physical bulletin board; the resident Black Market uses the ordinary merchant interface. Select an owned carried Chao or a nearby owned Chao.

- Teacher: four shared classroom places, timed drop-off, protected students and finished-student collection. Ten-minute classes continue while the server runs, including when the keeper is elsewhere or logged out. Offers rotate every thirty minutes. The exact same creature and equipment are returned.
- Doctor: actual saved stat/grade charts, personality/health and awards; free illness treatment and existing paid grade care.
- Principal: eight care and mechanics topics tailored to this mod.
- Fortune-Teller: lucky-name suggestions, another suggestion, acceptance or a custom name.
- Bulletin board: live current class, next rotation and occupancy.
- Black Market: persistent stock and ordinary refresh rules.

The central hub transporter returns the visitor to the original dimension, location and game mode. Walk out of it after arrival before stepping back in. `/chao return` is a fallback exit from either room. Travel journals persist across relogging, death and dimension changes. Building, hostile spawns, explosions, player damage and destructive interactions are restricted inside these rooms; ordinary world gamerules remain unchanged.

See [Kindergarten services and adaptations](KINDERGARTEN_024.md) and [room construction](ENVIRONMENTS_024.md). Legacy Lesson Desk/Doctor Station registrations remain for save compatibility, but their new crafting and creative acquisition are retired.

## Training supplies

Every qualifying hostile death drops exactly one randomly selected Swim, Fly, Run or Power Chaos Drive. Looting does not multiply it. Canceled drops and `doMobLoot=false` still apply. The Ender Dragon's special death sequence has a one-time protected drop. Custom enemies can be added through `chao_garden:chaos_drive_enemies`.

DNA can be found in any chest loot table, including modded chest tables recognized by their real chest context. Each ordinary animal has weight 3; Dragon, Unicorn and Phoenix have weight 2 each. Thus each mythical sample is two-thirds as likely as each ordinary sample, rather than extremely rare. Existing DNA mob sources remain.

Every Black Market stock includes all four drives at **1 emerald each, unlimited stock**; three rotating ordinary DNA offers at **1 emerald each, four uses per offer**; and one rotating mythical DNA offer at **2 emeralds each, two uses**. Existing ordinary/shiny/gem egg distribution rules remain.

## Chao choices and flight

Clothes, hats, lenses and cosmetic wings are gifts. The Chao holds and considers an offered item. Its mood determines a bounded 15–90% acceptance chance while awake; an unwilling Chao throws it away. The choice is saved, so retries or reloading cannot reroll a pending gift. Inventory and old equipment components are preserved. Earned medal selection is unchanged.

An eggshell hat preserves the original egg's coat, including shiny and rare surfaces. It covers the whole head with two eye openings. The original shell remains recorded through carrying and reincarnation. See [gift choices and shell inheritance](WARDROBE_024.md) for the exact mood formula and transfer rules. The Garden CD now uses a black vinyl-disc sprite with a small cyan/gold label and a transparent spindle hole. Its track is unchanged.

All eligible adult equipped Chao provide **actual vanilla elytra movement**, activated with the normal airborne double jump. Below Fly 99, flight expires after a skill-scaled two-to-twelve-second window; it then stops and ordinary falling resumes. Re-equipping or repeatedly stopping/starting midair cannot renew that window. Landing or entering water restores it. Fly 99 and all-S Omochao permit unlimited flight while energy lasts; the robot retains its five-second boost cooldown. Chao remain attached to the keeper's back during supported flight. Ordinary chest elytra work independently.

## Scope

This release recreates the supplied rooms using Minecraft blocks and implements their listed services. Other hub gardens, original voice dialogue, downloadable bulletin posts and a complete per-bout historical karate chart are not included. Kindergarten behavior and source conflicts are documented as Minecraft adaptations. Existing Chao, world items, eggs, companions and gardens retain their saved state. Already-generated chest contents are not rerolled.

The current [gallery](release-gallery-0.24.0.html) contains actual client captures. [TESTING.md](../TESTING.md) records fresh checks and their limits. Earlier galleries and reports remain labeled as historical.
