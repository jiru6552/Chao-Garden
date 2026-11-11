package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real inventories, blocks, persistent entities and loaded lesson ticks. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenGameTests {
  private static final BlockPos AT=new BlockPos(2,2,2),STATION=new BlockPos(4,2,2);
  private static Player keeper(GameTestHelper h) {
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;p.getInventory().clearContent();
    var at=h.absolutePos(AT);p.moveTo(at.getX()+.5,at.getY(),at.getZ()+1.5,0,0);return p;
  }
  private static ChaoEntity chao(GameTestHelper h,Player p) {
    h.setBlock(AT.below(),Blocks.STONE);h.setBlock(STATION.below(),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var state=new ChaoState();state.hunger=80;state.energy=100;
    c.setState(state);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);
    var at=h.absolutePos(AT);c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);h.getLevel().addFreshEntity(c);return c;
  }
  private static int count(Player p,Item item) {int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}

  @GameTest(template="empty",timeoutTicks=40)
  public static void doctorRequiresActualStationKeeperAndMedicineThenCuresOnce(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p);for(int i=0;i<6;i++)c.state().setIllness(i,-100);
    p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("chao_medicine"),2));
    h.assertTrue(!ChaoCareServices.cure(p,c),"A network request cannot cure remotely without a real station");
    h.setBlock(STATION,DoctorContent.DOCTOR_STATION.get());
    var stranger=keeper(h);stranger.setUUID(java.util.UUID.randomUUID());h.assertTrue(!ChaoCareServices.cure(stranger,c),"Another keeper cannot treat this creature");
    h.assertTrue(ChaoCareServices.cure(p,c),"Owned nearby Chao is treated at a real Doctor Station");
    for(int i=0;i<6;i++)h.assertTrue(c.state().illness(i)==100,"All six source illness meters are treated and protected");
    h.assertTrue(count(p,ChaoGarden.item("chao_medicine"))==1,"Exactly one actual medicine consumed");
    h.assertTrue(!ChaoCareServices.cure(p,c) && count(p,ChaoGarden.item("chao_medicine"))==1,"Healthy repeated request consumes nothing");
    c.state().setIllness(0,-100);p.moveTo(p.getX()+20,p.getY(),p.getZ(),0,0);
    h.assertTrue(!ChaoCareServices.cure(p,c) && c.state().isIll(),"Out-of-range requests cannot alter health");h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void onlyDaycareCanStartLessonsEvenWithAnOldDeskOrForgedRequest(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p);String offered=ChaoCareServices.currentLesson(h.getLevel().getGameTime());
    h.assertTrue(!ChaoCareServices.beginLesson(p,c,offered),"Analyzer cannot start a lesson outside daycare");
    h.setBlock(STATION,DoctorContent.LESSON_DESK.get());
    h.assertTrue(!ChaoCareServices.beginLesson(p,c,offered),"A legacy physical desk cannot start lessons");
    ChaoCareServices.handle(p,c,"learn",offered);
    h.assertTrue(!ChaoCareServices.hasLesson(c)&&ChaoCareServices.lessonLevel(c.state(),offered)==0,"Forged old analyzer action cannot teach");
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=70)
  public static void legacyFreestandingLessonStopsAfterReloadAndKeepsEarnedKnowledge(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p);h.setBlock(STATION,DoctorContent.LESSON_DESK.get());
    c.state().setTrait("lesson.active","song");c.state().setTraitInt("lesson.remaining",4);
    c.state().setTrait("lesson.desk",Long.toString(h.absolutePos(STATION).asLong()));
    c.state().setTraitInt("learned.song",2);c.state().setTraitInt("lesson.reward.bell",1);
    var saved=new CompoundTag();c.saveWithoutId(saved);c.discard();
    var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);
    h.runAfterDelay(12,()->{
      h.assertTrue(!ChaoCareServices.hasLesson(restored),"Reload retires a freestanding pending lesson");
      h.assertTrue(ChaoCareServices.lessonLevel(restored.state(),"song")==2,"Previously learned levels remain unchanged");
      h.assertTrue(restored.state().traitInt("lesson.reward.bell",0)==1,"Already earned instrument remains claimable");h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void gradeCareConsumesMixedEmeraldCurrencyAndKeepsHiddenAllele(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p);h.setBlock(STATION,DoctorContent.DOCTOR_STATION.get());
    c.state().levels[ChaoState.SWIM]=99;c.state().grades[ChaoState.SWIM]=1;
    c.state().gradeA[ChaoState.SWIM]=1;c.state().gradeB[ChaoState.SWIM]=5;c.state().points[ChaoState.SWIM]=777;
    p.getInventory().setItem(0,new ItemStack(Items.EMERALD_BLOCK,24));
    h.assertTrue(ChaoCareServices.upgrade(p,c,ChaoState.SWIM),"Eligible maximum-level ability can receive paid grade care");
    h.assertTrue(ChaoCareServices.wallet(p)==16,"Two hundred emeralds paid from216 with correct change");
    h.assertTrue(c.state().grades[ChaoState.SWIM]==2 && c.state().gradeB[ChaoState.SWIM]==5,"Only displayed grade allele changes");
    h.assertTrue(c.state().levels[ChaoState.SWIM]==0 && c.state().points[ChaoState.SWIM]==0,"Upgraded ability starts training again");
    h.assertTrue(!ChaoCareServices.upgrade(p,c,ChaoState.SWIM) && ChaoCareServices.wallet(p)==16,"Repeated ineligible request does not charge");h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void wardrobeMovesActualComponentsAndDyeWithoutDuplicatingItems(GameTestHelper h) {
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();var keeperAt=h.absolutePos(AT);p.teleportTo(keeperAt.getX()+.5,keeperAt.getY(),keeperAt.getZ()+1.5);var c=chao(h,p);var glasses=ChaoGarden.item("round_glasses_accessory");
    var gift=new ItemStack(glasses,2);gift.set(DataComponents.CUSTOM_NAME,Component.literal("My exact glasses"));p.getInventory().setItem(0,gift);
    var old=new ItemStack(ChaoGarden.item("aviators_accessory"));c.setItemSlot(EquipmentSlot.OFFHAND,old);
    h.assertTrue(ChaoCareServices.equip(p,c,0),"Inventory accessory is offered");
    WardrobeGameTests.finishAccepted(c);
    h.assertTrue(c.getItemBySlot(EquipmentSlot.OFFHAND).getHoverName().getString().equals("My exact glasses"),"Custom components survive equip");
    h.assertTrue(count(p,glasses)==1 && count(p,old.getItem())==1,"One actual new item consumed and previous item returned once");
    h.assertTrue(ChaoCareServices.remove(p,c,EquipmentSlot.OFFHAND) && count(p,glasses)==2,"Removing returns the same item exactly once");
    h.assertTrue(!ChaoCareServices.remove(p,c,EquipmentSlot.MAINHAND),"Wardrobe cannot take pending gifts from the main hand");
    p.getInventory().setItem(4,new ItemStack(ChaoGarden.item("blue_hoodie_accessory")));
    p.getInventory().setItem(5,new ItemStack(Items.RED_DYE,2));
    h.assertTrue(ChaoCareServices.equip(p,c,4),"Real hoodie is offered");WardrobeGameTests.finishAccepted(c);
    h.assertTrue(ChaoCareServices.dye(p,c,EquipmentSlot.CHEST),"Real dye changes the accepted hoodie");
    h.assertTrue(count(p,Items.RED_DYE)==1 && c.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.DYED_COLOR),"One real dye consumed; colour saved on actual equipment");
    h.assertTrue(!ChaoCareServices.dye(p,c,EquipmentSlot.CHEST) && count(p,Items.RED_DYE)==1,"Identical tint is a no-op");
    var saved=new CompoundTag();c.saveWithoutId(saved);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
    h.assertTrue(ItemStack.isSameItemSameComponents(c.getItemBySlot(EquipmentSlot.CHEST),restored.getItemBySlot(EquipmentSlot.CHEST)),"Equipment and colour survive entity reload");h.getLevel().getServer().getPlayerList().remove(p);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void lessonInstrumentClaimsArePhysicalAndCannotReplayAfterReload(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p);c.state().setTraitInt("learned.bell",1);c.state().setTraitInt("lesson.reward.bell",1);
    var stranger=keeper(h);stranger.setUUID(java.util.UUID.randomUUID());
    h.assertTrue(!ChaoCareServices.claimLessonRewards(stranger,c),"Another keeper cannot claim an earned instrument");
    h.assertTrue(ChaoCareServices.claimLessonRewards(p,c),"Completed lesson yields its physical first-learning instrument");
    h.assertTrue(count(p,ChaoGarden.item("chao_bell"))==1,"One actual instrument received");
    var saved=new CompoundTag();c.saveWithoutId(saved);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
    h.assertTrue(!ChaoCareServices.claimLessonRewards(p,restored) && count(p,ChaoGarden.item("chao_bell"))==1,"Reload cannot repeat the claimed reward");
    h.assertTrue(ChaoCareServices.lessonLevel(restored.state(),"bell")==1,"Claiming retains the learned skill");h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void customizationCanOnlySelectMedalsTheChaoEarned(GameTestHelper h) {
    var p=keeper(h);var c=chao(h,p);
    h.assertTrue(!ChaoCareServices.selectMedal(p,c,"race_champion"),"A forged request cannot grant a medal");
    c.state().setTraitInt("medal.race_bronze",1);
    h.assertTrue(ChaoCareServices.selectMedal(p,c,"race_bronze") && c.state().trait("medal.selected").equals("race_bronze"),"An earned medal can be displayed");
    h.assertTrue(!ChaoCareServices.selectMedal(p,c,"forged_medal"),"Unknown medal IDs are rejected");
    h.assertTrue(ChaoCareServices.selectMedal(p,c,"") && c.state().traitInt("medal.race_bronze",0)==1,"Hiding a medal retains the earned unlock");h.succeed();
  }
}
