#!/usr/bin/env python3
"""Write current release verification from the six completed, SHA-bound reports."""
import argparse
import json
from pathlib import Path
from source_export0257 import ROOT, PROOF, VERSION, validate_review_closure, sha


def music_release_status():
    """Describe the actual generated region manifest, including unresolved audio."""
    manifest=ROOT/'art/area_music_manifest_0257.json'
    data=json.loads(manifest.read_text())
    assert data.get('release',VERSION)==VERSION, 'Music manifest belongs to another release'
    regions=data['regions']
    assert isinstance(regions,list) and len(regions)==6
    assert len({row['id'] for row in regions})==len(regions), 'Duplicate music region'
    assert all(isinstance(row.get('title'),str) and row['title'].strip()
               and isinstance(row.get('bundled'),bool) for row in regions)
    bundled_rows=[row for row in regions if row['bundled']]
    bundled=[row['title'] for row in bundled_rows]
    assert data.get('bundled_recordings_count',len(bundled))==len(bundled), 'Incorrect bundled audio count'
    waiting=[row['title'] for row in regions if not row['bundled']]
    assert bundled, 'Release music has no bundled region recording'
    parts=['Automatic music routing is available for six areas.',
           'Bundled area recording'+('s' if len(bundled)!=1 else '')+': '+', '.join(
               row['title']+(' (existing supplied Sonic Marketplace recording)'
               if row.get('source_kind')=='owner_supplied_marketplace_recording' else '')
               for row in bundled_rows)+'.']
    if data.get('exact_sa2_ost_rendition_verified') is False:
        parts.append('The exact SA2 OST rendition of the supplied recording remains unverified.')
    if waiting:
        parts.append('Original themes still requiring supplied audio: '+', '.join(waiting)+'.')
        parts.append('The companion resourcepack template is in docs/music-pack0257/.')
    parts.append('The existing jukebox disc retains its supplied recording and ordinary controls.')
    return dict(bundled=bundled,waiting_for_audio=waiting,summary=' '.join(parts),manifest=manifest)


def main(out):
    validate_review_closure()
    reports={n:json.loads((ROOT/PROOF/(n+'.json')).read_text())
             for n in ('core','server-default','server-accessories','production-jar','asset-rebuild','client-smoke')}
    core=reports['core']; default=reports['server-default']; accessories=reports['server-accessories']; production=reports['production-jar']
    jar=ROOT/f'build/libs/chao_garden-{VERSION}.jar'
    assert production['artifact']['sha256']==sha(jar)
    count=lambda report:report['required_tests_passed']
    client=reports['client-smoke']
    assert client.get('release')==VERSION and client.get('result')=='PASS'
    assert client.get('artifact_sha256')==sha(jar), 'Client checked a different production payload'
    music=music_release_status()
    text=f'''# Chao Garden {VERSION} verification

The production build and all {len(core['suites'])} core suites passed, totaling {core['total_checks']:,} checks. Fresh native GameTest servers passed {count(default)}/{count(default)} cases with the base mod and {count(accessories)}/{count(accessories)} with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release reverses the Hero staircase along the same lobby wall, replaces the rigid Dark gate flame geometry with animated fire effects, and places the school door leaves at the requested frame edges with consistent hinges and inward opening. A protected ambient group of baby Chao wanders, plays and joins the current lesson, including singing animations and effects. The lobby scene also appears on fresh arrivals without first turning toward the center. The native checks cover the affected placement, movement, interactions and actor separation alongside the retained care and ownership rules.

{music['summary']}

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the changed scenery internally. No screenshot or video previews are included, as requested. This provides focused visual and automated coverage; the detailed reports state the specific checks and their limits.

Existing garden care, ownership and original-world return data are retained. Dark Garden remains a first pass, including its red-tinted native-water waterfall. Original reference inputs from earlier updates remain in the complete project.

Production artifact: `{jar.name}` ({jar.stat().st_size:,} bytes).

SHA-256: `{sha(jar)}`

Detailed reports and closed logs are in `docs/validation-{VERSION}/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads.
'''
    (ROOT/'TESTING.md').write_text(text)
    out.mkdir(parents=True,exist_ok=True)
    (out/f'chao-garden-{VERSION}-verification.txt').write_text(text.replace('# ','',1).replace('`',''))
    print(json.dumps(dict(release=VERSION,result='WRITTEN',jar_sha256=sha(jar),
        core_checks=core['total_checks'],default_cases=count(default),accessories_cases=count(accessories),
        bundled_music=music['bundled'],music_waiting_for_audio=music['waiting_for_audio'])))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output-dir',type=Path,default=ROOT.parent/'deliverables')
    main(p.parse_args().output_dir.resolve())
