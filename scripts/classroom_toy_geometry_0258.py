"""Compact painted play-city pieces with disjoint native cuboids.

Every x/z footprint stays inside its owner voxel. Structural parts meet at
shared faces rather than being buried inside a neighbouring building or roof.
"""
from math import sqrt

IDS = (
    'school_toy_house_red', 'school_toy_house_blue',
    'school_toy_house_yellow', 'school_toy_tower',
    'school_toy_tree', 'school_toy_ball',
)
COLORS = ('red', 'blue', 'yellow', 'green', 'gold', 'white', 'cream', 'wood')


def toy_model(identifier):
    if identifier not in IDS:
        raise ValueError('Unknown classroom toy: ' + identifier)
    elements = []

    def box(lo, hi, color):
        elements.append({'from': lo, 'to': hi, 'faces': {
            face: {'texture': '#' + color, 'uv': [0, 0, 16, 16]}
            for face in ('north', 'south', 'east', 'west', 'up', 'down')
        }})

    if 'house' in identifier:
        wall = identifier.removeprefix('school_toy_house_')
        door = 'green' if wall == 'blue' else 'blue'
        roof = 'green' if wall == 'red' else 'red'
        # Solid rear shell; the front is partitioned around coloured inlays.
        # The door/window aren't buried in another full front-wall cuboid.
        box([2, 0, 2], [14, 15, 13.5], wall)
        box([2, 0, 13.5], [5, 15, 14], wall)
        box([5, 0, 13.5], [8.5, 9, 14], door)
        box([5, 9, 13.5], [8.5, 15, 14], wall)
        box([8.5, 0, 13.5], [14, 6, 14], wall)
        box([8.5, 11, 13.5], [14, 15, 14], wall)
        box([8.5, 6, 13.5], [10, 11, 14], wall)
        box([13, 6, 13.5], [14, 11, 14], wall)
        box([10, 6, 13.5], [13, 11, 14], 'white')
        box([11.35, 6, 14], [11.65, 11, 14.12], 'cream')
        box([7.4, 4, 14], [8, 4.7, 14.3], 'gold')
        for low, high, inset in ((15, 18, 0), (18, 21, 2),
                                 (21, 24, 4), (24, 26, 6), (26, 28, 7)):
            box([inset, low, .5], [16 - inset, high, 15.5], roof)
    elif identifier == 'school_toy_tower':
        box([3, 0, 3], [13, 17, 12.5], 'yellow')
        box([3, 0, 12.5], [6, 17, 13], 'yellow')
        box([10, 0, 12.5], [13, 17, 13], 'yellow')
        box([6, 0, 12.5], [10, 10, 13], 'blue')
        box([6, 10, 12.5], [10, 17, 13], 'yellow')
        for low, high, inset in ((17, 20, 1), (20, 23, 3),
                                 (23, 26, 5), (26, 29, 6.5), (29, 30, 7.5)):
            box([inset, low, inset], [16 - inset, high, 16 - inset], 'red')
    elif identifier == 'school_toy_tree':
        box([6.5, 0, 6.5], [9.5, 14, 9.5], 'wood')
        for low, high, inset in ((14, 18, 3), (18, 24, 1),
                                 (24, 28, 3), (28, 30, 5.5)):
            box([inset, low, inset], [16 - inset, high, 16 - inset], 'green')
    else:
        for low, high, color in ((-1, -.65, 'blue'), (-.65, -.3, 'yellow'),
                                  (-.3, .3, 'red'), (.3, .65, 'yellow'),
                                  (.65, 1, 'blue')):
            width = 7 * sqrt(max(.1, 1 - ((low + high) / 2) ** 2))
            box([8 - width, 7 + 7 * low, 8 - width],
                [8 + width, 7 + 7 * high, 8 + width], color)
    return {'textures': {**{c: 'chao_garden:block/school025/' + c for c in COLORS},
                         'base': 'chao_garden:block/school025/red',
                         'particle': 'chao_garden:block/school025/red'},
            'elements': elements, 'ambientocclusion': False,
            'chao_classroom_toy_profile': '0258'}
