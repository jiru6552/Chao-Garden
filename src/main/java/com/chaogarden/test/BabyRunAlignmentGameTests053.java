package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual alignment handler, transport bounds and save boundaries.
 * Visual silhouette and UI drag acceptance belong to the real client study. */
@GameTestHolder("chao_baby53") @PrefixGameTestTemplate(false)
public final class BabyRunAlignmentGameTests053 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private static final String TEMPLATE="chao_baby53:activity_test";
  private BabyRunAlignmentGameTests053() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    for(int value:new int[]{-1000,-500,0,500,1000})
      add(tests,"creative_baby_alignment_"+(value+1000),40,h->accepted(h,value));
    for(String gate:List.of("survival","survival_forged_abilities","spectator","no_analyzer","far",
        "removed","state_dead","health_dead","cocoon","busy","construction","immortal_child"))
      add(tests,"reject_"+gate,40,h->deniedGate(h,gate));
    for(int value:new int[]{-1001,1001,Integer.MIN_VALUE,Integer.MAX_VALUE})
      add(tests,"reject_numeric_"+Integer.toUnsignedString(value),40,h->invalidValue(h,value));
    for(String identity:List.of("malformed","missing","wrong_type"))
      add(tests,"reject_identity_"+identity,40,h->invalidIdentity(h,identity));
    for(String access:List.of("foreign_owner","unowned","offhand_analyzer"))
      add(tests,"creative_existing_admin_policy_"+access,40,h->existingAdmin(h,access));
    for(int sign=-1;sign<=1;sign++) {
      int alignment=sign;
      add(tests,"adult_alignment_locked_"+(sign+1),40,h->adultLocked(h,alignment));
    }
    add(tests,"wire_codec_bounds_and_truncation",40,BabyRunAlignmentGameTests053::codec);
    add(tests,"same_value_is_noop",40,BabyRunAlignmentGameTests053::sameValue);
    for(int sign=-1;sign<=1;sign++)for(int quarter=0;quarter<=4;quarter++) {
      int alignment=sign,step=quarter;
      add(tests,"independent_baby_run_"+(sign+1)+"_"+(quarter*25),40,h->morphInputs(h,alignment,step));
    }
    add(tests,"alignment_survives_real_carrier_roundtrip",100,BabyRunAlignmentGameTests053::carrier);
    add(tests,"foreign_dimension_uuid_is_not_editable",40,BabyRunAlignmentGameTests053::foreignDimension);
    add(tests,"exact_eight_block_boundary_is_allowed",40,BabyRunAlignmentGameTests053::boundary);
    for(int index=0;index<legacyStates().size();index++) {
      int fixture=index;
      add(tests,"actual_prior_schema6_"+index,40,h->actualLegacy(h,fixture));
    }
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> body) {
    tests.add(new TestFunction("baby053_"+(tests.size()/8),name,TEMPLATE,timeout,0,true,body));
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo i) {}
      public void testPassed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r) {action.run();}
    });
  }
  private static ServerPlayer keeper(GameTestHelper h,GameType mode) {
    var p=TestConnections.makeSurvivalServerPlayer(h);p.setGameMode(mode);
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});
    return p;
  }
  private static ChaoState baby() {
    var s=new ChaoState();s.align=.125f;s.setEvolutionStrength(.4f);s.ageTicks=123;s.happiness=70;s.hunger=80;
    s.train(ChaoState.RUN,240,new Random(53));s.setTrait("baby53.foreign","preserve the earned record");
    return s;
  }
  private static ChaoEntity actor(GameTestHelper h,ChaoState state,ServerPlayer owner) {
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Actual Chao must create");
    c.setState(state);c.owner=owner.getUUID();c.setNoAi(true);c.setNoGravity(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);cleanup(h,c::discard);
    owner.teleportTo(c.getX()+1,c.getY(),c.getZ());return c;
  }
  private static boolean edit(ServerPlayer p,ChaoEntity c,int value) {
    return ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),value));
  }
  private static void near(GameTestHelper h,float actual,double expected,String why) {
    h.assertTrue(Math.abs(actual-expected)<.00001,why+": "+actual+" vs "+expected);
  }
  private static void accepted(GameTestHelper h,int value) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);var s=c.state();
    String before=s.encode();var expected=ChaoState.decode(before);expected.align=value/1000f;
    h.assertTrue(edit(p,c,value),"Creative holder must be able to set each Dark/Neutral/Hero endpoint");
    h.assertTrue(c.state().encode().equals(expected.encode()),"An accepted drag changes only baby alignment, not genes, stats, care, age or identity");
    h.assertTrue(p.getMainHandItem().is(ChaoGarden.item("chao_analyzer"))&&p.getMainHandItem().getCount()==1,
        "Dragging cannot consume or replace the Analyzer");
    near(h,HeroRunMorphology052.sample(s).babyRun(),.5,"Alignment cannot increase or erase the separate Run training target");
    near(h,HeroRunMorphology052.sample(s).babyHero(),value<=0?0:Math.min(1,value/500.0),"Hero care weight follows the accepted alignment");
    assertSyncAndNbt(h,c);h.succeed();
  }
  private static void deniedGate(GameTestHelper h,String gate) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);
    switch(gate) {
      case "survival"->p.setGameMode(GameType.SURVIVAL);
      case "survival_forged_abilities"->{p.setGameMode(GameType.SURVIVAL);p.getAbilities().instabuild=true;}
      case "spectator"->p.setGameMode(GameType.SPECTATOR);
      case "no_analyzer"->{p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.STICK));}
      case "far"->p.teleportTo(c.getX()+8.01,c.getY(),c.getZ());
      case "removed"->c.discard();
      case "state_dead"->c.state().dead=true;
      case "health_dead"->c.setHealth(0);
      case "cocoon"->c.state().cocoonTicks=200;
      case "busy"->c.getPersistentData().putBoolean("ChaoActivity",true);
      case "construction"->c.state().setTraitInt("content.construction",1);
      case "immortal_child"->c.state().immortal=true;
      default->throw new IllegalStateException(gate);
    }
    String before=c.state().encode();boolean changed=edit(p,c,900);String after=c.state().encode();
    c.getPersistentData().remove("ChaoActivity");
    h.assertFalse(changed,"Forged request must fail its authoritative gate: "+gate);
    h.assertTrue(before.equals(after),"Denied request must be a complete saved-state no-op: "+gate);
    h.succeed();
  }
  private static void invalidValue(GameTestHelper h,int value) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);String before=c.state().encode();
    h.assertFalse(edit(p,c,value),"Wire values outside[-1000,1000] must be rejected, not silently clamped");
    h.assertTrue(before.equals(c.state().encode()),"Invalid numerical payload cannot alter alignment or any other saved field");h.succeed();
  }
  private static void invalidIdentity(GameTestHelper h,String identity) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);String id;
    if(identity.equals("malformed"))id="not-a-uuid";
    else if(identity.equals("missing"))id=UUID.randomUUID().toString();
    else {
      var other=new ArmorStand(h.getLevel(),c.getX(),c.getY(),c.getZ());other.setNoGravity(true);
      h.getLevel().addFreshEntity(other);cleanup(h,other::discard);id=other.getUUID().toString();
    }
    String before=c.state().encode();
    h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(id,900)),"Malformed, stale and non-Chao identities cannot edit a nearby creature");
    h.assertTrue(before.equals(c.state().encode()),"Rejected identity cannot accidentally edit the nearest Chao");h.succeed();
  }
  private static void existingAdmin(GameTestHelper h,String access) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);
    if(access.equals("foreign_owner"))c.owner=UUID.randomUUID();
    else if(access.equals("unowned"))c.owner=null;
    else {p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));}
    h.assertTrue(edit(p,c,-750),"Creative admin/ownership policy and either-hand Analyzer access must remain consistent: "+access);
    near(h,c.state().align,-.75,"Accepted existing admin policy applies its exact value");assertSyncAndNbt(h,c);h.succeed();
  }
  private static void adultLocked(GameTestHelper h,int alignment) {
    var p=keeper(h,GameType.CREATIVE);var s=baby();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=.8f*alignment;
    s.adultAgeTicks=456;s.setEvolutionStrength(.8f);var c=actor(h,s,p);String before=s.encode();
    int value=alignment>0?-1000:1000;
    h.assertFalse(edit(p,c,value),"Creative alignment control must remain read-only for every adult identity");
    h.assertTrue(before.equals(s.encode()),"Adult rejection must preserve first alignment/type, maturity, stats and genes exactly");
    near(h,s.lockedAlignment(),.8*alignment,"Saved first-evolution alignment cannot cross sides");assertSyncAndNbt(h,c);
    var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.getLevel().addFreshEntity(restored);cleanup(h,restored::discard);
    h.assertFalse(edit(p,restored,value),"Adult lock survives actual entity NBT reload");
    h.assertTrue(before.equals(restored.state().encode()),"Reloaded adult remains the same locked first evolution");h.succeed();
  }
  private static void codec(GameTestHelper h) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);String before=c.state().encode();
    for(int value:new int[]{-1000,0,1000,Integer.MAX_VALUE}) {
      var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
      try {
        var input=new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),value);
        ChaoCareNetwork.AlignmentEdit.CODEC.encode(buffer,input);
        var decoded=ChaoCareNetwork.AlignmentEdit.CODEC.decode(buffer);
        h.assertTrue(decoded.equals(input)&&!buffer.isReadable(),"Typed UUID/integer packet roundtrips exactly with no hidden trailing fields");
        if(value==Integer.MAX_VALUE)h.assertFalse(ChaoCareNetwork.editAlignment(p,decoded),"A valid VarInt with invalid alignment still fails server authority");
      } finally {buffer.release();}
    }
    var tooLong=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());boolean bounded=false;
    try {ChaoCareNetwork.AlignmentEdit.CODEC.encode(tooLong,new ChaoCareNetwork.AlignmentEdit("x".repeat(37),500));}
    catch(RuntimeException expected){bounded=true;}finally{tooLong.release();}
    h.assertTrue(bounded,"Alignment transport bounds the identity string to36characters");
    var truncated=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());boolean rejected=false;
    try {truncated.writeUtf(c.getUUID().toString(),36);ChaoCareNetwork.AlignmentEdit.CODEC.decode(truncated);}
    catch(RuntimeException expected){rejected=true;}finally{truncated.release();}
    h.assertTrue(rejected,"Truncated payload cannot become a default alignment edit");
    h.assertTrue(before.equals(c.state().encode()),"Encoding/decoding malformed requests cannot modify saved state");h.succeed();
  }
  private static void sameValue(GameTestHelper h) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);String before=c.state().encode();
    h.assertFalse(edit(p,c,125),"Repeated drag sample of the current value is a no-op");
    h.assertTrue(before.equals(c.state().encode()),"No-op drag must preserve byte-exact saved state");h.succeed();
  }
  private static void morphInputs(GameTestHelper h,int alignment,int quarter) {
    var p=keeper(h,GameType.CREATIVE);var s=new ChaoState();s.align=alignment;s.setEvolutionStrength(.8f);
    s.train(ChaoState.RUN,120*quarter,new Random(53));var c=actor(h,s,p);String before=s.encode();
    var sample=HeroRunMorphology052.sample(s);
    h.assertTrue(sample.enabled()&&!sample.adult(),"Run baby must use the continuous path at Dark, Neutral and Hero alignment");
    near(h,sample.babyRun(),quarter*.25,"Run development is independent of Hero alignment");
    near(h,sample.babyHero(),alignment>0?1:0,"Hero remains a distinct alignment influence");
    h.assertTrue(before.equals(s.encode()),"Reading independent morph inputs cannot change saved state");assertSyncAndNbt(h,c);h.succeed();
  }
  private static void carrier(GameTestHelper h) {
    var p=keeper(h,GameType.CREATIVE);var s=baby();s.animalParts[ChaoState.ARMS]=6;s.animalParts[ChaoState.FEET]=6;
    s.animalPartsMask=1<<6;s.animalMask=1<<6;var c=actor(h,s,p);c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.SKELETON_SKULL));
    h.assertTrue(edit(p,c,875),"Creative baby alignment edit applies before real carrier capture");String encoded=s.encode();UUID uuid=c.getUUID();
    p.setGameMode(GameType.SURVIVAL);var stack=new ItemStack(ChaoGarden.item("chao_carrier"));
    h.assertTrue(ChaoCarrierItem.capture(stack,c,p)&&c.isRemoved(),"Actual carrier captures the edited baby, not a demonstration copy");
    var roundtrip=ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow();
    h.assertTrue(ChaoCarrierItem.state(roundtrip).encode().equals(encoded)&&ChaoCarrierItem.storedUUID(roundtrip).equals(uuid),
        "Inventory serialization preserves edited alignment and complete baby state");
    h.runAtTickTime(25,()->{
      h.assertTrue(ChaoCarrierItem.state(roundtrip).encode().equals(encoded),"Stored baby does not age or lose alignment while unloaded");
      p.setItemInHand(InteractionHand.MAIN_HAND,roundtrip);var floor=h.absolutePos(AT.below());
      var hit=new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false);
      h.assertTrue(roundtrip.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction()&&roundtrip.isEmpty(),"Real release consumes the carrier once");
      var restored=(ChaoEntity)h.getLevel().getEntity(uuid);h.assertTrue(restored!=null,"Release retains the same UUID");cleanup(h,restored::discard);
      h.assertTrue(restored.state().encode().equals(encoded)&&restored.getItemBySlot(EquipmentSlot.HEAD).is(Items.SKELETON_SKULL),
          "Release preserves alignment, independent Run training, genes and actual hat/animal parts");
      assertSyncAndNbt(h,restored);h.succeed();
    });
  }
  private static void foreignDimension(GameTestHelper h) {
    var p=keeper(h,GameType.CREATIVE);var local=actor(h,baby(),p);var otherLevel=p.server.getLevel(Level.NETHER);
    h.assertTrue(otherLevel!=null&&otherLevel!=h.getLevel(),"Actual other server dimension must exist for the authority boundary");
    var other=ChaoGarden.CHAO.get().create(otherLevel);other.setState(baby());other.owner=p.getUUID();other.setNoAi(true);other.setNoGravity(true);
    other.moveTo(local.getX(),64,local.getZ());otherLevel.getChunkAt(other.blockPosition());otherLevel.addFreshEntity(other);cleanup(h,other::discard);
    String before=other.state().encode();
    h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(other.getUUID().toString(),900)),"UUID requests resolve only the player's actual dimension");
    h.assertTrue(before.equals(other.state().encode()),"A foreign-dimension Chao cannot be edited through a nearby/local request");h.succeed();
  }
  private static void boundary(GameTestHelper h) {
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,baby(),p);p.teleportTo(c.getX()+8,c.getY(),c.getZ());
    h.assertTrue(edit(p,c,500),"Existing care distance includes exactlyeight blocks");near(h,c.state().align,.5,"Boundary-authorized edit uses the exact value");h.succeed();
  }
  private static void assertSyncAndNbt(GameTestHelper h,ChaoEntity c) {
    c.sync();String encoded=c.state().encode();var updates=c.getEntityData().getNonDefaultValues();
    h.assertTrue(updates!=null&&updates.stream().anyMatch(v->v.value() instanceof String value&&value.equals(encoded)),
        "Actual entity-data synchronization contains the authoritative complete alignment/morph inputs");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(updates);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(v->v.value() instanceof String value&&value.equals(encoded)),
        "An observer receives the same authoritative encoded state");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.getUUID().equals(c.getUUID())
        &&java.util.Objects.equals(restored.owner,c.owner),"Entity NBT roundtrip preserves alignment, UUID, owner and every unrelated saved field");
    near(h,HeroRunMorphology052.sample(restored.state()).babyRun(),HeroRunMorphology052.sample(c.state()).babyRun(),"Reloaded independent Run target is unchanged");
    observer.discard();restored.discard();
  }
  private record Legacy(String source,String sha256,String encoded) {}
  private static List<Legacy> legacyStates() {
    try(var stream=BabyRunAlignmentGameTests053.class.getResourceAsStream("/data/chao_baby53/legacy_states.json")) {
      if(stream==null)throw new IllegalStateException("Missing actual older-save fixtures");
      var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8));var result=new ArrayList<Legacy>();
      for(var entry:json.getAsJsonObject().getAsJsonArray("states")) {var value=entry.getAsJsonObject();result.add(new Legacy(value.get("source").getAsString(),value.get("sha256").getAsString(),value.get("encoded").getAsString()));}
      return result;
    }catch(Exception error){throw new IllegalStateException("Cannot read actual saved Chao records",error);}
  }
  private static void actualLegacy(GameTestHelper h,int index) {
    var old=legacyStates().get(index);
    try {h.assertTrue(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(old.encoded().getBytes(StandardCharsets.UTF_8))).equals(old.sha256()),"Actual saved fixture retains its extracted source hash");}
    catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    var state=ChaoState.decode(old.encoded());h.assertTrue(state.encode().equals(old.encoded()),"Older state is byte-exact before any intentional Creative edit: "+old.source());
    var p=keeper(h,GameType.CREATIVE);var c=actor(h,state,p);String before=state.encode();
    if(state.adult||state.immortal||state.dead) {
      h.assertFalse(edit(p,c,375),"Existing adult/Chaos/dead records cannot be unlocked by the Creative bar");
      h.assertTrue(before.equals(state.encode()),"Locked existing record retains byte-exact unrelated data");
    }else {
      var expected=ChaoState.decode(before);expected.align=.375f;
      h.assertTrue(edit(p,c,375),"Existing baby may receive an intentional Creative alignment edit");
      h.assertTrue(expected.encode().equals(state.encode()),"Only existing baby alignment changes, never saved care, genes, stats or maturity");
    }
    assertSyncAndNbt(h,c);h.succeed();
  }
}
