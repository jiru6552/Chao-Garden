"""Pure block translations of CWE headphone/pacifier designs; no resource writes."""

def _box(g,mesh,bounds,key,rect=(.1,.15,.4,.45),overrides=None):
    part=g.Mesh();part.box(bounds[:3],bounds[3:],key)
    for index,(pts,uv,mat) in enumerate(part.faces):
        u,v,U,V=(overrides or {}).get(index,rect)
        mesh.face(pts,key,[(u+s*(U-u),v+t*(V-v)) for s,t in uv])


def headphones(g):
    """Three cuboid band panels and padded cuboid cups with correct UV strips."""
    band='cwe_headphones_ffffffff_0'
    cup='cwe_headphones_25ff00ff_0'
    m=g.Mesh()
    # The original band uses this solid charcoal strip, never the cup artwork.
    strip=(.8515625,.05078125,.95703125,.96875)
    # Canonical skull coordinates: the band's lower face stays above the crown.
    _box(g,m,[-3.58,-6.40,-.65,3.58,-6.08,.65],band,strip)
    _box(g,m,[-3.94,-6.40,-.65,-3.58,-2.1,.65],band,strip)
    _box(g,m,[3.58,-6.40,-.65,3.94,-2.1,.65],band,strip)
    # Original left/right cup buttons occupy distinct cells in the same source
    # atlas. Preserve them on the outer side of each cuboid ear cup.
    vent=(.4609375,.0078125,.7734375,.140625)
    for sign,button in [(-1,(.625,.5,.75,.625)),(1,(.625,.25,.75,.375))]:
        x,X=(-4.50,-3.66) if sign<0 else (3.66,4.50)
        outer_face=2 if sign<0 else 3
        _box(g,m,[x,-3.8,-1.30,X,-1.1,1.30],cup,
             overrides={0:vent,outer_face:button})
        # Thin rectangular pads meet the skin without moving the button shell.
        x,X=(-3.67,-3.55) if sign<0 else (3.55,3.67)
        _box(g,m,[x,-3.7,-1.20,X,-1.2,1.20],band,strip)
    return m,{
        'reference_head':[-3.5,-6,-3,3.5,0,3],
        'bounds':m.bounds(),
        'crown_contact':-6.08,
        'crown_contact_y':-6.08,
        'cup_contact_x':[-3.55,3.55],
        'source_band_uv':[.8515625,.05078125,.95703125,.96875]
    }


def pacifier(g):
    """Yellow cuboid shield, pink rear nipple and open rectangular front loop."""
    pink='cwe_alo_mask_teethingring00_ff7fccff_0'
    yellow='cwe_alo_mask_teethingring00_ffffb2ff_52'
    face='cwe_alo_mask_teethingring00_ffffb2ff_20'
    m=g.Mesh()
    # Source pink triangles all sample one white texel; the diffuse tint carries
    # their color. Yellow sides use the actual original strip, likewise solid.
    pink_uv=(0,0,0,0)
    side_uv=(.125,.85,.125,.85)
    shield=g.Mesh();shield.box([-.93,1.27,-3.88],[.93,2.53,-3.62],yellow)
    for i,(pts,uv,key) in enumerate(shield.faces):
        if i==0:
            # Material20 is two repeated source tiles wide. Use one complete
            # source circle on the front guard, rather than both repetitions.
            m.face(pts,face,[(u*.5,v) for u,v in uv])
        else:m.face(pts,yellow,[(.125,.85)]*len(pts))
    # Rear nipple touches the actual mouth; front ring remains fully open and
    # has source-like depth instead of being flattened into the shield plane.
    guard=g.Mesh();guard.faces=list(m.faces)
    nipple=g.Mesh();loop=g.Mesh()
    _box(g,nipple,[-.22,1.68,-3.62,.22,2.12,-3.05],pink,pink_uv)
    _box(g,loop,[-.66,1.74,-4.88,-.47,2.06,-3.88],pink,pink_uv)
    _box(g,loop,[.47,1.74,-4.88,.66,2.06,-3.88],pink,pink_uv)
    _box(g,loop,[-.47,1.74,-4.88,.47,2.06,-4.69],pink,pink_uv)
    _box(g,loop,[-.47,1.74,-4.07,.47,2.06,-3.88],pink,pink_uv)
    m.faces.extend(nipple.faces);m.faces.extend(loop.faces)
    m.named_parts={'guard':guard,'nipple':nipple,'ring':loop}
    return m,{
        'bounds':m.bounds(),
        'kind':'mouth',
        'reference_head_width':7,
        'mouth':[0,1.9,-3.05],
        'mouthpoint':[0,1.9,-3.05],
        'front_back':-3.05,
        'guard_center':[0,1.9,-3.75],
        'reference_head':[-3.5,-6,-3,3.5,0,3]
    }
