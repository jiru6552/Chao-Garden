package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.CompanionFlight;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native keyboard/mixin regression without optional mods. Synthetic permits test prediction only. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class CompanionInputVisualSmoke046 {
  private static final String[] CASES={"armor_without_permit","ordinary_chest_elytra","disabled_permit",
      "expired_finite_permit","finite_prediction_server_rejects","unlimited_prediction_server_rejects"};
  private static boolean started,setup;
  private static int ticks,clientPhase=-1;
  private static volatile int phase=-1,elapsed;
  private static volatile boolean predicted;
  private static long began;
  private static boolean enabled(){return Boolean.getBoolean("chao_garden.companion046InputStudy");}
  private static void require(boolean result,String message){if(!result)throw new IllegalStateException("COMPANION046_INPUT "+message);}
  private static void next(MinecraftServer server,int index){
    phase=index;predicted=false;began=server.overworld().getGameTime();elapsed=0;
    var player=server.getPlayerList().getPlayers().getFirst();player.stopFallFlying();player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
    player.getAbilities().flying=false;player.getAbilities().mayfly=false;player.onUpdateAbilities();player.teleportTo(.5,120,.5);player.setOnGround(false);
    player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(index==1?Items.ELYTRA:Items.DIAMOND_CHESTPLATE));
    LogUtils.getLogger().info("COMPANION046_INPUT_CASE {} {}",index,CASES[index]);
  }
  @SubscribeEvent public static void server(ServerTickEvent.Post event){
    if(!enabled()||phase<0||phase>=CASES.length)return;var server=event.getServer();elapsed=(int)(server.overworld().getGameTime()-began);
    if(elapsed<75)return;var player=server.getPlayerList().getPlayers().getFirst();
    require(predicted==(phase==1||phase>=4),"wrong native client input result for "+CASES[phase]+" predicted="+predicted);
    require(player.isFallFlying()==(phase==1),"server accepted an unauthorized permit or broke native elytra: "+CASES[phase]);
    require(phase==1||player.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"armor was changed");
    if(phase+1<CASES.length)next(server,phase+1);else phase=CASES.length;
  }
  @SubscribeEvent public static void predictionPermit(ClientTickEvent.Pre event){
    if(!enabled()||phase<2||phase>=CASES.length||elapsed<24||elapsed>=29)return;
    var player=Minecraft.getInstance().player;if(player==null)return;
    // Supply the controlled prediction fixture immediately before the real
    // input edge. Login/teleport status packets may overwrite an earlier
    // fixture; the server still has no equipped Chao and must reject launch.
    // Inactive is the status produced by babies, exhaustion and unequip; a
    // stale wing bit and an expired finite budget must both remain denied.
    CompanionFlight.receive(player.getUUID(),phase!=2,phase==2||phase==5,false,phase==3?0:242,0);
    require(CompanionFlight.allowed(player)==(phase>=4),"wrong controlled prediction permit at input edge: "+CASES[phase]);
  }
  @SubscribeEvent public static void client(ClientTickEvent.Post event){
    if(!enabled())return;var mc=Minecraft.getInstance();ticks++;
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen){
      started=true;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.pauseOnLostFocus=false;
      mc.createWorldOpenFlows().createFreshLevel("CompanionInput046-"+System.currentTimeMillis(),new LevelSettings("CompanionInput046",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new net.minecraft.world.level.levelgen.WorldOptions(2546L,false,false),a->a.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(),mc.screen);
    }
    if(!setup&&mc.player!=null&&mc.getSingleplayerServer()!=null){setup=true;mc.setScreen(null);mc.getSingleplayerServer().execute(()->next(mc.getSingleplayerServer(),0));}
    if(mc.player!=null&&phase>=0&&phase<CASES.length){
      if(clientPhase!=phase){
        clientPhase=phase;CompanionFlight.clear();mc.options.keyJump.setDown(false);
      }
      if(mc.player.isFallFlying()&&elapsed>=25)predicted=true;
      mc.options.keyJump.setDown(elapsed>=25&&elapsed<28);
    }
    if(phase==CASES.length){CompanionFlight.clear();LogUtils.getLogger().info("COMPANION046_INPUT_COMPLETE: six real keyboard cases; ordinary elytra preserved; inactive/expired denied; synthetic predicted permits rejected by server");mc.stop();}
    if(ticks>7000)throw new IllegalStateException("Companion input client study timed out");
  }
}
