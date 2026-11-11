"""Chunky Chao crowns authored in the official add-on's cuboid language.

The original broad head cube remains visible. Each themed crown is a small
set of large, editable blocks, with occasional deliberate block rotations and
flat terminal caps. The previous outlines remain design guides for silhouette
and theme identity; they are not exported as triangular skull surfaces.
"""
from head_profiles_tech import PROFILES as TECH
from head_profiles_magic import PROFILES as MAGIC
from head_profiles_adventure import PROFILES as ADVENTURE


SWEPT = {'create', 'mekanism', 'railcraft', 'enderio', 'draconic_evolution',
         'ice_fire', 'alexs_mobs', 'iron_spells', 'alexs_caves'}
ROUNDED = {'botania', 'aether', 'undergarden', 'computercraft',
           'mystical_agriculture', 'immersive_engineering'}
CURLED = {'hexerei', 'occultism', 'natures_aura', 'born_in_chaos'}
LUNAR = {'astral_sorcery', 'psi', 'evilcraft', 'vampirism',
         'deeper_darker', 'ars_nouveau'}


def side_style(ident):
    return ('swept' if ident in SWEPT else 'rounded' if ident in ROUNDED
            else 'curled' if ident in CURLED else 'lunar' if ident in LUNAR
            else 'stepped')


def box(name, material, center, size, rotation=(0, 0, 0), pivot=None):
    return dict(name=name, material=material, center=center, size=size,
                rotation=rotation, pivot=pivot or center)


def lobe(name, x, height, width=2, depth=2.5, z=.5, rx=0, rz=0,
         cap_width=None, cap_depth=None, material='head', tip='tuft', base=-3.55):
    """One chunky root and one flat cap, transformed around a shared base.

    The base sits .2 units inside the existing skull. Sharing the pivot and
    rotation keeps the cap attached even on backward or outward tilted quills.
    """
    pivot = (x, base, z)
    rotation = (rx, 0, rz)
    return [box(name, material, (x, base-height/2, z),
                (width, height, depth), rotation, pivot),
            box(name+'_cap', tip, (x, base-height-.25, z),
                (cap_width or width, .5, cap_depth or depth), rotation, pivot)]


def pair(name, height, width=2, depth=2.5, spread=2.25, z=.5,
         rx=0, rz=20, cap_width=None, cap_depth=None, tip='tuft'):
    return sum((lobe(name+('_left' if side < 0 else '_right'), side*spread,
                     height, width, depth, z, rx, side*rz,
                     cap_width, cap_depth, tip=tip)
                for side in (-1, 1)), [])


