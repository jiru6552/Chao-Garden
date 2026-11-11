package com.chaogarden.client;

import com.chaogarden.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.*;
import com.chaogarden.hub.HubContent;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.test.InstrumentSchoolFixture02525;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

/** Opt-in actual-client evidence. Excluded from the installable JAR. */
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class Refine02537VisualSmoke {
  private static final String[] ITEMS={"chao_key","garden_guide","chao_garden_music_disc","chao_teleporter","chao_fruit_log","dark_chao_fruit_log"};
  private static final String[] NAMES={"inventory","key-hand","journal-hand","disc-hand","teleporter-hand","log-hand","dark-log-hand","trees","lessons-none","lessons-current","lessons-paused","lessons-complete","doctor","bubble-baby","bubble-adult","bubble-hurt","bubble-rest","bubble-pet","bubble-eat","cymbals","drum"};
  private static final BlockPos ROOT=new BlockPos(128,80,125);
  private static boolean started,setup,command,acted;private static int ticks,phase=-1,at,happyFrames,hurtFrames,priorityFrames;private static volatile boolean ready;private static volatile int subjectId;private static ChaoEntity subject;
  private static net.minecraft.world.entity.decoration.ArmorStand camera;
  private static final Map<String,List<Float>> volumes=new java.util.concurrent.ConcurrentHashMap<>();
  private static final Map<Integer,Integer> priorities=new HashMap<>();
  private static String mode(){return System.getProperty("chao.refine.visual","");}
  private static void check(boolean value,String message){if(!value)throw new IllegalStateException("REFINE02537 "+message);}
  private static void file(String name,String value){try{var p=Path.of("/tmp/chao-refine/evidence");Files.createDirectories(p);Files.writeString(p.resolve(name),value);}catch(Exception e){throw new RuntimeException(e);}}
  private static ChaoEntity actor(net.minecraft.server.level.ServerPlayer p,boolean adult){
    var c=ChaoGarden.CHAO.get().create(p.serverLevel());var s=new ChaoState();s.adult=adult;s.happiness=80;s.hunger=80;s.energy=100;c.setState(s);c.owner=p.getUUID();c.setPersistenceRequired();c.moveTo(128.5,80,132,0,0);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.setCustomName(net.minecraft.network.chat.Component.literal("Refine subject"));p.serverLevel().addFreshEntity(c);subjectId=c.getId();return c;
  }
  private static void stage(int i){
    var mc=Minecraft.getInstance();phase=i;at=ticks;ready=false;command=false;acted=false;mc.setScreen(null);mc.setCameraEntity(mc.player);mc.options.hideGui=i==7||i>=13;
    if(i<8)mc.player.getInventory().selected=i==0?0:i-1;
    mc.getSingleplayerServer().execute(()->{
      var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().getFirst();
      if(i<8){p.connection.teleport(128.5,80,133.5,180,0);p.getInventory().selected=i==0?0:i-1;p.inventoryMenu.broadcastChanges();}
      if(i==8){subject=actor(p,false);p.connection.teleport(128.5,80,134.5,180,0);}
      if(i==9){subject.discard();var l=s.getLevel(HubContent.KINDERGARTEN);check(HubContent.ensure(l),"kindergarten ensured");InstrumentSchoolFixture02525.visit(p);subject=InstrumentSchoolFixture02525.enroll(p,8,"maracas",false,12000);subjectId=subject.getId();}
      if(i==10){subject.state().hunger=10;ChaoCareServices.tickLearning(subject);subject.sync();check(subject.state().traitInt("lesson.paused",0)==1,"real lesson paused");}
      if(i==11){subject.state().hunger=80;subject.state().setTraitInt("lesson.remaining",1);ChaoCareServices.tickLearning(subject);subject.sync();check(!ChaoCareServices.hasLesson(subject),"lesson completed");}
      if(i==13){check(KindergartenContent.retrieve(p,subject.getUUID()),"student retrieved");p.teleportTo(s.overworld(),128.5,80,134.5,Set.of(),180,0);}
      if(i>=13){if(subject!=null&&subject.isAlive()){ToyPreview.stop(subject);subject.discard();}p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();subject=actor(p,i==14||i>=19);p.connection.teleport(128.5,80,134.5,180,0);}
      ready=true;
    });
  }
  private static void cam(double x,double y,double z,double tx,double ty,double tz){var mc=Minecraft.getInstance();if(camera==null||camera.level()!=mc.level)camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,x,y,z);
    double dx=tx-x,dy=ty-y,dz=tz-z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
    camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;mc.setCameraEntity(camera);
  }
  private static ChaoEntity clientSubject(){var mc=Minecraft.getInstance();return mc.level!=null&&mc.level.getEntity(subjectId) instanceof ChaoEntity c?c:null;}
  private static void capture(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"02537-"+name+".png",mc.getMainRenderTarget(),x->LogUtils.getLogger().info("REFINE02537_CAPTURE {}",x.getString()));}
  @SubscribeEvent public static void resolved(PlaySoundSourceEvent e){
    if(mode().isBlank())return;String id=e.getSound().getLocation().toString();float expected=id.equals("chao_garden:toy.cymbals")?.36f:id.equals("minecraft:block.note_block.basedrum")?.28f:0;
    if(expected==0)return;float v=e.getSound().getVolume();check(Math.abs(v-expected)<.0001,"resolved sound gain "+id+" = "+v);volumes.computeIfAbsent(id,k->new ArrayList<>()).add(v);
  }
  @SubscribeEvent public static void before(RenderLivingEvent.Pre<?,?> e){
    if(mode().isBlank()||!(e.getEntity() instanceof ChaoEntity c)||c.getId()!=subjectId)return;
    // Deliberately retain a stale toy clock during priority reactions, as if packets arrived out of order.
    if(phase>=15&&phase<=18&&(c.hurtTime>0||c.isResting()||c.isBeingPetted()||c.isEating()))c.setToyPlaying(BubblePlayRules.KIND,100);
  }
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> e){
    if(mode().isBlank()||!(e.getEntity() instanceof ChaoEntity c)||c.getId()!=subjectId)return;
    var model=(ChaoModel)e.getRenderer().getModel();check(model.officialPosesFinite(),"finite actual rendered pose");
    if((phase==13||phase==14)&&ChaoBubbleAnimation.happy(c,0)){check(model.eyeExpression()==1,"bubble happy eyes");happyFrames++;}
    if(phase==15&&c.hurtTime>0){check(!ChaoBubbleAnimation.happy(c,0)&&model.officialEmotion()==1&&model.eyeExpression()==2,"hurt wins over stale bubble clock");if(hurtFrames++==2)capture("bubble-hurt-priority");}
    if(phase>=16&&phase<=18&&(c.isResting()||c.isBeingPetted()||c.isEating())){check(!ChaoBubbleAnimation.happy(c,0),"care reaction wins over stale bubble clock");priorityFrames++;priorities.merge(phase,1,Integer::sum);}
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post e){
    if(mode().isBlank())return;var mc=Minecraft.getInstance();ticks++;
    if(mode().equals("reload")&&started&&mc.level==null&&mc.screen!=null&&ticks%40==0){
      LogUtils.getLogger().info("REFINE02537_RELOAD_SCREEN {}",mc.screen.getClass().getName());
      for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button button){
        String label=button.getMessage().getString();LogUtils.getLogger().info("REFINE02537_RELOAD_BUTTON {}",label);
        if(Files.isRegularFile(Path.of(mc.gameDirectory.toString(),"saves","Refine02537","level.dat"))&&Set.of("Proceed","I know what I'm doing!","Load World","Continue").contains(label)){button.onPress();break;}
      }
    }
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){started=true;mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);mc.options.fov().set(60);mc.options.framerateLimit().set(30);mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
      if(mode().equals("reload"))mc.createWorldOpenFlows().openWorld("Refine02537",()->{});
      else mc.createWorldOpenFlows().createFreshLevel("Refine02537",new LevelSettings("Refine02537",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2537L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);mc.getSingleplayerServer().execute(()->{
      var s=mc.getSingleplayerServer();var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();
      if(mode().equals("reload")){
        l.getChunkAt(ROOT);check(l.getBlockState(ROOT).is(FruitContent.DARK_TRUNK.get()),"saved Dark log identity");var crown=ROOT.above(4);check(l.getBlockState(crown).getValue(ChaoTreeCrownBlock.BARE),"saved bare crown");
        for(var entry:FruitContent.darkWoodPlan(ROOT).entrySet())check(l.getBlockState(entry.getKey()).equals(entry.getValue()),"saved Dark branch state");
        for(var d:Direction.Plane.HORIZONTAL){var at=crown.below().relative(d);var st=l.getBlockState(at);check(st.is(FruitContent.podForFruit("dark_garden_fruit"))&&st.getValue(HangingFruitBlock.AGE)==d.ordinal()%4&&st.canSurvive(l,at),"saved fruit age and support");}
        check(GardenTreeData.get(l).count()>=2,"saved tree ledger");check(!GardenTreeData.get(l).mayWater(ROOT,l.getGameTime()),"saved watering cooldown");
        var at=crown.below().north();l.setBlock(at,l.getBlockState(at).setValue(HangingFruitBlock.AGE,3),3);check(FruitContent.podForFruit("dark_garden_fruit").harvest(l,at,l.getBlockState(at)),"harvest after real process reload");FruitContent.treeForFruit("dark_garden_fruit").performBonemeal(l,l.random,crown,l.getBlockState(crown));check(l.getBlockState(at).getValue(HangingFruitBlock.AGE)==1,"regrow after real process reload");file("reload-pass.txt","Actual integrated-server process stopped/reopened: Dark logs, branch axes, crown, fruit ages, support, tree ledger and watering cooldown persisted; harvest and regrowth passed.\n");mc.execute(mc::stop);return;
      }
      l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,s);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,s);l.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,s);
      for(var pos:BlockPos.betweenClosed(118,79,118,150,88,142))l.setBlock(pos,pos.getY()==79?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      for(boolean dark:new boolean[]{true,false}){var root=dark?ROOT:ROOT.east(10);String fruit=dark?"dark_garden_fruit":"garden_fruit";l.setBlock(root,FruitContent.saplingForFruit(fruit).defaultBlockState(),3);GardenTreeData.get(l).planted(root,fruit);check(FruitContent.growTree(l,root,fruit),"seed-grown visual tree");for(var pod:FruitContent.podPositions(l,root.above(4),fruit))l.setBlock(pod,l.getBlockState(pod).setValue(HangingFruitBlock.AGE,3),3);}
      p.getInventory().clearContent();for(int i=0;i<ITEMS.length;i++)p.getInventory().setItem(i,new ItemStack(ChaoGarden.item(ITEMS[i])));mc.execute(()->stage(0));
    });}
    if(!ready||phase<0||mc.player==null)return;int elapsed=ticks-at;
    if(phase==0&&elapsed>35&&!(mc.screen instanceof InventoryScreen))mc.setScreen(new InventoryScreen(mc.player));
    if(phase==7)cam(141,85,139,133,83,125);
    var c=clientSubject();
    if(phase>=8&&phase<=12&&c!=null&&elapsed>40&&!(mc.screen instanceof ChaoInspectionScreen)){var screen=new ChaoInspectionScreen(c,4);screen.showCare(phase==12?0:1);mc.setScreen(screen);}
    if(phase>=13&&c!=null){cam(129.3,c.getY()+(c.state().adult?1.15:.7),134.5,128.5,c.getY()+(c.state().adult?.8:.5),132);
      if(elapsed>20&&!command){command=true;mc.player.connection.sendCommand("chao testtoy "+(phase==19?"cymbals":phase==20?"drum":"bubble_wand")+" loop");}
      if(elapsed>50&&!acted&&phase>=15&&phase<=18){acted=true;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();ToyPreview.stop(subject);
        if(phase==15){subject.setInvulnerable(false);check(subject.hurt(subject.level().damageSources().generic(),1),"real hurt reaction");}
        if(phase==16){subject.state().energy=10;subject.state().sleeping=true;subject.sync();}
        if(phase==17)check(subject.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"real pet reaction");
        if(phase==18){subject.state().hunger=10;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("garden_fruit")));check(subject.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"real feeding reaction");}
      });}
    }
    if(elapsed==80)capture(NAMES[phase]);
    if(elapsed>115){
      if(phase<20)stage(phase+1);
      else{check(happyFrames>20&&hurtFrames>0&&priorityFrames>10,"observed expression frames");for(int k=16;k<=18;k++)check(priorities.getOrDefault(k,0)>0,"individual priority observed "+k);check(volumes.getOrDefault("chao_garden:toy.cymbals",List.of()).size()>0&&volumes.getOrDefault("minecraft:block.note_block.basedrum",List.of()).size()>0,"both sounds decoded and dispatched");file("visual-pass.txt","21 actual-client scenes: inventory and hand appearances, bare Dark/ordinary palm trees, real class/no-class/paused/completed/doctor screens; baby/adult bubble, stale-clock hurt/rest/pet/eating priority. happyFrames="+happyFrames+" hurtFrames="+hurtFrames+" priorityFrames="+priorityFrames+" sounds="+volumes+". Null audio: human listening still required.\n");phase=-2;mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();ToyPreview.stop(subject);var l=s.overworld();for(var d:Direction.Plane.HORIZONTAL){var pos=ROOT.above(3).relative(d);l.setBlock(pos,l.getBlockState(pos).setValue(HangingFruitBlock.AGE,d.ordinal()%4),3);}GardenTreeData.get(l).watered(ROOT,l.getGameTime());s.saveEverything(true,true,true);mc.execute(mc::stop);});}
    }
    if(ticks>14000)throw new IllegalStateException("visual timeout");
  }
}
