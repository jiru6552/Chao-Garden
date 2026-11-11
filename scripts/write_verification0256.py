#!/usr/bin/env python3
"""Write current release verification from completed reports, without historical claims."""
import argparse
import json
from pathlib import Path
from source_export0256 import ROOT, PROOF, VERSION, validate_review_closure, sha


def main(out):
    validate_review_closure()
    reports={n:json.loads((ROOT/PROOF/(n+'.json')).read_text()) for n in ('core','server-default','server-accessories','production-jar','asset-rebuild')}
    core=reports['core']; default=reports['server-default']; accessories=reports['server-accessories']; production=reports['production-jar']
    jar=ROOT/f'build/libs/chao_garden-{VERSION}.jar'
    assert production['artifact']['sha256']==sha(jar)
    count=lambda report:report['required_tests_passed']
    client_path=ROOT/PROOF/'client-smoke.json'
    assert client_path.is_file(), 'Native client bake and focused internal review still pending'
    client=json.loads(client_path.read_text())
    assert client.get('release')==VERSION and client.get('result')=='PASS'
    assert client.get('artifact_sha256')==sha(jar), 'Client checked a different production payload'
    text=f'''# Chao Garden {VERSION} verification

The production build and all {len(core['suites'])} core suites passed, totaling {core['total_checks']:,} checks. Fresh native GameTest servers passed {count(default)}/{count(default)} cases with the base mod and {count(accessories)}/{count(accessories)} with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release moves both garden stairways along the lobby wall, joins their railings at turns and landings, removes the obsolete stair support blocks, and rebuilds the Hero and Dark gates from the supplied game pictures. It also attaches Dark fruit to visible branches, restricts tombstone engravings to the front and back, and corrects the Ionic capital and shaft clearances. Existing IDs, garden care data and original-world return journals are retained. The native suite checks placement, movement, interactions and ownership alongside the asset and geometry guards.

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource-domain, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the changed scenery internally. No screenshot/video previews are included, as requested. This is focused visual and automated coverage; it does not claim exhaustive manual gameplay or a perfect reconstruction of the original game.

The attached Hero and Dark gate pictures are the primary visual references for this update. Dark Garden remains a first pass: its native-water waterfall shares the red pool tint. Its custom storm sky hides the vanilla sun and moon while logical daytime skylight preserves ordinary crop and fruit growth.

Production artifact: `{jar.name}` ({jar.stat().st_size:,} bytes).

SHA-256: `{sha(jar)}`

Detailed reports and closed logs are in `docs/validation-{VERSION}/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads. The user-supplied feedback and gate reference pictures are retained in `docs/references-{VERSION}/`.
'''
    (ROOT/'TESTING.md').write_text(text)
    out.mkdir(parents=True,exist_ok=True)
    (out/f'chao-garden-{VERSION}-verification.txt').write_text(text.replace('# ','',1).replace('`',''))
    print(json.dumps(dict(release=VERSION,result='WRITTEN',jar_sha256=sha(jar),core_checks=core['total_checks'],default_cases=count(default),accessories_cases=count(accessories))))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output-dir',type=Path,default=ROOT.parent/'deliverables')
    main(p.parse_args().output_dir.resolve())
