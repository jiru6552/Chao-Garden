package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.*;
import com.chaogarden.client.activities.GardenScreen;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in native UI + inventory-packet regression; excluded from the production JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Refinement02517VisualSmoke {
  private static boolean started,setup;private static volatile boolean ready;private static volatile RuntimeException failure;
  private static int ticks;private static final BlockPos DEVICE=new BlockPos(8,80,8);
  private static final java.util.concurrent.atomic.AtomicInteger saves=new java.util.concurrent.atomic.AtomicInteger();
  private static void capture(String label){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"02517-"+label+".png",mc.getMainRenderTarget(),c->{saves.incrementAndGet();LogUtils.getLogger().info("CHAO02517_CAPTURE {}",label);});}
  private static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException("02517 "+msg);}
  private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work){var mc=Minecraft.getInstance();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{try{work.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id));}catch(RuntimeException e){failure=e;}});}
  private static void press(String text){var s=Minecraft.getInstance().screen;for(var w:s.children())if(w instanceof AbstractButton b&&b.getMessage().getString().equals(text)){b.onPress();return;}throw new IllegalStateException("02517 missing button "+text);}
  private static void drag(int inventory,int slot){
    var mc=Minecraft.getInstance();check(mc.screen instanceof GardenScreen,"Garden screen absent during drag");
    float scale=Math.min(1f,Math.min((mc.screen.width-12f)/320,(mc.screen.height-12f)/302));int x=Math.round((mc.screen.width-320*scale)/2),y=Math.round((mc.screen.height-302*scale)/2);
    mc.screen.mouseClicked(x+(88+inventory*18)*scale,y+250*scale,0);mc.screen.mouseReleased(x+(29+slot*50)*scale,y+90*scale,0);
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(!Boolean.getBoolean("chao_garden.refinement02517"))return;var mc=Minecraft.getInstance();
    if(!started&&mc.screen instanceof TitleScreen){started=true;mc.options.renderDistance().set(6);mc.options.simulationDistance().set(5);mc.options.guiScale().set(2);mc.options.pauseOnLostFocus=false;
      mc.createWorldOpenFlows().createFreshLevel("ChaoVisual02517-"+System.currentTimeMillis(),new net.minecraft.world.level.LevelSettings("Chao visual 02517",GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2517L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);return;
    }
    if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
    if(failure!=null)throw failure;
    if(!setup){setup=true;mc.setScreen(null);server(p->{
      var l=p.serverLevel();
      check(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("chao_garden:garden_bell"))==ChaoGarden.item("garden_block"),"Removed Bell alias");
      check(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("chao_garden:robot_component"))==ChaoGarden.item("omochao_parts_box"),"Removed robot component alias");
      l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.server);
      for(var at:BlockPos.betweenClosed(0,79,0,26,92,26))l.setBlock(at,at.getY()==79?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      for(int x=2;x<=23;x++)for(int z=2;z<=23;z++)if(x==2||x==23||z==2||z==23)l.setBlock(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState(),3);
      l.setBlock(DEVICE,ActivityContent.GARDEN.get().defaultBlockState(),3);check(com.chaogarden.fruit.FruitContent.growTree(l,new BlockPos(16,80,16),"garden_fruit"),"UI fixture tree");
      p.connection.teleport(10.5,80,11.5,145,15);p.setNoGravity(true);p.getInventory().clearContent();
      for(int i=0;i<6;i++){var c=ChaoGarden.CHAO.get().create(l);c.owner=p.getUUID();var s=new com.chaogarden.core.ChaoState();s.expressedColor=i;c.setState(s);c.setCustomName(Component.literal(new String[]{"Clover","Pip","Mochi","Pebble","Nova","Bubbles"}[i]));var stack=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(stack,c);stack.set(DataComponents.CUSTOM_NAME,c.getName());p.getInventory().setItem(i,stack);}
      ready=true;
    });return;}
    if(!ready)return;ticks++;
    if(ticks==30){server(p->ActivityManager.open(p,DEVICE));}
    if(ticks==60){check(mc.screen instanceof GardenScreen,"Inactive garden screen");capture("garden-inactive");press("Activate garden");}
    if(ticks==90){capture("garden-active");drag(0,0);}
    if(ticks==110)drag(1,1);if(ticks==130)drag(2,2);if(ticks==150)drag(3,3);if(ticks==170)drag(4,4);if(ticks==190)drag(5,5);
    if(ticks==220){capture("garden-six-residents");server(p->{var d=GardenRegistry.get(p.server);var place=GardenRegistry.Place.at(p.serverLevel(),DEVICE);check(d.areas.containsKey(place)&&d.entries.values().stream().filter(e->e.garden.equals(place)).count()==6,"Native drag did not register six");for(int i=0;i<6;i++)check(p.getInventory().getItem(i).getCount()==1&&ChaoCarrierItem.filled(p.getInventory().getItem(i)),"Native drag consumed item");LogUtils.getLogger().info("CHAO02517_NATIVE_DRAG slots=6 itemsPreserved=6 serverPackets=true");});}
    if(ticks==240){float sc=Math.min(1f,Math.min((mc.screen.width-12f)/320,(mc.screen.height-12f)/302));mc.screen.mouseClicked((mc.screen.width-320*sc)/2+129*sc,(mc.screen.height-302*sc)/2+90*sc,0);}
    if(ticks==260){capture("garden-unregistered");server(p->{var d=GardenRegistry.get(p.server);check(!d.entries.containsKey(ChaoCarrierItem.storedUUID(p.getInventory().getItem(2))),"Icon click failed to unregister");});}
    if(ticks==280){mc.setScreen(new AdventureGuideScreen(ChaoGarden.item("garden_guide").getDefaultInstance().get(DataComponents.WRITTEN_BOOK_CONTENT)));}
    if(ticks==300){capture("journal-chapters");press("> Food & trees");}
    if(ticks==320){capture("journal-sections");for(var w:mc.screen.children())if(w instanceof EditBox box){box.setValue("Kindergarten");break;}}
    if(ticks==340){capture("journal-search-chapter");check(mc.screen.children().stream().anyMatch(w->w instanceof AbstractButton b&&b.getMessage().getString().equals("Classroom")),"Chapter search omitted its sections");for(var w:mc.screen.children())if(w instanceof EditBox box){box.setValue("Watering");break;}}
    if(ticks==360){capture("journal-search-section");press("Watering trees");}
    if(ticks==380){capture("journal-reading");mc.setScreen(null);Cwe02516VisualSmoke.select("cwe-native-0");Refinement02515VisualSmoke.select("refinement-native-0");Refinement02515VisualSmoke.select("");}
    if(ticks==400){Cwe02516VisualSmoke.select("");mc.player.getInventory().selected=6;mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(6));server(p->{p.getInventory().selected=6;p.getInventory().setItem(6,new ItemStack(ChaoGarden.item("chao_accordion")));p.connection.teleport(p.getX(),p.getY(),p.getZ(),0,10);});}
    if(ticks==440){check(mc.player.getMainHandItem().is(ChaoGarden.item("chao_accordion")),"Actual accordion hand slot");capture("accordion-held");}
    if(ticks==460)server(p->{p.getInventory().setItem(6,new ItemStack(ChaoGarden.item("chao_garden_music_disc")));});
    if(ticks==490){check(mc.player.getMainHandItem().is(ChaoGarden.item("chao_garden_music_disc")),"Actual disc hand slot");capture("disc-held");}
    if(ticks==570)server(p->{var at=new BlockPos(15,80,10);for(int i=0;i<49;i++)p.serverLevel().setBlock(at.offset(KarateMatBlock.offset(i)),ActivityContent.MAT.get().defaultBlockState().setValue(KarateMatBlock.TILE,i),3);p.connection.teleport(15.5,86,5,0,55);});
    if(ticks==610)capture("tatami-carpet");
    if(ticks==640){check(saves.get()==13,"Screenshot writes incomplete: "+saves.get());LogUtils.getLogger().info("CHAO02517_VISUAL_PASS screenshots={} nativeDrag=true unregister=true journalChapterSearch=true journalSectionSearch=true",saves.get());mc.stop();}
  }
}
