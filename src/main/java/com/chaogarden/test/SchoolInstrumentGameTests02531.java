package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.*;
import com.chaogarden.toys.InstrumentPerformance;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false)
public final class SchoolInstrumentGameTests02531 {
  @GameTest(template="empty",timeoutTicks=1400,batch="school_instruments02531") public static void realEnrolledBabyAdultPracticePauseCompleteAndReturnExactlyOneReward(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();var school=p.server.getLevel(KindergartenContent.DIMENSION);h.assertTrue(HubContent.ensure(school),"actual school builds");InstrumentSchoolFixture02525.visit(p);
    var actors=new ArrayList<ChaoEntity>();long[] start={-1};boolean[] paused={false},resumed={false};int[] silence={0};
    Runnable clean=()->{var roster=KindergartenRoster.get(p.server);for(var c:actors){roster.students.remove(c.getUUID());c.discard();InstrumentPerformanceGameTests02531.SOUNDS.remove(c);}roster.setDirty();p.server.getPlayerList().remove(p);};
    h.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,GameTestRunner r){clean.run();}public void testFailed(GameTestInfo i,GameTestRunner r){clean.run();}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){}});
    h.onEachTick(()->{
      if(start[0]<0)return;long elapsed=school.getGameTime()-start[0];
      for(var c:actors)if(InstrumentPerformance.active(c,"chao_"+c.state().trait("lesson.active")))h.assertTrue(c.getHeldChaoItem().isEmpty(),"practice does not mint a real prop");
      var c=actors.get(2);
      if(elapsed>=70&&!paused[0]){paused[0]=true;c.state().hunger=10;ChaoCareServices.tickLearning(c);silence[0]=InstrumentPerformanceGameTests02531.SOUNDS.get(c).size();}
      if(paused[0]&&!resumed[0]){
        h.assertTrue(!InstrumentPerformance.active(c,"chao_drum")&&InstrumentPerformanceGameTests02531.SOUNDS.get(c).size()==silence[0],"paused lesson is silent and has no prop");
        if(elapsed>=90){resumed[0]=true;c.state().hunger=83;}
      }
    });
    h.startSequence().thenWaitUntil(()->{KindergartenContent.ensureServices(school);h.assertTrue(school.isPositionEntityTicking(KindergartenContent.Role.TEACHER.anchor),"wait for native classroom ticking");})
      .thenExecute(()->{
        for(int n=0;n<4;n++){var c=InstrumentSchoolFixture02525.enroll(p,n,n<2?"maracas":"drum",n%2==1,335);actors.add(c);InstrumentPerformanceGameTests02531.SOUNDS.put(c,new ArrayList<>());}start[0]=school.getGameTime();
      }).thenWaitUntil(()->{
        h.assertTrue(school.getGameTime()-start[0]>380,"observe two cycles and lesson pause");
        for(int n=0;n<4;n++){var c=actors.get(n);String lesson=n<2?"maracas":"drum";var cues=InstrumentPerformanceGameTests02531.SOUNDS.get(c);
          h.assertTrue(!ChaoCareServices.hasLesson(c)&&c.state().traitInt("learned."+lesson,0)==1,"real lesson completes once for each age/instrument");
          h.assertTrue(cues.size()>0&&(n==2||cues.size()==(n<2?62:32)),"exact two-cycle cue count for uninterrupted pupils: "+n+" "+cues);
          h.assertTrue(c.state().happiness==84&&c.state().adult==(n%2==1),"class practice does not alter age or care");
          h.assertTrue(KindergartenContent.isBoarded(c),"same body remains owned by daycare");
        }
      }).thenExecute(()->{
        for(var c:actors)h.assertTrue(KindergartenContent.retrieve(p,c.getUUID()),"teacher returns completed student");
        for(String lesson:List.of("maracas","drum")){int count=0;for(int n=0;n<36;n++){ItemStack item=p.getInventory().getItem(n);if(item.is(ChaoGarden.item("chao_"+lesson)))count+=item.getCount();}h.assertTrue(count==2,"exactly one real instrument reward per student");}
      }).thenSucceed();
  }
}
