package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEggEntity;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual entity names must enter both supported birth paths and survive the egg. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class FamilyNameGameTests {
  private static ChaoEntity parent(GameTestHelper h,String name,int x,long seed){
    h.setBlock(new BlockPos(x,1,2),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var state=ChaoState.random(new Random(seed));
    state.adult=true;state.happiness=100;c.setState(state);c.setNoAi(true);c.setNoGravity(true);
    c.setCustomName(Component.literal(name));c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(x,2,2))));
    h.getLevel().addFreshEntity(c);return c;
  }
  @GameTest(template="activity_test",timeoutTicks=40)
  public static void realNamedParentsAndGrandparentsSurviveEntityReload(GameTestHelper h){
    var maple=parent(h,"Maple",2,1);var pine=parent(h,"カオス 🌟",5,2);
    var child=(ChaoEntity)maple.getBreedOffspring(h.getLevel(),pine);
    h.assertTrue(child!=null,"The two adult parents could not create an offspring");
    h.assertTrue(child.state().familyName("family.parent.0").equals("Maple")
        &&child.state().familyName("family.parent.1").equals("カオス 🌟"),"The actual entity birth path omitted parent names");
    child.setCustomName(Component.literal("Willow"));child.state().adult=true;child.state().happiness=100;
    var fern=parent(h,"Fern",8,3);var descendant=(ChaoEntity)child.getBreedOffspring(h.getLevel(),fern);
    h.assertTrue(descendant!=null,"Second-generation actual parents could not create an offspring");
    var save=new CompoundTag();descendant.saveWithoutId(save);
    var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(save);
    h.assertTrue(restored.state().familyName("family.parent.0").equals("Willow")
        &&restored.state().familyName("family.parent.1").equals("Fern"),"Entity reload lost named parents");
    h.assertTrue(restored.state().familyName("family.grandparent.0").equals("Maple")
        &&restored.state().familyName("family.grandparent.1").equals("カオス 🌟"),"A later birth or reload lost the named grandparent snapshots");
    h.assertTrue(restored.state().trait("family.grandparent.0.id").equals(maple.state().lineageId()),"Display names replaced the stable lineage key");
    var spawnEggBaby=(ChaoEntity)maple.getBreedOffspring(h.getLevel(),maple);
    h.assertTrue(spawnEggBaby!=null&&spawnEggBaby.state().trait("family.parent.0.id").isEmpty(),"A vanilla spawn-egg self call invented a real family");
    maple.discard();pine.discard();fern.discard();child.discard();descendant.discard();restored.discard();spawnEggBaby.discard();h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=50)
  public static void courtshipEggCarriesNamesThroughItemEntityReloadAndHatch(GameTestHelper h){
    var maple=parent(h,"Maple",2,10);var pine=parent(h,"Pine",5,11);
    var keeper=h.makeMockPlayer(GameType.SURVIVAL);UUID owner=keeper.getUUID();maple.owner=pine.owner=owner;
    maple.setInLove(keeper);pine.setInLove(keeper);maple.spawnChildFromBreeding(h.getLevel(),pine);
    var area=new AABB(h.absolutePos(new BlockPos(3,2,2))).inflate(10);
    var eggs=h.getLevel().getEntitiesOfClass(ChaoEggEntity.class,area,e->owner.equals(e.owner));
    h.assertTrue(eggs.size()==1,"Actual courtship did not produce one physical egg");
    var egg=eggs.getFirst();
    var item=egg.snapshotItem();var itemState=ChaoState.decode(item.get(DataComponents.CUSTOM_DATA).copyTag().getString("Chao"));
    h.assertTrue(itemState.familyName("family.parent.0").equals("Maple")
        &&itemState.familyName("family.parent.1").equals("Pine"),"The real egg item snapshot omitted parent names");
    var save=new CompoundTag();egg.saveWithoutId(save);egg.discard();
    var restored=ChaoGarden.CHAO_EGG.get().create(h.getLevel());restored.load(save);h.getLevel().addFreshEntity(restored);
    restored.setIncubation(GardenConfig.HATCH_TICKS.get()-1);
    h.runAfterDelay(6,()->{
      var babies=h.getLevel().getEntitiesOfClass(ChaoEntity.class,area,c->owner.equals(c.owner)&&!c.state().adult);
      h.assertTrue(babies.size()==1,"The saved courtship egg did not hatch one child");
      var born=babies.getFirst();
      h.assertTrue(born.state().familyName("family.parent.0").equals("Maple")
          &&born.state().familyName("family.parent.1").equals("Pine"),"The hatch lost the actual parents' display names");
      h.assertTrue(born.state().trait("family.parent.0.id").equals(maple.state().lineageId()),"Hatching changed the family identity");
      maple.discard();pine.discard();born.discard();h.succeed();
    });
  }
}
