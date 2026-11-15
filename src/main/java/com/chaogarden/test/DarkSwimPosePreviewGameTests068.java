package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ToyPreview;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** New operator adapters exercise existing temporary sessions through the real dispatcher.
 * Native lifecycle/authority checks do not claim client rendering or listening. */
@GameTestHolder("chao_dark_swim68") @PrefixGameTestTemplate(false)
public final class DarkSwimPosePreviewGameTests068 {
  private static final List<String> POSES=List.of("crawl","paddle","swim","glide","flail");
  private static final String TEMPLATE="chao_morph54:activity_test",RESTORE="ChaoToyPreviewRestoreNoAi";
  private static final BlockPos AT=new BlockPos(3,2,3);
  private DarkSwimPosePreviewGameTests068() {}
  private record Fixture(ChaoEntity actor,ServerPlayer player,boolean noAi,String state,ItemStack hat,ItemStack realItem) {}
  private static void cleanup(GameTestHelper h,Runnable action){
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
    });
  }
  private static Fixture fixture(GameTestHelper h,boolean noAi){
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production Chao creates");
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.SWIM_TYPE;s.align=-1;s.ageTicks=2345;s.adultAgeTicks=432;
    s.hunger=80;s.energy=100;s.happiness=70;s.boredom=20;s.setEvolutionStrength(1.2f);
    s.levels[ChaoState.RUN]=9;s.progress[ChaoState.RUN]=60;s.levels[ChaoState.FLY]=9;s.progress[ChaoState.FLY]=60;
    s.setTrait("preview68.earned","retain genetics, lessons, age and care");s.setTrait("lesson.learned.triangle","1");
    c.setState(ChaoState.decode(s.encode()));c.setInvulnerable(true);c.setNoGravity(false);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));c.setOnGround(true);h.getLevel().addFreshEntity(c);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.setNoAi(noAi);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));c.getPersistentData().putLong("ChaoToyNextPlay",123456789);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    p.getInventory().setItem(5,new ItemStack(Items.ENDER_PEARL,3));p.teleportTo(c.getX()+1,c.getY(),c.getZ());c.owner=p.getUUID();
    var f=new Fixture(c,p,noAi,c.state().encode(),c.getItemBySlot(EquipmentSlot.HEAD).copy(),p.getInventory().getItem(5).copy());
    cleanup(h,()->{ToyPreview.stop(c);c.discard();if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});return f;
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException rejected){return -1;}
  }
  private static int command(Fixture f,String id,String mode){return command(f.player(),"chao testpose "+id+(mode.isEmpty()?"":" "+mode),2);}
  private static void items(GameTestHelper h,Fixture f,ChaoEntity c){
    h.assertTrue(ItemStack.matches(c.getItemBySlot(EquipmentSlot.HEAD),f.hat())&&ItemStack.matches(f.player().getInventory().getItem(5),f.realItem()),"Preview retains actual equipment and keeper inventory items");
    h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"Pose previews create no real hand props");
    h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(2)).isEmpty(),"Preview mints no dropped items");
  }
  private static void restored(GameTestHelper h,Fixture f,ChaoEntity c,String state){
    h.assertTrue(!ToyPreview.active(c)&&c.isNoAi()==f.noAi()&&!c.isNoGravity()&&c.toyPlayingTicks()==0&&c.toyPlayingKind().isEmpty()&&c.toyTarget()==null&&!c.getPersistentData().contains(RESTORE),"Cleanup restores original AI and clears only temporary session controls");
    h.assertTrue(c.state().encode().equals(state)&&c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"Cleanup preserves exact care, genes, lessons, age and ordinary cooldown");items(h,f,c);
  }
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();
    for(String pose:POSES)for(boolean noAi:new boolean[]{false,true})
      tests.add(new TestFunction("dark_swim068_pose_play_"+(tests.size()/4),"pose_"+pose+"_once_loop_stop_original_noai_"+noAi,TEMPLATE,380,0,true,h->play(h,pose,noAi)));
    for(String pose:POSES)for(boolean noAi:new boolean[]{false,true})for(String mode:List.of("stop","interaction","damage","save_reload","unload","disconnect"))
      tests.add(new TestFunction("dark_swim068_pose_cleanup_"+(tests.size()/8),"pose_"+pose+"_cleanup_"+mode+"_original_noai_"+noAi,TEMPLATE,80,0,true,h->interrupt(h,pose,noAi,mode)));
    for(String pose:POSES)tests.add(new TestFunction("dark_swim068_pose_guards","pose_"+pose+"_rejects_actual_held_item",TEMPLATE,40,0,true,h->heldGuard(h,pose)));
    tests.add(new TestFunction("dark_swim068_pose_commands","pose_native_permissions_catalog_and_invalid_arguments",TEMPLATE,40,0,true,DarkSwimPosePreviewGameTests068::permissions));
    tests.add(new TestFunction("dark_swim068_pose_commands","pose_switch_and_named_stop_preserve_nearest_selection",TEMPLATE,40,0,true,DarkSwimPosePreviewGameTests068::switchAndNearest));
    return tests;
  }
  private static void play(GameTestHelper h,String pose,boolean noAi){
    var f=fixture(h,noAi);var c=f.actor();h.assertTrue(command(f,pose,"once")==1,"Actual once command starts "+pose);
    h.assertTrue(c.toyPlayingKind().equals("preview_"+pose)&&ToyPreview.poseId(c.toyPlayingKind()).equals(pose)&&c.toyPlayingTicks()==160&&c.isNoAi(),"Existing tracked160tick clock selects the production pose token");
    h.onEachTick(()->{if(ToyPreview.active(c)){ToyPreview.tick(c);ToyPreview.tick(c);h.assertTrue(c.state().encode().equals(f.state()),"Preview freezes all exact saved care and earned state without duplicate tick effects");items(h,f,c);}});
    h.runAtTickTime(160,()->{
      ToyPreview.tick(c);restored(h,f,c,f.state());
      String beforeSettle=ToyPreview.unavailable(c);
      if(!c.onGround()){
        // Restoring ordinary AI can clear the stationary ground flag in this same
        // tick. Resolve a real downward collision with the existing fixture floor;
        // never force onGround or change the production dry-ground start guard.
        var before=c.position();var floor=c.blockPosition().below();
        h.assertTrue(!c.isInWater()&&!h.getLevel().getBlockState(floor).getCollisionShape(h.getLevel(),floor).isEmpty(),"Restart fixture retains real dry support beneath its unchanged standing position");
        c.move(MoverType.SELF,new Vec3(0,-.01,0));
        h.assertTrue(c.onGround()&&c.position().distanceToSqr(before)<1e-12,"Actual vanilla collision settles the stationary actor on its existing floor without teleporting");
        restored(h,f,c,f.state());
        com.mojang.logging.LogUtils.getLogger().info("DARKSWIM068_POSE_FIXTURE_SETTLE pose={} originalNoAi={} beforeGuard={} actualDownwardCollision=true positionUnchanged=true stateItemsAndControlsExact=true",pose,noAi,beforeSettle);
      }
      String reason=ToyPreview.unavailable(c);
      h.assertTrue(reason.isEmpty(),"Completed once restart guard: "+reason+"; onGround="+c.onGround()+" inWater="+c.isInWater()+" behavior="+c.getBehavior()+" hurt="+c.hurtTime+" noAi="+c.isNoAi()+" pos="+c.position()+" player="+f.player().position()+" kind="+c.toyPlayingKind()+" ticks="+c.toyPlayingTicks());
      h.assertTrue(command(f,pose,"loop")==1,"Actual loop command restarts after completed once");
    });
    h.runAtTickTime(325,()->{
      ToyPreview.tick(c);h.assertTrue(ToyPreview.active(c)&&c.toyPlayingTicks()>0&&c.toyPlayingTicks()<=160&&c.toyPlayingKind().equals("preview_"+pose),"Loop crossed one full production phrase and remains active");
      h.assertTrue(command(f,pose,"stop")==1,"Per-pose native stop succeeds");restored(h,f,c,f.state());h.succeed();
    });
  }
  private static void interrupt(GameTestHelper h,String pose,boolean noAi,String mode){
    var f=fixture(h,noAi);var c=f.actor();h.assertTrue(command(f,pose,"loop")==1,"Actual loop starts before "+mode);
    h.runAtTickTime(40,()->{
      h.assertTrue(ToyPreview.active(c)&&c.state().encode().equals(f.state()),"No care, age or training changes before interruption");
      String expected=f.state();
      switch(mode){
        case "stop"->h.assertTrue(command(f.player(),"chao testpose stop",2)==1,"Global native pose stop succeeds");
        case "interaction"->{
          f.player().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));var held=f.player().getMainHandItem().copy();
          h.assertTrue(c.mobInteract(f.player(),InteractionHand.MAIN_HAND).consumesAction(),"Actual Analyzer interaction stops the session");
          h.assertTrue(ItemStack.matches(held,f.player().getMainHandItem()),"Interruption preserves the actual Analyzer item");
        }
        case "damage"->{
          c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"Real damage is accepted during preview");
          expected=c.state().encode();h.assertTrue(c.state().ageTicks==2345&&c.state().adultAgeTicks==432,"Damage and preview preserve chronology");
        }
        case "disconnect"->f.player().server.getPlayerList().remove(f.player());
        case "save_reload","unload"->{
          var tag=new CompoundTag();c.saveWithoutId(tag);
          h.assertTrue(tag.getBoolean("NoAI")==noAi&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"Saved snapshot excludes temporary preview clock and uses original NoAI");
          h.assertTrue(ToyPreview.active(c)&&c.isNoAi()&&c.state().encode().equals(f.state()),"Saving leaves the live session and exact care intact");
          c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
          h.runAfterDelay(2,()->{
            restored(h,f,c,f.state());
            if(mode.equals("save_reload")){
              var loaded=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(loaded!=null,"Actual saved Chao reloads");loaded.load(tag);h.getLevel().addFreshEntity(loaded);cleanup(h,loaded::discard);
              restored(h,f,loaded,f.state());h.assertTrue(loaded.getUUID().equals(c.getUUID())&&loaded.owner.equals(c.owner),"Entity NBT retains UUID and keeper through preview reload");
            }h.succeed();
          });return;
        }
      }
      ToyPreview.tick(c);restored(h,f,c,expected);h.succeed();
    });
  }
  private static void heldGuard(GameTestHelper h,String pose){
    var f=fixture(h,false);var c=f.actor();var stack=new ItemStack(Items.APPLE,2);c.setItemSlot(EquipmentSlot.MAINHAND,stack);
    h.assertTrue(command(f,pose,"once")==0&&!ToyPreview.active(c)&&!c.isNoAi(),"Actual held item refuses pose start without a temporary session");
    h.assertTrue(ItemStack.matches(c.getMainHandItem(),new ItemStack(Items.APPLE,2))&&c.state().encode().equals(f.state()),"Rejected start preserves real held stack and exact saved state");h.succeed();
  }
  private static void permissions(GameTestHelper h){
    var f=fixture(h,false);var dispatcher=f.player().server.getCommands().getDispatcher();
    for(String pose:POSES){
      for(String mode:List.of("once","loop","stop"))h.assertTrue(command(f.player(),"chao testpose "+pose+" "+mode,0)==-1,"Creative non-operator cannot invoke operator pose modes");
      var modes=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testpose "+pose+" ",f.player().createCommandSourceStack().withPermission(2))).join().getList().stream().map(v->v.getText()).toList();
      h.assertTrue(modes.containsAll(List.of("once","loop","stop")),"All actual preview modes are suggested for "+pose);
    }
    var names=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testpose ",f.player().createCommandSourceStack().withPermission(2))).join().getList().stream().map(v->v.getText()).toList();
    h.assertTrue(names.containsAll(POSES)&&names.containsAll(List.of("sit","list","stop")),"Pose catalog retains sit and lists all five production motion adapters");
    h.assertTrue(command(f.player(),"chao testpose list",2)==1&&command(f.player(),"chao testtoy list",2)==1,"Both actual help commands execute");
    for(String invalid:List.of("chao testpose unknown once","chao testpose crawl invalid"))h.assertTrue(command(f.player(),invalid,2)==-1&&!ToyPreview.active(f.actor()),"Invalid pose arguments remain read-only");
    h.assertTrue(f.actor().state().encode().equals(f.state()),"Permission and catalog probes change no care or session data");h.succeed();
  }
  private static void switchAndNearest(GameTestHelper h){
    var f=fixture(h,false);var other=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(other!=null,"Decoy Chao creates");
    other.moveTo(f.player().getX()+6,f.actor().getY(),f.actor().getZ());other.setOnGround(true);h.getLevel().addFreshEntity(other);cleanup(h,other::discard);
    h.assertTrue(command(f,"crawl","loop")==1&&ToyPreview.active(f.actor())&&!ToyPreview.active(other),"Native preview selects only the nearest real Chao");
    h.assertTrue(command(f,"paddle","stop")==0&&ToyPreview.active(f.actor()),"Wrong per-pose stop cannot cancel another active pose");
    h.assertTrue(command(f,"swim","loop")==1&&f.actor().toyPlayingKind().equals("preview_swim")&&f.actor().toyPlayingTicks()==160,"Switch reuses cleanup and restarts the existing clock");
    h.assertTrue(command(f.player(),"chao testtoy stop",2)==1,"Existing global toy stop also clears a pose adapter");restored(h,f,f.actor(),f.state());h.succeed();
  }
}
