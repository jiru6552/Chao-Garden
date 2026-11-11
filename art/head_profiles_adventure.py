"""Authored connected Chao crown profiles for confirmed 1.21.1 adventure mods.

Coordinates are the left-to-right upper head outline in local Chao head space.
The shared loft joins these crowns to the face and rounded rear of the skull.
These are skin evolutions, not hats, rods, animal gifts or imported mod meshes.
Availability evidence and design interpretation: docs/research/HEAD_REFINEMENT_ADVENTURE.md.
"""

PROFILES = {
    'cataclysm': {
        'name': 'Ignis furnace crown',
        'crown': [(-4, -2.6), (-3.7, -4.05), (-2.85, -6.15),
                  (-1.55, -4.8), (0, -4.25), (1.55, -4.8),
                  (2.85, -6.15), (3.7, -4.05), (4, -2.6)],
        'sweep': 1.5, 'tip_y': -4.9,
        'description': 'Broad basalt temple roots flow into the paired flared Ignis ridges; a low central saddle keeps the Chao face soft.',
    },
    'ice_fire': {
        'name': 'Swept dragon crown',
        'crown': [(-4, -2.6), (-3.85, -4.2), (-3.1, -6.4),
                  (-2.05, -4.65), (-.7, -4.45), (0, -5.25),
                  (.7, -4.45), (2.05, -4.65), (3.1, -6.4),
                  (3.85, -4.2), (4, -2.6)],
        'sweep': 1.9, 'tip_y': -5.25,
        'description': 'Joined dragon temples taper into rear swept horns around a short central dorsal crown. No long snout or separate circlet.',
    },
    'aether': {
        'name': 'Cloudwing crown',
        'crown': [(-4, -2.6), (-3.75, -4.2), (-2.75, -5.35),
                  (-1.5, -5.0), (0, -5.8), (1.5, -5.0),
                  (2.75, -5.35), (3.75, -4.2), (4, -2.6)],
        'sweep': 1.15, 'tip_y': -5.1,
        'description': 'A pillowy cloud crown rises in three broad lobes and sweeps toward featherlike sky tips, all continuous with the upper head.',
    },
    'twilight_forest': {
        'name': 'Twilight antlerbud crown',
        'crown': [(-4, -2.6), (-3.65, -4.0), (-3.15, -6.3),
                  (-2.25, -5.15), (-1.35, -5.6), (0, -4.25),
                  (1.35, -5.6), (2.25, -5.15), (3.15, -6.3),
                  (3.65, -4.0), (4, -2.6)],
        'sweep': .9, 'tip_y': -5.15,
        'description': 'Twin broad leaf roots each fork into a compact forest stag bud; the shared dusk crown prevents a pair of detached antler sticks.',
    },
    'alexs_caves': {
        'name': 'Toxic cave fan crown',
        'crown': [(-4, -2.6), (-3.55, -4.1), (-2.7, -5.55),
                  (-1.65, -4.65), (-.55, -6.25), (.65, -4.95),
                  (1.95, -5.8), (3.15, -4.6), (4, -2.6)],
        'sweep': 1.65, 'tip_y': -5.15,
        'description': 'One asymmetric rear fan grows three mineral lobes from a fleshy charcoal green crown, with restrained luminous terminal color.',
    },
    'alexs_mobs': {
        'name': 'Crow feather crown',
        'crown': [(-4, -2.6), (-3.55, -3.9), (-2.4, -4.75),
                  (-1.25, -4.45), (-.25, -5.7), (.65, -5.0),
                  (1.8, -6.15), (2.75, -4.7), (3.5, -3.9), (4, -2.6)],
        'sweep': 1.95, 'tip_y': -4.8,
        'description': 'A low naturalist colored forehead flows into a swept three feather crow crest. This is an authored theme evolution, not animal gift attachments.',
    },
    'cobblemon': {
        'name': 'Partner creature crown',
        'crown': [(-4, -2.6), (-3.75, -4.35), (-2.75, -6.25),
                  (-1.65, -4.45), (0, -4.95), (1.65, -4.45),
                  (2.75, -6.25), (3.75, -4.35), (4, -2.6)],
        'sweep': .8, 'tip_y': -5.1,
        'description': 'Soft twin companion ear lobes and a small central brow tuft evolve directly from the skull. The separate capture ball marker supplies explicit mod identity.',
    },
    'undergarden': {
        'name': 'Gloom fungus crown',
        'crown': [(-4, -2.6), (-3.95, -4.25), (-3.0, -5.15),
                  (-1.7, -5.65), (0, -5.9), (1.7, -5.65),
                  (3.0, -5.15), (3.95, -4.25), (4, -2.6)],
        'sweep': .55, 'tip_y': -5.35,
        'description': 'The whole upper skull becomes a broad low mushroom dome, with no stem, brim, or separate hat. A narrow luminous crown transition recalls underground fungus.',
    },
    'deeper_darker': {
        'name': 'Sculk listener crown',
        'crown': [(-4, -2.6), (-3.85, -4.35), (-3.1, -6.05),
                  (-2.0, -5.2), (-1.4, -4.2), (0, -4.05),
                  (1.4, -4.2), (2.0, -5.2), (3.1, -6.05),
                  (3.85, -4.35), (4, -2.6)],
        'sweep': 1.1, 'tip_y': -5.2,
        'description': 'Thick paired listening fins flow from the upper sides and curl upward at their ends. Their broad sculk roots replace the wire antenna silhouette.',
    },
    'quark': {
        'name': 'Stoneling crown',
        'crown': [(-4, -2.6), (-3.7, -4.0), (-2.4, -4.65),
                  (-.9, -4.75), (.4, -5.6), (1.6, -5.15),
                  (2.9, -4.45), (3.7, -3.9), (4, -2.6)],
        'sweep': .7, 'tip_y': -5.05,
        'description': 'A continuous faceted stoneling scalp carries a modest asymmetric rear stone lobe, replacing the slab cap and detached ore block.',
    },
    'vampirism': {
        'name': 'Vampire bat crown',
        'crown': [(-4, -2.6), (-3.7, -4.35), (-2.8, -6.45),
                  (-1.8, -4.6), (0, -3.85), (1.8, -4.6),
                  (2.8, -6.45), (3.7, -4.35), (4, -2.6)],
        'sweep': 1.4, 'tip_y': -5.2,
        'description': 'Two swept bat ear lobes grow from a widow peak crown with a low central saddle; the vampire face remains open, with the body cape subordinate.',
    },
    'born_in_chaos': {
        'name': 'Crooked pumpkin crown',
        'crown': [(-4, -2.6), (-3.65, -4.35), (-2.6, -5.1),
                  (-1.5, -4.9), (-.9, -5.5), (-.25, -6.35),
                  (.55, -5.25), (1.65, -4.95), (2.8, -4.85),
                  (3.7, -4.0), (4, -2.6)],
        'sweep': .8, 'tip_y': -5.4,
        'description': 'The head itself becomes a broad three lobed pumpkin contour with a short crooked rear stalk, replacing all witch hat and band geometry.',
    },
}
