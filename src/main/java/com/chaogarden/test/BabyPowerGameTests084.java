package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.core.BabyPowerMorphology084;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.entity.ChaoEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual Creative command/maturity inputs and ordinary entity persistence. */
@GameTestHolder("chao_fixes84") @PrefixGameTestTemplate(false)
public final class BabyPowerGameTests084 {
  private BabyPowerGameTests084() {}
  @GameTestGenerator public static Collection<TestFunction> tests(){
    var tests=new ArrayList<TestFunction>();
    for(int alignment:new int[]{-100,0,100}){
      int value=alignment;
      tests.add(new TestFunction("baby_power84","creative_power_age_opposition_save_"+value,
          "chao_fixes84:activity_test",80,0,true,h->development(h,value)));
    }
    return tests;
  }
  private static int command(ServerPlayer player,String text){
    try{return player.server.getCommands().getDispatcher().execute(text,player.createCommandSourceStack().withPermission(0));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(text,error);}
  }
  private static void maturity(GameTestHelper h,ServerPlayer player,ChaoEntity chao,int value){
    h.assertTrue(ChaoCareNetwork.editMaturity(player,new ChaoCareNetwork.MaturityEdit(chao.getUUID().toString(),value)),
        "Real Creative Analyzer input accepts the existing baby maturity range");
  }
  private static void near(GameTestHelper h,float actual,float expected,String message){
    h.assertTrue(Float.isFinite(actual)&&Math.abs(actual-expected)<.00001f,message+" actual="+actual+" expected="+expected);
  }
  private static void development(GameTestHelper h,int alignment){
    var player=TestConnections.makeSurvivalServerPlayer(h);
    player.setGameMode(GameType.CREATIVE);
    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    var chao=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(chao!=null,"Actual baby creates");
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){cleanup();}
      public void testFailed(GameTestInfo i,GameTestRunner r){cleanup();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){cleanup();}
      private void cleanup(){chao.discard();if(player.server.getPlayerList().getPlayer(player.getUUID())==player)player.server.getPlayerList().remove(player);}
    });
    var state=new ChaoState();state.ageTicks=1234;state.adultAgeTicks=456;state.hunger=100;state.energy=100;
    state.shiny=alignment<0;state.setTrait("baby84.history","retain");state.setEvolutionStrength(.4f);
    chao.setState(state);chao.owner=player.getUUID();chao.setNoAi(true);chao.setNoGravity(true);
    chao.setCustomName(Component.literal("BabyPower84 "+UUID.randomUUID()));
    chao.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("round_glasses_accessory")));
    var at=h.absolutePos(new BlockPos(4,2,4));h.setBlock(new BlockPos(4,1,4),Blocks.STONE);
    chao.moveTo(Vec3.atBottomCenterOf(at));h.assertTrue(h.getLevel().addFreshEntity(chao),"Real baby publishes");
    player.teleportTo(chao.getX()+1,chao.getY(),chao.getZ());
    h.assertTrue(command(player,"chao ~ "+alignment+" Power 99")==1,"Actual command sets a baby's Power99");
    h.assertTrue(chao.state().levels[ChaoState.POWER]==99&&chao.state().prospectivePrimaryType()==ChaoState.POWER_TYPE,
        "Power99 participates in ordinary first-evolution selection");
    for(int value:new int[]{0,500,990}){
      maturity(h,player,chao,value);var sample=BabyPowerMorphology084.sample(chao.state());
      h.assertTrue(sample.enabled()&&sample.training()==1,"The real edited inputs admit Power baby development");
      near(h,sample.growth(),value/1000f,"Power develops across young, intermediate and late baby maturity");
      h.assertTrue(!chao.state().adult&&chao.state().ageTicks==1234&&chao.state().adultAgeTicks==456
          &&!chao.state().dead&&chao.state().cocoonTicks==0&&chao.state().reincarnations==0,
          "Visual testing never changes lifespan or triggers an evolution/death/rebirth");
    }
    String encoded=chao.state().encode();
    h.assertTrue(!ChaoCareNetwork.editMaturity(player,new ChaoCareNetwork.MaturityEdit(chao.getUUID().toString(),1000))
        &&encoded.equals(chao.state().encode()),"Existing child100percent evolution boundary remains atomic");
    chao.sync();var tracked=chao.getEntityData().getNonDefaultValues();
    h.assertTrue(tracked!=null,"Actual edited baby has tracked entity data");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(tracked);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(v->encoded.equals(v.value())),
        "Published state carries the same Power and maturity inputs for client reconstruction");
    var saved=new CompoundTag();chao.saveWithoutId(saved);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.owner.equals(player.getUUID())
        &&ItemStack.matches(restored.getItemBySlot(EquipmentSlot.OFFHAND),chao.getItemBySlot(EquipmentSlot.OFFHAND)),
        "Normal entity save/reload preserves state, ownership and glasses exactly");
    near(h,BabyPowerMorphology084.sample(restored.state()).growth(),.99f,"Reload reconstructs the same Power shape inputs");
    h.assertTrue(command(player,"chao ~ "+alignment+" Run 99")==1,"Opposing training uses the same command path");
    near(h,BabyPowerMorphology084.sample(chao.state()).growth(),0,"Equal Run and Power return to balance");
    h.assertTrue(chao.state().prospectivePrimaryType()==ChaoState.RUN_TYPE,"Most recently raised stat wins an actual tie");
    h.assertTrue(command(player,"chao ~ "+alignment+" Power 99")==1
        &&chao.state().prospectivePrimaryType()==ChaoState.RUN_TYPE,"Reapplying an unchanged value does not invent recency");
    h.assertTrue(command(player,"chao ~ "+alignment+" Power 0")==1,"Stat decreases remain supported");
    near(h,BabyPowerMorphology084.sample(chao.state()).growth(),0,"Power decrease removes its added feature influence");
    h.assertTrue(HeroRunMorphology052.sample(chao.state()).babyGrowth()>.98f,
        "Existing Run baby development resumes on the other side of balance");
    near(h,chao.state().evolutionStrength(),.99f,"Opposing training never makes the baby biologically younger");
    h.assertTrue(chao.state().trait("baby84.history").equals("retain")&&chao.state().shiny==(alignment<0),
        "New shape policy preserves history and finish");
    h.succeed();
  }
}
