"""Original cuboid recreations of the two mods' recognizable goggles.

Reference evidence: docs/research/GOGGLE_STUDY_09.md. No source textures or
model elements are imported. The two models share forehead placement but use
different lens/rim construction, bridges and strap hardware.
"""

GOGGLE_PALETTES = {
    'create': {
        'goggle_frame': (169, 115, 56),
        'goggle_lens': (52, 84, 88),
        'goggle_strap': (70, 41, 25),
        'goggle_highlight': (239, 205, 116),
    },
    'thaumcraft': {
        'goggle_frame': (183, 130, 33),
        'goggle_lens': (47, 13, 91),
        'goggle_strap': (55, 32, 28),
        'goggle_highlight': (240, 190, 72),
    },
}


def append_goggles(api, ident):
    if ident not in GOGGLE_PALETTES:
        return []
    cube = api['cuboid']
    parts = []
    cy, front, glass = -2.5, -3.65, -3.55

    def add(name, material, centre, size):
        parts.append(cube('goggles_' + ident + '_' + name,
                          'goggle_' + material, centre, size))

    if ident == 'create':
        # The official wearable partial model has seven brass frame blocks:
        # two outer jambs, paired upper/lower rims and one broad centre bridge.
        # The bridge itself forms the two inner jambs.
        for side, label in ((-1, 'left'), (1, 'right')):
            cx = side * 1.75
            add(label + '_upper_rim', 'highlight', (cx, cy-.75, front), (1.5, .5, .5))
            add(label + '_lower_rim', 'frame', (cx, cy+.75, front), (1.5, .5, .5))
            add(label + '_outer_rim', 'frame', (side*2.75, cy, front), (.5, 1, .5))
            add(label + '_lens', 'lens', (cx, cy, glass), (1.5, 1, .5))
        add('broad_brass_bridge', 'frame', (0, cy, front), (2, 1, .5))
        join_centre, join_width = 3.5, 1.5
    else:
        # Thaumcraft's gold, near-square rings surround purple panes. Each
        # ring has its own inner jamb, with a smaller gold bridge between them.
        for side, label in ((-1, 'left'), (1, 'right')):
            cx = side * 1.5
            add(label + '_upper_rim', 'highlight', (cx, cy-.75, front), (1, .5, .5))
            add(label + '_lower_rim', 'frame', (cx, cy+.75, front), (1, .5, .5))
            for direction, edge in ((-1, 'left'), (1, 'right')):
                add(label + '_' + edge + '_rim', 'frame',
                    (cx+direction*.75, cy, front), (.5, 1, .5))
            add(label + '_lens', 'lens', (cx, cy, glass), (1, 1, .5))
        add('small_gold_bridge', 'frame', (0, cy, front), (1, .5, .5))
        join_centre, join_width = 3.25, 2

    # The front joins touch both the frames and the upper temples. The strap
    # wraps around the head below the crown, with no band stretched over it.
    for side, label in ((-1, 'left'), (1, 'right')):
        add(label + '_temple_join', 'strap',
            (side*join_centre, cy, -3.35), (join_width, .5, .5))
        add(label + '_side_strap', 'strap', (side*4, cy, 0), (.5, .5, 6.5))
    add('rear_strap', 'strap', (0, cy, 3.25), (8.5, .5, .5))
    if ident == 'thaumcraft':
        # A gold adjustment buckle is visible on the original armor atlas.
        add('gold_strap_buckle', 'frame', (4.3, cy, 1.5), (.5, 1, 1))
    return parts
