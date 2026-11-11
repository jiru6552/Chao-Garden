package com.chaogarden.client;

import com.chaogarden.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Actual chat-command packets and ordinary clients. Not packaged in the release. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Toy02535CommandsVisualSmoke {
  private static final Path DIR=Path.of("/tmp/chao-triangle/commands");
  private static final String[] TOYS={"triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle","triangle"};
  private static final String[] VARIANTS={"baby_neutral","adult_neutral_type0","animal_rabbit_baby","adult_hero_type2","animal_scorpion_baby","omochao_adult","animal_mole_baby","chaos_2","baby_dark","adult_dark_type4"};
  private static String mode(){return System.getProperty("chao_garden.toy02535Commands","");}
  private static boolean started,setup,ready,acted,disconnected,rejoined,ownerLeft;private static long reconnectAt;
  private static int ticks,frames,renders,phase=-1;private static long began=-1,spawned;private static ChaoEntity actor;private static String state;
  private static final Set<String> sent=new HashSet<>(),seen=new HashSet<>();private static final Map<String,Integer> sounds=new TreeMap<>();
  private static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TOY02535 "+text);}
  private static void file(String name,String text){try{Files.createDirectories(DIR);Files.writeString(DIR.resolve(name),text);}catch(Exception e){throw new RuntimeException(e);}}
  private static int current(){try{return Integer.parseInt(Files.readString(DIR.resolve("phase.txt")).trim());}catch(Exception e){return -1;}}
  private static void round(net.minecraft.server.MinecraftServer server,int next){
    if(actor!=null)actor.discard();var l=server.overworld();phase=next;began=-1;acted=false;spawned=l.getGameTime();
    actor=ChaoGarden.CHAO.get().create(l);var variant=VARIANTS[next%VARIANTS.length];
    actor.setState(com.chaogarden.test.ToyVariantCases02522.all().stream().filter(v->v.name().equals(variant)).findFirst().orElseThrow().state());
    actor.setInvulnerable(true);actor.moveTo(.5,80,.5,0,0);actor.setCustomName(Component.literal("Preview "+next));l.addFreshEntity(actor);
    actor.goalSelector.removeAllGoals(g->true);actor.targetSelector.removeAllGoals(g->true);
    actor.owner=server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals("ToyHost")).findFirst().orElseThrow().getUUID();
    file("phase.txt",Integer.toString(next));LogUtils.getLogger().info("TOY02535_CASE {} {} {}",next,TOYS[next],variant);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post e){
    if(!mode().equals("host")||!ready||phase>=16)return;var server=e.getServer();var l=server.overworld();
    for(var p:server.getPlayerList().getPlayers())if(!p.getPersistentData().getBoolean("Toy02535Camera")){
      p.getPersistentData().putBoolean("Toy02535Camera",true);server.getPlayerList().op(p.getGameProfile());p.setGameMode(GameType.SPECTATOR);p.connection.teleport(1.15,79.25,3.2,163,10);
    }
    if(phase<0){if(server.getPlayerList().getPlayerCount()>=2)round(server,0);return;}
    check(l.getGameTime()-spawned<900,"command phase timed out "+phase);
    if(ToyPreview.active(actor)&&began<0){began=l.getGameTime();state=actor.state().encode();LogUtils.getLogger().info("TOY02535_COMMAND_STARTED {} {}",phase,actor.toyPlayingKind());}
    if(began<0)return;long elapsed=l.getGameTime()-began;
    if(ToyPreview.active(actor)){check(actor.state().encode().equals(state),"preview preserves complete Chao state");check(actor.getHeldChaoItem().isEmpty()&&actor.isNoAi(),"temporary prop and AI pause");}
    if((phase==12||phase==13)&&elapsed>=56&&!acted){
      acted=true;
      if(phase==12){var keeper=server.getPlayerList().getPlayer(actor.owner);keeper.getInventory().clearContent();check(actor.mobInteract(keeper,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"pet interrupts preview");}
      else{actor.setInvulnerable(false);check(actor.hurt(l.damageSources().generic(),1),"damage interrupts preview");}
    }
    int end=phase<10?185:phase==10?360:phase==14?130:phase==15?100:90;
    if(elapsed>=end){
      check(!ToyPreview.active(actor)&&actor.toyPlayingTicks()==0&&!actor.isNoAi(),"preview fully stops "+phase);
      if(phase!=12&&phase!=13)check(actor.state().happiness==20,"no play rewards "+phase);
      LogUtils.getLogger().info("TOY02535_CASE_PASS {}",phase);
      if(phase<15)round(server,phase+1);else{phase=16;file("phase.txt","16");file("server-pass.txt","16 real-command cases; triangle both ages and replacements; loop, stop, switch, pet, damage, observer rejoin and preview-owner disconnect; no rewards\n");}
    }
  }
  private static final java.util.Set<Float> heardPitches=java.util.concurrent.ConcurrentHashMap.newKeySet();
  @SubscribeEvent public static void resolvedSound(net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent e){
    if(mode().isEmpty()||!e.getSound().getLocation().toString().equals("minecraft:block.note_block.bell"))return;
    float pitch=e.getSound().getPitch();check(pitch>.5f&&pitch<1.95f,"resolved triangle pitch retains clamp headroom");
    check(Math.abs(pitch-1f)<.00001f,"resolved triangle pitch");heardPitches.add(pitch);
    LogUtils.getLogger().info("TOY02534_RESOLVED_PITCH {}",pitch);
  }
  @SubscribeEvent public static void sound(PlaySoundEvent e){
    if(mode().isEmpty()||e.getSound()==null)return;String name=e.getSound().getLocation().toString();
    if(name.equals("minecraft:block.note_block.bell")||name.equals("minecraft:block.note_block.flute")||name.startsWith("chao_garden:toy.")||name.equals("minecraft:block.bubble_column.bubble_pop"))sounds.merge(name,1,Integer::sum);
  }
  @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> e){
    if(mode().isEmpty()||!(e.getEntity() instanceof ChaoEntity c)||!c.hasCustomName()||!c.getName().getString().startsWith("Preview ")||c.toyPlayingTicks()<=0)return;
    check(e.getRenderer().getModel() instanceof ChaoModel,"current renderer retained");var model=(ChaoModel)e.getRenderer().getModel();check(model.officialPosesFinite(),"finite pose");
    boolean active=switch(c.toyPlayingKind()){
      case "preview_sit"->true;
      case "chao_castanets"->ChaoCastanetsAnimation.active(c);
      case "chao_triangle"->ChaoTriangleAnimation.active(c);
      case "chao_rattle"->ChaoRattleAnimation.active(c);
      case "chao_bubble_wand"->ChaoBubbleAnimation.active(c);
      case "chao_maracas"->ChaoMaracasAnimation.active(c);
      case "chao_bell"->ChaoBellAnimation.active(c);
      case "chao_flute"->ChaoFluteAnimation.active(c);
      default->false;
    };
    if(c.hurtTime==0&&!c.isBeingPetted())check(active,"production animation active "+c.toyPlayingKind());renders++;
  }
  private static void send(String key,String command){if(sent.add(key)){Minecraft.getInstance().player.connection.sendCommand(command);LogUtils.getLogger().info("TOY02535_SENT {} {}",mode(),command);}}
  private static void disconnect(Minecraft mc,String label){started=false;reconnectAt=System.currentTimeMillis()+3000;LogUtils.getLogger().info("TOY02535_DISCONNECT {}",label);mc.level.disconnect();mc.disconnect(new TitleScreen());}
  @SubscribeEvent public static void client(ClientTickEvent.Post e){
    if(mode().isEmpty())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.fov().set(42);mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
      if(mode().equals("host")){
        started=true;mc.createWorldOpenFlows().createFreshLevel("Toy02535-"+System.currentTimeMillis(),new LevelSettings("Toy02535",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2527L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
      }else if(Files.exists(DIR.resolve("host-ready.txt"))&&System.currentTimeMillis()>=reconnectAt){started=true;ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25581"),new ServerData("Preview study","127.0.0.1:25581",ServerData.Type.LAN),false,null);}
    }
    if(mode().equals("host")&&!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){
      setup=true;mc.setScreen(null);ChaoRenderer.suppressNames=true;mc.getSingleplayerServer().execute(()->{
        var server=mc.getSingleplayerServer();var l=server.overworld();l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        for(var at:BlockPos.betweenClosed(-10,79,-10,10,88,10))l.setBlock(at,at.getY()==79?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        check(server.publishServer(GameType.SPECTATOR,false,25581),"LAN publishes");ready=true;file("host-ready.txt","25581");
      });
    }
    int p=current();
    if(mc.player!=null&&mc.level!=null&&p>=0&&p<16){
      for(var entity:mc.level.entitiesForRendering())if(entity instanceof ChaoEntity c&&c.hasCustomName()&&c.getName().getString().equals("Preview "+p)){
        var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,c.getYRot());var at=c.position().add(forward.scale(2.6)).add(-forward.z*.65,0,forward.x*.65);
        float yaw=(float)(Math.toDegrees(Math.atan2(c.getZ()-at.z,c.getX()-at.x))-90);float pitch=(float)Math.toDegrees(Math.atan2(.4,Math.hypot(c.getX()-at.x,c.getZ()-at.z)));
        mc.player.moveTo(at.x,c.getY()+.85-mc.player.getEyeHeight(),at.z,yaw,pitch);mc.player.yRotO=yaw;mc.player.xRotO=pitch;
        boolean controller=p==15?mode().equals("observer"):mode().equals("host");
        if(controller&&c.tickCount>=12)send(p+"-start","chao "+(TOYS[p].equals("sit")?"testpose ":"testtoy ")+TOYS[p]+(p<10?" once":" loop"));
        if(c.toyPlayingTicks()>0){
          seen.add(p+":"+c.toyPlayingKind());int elapsed=160-c.toyPlayingTicks();
          if(mode().equals("observer")&&p==10&&!disconnected&&elapsed>=45){disconnected=true;disconnect(mc,"observer-during-loop");return;}
          if(mode().equals("observer")&&p==10&&disconnected&&!rejoined){rejoined=true;file("rejoin-pass.txt","Observer rejoined real command loop at phase "+elapsed+"\n");}
          if(controller&&p==11&&elapsed>=56)send(p+"-stop","chao testtoy triangle stop");
          if(controller&&p==14&&c.toyPlayingKind().equals("chao_triangle")&&elapsed>=55)send(p+"-switch","chao testtoy rattle once");
          if(controller&&p==14&&c.toyPlayingKind().equals("chao_rattle")&&elapsed>=55)send(p+"-stop","chao testtoy stop");
          if(controller&&p==15&&!ownerLeft&&elapsed>=55){ownerLeft=true;disconnect(mc,"preview-owner");return;}
        }
        if(controller&&p==10&&c.tickCount>=350)send(p+"-stop","chao testtoy stop");
        if(ticks%(mode().equals("host")?4:12)==0){String name=String.format("02535-%s-%02d-%04d-t%03d.png",mode(),p,frames++,c.tickCount);Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),x->{});}
        break;
      }
    }
    if(p==16&&mc.player!=null){
      check(heardPitches.size()==1,"triangle bell pitch reached an audio source");file("resolved-pitches-"+mode()+".txt",new java.util.TreeSet<>(heardPitches)+"; Minecraft pitch range 0.5–2.0; bell event at original pitch; listening outstanding\n");
      check(seen.stream().map(x->x.substring(x.indexOf(':')+1)).distinct().count()==2,"triangle and switch-source animations seen");check(sounds.size()==2&&renders>200,"both sound events received and poses rendered");
      if(mode().equals("observer")){
        check(disconnected&&rejoined&&ownerLeft,"observer rejoin and owner disconnect exercised");file("observer-pass.txt","frames="+frames+" renders="+renders+" seen="+seen+" sounds="+sounds+"; null audio, listening outstanding\n");LogUtils.getLogger().info("TOY02535_OBSERVER_PASS {}",sounds);mc.stop();
      }else if(Files.exists(DIR.resolve("observer-pass.txt"))){file("host-pass.txt","frames="+frames+" renders="+renders+" seen="+seen+" sounds="+sounds+"; real chat command packets\n");LogUtils.getLogger().info("TOY02535_HOST_PASS {}",sounds);mc.stop();}
    }
    check(ticks<18000,"client timeout");
  }
}
