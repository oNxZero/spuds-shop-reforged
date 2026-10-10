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
    patches = [e for e in model['elements'] if e['name'].startswith('pixel_')]
    assert model['render_type'] == 'minecraft:cutout'
    assert len(patches) > 0
    area = 0
    for a in patches:
        assert 'up' in a['faces']
        area += (a['to'][0]-a['from'][0])*(a['to'][2]-a['from'][2])
        assert a['from'][1] == 0 and a['to'][1] == 1
        for b in patches:
            if a is b:
                continue
            overlap_x = min(a['to'][0], b['to'][0])-max(a['from'][0], b['from'][0])
            overlap_z = min(a['to'][2], b['to'][2])-max(a['from'][2], b['from'][2])
            assert overlap_x <= 0 or overlap_z <= 0, f'overlap in rug state {mask}'
    width = 14 + bool(mask & 2) + bool(mask & 8)
    depth = 14 + bool(mask & 1) + bool(mask & 4)
    assert area <= width*depth
    if mask == 0:
        assert area == 164, 'circular rug silhouette changed'
        assert not any(e['from'][0] == 1 and e['from'][2] == 1 for e in patches)
for color in COLORS:
    identifier = 'rug_shop' if color == 'red' else 'rug_shop_'+color
    state = read(GENERATED / f'blockstates/{identifier}.json')
    assert len(state['multipart']) == 20
    assert sum('facing' in part['when'] for part in state['multipart']) == 4

crate = read(ASSETS / 'models/block/traders_crate.json')
for element in crate['elements']:
    assert all(a < b for a, b in zip(element['from'], element['to'])), 'inverted crate face winding'
    if element['name'].endswith(('_foot_rail', '_rear_leg', '_brace', '_saddle')) or element['name'] == 'rear_crossbeam':
        for x, y, z in vertices(element):
            # Exact underside of the tilted closed tray floor, y=z-5.
            assert y <= z-5+1e-6, f'{element["name"]} penetrates the tray floor'
        if element['name'] == 'rear_crossbeam':
            assert element['faces']['east']['texture'] == '#planks'
            assert element['faces']['up']['texture'] == '#planks'
            assert 'rotation' not in element['faces']['up']
        elif element['name'].endswith('_foot_rail'):
            assert element['faces']['north']['texture'] == '#planks'
            assert element['faces']['up']['texture'] == '#planks'
        elif element['name'].endswith('_rear_leg'):
            assert element['faces']['up']['texture'] == '#planks'
# Check actual shared interior points at both ends of each bearer. Bounding
# boxes alone would accept the disconnected diagonal joints seen in-game.
def contains(element, point):
    x,y,z = point
    rotation = element.get('rotation', {})
    angle = -math.radians(rotation.get('angle', 0))
    ox,oy,oz = rotation.get('origin', [0,0,0])
    dy,dz = y-oy,z-oz
    y,z = oy+math.cos(angle)*dy-math.sin(angle)*dz, oz+math.sin(angle)*dy+math.cos(angle)*dz
    return all(a < v < b for a,v,b in zip(element['from'],[x,y,z],element['to']))
parts = {e['name']:e for e in crate['elements']}
for side,x in [('left',1.375),('right',14.625)]:
    for other,point in [('foot_rail',(x,1.05,6.5)),('rear_leg',(x,7.5,13.5))]:
        assert contains(parts[side+'_saddle'],point) and contains(parts[side+'_'+other],point), 'disconnected crate joint'
    assert parts[side+'_rear_leg']['from'][0] > parts[side+'_saddle']['from'][0], 'coplanar joint faces'

for wood in WOODS:
    identifier = 'crate_shop' if wood == 'oak' else 'crate_shop_'+wood
    model = read(ASSETS / f'models/block/{identifier}.json')
    assert model['textures']['planks'] == f'minecraft:block/{wood}_planks'
    for folder in ['blockstates', 'models/item']:
        assert (ASSETS / f'{folder}/{identifier}.json').exists()
    for folder in ['loot_table/blocks']:
        assert (DATA / f'spudaciousshops/{folder}/{identifier}.json').exists()
    recipe = read(DATA / f'spudaciousshops/recipe/{identifier + ("_from_planks" if wood == "oak" else "")}.json')
    assert recipe['result']['id'] == 'spudaciousshops:'+identifier
for stem, count in [('shelf_shop', 2), ('shelf_shop_bottom', 1), ('shelf_shop_top', 1)]:
    model = read(ASSETS / f'models/block/shelf/{stem}.json')
    tags = [e for e in model['elements'] if e['name'] == 'price_tag_board']
    assert len(tags) == count
    for tag in tags:
        assert tag['spuds_board_bounds'][1][0]-tag['spuds_board_bounds'][0][0] == 7
        assert tag['spuds_board_bounds'][1][1]-tag['spuds_board_bounds'][0][1] == 3
print('Passed: 16 rug layouts without overlaps/gaps; closed crate tray and beam axes; 11 crate variants; hanging tag sizes.')

# Hanging hooks keep their transparent metal atlas instead of becoming wooden strips.
for wood in WOODS:
    identifier = 'hook_shop' if wood == 'oak' else 'hook_shop_'+wood
    state = read(ASSETS / f'blockstates/{identifier}.json')
    assert len(state['multipart']) == 8
    item = read(ASSETS / f'models/item/{identifier}.json')
    links = [e for e in item['elements'] if e['name'] == 'chain_link']
    assert links and all(f['texture'] == '#chain' for e in links for f in e['faces'].values())
    assert item['textures']['chain'] == 'minecraft:block/chain'
    assert item['textures']['gold'] == 'minecraft:block/gold_block'
    assert item['textures']['planks'] == f'minecraft:block/{wood}_planks'
    assert read(DATA / f'spudaciousshops/recipe/{identifier}.json')['result']['id'] == 'spudaciousshops:'+identifier
    assert sum(e['name'].startswith('sign_trim_') for e in item['elements']) == 16
rug_sign = read(ASSETS / 'models/block/rug_shop/price_sign.json')
board = next(e for e in rug_sign['elements'] if e['name'] == 'rug_sign_board')
assert board['to'][2] < 8, 'rug price sign must face the front'
plaque = next(e for e in crate['elements'] if e['name'] == 'plaque')
assert plaque['spuds_board_bounds'][1][1]-plaque['spuds_board_bounds'][0][1] == 3.5, 'crate tag must use a compact single price row'
print('Passed: 11 hanging wood variants, metal hook UVs, bordered signs, front rug sign, compact crate tag.')

counter = read(ASSETS / 'models/block/shop_window.json')
plate = next(e for e in counter['elements'] if e['name'] == 'counter_plate')
assert counter['transform'] == {'scale':[.7,.65,1],'origin':[.5,0,.5]}
points = [(8+(x-8)*.7,y*.65,z) for x,y,z in vertices(plate)]
assert abs(max(y for x,y,z in points)-5.2) < 1e-6
assert min(y for x,y,z in points) >= 0
assert abs(max(x for x,y,z in points)-min(x for x,y,z in points)-9.8) < 1e-6
assert max(z for x,y,z in points)-min(z for x,y,z in points) < 8
assert sum(e['name'] == 'sign_bracket' for e in counter['elements']) == 1
assert not any(e['name'] == 'sign_foot' for e in counter['elements'])
for e in counter['elements']:
    assert min(y for x,y,z in vertices(e)) >= -1e-6, e['name']+' extends below the floor'
print('Passed: centered compact counter plate, slab-height rear, timber supports above floor, single sign bracket.')
