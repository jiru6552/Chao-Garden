package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.market.*;
import com.chaogarden.fruit.*;
import com.chaogarden.hub.*;
import com.chaogarden.toys.*;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Explicit opt-in dedicated process fixture, excluded from the release JAR. */
@EventBusSubscriber(modid="chao_garden")
public final class PlayableDedicatedSmoke02536 {
 static final Path DIR=Path.of("/tmp/chao-playable/evidence/dedicated");static int ticks,loadWait;static boolean done;static int naturalStart=-1;static boolean naturalInterrupted;static ChaoEntity baby,adult;static BlackMarketTrader trader;
 static final BlockPos ROOT=new BlockPos(140,80,128),PORTAL=new BlockPos(128,80,128),BOX=new BlockPos(130,80,131);
 static void check(boolean ok,String why){if(!ok)throw new IllegalStateException("PLAYABLE02536_DEDICATED "+why);LogUtils.getLogger().info("PLAYABLE02536_PASS {}",why);}
 static int cmd(ServerPlayer p,String c){try{return p.server.getCommands().getDispatcher().execute(c,p.createCommandSourceStack().withPermission(2));}catch(Exception e){throw new RuntimeException(e);}}
 static void buy(ServerPlayer p){trader.setTradingPlayer(p);var menu=new MerchantMenu(36,p.getInventory(),trader);var o=menu.getOffers();int i=java.util.stream.IntStream.range(0,o.size()).filter(n->o.get(n).getResult().is(ChaoGarden.item("chao_key"))).findFirst().orElseThrow();p.getInventory().setItem(9,new ItemStack(Items.EMERALD,4));menu.setSelectionHint(i);menu.tryMoveItems(i);menu.quickMoveStack(p,2);menu.removed(p);check(!o.get(i).isOutOfStock(),"wild key remains purchasable after real merchant transaction");}
 static ChaoEntity chao(ServerPlayer p,boolean grown,double x){var c=ChaoGarden.CHAO.get().create(p.server.overworld());var state=new ChaoState();state.adult=grown;state.hunger=20;state.energy=100;state.happiness=80;c.setState(state);c.owner=p.getUUID();c.setPersistenceRequired();c.moveTo(x,80,134,180,0);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.setCustomName(net.minecraft.network.chat.Component.literal(grown?"Smoke adult":"Smoke baby"));p.server.overworld().addFreshEntity(c);return c;}
 static void save(net.minecraft.server.MinecraftServer s,String name,String text){try{Files.createDirectories(DIR);Files.writeString(DIR.resolve(name),text);}catch(Exception ex){throw new RuntimeException(ex);}s.saveEverything(true,true,true);s.halt(false);done=true;}
 @SubscribeEvent public static void tick(ServerTickEvent.Post e){int pass=Integer.getInteger("chao.playable.server",0);if(pass==0||done)return;var server=e.getServer();if(server.getPlayerList().getPlayers().isEmpty())return;var p=server.getPlayerList().getPlayers().getFirst();var l=server.overworld();ticks++;
 try{
 if(ticks==1){check(server.isDedicatedServer()&&p.connection!=null,"actual dedicated server with connected client");
  if(pass==1){
   l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
   for(var at:BlockPos.betweenClosed(118,79,118,150,87,142))l.setBlock(at,at.getY()==79?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   p.setGameMode(GameType.SURVIVAL);p.connection.teleport(128.5,80,132.5,180,0);p.getInventory().clearContent();
   trader=MarketContent.TRADER.get().create(l);trader.beginVisit(l.getGameTime());trader.setNoAi(true);trader.setPersistenceRequired();trader.moveTo(125,80,132,0,0);l.addFreshEntity(trader);buy(p);buy(p);check(p.getInventory().countItem(ChaoGarden.item("chao_key"))==2&&p.getInventory().countItem(Items.EMERALD)==0,"two keys purchased for eight emeralds in survival");
   var table=new BlockPos(129,80,132);l.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);var crafting=new CraftingMenu(37,p.getInventory(),ContainerLevelAccess.create(l,table));
   for(int i=0;i<9;i++)crafting.getSlot(i+1).setByPlayer(new ItemStack(i==4?ChaoGarden.item("chao_key"):i%3==1?Items.LIGHT_WEIGHTED_PRESSURE_PLATE:Items.GLASS_PANE));
   p.getInventory().removeItem(p.getInventory().findSlotMatchingItem(new ItemStack(ChaoGarden.item("chao_key"))),1);
   check(crafting.getSlot(0).getItem().is(ChaoGarden.item("chao_teleporter")),"native crafting menu resolves exact six-pane two-plate key recipe");
   crafting.quickMoveStack(p,0);check(crafting.getSlot(5).getItem().isEmpty()&&p.getInventory().countItem(ChaoGarden.item("chao_teleporter"))==1,"native result transaction consumes recipe once");crafting.removed(p);
   int slot=p.getInventory().findSlotMatchingItem(new ItemStack(ChaoGarden.item("chao_teleporter")));var stack=p.getInventory().removeItemNoUpdate(slot);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
   var hit=new BlockHitResult(Vec3.atCenterOf(PORTAL.below()).add(0,.5,0),Direction.UP,PORTAL.below(),false);check(p.gameMode.useItemOn(p,l,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"survival teleporter item placement");check(stack.isEmpty()&&l.getBlockState(PORTAL).is(HubContent.TELEPORTER.get())&&l.getBlockState(PORTAL.above()).is(HubContent.TELEPORTER.get()),"placement consumes one and creates both portal halves");
   p.connection.teleport(128.5,80.1,128.5,180,0);
  }else{
   var saved=net.minecraft.nbt.NbtIo.readCompressed(DIR.resolve("identities.nbt"),net.minecraft.nbt.NbtAccounter.unlimitedHeap());l.getChunkAt(PORTAL);l.getChunkAt(ROOT);baby=(ChaoEntity)l.getEntity(saved.getUUID("Baby"));adult=(ChaoEntity)l.getEntity(saved.getUUID("Adult"));trader=(BlackMarketTrader)l.getEntity(saved.getUUID("Trader"));
   if((baby==null||adult==null||trader==null)&&++loadWait<200){ticks=0;return;}
   check(baby!=null&&adult!=null&&trader!=null,"same saved Chao and wild trader UUIDs survive process restart");
   check(p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&p.getInventory().countItem(ChaoGarden.item("chao_key"))==1,"survival mode and remaining key persist");
   check(l.getBlockState(PORTAL).is(HubContent.TELEPORTER.get())&&l.getBlockState(PORTAL.above()).is(HubContent.TELEPORTER.get()),"both teleporter halves persist");
   var crown=ROOT.above(4);check(l.getBlockState(crown).getValue(ChaoTreeCrownBlock.GARDEN)&&l.getBlockEntity(crown) instanceof FruitCrownEntity,"planted palm state and mesh entity reload");
   for(var pod:FruitContent.podPositions(l,crown,"garden_fruit"))check(l.getBlockState(pod).is(FruitContent.podForFruit("garden_fruit"))&&l.getBlockState(pod).canSurvive(l,pod),"real fruit support after disk reload");
   check(((net.minecraft.world.level.block.entity.JukeboxBlockEntity)l.getBlockEntity(BOX)).getTheItem().is(ChaoGarden.item("chao_garden_music_disc")),"Chao disc survives in jukebox");
   check(baby.state().fruitCount==1&&adult.state().fruitCount==1&&baby.owner.equals(p.getUUID())&&adult.owner.equals(p.getUUID()),"care and ownership persist without duplicate meal");
   check(!ToyPreview.active(baby)&&baby.toyPlayingTicks()==0&&!baby.isNoAi(),"saved looping toy does not resume or leave Chao frozen");
   check(trader.getOffers().stream().filter(o->o.getResult().is(ChaoGarden.item("chao_key"))&&!o.isOutOfStock()).count()==1,"wild trader key survives true disk reload");
   p.connection.teleport(128.5,80.1,128.5,180,0);
  }
 }
 if(ticks==70){check(p.level().dimension().equals(HubContent.HUB),"walk-through teleporter enters Chao lobby");check(HubContent.returnHome(p),"return transporter transaction");check(p.level().dimension().equals(Level.OVERWORLD)&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"return restores home dimension and survival mode");}
 if(pass==2&&ticks==100){check(baby.state().fruitCount==1&&adult.state().fruitCount==1,"no meal duplicated after resumed ticking");save(server,"reload-pass.txt","Actual dedicated stop/start with client reconnect: persisted identities, key/stock, paired teleporter + round trip, palm fruit, jukebox disc, care, stopped preview.\n");return;}
 if(pass==1&&ticks==110){p.connection.teleport(138.5,80,131.5,180,0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("garden_fruit_seed")));var hit=new BlockHitResult(Vec3.atCenterOf(ROOT.below()).add(0,.5,0),Direction.UP,ROOT.below(),false);check(p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction(),"survival seed planting");check(p.getMainHandItem().isEmpty()&&l.getBlockState(ROOT).is(FruitContent.saplingForFruit("garden_fruit")),"seed consumed once");check(FruitContent.growTree(l,ROOT,"garden_fruit"),"seed grows garden palm");check(GardenTools.water(l,ROOT),"watering real palm ripens fruit");var pod=ROOT.above(3).north();check(FruitContent.podForFruit("garden_fruit").harvest(l,pod,l.getBlockState(pod)),"harvest produces real fruit");check(l.getBlockState(pod).getValue(HangingFruitBlock.AGE)==0,"harvest resets growth");FruitContent.treeForFruit("garden_fruit").performBonemeal(l,l.random,ROOT.above(4),l.getBlockState(ROOT.above(4)));check(l.getBlockState(pod).getValue(HangingFruitBlock.AGE)==1,"fruit regrows after harvest");
  baby=chao(p,false,128.5);adult=chao(p,true,145.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("garden_fruit"),2));baby.mobInteract(p,InteractionHand.MAIN_HAND);adult.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().isEmpty()&&baby.getMainHandItem().getCount()==1&&adult.getMainHandItem().getCount()==1,"baby and adult accept exactly one fruit each");
  l.setBlock(BOX,Blocks.JUKEBOX.defaultBlockState(),3);p.connection.teleport(130.5,80,133,180,0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_garden_music_disc")));var click=new BlockHitResult(Vec3.atCenterOf(BOX),Direction.UP,BOX,false);check(p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,click).consumesAction(),"Chao disc inserts into real jukebox");check(p.getMainHandItem().isEmpty()&&l.getBlockState(BOX).getValue(JukeboxBlock.HAS_RECORD),"jukebox takes one custom-song disc");
 }
 if(pass==1&&ticks==230){check(baby.state().fruitCount==1&&adult.state().fruitCount==1&&baby.state().hunger>20&&adult.state().hunger>20,"baby/adult meals complete exactly once");p.connection.teleport(baby.getX()+2,80,baby.getZ(),0,0);check(cmd(p,"chao testtoy rattle loop")==1&&ToyPreview.active(baby),"real production rattle preview starts");}
 if(pass==1&&ticks==250){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);baby.mobInteract(p,InteractionHand.MAIN_HAND);check(!ToyPreview.active(baby)&&baby.getAffectionTicks()>0,"pet interrupts rattle and starts care");p.connection.teleport(adult.getX()+2,80,adult.getZ(),0,0);check(cmd(p,"chao testtoy trumpet loop")==1&&ToyPreview.active(adult),"real production adult trumpet starts");}
 if(pass==1&&ticks==275){check(adult.hurt(l.damageSources().generic(),1),"adult accepts actual damage during trumpet");var n=new CompoundTag();trader.saveWithoutId(n);n.putLong("MarketRestockAt",l.getGameTime()-1);trader.load(n);trader.aiStep();check(trader.getOffers().stream().filter(o->o.getResult().is(ChaoGarden.item("chao_key"))&&!o.isOutOfStock()).count()==1,"real wild stock refresh keeps key");}
 if(pass==1&&ticks==280)check(!ToyPreview.active(adult)&&adult.toyPlayingTicks()==0,"hurt interrupts adult trumpet by the next entity tick");
 if(pass==1&&ticks==340){adult.state().boredom=100;adult.state().hunger=90;adult.state().energy=100;adult.state().points[ChaoState.RUN]=667;adult.state().personality=com.chaogarden.core.ChaoPreferences.EASILY_BORED;adult.sync();adult.getRandom().setSeed(18);adult.moveTo(146.5,80,134.5,0,0);l.setBlock(new BlockPos(148,80,134),ToyContent.block("chao_rattle").defaultBlockState(),3);adult.goalSelector.addGoal(4,new ToyContent.PlayGoal(adult));}
 if(pass==1&&ticks>340&&naturalStart<0&&adult.toyPlayingKind().equals("chao_rattle")&&adult.toyPlayingTicks()>0){naturalStart=ticks;check(true,"adult naturally starts a real placed rattle");}
 if(pass==1&&naturalStart>0&&ticks==naturalStart+30)l.destroyBlock(new BlockPos(148,80,134),true);
 if(pass==1&&naturalStart>0&&ticks==naturalStart+32){check(adult.toyPlayingTicks()==0&&adult.getHeldChaoItem().isEmpty(),"breaking the real placed rattle stops play and clears temporary prop");naturalInterrupted=true;}
 if(pass==1&&ticks==550){check(naturalInterrupted,"real placed-toy interruption completed");p.connection.teleport(baby.getX()+2,80,baby.getZ(),0,0);check(cmd(p,"chao testtoy triangle loop")==1&&ToyPreview.active(baby),"triangle active before shutdown for reload interruption");}
 if(pass==1&&ticks==575){var n=new CompoundTag();n.putUUID("Baby",baby.getUUID());n.putUUID("Adult",adult.getUUID());n.putUUID("Trader",trader.getUUID());Files.createDirectories(DIR);net.minecraft.nbt.NbtIo.writeCompressed(n,DIR.resolve("identities.nbt"));save(server,"survival-pass.txt","Connected survival client: real merchant payments, crafting/result consumption, paired placement, portal round trip, seed/growth/watering/harvest/regrowth, baby/adult eating, jukebox, pet/hurt preview interruptions, naturally played rattle interrupted by breaking its real block, stock refresh. Save while triangle looping.\n");}
 }catch(Throwable failure){LogUtils.getLogger().error("PLAYABLE02536_DEDICATED_FAIL",failure);try{Files.createDirectories(DIR);Files.writeString(DIR.resolve("failure-"+pass+".txt"),failure.toString()+"\n");}catch(Exception ignored){}server.halt(false);done=true;}
 }
}
