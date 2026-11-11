package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.hub.ChaoTeleporterBlock;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Real dimension travel, physical tube placement, return journals and room protection. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class HubWorldGameTests {
  private static final BlockPos CENTER=new BlockPos(2,2,2);
  private static ServerPlayer player(GameTestHelper h,GameType mode){
    var level=h.getLevel();var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"hub-visitor"),false);
    var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
    var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);NetworkRegistry.configureMockConnection(connection);
    level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(mode);
    var at=h.absolutePos(CENTER);
    for(BlockPos p:BlockPos.betweenClosed(at.offset(-3,-1,-3),at.offset(3,-1,3)))level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
    player.teleportTo(at.getX()+.5,at.getY()+.1,at.getZ()+.5);player.setNoGravity(true);return player;
  }
  private static ItemStack heldChao(GameTestHelper h,ServerPlayer p){
    var chao=ChaoGarden.CHAO.get().create(h.getLevel());chao.owner=p.getUUID();chao.setState(ChaoState.egg(4,true,true,0,new Random(24)));
    chao.setCustomName(net.minecraft.network.chat.Component.literal("Hub Traveller"));
    var stack=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(stack,chao,p),"Could not prepare genuine held Chao");return stack;
  }
  private static void finish(ServerPlayer player){
    if(HubContent.isHubDimension(player.level()))HubContent.returnHome(player);
    player.server.getPlayerList().remove(player);
  }

  @GameTest(template="empty",timeoutTicks=160)
  public static void physicalTubeStepLatchAndSameCreatureRoundTrip(GameTestHelper h){
    var player=player(h,GameType.SURVIVAL);var origin=player.position();var originalLevel=player.serverLevel();
    var carrier=heldChao(h,player);var copied=carrier.copy();var identity=ChaoCarrierItem.storedUUID(carrier);
    player.getInventory().setItem(5,carrier);player.getInventory().setItem(6,new ItemStack(Items.DIAMOND,9));
    var tube=new ItemStack(ChaoGarden.item("chao_teleporter"));player.setItemInHand(InteractionHand.MAIN_HAND,tube);
    var base=h.absolutePos(CENTER);var floor=base.below();
    var used=tube.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
        new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
    h.assertTrue(used.consumesAction()&&tube.isEmpty(),"A survival tube did not place from one item");
    h.assertTrue(originalLevel.getBlockState(base).is(HubContent.TELEPORTER.get())
        &&originalLevel.getBlockState(base.above()).getValue(ChaoTeleporterBlock.HALF)==DoubleBlockHalf.UPPER,"Tube's physical two halves missing");
    HubContent.tickPlayer(player);player.hasChangedDimension();
    h.assertTrue(player.level().dimension().equals(HubContent.HUB)&&player.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Walking into actual tube did not enter adventure hub");
    h.assertTrue(ItemStack.matches(copied,player.getInventory().getItem(5))&&ChaoCarrierItem.storedUUID(player.getInventory().getItem(5)).equals(identity),"Trip changed held Chao state/identity");
    h.runAfterDelay(45,()->{
      h.assertTrue(player.level().dimension().equals(HubContent.HUB),"Arrival bounced straight back from center tube");
      player.teleportTo(4.5,80.1,4.5);HubContent.tickPlayer(player);
      player.teleportTo(.5,80.1,.5);HubContent.tickPlayer(player);player.hasChangedDimension();
      h.assertTrue(player.level()==originalLevel&&player.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Stepping out and back did not restore original world/mode");
      h.assertTrue(player.position().distanceTo(origin)<5&&!HubContent.hasReturnJournal(player),"Return landing is not near original tube or journal remains active");
      h.assertTrue(ItemStack.matches(copied,player.getInventory().getItem(5))&&player.getInventory().getItem(6).getCount()==9,"Portal round trip altered inventory");
      h.assertTrue(originalLevel.getEntity(identity)==null,"Travel released/duplicated the held Chao");finish(player);h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void kindergartenJourneyKeepsOriginAndMultiplayerModes(GameTestHelper h){
    var first=player(h,GameType.CREATIVE);var second=player(h,GameType.ADVENTURE);
    first.getAbilities().flying=true;var original=h.getLevel();
    h.assertTrue(HubContent.enterHub(first)&&HubContent.enterHub(second),"Two keepers could not enter shared hub");
    h.assertTrue(HubContent.visitKindergarten(first)&&first.level().dimension().equals(HubContent.KINDERGARTEN),"Kindergarten door route unavailable");
    h.assertTrue(second.level().dimension().equals(HubContent.HUB),"One keeper's route moved another keeper");
    var data=HubWorldData.get(first.server);var saved=data.save(new CompoundTag(),first.registryAccess());var loaded=HubWorldData.load(saved,first.registryAccess());
    h.assertTrue(loaded.visit(first.getUUID()).getString("Dimension").equals(original.dimension().location().toString())
        &&loaded.visit(first.getUUID()).getInt("GameMode")==GameType.CREATIVE.getId(),"Internal route overwrote persistent original world/mode");
    first.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).remove("ChaoHubReturn");
    HubContent.login(new PlayerEvent.PlayerLoggedInEvent(first));
    h.assertTrue(HubContent.hasReturnJournal(first)&&first.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Re-login did not recover authoritative visit journal");
    h.assertTrue(HubContent.returnToHub(first)&&HubContent.returnHome(first),"Kindergarten -> lobby -> original world route failed");
    h.assertTrue(first.level()==original&&first.gameMode.getGameModeForPlayer()==GameType.CREATIVE&&first.getAbilities().flying,"Creative/flying mode not restored exactly");
    h.assertTrue(second.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&HubContent.hasReturnJournal(second),"First keeper's return cleared another journal");
    h.assertTrue(HubContent.returnHome(second)&&second.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Original Adventure mode was not retained");
    finish(first);finish(second);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void deniedTravelAndOccupiedCursorPreservePlayerState(GameTestHelper h){
    var p=player(h,GameType.CREATIVE);var before=p.position();var world=p.level();
    p.inventoryMenu.setCarried(new ItemStack(Items.EMERALD,7));
    h.assertTrue(!HubContent.enterHub(p)&&p.inventoryMenu.getCarried().getCount()==7&&!HubContent.hasReturnJournal(p),"Occupied cursor was consumed or admitted into a portal");
    p.inventoryMenu.setCarried(ItemStack.EMPTY);
    Consumer<EntityTravelToDimensionEvent> deny=event->{if(event.getEntity()==p&&event.getDimension().equals(HubContent.HUB))event.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(deny);
    try{h.assertTrue(!HubContent.enterHub(p),"Canceled travel was reported successful");}
    finally{NeoForge.EVENT_BUS.unregister(deny);}
    h.assertTrue(p.level()==world&&p.position().equals(before)&&p.gameMode.getGameModeForPlayer()==GameType.CREATIVE&&!HubContent.hasReturnJournal(p),"Denied travel changed mode/location or leaked return journal");
    h.assertTrue(HubContent.enterHub(p),"Valid retry failed after a canceled transition");
    Consumer<PlayerEvent.PlayerChangeGameModeEvent> denyRestore=event->{
      if(event.getEntity()==p&&event.getNewGameMode()==GameType.CREATIVE)event.setCanceled(true);
    };
    NeoForge.EVENT_BUS.addListener(denyRestore);
    try{
      h.assertTrue(HubContent.returnHome(p)&&p.level()==world&&HubContent.hasReturnJournal(p),"Denied mode restoration lost the recoverable journal");
      h.assertTrue(!HubContent.enterHub(p)&&HubWorldData.get(p.server).visit(p.getUUID()).getInt("GameMode")==GameType.CREATIVE.getId(),"Re-entry overwrote a temporarily vetoed original mode");
    }finally{NeoForge.EVENT_BUS.unregister(denyRestore);}
    HubContent.tickPlayer(p);
    h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.CREATIVE&&!HubContent.hasReturnJournal(p),"Return mode did not retry safely after temporary denial ended");
    finish(p);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void roomProtectionDefeatsCanBreakWithoutChangingGlobalRules(GameTestHelper h){
    var p=player(h,GameType.SURVIVAL);var rules=p.server.overworld().getGameRules().createTag().copy();
    var hub=p.server.getLevel(HubContent.HUB);var school=p.server.getLevel(HubContent.KINDERGARTEN);
    h.assertTrue(hub!=null&&school!=null&&HubContent.ensure(hub)&&HubContent.ensure(school),"Both data-defined dimensions must build");
    h.assertTrue(p.server.overworld().getGameRules().createTag().equals(rules),"Room preparation changed shared server gamerules");
    h.assertTrue(HubContent.enterHub(p),"Could not enter room protection fixture");
    var target=new BlockPos(8,79,8);var state=hub.getBlockState(target);
    var pick=new ItemStack(Items.DIAMOND_PICKAXE);pick.set(DataComponents.CAN_BREAK,new AdventureModePredicate(List.of(BlockPredicate.Builder.block().of(state.getBlock()).build()),true));
    p.setItemInHand(InteractionHand.MAIN_HAND,pick);
    h.assertTrue(!p.gameMode.destroyBlock(target)&&hub.getBlockState(target).equals(state),"CanBreak component bypassed protected shared scenery");
    p.teleportTo(8.5,80.1,8.5);p.hasChangedDimension();p.invulnerableTime=0;float health=p.getHealth();
    p.hurt(p.damageSources().generic(),5);h.assertTrue(p.getHealth()==health,"Scenic dimension harmed its visitor");
    h.assertTrue(p.server.overworld().getGameRules().createTag().equals(rules),"Visit changed shared world rules");
    var hostile=net.minecraft.world.entity.EntityType.ZOMBIE.create(hub);hostile.setPos(10.5,80.1,10.5);
    h.assertTrue(!hub.addFreshEntity(hostile),"Hostile mob entered the protected public room");
    finish(p);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=100)
  public static void missingOriginFallbackAndRespawnRestoreTheOriginalMode(GameTestHelper h){
    var p=player(h,GameType.SPECTATOR);
    h.assertTrue(HubContent.enterHub(p),"Spectator could not visit hub");
    var journal=HubWorldData.get(p.server).visit(p.getUUID());journal.putString("Dimension","missing_mod:removed_world");journal.putDouble("X",Double.NaN);
    HubWorldData.get(p.server).put(p.getUUID(),journal);
    h.assertTrue(HubContent.returnHome(p)&&p.level()==p.server.overworld()&&p.gameMode.getGameModeForPlayer()==GameType.SPECTATOR,"Missing origin failed safe fallback or lost spectator mode");
    finish(p);
    var mortal=player(h,GameType.CREATIVE);h.assertTrue(HubContent.enterHub(mortal),"Could not prepare real respawn case");
    mortal.hasChangedDimension();mortal.setHealth(0);mortal.die(mortal.damageSources().genericKill());
    var reborn=mortal.server.getPlayerList().respawn(mortal,false,Entity.RemovalReason.KILLED);reborn.connection.player=reborn;
    h.assertTrue(!HubContent.isHubDimension(reborn.level())&&reborn.gameMode.getGameModeForPlayer()==GameType.CREATIVE&&!HubContent.hasReturnJournal(reborn),"Actual death/respawn left Adventure mode or a stale return journal outside the hub");
    finish(reborn);h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=80)
  public static void directEntryWithoutJournalRespectsAdventureServerDefault(GameTestHelper h){
    var p=player(h,GameType.ADVENTURE);var server=p.server;var previous=server.getDefaultGameType();
    try{
      server.setDefaultGameType(GameType.ADVENTURE);
      var hub=server.getLevel(HubContent.HUB);h.assertTrue(hub!=null&&HubContent.ensure(hub),"Hub room not available");
      p.teleportTo(hub,.5,80.1,3.5,0,0);
      h.assertTrue(HubContent.hasReturnJournal(p),"Direct dimension entry did not make an escape journal");
      h.assertTrue(HubContent.returnHome(p)&&p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Unknown prior mode granted survival building rights on an Adventure server");
    }finally{server.setDefaultGameType(previous);finish(p);}
    h.succeed();
  }

}
