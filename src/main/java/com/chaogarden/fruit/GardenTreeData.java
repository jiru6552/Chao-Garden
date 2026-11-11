package com.chaogarden.fruit;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Tracks only newly planted trees. Authored and pre-upgrade trees stay permanent.
 * Timings are Minecraft adaptations: 60 active minutes, Strong 120, then a
 * one-minute bare-tree phase. No offline aging or forced chunk loading.
 */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class GardenTreeData extends SavedData {
  public static final int ORDINARY_LIFE=72000,STRONG_LIFE=144000,WITHER_TICKS=1200;
  private static final Factory<GardenTreeData> FACTORY=new Factory<>(GardenTreeData::new,GardenTreeData::load);
  private final Map<BlockPos,Tree> trees=new LinkedHashMap<>();
  private static final class Tree {
    final String fruit;int age;boolean grown;long lastWater=Long.MIN_VALUE;
    Tree(String fruit){this.fruit=fruit;}
    int life(){return fruit.equals("strong_fruit")?STRONG_LIFE:ORDINARY_LIFE;}
    boolean permanent(){return com.chaogarden.activities.GardenArea.FOOD.contains(fruit);}
    boolean withered(){return !permanent()&&grown&&age>=life();}
  }
  public static GardenTreeData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"chao_planted_trees");}
  private void prune(ServerLevel level) {
    if(trees.entrySet().removeIf(e->level.isAreaLoaded(e.getKey(),2)&&!validRoot(level,e.getKey(),e.getValue())))setDirty();
  }
  private static boolean validRoot(ServerLevel level,BlockPos pos,Tree tree) {
    return tree.grown?FruitContent.isTrunk(level.getBlockState(pos)):level.getBlockState(pos).is(FruitContent.saplingForFruit(tree.fruit));
  }
  public boolean canPlant(ServerLevel level,BlockPos pos) {
    prune(level);
    if(HubContent.isGardenDimension(level)&&trees.size()>=4)return false;
    // Reserve full future canopies for seedlings too, before either has grown.
    return trees.keySet().stream().noneMatch(p->Math.abs(p.getX()-pos.getX())<=4
        &&Math.abs(p.getZ()-pos.getZ())<=4&&Math.abs(p.getY()-pos.getY())<7);
  }
  public int count(){return trees.size();}
  public void planted(BlockPos pos,String fruit){trees.put(pos.immutable(),new Tree(fruit));setDirty();}
  public void grown(BlockPos pos){Tree tree=trees.get(pos);if(tree!=null){tree.grown=true;tree.age=0;setDirty();}}
  public boolean withered(BlockPos root){Tree t=trees.get(root);return t!=null&&t.withered();}
  public boolean mayWater(BlockPos root,long now){Tree t=trees.get(root);return t==null||!t.withered()&&(t.lastWater==Long.MIN_VALUE||now-t.lastWater>=200);}
  public boolean watered(BlockPos root,long now) {
    Tree tree=trees.get(root);if(tree==null||!mayWater(root,now))return false;
    tree.lastWater=now;
    if(tree.grown)tree.age=Math.max(0,tree.age-200); // ten seconds per watering; no stored infinite lifespan.
    setDirty();return true;
  }
  @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
    if(event.getLevel() instanceof ServerLevel level&&level.getGameTime()%20==0)
      get(level).advance(level,20);
  }
  public void advance(ServerLevel level,int ticks) {
    prune(level);
    var it=trees.entrySet().iterator();boolean changed=false;
    while(it.hasNext()) {
      var entry=it.next();BlockPos root=entry.getKey();Tree tree=entry.getValue();
      if(tree.grown&&!tree.withered()&&level.isAreaLoaded(root,3))FruitContent.migratePlantedPalm(level,root,tree.fruit);
      if(tree.permanent()||!tree.grown||!level.isAreaLoaded(root,3)||!level.hasNearbyAlivePlayer(root.getX(),root.getY(),root.getZ(),64))continue;
      boolean wasWithered=tree.withered();tree.age+=Math.max(0,Math.min(ticks,20));changed=true;
      if(!wasWithered&&tree.withered())clearTree(level,root,tree,false);
      if(tree.age>=tree.life()+WITHER_TICKS){clearTree(level,root,tree,true);it.remove();}
    }
    if(changed)setDirty();
  }
  private static void clearTree(ServerLevel level,BlockPos root,Tree tree,boolean wood) {
    // Clear only this tree's exact generated footprint and matching block types.
    BlockPos crown=FruitContent.crownPosition(root);
    for(BlockPos pod:FruitContent.podPositions(level,crown,tree.fruit))
      if(level.getBlockState(pod).is(FruitContent.podForFruit(tree.fruit)))level.removeBlock(pod,false);
    var crownState=level.getBlockState(crown);
    boolean garden=crownState.is(FruitContent.treeForFruit(tree.fruit))&&crownState.getValue(ChaoTreeCrownBlock.GARDEN);
    for(int y=0;!garden&&y<3;y++) {
      int r=y==2?1:2;
      for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++) {
        if(Math.abs(x)==r&&Math.abs(z)==r)continue;
        BlockPos at=crown.offset(x,y,z);
        if(level.getBlockState(at).is(FruitContent.LEAVES.get())&&!level.getBlockState(at).getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT))level.removeBlock(at,false);
      }
    }
    if(garden)
      level.setBlock(crown,crownState.setValue(ChaoTreeCrownBlock.WITHERED,true),3);
    if(!wood)return;
    for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL) {
      BlockPos at=crown.relative(side);if(!garden&&level.getBlockState(at).is(FruitContent.TRUNK.get()))level.removeBlock(at,false);
    }
    if(level.getBlockState(crown).is(FruitContent.treeForFruit(tree.fruit)))level.removeBlock(crown,false);
    for(int y=3;y>=0;y--)if(level.getBlockState(root.above(y)).is(FruitContent.TRUNK.get()))level.removeBlock(root.above(y),false);
  }
  public static GardenTreeData load(CompoundTag tag,HolderLookup.Provider registries) {
    var data=new GardenTreeData();var list=tag.getList("Trees",10);
    for(int i=0;i<list.size();i++) {
      var row=list.getCompound(i);String fruit=FruitCatalog.canonical(row.getString("Fruit"));
      if(!FruitContent.FRUITS.contains(fruit))continue;
      Tree tree=new Tree(fruit);tree.grown=row.getBoolean("Grown");tree.age=Math.max(0,Math.min(tree.life()+WITHER_TICKS,row.getInt("Age")));
      tree.lastWater=row.contains("LastWater")?row.getLong("LastWater"):Long.MIN_VALUE;
      data.trees.put(BlockPos.of(row.getLong("Root")),tree);
    }
    return data;
  }
  @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
    var list=new ListTag();trees.forEach((pos,tree)->{
      var row=new CompoundTag();row.putLong("Root",pos.asLong());row.putString("Fruit",tree.fruit);
      row.putBoolean("Grown",tree.grown);row.putInt("Age",tree.age);row.putLong("LastWater",tree.lastWater);list.add(row);
    });tag.put("Trees",list);return tag;
  }
}
