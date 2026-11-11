package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Production protected-Survival placement and voluntary toy-play fixture, development only. */
final class World025VisualSmokeCare {
  private static UUID child,egg;
  private static final BlockPos EGG_GROUND=new BlockPos(-1,79,5),CHILD_GROUND=new BlockPos(1,79,5),TOY_GROUND=new BlockPos(1,79,7);
  private static final Map<BlockPos,BlockState> OWN_PLACEMENTS=new LinkedHashMap<>();
  private World025VisualSmokeCare(){}
  static void prepare(ServerPlayer p,String key){
    var level=p.serverLevel();
    if(!level.dimension().equals(HubContent.GARDEN))throw new IllegalStateException("Garden proof must run in its actual dimension");
    if(p.gameMode.getGameModeForPlayer()!=net.minecraft.world.level.GameType.SURVIVAL)throw new IllegalStateException("Garden care must use its production protected Survival mode");
    if(key.equals("garden-egg-release")){
      p.getInventory().clearContent();
      place(p,EGG_GROUND,new ItemStack(ChaoGarden.item("normal_egg")));
      var eggs=level.getEntitiesOfClass(ChaoEggEntity.class,new net.minecraft.world.phys.AABB(EGG_GROUND).inflate(2));
      var created=eggs.stream().filter(e->p.getUUID().equals(e.owner)).findFirst().orElseThrow(()->new IllegalStateException("Real egg use did not place an owned egg"));
      created.getPersistentData().putBoolean(World025VisualSmoke.FIXTURE,true);egg=created.getUUID();
      var c=ChaoGarden.CHAO.get().create(level);if(c==null)throw new IllegalStateException("Cannot create release fixture");
      var state=new ChaoState();state.hunger=90;state.energy=100;state.boredom=100;state.happiness=80;state.personality=ChaoPreferences.EASILY_BORED;
      c.owner=p.getUUID();c.setState(state);c.setNoAi(true);c.getPersistentData().putBoolean(World025VisualSmoke.FIXTURE,true);
      var held=new ItemStack(ChaoGarden.item("chao_carrier"));if(!ChaoCarrierItem.capture(held,c,p))throw new IllegalStateException("Study capture failed");
      child=ChaoCarrierItem.storedUUID(held);place(p,CHILD_GROUND,held);
      if(!(level.getEntity(child) instanceof ChaoEntity released)||!held.isEmpty())throw new IllegalStateException("Actual carrier did not consume and release same Chao");
      LogUtils.getLogger().info("CHAO_WORLD025_GARDEN_CARE mode={} egg={} releasedChao={} heldConsumed=true",p.gameMode.getGameModeForPlayer(),egg,child);
    }else{
      // The placement proof is already saved. Retire only its tagged egg now
      // rather than leaving a future untagged hatchling/shell in this fixture.
      if(egg!=null&&level.getEntity(egg) instanceof ChaoEggEntity previous&&previous.getPersistentData().getBoolean(World025VisualSmoke.FIXTURE))previous.discard();
      egg=null;
      if(!(level.getEntity(child) instanceof ChaoEntity c))throw new IllegalStateException("Missing previously released garden Chao");
      var at=TOY_GROUND.above();OWN_PLACEMENTS.putIfAbsent(at,level.getBlockState(at));place(p,TOY_GROUND,new ItemStack(ChaoGarden.item("chao_tv")));
      if(!level.getBlockState(at).is(ToyContent.block("chao_tv"))||level.getBlockState(at).getValue(ChaoToyBlock.PLAYING))throw new IllegalStateException("Toy placement did not create exactly one inactive physical TV");
      c.setNoAi(false);c.getRandom().setSeed(18);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
      LogUtils.getLogger().info("CHAO_WORLD025_TOY_ADMISSION actualPlaced=true forcedPlay=false actor={} toy={}",child,at);
    }
  }
  private static void place(ServerPlayer p,BlockPos ground,ItemStack stack){
    p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();
    var hit=new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false);
    var result=p.gameMode.useItemOn(p,p.serverLevel(),stack,InteractionHand.MAIN_HAND,hit);
    if(!result.consumesAction())throw new IllegalStateException("Actual protected-Survival item admission refused "+stack+" at "+ground+": "+result);
    p.inventoryMenu.broadcastChanges();
  }
  static boolean clientReady(Minecraft mc,String key){
    if(child==null)return false;
    var c=mc.level.getEntitiesOfClass(ChaoEntity.class,new net.minecraft.world.phys.AABB(-8,78,-4,9,85,15),found->found.getUUID().equals(child)).stream().findFirst().orElse(null);
    if(c==null)return false;
    if(key.equals("garden-egg-release"))return egg!=null&&!mc.level.getEntitiesOfClass(ChaoEggEntity.class,new net.minecraft.world.phys.AABB(-8,78,-4,9,85,15),found->found.getUUID().equals(egg)).isEmpty();
    boolean playing=ToyContent.isPlaying(c)&&mc.level.getBlockState(TOY_GROUND.above()).is(ToyContent.block("chao_tv"))&&mc.level.getBlockState(TOY_GROUND.above()).getValue(ChaoToyBlock.PLAYING);
    if(playing&&c.tickCount%20==0)LogUtils.getLogger().info("CHAO_WORLD025_TOY_ACTIVE actor={} kind={} remaining={}",child,ToyContent.playingKind(c),ToyContent.playRemainingTicks(c));
    return playing;
  }
  static void diagnose(ServerPlayer p){
    if(p.tickCount%40!=0||child==null||!(p.serverLevel().getEntity(child) instanceof ChaoEntity c))return;
    var toy=p.serverLevel().getBlockState(TOY_GROUND.above());var path=c.getNavigation().getPath();
    BlockPos stand=toy.getBlock() instanceof ChaoToyBlock?TOY_GROUND.above().relative(toy.getValue(ChaoToyBlock.FACING)):TOY_GROUND;
    var candidate=c.getNavigation().createPath(stand,0);
    LogUtils.getLogger().info("CHAO_WORLD025_TOY_DIAG actor={} age={} pos={} velocity={} mayPlay={} noAi={} noGravity={} onGround={} stay={} behavior={} handling={} resting={} sleeping={} energy={} hunger={} boredom={} target={} pathEnd={} pathIndex={} canReachStand={} stand={} standAir={} standSupport={} facing={} nextPlay={} runningGoals={}",
      child,c.tickCount,c.position(),c.getDeltaMovement(),ToyContent.mayPlay(c),c.isNoAi(),c.isNoGravity(),c.onGround(),c.isStaying(),c.getBehavior(),c.getItemAction(),c.isResting(),c.state().sleeping,c.state().energy,c.state().hunger,c.state().boredom,
      c.getPersistentData().contains("ChaoToyTarget")?ToyContent.target(c):"none",path==null?"none":path.getEndNode(),path==null?-1:path.getNextNodeIndex(),candidate!=null&&candidate.canReach(),stand,
      p.level().getBlockState(stand).getCollisionShape(p.level(),stand).isEmpty(),p.level().getBlockState(stand.below()).isFaceSturdy(p.level(),stand.below(),Direction.UP),toy.getBlock() instanceof ChaoToyBlock?toy.getValue(ChaoToyBlock.FACING):"none",c.getPersistentData().getLong("ChaoToyNextPlay"),
      c.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getGoal().getClass().getSimpleName()).toList());
  }
  static void cleanup(MinecraftServer server){
    var level=server.getLevel(HubContent.GARDEN);
    if(level!=null)for(var entry:OWN_PLACEMENTS.entrySet())if(level.getBlockState(entry.getKey()).is(ToyContent.block("chao_tv")))level.setBlock(entry.getKey(),entry.getValue(),3);
    OWN_PLACEMENTS.clear();child=null;egg=null;
  }
}
