package com.chaogarden.test;

import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.EquippedRun084;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.EquippedAbilityRules080;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual slots, native flight input/travel and the real inbound position path.
 * Direct native travel is separate from real keyboard/TCP client evidence. */
@GameTestHolder("chao_run84") @PrefixGameTestTemplate(false)
public final class EquippedRunGameTests084 {
  private static final String TEMPLATE="chao_run84:activity_test";
  private static void near(GameTestHelper h,double a,double b,double epsilon,String message){h.assertTrue(Double.isFinite(a)&&Math.abs(a-b)<=epsilon,message+" actual="+a+" expected="+b);}
  private static CompanionSkillGameTests.Equipped equip(GameTestHelper h,int alignment){
    h.assertTrue(AccessoriesBridge.available(),"Real Accessories is required for these Run tests");
    var p=TestConnections.makeSurvivalServerPlayer(h);var s=new ChaoState();
    s.adult=true;s.align=alignment;s.primaryType=ChaoState.RUN_TYPE;s.energy=100;s.hunger=100;
    s.grades[ChaoState.RUN]=5;s.gradeA[ChaoState.RUN]=s.gradeB[ChaoState.RUN]=5;
    h.assertTrue(s.useHyperFruit(ChaoState.RUN),"Expressed X fixture uses the existing rank action");
    s.levels[ChaoState.RUN]=99;s.points[ChaoState.RUN]=9900;
    // Equal opposing values supply normal earned unlimited companion flight
    // without changing this adult's exact Run secondary or first identity.
    s.levels[ChaoState.FLY]=s.levels[ChaoState.SWIM]=99;
    var e=CompanionSkillGameTests.equip(h,ChaoState.decode(s.encode()),p);
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){clean();}
      public void testFailed(GameTestInfo i,GameTestRunner r){clean();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){clean();}
      private void clean(){CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);p.server.getPlayerList().remove(p);}
    });return e;
  }
  private static float nativeFlightSpeed(ServerPlayer p){
    try{var m=Player.class.getDeclaredMethod("getFlyingSpeed");m.setAccessible(true);return (float)m.invoke(p);}
    catch(ReflectiveOperationException error){throw new AssertionError(error);}
  }
  private static void acknowledge(ServerPlayer p){
    try{var f=p.connection.getClass().getDeclaredField("awaitingTeleport");f.setAccessible(true);
      p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(f.getInt(p.connection)));p.connection.resetPosition();}
    catch(ReflectiveOperationException error){throw new AssertionError(error);}
  }
  @GameTestGenerator public static Collection<TestFunction> tests(){
    var out=new ArrayList<TestFunction>();
    for(int alignment=-1;alignment<=1;alignment++){int value=alignment;out.add(new TestFunction("run084_authority","actual_100000percent_attribute_and_creative_input_alignment_"+(alignment+1),TEMPLATE,100,0,true,h->attribute(h,value)));}
    out.add(new TestFunction("run084_physics","creative_native_travel_scales_once_and_preserves_stored_abilities",TEMPLATE,100,0,true,EquippedRunGameTests084::creative));
    out.add(new TestFunction("run084_physics","companion_glide_scales_displacement_without_compounding_aero_velocity",TEMPLATE,100,0,true,EquippedRunGameTests084::glide));
    out.add(new TestFunction("run084_validation","actual_bounded_position_acceptance_rejection_and_revoke",TEMPLATE,100,0,true,EquippedRunGameTests084::validation));
    return out;
  }
  private static void attribute(GameTestHelper h,int alignment){
    var e=equip(h,alignment);h.runAfterDelay(12,()->{
      var p=e.keeper();var c=CompanionSkillGameTests.body(h,e);
      h.assertTrue(EquippedAbilityRules080.eligible(c.state(),ChaoState.RUN),"Earned flight balance keeps exact Run qualification");
      near(h,CompanionContent.secretRunBonus084(p),1000,0,"Current real slot owns +100000% Run bonus");
      near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED)/p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),1001,.0001,"Actual native attribute is1001x and not capped at default base");
      p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.setOnGround(false);p.setSprinting(false);
      float raw=p.getAbilities().getFlyingSpeed();near(h,nativeFlightSpeed(p),raw*1001,.00002,"Actual Player flight getter scales horizontal input");
      near(h,p.getAbilities().getFlyingSpeed(),raw,0,"Stored abilities remain the original value");
      c.state().grades[ChaoState.RUN]=5;c.sync();
      near(h,EquippedRun084.multiplier(p),1,0,"Server revokes flight authority immediately on rank loss");
      near(h,nativeFlightSpeed(p),raw,0,"Ordinary trained speed cannot leak into Creative flight");
      h.succeed();
    });
  }
  private static void creative(GameTestHelper h){
    var e=equip(h,0);h.runAfterDelay(12,()->{
      var p=e.keeper();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.setOnGround(false);p.setNoGravity(true);p.setSprinting(false);
      // A small custom base keeps both native paired moves inside the loaded
      // test arena. Real default-speed keyboard travel is a separate client run.
      p.getAbilities().setFlyingSpeed(.001f);var origin=p.position().add(0,4,0);p.teleportTo(origin.x,origin.y,origin.z);
      p.setDeltaMovement(Vec3.ZERO);p.travel(new Vec3(0,0,1));double boosted=p.position().subtract(origin).horizontalDistance();
      h.assertTrue(boosted>0,"Native Creative travel really moved");
      near(h,p.getAbilities().getFlyingSpeed(),.001f,0,"Native travel never changes the saved custom flySpeed");
      CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);
      p.teleportTo(origin.x,origin.y,origin.z);p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);p.travel(new Vec3(0,0,1));double ordinary=p.position().subtract(origin).horizontalDistance();
      h.assertTrue(ordinary>0,"Ordinary reference must exceed the native small-motion cutoff before comparing ratios");
      near(h,boosted/ordinary,1001,.02,"Paired actual Creative displacement has1001x input factor without velocity compounding");
      near(h,p.getAbilities().getFlyingSpeed(),.001f,0,"Unequip restores no guessed .05 default over custom data");h.succeed();
    });
  }
  private static void glide(GameTestHelper h){
    var e=equip(h,0);h.runAfterDelay(12,()->{
      var p=e.keeper();p.setNoGravity(true);p.setOnGround(false);p.setXRot(0);p.setYRot(0);p.startFallFlying();
      h.assertTrue(p.isFallFlying()&&com.chaogarden.companion.CompanionFlight.allowed(p),"Real earned companion flight is authorized without granting Creative flight");
      var origin=p.position().add(0,4,0);p.teleportTo(origin.x,origin.y,origin.z);p.setDeltaMovement(.001,0,0);
      p.travel(Vec3.ZERO);var moved=p.position().subtract(origin);var aero=p.getDeltaMovement();
      h.assertTrue(moved.length()>0&&aero.length()>.00001,"Actual glide move and aerodynamic velocity are nonzero");
      near(h,moved.length()/aero.length(),1001,.01,"Native fall-flying move scales only physical displacement");
      h.assertTrue(aero.length()<.002,"Stored aerodynamic velocity is not multiplied by1001");
      p.travel(Vec3.ZERO);h.assertTrue(p.getDeltaMovement().length()<.002,"Second native glide tick cannot exponentially compound speed");
      CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);
      p.startFallFlying();p.setOnGround(false);p.setDeltaMovement(.001,0,0);var before=p.position();p.travel(Vec3.ZERO);
      near(h,p.position().subtract(before).length()/p.getDeltaMovement().length(),1,.001,"Unequip immediately removes the physical glide multiplier");
      h.assertTrue(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Run never grants Creative flight permissions");h.succeed();
    });
  }
  private static void validation(GameTestHelper h){
    var e=equip(h,0);h.runAfterDelay(12,()->{
      var p=e.keeper();var at=p.position();p.teleportTo(at.x,h.getLevel().getMaxBuildHeight()-8,at.z);acknowledge(p);p.setDeltaMovement(Vec3.ZERO);
      var start=p.position();
      // Above templates, warm only a bounded40-block swept corridor, retaining
      // normal collision and the actual native movement-packet handler.
      for(int x=0;x<=40;x+=8)h.getLevel().getChunkAt(BlockPos.containing(start).offset(x,0,0));
      h.assertTrue(h.getLevel().noCollision(p,p.getBoundingBox().expandTowards(25,0,0)),"Movement probe has an ordinary clear loaded corridor");
      p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(start.x+20,start.y,start.z,false));
      near(h,p.getX(),start.x+20,.0001,"A valid current Run packet can exceed the old ten-block allowance");
      var diagnostic=EquippedRun084.movementCheck(p);h.assertTrue(diagnostic!=null&&diagnostic.multiplier()==1001&&diagnostic.authorized()>diagnostic.ordinary(),"Native expected-motion hook recorded current server-authenticated Run authority");
      var accepted=p.position();p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(start.x+100000,start.y,start.z,false));
      near(h,p.getX(),accepted.x,.0001,"Unbounded requested displacement still fails the original native movement check");
      acknowledge(p);CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);p.setDeltaMovement(Vec3.ZERO);p.connection.resetPosition();
      var base=p.position();p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(base.x+30,base.y,base.z,false));
      near(h,p.getX(),base.x,.0001,"Unequipped player no longer receives Run's movement allowance, even with up to five pending packets");
      h.assertTrue(EquippedRun084.movementCheck(p)==null,"Diagnostic authority is revoked with actual slot");h.succeed();
    });
  }
}
