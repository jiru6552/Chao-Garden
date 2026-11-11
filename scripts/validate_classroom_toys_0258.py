#!/usr/bin/env python3
"""Check actual play-city cuboids, native bounds and authored room clearance."""
from copy import deepcopy
from itertools import combinations
import json
from pathlib import Path
import re
from classroom_toy_geometry_0258 import IDS

ROOT = Path(__file__).resolve().parents[1]
ASSET = Path('src/main/resources/assets/chao_garden/models/block')
checks = 0


def require(ok, message):
    global checks
    checks += 1
    if not ok:
        raise AssertionError(message)


def overlap(a, b):
    return all(min(a[i + 3], b[i + 3]) - max(a[i], b[i]) > 1e-8 for i in range(3))


def touches(a, b):
    spans = [min(a[i + 3], b[i + 3]) - max(a[i], b[i]) for i in range(3)]
    return sum(v > 1e-8 for v in spans) == 2 and all(v >= -1e-8 for v in spans)


def bounds(model):
    return [min(e['from'][i] for e in model['elements']) for i in range(3)] + [
        max(e['to'][i] for e in model['elements']) for i in range(3)]


def geometry(models, declared):
    require(set(models) == set(IDS) == set(declared), 'Play-city model/native catalog differs')
    for name, model in models.items():
        require(model.get('chao_classroom_toy_profile') == '0258', 'Old intersecting play-city profile: ' + name)
        parts = []
        for element in model['elements']:
            require('rotation' not in element, 'Toy collision contract needs rotated element review: ' + name)
            a, b = element['from'], element['to']
            require(all(b[i] > a[i] for i in range(3)), 'Empty/inverted structural toy cuboid: ' + name)
            require(0 <= a[0] < b[0] <= 16 and 0 <= a[2] < b[2] <= 16 and 0 <= a[1] < b[1] <= 32,
                    'Roof/canopy escapes its owning footprint or legal height: ' + name)
            require(set(element['faces']) == {'north', 'south', 'east', 'west', 'up', 'down'},
                    'Open structural play-city cuboid: ' + name)
            parts.append(a + b)
        for a, b in combinations(parts, 2):
            require(not overlap(a, b), 'Structural play-city cuboids interpenetrate: ' + name)
        reached = {0}
        while True:
            attached = {i for i, p in enumerate(parts) if any(touches(p, parts[j]) for j in reached)}
            if attached <= reached:
                break
            reached |= attached
        require(len(reached) == len(parts), 'Floating disconnected play-city part: ' + name)
        require(all(abs(a - b) < 1e-7 for a, b in zip(bounds(model), declared[name])),
                'Native toy bounds differ from its actual roof/canopy: ' + name)


def placements(method, models):
    found = [(int(x), int(y), int(z), name) for x, y, z, name in re.findall(
        r'p\.decor\((-?\d+),(-?\d+),(-?\d+),"(school_toy_[a-z_]+)"\)', method)]
    expected = {'school_toy_house_red': 1, 'school_toy_house_blue': 1,
                'school_toy_house_yellow': 1, 'school_toy_tower': 2, 'school_toy_tree': 2}
    require({name: sum(p[3] == name for p in found) for name in set(p[3] for p in found)} == expected,
            'Authored play city omits pieces or duplicates decorative play blocks')
    boxes = []
    for x, y, z, name in found:
        local = bounds(models[name])
        world = [v / 16 + (x, y, z)[i % 3] for i, v in enumerate(local)]
        require(-24 < world[0] and world[3] < -13 and -21 <= world[2] and world[5] <= -15.5,
                'Play-city piece clips a room wall or enters reserved class/play floor: ' + name)
        require(world[1] == 80 and world[4] < 85, 'Play city floats or enters the classroom ceiling')
        boxes.append(world)
    for a, b in combinations(boxes, 2):
        # At least half a model pixel remains between independent painted props.
        pad = lambda c: [v - .015625 if i < 3 else v + .015625 for i, v in enumerate(c)]
        require(not overlap(pad(a), pad(b)), 'Independent play-city objects clip/touch')
    teacher = [-18.25, 80, -17, -16.75, 81.25, -16]
    require(all(not overlap(b, teacher) for b in boxes), 'Play city intersects the real lesson desk')
    require(not re.search(r'Blocks\.(?:BLUE_CONCRETE|WHITE_CONCRETE|BLACK_CARPET)', method),
            'Intersecting vanilla concrete placeholder remains in the play city')
    require('school_toy_ball' not in method, 'Decorative ball duplicates the actual native pupil toy')


def validate(root=ROOT):
    models = {name: json.loads((root / ASSET / (name + '.json')).read_text()) for name in IDS}
    source = (root / 'src/main/java/com/chaogarden/core/ClassroomToyGeometry0258.java').read_text()
    declared = {}
    for names, values in re.findall(r'case ([^\n]+)->new double\[\]\{([^}]+)\}', source):
        for name in re.findall(r'"([a-z_]+)"', names):
            declared[name] = [float(n) for n in values.split(',')]
    geometry(models, declared)
    rooms = (root / 'src/main/java/com/chaogarden/kindergarten/KindergartenRooms025.java').read_text()
    method = rooms[rooms.index('  private static void classroom(Plan p){'):rooms.index('  private static void clinic(Plan p){')]
    placements(method, models)
    native = (root / 'src/main/java/com/chaogarden/kindergarten/KindergartenDecor025.java').read_text()
    require('case TOY->toyShape(state)' in native and 'ClassroomToyGeometry0258.bounds(id)' in native,
            'Native toy collision still uses the old generic one-block stub')
    return models, declared, method


def negative_fixtures(models, declared, method):
    rejected = 0

    def reject(action, label):
        nonlocal rejected
        try:
            action()
        except AssertionError:
            rejected += 1
            return
        raise AssertionError('Accepted malformed classroom regression: ' + label)

    bad = deepcopy(models);bad['school_toy_tree']['elements'][2]['to'][0] = 18
    reject(lambda: geometry(bad, declared), 'oversized canopy')
    bad = deepcopy(models);bad['school_toy_house_red']['elements'][0]['to'][2] = 14
    reject(lambda: geometry(bad, declared), 'front inlays embedded in shell')
    bad = deepcopy(models);bad['school_toy_tower']['elements'][-1]['from'][1] += .5
    reject(lambda: geometry(bad, declared), 'floating tower cap')
    bad = deepcopy(declared);bad['school_toy_tree'][4] = 16
    reject(lambda: geometry(models, bad), 'old collision misses canopy')
    reject(lambda: placements(method.replace('p.decor(-22,80,-19,"school_toy_tower")',
                                            'p.decor(-23,80,-21,"school_toy_tower")'), models),
           'tower phases through house')
    reject(lambda: placements(method.replace('p.decor(-14,80,-17,"school_toy_tree")',
                                            'p.decor(-14,80,-14,"school_toy_tree")'), models),
           'tree invades lesson/play area')
    reject(lambda: placements(method + 'p.box(-15,80,-20,-14,80,-18,Blocks.BLUE_CONCRETE);', models),
           'concrete placeholder clips the play city')
    return rejected


if __name__ == '__main__':
    models, declared, method = validate()
    negative = negative_fixtures(models, declared, method)
    print(f'Classroom play city0258: {checks:,} geometric/native bindings passed; {negative} malformed regressions rejected.')
