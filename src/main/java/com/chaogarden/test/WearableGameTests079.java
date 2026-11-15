package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoState;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** All canonical wearable identities through real consent, item codecs and removal.
 * Rendering and eye-hole clearance belong to the separate emitted-geometry tests. */
@GameTestHolder("chao_wear79") @PrefixGameTestTemplate(false)
public final class WearableGameTests079 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private static final List<String> IDS=List.of(
      "chao_garden:apple_hat","chao_garden:cardboard_box_hat","chao_garden:pan_hat",
      "chao_garden:paper_bag_hat","chao_garden:empty_can_hat","chao_garden:stump_hat",
      "chao_garden:flower_pot_hat","chao_garden:bucket_hat","chao_garden:red_wool_beanie",
      "chao_garden:blue_wool_beanie","chao_garden:black_white_wool_beanie","chao_garden:eggshell_hat",
      "minecraft:carved_pumpkin","minecraft:melon","minecraft:skeleton_skull",
      "chao_garden:baseball_cap_accessory","chao_garden:bowtie_accessory",
      "chao_garden:round_glasses_accessory","chao_garden:aviators_accessory",
      "chao_garden:headphones_accessory","chao_garden:pacifier_accessory",
      "chao_garden:press_hat_accessory","chao_garden:blue_hoodie_accessory",
      "chao_garden:pink_hoodie_accessory","chao_garden:cheese_bowtie_accessory");

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();
    for(int i=0;i<IDS.size();i++)for(boolean adult:new boolean[]{false,true}){
      final int index=i;String id=IDS.get(i);
      tests.add(new TestFunction("wear079_lifecycle",id.replace(':','_')+"_"+(adult?"adult":"baby"),
          "chao_hats50:activity_test",100,0,true,h->lifecycle(h,index,adult)));
    }
    return tests;
  }
  private static void cleanup(GameTestHelper h,Runnable action){
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
    });
  }
  private static ServerPlayer keeper(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    p.teleportTo(Vec3.atBottomCenterOf(h.absolutePos(AT)).x,h.absolutePos(AT).getY(),
        h.absolutePos(AT).getZ()+1.5);p.setNoGravity(true);
    cleanup(h,()->p.server.getPlayerList().remove(p));return p;
  }
  private static void lifecycle(GameTestHelper h,int index,boolean adult){
    var p=keeper(h);var stranger=keeper(h);h.setBlock(AT.below(),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());
    var s=ChaoState.egg(index%7,index%2==0,true,0,new Random(7900+index));
    s.adult=adult;s.align=index%3-1;s.primaryType=adult?index%5:0;s.happiness=100;s.energy=100;s.hunger=80;
    s.setTrait("foreign.wear079","preserve me");
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.setStaying(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);cleanup(h,c::discard);
    var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(IDS.get(index)));
    var offered=new ItemStack(item,2);offered.set(DataComponents.CUSTOM_NAME,Component.literal("My actual "+IDS.get(index)));
    var data=new CompoundTag();data.putString("foreign.saved","opaque components retained");
    if(IDS.get(index).endsWith("eggshell_hat"))data.putString("EggShellCoat",s.encode());
    offered.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
    var slot=ChaoCareServices.accessorySlot(offered);h.assertTrue(slot!=null,"Canonical item has wardrobe slot");
    var expected=offered.copyWithCount(1);p.getInventory().setItem(0,offered);
    stranger.getInventory().setItem(0,expected.copy());
    h.assertFalse(ChaoCareServices.equip(stranger,c,0),"Foreign keeper cannot offer clothing");
    h.assertTrue(ChaoCareServices.equip(p,c,0),"Owner offers real inventory item");
    h.assertTrue(offered.getCount()==1&&ItemStack.matches(c.getMainHandItem(),expected),"Exactly one actual stack transfers to consideration");
    WardrobeGameTests.finishAccepted(c);
    h.assertTrue(ItemStack.matches(c.getItemBySlot(slot),expected)&&!c.isHandlingItem(),"Consent finishes into intended equipment slot");
    var tag=new CompoundTag();c.saveWithoutId(tag);var loaded=ChaoGarden.CHAO.get().create(h.getLevel());loaded.load(tag);
    h.assertTrue(ItemStack.matches(loaded.getItemBySlot(slot),expected),"Entity NBT preserves wearable and arbitrary components");
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(carrier,c);
    var released=ChaoGarden.CHAO.get().create(h.getLevel());released.load(ChaoCarrierItem.entityData(carrier));
    h.assertTrue(ItemStack.matches(released.getItemBySlot(slot),expected),"Carrier codec preserves exact wearable");
    for(var copy:List.of(c,loaded,released)){
      h.assertTrue(copy.state().adult==adult&&copy.state().align==s.align&&copy.state().primaryType==s.primaryType,
          "Wardrobe and reload preserve stage, alignment and first-type identity");
      h.assertTrue(copy.state().trait("foreign.wear079").equals("preserve me"),"Unrelated saved data survives");
    }
    h.assertFalse(ChaoCareServices.remove(stranger,c,slot),"Foreign keeper cannot remove actual equipment");
    h.assertTrue(ChaoCareServices.remove(p,c,slot)&&c.getItemBySlot(slot).isEmpty(),"Owner removes wearable");
    h.assertTrue(p.getInventory().countItem(item)==2,"Removal returns exactly one original item");
    int exactReturned=0;
    for(int i=0;i<36;i++){
      var returned=p.getInventory().getItem(i);
      if(returned.is(item)){
        h.assertTrue(ItemStack.isSameItemSameComponents(returned,expected),"Returned item retains every original component");
        exactReturned+=returned.getCount();
      }
    }
    h.assertTrue(exactReturned==2,"Both retained and returned copies preserve exact components");
    h.assertFalse(ChaoCareServices.remove(p,c,slot),"Repeated removal cannot duplicate item");
    h.assertTrue(p.getInventory().countItem(item)==2,"Count remains stable after repeated removal");
    h.succeed();
  }
}
