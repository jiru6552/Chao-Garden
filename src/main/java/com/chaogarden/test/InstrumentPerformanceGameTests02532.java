package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false) @EventBusSubscriber(modid="chao_garden")
public final class InstrumentPerformanceGameTests02532 {
  public static final Map<ChaoEntity,List<Integer>> SOUNDS=new WeakHashMap<>();
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition e){
    if(e.getLevel().isClientSide||e.getSound()==null||e.getSound().value()!=net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value()&&e.getSound().value()!=ChaoSounds.MARACAS.get())return;
    SOUNDS.forEach((c,t)->{if(c.level()==e.getLevel()&&e.getPosition().distanceToSqr(c.position().add(0,c.getBbHeight()*.55,0))<1e-8)t.add((int)c.getBehaviorElapsed(0));});
  }
  public static void prepare(ChaoEntity c,String lesson){
    c.state().setTraitInt("learned."+lesson,1);c.state().setEmotion(ChaoEmotion.BOREDOM,8000);
    for(int emotion:new int[]{ChaoEmotion.JOY,ChaoEmotion.FEAR,ChaoEmotion.CRY,ChaoEmotion.ANGER,ChaoEmotion.RELAX,ChaoEmotion.LONELY,ChaoEmotion.TIREDNESS})c.state().setEmotion(emotion,0);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.goalSelector.addGoal(2,new ChaoBehaviorGoal(c));c.sync();
  }
  @GameTestGenerator public static Collection<TestFunction> music(){
    var out=new ArrayList<TestFunction>();int n=0;
    for(String kind:List.of("flute"))for(boolean adult:new boolean[]{false,true})for(String mode:List.of("complete","stay","hurt","pet","reload"))
      out.add(new TestFunction("instrument02532_"+(n++/4),"flute_learned_"+kind+"_"+adult+"_"+mode,"chao_garden:activity_test",1800,0,true,h->play(h,kind,adult,mode)));
    return out;
  }
  private static void play(GameTestHelper h,String lesson,boolean adult,String mode){
    var p=RattleGameTests02520.floor(h);h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
    ChaoEntity[] actor={RattleGameTests02520.actor(h,p,adult,ChaoState.NORMAL,false)};prepare(actor[0],lesson);SOUNDS.put(actor[0],new ArrayList<>());
    long[] began={-1};boolean[] changed={false};int[] atStop={-1};var keeper=new net.minecraft.server.level.ServerPlayer[1];
    h.onEachTick(()->{
      var c=actor[0];if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT&&began[0]<0){began[0]=h.getLevel().getGameTime();h.assertTrue(c.behaviorDetail().equals(lesson),"real behavior goal chose learned instrument");}
      if(began[0]<0)return;
      if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT){
        h.assertTrue(c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty(),"performance creates no real held copy");
        h.assertTrue(InstrumentPerformance.active(c,"chao_"+lesson)==(!changed[0]||mode.equals("reload")||mode.equals("complete")),"presentation stops immediately while the AI clears the interrupted behavior");
        // Repeated calls on the same loaded tick must not double a cue.
        InstrumentPerformance.tick(c);InstrumentPerformance.tick(c);
      }
      if(!changed[0]&&c.getBehaviorElapsed(0)>=64&&!mode.equals("complete")){
        changed[0]=true;atStop[0]=SOUNDS.get(c).size();
        if(mode.equals("stay"))c.setStaying(true);
        else if(mode.equals("hurt")){c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"real damage accepted");}
        else if(mode.equals("pet")){
          keeper[0]=TestConnections.makeSurvivalServerPlayer(h);keeper[0].getInventory().clearContent();keeper[0].teleportTo(c.getX()+1,c.getY(),c.getZ());c.owner=keeper[0].getUUID();
          h.assertTrue(c.mobInteract(keeper[0],net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"real pet accepted");
        }else{
          float before=c.getBehaviorElapsed(0);int remaining=c.getBehaviorTicks();var sounds=SOUNDS.remove(c);var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();
          var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);restored.goalSelector.removeAllGoals(g->true);restored.targetSelector.removeAllGoals(g->true);restored.goalSelector.addGoal(2,new ChaoBehaviorGoal(restored));h.getLevel().addFreshEntity(restored);actor[0]=restored;SOUNDS.put(restored,sounds);
          h.assertTrue(restored.getBehaviorElapsed(0)==before&&restored.getBehaviorTicks()==remaining,"native NBT retains phase and remaining time");
          InstrumentPerformance.tick(restored);h.assertTrue(sounds.size()==atStop[0],"reload on a ringing tick cannot replay its cue");
        }
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&h.getLevel().getGameTime()-began[0]>=180,"observe performance and exit");var c=actor[0];var sounds=SOUNDS.get(c);
      h.assertTrue(c.getBehavior()!=ChaoEntity.Behavior.INSTRUMENT,"performance exits");
      int expected=mode.equals("complete")||mode.equals("reload")?(lesson.equals("maracas")?31:8):atStop[0];
      h.assertTrue(sounds.size()==expected,"exact authored cues, no sound after interruption: "+sounds);
      h.assertTrue(new HashSet<>(sounds).size()==sounds.size(),"no repeated elapsed cue");
      h.assertTrue(c.state().traitInt("learned."+lesson,0)==1,"performance does not teach twice");
      if(keeper[0]!=null)keeper[0].server.getPlayerList().remove(keeper[0]);SOUNDS.remove(c);
    });
  }
  @GameTest(template="activity_test",timeoutTicks=300,batch="flute_join") public static void joiningInstrumentUsesExistingBeatAndEntryBlend(GameTestHelper h){
    var p=RattleGameTests02520.floor(h);h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);var c=RattleGameTests02520.actor(h,p,true,ChaoState.NORMAL,false);c.setNoAi(true);c.state().setTraitInt("learned.flute",1);c.startBehavior(ChaoEntity.Behavior.INSTRUMENT,80);c.setBehaviorDetail("flute");
    h.assertTrue(InstrumentPerformance.phase(c,0)==80&&InstrumentPerformance.presence(c,0)==0,"late musician starts on leader beat with an empty entry blend");
    h.runAfterDelay(16,()->{h.assertTrue(InstrumentPerformance.phase(c,0)>90&&InstrumentPerformance.presence(c,0)==1,"joins the shared beat after raising hands");h.succeed();});
  }
}
