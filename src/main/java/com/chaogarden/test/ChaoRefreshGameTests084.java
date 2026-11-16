package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoRefreshCommands084;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real dispatcher, body/item NBT and actual Accessories projection boundaries.
 * Encoded observer metadata is not a claim of an actual second client. */
@GameTestHolder("chao_fixes84") @PrefixGameTestTemplate(false)
public final class ChaoRefreshGameTests084 {
  private static final String TEMPLATE="chao_fixes84:activity_test";
  private ChaoRefreshGameTests084() {}
  @GameTestGenerator public static Collection<TestFunction> tests(){return List.of(
      new TestFunction("refresh84_body","refresh_loaded_variants_preserves_complete_valid_state_and_equipment",TEMPLATE,100,0,true,ChaoRefreshGameTests084::body),
      new TestFunction("refresh84_selection","refresh_dispatcher_permissions_unique_names_nearest_and_reserved_name",TEMPLATE,100,0,true,ChaoRefreshGameTests084::selection),
      new TestFunction("refresh84_unavailable","refresh_rejects_cocoon_construction_and_missing_targets_atomically",TEMPLATE,100,0,true,ChaoRefreshGameTests084::unavailable),
      new TestFunction("refresh84_held","refresh_held_preserves_raw_state_components_and_rejects_duplicates",TEMPLATE,100,0,true,ChaoRefreshGameTests084::held),
      // Actual Accessories belongs to its existing optional-runtime namespace;
      // the four core refresh cases still execute in a minimal installation.
      new TestFunction("refresh84_equipped","refresh_actual_managed_body_commits_same_carrier_and_reload","chao_bonus80:activity_test",200,0,true,ChaoRefreshGameTests084::equipped));}
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static ServerPlayer player(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    var at=h.absolutePos(new BlockPos(3,2,3));p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);p.setNoGravity(true);
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});return p;
  }
  private static ChaoEntity actor(GameTestHelper h,ServerPlayer p,String name,double dx){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Registered Chao creates");
    var s=ChaoState.egg(4,true,true,0,new Random(8401));s.hunger=100;s.energy=100;s.ageTicks=12345;s.setEvolutionStrength(.43f);
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.moveTo(p.getX()+dx,p.getY(),p.getZ(),0,0);
    c.setCustomName(Component.literal(name));h.assertTrue(h.getLevel().addFreshEntity(c),"Real refresh target publishes");cleanup(h,c::discard);return c;
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){return 0;}
  }
  private static String quoted(ChaoEntity c){return "\""+c.getName().getString().replace("\\","\\\\").replace("\"","\\\"")+"\"";}
  private static void pinned(ChaoEntity c){c.setOfficialComparison(true);c.setSourceReferenceKey("dark_run");}
  private static CompoundTag save(ChaoEntity c){var tag=new CompoundTag();c.saveWithoutId(tag);return tag;}
  private static CompoundTag withoutAppearance(CompoundTag tag){var out=tag.copy();out.remove("OfficialComparison");out.remove("SourceReferenceRig");return out;}
  private static void refreshed(GameTestHelper h,ChaoEntity c,String encoded,CompoundTag before){
    h.assertTrue(!c.officialComparison()&&c.sourceReferenceKey().isEmpty(),"Official/reference overrides cleared");
    h.assertTrue(c.state().encode().equals(encoded),"All valid care, genes, stats, age, strength and locked identity bytes preserved");
    var after=save(c);h.assertTrue(withoutAppearance(after).equals(withoutAppearance(before)),"Full entity NBT differs only in the two appearance keys");
    var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(after);
    h.assertTrue(restored.getUUID().equals(c.getUUID())&&!restored.officialComparison()&&restored.sourceReferenceKey().isEmpty(),"Same identity and refreshed appearance persist through native entity reload");
    h.assertTrue(restored.state().encode().equals(encoded),"Reload retains exact current state");
    for(var slot:EquipmentSlot.values())h.assertTrue(ItemStack.matches(c.getItemBySlot(slot),restored.getItemBySlot(slot)),"Reload preserves actual equipment "+slot);
  }
  private static void body(GameTestHelper h){
    var p=player(h);var c=actor(h,p,"Refresh84 "+UUID.randomUUID(),1);
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("aviators_accessory")));
    c.getPersistentData().putInt("ChaoBabyAlignmentHeroTicks082",431);c.getPersistentData().putString("ForeignModState","keep");
    for(int alignment=-1;alignment<=1;alignment++)for(int type=0;type<=4;type++){
      var s=ChaoState.egg(4,true,true,0,new Random(8400+type));s.adult=true;s.align=alignment;s.primaryType=type;
      s.ageTicks=12345;s.adultAgeTicks=34567;s.setEvolutionStrength(.731f);s.energy=93;s.hunger=87;
      s.levels[ChaoState.RUN]=37;s.points[ChaoState.RUN]=2345;s.progress[ChaoState.RUN]=67;
      s.levels[ChaoState.POWER]=52;s.points[ChaoState.POWER]=3456;s.swimFly=-.4f;s.runPower=.3f;s.setTrait("refresh84.existing","retained");
      c.setState(ChaoState.decode(s.encode()));pinned(c);String before=c.state().encode();var nbt=save(c);
      h.assertTrue(command(p,"chao refresh "+quoted(c),2)==1,"Operator refresh accepts exact named adult family");refreshed(h,c,before,nbt);
    }
    for(int alignment=-1;alignment<=1;alignment++){
      var s=ChaoState.egg(3,true,false,0,new Random(8450));s.align=alignment;s.ageTicks=54321;s.setEvolutionStrength(.47f);
      c.setState(s);pinned(c);String before=c.state().encode();var nbt=save(c);
      h.assertTrue(command(p,"chao refresh ~",2)==1,"Operator nearest baby refresh succeeds");refreshed(h,c,before,nbt);
    }
    c.getEntityData().packDirty();String exact=c.state().encode();
    h.assertTrue(command(p,"chao refresh ~",2)==1,"Already-garden refresh remains valid");
    var forced=c.getEntityData().packDirty();h.assertTrue(forced!=null&&forced.stream().anyMatch(v->v.value() instanceof String value&&value.equals(exact)),"Unchanged current state is force-republished through native tracking metadata");
    h.succeed();
  }
  private static void selection(GameTestHelper h){
    var p=player(h);var c=actor(h,p,"Refresh Select "+UUID.randomUUID(),1);var other=actor(h,p,"Other "+UUID.randomUUID(),3);pinned(c);pinned(other);
    p.setGameMode(GameType.CREATIVE);
    h.assertTrue(command(p,"chao refresh "+quoted(c),0)==0&&ChaoRefreshCommands084.refresh(p.createCommandSourceStack().withPermission(0),c.getName().getString())==0&&c.officialComparison(),"Creative non-op cannot refresh through dispatcher or handler");p.setGameMode(GameType.SURVIVAL);
    h.assertTrue(command(p,"chao refresh "+quoted(c).toUpperCase(java.util.Locale.ROOT),2)==1&&!c.officialComparison()&&other.officialComparison(),"Case-insensitive unique name selects only intended loaded Chao");
    pinned(c);h.assertTrue(command(p,"chao refresh ~",2)==1&&!c.officialComparison()&&other.officialComparison(),"Nearest spherical selection selects actual closer target");
    pinned(c);other.setCustomName(c.getCustomName());h.assertTrue(command(p,"chao refresh "+quoted(c),2)==0&&c.officialComparison()&&other.officialComparison(),"Ambiguous names reject without choosing either creature");
    other.moveTo(p.getX()-1,p.getY(),p.getZ(),0,0);h.assertTrue(command(p,"chao refresh ~",2)==0&&c.officialComparison()&&other.officialComparison(),"Equally near Chao reject without silent selection");
    other.discard();c.setCustomName(Component.literal("held"));h.assertTrue(command(p,"chao refresh \"held\"",2)==1&&!c.officialComparison(),"Quoted reserved name held selects creature rather than held-item subcommand");
    c.discard();h.assertTrue(command(p,"chao refresh ~",2)==0,"No nearby actual Chao returns failure");h.succeed();
  }
  private static void unavailable(GameTestHelper h){
    var p=player(h);var c=actor(h,p,"Refresh Unavailable "+UUID.randomUUID(),1);pinned(c);
    h.assertTrue(command(p,"chao refresh",2)==0&&command(p,"chao refresh \"\"",2)==0&&command(p,"chao refresh \"Missing Unique "+UUID.randomUUID()+"\"",2)==0&&c.officialComparison(),"Missing target/name does not mutate a convenient nearby creature");
    c.state().cocoonTicks=20;String encoded=c.state().encode();h.assertTrue(command(p,"chao refresh "+quoted(c),2)==0&&c.state().encode().equals(encoded)&&c.officialComparison(),"Cocoon refresh rejects atomically");
    c.state().cocoonTicks=0;c.state().setTraitInt("content.construction",1);encoded=c.state().encode();h.assertTrue(command(p,"chao refresh "+quoted(c),2)==0&&c.state().encode().equals(encoded)&&c.officialComparison(),"Construction refresh rejects atomically");h.succeed();
  }
  private static void held(GameTestHelper h){
    var p=player(h);var c=actor(h,p,"Refresh Item "+UUID.randomUUID(),1);pinned(c);var snapshot=save(c);
    var stack=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.storeEntity(stack,snapshot);CompanionContent.token(stack,true);
    stack.set(DataComponents.CUSTOM_NAME,Component.literal("Exact saved carrier"));stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
    var outer=ChaoCarrierItem.data(stack);outer.putString("ForeignCarrierComponent","keep");ChaoCarrierItem.setData(stack,outer);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var exact=stack.copy();h.assertTrue(command(p,"chao refresh held",2)==0&&ItemStack.matches(stack,exact),"Held duplicate of actual live world body is rejected byte-for-byte");c.discard();
    h.assertTrue(command(p,"chao refresh held",0)==0&&ItemStack.matches(stack,exact),"Non-operator held refresh rejected");
    var expected=stack.copy();var updated=ChaoCarrierItem.entityData(expected);updated.putBoolean("OfficialComparison",false);updated.remove("SourceReferenceRig");ChaoCarrierItem.storeEntity(expected,updated);
    h.assertTrue(command(p,"chao refresh held",2)==1&&ItemStack.matches(stack,expected),"Held refresh changes only two saved appearance keys and no other item/entity component");
    h.assertTrue(ChaoCarrierItem.entityData(stack).getString("Chao").equals(snapshot.getString("Chao"))&&ChaoCarrierItem.storedUUID(stack).equals(c.getUUID()),"Raw encoded state and UUID retained verbatim");
    var reloaded=ItemStack.parseOptional(p.registryAccess(),(CompoundTag)stack.save(p.registryAccess()));h.assertTrue(ItemStack.matches(stack,reloaded),"Actual carrier component serialization retains the refreshed item");
    var foreign=ChaoCarrierItem.entityData(stack);foreign.putUUID("Keeper",UUID.randomUUID());ChaoCarrierItem.storeEntity(stack,foreign);exact=stack.copy();h.assertTrue(command(p,"chao refresh held",2)==0&&ItemStack.matches(stack,exact),"Existing keeper/recovery admission is not bypassed by operator held refresh");
    var missing=ChaoCarrierItem.entityData(stack);missing.remove("UUID");ChaoCarrierItem.storeEntity(stack,missing);exact=stack.copy();h.assertTrue(command(p,"chao refresh held",2)==0&&ItemStack.matches(stack,exact),"Malformed missing identity is retained rather than guessed/replaced");h.succeed();
  }
  private static void equipped(GameTestHelper h){
    h.assertTrue(AccessoriesBridge.available(),"Actual Accessories runtime is required; no skip counts as coverage");
    for(int x=0;x<10;x++)for(int z=0;z<10;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();var s=ChaoState.egg(4,true,true,0,new Random(8444));s.adult=true;s.align=1;s.primaryType=ChaoState.RUN_TYPE;s.hunger=100;s.energy=100;s.setEvolutionStrength(.65f);s.points[ChaoState.RUN]=800;
    var equipped=CompanionSkillGameTests.equip(h,ChaoState.decode(s.encode()),p);
    cleanup(h,()->{CompanionContent.unequip(p,equipped.inventory().getItem(0));equipped.inventory().setItem(0,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});
    h.startSequence().thenWaitUntil(()->h.assertTrue(CompanionContent.findEntity(p.server,equipped.chao()) instanceof ChaoEntity,"Waiting for real managed body"))
        .thenExecute(()->{
          var body=(ChaoEntity)CompanionContent.findEntity(p.server,equipped.chao());body.setCustomName(Component.literal("Refresh Equipped "+UUID.randomUUID()));pinned(body);CompanionContent.itemStateChanged(body);
          String before=body.state().encode();var uuid=body.getUUID();var outer=ChaoCarrierItem.data(equipped.inventory().getItem(0));
          h.assertTrue(command(p,"chao refresh "+quoted(body),2)==1&&!body.officialComparison()&&body.sourceReferenceKey().isEmpty(),"Actual loaded managed projection refreshes");
          var item=equipped.inventory().getItem(0);var saved=ChaoCarrierItem.entityData(item);
          h.assertTrue(body.state().encode().equals(before)&&saved.getString("Chao").equals(before)&&ChaoCarrierItem.storedUUID(item).equals(uuid),"Same managed body and exact current state immediately reach actual carrier");
          h.assertTrue(!saved.getBoolean("OfficialComparison")&&!saved.contains("SourceReferenceRig")&&CompanionContent.token(item,false).equals(outer.getUUID(CompanionContent.TOKEN)),"Actual saved appearance keys updated without replacing equipment token");
          var clone=item.copy();p.setItemInHand(InteractionHand.MAIN_HAND,clone);h.assertTrue(command(p,"chao refresh held",2)==0&&ItemStack.matches(clone,item),"Held copy cannot overwrite an actual equipped identity");
          var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);h.assertTrue(restored.getUUID().equals(uuid)&&restored.state().encode().equals(before)&&!restored.officialComparison()&&restored.sourceReferenceKey().isEmpty(),"Managed save reload preserves same state and current appearance");h.succeed();
        });
  }
}
