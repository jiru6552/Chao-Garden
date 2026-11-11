package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real ticking, held-item commits and inventory/entity persistence for gradual evolution. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GradualEvolutionGameTests {
  private static final BlockPos AT=new BlockPos(2,2,2);
  private static ChaoEntity spawn(GameTestHelper h,Player keeper,ChaoState state){
    h.setBlock(AT.below(),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());c.setState(state);c.owner=keeper.getUUID();
    c.setNoAi(true);c.setNoGravity(true);c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));
    h.getLevel().addFreshEntity(c);return c;
  }
  private static Player keeper(GameTestHelper h){var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;return p;}
  private static void near(GameTestHelper h,float actual,double expected,String why){h.assertTrue(Math.abs(actual-expected)<.00001,why+": "+actual+" versus "+expected);}

  @GameTest(template="empty",timeoutTicks=250)
  public static void realFirstCocoonUsesGreatestStatHistoryAndLocksItsIdentity(GameTestHelper h){
    var p=keeper(h);var s=new ChaoState();s.points[ChaoState.FLY]=s.points[ChaoState.RUN]=100;s.points[ChaoState.STAMINA]=50;
    s.train(ChaoState.FLY,1);s.train(ChaoState.RUN,1);s.swimFly=1;s.align=.8f;
    s.ageTicks=GardenConfig.CHILD_TICKS.get()-1;var c=spawn(h,p,s);String lineage=s.lineageId();
    h.runAtTickTime(4,()->{
      h.assertTrue(s.adult&&s.primaryType==ChaoState.RUN_TYPE&&s.align==.8f,
          "Real first cocoon ignored highest-point tie history or replaced the chosen alignment");
      h.assertTrue(s.cocoonTicks>0&&s.adultAgeTicks==0&&s.evolutionStrength()==0&&s.grades[ChaoState.RUN]==3,
          "First evolution did not start at the young-adult base with its actual cocoon and grade bonus");
      var saved=new CompoundTag();c.saveWithoutId(saved);var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(saved);
      h.assertTrue(copy.state().lineageId().equals(lineage)&&copy.state().primaryType==ChaoState.RUN_TYPE
          &&copy.state().evolutionStrength()==0,"An actual in-cocoon entity save lost first identity or maturity reset");
      copy.discard();s.align=-1;s.primaryType=ChaoState.FLY_TYPE;c.sync();
      h.assertTrue(s.align==.8f&&s.primaryType==ChaoState.RUN_TYPE,"A later public-field/sync rewrite unlocked the first evolution");
    });
    h.runAtTickTime(220,()->{
      h.assertTrue(s.cocoonTicks==0&&s.adultAgeTicks>0&&s.evolutionStrength()>0&&s.evolutionStrength()<.01f,
          "Adult development did not begin gradually after its cocoon");
      s.train(ChaoState.SWIM,1500);s.pet(-1);c.sync();
      h.assertTrue(s.primaryType==ChaoState.RUN_TYPE&&s.align==.8f&&s.cocoonTicks==0,
          "Later training/care caused a second cocoon or changed the fixed primary/alignment");
      c.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=180)
  public static void realSwimDriveCanOpposeFlyPrimaryWithoutAnotherCocoon(GameTestHelper h){
    var p=keeper(h);var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.FLY_TYPE;s.align=.75f;s.swimFly=-.49f;s.hunger=70;
    s.setEvolutionStrength(.9f);var c=spawn(h,p,s);var drive=new ItemStack(ChaoGarden.item("swim_drive"));
    p.setItemInHand(InteractionHand.MAIN_HAND,drive);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(drive.isEmpty()&&s.driveCount==0,"A held drive applied its benefits immediately");
    h.runAtTickTime(140,()->{
      h.assertTrue(s.driveCount==1&&s.swimFly<-.5f&&s.typeName().equals("Fly/Swim"),
          "The actual considered drive did not cross into opposite secondary Swim typing");
      h.assertTrue(s.primaryType==ChaoState.FLY_TYPE&&s.align==.75f&&s.cocoonTicks==0,
          "Mutable secondary care replaced its fixed Fly primary/alignment or created another cocoon");
      h.assertTrue(c.getMainHandItem().isEmpty()&&s.secondaryTypes().length==1,
          "Opposing Swim/Fly secondaries coexisted or the consumed drive remained held");
      c.discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=180)
  public static void actualFiveStatFruitAddsOneMealOfStrengthAcrossEatingReload(GameTestHelper h){
    var p=keeper(h);var s=new ChaoState();s.adult=true;s.hunger=20;s.setEvolutionStrength(.4f);
    var first=spawn(h,p,s);var live=new ChaoEntity[]{first};double baseline=s.evolutionStrength();
    var fruit=new ItemStack(ChaoGarden.item("chao_fruit"));p.setItemInHand(InteractionHand.MAIN_HAND,fruit);first.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(fruit.isEmpty()&&s.fruitCount==0,"The offered multi-stat fruit was not held before commitment");
    h.runAtTickTime(50,()->{
      h.assertTrue(live[0].getItemAction()==ChaoEntity.ItemAction.EAT&&live[0].state().fruitCount==0,
          "The fruit was committed before its actual eating animation");
      var saved=new CompoundTag();live[0].saveWithoutId(saved);live[0].discard();
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);live[0]=restored;
    });
    h.runAtTickTime(140,()->{
      var actual=live[0].state();
      h.assertTrue(actual.fruitCount==1&&live[0].getMainHandItem().isEmpty(),"A meal was lost or replayed after saving while eating");
      for(int stat=0;stat<5;stat++)h.assertTrue(actual.progress[stat]==50,"The Chao Fruit did not train each of its five stats exactly once");
      near(h,actual.evolutionStrength(),baseline+actual.adultAgeTicks/(4.0*GardenConfig.CHILD_TICKS.get())+.005,
          "One multi-stat fruit granted multiple maturity bonuses or load replayed elapsed age");
      live[0].discard();h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=90)
  public static void heldItemNbtRoundtripAndReleaseKeepMaturityAndFirstIdentity(GameTestHelper h){
    var p=keeper(h);var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.SWIM_TYPE;s.align=-.75f;s.swimFly=1;s.runPower=-1;
    s.train(ChaoState.FLY,1);s.train(ChaoState.RUN,1);s.setEvolutionStrength(.6f);s.setTrait("contest.race.wins",7);
    var c=spawn(h,p,s);var uuid=c.getUUID();String lineage=s.lineageId();var held=new ItemStack[]{ItemStack.EMPTY};var savedStrength=new float[1];var savedAge=new int[1];
    h.runAtTickTime(8,()->{
      p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);
      h.assertTrue(CompanionContent.onInteractChao(c,p,InteractionHand.MAIN_HAND).consumesAction()&&c.isRemoved(),
          "Actual sneak pickup did not convert this evolving Chao into its held item");
      ItemStack original=p.getMainHandItem();held[0]=ItemStack.parse(h.getLevel().registryAccess(),original.save(h.getLevel().registryAccess())).orElseThrow();
      var saved=ChaoCarrierItem.state(held[0]);savedStrength[0]=saved.evolutionStrength();savedAge[0]=saved.adultAgeTicks;
      h.assertTrue(saved.encode().startsWith("6;")&&saved.lastTrainedStat()==ChaoState.RUN&&saved.lineageId().equals(lineage),
          "Real inventory NBT serialization lost schema6 maturity history or stable lineage");
      p.setItemInHand(InteractionHand.MAIN_HAND,held[0]);
    });
    h.runAtTickTime(30,()->{
      var saved=ChaoCarrierItem.state(held[0]);near(h,saved.evolutionStrength(),savedStrength[0],"A stored item aged while its entity was not loaded");
      h.assertTrue(saved.adultAgeTicks==savedAge[0],"Carrying added off-world adult ticks");p.setShiftKeyDown(false);
      var at=h.absolutePos(AT);var hit=new BlockHitResult(Vec3.atCenterOf(at.below()),Direction.UP,at.below(),false);
      h.assertTrue(held[0].getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction()&&held[0].isEmpty(),
          "Real held-item release failed or left a reusable carrier");
    });
    h.runAtTickTime(55,()->{
      var restored=(ChaoEntity)h.getLevel().getEntity(uuid);h.assertTrue(restored!=null,"Release changed or lost the actual Chao UUID");var actual=restored.state();
      near(h,actual.evolutionStrength(),savedStrength[0]+(actual.adultAgeTicks-savedAge[0])/(4.0*GardenConfig.CHILD_TICKS.get()),
          "Entity release/reload counted its saved adult age a second time");
      h.assertTrue(actual.lineageId().equals(lineage)&&actual.primaryType==ChaoState.SWIM_TYPE&&actual.align==-.75f
          &&actual.lastTrainedStat()==ChaoState.RUN&&actual.intTrait("contest.race.wins")==7,
          "Release lost its fixed first type, training tie history or lifetime rewards");
      restored.discard();p.getInventory().clearContent();h.succeed();
    });
  }
}
