"""Original block-built shop accessories and Chao hat meshes, fitted to source rigs.

The merchant reference is the supplied SA2 shop screenshot: neutral blue Chao,
small round dark lenses and pale lower-face mask. Each hat is an independent
skull child, allowing every ordinary movement pose to drive it without lag.
"""
from pathlib import Path
import copy
import json
import math
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
MODEL=RES/'models/entity'
HATS=['apple_hat','cardboard_box_hat','pan_hat','paper_bag_hat','empty_can_hat',
      'stump_hat','flower_pot_hat','bucket_hat','watermelon_hat',
      'red_wool_beanie','blue_wool_beanie','black_white_wool_beanie']
NAMES=['Apple Hat','Cardboard Box Hat','Pan Hat','Paper Bag Hat','Empty Can Hat',
       'Stump Hat','Flower Pot Hat','Bucket Hat','Watermelon Hat',
       'Red Wool Beanie','Blue Wool Beanie','Black and White Wool Beanie']
PALETTE={
 'ink':'#171d24','lens':'#152b33','lens_glint':'#49606a','white':'#edf0e9',
 'mask':'#d1dce0','mask_dark':'#a6bac2','red':'#c84543','red_light':'#ef6451',
 'red_dark':'#822f38','green':'#568443','leaf':'#8ab94c','stem':'#6b4c2e',
 'box':'#b98b53','box_light':'#d6ab6b','box_dark':'#795731','tape':'#ebc88a',
 'iron':'#8c9a9f','iron_light':'#c2ced0','iron_dark':'#505f68','bag':'#c3a173',
 'bag_light':'#dfc496','bag_dark':'#91714b','wood':'#815a38','wood_light':'#b5824b',
 'wood_dark':'#513c2c','terracotta':'#b76a4a','terracotta_light':'#d88d65',
 'terracotta_dark':'#7e4234','melon':'#3e8a4e','melon_light':'#7dba56',
 'melon_dark':'#235a40','blue':'#3874b2','blue_light':'#629dd1','blue_dark':'#294879',
 'cream':'#f0e5c9','black':'#30343d','black_light':'#525862','black_dark':'#1b2029'}
KEYS=list(PALETTE)

def uv(color):
    i=KEYS.index(color);x=i%8*8;y=i//8*8
    return [[x+2,y+2],[x+6,y+2],[x+6,y+6],[x+2,y+6]]

def box(name,lo,hi,color,top=None,side=None):
    x,y,z=lo;X,Y,Z=hi
    vertices=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]]
    # Outward winding, matching the established entity quad exporter.
    sides=[[0,3,2,1],[4,5,6,7],[0,1,5,4],[3,7,6,2],[0,4,7,3],[1,2,6,5]]
    colors=[color,side or color,top or color,side or color,side or color,side or color]
    return {'name':name,'material':'hat','shading':'flat','vertices':vertices,
            'faces':[{'v':v,'uv':uv(c)} for v,c in zip(sides,colors)]}

def prism(name,cx,cy,z,r,depth,color,n=8):
    vertices=[]
    for dz in [0,depth]:
        for i in range(n):
            a=math.pi*2*i/n+math.pi/8
            vertices.append([cx+math.cos(a)*r,cy+math.sin(a)*r,z+dz])
    faces=[]
    # Split both caps into triangles (renderer's polygon fan supports >4 too).
    faces.append({'v':list(reversed(range(n))),'uv':[uv(color)[i%4] for i in range(n)]})
    faces.append({'v':list(range(n,n*2)),'uv':[uv(color)[i%4] for i in range(n)]})
    for i in range(n):
        j=(i+1)%n;faces.append({'v':[i,j,j+n,i+n],'uv':uv(color)})
    return {'name':name,'material':'hat','shading':'flat','vertices':vertices,'faces':faces}

