package com.chaogarden.care;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-owned kindergarten transactions and loaded-time lessons. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class ChaoCareServices {
  public record Lesson(String id,String name,int maximum) {}
  public static final List<Lesson> LESSONS=List.of(
      new Lesson("song","Song",5),new Lesson("drawing","Drawing",5),
      new Lesson("shake_dance","Shake dance",1),new Lesson("spin_dance","Spin dance",1),
      new Lesson("step_dance","Step dance",1),new Lesson("gogo_dance","Gogo dance",1),
      new Lesson("exercise","Exercise",1),new Lesson("bell","Bell",1),
      new Lesson("castanets","Castanets",1),new Lesson("cymbals","Cymbals",1),
      new Lesson("drum","Drum",1),new Lesson("flute","Flute",1),
      new Lesson("maracas","Maracas",1),new Lesson("tambourine","Tambourine",1),
      new Lesson("trumpet","Trumpet",1),new Lesson("triangle","Triangle",1),
      new Lesson("guitar","Guitar",1),new Lesson("accordion","Accordion",1));
  private static final String[] CYCLE={"song","shake_dance","drawing","bell","drum","song",
      "spin_dance","tambourine","drawing","gogo_dance","trumpet","song","maracas","step_dance",
      "drawing","castanets","flute","song","exercise","cymbals","drawing","triangle","guitar","accordion"};
  private static final Set<String> INSTRUMENTS=Set.of("bell","castanets","cymbals","drum","flute","maracas",
      "tambourine","trumpet","triangle","guitar","accordion");
  public static final EquipmentSlot[] WARDROBE={EquipmentSlot.HEAD,EquipmentSlot.FEET,
      EquipmentSlot.OFFHAND,EquipmentSlot.LEGS,EquipmentSlot.CHEST};
  public static final List<String> MEDALS=List.of("race_bronze","race_silver","race_gold","race_champion",
      "karate_bronze","karate_silver","karate_gold","karate_champion");
  private ChaoCareServices() {}
  public static Lesson lesson(String id) {return LESSONS.stream().filter(l->l.id().equals(id)).findFirst().orElse(null);}
  public static String currentLesson(long gameTime) {
    return CYCLE[(int)Math.floorMod(gameTime/Math.max(20,GardenConfig.LESSON_OFFER_TICKS.get()),CYCLE.length)];
  }
  public static int lessonOfferRemaining(long gameTime) {
    int interval=Math.max(20,GardenConfig.LESSON_OFFER_TICKS.get());return interval-(int)Math.floorMod(gameTime,interval);
  }
  public static int lessonLevel(ChaoState state,String id) {
    Lesson lesson=lesson(id);return lesson==null?0:Math.max(0,Math.min(lesson.maximum(),state.traitInt("learned."+id,0)));
  }
  public static boolean knowsLesson(ChaoEntity chao,String id) {
    return id.equals("break_dance")?allDancesKnown(chao.state()):id.equals("shovel")?
        chao.state().traitInt("reward.shovel",0)>0:lessonLevel(chao.state(),id)>0;
  }
  public static boolean allDancesKnown(ChaoState state) {
    return lessonLevel(state,"shake_dance")>0 && lessonLevel(state,"spin_dance")>0
        && lessonLevel(state,"step_dance")>0 && lessonLevel(state,"gogo_dance")>0;
  }
  public static void grantReward(ChaoEntity chao,String reward) {
    if(reward!=null && reward.matches("[a-z0-9_]{1,30}")) {
      chao.state().setTraitInt("reward."+reward,1);refresh(chao);
    }
  }
  public static boolean hasLesson(ChaoEntity chao) {
    return lesson(chao.state().trait("lesson.active"))!=null && chao.state().traitInt("lesson.remaining",0)>0;
  }
  public static boolean isLearning(ChaoEntity chao) {return hasLesson(chao) && chao.state().traitInt("lesson.paused",0)==0;}
  public static boolean canManage(Player keeper,ChaoEntity chao) {
    return keeper!=null && chao!=null && keeper.level()==chao.level() && chao.isAlive()
        && !chao.state().dead && chao.canCare(keeper) && keeper.distanceToSqr(chao)<=64;
  }
  private static boolean idle(ChaoEntity chao) {
    return !chao.isCocooned() && !chao.isHandlingItem() && !ActivityManager.isBusy(chao);
  }
  public static Optional<BlockPos> nearbyStation(Player player,ChaoEntity chao,ChaoSchoolBlock.Kind kind) {
    var origin=chao.blockPosition();
    for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-8,-3,-8),origin.offset(8,3,8))) {
      if(chao.level().getBlockState(pos).getBlock() instanceof ChaoSchoolBlock school && school.kind==kind
          && chao.distanceToSqr(pos.getCenter())<=64
          && (player==null || player.distanceToSqr(pos.getCenter())<=64))return Optional.of(pos.immutable());
    }
    return Optional.empty();
  }
  public static boolean beginLesson(Player keeper,ChaoEntity chao,String id) {
    Lesson lesson=lesson(id);
    if(keeper==null || chao==null || keeper.level()!=chao.level() || !chao.isAlive() || chao.state().dead
        || !keeper.getUUID().equals(chao.owner) || keeper.distanceToSqr(chao)>64
        || !com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao)
        || !idle(chao) || hasLesson(chao) || lesson==null
        || !currentLesson(chao.level().getGameTime()).equals(id) || CompanionContent.isManaged(chao)
        || chao.state().sleeping || chao.state().isIll() || chao.state().hunger<20
        || lessonLevel(chao.state(),id)>=lesson.maximum())return false;
    var desk=nearbyStation(keeper,chao,ChaoSchoolBlock.Kind.CLASSROOM);if(desk.isEmpty())return false;
    String key=Long.toString(desk.get().asLong());
    long occupied=chao.level().getEntitiesOfClass(ChaoEntity.class,new AABB(desk.get()).inflate(10),
        c->c.isAlive() && hasLesson(c) && c.state().trait("lesson.desk").equals(key)).size();
    if(occupied>=GardenConfig.CLASSROOM_CAPACITY.get())return false;
    var s=chao.state();
    if(!s.setTrait("lesson.active",id) || !s.setTraitInt("lesson.remaining",GardenConfig.LESSON_TICKS.get())
        || !s.setTrait("lesson.desk",key)) {cancelLesson(chao);return false;}
    s.setTraitInt("lesson.paused",0);if(chao.owner==null)chao.owner=keeper.getUUID();
    faceDesk(chao,desk.get());
    chao.startBehavior(ChaoEntity.Behavior.LEARN,GardenConfig.LESSON_TICKS.get());refresh(chao);return true;
  }
  public static void cancelLesson(ChaoEntity chao) {
    var s=chao.state();s.setTrait("lesson.active","");s.setTraitInt("lesson.remaining",0);
    s.setTrait("lesson.desk","");s.setTraitInt("lesson.paused",0);
    if(chao.getBehavior()==ChaoEntity.Behavior.LEARN)chao.clearBehavior();refresh(chao);
  }
  @SubscribeEvent public static void entityTick(EntityTickEvent.Post event) {
    if(event.getEntity() instanceof ChaoEntity chao && !chao.level().isClientSide)tickLearning(chao);
  }
  public static void tickLearning(ChaoEntity chao) {
    if(!hasLesson(chao))return;
    // Retire old freestanding lessons safely, retaining knowledge and earned rewards.
    if(!com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao)){cancelLesson(chao);return;}
    var s=chao.state();BlockPos desk;
    try{desk=BlockPos.of(Long.parseLong(s.trait("lesson.desk")));}
    catch(NumberFormatException invalid){cancelLesson(chao);return;}
    var block=chao.level().getBlockState(desk).getBlock();
    boolean allowed=chao.isAlive() && !s.dead && idle(chao) && !s.sleeping && !s.isIll()
        && s.hunger>=20 && !CompanionContent.isManaged(chao) && chao.distanceToSqr(desk.getCenter())<=64
        && block instanceof ChaoSchoolBlock school && school.kind==ChaoSchoolBlock.Kind.CLASSROOM;
    if(allowed) {
      var peers=chao.level().getEntitiesOfClass(ChaoEntity.class,new AABB(desk).inflate(10),
          c->c.isAlive() && hasLesson(c) && c.state().trait("lesson.desk").equals(s.trait("lesson.desk")))
          .stream().sorted(Comparator.comparing(c->c.getUUID().toString())).toList();
      allowed=peers.indexOf(chao)<GardenConfig.CLASSROOM_CAPACITY.get();
    }
    if(!allowed) {
      s.setTraitInt("lesson.paused",1);
      if(chao.getBehavior()==ChaoEntity.Behavior.LEARN)chao.clearBehavior();
      if(chao.tickCount%20==0)refresh(chao);return;
    }
    s.setTraitInt("lesson.paused",0);
    faceDesk(chao,desk);
    if(chao.getBehavior()!=ChaoEntity.Behavior.LEARN)
      chao.startBehavior(ChaoEntity.Behavior.LEARN,Math.min(24000,s.traitInt("lesson.remaining",0)));
    com.chaogarden.toys.InstrumentPerformance.tick(chao);
    int remaining=s.traitInt("lesson.remaining",0)-1;
    if(remaining>0) {s.setTraitInt("lesson.remaining",remaining);if(chao.tickCount%20==0)refresh(chao);return;}
    String id=s.trait("lesson.active");Lesson learned=lesson(id);int next=lessonLevel(s,id)+1;
    // Clear the pending transaction first, so a completed save can never replay it.
    cancelLesson(chao);
    if(learned!=null && s.setTraitInt("learned."+id,Math.min(learned.maximum(),next))
        && next==1 && INSTRUMENTS.contains(id))s.setTraitInt("lesson.reward."+id,1);
    if(chao.owner!=null && chao.level() instanceof ServerLevel server) {
      var keeper=server.getPlayerByUUID(chao.owner);if(keeper!=null)claimLessonRewards(keeper,chao);
    }
    s.boredom=Math.max(0,s.boredom-35);
    chao.playSound(SoundEvents.PLAYER_LEVELUP,.35f,1.6f);refresh(chao);
  }
  public static boolean cure(Player keeper,ChaoEntity chao) {
    if(!canManage(keeper,chao) || !idle(chao) || !chao.state().isIll()
        || nearbyStation(keeper,chao,ChaoSchoolBlock.Kind.DOCTOR).isEmpty())return false;
    int slot=findItem(keeper,ChaoGarden.item("chao_medicine"));if(slot<0 && !keeper.getAbilities().instabuild)return false;
    if(!keeper.getAbilities().instabuild)keeper.getInventory().getItem(slot).shrink(1);
    chao.state().cureAllIllnesses();chao.playSound(SoundEvents.BREWING_STAND_BREW,.3f,1.5f);refresh(chao);return true;
  }
  public static int wallet(Player player) {
    int total=0;for(int i=0;i<36;i++) {var s=player.getInventory().getItem(i);
      if(s.is(Items.EMERALD))total+=s.getCount();else if(s.is(Items.EMERALD_BLOCK))total+=s.getCount()*9;}
    return total;
  }
  public static boolean upgrade(Player keeper,ChaoEntity chao,int stat) {
    if(!canManage(keeper,chao) || !idle(chao) || stat<0 || stat>4
        || !chao.state().canUpgradeGrade(stat)
        || nearbyStation(keeper,chao,ChaoSchoolBlock.Kind.DOCTOR).isEmpty())return false;
    int cost=(chao.state().gradeUpgradeCostRings(stat)+99)/100;
    if(!keeper.getAbilities().instabuild && wallet(keeper)<cost)return false;
    // The trait budget can reject an upgrade; never charge before it commits.
    if(!chao.state().upgradeGrade(stat))return false;
    if(!keeper.getAbilities().instabuild)pay(keeper,cost);
    chao.playSound(SoundEvents.PLAYER_LEVELUP,.4f,1.4f);refresh(chao);return true;
  }
  private static void pay(Player player,int cost) {
    int remaining=cost;
    for(int i=0;i<36 && remaining>0;i++) {var s=player.getInventory().getItem(i);if(s.is(Items.EMERALD)) {
      int taken=Math.min(remaining,s.getCount());s.shrink(taken);remaining-=taken;}}
    for(int i=0;i<36 && remaining>0;i++) {var s=player.getInventory().getItem(i);if(s.is(Items.EMERALD_BLOCK)) {
      int taken=Math.min((remaining+8)/9,s.getCount());s.shrink(taken);remaining-=taken*9;}}
    if(remaining<0)give(player,new ItemStack(Items.EMERALD,-remaining));
    if(remaining>0)throw new IllegalStateException("Validated wallet changed during server action");
  }
  public static void handle(Player keeper,ChaoEntity chao,String action,String value) {
    if(!canManage(keeper,chao))return;
    var data=keeper.getPersistentData();long now=keeper.level().getGameTime();
    if(data.contains("ChaoCareLastAction") && now-data.getLong("ChaoCareLastAction")<4)return;
    data.putLong("ChaoCareLastAction",now);boolean done=false;
    switch(action) {
      case "cure" -> done=cure(keeper,chao);
      case "learn" -> done=beginLesson(keeper,chao,value);
      case "cancel" -> {if(hasLesson(chao) && !com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao)){cancelLesson(chao);done=true;}}
      case "upgrade" -> {try{done=upgrade(keeper,chao,Integer.parseInt(value));}catch(NumberFormatException ignored){}}
      case "equip" -> {try{done=equip(keeper,chao,Integer.parseInt(value));}catch(NumberFormatException ignored){}}
      case "remove" -> {try{done=remove(keeper,chao,EquipmentSlot.valueOf(value));}catch(IllegalArgumentException ignored){}}
      case "dye" -> {try{done=dye(keeper,chao,EquipmentSlot.valueOf(value));}catch(IllegalArgumentException ignored){}}
      case "medal" -> done=selectMedal(keeper,chao,value);
      case "claim" -> done=claimLessonRewards(keeper,chao);
      default -> {return;}
    }
    keeper.displayClientMessage(Component.literal(done?"Chao care updated.":"That care action is not available here or your Chao is not ready."),true);
    keeper.getInventory().setChanged();
  }
  private static int findItem(Player keeper,Item item) {
    for(int i=0;i<36;i++)if(keeper.getInventory().getItem(i).is(item))return i;return -1;
  }
  public static boolean wardrobeSlot(EquipmentSlot slot) {return Arrays.asList(WARDROBE).contains(slot);}
  public static EquipmentSlot accessorySlot(ItemStack item) {
    if(com.chaogarden.market.ChaoHats.isHat(item))return EquipmentSlot.HEAD;
    return com.chaogarden.cwe.CweCatalog.isAccessory(item)
        ?com.chaogarden.cwe.CweCatalog.slot(BuiltInRegistries.ITEM.getKey(item.getItem()).getPath()):null;
  }
  public static boolean equip(Player keeper,ChaoEntity chao,int index) {
    if(!canManage(keeper,chao) || !idle(chao) || index<0 || index>=36)return false;
    var source=keeper.getInventory().getItem(index);var slot=accessorySlot(source);if(slot==null)return false;
    return chao.offerWardrobeItem(keeper,source);
  }
  public static boolean remove(Player keeper,ChaoEntity chao,EquipmentSlot slot) {
    if(!canManage(keeper,chao) || !idle(chao) || !wardrobeSlot(slot))return false;
    if (!chao.removeWearable(keeper,slot)) return false;
    refresh(chao);return true;
  }
  public static boolean dye(Player keeper,ChaoEntity chao,EquipmentSlot slot) {
    if(!canManage(keeper,chao) || !idle(chao) || !wardrobeSlot(slot))return false;
    var stack=chao.getItemBySlot(slot);if(stack.isEmpty() || slot==EquipmentSlot.HEAD || accessorySlot(stack)!=slot)return false;
    if(!(stack.getItem() instanceof com.chaogarden.cwe.CweGiftItem gift) || !gift.gift.dyable())return false;
    int index=dyeIndex(keeper);
    if(index<0)return false;var source=keeper.getInventory().getItem(index);var dye=(DyeItem)source.getItem();
    var existing=stack.get(DataComponents.DYED_COLOR);
    if(existing!=null && existing.rgb()==dye.getDyeColor().getTextColor())return false;
    var changed=stack.copy();changed.set(DataComponents.DYED_COLOR,new DyedItemColor(dye.getDyeColor().getTextColor(),true));
    source.shrink(1);chao.setItemSlot(slot,changed);refresh(chao);return true;
  }
  public static int dyeIndex(Player keeper) {
    int selected=keeper.getInventory().selected;
    if(keeper.getInventory().getItem(selected).getItem() instanceof DyeItem)return selected;
    for(int i=0;i<36;i++)if(keeper.getInventory().getItem(i).getItem() instanceof DyeItem)return i;
    return -1;
  }
  public static boolean selectMedal(Player keeper,ChaoEntity chao,String medal) {
    if(!canManage(keeper,chao) || !idle(chao) || (!medal.isEmpty()
        && (!MEDALS.contains(medal) || chao.state().traitInt("medal."+medal,0)<=0)))return false;
    if(!chao.state().setTrait("medal.selected",medal))return false;refresh(chao);return true;
  }
  public static int pendingLessonRewards(ChaoState state) {
    int count=0;for(String id:INSTRUMENTS)if(state.traitInt("lesson.reward."+id,0)==1)count++;return count;
  }
  public static boolean claimLessonRewards(Player keeper,ChaoEntity chao) {
    if(!canManage(keeper,chao))return false;
    return commitLessonRewards(keeper,chao);
  }
  /** Commit reward markers before any inventory insertion/drop callback can reenter care. */
  public static List<ItemStack> settleLessonRewards(ChaoState state) {
    var rewards=new ArrayList<ItemStack>();
    for(String id:INSTRUMENTS)if(state.traitInt("lesson.reward."+id,0)==1) {
      var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"chao_"+id));
      if(item!=Items.AIR && state.setTraitInt("lesson.reward."+id,2))rewards.add(new ItemStack(item));
    }
    return rewards;
  }
  private static boolean commitLessonRewards(Player keeper,ChaoEntity chao) {
    var rewards=settleLessonRewards(chao.state());
    for(var reward:rewards)give(keeper,reward);
    if(!rewards.isEmpty())refresh(chao);return !rewards.isEmpty();
  }
  private static void give(Player keeper,ItemStack stack) {
    if(!keeper.addItem(stack)) {var dropped=keeper.drop(stack,false);
      if(dropped!=null)dropped.getPersistentData().putLong("ChaoRejectedUntil",keeper.level().getGameTime()+200);}
  }
  private static void faceDesk(ChaoEntity chao,BlockPos desk) {
    var at=desk.getCenter();double x=at.x-chao.getX(),z=at.z-chao.getZ();
    float yaw=(float)(Math.atan2(z,x)*180/Math.PI)-90;
    chao.setYRot(yaw);chao.yBodyRot=yaw;chao.setYHeadRot(yaw);
    chao.getLookControl().setLookAt(at.x,desk.getY()+.8,at.z,30,30);
  }
  private static void refresh(ChaoEntity chao) {chao.sync();CompanionContent.itemStateChanged(chao);}
}
