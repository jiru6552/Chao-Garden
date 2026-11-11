#!/usr/bin/env python3
"""Verify actual pitched vocal samples, rests, bounded spatial events and provenance."""
from pathlib import Path
import copy,hashlib,importlib.util,json,subprocess
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
CHECKS=0
def require(value,message):
    global CHECKS;CHECKS+=1
    if not value:raise AssertionError(message)
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def validate(manifest,sounds,locale):
    require(manifest['source_kind']=='original_melody_from_provided_chao_voice' and manifest['exact_sa2_recording'] is False,'Original arrangement misrepresented as the game recording')
    require(manifest['source_sha256']==sha(ROOT/'src/main/resources'/manifest['source_voice']),'Singing source voice changed')
    require(manifest['cycle_ticks']==120 and manifest['slot_phrase_starts']=={'2':0,'3':60},'Pupils lost their alternating vocal cadence')
    require(manifest['volume']==.75 and manifest['attenuation_distance']==12,'Singing became global or inaudibly quiet')
    require({row['id'] for row in manifest['phrases']}=={'a','b'} and len(manifest['phrases'])==2,'Wrong vocal phrase set')
    for row in manifest['phrases']:
        key=row['id'];path=ROOT/'src/main/resources'/row['asset']
        require(sha(path)==row['sha256'],'Actual singing file changed')
        event='chao.singing_'+key
        require(row['sound_event']=='chao_garden:'+event,'Manifest sound identifier differs from the registered phrase')
        require(sounds[event]=={'subtitle':'subtitles.chao_garden.'+event,'sounds':[{'name':'chao_garden:chao/singing_'+key,'attenuation_distance':12}]},'Song event falls back to a reaction chirp or unbounded/streamed sound')
        require(locale.get('subtitles.chao_garden.'+event)=='Baby Chao sings','Vocal subtitle absent')
        info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration:stream=codec_name,channels,sample_rate','-of','json',str(path)]))
        stream=info['streams'][0]
        require(stream['codec_name']=='vorbis' and stream['channels']==1 and int(stream['sample_rate'])==22050,'Vocal is not a native spatial monoVorbis sample')
        require(abs(float(info['format']['duration'])-2.8)<.005 and row['duration_seconds']==2.8,'Phrase exceeds its no-overlap window')
        raw=subprocess.check_output(['ffmpeg','-nostdin','-v','error','-i',str(path),'-f','f32le','-ac','1','-ar','22050','pipe:1'])
        audio=np.frombuffer(raw,dtype='<f4').astype(float);rate=22050
        require(.25<float(np.max(np.abs(audio)))<.85,'Silent or clipped vocal sample')
        require(len(set(row['midi_notes']))>=4 and len(row['midi_notes'])==6,'Reaction repeated on a single pitch instead of singing a melody')
        require(row['onset_ticks']==[0,8,16,24,36,44] and row['duration_ticks']==[7,7,7,11,7,11],'Sung timing no longer matches the native note effects')
        for onset,duration,midi in zip(row['onset_ticks'],row['duration_ticks'],row['midi_notes']):
            start=round((onset+duration*.35)/20*rate);end=round((onset+duration*.65)/20*rate)
            segment=audio[start:end];segment-=segment.mean();rms=float(np.sqrt(np.mean(segment*segment)))
            require(rms>.05,'A scored syllable is silent')
            target=440*2**((midi-69)/12);expected=rate/target
            corr=np.correlate(segment,segment,'full')[len(segment)-1:]
            lo,hi=round(expected*.95),round(expected*1.05)+1
            lag=lo+int(np.argmax(corr[lo:hi]));quality=float(corr[lag]/max(corr[0],1e-12))
            require(abs(rate/lag/target-1)<.04 and quality>.65,'Voiced syllable does not sustain its scored musical pitch')
            # Last portion of each explicit rest remains quiet after codec ringing.
            rest_start=round((onset+duration)/20*rate)+round(.028*rate)
            rest_end=min(len(audio),round((onset+duration+1)/20*rate))
            require(float(np.sqrt(np.mean(audio[rest_start:rest_end]**2)))<.012,'Musical phrase lost syllable separation/rests')
    require(len(manifest['existing_audio_sha256'])==6,'Provided voice/CD preservation set changed')
    for name,digest in manifest['existing_audio_sha256'].items():require(sha(RES/name)==digest,'A supplied voice/music recording was rewritten')

def main():
    manifest=json.loads((ROOT/'art/school_singing_manifest_0258.json').read_text());sounds=json.loads((RES/'sounds.json').read_text());locale=json.loads((RES/'lang/en_us.json').read_text())
    validate(manifest,sounds,locale)
    mutations=[]
    changed=copy.deepcopy(manifest);changed['exact_sa2_recording']=True;mutations.append((changed,sounds,'unprovided game singing claim'))
    changed=copy.deepcopy(manifest);changed['phrases'][0]['midi_notes']=[72]*6;mutations.append((changed,sounds,'single reaction pitch'))
    changed=copy.deepcopy(manifest);changed['phrases'][0]['midi_notes'][2]=80;mutations.append((changed,sounds,'out-of-tune syllable'))
    changed=copy.deepcopy(sounds);changed['chao.singing_a']['sounds'][0]['name']='chao_garden:chao/chat_1';mutations.append((manifest,changed,'ordinary babble substitution'))
    changed=copy.deepcopy(manifest);changed['slot_phrase_starts']['3']=0;mutations.append((changed,sounds,'overlapping response'))
    for bad_manifest,bad_sounds,label in mutations:
        try:validate(bad_manifest,bad_sounds,locale)
        except AssertionError:pass
        else:raise AssertionError('Accepted '+label)
    print(f'School singing0258: {CHECKS} audio/score/spatial/provenance checks; {len(mutations)} malformed fixtures rejected.')

if __name__=='__main__':main()
