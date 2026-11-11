#!/usr/bin/env python3
"""Install the eleven converted, hash-bound tracks from the supplied ADX archive."""
from pathlib import Path
import hashlib,json,shutil

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/chao_garden'

def main():
    report=json.loads((ROOT/'art/audio02511/import-report.json').read_text())
    assert len(report['recordings'])==11 and report['classroom_chorus_count']==6
    sounds=json.loads((ASSETS/'sounds.json').read_text())
    chorus=[]
    for row in report['recordings']:
        source=ROOT/row['canonical_file'];original=ROOT/'art/audio02511/source-adx'/row['source_file'].lower()
        assert hashlib.sha256(source.read_bytes()).hexdigest()==row['sha256']
        assert hashlib.sha256(original.read_bytes()).hexdigest()==row['source_sha256']
        target=RES/row['asset'];target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
        resource='chao_garden:'+row['asset'].removeprefix('assets/chao_garden/sounds/').removesuffix('.ogg')
        entry={'name':resource,'stream':True}
        if row['kind']=='area':
            event='music.area.'+row['id']
            sounds[event]={'subtitle':'subtitles.chao_garden.'+event,'sounds':[entry]}
        else:
            entry['attenuation_distance']=12
            chorus.append(entry)
    sounds['chao.classroom_chorus']={'sounds':chorus}
    (ASSETS/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
    path=ROOT/'art/area_music_manifest_0257.json';manifest=json.loads(path.read_text())
    for row in manifest['regions']:
        imported=next((r for r in report['recordings'] if r['kind']=='area' and r['id']==row['id']),None)
        if imported:
            row.update(bundled=True,source_kind='user_supplied_sa2_adx',asset=imported['asset'],sha256=imported['sha256'],
                       source_file=imported['source_file'],source_sha256=imported['source_sha256'],duration_seconds=imported['duration_seconds'])
    manifest.pop('exact_sa2_ost_rendition_verified',None)
    manifest.update(release='0.25.11',bundled_recordings_count=5,supplied_adx_conversion_verified=True,
                    missing_regional_track_ids=['black_market'],
                    scope='Five region themes converted from the supplied ADX archive. Provenance and codec/waveform verification are recorded in art/audio02511; no listening claim. The original jukebox recording is retained.')
    path.write_text(json.dumps(manifest,indent=2)+'\n')
    print('Supplied ADX02511: five area themes and six positional classroom singing streams included; jukebox/voices retained.')

if __name__=='__main__':main()
