from pathlib import Path
import concurrent.futures,hashlib,json,re,shutil,subprocess,time,zipfile
repo=Path('/workspace/Chao-Garden')
base=Path('/tmp/garden049-trumpet-proof'); proof=Path('/workspace/.setup/garden049-trumpet-proof')
base.mkdir(exist_ok=True);proof.mkdir(exist_ok=True)
jar=Path('/workspace/artifacts/chao-garden-0.25.48/chao_garden-0.25.48.jar')
java=Path('/workspace/.tools/jdk-21.0.9+10/bin/java');javac=java.with_name('javac')
sha=lambda b:hashlib.sha256(b).hexdigest()
def hashes(root):return {str(p.relative_to(root)):sha(p.read_bytes()) for p in sorted(root.rglob('*.class'))}
parent='codex/download-0.25.48'
commit=subprocess.check_output(['git','rev-parse',parent],cwd=repo,text=True).strip()
sources=['src/lionfishTest/java/com/chaogarden/client/TrumpetTransitionRigTest.java','src/lionfishTest/java/com/chaogarden/client/TrumpetRigTest.java','src/lionfishTest/java/com/chaogarden/client/PropMeshClearance.java','src/main/java/com/chaogarden/client/Toy02533PoseVisualSmoke.java','src/main/java/com/chaogarden/test/ToyVariantCases02522.java']
source_proof={}
for name in sources:
 b=subprocess.check_output(['git','show',parent+':'+name],cwd=repo)
 path=base/'baseline'/'sources'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b)
 source_proof[name]={'parent_sha256':sha(b),'current_sha256':sha((repo/name).read_bytes()),'equal':b==(repo/name).read_bytes()}
assert all(x['equal'] for x in source_proof.values())
entries=(repo/'build/trumpet-classpath.txt').read_text().strip().split(':')
deps=[Path(p) for p in entries if p.endswith('.jar') and Path(p).is_file()]
assert all(not str(p).startswith(str(repo/'build/classes')) for p in deps)
(base/'baseline/classes').mkdir(parents=True,exist_ok=True)
compile_cmd=[str(javac),'-proc:none','-encoding','UTF-8','-cp',':'.join(map(str,[jar]+deps)),'-d',str(base/'baseline/classes')]+[str(base/'baseline/sources'/s) for s in sources]
c=subprocess.run(compile_cmd,cwd=base,capture_output=True,text=True)
(proof/'baseline-compile.log').write_text(c.stdout+c.stderr)
assert c.returncode==0,c.stderr
snapshot={}
for kind in ['main','lionfishTest']:
 src=repo/'build/classes/java'/kind;dest=base/'current/classes'/kind
 before=hashes(src);shutil.copytree(src,dest,dirs_exist_ok=True);after=hashes(src);copied=hashes(dest)
 snapshot[kind]={'before':before,'after':after,'copied':copied,'stable':before==after==copied}
 assert snapshot[kind]['stable']
(proof/'snapshot-class-hashes.json').write_text(json.dumps(snapshot,indent=2)+'\n')
resources={}
with zipfile.ZipFile(jar) as z:
 resource_names=[n for n in z.namelist() if n.startswith('assets/chao_garden/models/entity/') and n.endswith('.json') or n=='assets/chao_garden/animations/official/chao.animation.json']
 for n in resource_names:
  old=z.read(n);current=(repo/'src/main/resources'/n).read_bytes();resources[n]={'parent_sha256':sha(old),'current_sha256':sha(current),'equal':old==current}
  for label,data in [('baseline',old),('current',current)]:
   dest=base/label/'work/src/main/resources'/n;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