def hats():
    out={h:[] for h in HATS}
    def b(h,n,lo,hi,c,t=None,s=None):out[h].append(box(n,lo,hi,c,t,s))
    for h,c,light,dark in [('apple_hat','red','red_light','red_dark'),
                          ('watermelon_hat','melon','melon_light','melon_dark')]:
        b(h,'fruit_crown',[-3.7,-7.6,-3.2],[3.7,-5.1,3.2],c,light,dark)
        b(h,'fruit_shoulder',[-3.05,-8.55,-2.65],[3.05,-7.6,2.65],c,light)
        b(h,'fruit_top',[-1.9,-8.9,-1.7],[1.9,-8.55,1.7],light)
        if h=='apple_hat':
            b(h,'stem',[-.3,-10,-.25],[.3,-8.8,.3],'stem')
            b(h,'leaf',[.2,-9.5,-.45],[2,-9.12,.4],'leaf')
        else:
            for x in [-2.65,-.8,1.1,2.9]:
                b(h,'rind_stripe',[x,-7.6,-3.24],[x+.35,-5.1,-3.19],'melon_dark')
                b(h,'rind_top',[x*.8,-8.6,-2.7],[x*.8+.3,-7.6,-2.64],'melon_dark')
    h='cardboard_box_hat'
    b(h,'box',[-3.8,-9,-3.35],[3.8,-5.1,3.35],'box','box_light','box_dark')
    b(h,'tape',[-.48,-9.06,-3.39],[.48,-5.7,3.4],'tape')
    b(h,'flap_line',[-3.8,-7,-3.4],[3.8,-6.85,-3.34],'box_dark')
    for h in ['pan_hat','empty_can_hat','bucket_hat']:
        height={'pan_hat':1.5,'empty_can_hat':3.7,'bucket_hat':3}[h]
        top=-5.45-height
        b(h,'metal_body',[-3.6,top,-3.2],[3.6,-5.35,3.2],'iron','iron_light','iron_dark')
        b(h,'rim',[-3.9,-5.65,-3.45],[3.9,-5.15,3.45],'iron_light')
        if h=='pan_hat':
            b(h,'pan_handle',[3.5,-6.6,-.4],[6.9,-5.9,.4],'iron_dark')
            b(h,'handle_end',[6.7,-6.8,-.7],[7.8,-5.7,.7],'black')
        elif h=='empty_can_hat':
            for yy in [top+.5,top+2.8]:
                b(h,'can_ring',[-3.7,yy,-3.3],[3.7,yy+.2,3.3],'iron_light')
            b(h,'label',[-2.6,top+1,-3.27],[2.6,top+2.7,-3.21],'cream')
            b(h,'label_fruit',[-.6,top+1.4,-3.31],[.6,top+2.3,-3.26],'red')
        else:
            # Block-built handle arch, kept clear of the Chao face.
            for x in [-4,3.5]: b(h,'handle_side',[x,top-1.8,-.3],[x+.5,top+1,.3],'iron_dark')
            b(h,'handle_top',[-4,top-2,-.3],[4,top-1.5,.3],'iron_dark')
    h='paper_bag_hat'
    b(h,'bag',[-3.65,-9,-3.25],[3.65,-5.05,3.25],'bag','bag_light','bag_dark')
    for x in [-2.7,-1.1,.5,2.1]:b(h,'fold',[x,-9.4,-3.25],[x+.8,-8.9,3.25],'bag_light')
    b(h,'paper_seam',[-.12,-8.7,-3.29],[.12,-5.3,-3.24],'bag_dark')
    h='stump_hat'
    b(h,'stump',[-3.7,-8,-3.2],[3.7,-5.05,3.2],'wood','wood_light','wood_dark')
    b(h,'cut_top',[-3.2,-8.06,-2.75],[3.2,-7.96,2.75],'box_light')
    for w in [2.7,1.8,.8]:
        for x in [-w,w-.16]:b(h,'growth_ring',[x,-8.09,-w*.8],[x+.16,-8.03,w*.8],'wood')
        for z in [-w*.8,w*.8-.16]:b(h,'growth_ring',[-w,-8.09,z],[w,-8.03,z+.16],'wood')
    for x in [-2.8,-1,.8,2.5]:b(h,'bark',[x,-7.8,-3.24],[x+.27,-5.2,-3.19],'wood_dark')
    h='flower_pot_hat'
    b(h,'pot_base',[-3,-8.1,-2.7],[3,-6,2.7],'terracotta','terracotta_light','terracotta_dark')
    b(h,'pot_rim',[-3.8,-6.2,-3.35],[3.8,-5.1,3.35],'terracotta_light','terracotta','terracotta_dark')
    b(h,'pot_foot',[-2.55,-8.55,-2.25],[2.55,-8.08,2.25],'terracotta_dark')
    for h,c,l,d in [('red_wool_beanie','red','red_light','red_dark'),
                    ('blue_wool_beanie','blue','blue_light','blue_dark'),
                    ('black_white_wool_beanie','black','black_light','black_dark')]:
        b(h,'wool_crown',[-3.65,-7.8,-3.15],[3.65,-5.2,3.15],c,l,d)
        b(h,'wool_top',[-2.8,-8.6,-2.5],[2.8,-7.8,2.5],c,l,d)
        b(h,'pompom',[-.85,-9.8,-.85],[.85,-8.5,.85],l)
        stripe='cream' if h=='black_white_wool_beanie' else d
        for yy in [-7.3,-6.2]:b(h,'stripe',[-3.7,yy,-3.2],[3.7,yy+.35,3.2],stripe)
        b(h,'folded_brim',[-3.85,-5.8,-3.35],[3.85,-5.0,3.35],l,c,d)
        for x in [-3,-2,-1,0,1,2,3]:b(h,'knit_rib',[x,-5.75,-3.4],[x+.18,-5.05,-3.34],d)
    return out

