#!/usr/bin/env python3
"""Write current release verification from the six completed, SHA-bound reports."""
import argparse
import json
from pathlib import Path
from source_export0258 import ROOT, PROOF, VERSION, validate_review_closure, sha


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



def singing_release_status():
    """Describe the generated vocal adaptation without claiming a game recording."""
    manifest=ROOT/'art/school_singing_manifest_0258.json'
    data=json.loads(manifest.read_text())
    assert data.get('release',VERSION)==VERSION, 'Singing manifest belongs to another release'
    assert data['source_kind']=='original_melody_from_provided_chao_voice', 'Unreviewed singing provenance'
    assert data['exact_sa2_recording'] is False, 'Singing disclosure needs review for the actual source'
    phrases=data['phrases']
    assert isinstance(phrases,list) and {p['id'] for p in phrases}=={'a','b'} and len(phrases)==2, 'Missing or duplicate singing phrases'
    resources=ROOT/'src/main/resources'
    source=resources/data['source_voice']
    assert source.is_file() and sha(source)==data['source_sha256'], 'Provided voice source changed'
    for phrase in phrases:
        name='singing_'+phrase['id']
        assert phrase['asset']==f'assets/chao_garden/sounds/chao/{name}.ogg' and phrase['sound_event']=='chao_garden:chao.'+name, 'Wrong singing asset/event'
        asset=resources/phrase['asset']
        assert asset.is_file() and sha(asset)==phrase['sha256'], 'Generated vocal phrase changed: '+name
        assert phrase['codec']=='Vorbis' and phrase['duration_seconds']>0, 'Missing singing audio metadata'
    retained=data['existing_audio_sha256']
    assert len(retained)==6, 'Incorrect retained audio inventory'
    for relative,digest in retained.items():
        assert sha(resources/'assets/chao_garden'/relative)==digest, 'Existing voice or CD audio changed: '+relative
    return dict(manifest=manifest,summary='Song lessons use original pitched Chao-vocal phrases arranged from the supplied voice timbre, with notes and rests. These are a newly arranged adaptation rather than the original SA2 singing recording.')


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
    singing=singing_release_status()
    text=f'''# Chao Garden {VERSION} verification

The production build and all {len(core['suites'])} core suites passed, totaling {core['total_checks']:,} checks. Fresh native GameTest servers passed {count(default)}/{count(default)} cases with the base mod and {count(accessories)}/{count(accessories)} with the pinned real Accessories and owo dependencies. Both profiles used the exact final production JAR.

This release raises the Dark Garden fruit trees by one block, keeps all ambient baby Chao inside the classroom, rebuilds the classroom play-city props with separated geometry, and reverses the ornate school doors while closing their frame gaps. The door leaves retain their requested frame-edge placement. Existing care, ownership and player-student separation remain part of the native regression coverage.

{music['summary']}

{singing['summary']}

The complete asset pipeline was replayed in a separate source copy. Generated resources and Java mesh tables matched the production source. Resource, texture, geometry and malformed-input guards passed. Production classes and resources were compared byte for byte with both server-tested artifacts; development fixtures and test classes are excluded from the JAR.

A focused native client smoke check baked the models and reviewed the changed scenery internally. No screenshot or video previews are included, as requested. This provides focused visual and automated coverage; the detailed reports state the specific checks and their limits.

Existing garden care, ownership and original-world return data are retained. Dark Garden remains a first pass, including its red-tinted native-water waterfall. Original reference inputs from earlier updates remain in the complete project.

Production artifact: `{jar.name}` ({jar.stat().st_size:,} bytes).

SHA-256: `{sha(jar)}`

Detailed reports and closed logs are in `docs/validation-{VERSION}/`. The complete ZIP is checked for unique safe paths, CRC integrity, executable Gradle launcher and exact source/JAR payloads.
'''
    old_testing=ROOT/'TESTING.md'
    preserved=ROOT/'TESTING-0.25.7.md'
    if old_testing.exists() and old_testing.read_text().startswith('# Chao Garden 0.25.7 verification') and not preserved.exists():
        preserved.write_bytes(old_testing.read_bytes())
    assert preserved.is_file() and preserved.read_text().startswith('# Chao Garden 0.25.7 verification'), 'Previous verification notes were not preserved'
    old_testing.write_text(text)
    out.mkdir(parents=True,exist_ok=True)
    (out/f'chao-garden-{VERSION}-verification.txt').write_text(text.replace('# ','',1).replace('`',''))
    print(json.dumps(dict(release=VERSION,result='WRITTEN',jar_sha256=sha(jar),
        core_checks=core['total_checks'],default_cases=count(default),accessories_cases=count(accessories),
        bundled_music=music['bundled'],music_waiting_for_audio=music['waiting_for_audio'],
        singing_provenance='original_melody_from_provided_chao_voice',exact_sa2_singing_recording=False)))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output-dir',type=Path,default=ROOT.parent/'deliverables')
    main(p.parse_args().output_dir.resolve())
