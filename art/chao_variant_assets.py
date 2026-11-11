"""Original mod-inspired forms for the shared Chao rig.

The module receives the canonical generator's helpers instead of importing it,
so it never creates a second rig or writes assets as an import side effect.
Every theme has separate head, body and floating-marker groups. Confirmed
1.21.1 themes use a few authored cuboid crests on the shared rectangular head.
Create and Thaumcraft add their own forehead goggles. This is independent of
the deferred small-animal appearance system.
"""
from __future__ import annotations

import math
from chao_head_morphs import HEAD_PROFILES, head_parts
from chao_goggles import append_goggles


def append_variant_bones(api, definitions):
    bone, cube = api['bone'], api['cuboid']
    plane, extrude, torus = api['pixel_plane'], api['extrude'], api['torus']
    expected = {
        'create', 'thaumcraft', 'astral_sorcery', 'botania', 'blood_magic',
        'ae2', 'refined_storage', 'cataclysm', 'ice_fire', 'aether',
        'twilight_forest', 'nether', 'end', 'mekanism',
        'immersive_engineering', 'thermal', 'pneumaticcraft', 'computercraft',
        'enderio', 'actually_additions', 'railcraft', 'industrialcraft',
        'buildcraft', 'ars_nouveau', 'occultism', 'hexerei', 'natures_aura',
        'roots', 'iron_spells', 'projecte', 'psi', 'malum', 'evilcraft',
        'mystical_agriculture', 'hexcasting', 'alexs_caves', 'alexs_mobs',
        'cobblemon', 'undergarden', 'deeper_darker', 'blue_skies',
        'betweenlands', 'draconic_evolution', 'tinkers', 'galacticraft',
        'quark', 'vampirism', 'born_in_chaos',
    }

    for definition in definitions:
        ident = definition['id']
        if ident not in expected:
            raise ValueError('No authored accessory design for Chao variant '+ident)
        prefix = 'variant_'+ident
        head, body, marker = [], [], []

        def h(name, material, center, size, rotation=(0, 0, 0)):
            head.append(cube(name, material, center, size, rotation))

        def b(name, material, center, size, rotation=(0, 0, 0)):
            body.append(cube(name, material, center, size, rotation))

        def m(name, material, center, size, rotation=(0, 0, 0)):
            marker.append(cube(name, material, center, size, rotation))

        def cap(name='cap', brim=True, tall=1.5):
            h(name+'_crown', 'accent', (0, -3.75-tall/2, .1), (5.5, tall, 4.5))
            if brim:
                h(name+'_brim', 'crest_tip', (0, -3.85, -2.6), (7, .5, 2))

        def goggles(name='goggles'):
            # Parked above the forehead, leaving the full Chao eye planes free.
            h(name+'_strap', 'collar', (0, -3.8, -2.3), (6, .5, .5))
            for side in (-1, 1):
                h(name+'_rim_'+str(side), 'accent', (side*1.45, -4.25, -2.4), (2, 1.5, 1))
                h(name+'_lens_'+str(side), 'shine', (side*1.45, -4.25, -2.95), (1, .5, .5))

        def horns(name='horn', height=2.5, spread=2.4):
            for side in (-1, 1):
                h(name+'_'+str(side), 'tuft', (side*spread, -4.55, 1.0), (1, height, 1.5), (0, 0, side*22.5))
                h(name+'_tip_'+str(side), 'crest_tip', (side*(spread+.45), -5.7, 1.0), (1, .5, 1), (0, 0, side*22.5))

        def crystal_crest(name='crystal', triple=False):
            h(name+'_center', 'crest_tip', (0, -5.0, .1), (1.5, 2.5, 1.5), (0, 0, 15))
            if triple:
                for side in (-1, 1):
                    h(name+'_side_'+str(side), 'accent', (side*1.65, -4.55, .3), (1, 2, 1), (0, 0, side*27))

        def leaves(name='leaf', count=3):
            for i in range(count):
                side = i-(count-1)/2
                h(name+'_'+str(i), 'crest_tip' if i % 2 else 'tuft',
                  (side*1.35, -4.65-abs(side)*.12, .1+abs(side)*.2),
                  (1, 2.5, 1.5), (-15, 0, side*25))

        def backpack(name='pack', material='accent', pair=False):
            if pair:
                for side in (-1, 1):
                    b(name+'_'+str(side), material, (side*.95, .55, 2.55), (1, 2.5, 1.5))
            else:
                b(name, material, (0, .6, 2.55), (2.5, 2.5, 1.5))

        def book_pack(name='book', material='accent'):
            b(name+'_pages', 'shine', (0, .55, 2.55), (2, 2.5, 1))
            b(name+'_cover', material, (0, .55, 3.15), (2.5, 3, .5))
            b(name+'_clasp', 'crest_tip', (.8, .55, 3.45), (.5, .5, .5))

        def cube_marker(name='emote'):
            m(name, 'orb', (0, 0, 0), (2, 2, 2))

        def crystal_marker(name='gem'):
            m(name, 'orb', (0, 0, 0), (1.5, 2, 1.5), (0, 0, 45))

        def gear_marker(name='cog'):
            m(name+'_hub', 'orb', (0, 0, 0), (1.5, 1.5, 1))
            for i, (x, y) in enumerate(((0, -1.25), (1.25, 0), (0, 1.25), (-1.25, 0))):
                m(name+'_tooth_'+str(i), 'crest_tip', (x, y, 0), (1, 1, 1))

        def star_marker(name='star', points=4):
            outline = []
            for i in range(points*2):
                angle = -math.pi/2 + i*math.pi/points
                radius = 1.45 if i % 2 == 0 else .55
                outline.append((math.cos(angle)*radius, math.sin(angle)*radius))
            marker.append(extrude(name, 'orb', outline, .5))

        def ring_marker(name='ring'):
            marker.append(torus(name, 'orb', 1.25, .25, 12, 4))
            m(name+'_core', 'crest_tip', (0, 0, 0), (.5, 1, .5))

        def lantern_marker(name='lantern'):
            m(name+'_light', 'orb', (0, 0, 0), (1.5, 1.5, 1.5))
            for y in (-1, 1):
                m(name+'_cap_'+str(y), 'accent', (0, y, 0), (2, .5, 2))

        def tablet_marker(name='tablet'):
            m(name+'_case', 'accent', (0, 0, 0), (2, 2.5, 1))
            m(name+'_screen', 'orb', (0, -.2, -.55), (1, 1.5, .5))

        # The atlas owns the exact glyph. The patch has a transparent surround.
        body.append(plane('theme_glyph', 'variant_detail', -.9, -.65, 1.8, 1.8,
                          z=-1.78, uv_size=(16, 16)))

        if ident == 'create':
            h('cog_mount', 'accent', (0, -4.8, 1.3), (3, 1, 2))
            h('offset_gear_tooth', 'crest_tip', (1.6, -5.2, 1.3), (1, 1, 1))
            backpack('kinetic_housing', 'accent')
            b('shaft', 'crest_tip', (0, .6, 3.55), (1, 2, .5))
            gear_marker('brass_cog')
        elif ident == 'thaumcraft':
            h('wisp_socket', 'accent', (0, -4.1, 1.2), (2, 1, 2))
            book_pack('research_tome', 'collar')
            crystal_marker('vis_wisp')
        elif ident == 'astral_sorcery':
            crystal_crest('rock_crystal', True)
            h('marble_circlet', 'accent', (0, -2.8, -3.35), (6, .5, .5))
            b('constellation_belt', 'crest_tip', (0, 1.3, -1.8), (4, .5, .5))
            star_marker('celestial_star', 4)
        elif ident == 'botania':
            leaves('livingwood_leaf', 2)
            h('petal_center', 'orb', (0, -4.8, -1.8), (1, 1, 1))
            for side in (-1, 1):
                h('flower_petal_'+str(side), 'accent', (side*1.4, -4.65, -1.7), (1, 1.5, 1), (0, 0, side*30))
            b('leaf_tunic', 'tuft', (0, 1.3, 1.85), (3, 1, .5))
            star_marker('mana_flower', 5)
        elif ident == 'blood_magic':
            h('ritual_circlet', 'collar', (0, -2.9, -3.4), (6, .5, .5))
            for side in (-1, 1):
                h('altar_prong_'+str(side), 'accent', (side*2.6, -4.4, .1), (1, 1.5, 1))
            b('ritual_belt', 'accent', (0, 1.4, -1.85), (4, .5, .5))
            marker.append(extrude('blood_drop', 'orb', [(0,-1.6),(-1.1,.2),(-.7,1.1),(0,1.4),(.7,1.1),(1.1,.2)], .6))
        elif ident == 'ae2':
            crystal_crest('fluix_cluster', True)
            for side in (-1, 1):
                h('quartz_temple_'+str(side), 'accent', (side*3.5, -2.5, -.1), (.5, 1.5, 2))
            backpack('storage_cell', 'accent')
            crystal_marker('fluix_cell')
        elif ident == 'refined_storage':
            h('controller_brow', 'accent', (0, -2.85, -3.4), (7, .5, .5))
            h('square_antenna', 'crest_tip', (2.3, -4.9, 1), (1, 2.5, 1))
            h('antenna_cap', 'orb', (2.3, -6.25, 1), (1.5, .5, 1.5))
            backpack('disk_drive', 'accent')
            tablet_marker('storage_disk')
        elif ident == 'cataclysm':
            for side in (-1, 1):
                h('ignis_crown_'+str(side), 'accent', (side*2.25, -4.4, .1), (1.5, 2, 2), (0, 0, side*20))
            h('basalt_crest', 'tuft', (0, -4.4, 1.2), (2, 1.5, 2))
            b('ignis_plate', 'collar', (0, .6, -1.95), (2.5, 2, .5))
            # Re-add the front glyph beyond the raised breastplate.
            body[-1:] = [body[-1], plane('ignis_rune', 'variant_detail', -.8, -.2, 1.6, 1.6, z=-2.22, uv_size=(16,16))]
            star_marker('ignis_core', 4)
        elif ident == 'ice_fire':
            horns('dragon_horn', 2.5, 2.2)
            for i in range(2):
                h('dragon_dorsal_'+str(i), 'crest_tip', (0, -4.3, i*1.6), (1, 1.5, 1.5), (-25, 0, 0))
            b('scale_backplate', 'collar', (0, .7, 2.2), (3, 2.5, 1))
            crystal_marker('elemental_egg')
        elif ident == 'aether':
            h('valkyrie_circlet', 'accent', (0, -2.85, -3.4), (6.5, .5, .5))
            for side in (-1, 1):
                h('cloud_winglet_'+str(side), 'shine', (side*3.8, -3.35, .4), (2, 1, 2), (0, 0, side*25))
            b('sky_pendant', 'accent', (0, -.6, -1.85), (1, 1, .5))
            star_marker('sun_spirit', 4)
        elif ident == 'twilight_forest':
            for side in (-1, 1):
                h('antler_stem_'+str(side), 'tuft', (side*2.4, -4.8, .4), (.5, 2.5, .5), (0, 0, side*15))
                h('antler_branch_'+str(side), 'crest_tip', (side*3.0, -5.0, .4), (1.5, .5, .5), (0, 0, -side*30))
            b('moss_cloak', 'collar', (0, .6, 2.0), (3, 2.5, .5))
            lantern_marker('firefly')
        elif ident == 'nether':
            for side in (-1, 1):
                h('basalt_spire_'+str(side), 'tuft', (side*2.3, -4.65, 1), (1.5, 2.5, 1.5), (0, 0, side*18))
            h('lava_ridge', 'crest_tip', (0, -3.95, -.1), (2, .5, 3))
            backpack('blackstone_vent', 'collar')
            marker.append(extrude('nether_flame', 'orb', [(-.9,1.2),(-1.1,.1),(-.3,-1.5),(.1,-.2),(.7,-1.1),(1.1,.4),(.6,1.2)], .6))
        elif ident == 'end':
            h('end_crystal_base', 'collar', (0, -4, .3), (3, .5, 3))
            crystal_crest('end_spire')
            for side in (-1, 1):
                h('chorus_bud_'+str(side), 'accent', (side*2.4, -4.15, 1.1), (1, 1, 1))
            b('purpur_belt', 'accent', (0, 1.3, -1.8), (4, .5, .5))
            crystal_marker('end_crystal')
        elif ident == 'mekanism':
            h('steel_helmet', 'collar', (0, -3.9, .25), (6, 1, 5))
            for side in (-1, 1):
                h('helmet_vent_'+str(side), 'accent', (side*3.2, -2.5, 1.0), (1, 1.5, 2))
            backpack('gas_tank', 'crest_tip', True)
            cube_marker('energy_tablet')
        elif ident == 'immersive_engineering':
            goggles('engineer_goggles')
            b('coil_frame', 'collar', (0, .6, 2.4), (3, 2.5, 1))
            for y in (-.1, .6, 1.3):
                b('copper_winding_'+str(y), 'accent', (0, y, 3.0), (2.5, .5, .5))
            gear_marker('copper_coil')
        elif ident == 'thermal':
            for side in (-1, 1):
                h('thermal_fin_'+str(side), 'tuft' if side < 0 else 'crest_tip', (side*2.1, -4.4, .6), (1.5, 2, 2), (0, 0, side*20))
            backpack('redstone_coil', 'accent')
            b('coil_band', 'crest_tip', (0, .6, 3.45), (2, .5, .5))
            gear_marker('electrum_gear')
        elif ident == 'pneumaticcraft':
            cap('pressure_helmet', False, 1)
            for side in (-1, 1):
                h('helmet_vent_'+str(side), 'crest_tip', (side*2.7, -4.15, -.1), (.5, 1, 2))
            backpack('air_canister', 'crest_tip', True)
            m('pressure_gauge', 'accent', (0, 0, 0), (2.5, 2.5, 1))
            m('gauge_face', 'shine', (0, 0, -.6), (1.5, 1.5, .5))
            m('gauge_needle', 'orb', (.2, -.15, -.9), (.5, 1.5, .5), (0,0,35))
        elif ident == 'computercraft':
            h('terminal_cap', 'accent', (0, -4, .2), (5, 1, 4))
            h('wireless_modem', 'crest_tip', (2.8, -4.5, 1), (1, 1.5, 1))
            backpack('turtle_shell', 'collar')
            tablet_marker('floppy_disk')
        elif ident == 'enderio':
            for side in (-1, 1):
                h('darksteel_rail_'+str(side), 'collar', (side*2.7, -4.0, .4), (1, 1, 4))
            h('ender_capacitor', 'crest_tip', (0, -4.7, 1.2), (1.5, 1.5, 1.5))
            b('conduit_spine', 'accent', (0, .5, 2.25), (1, 3, 1))
            ring_marker('ender_core')
        elif ident == 'actually_additions':
            h('laser_lens', 'shine', (0, -4.2, -2.1), (2.5, 1.5, 1))
            h('lens_mount', 'collar', (0, -4.2, -.9), (3.5, 1.5, 1))
            crystal_crest('empowered_crystal')
            backpack('laser_relay', 'accent')
            crystal_marker('empowered_lens')
        elif ident == 'railcraft':
            cap('conductor_cap', True, 1.5)
            h('rail_badge', 'crest_tip', (0, -4.45, -2.45), (1.5, .5, .5))
            b('signal_satchel', 'collar', (1.3, .75, 1.9), (1, 2, 1))
            lantern_marker('signal_lamp')
        elif ident == 'industrialcraft':
            cap('rubber_hood', False, 1)
            h('circuit_crest', 'crest_tip', (0, -4.8, .3), (2, 1, 2))
            backpack('re_battery', 'collar', True)
            m('battery_case', 'accent', (0, 0, 0), (1.5, 2.5, 1.5))
            m('battery_terminal', 'orb', (0, -1.5, 0), (1, .5, 1))
        elif ident == 'buildcraft':
            cap('builders_hardhat', True, 1.5)
            h('safety_ridge', 'crest_tip', (0, -5.1, .3), (1, .5, 4))
            backpack('engine_pack', 'collar')
            m('transport_pipe', 'accent', (0, 0, 0), (1.5, 2.5, 1.5))
            m('pipe_window', 'shine', (0, 0, -.9), (1, 1, .5))
        elif ident == 'ars_nouveau':
            h('mage_circlet', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            crystal_crest('source_gem')
            book_pack('glyph_book', 'collar')
            crystal_marker('source_spark')
        elif ident == 'occultism':
            h('chalk_circlet', 'shine', (0, -2.85, -3.4), (6, .5, .5))
            h('spirit_seal', 'accent', (0, -4.2, .7), (2.5, 1, 2.5), (0, 30, 0))
            b('chalk_sigil_case', 'collar', (0, .65, 2.2), (2.5, 2.5, 1))
            star_marker('bound_spirit', 5)
        elif ident == 'hexerei':
            h('witch_hat_brim', 'collar', (0, -3.95, .2), (8.5, .5, 6))
            h('witch_hat_base', 'tuft', (0, -4.9, .35), (4.5, 1.5, 3.5))
            h('witch_hat_tip', 'crest_tip', (.6, -6.2, .6), (2, 1.5, 2), (0, 0, -22.5))
            b('willow_satchel', 'accent', (1.2, .7, 2.0), (1.5, 2, 1))
            star_marker('herbal_wisp', 4)
        elif ident == 'natures_aura':
            leaves('golden_laurel', 3)
            h('aura_circlet', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            b('gold_leaf_clasp', 'crest_tip', (0, -.5, -1.85), (1.5, .5, .5))
            marker.append(extrude('golden_leaf', 'orb', [(0,-1.5),(-1,.1),(0,1.4),(1,.1)], .5))
        elif ident == 'roots':
            leaves('elder_sprout', 2)
            h('bark_circlet', 'collar', (0, -2.9, -3.4), (6, .5, .5))
            b('root_belt', 'accent', (0, 1.35, -1.8), (4, .5, .5))
            lantern_marker('grove_rune')
        elif ident == 'iron_spells':
            h('wizard_hood', 'collar', (0, -4.05, .5), (6, 1.5, 5))
            h('hood_peak', 'tuft', (0, -5.4, 1.05), (3.5, 1.5, 3), (-15, 0, 0))
            h('spell_gem', 'crest_tip', (0, -3.95, -2.3), (1, 1, .5))
            b('scroll_case', 'accent', (1.2, .6, 2.0), (1, 3, 1))
            tablet_marker('spell_scroll')
        elif ident == 'projecte':
            h('transmutation_diadem', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            h('matter_gem', 'crest_tip', (0, -4.3, .3), (2, 1.5, 2), (0, 0, 20))
            b('red_matter_cloak', 'collar', (0, .6, 2.0), (3.5, 2.5, .5))
            ring_marker('transmutation_ring')
        elif ident == 'psi':
            h('psimetal_headband', 'accent', (0, -2.85, -3.4), (6.5, .5, .5))
            for side in (-1, 1):
                h('psi_antenna_'+str(side), 'crest_tip', (side*3.1, -4.35, .9), (.5, 2, .5), (0,0,side*18))
            b('cad_battery', 'collar', (0, .7, 2.2), (2, 2.5, 1))
            ring_marker('psi_loop')
        elif ident == 'malum':
            horns('spiritwood_prong', 2, 2.3)
            h('soulstone_circlet', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            b('soulwood_clasp', 'crest_tip', (0, -.6, -1.9), (1, 1, .5))
            star_marker('soul_spirit', 6)
        elif ident == 'evilcraft':
            horns('dark_gem_spike', 2, 2.0)
            h('blood_band', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            backpack('blood_chest', 'collar')
            crystal_marker('dark_gem')
        elif ident == 'mystical_agriculture':
            leaves('inferium_sprout', 3)
            h('seed_pod', 'accent', (0, -4.3, -1.9), (1.5, 1.5, 1))
            b('seed_pouch', 'collar', (1.1, .8, 1.9), (1.5, 1.5, 1))
            crystal_marker('essence_seed')
        elif ident == 'hexcasting':
            h('amethyst_crown_base', 'collar', (0, -3.95, .3), (4, .5, 3))
            crystal_crest('charged_amethyst', True)
            book_pack('hex_notebook', 'accent')
            star_marker('hex_pattern', 6)
        elif ident == 'alexs_caves':
            for side in (-1, 1):
                h('uranium_crystal_'+str(side), 'crest_tip', (side*1.55, -4.65, .8), (1.5, 2, 1.5), (0,0,side*25))
            h('cave_lens', 'accent', (0, -3.0, -3.4), (2, .5, .5))
            backpack('hazmat_filter', 'collar')
            ring_marker('radiant_atom')
        elif ident == 'alexs_mobs':
            cap('naturalist_pith_helmet', True, 1.5)
            h('crow_feather', 'crest_tip', (2.25, -5.1, .4), (.5, 2.5, 1), (0,0,20))
            b('field_satchel', 'collar', (1.0, .8, 2.0), (1.5, 2, 1))
            tablet_marker('field_guide')
        elif ident == 'cobblemon':
            cap('trainer_cap', True, 1.5)
            h('trainer_badge', 'shine', (0, -4.45, -2.45), (1, 1, .5))
            backpack('trainer_pack', 'collar')
            m('capture_ball_upper', 'orb', (0, -.75, 0), (2, 1, 2))
            m('capture_ball_lower', 'shine', (0, .75, 0), (2, 1, 2))
            m('capture_ball_band', 'collar', (0, 0, 0), (2, .5, 2))
        elif ident == 'undergarden':
            h('fungus_stem', 'collar', (0, -4.25, .5), (1.5, 1, 1.5))
            h('fungus_cap', 'crest_tip', (0, -5.0, .5), (5, 1, 3.5))
            for side in (-1, 1):
                h('fungus_lamp_'+str(side), 'orb', (side*1.8, -4.45, -.3), (1, .5, 1))
            b('root_backplate', 'tuft', (0, .7, 2.1), (3, 2.5, .5))
            lantern_marker('glimmergourd')
        elif ident == 'deeper_darker':
            for side in (-1, 1):
                h('sculk_sensor_'+str(side), 'crest_tip', (side*2.5, -4.8, .7), (.5, 2.5, .5), (0,0,side*18))
                h('sensor_tip_'+str(side), 'orb', (side*2.95, -5.8, .7), (1, .5, 1))
            for y in (-.3, .4, 1.1):
                b('sculk_rib_'+str(y), 'accent', (0, y, -1.85), (3, .5, .5))
            m('soul_core', 'orb', (0, 0, 0), (1.5, 2, 1.5))
            for side in (-1, 1):
                m('soul_ear_'+str(side), 'crest_tip', (side*1.1, -.3, 0), (.5, 1.5, .5))
        elif ident == 'blue_skies':
            crystal_crest('moonstone', True)
            h('sky_diadem', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            b('lunar_clasp', 'crest_tip', (0, -.6, -1.85), (1, 1, .5))
            star_marker('sun_and_moon', 4)
        elif ident == 'betweenlands':
            h('fungal_brim', 'tuft', (0, -4.1, .3), (8, .5, 5.5))
            h('peat_cap', 'collar', (-.6, -4.85, .7), (4, 1, 3))
            h('shelf_fungus', 'crest_tip', (2.4, -4.55, -.5), (2, .5, 2))
            b('root_weave', 'accent', (0, .8, 2.0), (3, 2, .5))
            lantern_marker('swamp_talisman')
        elif ident == 'draconic_evolution':
            horns('wyvern_crown', 2.5, 2.6)
            h('draconic_core_mount', 'accent', (0, -4.4, .3), (1.5, 1.5, 1.5))
            backpack('energy_core', 'collar')
            ring_marker('draconic_core')
        elif ident == 'tinkers':
            goggles('smith_goggles')
            h('slime_crest', 'crest_tip', (0, -4.5, 1.1), (2, 1.5, 2))
            b('smeltery_apron', 'collar', (0, .7, -1.95), (2.5, 2, .5))
            body.append(plane('smith_badge', 'variant_detail', -.75, -.1, 1.5, 1.5, z=-2.22, uv_size=(16,16)))
            gear_marker('cobalt_ingot')
        elif ident == 'galacticraft':
            h('helmet_top', 'shine', (0, -4.15, .2), (6.5, 1, 5.5))
            for side in (-1, 1):
                h('helmet_side_'+str(side), 'accent', (side*3.6, -2.75, .3), (.5, 2, 4))
            h('radio_antenna', 'crest_tip', (2.6, -5.1, 1.2), (.5, 2, .5))
            backpack('oxygen_tank', 'accent', True)
            m('satellite_core', 'orb', (0, 0, 0), (1.5, 1.5, 1.5))
            for side in (-1, 1):
                m('solar_panel_'+str(side), 'crest_tip', (side*1.7, 0, 0), (1.5, 1, .5))
        elif ident == 'quark':
            h('stoneling_cap', 'collar', (0, -4.1, .4), (5.5, 1, 4.5))
            h('ore_crest', 'crest_tip', (.65, -5.05, .6), (2, 1, 2), (0,0,15))
            backpack('toretoise_shell', 'accent')
            crystal_marker('heart_of_diamond')
        elif ident == 'vampirism':
            for side in (-1, 1):
                h('high_collar_'+str(side), 'collar', (side*2.8, -2.1, 2.7), (1, 2, 1), (-12,0,side*12))
            h('ruby_circlet', 'accent', (0, -2.9, -3.4), (6, .5, .5))
            b('vampire_cape', 'collar', (0, .6, 2), (3.5, 2.5, .5))
            marker.append(extrude('ruby_drop', 'orb', [(0,-1.5),(-1,.1),(-.6,1.2),(.6,1.2),(1,.1)], .6))
        elif ident == 'born_in_chaos':
            h('pumpkin_hat_brim', 'collar', (0, -3.95, .3), (7.5, .5, 5))
            h('crooked_hat', 'tuft', (.25, -5.05, .5), (3.5, 2, 3), (0,0,-10))
            h('pumpkin_hat_band', 'accent', (0, -4.3, -1.15), (4, .5, .5), (0,0,-10))
            b('ragged_cloak', 'collar', (0, .65, 2), (3, 2.5, .5))
            m('pumpkin_lantern', 'orb', (0, 0, 0), (2, 2, 2))
            m('pumpkin_stalk', 'crest_tip', (0, -1.3, 0), (.5, .5, .5), (0,0,-20))
        else:
            raise AssertionError('Missing design branch for '+ident)

        if not head or not body or not marker:
            raise AssertionError('Incomplete themed Chao geometry: '+ident)
        if ident in HEAD_PROFILES:
            head = head_parts(api, ident)
        if ident in ('create', 'thaumcraft'):
            head.extend(append_goggles(api, ident))
        bone(prefix+'_head', 'head', [0, 0, 0], head)
        bone(prefix+'_body', 'body', [0, 0, 0], body)
        bone(prefix+'_marker', 'head', [0, -9.8, .75], marker)