# These are authored layouts, not a voxelization of a continuous slope. The
# few large blocks echo the official Run/Power quills, Fly forks and baby tuft.
BLOCK_CROWNS = {
    'create': (
        lobe('leading_fold', -.5, 1.5, 3, 3.5, 1.0, -25, -10,
             cap_width=2.5, cap_depth=3)
        + lobe('left_tooth', -2.5, 1, 2, 2.5, .7, -15, -20,
               cap_width=1.5, cap_depth=2)
        + lobe('right_tooth', 2.1, 1, 2, 3, 1.2, -25, 15,
               cap_width=1.5, cap_depth=2.5)),
    'ae2': (
        lobe('fluix_peak', 0, 2, 2.5, 3, .5,
             cap_width=2, cap_depth=2.5)
        + lobe('quartz_left', -2.5, 1, 1.5, 2.5, .3, 0, -15,
               cap_width=1.5, cap_depth=2)
        + lobe('quartz_right', 2.5, 1, 1.5, 2.5, .3, 0, 15,
               cap_width=1.5, cap_depth=2)),
    'refined_storage': [
        box('quartz_root', 'head', (0, -4.0, .3), (5, 1, 4)),
        box('square_droplet', 'head', (0, -4.9, .8), (3, 1, 3)),
        box('disk_cap', 'tuft', (0, -5.6, 1.0), (2.5, .5, 2.5)),
    ],
    'mekanism': (
        pair('turbine', 1.5, 2.5, 3.5, 2.1, .8, -30, 15,
             cap_width=2, cap_depth=3)
        + [box('osmium_bridge', 'head', (0, -3.9, .8), (3, .5, 3))]),
    'immersive_engineering': (
        pair('coil', 1, 2.5, 3.5, 1.65, .4, -10, 10,
             cap_width=2, cap_depth=3)),
    'pneumaticcraft': (
        lobe('pressure_fold', 0, 1.5, 4, 4, .8, -15,
             cap_width=3, cap_depth=3)
        + [box('pressure_tip_cap', 'tuft', (0, -5.7, 1.3), (2, .5, 2.5))]),
    'computercraft': [
        box('shell_root', 'head', (0, -4, .3), (6, 1, 4.5)),
        box('shell_step', 'head', (0, -4.7, .5), (4, .5, 3.5)),
        box('terminal_cap', 'tuft', (0, -5.15, .7), (2, .5, 2.5)),
    ],
    'enderio': (
        pair('conduit_quill', 1.5, 2, 3.5, 2.2, .9, -30, 20,
             cap_width=1.5, cap_depth=3)
        + [box('conduit_root', 'head', (0, -3.95, .8), (3, .5, 3))]),
    'actually_additions': [
        box('empowered_root', 'head', (0, -4, .4), (5, 1, 4)),
        box('crystal_step', 'head', (0, -4.95, .6), (3.5, 1, 3)),
        box('lens_cap', 'tuft', (0, -5.7, .8), (2, .5, 2.5)),
    ],
    'railcraft': (
        lobe('locomotive_fold', 0, 1.5, 4, 4.5, .9, -30,
             cap_width=3, cap_depth=3.5)
        + [box('rail_shoulder', 'head', (0, -3.9, -.6), (5, .5, 2))]),
    'industrialcraft': (
        lobe('battery_fold', 0, 1.5, 2.5, 3.5, .6, -10,
             cap_width=2, cap_depth=3)
        + pair('insulator', 1, 1.5, 2.5, 2.4, .4, -10, 15,
               cap_width=1.5, cap_depth=2)),
    'buildcraft': (
        lobe('quarry_center', 0, 1.5, 2, 3, .7, -15,
             cap_width=1.5, cap_depth=2.5)
        + pair('quarry_tooth', 1, 1.5, 2.5, 2.3, .5, -10, 20,
               cap_width=1.5, cap_depth=2)),
    'draconic_evolution': (
        pair('wyvern_horn', 2, 1.5, 2.5, 2.4, .8, -25, 25,
             cap_width=1.5, cap_depth=2)
        + [box('draconic_ridge', 'head', (0, -4.05, 1.0), (2, 1, 3), (-20, 0, 0))]),
    'galacticraft': [
        box('rocket_root', 'head', (0, -4, .3), (4, 1, 4)),
        box('rocket_nose', 'head', (0, -4.95, .4), (2.5, 1, 3)),
        box('nose_cap', 'tuft', (0, -5.7, .5), (1.5, .5, 1.5)),
    ],
    'astral_sorcery': (
        pair('lunar_fork', 1.5, 2, 3, 2.2, .4, 15, 25,
             cap_width=1.5, cap_depth=2.5)
        + [box('star_basin', 'head', (0, -3.9, .3), (3, .5, 3))]),
    'botania': (
        lobe('central_petal', 0, 1.5, 2.5, 3, .4, -15,
             cap_width=2, cap_depth=2.5)
        + pair('outer_petal', 1, 2.5, 2.5, 2.4, .2, -10, 25,
               cap_width=2, cap_depth=2)),
    'blood_magic': [
        box('slate_root', 'head', (0, -4, .3), (5, 1, 4)),
        box('blood_drop', 'head', (0, -4.95, .5), (3.5, 1, 3)),
        box('ruby_cap', 'tuft', (0, -5.7, .7), (2, .5, 2)),
    ],
    'ars_nouveau': (
        pair('starbuncle_ear', 1.5, 2, 2.5, 2.45, .4, 10, 15,
             cap_width=1.5, cap_depth=2)
        + lobe('source_tuft', 0, .5, 2, 2.5, .8, -15,
               cap_width=1.5, cap_depth=2)),
    'occultism': (
        lobe('spirit_hook', .4, 2, 3, 3.5, .7, -20, -20,
             cap_width=2.5, cap_depth=2.5)
        + [box('wisp_shoulder', 'head', (-1.7, -4, .4), (2.5, 1, 3))]),
    'hexerei': [
        box('willow_root', 'head', (0, -4, .6), (5, 1, 3.5), (-10, 0, 0)),
        box('willow_bend', 'head', (.8, -4.7, 1.1), (3.5, 1, 3), (-15, 0, -15)),
        box('drooping_tip_cap', 'tuft', (2, -5, 1.7), (2, .5, 2.5), (-15, 0, -25)),
    ],
    'natures_aura': (
        lobe('golden_leaf', -.4, 1.5, 3.5, 3, .4, -10, -20,
             cap_width=3, cap_depth=2.5, tip='tuft')
        + lobe('seed_leaf', 1.9, .5, 2, 2.5, .2, -10, 20,
               cap_width=1.5, cap_depth=2, tip='tuft')),
    'roots': (
        lobe('sprout_left', -2.35, 1, 2, 2.5, .4, -10, -20,
             cap_width=1.5, cap_depth=2)
        + lobe('sprout_center', -.35, 1.5, 2, 3, .8, -15, -10,
               cap_width=1.5, cap_depth=2.5)
        + lobe('sprout_right', 2.1, 1, 2, 2.5, .4, -10, 25,
               cap_width=1.5, cap_depth=2)),
    'iron_spells': (
        lobe('spell_wave', 0, 1.5, 3, 4, .9, -30,
             cap_width=2.5, cap_depth=3)
        + [box('temple_fold_left', 'head', (-2.4, -4, .3), (2, 1, 2.5), (0, 0, -20)),
           box('temple_fold_right', 'head', (2.4, -4, .3), (2, 1, 2.5), (0, 0, 20))]),
    'projecte': [
        box('matter_root', 'head', (0, -4, .4), (5, 1, 4)),
        box('stone_step', 'head', (0, -4.95, .6), (3, 1, 3)),
        box('philosopher_cap', 'tuft', (0, -5.7, .8), (2, .5, 2)),
    ],
    'psi': (
        pair('psionic_fork', 1.5, 2, 3, 1.8, .5, 0, 0,
             cap_width=2, cap_depth=2.5)
        + [box('flow_root', 'head', (0, -3.9, .4), (3.5, .5, 3))]),
    'malum': (
        lobe('spirit_left', -2.3, 1.5, 2, 2.5, .7, -15, -15,
             cap_width=1.5, cap_depth=2)
        + lobe('spirit_center', -.3, 2, 2, 3, .8, -20, -10,
               cap_width=1.5, cap_depth=2.5)
        + lobe('spirit_right', 2.05, 1, 2, 2.5, .7, -20, 20,
               cap_width=1.5, cap_depth=2)),
    'evilcraft': (
        pair('vengeance_fold', 1, 3, 3, 2, .6, -25, 20,
             cap_width=2.5, cap_depth=2.5)
        + [box('dark_gem_root', 'head', (0, -3.9, .5), (2, .5, 3))]),
    'mystical_agriculture': (
        pair('essence_leaf', 1, 2.5, 3, 2, .4, -10, 30,
             cap_width=2, cap_depth=2.5)
        + [box('seed_bud', 'head', (0, -4, .5), (1.5, 1, 2.5))]),
    'hexcasting': (
        lobe('amethyst_center', 0, 2, 2, 2.5, .5, 0,
             cap_width=1.5, cap_depth=2)
        + [box('hex_facet_left', 'head', (-2.3, -4.1, .4), (2, 1, 2.5), (0, 0, -20)),
           box('hex_facet_right', 'head', (2.3, -4.1, .4), (2, 1, 2.5), (0, 0, 20))]),
    'cataclysm': (
        pair('ignis_fork', 1.5, 2.5, 3, 2.25, .7, -15, 15,
             cap_width=2, cap_depth=2.5)
        + [box('furnace_root', 'head', (0, -3.9, .6), (3, .5, 3))]),
    'ice_fire': (
        pair('dragon_horn', 1.5, 2, 3, 2.4, .9, -30, 15,
             cap_width=1.5, cap_depth=2.5)
        + [box('dragon_dorsal', 'head', (0, -4, 1.0), (2, 1, 3), (-25, 0, 0))]),
    'aether': [
        box('cloud_root', 'head', (0, -4, .4), (6, 1, 4.5)),
        box('cloud_left', 'head', (-1.8, -4.7, .6), (2.5, 1, 3.5), (0, 0, -15)),
        box('cloud_right', 'head', (1.8, -4.7, .6), (2.5, 1, 3.5), (0, 0, 15)),
        box('sky_cap', 'tuft', (0, -5.25, .9), (3, .5, 3)),
    ],
    'twilight_forest': (
        pair('antler_bud', 1.5, 2, 2.5, 2.25, .5, -10, 20,
             cap_width=1.5, cap_depth=2, tip='crest_tip')
        + [box('branch_left', 'crest_tip', (-3.15, -4.8, .5), (1.5, .5, 2),
               (-10, 0, -20), (-2.25, -3.55, .5)),
           box('branch_right', 'crest_tip', (3.15, -4.8, .5), (1.5, .5, 2),
               (-10, 0, 20), (2.25, -3.55, .5))]),
    'alexs_caves': (
        lobe('mineral_left', -2.3, 1, 2, 3, .7, -25, -15,
             cap_width=1.5, cap_depth=2.5)
        + lobe('mineral_center', -.4, 2, 2, 3, .9, -25, -5,
               cap_width=1.5, cap_depth=2.5)
        + lobe('mineral_right', 2, 1.5, 2, 3, .8, -25, 20,
               cap_width=1.5, cap_depth=2.5)),
    'alexs_mobs': (
        lobe('feather_left', -2.25, 1, 1.5, 3, .8, -30, -15,
             cap_width=1.5, cap_depth=2.5)
        + lobe('feather_center', -.2, 1.5, 2, 3.5, 1.0, -30, -5,
               cap_width=1.5, cap_depth=3)
        + lobe('feather_right', 2.0, 2, 1.5, 3, 1.0, -30, 15,
               cap_width=1.5, cap_depth=2.5)),
    'cobblemon': (
        pair('partner_ear', 1.5, 2, 2.5, 2.35, .4, 0, 15,
             cap_width=1.5, cap_depth=2)
        + [box('partner_tuft', 'head', (0, -4, .6), (2, 1, 2.5))]),
    'undergarden': [
        box('fungus_root', 'head', (0, -4, .4), (6.5, 1, 4.5)),
        box('gloom_step', 'head', (0, -4.75, .6), (5, .5, 3.5)),
        box('glimmer_cap', 'tuft', (0, -5.25, .8), (3, .5, 2.5)),
    ],
    'deeper_darker': (
        pair('listening_fin', 1.5, 2.5, 3, 2.1, .5, 15, 20,
             cap_width=2, cap_depth=2.5)
        + [box('sculk_saddle', 'head', (0, -3.9, .4), (3, .5, 3))]),
    'quark': [
        box('stone_root', 'head', (0, -4, .4), (5.5, 1, 4)),
        box('offset_stone', 'head', (-.4, -4.75, .6), (3.5, .5, 3)),
        box('stoneling_lobe', 'head', (.7, -5.25, .9), (2, .5, 2)),
        box('diamond_cap', 'tuft', (.7, -5.75, .9), (1.5, .5, 1.5)),
    ],
    'vampirism': (
        pair('bat_ear', 1.5, 2, 2.5, 2.3, .5, 10, 25,
             cap_width=1.5, cap_depth=2)
        + [box('widow_peak', 'head', (0, -3.9, .3), (2, .5, 3))]),
    'born_in_chaos': (
        [box('pumpkin_root', 'head', (0, -4, .4), (6, 1, 4)),
         box('pumpkin_step', 'head', (0, -4.75, .6), (4.5, .5, 3.5))]
        + lobe('crooked_stalk', -.25, 1, 1.5, 1.5, .8, -10, -20,
               cap_width=1, cap_depth=1.5, base=-4.8)),
}

