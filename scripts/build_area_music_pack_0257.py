#!/usr/bin/env python3
"""Build a companion resourcepack from the user's supplied named OGG files."""
import argparse,hashlib,json,zipfile
from pathlib import Path

AREAS=('lobby','neutral','hero','dark','kindergarten','black_market')

def build(source,target):
    assert source.is_dir(),'Input directory must contain your supplied OGG recordings'
    tracks={area:source/(area+'.ogg') for area in AREAS if (source/(area+'.ogg')).is_file()}
    assert tracks,'No named regional OGG recordings found'
    sounds={};manifest=[]
    for area,path in tracks.items():
        payload=path.read_bytes();assert payload[:4]==b'OggS' and len(payload)>28,'Not an OGG container: '+path.name
        header=27+payload[26]
        assert payload[header:header+7]==b'\x01vorbis','A Vorbis OGG recording is required: '+path.name
        sounds['music.area.'+area]={'replace':True,'sounds':[{'name':'chao_garden:music/area/'+area,'stream':True}]}
        manifest.append({'area':area,'source_file':path.name,'sha256':hashlib.sha256(payload).hexdigest()})
    target.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
        z.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':34,'description':'Chao Garden supplied area music'}}))
        z.writestr('assets/chao_garden/sounds.json',json.dumps(sounds,indent=2)+'\n')
        for area,path in tracks.items():z.write(path,'assets/chao_garden/sounds/music/area/'+area+'.ogg')
        z.writestr('supplied-recordings.json',json.dumps(manifest,indent=2)+'\n')
    with zipfile.ZipFile(target) as z:assert z.testzip() is None
    print(json.dumps({'file':str(target.resolve()),'provided_areas':list(tracks),'missing_areas':[a for a in AREAS if a not in tracks]}))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('source',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();build(a.source.resolve(),a.output.resolve())
