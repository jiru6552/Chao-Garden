# Adventure and dimension emblems - 0.13

The original emblems in `art/theme_icons_adventure.py` replace broadly shared
flames, flowers and abstract shapes with recognizable items, creatures and
portals. Each has a native 8px egg symbol and a 16px badge. The eight-pixel
designs are directly authored; they are never reduced from a detailed image.
The larger badges add individual-texel highlights and object-specific marks.
All pixels use binary alpha. No source mod texture is copied or redistributed.

These are compact visual tributes, not exact reproductions of official logos.
They do not introduce items or alter compatibility gates.

| Theme | Motif | Primary project/reference |
| --- | --- | --- |
| Cataclysm | Dark Ignis helmet with gold horns and orange molten face | [Official project](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm) |
| Ice and Fire | Mirrored red fire-dragon and blue ice-dragon heads | [Official repository](https://github.com/AlexModGuy/Ice_and_Fire) |
| The Aether | Glowstone frame and pale-blue cloud portal | [Official repository](https://github.com/The-Aether-Team/The-Aether) |
| Twilight Forest | Purple portal pool ringed with green foliage and colored flowers | [Official repository](https://github.com/TeamTwilight/twilightforest) |
| Nether | Obsidian frame and purple portal sparks | [Minecraft](https://www.minecraft.net/) |
| End | Teal Ender Eye with dark vertical pupil | [Minecraft](https://www.minecraft.net/) |
| Alex's Caves | Luminous green uranium with a dark radiation mark | [Official item registry](https://github.com/AlexModGuy/AlexsCaves/blob/main/src/main/java/com/github/alexmodguy/alexscaves/server/item/ACItemRegistry.java) |
| Alex's Mobs | Brown Animal Dictionary with pawprint and pale page edge | [Official repository](https://github.com/AlexModGuy/AlexsMobs) |
| Cobblemon | Red and cream capture ball with central button | [Official project](https://cobblemon.com/en) |
| The Undergarden | Turquoise Catalyst orb, gray casing and copper handle | [Official repository](https://github.com/quek04/undergarden) and [official project](https://www.curseforge.com/minecraft/mc-mods/the-undergarden) |
| Deeper and Darker | Dark sculk/Warden face with cyan antennae and mouth | [Official repository](https://github.com/KyaniteMods/DeeperAndDarker) |
| Blue Skies | Blue Journal with gold spine, white star and pale page edge | [Official repository description](https://gitlab.com/modding-legacy/blue-skies) and [publisher project description](https://modrinth.com/mod/blue-skies) |
| The Betweenlands | Four joined green Swamp Talisman pieces around an open center | [Official repository](https://github.com/Angry-Pixel/The-Betweenlands) |
| Quark | Bright cyan Heart of Diamond | [Official repository](https://github.com/VazkiiMods/Quark) and [publisher project](https://www.curseforge.com/minecraft/mc-mods/quark) |
| Vampirism | Pale vampire fangs over a crimson blood drop | [Official repository](https://github.com/TeamLapen/Vampirism) |
| Born in Chaos | Pale Nightmare Stalker skull, dark horns and deep eye/maw recess | [Publisher gallery](https://www.curseforge.com/minecraft/mc-mods/born-in-chaos/gallery) |

The referenced primary repositories and publisher descriptions were inspected
for motifs; the miniature compositions and palettes above are authored choices.
Portal frames, colored pupils, skull cavities and book spines deliberately
survive in the small version. The 16px artwork uses additional highlights
without smoothing the edges. The private comparison sheet shows both native
sizes and nearest-neighbor enlargements against a Chao-like coat color.

Validation covered all 32 outputs: dimensions, RGBA mode, binary transparency,
16 distinct emblems at each size, and rejection of unknown IDs. Visual review
included the native-size samples as well as the enlarged forms.
