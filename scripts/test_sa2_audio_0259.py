#!/usr/bin/env python3
"""Exercise real ADX decoding, pack output, failures and overwrite protection."""
from pathlib import Path
import importlib.util
import json
import subprocess
import tempfile
import zipfile
import shutil

spec=importlib.util.spec_from_file_location('sa2_import',Path(__file__).with_name('import_sa2_audio_0259.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

def main():
    with tempfile.TemporaryDirectory(prefix='chao-audio-test-') as tmp:
        root=Path(tmp);source=root/'game/resource/gd_PC/ADX';source.mkdir(parents=True)
        tone=root/'fixture.adx'
        subprocess.run(['ffmpeg','-v','error','-f','lavfi','-i','sine=frequency=440:duration=0.3',
                        '-c:a','adpcm_adx',str(tone)],check=True)
        names=list(module.AREA_FILES.values())+list(module.CHORUS_FILES)
        for name in names:shutil.copyfile(tone,source/name.upper())
        target=root/'pack.zip';result=module.build(root/'game',target)
        assert result['classroom_chorus_count']==6 and not result['missing_requested_areas']
        with zipfile.ZipFile(target) as z:
            assert z.testzip() is None
            sounds=json.loads(z.read('assets/chao_garden/sounds.json'))
            assert len(sounds['chao.classroom_chorus']['sounds'])==6
            assert all(s['stream'] for s in sounds['chao.classroom_chorus']['sounds'])
            assert 'music.chao_garden' not in sounds # Jukebox disc is not replaced.
            assert 'music.area.black_market' not in sounds # Unidentified file is not guessed.
            assert 'chao.singing_a' not in sounds # Runtime mutes fallback only when chorus is selected.
            report=json.loads(z.read('sa2-import-report.json'))
            assert len(report['recordings'])==11
            assert all(r['channels']==(1 if r['kind']=='chorus' else 2) for r in report['recordings'])
            assert not report['learned_solo_songs_imported']
            assert all(0<r['duration_seconds']<1 for r in report['recordings'])
        before=target.read_bytes()
        (source/'CHAO_HALL.ADX').unlink()
        try:module.build(source,target)
        except ValueError:pass
        else:raise AssertionError('Missing requested track accepted without partial flag')
        assert target.read_bytes()==before
        result=module.build(source,root/'partial.zip',allow_partial=True)
        assert result['missing_requested_areas']==['lobby']
        (source/'chao_g_bgm_h.adx').write_bytes(tone.read_bytes())
        try:module.locate(source)
        except ValueError:pass
        else:raise AssertionError('Ambiguous case-duplicate input accepted')
        (source/'chao_g_bgm_h.adx').unlink()
        (source/'CHAO_G_BGM_H.ADX').write_bytes(b'not audio')
        try:module.build(source,target,allow_partial=True)
        except subprocess.SubprocessError:pass
        else:raise AssertionError('Corrupt game recording accepted')
        assert target.read_bytes()==before
    print('SA2 importer: real synthetic ADX conversion and 11 Vorbis outputs passed; missing, ambiguous and corrupt inputs rejected; previous output preserved.')
if __name__=='__main__':main()
