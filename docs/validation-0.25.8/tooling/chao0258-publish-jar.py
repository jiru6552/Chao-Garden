#!/usr/bin/env python3
"""Publish only closed, independently verified archive bytes across the mirrored workspace."""
from pathlib import Path
import hashlib,json,os,sys,time,zipfile
from chao_artifact_io import write_json_atomic
source=Path(sys.argv[1]).resolve();root=Path(sys.argv[2]).resolve();base=root.parent

def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def verify(p):
 with zipfile.ZipFile(p) as z:
  assert z.testzip() is None and len(z.namelist())==len(set(z.namelist()))
 return sha(p)
def fsync_dir(p):
 fd=os.open(p,os.O_RDONLY|os.O_DIRECTORY)
 try:os.fsync(fd)
 finally:os.close(fd)
expected=verify(source);payload=source.read_bytes();assert hashlib.sha256(payload).hexdigest()==expected
canonical_dir=source.parent/'immutable';canonical_dir.mkdir(exist_ok=True)
canonical=canonical_dir/f'chao_garden-0.25.8-{expected}.jar'
if not canonical.exists():
 temp=canonical.with_suffix('.jar.tmp')
 with temp.open('wb') as f:f.write(payload);f.flush();os.fsync(f.fileno())
 assert verify(temp)==expected;os.replace(temp,canonical);fsync_dir(canonical.parent)
assert verify(canonical)==expected
stage_dir=base/'.rsync-tmp'/'chao0258-publish';stage_dir.mkdir(parents=True,exist_ok=True)
published_dir=base/'validated0258';published_dir.mkdir(exist_ok=True)
immutable=published_dir/canonical.name;standard=root/'build/libs/chao_garden-0.25.8.jar';standard.parent.mkdir(parents=True,exist_ok=True)
for target in (immutable,standard):
 temp=stage_dir/(target.name+'.tmp')
 with temp.open('wb') as f:f.write(payload);f.flush();os.fsync(f.fileno())
 assert verify(temp)==expected
 os.utime(temp,None);os.replace(temp,target);fsync_dir(target.parent)
 assert verify(target)==expected
report=dict(release='0.25.8',result='PASS',sha256=expected,size_bytes=len(payload),canonical_outside_synced_workspace=str(canonical),immutable_published=str(immutable),standard_published=str(standard),published_wall_clock_ns=time.time_ns(),method='Gradle produces outside scratch; closed bytes independently CRC/hash verified and fsynced. Distinct canonical copy outside scratch. New SHA-qualified publication and standard path use .rsync-tmp staging plus atomic rename/fresh timestamps. No hardlinks; future Gradle outputs remain outside scratch.')
write_json_atomic(root/'docs/validation-0.25.8/archive-publication.json',report)
print(json.dumps(report,indent=2))
