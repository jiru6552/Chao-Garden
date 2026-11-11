#!/usr/bin/env python3
"""Author the complete audit capture plan; never generate approval evidence."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]


def main():
    old=json.loads((ROOT/'docs/release-scenes-0.21.0.json').read_text())
    p=json.loads(json.dumps(old))
    p.update(release='0.22.0',log_prefix='CHAO_AUDIT022',filename_prefix='chao022-')
    themes=json.loads((ROOT/'src/main/resources/assets/chao_garden/chao_variants.json').read_text())['variants']
    new=[]
    def add(key,title,caption,category):
        new.append(dict(key=key,title=title,subtitle='Actual Minecraft Java 1.21.1 / NeoForge capture',caption=caption,category=category,scene=key))
    for t in themes:
        ident,name=t['id'],t['name']
        add(f'themegrid-{ident}-0',f'{name}: adult forms and maturity',
            'Rows: Neutral/Normal, Hero/Swim and Dark/Fly. Left to right: young, half mature and fully adult. Each theme keeps its palette while using native first-type and gradual-evolution geometry.','themed-forms')
        add(f'themegrid-{ident}-1',f'{name}: Run, Power and child forms',
            'Rows: Neutral/Run, Hero/Power and a child with Fly+Power tendencies. Left to right: increasing independent maturity. Child shaping stays restrained; adult first types remain fixed.','themed-forms')
        add(f'themeclose-{ident}-front',f'{name}: native adult close-up (front)',
            'A mature themed adult uses the correct native UV layout, protected eyes, fitted forehead details and visible torso emblem. Typed head features remain part of the main form.','themed-forms')
        add(f'themeclose-{ident}-side',f'{name}: native adult close-up (side)',
            'Side view of native head proportions, typed crest and back attachments. Forehead details follow the rendered head; torso details follow the actual body.','themed-forms')
    for i in range(9):
        count=36 if i<8 else 26
        add(f'catalog-all-{i}',f'Item audit: inventory page {i+1} of 9',
            f'{count} actual registered items on this page, from the complete 314-entry namespace registry. Compatibility IDs are included in this audit, which is not an unlock list. The last page keeps unused slots empty.','item-audit')
    eggs=[
        ('colors','Placed eggs: all sixteen colors','Actual closed rounded shells with independent atlases. This view checks colors, wrapped longitude edges and the bottom closure.'),
        ('jewels','Placed eggs: all thirteen jewels','Thirteen actual placed jewel shells, including Moon. Materials share the same closed shell geometry.'),
        ('themes-0','Placed theme eggs: first ten','Ten retained theme palettes on actual placed shells. This checks theme palette consistency; canonical glyphs are on inventory eggs and Chao emblems.'),
        ('themes-1','Placed theme eggs: remaining nine','The remaining nine retained theme palettes on actual shells. Optional identity fixtures enable the mod gates for this visual audit.'),
        ('themes-shiny-0','Shiny theme eggs: first ten','Actual shiny placed eggs check theme palette consistency, shell continuity and the live reflection layer together.'),
        ('themes-shiny-1','Shiny theme eggs: remaining nine','Actual shiny placed eggs retain coherent palettes while the reflection layer supplies continuous shine across the closed shell.'),
        ('special','Natural and special egg finishes','Neutral, Hero and Dark natural palettes plus colored, shiny, two-tone and Moon finishes share the same closed rounded shell.'),
        ('close-nether','Netherrack egg: placed close-up','The rounded shell carries a recognizably granular netherrack-inspired material, rather than relying on red color alone.'),
        ('close-end','End egg: animated portal surface',"The closed shell uses Minecraft's live End-portal render surface, with no painted purple rim."),
        ('close-shiny','Shiny egg: continuous surface','The live reflection layer runs across the rounded shell. Its base albedo matches the ordinary counterpart and has no painted white bottom cap.'),
        ('close-moon','Moon egg: placed texture close-up','Moon uses the retained special egg texture on the normal shell. It is a coat/texture identity rather than a crescent-shaped egg.'),
    ]
    for key,title,caption in eggs:add('eggaudit-'+key,title,caption,'egg-audit')
    for key,title,caption in [
        ('hero','Hero head proportion comparison','Hero Normal, Fly and Power young adults share corrected six-unit head cores. Eye planes, joints, torso dimensions and crest lengths stay fixed.'),
        ('dark','Dark head proportion comparison','Dark Normal, Swim and Run use normalized head cores. The Swim family keeps its originally shorter profile where appropriate.'),
        ('special','Retained character proportion comparison','Tails, Amy, Knuckles and Omochao retain distinct approved anatomy and native skeletons. Their whole silhouettes share one view.'),
        ('chaos','Light, Angel and Devil Chaos proportions','Three Chaos skull cores share the same dimensions. Central side locks, broader Hero appendages and stronger Dark horns remain distinct; transparent tips stay attached.'),
    ]:add('proportion-'+key,title,caption,'proportion-audit')
    life=[
        ('egg','Lifecycle: individual egg','An actual individual egg begins the ordinary life cycle. Fixtures accelerate elapsed clocks while retaining real entities and transitions.'),
        ('baby','Lifecycle: baby Chao','The real egg hatches into a baby. Earned Run points control crawling and later walking; age alone does not grant movement skills.'),
        ('cocoon','Lifecycle: first evolution cocoon','The actual first-evolution state selects blue/yellow material. A real 200-tick cocoon precedes the locked first adult type.'),
        ('young','Lifecycle: young adult','The first cocoon finishes into a young adult with fixed alignment and first type. Independent maturity and later secondary training begin from this base.'),
        ('adult','Lifecycle: fully grown adult','The same adult life grows to full maturity without a second evolution cocoon. Opposing secondary influences use signed axes.'),
        ('death','Lifecycle: death cocoon','Low terminal happiness selects the real gray/white death outcome. Separate blocked-drop tests prove held items and worn equipment survive cleanup retries.'),
        ('rebirth','Lifecycle: reincarnation cocoon','Sufficient terminal care selects the real peach/pink reincarnation outcome. Identity, hereditary grades and lineage continue through the next egg.'),
        ('reborn-egg','Lifecycle: next-life egg','The actual reincarnation transaction produces one next-life egg. Physical pickup, serialization, placement and hatching are covered by server tests.'),
        ('stats-before','Reincarnation: trained stats before','Points begin at 1255, 2509, 1699, 21 and 1009 in Swim/Fly/Run/Power/Stamina order. Hereditary grades are S/S/B/S/B.'),
        ('stats-after','Reincarnation: exact ten-percent carry','After the actual next-life hatch, points are 125, 250, 169, 2 and 100. Every level is 1, progress is 0, and hereditary grades remain S/S/B/S/B.'),
    ]
    for key,title,caption in life:add('lifecycle-'+key,title,caption,'lifecycle-audit')
    for i,g in enumerate('EDCBAS'):
        add(f'grade-audit-{i}',f'Stat training: grade {g}',
            f'A fixture seeds grade {g} and performs one real 100-XP level-up per visible skill. The gain is uniform within {11+3*i}-{15+3*i} points. These are individual training levels, not simulated lifetimes.','grade-audit')
    for view in ('front','side'):
        add('merchant-audit-'+view,'Black Market Chao: connected costume ('+view+')',
            'The real merchant uses the native Chao model and motion system. Sunglasses, bridge, side pieces and mask form a connected face costume.','merchant-audit')
    targeted=[('create','power','front'),('create','power','back'),('cataclysm','power','front'),('cataclysm','power','back'),('create','fly','back'),('thaumcraft','fly','back'),('ice_fire','fly','back')]
    for ident,kind,view in targeted:
        name=next(t['name'] for t in themes if t['id']==ident)
        add(f'themeaudit-{ident}-{kind}-{view}',f'{name}: mature {kind.title()} detail ({view})',
            'Targeted view of mature geometry and fitted theme details. Chest symbols stay exposed on widened Power bodies; back details follow the real wing/torso anchors.','themed-forms')
    assert len(new)==125
    p['features']=old['features']+new
    for motion in p['motions']:
        motion.pop('review_frames',None);motion.pop('omission_reason',None)
        if motion['key']=='accessory-walk':
            motion['caption']='Actual walking with a complete outfit. The observer follows the real route so all 24 whole actors remain in frame. Recorded game-clock intervals are retained; no motion is synthesized or interpolated.'
    first=[f'catalog-all-{i}' for i in range(9)]+[f['key'] for f in new if f['key'].startswith(('lifecycle-','grade-audit-'))]
    p['capture_order']=first+[f['key'] for f in p['features'] if f['key'] not in first]+[key for m in p['motions'] for key in m['frames']]
    # Fifty retained core pages leave room for eighty-five targeted audit pages.
    remove={'growth-0-2-front','growth-0-3-side','growth-1-2-side','growth-2-4-front','reverse-0-2-side','baby-3-front'}
    models=set(old['pdf'][:22])-remove
    models.update(old['pdf'][44:53]);models.update(['amy-front',*old['pdf'][65:72]])
    robots={'chart-robot-family-0','chart-robot-family-3','chart-robot-health','chart-robot-genes','robot-coat-3','robot-stage-4','robot-generation-3','catalog-robot-parts','robot-heart-drop'}
    ui={'chart-family','chart-family-legacy',*old['pdf'][72:78]}
    assert (len(models),len(robots),len(ui))==(33,9,8)
    keep=models|robots|ui
    selected=[key for key in old['pdf'] if key in keep]
    assert len(selected)==50
    for theme in themes:selected.extend([f'themegrid-{theme["id"]}-0',f'themeclose-{theme["id"]}-front'])
    selected.extend(f'catalog-all-{i}' for i in range(9))
    selected.extend('eggaudit-'+key for key in ('colors','jewels','special','close-nether','close-end','close-shiny','close-moon','themes-shiny-0','themes-shiny-1'))
    selected.extend(f['key'] for f in new if f['category'] in {'proportion-audit','lifecycle-audit','grade-audit','merchant-audit'} or f['key'].startswith('themeaudit-'))
    p['pdf']=selected
    p['required_jar_entries']+=['assets/chao_garden/models/entity/chao_cocoon.json','assets/chao_garden/variant_native_atlases_022.json',
        *[f'assets/chao_garden/textures/entity/chao/cocoon_{k}.png' for k in ('evolution','rebirth','death')]]
    assert len(p['features'])==345 and len(p['capture_order'])==465 and len(set(p['capture_order']))==465 and len(p['pdf'])==135
    (ROOT/'docs/release-scenes-0.22.0.json').write_text(json.dumps(p,indent=2)+'\n')
    review=dict(release='0.22.0',result='PLAN_ONLY',static_views=345,raw_motion_frames=120,pdf_pages=135,assignments={})
    for role in ('proportions','variants','items-eggs','lifecycle','ui-companions'):
        keys=[]
        for feature in p['features']:
            key,category=feature['key'],feature['category']
            if category=='themed-forms':owner='variants'
            elif category in {'item-audit','egg-audit'}:owner='items-eggs'
            elif category in {'lifecycle-audit','grade-audit'} or category.startswith('robot-'):owner='lifecycle'
            elif category in {'inspector','companion-curves','inspector-growth','companion-live'}:owner='ui-companions'
            else:owner='proportions'
            if owner==role:keys.append(key)
        review['assignments'][role]=dict(static_keys=keys,pdf_pages=[i+1 for i,key in enumerate(selected) if key in keys])
    assert sum(len(v['static_keys']) for v in review['assignments'].values())==345
    assert sorted(i for v in review['assignments'].values() for i in v['pdf_pages'])==list(range(1,136))
    path=ROOT/'docs/validation-0.22/visual-review-plan.json';path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(review,indent=2)+'\n')
    print('Release022 plan: 345 static +120 motion =465 actual captures; 135 PDF pages.')
    print({r:(len(v['static_keys']),len(v['pdf_pages'])) for r,v in review['assignments'].items()})


if __name__=='__main__':main()
