package com.chaogarden.toys;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Existing duck/ring IDs become two appearances of the same physical flotation aid. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class FloatToyContent080 {
  private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,ChaoGarden.MODID);
  public static final DeferredHolder<EntityType<?>,EntityType<FloatToyEntity080>> FLOAT=
      ENTITIES.register("chao_float",()->EntityType.Builder.<FloatToyEntity080>of(FloatToyEntity080::new,MobCategory.MISC)
          .sized(.98f,.68f).clientTrackingRange(10).updateInterval(1).build("chao_float"));
  private static final Set<UUID> PLACING=new HashSet<>(),REFUNDING=new HashSet<>();
  private static final Set<String> CONVERTING=new HashSet<>();
  private record Placement(ServerLevel level,AABB bounds){}
  private static final List<Placement> RESERVATIONS=new java.util.ArrayList<>();
  private static final Set<ChaoEntity> GOALS=Collections.newSetFromMap(new WeakHashMap<>());
  private static final String REFUND="ChaoFloatPlacementRefund080";
  private FloatToyContent080(){}
  public static void register(IEventBus bus){ENTITIES.register(bus);}
  public static boolean kind(String id){return id.equals("chao_lifesaver")||id.equals("chao_rubber_duck");}
  public static boolean supported(ChaoEntity c){return c.getVehicle() instanceof FloatToyEntity080 f&&f.committed();}
  public static FloatToyEntity080 vessel(ChaoEntity c){return c.getVehicle() instanceof FloatToyEntity080 f?f:null;}
  public static boolean pushing(ChaoEntity c){
    return !c.isPassenger()&&!c.level().getEntitiesOfClass(FloatToyEntity080.class,c.getBoundingBox().inflate(2),
        f->(f.phase()==FloatToyEntity080.PUSH||f.phase()==FloatToyEntity080.LAUNCH_REPOSITION)&&f.claimedBy(c)).isEmpty();
  }
  @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
    if(!event.getLevel().isClientSide&&event.getEntity() instanceof ChaoEntity c&&GOALS.add(c))
      // Activity/companion goals were registered first at0; this rescue/play
      // goal outranks the ordinary FloatGoal without changing that fallback.
      c.goalSelector.addGoal(0,new FloatToyGoal080(c));
  }
  /** Prevent the unsafe detachment itself, rather than relying on a remount
   * that another event listener could cancel. Real removal/unload/capture wins. */
  @SubscribeEvent public static void mount(EntityMountEvent event){
    if(event.getLevel().isClientSide||!(event.getEntityMounting() instanceof ChaoEntity c)||c.isRemoved()||!c.isAlive())return;
    if(event.isDismounting()&&event.getEntityBeingMounted() instanceof FloatToyEntity080 f&&!f.isRemoved()
        &&f.rider()==c&&!f.safeToDetach(c)){
      f.requestLand();event.setCanceled(true);
    }
    if(event.isMounting()&&c.getVehicle() instanceof FloatToyEntity080 old&&event.getEntityBeingMounted()!=old
        &&!old.isRemoved()&&!old.safeToDetach(c))event.setCanceled(true);
  }
  public static FloatToyEntity080 newCandidate(ServerLevel level,String kind,ItemStack payload,Vec3 position,float yaw){
    var entity=FLOAT.get().create(level);if(entity==null)return null;
    entity.configure(kind,payload);entity.moveTo(position.x,position.y,position.z,yaw,0);return entity;
  }
  /** Boat-style fluid ray placement, called by the existing BlockItem ID.
   * No item is consumed until it is reserved; a failed publication returns it. */
  public static InteractionResult place(Level level,Player player,InteractionHand hand,ItemStack source,String kind,
      Vec3 hit,BlockPos clicked,Direction face){
    if(!kind(kind)||player==null||source.isEmpty()||!player.mayUseItemAt(clicked,face,source)
        ||player.getEyePosition().distanceToSqr(hit)>36||!level.hasChunkAt(clicked)||!level.mayInteract(player,clicked))return InteractionResult.FAIL;
    if(level.isClientSide)return InteractionResult.SUCCESS;
    var server=(ServerLevel)level;
    if(PLACING.contains(player.getUUID())||player.getPersistentData().contains(REFUND))return InteractionResult.FAIL;
    Vec3 point=hit;Double water=FloatToyRoutes080.surfaceAt(server,hit);
    if(level.getFluidState(clicked).is(FluidTags.WATER)&&water!=null)point=new Vec3(hit.x,water-.05,hit.z);
    var candidate=newCandidate(server,kind,source.copyWithCount(1),point,player.getYRot());
    if(candidate==null)return InteractionResult.FAIL;
    var box=candidate.getBoundingBox();
    if(!server.getWorldBorder().isWithinBounds(box)||!server.hasChunkAt(BlockPos.containing(box.minX,box.minY,box.minZ))
        ||!server.hasChunkAt(BlockPos.containing(box.maxX,box.maxY,box.maxZ))||!server.noCollision(candidate,box)
        ||!server.getEntitiesOfClass(FloatToyEntity080.class,box.inflate(.005),f->!f.isRemoved()).isEmpty()
        ||RESERVATIONS.stream().anyMatch(r->r.level()==server&&r.bounds().intersects(box.inflate(.005)))){
      candidate.discardUncommitted();return InteractionResult.FAIL;
    }
    PLACING.add(player.getUUID());var reservation=new Placement(server,box.inflate(.005));RESERVATIONS.add(reservation);
    boolean spent=!player.getAbilities().instabuild;
    var reserved=spent?source.split(1):source.copyWithCount(1);
    try{
      if(!server.addFreshEntity(candidate)||candidate.isRemoved()){
        if(!candidate.isRemoved())candidate.discardUncommitted();
        if(spent)refund(player,hand,source,reserved);return InteractionResult.FAIL;
      }
      candidate.commit();server.gameEvent(player,GameEvent.ENTITY_PLACE,point);return InteractionResult.CONSUME;
    }finally{PLACING.remove(player.getUUID());RESERVATIONS.remove(reservation);}
  }
  private static void refund(Player player,InteractionHand hand,ItemStack original,ItemStack reserved){
    var current=player.getItemInHand(hand);
    if(current==original){original.grow(reserved.getCount());return;}
    if(current.isEmpty()&&original.isEmpty()){player.setItemInHand(hand,reserved.copy());return;}
    var retry=reserved.copy();if(player.getInventory().add(retry))return;
    // A canceled spawn callback may replace/fill the original hand. Retain the
    // one unresolved stack in saved player data, rather than lose it to another
    // cancelable drop publication. New placement waits until this refund clears.
    player.getPersistentData().put(REFUND,retry.save(player.registryAccess()));
  }
  @SubscribeEvent public static void refundTick(PlayerTickEvent.Post event){
    var player=event.getEntity();if(player.level().isClientSide||player.tickCount%20!=0||!player.getPersistentData().contains(REFUND)||!REFUNDING.add(player.getUUID()))return;
    try{
      var item=ItemStack.parse(player.registryAccess(),player.getPersistentData().getCompound(REFUND)).orElse(ItemStack.EMPTY);
      if(item.isEmpty()){player.getPersistentData().remove(REFUND);return;}
      if(player.getInventory().add(item))player.getPersistentData().remove(REFUND);
      else player.getPersistentData().put(REFUND,item.save(player.registryAccess()));
    }finally{REFUNDING.remove(player.getUUID());}
  }
  public static boolean legacyAvailable(ServerLevel level,BlockPos at,ChaoEntity candidate){
    if(!level.hasChunkAt(at))return false;var state=level.getBlockState(at);
    return state.getBlock() instanceof ChaoToyBlock toy&&kind(toy.toy)&&!state.getValue(ChaoToyBlock.PLAYING)
        &&ToyContent.available(level,at,candidate)&&!CONVERTING.contains(key(level,at));
  }
  /** Existing block and item IDs remain valid. Conversion publishes a hidden,
   * noncollectible candidate first; the real block is removed only on success. */
  public static FloatToyEntity080 convertLegacy(ServerLevel level,BlockPos at,ChaoEntity candidate){
    if(!legacyAvailable(level,at,candidate))return null;var old=level.getBlockState(at);var toy=(ChaoToyBlock)old.getBlock();
    String key=key(level,at);CONVERTING.add(key);
    var point=Vec3.atBottomCenterOf(at).add(0,old.getValue(ChaoToyBlock.WATERLOGGED)?.8:0,0);
    var entity=newCandidate(level,toy.toy,new ItemStack(ChaoGarden.item(toy.toy)),point,old.getValue(ChaoToyBlock.FACING).toYRot());
    try{
      if(entity==null||!level.addFreshEntity(entity)||entity.isRemoved()){
        if(entity!=null&&!entity.isRemoved())entity.discardUncommitted();return null;
      }
      // Reentrant callbacks cannot alter our reserved source through this API.
      if(!level.getBlockState(at).equals(old)){entity.discardUncommitted();return null;}
      BlockState replacement=old.getValue(ChaoToyBlock.WATERLOGGED)?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState();
      if(!level.setBlock(at,replacement,3)){entity.discardUncommitted();return null;}
      if(entity.isRemoved()){
        // A block-update callback may remove our still-uncommitted candidate.
        // Restore only our own unchanged replacement cell, never another edit.
        if(level.getBlockState(at).equals(replacement))level.setBlock(at,old,3);
        return null;
      }
      entity.commit();return entity;
    }finally{CONVERTING.remove(key);}
  }
  private static String key(ServerLevel level,BlockPos at){return level.dimension().location()+":"+at.asLong();}
  public static List<FloatToyEntity080> nearby(ChaoEntity c,double radius){return c.level().getEntitiesOfClass(FloatToyEntity080.class,c.getBoundingBox().inflate(radius,16,radius),f->f.committed()&&!f.isRemoved());}
}
