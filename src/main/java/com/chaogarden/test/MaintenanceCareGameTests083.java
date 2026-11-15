package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real accepted care transactions, not a test-only throttle implementation. */
@GameTestHolder("chao_maintenance83") @PrefixGameTestTemplate(false)
public final class MaintenanceCareGameTests083 {
  private MaintenanceCareGameTests083() {}
  @GameTestGenerator public static Collection<TestFunction> tests(){return List.of(
      new TestFunction("maintenance83_care","legacy_future_clock_cannot_block_real_care_and_live_requests_still_throttle",
          "chao_maintenance83:activity_test",100,0,true,MaintenanceCareGameTests083::legacyClock));}
  private static void legacyClock(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);
    var chao=ChaoGarden.CHAO.get().create(h.getLevel());
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){cleanup();}
      public void testFailed(GameTestInfo i,GameTestRunner r){cleanup();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){cleanup();}
      private void cleanup(){chao.discard();if(player.server.getPlayerList().getPlayer(player.getUUID())==player)player.server.getPlayerList().remove(player);}
    });
    var at=h.absolutePos(new BlockPos(2,2,2));
    player.teleportTo(at.getX()+.5,at.getY(),at.getZ()+1.5);
    var state=new ChaoState();state.energy=100;state.hunger=100;
    state.setTraitInt("medal.race_bronze",1);
    chao.setState(state);chao.owner=player.getUUID();chao.setNoAi(true);chao.setNoGravity(true);
    chao.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    h.assertTrue(h.getLevel().addFreshEntity(chao),"Real care target publishes");
    h.assertTrue(!ChaoCareServices.hasLesson(chao),"Fresh inactive Chao has no lesson");
    chao.state().setTrait("lesson.active","song");chao.state().setTraitInt("lesson.remaining",4);
    h.assertTrue(ChaoCareServices.hasLesson(chao),"Known lesson with positive time remains active");
    chao.state().setTraitInt("lesson.remaining",0);
    h.assertTrue(!ChaoCareServices.hasLesson(chao),"Expired lesson is inactive");
    chao.state().setTrait("lesson.active","unknown_maintenance");chao.state().setTraitInt("lesson.remaining",4);
    h.assertTrue(!ChaoCareServices.hasLesson(chao),"An unknown ID is inactive despite positive time");
    chao.state().setTrait("lesson.active","");chao.state().setTrait("lesson.remaining","");
    h.assertTrue(!ChaoCareServices.hasLesson(chao),"Predicate checks restore fresh inactive lesson fields before real care");
    player.getPersistentData().putLong("ChaoCareLastAction",Long.MAX_VALUE);
    ChaoCareServices.handle(player,chao,"medal","race_bronze");
    h.assertTrue(chao.state().trait("medal.selected").equals("race_bronze"),"Future legacy/dimension timestamp cannot block an authorized real care transaction");
    ChaoCareServices.handle(player,chao,"medal","");
    h.assertTrue(chao.state().trait("medal.selected").equals("race_bronze"),"Duplicate same-server-tick request is still denied");
    h.runAfterDelay(4,()->{
      ChaoCareServices.handle(player,chao,"medal","");
      h.assertTrue(chao.state().trait("medal.selected").isEmpty(),"Authorized care succeeds after four actual server ticks");
      h.assertTrue(chao.state().traitInt("medal.race_bronze",0)==1,"Throttle never removes the earned medal");
      h.assertTrue(player.getPersistentData().getLong("ChaoCareLastAction")==Long.MAX_VALUE,"Legacy saved data is not rewritten or deleted");
      var saved=new CompoundTag();chao.saveWithoutId(saved);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
      h.assertTrue(restored.state().traitInt("medal.race_bronze",0)==1&&restored.state().trait("medal.selected").isEmpty(),"Accepted customization and unlock survive the normal entity codec");
      h.succeed();
    });
  }
}