def fit(official,hatmeshes):
    base=[copy.deepcopy(b) for b in official['bones'] if '__cube_' not in b['name']]
    for b in base:b['meshes']=[]
    candidates=[b for b in official['bones'] if b['name'].startswith('skull__cube_')]
    def volume(b):
        v=[v for m in b['meshes'] for v in m['vertices']]
        return math.prod(max(p[i] for p in v)-min(p[i] for p in v) for i in range(3))
    core=max(candidates,key=volume);v=[v for m in core['meshes'] for v in m['vertices']]
    height=max(p[1] for p in v)-min(p[1] for p in v)
    bottom=max(p[1] for p in v)+core['pivot'][1]
    for h,meshes in hatmeshes.items():
        m=copy.deepcopy(meshes)
        for mesh in m:
            for p in mesh['vertices']:p[1]=p[1]*(height/6)+bottom
        base.append({'name':h,'parent':'skull','pivot':[0,0,0],'meshes':m})
    return {'format_version':1,'texture_width':64,'texture_height':48,'bones':base}

def merchant_mesh():
    meshes=[]
    for side in [-1,1]:
        meshes.append(prism('round_lens',side*1.55,-2.35,-3.48,.7,.22,'lens'))
        # The old front frame stopped at the outer lens (x=2.20), while the
        # temple began at x=3.40: an entire model pixel of empty air. Overlap
        # the lens, corner and temple so every part remains joined from above.
        x0,x1=(-3.65,-2.05) if side<0 else (2.05,3.65)
        meshes.append(box('frame_corner',[x0,-2.48,-3.50],[x1,-2.22,-3.21],'ink'))
        x0,x1=(-3.65,-3.40) if side<0 else (3.40,3.65)
        meshes.append(box('temple',[x0,-2.48,-3.39],[x1,-2.22,2.40],'ink'))
        meshes.append(box('temple_hook',[x0,-2.39,2.12],[x1,-1.73,2.45],'ink'))
    meshes.append(box('nose_bridge',[-1.00,-2.5,-3.5],[1.00,-2.25,-3.28],'ink'))
    # Keep the front cloth in front of z=-3, then turn it around both cheeks.
    # Its side straps now overlap the face instead of beginning 0.22px behind
    # the front cloth and 0.07px beyond its side edges.
    meshes.append(box('face_mask',[-3.55,-1.65,-3.29],[3.55,-.12,-3.11],'mask','white','mask_dark'))
    meshes.append(box('mask_lower',[-3.15,-.18,-3.27],[3.15,.16,-2.84],'mask_dark'))
    for y in [-1.2,-.7]:meshes.append(box('mask_pleat',[-2.7,y,-3.32],[2.7,y+.09,-3.275],'white'))
    for side in [-1,1]:
        x0,x1=(-3.63,-3.38) if side<0 else (3.38,3.63)
        meshes.append(box('mask_cheek',[x0,-1.65,-3.27],[x1,-.12,-2.94],'mask_dark'))
        for y in [-1.60,-.34]:
            meshes.append(box('mask_side_strap',[x0,y,-3.16],[x1,y+.19,3.22],'mask_dark'))
    # Chao do not have ears to hook elastic onto. Two connected rear bands
    # support the mask around the head, without adding ears or extra costume.
    for y in [-1.60,-.34]:
        meshes.append(box('mask_rear_strap',[-3.53,y,3.04],[3.53,y+.19,3.24],'mask_dark'))
    official=json.loads((MODEL/'official/chao_baby.json').read_text())
    return fit(official,{'merchant_accessories':meshes})

