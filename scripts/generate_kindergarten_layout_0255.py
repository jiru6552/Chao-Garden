#!/usr/bin/env python3
"""Preserve the Fortune-Teller curtain's room-facing gold end after its180-degree turn."""
from copy import deepcopy
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'
MODEL=AS/'models/block'


def write(path, document):
    path.write_text(json.dumps(document,indent=2)+'\n')


def main():
    identifier='school_purple_room_curtain_tie'
    source=json.loads((MODEL/(identifier+'.json')).read_text())
    mirrored=deepcopy(source)
    for element in mirrored['elements']:
        low,high=element['from'][2],element['to'][2]
        element['from'][2],element['to'][2]=16-high,16-low
        # Reflection exchanges the physical front/back normals along with
        # their complete authored UVs. Rotation alone would bury the tassel.
        faces=element['faces']
        faces['north'],faces['south']=faces['south'],faces['north']
    write(MODEL/(identifier+'_east.json'),mirrored)
    path=AS/'blockstates'/(identifier+'.json')
    states=json.loads(path.read_text())
    states['variants']['facing=east']={'model':'chao_garden:block/'+identifier+'_east','y':270}
    write(path,states)
    print('Kindergarten0255: Fortune-Teller right curtain tie mirrored; existing green curtains unchanged.')


if __name__=='__main__':main()
