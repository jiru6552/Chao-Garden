package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.cwe.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class CweContentGameTests {
  @GameTest(template="empty",timeoutTicks=40)
  public static void cweCatalogResolvesPhysicalItemsAndMatchingOrchards(GameTestHelper h){
    for(String id:CweCatalog.gifts().keySet()){
      h.assertTrue(ChaoGarden.item(id) instanceof CweGiftItem,"Missing real gift "+id);
      h.assertTrue(com.chaogarden.market.MarketStock.catalog().get(id).sourceRings()==CweCatalog.gift(id).rings(),"Gift catalog and merchant disagree on fixed price "+id);
    }
    for(String id:CweCatalog.FRUITS){
      h.assertTrue(ChaoGarden.item(id) instanceof com.chaogarden.item.CareItem,"Missing food "+id);
      h.assertTrue(com.chaogarden.fruit.FruitContent.saplingForFruit(id)!=null,"Missing renewable crop "+id);
      h.assertTrue(!com.chaogarden.fruit.FruitCatalog.isNatural(id),"CWE fruit bypassed market-first discovery");
    }
    for(int animal=21;animal<31;animal++)h.assertTrue(com.chaogarden.genes.GeneContent.gene(animal) instanceof com.chaogarden.genes.GeneItem,"Missing extra DNA sample");
    h.assertTrue(!com.chaogarden.market.MarketStock.catalog().containsKey("eggshell_hat"),"Hatch-only top shell should not appear in shop");h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void accessoriesPreserveActualStackComponentsAcrossEntitySave(GameTestHelper h){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    var first=new ItemStack(ChaoGarden.item("bowtie_accessory"));first.set(DataComponents.DYED_COLOR,new DyedItemColor(0x49ac78,true));
    first.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Garden prize"));
    c.setItemSlot(EquipmentSlot.LEGS,first.copy());
    var glasses=new ItemStack(ChaoGarden.item("round_glasses_accessory"));c.setItemSlot(EquipmentSlot.OFFHAND,glasses.copy());
    var saved=new CompoundTag();c.saveWithoutId(saved);var loaded=ChaoGarden.CHAO.get().create(h.getLevel());loaded.load(saved);
    h.assertTrue(ItemStack.isSameItemSameComponents(first,loaded.getItemBySlot(EquipmentSlot.LEGS)),"Neck item's actual dye/name disappeared");
    h.assertTrue(loaded.getItemBySlot(EquipmentSlot.OFFHAND).is(glasses.getItem()),"Independent face slot disappeared");
    var replacement=new ItemStack(ChaoGarden.item("cheese_bowtie_accessory"));((CweGiftItem)replacement.getItem()).apply(loaded,replacement,player);
    long returned=player.getInventory().items.stream().filter(x->ItemStack.isSameItemSameComponents(first,x)).mapToInt(ItemStack::getCount).sum();
    h.assertTrue(returned==0&&ItemStack.isSameItemSameComponents(first,loaded.getItemBySlot(EquipmentSlot.LEGS)),"Standalone accessory apply bypassed the real offer or produced free equipment");h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void droppedAttractorFruitConsumesOneUnitAndHonorsOwnership(GameTestHelper h){
    var p=h.absolutePos(new BlockPos(3,2,3));var level=h.getLevel();
    var fruit=new ItemEntity(level,p.getX(),p.getY(),p.getZ(),new ItemStack(ChaoGarden.item("orange_fruit"),2));fruit.setPickUpDelay(0);fruit.setOnGround(true);level.addFreshEntity(fruit);
    h.assertTrue(!CweContent.attract(fruit),"Fresh fruit converted before the waiting period");
    fruit.getPersistentData().putLong("CweAttractAt",level.getGameTime()-1);fruit.setTarget(java.util.UUID.randomUUID());
    h.assertTrue(!CweContent.attract(fruit)&&fruit.getItem().getCount()==2,"Ownership-restricted item was consumed");fruit.setTarget(null);
    h.assertTrue(CweContent.attract(fruit)&&fruit.getItem().getCount()==1,"Exactly one fruit must become one DNA sample");
    h.assertTrue(!CweContent.attract(fruit),"Repeated tick duplicated DNA without another waiting period");
    int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2),e->e.getItem().getItem() instanceof com.chaogarden.genes.GeneItem).stream().mapToInt(e->e.getItem().getCount()).sum();
    h.assertTrue(count==1,"Attraction produced more than one physical sample");
    int[] orangeWeights=new int[31];for(int roll=0;roll<100;roll++)orangeWeights[CweContent.attractedAnimal("orange_fruit",roll)]++;
    h.assertTrue(orangeWeights[21]==40&&orangeWeights[22]==40&&orangeWeights[23]==20,"Orange pool changed source40/40/20 weights");
    for(String id:CweContent.attractingFruits())for(int roll=0;roll<100;roll++)h.assertTrue(CweContent.attractedAnimal(id,roll)>=0&&CweContent.attractedAnimal(id,roll)<31,"Invalid weighted animal index");h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void characterEvolutionConditionsAndLensGateAreReal(GameTestHelper h){
    for(int type:new int[]{ChaoState.FLY_TYPE,ChaoState.RUN_TYPE,ChaoState.POWER_TYPE}){
      var c=ChaoGarden.CHAO.get().create(h.getLevel());var s=c.state();s.adult=true;s.reincarnations=2;s.animalMask=ChaoState.ALL_ANIMALS;s.happiness=80;s.primaryType=type;
      CweContent.onFirstEvolution(c);String expected=type==ChaoState.FLY_TYPE?"tails":type==ChaoState.RUN_TYPE?"amy":"knuckles";
      h.assertTrue(s.character.equals(expected)&&s.immortal,"Character evolution route did not produce "+expected);
    }
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var lens=new ItemStack(ChaoGarden.item("blue_lens"));var item=(CweGiftItem)lens.getItem();
    h.assertTrue(!item.canUse(c,lens),"Lens bypassed Skeleton Dog hat permission");c.state().hatsUnlocked=true;item.apply(c,lens,null);
    h.assertTrue(c.state().eyeColor==1,"An active lens failed after Skeleton Dog permission");
    c.state().eyeColor=12;c.state().adult=true;c.state().align=-1;c.state().primaryType=ChaoState.POWER_TYPE;CweContent.onFirstEvolution(c);
    h.assertTrue(c.state().character.equals("normal")&&!c.state().immortal&&c.state().eyeColor==0,"Retired Spartoi lens route survived first evolution");
    h.assertTrue(CweCatalog.gift("spartoi_lens")==null&&!com.chaogarden.market.MarketStock.catalog().containsKey("spartoi_lens"),"Retired lens remained in an active catalog");
    h.assertTrue(ChaoGarden.item("spartoi_lens") instanceof RetiredCweGiftItem&&!ChaoGarden.visibleInCreative(ChaoGarden.item("spartoi_lens")),"Compatibility lens is missing or advertised as active");
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void retiredCharacterEntitySavesKeepOwnershipIdentityAndPaidEquipment(GameTestHelper h){
    for(String retired:java.util.List.of("spartoi","egg")){
      var original=ChaoGarden.CHAO.get().create(h.getLevel());var state=ChaoState.egg(4,true,true,13,new java.util.Random(40));
      state.adult=true;state.immortal=true;state.setTrait("rare.parts",1);state.setTrait("learned.bell",1);state.setTrait("contest.race.wins",12);
      state.animalParts[ChaoState.EARS]=6;state.points[0]=1234;state.reincarnations=4;state.adultAgeTicks=777;
      original.setState(state);original.owner=java.util.UUID.randomUUID();original.setCustomName(net.minecraft.network.chat.Component.literal("Keep my Chao"));
      original.getPersistentData().putString("OtherModRecord","untouched");
      var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.OFFHAND,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
      var stacks=new ItemStack[slots.length];
      for(int i=0;i<slots.length;i++){
        stacks[i]=new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET);stacks[i].set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Paid equipment "+i));
        stacks[i].set(DataComponents.DYED_COLOR,new DyedItemColor(0x43a577+i,true));original.setItemSlot(slots[i],stacks[i].copy());
      }
      var saved=new CompoundTag();original.saveWithoutId(saved);String[] fields=saved.getString("Chao").split(";",25);
      fields[23]=retired;String[] appearance=fields[21].split(",");appearance[11]="12";fields[21]=String.join(",",appearance);saved.putString("Chao",String.join(";",fields));
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
      h.assertTrue(restored.getUUID().equals(original.getUUID())&&original.owner.equals(restored.owner),"Character retirement changed entity or keeper identity");
      h.assertTrue(restored.getCustomName().getString().equals("Keep my Chao")&&restored.getPersistentData().getString("OtherModRecord").equals("untouched"),"Retirement changed names or unrelated mod data");
      h.assertTrue(restored.state().character.equals("normal")&&restored.state().eyeColor==0&&restored.state().immortal==retired.equals("egg"),"Retired appearance did not become the correct ordinary/Chaos equivalent");
      h.assertTrue(restored.state().points[0]==1234&&restored.state().jewel()==13&&restored.state().isShiny()
          &&restored.state().animalParts[ChaoState.EARS]==6&&restored.state().intTrait("learned.bell")==1&&restored.state().intTrait("contest.race.wins")==12,"Retirement lost coat, acquired parts or progress");
      for(int i=0;i<slots.length;i++)h.assertTrue(ItemStack.matches(stacks[i],restored.getItemBySlot(slots[i])),"Retirement changed exact paid equipment in "+slots[i]);
    }
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=130)
  public static void retiredLensReturnsAsAnUnrecognizedObjectWithoutChangingTheChao(GameTestHelper h){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var at=h.absolutePos(new BlockPos(2,2,2));
    c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5);c.setNoAi(true);c.setNoGravity(true);
    var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.getAbilities().instabuild=false;
    var state=new ChaoState();state.eyeColor=11;state.hatsUnlocked=true;c.setState(state);c.owner=player.getUUID();h.getLevel().addFreshEntity(c);
    var retired=new ItemStack(ChaoGarden.item("spartoi_lens"),2);retired.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Old inventory lens"));
    var expected=retired.copyWithCount(1);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,retired);
    h.assertTrue(c.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction()&&retired.getCount()==1,"One legacy item must enter ordinary item inspection");
    h.runAfterDelay(105,()->{
      h.assertTrue(c.state().character.equals("normal")&&c.state().eyeColor==11,"Retired lens modified the Chao");
      var returned=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4),e->ItemStack.isSameItemSameComponents(e.getItem(),expected));
      h.assertTrue(returned.stream().mapToInt(e->e.getItem().getCount()).sum()==1&&c.getMainHandItem().isEmpty(),"Legacy inspection lost, changed or duplicated its actual stack");
      returned.forEach(ItemEntity::discard);c.discard();h.succeed();
    });
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void fourPartsBoxesConstructOneOwnedOmochaoWithConsistentGenes(GameTestHelper h){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var p=h.absolutePos(new BlockPos(3,2,3));c.moveTo(p.getX(),p.getY(),p.getZ());h.getLevel().addFreshEntity(c);
    var keeper=TestConnections.makeServerPlayer(h);c.owner=keeper.getUUID();c.setState(ChaoState.egg(4,true,false,0,new java.util.Random(9)));
    var registry=com.chaogarden.activities.GardenRegistry.get(h.getLevel().getServer());var garden=registry.register(keeper,p.offset(2,0,0));
    h.assertTrue(!CweContent.assembleOmochao(c),"Unqualified builder accepted a parts box");c.state().setTrait("reward.wrench",1);
    java.util.UUID prototypeId=null;
    for(int part=0;part<4;part++){
      h.assertTrue(CweContent.assembleOmochao(c),"Assembly failed at box "+part);
      var robot=h.getLevel().getEntitiesOfClass(com.chaogarden.entity.ChaoEntity.class,new AABB(p).inflate(3),e->e.state().character.equals("omochao"));
      h.assertTrue(robot.size()==1,"Each box must update one existing prototype");var prototype=robot.getFirst();
      if(part==0){
        prototypeId=prototype.getUUID();String actual=c.state().trait("content.omochao_uuid");
        c.state().setTrait("content.omochao_uuid",java.util.UUID.randomUUID().toString());
        h.assertTrue(!CweContent.assembleOmochao(c)&&c.state().intTrait("content.omochao_parts")==1,"Missing/unloaded prototype spawned a duplicate");c.state().setTrait("content.omochao_uuid",actual);
        c.setState(ChaoState.decode(c.state().encode()));
      }
      h.assertTrue(prototype.getUUID().equals(prototypeId),"Assembly replaced the prototype identity");
      if(part<3){
        h.assertTrue(prototype.state().intTrait("content.construction")==part+1&&prototype.isNoAi()&&prototype.isInvulnerable(),"Partial robot is not visibly staged and frozen");
        h.assertTrue(!prototype.canCare(keeper)&&!registry.bind(keeper,prototype,garden),"Incomplete robot could enter a garden or receive care");
        var before=prototype.position();com.chaogarden.activities.ActivityManager.callHome(prototype,garden.place());
        h.assertTrue(prototype.position().equals(before),"Garden transport moved the unfinished assembly");
        h.assertTrue(!com.chaogarden.item.ChaoCarrierItem.capture(new ItemStack(ChaoGarden.item("chao_carrier")),prototype,keeper)&&prototype.isAlive(),"Capture removed the unfinished assembly");
      }
      else h.assertTrue(prototype.state().intTrait("content.construction")==0&&!prototype.isNoAi()&&!prototype.isInvulnerable(),"Completed robot was not released into normal behavior");
    }
    h.assertTrue(!CweContent.assembleOmochao(c),"Fifth box duplicated the completed robot");
    var built=h.getLevel().getEntitiesOfClass(com.chaogarden.entity.ChaoEntity.class,new AABB(p).inflate(3),e->e.state().character.equals("omochao"));
    h.assertTrue(built.size()==1&&c.owner.equals(built.getFirst().owner),"Assembly did not produce exactly one owned Omochao");var s=built.getFirst().state();
    h.assertTrue(s.colorGenes[0]==4&&s.colorGenes[1]==4&&s.expressedColor==4&&s.shiny&&s.adult&&!s.immortal
        &&java.util.Arrays.stream(s.grades).allMatch(g->g==2),"Omochao coat, young-adult state or initial C ranks disagree");
    h.assertTrue(java.util.Arrays.stream(s.levels).sum()==0,"Builder's trained skills leaked into robot");
    h.assertTrue(registry.bind(keeper,built.getFirst(),garden),"Completed Omochao cannot enter a garden");
    registry.removeGarden(garden.place());built.getFirst().discard();c.discard();h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=160)
  public static void cosmeticGiftWaitsForConsiderationAndCommitsOnlyOnce(GameTestHelper h){
    var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.getAbilities().instabuild=false;
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var at=h.absolutePos(new BlockPos(3,2,3));c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);c.setNoAi(true);c.setNoGravity(true);c.owner=player.getUUID();h.getLevel().addFreshEntity(c);
    var mirror=new ItemStack(ChaoGarden.item("reverse_mirror"),2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,mirror);c.offerItem(player,net.minecraft.world.InteractionHand.MAIN_HAND);
    h.assertTrue(mirror.getCount()==1&&!c.state().negativePalette&&c.getMainHandItem().is(mirror.getItem()),"Gift applied instantly or did not transfer one real item");
    h.runAtTickTime(15,()->h.assertTrue(!c.state().negativePalette&&c.isHandlingItem(),"Gift skipped the thinking animation"));
    h.runAtTickTime(140,()->{h.assertTrue(c.state().negativePalette&&c.getMainHandItem().isEmpty()&&!c.isHandlingItem(),"Mirror failed to commit or replayed its toggle twice");c.discard();h.succeed();});
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void suspiciousPotionUsesTheAdultLifecycleInsteadOfChangingAnimalRules(GameTestHelper h){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var potion=new ItemStack(ChaoGarden.item("suspicious_potion"));var item=(CweGiftItem)potion.getItem();
    h.assertTrue(!item.canUse(c,potion),"A baby accepted the adult lifetime potion");var state=c.state();state.adult=true;state.happiness=80;
    h.assertTrue(item.canUse(c,potion),"A mortal adult refused the lifetime potion");item.apply(c,potion,null);
    h.assertTrue(state.intTrait("rare.parts")==0&&!state.immortal,"Potion invented rare animal-part permission");
    var transition=state.tick(1200,com.chaogarden.GardenConfig.ADULT_TICKS.get(),new java.util.Random(4));
    h.assertTrue(transition==ChaoState.Transition.REINCARNATED&&state.reincarnations==1,"Happy adult did not reincarnate at its forced lifetime boundary");
    state.adult=true;state.immortal=true;h.assertTrue(!item.canUse(c,potion),"Potion defeated an immortal Chao");h.succeed();
  }

}
