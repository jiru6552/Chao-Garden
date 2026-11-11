#!/usr/bin/env python3
"""Validate area music availability, the retained supplied recording, and optional pack input."""
from pathlib import Path
import hashlib,importlib.util,json,tempfile,zipfile

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
AREAS={'lobby','neutral','hero','dark','kindergarten','black_market'}
CHECKS=0
def require(value,message):
    global CHECKS;CHECKS+=1
    if not value:raise AssertionError(message)

def validate(sounds,manifest,locale):
    rows=manifest['regions'];require({r['id'] for r in rows}==AREAS and len(rows)==6,'Wrong regional music domain')
    require(manifest['bundled_recordings_count']==5 and manifest['supplied_adx_conversion_verified'] is True,'Supplied five-track catalog is inaccurate')
    for row in rows:
        area=row['id'];event='music.area.'+area
        require(row['sound_event']=='chao_garden:'+event,'Wrong sound event identifier')
        require('subtitles.chao_garden.'+event in locale,'Area music subtitle missing')
        entry=sounds[event]
        if area!='black_market':
            require(row['bundled'] and row['source_kind']=='user_supplied_sa2_adx','Supplied audio provenance changed')
            require(entry['sounds']==[{'name':'chao_garden:music/area/'+area,'stream':True}],'Area is not bound to its supplied recording')
        else:
            require(not row['bundled'] and row['source_kind']=='user_recording_required','Unavailable regional audio claimed included')
            require(entry['sounds']==[],'Unprovided soundtrack has a missing or substituted audio reference')
    require(sounds['music.chao_garden']['sounds']==[{'name':'chao_garden:records/chao_garden','stream':True}],'Existing jukebox CD changed')
    require(set(manifest['missing_regional_track_ids'])=={'black_market'},'Unavailable track list is inaccurate')

def main():
    sounds=json.loads((RES/'sounds.json').read_text());manifest=json.loads((ROOT/'art/area_music_manifest_0257.json').read_text());locale=json.loads((RES/'lang/en_us.json').read_text())
    validate(sounds,manifest,locale)
    track=RES/'sounds/records/chao_garden.ogg';digest=hashlib.sha256(track.read_bytes()).hexdigest()
    require(digest==json.loads((ROOT/'art/music_asset_manifest.json').read_text())['track_sha256']=='d9f873eef995064d267455f0c761493ea59417011fa02525008f175d9f94e724','Supplied finite CD recording bytes changed')
    for row in manifest['regions']:
        if row['bundled']:
            require(hashlib.sha256((ROOT/'src/main/resources'/row['asset']).read_bytes()).hexdigest()==row['sha256'],'Regional manifest does not bind actual recording')
    for file in ('call.ogg','chat_1.ogg','chat_2.ogg','chat_3.ogg','startled.ogg'):
        row=next(r for r in json.loads((ROOT/'docs/audio-provenance.json').read_text()) if r['asset']==file)
        require(hashlib.sha256((RES/'sounds/chao'/file).read_bytes()).hexdigest()==row['output_sha256'],'Existing Chao voice changed')
    client=(ROOT/'src/main/java/com/chaogarden/client/AreaMusic0257.java').read_text()
    for fragment in ('SoundSource.MUSIC','this.looping=true','this.relative=true','SoundInstance.Attenuation.NONE',
                     'if(mc.level==null||mc.player==null){clear(mc)', 'RECORDS.isEmpty()?.65f:0',
                     'event.getWeight()>0', 'desired!=Track.NONE&&current==null', 'mc.getMusicManager().stopPlaying()'):
        require(fragment in client,'Missing live music controller binding: '+fragment)
    # The supplied Hero recording must never be replaced with the Neutral tune.
    broken=json.loads(json.dumps(sounds));broken['music.area.hero']['sounds']=broken['music.area.neutral']['sounds']
    try:validate(broken,manifest,locale)
    except AssertionError:pass
    else:raise AssertionError('Accepted unrelated soundtrack substitution')
    broken_manifest=json.loads(json.dumps(manifest));broken_manifest['bundled_recordings_count']=6
    try:validate(sounds,broken_manifest,locale)
    except AssertionError:pass
    else:raise AssertionError('Accepted unprovided full soundtrack claim')
    spec=importlib.util.spec_from_file_location('area_pack',ROOT/'scripts/build_area_music_pack_0257.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
    with tempfile.TemporaryDirectory(prefix='chao0257-music-pack-test-') as directory:
        temp=Path(directory);source=temp/'input';source.mkdir();output=temp/'pack.zip'
        (source/'neutral.ogg').write_bytes(track.read_bytes());builder.build(source,output)
        with zipfile.ZipFile(output) as archive:
            require(archive.testzip() is None,'Companion music pack CRC failed')
            require(set(archive.namelist())=={'pack.mcmeta','assets/chao_garden/sounds.json','assets/chao_garden/sounds/music/area/neutral.ogg','supplied-recordings.json'},'Partial music pack contains unprovided files')
            require(archive.read('assets/chao_garden/sounds/music/area/neutral.ogg')==track.read_bytes(),'Pack altered supplied audio')
            require(json.loads(archive.read('assets/chao_garden/sounds.json'))['music.area.neutral']['replace'] is True,'Pack cannot override default event')
        (source/'hero.ogg').write_bytes(b'OggS'+bytes(60))
        try:builder.build(source,temp/'invalid.zip')
        except AssertionError:pass
        else:raise AssertionError('Accepted non-Vorbis OGG input')
    print(f'Area music0257: {CHECKS} audio/resource/controller checks; malformed substitution, attribution and codec fixtures rejected.')

if __name__=='__main__':main()
