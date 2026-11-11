#!/usr/bin/env python3
"""Execute extracted production methods against small stubs; not a native GameTest."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]/'src/main/java/com/chaogarden/activities'
def method(file,start):
 s=(root/file).read_text();i=s.index(start);b=s.index('{',i);depth=1;e=b+1
 while depth:
  depth+=(s[e]=='{')-(s[e]=='}');e+=1
 return s[i:e].replace('@Override ','')
mat=method('ActivityManager.java','  public static List<BlockPos> mat(')
drops=method('KarateMatBlock.java','  @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(')
drops=drops.replace('net.minecraft.world.item.ItemStack','ItemStack').replace('net.minecraft.world.level.storage.loot.LootParams','LootParams').replace('net.minecraft.world.level.storage.loot.parameters.LootContextParams','LootContextParams')
source='''import java.util.*;
class TatamiCheck {
 enum Direction { E(1,0),W(-1,0),N(0,-1),S(0,1); final int x,z;Direction(int x,int z){this.x=x;this.z=z;} static class Plane {static final Direction[] HORIZONTAL=values();}}
 record BlockPos(int x,int y,int z){BlockPos offset(int dx,int dy,int dz){return new BlockPos(x+dx,y+dy,z+dz);} BlockPos offset(BlockPos p){return offset(p.x,p.y,p.z);} BlockPos subtract(BlockPos p){return offset(-p.x,-p.y,-p.z);} BlockPos relative(Direction d){return offset(d.x,0,d.z);} int getX(){return x;}int getY(){return y;}int getZ(){return z;} static BlockPos containing(BlockPos p){return p;}}
 static class ActivityBlock {enum Kind {MAT}}
 static class BlockState {final Object block;final int tile;BlockState(Object b,int t){block=b;tile=t;}int getValue(String key){return tile;}boolean is(Object b){return block==b;}}
 static class ServerLevel {Map<BlockPos,BlockState> states=new HashMap<>();boolean loaded=true;BlockState getBlockState(BlockPos p){return states.getOrDefault(p,new BlockState(null,-1));}boolean hasChunkAt(BlockPos p){return loaded;}}
 static class ActivityContent {static final Holder MAT=new Holder();static class Holder {final KarateMatBlock block=new KarateMatBlock();KarateMatBlock get(){return block;}}}
 static ActivityBlock.Kind kind(ServerLevel l,BlockPos p){return l.getBlockState(p).is(ActivityContent.MAT.get())?ActivityBlock.Kind.MAT:null;}
 static class ItemStack {}
 static class LootContextParams {static String ORIGIN="origin";}
 static class LootParams {static class Builder {final ServerLevel level;final BlockPos origin;boolean suppress;BlockState selected;Builder(ServerLevel l,BlockPos o){level=l;origin=o;}BlockPos getParameter(String k){return origin;}ServerLevel getLevel(){return level;}}}
 static class Base {protected List<ItemStack> getDrops(BlockState s,LootParams.Builder p){p.selected=s;return !p.suppress&&(s.tile==24||s.tile==49)?List.of(new ItemStack()):List.of();}}
 static class KarateMatBlock extends Base {static final String TILE="tile";static final int CENTER=24,LEGACY=49;
 static BlockPos offset(int t){return new BlockPos(t%7-3,0,t/7-3);}static BlockPos center(BlockPos p,BlockState s){return s.tile==49?p:p.subtract(offset(s.tile));}
'''+drops+'''\n}
'''+mat+'''
 static int assertions;static void check(boolean yes,String reason){assertions++;if(!yes)throw new AssertionError(reason);}
 static void assembly(ServerLevel l,BlockPos c,boolean legacy){for(int i=0;i<49;i++)l.states.put(c.offset(KarateMatBlock.offset(i)),new BlockState(ActivityContent.MAT.get(),legacy?49:i));}
 public static void main(String[] args){
 var l=new ServerLevel();var a=new BlockPos(0,0,0);var b=new BlockPos(7,0,0);assembly(l,a,false);var first=mat(l,a);check(first.size()==49,"single");assembly(l,b,false);check(first.equals(mat(l,a)),"neighbor changes original");check(Collections.disjoint(first,mat(l,b))&&mat(l,b).size()==49,"merged assemblies");
 for(var p:first)check(mat(l,p).equals(first),"unstable clicked tile");check(first.getFirst().equals(a.offset(-3,0,-3)),"controller migrated");
 l.states.put(a.offset(-3,0,-3),new BlockState(ActivityContent.MAT.get(),49));check(mat(l,a).isEmpty(),"borrows legacy tile");assembly(l,a,true);check(mat(l,a).size()==49&&mat(l,b).size()==49,"legacy adjacency");assembly(l,a,false);l.loaded=false;check(mat(l,a).isEmpty(),"unloaded assembly accepted");l.loaded=true;
 var block=ActivityContent.MAT.get();for(int i=0;i<49;i++){var pos=a.offset(KarateMatBlock.offset(i));var params=new LootParams.Builder(l,pos);check(block.getDrops(l.getBlockState(pos),params).size()==1,"missing loot from tile "+i);check(params.selected.tile==24,"not center loot "+i);params.suppress=true;check(block.getDrops(l.getBlockState(pos),params).isEmpty(),"lost context "+i);}
 var edge=a.offset(-3,0,-3);var edgeState=l.getBlockState(edge);l.states.remove(a);check(block.getDrops(edgeState,new LootParams.Builder(l,edge)).isEmpty(),"orphan duplicate drop");check(block.getDrops(new BlockState(block,49),new LootParams.Builder(l,edge)).size()==1,"legacy loot changed");
 System.out.println(assertions+" isolated checks passed using extracted production mat/getDrops methods; Minecraft APIs are stubs, not a native test.");
 }
}
'''
if __name__ == '__main__':
 from isolated_java_support import run_java_source
 run_java_source('TatamiCheck', source)
