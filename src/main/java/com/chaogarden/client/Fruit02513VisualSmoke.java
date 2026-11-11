package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.*;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Development-only live action fixtures. Native AI, synced item actions and
 * ordinary item renderers are observed; no client action/pose state is forced. */
public final class Fruit02513VisualSmoke {
  private static final BlockPos ROOT=new BlockPos(0,95,5);
  private static final Map<BlockPos,BlockState> original=new LinkedHashMap<>();
  private static final Set<String> captured=java.util.concurrent.ConcurrentHashMap.newKeySet();
  private static final List<UUID> drops=new ArrayList<>();
  private static String key="";private static UUID actor;private static boolean requested;
  private static long traced=-1;private static int actorId;private static boolean audited;private static int frames;private static volatile int saved;private static volatile RuntimeException failure;
  public static void prepare(ServerPlayer p,String scene) {
    if(!key.isEmpty()) {
      if(actor!=null&&p.serverLevel().getEntity(actor) instanceof ChaoEntity previous) {
        if(key.endsWith("plant")&&(!captured.stream().anyMatch(c->c.startsWith(key+"-plant-"))||!p.serverLevel().getBlockState(ROOT).is(FruitContent.saplingForFruit("round_fruit")))
            ||key.endsWith("eat")&&(!captured.contains(key+"-eat-6")||!previous.getMainHandItem().isEmpty())
            ||key.endsWith("water")&&(!captured.contains(key+"-water-finish")||p.serverLevel().getBlockState(ROOT).getValue(ChaoSaplingBlock.STAGE)!=1))
          throw new IllegalStateException("02513 live scene did not finish: "+key);
        LogUtils.getLogger().info("CHAO_FRUIT02513_COMPLETE scene={} realActionObserved=true actualResult=true",key);previous.discard();
      }
      for(var id:drops)if(p.serverLevel().getEntity(id)!=null)p.serverLevel().getEntity(id).discard();drops.clear();
      original.forEach((at,state)->p.serverLevel().setBlock(at,state,3));original.clear();
      p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
    }
    key=scene.startsWith("fruit-use-")?scene:"";actor=null;requested=false;
    if(key.isEmpty())return;
    if(key.endsWith("finish")){key="";return;}
    if(key.contains("hands")) {
      p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item(key.endsWith("can")?"chao_watering_can":"garden_fruit")));
      p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ChaoGarden.item(key.endsWith("can")?"dark_garden_fruit":"hero_garden_fruit_seed")));
      return;
    }
    var level=p.serverLevel();
    for(var at:BlockPos.betweenClosed(ROOT.offset(-4,-1,-3),ROOT.offset(4,8,6))) {
      var pos=at.immutable();original.put(pos,level.getBlockState(pos));
      level.setBlock(pos,at.getY()==ROOT.getY()-1?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    }
    level.setBlock(ROOT.below(),Blocks.DIRT.defaultBlockState(),3);
    if(key.endsWith("drops")) {
      int i=0;for(String id:List.of("heart_fruit","dark_garden_fruit","round_fruit_seed","chao_watering_can","chao_shovel")) {
        var drop=new ItemEntity(level,-1.0+i++*.85,95.05,6,new ItemStack(ChaoGarden.item(id)));
        drop.setDeltaMovement(Vec3.ZERO);drop.setUnlimitedLifetime();drop.setNeverPickUp();level.addFreshEntity(drop);drops.add(drop.getUUID());
      }return;
    }
    var c=ChaoGarden.CHAO.get().create(level);var s=new ChaoState();s.hunger=key.endsWith("eat")?25:80;s.energy=90;
    s.points[ChaoState.RUN]=50;s.setTrait("reward.shovel",1);s.setTrait("reward.watering_can",1);
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.moveTo(.5,95, key.endsWith("plant")?8.5:6.5,180,0);
    c.yBodyRot=c.yBodyRotO=c.yHeadRot=c.yHeadRotO=180;
    level.addFreshEntity(c);actor=c.getUUID();actorId=c.getId();
  }
  private static void audit(Minecraft mc) {
    var ids=new ArrayList<>(FruitCatalog.registeredFoods());FruitCatalog.TREES.forEach(f->ids.add(f+"_seed"));ids.add("chao_watering_can");ids.add("chao_shovel");int contexts=0;
    for(String id:ids)for(var context:List.of(net.minecraft.world.item.ItemDisplayContext.GUI,net.minecraft.world.item.ItemDisplayContext.NONE,
        net.minecraft.world.item.ItemDisplayContext.GROUND,net.minecraft.world.item.ItemDisplayContext.FIXED,
        net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND,net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND,net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
      var stack=new ItemStack(ChaoGarden.item(id));var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
      var pose=new com.mojang.blaze3d.vertex.PoseStack();var selected=model.applyTransform(context,pose,context==net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      var quads=selected.getQuads(null,null,net.minecraft.util.RandomSource.create(13));
      if(quads.isEmpty()||!pose.last().pose().isFinite()||pose.last().pose().determinant()<=0||quads.stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))throw new IllegalStateException("02513 bad item context "+id+" "+context);
      contexts++;
    }
    audited=true;LogUtils.getLogger().info("CHAO_FRUIT02513_CONTEXTS items={} contexts={} missingMaterials=0 finitePositiveTransforms=true actualNativeBakedModels=true",ids.size(),contexts);
  }
  public static void tick(Minecraft mc,boolean begun) {
    if(!key.isEmpty()&&!audited)audit(mc);
    if(failure!=null)throw failure;
    if(!key.isEmpty()&&actor!=null&&begun) {
      UUID traceActor=actor;
      mc.getSingleplayerServer().execute(()->{
        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if(p.serverLevel().getEntity(traceActor) instanceof ChaoEntity c&&c.tickCount%20==0&&traced!=p.level().getGameTime()) {
          traced=p.level().getGameTime();LogUtils.getLogger().info("CHAO_FRUIT02513_TRACE scene={} pos={} action={} ticks={} path={} speed={} behavior={}",key,c.position(),c.getItemAction(),c.getItemActionTicks(),c.getNavigation().getPath(),c.getSpeed(),c.getBehavior());
        }
      });
    }
    if(key.isEmpty()||!begun||requested||actor==null)return;requested=true;String scene=key;UUID id=actor;
    mc.getSingleplayerServer().execute(()->{
      var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
      var c=(ChaoEntity)p.serverLevel().getEntity(id);
      if(scene.endsWith("water")) {
        p.serverLevel().setBlock(ROOT,FruitContent.saplingForFruit("round_fruit").defaultBlockState(),3);
        GardenTreeData.get(p.serverLevel()).planted(ROOT,"round_fruit");c.setNoAi(false);
      }else {
        var stack=new ItemStack(ChaoGarden.item(scene.endsWith("plant")?"round_fruit_seed":"hero_garden_fruit"));
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);c.offerItem(p,InteractionHand.MAIN_HAND);
        if(!stack.isEmpty())failure=new IllegalStateException("02513 fixture offer did not transfer a single real item");
        if(scene.endsWith("plant"))c.setNoAi(false);
      }
      LogUtils.getLogger().info("CHAO_FRUIT02513_START scene={} actor={} realOfferOrIdleAI=true",scene,id);
    });
  }
  public static void frame(Minecraft mc) {
    if(failure!=null)throw failure;
    if(key.isEmpty()||actor==null||!requested||!(mc.level.getEntity(actorId) instanceof ChaoEntity c))return;
    String phase="";float progress=0;
    if(c.getItemAction()==ChaoEntity.ItemAction.GARDEN&&c.walkAnimation.speed()>.01f)phase="walk";
    if(c.getItemAction()==ChaoEntity.ItemAction.PLANT||c.getItemAction()==ChaoEntity.ItemAction.EAT) {
      progress=c.getItemActionProgress(0);phase=c.getItemAction().name().toLowerCase()+"-"+(int)(progress*12);
    }
    if(c.getBehavior()==ChaoEntity.Behavior.WATER) {
      progress=c.getBehaviorProgress(0);phase="water-"+(progress<.15f?"raise":progress<.45f?"pour":progress<.75f?"hold":progress<.9f?"lower":"finish");
    }
    if(phase.isEmpty()||!captured.add(key+"-"+phase))return;
    var renderer=mc.getEntityRenderDispatcher().getRenderer(c);
    if(renderer instanceof ChaoRenderer cr&&!cr.getModel().officialPosesFinite())throw new IllegalStateException("02513 non-finite live pose");
    String name=key+"-"+phase+".png";frames++;
    LogUtils.getLogger().info("CHAO_FRUIT02513_FRAME scene={} phase={} progress={} item={} movement={} actualClientState=true",key,phase,progress,c.getHeldChaoItem(),c.walkAnimation.speed());
    Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{
      if(!(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)||!t.getKey().equals("screenshot.success"))failure=new IllegalStateException(message.getString());
      else{saved++;LogUtils.getLogger().info("CHAO_FRUIT02513_SAVED file={} frames={} saved={}",name,frames,saved);}
    });
  }
}
