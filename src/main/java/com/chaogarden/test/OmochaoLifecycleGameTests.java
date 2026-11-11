package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.cwe.CweContent;
import com.chaogarden.cwe.CweRobotPartItem;
import com.chaogarden.cwe.OmochaoLifecycle;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEggEntity;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real actor/item/NBT transactions for the finite mechanical life, not a simulated output counter. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class OmochaoLifecycleGameTests {
  private OmochaoLifecycleGameTests(){}
  private static ChaoEntity builder(GameTestHelper h,String name,int x,UUID owner){
    for(int floorX=Math.max(0,x-3);floorX<=Math.min(15,x+3);floorX++)for(int floorZ=0;floorZ<=6;floorZ++)
      h.setBlock(new BlockPos(floorX,1,floorZ),Blocks.STONE);
    var chao=ChaoGarden.CHAO.get().create(h.getLevel());var s=ChaoState.egg(4,false,true,0,new Random(h.getLevel().random.nextLong()));
    s.adult=true;s.happiness=90;s.energy=100;s.hunger=80;s.setTrait("reward.wrench",1);
    chao.setState(s);chao.owner=owner;chao.setCustomName(Component.literal(name));chao.setNoAi(true);chao.setNoGravity(true);
    var at=h.absolutePos(new BlockPos(x,2,3));chao.moveTo(at.getX()+.1,at.getY(),at.getZ()+.1);h.getLevel().addFreshEntity(chao);return chao;
  }
  private static ChaoEntity built(GameTestHelper h,ChaoEntity builder,ItemStack first){
    h.assertTrue(CweContent.assembleOmochao(builder,first),"First real reconstruction part was refused");
    if(!first.isEmpty())first.shrink(1);
    var robot=(ChaoEntity)h.getLevel().getEntity(UUID.fromString(builder.state().trait("content.omochao_uuid")));
    UUID actor=robot.getUUID();
    for(int i=1;i<4;i++){h.assertTrue(CweContent.assembleOmochao(builder),"Remaining Parts Box failed");h.assertTrue(robot.getUUID().equals(actor),"Parts replaced the prototype actor");}
    robot.setNoAi(true);robot.setNoGravity(true);return robot;
  }
  private static List<ItemEntity> outputs(GameTestHelper h,UUID actor){
    return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(BlockPos.ZERO)).inflate(32),item->{
      var tag=CweRobotPartItem.origin(item.getItem());return tag.hasUUID("Robot")&&tag.getUUID("Robot").equals(actor);
    });
  }
  private static ItemStack endAsHeart(GameTestHelper h,ChaoEntity robot){
    UUID actor=robot.getUUID();robot.state().adultAgeTicks=19;robot.state().happiness=100;
    var transition=OmochaoRules.tickRobot(robot.state(),20);
    h.assertTrue(transition==ChaoState.Transition.REINCARNATED&&OmochaoLifecycle.finish(robot,transition),"Actual cared-for robot did not yield its physical heart");
    var drops=outputs(h,actor);h.assertTrue(drops.size()==1&&drops.getFirst().getItem().is(ChaoGarden.item("robo_heart")),"Heart output was missing or duplicated");
    ItemStack heart=drops.getFirst().getItem().copy();drops.getFirst().discard();return heart;
  }

  @GameTest(template="activity_test",timeoutTicks=50)
  public static void realHeartsRebuildCThroughSWithCreatorAncestryAndFreedGardens(GameTestHelper h){
    var keeper=TestConnections.makeServerPlayer(h);var registry=GardenRegistry.get(h.getLevel().getServer());
    var garden=registry.register(keeper,h.absolutePos(new BlockPos(1,1,1)));var builders=new ArrayList<ChaoEntity>();
    ItemStack heart=ItemStack.EMPTY;String previousCreator="";UUID lastRobot=null;
    for(int generation=0;generation<4;generation++){
      String name="Creator "+generation;var creator=builder(h,name,2+generation*3,keeper.getUUID());builders.add(creator);
      var robot=built(h,creator,heart);int expected=2+generation;
      h.assertTrue(robot.state().adult&&!robot.state().immortal&&robot.state().adultAgeTicks==0
          &&Arrays.stream(robot.state().grades).allMatch(g->g==expected),"Reconstruction did not start young adult at the correct uniform C/B/A/S tier");
      h.assertTrue(Arrays.stream(robot.state().levels).sum()==0&&Arrays.equals(robot.state().grades,robot.state().gradeA)
          &&Arrays.equals(robot.state().gradeA,robot.state().gradeB),"New rank/genes are inconsistent or copied learned levels");
      h.assertTrue(robot.state().familyName("family.parent.0").equals(name)&&robot.state().trait("family.parent.1.id").isEmpty(),"The builder is not the sole parent");
      if(generation>0)h.assertTrue(robot.state().familyName("family.grandparent.0").equals(previousCreator),"The previous creator did not move to grandparent");
      h.assertTrue(lastRobot==null||!lastRobot.equals(robot.getUUID()),"Heart reconstruction reused an ended actor UUID");lastRobot=robot.getUUID();
      h.assertTrue(registry.bind(keeper,robot,garden),"Robot could not use the freed garden slot");
      if(generation==2){
        robot.state().setTrait(OmochaoRules.RANK,"");
        for(int i=0;i<256;i++)if(!robot.state().setTrait("record.robot"+i,i))break;
        h.assertTrue(robot.state().traitEntries().size()==256&&robot.state().trait(OmochaoRules.RANK).isEmpty(),"Full-map A-rank fixture did not exercise missing optional rank data");
      }
      heart=endAsHeart(h,robot);h.assertTrue(!registry.entries.containsKey(lastRobot),"Ended robot left an orphan garden entry");
      // Use the engine's real item codec, then the next generation consumes this exact payload.
      heart=ItemStack.parse(h.getLevel().registryAccess(),heart.save(h.getLevel().registryAccess())).orElseThrow();
      h.assertTrue(CweRobotPartItem.validHeart(heart)&&CweRobotPartItem.heartData(heart).getInt("Rank")==Math.min(5,expected+1),"Serialized heart lost its bounded next tier");
      h.assertTrue(creator.state().intTrait("content.omochao_parts")==0,"Loaded old creator's completed construction claim was not released");
      previousCreator=name;
    }
    h.assertTrue(h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,new AABB(h.absolutePos(BlockPos.ZERO)).inflate(16),e->keeper.getUUID().equals(e.owner)).isEmpty(),"A mechanical life created an ordinary baby egg");
    registry.removeGarden(garden.place());for(var creator:builders)creator.discard();h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=320)
  public static void aRealHeartGiftIsConsideredConsumedOnceAndASecondHeartReturned(GameTestHelper h){
    var player=h.makeMockPlayer(GameType.SURVIVAL);player.getAbilities().instabuild=false;
    var firstCreator=builder(h,"Maple",2,player.getUUID());var secondCreator=builder(h,"Pine",6,player.getUUID());
    ItemStack first=endAsHeart(h,built(h,firstCreator,ItemStack.EMPTY)),second=endAsHeart(h,built(h,secondCreator,ItemStack.EMPTY));
    var receiver=builder(h,"Willow",10,player.getUUID());receiver.state().personality=com.chaogarden.core.ChaoPreferences.CURIOUS;
    ItemStack offered=first.copy();player.setItemInHand(InteractionHand.MAIN_HAND,offered);receiver.offerItem(player,InteractionHand.MAIN_HAND);
    h.assertTrue(offered.isEmpty()&&receiver.getMainHandItem().is(ChaoGarden.item("robo_heart"))&&receiver.state().intTrait("content.omochao_parts")==0,"Heart changed stats instantly or failed to transfer one real item");
    h.runAtTickTime(15,()->h.assertTrue(receiver.state().intTrait("content.omochao_parts")==0,"Heart skipped the thinking phase"));
    h.runAtTickTime(140,()->{
      var robot=(ChaoEntity)h.getLevel().getEntity(UUID.fromString(receiver.state().trait("content.omochao_uuid")));
      h.assertTrue(receiver.state().intTrait("content.omochao_parts")==1&&robot.state().grades[0]==3&&robot.state().intTrait("robot.heart_used")==1,"Heart did not commit exactly one B-rank prototype stage");
      var saved=new CompoundTag();robot.saveWithoutId(saved);var loaded=ChaoGarden.CHAO.get().create(h.getLevel());loaded.load(saved);
      h.assertTrue(loaded.state().grades[0]==3&&loaded.state().intTrait("robot.heart_used")==1&&loaded.state().trait("robot.previous_creator.name").equals(ChaoState.encodeFamilyName("Maple")),"Construction reload lost the inserted heart's tier or previous creator");
      player.setItemInHand(InteractionHand.MAIN_HAND,second.copy());receiver.offerItem(player,InteractionHand.MAIN_HAND);
    });
    h.runAtTickTime(285,()->{
      var robot=(ChaoEntity)h.getLevel().getEntity(UUID.fromString(receiver.state().trait("content.omochao_uuid")));
      h.assertTrue(receiver.state().intTrait("content.omochao_parts")==1&&robot.state().grades[0]==3,"A second heart advanced or upgraded the same robot");
      var returned=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(receiver.blockPosition()).inflate(5),i->ItemStack.isSameItemSameComponents(i.getItem(),second));
      h.assertTrue(returned.stream().mapToInt(i->i.getItem().getCount()).sum()==1,"Rejected second heart was consumed or duplicated");
      for(int i=0;i<3;i++)h.assertTrue(CweContent.assembleOmochao(receiver),"Normal remaining boxes failed after rejecting another heart");
      h.assertTrue(robot.state().grades[0]==3&&robot.state().familyName("family.parent.0").equals("Willow")&&robot.state().familyName("family.grandparent.0").equals("Maple"),"Completion lost paid heart ancestry/rank");
      for(var i:returned)i.discard();robot.discard();receiver.discard();firstCreator.discard();secondCreator.discard();h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=60)
  public static void blockedPhysicalOutputSurvivesReloadThenReturnsOnePartAndEquipment(GameTestHelper h){
    var keeper=TestConnections.makeServerPlayer(h);var creator=builder(h,"Keeper",3,keeper.getUUID());var robot=built(h,creator,ItemStack.EMPTY);
    UUID actor=robot.getUUID();var registry=GardenRegistry.get(h.getLevel().getServer());var garden=registry.register(keeper,h.absolutePos(new BlockPos(1,1,1)));
    h.assertTrue(registry.bind(keeper,robot,garden),"Initial robot garden bind failed");
    ItemStack hat=new ItemStack(Items.LEATHER_HELMET);hat.set(DataComponents.CUSTOM_NAME,Component.literal("Robot hat "+actor));
    ItemStack held=new ItemStack(Items.DIAMOND,2);held.set(DataComponents.CUSTOM_NAME,Component.literal("Held "+actor));
    robot.setItemSlot(EquipmentSlot.HEAD,hat.copy());robot.setItemSlot(EquipmentSlot.MAINHAND,held.copy());
    for(int i=0;i<256;i++)if(!robot.state().setTrait("record.full"+i,i))break;
    h.assertTrue(robot.state().traitEntries().size()==256,"Pending-output fixture must fill the optional feature map");
    var blocked=new AtomicBoolean(true);Consumer<EntityJoinLevelEvent> listener=event->{
      if(event.getEntity() instanceof ItemEntity item){var origin=CweRobotPartItem.origin(item.getItem());if(origin.hasUUID("Robot")&&origin.getUUID("Robot").equals(actor)&&blocked.get())event.setCanceled(true);}
    };NeoForge.EVENT_BUS.addListener(listener);
    robot.state().adultAgeTicks=GardenConfig.ADULT_TICKS.get()-1;robot.state().happiness=0;
    h.runAfterDelay(3,()->{
      h.assertTrue(!robot.isRemoved()&&robot.state().dead&&robot.state().robotPendingOutcome()==1&&outputs(h,actor).isEmpty(),"A refused output removed the robot or forgot its death outcome");
      h.assertTrue(registry.entries.containsKey(actor)&&!robot.canCare(keeper),"Pending output freed the garden or allowed dead-robot care/capture");
      h.assertTrue(robot.getItemBySlot(EquipmentSlot.HEAD).getCount()==1&&robot.getMainHandItem().getCount()==2,"Refused output lost real equipment");
      var save=new CompoundTag();robot.saveWithoutId(save);robot.discard();
      h.runAfterDelay(2,()->{
        var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(save);h.getLevel().addFreshEntity(restored);blocked.set(false);
        h.runAfterDelay(3,()->{
          NeoForge.EVENT_BUS.unregister(listener);
          var drops=outputs(h,actor);h.assertTrue(restored.isRemoved()&&drops.size()==1&&drops.getFirst().getItem().is(ChaoGarden.item("robot_component")),"Reload retry did not commit exactly one component");
          h.assertTrue(!registry.entries.containsKey(actor),"Committed mechanical death left an old garden slot");
          var area=new AABB(restored.blockPosition()).inflate(4);var items=h.getLevel().getEntitiesOfClass(ItemEntity.class,area);
          h.assertTrue(items.stream().filter(i->ItemStack.isSameItemSameComponents(i.getItem(),hat)).mapToInt(i->i.getItem().getCount()).sum()==1
              &&items.stream().filter(i->ItemStack.isSameItemSameComponents(i.getItem(),held)).mapToInt(i->i.getItem().getCount()).sum()==2,"Lifecycle retry duplicated or lost the actual worn/held stacks");
          OmochaoLifecycle.finish(restored,ChaoState.Transition.DIED);h.assertTrue(outputs(h,actor).size()==1,"Repeating completion on a discarded actor duplicated its component");
          var committedSave=new CompoundTag();restored.saveWithoutId(committedSave);var reloadedCommitted=ChaoGarden.CHAO.get().create(h.getLevel());reloadedCommitted.load(committedSave);
          h.assertTrue(reloadedCommitted.state().robotOutputCommitted()&&reloadedCommitted.state().intTrait("robot.output_committed")==0,"Full-map commit flag was not independently encoded");
          OmochaoLifecycle.resumePending(reloadedCommitted);h.assertTrue(reloadedCommitted.isRemoved()&&outputs(h,actor).size()==1,"Reloading a committed full-map actor produced another component");
          registry.removeGarden(garden.place());for(var i:items)i.discard();creator.discard();h.succeed();
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=40)
  public static void recoveredOriginReleasesAnUnloadedOldCreatorsClaimButNotALivingRobot(GameTestHelper h){
    var creator=builder(h,"Original creator",3,UUID.randomUUID());var robot=built(h,creator,ItemStack.EMPTY);
    var oldClaim=ChaoState.decode(creator.state().encode());var fabricated=CweRobotPartItem.recovered(robot.state(),robot.getUUID(),false);
    h.assertTrue(!CweContent.canAssembleOmochao(creator,fabricated),"A copied origin bypassed the one-living-robot claim");
    UUID oldActor=robot.getUUID();OmochaoLifecycle.finish(robot,ChaoState.Transition.DIED);var component=outputs(h,oldActor).getFirst();
    creator.setState(oldClaim); // Its chunk was not loaded when the old body ended.
    h.runAfterDelay(3,()->{
      h.assertTrue(CweContent.canAssembleOmochao(creator,component.getItem()),"Recovered origin could not resume the original creator after reload");
      var next=built(h,creator,component.getItem());if(component.getItem().isEmpty())component.discard();
      h.assertTrue(!next.getUUID().equals(oldActor)&&Arrays.stream(next.state().grades).allMatch(g->g==2),"An ordinary component cloned the old robot or granted a heart rank");
      next.discard();creator.discard();h.succeed();
    });
  }
}
