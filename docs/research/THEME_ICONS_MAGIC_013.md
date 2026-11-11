# Magic-theme emblem study - 0.13.0

The old art reused generic stars, leaves, drops and book rectangles across different mods. The new emblems identify a particular object or visual language from each project, using its characteristic materials and colors. These are original, manually drawn pixel symbols rather than extracted textures or exact logo reproductions.

Each motif has an explicitly authored **8x8** egg mark and a **16x16** chest emblem with extra native-pixel detail. The small mark is never produced by shrinking the detailed art. Transparency is binary, and no outline fringe is synthesized. The 16px version retains the same silhouette except where the larger grid permits the actual five-point occult diagram and individual constellation stars.

| Theme | Identifying motif | Primary reference |
| --- | --- | --- |
| Thaumcraft | Paired gold Goggles of Revealing with purple lenses, using the already verified publisher reference from the goggle study | [Azanor's project](https://www.curseforge.com/minecraft/mc-mods/thaumcraft), [previous direct-asset reference study](GOGGLE_STUDY_09.md) |
| Astral Sorcery | Blue constellation links joining five pale starlight nodes | [Official repository](https://github.com/HellFirePvP/AstralSorcery/) |
| Botania | Terrasteel-green Terra Blade, pale blade edge and gold guard | [Official Lexicon, Terra Blade](https://botaniamod.net/lexicon.html) |
| Blood Magic | Polished red Blood Orb in a darker rim | [WayofTime's repository and Blood Orb FAQ](https://github.com/WayofTime/BloodMagic) |
| Ars Nouveau | Purple spellbook with gold binding and a cyan cover jewel | [Author-maintained repository](https://github.com/baileyholl/Ars-Nouveau), [Spell Books](https://ars.guide/book/spell_casting/spell_books/) |
| Occultism | Ivory chalk pentacle on a dark violet circular field | [Author-maintained repository](https://github.com/klikli-dev/occultism) |
| Hexerei | Brown witch's broom with a bound straw brush | [JoeFoxe's repository](https://github.com/JoeFoxe/Hexerei) |
| Nature's Aura | Gold Leaf with bright gold vein and green stem | [Ellpeck's repository](https://github.com/Ellpeck/NaturesAura) |
| Roots | Forked wooden herb staff with green leaves | [MysticMods repository](https://github.com/MysticMods/Roots), [project documentation](https://moddedmc.wiki/en/project/roots/latest) |
| Iron's Spells 'n Spellbooks | Open, gold-bound book with blue covers and parchment pages | [Official Spellbooks documentation](https://iron.wiki/spellbooks/) |
| Malum | Warm spirit flame enclosed by a violet soul outline | [SammySemicolon's repository](https://github.com/SammySemicolon/Malum-Mod), [author's project description](https://modrinth.com/project/malum) |
| EvilCraft | Dark Gem with a red-violet pointed crystal silhouette | [Author's Origins of Darkness](https://evilcraft.rubensworks.net/book/tag_index.html), [author's project page](https://modrinth.com/project/evilcraft) |
| Mystical Agriculture | Bright green Inferium Essence cluster | [Blake's official Inferium Essence documentation](https://www.blakesmods.com/wiki/1.20/mysticalagriculture/items/inferium-essence) |
| Hex Casting | Purple media crystal set on a brown casting staff | [Official Hex Book, staves](https://hexcasting.hexxy.media/v/latest/main/en_us/) |
| Draconic Evolution | Gold-orange Draconic Core with a dark violet central aperture | [Official repository](https://github.com/Draconic-Inc/Draconic-Evolution) |
| Tinkers' Construct | Broad cobalt-blue hammer head and wooden handle | [SlimeKnights repository](https://github.com/SlimeKnights/TinkersConstruct), [official gameplay documentation](https://slimeknights.github.io/docs/gameplay/tinkers-construct-3/) |

The motifs communicate visual heritage only; they do not change which mods are available or compatible. The existing catalog and mod-loaded conditions remain the source of availability.

Implementation: `art/theme_icons_magic.py`. It exposes `ICON_IDS` and `render_theme(ident, size=8|16)` and rejects unsupported names or sizes. Visual review covered both native sizes, nearest-neighbor enlargements, and all 16 marks composited on their actual egg-family shell colors.
