"""Authored head-morph outlines for verified 1.21.1 magic themes.

These are connected upper cranium profiles, not detached costume pieces.
All points run left to right from the shared cheek joins; the canonical loft
adds the unchanged readable Chao face and rounded rear volume.
"""

PROFILES = {
    'astral_sorcery': {
        'name': 'Crescent cranium',
        'crown': [(-4,-2.6),(-4.25,-4.25),(-3.05,-6.15),(-2.15,-4.25),(0,-3.6),(2.15,-4.25),(3.05,-6.15),(4.25,-4.25),(4,-2.6)],
        'sweep': 1.2, 'tip_y': -4.65,
        'description': 'The temples rise into a single broad crescent silhouette, with two inward sloping lunar tips and a low central star basin. The moon shape grows out of the head rather than sitting on it.',
    },
    'botania': {
        'name': 'Mana petal head',
        'crown': [(-4,-2.6),(-4.3,-4.25),(-3.15,-5.0),(-1.85,-4.35),(0,-5.8),(1.85,-4.35),(3.15,-5.0),(4.3,-4.25),(4,-2.6)],
        'sweep': .65, 'tip_y': -4.45,
        'description': 'A broad three-petal fan opens directly from the forehead and temples; the central petal is taller and the outer petals curl back with the skull. Inspired by mana flowers and the Gaia flower silhouette.',
    },
    'blood_magic': {
        'name': 'Ritual droplet head',
        'crown': [(-4,-2.6),(-3.6,-3.75),(-2.0,-4.1),(-1.35,-5.2),(0,-6.35),(1.35,-5.2),(2.0,-4.1),(3.6,-3.75),(4,-2.6)],
        'sweep': 1.0, 'tip_y': -4.55,
        'description': 'The whole upper head tapers into a plump ruby droplet, with broad low slate-like shoulders framing it. This carries the Blood Orb and ritual-slate language without a worn crown.',
    },
    'ars_nouveau': {
        'name': 'Starbuncle tuft head',
        'crown': [(-4,-2.6),(-4.3,-4.05),(-3.05,-6.3),(-1.7,-4.15),(-.7,-4.3),(0,-5.15),(.7,-4.3),(1.7,-4.15),(3.05,-6.3),(4.3,-4.05),(4,-2.6)],
        'sweep': 1.45, 'tip_y': -4.65,
        'description': 'Two broad Starbuncle-like ear lobes rise from the temples, paired with a small central source-flame tuft. The ears share the same fleshy roots as the head, recalling Chao evolution spikes.',
    },
    'occultism': {
        'name': 'Otherworld wisp head',
        'crown': [(-4,-2.6),(-3.5,-3.8),(-2.1,-4.4),(-1.4,-5.45),(.9,-6.3),(.45,-4.8),(2.5,-4.0),(4,-2.6)],
        'sweep': 1.75, 'tip_y': -4.4,
        'description': 'A soft asymmetric spirit flame grows from the skull and leans to one side, with an elongated taper and broad pale roots. The contour suggests an Otherworld spirit rather than horns or a summoning hat.',
    },
    'hexerei': {
        'name': 'Willow curl head',
        'crown': [(-4,-2.6),(-3.65,-3.8),(-2.4,-4.45),(-.6,-5.0),(1.25,-5.65),(2.75,-5.35),(3.75,-4.15),(3.45,-3.55),(4,-2.6)],
        'sweep': 1.65, 'tip_y': -4.5,
        'description': 'The upper cranium draws into a broad, drooping willow curl with a soft off-center tip. The flowing woodland witch shape is part of the Chao head, with no brim or separate pointed hat.',
    },
    'natures_aura': {
        'name': 'Golden leaf head',
        'crown': [(-4,-2.6),(-3.7,-3.8),(-2.0,-4.45),(-1.1,-5.65),(.7,-6.05),(1.2,-4.4),(3.2,-4.1),(4,-2.6)],
        'sweep': .95, 'tip_y': -4.35,
        'description': 'A single broad leaf blade unfurls from the crown, with a longer offset golden tip and lower seed-leaf shoulder. This follows golden leaves and ambient aura rather than adding a laurel accessory.',
    },
    'roots': {
        'name': 'Wildroot sprout head',
        'crown': [(-4,-2.6),(-4.1,-3.7),(-2.9,-4.95),(-1.7,-4.2),(-.8,-5.35),(.5,-4.1),(2.4,-5.1),(3.35,-4.25),(4,-2.6)],
        'sweep': .55, 'tip_y': -4.4,
        'description': 'Three unequal leaf-like lobes split from one broad root crown. The low branching outline is a living sprout, with moss-to-bark tips and no detached sticks.',
    },
    'iron_spells': {
        'name': 'Arcane wave head',
        'crown': [(-4,-2.6),(-4.35,-3.9),(-3.0,-4.6),(-1.6,-3.95),(0,-5.95),(1.6,-3.95),(3.0,-4.6),(4.35,-3.9),(4,-2.6)],
        'sweep': 1.9, 'tip_y': -4.45,
        'description': 'A strong swept central spell-wave fin rises between two low curled temple lobes. The profile borrows the mod\'s arcane armor language while keeping an open, expressive Chao face.',
    },
    'projecte': {
        'name': 'Philosopher diamond head',
        'crown': [(-4,-2.6),(-3.2,-3.7),(-1.9,-4.35),(0,-6.1),(1.9,-4.35),(3.2,-3.7),(4,-2.6)],
        'sweep': .55, 'tip_y': -4.35,
        'description': 'A broad geometric diamond emerges as the head itself, with continuous dark-matter shoulders and one Philosopher\'s Stone red point. No ring or gem is balanced on top.',
    },
    'psi': {
        'name': 'Psionic split head',
        'crown': [(-4,-2.6),(-3.7,-3.7),(-2.55,-4.6),(-1.85,-5.6),(-.85,-5.6),(0,-4.15),(.85,-5.6),(1.85,-5.6),(2.55,-4.6),(3.7,-3.7),(4,-2.6)],
        'sweep': 1.35, 'tip_y': -4.7,
        'description': 'The crown divides into two clean, blunt psimetal lobes around a central notch, echoing a Psi glyph and programmed flow. Broad roots and cyan tips keep the effect grown rather than antenna-like.',
    },
    'malum': {
        'name': 'Soulwood flame head',
        'crown': [(-4,-2.6),(-4.0,-3.85),(-2.75,-5.6),(-1.6,-4.5),(-.65,-6.25),(.65,-4.75),(2.5,-5.15),(3.65,-3.85),(4,-2.6)],
        'sweep': 1.5, 'tip_y': -4.65,
        'description': 'Three tapering spirit-flame lobes share a broad soulwood crown, with the off-center middle flame tallest. The outline suggests flowing Spirit Arcana and branching soulwood rather than mounted soul rods.',
    },
    'evilcraft': {
        'name': 'Dark gem swept head',
        'crown': [(-4,-2.6),(-4.15,-4.05),(-3.1,-5.3),(-1.55,-4.4),(0,-4.85),(1.55,-4.4),(3.1,-5.3),(4.15,-4.05),(4,-2.6)],
        'sweep': 1.95, 'tip_y': -4.55,
        'description': 'Two low bat-wing-like temple sweeps and a shallow dark-gem brow reshape the skull. Crimson swept tips recall EvilCraft\'s dark gem and vengeance spirits without bottles or horn props.',
    },
    'mystical_agriculture': {
        'name': 'Essence seedling head',
        'crown': [(-4,-2.6),(-4.25,-3.85),(-3.1,-5.25),(-1.25,-4.55),(0,-4.05),(1.25,-4.55),(3.1,-5.25),(4.25,-3.85),(4,-2.6)],
        'sweep': .4, 'tip_y': -4.35,
        'description': 'The entire upper head opens into two broad seed leaves around a small central bud. Wide rounded roots and light essence tips make it a living Inferium seedling, not a potted plant accessory.',
    },
    'hexcasting': {
        'name': 'Amethyst chevron head',
        'crown': [(-4,-2.6),(-3.8,-3.9),(-2.8,-4.75),(-1.4,-4.35),(0,-6.35),(1.4,-4.35),(2.8,-4.75),(3.8,-3.9),(4,-2.6)],
        'sweep': 1.15, 'tip_y': -4.55,
        'description': 'A strong six-sided amethyst peak grows between two smaller low facets, turning the Chao tuft into a connected chevron silhouette. The facets echo hex-grid spell patterns and amethyst without sticks or loose crystals.',
    },
}
