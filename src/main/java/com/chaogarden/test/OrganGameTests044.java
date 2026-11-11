package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** Actual navigation, command dispatch and sound events; no simulated reward/animation path. */
@net.neoforged.fml.common.EventBusSubscriber(modid="chao_garden")
@GameTestHolder("chao_organ44") @PrefixGameTestTemplate(false)
public final class OrganGameTests044 {
 static final Map<ChaoEntity,List<Integer>> CUES=new WeakHashMap<>();
 @net.neoforged.bus.api.SubscribeEvent public static void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition e){
  if(e.getSound()==null)return;
  if(e.getSound().value()!=ChaoSounds.ORGAN.get()&&e.getSound().value()!=net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value())return;
  CUES.forEach((c,list)->{var p=c.toyTarget();if(p!=null&&c.level()==e.getLevel()&&e.getPosition().distanceToSqr(new Vec3(p.getX()+.5,p.getY()+OrganPlayRules.KEY_Y,p.getZ()+.5))<1e-6)
   list.add(e.getSound().value()==ChaoSounds.ORGAN.get()?160-c.toyPlayingTicks():-999);});
 }
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_false_NORTH(GameTestHelper h){natural(h,false,Direction.NORTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_false_SOUTH(GameTestHelper h){natural(h,false,Direction.SOUTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_false_EAST(GameTestHelper h){natural(h,false,Direction.EAST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_false_WEST(GameTestHelper h){natural(h,false,Direction.WEST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_hurt") public static void organ_false_hurt(GameTestHelper h){natural(h,false,Direction.NORTH,"hurt");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_remove") public static void organ_false_remove(GameTestHelper h){natural(h,false,Direction.NORTH,"remove");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_reload") public static void organ_false_reload(GameTestHelper h){natural(h,false,Direction.NORTH,"reload");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_move") public static void organ_false_move(GameTestHelper h){natural(h,false,Direction.NORTH,"move");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_true_NORTH(GameTestHelper h){natural(h,true,Direction.NORTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_true_SOUTH(GameTestHelper h){natural(h,true,Direction.SOUTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_true_EAST(GameTestHelper h){natural(h,true,Direction.EAST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_complete") public static void organ_true_WEST(GameTestHelper h){natural(h,true,Direction.WEST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_baby_NORTH(GameTestHelper h){natural(h,false,Direction.NORTH,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_baby_SOUTH(GameTestHelper h){natural(h,false,Direction.SOUTH,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_baby_EAST(GameTestHelper h){natural(h,false,Direction.EAST,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_baby_WEST(GameTestHelper h){natural(h,false,Direction.WEST,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_adult_NORTH(GameTestHelper h){natural(h,true,Direction.NORTH,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_adult_SOUTH(GameTestHelper h){natural(h,true,Direction.SOUTH,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_adult_EAST(GameTestHelper h){natural(h,true,Direction.EAST,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_max_run") public static void max_run_adult_WEST(GameTestHelper h){natural(h,true,Direction.WEST,"max_run");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_hurt") public static void organ_true_hurt(GameTestHelper h){natural(h,true,Direction.NORTH,"hurt");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_remove") public static void organ_true_remove(GameTestHelper h){natural(h,true,Direction.NORTH,"remove");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_reload") public static void organ_true_reload(GameTestHelper h){natural(h,true,Direction.NORTH,"reload");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_move") public static void organ_true_move(GameTestHelper h){natural(h,true,Direction.NORTH,"move");}
 @GameTest(template="activity_test",timeoutTicks=500,batch="organ_blocked") public static void blocked_NORTH(GameTestHelper h){blocked(h,Direction.NORTH);}
 @GameTest(template="activity_test",timeoutTicks=500,batch="organ_blocked") public static void blocked_SOUTH(GameTestHelper h){blocked(h,Direction.SOUTH);}
 @GameTest(template="activity_test",timeoutTicks=500,batch="organ_blocked") public static void blocked_EAST(GameTestHelper h){blocked(h,Direction.EAST);}
 @GameTest(template="activity_test",timeoutTicks=500,batch="organ_blocked") public static void blocked_WEST(GameTestHelper h){blocked(h,Direction.WEST);}
 @GameTest(template="activity_test",timeoutTicks=60,batch="organ_clearance") public static void close_contact_clearance(GameTestHelper h){clearance(h);}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_once") public static void preview_once(GameTestHelper h){preview(h,"once");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_stop") public static void preview_stop(GameTestHelper h){preview(h,"stop");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_hurt") public static void preview_hurt(GameTestHelper h){preview(h,"hurt");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_remove") public static void preview_remove(GameTestHelper h){preview(h,"remove");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_move") public static void preview_move(GameTestHelper h){preview(h,"move");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_interact") public static void preview_interact(GameTestHelper h){preview(h,"interact");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_reload") public static void preview_reload(GameTestHelper h){preview(h,"reload");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_logout") public static void preview_logout(GameTestHelper h){preview(h,"logout");}
 @GameTest(template="activity_test",timeoutTicks=400,batch="organ_preview_distance") public static void preview_distance(GameTestHelper h){preview(h,"distance");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_crawl") public static void crawler_NORTH(GameTestHelper h){natural(h,false,Direction.NORTH,"crawl");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_crawl") public static void crawler_EAST(GameTestHelper h){natural(h,false,Direction.EAST,"crawl");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_crawl") public static void crawler_SOUTH(GameTestHelper h){natural(h,false,Direction.SOUTH,"crawl");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="organ_crawl") public static void crawler_WEST(GameTestHelper h){natural(h,false,Direction.WEST,"crawl");}
 static void natural(GameTestHelper h,boolean adult,Direction facing,String mode){
  boolean complete=mode.equals("complete")||mode.equals("crawl")||mode.equals("max_run");
  var at=RattleGameTests02520.floor(h);var l=h.getLevel();l.setBlock(at,ToyContent.block(OrganPlayRules.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
  var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,adult);
  if(!adult&&!mode.equals("crawl"))c.state().points[ChaoState.RUN]=LocomotionRules.WALK_POINTS;
  if(mode.equals("max_run"))c.state().points[ChaoState.RUN]=9999;
  // Start behind the instrument: real navigation must walk round to its keys.
  c.moveTo(Vec3.atBottomCenterOf(at.relative(facing.getOpposite(),3)));
  var anchor=ToyContent.keyboardAnchor(c,at);
  if(mode.equals("crawl"))c.moveTo(anchor);
  CUES.put(c,new ArrayList<>());
  var began=new long[]{-1};var ended=new boolean[]{false};
  h.onEachTick(()->{
   if(mode.equals("max_run")&&c.tickCount>1)h.assertTrue(Math.abs(c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-.44)<1e-6,"max-Run approach actually uses the physical speed cap");
   if(began[0]<0)h.assertTrue(c.getY()<anchor.y+.15,"flat approach must go round the organ without jumping onto its cabinet");
   if(c.toyPlayingKind().equals(OrganPlayRules.KIND)&&began[0]<0)began[0]=c.getPersistentData().getLong("ChaoToyUntil")-160;
   if(began[0]<0||ended[0])return;
   if(c.toyPlayingTicks()>0){
    h.assertTrue(at.equals(c.toyTarget()),"actual organ remains reserved");
    h.assertTrue(c.position().distanceToSqr(anchor)<.007,"performer stays at key-side anchor, never a side");
    var offset=c.position().subtract(Vec3.atBottomCenterOf(at));
    double front=offset.dot(new Vec3(facing.getStepX(),0,facing.getStepZ()));
    h.assertTrue(Math.abs(front-ToyInteractionRules.keyboardDistance(OrganPlayRules.KIND,c.getBbWidth()))<.02&&front<.32,"must approach close authored key front, without old arm-stretch gap");
    h.assertTrue(l.noCollision(c,c.getBoundingBox()),"body clear of cabinet");
    if(l.getGameTime()-began[0]>1)h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(c.getYRot()-facing.getOpposite().toYRot()))<1,"body faces keys for every block orientation");
   }
   if(!complete&&l.getGameTime()-began[0]>=60){
    ended[0]=true;
    switch(mode){
     case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(l.damageSources().generic(),1),"real damage interrupts");}
     case "remove"->l.removeBlock(at,false);
     case "move"->c.setPos(c.getX()+3,c.getY(),c.getZ());
     case "reload"->{var n=new CompoundTag();c.saveWithoutId(n);c.discard();var copy=ChaoGarden.CHAO.get().create(l);copy.load(n);l.addFreshEntity(copy);h.assertTrue(copy.toyPlayingTicks()==0&&copy.toyTarget()==null,"reload cancels interaction");copy.discard();}
    }
   }
  });
  h.succeedWhen(()->{
   h.assertTrue(began[0]>=0&&l.getGameTime()-began[0]>=175,"observe full interaction; began="+began[0]+" pos="+c.position()+" anchor="+anchor+" target="+c.toyTarget()+" grounded="+c.onGround()+" eligible="+ToyContent.mayPlay(c)+" navDone="+c.getNavigation().isDone()+" velocity="+c.getDeltaMovement()+" collided="+c.horizontalCollision);
   h.assertTrue(c.isRemoved()||c.toyPlayingTicks()==0&&c.toyTarget()==null,"session cleans target and clock");
   if(!mode.equals("remove"))h.assertFalse(l.getBlockState(at).getValue(ChaoToyBlock.PLAYING),"organ restored idle");
   var expected=new ArrayList<Integer>();for(int t=0;t<(complete?160:60);t++)if(OrganPlayRules.soundDue(t)){expected.add(t);expected.add(t);}
   h.assertTrue(CUES.get(c).equals(expected),"one two-note chord per contact, no extra harp/late cues: "+CUES.get(c));
   if(complete)h.assertTrue(c.state().happiness==24,"unchanged single completion reward");
   h.assertTrue(c.getMainHandItem().isEmpty(),"no cloned inventory toy");CUES.remove(c);
  });
 }
 static void blocked(GameTestHelper h,Direction facing){
  var at=RattleGameTests02520.floor(h);var l=h.getLevel();l.setBlock(at,ToyContent.block(OrganPlayRules.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
  l.setBlock(at.relative(facing),Blocks.STONE.defaultBlockState(),3);
  var c=RattleGameTests02520.actor(h,at,true,ChaoState.NORMAL,false);c.moveTo(Vec3.atBottomCenterOf(at.relative(facing.getOpposite(),3)));
  h.onEachTick(()->h.assertFalse(ToyContent.isPlaying(c),"blocked organ keys cannot fall back to sides or rear"));h.runAfterDelay(420,h::succeed);
 }
 static void clearance(GameTestHelper h){
  var at=RattleGameTests02520.floor(h);var l=h.getLevel();
  for(boolean adult:new boolean[]{false,true})for(var facing:Direction.Plane.HORIZONTAL){
   var state=ToyContent.block(OrganPlayRules.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing);l.setBlock(at,state,3);
   var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,false);c.setNoAi(true);c.moveTo(ToyContent.keyboardAnchor(c,at));
   h.assertTrue(l.noCollision(c,c.getBoundingBox()),"baby/adult close anchor clears actual collision in "+facing);
   var deeper=new Vec3(-facing.getStepX()*.06,0,-facing.getStepZ()*.06);
   h.assertFalse(l.noCollision(c,c.getBoundingBox().move(deeper)),"rear cabinet remains solid to Chao in "+facing);
   h.assertTrue(state.getCollisionShape(l,at).bounds().equals(state.getShape(l,at).bounds()),"full player collision and organ outline stay intact");
   l.setBlock(at.relative(facing),Blocks.STONE.defaultBlockState(),3);
   h.assertTrue(ToyContent.previewKeyboard(c,OrganPlayRules.KIND)==null,"close preview cannot bypass blocked authored front in "+facing);
   h.assertFalse(ToyContent.holdKeyboard(c,at),"new front obstruction interrupts close contact in "+facing);
   l.setBlock(at.relative(facing),Blocks.AIR.defaultBlockState(),3);
   h.assertTrue(at.equals(ToyContent.previewKeyboard(c,OrganPlayRules.KIND)),"clear front restores close preview in "+facing);
   c.discard();
  }
  h.succeed();
 }
 static void preview(GameTestHelper h,String mode){
  var at=RattleGameTests02520.floor(h);var l=h.getLevel();l.setBlock(at,ToyContent.block(OrganPlayRules.KIND).defaultBlockState(),3);
  var c=RattleGameTests02520.actor(h,at,false,ChaoState.NORMAL,true);var anchor=ToyContent.keyboardAnchor(c,at);c.setPos(anchor.x,anchor.y,anchor.z);c.setOnGround(true);
  boolean priorAi=!mode.equals("once");c.setNoAi(priorAi);c.getPersistentData().putLong("ChaoToyNextPlay",123456789);
  var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1,c.getY(),c.getZ());String before=c.state().encode();
  CUES.put(c,new ArrayList<>());
  try{
   var dispatcher=p.server.getCommands().getDispatcher();
   h.assertTrue(dispatcher.execute("chao testtoy organ "+(mode.equals("once")?"once":"loop"),p.createCommandSourceStack().withPermission(2))==1,"native organ preview command");
   var other=RattleGameTests02520.actor(h,at,true,ChaoState.NORMAL,false);other.setNoAi(true);other.setPos(anchor.x,anchor.y,anchor.z);
   h.assertTrue(ToyContent.previewKeyboard(other,OrganPlayRules.KIND)==null,"reserved organ is unavailable to a second actor");other.discard();
  }catch(Exception e){throw new RuntimeException(e);}
  int end=mode.equals("stop")?335:mode.equals("once")?175:80;
  if(!mode.equals("once"))h.runAtTickTime(mode.equals("stop")?330:40,()->{
   switch(mode){
    case "stop"->{h.assertTrue(before.equals(c.state().encode()),"loop preserves care, lessons, genetics and age");try{h.assertTrue(p.server.getCommands().getDispatcher().execute("chao testtoy organ stop",p.createCommandSourceStack().withPermission(2))==1,"native stop command");}catch(Exception e){throw new RuntimeException(e);}}
    case "hurt"->{c.setInvulnerable(false);c.hurt(l.damageSources().generic(),1);}
    case "remove"->l.removeBlock(at,false);
    case "move"->c.setPos(c.getX()+3,c.getY(),c.getZ());
    case "interact"->{c.owner=p.getUUID();c.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);}
    case "reload"->{var n=new CompoundTag();c.saveWithoutId(n);c.discard();var copy=ChaoGarden.CHAO.get().create(l);copy.load(n);l.addFreshEntity(copy);h.assertTrue(copy.isNoAi()==priorAi&&copy.toyTarget()==null&&copy.toyPlayingTicks()==0,"load restores AI without replay");copy.discard();}
    case "logout"->p.server.getPlayerList().remove(p);
    case "distance"->p.teleportTo(c.getX()+70,c.getY(),c.getZ());
   }
  });
  h.runAtTickTime(end,()->{
   h.assertFalse(ToyPreview.active(c),"preview stopped cleanly");h.assertTrue(c.isNoAi()==priorAi&&c.toyTarget()==null&&c.toyPlayingTicks()==0,"AI and session restored");
   h.assertTrue(c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"preview preserves cooldown");
   if(!mode.equals("remove"))h.assertFalse(l.getBlockState(at).getValue(ChaoToyBlock.PLAYING),"real organ returns idle");
   var ticks=CUES.get(c);int cutoff=mode.equals("stop")?330:mode.equals("once")?160:41;
   var expected=new ArrayList<Integer>();for(int t=0;t<cutoff;t++)if(OrganPlayRules.soundDue(t%160)){expected.add(t%160);expected.add(t%160);}
   h.assertTrue(ticks.equals(expected),"preview uses production sound clock and stops cues: "+ticks);
   CUES.remove(c);p.server.getPlayerList().remove(p);h.succeed();
  });
 }
}
