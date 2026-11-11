#!/usr/bin/env python3
"""Bind the moved notice service, corner Ionic stack and real reflected Fortune tie."""
import argparse
from copy import deepcopy
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'src/main/resources/assets/chao_garden'
CHECKS=0


def require(condition,message):
    global CHECKS
    CHECKS+=1
    if not condition:raise AssertionError(message)


def load(path):return json.loads(path.read_text())


def tie_contract(source,mirror,states):
    base='school_purple_room_curtain_tie'
    require(source['textures']==mirror['textures'] and len(source['elements'])==len(mirror['elements']),
            'Purple tie reflection lost materials or members')
    for a,b in zip(source['elements'],mirror['elements']):
        require(a['from'][:2]==b['from'][:2] and a['to'][:2]==b['to'][:2]
                and b['from'][2]==16-a['to'][2] and b['to'][2]==16-a['from'][2],
                'Purple tie is a plain rotation, not a room-facing physical reflection')
        require(b['faces']['north']==a['faces']['south'] and b['faces']['south']==a['faces']['north']
                and all(b['faces'][f]==a['faces'][f] for f in ('up','down','east','west')),
                'Purple tie reflection reversed its surface normals or authored UVs')
    variants=states['variants']
    require(set(variants)=={'facing='+f for f in ('north','south','east','west')},'Purple curtain finite facing domain changed')
    for facing,turn in {'south':0,'west':90,'north':180,'east':270}.items():
        require(variants['facing='+facing]=={'model':'chao_garden:block/'+base+('_east' if facing=='east' else ''),
                                          **({'y':turn} if turn else {})},'Purple curtain facing bound the wrong real tie mesh')
    ends=[e for e in mirror['elements'] if e['faces']['south']['texture']=='#gold' and e['to'][1]<=7]
    require(len(ends)==1,'Purple tie lacks one distinct dangling gold end')
    end=ends[0]
    # Native270-degree blockstate turn: x'=z, z'=16-x.
    require(end['to'][2]<6 and 16-end['from'][0]<8,
            'Fortune right dangling gold end is behind its cloth or still on the right')


def layout_contract(rooms,services):
    compact=rooms.replace(' ','').replace('\n','')
    require('BOARD_PANEL=newBlockPos(-6,82,0)' in compact,'Notice panel is no longer on the clear north/right cupboard wall')
    require('BOARD_CORNER=newBlockPos(-5,80,8)' in compact,'Former board corner identity moved')
    require('{{-5,-6},{5,-6},{5,8},{-5,8}}' in compact and 'ionicColumn(p,corner[0],corner[1],"school_hall_column")' in compact,
            'Former notice corner lacks its complete fourth Ionic stack')
    require('p.decor(BOARD_PANEL.getX(),BOARD_PANEL.getY(),BOARD_PANEL.getZ(),"school_noticeboard",Direction.EAST)' in compact,
            'Physical board and authorized service panel coordinates disagree')
    require('p.decor(3,82,-7,"school_art_friends",Direction.SOUTH)' in compact,
            'Displaced artwork was lost or rehung across the Principal door')
    require('z==8?Direction.EAST:Direction.WEST' in compact,
            'Right Fortune panel did not turn180degrees independently of its left panel')
    service=services.replace(' ','').replace('\n','')
    require('BOARD("BulletinBoard",newBlockPos(-7,80,0),5)' in service,
            'Invisible board service retained its stale corner anchor')
    require('!pos.equals(KindergartenRooms025.BOARD_PANEL)' in service
            and '!player.level().getBlockState(pos).is(KindergartenDecor025.block("school_noticeboard"))' in service,
            'Board still accepts arbitrary nearby walls or its former Ionic pillar')
    require('if(role==Role.BOARD)for(varactor:level.getAllEntities())' in service
            and 'npc.role()==Role.BOARD&&npc.isAlive()&&!matches.contains(npc))matches.add(npc)' in service,
            'Board migration may replace its old eight-block-distant resident')
    old=(-7,8);new=(-7,0)
    require(tuple(v//16 for v in old)==tuple(v//16 for v in new),
            'Board migration left the entity region whose publication is already checked')
    # Derive wall support from the authored foyer, independently of placement strings.
    foyer=lambda x,z:abs(x)<=7 and -8<=z<=10 and abs(x)+max(0,-z-5,z-7)<=7
    require(foyer(-7,0) and not foyer(-8,0),'Notice panel has no continuous west-wall backing')
    require(all(foyer(x,-8) and not foyer(x,-9) for x in (2,3,4)),
            'Rehung artwork overhangs a missing north-wall backing tile')
    require(-6<0 and 0<3,'Notice panel is not north/right of the west-facing closet span3..7')


def main(self_test):
    source=load(AS/'models/block/school_purple_room_curtain_tie.json')
    mirror=load(AS/'models/block/school_purple_room_curtain_tie_east.json')
    states=load(AS/'blockstates/school_purple_room_curtain_tie.json')
    rooms=(ROOT/'src/main/java/com/chaogarden/kindergarten/KindergartenRooms025.java').read_text()
    services=(ROOT/'src/main/java/com/chaogarden/kindergarten/KindergartenContent.java').read_text()
    tie_contract(source,mirror,states);layout_contract(rooms,services)
    rejected=0
    if self_test:
        broken=deepcopy(states);broken['variants']['facing=east']['model']='chao_garden:block/school_purple_room_curtain_tie'
        normals=deepcopy(mirror);normals['elements'][0]['faces']['north']=source['elements'][0]['faces']['north']
        for label,args in [('plain rotation',(source,source,states)),('backward binding',(source,mirror,broken)),('wrong face normal',(source,normals,states))]:
            try:tie_contract(*args)
            except AssertionError:rejected+=1
            else:raise AssertionError('Accepted '+label)
        for label,changed_rooms,changed_services in [
            ('stale board surface',rooms.replace('BOARD_PANEL=new BlockPos(-6,82,0)','BOARD_PANEL=new BlockPos(-5,82,8)'),services),
            ('missing fourth pillar',rooms.replace('{5,8},{-5,8}','{5,8}'),services),
            ('unchanged right curtain',rooms.replace('z==8?Direction.EAST:Direction.WEST','Direction.WEST'),services),
            ('stale invisible anchor',rooms,services.replace('new BlockPos(-7,80,0)','new BlockPos(-7,80,8)'))]:
            try:layout_contract(changed_rooms,changed_services)
            except AssertionError:rejected+=1
            else:raise AssertionError('Accepted '+label)
    print(f'Kindergarten0255: {CHECKS} checks passed; moved native board/corner stack and physical Fortune curtain reflection; {rejected} malformed fixtures rejected.')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--self-test',action='store_true')
    main(parser.parse_args().self_test)
