#!/usr/bin/env python3
"""Run one fresh final profile, then bind its complete production bytes to the release artifact."""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys
p=argparse.ArgumentParser();p.add_argument('--root',required=True,type=Path);p.add_argument('--profile',required=True,choices=['default','accessories']);p.add_argument('--world',required=True,type=Path);p.add_argument('--log',required=True,type=Path);p.add_argument('--expected-sha256',required=True);a=p.parse_args()
root=a.root.resolve();world=a.world.resolve();log=a.log.resolve();expected=a.expected_sha256;tooling=Path(__file__).resolve().parent
assert len(expected)==64 and all(c in '0123456789abcdef' for c in expected)
assert 'mod_version=0.25.8' in (root/'gradle.properties').read_text()
assert world.is_dir() and {f.name for f in world.iterdir()}=={'eula.txt'},'A genuinely fresh disposable directory is required'
assert not log.exists() and not log.with_suffix(log.suffix+'.working').exists()
preflight=json.loads((root/'docs/validation-0.25.8/pre-server-jar.json').read_text());assert preflight['result']=='PASS' and preflight['artifact']['sha256']==expected
staging=Path('/tmp/chao0258-production-unshared');staging.mkdir(exist_ok=True)
args=['runGameTestServer',f'-PchaoGameTestDir={world}',f'-PchaoArchiveStagingDir={staging}']
if a.profile=='accessories':args+=['-PchaoVariantFixtures=true','-PchaoAccessoriesTest=true','-PchaoAccessoriesDir=/workspace/tools/dependencies']
subprocess.run([sys.executable,str(tooling/'run-chao-check.py'),str(log),str(root),*args],check=True)
jar=root/'build/libs/chao_garden-0.25.8.jar'
with jar.open('rb') as f:actual=hashlib.file_digest(f,'sha256').hexdigest()
assert actual==expected,'Production artifact changed; this profile cannot be promoted'
subprocess.run([sys.executable,str(tooling/'chao0258-record-server.py'),str(root),a.profile,str(log),str(world)],check=True)
print('CHAO_FINAL0258_PROFILE_COMPLETE',a.profile,actual,flush=True)
