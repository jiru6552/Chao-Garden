#!/usr/bin/env python3
"""Arrange two original vocal phrases from the already supplied Chao call.

This is a new melody and a pitch-synchronous vocal arrangement, not an imported
SA2 singing recording. Existing reaction voices and the supplied CD are inputs
only and are never rewritten. Ogg serials/CRCs are normalized for byte replay.
"""
from pathlib import Path
import hashlib, json, math, struct, subprocess, tempfile
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
RATE=22050
ONSET_TICKS=(0,8,16,24,36,44)
DURATION_TICKS=(7,7,7,11,7,11)
MELODIES={'a':(72,74,76,79,76,72),'b':(67,72,74,76,74,72)}
SOURCE='sounds/chao/call.ogg'
SOURCE_HASH='f3de2f27ecbe1a2a63ed7919637a503d76d7f493fdbf145caa231f603cd8384c'

def sha(data):return hashlib.sha256(data).hexdigest()
def decode(path):
    return np.frombuffer(subprocess.check_output(['ffmpeg','-nostdin','-v','error','-i',str(path),
        '-f','f32le','-ac','1','-ar',str(RATE),'pipe:1']),dtype='<f4').astype(np.float64)

def pitch_marks(source):
    # The provided clean call has a stable approximately450Hz voiced vowel.
    # Follow its actual positive pulse peaks, retaining the vocal spectral shape.
    start,end=int(.055*RATE),int(.365*RATE)
    sample=source[int(.12*RATE):int(.28*RATE)];sample=sample-sample.mean()
    corr=np.correlate(sample,sample,mode='full')[len(sample)-1:]
    lo,hi=round(RATE/550),round(RATE/350)
    period=int(lo+np.argmax(corr[lo:hi]))
    center=start+int(np.argmax(source[start:start+period]))
    marks=[]
    while center<end:
        marks.append(center)
        expected=center+period;radius=max(2,round(period*.15))
        left,right=expected-radius,expected+radius+1
        center=left+int(np.argmax(source[left:right]))
    return np.asarray(marks),period

def vocal_note(source,marks,period,midi,ticks):
    count=round(ticks/20*RATE);out=np.zeros(count);weights=np.zeros(count)
    half=period;window=np.hanning(half*2+1)
    target=440*2**((midi-69)/12)
    center=0.0
    while center<count:
        elapsed=center/RATE
        # A very small natural vibrato; the score remains unambiguously pitched.
        vibrato=2**((.10*math.sin(elapsed*2*math.pi*4.5))/12)
        source_index=min(len(marks)-1,round(center/max(1,count-1)*(len(marks)-1)))
        mark=int(marks[source_index]);grain=source[mark-half:mark+half+1]
        at=round(center);left=max(0,at-half);right=min(count,at+half+1)
        first=left-(at-half);last=first+right-left
        out[left:right]+=grain[first:last]*window[first:last]
        weights[left:right]+=window[first:last]
        center+=RATE/(target*vibrato)
    out/=np.maximum(weights,.001)
    out-=out.mean()
    # Each sung syllable has its own attack/release, separated by real rests.
    attack=min(count,round(.018*RATE));release=min(count,round(.055*RATE))
    out[:attack]*=np.sin(np.linspace(0,math.pi/2,attack))**2
    out[-release:]*=np.cos(np.linspace(0,math.pi/2,release))**2
    peak=max(.001,float(np.max(np.abs(out))));out*=.70/peak
    return out

def ogg_crc(data):
    crc=0
    for value in data:
        crc^=value<<24
        for _ in range(8):crc=((crc<<1)^0x04c11db7 if crc&0x80000000 else crc<<1)&0xffffffff
    return crc

def normalize_ogg(data,serial):
    data=bytearray(data);offset=0
    while offset<len(data):
        assert data[offset:offset+4]==b'OggS'
        size=27+data[offset+26];size+=sum(data[offset+27:offset+27+data[offset+26]])
        data[offset+14:offset+18]=struct.pack('<I',serial)
        data[offset+22:offset+26]=bytes(4)
        data[offset+22:offset+26]=struct.pack('<I',ogg_crc(data[offset:offset+size]))
        offset+=size
    assert offset==len(data)
    return bytes(data)