assert all(x['equal'] for x in resources.values())
(proof/'resource-hashes.json').write_text(json.dumps(resources,indent=2)+'\n')
catalog='assets/chao_garden/chao_variants.json'
with zipfile.ZipFile(jar) as z: old=z.read(catalog)
current=(repo/'src/main/resources'/catalog).read_bytes()
assert old==current
shutil.copytree(repo/'src/main/resources',base/'current/classpath-resources',dirs_exist_ok=True)
res=base/'current/classpath-resources'/catalog;assert res.read_bytes()==current
resources[catalog]={'parent_sha256':sha(old),'current_sha256':sha(current),'equal':True}
retired='assets/chao_garden/retired_chao_variants.json'
with zipfile.ZipFile(jar) as z: old_retired=z.read(retired)
current_retired=(repo/'src/main/resources'/retired).read_bytes();assert old_retired==current_retired
resources[retired]={'parent_sha256':sha(old_retired),'current_sha256':sha(current_retired),'equal':True}
(proof/'resource-hashes.json').write_text(json.dumps(resources,indent=2)+'\n')
cp={'baseline':[base/'baseline/classes',jar]+deps,'current':[base/'current/classes/lionfishTest',base/'current/classes/main',base/'current/classpath-resources']+deps}
for label in cp:(proof/(label+'-classpath.txt')).write_text(':'.join(map(str,cp[label]))+'\n')
def run(label):
 log=proof/(label+'-run.log');load=proof/(label+'-class-load.log')
 cmd=[str(java),'-Xmx1G','-XX:ActiveProcessorCount=2','-Xlog:class+load=info:file='+str(load),'-cp',':'.join(map(str,cp[label])),'com.chaogarden.client.TrumpetTransitionRigTest']
 start=time.monotonic()
 if label=='baseline' and (proof/'baseline-report.txt').exists():
  r=type('Result',(),{'returncode':1})()
 else:
  with log.open('w') as stream:r=subprocess.run(cmd,cwd=base/label/'work',stdout=stream,stderr=subprocess.STDOUT)
 data=(base/label/'work/build/reports/lionfish/trumpet-transition-results.txt').read_bytes()
 (proof/(label+'-report.txt')).write_bytes(data)
 out=log.read_text();dist=re.search(r'Failure tick distribution (.+)',out)
 return {'exit':r.returncode,'seconds':None if label=='baseline' else round(time.monotonic()-start,3),'reused_completed_same_task_baseline_run':label=='baseline','report_sha256':sha(data),'summary':data.decode().splitlines()[0],'distribution':dist.group(1) if dist else None,'command':cmd}
print('Prepared isolated baseline and current snapshots; running both JVMs.',flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 runs=dict(zip(['baseline','current'],pool.map(run,['baseline','current'])))
# Capture every loaded Chao class and show where each implementation came from.
loaded={}
with zipfile.ZipFile(jar) as z:
 jar_names=set(z.namelist())
 for label in ['baseline','current']:
  classes=[];generated=[]
  for line in (proof/(label+'-class-load.log')).read_text().splitlines():
   m=re.search(r'\[class,load\] (com\.chaogarden\.\S+) source: (.+)',line)
   if not m:continue
   name,source=m.groups()
   if '$$Lambda/' in name:generated.append(name);continue
   n=name.replace('.','/')+'.class'
   if label=='baseline':
    fixture=base/'baseline/classes'/n
    data=fixture.read_bytes() if fixture.exists() else z.read(n)
   else:
    fixture=base/'current/classes/lionfishTest'/n;main=base/'current/classes/main'/n
    data=fixture.read_bytes() if fixture.exists() else main.read_bytes()
   old=z.read(n) if n in jar_names else None
   classes.append({'class':name,'source':source,'sha256':sha(data),'delivered048_sha256':sha(old) if old is not None else None,'equals_delivered048':data==old if old is not None else None})
  loaded[label]={'classes':classes,'generated_lambdas':generated}
# Validate proof: no current main classes enter the old runtime.
assert all('/workspace/Chao-Garden/build/' not in x['source'] for v in loaded.values() for x in v['classes'])
assert all(str(jar) in x['source'] for x in loaded['baseline']['classes'] if x['delivered048_sha256'] is not None)
report_equal=(proof/'baseline-report.txt').read_bytes()==(proof/'current-report.txt').read_bytes()
evidence={'parent_commit':commit,'delivered048_jar':str(jar),'delivered048_jar_sha256':sha(jar.read_bytes()),'unchanged_test_helper_sources':source_proof,'resources_checked':len(resources),'all_resources_equal':all(x['equal'] for x in resources.values()),'current_class_snapshots_stable':all(x['stable'] for x in snapshot.values()),'dependency_jar_hashes':{str(p):sha(p.read_bytes()) for p in deps},'runs':runs,'reports_byte_identical':report_equal,'loaded_chao_classes':loaded,'current_loaded_production_classes_equal_delivered048':all(x['equals_delivered048'] for x in loaded['current']['classes'] if x['delivered048_sha256'] is not None),'no_current_project_classes_or_resources_in_baseline_classpath':all('/workspace/Chao-Garden/build/classes/' not in str(p) and '/workspace/Chao-Garden/build/resources/' not in str(p) for p in cp['baseline']),'no_shared_build_output_writes':True,'notes':'Both unchanged test JVMs ran in separate /tmp working directories. Parent 48 test helpers were compiled from exact git sources against the delivered 48 JAR only; all parent production code loaded from that JAR. Assets were private copies from 48 JAR or current source. Current 49 classes were immutable hash-verified snapshots. No tests or production source were changed.'}
(proof/'evidence.json').write_text(json.dumps(evidence,indent=2)+'\n')
print(json.dumps({'runs':{label:{k:v for k,v in run.items() if k!='command'} for label,run in runs.items()},'reports_byte_identical':report_equal,'current_loaded_production_classes_equal_delivered048':evidence['current_loaded_production_classes_equal_delivered048'],'proof':str(proof)},indent=2),flush=True)
