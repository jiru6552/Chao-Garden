#!/usr/bin/env python3
"""Finite internal collision-state resource for the scene-rendered celestial stairs."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'

def main():
    states={'variants':{'':{'model':'chao_garden:block/lobby_floor_stairs'}}}
    model={'textures':{'particle':'chao_garden:scene/lobby_glass'},'elements':[]}
    for path,value in ((AS/'blockstates/lobby_floor_stairs.json',states),(AS/'models/block/lobby_floor_stairs.json',model)):
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(value,indent=2)+'\n')
    print('Lobby approaches0255: one protected internal stair ID; shared scene material and finite curved collision, no item/loot.')

if __name__=='__main__':main()
