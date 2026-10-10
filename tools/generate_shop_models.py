#!/usr/bin/env python3
"""Generate resource-pack-aware wood models and non-overlapping connected rugs.

Run from any directory with Python 3. No image processing or dependencies needed.
Existing registry IDs and inventories are preserved.
"""
from pathlib import Path
import json
import itertools
import copy
import math

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
            'particle': f'minecraft:block/{wood}_planks',
            'gold': 'minecraft:block/gold_block', 'metal': 'minecraft:block/iron_block',
            'chain': 'minecraft:block/chain'}


def cube(name, lo, hi, texture='planks', axis=None, rotation=None):
    faces = {}
    for face in FACES:
        end = axis and face in {'x': ['east', 'west'], 'y': ['up', 'down'],
                               'z': ['north', 'south']}[axis]
        dx, dy, dz = [b-a for a,b in zip(lo,hi)]
        width, height = (dx,dz) if face in ['up','down'] else ((dz,dy) if face in ['east','west'] else (dx,dy))
        info = {'uv': [0, 0, min(16,width), min(16,height)], 'texture': '#log_end' if end else '#' + texture}
        # Log side grain follows the actual beam axis, including its top/bottom faces.
        if axis == 'x' and face in ['north', 'south', 'up', 'down']:
            info['rotation'] = 90
        if axis == 'z' and face in ['east', 'west']:
            info['rotation'] = 90
        faces[face] = info
    if texture == 'log':
        # Timber supports use the shop's plank material, not a narrow sliver of
        # stripped-log grain that becomes stretched parallel lines on thin rails.
        for face, info in faces.items():
            info['texture'] = '#planks'
            info.pop('rotation', None)
            uv = info['uv']
            w,h=max(.25,uv[2]),max(.25,uv[3])
            if w >= h:
                info['uv']=[0,0,16,max(4,16*h/w)]
            else:
                info['uv']=[0,0,max(4,16*w/h),16]
    element = {'name': name, 'from': lo, 'to': hi, 'faces': faces}
    if rotation:
        element['rotation'] = rotation
    return element


def retexture(element, key):
    for info in element['faces'].values():
        info['texture'] = '#' + key
        lo, hi = element['from'], element['to']
        dx,dy,dz = [abs(b-a) for a,b in zip(lo,hi)]
        face = next(k for k,v in element['faces'].items() if v is info)
        w,h = (dx,dz) if face in ['up','down'] else ((dz,dy) if face in ['east','west'] else (dx,dy))
        info['uv'] = [0,0,min(16,w),min(16,h)]
        info.pop('rotation', None)


