package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPersonality;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import com.chaogarden.interaction.ChaoFruitSeekingGoal;
import com.chaogarden.interaction.ChaoPickupItemGoal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server decisions and real transactions across two bodies, saves and lifecycle boundaries. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class BehaviorGameTests {
  private static void room(GameTestHelper h) {
    for(int x=1;x<=17;x++)for(int z=1;z<=14;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=10;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
  }
  private static ChaoEntity chao(GameTestHelper h,int x,int z,boolean ai) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Chao creates");
    var s=new ChaoState();s.adult=true;s.hunger=80;s.energy=100;s.happiness=30;
    s.points[ChaoState.RUN]=667;c.setState(s);c.setNoAi(!ai);c.getRandom().setSeed(x*31+z);
    var at=h.absolutePos(new BlockPos(x,1,z));c.moveTo(Vec3.atBottomCenterOf(at));
    h.getLevel().addFreshEntity(c);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);return c;
  }
  private static void readySocial(ChaoEntity c) {
    var data=c.exportRelationships();data.putLong("NextSocial",0);c.restoreRelationships(data);
  }
  @GameTest(template="activity_test",timeoutTicks=340)
  public static void completedTalksUnlockGossipAndAffectionAndCommitExactlyOnce(GameTestHelper h) {
    room(h);var a=chao(h,3,4,false);var b=chao(h,4,4,false);
    h.assertTrue(!a.startSocialSession(b,Behavior.GOSSIP,20)&&!a.startSocialSession(b,Behavior.HUG,20),
        "Strangers must talk before gossiping or hugging");
    for(int i=0;i<10;i++) {
      final int expected=i+1;
      h.runAtTickTime(1+i*26,()->{
        readySocial(a);readySocial(b);
        h.assertTrue(a.startSocialSession(b,Behavior.TALK,20),"Two available Chao begin a conversation");
      });
      h.runAtTickTime(25+i*26,()->{
        h.assertTrue(a.completedConversations(b.getUUID())==expected&&b.completedConversations(a.getUUID())==expected,
            "Each complete conversation increments both records exactly once");
        if(expected==5) {
          readySocial(a);readySocial(b);
          h.assertTrue(a.startSocialSession(b,Behavior.GOSSIP,20),"Five completed talks unlock gossip");
          a.clearBehavior();
          h.assertTrue(!b.isBehaving(),"Interrupting either body cancels its partner without a reward");
        }
      });
    }
    h.runAtTickTime(270,()->{
      readySocial(a);readySocial(b);
      h.assertTrue(a.startSocialSession(b,Behavior.HUG,30),"Ten completed talks unlock a mutual hug");
      var saved=new CompoundTag();a.saveWithoutId(saved);UUID identity=a.stableFriendIdentity();a.discard();
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);
      h.assertTrue(restored.stableFriendIdentity().equals(identity)&&restored.completedConversations(b.getUUID())==10,
          "Reload retains the stable friend identity and completed history");
      h.assertTrue(!restored.isBehaving(),"An unfinished saved shared action grants no completion");
      h.runAfterDelay(40,()->{
        h.assertTrue(restored.completedConversations(b.getUUID())==10&&b.completedConversations(identity)==10,
            "Neither a cancelled hug nor its stale partner creates a conversation");h.succeed();
      });
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void lonelyChaoActuallyApproachAndTalkWithoutPlayerCommands(GameTestHelper h) {
    room(h);var a=chao(h,3,6,true);var b=chao(h,9,6,true);Vec3 start=a.position();
    for(var c:List.of(a,b)) {
      c.state().setEmotion(ChaoEmotion.LONELY,10000);c.state().setPersonalitySlider(ChaoPersonality.CHATTY,100);
      c.goalSelector.addGoal(2,new ChaoBehaviorGoal(c));c.sync();
    }
    var observed=new AtomicBoolean();h.onEachTick(()->{
      if(a.getBehavior()==Behavior.TALK&&b.getBehavior()==Behavior.TALK)observed.set(true);
    });
    h.succeedWhen(()->{
      h.assertTrue(observed.get(),"Both Chao participate in the same timed conversation");
      h.assertTrue(a.completedConversations(b.getUUID())>0&&b.completedConversations(a.getUUID())>0,
          "The voluntary conversation completes on the server");
      h.assertTrue(a.distanceToSqr(start)>1||b.getX()<h.absolutePos(new BlockPos(8,1,6)).getX(),
          "Social selection causes actual navigation rather than remote posing");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1200)
  public static void illnessSymptomsDoNotCureButOneConsideredMedicineDoes(GameTestHelper h) {
    room(h);var c=chao(h,3,4,true);c.state().setIllness(ChaoIllness.COUGH,-100);
    c.state().setEmotion(ChaoEmotion.CONDITION,0);c.goalSelector.addGoal(2,new ChaoBehaviorGoal(c));
    var offered=new AtomicBoolean();var saved=new CompoundTag();var live=new ChaoEntity[]{c};
    h.onEachTick(()->{
      if(!offered.get()&&c.getBehavior()==Behavior.COUGH) {
        offered.set(true);h.assertTrue(c.state().isIll(),"A visible cough is a symptom, not a cure");
        var p=h.makeMockPlayer(GameType.SURVIVAL);c.owner=p.getUUID();
        var medicine=new ItemStack(ChaoGarden.item("chao_medicine"),2);p.setItemInHand(InteractionHand.MAIN_HAND,medicine);
        c.offerItem(p,InteractionHand.MAIN_HAND);
        h.assertTrue(c.state().isIll()&&medicine.getCount()==1&&c.getItemAction()==ChaoEntity.ItemAction.THINK,
            "One real dose transfers to the hands without an instant cure");
        c.setNoAi(true);c.setNoGravity(true);
        h.runAfterDelay(45,()->{
          h.assertTrue(c.state().isIll()&&c.getItemAction()==ChaoEntity.ItemAction.TRAIN,
              "Treatment follows a complete consideration and medicine animation");
          c.saveWithoutId(saved);c.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());
          restored.load(saved);h.getLevel().addFreshEntity(restored);live[0]=restored;
        });
        h.runAfterDelay(115,()->{
          h.assertTrue(!live[0].state().isIll()&&live[0].state().illness(ChaoIllness.COUGH)==100
              &&live[0].state().illnessImmunityTicks(ChaoIllness.COUGH)>23000&&live[0].getMainHandItem().isEmpty(),
              "The saved real dose commits one cure with post-treatment immunity");
          h.assertTrue(medicine.getCount()==1,"Treatment never consumes the unoffered inventory dose");
          h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,live[0].getBoundingBox().inflate(4),
              e->e.getItem().is(ChaoGarden.item("chao_medicine"))).isEmpty(),"The consumed dose is never also returned");h.succeed();
        });
      }
    });
  }
  @GameTest(template="activity_test",timeoutTicks=130)
  public static void pettingDisturbsSleepAndGivesARealAwakeRecoveryPeriod(GameTestHelper h) {
    room(h);var c=chao(h,3,4,false);var p=h.makeMockPlayer(GameType.SURVIVAL);c.owner=p.getUUID();
    c.state().sleeping=true;c.state().setEmotion(ChaoEmotion.SLEEPINESS,9500);c.sync();
    h.runAtTickTime(3,()->{
      h.assertTrue(c.isResting(),"A sleepy Chao rests");
      p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);c.mobInteract(p,InteractionHand.MAIN_HAND);
      h.assertTrue(!c.state().sleeping&&c.getBehavior()==Behavior.GROGGY
          &&c.state().emotion(ChaoEmotion.TIREDNESS)>=2900,"Waking by petting adds tiredness and a groggy reaction");
    });
    h.runAtTickTime(35,()->{
      h.assertTrue(!c.state().sleeping&&!c.isResting(),"The disturbed Chao does not fall asleep again on the next tick");
      c.state().setEmotion(ChaoEmotion.SLEEPINESS,0);c.state().setEmotion(ChaoEmotion.TIREDNESS,0);
      c.state().energy=100;c.state().sleeping=true;c.sync();
    });
    h.runAtTickTime(40,()->{
      h.assertTrue(!c.state().sleeping&&!c.isResting(),"A fully recovered Chao also wakes naturally");h.succeed();
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1400)
  public static void hungryChaoShakesAnActualTreeThenPicksUpAndEatsItsFruit(GameTestHelper h) {
    room(h);var base=h.absolutePos(new BlockPos(9,1,7));h.getLevel().setBlock(base.below(),Blocks.DIRT.defaultBlockState(),3);
    h.assertTrue(FruitContent.growTree(h.getLevel(),base,"garden_fruit"),"A real supported orchard tree grows");
    var pods=FruitContent.podPositions(h.getLevel(),FruitContent.crownPosition(base),"garden_fruit");
    for(var pos:pods)h.getLevel().setBlock(pos,h.getLevel().getBlockState(pos).setValue(HangingFruitBlock.AGE,3),3);
    var c=chao(h,4,7,true);c.state().hunger=15;c.sync();Vec3 start=c.position();
    c.goalSelector.addGoal(3,new ChaoFruitSeekingGoal(c));c.goalSelector.addGoal(5,new ChaoPickupItemGoal(c));
    var shaking=new AtomicBoolean();var held=new AtomicBoolean();h.onEachTick(()->{
      if(c.getBehavior()==Behavior.SHAKE_TREE)shaking.set(true);if(c.isHandlingItem())held.set(true);
    });
    h.succeedWhen(()->{
      h.assertTrue(shaking.get()&&c.distanceToSqr(start)>2,"Hungry Chao walks to the trunk and shakes it");
      h.assertTrue(held.get()&&c.state().fruitCount>0&&c.state().hunger>15,"Dropped fruit enters the normal held-item eating transaction"
          +" heldObserved="+held.get()+" meals="+c.state().fruitCount+" fullness="+c.state().hunger
          +" pos="+c.position()+" action="+c.getItemAction()+" behavior="+c.getBehavior()
          +" eligible="+c.canInspectNearbyItems()+" ground="+c.onGround()+" sleep="+c.isResting()
          +" drops="+h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(8)).stream()
              .map(e->e.getItem()+"@"+e.position()+" delay="+e.hasPickUpDelay()+" sight="+c.hasLineOfSight(e)).toList());
      h.assertTrue(pods.stream().anyMatch(pos->h.getLevel().getBlockState(pos).getValue(HangingFruitBlock.AGE)==0),
          "Eating uses a harvested pod, rather than inventing fruit");
    });
  }
  private static void cornerFruitPickup(GameTestHelper h,boolean adult){
    room(h);var c=chao(h,8,8,true);c.state().adult=adult;c.state().hunger=15;c.sync();
    var origin=h.absolutePos(new BlockPos(8,1,8));
    // Same reached path cell, opposite corners: this reproduces the orchard
    // stall while remaining inside the real held-item transaction's reach.
    c.setPos(origin.getX()+.235,origin.getY(),origin.getZ()+.121);
    var fruit=new ItemEntity(h.getLevel(),origin.getX()+.971,origin.getY(),origin.getZ()+.955,
        new ItemStack(ChaoGarden.item("garden_fruit")));
    fruit.setDeltaMovement(Vec3.ZERO);fruit.setNoPickUpDelay();h.getLevel().addFreshEntity(fruit);
    h.assertTrue(c.distanceToSqr(fruit)>1.2&&c.distanceToSqr(fruit)<2.25,"regression drop occupies the old distance gap");
    c.goalSelector.addGoal(5,new ChaoPickupItemGoal(c));var held=new AtomicBoolean();
    h.onEachTick(()->{if(c.isHandlingItem())held.set(true);});
    h.succeedWhen(()->{
      h.assertTrue(held.get()&&c.state().fruitCount==1&&c.state().hunger>15,"reached-cell fruit enters one real held-item meal");
      h.assertTrue(fruit.isRemoved()&&c.getMainHandItem().isEmpty(),"source consumed once with no stranded hand item");
    });
  }
  @GameTest(template="activity_test",timeoutTicks=260) public static void babyPicksUpFruitAtFarCornerOfReachedCell(GameTestHelper h){cornerFruitPickup(h,false);}
  @GameTest(template="activity_test",timeoutTicks=260) public static void adultPicksUpFruitAtFarCornerOfReachedCell(GameTestHelper h){cornerFruitPickup(h,true);}
  @GameTest(template="activity_test",timeoutTicks=280)
  public static void reincarnationWaitsForCocoonAndTransfersAllRealEquipmentOnce(GameTestHelper h) {
    room(h);var c=chao(h,4,5,false);c.state().happiness=90;c.state().adultAgeTicks=GardenConfig.ADULT_TICKS.get()-1;
    c.owner=UUID.randomUUID();UUID owner=c.owner;UUID identity=c.stableFriendIdentity();
    EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.OFFHAND,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    ItemStack[] worn=new ItemStack[slots.length];
    for(int i=0;i<slots.length;i++) {
      worn[i]=new ItemStack(Items.GOLD_NUGGET);worn[i].set(DataComponents.CUSTOM_NAME,Component.literal("Paid costume "+slots[i].getName()));
      c.setItemSlot(slots[i],worn[i].copy());
    }
    var area=new AABB(h.absolutePos(new BlockPos(4,1,5))).inflate(5);
    h.runAtTickTime(30,()->h.assertTrue(c.isAlive()&&c.isRebirthCocoon()
        &&h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,area,e->owner.equals(e.owner)).isEmpty(),
        "Reincarnation keeps the real Chao in a cocoon before creating an egg"));
    h.runAtTickTime(215,()->{
      var eggs=h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,area,e->owner.equals(e.owner));
      h.assertTrue(c.isRemoved()&&eggs.size()==1,"Exactly one egg replaces the completed cocoon");
      eggs.getFirst().setIncubation(GardenConfig.HATCH_TICKS.get()-1);
    });
    h.runAtTickTime(225,()->{
      var children=h.getLevel().getEntitiesOfClass(ChaoEntity.class,area,e->owner.equals(e.owner));
      h.assertTrue(children.size()==1&&children.getFirst().state().reincarnations==1,"The reborn egg hatches exactly one next life");
      var born=children.getFirst();h.assertTrue(born.stableFriendIdentity().equals(identity),"Friend identity survives a new entity and life");
      for(int i=0;i<slots.length;i++)h.assertTrue(ItemStack.matches(born.getItemBySlot(slots[i]),worn[i]),
          "Rebirth retains exact paid equipment components in "+slots[i]);
      h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(Items.GOLD_NUGGET)).isEmpty(),
          "Transferred equipment is never also dropped");h.succeed();
    });
  }
  @GameTest(template="activity_test",timeoutTicks=720)
  public static void birthdayCalendarWaitsWelcomesGuestsAndCannotDuplicateCakeOnReload(GameTestHelper h) {
    room(h);var host=chao(h,4,5,false);var guest=chao(h,8,5,true);
    int year=GardenConfig.BIRTHDAY_YEAR_TICKS.get();h.assertTrue(year>0,"Test uses the enabled default birthday calendar");
    host.state().adultAgeTicks=year;host.state().hunger=50;host.sync();
    guest.goalSelector.addGoal(2,new ChaoBehaviorGoal(guest));
    h.assertTrue(host.startBirthdayParty()&&!host.startBirthdayParty(),"A year boundary spends one annual party credit before starting");
    var joined=new AtomicBoolean();var saved=new CompoundTag();var live=new ChaoEntity[]{host};
    h.onEachTick(()->{if(guest.getBehavior()==Behavior.PARTY)joined.set(true);});
    h.runAtTickTime(220,()->{
      h.assertTrue(joined.get(),"An awake nearby Chao walks to the host and joins the birthday party");
      h.assertTrue(host.state().fruitCount==0&&host.getMainHandItem().isEmpty(),"The host waits before receiving cake");
      host.saveWithoutId(saved);host.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());
      restored.load(saved);h.getLevel().addFreshEntity(restored);live[0]=restored;
      h.assertTrue(restored.getBehavior()==Behavior.BIRTHDAY&&restored.getBehaviorTicks()<300,
          "Reload retains the remaining wait instead of restarting a birthday");
    });
    h.runAtTickTime(530,()->h.assertTrue(live[0].isHandlingItem()&&ChaoEntity.isBirthdayServing(live[0].getMainHandItem())
        &&live[0].state().fruitCount==0,"One generated birthday cake enters the usual held-food decision"));
    h.runAtTickTime(650,()->{
      h.assertTrue(live[0].state().fruitCount==1&&live[0].getMainHandItem().isEmpty()&&!live[0].canStartBirthday(),
          "Exactly one cake is eaten and the same adult year cannot start another party");
      h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,live[0].getBoundingBox().inflate(5),
          e->ChaoEntity.isBirthdayServing(e.getItem())).isEmpty(),"An eaten birthday serving is not also a collectible copy");h.succeed();
    });
  }
}
