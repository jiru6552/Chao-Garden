package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Opt-in native analyzer regression and review frames; never packaged in the production JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Analyzer02518VisualSmoke {
  private static boolean started,setup;private static volatile boolean ready;private static volatile RuntimeException failure;
  private static volatile String fixture;private static volatile int entity;private static int ticks,restoredFrames;private static float[] rotation;
  private static final java.util.concurrent.atomic.AtomicInteger saves=new java.util.concurrent.atomic.AtomicInteger();
  private static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException("02518 "+msg);}
  private static void checkTailLabels(){
    var s=new ChaoState();
    check(ChaoInspectionScreen.partName(s,ChaoState.TAIL).equals("Chao"),"Ordinary tail mislabeled");
    s.ghostBody=true;
    check(ChaoInspectionScreen.partName(s,ChaoState.TAIL).equals("Hidden"),"Ghost tail mislabeled");
    s.ghostBody=false;s.immortal=true;
    for(float alignment:new float[]{-1f,-.5f,-.499f,0f,.499f,.5f,1f}){
      s.align=alignment;
      String expected=alignment<=-.5f?"Chao":"None";
      check(ChaoInspectionScreen.partName(s,ChaoState.TAIL).equals(expected),"Chaos tail mislabeled at alignment "+alignment);
    }
    s.character="omochao";
    check(ChaoInspectionScreen.partName(s,ChaoState.TAIL).equals("Mechanical"),"Robot tail mislabeled");
  }
  private static ChaoEntity chao(){return (ChaoEntity)Minecraft.getInstance().level.getEntity(entity);}
  private static float[] rotation(){var c=chao();return new float[]{c.yBodyRot,c.yBodyRotO,c.getYRot(),c.yRotO,c.yHeadRot,c.yHeadRotO,c.getXRot(),c.xRotO};}
  @SubscribeEvent public static void before(ScreenEvent.Render.Pre e){if(Boolean.getBoolean("chao_garden.analyzer02518")&&e.getScreen() instanceof ChaoInspectionScreen)rotation=rotation();}
  @SubscribeEvent public static void after(ScreenEvent.Render.Post e){if(Boolean.getBoolean("chao_garden.analyzer02518")&&e.getScreen() instanceof ChaoInspectionScreen){check(java.util.Arrays.equals(rotation,rotation()),"Preview changed the live entity rotation");restoredFrames++;}}
  private static void capture(String label){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"02518-"+label+".png",mc.getMainRenderTarget(),c->{saves.incrementAndGet();LogUtils.getLogger().info("CHAO02518_CAPTURE {}",label);});}
  private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}catch(RuntimeException e){failure=e;}});}
  private static void press(String text){var s=(AdventureScreen)Minecraft.getInstance().screen;for(var w:s.children())if(w instanceof AbstractButton b&&b.visible&&b.getMessage().getString().equals(text)){check(b.active,"Inactive "+text);s.mouseClicked(s.screenX(b.getX()+b.getWidth()/2f),s.screenY(b.getY()+b.getHeight()/2f),0);return;}throw new IllegalStateException("02518 missing button "+text);}
  private static void hover(int x,int y){var mc=Minecraft.getInstance();var s=(AdventureScreen)mc.screen;
    try{var move=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);move.setAccessible(true);move.invoke(mc.mouseHandler,mc.getWindow().getWindow(),s.screenX(x)*(double)mc.getWindow().getScreenWidth()/s.width,s.screenY(y)*(double)mc.getWindow().getScreenHeight()/s.height);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!Boolean.getBoolean("chao_garden.analyzer02518"))return;var mc=Minecraft.getInstance();
    if(!started&&mc.screen instanceof TitleScreen){checkTailLabels();started=true;mc.getWindow().setWindowed(1280,720);mc.options.renderDistance().set(6);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.pauseOnLostFocus=false;
      mc.createWorldOpenFlows().createFreshLevel("ChaoAnalyzer02518-"+System.currentTimeMillis(),new net.minecraft.world.level.LevelSettings("Chao analyzer 02518",GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2518L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);return;
    }
    if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;if(failure!=null)throw failure;
    if(!setup){setup=true;mc.setScreen(null);server(p->{var l=p.serverLevel();l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.server);
      for(var at:BlockPos.betweenClosed(0,79,0,15,85,15))l.setBlock(at,at.getY()==79?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      p.connection.teleport(8.5,80,10.5,180,10);p.setNoGravity(true);p.getInventory().clearContent();p.getInventory().setItem(6,new ItemStack(ChaoGarden.item("chao_analyzer")));
      var c=ChaoGarden.CHAO.get().create(l);c.owner=p.getUUID();var s=new ChaoState();s.hunger=95;s.energy=100;s.happiness=90;s.reincarnations=2;s.ageTicks=1200;
      for(int i=0;i<8;i++){for(int a=0;a<21;a++)if((ChaoState.animalSlots(a)&(1<<i))!=0){s.animalParts[i]=a;break;}}
      s.animalMask=0x155555;s.animalPartsMask=1;s.expressedColor=0;s.colorGenes=new int[]{0,10};s.toneGenes=new int[]{0,1};s.shinyGenes=new int[]{0,1};s.jewelGenes=new int[]{0,1};s.favoriteFruit=2;s.favoriteFruitGenes=new int[]{0,2};
      s.swimFly=.35f;s.runPower=-.7f;s.intelligenceGenes=new int[]{80,192};s.luckGenes=new int[]{24,205};
      for(int i=0;i<5;i++){s.grades[i]=i;s.gradeA[i]=i;s.gradeB[i]=5;s.levels[i]=12+i*8;s.points[i]=400+i*350;s.progress[i]=20+i*15;}
      s.setTraitInt("learned.song",3);s.setTraitInt("learned.bell",1);s.setTraitInt("learned.drawing",2);s.setTraitInt("lesson.reward.bell",1);
      fixture=s.encode();c.setState(s);c.setCustomName(Component.literal("Mochi"));c.setNoAi(true);c.moveTo(8.5,80,8.5,0,0);l.addFreshEntity(c);entity=c.getId();ready=true;
    });return;}
    if(!ready||chao()==null)return;ticks++;
    if(ticks==30){mc.setScreen(new ChaoInspectionScreen(chao()));hover(103,155);}
    if(ticks==55){capture("stats");hover(40,125);}
    if(ticks==75){capture("look-upper-left");hover(168,125);}
    if(ticks==95){capture("look-upper-right");hover(40,215);}
    if(ticks==115){capture("look-lower-left");hover(168,215);}
    if(ticks==135){capture("look-lower-right");hover(103,155);server(p->{var c=(ChaoEntity)p.serverLevel().getEntity(entity);c.state().grades[4]=5;c.sync();});}
    if(ticks==160){capture("stats-s");press("GENES");}
    if(ticks==180){capture("genes-appearance");press("SKILL GENES");}
    if(ticks==200){capture("genes-skills");press("GROWTH");}
    if(ticks==220){capture("genes-growth");press("PARTS");}
    if(ticks==240){capture("parts");press("ANIMAL DNA");}
    if(ticks==260){capture("dna-all");press("MISSING ONLY");}
    if(ticks==280){capture("dna-missing");press("BODY PARTS");}
    if(ticks==300){press("CARE");}
    if(ticks==310){press("LESSONS");}
    if(ticks==335){capture("care-lessons");check(mc.screen.children().stream().noneMatch(w->w instanceof AbstractButton b&&b.getMessage().getString().contains("JOIN")),"Old join button survives");press("CLAIM INSTRUMENTS");}
    if(ticks==355){check(chao().state().traitInt("lesson.reward.bell",0)==2,"Legacy reward cannot be claimed");capture("care-after-claim");press("PARTS");server(p->{var c=(ChaoEntity)p.serverLevel().getEntity(entity);var s=ChaoState.decode(fixture);s.adult=true;c.setState(s);});}
    if(ticks==375){capture("parts-adult");server(p->{var c=(ChaoEntity)p.serverLevel().getEntity(entity);var s=ChaoState.decode(fixture);s.adult=true;s.immortal=true;c.setState(s);});}
    if(ticks==395){capture("parts-chaos");check(mc.screen.children().stream().noneMatch(w->w instanceof AbstractButton b&&b.visible&&b.getMessage().getString().equals("ANIMAL DNA")),"Completed Chaos form still offers unmet DNA checklist");server(p->{var c=(ChaoEntity)p.serverLevel().getEntity(entity);var s=ChaoState.decode(fixture);com.chaogarden.core.OmochaoRules.initialize(s,ChaoState.decode(fixture),"Builder",3);c.setState(s);});}
    if(ticks==415){capture("parts-robot");check(ChaoInspectionScreen.partName(chao().state(),ChaoState.WINGS).equals("Mechanical"),"Robot body mislabeled");server(p->{var c=(ChaoEntity)p.serverLevel().getEntity(entity);var s=ChaoState.decode(fixture);s.giveAnimal(20);c.setState(s);});}
    if(ticks==435){capture("parts-ghost");check(ChaoInspectionScreen.partName(chao().state(),ChaoState.FEET).equals("Hidden"),"Ghost feet mislabeled");}
    if(ticks==455){mc.setScreen(null);mc.player.getInventory().selected=6;mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(6));}
    if(ticks==490){check(mc.player.getMainHandItem().is(ChaoGarden.item("chao_analyzer")),"Wrong held item");capture("tablet-held");mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
    if(ticks==515)capture("tablet-inventory");
    if(ticks==540){check(saves.get()==20,"Incomplete captures "+saves.get());check(restoredFrames>50,"Too few render-state checks");LogUtils.getLogger().info("CHAO02518_VISUAL_PASS screenshots={} restoredFrames={} nativeButtons=true rewardClaim=true specialBodies=true",saves.get(),restoredFrames);mc.stop();}
  }
}
