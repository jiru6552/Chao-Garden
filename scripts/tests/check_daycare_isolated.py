#!/usr/bin/env python3
"""Execute extracted production methods against small stubs; not a native GameTest."""
from pathlib import Path
r=Path(__file__).resolve().parents[2]/'src/main/java/com/chaogarden'
def method(f,start):
 s=(r/f).read_text();i=s.index(start);e=s.index('{',i)+1;depth=1
 while depth:depth+=(s[e]=='{')-(s[e]=='}');e+=1
 return s[i:e].replace('com.chaogarden.kindergarten.','')
managed=method('activities/GardenRegistry.java','  public static boolean managed(')
boarded=method('kindergarten/KindergartenContent.java','  public static boolean isBoarded(')
recall=method('activities/ActivityManager.java','  public static void callHome(')
s='''import java.util.*;
class DaycareCheck {
 static class Level {boolean school;Level(boolean s){school=s;}}
 static class Server {KindergartenRoster roster=new KindergartenRoster();}
 static class ServerLevel extends Level {final Server server;int chunks;ServerLevel(Server s,boolean school){super(school);server=s;}Server getServer(){return server;}void getChunkAt(BlockPos p){chunks++;}}
 record BlockPos(int x,int y,int z){BlockPos offset(int x,int y,int z){return new BlockPos(this.x+x,this.y+y,this.z+z);}}
 static class Vec3 {double x,y,z;Vec3(double x,double y,double z){this.x=x;this.y=y;this.z=z;}}
 enum RelativeMovement {X}
 static class Persistent {Set<String> keys=new HashSet<>();boolean contains(String k){return keys.contains(k);}}
 static class ChaoEntity {Level world;UUID id=UUID.randomUUID(),owner=UUID.randomUUID();Persistent data=new Persistent();boolean busy,constructing,staying=true;int teleports;ChaoEntity(Level l){world=l;}Level level(){return world;}UUID getUUID(){return id;}Persistent getPersistentData(){return data;}void setStaying(boolean s){staying=s;}float getYRot(){return 0;}void teleportTo(ServerLevel w,double x,double y,double z,Set<RelativeMovement> flags,float yaw,int pitch){world=w;teleports++;}}
 static class KindergartenPupil0257 extends ChaoEntity {KindergartenPupil0257(Level l){super(l);}}
 static class KindergartenRoster {record Student(UUID keeper){}Map<UUID,Student> students=new HashMap<>();static KindergartenRoster get(Server s){return s.roster;}}
 static class KindergartenContent {static final String STUDENT="student";static boolean inKindergarten(Level l){return l.school;}
'''+boarded+'''\n}
 static class GardenRegistry {static boolean constructing(ChaoEntity c){return c.constructing;}record Place(ServerLevel target){ServerLevel level(Server s){return target;}BlockPos block(){return new BlockPos(0,0,0);}}
'''+managed+'''\n}
 static class ActivityManager {static boolean isBusy(ChaoEntity c){return c.busy;}static Vec3 safeSpot(ServerLevel l,BlockPos p){return new Vec3(p.x(),p.y(),p.z());}
'''+recall+'''\n}
 static int assertions;static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
 for(int mask=0;mask<32;mask++){
 var server=new Server();var school=new ServerLevel(server,(mask&1)!=0);var home=new ServerLevel(server,false);var child=new ChaoEntity(school);
 boolean marker=(mask&2)!=0,roster=(mask&4)!=0,owner=(mask&8)!=0,companion=(mask&16)!=0;
 if(marker)child.data.keys.add("student");if(companion)child.data.keys.add("ChaoCompanion");
 if(roster)server.roster.students.put(child.id,new KindergartenRoster.Student(owner?child.owner:UUID.randomUUID()));
 boolean boarded=school.school&&marker&&roster&&owner;
 check(KindergartenContent.isBoarded(child)==boarded,"boarding truth table "+mask);
 check(GardenRegistry.managed(child)==(boarded||companion),"managed truth table "+mask);
 ActivityManager.callHome(child,new GardenRegistry.Place(home));
 check(child.teleports==(boarded?0:1),"direct recall "+mask);check(home.chunks==(boarded?0:1),"recall loaded home for student "+mask);
 }
 var server=new Server();var school=new ServerLevel(server,true);var home=new ServerLevel(server,false);
 var c=new ChaoEntity(school);c.data.keys.add("student");server.roster.students.put(c.id,new KindergartenRoster.Student(c.owner));
 var restored=new ChaoEntity(school);restored.id=c.id;restored.owner=c.owner;restored.data.keys.add("student");server.roster.students.put(c.id,new KindergartenRoster.Student(c.owner));
 check(GardenRegistry.managed(restored),"restored student lost protection");ActivityManager.callHome(restored,new GardenRegistry.Place(home));check(restored.teleports==0,"restored student recalled");
 server.roster.students.remove(c.id);restored.data.keys.remove("student");check(!GardenRegistry.managed(restored),"released student still managed");ActivityManager.callHome(restored,new GardenRegistry.Place(home));check(restored.teleports==1,"released student no longer recalled");
 check(GardenRegistry.managed(new KindergartenPupil0257(school)),"existing NPC pupil protection lost");
 c=new ChaoEntity(home);c.busy=true;ActivityManager.callHome(c,new GardenRegistry.Place(school));check(c.teleports==0,"existing activity guard lost");c.busy=false;c.constructing=true;ActivityManager.callHome(c,new GardenRegistry.Place(school));check(c.teleports==0,"existing construction guard lost");
 System.out.println(assertions+" isolated checks passed using production isBoarded/managed/callHome methods; Minecraft APIs are stubs, not a native test.");
 }
}
'''
if __name__ == '__main__':
 from isolated_java_support import run_java_source
 run_java_source('DaycareCheck', s)
