package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.core.*;
import com.chaogarden.care.*;
import com.chaogarden.activities.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="chao_garden")
@GameTestHolder("chao_focused42") @PrefixGameTestTemplate(false)
public final class FocusedGameTests042 {
 static final Map<ChaoEntity,List<Integer>> CUES=new WeakHashMap<>();
 @net.neoforged.bus.api.SubscribeEvent public static void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition e){
  if(e.getSound()==null||e.getSound().value()!=net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value())return;
  CUES.forEach((c,list)->{var p=c.toyTarget();if(p!=null&&c.level()==e.getLevel()&&e.getPosition().distanceToSqr(new Vec3(p.getX()+.5,p.getY()+.21,p.getZ()+.5))<1e-6)list.add(160-c.toyPlayingTicks());});
 }

 @GameTest(template="activity_test",timeoutTicks=100)
 public static void analyzerAuthorizationAndSave(GameTestHelper h){
  var at=RattleGameTests02520.floor(h);var c=RattleGameTests02520.actor(h,at,true,ChaoState.NORMAL,false);c.setNoAi(true);
  var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1,c.getY(),c.getZ());c.owner=p.getUUID();
  var edit=new ChaoCareNetwork.StatEdit(c.getUUID().toString(),0,1,10);String before=c.state().encode();
  h.assertFalse(ChaoCareNetwork.editStats(p,edit),"forged Survival request denied");h.assertTrue(before.equals(c.state().encode()),"denial changes nothing");
  p.setGameMode(GameType.CREATIVE);h.assertTrue(ChaoCareNetwork.editStats(p,edit),"Creative owner accepted");
  h.assertTrue(c.state().levels[0]==10&&c.state().points[0]==10*(13+3*c.state().grades[0]),"dependent points updated");
  for(int i=0;i<4;i++)h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),i,2,75)),"progress edit accepted");
  var encoded=c.getEntityData().packDirty();h.assertTrue(encoded!=null&&!encoded.isEmpty(),"edits dirty synchronized entity state");
  var n=new CompoundTag();c.saveWithoutId(n);var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(n);h.assertTrue(copy.state().encode().equals(c.state().encode()),"native NBT round trip retains edits");
  h.assertFalse(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),5,1,1)),"invalid stat denied");
  h.assertFalse(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit("bad",0,0,0)),"malformed UUID denied");
  h.assertFalse(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),0,1,999)),"forged delta denied");
  p.teleportTo(c.getX()+20,c.getY(),c.getZ());h.assertFalse(ChaoCareNetwork.editStats(p,edit),"distance denied");
  p.teleportTo(c.getX()+1,c.getY(),c.getZ());p.setGameMode(GameType.SPECTATOR);h.assertFalse(ChaoCareNetwork.editStats(p,edit),"Spectator denied");
  p.server.getPlayerList().remove(p);h.succeed();
 }
 @GameTest(template="activity_test",timeoutTicks=100)
 public static void gardenSquareAndLegacyPersistence(GameTestHelper h){
  var l=h.getLevel();
  for(var at:List.of(new BlockPos(5,90,7),new BlockPos(-1,90,-17),new BlockPos(16,90,16))){
   var a=GardenArea.inspect(l,at);h.assertTrue(a.chunkSquare,"no enclosure, tree or floor required");int x=((at.getX()>>4)-1)*16,z=((at.getZ()>>4)-1)*16;
   for(var p:List.of(new Vec3(x,90,z),new Vec3(x+47.999,90,z+47.999),new Vec3(x+1,l.getMinBuildHeight(),z+1)))h.assertTrue(a.contains(l,p),"square includes all nine chunks");
   for(var p:List.of(new Vec3(x-.001,90,z),new Vec3(x+48,90,z),new Vec3(x,90,z-.001),new Vec3(x,90,z+48),new Vec3(x,l.getMaxBuildHeight(),z)))h.assertFalse(a.contains(l,p),"exclusive outer boundary");
   var restored=GardenArea.load(a.save());h.assertTrue(restored.chunkSquare&&restored.contains(l,new Vec3(x+47,90,z+47)),"square NBT round trip");
  }
  var old=new GardenArea(false,List.of(new BlockPos(8,90,8).asLong()),new BlockPos(8,90,8));var restored=GardenArea.load(old.save());
  h.assertFalse(restored.chunkSquare,"legacy format retains footprint");h.assertTrue(restored.contains(l,new Vec3(8.5,90,8.5)),"old inside preserved");h.assertFalse(restored.contains(l,new Vec3(9.5,90,8.5)),"old outside preserved");h.succeed();
 }
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_false_NORTH_complete(GameTestHelper h){piano(h,false,Direction.NORTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_false_SOUTH_complete(GameTestHelper h){piano(h,false,Direction.SOUTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_false_EAST_complete(GameTestHelper h){piano(h,false,Direction.EAST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_false_WEST_complete(GameTestHelper h){piano(h,false,Direction.WEST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_hurt") public static void piano_false_NORTH_hurt(GameTestHelper h){piano(h,false,Direction.NORTH,"hurt");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_remove") public static void piano_false_NORTH_remove(GameTestHelper h){piano(h,false,Direction.NORTH,"remove");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_reload") public static void piano_false_NORTH_reload(GameTestHelper h){piano(h,false,Direction.NORTH,"reload");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_preview") public static void piano_false_NORTH_preview(GameTestHelper h){piano(h,false,Direction.NORTH,"preview");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_true_NORTH_complete(GameTestHelper h){piano(h,true,Direction.NORTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_true_SOUTH_complete(GameTestHelper h){piano(h,true,Direction.SOUTH,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_true_EAST_complete(GameTestHelper h){piano(h,true,Direction.EAST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_complete") public static void piano_true_WEST_complete(GameTestHelper h){piano(h,true,Direction.WEST,"complete");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_hurt") public static void piano_true_NORTH_hurt(GameTestHelper h){piano(h,true,Direction.NORTH,"hurt");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_remove") public static void piano_true_NORTH_remove(GameTestHelper h){piano(h,true,Direction.NORTH,"remove");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_reload") public static void piano_true_NORTH_reload(GameTestHelper h){piano(h,true,Direction.NORTH,"reload");}
 @GameTest(template="activity_test",timeoutTicks=1800,batch="piano42_preview") public static void piano_true_NORTH_preview(GameTestHelper h){piano(h,true,Direction.NORTH,"preview");}
 static void piano(GameTestHelper h,boolean adult,Direction facing,String mode){
  var at=RattleGameTests02520.floor(h);var l=h.getLevel();l.setBlock(at,ToyContent.block(PianoPlayRules.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
  var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,adult);var anchor=ToyContent.keyboardAnchor(c,at);c.setPos(anchor.x,anchor.y,anchor.z);c.setOnGround(true);
  CUES.put(c,new ArrayList<>());var began=new long[]{-1};var ended=new boolean[]{false};var keeper=new net.minecraft.server.level.ServerPlayer[1];
  if(mode.equals("preview")){
   keeper[0]=TestConnections.makeSurvivalServerPlayer(h);keeper[0].teleportTo(c.getX(),c.getY(),c.getZ()+2);
   c.setNoAi(true);String data=c.state().encode();
   try{h.assertTrue(keeper[0].server.getCommands().getDispatcher().execute("chao testtoy piano loop",keeper[0].createCommandSourceStack().withPermission(2))==1,"native piano command");}catch(Exception e){throw new RuntimeException(e);}
   h.runAtTickTime(335,()->{h.assertTrue(ToyPreview.active(c)&&c.toyTarget().equals(at),"loop keeps real piano reservation");h.assertTrue(data.equals(c.state().encode()),"preview awards no care, age or lessons");
    ToyPreview.stop(c);h.assertTrue(c.isNoAi()&&c.toyTarget()==null&&!l.getBlockState(at).getValue(ChaoToyBlock.PLAYING),"stop restores AI and idle block");keeper[0].server.getPlayerList().remove(keeper[0]);h.succeed();});return;
  }
  h.onEachTick(()->{
   if(c.toyPlayingKind().equals(PianoPlayRules.KIND)&&began[0]<0)began[0]=l.getGameTime();
   if(began[0]<0||ended[0])return;
   if(c.toyPlayingTicks()>0){h.assertTrue(at.equals(c.toyTarget()),"real target synchronized");h.assertTrue(c.position().distanceToSqr(anchor)<.01,"front contact anchor");h.assertTrue(l.noCollision(c,c.getBoundingBox()),"body clear of cabinet");}
   if(!mode.equals("complete")&&l.getGameTime()-began[0]>=60){
    ended[0]=true;
    if(mode.equals("hurt")){c.setInvulnerable(false);h.assertTrue(c.hurt(l.damageSources().generic(),1),"real damage");}
    if(mode.equals("remove"))l.removeBlock(at,false);
    if(mode.equals("reload")){var n=new CompoundTag();c.saveWithoutId(n);c.discard();var copy=ChaoGarden.CHAO.get().create(l);copy.load(n);l.addFreshEntity(copy);h.assertTrue(copy.toyPlayingTicks()==0&&copy.toyTarget()==null,"reload cancels session");copy.discard();}
   }
  });
  h.succeedWhen(()->{h.assertTrue(began[0]>=0&&l.getGameTime()-began[0]>=175,"observe whole interaction");h.assertTrue(c.isRemoved()||c.toyPlayingTicks()==0&&c.toyTarget()==null,"session cleaned");if(!mode.equals("remove"))h.assertFalse(l.getBlockState(at).getValue(ChaoToyBlock.PLAYING),"piano returned idle");h.assertTrue(c.getHeldChaoItem().isEmpty(),"no cloned item");
    var expected=new ArrayList<Integer>();for(int t=0;t<(mode.equals("complete")?160:60);t++)if(PianoPlayRules.soundDue(t))expected.add(t);
    h.assertTrue(CUES.get(c).equals(expected),"one sound per authored press, silence after interruption: "+CUES.get(c));CUES.remove(c);});
 }
}
