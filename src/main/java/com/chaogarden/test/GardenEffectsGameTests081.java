package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.GardenProgressionRules081;
import com.chaogarden.core.GardenProgressionRules081.Garden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.GardenCare081;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HeroGardenRooms0253;
import com.chaogarden.hub.DarkGardenRooms0255;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real entity care and the existing exclusive companion body, with no new clock scheduler. */
@GameTestHolder("chao_garden_effect81") @PrefixGameTestTemplate(false)
public final class GardenEffectsGameTests081 {
  private static final String TEMPLATE="chao_garden_effect81:activity_test";
  private static void near(GameTestHelper h,double actual,double expected,String why){h.assertTrue(Math.abs(actual-expected)<.000004,why+" actual="+actual+" expected="+expected);}
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static ChaoState adult(float alignment){
    var s=new ChaoState();s.adult=true;s.align=alignment;s.primaryType=ChaoState.RUN_TYPE;s.hunger=20;s.energy=100;s.setEvolutionStrength(0);
    return ChaoState.decode(s.encode());
  }
  @GameTestGenerator public static Collection<TestFunction> tests(){return List.of(
      new TestFunction("garden_effect81_loaded","actual_gardens_apply_rates_fruit_alignment_and_reload_once",TEMPLATE,300,0,true,GardenEffectsGameTests081::loaded),
      new TestFunction("garden_effect81_equipped","actual_equipped_body_changes_location_without_double_care_or_item_aging",TEMPLATE,500,0,true,GardenEffectsGameTests081::equipped));}
  private static final class Probe {
    final ServerLevel level;final Garden garden;final boolean child,food;final BlockPos floor;final BlockState previous;final float alignment;
    ChaoEntity actor;ItemEntity source;int initialAge,initialAdultAge;long initialClock;float initialStrength,initialAlignment;
    Probe(ServerLevel level,Garden garden,boolean child,boolean food,int index){
      this.level=level;this.garden=garden;this.child=child;this.food=food;alignment=child?0:food?-.8f:index==0?-.8f:index==1?0:.8f;
      floor=new BlockPos(4+index*2,119,8);previous=level.getBlockState(floor);
      level.setBlock(floor,Blocks.STONE.defaultBlockState(),3);
      var state=child?new ChaoState():adult(alignment);state.hunger=20;
      actor=ChaoGarden.CHAO.get().create(level);actor.setState(state);actor.setNoAi(true);actor.setNoGravity(true);actor.setOnGround(true);
      actor.moveTo(Vec3.atBottomCenterOf(floor.above()));actor.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
      if(!level.addFreshEntity(actor))throw new IllegalStateException("Actual progression fixture publication rejected");
    }
    void begin(){initialAge=actor.state().ageTicks;initialAdultAge=actor.state().adultAgeTicks;initialClock=level.getGameTime();initialStrength=actor.state().evolutionStrength();initialAlignment=actor.state().align;}
  }
  private record Forced(ServerLevel level,ChunkPos chunk){}
  private static void loaded(GameTestHelper h){
    var server=h.getLevel().getServer();var levels=List.of(server.getLevel(HubContent.GARDEN),server.getLevel(HubContent.HERO_GARDEN),server.getLevel(HubContent.DARK_GARDEN));
    h.assertTrue(levels.stream().allMatch(level->level!=null),"All three actual garden dimensions exist");
    var forced=new ArrayList<Forced>();var probes=new ArrayList<Probe>();boolean[] finished={false};
    cleanup(h,()->{if(finished[0])return;finished[0]=true;for(var p:probes){if(p.actor!=null)p.actor.discard();if(p.source!=null)p.source.discard();p.level.setBlock(p.floor,p.previous,3);}for(var f:forced)f.level.setChunkForced(f.chunk.x,f.chunk.z,false);});
    for(var level:levels){
      h.assertTrue(HubContent.ensure(level),"Existing authored garden scenery can be prepared before placing care probes");
      var garden=GardenCare081.garden(level);
      h.assertTrue(garden==(level.dimension().equals(HubContent.HERO_GARDEN)?Garden.HERO:level.dimension().equals(HubContent.DARK_GARDEN)?Garden.DARK:Garden.NORMAL),"Resolver uses the actual existing dimension identity");
      var chunk=new ChunkPos(new BlockPos(8,120,8));if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(chunk.x,chunk.z,true);forced.add(new Forced(level,chunk));}
      for(int variant=0;variant<5;variant++)probes.add(new Probe(level,garden,variant==3,variant==4,variant));
    }
    h.startSequence().thenWaitUntil(()->{
      for(var p:probes)h.assertTrue(p.level.isPositionEntityTicking(p.actor.blockPosition())&&p.actor.state().ageTicks>0,"Waiting for actual ticking garden actors");
    }).thenExecute(()->{
      for(var p:probes){p.begin();if(p.food){
        p.actor.setOnGround(true);p.source=new ItemEntity(p.level,p.actor.getX()+.1,p.actor.getY(),p.actor.getZ(),new ItemStack(ChaoGarden.item("chao_fruit")));
        p.source.setNoPickUpDelay();p.source.setNoGravity(true);p.level.addFreshEntity(p.source);
        h.assertTrue(p.actor.pickUpForInspection(p.source)&&p.source.isRemoved()&&p.actor.state().fruitCount==0,"Real one-item fruit enters the held transaction before any benefit");
      }}
    }).thenExecuteAfter(40,()->{
      for(var p:probes)if(!p.food)verifyProbe(h,p,false);
      var p=probes.stream().filter(v->v.food&&v.garden==Garden.HERO).findFirst().orElseThrow();
      var saved=new CompoundTag();p.actor.saveWithoutId(saved);var uuid=p.actor.getUUID();p.actor.discard();
      var replacement=ChaoGarden.CHAO.get().create(p.level);replacement.load(saved);h.assertTrue(p.level.addFreshEntity(replacement),"Mid-meal entity reload republishes successfully");p.actor=replacement;
      h.assertTrue(p.actor.getUUID().equals(uuid)&&p.actor.state().fruitCount==0&&p.actor.isHandlingItem(),"Reload retains the same Chao and uncommitted real fruit");
    }).thenExecuteAfter(120,()->{
      for(var p:probes)verifyProbe(h,p,p.food);
      h.succeed();
    });
  }
  private static void verifyProbe(GameTestHelper h,Probe p,boolean meal){
    var s=p.actor.state();double childTicks=com.chaogarden.core.GradualEvolutionRules.durationTicks(GardenConfig.CHILD_TICKS.get());
    int elapsed=(p.child?s.ageTicks:s.adultAgeTicks)-(p.child?p.initialAge:p.initialAdultAge);
    h.assertTrue(Math.abs(elapsed-(p.level.getGameTime()-p.initialClock))<=1,"Loaded care advances once per actual garden tick (one tick allowed for cross-dimension/reload phase)");
    double rate=p.child?1/childTicks:GardenProgressionRules081.adultMultiplier(p.garden)/(4*childTicks);
    near(h,s.evolutionStrength(),p.initialStrength+elapsed*rate+(meal?.005:0),"Actual loaded strength follows this garden's rate with exactly one independent fruit commit");
    h.assertTrue(!p.actor.isRemoved()&&s.ageTicks-p.initialAge==elapsed,"Location does not accelerate biological time");
    h.assertTrue(p.actor.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN),"Care and reload retain actual head equipment");
    if(p.child){near(h,s.align,p.initialAlignment+elapsed*GardenProgressionRules081.childAlignmentDelta(p.garden,GardenConfig.CHILD_TICKS.get()),"Actual child alignment nudges only toward its current garden");h.assertTrue(!s.adult,"Garden multiplier does not accelerate first evolution");}
    else h.assertTrue(s.lockedAlignment()==p.alignment&&s.primaryType==ChaoState.RUN_TYPE,"Actual adult keeps its alignment and first type in all three gardens");
    for(int stat=0;stat<5;stat++)h.assertTrue(s.levels[stat]==0&&s.points[stat]==0&&s.progress[stat]==(meal?50:0),"Gardens do not multiply or invent numerical stat training");
    if(meal)h.assertTrue(s.fruitCount==1&&p.actor.getMainHandItem().isEmpty(),"Saved or ordinary meal commits exactly once");
  }
  private static void awaitBody(GameTestHelper h,CompanionSkillGameTests.Equipped e,ServerLevel level,long deadline,Consumer<ChaoEntity> ready){
    var entity=CompanionContent.findEntity(e.keeper().server,e.chao());
    if(entity instanceof ChaoEntity c&&c.level()==level&&!c.isRemoved()&&CompanionContent.isManaged(c)){ready.accept(c);return;}
    h.assertTrue(h.getTick()<deadline,"Actual equipped body did not reach its player's actual garden within the bounded window");h.runAfterDelay(1,()->awaitBody(h,e,level,deadline,ready));
  }
  private static int care(ChaoEntity body){return body.getPersistentData().getInt("ChaoEquippedCareTicks");}
  private static int careDelta(int before,ChaoEntity body){return Math.floorMod(care(body)-before,6000);}
  private static void equipped(GameTestHelper h){
    var s=adult(.8f);var e=CompanionSkillGameTests.equip(h,s);var p=e.keeper();boolean[] cleaned={false};
    cleanup(h,()->{if(cleaned[0])return;cleaned[0]=true;CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);p.server.getPlayerList().remove(p);});
    h.runAfterDelay(12,()->{
      var hero=p.server.getLevel(HubContent.HERO_GARDEN);h.assertTrue(hero!=null,"Actual Hero dimension is available");
      p.changeDimension(new DimensionTransition(hero,HeroGardenRooms0253.SPAWN,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING));
      awaitBody(h,e,hero,h.getTick()+100,body->{
        float initial=body.state().evolutionStrength();int before=care(body);long heroClock=hero.getGameTime();
        p.getAbilities().instabuild=false;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_fruit")));
        h.assertTrue(body.offerItem(p,InteractionHand.MAIN_HAND).consumesAction()&&body.isHandlingItem()&&body.state().fruitCount==0,"A real equipped-body fruit begins without applying its benefit immediately");
        h.runAfterDelay(140,()->{
          var current=(ChaoEntity)CompanionContent.findEntity(p.server,e.chao());
          h.assertTrue(current!=null&&current.level()==hero,"Actual equipped Hero body remains in the same garden");
          h.assertTrue(Math.abs(careDelta(before,current)-(hero.getGameTime()-heroClock))<=1,"Equipped care runs once per actual Hero tick");
          near(h,current.state().evolutionStrength(),initial+careDelta(before,current)*2/(4.0*GardenConfig.CHILD_TICKS.get())+.005,"Equipped Hero passive rate is doubled exactly once and real fruit remains independent");
          h.assertTrue(current.state().ageTicks==0&&current.state().adultAgeTicks==0&&current.state().fruitCount==1,"Equipped lifespan remains paused and actual fruit commits once");
          var snapshot=ChaoCarrierItem.state(e.inventory().getItem(0));h.assertTrue(snapshot.fruitCount==1&&snapshot.lockedAlignment()==.8f&&snapshot.primaryType==ChaoState.RUN_TYPE,"Real carrier snapshot synchronizes fruit and locked identity");
          var dark=p.server.getLevel(HubContent.DARK_GARDEN);p.changeDimension(new DimensionTransition(dark,DarkGardenRooms0255.SPAWN,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING));
          awaitBody(h,e,dark,h.getTick()+100,next->{
            float darkInitial=next.state().evolutionStrength();int darkBefore=care(next);long darkClock=dark.getGameTime();
            h.runAfterDelay(30,()->{
              var last=(ChaoEntity)CompanionContent.findEntity(p.server,e.chao());h.assertTrue(last!=null&&last.level()==dark,"Actual equipped body follows the real Dark transition");
              h.assertTrue(Math.abs(careDelta(darkBefore,last)-(dark.getGameTime()-darkClock))<=1,"Equipped care runs once per actual Dark tick");
              near(h,last.state().evolutionStrength(),darkInitial+careDelta(darkBefore,last)*.5/(4.0*GardenConfig.CHILD_TICKS.get()),"The same Hero Chao receives the Dark half-rate without a second care clock");
              h.assertTrue(last.state().ageTicks==0&&last.state().adultAgeTicks==0&&last.state().lockedAlignment()==.8f&&last.state().primaryType==ChaoState.RUN_TYPE,"Moving an equipped adult to Dark cannot change alignment, first type or lifespan");
              var held=e.inventory().getItem(0);CompanionContent.unequip(p,held);e.inventory().setItem(0,ItemStack.EMPTY);String stored=ChaoCarrierItem.state(held).encode();p.setItemInHand(InteractionHand.MAIN_HAND,held);
              h.runAfterDelay(25,()->{
                h.assertTrue(CompanionContent.findEntity(p.server,e.chao())==null&&ChaoCarrierItem.state(held).encode().equals(stored),"Unequipped inventory Chao receives neither off-world age nor garden credit");h.succeed();
              });
            });
          });
        });
      });
    });
  }
}
