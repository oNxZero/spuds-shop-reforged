#!/usr/bin/env python3
"""Generate resource-pack-aware wood models and non-overlapping connected rugs.

Run from any directory with Python 3. No image processing or dependencies needed.
Existing registry IDs and inventories are preserved.
"""
from pathlib import Path
import json
import itertools

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/spudaciousshops'
GENERATED = ROOT / 'src/generated/resources/assets/spudaciousshops'
DATA = ROOT / 'src/main/resources/data'
WOODS = ['acacia', 'bamboo', 'birch', 'cherry', 'crimson', 'dark_oak',
         'mangrove', 'oak', 'spruce', 'warped', 'jungle']
COLORS = ['red', 'white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime',
          'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'black']
FACES = ['north', 'east', 'south', 'west', 'up', 'down']


def read(path):
    return json.loads(path.read_text())


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def wood_textures(wood):
    if wood == 'bamboo':
        log, end = 'stripped_bamboo_block', 'stripped_bamboo_block_top'
    elif wood in ('crimson', 'warped'):
        log, end = f'stripped_{wood}_stem', f'stripped_{wood}_stem_top'
    else:
        log, end = f'stripped_{wood}_log', f'stripped_{wood}_log_top'
    return {'planks': f'minecraft:block/{wood}_planks',
            'log': f'minecraft:block/{log}', 'log_end': f'minecraft:block/{end}',
            'particle': f'minecraft:block/{wood}_planks'}


def cube(name, lo, hi, texture='planks', axis=None, rotation=None):
    faces = {}
    for face in FACES:
        end = axis and face in {'x': ['east', 'west'], 'y': ['up', 'down'],
                               'z': ['north', 'south']}[axis]
        info = {'uv': [0, 0, 16, 16], 'texture': '#log_end' if end else '#' + texture}
        # Log side grain follows the actual beam axis, including its top/bottom faces.
        if axis == 'x' and face in ['north', 'south', 'up', 'down']:
            info['rotation'] = 90
        if axis == 'z' and face in ['east', 'west']:
            info['rotation'] = 90
        faces[face] = info
    element = {'name': name, 'from': lo, 'to': hi, 'faces': faces}
    if rotation:
        element['rotation'] = rotation
    return element


def retexture(element, key):
    for info in element['faces'].values():
        info['texture'] = '#' + key
        info['uv'] = [0, 0, 16, 16]
        info.pop('rotation', None)


def shelves():
    for stem, offsets in [('shelf_shop_bottom', [0]), ('shelf_shop_top', [7]),
                          ('shelf_shop', [0, 7])]:
        path = ASSETS / f'models/block/shelf/{stem}.json'
        model = read(path)
        # Preserve the original shelf geometry; regenerate only our front tags.
        elements = [e for e in model['elements'] if not e.get('name', '').startswith('price_tag')]
        for element in elements:
            retexture(element, 'planks')
        for offset in offsets:
            # Same eight-by-four model-unit board as the normal shop plaque.
            elements.append(cube('price_tag_board', [4, offset, 8.35], [12, offset+4, 9.1]))
            elements.append(cube('price_tag_face', [4.25, offset+.25, 8.25],
                                 [11.75, offset+3.75, 8.35]))
            # Short metal hangers connect the sign to the shelf's front rail.
            for x in [4.75, 10.75]:
                elements.append(cube('price_tag_hanger', [x, offset+2.5, 8.625],
                                     [x+.5, offset+4.5, 9.25], 'metal'))
        model['elements'] = elements
        model['textures'] = {**wood_textures('oak'), 'metal': 'minecraft:block/iron_block'}
        model.pop('groups', None)
        model.pop('texture_size', None)
        write(path, model)
    for wood in WOODS:
        for suffix in ['', '_bottom', '_top']:
            write(GENERATED / f'models/block/shelf_shop_{wood}{suffix}.json', {
                'parent': f'spudaciousshops:block/shelf/shelf_shop{suffix}',
                'textures': wood_textures(wood)})


def other_wood_models():
    path = ASSETS / 'models/block/shop.json'
    model = read(path)
    for element in model['elements']:
        if element.get('name') != 'glass':
            retexture(element, 'planks')
    model['textures'].update(wood_textures('oak'))
    model.pop('texture_size', None)
    write(path, model)
    for wood in WOODS:
        path = ASSETS / f'models/block/shop_{wood}.json'
        model = read(path)
        model['textures'].update(wood_textures(wood))
        write(path, model)
    # Hook and windowsill price boards are also ordinary pack-provided wood.
    path = ASSETS / 'models/block/hook_shop_plaque.json'
    model = read(path)
    for element in model['elements']:
        retexture(element, 'planks')
    model['textures'].update(wood_textures('oak'))
    write(path, model)
    path = ASSETS / 'models/block/shop_window.json'
    model = read(path)
    for element in model['elements']:
        if element.get('name') == 'plaque':
            retexture(element, 'planks')
    model['textures']['planks'] = 'minecraft:block/oak_planks'
    write(path, model)
    # The hook inventory model has its own embedded wooden plaque.
    path = ASSETS / 'models/item/hook_shop.json'
    model = read(path)
    for element in model.get('elements', []):
        if 'plaque' in element.get('name', '').lower():
            retexture(element, 'planks')
    model['textures']['planks'] = 'minecraft:block/oak_planks'
    write(path, model)


def crates():
    path = ASSETS / 'models/block/traders_crate.json'
    old = read(path)
    plaque = next(e for e in old['elements'] if e.get('name') == 'plaque')
    retexture(plaque, 'planks')
    tilt = {'angle': -45, 'axis': 'x', 'origin': [8, -1, 4]}
    elements = [
        cube('tray_floor', [1, -1, 4], [15, 0, 20], rotation=tilt),
        cube('tray_left_wall', [1, 0, 4], [2, 5, 20], rotation=tilt),
        cube('tray_right_wall', [14, 0, 4], [15, 5, 20], rotation=tilt),
        cube('tray_front_wall', [2, 0, 4], [14, 5, 5], rotation=tilt),
        cube('tray_back_wall', [2, 0, 19], [14, 5, 20], rotation=tilt),
        cube('tray_left_rim', [.75, 5, 3.75], [2.25, 6, 20.25], rotation=tilt),
        cube('tray_right_rim', [13.75, 5, 3.75], [15.25, 6, 20.25], rotation=tilt),
        cube('tray_front_rim', [2.25, 5, 3.75], [13.75, 6, 5.25], rotation=tilt),
        cube('tray_back_rim', [2.25, 5, 18.75], [13.75, 6, 20.25], rotation=tilt),
        plaque,
    ]
    for side, x in [('left', .75), ('right', 14)]:
        elements += [
            cube(side+'_foot_rail', [x, 0, 6.75], [x+1.25, 1.25, 14], 'log', 'z'),
            cube(side+'_rear_leg', [x, 1.25, 12], [x+1.25, 6.75, 14], 'log', 'y'),
            # The upper faces of these saddles exactly follow the floor underside.
            cube(side+'_saddle', [x, -2.25, 14.5], [x+1.25, -1, 18.5], 'log', 'z', tilt),
            cube(side+'_brace', [x+.25, 1.5, 8], [x+1, 8.5, 9], 'log', 'y',
                 {'angle': 45, 'axis': 'x', 'origin': [x+.625, 1.5, 8.5]}),
        ]
    elements.append(cube('rear_crossbeam', [2, .25, 12], [14, 1.5, 14], 'log', 'x'))
    model = {'credit': 'Original Spudacious5705 design; Reforged closed tray and timber stand',
             'textures': wood_textures('spruce'), 'elements': elements,
             'display': {'gui': {'rotation': [25, 225, 0], 'translation': [0, 0, 0], 'scale': [.65, .65, .65]},
                         'fixed': {'rotation': [0, 180, 0], 'scale': [.6, .6, .6]}}}
    write(path, model)
    # Preserve the original block/item while giving new crates a consistent inventory model.
    write(ASSETS / 'models/item/crate_shop.json', {'parent': 'spudaciousshops:block/traders_crate'})
    axes = read(DATA / 'minecraft/tags/block/mineable/axe.json')
    for wood in WOODS:
        block_id = 'crate_shop_' + wood
        write(ASSETS / f'models/block/{block_id}.json', {
            'parent': 'spudaciousshops:block/traders_crate', 'textures': wood_textures(wood)})
        write(ASSETS / f'models/item/{block_id}.json', {'parent': 'spudaciousshops:block/'+block_id})
        write(ASSETS / f'blockstates/{block_id}.json', {'variants': {
            'facing='+face: {'model': 'spudaciousshops:block/'+block_id, **({'y': angle} if angle else {})}
            for face, angle in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
        write(DATA / f'spudaciousshops/loot_table/blocks/{block_id}.json', {
            'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
                {'type': 'minecraft:item', 'name': 'spudaciousshops:'+block_id}],
                'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
        write(DATA / f'spudaciousshops/recipe/{block_id}.json', {
            'type': 'minecraft:crafting_shaped', 'category': 'building',
            'pattern': ['PPP', 'FBF', 'PPP'], 'key': {
                'P': {'item': 'minecraft:'+wood+'_planks'},
                'F': {'item': 'minecraft:'+wood+'_fence'}, 'B': {'item': 'minecraft:barrel'}},
            'result': {'id': 'spudaciousshops:'+block_id, 'count': 1}})
        identifier = 'spudaciousshops:'+block_id
        if identifier not in axes['values']:
            axes['values'].append(identifier)
    write(DATA / 'minecraft/tags/block/mineable/axe.json', axes)
    shop_tag_path = DATA / 'spudaciousshops/tags/block/spuds_shops.json'
    shop_tag = read(shop_tag_path)
    for wood in WOODS:
        identifier = 'spudaciousshops:crate_shop_'+wood
        if identifier not in shop_tag['values']:
            shop_tag['values'].append(identifier)
    write(shop_tag_path, shop_tag)
    for path in (ASSETS / 'lang').glob('*.json'):
        model = read(path)
        for wood in WOODS:
            # Provide readable fallback names in every bundled language.
            label = wood.replace('_', ' ').title()+' Crate Shop'
            model['block.spudaciousshops.crate_shop_'+wood] = label
            model['item.spudaciousshops.crate_shop_'+wood] = label
        write(path, model)


def clipped_uv(source, x1, z1, x2, z2):
    lo, hi = source['from'], source['to']
    u1, v1, u2, v2 = source['faces']['up']['uv']
    def lerp(value, a, b, start, end):
        return round(start+(end-start)*(value-a)/(b-a), 6)
    return [lerp(x1, lo[0], hi[0], u1, u2), lerp(z1, lo[2], hi[2], v1, v2),
            lerp(x2, lo[0], hi[0], u1, u2), lerp(z2, lo[2], hi[2], v1, v2)]


def rugs():
    sources = {part: read(ASSETS / f'models/block/rug_shop/rug_shop_{part}.json')['elements'][0]
               for part in ['main', 'north', 'east', 'south', 'west']}
    for mask in range(16):
        north, east, south, west = [bool(mask & (1 << i)) for i in range(4)]
        xs = [0 if west else 1, 5, 11, 16 if east else 15]
        zs = [0 if north else 1, 5, 11, 16 if south else 15]
        elements = []
        for row, col in itertools.product(range(3), repeat=2):
            x1, x2, z1, z2 = xs[col], xs[col+1], zs[row], zs[row+1]
            horizontal = ('west' if west else None) if col == 0 else (('east' if east else None) if col == 2 else None)
            vertical = ('north' if north else None) if row == 0 else (('south' if south else None) if row == 2 else None)
            if horizontal and vertical:
                # Connected corners contain carpet, with no leftover gold rim.
                uv = clipped_uv(sources['main'], 5, 5, 11, 11)
            else:
                uv = clipped_uv(sources[horizontal or vertical or 'main'], x1, z1, x2, z2)
            elements.append({'name': f'patch_{row}_{col}', 'from': [x1, .01, z1], 'to': [x2, .01, z2],
                             'faces': {'up': {'uv': uv, 'texture': '#all'}}})
        write(ASSETS / f'models/block/rug_shop/connected_{mask}.json', {
            'textures': {'all': 'spudaciousshops:block/rug/red', 'particle': '#all'}, 'elements': elements})
    for color in COLORS:
        block_id = 'rug_shop' if color == 'red' else 'rug_shop_'+color
        variants = {}
        for mask in range(16):
            name = f'rug_shop_{color}_connected_{mask}'
            write(GENERATED / f'models/block/{name}.json', {
                'parent': f'spudaciousshops:block/rug_shop/connected_{mask}',
                'textures': {'all': f'spudaciousshops:block/rug/{color}', 'particle': '#all'}})
            state = ','.join(f'{direction}={str(bool(mask & (1 << i))).lower()}'
                             for i, direction in enumerate(['north', 'east', 'south', 'west']))
            variants[state] = {'model': 'spudaciousshops:block/'+name}
        write(GENERATED / f'blockstates/{block_id}.json', {'variants': variants})


if __name__ == '__main__':
    shelves()
    other_wood_models()
    crates()
    rugs()
    print('Generated wood-aware shops, eleven crate variants, and seamless rug connection geometry.')
