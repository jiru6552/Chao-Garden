package com.chaogarden.test;

import com.chaogarden.core.LobbyApproachGeometry0255;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.LobbyApproachDecor0255;
import com.chaogarden.hub.LobbyDecor025;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real finite transactions in an offset private test footprint, without resetting server unlocks. */
@GameTestHolder("chao_lobby81") @PrefixGameTestTemplate(false)
public final class GardenLobbyGameTests081 {
  private static final int FLAGS=2|16;
  @GameTestGenerator public static Collection<TestFunction> tests(){
    var out=new ArrayList<TestFunction>();
    for(int mask=0;mask<4;mask++){final int m=mask;out.add(new TestFunction("lobby81_plan","independent_authored_access_mask_"+m,"chao_lobby81:activity_test",100,0,true,h->blueprint(h,m)));}
    out.add(new TestFunction("lobby81_transaction","protected_edit_aborts_then_retry_and_occupancy_safeguard","chao_lobby81:activity_test",100,0,true,GardenLobbyGameTests081::transaction));
    out.add(new TestFunction("lobby81_legacy","exact_old_open_lobby_closes_without_rebuilding_other_cells","chao_lobby81:activity_test",100,0,true,GardenLobbyGameTests081::legacy));
    return out;
  }
  private static BlockState at(Map<BlockPos,BlockState> plan,BlockPos pos){return plan.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
  private static void blueprint(GameTestHelper h,int mask){
    var p=HubRooms.plan081((mask&1)!=0,(mask&2)!=0).blocks;
    var main=p.get(HubRooms.SCENE_ANCHOR081);
    h.assertTrue(main.getValue(LobbyDecor025.SceneAnchor.GARDEN_LAYOUT81)&&LobbyDecor025.sceneAccess081(main)==mask,"Native scene state carries the independent client mask");
    var encoded=net.minecraft.nbt.NbtUtils.writeBlockState(main);
    var restored=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),encoded);
    h.assertTrue(restored.equals(main)&&LobbyDecor025.sceneAccess081(restored)==mask,"Vanilla block-state persistence retains the render mask for reload and late chunk delivery");
    for(boolean hero:new boolean[]{true,false}){
      boolean open=(mask&(hero?1:2))!=0;var gate=at(p,hero?HubRooms.HERO_DOOR:HubRooms.DARK_DOOR);
      h.assertTrue(open?gate.is(LobbyDecor025.SCENE_ANCHOR.get())&&gate.getValue(LobbyDecor025.SceneAnchor.PORTAL):gate.isAir(),"Portal presence follows only its own side");
      for(var cell:LobbyApproachGeometry0255.nativeCells().keySet()){
        if((cell.x()>0)!=hero)continue;var state=at(p,new BlockPos(cell.x(),cell.y(),cell.z()));
        if(!open)h.assertTrue(!state.is(LobbyApproachDecor0255.FLOOR_STAIRS.get()),"Locked side contains no staircase collision cell");
      }
    }
    if((mask&2)==0)for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++)if(Math.hypot(x,z)<=25&&x<0)
      h.assertTrue(at(p,new BlockPos(x,79,z)).is(LobbyDecor025.GLASS_FLOOR.get()),"Locked Dark side keeps its entire main-floor disk solid");
    if((mask&1)==0)for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++)if(Math.hypot(x,z)<=25&&x>0)
      h.assertTrue(at(p,new BlockPos(x,79,z)).is(LobbyDecor025.GLASS_FLOOR.get()),"Locked Hero side keeps its entire main-floor disk solid");
    h.assertTrue(LobbyDecor025.sceneAccess081(LobbyDecor025.SCENE_ANCHOR.get().defaultBlockState())==3,"Unmigrated legacy scene retains matching old visible geometry until a guarded commit");h.succeed();
  }
  private static HubRooms.Refresh081 refresh(GameTestHelper h,BlockPos origin,int mask){
    try{var method=HubRooms.class.getDeclaredMethod("refreshPlan081",net.minecraft.server.level.ServerLevel.class,BlockPos.class,int.class);method.setAccessible(true);return (HubRooms.Refresh081)method.invoke(null,h.getLevel(),origin,mask);}
    catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  private static final class Fixture implements AutoCloseable {
    final GameTestHelper h;final BlockPos origin;final LinkedHashMap<BlockPos,BlockState> saved=new LinkedHashMap<>();final BlockPos sentinel;
    Fixture(GameTestHelper h,int mask,boolean old){
      this.h=h;origin=h.absolutePos(new BlockPos(0,old?70:150,0));
      for(var local:HubRooms.plan081(true,true).blocks.keySet())remember(local.offset(origin));
      for(var local:HubRooms.plan081(false,false).blocks.keySet())remember(local.offset(origin));
      sentinel=origin.offset(0,77,1);remember(sentinel);
      for(var p:saved.keySet())h.assertTrue(h.getLevel().getBlockState(p).isAir(),"Offset fixture refuses to overwrite preexisting blocks");
      var plan=HubRooms.plan081((mask&1)!=0,(mask&2)!=0).blocks;
      for(var entry:plan.entrySet())h.getLevel().setBlock(entry.getKey().offset(origin),entry.getValue(),FLAGS);
      if(old)h.getLevel().setBlock(HubRooms.SCENE_ANCHOR081.offset(origin),LobbyDecor025.SCENE_ANCHOR.get().defaultBlockState(),FLAGS);
      h.getLevel().setBlock(sentinel,Blocks.DIAMOND_BLOCK.defaultBlockState(),FLAGS);
    }
    void remember(BlockPos p){saved.putIfAbsent(p,h.getLevel().getBlockState(p));}
    Map<BlockPos,BlockState> snapshot(){var m=new LinkedHashMap<BlockPos,BlockState>();for(var p:saved.keySet())m.put(p,h.getLevel().getBlockState(p));return m;}
    int mask(){return LobbyDecor025.sceneAccess081(h.getLevel().getBlockState(HubRooms.SCENE_ANCHOR081.offset(origin)));}
    public void close(){for(var entry:saved.entrySet())h.getLevel().setBlock(entry.getKey(),entry.getValue(),FLAGS);}
  }
  private static void transaction(GameTestHelper h){
    try(var f=new Fixture(h,0,false)){
      var first=refresh(h,f.origin,1);h.assertTrue(first.applied()&&first.changed()>0&&f.mask()==1,"Hero access installs alone");
      var gate=HubRooms.DARK_DOOR.offset(f.origin);h.getLevel().setBlock(gate,Blocks.CHEST.defaultBlockState(),FLAGS);
      var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(gate);chest.setItem(0,new ItemStack(Items.DIAMOND,3));
      var before=f.snapshot();var blocked=refresh(h,f.origin,3);
      h.assertTrue(!blocked.applied()&&blocked.changed()==0&&blocked.conflicts().contains(gate)&&f.mask()==1,"Player edit blocks the entire Dark transition before floor/portal changes");
      h.assertTrue(before.equals(f.snapshot())&&chest.getItem(0).getCount()==3,"Blocked patch preserves every cell and the real chest inventory");
      // Removing the fixture's real chest invokes vanilla onRemove and drops
      // its contents. Recover only newly released, matching fixture goods;
      // otherwise the production occupied-cell guard correctly defers retry.
      var dropArea=new net.minecraft.world.phys.AABB(gate).inflate(2);
      var priorDrops=new HashSet<UUID>();for(var e:h.getLevel().getEntitiesOfClass(ItemEntity.class,dropArea))priorDrops.add(e.getUUID());
      h.getLevel().setBlock(gate,Blocks.AIR.defaultBlockState(),FLAGS);
      var released=h.getLevel().getEntitiesOfClass(ItemEntity.class,dropArea,e->!priorDrops.contains(e.getUUID())
          &&(ItemStack.isSameItemSameComponents(e.getItem(),new ItemStack(Items.DIAMOND))||ItemStack.isSameItemSameComponents(e.getItem(),new ItemStack(Items.CHEST))));
      int diamonds=released.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
      int chests=released.stream().filter(e->e.getItem().is(Items.CHEST)).mapToInt(e->e.getItem().getCount()).sum();
      h.assertTrue(diamonds==3&&chests<=1,"Removing the owned fixture chest returns its exact three preserved diamonds without duplicating its shell; diamonds="+diamonds+" chests="+chests);
      for(var e:released)e.discard();
      var both=refresh(h,f.origin,3);h.assertTrue(both.applied()&&f.mask()==3,"Restoring the exact expected cell permits a safe retry"
          +" applied="+both.applied()+" changed="+both.changed()+" conflicts="+both.conflicts()
          +" nearbyActors="+h.getLevel().getEntities((net.minecraft.world.entity.Entity)null,dropArea,e->!e.isRemoved()).stream()
              .map(e->e.getClass().getSimpleName()+"@"+e.position()+" id="+e.getUUID()+(e instanceof ItemEntity item?" stack="+item.getItem():"")).toList());
      h.assertTrue(refresh(h,f.origin,3).changed()==0,"Repeating the installed transition does not duplicate blocks");
      h.assertTrue(refresh(h,f.origin,0).applied(),"Isolated geometry fixture can return to its own closed baseline without changing persistent unlocks");
      BlockPos floor=HubRooms.plan081(false,false).blocks.keySet().stream().filter(p->p.getY()==79&&LobbyApproachGeometry0255.floorOpening(p.getX(),p.getZ())).findFirst().orElseThrow().offset(f.origin);
      var item=new ItemEntity(h.getLevel(),floor.getX()+.5,floor.getY()+1,floor.getZ()+.5,new ItemStack(Items.APPLE));item.setNoGravity(true);h.getLevel().addFreshEntity(item);
      try{var occupied=refresh(h,f.origin,2);h.assertTrue(!occupied.applied()&&occupied.changed()==0&&f.mask()==0,"A real actor on changing floor support delays the whole opening");}
      finally{item.discard();}
      h.assertTrue(refresh(h,f.origin,2).applied()&&f.mask()==2,"Vacating support permits the same production transaction");
      h.assertTrue(h.getLevel().getBlockState(f.sentinel).is(Blocks.DIAMOND_BLOCK),"No room-wide rebuild touches the unrelated keeper edit");h.succeed();
    }
  }
  private static void legacy(GameTestHelper h){
    try(var f=new Fixture(h,3,true)){
      var result=refresh(h,f.origin,0);h.assertTrue(result.applied()&&result.changed()>0&&f.mask()==0,"The exact old always-open gate plan migrates to locked");
      for(var cell:LobbyApproachGeometry0255.nativeCells().keySet())h.assertTrue(!h.getLevel().getBlockState(new BlockPos(cell.x(),cell.y(),cell.z()).offset(f.origin)).is(LobbyApproachDecor0255.FLOOR_STAIRS.get()),"Legacy stairs are absent after commit");
      h.assertTrue(h.getLevel().getBlockState(HubRooms.DARK_DOOR.offset(f.origin)).isAir()&&h.getLevel().getBlockState(HubRooms.HERO_DOOR.offset(f.origin)).isAir(),"Both old portal anchors close together with the floor");
      h.assertTrue(h.getLevel().getBlockState(f.sentinel).is(Blocks.DIAMOND_BLOCK),"Legacy migration preserves unrelated blocks inside the room bounds");h.succeed();
    }
  }
}