def sprites():
    # Authored at native 16px, with each silhouette sharing the in-world hat palette.
    for h in HATS:
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        def c(key):return PALETTE[key]
        if h in ['apple_hat','watermelon_hat']:
            apple=h=='apple_hat';p='red' if apple else 'melon';dark=p+'_dark';light=p+'_light'
            d.rounded_rectangle((2,4,13,14),radius=3,fill=c(p),outline=c(dark))
            d.rectangle((4,3,11,5),fill=c(light));d.line((4,6,4,10),fill=c(light))
            if apple:
                d.rectangle((7,1,8,4),fill=c('stem'));d.polygon([(9,2),(12,1),(11,3),(9,3)],fill=c('leaf'))
            else:
                for x in [4,7,10]:d.line((x,5,x,13),fill=c(dark))
        elif h=='cardboard_box_hat':
            d.rectangle((2,4,13,13),fill=c('box'),outline=c('box_dark'))
            d.polygon([(2,4),(5,1),(13,2),(13,4)],fill=c('box_light'))
            d.rectangle((7,2,8,13),fill=c('tape'));d.line((2,8,13,8),fill=c('box_dark'))
        elif h=='paper_bag_hat':
            d.polygon([(3,2),(5,3),(7,1),(9,3),(12,2),(13,14),(2,14)],fill=c('bag'),outline=c('bag_dark'))
            d.line((4,5,4,12),fill=c('bag_light'));d.line((9,4,9,13),fill=c('bag_dark'))
        elif h=='stump_hat':
            d.rectangle((2,5,13,13),fill=c('wood'),outline=c('wood_dark'));d.ellipse((2,2,13,7),fill=c('wood_light'),outline=c('wood_dark'))
            d.ellipse((4,3,11,6),outline=c('wood'));d.ellipse((6,4,9,5),outline=c('wood_dark'))
            for x in [4,8,11]:d.line((x,8,x-1,12),fill=c('wood_dark'))
        elif h=='flower_pot_hat':
            d.polygon([(4,2),(11,2),(13,12),(2,12)],fill=c('terracotta'),outline=c('terracotta_dark'))
            d.rectangle((1,11,14,14),fill=c('terracotta_light'),outline=c('terracotta_dark'));d.line((5,4,4,9),fill=c('terracotta_light'))
        elif h in ['pan_hat','empty_can_hat','bucket_hat']:
            if h=='pan_hat':
                d.rectangle((1,7,10,12),fill=c('iron_dark'));d.ellipse((1,4,11,9),fill=c('iron'),outline=c('iron_dark'))
                d.rectangle((10,7,15,9),fill=c('black'));d.line((3,6,8,6),fill=c('iron_light'))
            else:
                if h=='bucket_hat':d.arc((3,0,12,11),180,360,fill=c('iron_dark'),width=1)
                d.rectangle((3,4,12,13),fill=c('iron'),outline=c('iron_dark'));d.ellipse((2,2,13,6),fill=c('iron_light'),outline=c('iron_dark'))
                d.line((4,6,4,12),fill=c('iron_light'))
                if h=='empty_can_hat':
                    d.rectangle((5,7,11,10),fill=c('cream'));d.rectangle((7,8,9,9),fill=c('red'));d.line((3,12,12,12),fill=c('iron_light'))
        else:
            p='red' if h.startswith('red') else 'blue' if h.startswith('blue') else 'black'
            d.rounded_rectangle((3,3,12,12),radius=3,fill=c(p),outline=c(p+'_dark'));d.rectangle((2,10,13,14),fill=c(p+'_light'),outline=c(p+'_dark'))
            d.rectangle((6,0,9,3),fill=c(p+'_light'));stripe='cream' if p=='black' else p+'_dark'
            d.line((4,6,11,6),fill=c(stripe));d.line((3,8,12,8),fill=c(stripe))
            for x in [4,6,8,10,12]:d.line((x,11,x,13),fill=c(p+'_dark'))
        texture=RES/'textures/item'/f'{h}.png';texture.parent.mkdir(parents=True,exist_ok=True);im.save(texture)
        (RES/'models/item'/f'{h}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'chao_garden:item/{h}'}},separators=(',',':'))+'\n')

def write_meshes():
    """Refresh source-skeleton copies without touching item art or names."""
    dest=MODEL/'hats';dest.mkdir(parents=True,exist_ok=True)
    h=hats()
    for path in sorted((MODEL/'official').glob('chao_*.json')):
        data=fit(json.loads(path.read_text()),h)
        (dest/path.name).write_text(json.dumps(data,separators=(',',':'))+'\n')
    (MODEL/'market_accessories.json').write_text(json.dumps(merchant_mesh(),separators=(',',':'))+'\n')

def main():
    atlas=Image.new('RGBA',(64,48))
    d=ImageDraw.Draw(atlas)
    for i,key in enumerate(KEYS):
        x=i%8*8;y=i//8*8;d.rectangle((x,y,x+7,y+7),fill=PALETTE[key])
    out=RES/'textures/entity/chao/market_accessories.png';out.parent.mkdir(parents=True,exist_ok=True);atlas.save(out)
    write_meshes()
    # Keep the original baby anatomy palette; accessories provide the costume.
    original=Image.open(ROOT/'art/imported_chao/textures/baby.png').convert('RGBA')
    original.save(RES/'textures/entity/chao/official/black_market.png')
    sprites()
    lang=RES/'lang/en_us.json';data=json.loads(lang.read_text())
    for h,name in zip(HATS,NAMES):data['item.chao_garden.'+h]=name
    lang.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
    print('Generated20 fitted hat rigs,12hat sprites and neutral merchant costume.')

if __name__=='__main__':main()
