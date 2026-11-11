package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoEggItem;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real held-item transactions, wall/floor physics and post-reload hatching. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class EggInteractionGameTests {
  private EggInteractionGameTests() {}
  private static void room(GameTestHelper h) {
    for(int x=1;x<=9;x++)for(int z=1;z<=6;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
  }
  private static Player keeper(GameTestHelper h,boolean creative) {
    Player p=h.makeMockPlayer(creative?GameType.CREATIVE:GameType.SURVIVAL);
    p.getAbilities().mayBuild=true;p.getAbilities().instabuild=creative;
    p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,1,2))));
    p.setYRot(-90);p.setXRot(0);return p;
  }
  private static ChaoState identity() {
    var s=ChaoState.egg(8,true,false,3,new Random(19019));
    s.happiness=73;s.bond=44;s.kindnessGenes=new int[]{2,3};s.aggressiveGenes=new int[]{1,2};
    return s;
  }
  private static UseOnContext placement(GameTestHelper h,Player p) {
    BlockPos floor=h.absolutePos(new BlockPos(3,0,3));
    return new UseOnContext(p,InteractionHand.MAIN_HAND,
        new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false));
  }
  private static AABB box(GameTestHelper h) {
    return new AABB(Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(0,0,0))),
        Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(11,8,8))));
  }
  private static java.util.List<ChaoEntity> babies(GameTestHelper h,Player p) {
    return h.getLevel().getEntitiesOfClass(ChaoEntity.class,box(h),c->p.getUUID().equals(c.owner));
  }
  private static void wall(GameTestHelper h) {
    for(int y=1;y<=5;y++)for(int z=1;z<=5;z++)h.setBlock(new BlockPos(6,y,z),Blocks.STONE);
  }

  @GameTest(template="activity_test",timeoutTicks=70)
  public static void gentleRockRequiresEightSecondsAndCommitsGenesOnlyAtHatching(GameTestHelper h) {
    room(h);Player p=keeper(h,false);ChaoState state=identity();String initial=state.encode();
    ItemStack stack=ChaoEggItem.containing(state,21);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var item=(ChaoEggItem)stack.getItem();item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
    for(int i=1;i<ChaoEggItem.ROCK_TICKS;i++)item.onUseTick(h.getLevel(),p,stack,ChaoEggItem.USE_DURATION-i);
    CompoundTag data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
    h.assertTrue(data.getInt("HatchTreatment")==ChaoEggEntity.NATURAL,"Short rocking prepared a premature hatch");
    item.onUseTick(h.getLevel(),p,stack,ChaoEggItem.USE_DURATION-ChaoEggItem.ROCK_TICKS);
    item.releaseUsing(stack,h.getLevel(),p,ChaoEggItem.USE_DURATION-ChaoEggItem.ROCK_TICKS);
    data=stack.get(DataComponents.CUSTOM_DATA).copyTag();
    h.assertTrue(data.getInt("HatchTreatment")==ChaoEggEntity.GENTLE,"Completed rocking did not prepare a gentle hatch");
    h.assertTrue(data.getString("Chao").equals(initial),"Rocking applied hatching genes before a creature hatched");
    h.assertTrue(item.useOn(placement(h,p)).consumesAction()&&stack.isEmpty(),"Prepared individual egg failed to place/consume");
    h.runAfterDelay(35,()->{
      var list=babies(h,p);h.assertTrue(list.size()==1,"Gentle egg did not hatch exactly one creature");
      var born=list.getFirst().state();
      h.assertTrue(Arrays.equals(born.kindnessGenes,new int[]{3,4})&&Arrays.equals(born.aggressiveGenes,new int[]{0,1}),
          "Gentle hatching did not change the two inherited personality genes exactly once");
      h.assertTrue(born.happiness==73&&born.bond==44,"Gentle hatching invented a happiness or bond reward");
      h.assertTrue(born.isShiny()&&born.jewel()==3&&Arrays.equals(born.colorGenes,state.colorGenes),"Gentle hatch lost genetic identity");
      list.forEach(ChaoEntity::discard);h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=50)
  public static void chargedCreativeThrowUsesRealWallImpactAndConsumesIndividualEgg(GameTestHelper h) {
    room(h);wall(h);Player p=keeper(h,true);ChaoState state=identity();
    ItemStack stack=ChaoEggItem.containing(state,27);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var item=(ChaoEggItem)stack.getItem();p.setShiftKeyDown(true);item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
    item.releaseUsing(stack,h.getLevel(),p,ChaoEggItem.USE_DURATION-20);
    h.assertTrue(stack.isEmpty(),"Creative throw retained a duplicate individual egg");
    h.assertTrue(h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,box(h),e->p.getUUID().equals(e.owner)).size()==1,
        "Throw did not create one actual flying egg");
    h.runAfterDelay(12,()->{
      var list=babies(h,p);h.assertTrue(list.size()==1,"Wall impact did not hatch exactly one creature");
      ChaoState born=list.getFirst().state();
      h.assertTrue(Arrays.equals(born.kindnessGenes,new int[]{1,2})&&Arrays.equals(born.aggressiveGenes,new int[]{2,3}),
          "Rough hatch gene effects are missing or duplicated");
      h.assertTrue(born.happiness==73&&born.bond==34,"Rough hatching must cost ten bond without a happiness penalty");
      h.assertTrue(h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,box(h),e->p.getUUID().equals(e.owner)).isEmpty(),
          "A hatchable shell remained after wall hatching");
      list.forEach(ChaoEntity::discard);h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=50)
  public static void thrownFloorLandingDoesNotCountAsRoughHatching(GameTestHelper h) {
    room(h);Player p=keeper(h,false);var egg=ChaoGarden.CHAO_EGG.get().create(h.getLevel());
    egg.setState(identity());egg.owner=p.getUUID();egg.setIncubation(17);
    Vec3 at=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4,4,3)));egg.setPos(at);
    egg.throwWithVelocity(new Vec3(0,-.55,0),p);h.getLevel().addFreshEntity(egg);
    h.runAfterDelay(20,()->{
      h.assertTrue(!egg.isRemoved()&&egg.onGround(),"A floor landing broke or lost the egg");
      h.assertTrue(egg.hatchTreatment()==ChaoEggEntity.NATURAL&&!egg.isThrown(),"A settled floor throw became a rough hatch");
      h.assertTrue(babies(h,p).isEmpty(),"Throwing onto the floor hatched a creature");
      h.assertTrue(egg.incubation()>17,"A settled egg did not resume natural incubation");
      egg.discard();h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=60)
  public static void savedFlightRetainsComponentsAndAllRebornEquipmentUntilSingleHatch(GameTestHelper h) {
    room(h);wall(h);Player p=keeper(h,false);ChaoState state=identity();
    ItemStack source=ChaoEggItem.containing(state,41);CompoundTag custom=source.get(DataComponents.CUSTOM_DATA).copyTag();
    custom.putString("ForeignPayload","Keep this exact egg metadata");source.set(DataComponents.CUSTOM_DATA,CustomData.of(custom));
    source.set(DataComponents.REPAIR_COST,7);source.set(DataComponents.CUSTOM_NAME,Component.literal("Inherited shell"));
    ItemStack hat=new ItemStack(ChaoGarden.item("eggshell_hat"));hat.set(DataComponents.CUSTOM_NAME,Component.literal("Family shell"));
    hat.set(DataComponents.DYED_COLOR,new DyedItemColor(0xc32453,true));hat.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
    CompoundTag shellCoat=new CompoundTag();shellCoat.putString("EggShellCoat",state.encode());hat.set(DataComponents.CUSTOM_DATA,CustomData.of(shellCoat));
    ItemStack accessory=new ItemStack(Items.GLASS_BOTTLE);accessory.set(DataComponents.CUSTOM_NAME,Component.literal("Family accessory"));
    ItemStack body=new ItemStack(Items.LEATHER_CHESTPLATE);body.set(DataComponents.REPAIR_COST,4);
    ItemStack neck=new ItemStack(Items.GOLD_NUGGET);neck.set(DataComponents.CUSTOM_NAME,Component.literal("Family necklace"));
    ItemStack extraHat=new ItemStack(Items.FEATHER);extraHat.set(DataComponents.CUSTOM_NAME,Component.literal("Family feather"));
    CompoundTag equipment=new CompoundTag();equipment.put("head",hat.save(h.getLevel().registryAccess()));
    equipment.put("offhand",accessory.save(h.getLevel().registryAccess()));
    equipment.put("chest",body.save(h.getLevel().registryAccess()));equipment.put("legs",neck.save(h.getLevel().registryAccess()));
    equipment.put("feet",extraHat.save(h.getLevel().registryAccess()));
    UUID identity=UUID.randomUUID(),friend=UUID.randomUUID();CompoundTag relations=new CompoundTag();
    relations.putUUID("FriendIdentity",identity);relations.putLong("NextSocial",1200);
    var friends=new net.minecraft.nbt.ListTag();var record=new CompoundTag();record.putUUID("Friend",friend);record.putInt("Talks",7);
    friends.add(record);relations.put("ChaoFriends",friends);
    var egg=ChaoGarden.CHAO_EGG.get().create(h.getLevel());egg.setState(state);egg.owner=p.getUUID();
    egg.chaoName=Component.literal("Third Life");egg.setIncubation(41);egg.setSourceItem(source);egg.setRebornEquipment(equipment);
    egg.setRebornRelationships(relations);
    egg.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,2,2))));egg.throwWithVelocity(new Vec3(.9,.02,0),p);
    h.getLevel().addFreshEntity(egg);CompoundTag saved=new CompoundTag();egg.saveWithoutId(saved);egg.discard();
    var restored=ChaoGarden.CHAO_EGG.get().create(h.getLevel());restored.load(saved);
    h.assertTrue(restored.isThrown()&&restored.incubation()==41&&restored.rebornEquipment().equals(equipment),
        "Reload reset flight, incubation or rebirth equipment");
    ItemStack recovered=restored.snapshotItem();
    h.assertTrue(recovered.getOrDefault(DataComponents.REPAIR_COST,0)==7
        &&recovered.get(DataComponents.CUSTOM_DATA).copyTag().getString("ForeignPayload").equals("Keep this exact egg metadata")
        &&recovered.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("RebornRelationships").equals(relations),
        "Flight reload/recovery lost foreign components");
    h.getLevel().addFreshEntity(restored);
    h.runAfterDelay(12,()->{
      var list=babies(h,p);h.assertTrue(list.size()==1,"Reloaded wall collision did not create one Chao");
      var born=list.getFirst();
      h.assertTrue(born.getName().getString().equals("Third Life"),"Reloaded wall hatch lost its name");
      h.assertTrue(ItemStack.matches(born.getItemBySlot(EquipmentSlot.HEAD),hat)
          &&ItemStack.matches(born.getItemBySlot(EquipmentSlot.OFFHAND),accessory)
          &&ItemStack.matches(born.getItemBySlot(EquipmentSlot.CHEST),body)
          &&ItemStack.matches(born.getItemBySlot(EquipmentSlot.LEGS),neck)
          &&ItemStack.matches(born.getItemBySlot(EquipmentSlot.FEET),extraHat),"All five rebirth equipment slots/components did not restore once");
      h.assertTrue(born.stableFriendIdentity().equals(identity)&&born.completedConversations(friend)==7,
          "Reloaded rebirth egg lost stable friendships");
      h.assertTrue(born.nextSocialTime()>h.getLevel().getGameTime(),"Rebirth reset the pending social cooldown");
      h.assertTrue(Arrays.equals(born.state().kindnessGenes,new int[]{1,2}),"Reloaded rough hatch replayed its gene cost");
      var shells=h.getLevel().getEntitiesOfClass(ItemEntity.class,box(h),e->e.getItem().is(ChaoGarden.item("eggshell_hat")));
      h.assertTrue(shells.size()==1&&shells.getFirst().getItem().getCount()==1,"One hatch did not leave exactly one useful top shell");
      ItemStack shell=shells.getFirst().getItem();
      ChaoState coat=ChaoState.decode(shell.get(DataComponents.CUSTOM_DATA).copyTag().getString("EggShellCoat"));
      h.assertTrue(shell.has(DataComponents.DYED_COLOR)&&shell.getOrDefault(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,false)
          &&Arrays.equals(coat.colorGenes,state.colorGenes)&&coat.jewel()==state.jewel(),"Hatched shell lost coat material/components");
      shells.forEach(ItemEntity::discard);
      born.discard();h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=40)
  public static void survivalAndCreativeRecoveryPreserveComponentsWithFullInventories(GameTestHelper h) {
    room(h);
    for(boolean creative:new boolean[]{false,true})checkRecovery(h,creative);
    h.succeed();
  }

  private static void checkRecovery(GameTestHelper h,boolean creative) {
    Player p=keeper(h,creative);Player other=keeper(h,false);
    ItemStack source=ChaoEggItem.containing(identity(),99);source.set(DataComponents.REPAIR_COST,11);
    source.set(DataComponents.CUSTOM_NAME,Component.literal("Named inherited egg"));
    CompoundTag custom=source.get(DataComponents.CUSTOM_DATA).copyTag();custom.putString("ForeignPayload","survives recovery");
    source.set(DataComponents.CUSTOM_DATA,CustomData.of(custom));
    var egg=ChaoGarden.CHAO_EGG.get().create(h.getLevel());egg.setState(identity());egg.setSourceItem(source);
    egg.owner=p.getUUID();egg.setIncubation(99);egg.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3,1,3))));
    h.getLevel().addFreshEntity(egg);
    h.assertTrue(egg.interact(other,InteractionHand.MAIN_HAND)==InteractionResult.FAIL&&!egg.isRemoved(),
        "A different keeper recovered an owned egg");
    for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
    h.assertTrue(egg.interact(p,InteractionHand.MAIN_HAND).consumesAction()&&egg.isRemoved(),"Full inventory prevented safe egg recovery");
    var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box(h),e->e.getItem().getItem() instanceof ChaoEggItem);
    h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Recovery created multiple egg items");
    ItemStack result=drops.getFirst().getItem();CompoundTag payload=result.get(DataComponents.CUSTOM_DATA).copyTag();
    h.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST,0)==11
        &&result.getHoverName().getString().equals("Named inherited egg")
        &&payload.getString("ForeignPayload").equals("survives recovery")&&payload.hasUUID("Keeper"),
        "Owned egg recovery dropped item components or keeper data");
    drops.forEach(ItemEntity::discard);
  }

  @GameTest(template="activity_test",timeoutTicks=40)
  public static void aLoadedIndividualEggUuidCannotBePlacedTwice(GameTestHelper h) {
    room(h);Player p=keeper(h,true);ChaoState state=identity();
    ItemStack stack=ChaoEggItem.containing(state,com.chaogarden.GardenConfig.HATCH_TICKS.get()-1),duplicate=stack.copy();
    UUID eggId=stack.get(DataComponents.CUSTOM_DATA).copyTag().getUUID("EggId");
    var item=(ChaoEggItem)stack.getItem();p.setItemInHand(InteractionHand.MAIN_HAND,stack);
    h.assertTrue(item.useOn(placement(h,p)).consumesAction()&&stack.isEmpty(),"Initial individual placement failed");
    p.setItemInHand(InteractionHand.MAIN_HAND,duplicate);
    h.assertTrue(item.useOn(placement(h,p))==InteractionResult.FAIL&&!duplicate.isEmpty(),
        "A loaded duplicate UUID was accepted or consumed");
    h.assertTrue(h.getLevel().getEntity(eggId) instanceof ChaoEggEntity,"Placed egg lost its stable UUID");
    h.runAfterDelay(4,()->{
      var list=babies(h,p);h.assertTrue(list.size()==1,"One live egg UUID did not produce exactly one hatchling");
      h.assertTrue(!list.getFirst().getUUID().equals(eggId),"An ordinary newborn reused the shell entity UUID");
      h.assertTrue(Arrays.equals(list.getFirst().state().kindnessGenes,new int[]{2,3})
          &&list.getFirst().state().happiness==73&&list.getFirst().state().bond==44,
          "Natural hatching changed genes, happiness or bond");
      list.forEach(ChaoEntity::discard);h.succeed();
    });
  }

  // Keep cold-dimension generation out of the parallel default batch.
  @GameTest(template="activity_test",batch="egg02511_cross_dimension_identity",timeoutTicks=800)
  public static void gardenRebirthKeepsOneIdentityAcrossEggItemsReloadAndDimensionCollision(GameTestHelper h) {
    room(h);var p=TestConnections.makeServerPlayer(h);p.getInventory().clearContent();
    p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2,1,2))));
    BlockPos gardenPos=new BlockPos(4,1,4);h.setBlock(gardenPos,ActivityContent.GARDEN.get());
    var data=GardenRegistry.get(h.getLevel().getServer());var garden=data.register(p,h.absolutePos(gardenPos));
    var cleanup=new ArrayList<Runnable>();
    cleanup.add(()->{
      data.removeGarden(garden.place());
      h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,box(h),e->p.getUUID().equals(e.owner)).forEach(ChaoEggEntity::discard);
      babies(h,p).forEach(ChaoEntity::discard);p.server.getPlayerList().remove(p);
    });
    h.testInfo.addListener(new GameTestListener() {
      private void finish() {
        for(int i=cleanup.size()-1;i>=0;i--)try{cleanup.get(i).run();}catch(RuntimeException ignored){}
        cleanup.clear();
      }
      @Override public void testStructureLoaded(GameTestInfo test){}
      @Override public void testPassed(GameTestInfo test,GameTestRunner runner){finish();}
      @Override public void testFailed(GameTestInfo test,GameTestRunner runner){finish();}
      @Override public void testAddedForRerun(GameTestInfo original,GameTestInfo rerun,GameTestRunner runner){}
    });
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var state=identity();state.adult=true;
    state.happiness=100;state.adultAgeTicks=GardenConfig.ADULT_TICKS.get()-1;c.setState(state);
    c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.setCustomName(Component.literal("Garden Next Life"));
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4,1,3))));h.getLevel().addFreshEntity(c);
    UUID bodyId=c.getUUID(),friendId=c.stableFriendIdentity();
    h.assertTrue(data.bind(p,c,garden),"The original living Chao could not bind to its garden");
    h.runAfterDelay(3,()->{
      h.assertTrue(c.isRebirthCocoon()&&!c.isRemoved(),"The garden resident skipped its real rebirth cocoon");
      // Force completion on Post tick40: discarded living bodies must retain
      // the ledger slot while their genuine next-life egg exists.
      c.state().cocoonTicks=1;c.tickCount=39;
    });
    h.runAfterDelay(7,()->{
      var eggs=h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,box(h),e->p.getUUID().equals(e.owner));
      h.assertTrue(c.isRemoved()&&eggs.size()==1,"The completed cocoon did not produce one real egg");
      h.assertTrue(data.entries.containsKey(bodyId)&&data.entries.values().stream().filter(e->e.garden.equals(garden.place())).count()==1,
          "Tick40 cleanup lost or duplicated the reserved garden slot");
      ChaoEggEntity egg=eggs.getFirst();h.assertTrue(egg.interact(p,InteractionHand.MAIN_HAND).consumesAction(),"Rebirth egg could not be picked up");
      ItemStack carried=p.getInventory().items.stream().filter(s->s.getItem() instanceof ChaoEggItem).findFirst().orElseThrow();
      ItemStack itemReload=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)carried.save(h.getLevel().registryAccess()));
      h.assertTrue(itemReload.get(DataComponents.CUSTOM_DATA).copyTag().getUUID("RebornChaoId").equals(bodyId),
          "Egg pickup/item reload lost the original body UUID");
      p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,itemReload);
      UUID eggId=itemReload.get(DataComponents.CUSTOM_DATA).copyTag().getUUID("EggId");
      h.assertTrue(itemReload.getItem().useOn(placement(h,p)).consumesAction()&&itemReload.isEmpty(),"Reloaded individual egg did not place once");
      var placed=(ChaoEggEntity)h.getLevel().getEntity(eggId);var saved=new CompoundTag();placed.saveWithoutId(saved);placed.discard();
      var restored=ChaoGarden.CHAO_EGG.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);
      h.assertTrue(saved.getUUID("RebornChaoId").equals(bodyId),"Placed egg NBT lost the original body UUID");
      var other=h.getLevel().getServer().getLevel(h.getLevel().dimension().equals(Level.NETHER)?Level.OVERWORLD:Level.NETHER);
      h.assertTrue(other!=null,"Cross-dimension uniqueness test needs the second vanilla dimension");
      var remotePos=new BlockPos(0,80,0);boolean wasForced=other.getForcedChunks().contains(0L);
      if(!wasForced)other.setChunkForced(0,0,true);
      cleanup.add(()->{if(!wasForced)other.setChunkForced(0,0,false);});
      // A FULL block chunk alone does not finish its accessible/entity-ticking
      // future: vanilla requires its two-chunk neighbourhood as well. The
      // accelerated GameTest clock can exhaust 800 ticks before cold generation
      // completes, so prepare the actual remote chunk before staging the actor.
      for(int cx=-2;cx<=2;cx++)for(int cz=-2;cz<=2;cz++)other.getChunk(cx,cz);
      long readyDeadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
      h.getLevel().getServer().managedBlock(()->{
        other.getChunkSource().pollTask();
        return other.isPositionEntityTicking(remotePos)||System.nanoTime()>=readyDeadline;
      });
      h.assertTrue(other.isPositionEntityTicking(remotePos),"Remote fixture chunk did not become entity-ticking");
      var blocker=EntityType.ARMOR_STAND.create(other);blocker.setUUID(bodyId);blocker.setNoGravity(true);blocker.setPos(Vec3.atCenterOf(remotePos));
      cleanup.add(()->{blocker.discard();if(!wasForced)other.setChunkForced(0,0,false);});
      h.assertTrue(other.addFreshEntity(blocker),"Could not stage the existing actor in another dimension");
      String before=restored.state().encode();
      h.startSequence()
          // Full block generation does not synchronously expose the entity
          // section in a previously idle dimension. Await real tracking first.
          .thenWaitUntil(()->h.assertTrue(other.getEntity(bodyId)==blocker,
              "Remote actor is not yet tracked for the global UUID check; removed="+blocker.isRemoved()))
          .thenExecute(()->restored.setIncubation(GardenConfig.HATCH_TICKS.get()-1))
          .thenIdle(4)
          .thenExecute(()->h.assertTrue(!restored.isRemoved()&&babies(h,p).isEmpty()&&restored.state().encode().equals(before),
              "An existing cross-dimension body UUID consumed, mutated or duplicated the rebirth egg"))
          .thenExecute(()->{blocker.discard();if(!wasForced)other.setChunkForced(0,0,false);})
          .thenIdle(4)
          .thenExecute(()->{
            var born=babies(h,p);h.assertTrue(born.size()==1,"Unblocked next-life egg did not hatch exactly one body");
            var baby=born.getFirst();
            h.assertTrue(baby.getUUID().equals(bodyId)&&baby.stableFriendIdentity().equals(friendId)
                &&GardenRegistry.find(h.getLevel().getServer(),bodyId)==baby,"New life lost garden or friendship identity");
            h.assertTrue(data.entries.containsKey(bodyId)&&data.entries.get(bodyId).garden.equals(garden.place())
                &&data.entries.values().stream().filter(e->e.garden.equals(garden.place())).count()==1,
                "Rebirth left an old row, added a second slot or lost the assigned garden");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,box(h),e->p.getUUID().equals(e.owner)).isEmpty(),
                "A second hatchable rebirth egg remained");
          }).thenSucceed();
    });
  }
}