BLOCK_DESCRIPTIONS = {
    'create': 'Three broad rectangular cog folds rise from the main head, with a backward leading block and flat brass caps. Forehead Engineer\'s Goggles carry the Create equipment cue.',
    'ae2': 'One tall fluix block and two shorter quartz blocks form a cut-ended crystal crown on the unchanged Chao head.',
    'refined_storage': 'A broad quartz root steps into a squared central droplet with a flat storage-colored cap.',
    'mekanism': 'Two chunky osmium turbine lobes lean backward from the head, with a low connecting block and flat terminal bands.',
    'immersive_engineering': 'Two wide copper coil lobes form a low block crown with a central saddle and flat cut ends.',
    'pneumaticcraft': 'A thick backward pressure fold narrows through two large rectangular steps while the broad face stays intact.',
    'computercraft': 'Three broad shell steps create a low turtle-like crown above the original rectangular head.',
    'enderio': 'Two substantial ender-dark conduit quills sweep backward, joined visually by a low central root and flat color caps.',
    'actually_additions': 'Three large stepped blocks form an empowered-crystal crest with a broad root and a flat colored lens tip.',
    'railcraft': 'A single wide locomotive fold leans rearward above a low rail shoulder, ending in a broad cut-off color cap.',
    'industrialcraft': 'A central battery fold and two smaller insulation blocks form a compact scalloped crown with flat tips.',
    'buildcraft': 'Three broad quarry-tooth blocks rise at different heights, each ending in a rectangular color band.',
    'draconic_evolution': 'Two thick, outward and backward rotated wyvern horns flank one low rectangular dorsal ridge.',
    'galacticraft': 'A clean stack of three substantial moon-grey rocket-nose blocks ends in a flat cap above the original skull.',
    'astral_sorcery': 'Two broad tilted lunar forks frame a low star basin; each fork is a rectangular root and flat terminal cap.',
    'botania': 'Three chunky mana-petal lobes spread from the main head, with a taller central block and broad pink cut ends.',
    'blood_magic': 'Large ritual-slate steps rise into a blocky ruby droplet crown with one broad red terminal cap.',
    'ars_nouveau': 'Two broad Starbuncle ear blocks surround a short central source tuft, using rectangular roots and flat caps.',
    'occultism': 'One broad offset spirit hook and a low shoulder block suggest an Otherworld wisp through a few chunky rotated pieces.',
    'hexerei': 'Three substantial rectangular blocks make a low off-center willow bend with a drooping flat tip.',
    'natures_aura': 'A broad offset golden-leaf fold and shorter seed-leaf block use wide roots and flat golden caps.',
    'roots': 'Three unequal rectangular sprout lobes form a low branching crown, with moss-colored roots and cut-off tips.',
    'iron_spells': 'One broad rearward spell-wave block rises between two short tilted temple folds and ends in a flat cap.',
    'projecte': 'Dark-matter shoulders step through three large blocks into a flat Philosopher\'s Stone-colored tip.',
    'psi': 'Two blunt squared psionic forks rise around a low connecting block, preserving the clean notch of the previous silhouette.',
    'malum': 'Three unequal soulwood spirit blocks lean in different directions, with a taller offset middle lobe and flat spirit-colored caps.',
    'evilcraft': 'Two wide tilted vengeance folds flank a low dark-gem root, with broad crimson cut ends.',
    'mystical_agriculture': 'Two substantial seed-leaf blocks open around a low central bud; their flat essence-colored ends keep the plant shape in the add-on style.',
    'hexcasting': 'One tall amethyst block sits between two short tilted temple facets, ending in a flat cap rather than a triangular peak.',
    'cataclysm': 'Two large rotated Ignis forks grow from the top of the original head, with a low furnace root and flat warm caps.',
    'ice_fire': 'Two thick rear-swept dragon horn blocks flank a low rectangular dorsal ridge; their ends remain visibly cut off.',
    'aether': 'A broad cloud root supports two tilted rectangular cloud lobes and a low sky-colored cap.',
    'twilight_forest': 'Two broad forest antler-bud blocks have compact rectangular side branches and golden flat tips.',
    'alexs_caves': 'Three unequal chunky mineral blocks form an asymmetric backward fan with small flat luminous caps.',
    'alexs_mobs': 'Three substantial crow-feather blocks lean backward at unequal heights, with broad naturalist-colored roots and dark flat ends.',
    'cobblemon': 'Two chunky partner-ear blocks flank a short rectangular brow tuft, while the capture-ball marker carries the explicit mod cue.',
    'undergarden': 'Three wide rectangular tiers suggest a low gloom-fungus crown without a stalk or overhanging brim.',
    'deeper_darker': 'Two broad sculk listening-fin blocks rise from a low saddle, with flat cyan terminals instead of thin sensors.',
    'quark': 'Four offset stone steps make a compact stoneling crown, with one modest flat terminal cap.',
    'vampirism': 'Two broad tilted bat-ear blocks frame a low widow-peak root, ending in flat pale caps.',
    'born_in_chaos': 'Two broad pumpkin blocks support a short crooked rectangular stalk with a flat terminal band.',
}