def shelves():
    for stem, offsets in [('shelf_shop_bottom', [0]), ('shelf_shop_top', [7]),
                          ('shelf_shop', [0, 7])]:
        path = ASSETS / f'models/block/shelf/{stem}.json'
        model = read(path)
        # Preserve the original shelf geometry; regenerate only our front tags.
        elements = [e for e in model['elements'] if not e.get('name', '').startswith(('price_tag','detail_'))]
        for element in elements:
            retexture(element, 'planks')
        for offset in offsets:
            elements.append(cube('detail_shelf_lip', [1.5,offset+2,8.85], [14.5,offset+2.5,9.15], 'log','x'))
            # Same eight-by-four model-unit board as the normal shop plaque.
            elements.append(cube('price_tag_board', [4.5, offset-1.5, 8.35], [11.5, offset+1.5, 9.1]))
            elements.append(cube('price_tag_face', [4.75, offset-1.25, 8.25],
                                 [11.25, offset+1.25, 8.35]))
            # Short metal hangers connect the sign to the shelf's front rail.
            for x in [5, 10.5]:
                elements.append(cube('price_tag_hanger', [x, offset+1.25, 8.625],
                                     [x+.5, offset+3, 9.25], 'metal'))
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
    model['elements'] = [e for e in model['elements'] if e.get('name') != 'shelfinverted']
    for e in model['elements']:
        if e.get('name') is None and e['from'] == [3,8,6]:
            e['to'][1] = 12
        for face in e['faces'].values():
            if face['texture'] == 'glass': face['texture'] = '#glass'
    model['elements'] = [e for e in model['elements'] if not e.get('name','').startswith('detail_')]
    model['elements'] += [
        cube('detail_front_lower_rail', [.5,6.15,1.75], [15.5,6.65,2.05], 'log','x'),
        cube('detail_front_upper_rail', [.5,11.2,1.75], [15.5,11.8,2.05], 'log','x'),
    ]
    for x in [1.25,14.25]:
        model['elements'].append(cube('detail_corner_pin',[x,8.4,2.65],[x+.5,8.9,3.05],'metal'))
    model['textures']['metal'] = 'minecraft:block/iron_block'
    model['textures'].update(wood_textures('oak'))
    model.pop('texture_size', None)
    write(path, model)
    for wood in WOODS:
        path = ASSETS / f'models/block/shop_{wood}.json'
        model = read(path)
        model['textures'].update(wood_textures(wood))
        write(path, model)
    # Inventory variants share the placed shop geometry, including its cushion.
    cushion = read(ASSETS / 'models/block/cushion.json')['elements'][0]
    cushion['from'], cushion['to'] = [3,18,6], [13,19,13]
    cushion['rotation'] = {'angle': -22.5, 'axis': 'x', 'origin': [0,12,16]}
    # Match the 32px CubeListBuilder atlas: top starts after the 7px depth strip.
    cushion_uvs = {
        'up': [3.5, 0, 8.5, 3.5], 'down': [8.5, 0, 13.5, 3.5],
        'north': [3.5, 3.5, 8.5, 4], 'south': [3.5, 3.5, 8.5, 4],
        'east': [8.5, 3.5, 12, 4], 'west': [0, 3.5, 3.5, 4],
    }
    for direction, face in cushion['faces'].items():
        face['texture'] = '#cushion'
        face['uv'] = cushion_uvs[direction]
    for wood in WOODS:
        for color in COLORS:
            item = read(ASSETS / 'models/block/shop.json')
            item['textures'].update(wood_textures(wood))
            item['textures']['cushion'] = f'spudaciousshops:block/cushion/{color}'
            item['elements'] = item['elements'] + [cushion]
            write(ASSETS / f'models/item/shop_{wood}_{color}.json',item)
        write(ASSETS / f'models/item/shop_{wood}.json',
              {'parent': f'spudaciousshops:item/shop_{wood}_red'})
    # Hook and windowsill price boards are also ordinary pack-provided wood.
    path = ASSETS / 'models/block/hook_shop_plaque.json'
    model = read(path)
    for element in model['elements']:
        if element.get('name') == 'hook':
            element['faces'] = {
                'north': {'uv':[6,0,9,7], 'texture':'#2'},
                'south': {'uv':[9,0,6,7], 'texture':'#2'}}
        else: retexture(element, 'planks')
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
    plaque['from'], plaque['to'] = [4.5,1.25,-2.5], [11.5,4.75,-1.5]
    plaque['rotation'] = {'angle':22.5,'axis':'x','origin':[8,3,-2.5]}
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
        # One continuous bearer follows the tray; no separate saddle/brace joint.
        # Recess the upright inside it, keeping the tenon faces hidden and avoiding
        # coincident side faces at the intersection.
        elements += [
            cube(side+'_foot_rail', [x, 0, 6.25], [x+1.25, 1.25, 15.25], 'log', 'z'),
            cube(side+'_rear_leg', [x+.25, 1, 13], [x+1, 8, 14], 'log', 'y'),
            cube(side+'_saddle', [x+.125, -2.25, 7], [x+1.125, -1, 18.5], 'log', 'z', tilt),
        ]
    elements.append(cube('rear_crossbeam', [1.875, .25, 13], [14.125, 1, 14], 'log', 'x'))
    model = {'credit': 'Original Spudacious5705 design; Reforged closed tray and timber stand',
             'textures': wood_textures('oak'), 'elements': elements,
             'display': {'gui': {'rotation': [25, 225, 0], 'translation': [0, 0, 0], 'scale': [.65, .65, .65]},
                         'fixed': {'rotation': [0, 180, 0], 'scale': [.6, .6, .6]},
                         'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [.39, .39, .39]},
                         'thirdperson_lefthand': {'rotation': [75, 225, 0], 'translation': [0, 2.5, 0], 'scale': [.39, .39, .39]},
                         'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [.455, .455, .455]},
                         'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [.455, .455, .455]},
                         'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.25, .25, .25]}}}
    write(path, model)
    # Preserve the original block/item while giving new crates a consistent inventory model.
    write(ASSETS / 'models/item/crate_shop.json', {'parent': 'spudaciousshops:block/traders_crate'})
    axes = read(DATA / 'minecraft/tags/block/mineable/axe.json')
    for wood in WOODS:
        block_id = 'crate_shop' if wood == 'oak' else 'crate_shop_' + wood
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
        write(DATA / f'spudaciousshops/recipe/{block_id + ("_from_planks" if wood == "oak" else "")}.json', {
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
        identifier = 'spudaciousshops:crate_shop' if wood == 'oak' else 'spudaciousshops:crate_shop_'+wood
        if identifier not in shop_tag['values']:
            shop_tag['values'].append(identifier)
    write(shop_tag_path, shop_tag)
    for path in (ASSETS / 'lang').glob('*.json'):
        model = read(path)
        for wood in WOODS:
            # Provide readable fallback names in every bundled language.
            label = wood.replace('_', ' ').title()+' Crate Shop'
            model['block.spudaciousshops.'+('crate_shop' if wood == 'oak' else 'crate_shop_'+wood)] = label
            model['item.spudaciousshops.'+('crate_shop' if wood == 'oak' else 'crate_shop_'+wood)] = label
        write(path, model)


def counters():
    # Centered half-footprint tray: front surface y=1, rear y=8.
    length = 7 * 2 ** .5
    tilt = {'angle':-45,'axis':'x','origin':[8,1,4.5]}
    elements = [
        cube('counter_plate',[1,0,4.5],[15,1,4.5+length],rotation=tilt),
        cube('counter_back_lip',[1,1,4.5+length-.4],[15,1.5,4.5+length],rotation=tilt),
        cube('counter_left_lip',[1,1,4.5],[1.5,1.5,4.5+length-.4],rotation=tilt),
        cube('counter_right_lip',[14.5,1,4.5],[15,1.5,4.5+length-.4],rotation=tilt),
        cube('plaque',[4.5,1,3.6],[11.5,4,4.2],rotation={'angle':22.5,'axis':'x','origin':[8,1,3.6]}),
    ]
    for x in [1.5,13.25]:
        elements += [
            cube('counter_foot',[x,0,4],[x+1.25,.5,12],'log','z'),
            cube('counter_bearer',[x+.125,-.7,4.8],[x+1.125,0,4.5+length-.3],'log','z',tilt),
            cube('counter_rear_leg',[x+.25,.4,10.5],[x+1,6,11.2],'log','y'),
        ]
    elements += [
        cube('counter_crossbeam',[2.75,.15,10.5],[13.25,.65,11.2],'log','x'),
        # One compact centered bracket replaces the tall pair of sign feet.
        cube('sign_bracket',[7.5,.25,3.9],[8.5,1.7,4.7],'log','z'),
    ]
    model = {'textures':wood_textures('oak'),'elements':elements,
             'transform':{'scale':[.7,.65,1],'origin':[.5,0,.5]},
             'display':{'gui':{'rotation':[25,225,0],'scale':[.75,.75,.75]},
                        'ground':{'translation':[0,2,0],'scale':[.35,.35,.35]},
                        'fixed':{'rotation':[0,180,0],'scale':[.6,.6,.6]}}}
    write(ASSETS / 'models/block/shop_window.json',model)
    axe = read(DATA / 'minecraft/tags/block/mineable/axe.json')
    pickaxe = read(DATA / 'minecraft/tags/block/mineable/pickaxe.json')
    shops = read(DATA / 'spudaciousshops/tags/block/spuds_shops.json')
    for wood in WOODS:
        suffix = 'calcite' if wood == 'oak' else ('andesite' if wood == 'spruce' else wood)
        identifier = 'shop_window_'+suffix
        write(GENERATED / f'models/block/{identifier}.json', {
            'parent':'spudaciousshops:block/shop_window','textures':wood_textures(wood)})
        write(GENERATED / f'models/item/{identifier}.json', {'parent':'spudaciousshops:block/'+identifier})
        write(GENERATED / f'blockstates/{identifier}.json', {'variants':{
            'facing='+face:{'model':'spudaciousshops:block/'+identifier,**({'y':angle} if angle else {})}
            for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
        write(DATA / f'spudaciousshops/recipe/{identifier}.json', {
            'type':'minecraft:crafting_shaped','category':'building','pattern':['PPP',' C '],
            'key':{'P':{'item':'minecraft:'+wood+'_planks'},'C':{'item':'minecraft:chest'}},
            'result':{'id':'spudaciousshops:'+identifier,'count':1}})
        write(DATA / f'spudaciousshops/loot_table/blocks/{identifier}.json', {
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'spudaciousshops:'+identifier}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        key = 'spudaciousshops:'+identifier
        if key not in axe['values']: axe['values'].append(key)
        if key not in shops['values']: shops['values'].append(key)
        if key in pickaxe['values']: pickaxe['values'].remove(key)
        for path in (ASSETS / 'lang').glob('*.json'):
            lang = read(path)
            for prefix in ['block','item']: lang[prefix+'.spudaciousshops.'+identifier] = wood.replace('_',' ').title()+' Counter Shop'
            write(path,lang)
    write(DATA / 'minecraft/tags/block/mineable/axe.json',axe)
    write(DATA / 'minecraft/tags/block/mineable/pickaxe.json',pickaxe)
    write(DATA / 'spudaciousshops/tags/block/spuds_shops.json',shops)
    path = ASSETS / 'lang/en_us.json'
    lang = read(path)
    for key in list(lang):
        identifier = key.split('spudaciousshops.')[-1]
        for wood in WOODS:
            if identifier == 'shop_'+wood:
                lang[key] = wood.replace('_',' ').title()+' Glass Display Shop'
            elif identifier.startswith('shop_'+wood+'_'):
                color = identifier[len('shop_'+wood+'_'):].replace('_',' ').title()
                lang[key] = wood.replace('_',' ').title()+' Glass Display Shop ('+color+' Cushion)'
        if identifier == 'hook_shop':lang[key] = 'Hanging Display Shop'
        if identifier == 'rug_shop':lang[key] = 'Red Rug Shop'
        elif identifier.startswith('rug_shop_'):lang[key] = identifier[len('rug_shop_'):].replace('_',' ').title()+' Rug Shop'
    write(path,lang)


def clipped_uv(source, x1, z1, x2, z2):
    lo, hi = source['from'], source['to']
    u1, v1, u2, v2 = source['faces']['up']['uv']
    def lerp(value, a, b, start, end):
        return round(start+(end-start)*(value-a)/(b-a), 6)
    return [lerp(x1, lo[0], hi[0], u1, u2), lerp(z1, lo[2], hi[2], v1, v2),
            lerp(x2, lo[0], hi[0], u1, u2), lerp(z2, lo[2], hi[2], v1, v2)]


def rugs():
    source = read(ASSETS / 'models/block/rug_shop/rug_shop_main.json')['elements'][0]
    for mask in range(16):
        north, east, south, west = [bool(mask & (1 << i)) for i in range(4)]
        xmin,xmax = (0 if west else 1),(16 if east else 15)
        zmin,zmax = (0 if north else 1),(16 if south else 15)
        # Pixel silhouette of the original circular rug texture (first 16x16 tile).
        # Keep rounded corners wherever neither adjoining edge is connected.
        inset = {1:5,2:3,3:2,4:2,11:2,12:2,13:3,14:5}
        cells = set()
        for z in range(zmin,zmax):
            for x in range(xmin,xmax):
                left,right = 1,15
                if z < 5 and not north:
                    if not west: left = inset.get(z,1)
                    if not east: right = 16-inset.get(z,1)
                if z > 10 and not south:
                    if not west: left = inset.get(z,1)
                    if not east: right = 16-inset.get(z,1)
                if (west or x >= left) and (east or x < right): cells.add((x,z))
        elements = []
        for x,z in sorted(cells):
            uv = clipped_uv(source,x,z,x+1,z+1)
            if west and x < 5: uv[0],uv[2] = 4.5,4
            if east and x >= 11: uv[0],uv[2] = 4,3.5
            if north and z < 5: uv[1],uv[3] = 4.5,4
            if south and z >= 11: uv[1],uv[3] = 4,3.5
            faces = {'up': {'uv': uv,'texture':'#all'}}
            for direction,dx,dz in [('north',0,-1),('east',1,0),('south',0,1),('west',-1,0)]:
                if (x+dx,z+dz) in cells: continue
                if (direction == 'north' and north and z == 0 or
                    direction == 'east' and east and x == 15 or
                    direction == 'south' and south and z == 15 or
                    direction == 'west' and west and x == 0): continue
                faces[direction] = {'uv':[0,0,1,1],'texture':'#edge'}
            elements.append({'name':f'pixel_{x}_{z}','from':[x,0,z],'to':[x+1,1,z+1],'faces':faces})
        write(ASSETS / f'models/block/rug_shop/connected_{mask}.json', {
            'render_type':'minecraft:cutout',
            'textures': {**wood_textures('oak'),'all': 'spudaciousshops:block/rug/red', 'edge':'minecraft:block/red_wool', 'particle': '#all'}, 'elements': elements})
    sign = [cube('rug_sign_board',[5,1,2],[11,4,2.6], rotation={'angle':22.5,'axis':'x','origin':[8,1,2]}),
            cube('rug_sign_left_foot',[5.5,1,2.7],[6,1.8,4],'log','z'),
            cube('rug_sign_right_foot',[10,1,2.7],[10.5,1.8,4],'log','z')]
    write(ASSETS / 'models/block/rug_shop/price_sign.json', {
        'textures':wood_textures('oak'),'elements':sign})
    for color in COLORS:
        block_id = 'rug_shop' if color == 'red' else 'rug_shop_'+color
        variants = {}
        for mask in range(16):
            name = f'rug_shop_{color}_connected_{mask}'
            write(GENERATED / f'models/block/{name}.json', {
                'parent': f'spudaciousshops:block/rug_shop/connected_{mask}',
                'textures': {'all': f'spudaciousshops:block/rug/{color}', 'edge':f'minecraft:block/{color}_wool', 'particle': '#all'}})
            state = ','.join(f'{direction}={str(bool(mask & (1 << i))).lower()}'
                             for i, direction in enumerate(['north', 'east', 'south', 'west']))
            variants[state] = {'model': 'spudaciousshops:block/'+name}
        multipart = [{'when': dict(part.split('=') for part in state.split(',')), 'apply':apply}
                     for state,apply in variants.items()]
        multipart += [{'when':{'facing':face}, 'apply':{
            'model':'spudaciousshops:block/rug_shop/price_sign', **({'y':angle} if angle else {})}}
            for face,angle in [('north',0),('east',90),('south',180),('west',270)]]
        write(GENERATED / f'blockstates/{block_id}.json', {'multipart': multipart})
        write(GENERATED / f'models/item/{block_id}.json', {
            'render_type':'minecraft:cutout',
            'textures':{**wood_textures('oak'),'all':f'spudaciousshops:block/rug/{color}',
                        'edge':f'minecraft:block/{color}_wool'},
            'elements': read(ASSETS / 'models/block/rug_shop/connected_0.json')['elements']+sign,
            'display': {'gui': {'rotation':[30,225,0],'scale':[.75,.75,.75]},
                        'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,4,0],'scale':[.4,.4,.4]},
                        'thirdperson_lefthand':{'rotation':[75,225,0],'translation':[0,4,0],'scale':[.4,.4,.4]},
                        'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,4,0],'scale':[.5,.5,.5]},
                        'firstperson_lefthand':{'rotation':[0,225,0],'translation':[0,4,0],'scale':[.5,.5,.5]},
                        'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]}}})


def bordered(model):
    """Slim two-pixel wooden frame with one-pixel cutouts at the outer corners."""
    model['elements'] = [e for e in model.get('elements',[]) if not e.get('name','').startswith('sign_trim_') and e.get('name') != 'price_tag_face']
    for e in list(model['elements']):
        if e.get('name') not in ('plaque','price_tag_board','rug_sign_board'):
            continue
        bounds = e.setdefault('spuds_board_bounds', [list(e['from']),list(e['to'])])
        (x,y,z),(X,Y,Z) = bounds
        e['from'], e['to'] = [x+.25,y+.25,z], [X-.25,Y-.25,Z]
        retexture(e,'planks')
        # Show a broad section of the actual plank texture, preserving its grain aspect.
        panel_height = 16 * (Y-y-.5) / (X-x-.5)
        for face in ('north','south'):
            e['faces'][face]['uv'] = [0,0,16,panel_height]
        frame = [
            ('bottom',[x+.25,y-.25,z-.0625],[X-.25,y+.25,Z+.0625]),
            ('top',[x+.25,Y-.25,z-.0625],[X-.25,Y+.25,Z+.0625]),
            ('left',[x-.25,y+.25,z-.0625],[x+.25,Y-.25,Z+.0625]),
            ('right',[X-.25,y+.25,z-.0625],[X+.25,Y-.25,Z+.0625])]
        # A corner is a 2x2 group of border pixels with its outermost pixel absent.
        for cx,sx in [(x,1),(X,-1)]:
            for cy,sy in [(y,1),(Y,-1)]:
                for px,py in [(1,0),(0,1),(1,1)]:
                    xx=cx+sx*(-.25+px*.25)
                    yy=cy+sy*(-.25+py*.25)
                    frame.append(('corner',[min(xx,xx+sx*.25),min(yy,yy+sy*.25),z-.0625],
                                  [max(xx,xx+sx*.25),max(yy,yy+sy*.25),Z+.0625]))
        for side,lo,hi in frame:
            part=cube('sign_trim_'+side,lo,hi,'planks',rotation=e.get('rotation'))
            for face,info in part['faces'].items():
                if side=='corner': info['uv']=[12,10,13,11]
                elif face in ('north','south'):
                    info['uv']=[0,10,16,12] if side in ('top','bottom') else [12,0,14,16]
                else: info['uv']=[12,10,14,12]
            model['elements'].append(part)
        # Back and edge grain is continuous across the panel and all border pieces.
        # Project UVs from the whole board instead of squeezing the same tiny patch
        # onto every thin side face.
        density = 16 / (X-x+.5)
        parts = [e] + [part for part in model['elements'] if part.get('name','').startswith('sign_trim_')
                       and part.get('rotation') == e.get('rotation')
                       and part['from'][0] >= x-.25 and part['to'][0] <= X+.25
                       and part['from'][1] >= y-.25 and part['to'][1] <= Y+.25]
        for part in parts:
            lo,hi=part['from'],part['to']
            for face,info in part['faces'].items():
                if face == 'south':
                    info['uv']=[(lo[0]-x+.25)*density,(Y+.25-hi[1])*density,
                                (hi[0]-x+.25)*density,(Y+.25-lo[1])*density]
                elif face in ('east','west'):
                    info['uv']=[(lo[2]-z+.0625)*4,(Y+.25-hi[1])*density,
                                (hi[2]-z+.0625)*4,(Y+.25-lo[1])*density]
                elif face in ('up','down'):
                    info['uv']=[(lo[0]-x+.25)*density,(lo[2]-z+.0625)*4,
                                (hi[0]-x+.25)*density,(hi[2]-z+.0625)*4]
    return model


def chain_links(ymin, ymax):
    # Exact vanilla crossed-plane chain geometry, cropped without stretching its UVs.
    elements=[]
    for lo,hi,faces in [([6.5,ymin,8],[9.5,ymax,8],{'north':[3,16-ymax,0,16-ymin],'south':[0,16-ymax,3,16-ymin]}),
                        ([8,ymin,6.5],[8,ymax,9.5],{'west':[6,16-ymax,3,16-ymin],'east':[3,16-ymax,6,16-ymin]})]:
        elements.append({'name':'chain_link','from':lo,'to':hi,'shade':False,
            'rotation':{'origin':[8,8,8],'axis':'y','angle':45},
            'faces':{face:{'uv':uv,'texture':'#chain'} for face,uv in faces.items()}})
    return elements


def hanging_shops():
    # Keep legacy hook_shop as oak so existing placed shops survive the update.
    base = read(ASSETS / 'models/block/hook_shop_plaque.json')
    base['elements'] = [e for e in base['elements'] if e.get('name') not in ('hook','chain_link')]
    base['elements'] += chain_links(2, 9)
    base['textures']['chain'] = 'minecraft:block/chain'
    base = bordered(base)
    write(ASSETS / 'models/block/hook_shop_plaque.json',base)
    chain = {'render_type':'minecraft:cutout','textures':{'chain':'minecraft:block/chain','particle':'minecraft:block/chain'},'elements':chain_links(13, 16)}
    write(ASSETS / 'models/block/hook_shop_chain.json',chain)
    state = read(ASSETS / 'blockstates/hook_shop.json')
    for wood in WOODS:
        identifier = 'hook_shop' if wood == 'oak' else 'hook_shop_'+wood
        board = 'hook_shop_plaque' if wood == 'oak' else identifier+'_plaque'
        write(ASSETS / f'models/block/{board}.json', {
            'parent':'spudaciousshops:block/hook_shop_plaque','textures':wood_textures(wood)} if wood != 'oak' else base)
        variant = copy.deepcopy(state)
        for part in variant['multipart']:
            if part['apply']['model'].endswith('hook_shop_plaque'):
                part['apply']['model'] = 'spudaciousshops:block/'+board
        write(ASSETS / f'blockstates/{identifier}.json',variant)
        item = copy.deepcopy(base)
        item['elements'] += chain['elements']
        item['textures'].update(wood_textures(wood))
        item['display']['gui'] = {'rotation':[20,30,0],'scale':[.8,.8,.8]}
        write(ASSETS / f'models/item/{identifier}.json',item)
        write(DATA / f'spudaciousshops/loot_table/blocks/{identifier}.json', {
            'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'spudaciousshops:'+identifier}],
            'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
        write(DATA / f'spudaciousshops/recipe/{identifier}.json', {
            'type':'minecraft:crafting_shaped','category':'building','pattern':[' C ',' P ',' I '],
            'key':{'C':{'item':'minecraft:chain'},'P':{'item':'minecraft:'+wood+'_planks'},'I':{'item':'minecraft:iron_nugget'}},
            'result':{'id':'spudaciousshops:'+identifier,'count':1}})
        for tag in ['minecraft/tags/block/mineable/axe','spudaciousshops/tags/block/spuds_shops']:
            path=DATA/(tag+'.json'); data=read(path)
            if 'spudaciousshops:'+identifier not in data['values']: data['values'].append('spudaciousshops:'+identifier)
            write(path,data)
        for path in (ASSETS/'lang').glob('*.json'):
            lang=read(path)
            for prefix in ['item','block']: lang[prefix+'.spudaciousshops.'+identifier]=wood.replace('_',' ').title()+' Hanging Display Shop'
            write(path,lang)


def sign_borders():
    paths = [ASSETS/'models/block/shop.json', ASSETS/'models/block/shop_window.json',
             ASSETS/'models/block/traders_crate.json', ASSETS/'models/block/rug_shop/price_sign.json']
    paths += list((ASSETS/'models/block/shelf').glob('*.json'))
    paths += list((ASSETS/'models/item').glob('shop_*.json'))
    paths += list(GENERATED.glob('models/item/rug_shop*.json'))
    for path in paths:
        model=read(path)
        if 'elements' in model: write(path,bordered(model))


def inventory_icons():
    """Three-quarter inventory previews with directional lighting and centered silhouettes."""
    def elements(model, seen=None):
        if 'elements' in model: return model['elements']
        parent=model.get('parent','')
        seen=set() if seen is None else seen
        if not parent.startswith('spudaciousshops:') or parent in seen: return []
        seen.add(parent)
        relative='models/'+parent.split(':',1)[1]+'.json'
        for root in [ASSETS,GENERATED]:
            path=root/relative
            if path.exists(): return elements(read(path),seen)
        return []

    def root_transform(model, seen=None):
        if 'transform' in model: return model['transform']
        parent=model.get('parent','');seen=set() if seen is None else seen
        if not parent.startswith('spudaciousshops:') or parent in seen: return None
        seen.add(parent)
        for root in [ASSETS,GENERATED]:
            path=root/('models/'+parent.split(':',1)[1]+'.json')
            if path.exists(): return root_transform(read(path),seen)
        return None

    def rotate(point, axis, degrees):
        x,y,z=point; c=math.cos(math.radians(degrees)); t=math.sin(math.radians(degrees))
        if axis=='x': return [x,c*y-t*z,t*y+c*z]
        if axis=='y': return [c*x+t*z,y,-t*x+c*z]
        return [c*x-t*y,t*x+c*y,z]

    for root in [ASSETS,GENERATED]:
        for path in (root/'models/item').glob('*.json'):
            if not path.stem.startswith(('shop_','hook_shop','rug_shop','crate_shop','shelf_shop')): continue
            model=read(path)
            points=[]
            root_scale=root_transform(model)
            for part in elements(model):
                for point in itertools.product(*zip(part['from'],part['to'])):
                    point=list(point)
                    turn=part.get('rotation')
                    if turn:
                        origin=turn['origin']
                        point=rotate([v-o for v,o in zip(point,origin)],turn['axis'],turn['angle'])
                        point=[v+o for v,o in zip(point,origin)]
                    if root_scale:
                        origin=[v*16 for v in root_scale['origin']]
                        point=[o+(v-o)*scale for v,o,scale in zip(point,origin,root_scale['scale'])]
                    point=[v-8 for v in point]
                    # Match ItemTransform's rotationXYZ order.
                    point=rotate(rotate(point,'y',225),'x',30)
                    points.append(point)
            if not points: continue
            xmin,xmax=min(p[0] for p in points),max(p[0] for p in points)
            ymin,ymax=min(p[1] for p in points),max(p[1] for p in points)
            scale=round(min(1.15,14.5/max(xmax-xmin,ymax-ymin,.01)),6)
            model['gui_light']='side'
            model.setdefault('display',{})['gui']={
                'rotation':[30,225,0],
                'translation':[round(-(xmin+xmax)*scale/2,6),round(-(ymin+ymax)*scale/2,6),0],
                'scale':[scale]*3}
            write(path,model)



if __name__ == '__main__':
    shelves()
    other_wood_models()
    crates()
    counters()
    rugs()
    sign_borders()
    hanging_shops()
    inventory_icons()
    print('Generated wood-aware shops, eleven crate variants, and seamless rug connection geometry.')
