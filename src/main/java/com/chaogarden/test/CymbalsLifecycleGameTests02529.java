package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoSounds;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

/** Live AI sessions on one real placed block, including actual dispatched sound events. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class CymbalsLifecycleGameTests02529 {
  private static final Map<ChaoEntity,List<Long>> SOUNDS=new WeakHashMap<>();
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition event) {
    if(event.getLevel().isClientSide||event.getSound()==null||event.getSound().value()!=ChaoSounds.CYMBALS.get())return;
    SOUNDS.forEach((chao,ticks)->{
      if(chao.level()==event.getLevel()&&event.getPosition().distanceToSqr(chao.position().add(0,chao.getBbHeight()*.55,0))<1e-8)
        ticks.add(chao.level().getGameTime());
    });
  }
  static BlockPos floor(GameTestHelper h) {
    for(int x=1;x<=20;x++)for(int z=1;z<=20;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var pos=h.absolutePos(new BlockPos(9,1,9));
    h.getLevel().setBlock(pos,ToyContent.block(CymbalsPlayRules.KIND).defaultBlockState(),3);return pos;
  }
  static ChaoEntity actor(GameTestHelper h,BlockPos pos,boolean adult,int type,boolean animalArms) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"real Chao creates");
    var s=new ChaoState();s.adult=adult;s.primaryType=type;s.happiness=20;s.hunger=90;s.energy=100;s.boredom=100;
    s.personality=ChaoPreferences.EASILY_BORED;if(adult)s.points[ChaoState.RUN]=667;
    if(animalArms)for(int i=0;i<ChaoState.ANIMALS.length;i++)if((ChaoState.animalSlots(i)&(1<<ChaoState.ARMS))!=0){s.animalParts[ChaoState.ARMS]=i;break;}
    s.setTraitInt("learned.cymbals",1);c.setState(s);c.setInvulnerable(true);c.getRandom().setSeed(18);c.moveTo(pos.getX()-1.5,pos.getY(),pos.getZ()+.5,0,0);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));SOUNDS.put(c,new ArrayList<>());return c;
  }
  private static long start(ChaoEntity c){return c.getPersistentData().getLong("ChaoToyUntil")-CymbalsPlayRules.DURATION;}
  private static int drops(GameTestHelper h,BlockPos pos) {
    return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3),e->e.getItem().is(ChaoGarden.item(CymbalsPlayRules.KIND)))
        .stream().mapToInt(e->e.getItem().getCount()).sum();
  }
  private static void complete(GameTestHelper h,boolean adult,int type,boolean animalArms) {
    var pos=floor(h);var c=actor(h,pos,adult,type,animalArms);var began=new AtomicLong(-1);
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;
      began.compareAndSet(-1,start(c));
      h.assertTrue(!c.isNoAi()&&c.toyPlayingKind().equals(CymbalsPlayRules.KIND),"ordinary live AI selects the placed cymbals");
      h.assertTrue(c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty(),"presentation never mints a held inventory stack");
      var block=h.getLevel().getBlockState(pos);
      h.assertTrue(block.is(ToyContent.block(CymbalsPlayRules.KIND)),"one recoverable source stays placed");
      h.assertTrue(block.getRenderShape()==RenderShape.INVISIBLE,"ground model hidden for both native and replacement hands");
      h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"Chao remains clear of source collision");
    });
    h.succeedWhen(()->{
      h.assertTrue(began.get()>=0&&h.getLevel().getGameTime()>=began.get()+CymbalsPlayRules.DURATION+12,"observe full live session and recovery");
      h.assertTrue(!ToyContent.isPlaying(c)&&!c.getPersistentData().contains("ChaoToyTarget"),"completed session releases ownership");
      h.assertTrue(c.state().happiness==24,"age-appropriate reward granted exactly once");
      var state=h.getLevel().getBlockState(pos);
      h.assertTrue(!state.getValue(ChaoToyBlock.PLAYING)&&state.getRenderShape()==RenderShape.MODEL,"cymbals is visible and idle again");
      h.assertTrue(drops(h,pos)==0,"no duplicate cymbals drops");
      var soundTicks=SOUNDS.get(c);var expected=new ArrayList<Long>();
      for(int t=0;t<CymbalsPlayRules.DURATION;t++)if(CymbalsPlayRules.soundDue(t))expected.add(began.get()+t);
      h.assertTrue(soundTicks.equals(expected),"actual server sounds match all 4 contact keyframes exactly: "+soundTicks+" expected "+expected);
      SOUNDS.remove(c);
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyUsesPlacedCymbalsWithSynchronizedSound(GameTestHelper h){complete(h,false,ChaoState.NORMAL,false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultUsesPlacedCymbalsWithSynchronizedSound(GameTestHelper h){complete(h,true,ChaoState.NORMAL,false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultRunUsesPlacedCymbalsWithSynchronizedSound(GameTestHelper h){complete(h,true,ChaoState.RUN_TYPE,false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void animalArmsUsePlacedCymbals(GameTestHelper h){complete(h,true,ChaoState.NORMAL,true);}

  static void interrupt(GameTestHelper h,String change,boolean adult) {
    var pos=floor(h);var c=actor(h,pos,adult,ChaoState.NORMAL,false);var changed=new AtomicLong(-1);
    var seen=new ArrayList<ChaoEntity>();seen.add(c);var countAtCancel=new int[1];
    var expectedHappiness=new int[]{20};
    var keeper=new net.minecraft.server.level.ServerPlayer[1];
    Runnable cleanup=()->{if(keeper[0]!=null&&keeper[0].server.getPlayerList().getPlayer(keeper[0].getUUID())==keeper[0])keeper[0].server.getPlayerList().remove(keeper[0]);};
    h.testInfo.addListener(new GameTestListener(){
      @Override public void testStructureLoaded(GameTestInfo info){}
      @Override public void testPassed(GameTestInfo info,GameTestRunner runner){cleanup.run();}
      @Override public void testFailed(GameTestInfo info,GameTestRunner runner){cleanup.run();}
      @Override public void testAddedForRerun(GameTestInfo oldInfo,GameTestInfo newInfo,GameTestRunner runner){cleanup.run();}
    });
    h.onEachTick(()->{
      if(changed.get()>=0||!ToyContent.isPlaying(c)||h.getLevel().getGameTime()-start(c)<40)return;
      changed.set(h.getLevel().getGameTime());countAtCancel[0]=SOUNDS.get(c).size();
      if(change.equals("break"))h.getLevel().destroyBlock(pos,true);
      else if(change.equals("move"))c.setPos(c.getX()+3,c.getY(),c.getZ());
      else if(change.equals("stay"))c.setStaying(true);
      else if(change.equals("ride")){
        var boat=new net.minecraft.world.entity.vehicle.Boat(h.getLevel(),c.getX(),c.getY(),c.getZ());
        h.getLevel().addFreshEntity(boat);h.assertTrue(c.startRiding(boat,true),"Chao enters rescue boat");
      }
      else if(change.equals("item"))c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE));
      else if(change.equals("hurt")){
        c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"real damage interrupts play");
        h.assertTrue(c.isBehaving()&&c.state().happiness<20,"damage produces the real care response");
        expectedHappiness[0]=c.state().happiness;
      }
      else if(change.equals("pet")){
        keeper[0]=TestConnections.makeSurvivalServerPlayer(h);keeper[0].getInventory().clearContent();
        keeper[0].teleportTo(c.getX()+1,c.getY(),c.getZ());c.owner=keeper[0].getUUID();
        h.assertTrue(c.mobInteract(keeper[0],net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"real empty-hand care interaction accepted");
        h.assertTrue(c.getAffectionTicks()>0&&c.state().happiness==22,"petting begins and grants only its normal care reward");
        expectedHappiness[0]=22;
      }
      else {
        var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();
        var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);h.getLevel().addFreshEntity(restored);seen.add(restored);
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(changed.get()>=0&&h.getLevel().getGameTime()>=changed.get()+20,"observe interrupted session");
      var current=seen.get(seen.size()-1);
      h.assertTrue(!ToyContent.isPlaying(current)&&current.state().happiness==expectedHappiness[0],"cancellation grants no toy reward");
      h.assertTrue(SOUNDS.get(c).size()==countAtCancel[0],"no shake sound after cancellation");
      var state=h.getLevel().getBlockState(pos);
      if(change.equals("break"))h.assertTrue(state.isAir()&&drops(h,pos)==1,"breaking held source drops exactly one real cymbals");
      else h.assertTrue(state.is(ToyContent.block(CymbalsPlayRules.KIND))&&!state.getValue(ChaoToyBlock.PLAYING)
          &&state.getRenderShape()==RenderShape.MODEL&&drops(h,pos)==0,"cancellation returns the one placed cymbals");
      SOUNDS.remove(c);
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1800) public static void breakingCymbalsStopsSoundAndDropsOnce(GameTestHelper h){interrupt(h,"break",true);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void displacedCymbalsPlayerStopsWithoutReward(GameTestHelper h){interrupt(h,"move",true);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void reloadedCymbalsPlayerReturnsSourceWithoutReward(GameTestHelper h){interrupt(h,"reload",true);}

  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyDamageCancels(GameTestHelper h){interrupt(h,"hurt",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultDamageCancels(GameTestHelper h){interrupt(h,"hurt",true);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyPetCancels(GameTestHelper h){interrupt(h,"pet",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultPetCancels(GameTestHelper h){interrupt(h,"pet",true);}
  @GameTest(template="activity_test",timeoutTicks=600) public static void unlearnedBabyCannotPlay(GameTestHelper h){unlearned(h,false);}
  @GameTest(template="activity_test",timeoutTicks=600) public static void unlearnedAdultCannotPlay(GameTestHelper h){unlearned(h,true);}
  private static void unlearned(GameTestHelper h,boolean adult){
    var p=floor(h);var c=actor(h,p,adult,ChaoState.NORMAL,false);c.state().setTraitInt("learned.cymbals",0);c.sync();
    int start=c.tickCount;
    h.onEachTick(()->h.assertTrue(!ToyContent.isPlaying(c)&&!c.getPersistentData().contains("ChaoToyTarget"),"unlearned Chao cannot reserve cymbals"));
    h.succeedWhen(()->{h.assertTrue(c.tickCount-start>=200,"observe actual AI selection attempts");
      h.assertTrue(c.state().happiness==20&&h.getLevel().getBlockState(p).getRenderShape()==RenderShape.MODEL,"lesson gate retains visible source and happiness");SOUNDS.remove(c);});
  }
}
