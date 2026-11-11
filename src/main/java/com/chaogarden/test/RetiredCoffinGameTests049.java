package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityRewards;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.market.*;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

/** Native legacy IDs and NBT without inventory/chunk sweeps or item conversion. */
@GameTestHolder("chao_retired49") @PrefixGameTestTemplate(false)
public final class RetiredCoffinGameTests049 {
  private static final String KIND="chao_coffin";
  private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,KIND);
  private static RetiredCoffinBlock retired(){return (RetiredCoffinBlock)BuiltInRegistries.BLOCK.get(ID);}
  private static BlockPos floor(GameTestHelper h){var at=RattleGameTests02520.floor(h);h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);return at;}
  private static void cleanup(GameTestHelper h,Runnable action){
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
    });
  }
  private static ChaoEntity oldActor(GameTestHelper h,BlockPos at){
    var c=RattleGameTests02520.actor(h,at,true,ChaoState.NORMAL,false);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.state().setTrait("foreign.keep","care history");
    var opaque=new CompoundTag();opaque.putString("UnrelatedForeignPayload","must survive");
    c.getPersistentData().put("ForeignMod",opaque);return c;
  }
  private static CompoundTag mountedSave(ChaoEntity c,BlockPos at){
    var tag=c.getPersistentData();
    tag.putLong("ChaoToyTarget",at.asLong());tag.putLong("ChaoToyUntil",c.level().getGameTime()+100);
    tag.putString("ChaoToyKind",KIND);tag.putBoolean("ChaoToyMounted",true);
    tag.putLong("ChaoToyReturn",at.north(2).asLong());tag.putBoolean("ChaoToyGravity",c.isNoGravity());
    c.moveTo(Vec3.atBottomCenterOf(at).add(0,.20,0));c.setNoGravity(true);c.setToyPlaying(KIND,100);c.setToyTarget(at);
    var saved=new CompoundTag();c.saveWithoutId(saved);return saved;
  }
  private static ChaoEntity load(GameTestHelper h,CompoundTag tag){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"legacy actor creates");
    c.load(tag);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    h.getLevel().addFreshEntity(c);return c;
  }
  @GameTest(template="activity_test",timeoutTicks=50,batch="retired49_static")
  public static void hiddenItemCannotPlaceOrForcePlay(GameTestHelper h){
    var at=floor(h);var item=ChaoGarden.item(KIND);
    h.assertTrue(item instanceof RetiredCoffinItem&&!(item instanceof BlockItem)&&!ChaoGarden.visibleInCreative(item),"former ID is hidden ordinary compatibility data, never a block item");
    h.assertTrue(!ToyContent.ids().contains(KIND)&&!MarketStock.TOYS.contains(KIND)&&!MarketStock.catalog().containsKey(KIND),"retired coffin is absent from all toy/market selection");
    h.assertTrue(h.getLevel().getRecipeManager().byKey(ID).isEmpty(),"no retired recipe remains");
    var player=h.makeMockPlayer(GameType.SURVIVAL);player.getAbilities().mayBuild=true;
    var stack=new ItemStack(item,7);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var result=item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at.below()).add(0,.5,0),Direction.UP,at.below(),false)));
    h.assertFalse(result.consumesAction(),"legacy item cannot place a block");
    h.assertTrue(stack.getCount()==7&&h.getLevel().getBlockState(at).isAir(),"inert use preserves count and unrelated world");
    var actor=oldActor(h,at);h.assertFalse(item.interactLivingEntity(stack,player,actor,InteractionHand.MAIN_HAND).consumesAction(),"legacy item cannot activate Chao interaction");actor.discard();
    var dark=new ChaoState();dark.align=-1;h.assertFalse(ActivityRewards.recordVictory(dark,"race",10).contains(KIND),"race victories never regrant a removed coffin");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=50,batch="retired49_static")
  public static void oldStacksAndNestedComponentsRoundTripExactly(GameTestHelper h){
    floor(h);var registries=h.getLevel().registryAccess();
    var stack=new ItemStack(ChaoGarden.item(KIND),11);
    stack.set(DataComponents.CUSTOM_NAME,Component.literal("An old named keepsake"));
    var opaque=new CompoundTag();opaque.putString("ForeignMod","do not rewrite");var nested=new CompoundTag();nested.putIntArray("OpaqueNumbers",new int[]{4,9,0,-17});opaque.put("Nested",nested);
    stack.set(DataComponents.CUSTOM_DATA,CustomData.of(opaque));
    var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(197);sword.set(DataComponents.CUSTOM_NAME,Component.literal("Unrelated sword"));
    stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,3),sword)));
    var decoded=ItemStack.parseOptional(registries,(CompoundTag)stack.save(registries));
    h.assertTrue(ItemStack.matches(decoded,stack),"legacy registered ID preserves count, name, opaque custom data and nested component inventory");
    var outer=new ItemStack(Items.SHULKER_BOX);outer.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(stack,new ItemStack(Items.EMERALD,23),sword)));
    var outerLoaded=ItemStack.parseOptional(registries,(CompoundTag)outer.save(registries));
    h.assertTrue(ItemStack.matches(outerLoaded,outer),"saving a real enclosing item does not sweep or replace the retired stack or its neighbors");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=50,batch="retired49_static")
  public static void everyLegacyBlockStateLoadsAndBreaksWithoutToyLoot(GameTestHelper h){
    var at=floor(h);var world=h.getLevel();var neighboring=at.east();world.setBlock(neighboring,Blocks.GOLD_BLOCK.defaultBlockState(),3);
    int count=0;
    for(Direction facing:Direction.Plane.HORIZONTAL)for(boolean playing:new boolean[]{false,true})for(int frame=0;frame<4;frame++)for(boolean wet:new boolean[]{false,true}){
      var encoded=new CompoundTag();encoded.putString("Name",ID.toString());var properties=new CompoundTag();
      properties.putString("facing",facing.getName());properties.putString("playing",Boolean.toString(playing));properties.putString("frame",Integer.toString(frame));properties.putString("waterlogged",Boolean.toString(wet));encoded.put("Properties",properties);
      var state=NbtUtils.readBlockState(world.holderLookup(Registries.BLOCK),encoded);
      h.assertTrue(state.is(retired())&&state.getValue(RetiredCoffinBlock.FACING)==facing&&state.getValue(RetiredCoffinBlock.PLAYING)==playing&&state.getValue(RetiredCoffinBlock.FRAME)==frame&&state.getValue(RetiredCoffinBlock.WATERLOGGED)==wet,"old four-property palette remains decodable");
      world.setBlock(at,state,3);h.assertTrue(state.getCollisionShape(world,at).isEmpty(),"legacy marker has no collision or active furniture behavior");
      h.assertFalse(((Object)state.getBlock()) instanceof ChaoToyBlock,"legacy marker is never considered a playable toy");
      world.destroyBlock(at,true);if(wet)h.assertTrue(world.getFluidState(at).isSource(),"breaking a legacy waterlogged marker retains its source water");world.setBlock(at,Blocks.AIR.defaultBlockState(),3);count++;
    }
    h.assertTrue(count==64,"all saved palette combinations were tested");
    h.assertTrue(world.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(3),e->e.getItem().is(ChaoGarden.item(KIND))).isEmpty(),"breaking markers never restores an obtainable coffin");
    h.assertTrue(world.getBlockState(neighboring).is(Blocks.GOLD_BLOCK),"retirement does not rewrite adjacent blocks");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=350,batch="retired49_goal")
  public static void naturalGoalIgnoresOldPlacedMarker(GameTestHelper h){
    var at=floor(h);h.getLevel().setBlock(at,retired().defaultBlockState().setValue(RetiredCoffinBlock.PLAYING,true).setValue(RetiredCoffinBlock.FRAME,3),3);
    var c=oldActor(h,at.north(2));c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));cleanup(h,c::discard);
    h.onEachTick(()->h.assertTrue(!ToyContent.isPlaying(c)&&c.toyTarget()==null,"old marker never starts or reserves a removed interaction"));
    h.runAfterDelay(250,()->{h.assertTrue(c.state().happiness==20,"legacy marker produces no gameplay reward");h.succeed();});
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="retired49_mount")
  public static void oldMountedBodySafelyCancelsWithoutChangingCare(GameTestHelper h){
    var at=floor(h);h.getLevel().setBlock(at,retired().defaultBlockState(),3);var original=oldActor(h,at.north(2));
    var saved=mountedSave(original,at);String exact=original.state().encode();UUID id=original.getUUID();original.discard();
    var loaded=load(h,saved);cleanup(h,loaded::discard);
    h.assertTrue(loaded.state().encode().equals(exact)&&loaded.getUUID().equals(id),"load cancellation preserves exact saved care, genetics, age, traits and entity identity");
    h.assertTrue(!ToyContent.isPlaying(loaded)&&loaded.toyTarget()==null&&!loaded.isNoGravity()&&!loaded.getPersistentData().contains("ChaoToyMounted"),"load cancels retired animation and restores original gravity on supported ground");
    h.runAfterDelay(8,()->{h.assertTrue(h.getLevel().noCollision(loaded,loaded.getBoundingBox())&&loaded.onGround()&&loaded.state().happiness==20,"legacy mount recovers without suffocation or completion reward");
      h.assertTrue(loaded.getPersistentData().getCompound("ForeignMod").getString("UnrelatedForeignPayload").equals("must survive"),"unrelated entity payload is untouched");h.succeed();});
  }
  @GameTest(template="activity_test",timeoutTicks=50,batch="retired49_mount")
  public static void deliberatelyDisabledGravityAndAiSurviveRetirement(GameTestHelper h){
    var at=floor(h);h.getLevel().setBlock(at,retired().defaultBlockState(),3);var original=oldActor(h,at.north(2));original.setNoAi(true);original.setNoGravity(true);
    var saved=mountedSave(original,at);String exact=original.state().encode();original.discard();var loaded=load(h,saved);cleanup(h,loaded::discard);
    h.assertTrue(!ToyContent.isPlaying(loaded)&&loaded.toyTarget()==null&&loaded.isNoAi()&&loaded.isNoGravity(),"retirement restores original true gravity/AI flags rather than forcing normal defaults");
    h.assertTrue(loaded.state().encode().equals(exact),"deliberate flags do not alter unrelated saved care state");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=100,batch="retired49_mount")
  public static void sealedLegacyMountKeepsRecoveryAcrossReload(GameTestHelper h){
    var at=floor(h);h.getLevel().setBlock(at,retired().defaultBlockState(),3);var original=oldActor(h,at.north(2));original.setInvulnerable(false);var saved=mountedSave(original,at);original.discard();
    for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<=4;dy++)if(dx!=0||dz!=0||dy!=0)h.getLevel().setBlock(at.offset(dx,dy,dz),Blocks.STONE.defaultBlockState(),3);
    var loaded=load(h,saved);var bodies=new ArrayList<ChaoEntity>();bodies.add(loaded);cleanup(h,()->bodies.forEach(ChaoEntity::discard));
    h.assertTrue(loaded.getPersistentData().getBoolean("ChaoToyDismountWait")&&loaded.isNoAi()&&loaded.isNoGravity()&&!ToyContent.isPlaying(loaded),"sealed old mount retains safe recovery ownership without replaying the toy");
    h.getLevel().setBlock(at,Blocks.STONE.defaultBlockState(),3);float health=loaded.getHealth();loaded.hurt(loaded.damageSources().inWall(),2);h.assertTrue(loaded.getHealth()==health,"legacy recovery suppresses wall suffocation");
    var again=new CompoundTag();loaded.saveWithoutId(again);loaded.discard();var recovered=load(h,again);bodies.add(recovered);
    h.assertTrue(recovered.isNoAi()&&recovered.isNoGravity()&&recovered.getPersistentData().getBoolean("ChaoToyDismountWait"),"a second load cannot lose pending gravity/AI recovery");
    h.getLevel().setBlock(at.east(3),Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(at.east(3).above(),Blocks.AIR.defaultBlockState(),3);
    h.runAfterDelay(25,()->{h.assertTrue(!recovered.isNoAi()&&!recovered.isNoGravity()&&!recovered.getPersistentData().contains("ChaoToyMounted")&&recovered.onGround(),"opening valid ground finishes recovery and restores original AI/gravity");
      h.assertTrue(h.getLevel().noCollision(recovered,recovered.getBoundingBox())&&recovered.state().happiness==20&&recovered.state().trait("foreign.keep").equals("care history"),"recovery never rewards or discards unrelated care history");h.succeed();});
  }
  @GameTest(template="activity_test",timeoutTicks=60,batch="retired49_static")
  public static void savedMerchantCannotContinueSellingLegacyCoffin(GameTestHelper h){
    floor(h);var trader=MarketContent.TRADER.get().create(h.getLevel());h.assertTrue(trader!=null,"merchant creates");
    trader.getOffers().clear();trader.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD,1),new ItemStack(ChaoGarden.item(KIND)),1,0,0));
    var saved=new CompoundTag();trader.saveWithoutId(saved);trader.discard();
    var loaded=MarketContent.TRADER.get().create(h.getLevel());loaded.load(saved);
    h.assertTrue(loaded.getOffers().stream().noneMatch(offer->offer.getResult().is(ChaoGarden.item(KIND))),"saved offers referencing retired coffin are invalidated by the real catalog validation");
    loaded.discard();h.succeed();
  }
}
