package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.FloatToyContent080;
import com.chaogarden.toys.FloatToyEntity080;
import com.chaogarden.toys.FloatToyRoutes080;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual player collision movement and physical hull ticks; no toy position/velocity seeding. */
@GameTestHolder("chao_fixes84") @PrefixGameTestTemplate(false)
public final class FloatToyBumpGameTests084 {
  private record Fixture(FloatToyEntity080 toy,ServerPlayer player,Vec3 origin) {}
  private FloatToyBumpGameTests084(){}
  @GameTestGenerator public static Collection<TestFunction> tests(){
    var out=new ArrayList<TestFunction>();
    for(String kind:List.of("chao_rubber_duck","chao_lifesaver")){
      for(boolean water:new boolean[]{false,true})for(Direction direction:Direction.Plane.HORIZONTAL)
        add(out,"contact_"+kind+"_"+(water?"water":"land")+"_"+direction,80,h->contact(h,kind,water,direction));
      for(boolean adult:new boolean[]{false,true})add(out,"rider_"+kind+"_"+adult,100,h->occupied(h,kind,adult));
      for(String mode:List.of("claim","push","reposition","board","preview","pending"))
        add(out,"anchor_"+kind+"_"+mode,100,h->anchor(h,kind,mode));
      add(out,"remote_and_spectator_"+kind,60,h->rejected(h,kind));
      add(out,"duplicate_contact_once_"+kind,80,h->duplicateContact(h,kind));
      add(out,"reload_and_single_pickup_"+kind,100,h->reload(h,kind));
      add(out,"solid_wall_"+kind,80,h->wall(h,kind));
    }
    return out;
  }
  private static void add(List<TestFunction> out,String name,int ticks,Consumer<GameTestHelper> test){
    out.add(new TestFunction("float_bump84_"+(out.size()/5),name,"chao_fixes84:activity_test",ticks,0,true,test));
  }
  private static void cleanup(GameTestHelper h,Runnable task){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,GameTestRunner r){task.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){task.run();}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){task.run();}
  });}
  private static Fixture fixture(GameTestHelper h,String kind,boolean water){
    for(int x=1;x<=22;x++)for(int z=1;z<=22;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);h.setBlock(new BlockPos(x,1,z),water?Blocks.WATER:Blocks.STONE);
      for(int y=2;y<=6;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    Vec3 at=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(12,2,12)));
    if(water)at=new Vec3(at.x,FloatToyRoutes080.surfaceAt(h.getLevel(),at)-FloatToyRoutes080.DRAFT,at.z);
    var stack=new ItemStack(ChaoGarden.item(kind));stack.set(DataComponents.CUSTOM_NAME,Component.literal("Bump persistence "+kind));
    var toy=FloatToyContent080.newCandidate(h.getLevel(),kind,stack,at,0);
    h.assertTrue(toy!=null&&h.getLevel().addFreshEntity(toy),"Single physical float publishes");toy.commit();
    cleanup(h,()->toy.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
    var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(water);player.setOnGround(!water);
    player.teleportTo(at.x-4,at.y,at.z);cleanup(h,()->{if(player.server.getPlayerList().getPlayer(player.getUUID())==player)player.server.getPlayerList().remove(player);});
    return new Fixture(toy,player,at);
  }
  private static Vec3 vector(Direction direction){return new Vec3(direction.getStepX(),0,direction.getStepZ());}
  private static void behind(Fixture f,Direction direction){Vec3 p=f.toy.position().subtract(vector(direction).scale(.84));f.player.teleportTo(p.x,p.y,p.z);}
  private static void walk(Fixture f,Direction direction){f.player.setDeltaMovement(Vec3.ZERO);f.player.move(MoverType.PLAYER,vector(direction).scale(.10).add(0,f.player.isNoGravity()?0:-.08,0));}
  private static void solePayload(GameTestHelper h,Fixture f){
    h.assertTrue(f.toy.committed()&&!f.toy.isRemoved()&&f.toy.storedItem().getCount()==1,"Exactly one committed payload remains");
    h.assertTrue(h.getLevel().getEntitiesOfClass(FloatToyEntity080.class,new AABB(f.origin,f.origin).inflate(10),e->e.committed()).size()==1,"Bumping creates no duplicate float");
    h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(f.origin,f.origin).inflate(10),e->e.getItem().is(ChaoGarden.item(f.toy.kind()))).isEmpty(),"Bumping never drops a second toy");
  }
  private static void contact(GameTestHelper h,String kind,boolean water,Direction direction){
    Fixture f=fixture(h,kind,water);behind(f,direction);AtomicBoolean velocity=new AtomicBoolean(),stepped=new AtomicBoolean();var previous=new AtomicReference<>(f.toy.position());
    h.onEachTick(()->{
      if(h.testInfo.isDone())return;
      walk(f,direction);Vec3 now=f.toy.position();
      h.assertTrue(now.subtract(previous.get()).horizontalDistance()<=.081,"Contact motion remains bounded physical movement, not a snap");previous.set(now);
      if(f.toy.getDeltaMovement().horizontalDistanceSqr()>1e-7)velocity.set(true);
      if(f.player.getY()>f.origin.y+.05)stepped.set(true);
    });
    h.runAtTickTime(45,()->{
      Vec3 delta=f.toy.position().subtract(f.origin);double along=delta.dot(vector(direction));
      h.assertTrue(velocity.get()&&along>.30,"Walking into "+kind+" really pushes it "+direction+" on "+(water?"water":"land")+": "+delta);
      h.assertTrue(delta.subtract(vector(direction).scale(along)).horizontalDistance()<.05,"Push follows the contacted side");
      h.assertTrue(f.toy.rider()==null&&f.toy.claimant().isEmpty(),"A player bump does not claim or mount the toy");
      if(water)h.assertTrue(f.toy.afloat(),"Water contact retains ordinary buoyancy");
      else if(kind.equals("chao_lifesaver"))h.assertTrue(stepped.get(),"Ordinary ground contact includes auto-stepping onto the low ring");
      solePayload(h,f);h.succeed();
    });
  }
  private static ChaoEntity rider(GameTestHelper h,Fixture f,boolean adult){
    ChaoEntity c=ChaoGarden.CHAO.get().create(h.getLevel());var state=new ChaoState();state.adult=adult;
    state.align=adult?1:0;state.primaryType=adult?ChaoState.FLY_TYPE:ChaoState.NORMAL;state.hunger=95;state.energy=95;state.points[ChaoState.SWIM]=667;
    c.setState(ChaoState.decode(state.encode()));c.setNoAi(true);c.setNoGravity(true);c.setInvulnerable(true);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("red_wool_beanie")));
    c.moveTo(f.toy.position().add(-.9,0,0));h.assertTrue(h.getLevel().addFreshEntity(c),"Real Chao publishes");
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    cleanup(h,()->c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));return c;
  }
  private static void occupied(GameTestHelper h,String kind,boolean adult){
    Fixture f=fixture(h,kind,true);ChaoEntity c=rider(h,f,adult);var go=new AtomicBoolean();var start=new AtomicReference<Vec3>();
    h.runAtTickTime(3,()->h.assertTrue(f.toy.board(c),"Normal physical boarding succeeds"));
    h.runAtTickTime(25,()->{
      h.assertTrue(f.toy.phase()==FloatToyEntity080.RIDE&&f.toy.rider()==c,"Entry completes before rider bumping");
      start.set(f.toy.position());behind(f,Direction.EAST);go.set(true);
    });
    h.onEachTick(()->{if(go.get()&&!h.testInfo.isDone()){
      walk(f,Direction.EAST);h.assertTrue(f.toy.rider()==c&&c.isAlive()&&c.getVehicle()==f.toy,"Player contact never ejects the real rider: tick="+h.getTick()+" phase="+f.toy.phase()+" toyRemoved="+f.toy.isRemoved()+" chaoRemoved="+c.isRemoved()+" alive="+c.isAlive()+" rider="+f.toy.rider()+" vehicle="+c.getVehicle());
      h.assertTrue(c.position().distanceToSqr(f.toy.boardPoint(c))<.02,"Rider stays connected to its physical seat");
    }});
    h.runAtTickTime(65,()->{
      h.assertTrue(f.toy.getX()-start.get().x>.2,"Occupied stable ride also responds to player contact");
      h.assertTrue(f.toy.claimedBy(c)&&f.toy.phase()==FloatToyEntity080.RIDE&&f.toy.afloat(),"Claim and safe ride survive movement");
      h.assertTrue(c.state().adult==adult&&c.state().primaryType==(adult?ChaoState.FLY_TYPE:ChaoState.NORMAL)
          &&c.state().align==(adult?1:0)&&c.getItemBySlot(EquipmentSlot.HEAD).is(ChaoGarden.item("red_wool_beanie")),"Age category, locked form, alignment and equipment remain intact");
      solePayload(h,f);h.succeed();
    });
  }
  private static void anchor(GameTestHelper h,String kind,String mode){
    boolean water=!mode.equals("push")&&!mode.equals("reposition")&&!mode.equals("claim");Fixture f=fixture(h,kind,water);
    ChaoEntity c=mode.equals("preview")?null:rider(h,f,true);var go=new AtomicBoolean();var start=new AtomicReference<Vec3>();
    int begin=mode.equals("pending")?25:3,finish=mode.equals("board")?10:begin+25;
    if(mode.equals("pending"))h.runAtTickTime(3,()->h.assertTrue(f.toy.board(c),"Pending-removal fixture boards normally"));
    h.runAtTickTime(begin,()->{
      switch(mode){
        case "claim"->h.assertTrue(f.toy.claim(c),"Chao owns the approaching float");
        case "push"->{f.toy.pushing(c,f.toy.position().add(0,0,1));h.assertTrue(f.toy.phase()==FloatToyEntity080.PUSH,"Chao push remains active");}
        case "reposition"->f.toy.pauseLaunch(c);
        case "board"->h.assertTrue(f.toy.board(c),"Real entry starts before bump test");
        case "preview"->f.toy.previewHeld(true);
        case "pending"->{
          h.assertTrue(f.toy.phase()==FloatToyEntity080.RIDE,"Real entry has completed");
          // The ordinary removal controller may still seek a safe shore. A
          // high solid rim gives this case no landing, so only player-induced
          // movement is under test while the rider safely waits aboard.
          for(int i=1;i<=22;i++)for(int y=2;y<=5;y++)for(BlockPos at:List.of(new BlockPos(1,y,i),new BlockPos(22,y,i),new BlockPos(i,y,1),new BlockPos(i,y,22)))h.setBlock(at,Blocks.STONE);
          f.toy.hurt(h.getLevel().damageSources().generic(),1);
        }
      }
      h.assertTrue(!f.toy.isPushable(),"Anchored controller phase rejects external bumping: "+mode);
      start.set(f.toy.position());behind(f,Direction.EAST);go.set(true);
    });
    h.onEachTick(()->{if(go.get()&&!h.testInfo.isDone())walk(f,Direction.EAST);});
    h.runAtTickTime(finish,()->{
      h.assertTrue(Math.abs(f.toy.getX()-start.get().x)<.001,"Player cannot move an anchored float sideways: "+mode);
      if(mode.equals("push"))h.assertTrue(f.toy.getZ()>start.get().z+.04,"Existing authorized Chao push still moves forward");
      else h.assertTrue(f.toy.position().subtract(start.get()).horizontalDistance()<.001,"Anchor keeps its validated horizontal point");
      if(mode.equals("board")||mode.equals("pending"))h.assertTrue(f.toy.rider()==c&&c.isAlive(),"Anchoring keeps its real passenger safe");
      solePayload(h,f);h.succeed();
    });
  }
  private static void rejected(GameTestHelper h,String kind){
    Fixture f=fixture(h,kind,false);Vec3 origin=f.toy.position();f.toy.push(f.player);
    h.runAtTickTime(8,()->{
      h.assertTrue(f.toy.position().subtract(origin).horizontalDistance()<.001,"Remote push callback cannot move the toy");
      f.player.setGameMode(GameType.SPECTATOR);behind(f,Direction.EAST);f.toy.push(f.player);
    });
    h.runAtTickTime(20,()->{
      h.assertTrue(f.player.isSpectator()&&f.toy.position().subtract(origin).horizontalDistance()<.001,"Spectator overlap never supplies a push");
      solePayload(h,f);h.succeed();
    });
  }
  private static void duplicateContact(GameTestHelper h,String kind){
    Fixture f=fixture(h,kind,false);var start=new AtomicReference<Vec3>();var injected=new AtomicBoolean();
    var hullTicks=new java.util.concurrent.atomic.AtomicInteger();var serverTick=new java.util.concurrent.atomic.AtomicInteger();
    Consumer<ServerTickEvent.Pre> contact=event->{
      if(event.getServer()!=h.getLevel().getServer()||h.testInfo.isDone()||injected.get()
          ||f.toy.tickCount<2||!h.getLevel().isPositionEntityTicking(f.toy.blockPosition()))return;
      // GameTestTicker runs after world entities. Inject before the world's
      // actual hull tick so100callbacks AND its own contact query belong to
      // the same server tick, rather than banking two legitimate tick contacts.
      f.player.setNoGravity(true);f.player.teleportTo(f.toy.getX()-.82,f.toy.getY(),f.toy.getZ());
      hullTicks.set(f.toy.tickCount);serverTick.set(event.getServer().getTickCount());
      start.set(f.toy.position());for(int i=0;i<100;i++)f.toy.push(f.player);
      injected.set(true);
    };
    NeoForge.EVENT_BUS.addListener(contact);cleanup(h,()->NeoForge.EVENT_BUS.unregister(contact));
    h.onEachTick(()->{
      if(h.testInfo.isDone()||!injected.get()||f.toy.tickCount<=hullTicks.get())return;
      h.assertTrue(f.toy.tickCount==hullTicks.get()+1,"Exactly one genuine ready hull tick supplies the dedup sample");
      h.assertTrue(h.getLevel().getServer().getTickCount()==serverTick.get(),"Callbacks and actual hull contact query share one server tick");
      double moved=f.toy.getX()-start.get().x;
      h.assertTrue(Math.abs(moved-.04)<.001,"One player/server tick supplies one0.04 impulse despite100 callbacks and the actual contact scan: moved="+moved+" velocity="+f.toy.getDeltaMovement());
      h.assertTrue(Math.abs(f.toy.getDeltaMovement().x-.026)<.001,"Real hull physics applies ordinary0.65drag once: velocity="+f.toy.getDeltaMovement());
      solePayload(h,f);h.succeed();
    });
  }
  private static void reload(GameTestHelper h,String kind){
    Fixture f=fixture(h,kind,false);behind(f,Direction.EAST);var active=new AtomicBoolean(true);h.onEachTick(()->{if(active.get()&&!h.testInfo.isDone())walk(f,Direction.EAST);});
    h.runAtTickTime(25,()->{
      h.assertTrue(f.toy.getX()-f.origin.x>.2,"Real contact moved toy before ordinary save");active.set(false);f.player.teleportTo(f.toy.getX()-4,f.toy.getY(),f.toy.getZ());
      var tag=new CompoundTag();h.assertTrue(f.toy.save(tag),"Bumped physical toy saves");Vec3 position=f.toy.position();var uuid=f.toy.getUUID();
      f.toy.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);var loaded=EntityType.loadEntityRecursive(tag,h.getLevel(),entity->entity);
      h.assertTrue(loaded instanceof FloatToyEntity080,"Existing float entity codec reloads");var restored=(FloatToyEntity080)loaded;
      h.assertTrue(h.getLevel().addFreshEntity(restored),"Reloaded toy publishes once");cleanup(h,()->restored.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
      h.assertTrue(restored.getUUID().equals(uuid)&&restored.position().equals(position)&&restored.storedItem().getCount()==1
          &&restored.storedItem().getHoverName().getString().equals("Bump persistence "+kind),"Identity, real position and item components survive reload");
      f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.player.setShiftKeyDown(true);
      h.assertTrue(restored.interact(f.player,InteractionHand.MAIN_HAND).consumesAction(),"Existing shift-empty-hand pickup still succeeds");
      int count=0;for(int i=0;i<f.player.getInventory().getContainerSize();i++){
        ItemStack item=f.player.getInventory().getItem(i);if(item.is(ChaoGarden.item(kind))){count+=item.getCount();h.assertTrue(item.getHoverName().getString().equals("Bump persistence "+kind),"Recovered payload keeps components");}
      }
      h.assertTrue(restored.isRemoved()&&count==1&&restored.storedItem().isEmpty(),"Bump/reload/pickup transfers exactly one real payload");h.succeed();
    });
  }
  private static void wall(GameTestHelper h,String kind){
    Fixture f=fixture(h,kind,false);int wall=h.absolutePos(new BlockPos(14,2,12)).getX();
    for(int z=9;z<=15;z++)for(int y=2;y<=4;y++)h.setBlock(new BlockPos(14,y,z),Blocks.STONE);
    behind(f,Direction.EAST);h.onEachTick(()->{if(!h.testInfo.isDone())walk(f,Direction.EAST);});
    h.runAtTickTime(55,()->{
      h.assertTrue(f.toy.getX()-f.origin.x>.2,"Player really pushed towards the wall");
      h.assertTrue(f.toy.getBoundingBox().maxX<=wall+.0001&&h.getLevel().noCollision(f.toy,f.toy.getBoundingBox().deflate(.0001)),"Ordinary physical hull cannot be pushed through a solid wall");
      solePayload(h,f);h.succeed();
    });
  }
}
