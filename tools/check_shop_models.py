#!/usr/bin/env python3
"""Check geometry, connected-rug coverage, crate penetration and material axes."""
import itertools
import json
import math
from pathlib import Path
from generate_shop_models import ROOT, ASSETS, GENERATED, DATA, WOODS, COLORS


def read(path):
    return json.loads(path.read_text())


def vertices(element):
    rotation = element.get('rotation', {})
    angle = math.radians(rotation.get('angle', 0))
    origin = rotation.get('origin', [0, 0, 0])
    assert not angle or rotation['axis'] == 'x'
    for v in itertools.product(*zip(element['from'], element['to'])):
        x, y, z = v
        if angle:
            dy, dz = y-origin[1], z-origin[2]
            y = origin[1] + math.cos(angle)*dy - math.sin(angle)*dz
            z = origin[2] + math.sin(angle)*dy + math.cos(angle)*dz
        yield x, y, z


for mask in range(16):
    model = read(ASSETS / f'models/block/rug_shop/connected_{mask}.json')
    patches = model['elements']
    assert len(patches) == 9
    area = 0
    for a in patches:
        assert list(a['faces']) == ['up'], 'one visible face per patch'
        area += (a['to'][0]-a['from'][0])*(a['to'][2]-a['from'][2])
        assert a['from'][1] == a['to'][1] == .01
        for b in patches:
            if a is b:
                continue
            overlap_x = min(a['to'][0], b['to'][0])-max(a['from'][0], b['from'][0])
            overlap_z = min(a['to'][2], b['to'][2])-max(a['from'][2], b['from'][2])
            assert overlap_x <= 0 or overlap_z <= 0, f'overlap in rug state {mask}'
    width = 14 + bool(mask & 2) + bool(mask & 8)
    depth = 14 + bool(mask & 1) + bool(mask & 4)
    assert area == width*depth, f'gap in rug state {mask}'
for color in COLORS:
    identifier = 'rug_shop' if color == 'red' else 'rug_shop_'+color
    state = read(GENERATED / f'blockstates/{identifier}.json')
    assert len(state['variants']) == 16 and 'multipart' not in state

crate = read(ASSETS / 'models/block/traders_crate.json')
for element in crate['elements']:
    assert all(a < b for a, b in zip(element['from'], element['to'])), 'inverted crate face winding'
    if element['name'].endswith(('_foot_rail', '_rear_leg', '_brace', '_saddle')) or element['name'] == 'rear_crossbeam':
        for x, y, z in vertices(element):
            # Exact underside of the tilted closed tray floor, y=z-5.
            assert y <= z-5+1e-6, f'{element["name"]} penetrates the tray floor'
        if element['name'] == 'rear_crossbeam':
            assert element['faces']['east']['texture'] == '#log_end'
            assert element['faces']['up']['texture'] == '#log'
            assert element['faces']['up']['rotation'] == 90
        elif element['name'].endswith('_foot_rail'):
            assert element['faces']['north']['texture'] == '#log_end'
            assert element['faces']['up']['texture'] == '#log'
        elif element['name'].endswith('_rear_leg'):
            assert element['faces']['up']['texture'] == '#log_end'
for wood in WOODS:
    identifier = 'crate_shop_'+wood
    model = read(ASSETS / f'models/block/{identifier}.json')
    assert model['textures']['planks'] == f'minecraft:block/{wood}_planks'
    for folder in ['blockstates', 'models/item']:
        assert (ASSETS / f'{folder}/{identifier}.json').exists()
    for folder in ['recipe', 'loot_table/blocks']:
        assert (DATA / f'spudaciousshops/{folder}/{identifier}.json').exists()
    recipe = read(DATA / f'spudaciousshops/recipe/{identifier}.json')
    assert recipe['result']['id'] == 'spudaciousshops:'+identifier
for stem, count in [('shelf_shop', 2), ('shelf_shop_bottom', 1), ('shelf_shop_top', 1)]:
    model = read(ASSETS / f'models/block/shelf/{stem}.json')
    tags = [e for e in model['elements'] if e['name'] == 'price_tag_board']
    assert len(tags) == count
    for tag in tags:
        assert tag['to'][0]-tag['from'][0] == 8
        assert tag['to'][1]-tag['from'][1] == 4
print('Passed: 16 rug layouts without overlaps/gaps; closed crate tray and beam axes; 11 crate variants; hanging tag sizes.')