RAW_PROFILES = {**TECH, **MAGIC, **ADVENTURE}
assert len(RAW_PROFILES) == len(TECH) + len(MAGIC) + len(ADVENTURE)
assert set(BLOCK_CROWNS) == set(BLOCK_DESCRIPTIONS) == set(RAW_PROFILES)
HEAD_PROFILES = {ident: {**profile, 'description': BLOCK_DESCRIPTIONS[ident]}
                 for ident, profile in RAW_PROFILES.items()}


def head_parts(api, ident):
    """Return native editable cuboids added to the separately drawn head cube."""
    cube = api['cuboid']
    return [cube('crown_'+ident+'_'+part['name'], part['material'],
                 part['center'], part['size'], part['rotation'], part['pivot'])
            for part in BLOCK_CROWNS[ident]]


def manifest():
    return {'format_version': 2, 'construction': 'cuboids_on_main_head',
            'ids': list(HEAD_PROFILES),
            'preview_ids': [*HEAD_PROFILES, 'thaumcraft'],
            'profiles': {ident: {'name': profile['name'],
                                  'description': profile['description'],
                                  'side_profile': side_style(ident),
                                  'crown_cubes': len(BLOCK_CROWNS[ident])}
                         for ident, profile in HEAD_PROFILES.items()}}
