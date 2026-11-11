#!/usr/bin/env python3
"""Merge optional area sound events; preserve all existing voices and jukebox audio."""
from pathlib import Path
import hashlib,json

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
TITLES={'lobby':'Chao Lobby','neutral':'Neutral Garden','hero':'Hero Garden','dark':'Dark Garden',
        'kindergarten':'Chao Kindergarten','black_market':'Black Market'}

def main():
    sounds=json.loads((RES/'sounds.json').read_text())
    locale=json.loads((RES/'lang/en_us.json').read_text())
    regions=[]
    for area,title in TITLES.items():
        event='music.area.'+area
        asset='records/chao_garden' if area=='neutral' else 'music/area/'+area
        # Optional events are intentionally empty until an ordinary resourcepack
        # supplies a recording. There are no missing-file references or silent
        # fabricated renditions of the requested game's soundtrack.
        sounds[event]={'subtitle':'subtitles.chao_garden.'+event,
                       'sounds':[{'name':'chao_garden:'+asset,'stream':True}] if area=='neutral' else []}
        locale['subtitles.chao_garden.'+event]=title+' music'
        row={'id':area,'title':title,'sound_event':'chao_garden:'+event,
             'asset':'assets/chao_garden/sounds/'+asset+'.ogg','bundled':area=='neutral',
             'resourcepack_file':area+'.ogg',
             'source_kind':'owner_supplied_marketplace_recording' if area=='neutral' else 'user_recording_required'}
        if area=='neutral':row['sha256']=hashlib.sha256((RES/'sounds/records/chao_garden.ogg').read_bytes()).hexdigest()
        regions.append(row)
    (RES/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
    (RES/'lang/en_us.json').write_text(json.dumps(locale,indent=2,ensure_ascii=False,sort_keys=True)+'\n')
    (ROOT/'art/area_music_manifest_0257.json').write_text(json.dumps({
        'release':'0.25.8','bundled_recordings_count':1,
        'exact_sa2_ost_rendition_verified':False,'regions':regions,
        'missing_regional_track_ids':[r['id'] for r in regions if not r['bundled']],
        'scope':'The existing owner-supplied Sonic Marketplace Neutral Garden CD is reused. Five other regional recordings are not bundled. This manifest does not claim that the supplied recording is the exact SA2 OST rendition. Optional music-pack builder accepts user-provided audio only.'},indent=2)+'\n')
    print('Area music0257:6streaming event slots;1existing supplied recording;5optional user recording slots;voices/CD preserved.')

if __name__=='__main__':main()
