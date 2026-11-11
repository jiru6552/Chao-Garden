"""Original short plastic/bead rattle shakes; deterministic synthesis, no sampled audio."""
from pathlib import Path
import json, math, random, struct, subprocess, tempfile, wave

root=Path(__file__).resolve().parents[1]
target=root/'src/main/resources/assets/chao_garden/sounds/toys'
target.mkdir(parents=True,exist_ok=True)
rate=44100
for variation in range(3):
    rng=random.Random(251920+variation)
    values=[0.0]*int(rate*.16)
    for grain in range(13):
        start=int(rate*(.002+grain*.007+rng.uniform(0,.004)))
        length=int(rate*rng.uniform(.015,.038))
        frequency=rng.uniform(1500,4200)
        strength=(.55+.45*math.sin(math.pi*grain/13))*rng.uniform(.65,1)
        previous=0.0
        for j in range(length):
            if start+j>=len(values):break
            t=j/rate;raw=rng.uniform(-1,1);noise=raw-previous*.72;previous=raw
            envelope=(1-math.exp(-t*2200))*math.exp(-t*170)
            values[start+j]+=strength*envelope*(noise*.7+math.sin(2*math.pi*frequency*t)*.17)
    peak=max(map(abs,values))
    samples=[round(max(-1,min(1,v/peak*.72))*32767) for v in values]
    with tempfile.TemporaryDirectory() as temp:
        wav=Path(temp)/'shake.wav'
        with wave.open(str(wav),'wb') as stream:
            stream.setnchannels(1);stream.setsampwidth(2);stream.setframerate(rate)
            stream.writeframes(struct.pack('<'+'h'*len(samples),*samples))
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(target/f'rattle_{variation+1}.ogg')],check=True)
sounds=root/'src/main/resources/assets/chao_garden/sounds.json'
data=json.loads(sounds.read_text())
data['toy.rattle']={'subtitle':'subtitles.chao_garden.toy.rattle','sounds':[{'name':f'chao_garden:toys/rattle_{v}','stream':False} for v in range(1,4)]}
sounds.write_text(json.dumps(data,indent=2)+'\n')
language=root/'src/main/resources/assets/chao_garden/lang/en_us.json'
data=json.loads(language.read_text());data['subtitles.chao_garden.toy.rattle']='Rattle shakes'
language.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
print('Generated three 0.16-second mono rattle samples and registered resources.')
