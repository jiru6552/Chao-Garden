package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

/** Actual goal, native item/save lifecycle and dispatcher checks for each garden activity. */
@GameTestHolder("chao_toys49") @PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class GardenToyGameTests049 {
  private static final List<String> KINDS=List.of("chao_rocking_horse","chao_toy_car","chao_sonic_doll","chao_shadow_doll","chao_broom","chao_picture_book","chao_pogo_stick","chao_crayons");
  private static final Set<String> MOUNTED=Set.of("chao_rocking_horse","chao_pogo_stick");
  private record Heard(String kind,List<Long> ticks) {}
  private static final Map<ChaoEntity,Heard> CUES=new WeakHashMap<>();
  private static String soundId(String kind){return switch(kind){
    case "chao_rocking_horse"->"minecraft:block.wood.step";
    case "chao_toy_car"->"chao_garden:toy.car";
    case "chao_sonic_doll","chao_shadow_doll"->"chao_garden:toy.doll";
    case "chao_broom"->"chao_garden:toy.broom";
    case "chao_picture_book"->"chao_garden:toy.book_page";
    case "chao_crayons"->"chao_garden:toy.crayon";
    default->"chao_garden:toy.pogo";
  };}
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition event){
    if(event.getLevel().isClientSide||event.getSound()==null)return;
    String id=BuiltInRegistries.SOUND_EVENT.getKey(event.getSound().value()).toString();
    if(!id.startsWith("chao_garden:toy.")&&!id.equals("minecraft:block.wood.step")&&!id.equals("minecraft:block.slime_block.step")&&!id.startsWith("minecraft:block.note_block."))return;
    CUES.forEach((c,heard)->{
      if(c.level()!=event.getLevel())return;
      var at=c.toyTarget();boolean actor=event.getPosition().distanceToSqr(c.position().add(0,.25,0))<1e-7;
      boolean source=at!=null&&event.getPosition().distanceToSqr(Vec3.atCenterOf(at))<.05;
      if(actor||source)heard.ticks.add(id.equals(soundId(heard.kind))?c.level().getGameTime():Long.MIN_VALUE);
    });
  }
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static void remove(ServerPlayer p){if(p!=null&&p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);}
  private static BlockPos floor(GameTestHelper h,String kind,Direction facing){var at=RattleGameTests02520.floor(h);h.getLevel().setBlock(at,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);return at;}
  private static ChaoEntity actor(GameTestHelper h,BlockPos at,String kind,boolean adult,boolean hands){
    var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,hands);c.setState(ChaoState.decode(c.state().encode()));
    if(!adult)c.state().points[ChaoState.RUN]=LocomotionRules.WALK_POINTS;
    c.state().setTrait("toy49.opaque","preserve-this-unrelated-value");c.sync();CUES.put(c,new Heard(kind,new ArrayList<>()));return c;
  }
  private static List<Long> heard(ChaoEntity c){return CUES.get(c).ticks;}
  private static long start(ChaoEntity c){return c.getPersistentData().getLong("ChaoToyUntil")-160;}
  // Authored times deliberately do not call the production predicates.
  private static boolean due(String kind,int tick){return switch(kind){
    case "chao_toy_car"->tick==32||tick==64||tick==96||tick==128;
    case "chao_sonic_doll","chao_shadow_doll"->tick==36||tick==84||tick==132;
    case "chao_picture_book"->tick==56||tick==120;
    default->tick>=24&&tick<=136&&(tick-24)%16==0;
  };}
  private static List<Long> expected(String kind,long began,int cutoff){var result=new ArrayList<Long>();for(int t=0;t<cutoff;t++)if(due(kind,t%160))result.add(began+t);return result;}
  private static int drops(GameTestHelper h,BlockPos at,String kind){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4),e->e.getItem().is(ChaoGarden.item(kind))).stream().mapToInt(e->e.getItem().getCount()).sum();}
  private static int command(ServerPlayer p,String text){try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(2));}catch(Exception e){throw new RuntimeException(e);}}

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();int[] n={0};java.util.function.BiConsumer<String,java.util.function.Consumer<GameTestHelper>> add=(name,body)->tests.add(new TestFunction("garden049_"+(n[0]++/8),name,"chao_toys49:activity_test",1800,0,true,body));
    for(String kind:KINDS){String shortName=kind.substring(5);
      for(boolean adult:new boolean[]{false,true}){
        for(Direction facing:Direction.Plane.HORIZONTAL)add.accept(shortName+"_complete_"+adult+"_"+facing,h->placed(h,kind,adult,facing,"complete"));
        for(boolean loop:new boolean[]{false,true})add.accept(shortName+"_preview_"+adult+"_"+loop,h->preview(h,kind,adult,loop));
      }
      for(String mode:List.of("hurt","move","remove","reload"))add.accept(shortName+"_placed_"+mode,h->placed(h,kind,mode.equals("reload"),Direction.WEST,mode));
      for(String mode:List.of("hurt","pet","item","logout","range","reload","stop"))add.accept(shortName+"_preview_interrupt_"+mode,h->previewInterrupt(h,kind,mode));
      for(String specimen:List.of("animal_rabbit_baby","chaos_2"))add.accept(shortName+"_variant_"+specimen,h->variant(h,kind,specimen));
      if(MOUNTED.contains(kind)){
        for(String mode:List.of("move","remove"))add.accept(shortName+"_preview_interrupt_"+mode,h->previewInterrupt(h,kind,mode));
        add.accept(shortName+"_low_roof",h->roof(h,kind,false));add.accept(shortName+"_new_roof",h->roof(h,kind,true));
        add.accept(shortName+"_deferred_exit_reload_recovery",h->deferred(h,kind));
        add.accept(shortName+"_external_ground_rescue",h->rescue(h,kind));
        for(boolean flags:new boolean[]{false,true})add.accept(shortName+"_sealed_preview_switch_"+flags,h->sealedSwitch(h,kind,flags));
      }
    }
    add.accept("flat_five_toys_real_collision_all_facings",GardenToyGameTests049::flatCollision);
    add.accept("existing_ring_non_swimmer",MobileToyGameTests02518::lifesaverSupportsNonSwimmer);
    add.accept("existing_ring_swimmer",MobileToyGameTests02518::lifesaverSupportsSwimmer);
    add.accept("existing_ring_drained_cleanup",MobileToyGameTests02518::drainedLifesaverCancelsAndRestoresGravity);
    add.accept("existing_ring_low_roof",MobileToyGameTests02518::ringRejectsLowRoof);
    add.accept("existing_ring_new_roof",MobileToyGameTests02518::ringCancelsNewRoof);
    return tests;
  }

  private static void placed(GameTestHelper h,String kind,boolean adult,Direction facing,String mode){
    var at=floor(h,kind,facing);var l=h.getLevel();var c=actor(h,at,kind,adult,adult);c.moveTo(Vec3.atBottomCenterOf(at.relative(facing.getOpposite(),3)));
    long[] began={-1},changed={-1};int[] countAtStop={-1};float[] happiness={20};var restored=new ChaoEntity[1];
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();if(restored[0]!=null){CUES.remove(restored[0]);restored[0].discard();}});
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);if(began[0]<0||changed[0]>=0)return;
      if(ToyContent.isPlaying(c)){
        h.assertTrue(kind.equals(c.toyPlayingKind())&&at.equals(c.toyTarget()),"actual AI reserves this source");
        h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"presentation creates no real inventory copy");
        if(MOUNTED.contains(kind)){
          h.assertTrue(c.isNoGravity(),"server owns mounted gravity");long elapsed=l.getGameTime()-began[0];
          if(elapsed>=24&&elapsed<=136){var state=l.getBlockState(at);var a=ToyContent.mountedAnchor(c,at,state,elapsed);var b=ToyContent.mountedAnchor(c,at,state,elapsed-1);h.assertTrue(Math.min(c.distanceToSqr(a),c.distanceToSqr(b))<.003,"rider follows production contact clock without floating away");}
        }else{
          h.assertTrue(l.noCollision(c,c.getBoundingBox()),"stationary actor clears source collision");
          h.assertTrue(l.getBlockState(at).getRenderShape()==RenderShape.INVISIBLE,"reserved physical source is hidden during hand presentation");
          if(l.getGameTime()-began[0]>2){var d=Vec3.atCenterOf(at).subtract(c.position());float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90);h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(c.getYRot()-yaw))<2,"actor faces the actual toy contact");}
        }
      }
      if(!mode.equals("complete")&&l.getGameTime()-began[0]>=64){changed[0]=l.getGameTime();countAtStop[0]=heard(c).size();
        switch(mode){
          case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(l.damageSources().generic(),1),"real damage accepted");happiness[0]=c.state().happiness;}
          case "move"->c.setPos(c.getX()+5,c.getY(),c.getZ());
          case "remove"->l.destroyBlock(at,true);
          case "reload"->{String before=c.state().encode();var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();var copy=ChaoGarden.CHAO.get().create(l);copy.load(tag);restored[0]=copy;CUES.put(copy,CUES.get(c));h.assertTrue(copy.state().encode().equals(before),"native reload preserves care, genes, age, lessons and opaque unrelated state");copy.goalSelector.removeAllGoals(g->true);copy.targetSelector.removeAllGoals(g->true);copy.goalSelector.addGoal(4,new ToyContent.PlayGoal(copy));l.addFreshEntity(copy);h.assertTrue(copy.toyTarget()==null&&copy.toyPlayingTicks()==0&&!copy.isNoGravity(),"loaded session releases stale reservation and gravity");}
        }
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&l.getGameTime()-began[0]>=180,"observe natural approach, full activity and exit: "+kind+" position="+c.position());var live=restored[0]==null?c:restored[0];boolean complete=mode.equals("complete");
      h.assertTrue(!ToyContent.isPlaying(live)&&live.toyTarget()==null&&!live.isNoGravity(),"exit clears clock, reservation and mounted gravity");
      h.assertTrue(live.state().happiness==(complete?24:happiness[0]),"only normal completion rewards exactly once");
      h.assertTrue(live.state().trait("toy49.opaque").equals("preserve-this-unrelated-value"),"unrelated save traits survive activity");
      if(complete){h.assertTrue(heard(c).equals(expected(kind,began[0],160)),"actual authored cues, no generic duplicate pulses: "+heard(c));h.assertTrue(live.getPersistentData().getLong("ChaoToyNextPlay")>l.getGameTime(),"ordinary cooldown retained");}
      else h.assertTrue(changed[0]>=0&&heard(c).size()==countAtStop[0],"no later cue after interruption");
      if(mode.equals("remove"))h.assertTrue(l.getBlockState(at).isAir()&&drops(h,at,kind)==1,"breaking source returns exactly one actual toy");
      else h.assertTrue(l.getBlockState(at).getRenderShape()==RenderShape.MODEL&&!l.getBlockState(at).getValue(ChaoToyBlock.PLAYING)&&drops(h,at,kind)==0,"source returns visible/idle without copies");
      h.assertTrue(live.getMainHandItem().isEmpty()&&live.getHeldChaoItem().isEmpty(),"exit leaves no prop item");
    });
  }

  private static void variant(GameTestHelper h,String kind,String specimen){
    var at=floor(h,kind,Direction.SOUTH);var state=ToyVariantCases02522.all().stream().filter(v->v.name().equals(specimen)).findFirst().orElseThrow().state();
    if(!state.adult)state.points[ChaoState.RUN]=LocomotionRules.WALK_POINTS;
    var c=actor(h,at,kind,state.adult,false);c.setState(state);String character=state.character;boolean immortal=state.immortal;long[] began={-1};
    cleanup(h,()->{CUES.remove(c);c.discard();});h.onEachTick(()->{if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);});
    h.succeedWhen(()->{h.assertTrue(began[0]>=0&&h.getLevel().getGameTime()-began[0]>=180,"observe actual replacement/Chaos interaction");h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==24,"variant completes and exits once");h.assertTrue(c.state().character.equals(character)&&c.state().immortal==immortal,"activity preserves identity");h.assertTrue(heard(c).equals(expected(kind,began[0],160)),"variant shares actual cue clock");});
  }

  private static void preview(GameTestHelper h,String kind,boolean adult,boolean loop){
    var at=floor(h,kind,Direction.EAST);var c=actor(h,at,kind,adult,adult);c.goalSelector.removeAllGoals(g->true);
    if(!MOUNTED.contains(kind)){h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);c.moveTo(Vec3.atBottomCenterOf(at));}
    c.setOnGround(true);c.setNoAi(adult);c.getPersistentData().putLong("ChaoToyNextPlay",123456789);String before=c.state().encode();boolean priorAi=c.isNoAi();
    var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1.5,c.getY(),c.getZ()+1.5);cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);});
    String id=kind.substring(5);h.assertTrue(command(p,"chao testtoy "+id+" "+(loop?"loop":"once"))==1,"real preview command starts without a real inventory prop");long began=h.getLevel().getGameTime();
    h.onEachTick(()->{if(ToyPreview.active(c)){ToyPreview.tick(c);ToyPreview.tick(c);h.assertTrue(c.state().encode().equals(before),"active preview preserves exact care, age, genes and lessons");h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"preview cannot mint items");}});
    h.runAtTickTime(loop?325:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(c),"loop reaches third production phrase");h.assertTrue(command(p,"chao testtoy "+id+" stop")==1,"native named stop executes");}else ToyPreview.tick(c);
      h.assertTrue(!ToyPreview.active(c)&&c.isNoAi()==priorAi&&!c.isNoGravity()&&c.toyPlayingTicks()==0&&c.toyTarget()==null,"once/stop restores original AI and gravity and clears source");
      h.assertTrue(c.state().encode().equals(before)&&c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"preview changes no care, rewards, lessons or cooldown");
      h.assertTrue(heard(c).equals(expected(kind,began,loop?320:160)),"preview cues reuse production clock idempotently: "+heard(c));
      if(MOUNTED.contains(kind))h.assertTrue(!h.getLevel().getBlockState(at).getValue(ChaoToyBlock.PLAYING),"real mounted source returns idle");else h.assertTrue(h.getLevel().getBlockState(at).isAir(),"hand preview creates no placed source");h.succeed();
    });
  }

  private static void previewInterrupt(GameTestHelper h,String kind,String mode){
    var at=floor(h,kind,Direction.SOUTH);var c=actor(h,at,kind,false,true);c.goalSelector.removeAllGoals(g->true);
    if(!MOUNTED.contains(kind)){h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);c.moveTo(Vec3.atBottomCenterOf(at));}c.setOnGround(true);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1.5,c.getY(),c.getZ()+1.5);c.owner=p.getUUID();
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);});h.assertTrue(command(p,"chao testtoy "+kind.substring(5)+" loop")==1,"actual loop starts");
    h.runAtTickTime(55,()->{int count=heard(c).size();var real=new ItemStack(Items.APPLE,2);real.set(DataComponents.CUSTOM_NAME,Component.literal("Keep my real item"));
      switch(mode){
        case "stop"->h.assertTrue(command(p,"chao testtoy stop")==1,"actual global stop executes");
        case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"actual damage accepted");}
        case "pet"->h.assertTrue(c.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"actual care interaction accepted");
        case "item"->c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,real);
        case "move"->c.setPos(c.getX()+5,c.getY(),c.getZ());
        case "remove"->h.getLevel().destroyBlock(at,true);
        case "logout"->remove(p);
        case "range"->p.teleportTo(c.getX()+70,c.getY(),c.getZ());
        case "reload"->{var tag=new CompoundTag();String before=c.state().encode();c.saveWithoutId(tag);c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);h.assertTrue(copy.state().encode().equals(before)&&!copy.isNoAi()&&!copy.isNoGravity()&&copy.toyTarget()==null&&copy.toyPlayingTicks()==0,"native saved preview restores ordinary state and never resumes");copy.discard();}
      }
      Runnable check=()->{ToyPreview.tick(c);h.assertTrue(!ToyPreview.active(c)&&!c.isNoAi()&&!c.isNoGravity()&&c.toyTarget()==null&&c.toyPlayingTicks()==0,"interrupt restores temporary state and source ownership");if(mode.equals("item"))h.assertTrue(ItemStack.matches(real,c.getMainHandItem()),"real replacement item and its components survive cleanup");h.runAfterDelay(20,()->{h.assertTrue(heard(c).size()==count,"no later cue after cancellation");h.succeed();});};
      if(mode.equals("move")||mode.equals("remove"))h.runAfterDelay(1,check);else check.run();
    });
  }

  private static void roof(GameTestHelper h,String kind,boolean during){
    var at=floor(h,kind,Direction.NORTH);var c=actor(h,at,kind,true,false);boolean[] changed={false};long[] began={-1};
    if(!during)h.getLevel().setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);cleanup(h,()->{CUES.remove(c);c.discard();});
    h.onEachTick(()->{
      if(!during){h.assertTrue(!ToyContent.isPlaying(c),"real roof rejects the full mounting/hopping path");return;}
      if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);
      if(changed[0]||began[0]<0||h.getLevel().getGameTime()-began[0]<64)return;changed[0]=true;int count=heard(c).size();h.getLevel().setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);
      h.runAfterDelay(8,()->{h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&c.state().happiness==20&&heard(c).size()==count,"new obstruction cancels safely without reward or later cue");h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"safe exit does not put actor in roof");h.succeed();});
    });
    if(!during)h.runAfterDelay(420,()->{h.assertTrue(heard(c).isEmpty(),"blocked mount makes no toy sounds");h.succeed();});
  }

  private static void deferred(GameTestHelper h,String kind){
    var at=floor(h,kind,Direction.NORTH);var l=h.getLevel();var original=actor(h,at,kind,true,false);var live=new ChaoEntity[]{original};long[] began={-1};boolean[] changed={false};
    cleanup(h,()->{CUES.remove(original);CUES.remove(live[0]);original.discard();if(live[0]!=original)live[0].discard();});
    h.onEachTick(()->{
      if(ToyContent.isPlaying(original)&&began[0]<0)began[0]=start(original);
      if(changed[0]||began[0]<0||l.getGameTime()-began[0]<64)return;changed[0]=true;
      for(var p:BlockPos.betweenClosed(at.offset(-5,-4,-5),at.offset(5,-1,5)))l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
      l.destroyBlock(at,true);
      h.runAfterDelay(2,()->{
        h.assertTrue(original.getPersistentData().getBoolean("ChaoToyDismountWait")&&original.isNoGravity()&&original.isNoAi()&&original.toyPlayingTicks()==0,"unsupported exit defers in place rather than teleporting into air");
        String state=original.state().encode();var tag=new CompoundTag();original.saveWithoutId(tag);original.discard();var copy=ChaoGarden.CHAO.get().create(l);copy.load(tag);l.addFreshEntity(copy);copy.goalSelector.removeAllGoals(g->true);copy.targetSelector.removeAllGoals(g->true);live[0]=copy;
        h.assertTrue(copy.state().encode().equals(state)&&copy.getPersistentData().getBoolean("ChaoToyDismountWait"),"deferred recovery and unrelated state survive real NBT reload");
        h.runAfterDelay(20,()->{
          h.assertTrue(copy.getPersistentData().getBoolean("ChaoToyDismountWait")&&copy.isNoGravity(),"no unsafe recovery without support");l.setBlock(at.east(2).below(),Blocks.STONE.defaultBlockState(),3);
          h.runAfterDelay(30,()->{h.assertTrue(!copy.getPersistentData().getBoolean("ChaoToyDismountWait")&&!copy.isNoGravity()&&!copy.isNoAi()&&l.noCollision(copy,copy.getBoundingBox()),"new clear support permits ordinary gravity and AI recovery");h.assertTrue(copy.state().happiness==20&&copy.toyPlayingTicks()==0&&copy.toyTarget()==null,"rescue never finishes or rewards the removed toy");h.succeed();});
        });
      });
    });
  }

  private static void rescue(GameTestHelper h,String kind){
    var at=floor(h,kind,Direction.NORTH);var l=h.getLevel();var c=actor(h,at,kind,true,false);long[] began={-1};boolean[] changed={false};
    cleanup(h,()->{CUES.remove(c);c.discard();});h.onEachTick(()->{
      if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);
      if(changed[0]||began[0]<0||l.getGameTime()-began[0]<64)return;changed[0]=true;int count=heard(c).size();var destination=Vec3.atBottomCenterOf(at.east(4));c.moveTo(destination);c.setOnGround(true);
      h.runAfterDelay(4,()->{h.assertTrue(!ToyContent.isPlaying(c)&&!c.isNoGravity()&&!c.isNoAi()&&c.distanceToSqr(destination)<.05,"external supported-ground rescue cancels ride without snapping back to source");h.assertTrue(c.state().happiness==20&&heard(c).size()==count,"rescue does not reward or continue sound");h.succeed();});
    });
  }

  private static void sealedSwitch(GameTestHelper h,String kind,boolean flags){
    var at=floor(h,kind,Direction.NORTH);var l=h.getLevel();var c=actor(h,at,kind,flags,false);c.goalSelector.removeAllGoals(g->true);c.setOnGround(true);c.setNoAi(flags);c.setNoGravity(flags);c.getPersistentData().putLong("ChaoToyNextPlay",123456789);
    var landingBlocker=new net.minecraft.world.entity.vehicle.Boat[1];
    var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1.5,c.getY(),c.getZ()+1.5);cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);if(landingBlocker[0]!=null)landingBlocker[0].discard();});
    h.assertTrue(command(p,"chao testtoy "+kind.substring(5)+" loop")==1,"actual mounted loop starts with existing AI/gravity flags");
    h.runAtTickTime(64,()->{
      int count=heard(c).size();for(var cell:BlockPos.betweenClosed(at.offset(-5,-4,-5),at.offset(5,-1,5)))l.setBlock(cell,Blocks.AIR.defaultBlockState(),3);
      l.setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);
      // A baby fits on the pogo's 3/16-block top below a one-block roof.
      // Occupy that legitimate fractional landing too; otherwise the fixture
      // is not sealed and accepting a replacement preview is correct.
      var blocker=new net.minecraft.world.entity.vehicle.Boat(l,at.getX()+.5,at.getY()+.19,at.getZ()+.5){
        @Override protected boolean canAddPassenger(net.minecraft.world.entity.Entity entity){return false;}
      };
      blocker.setNoGravity(true);blocker.noPhysics=true;l.addFreshEntity(blocker);landingBlocker[0]=blocker;
      var top=Vec3.atBottomCenterOf(at).add(0,l.getBlockState(at).getCollisionShape(l,at).max(Direction.Axis.Y)+.001,0);
      h.assertFalse(l.noCollision(c,c.getBoundingBox().move(top.subtract(c.position()))),"fixture also blocks the source toy's fractional top landing");
      h.assertTrue(command(p,"chao testtoy toy_car once")==0,"sealed mount cannot be replaced by a hand preview that overwrites recovery flags");
      h.assertTrue(!ToyPreview.active(c)&&c.getPersistentData().getBoolean("ChaoToyDismountWait")&&c.isNoAi()&&c.isNoGravity()&&c.toyPlayingTicks()==0,"failed switch retains safe deferred recovery ownership");
      h.assertFalse(c.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"rejected replacement leaves no new preview AI marker");
      h.assertTrue(c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"switch cannot alter saved cooldown");
      h.runAfterDelay(4,()->{
        h.assertFalse(c.isPassenger(),"landing blocker must not turn the suspended Chao into a boat passenger");
        var destination=Vec3.atBottomCenterOf(at.east(4));l.setBlock(at.east(4).below(),Blocks.STONE.defaultBlockState(),3);c.moveTo(destination);c.setOnGround(true);
        com.mojang.logging.LogUtils.getLogger().info("GARDEN049_RESCUE_START {} expectedFlags={} {}",kind,flags,recoveryDiag(c,destination));
        h.runAfterDelay(24,()->{h.assertTrue(!c.isPassenger()&&!c.getPersistentData().getBoolean("ChaoToyDismountWait")&&c.isNoAi()==flags&&c.isNoGravity()==flags&&c.distanceToSqr(destination)<.05,"supported rescue restores exact pre-preview AI and gravity flags; expected="+flags+" "+recoveryDiag(c,destination));h.assertTrue(c.state().happiness==20&&heard(c).size()==count,"rejected replacement and rescue grant no rewards or later sound");h.succeed();});
      });
    });
  }

  private static String recoveryDiag(ChaoEntity c,Vec3 destination){
    var tag=c.getPersistentData();return "tick="+c.tickCount+" passenger="+c.isPassenger()+" ai="+c.isNoAi()+" gravity="+c.isNoGravity()+" wait="+tag.getBoolean("ChaoToyDismountWait")+" pos="+c.position()+" destination="+destination+" distance="+c.distanceToSqr(destination)+" return="+BlockPos.of(tag.getLong("ChaoToyReturn"))+" savedGravity="+tag.getBoolean("ChaoToyGravity")+" savedAI="+tag.getBoolean("ChaoToyDismountNoAi")+" mounted="+tag.getBoolean("ChaoToyMounted")+" clear="+c.level().noCollision(c,c.getBoundingBox())+" support="+c.level().getBlockCollisions(c,c.getBoundingBox().move(0,-.02,0)).iterator().hasNext();
  }

  private static void flatCollision(GameTestHelper h){
    var at=RattleGameTests02520.floor(h);var l=h.getLevel();
    for(String kind:List.of("chao_accordion","chao_guitar","chao_triangle","chao_bubble_wand","chao_castanets"))for(Direction facing:Direction.Plane.HORIZONTAL){
      var state=ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing);l.setBlock(at,state,3);var shape=state.getCollisionShape(l,at);
      h.assertTrue(!shape.isEmpty()&&shape.min(Direction.Axis.Y)>=0&&shape.max(Direction.Axis.Y)<=.36,"flat floor toy has low grounded collision in "+kind+"/"+facing+": "+shape.bounds());
      var c=actor(h,at,kind,false,false);c.setNoAi(true);c.moveTo(at.getX()+.5,at.getY()+.37,at.getZ()+.5);h.assertTrue(l.noCollision(c,c.getBoundingBox()),"actor above low floor toy is not blocked by the old upright outline");CUES.remove(c);c.discard();
    }
    h.succeed();
  }
}
