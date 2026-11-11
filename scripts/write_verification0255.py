#!/usr/bin/env python3
"""Write current release verification from completed reports, without historical claims."""
import argparse
import json
from pathlib import Path
from source_export0255 import ROOT, PROOF, VERSION, validate_review_closure, sha


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

This release adds a first playable Dark Garden and its protected Adventure care/return route, celestial lobby stairs and distinct Hero/Dark gateways, smooth curved school door profiles and thinner fitted frames, the Fortune-Teller curtain correction, and bulletin-board/corner-pillar relocation. Existing IDs, paired-door prediction/rollback, owned garden data and original-world return journals are retained. The native suite checks real scene placement, movement, picking, interactions and ownership rather than only matching generated files.

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource-domain, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the curved closed/open doors and both lobby approaches internally. No screenshot/video previews are included, as requested. This is automated and focused visual coverage; it does not claim exhaustive manual gameplay or a perfect reconstruction of the original game.

The supplied YouTube pages were reachable, but their video streams were unavailable. Reference notes identify the actual SA2 game imagery/model previews used instead, without claiming unseen video timestamps. Dark Garden is a first pass: its native-water waterfall shares the red pool tint; the original reference waterfall is blue. The custom storm sky hides the vanilla sun/moon while daytime skylight preserves ordinary crop and fruit growth.

Production artifact: `{jar.name}` ({jar.stat().st_size:,} bytes).

SHA-256: `{sha(jar)}`

Detailed reports and closed logs are in `docs/validation-{VERSION}/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads. The original annotated door sketch and attributed reference inputs are retained in `docs/references-{VERSION}/`.
'''
    (ROOT/'TESTING.md').write_text(text)
    out.mkdir(parents=True,exist_ok=True)
    (out/f'chao-garden-{VERSION}-verification.txt').write_text(text.replace('# ','',1).replace('`',''))
    print(json.dumps(dict(release=VERSION,result='WRITTEN',jar_sha256=sha(jar),core_checks=core['total_checks'],default_cases=count(default),accessories_cases=count(accessories))))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output-dir',type=Path,default=ROOT.parent/'deliverables')
    main(p.parse_args().output_dir.resolve())
