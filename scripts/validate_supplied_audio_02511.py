#!/usr/bin/env python3
"""Verify the complete supplied ADX-to-Vorbis signal path and exact event mapping."""
from pathlib import Path
import hashlib,json,subprocess
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
EXPECTED={'lobby':'chao_hall.adx','neutral':'chao_g_bgm_n.adx','hero':'chao_g_bgm_h.adx',
          'dark':'chao_g_bgm_d.adx','kindergarten':'chao_k_m.adx'}
EXPECTED.update({f'chao_k_sing_{side}{i}':f'chao_k_sing_{side}{i}.adx' for side in ('hero','dark') for i in (1,2,3)})
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def pcm(path):
    raw=subprocess.check_output(['ffmpeg','-nostdin','-v','error','-i',str(path),'-f','f32le','-ac','1','-ar','22050','pipe:1'])
    return np.frombuffer(raw,dtype='<f4').astype(np.float64)
def main():
    report=json.loads((ROOT/'art/audio02511/import-report.json').read_text())
    supplied=json.loads((ROOT/'art/audio02511/supplied-inputs.json').read_text())
    sounds=json.loads((RES/'assets/chao_garden/sounds.json').read_text())
    assert report['supplied_archive_sha256']==supplied['archive_sha256']
    rows=report['recordings'];assert len(rows)==len(EXPECTED)==11 and {r['id'] for r in rows}==set(EXPECTED)
    chorus=sounds['chao.classroom_chorus']['sounds'];assert len(chorus)==6
    checked=[]
    for row in rows:
        name=EXPECTED[row['id']];assert row['source_file'].lower()==name
        original=ROOT/'art/audio02511/source-adx'/name;converted=RES/row['asset'];canonical=ROOT/row['canonical_file']
        assert sha(original)==row['source_sha256'] and sha(converted)==sha(canonical)==row['sha256']
        provided=next(x for x in supplied['recordings'] if x['source_file']==name)
        assert provided['sha256']==row['source_sha256']
        asset='chao_garden:'+row['asset'].removeprefix('assets/chao_garden/sounds/').removesuffix('.ogg')
        event='music.area.'+row['id'] if row['kind']=='area' else 'chao.classroom_chorus'
        bindings=sounds[event]['sounds'];wanted={'name':asset,'stream':True}
        if row['kind']=='chorus':wanted['attenuation_distance']=12
        assert wanted in bindings and (len(bindings)==1 if row['kind']=='area' else len(bindings)==6)
        info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_entries','stream=codec_name,channels,sample_rate:format=duration','-of','json',str(converted)]))
        stream=info['streams'][0];assert len(info['streams'])==1 and stream['codec_name']=='vorbis' and int(stream['sample_rate'])==44100
        assert stream['channels']==row['channels']==(2 if row['kind']=='area' else 1)
        assert abs(float(info['format']['duration'])-row['duration_seconds'])<.002
        before,after=pcm(original),pcm(converted);assert abs(len(before)-len(after))<=32
        n=min(len(before),len(after));before,after=before[:n],after[:n]
        correlation=float(np.corrcoef(before,after)[0,1]);ratio=float(np.sqrt(np.mean(after*after)/np.mean(before*before)))
        assert correlation>.97 and .97<ratio<1.03, (name,correlation,ratio)
        assert np.isfinite(after).all() and np.sqrt(np.mean(after*after))>.005
        checked.append({'id':row['id'],'seconds':row['duration_seconds'],'channels':row['channels'],
                        'source_samples':len(before),'converted_samples':len(after),'waveform_correlation':correlation,'rms_ratio':ratio})
    assert sounds['music.area.black_market']['sounds']==[]
    assert sounds['music.chao_garden']['sounds']==[{'name':'chao_garden:records/chao_garden','stream':True}]
    result={'release':'0.25.11','result':'PASS','source':'user-supplied ADX.zip','tracks':checked,
            'method':'Full decode of every supplied source and installed output, downmixed/resampled equally for signal comparison; original area stereo and positional chorus mono verified separately. This is not an audible listening claim.'}
    out=ROOT/'docs/validation-0.25.11/audio-signal-checks.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n')
    print('Supplied audio02511: all eleven complete source/output waveforms match; exact mappings, duration, codec, channels and native streaming bindings PASS.')
if __name__=='__main__':main()
