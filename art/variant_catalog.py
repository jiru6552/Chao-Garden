"""Original mod-history Chao themes; source of the shared runtime/art catalog."""
from pathlib import Path
import json
from chao_head_morphs import HEAD_PROFILES

ROOT = Path(__file__).resolve().parents[1]
# These IDs remain recognized only for loading old saves; no active theme uses them.
RETIRED_VARIANTS = tuple('''botania ae2 mekanism immersive_engineering thermal pneumaticcraft computercraft enderio actually_additions railcraft industrialcraft buildcraft hexerei natures_aura roots psi evilcraft mystical_agriculture hexcasting alexs_caves alexs_mobs undergarden deeper_darker blue_skies betweenlands tinkers galacticraft quark vampirism'''.split())

# ID | name | accepted mod IDs (OR) | vanilla recipe additions | four colors | icon | CurseForge slug
ROWS = '''
create|Create|create|copper_ingot,iron_ingot,redstone|468c88,d4a34d,805332,f5ce69|gear|create
thaumcraft|Thaumcraft|thaumcraft|gold_ingot,amethyst_shard,glass_bottle|635078,b78a44,60d9d6,b18ded|rune|thaumcraft
astral_sorcery|Astral Sorcery|astralsorcery|quartz,amethyst_shard,glowstone_dust|dedfdd,263b70,80d6ed,e5f6ff|star|astral-sorcery
botania|Botania|botania|vine,pink_petals,iron_ingot|75b875,e9a4c8,68c6d9,86e8bb|flower|botania
blood_magic|Neo Vitae|neovitae|redstone,ghast_tear,gold_ingot|542e43,bc4059,d9aa51,f05a72|drop|neovitae
ae2|Applied Energistics 2|ae2,appliedenergistics2|amethyst_shard,quartz,redstone|d9dfdf,8065bd,64d1df,b297ed|crystal|applied-energistics-2
refined_storage|Refined Storage|refinedstorage|quartz,diamond,redstone|e4e9ec,526776,55c5df,7be8f7|disk|refined-storage
cataclysm|L_Ender's Cataclysm|cataclysm|blackstone,blaze_rod,netherite_scrap|34343d,b84e31,e5b34b,ffad42|flame|lendercataclysm
ice_fire|Ice and Fire|iceandfire|packed_ice,blaze_powder,dragon_breath|81b7d5,cc6b4b,dceff1,79ddeb|dragon|ice-and-fire-dragons
aether|The Aether|aether|feather,glowstone_dust,gold_ingot|e5eff2,87c3e5,e6c86e,bceeff|cloud|aether
twilight_forest|The Twilight Forest|twilightforest|oak_sapling,glow_berries,diamond|6b8b86,8c70aa,d8c67c,dfeb8d|firefly|the-twilight-forest
nether|Nether||blackstone,magma_cream,blaze_powder|544048,b94b3f,eaa254,ffb553|flame|
end|End||end_stone,popped_chorus_fruit,ender_pearl|55476f,dad5a2,8fdbbe,ce9de9|eye|
mekanism|Mekanism|mekanism|iron_ingot,redstone,slime_ball|8e9ba4,315f68,71d0a9,8bec9e|energy|mekanism
immersive_engineering|Immersive Engineering|immersiveengineering|copper_ingot,leather,iron_ingot|786253,a96845,b4bec3,e8b164|coil|immersive-engineering
thermal|Thermal Series|thermal,thermal_foundation,thermal_expansion|iron_ingot,blaze_powder,lapis_lazuli|a8b1b4,e39345,5d9bc8,eecc6b|gear|thermal-expansion
pneumaticcraft|PneumaticCraft|pneumaticcraft|iron_ingot,redstone,glass|9aa4a5,c6544e,647986,8edbde|gauge|pneumaticcraft-repressurized
computercraft|CC: Tweaked|computercraft|stone,redstone,glass_pane|687d71,273e3c,79cf83,91e8a1|terminal|cc-tweaked
enderio|Ender IO|enderio|iron_ingot,ender_pearl,redstone|4d505f,7a6c88,84c78f,b6e69c|eye|ender-io
actually_additions|Actually Additions|actuallyadditions|quartz,lapis_lazuli,redstone|d8d9e1,54545f,62cbd4,9ae9eb|lens|actually-additions
railcraft|Railcraft|railcraft|rail,iron_ingot,copper_ingot|586f89,98704a,d8bd70,efcf80|rail|railcraft-reborn
industrialcraft|IndustrialCraft|ic2|iron_ingot,redstone,yellow_dye|dac35f,687c80,9cdebe,a4ee9f|circuit|industrial-craft
buildcraft|BuildCraft|buildcraftcore|gold_ingot,glass,redstone|d2b657,484b50,e9d985,83dce7|pipe|buildcraft
ars_nouveau|Ars Nouveau|ars_nouveau|amethyst_shard,book,gold_ingot|ac94ce,745e9a,e1c678,c5a1f1|book|ars-nouveau
occultism|Occultism|occultism|bone,black_dye,amethyst_shard|555263,e1dcce,ad88c9,d3b8ec|rune|occultism
hexerei|Hexerei|hexerei|oak_sapling,purple_dye,wheat|8c81a9,57556b,c8b589,cfb4e6|moon|hexerei
natures_aura|Nature's Aura|naturesaura|oak_leaves,gold_ingot,glow_berries|a4c87f,d5b85c,72997d,e6dd8e|leaf|natures-aura
roots|Roots / Roots Classic|roots,rootsclassic|oak_log,moss_block,wheat_seeds|967553,79a976,c4c084,abdb8d|sprout|roots
iron_spells|Iron's Spells 'n Spellbooks|irons_spellbooks|book,lapis_lazuli,gold_ingot|5777a4,dbb773,a5d1e3,7ec6ee|scroll|irons-spells-n-spellbooks
projecte|ProjectE|projecte|obsidian,redstone,gold_ingot|494354,c94657,dfbf69,e4757b|circle|projecte
psi|Psi|psi|iron_ingot,lapis_lazuli,redstone|e0e6e9,67bfd4,ccdfb2,9be7ed|circuit|psi
malum|Malum|malum|amethyst_shard,soul_sand,copper_ingot|685276,d99568,bd8ed2,e0a8ee|soul|malum
evilcraft|EvilCraft|evilcraft|black_dye,redstone,amethyst_shard|474851,b9435d,9bb574,e96880|drop|evilcraft
mystical_agriculture|Mystical Agriculture|mysticalagriculture|wheat_seeds,emerald,dirt|7fb56f,b9d28b,7fbbbd,c5e68c|sprout|mystical-agriculture
hexcasting|Hex Casting|hexcasting|amethyst_shard,stick,paper|827492,c694d2,e3be85,ddb4ef|hex|hexcasting
alexs_caves|Alex's Caves|alexscaves|copper_ingot,slime_ball,glow_ink_sac|6d8b67,a6cf65,5c797a,c5f28b|atom|alexs-caves
alexs_mobs|Alex's Mobs|alexsmobs|feather,leather,honeycomb|c3a376,715e4b,d7d0b0,e3c393|feather|alexs-mobs
cobblemon|Cobblemon|cobblemon|red_dye,white_dye,iron_ingot|75a99b,c95059,e9e6da,da505a|ball|cobblemon
undergarden|The Undergarden|undergarden|moss_block,glow_lichen,clay_ball|66765f,78bfb3,a2a37f,89ddd2|mushroom|the-undergarden
deeper_darker|Deeper and Darker|deeperdarker|sculk,echo_shard,cyan_dye|334c5b,67bfc0,9cc9bd,7ae2dc|sculk|deeperdarker
blue_skies|Blue Skies|blue_skies|blue_ice,quartz,gold_ingot|8dcbd1,dcc88a,6387b7,c6f1ec|sun|blue-skies
betweenlands|The Betweenlands|thebetweenlands|mud,brown_mushroom,vine|7c8061,ad986c,80a695,bfcea0|mushroom|angry-pixel-the-betweenlands-mod
draconic_evolution|Draconic Evolution|draconicevolution|obsidian,blaze_powder,dragon_breath|574467,dc8f4e,9d77bd,f2b55d|energy|draconic-evolution
tinkers|Tinkers' Construct|tconstruct|iron_ingot,slime_ball,copper_ingot|638fbb,b88c56,92d4a0,b3ebaa|hammer|tinkers-construct
galacticraft|Galacticraft|galacticraftcore,galacticraft|glass,iron_ingot,compass|e0e5df,748b9e,69bed4,c3edf3|planet|galacticraft-legacy
quark|Quark|quark|stone,oak_log,amethyst_shard|9d9b91,bc8b6c,8eae97,d4c597|stone|quark
vampirism|Vampirism|vampirism|redstone,black_dye,leather|6e5869,ad3b53,e6d8c6,ed7790|fang|vampirism-become-a-vampire
born_in_chaos|Born in Chaos|born_in_chaos_v1|bone,soul_sand,rotten_flesh|716a78,c69071,a1b3a6,e2ac77|skull|born-in-chaos
'''

