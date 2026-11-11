"""Authored low-poly fruit/seed meshes. Source renders guide shape, never pixels.

Coordinates are Minecraft model pixels. The same faces produce native OBJ
geometry and the inventory sprites, so a held fruit cannot change identity.
"""
import math
from PIL import Image
from sa2_fruit_art import PALETTES, ADVENTURE_SHAPES, adventure_color, rgb

CWE = dict(zip(
    ['orange_fruit','gba_orange_fruit','gba_blue_fruit','gba_pink_fruit',
     'gba_green_fruit','gba_purple_fruit','gba_yellow_fruit','gba_red_fruit',
     'shiny_fruit','hyper_swim_fruit','hyper_fly_fruit','hyper_run_fruit','hyper_power_fruit'],
    ['ed9725','f19527','579bcf','f396b9','8dc554','9b69bd','f0ce50','dc5c55',
     'd5b4ed','65b2de','a79ad7','7ec365','e67770']))
COLORS = {k:rgb(v[0]) for k,v in PALETTES.items()} | {k:rgb(v) for k,v in CWE.items()} | {'mint_candy':(125,209,171)}
SEEDS = tuple(k for k in COLORS if k != 'mint_candy')
FOODS = tuple(COLORS)

def mix(a,b,t):return tuple(round(x*(1-t)+y*t) for x,y in zip(a,b))
def shade(c,f):return tuple(max(0,min(255,round(v*f))) for v in c)

