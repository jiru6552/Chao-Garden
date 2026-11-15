package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Regression for onboard player care after an actual engine-ray experiment
 * confirmed the original ring hull hid the rider. Uses GameRenderer's vanilla
 * entity-pick routine/body query, not AABB arithmetic alone. */
@GameTestHolder("chao_floatray80") @PrefixGameTestTemplate(false)
public final class FloatToyRayGameTests080 {
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(String kind:List.of("chao_lifesaver","chao_rubber_duck"))for(boolean adult:new boolean[]{false,true})
      out.add(new TestFunction("floatray080","float_rider_ray_"+kind+"_"+adult,"chao_float80:activity_test",100,0,true,h->probe(h,kind,adult)));
    return out;
  }
  private static void probe(GameTestHelper h,String kind,boolean adult){
    for(int x=1;x<=18;x++)for(int z=1;z<=18;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
      for(int y=2;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
      if(x>=8&&x<=14&&z>=4&&z<=14)h.setBlock(new BlockPos(x,1,z),Blocks.WATER);
    }
    var at=h.absolutePos(new BlockPos(8,2,8));var c=ChaoGarden.CHAO.get().create(h.getLevel());
    var state=new ChaoState();state.adult=adult;state.align=adult?1:0;state.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    state.energy=100;state.hunger=100;c.setState(state);c.moveTo(Vec3.atBottomCenterOf(at.west()));c.setOnGround(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.setNoAi(true);
    var f=FloatToyContent080.newCandidate(h.getLevel(),kind,new net.minecraft.world.item.ItemStack(ChaoGarden.item(kind)),Vec3.atBottomCenterOf(at).add(0,-.05,0),0);
    h.assertTrue(f!=null&&h.getLevel().addFreshEntity(f),"Publish real float");f.commit();f.previewHeld(true);h.assertTrue(f.board(c),"Board real local rider");
    var p=TestConnections.makeServerPlayer(h);p.teleportTo(at.getX()+6,at.getY(),at.getZ()+6);
    Runnable cleanup=()->{c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);p.server.getPlayerList().remove(p);};
    h.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,GameTestRunner r){cleanup.run();}public void testFailed(GameTestInfo i,GameTestRunner r){cleanup.run();}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){cleanup.run();}});
    h.runAfterDelay(34,()->{
      h.assertTrue(f.rider()==c&&f.phase()==FloatToyEntity080.RIDE,"Actual seat completed before care-ray probe");
      h.assertTrue(f.storedItem().getCount()==1&&Math.abs(f.getBbWidth()-.98)<.0001
          &&Math.abs(f.getBbHeight()-(kind.equals("chao_lifesaver")?.20:.68))<.0001,"One payload and correct appearance-specific actual collision height");
      var eye=c.getEyePosition();var forward=Vec3.directionFromRotation(0,f.getYRot());var side=new Vec3(-forward.z,0,forward.x);
      for(var direction:List.of(forward,side))for(double distance:new double[]{1.2,3.0})for(boolean levelEye:new boolean[]{true,false}){
        var from=eye.add(direction.scale(distance));if(!levelEye)from=new Vec3(from.x,at.getY()+p.getEyeHeight(),from.z);
        p.teleportTo(from.x,from.y-p.getEyeHeight(),from.z);var delta=eye.subtract(from);double max=delta.length();
        var hit=ProjectileUtil.getEntityHitResult(p,from,eye,p.getBoundingBox().expandTowards(delta).inflate(1),e->!e.isSpectator()&&e.isPickable(),max*max);
        String chosen=hit==null?"none":hit.getEntity()==c?"CHAO":hit.getEntity()==f?"FLOAT":hit.getEntity().getUUID().toString();
        com.mojang.logging.LogUtils.getLogger().info("FLOAT080_CARE_RAY kind={} adult={} dir={} distance={} levelEye={} from={} target={} actorBox={} floatBox={} chosen={} actor={} float={} hit={}",kind,adult,direction,distance,levelEye,from,eye,c.getBoundingBox(),f.getBoundingBox(),chosen,c.getUUID(),f.getUUID(),hit==null?"none":hit.getLocation());
        h.assertTrue(hit!=null&&hit.getEntity()==c,"Visible onboard Chao remains the real care-ray target: "+kind+" adult="+adult+" direction="+direction+" distance="+distance+" levelEye="+levelEye+" chosen="+chosen);
      }
      h.succeed();
    });
  }
}
