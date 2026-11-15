"""Block translations of the supplied SA2 head covers, in canonical skull pixels.

The old fitted envelopes put every source head cover on the crown. These shared
shell panels surround the unchanged 7 x 6 x 6 skin instead. Raw provider meshes
and textures remain references; worn and custom item meshes share this translation.
All twelve retain their item identities and have real iris openings.
"""
from fruit_forms02512 import Mesh

HEAD = (-3.5, -6, -3, 3.5, 0, 3)
SHELL = (-3.75, -6.30, -3.30, 3.75, .20, 3.30)
WOOL = {
    'red_wool_beanie': ('alo_mask_wool00', 'alo_mask_wool01', 'alo_mask_wool02'),
    'blue_wool_beanie': ('alo_mask_wool_b00', 'alo_mask_woolb01', 'alo_mask_woolb02'),
    'black_white_wool_beanie': ('alo_mask_woolc00', 'alo_mask_woolc01', 'alo_mask_woolc02'),
}

class Shell:
    def __init__(self): self.mesh = Mesh()
    def box(self, lo, hi, mat, top=None, side=None, uv=(.02,.02,.98,.98), bottom=True, cap=True, ports=False):
        x,y,z=lo;X,Y,Z=hi;u,v,U,V=uv;t=[(u,v),(u,V),(U,V),(U,v)]
        if ports:
            def panel(a,b,c,d):
                self.plane((a,c,z),(b,d,z),mat,
                    (u+(a-x)/(X-x)*(U-u),v+(c-y)/(Y-y)*(V-v),
                     u+(b-x)/(X-x)*(U-u),v+(d-y)/(Y-y)*(V-v)))
            for a,b in [(y,-3.25),(-.75,Y)]:panel(x,X,a,b)
            for a,b in [(x,-3.2),(-.8,.8),(3.2,X)]:panel(a,b,-3.25,-.75)
        else:self.mesh.face([(x,y,z),(x,Y,z),(X,Y,z),(X,y,z)],mat,t)
        for points,k in [
            ([(X,y,Z),(X,Y,Z),(x,Y,Z),(x,y,Z)],side or mat),
            ([(x,y,Z),(x,Y,Z),(x,Y,z),(x,y,z)],side or mat),
            ([(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)],side or mat),
            ([(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)],(top or mat) if cap else None),
        ]:
            if k is not None: self.mesh.face(points,k,t)
        if bottom: self.mesh.face([(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],side or mat,t)
    def plane(self, lo, hi, mat, uv=(.02,.02,.98,.98)):
        x,y,z=lo;X,Y,Z=hi;u,v,U,V=uv
        self.mesh.face([(x,y,z),(x,Y,Z),(X,Y,Z),(X,y,z)],mat,[(u,v),(u,V),(U,V),(U,v)])
    def cloth_front(self,x,X,y,Y,z,mat):
        # A shared uninterrupted weave follows the same height on every panel.
        breaks=[y]+[v for v in (-4.14,-1.97) if y<v<Y]+[Y]
        for a,b in zip(breaks,breaks[1:]):
            row=int(((a+b)/2+6.30)/2.167)
            v0=(a+6.30-row*2.167)/2.167;v1=(b+6.30-row*2.167)/2.167
            self.plane((x,a,z),(X,b,z),mat,(.02,max(.02,v0),.98,min(.98,v1)))
    def wool(self, mats):
        cloth,ring,pom=mats;x,y,z,X,Y,Z=SHELL
        # Side and back panels are one shell, with no fabric cap across the neck.
        for a,b in zip((y,-4.14,-1.97),(-4.14,-1.97,Y)):
            self.box((x,a,z),(X,b,Z),cloth,bottom=False,cap=False)
            self.mesh.faces.pop(-4)  # front is authored around the eye ports below
        self.mesh.face([(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)],cloth)
        # The lower lip adds cloth thickness without a cap across the neck.
        self.mesh.face([(x,Y,z),(X,Y,z),(X-.16,Y,z+.16),(x+.16,Y,z+.16)],cloth)
        self.mesh.face([(X,Y,Z),(x,Y,Z),(x+.16,Y,Z-.16),(X-.16,Y,Z-.16)],cloth)
        self.mesh.face([(x,Y,Z),(x,Y,z),(x+.16,Y,z+.16),(x+.16,Y,Z-.16)],cloth)
        self.mesh.face([(X,Y,z),(X,Y,Z),(X-.16,Y,Z-.16),(X-.16,Y,z+.16)],cloth)
        for a,b in [(y,-3.25),(-.75,Y)]: self.cloth_front(x,X,a,b,z,cloth)
        for a,b in [(x,-3.2),(-.8,.8),(3.2,X)]: self.cloth_front(a,b,-3.25,-.75,z,cloth)
        # Keep the original yellow/mint/white trim. A solid edge texel cannot
        # accidentally turn an intended cloth panel into a transparent gap.
        edge=(2.5/32,16.5/32,2.5/32,16.5/32)
        for a,b in [(-3.2,-.8),(.8,3.2)]:
            for c,d,e,f in [(a-.14,b+.14,-3.39,-3.25),(a-.14,b+.14,-.75,-.61),
                            (a-.14,a,-3.25,-.75),(b,b+.14,-3.25,-.75)]:
                self.plane((c,e,z-.015),(d,f,z-.015),ring,edge)
        self.box((-3.15,-7.05,-2.75),(3.15,-6.29,2.75),cloth)
        self.box((-.68,-8.75,-.68),(.68,-7.04,.68),pom)
        return self.mesh

def mesh(name):
    s=Shell();x,y,z,X,Y,Z=SHELL
    if name in WOOL: return s.wool(WOOL[name])
    if name=='apple_hat':
        s.box((x,y,z),(X,Y,Z),'alo_mask_apple00',bottom=False,ports=True)
        s.box((-3.15,-6.98,-2.72),(3.15,y+.01,2.72),'alo_mask_apple00',uv=(.02,.02,.98,.18))
        s.box((-.22,-8.65,-.22),(.22,-6.96,.22),'alo_mask_stump01',uv=(.1,.1,.35,.7))
        s.box((.2,-8.08,-.20),(2.1,-7.77,.35),'alo_mask_apple01',uv=(.45,.45,.45,.45))
    elif name=='bucket_hat':
        s.box((x,-6.55,z),(X,Y,Z),'alo_mask_bucket01','alo_mask_bucket00',bottom=False,ports=True)
        s.box((x-.18,-.28,z-.18),(X+.18,.25,Z+.18),'alo_mask_bucket00',uv=(.05,.42,.95,.65),bottom=False,cap=False)
    elif name=='empty_can_hat':
        s.box((x,-6.55,z),(X,Y,Z),'alo_mask_can00',bottom=False,ports=True)
        s.box((x-.11,-6.68,z-.11),(X+.11,-6.40,Z+.11),'alo_mask_can00',uv=(.02,.03,.98,.12),bottom=False,cap=False)
        s.box((x-.11,-.08,z-.11),(X+.11,.26,Z+.11),'alo_mask_can00',uv=(.02,.03,.98,.12),bottom=False,cap=False)
    elif name=='cardboard_box_hat':
        s.box((x,-6.60,z),(X,Y,Z),'alo_mask_cdbox00','alo_mask_cdbox01',uv=(.02,.02,.98,.94),bottom=False,ports=True)
        s.box((x-.1,-.72,z-.1),(X+.1,.26,Z+.1),'alo_mask_cdbox00',uv=(.02,.90,.98,.98),bottom=False,cap=False)
    elif name=='flower_pot_hat':
        s.box((x,y,z),(X,Y,Z),'alo_mask_flowerpot00',bottom=False,ports=True)
        s.box((x-.27,-.7,z-.27),(X+.27,.28,Z+.27),'alo_mask_flowerpot01',bottom=False,cap=False)
        s.box((-2.8,-6.8,-2.45),(2.8,y+.01,2.45),'alo_mask_flowerpot01')
    elif name=='paper_bag_hat':
        s.box((x,-6.6,z),(X,.06,Z),'alo_mask_paperbag00',uv=(.02,.02,.98,.90),bottom=False,ports=True)
        # The supplied bag has a torn open lower hem, not punched eye holes.
        for side in (-1,1):
            for a in (-3.5,-1.5,.5,2.5):
                s.box((a,-.05,side*3.30-.06),(a+.85,.36,side*3.30+.06),'alo_mask_paperbag00',uv=(.1,.82,.8,.9))
    elif name=='pan_hat':
        s.box((x,y,z),(X,Y,Z),'alo_mask_stewpan00',bottom=False,ports=True)
        s.box((x-.25,-.28,z-.25),(X+.25,.25,Z+.25),'alo_mask_stewpan00',bottom=False,cap=False)
        # Source identity is the gold stewpan with a handle at either side.
        for side in (-1,1):
            a,b=sorted((side*3.95,side*5.30))
            s.box((a,-1.10,-.58),(b,-.48,.58),'alo_mask_stewpan00')
    elif name=='stump_hat':
        s.box((x,-6.55,z),(X,Y,Z),'alo_mask_stump01','alo_mask_stump00',bottom=False,ports=True)
        s.box((3.65,-4.75,-.4),(4.7,-4.05,.4),'alo_mask_stump01')
        s.box((4.2,-6.15,-.35),(4.9,-4.25,.35),'alo_mask_stump01')
        s.box((4.75,-6.15,-.2),(6.05,-5.85,.25),'alo_mask_apple01',uv=(.45,.45,.45,.45))
    elif name=='watermelon_hat':
        s.box((x,y,z),(X,Y,Z),'alo_mask_wm00',bottom=False,ports=True)
        s.box((-3.1,-6.9,-2.7),(3.1,y+.01,2.7),'alo_mask_wm00',uv=(.02,.02,.98,.18))
        s.box((-.18,-8,-.18),(.18,-6.89,.18),'alo_mask_wm00',uv=(.2,.08,.2,.08))
        s.box((-.9,-8.1,-.15),(.9,-7.87,.15),'alo_mask_wm00',uv=(.2,.08,.2,.08))
    else: raise ValueError(name)
    return s.mesh
