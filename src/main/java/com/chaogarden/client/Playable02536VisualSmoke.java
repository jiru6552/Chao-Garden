package com.chaogarden.client;
import com.chaogarden.*;
import com.chaogarden.fruit.*;
import com.chaogarden.hub.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class Playable02536VisualSmoke {
 private static boolean started,setup;private static int ticks,phase=-1,at;private static volatile boolean ready;private static net.minecraft.world.entity.decoration.ArmorStand camera;
 private static String mode(){return System.getProperty("chao.playable.visual","");}
 private static void stage(int i){var mc=Minecraft.getInstance();phase=i;at=ticks;ready=false;mc.setScreen(null);mc.setCameraEntity(mc.player);mc.options.hideGui=i>=2;
  mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();var p=s.getPlayerList().getPlayers().getFirst();
   var l=i>=4?s.getLevel(HubContent.KINDERGARTEN):s.overworld();
   p.teleportTo(l,i>=4?18.5:128.5,i>=4?87:80,i>=4?20:132.5,java.util.Set.of(),180,0);
   p.getInventory().selected=i==2?1:0;p.inventoryMenu.broadcastChanges();ready=true;
  });
 }
 private static void cam(double x,double y,double z,double tx,double ty,double tz){var mc=Minecraft.getInstance();if(camera==null||camera.level()!=mc.level)camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,x,y,z);
  double dx=tx-x,dy=ty-y,dz=tz-z;float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz)),pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
  camera.moveTo(x,y-camera.getEyeHeight(),z,yaw,pitch);camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();camera.yRotO=yaw;camera.xRotO=pitch;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;camera.setYBodyRot(yaw);camera.yBodyRotO=yaw;mc.setCameraEntity(camera);
 }
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){if(mode().isBlank())return;var mc=Minecraft.getInstance();ticks++;
  if(mode().equals("existing")&&ticks%60==0&&mc.screen!=null){LogUtils.getLogger().info("PLAYABLE_SCREEN {}",mc.screen.getClass().getName());for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.Button b){String text=b.getMessage().getString();LogUtils.getLogger().info("PLAYABLE_BUTTON {}",text);if(text.equals("Proceed")||text.equals("I know what I'm doing!")||text.equals("Load World")||text.equals("Continue")){b.onPress();break;}}}
  if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
  if(!started&&mc.screen instanceof TitleScreen){started=true;mc.options.renderDistance().set(5);mc.options.simulationDistance().set(5);mc.options.fov().set(60);mc.options.framerateLimit().set(30);mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.tutorialStep=net.minecraft.client.tutorial.TutorialSteps.NONE;
   if(mode().equals("existing"))mc.createWorldOpenFlows().openWorld("Existing02535",()->{});
   else mc.createWorldOpenFlows().createFreshLevel("Playable02536-"+System.currentTimeMillis(),new LevelSettings("Playable02536",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2536L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
  }
  if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var s=mc.getSingleplayerServer();var l=s.overworld();l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,s);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,s);
   for(var p:BlockPos.betweenClosed(118,79,118,140,87,140))l.setBlock(p,p.getY()==79?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   var root=new BlockPos(128,80,125);l.setBlock(root,FruitContent.saplingForFruit("garden_fruit").defaultBlockState(),3);GardenTreeData.get(l).planted(root,"garden_fruit");if(!FruitContent.growTree(l,root,"garden_fruit"))throw new IllegalStateException("tree growth");
   for(var pod:FruitContent.podPositions(l,FruitContent.crownPosition(root),"garden_fruit"))l.setBlock(pod,l.getBlockState(pod).setValue(HangingFruitBlock.AGE,3),3);
   var p=s.getPlayerList().getPlayers().getFirst();if(HubContent.hasReturnJournal(p))HubContent.returnHome(p);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(ChaoGarden.item("chao_garden_music_disc")));p.getInventory().setItem(1,new ItemStack(ChaoGarden.item("chao_key")));p.getInventory().setItem(2,new ItemStack(Items.MUSIC_DISC_BLOCKS));p.getInventory().setItem(3,new ItemStack(ChaoGarden.item("garden_fruit"),4));
   var school=s.getLevel(HubContent.KINDERGARTEN);var sentinel=new BlockPos(30,80,40);var prior=school.getBlockState(sentinel);
   if(mode().equals("existing"))school.setBlock(sentinel,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
   if(!HubContent.ensure(school))throw new IllegalStateException("school ensure");
   if(mode().equals("existing")){if(!school.getBlockState(sentinel).is(Blocks.DIAMOND_BLOCK))throw new IllegalStateException("existing player garden block reset");school.setBlock(sentinel,prior,3);LogUtils.getLogger().info("PLAYABLE02536_EXISTING_PRESERVED player block retained through ensure; room not reset");}s.getLevel(HubContent.KINDERGARTEN).setDayTime(6000);mc.execute(()->stage(0));
  });}
  if(phase>=0&&ready&&mc.player!=null&&ticks-at>50){
   if(phase==0&&!(mc.screen instanceof InventoryScreen))mc.setScreen(new InventoryScreen(mc.player));
   if(phase==3)cam(134,84,134,128.5,83,125.5);
   if(phase==4)cam(18,89.1,20,18,89.1,14.5);
   if(phase==5)cam(23,90,19,18,89.2,14.5);
   if(phase==6)cam(18,89,12.1,18,89,14.5);
   if(phase==7)cam(-18,89,12.1,-18,89,14.5);
   if(phase==0)org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),496,493);
   if(ticks-at==90)Screenshot.grab(mc.gameDirectory,"02536-"+mode()+"-"+phase+".png",mc.getMainRenderTarget(),x->LogUtils.getLogger().info("PLAYABLE02536_CAPTURE {}",x.getString()));
   if(ticks-at>115){if(phase==7){LogUtils.getLogger().info("PLAYABLE02536_VISUAL_DONE {}",mode());mc.stop();phase=-2;}else stage(phase+1);}
  }
  if(ticks>16000)throw new IllegalStateException("visual timeout");
 }
}