def rgb(value):
    return [int(value[i:i+2], 16) for i in (0,2,4)]

def catalog():
    out=[]
    for row in ROWS.strip().splitlines():
        id,name,mods,ingredients,colors,icon,slug=row.split('|')
        if id in RETIRED_VARIANTS:
            continue
        base,accent,trim,orb=map(rgb,colors.split(','))
        out.append(dict(id=id,name=name,mods=mods.split(',') if mods else [],head_morph=id in HEAD_PROFILES,
            ingredients=['minecraft:'+s for s in ingredients.split(',')], icon=icon,
            palette=dict(body=base,head=base,tuft=accent,accent=accent,
                crest_tip=trim,hand=trim,toe=accent,heel=base,wing=accent,
                belly=trim,collar=accent,orb=orb,variant_detail=trim),
            source='https://www.curseforge.com/minecraft/mc-mods/'+slug if slug else 'https://www.minecraft.net/'))
    next(v for v in out if v['id']=='cobblemon')['palette']['collar']=[48,58,61]
    active_ids={entry['id'] for entry in out}
    if len(out)!=19 or len(active_ids)!=19 or len(RETIRED_VARIANTS)!=29 or active_ids.intersection(RETIRED_VARIANTS):
        raise ValueError('Theme generation requires exactly nineteen retained themes and twenty-nine separate legacy IDs')
    return out

VARIANTS=catalog()

if __name__=='__main__':
    target=ROOT/'src/main/resources/assets/chao_garden/chao_variants.json'
    target.write_text(json.dumps({'format_version':1,'variants':VARIANTS},indent=2)+'\n')
    (target.parent/'retired_chao_variants.json').write_text(json.dumps(list(RETIRED_VARIANTS),indent=2)+'\n')
    print(f'Wrote {len(VARIANTS)} themes: {sum(bool(v["mods"]) for v in VARIANTS)} mod gates and 2 vanilla dimensions')