def render(source,marks,period,notes):
    out=np.zeros(round(2.8*RATE))
    for tick,duration,midi in zip(ONSET_TICKS,DURATION_TICKS,notes):
        at=round(tick/20*RATE);voice=vocal_note(source,marks,period,midi,duration)
        out[at:at+len(voice)]+=voice
    return np.round(np.clip(out,-1,1)*32767).astype('<i2').tobytes()

def main():
    original={name:sha((RES/name).read_bytes()) for name in
              ['sounds/chao/call.ogg','sounds/chao/chat_1.ogg','sounds/chao/chat_2.ogg',
               'sounds/chao/chat_3.ogg','sounds/chao/startled.ogg','sounds/records/chao_garden.ogg']}
    source_path=RES/SOURCE;assert sha(source_path.read_bytes())==SOURCE_HASH,'Provided call changed'
    source=decode(source_path);marks,period=pitch_marks(source)
    phrases=[]
    with tempfile.TemporaryDirectory(prefix='chao-singing0258-') as temp:
        for key,notes in MELODIES.items():
            pcm=render(source,marks,period,notes);encoded=Path(temp)/(key+'.ogg')
            subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-f','s16le','-ar',str(RATE),'-ac','1',
                '-i','pipe:0','-map_metadata','-1','-c:a','libvorbis','-q:a','5','-fflags','+bitexact',str(encoded)],input=pcm,check=True)
            data=normalize_ogg(encoded.read_bytes(),0x02580000+ord(key))
            relative='sounds/chao/singing_'+key+'.ogg';(RES/relative).write_bytes(data)
            phrases.append({'id':key,'sound_event':'chao_garden:chao.singing_'+key,'asset':'assets/chao_garden/'+relative,
                'sha256':sha(data),'pcm_sha256':sha(pcm),'duration_seconds':2.8,'codec':'Vorbis','sample_rate':RATE,'channels':1,
                'onset_ticks':list(ONSET_TICKS),'duration_ticks':list(DURATION_TICKS),'midi_notes':list(notes)})
    sounds=json.loads((RES/'sounds.json').read_text());locale=json.loads((RES/'lang/en_us.json').read_text())
    for key in MELODIES:
        event='chao.singing_'+key;subtitle='subtitles.chao_garden.'+event
        sounds[event]={'subtitle':subtitle,'sounds':[{'name':'chao_garden:chao/singing_'+key,'attenuation_distance':12}]}
        locale[subtitle]='Baby Chao sings'
    (RES/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
    (RES/'lang/en_us.json').write_text(json.dumps(locale,indent=2,ensure_ascii=False,sort_keys=True)+'\n')
    (ROOT/'art/school_singing_manifest_0258.json').write_text(json.dumps({
        'release':'0.25.8','source_kind':'original_melody_from_provided_chao_voice','exact_sa2_recording':False,
        'source_voice':'assets/chao_garden/'+SOURCE,'source_sha256':SOURCE_HASH,
        'arrangement':'Pitch-synchronous overlap-add retains the provided call vocal timbre; two newly composed short call-and-response melodies with syllable envelopes and rests.',
        'cycle_ticks':120,'slot_phrase_starts':{'2':0,'3':60},'volume':.75,'attenuation_distance':12,
        'phrases':phrases,'existing_audio_sha256':original,
        'scope':'Original arranged fallback phrases. The final 0.25.11 audio writer installs the supplied full-length classroom recordings; existing voices/CD remain untouched.'},indent=2)+'\n')
    assert all(sha((RES/name).read_bytes())==digest for name,digest in original.items())
    print('School singing0258: two original2.8s vocal phrases; six provided voice/music inputs unchanged.')

if __name__=='__main__':main()
