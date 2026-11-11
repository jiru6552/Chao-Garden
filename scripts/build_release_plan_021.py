#!/usr/bin/env python3
"""Declare actual-client0.21 views; the same plan drives capture, gallery and PDF."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
ALIGN=['Neutral','Hero','Dark'];TYPE=['Normal','Swim','Fly','Run','Power']
features=[];pdf=[]
def add(key,title,caption,category,scene=None,page=False):
 features.append(dict(key=key,title=title,subtitle='Actual Minecraft Java 1.21.1 / NeoForge capture',caption=caption,category=category,scene=scene or key))
 if page:pdf.append(key)
for a,align in enumerate(ALIGN):
 for p,primary in enumerate(TYPE):
  add(f'forms-{a}-{p}',f'{align}/{primary}: fully adult secondary forms','The first adult alignment/type stays fixed. Bottom to top: Swim(-1), Normal(0), Fly(+1). Left to right: Run(-1), Normal(0), Power(+1). Nine valid secondary targets at maturity1.20; each opposing pair is one signed axis.','adult-endpoints',page=(a,p) in [(0,0),(0,3),(1,2),(2,4)])
  for angle in ['front','side']:
   add(f'growth-{a}-{p}-{angle}',f'{align}/{primary}: young to fully adult ({angle})','Left to right: young adult(0.00), half mature(0.60), fully adult(1.20). The first adult type remains fixed; the same secondary training direction gradually changes the actual model. Normal uses its aged center target.','gradual-growth',page=(a,p,angle) in [(0,1,'side'),(0,2,'front'),(0,3,'side'),(1,0,'front'),(1,2,'side'),(2,2,'front'),(2,4,'front')])
for a,align in enumerate(ALIGN):
 for p in range(1,5):
  opposite={1:2,2:1,3:4,4:3}[p]
  add(f'opposite-{a}-{p}-side',f'{align}/{TYPE[p]} to {TYPE[opposite]} secondary','Left: original young-adult form. Right: the same fixed first type after full maturity and opposing later training. A first Swim type may develop a Fly secondary(and vice versa); a first Run may develop Power. The prohibition applies within the current secondary stage.','opposite-secondary',page=(a,p) in [(0,2),(1,1),(2,3)])
 for combo,mixed in enumerate(['Swim+Run','Swim+Power','Fly+Run','Fly+Power']):
  add(f'mixed-{a}-0-{combo}-front',f'{align}/Normal/{mixed}: gradual mixed form','Left to right: young, half mature, fully adult. The secondary combines one Swim/Fly choice and one Run/Power choice. Swim+Fly and Run+Power cannot be simultaneous secondary types.','valid-mixtures',page=a==0)
 for angle in ['front','side']:
  add(f'reverse-{a}-2-{angle}',f'{align}/Fly: reversible secondary ({angle})','Three fully mature Chao with the same locked Fly first type. Left to right: Swim secondary, balanced Normal secondary, Fly secondary. Later feeding/training can move through this continuum without a second cocoon or replacing the first type.','secondary-reversal',page=a==0)
for p,primary in enumerate(TYPE):
 add(f'baby-{p}-front',f'Child {primary} tendency: gentle shaping','Left to right: newly hatched, middle childhood, late childhood. Training tendencies remain changeable. The first adult choice is made at first evolution from the greatest trained stat; the most recently increased tied stat wins. These age values are controlled preview fixtures.','child-tendencies',page=p in [2,3])
chart_names={'status':'Status','genes':'Genes','parts':'Animal parts','dna':'Additional animal DNA','profile':'Personality','feelings':'Feelings','health':'Health','family':'Named family','family-legacy':'Legacy family','doctor':'Doctor','lessons':'Lessons','wardrobe':'Wardrobe'}
for name,title in chart_names.items():
 add('chart-'+name,title+' inspection','Live Inspector screen with readable typing, saved family names and a clean lower panel. Legacy ancestry without a saved name is labeled explicitly.','inspector',page=name in ['status','genes','family','family-legacy','wardrobe'])
 if name=='status':features[-1].update(title='Child maturity and prospective first type',caption='A child at strength0.75 shows75% of its1.00 first-evolution clock. The actual maturity tooltip identifies Fly as its current highest-stat prospective first type; childhood appearance remains mutable. Adults use a separate1.20 gradual clock.')
for age,label in enumerate(['young adult','half mature','fully adult']):
 add(f'chart-growth-{age}',f'Gradual evolution chart: {label}','The fixed first adult type and mutable secondary type are separate from maturity. The lower meter shows evolution strength, not remaining lifespan. This Fly first type is trained toward Swim+Power.','inspector-growth',page=True)
for stat,title in enumerate(['Swim','Fly','Run','Power']):
 for level in [0,50,99]:
  add(f'chart-companion-{stat}-{level}',f'{title} companion ability: level {level}','Actual skill tooltip reads the implemented companion curves. Garden movement milestones use points; companion bonuses use trained levels. Bonuses require an equipped, active, energized adult.','companion-curves',page=level in [0,99])
for generation,rank in enumerate('CBAS'):
 add(f'chart-robot-family-{generation}',f'Omochao {rank} rank: creator family','The robot was built through the actual four-part assembler. Each later generation uses the single physical heart returned by the previous cared-for robot. New creator is parent; old creator is grandparent. Lifespans are fast-forwarded in this preview.','robot-family',page=True)
for name,title in [('health','Illness immunity'),('genes','Animal-gene exclusion')]:
 add('chart-robot-'+name,'Omochao: '+title,'Omochao starts as a young adult, cannot become ill and cannot gain animal stats or animal parts. Drives, fruit, lessons and activities remain training options.','robot-care',page=True)
for a,align in enumerate(ALIGN):
 for p,primary in enumerate(TYPE):
  for angle in ['front','side','back']:
   add(f'accessory-{a}-{p}-{angle}',f'{align}/{primary}: fitted wardrobe ({angle})','Actual cap, glasses, collar, hoodie and medal on the fully mature mixed endpoint. Fitting follows sampled skull, iris, torso and upper-arm geometry. Caps hide native and authored crests while preserving skull and eyes.','accessory-fit',page=(a,p,angle) in [(0,0,'front'),(1,1,'side'),(1,3,'back'),(2,2,'side'),(2,4,'front')])
for angle in ['front','side','back']:
 add(f'accessory-child-{angle}',f'Child wardrobe ({angle})','Actual wearables on a young child. Clothing uses the same sampled anatomical contacts as adult forms.','accessory-fit',page=angle=='front')
for key,title in [('amy','Amy'),('omochao','Omochao'),('chaos0','Light Chaos'),('chaos1','Angel Chaos'),('chaos2','Devil Chaos')]:
 for angle in ['front','side','back']:
  add(f'accessoryspecial-{key}-{angle}',f'{title}: fitted wardrobe ({angle})','Actual worn accessories use the character anatomy. A closed cap hides matching opaque and glass head decorations; hands and feet remain visible.','accessory-special',page=(key,angle) in [('amy','side'),('omochao','front'),('chaos1','front')])
for index,title in enumerate(['Normal two-tone','Red monotone','Blue two-tone','Shiny pink','Silver jewel','Moon texture','Negative palette','Bright shiny coat']):
 add(f'robot-coat-{index}','Omochao: '+title,'Builder left, completed robot right. The actual assembler copies the builder coat onto the mechanical rig; fixed hardware and one spiral-eye pair retain their materials. The robot begins with all C grades and fresh levels.','robot-coats',page=index in [0,3,4,5])
for stage in range(1,5):
 add(f'robot-stage-{stage}',f'Omochao construction: part {stage} of 4','The actual same owned prototype progresses through four stages. Unfinished prototypes stay protected; completion releases normal robot behavior. A component or one calibrated heart can replace a Parts Box.','robot-construction',page=stage in [1,2,4])
for generation,rank in enumerate('CBAS'):
 add(f'robot-generation-{generation}',f'Reconstructed Omochao: all {rank} grades','Genuine assembler/lifecycle chain. A single care heart carries the former creator record and raises every grade by one(C to B to A to S, capped S). The new robot starts with fresh levels and its new builder as parent.','robot-generations',page=generation in [1,3])
add('catalog-robot-parts','Robot construction and recovery items','Parts Box, reusable Robot Component, calibrated Robo Heart and a training drive. The heart tooltip records the next grade and former creator. Components/hearts are lifecycle outputs, not ordinary market stock.','robot-items',page=True)
for key,title in [('component','Robot Component after breakdown'),('heart','Robo Heart after cared-for expiry')]:
 add('robot-'+key+'-drop',title,'The actual terminal lifecycle produces one physical item and removes the ended robot. Here its configured lifespan is fast-forwarded for the preview. Recovery preserves item conservation and allows reconstruction.','robot-recovery',page=key=='heart')
for angle in ['front','side','back']:
 add('amy-'+angle,'Amy Chao ('+angle+')','The retained refined pink head, ears, fringe/quills, red torso and white collar use the inherited animated skeleton.','characters',page=angle=='front')
for a,align in enumerate(['Light','Angel','Devil']):
 for angle in ['front','oblique','side','back']:
  add(f'chaos-{a}-{angle}',f'{align} Chaos anatomy ({angle})','Refined central downward side pieces, broader Hero appendages or stronger Dark horns distinguish the three Chaos forms. The opaque checker makes two-tone tip transparency visible; the body remains opaque.','chaos',page=angle in ['front','oblique'])
for index,title in [(3,'Shiny pink'),(4,'Silver jewel'),(5,'Moon texture')]:
 add('chaoscoat-'+str(index),title+' monotone Chaos','Inherited coat/shine remain visible. These monotone examples retain opaque tips; the two-tone examples demonstrate the icy translucent terminal surfaces.','chaos-coats',page=index==5)
add('characters-retained','Retained special characters','Tails, Amy, Knuckles and Omochao remain available. Ordinary eggs and the removed character migrations remain unchanged.','characters',page=False)
for level in [0,70,99]:
 add(f'companion-water-{level}',f'Actual water companion: Swim level {level}','Equipped through the real Accessories slot. The live player is submerged in actual water; the ordinary Inventory screen shows active effects. Night vision begins at70; water breathing at99. Speeds use the same level-scaled server modifiers.','companion-live',page=True)
add('companion-land-cleanup','Water benefits end on land','A real Swim99 companion first grants both effects underwater, then the player moves onto land. Only the water effects granted by this companion are removed; the screenshot waits for the client to confirm cleanup.','companion-live',page=True)
add('companion-flight-99','Actual max-Fly elytra flight','An actual level99 Fly Chao is equipped in the real Accessories slot. Vanilla fall-flying admission and travel run with no chest elytra item and no forced flight flag.','companion-live',page=True)
add('companion-robot-boost','Actual S-rank Omochao boost','An all-S robot is equipped through the real slot. Vanilla elytra flight admits it, then the real server boost succeeds; an immediate replay is rejected and the100-tick cooldown is asserted.','companion-live',page=True)
motions=[]
for key,title,caption,scene in [
 ('gradual-age','A young adult gradually matures','Controlled evolution-strength progression0.00 to1.20 through the actual live renderer; fixed first type Fly. This is a preview of the deformation, not a claim that natural aging lasts five seconds.','motion-gradual'),
 ('secondary-reverse','A mature secondary changes direction','Controlled signed secondary training influence+1 to-1 on one actual mature Fly first-type Chao. The first type remains fixed throughout.','motion-secondary'),
 ('accessory-walk','Wearables during a real walking route','Actual navigation and sampled animation keep cap, glasses, collar, hoodie/sleeves and medal attached.','motion-accessory-walk'),
 ('robot-motion','Mechanical Omochao motion','Actual robot material and animated rotor/key on a constructed young-adult robot.','motion-robot'),
 ('chaos-orbit','Ice from changing camera angles','Actual translucent hand, foot and head tips against contrasting opaque terrain.','motion-chaos-orbit')]:
 motions.append(dict(key=key,title=title,caption=caption,scene=scene,frames=[f'{key}-{i:02d}' for i in range(24)]))
 if key=='accessory-walk':
  motions[-1]['review_frames']=motions[-1]['frames'][:22]
  motions[-1]['omission_reason']='The final two captured walking moments reach or cross the static camera boundary. All 24 raw captures remain preserved; the review uses the first 22 complete views.'
plan=dict(release='0.21.0',log_prefix='CHAO_GRADUAL021',filename_prefix='chao021-',features=features,motions=motions,capture_order=[f['key'] for f in features]+[frame for m in motions for frame in m['frames']],pdf=pdf,required_jar_entries=[
 'com/chaogarden/core/GradualEvolutionRules.class','com/chaogarden/core/OmochaoRules.class','com/chaogarden/cwe/OmochaoLifecycle.class','com/chaogarden/cwe/CweRobotPartItem.class','com/chaogarden/companion/CompanionRules.class','com/chaogarden/client/ChaoChaosGlassLayer.class','com/chaogarden/client/OmochaoMaterial.class','assets/chao_garden/models/entity/gameplay/chao_chaos_light.json','assets/chao_garden/models/entity/gameplay/chao_chaos_angel.json','assets/chao_garden/models/entity/gameplay/chao_chaos_devil.json'])
def readable_copy(value):
 value=re.sub(r'(?<=[a-z])(?=[0-9]|[+-][0-9])',' ',value)
 value=re.sub(r'(?<=[0-9])(?=[A-Za-z])',' ',value)
 value=re.sub(r'(?<=[A-Za-z])(?=\()',' ',value)
 return value.replace('allthree','all three')
for item in features+motions:
 for field in ['title','subtitle','caption','pdf_title','pdf_subtitle','pdf_caption']:
  if field in item:item[field]=readable_copy(item[field])
(ROOT/'docs/release-scenes-0.21.0.json').write_text(json.dumps(plan,indent=2)+'\n')
print(f'{len(features)} feature views + {sum(len(m["frames"]) for m in motions)} motion frames = {len(plan["capture_order"])} captures; {len(pdf)} PDF pages')