def texture(name,seed=False,age=3):
    im=Image.new('RGBA',(32,32));base=COLORS[name]
    for y in range(32):
        v=y/31
        for x in range(32):
            u=x/32;c=base
            if seed:
                # The seven Battle seeds are single sprouting bulbs, not piles
                # of miniature fruit. Their distinct coats follow game renders.
                if name=='round_fruit':c=mix((202,235,240),(35,124,207),v)
                elif name=='square_fruit':c=(20,134,84) if v<.36+math.sin(u*25)*.05 else (228,222,38)
                elif name=='triangle_fruit':c=(240,113,23) if v>.83+math.sin(u*20)*.05 else (205,216,36)
                elif name=='hero_garden_fruit':c=mix((244,205,29),(235,33,190),min(1,v*2))
                elif name=='dark_garden_fruit':c=(142,69,19)
                elif name=='strong_fruit':c=mix((231,180,39),(225,106,24),v)
                elif name=='tasty_fruit':c=(226,188,56)
                elif name=='mushroom_alt':c=(167,184,59)
                else:c=mix(base,(180,145,77),.22)
                if name=='round_fruit' and x%8==0:c=shade(c,.68)
                if name=='dark_garden_fruit':c=shade(c,.8 if (x+y*2)%9<2 else 1.08)
            elif name in ADVENTURE_SHAPES:c=adventure_color(name,x//2,y//2)
            elif name=='square_fruit':c=mix((14,177,27),(48,238,141),v)
            elif name=='strong_fruit':c=mix((201,41,7),(250,207,61),max(0,(v-.28)/.72))
            elif name=='heart_fruit':c=mix((240,45,167),(145,18,114),v)
            elif name=='hero_garden_fruit':c=shade((238,229,113),.97+.07*math.cos(u*math.tau*7))
            elif name=='dark_garden_fruit':
                tile=(x+(y//8)%2*4)%8;row=y%8
                c=(219,124,28) if abs(tile-3.5)+abs(row-3.5)<3 else (155,46,13)
                if abs(tile-3.5)+abs(row-3.5)<1.2:c=(240,171,51)
            elif name.startswith('mushroom'):
                spots=[(6,5),(23,10),(14,24),(30,27)]
                if any((x-a)**2+(y-b)**2<12 for a,b in spots):c=(245,242,213)
                if name=='mushroom_alt':c=mix(c,(233,204,84),.15)
            elif name.startswith('hyper_') and y in (13,14,23,24):c=(251,232,157)
            elif name=='shiny_fruit' and abs(x-16)+abs(y-15)<5:c=(255,242,181)
            elif name=='mint_candy' and (x+y//2)%10<3:c=(229,252,234)
            if age<3:c=mix((87,139,57),c,(.16,.45,.72)[age])
            # Restrained material variation; directional light comes from mesh
            # normals, so the texture does not carry a mismatched painted shadow.
            c=shade(c,1+(.018 if (x*7+y*11)%17<4 else -.012))
            im.putpixel((x,y),c+(255,))
    return im

def material_images(name,seed=False,age=3):
    def solid(c):return Image.new('RGBA',(32,32),c+(255,))
    return {'fruit':texture(name,seed,age),'stem':solid((111,82,39)),
            'leaf':solid((65,156,52)),'leaf_light':solid((110,187,64)),
            'stalk':solid((228,201,139))}

class Mesh:
    def __init__(self):self.faces=[]
    def face(self,points,mat='fruit',uv=None):
        if uv is None:uv=[(0,0),(1,0),(1,1),(0,1)][:len(points)]
        self.faces.append((list(points),list(uv),mat))
    def lathe(self,rings,n=10,mat='fruit',angle=0):
        # Each ring is y, radius-x, radius-z, x-offset. Increasing winding is
        # clockwise when viewed from above; side quads face outwards.
        rows=[]
        for y,rx,rz,dx in rings:
            rows.append([(8+dx+rx*math.cos(angle+i*math.tau/n),y,8+rz*math.sin(angle+i*math.tau/n)) for i in range(n)])
        for j in range(len(rows)-1):
            for i in range(n):
                k=(i+1)%n;v0=j/(len(rows)-1);v1=(j+1)/(len(rows)-1)
                self.face([rows[j][i],rows[j+1][i],rows[j+1][k],rows[j][k]],mat,
                          [(i/n,1-v0),(i/n,1-v1),((i+1)/n,1-v1),((i+1)/n,1-v0)])
        for row,up in ((rows[0],False),(rows[-1],True)):
            mid=tuple(sum(p[a] for p in row)/n for a in range(3))
            for i in range(n):
                pts=[mid,row[(i+1)%n],row[i]] if up else [mid,row[i],row[(i+1)%n]]
                self.face(pts,mat,[(.5,.5),(1,0),(0,0)])
    def box(self,a,b,mat='stem'):
        x,y,z=a;X,Y,Z=b
        for p in [[(x,y,z),(x,Y,z),(X,Y,z),(X,y,z)],[(X,y,Z),(X,Y,Z),(x,Y,Z),(x,y,Z)],
                  [(x,y,Z),(x,Y,Z),(x,Y,z),(x,y,z)],[(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)],
                  [(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)]]:self.face(p,mat)
    def leaf(self,origin,sign=1,size=1):
        x,y,z=origin
        points=[(x,y,z),(x+sign*1.8*size,y+1.6*size,z-.6*size),
                (x+sign*4.2*size,y+1.7*size,z),(x+sign*2.3*size,y+.35*size,z+.8*size)]
        for p,mat in ((points,'leaf'),(list(reversed(points)),'leaf_light')):self.face(p,mat)
    def transformed(self,scale=1,center=(8,8,8),offset=(0,0,0)):
        m=Mesh();m.faces=[([tuple((p[i]-center[i])*scale+center[i]+offset[i] for i in range(3)) for p in pts],uv,mat) for pts,uv,mat in self.faces];return m
    def add(self,other):self.faces.extend(other.faces)
    def bounds(self):
        pts=[p for face,_,_ in self.faces for p in face]
        return [min(p[i] for p in pts) for i in range(3)]+[max(p[i] for p in pts) for i in range(3)]

def fruit(name,leaves=True):
    m=Mesh();n=10
    profile=[(2.7,2.4,2.4,0),(4,4.2,4.2,0),(7.5,5,5,0),(11.3,4.2,4.2,0),(13,2.4,2.4,0),(14,.5,.5,0)]
    if name in ('garden_fruit','hero_garden_fruit','dark_garden_fruit'):
        profile=[(2,1.2,1.2,0),(3.5,3.3,3.3,0),(7,4.7,4.7,0),(10.4,4,4,0),(13,2.2,2.2,0),(14,.35,.35,0)]
    elif name=='square_fruit':
        # Four planar sides, with a small bevel at the top and bottom.
        n=4;profile=[(3,5.2,5.2,0),(4,6,6,0),(11.5,6,6,0),(12.5,5.2,5.2,0)]
    elif name=='triangle_fruit':
        n=3;profile=[(2.5,5.8,5.8,0),(3.4,5.7,5.7,0),(13,.45,.45,0)]
    elif name=='tasty_fruit':
        profile=[(2.5,.6,1, -2),(4,2.5,2.6,-1.4),(7.3,3.9,3.4,0),(10.4,2.6,2.5,1.5),(13.4,.8,.9,2.5)]
    elif name in ('strong_fruit','rare_fruit','rough_fruit'):
        n=8;profile=[(2.5,2,2,0),(4,3.8,3.8,0),(8,4.8,4.8,0),(11,2.4,2.4,0),(14,.45,.45,0)]
    elif name in ('chao_fruit','tall_fruit','vitality_fruit','wonder_fruit','forbidden_fruit') or name.startswith('hyper_'):
        profile=[(2.4,2.6,2.6,0),(4,4.3,4.3,0),(7,4.6,4.6,0),(9.4,3.1,3.1,0),(12,1.4,1.4,0),(14,.5,.5,0)]
    elif name=='smart_fruit':
        profile=[(2.5,2,2,0),(4.5,4.4,4.4,0),(7.3,2.4,2.4,0),(9.5,4.2,4.2,0),(12.8,3.8,3.8,0),(14,.7,.7,0)]
    elif name.startswith('mushroom'):
        m.lathe([(2.2,1.7,1.7,0),(7.6,1.9,1.9,0)],8,'stalk')
        m.lathe([(7,4.9,4.9,0),(8.5,5.2,5.2,0),(11.5,3.8,3.8,0),(13.2,1.3,1.3,0)],10)
        return m
    elif name=='heart_fruit':
        outline=[(8,2.2),(12,5),(13,9),(11.5,12.3),(9,12.5),(8,10.8),(6.8,12.6),(4.1,12.1),(2.9,9),(4.2,5)]
        front=[(x,y,5.4) for x,y in outline];back=[(x,y,10.6) for x,y in outline]
        for i in range(len(outline)):
            k=(i+1)%len(outline)
            m.face([(8,8,4.7),front[k],front[i]],uv=[(.5,.5),(outline[k][0]/16,1-outline[k][1]/16),(outline[i][0]/16,1-outline[i][1]/16)])
            m.face([(8,8,11.3),back[i],back[k]],uv=[(.5,.5),(outline[i][0]/16,1-outline[i][1]/16),(outline[k][0]/16,1-outline[k][1]/16)])
            m.face([front[i],front[k],back[k],back[i]])
        if leaves:m.box((8,11.5,7.7),(8.45,14,8.2));m.leaf((8.2,13.4,8))
        return m
    elif name.startswith('gba_'):
        n=8;profile=[(2.5,2.4,2.4,0),(4.5,4.8,4.8,0),(9,5.1,5.1,0),(12.5,3.6,3.6,0),(13.6,1,1,0)]
    elif name=='mint_candy':
        m.lathe([(4.5,3.9,3.9,0),(5,4.5,4.5,0),(10,4.5,4.5,0),(10.5,3.9,3.9,0)],12);return m
    m.lathe(profile,n,angle=math.pi/4 if name=='square_fruit' else 0)
    top=profile[-1][0];dx=profile[-1][3]
    if leaves and name not in ('garden_fruit','hero_garden_fruit','dark_garden_fruit','strong_fruit','tasty_fruit','rough_fruit','rare_fruit','wonder_fruit','forbidden_fruit','vitality_fruit','tall_fruit'):
        m.box((7.8+dx,top-.1,7.8),(8.3+dx,top+1.5,8.3));m.leaf((8+dx,top+1.1,8),size=.8)
    return m

def seed(name):
    m=Mesh();n=8
    if name=='triangle_fruit':n=3;rings=[(2.5,3.5,3.5,0),(3.4,3.5,3.5,0),(8.3,.65,.65,0)]
    elif name=='square_fruit':n=4;rings=[(2.5,3.2,3.2,0),(7.3,3.2,3.2,0),(8.1,2.3,2.3,0)]
    else:rings=[(2,1.6,1.5,0),(3.6,3.3,2.9,0),(6.5,3,2.7,0),(8.1,1,1,0)]
    m.lathe(rings,n,angle=math.pi/4 if n==4 else 0)
    m.box((7.8,8,7.8),(8.2,12.8,8.2),'leaf');m.leaf((8,12.4,8),1,.75)
    if name!='round_fruit':m.leaf((8,12.1,8),-1,.65)
    return m

def normal(points):
    a,b,c=points[:3];u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)]
    n=(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]);d=math.sqrt(sum(x*x for x in n))
    return tuple(x/d for x in n) if d>1e-8 else (0,0,0)

def render(mesh,materials,size=32):
    # Orthographic software render of the exact native geometry, for inventory.
    yaw=.46;pitch=.22
    def rotate(p):
        x,y,z=p;x-=8;y-=8;z-=8
        X=x*math.cos(yaw)-z*math.sin(yaw);Z=x*math.sin(yaw)+z*math.cos(yaw)
        return X,y*math.cos(pitch)-Z*math.sin(pitch),y*math.sin(pitch)+Z*math.cos(pitch)
    points=[rotate(p) for ps,_,_ in mesh.faces for p in ps];span=max(max(p[i] for p in points)-min(p[i] for p in points) for i in (0,1))
    scale=(size-4)/span;cx=(max(p[0] for p in points)+min(p[0] for p in points))/2;cy=(max(p[1] for p in points)+min(p[1] for p in points))/2
    out=Image.new('RGBA',(size,size));depth=[-1.e9]*(size*size)
    for ps,uvs,mat in mesh.faces:
        rp=[rotate(p) for p in ps];n=normal(rp)
        if n[2]<=.001:continue
        light=.75+.25*max(0,(-n[0]+n[1]+n[2])/(3**.5))
        screen=[((p[0]-cx)*scale+size/2,(cy-p[1])*scale+size/2,p[2]) for p in rp]
        for i in range(1,len(ps)-1):
            tri=[screen[j] for j in (0,i,i+1)];uv=[uvs[j] for j in (0,i,i+1)];a,b,c=tri
            den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(den)<1e-8:continue
            for y in range(max(0,math.floor(min(p[1] for p in tri))),min(size,math.ceil(max(p[1] for p in tri)))):
                for x in range(max(0,math.floor(min(p[0] for p in tri))),min(size,math.ceil(max(p[0] for p in tri)))):
                    u=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den
                    v=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den;w=1-u-v
                    if min(u,v,w)<-1e-7:continue
                    z=sum(t*p[2] for t,p in zip((u,v,w),tri));at=y*size+x
                    if z<depth[at]:continue
                    tex=materials[mat];U=sum(t*p[0] for t,p in zip((u,v,w),uv));V=sum(t*p[1] for t,p in zip((u,v,w),uv))
                    color=tex.getpixel((min(tex.width-1,max(0,int(U*tex.width))),min(tex.height-1,max(0,int(V*tex.height)))))
                    if color[3]<128:continue
                    out.putpixel((x,y),shade(color[:3],light)+(255,));depth[at]=z
    return out
