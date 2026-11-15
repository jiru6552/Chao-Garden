package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LobbyApproachGeometry0255;
import com.chaogarden.hub.GardenUnlocks081;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Run ONLY this namespace in a fresh server world. Global unlocks are permanent;
 * this scenario deliberately does not erase history created by other tests. */
@GameTestHolder("chao_unlock_fresh81") @PrefixGameTestTemplate(false)
public final class GardenUnlockFreshGameTests081 {
  @GameTest(template="activity_test",timeoutTicks=400)
  public static void freshServerObtainsIndependentGardensAndSharesThemWithLaterPlayers(GameTestHelper h){
    var server=h.getLevel().getServer();var data=HubWorldData.get(server);
    h.assertTrue(!data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"Fresh-world-only scenario refuses to reset any prior server unlock history");
    var first=TestConnections.makeSurvivalServerPlayer(h);var late=new net.minecraft.server.level.ServerPlayer[1];
    var chao=ChaoGarden.CHAO.get().create(h.getLevel());
    try{
      var orphaned=server.getLevel(HubContent.HUB);var sentinel=new BlockPos(4,70,4);
      h.assertTrue(orphaned!=null&&data.save(new CompoundTag(),h.getLevel().registryAccess()).getCompound("Revisions").getInt(HubContent.HUB.location().toString())==0
          &&orphaned.getBlockState(HubRooms.SCENE_ANCHOR081).isAir(),"Fresh scenario has no lobby metadata or anchor before generation");
      orphaned.setBlockAndUpdate(sentinel,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
      var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)orphaned.getBlockEntity(sentinel);
      chest.setItem(0,new ItemStack(net.minecraft.world.item.Items.EMERALD,3));
      var exactChest=chest.saveWithFullMetadata(orphaned.registryAccess());
      h.assertTrue(!HubContent.ensure(orphaned)&&!HubContent.enterHub(first),"Missing both markers never authorizes rebuilding a nonempty orphaned lobby");
      h.assertTrue(orphaned.getBlockEntity(sentinel)==chest
          &&orphaned.getBlockState(sentinel).is(net.minecraft.world.level.block.Blocks.CHEST)
          &&exactChest.equals(chest.saveWithFullMetadata(orphaned.registryAccess()))
          &&orphaned.getBlockState(HubRooms.SCENE_ANCHOR081).isAir()
          &&orphaned.getBlockState(new BlockPos(0,79,0)).isAir()
          &&data.save(new CompoundTag(),h.getLevel().registryAccess()).getCompound("Revisions").getInt(HubContent.HUB.location().toString())==0
          &&!HubContent.hasReturnJournal(first),"Rejected orphaned generation preserves exact chest/items, absent architecture, revision and visitor state");
      // Remove only this test's known sentinel contents/block; never reset a
      // real lobby or unlock history to make a fresh-generation test pass.
      chest.clearContent();orphaned.setBlockAndUpdate(sentinel,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
      h.assertTrue(HubContent.enterHub(first),"First player enters the actual generated lobby");var hub=first.serverLevel();
      h.assertTrue(!HubRooms.accessInstalled081(hub,true)&&!HubRooms.accessInstalled081(hub,false),"Fresh Hero/Dark approaches and portal anchors are absent");
      for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++)if(Math.hypot(x,z)<=25&&LobbyApproachGeometry0255.floorOpening(x,z))
        h.assertTrue(hub.getBlockState(new BlockPos(x,79,z)).is(LobbyDecor025.GLASS_FLOOR.get()),"Every formerly cut-out lobby-floor cell is real solid footing while locked");
      h.assertTrue(!HubContent.visitHeroGarden(first)&&!HubContent.visitDarkGarden(first)&&first.level()==hub,"Actual server portal entry denies both locked destinations");
      // An upgraded save may already have a visitor in either old always-open
      // garden. Its return must not use the now absent raised/lowered stairs.
      for(var realm:java.util.List.of(HubContent.HERO_GARDEN,HubContent.DARK_GARDEN)){
        var oldGarden=server.getLevel(realm);h.assertTrue(oldGarden!=null&&HubContent.ensure(oldGarden),"Existing locked-garden visitor realm is available");
        var spawn=realm.equals(HubContent.HERO_GARDEN)?com.chaogarden.hub.HeroGardenRooms0253.SPAWN:com.chaogarden.hub.DarkGardenRooms0255.SPAWN;
        first.teleportTo(oldGarden,spawn.x,spawn.y,spawn.z,0,0);first.hasChangedDimension();
        h.assertTrue(HubContent.returnToHub(first)&&first.level()==hub&&first.position().distanceTo(HubContent.HUB_SPAWN)<.05,
            "Legacy visitor returns to the central landing while its side remains locked");
        var floor=BlockPos.containing(first.position()).below();
        h.assertTrue(hub.getBlockState(floor).isFaceSturdy(hub,floor,net.minecraft.core.Direction.UP),"Locked-side return has real solid footing rather than a fall or sealed basement");
      }
      h.assertTrue(!data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"Visiting a dimension administratively does not invent a Chao acquisition");
      var state=new ChaoState();state.align=.75f;chao.setState(state);chao.setNoAi(true);chao.setNoGravity(true);
      var at=h.absolutePos(new BlockPos(2,2,2));chao.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
      h.assertTrue(h.getLevel().addFreshEntity(chao),"Actual child can be published");GardenUnlocks081.observe(chao);
      h.assertTrue(!data.heroGardenUnlocked(),"An unowned aligned world creature is not an acquisition");
      chao.owner=first.getUUID();chao.sync();
      h.assertTrue(data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"Obtaining the actual Hero child opens Hero independently");
      h.assertTrue(HubRooms.accessInstalled081(hub,true)&&!HubRooms.accessInstalled081(hub,false),"Hero stairs/cutout/portal publish together without opening Dark");
      h.assertTrue(!HubContent.visitDarkGarden(first),"The still-locked Dark portal is denied after Hero unlock");
      h.assertTrue(HubContent.visitHeroGarden(first)&&first.level().dimension().equals(HubContent.HERO_GARDEN),"Unlocked Hero uses the actual dimension transition");
      h.assertTrue(HubContent.returnToHub(first)&&first.level()==hub,"Hero return preserves its journal and returns safely");
      var dark=new ChaoState();dark.align=-.8f;var item=new ItemStack(ChaoGarden.item("chao_carrier"));var entity=new CompoundTag();
      entity.putUUID("UUID",UUID.randomUUID());entity.putUUID("Keeper",first.getUUID());entity.putString("Chao",dark.encode());ChaoCarrierItem.storeEntity(item,entity);
      var before=item.save(first.registryAccess());first.getEnderChestInventory().setItem(26,item);GardenUnlocks081.scanPlayer(first);
      h.assertTrue(data.allGardensUnlocked()&&before.equals(item.save(first.registryAccess())),"Accessible stored Dark evidence opens only its missing side without changing its item");
      h.assertTrue(HubRooms.accessInstalled081(hub,true)&&HubRooms.accessInstalled081(hub,false),"Both installed portals retain the same shared unlock history");
      var restored=HubWorldData.load(data.save(new CompoundTag(),first.registryAccess()),first.registryAccess());
      h.assertTrue(restored.allGardensUnlocked()&&restored.visit(first.getUUID())!=null,"Serialized server restart data retains unlocks and the existing return journal");
      late[0]=TestConnections.makeSurvivalServerPlayer(h);
      h.assertTrue(HubContent.enterHub(late[0])&&GardenUnlocks081.canUsePortal(server,true)&&GardenUnlocks081.canUsePortal(server,false),"A later real server player sees the same persisted installations without a private unlock");
      h.assertTrue(server.getCommands().getDispatcher().execute("chao gardens unlock all",first.createCommandSourceStack().withPermission(2))==1,"Permission2 recovery command safely accepts already-unlocked state");
      h.succeed();
    }catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure){throw new IllegalStateException(failure);}
    finally{
      if(chao!=null)chao.discard();
      for(var player:new net.minecraft.server.level.ServerPlayer[]{first,late[0]})if(player!=null){if(HubContent.hasReturnJournal(player))HubContent.returnHome(player);server.getPlayerList().remove(player);}
    }
  }
}
