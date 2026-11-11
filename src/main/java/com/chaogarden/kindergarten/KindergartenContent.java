package com.chaogarden.kindergarten;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.market.BlackMarketTrader;
import com.chaogarden.market.MarketContent;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Physical, server-authorized Kindergarten services. A classroom ledger owns each boarded Chao. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class KindergartenContent {
  public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"kindergarten"));
  public static final String STUDENT="ChaoKindergartenStudent",RESIDENT="ChaoKindergartenResident";
  public static final BlockPos MARKET_ANCHOR=new BlockPos(-22,80,5);
  public enum Role {
    TEACHER("Classroom",new BlockPos(-18,80,-15),1),
    DOCTOR("Chao Doctor",new BlockPos(18,80,-15),2),
    PRINCIPAL("Principal",new BlockPos(0,80,-21),4),
    FORTUNE("Fortune-Teller",new BlockPos(22,80,5),8),
    BOARD("Bulletin Board",new BlockPos(-7,80,0),5);
    public final String title;public final BlockPos anchor;public final int color;
    Role(String title,BlockPos anchor,int color){this.title=title;this.anchor=anchor;this.color=color;}
  }
  private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,ChaoGarden.MODID);
  public static final DeferredHolder<EntityType<?>,EntityType<KindergartenNpc>> NPC=ENTITIES.register("kindergarten_staff",()->EntityType.Builder
      .<KindergartenNpc>of(KindergartenNpc::new,MobCategory.CREATURE).sized(.6f,.95f).eyeHeight(.7f).clientTrackingRange(10).build("kindergarten_staff"));
  public static final DeferredHolder<EntityType<?>,EntityType<KindergartenPupil0257>> PUPIL=ENTITIES.register("kindergarten_pupil",()->EntityType.Builder
      .<KindergartenPupil0257>of(KindergartenPupil0257::new,MobCategory.CREATURE).sized(.6f,.85f).eyeHeight(.6f).clientTrackingRange(10).build("kindergarten_pupil"));
  private static final Set<UUID> TRANSACTIONS=new HashSet<>();
  private static final Map<UUID,Session> SESSIONS=new HashMap<>();
  private static final net.neoforged.neoforge.common.world.chunk.TicketController CLASSROOM_TICKETS = new net.neoforged.neoforge.common.world.chunk.TicketController(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"classroom_students"));
  private static final net.neoforged.neoforge.common.world.chunk.TicketController RESIDENT_TICKETS = new net.neoforged.neoforge.common.world.chunk.TicketController(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"kindergarten_residents"),(level,helper)->{
    // Visitor tickets are transient. A crash must not turn an empty lobby into permanent residency.
    helper.getBlockTickets().keySet().forEach(helper::removeAllTickets);helper.getEntityTickets().keySet().forEach(helper::removeAllTickets);
  });
  private static final BlockPos RESIDENT_TICKET_OWNER=new BlockPos(0,80,0);
  private static final int[][] RESIDENT_CHUNKS={{-2,-1},{1,-1},{0,-2},{1,0},{-1,0},{-2,0},{0,0}};
  private static final String[] NAMES={"Buddy","Bubbles","Chacha","Chai","Chappy","Echo","Honey","Melody","Pebbles","Quartz","Star","Tiny"};
  private static final class Session {
    final int npc;final UUID nonce=UUID.randomUUID();long opened;
    long lastAction=Long.MIN_VALUE,lastRead=Long.MIN_VALUE,lastSelect=Long.MIN_VALUE;String selected="",proposal="";int topic=-1,tipPage;
    Session(int npc,long opened){this.npc=npc;this.opened=opened;}
  }
  private record Choice(String key,String name,ChaoState state,ItemStack stack,ChaoEntity body,KindergartenRoster.Student student){}
  private KindergartenContent(){}
  public static void register(IEventBus bus){ENTITIES.register(bus);KindergartenNetwork.register(bus);bus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent e)->{e.register(CLASSROOM_TICKETS);e.register(RESIDENT_TICKETS);} );bus.addListener((EntityAttributeCreationEvent e)->{e.put(NPC.get(),ChaoEntity.attributes().build());e.put(PUPIL.get(),ChaoEntity.attributes().build());});}
  public static boolean inKindergarten(Level level){return level.dimension().equals(DIMENSION);}
  public static boolean isResident(ChaoEntity chao){return chao instanceof KindergartenNpc||chao instanceof KindergartenPupil0257;}
  public static boolean isBoarded(ChaoEntity chao){
    if(!(chao.level() instanceof ServerLevel level)||!inKindergarten(level)||!chao.getPersistentData().contains(STUDENT))return false;
    var student=KindergartenRoster.get(level.getServer()).students.get(chao.getUUID());
    return student!=null&&student.keeper.equals(chao.owner);
  }
  /** This guard pauses garden aging/care, while the existing post-tick lesson worker still runs. */
  public static boolean tickStudent(ChaoEntity chao){
    if(!isBoarded(chao))return false;
    var student=KindergartenRoster.get(((ServerLevel)chao.level()).getServer()).students.get(chao.getUUID());
    chao.getNavigation().stop();chao.setDeltaMovement(Vec3.ZERO);chao.setNoAi(true);chao.setNoGravity(true);chao.setStaying(true);
    Vec3 seat=seat(student.seat);chao.setPos(seat);return true;
  }
  public static Vec3 seat(int index){return new Vec3(-20.5+(index%2)*2,80,-14.5+(index/2)*2);}
  public static void ensureServices(ServerLevel level){
    if(!inKindergarten(level))return;
    updateResidentTickets(level,!level.players().isEmpty());
    KindergartenHallDisplays0252.ensure(level);
    for(Role role:Role.values()){
      level.getChunkAt(role.anchor);
      // Entity-region loading is asynchronous. Never replace a saved resident before its chunk
      // has finished loading, or reentry could reroll market stock and duplicate staff.
      if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(role.anchor)))continue;
      var matches=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(5),n->n.role()==role&&n.isAlive());
      // The0.25.5 notice panel moves eight blocks within the same loaded chunk.
      // Reanchor the existing invisible service actor rather than creating a
      // duplicate UUID or losing the board service at its new physical panel.
      if(role==Role.BOARD)for(var actor:level.getAllEntities())
        if(actor instanceof KindergartenNpc npc&&npc.role()==Role.BOARD&&npc.isAlive()&&!matches.contains(npc))matches.add(npc);
      if(!matches.isEmpty()){
        var expected=residentId(role.name());matches.sort(Comparator.comparing((KindergartenNpc n)->!n.getUUID().equals(expected)).thenComparing(n->n.getUUID().toString()));
        var retained=matches.getFirst();for(int i=1;i<matches.size();i++)matches.get(i).discard();
        retained.setNoAi(true);retained.setNoGravity(true);retained.setInvulnerable(true);retained.setInvisible(role==Role.BOARD);retained.setCustomNameVisible(role!=Role.BOARD);
        retained.setPos(role.anchor.getX()+.5,role.anchor.getY()+staffLift(role),role.anchor.getZ()+.5);
        float facing=role==Role.FORTUNE?90:0;retained.setYRot(facing);retained.yBodyRot=facing;retained.setYHeadRot(facing);
        styleStaff(retained);continue;
      }
      var npc=NPC.get().create(level);if(npc==null)continue;
      npc.setUUID(residentId(role.name()));
      npc.role(role);var state=ChaoState.egg(role.color,false,false,0,new Random(240+role.ordinal()));state.adult=true;state.happiness=80;
      npc.setState(state);npc.setCustomName(Component.literal(role.title));npc.setCustomNameVisible(role!=Role.BOARD);if(role==Role.BOARD)npc.setInvisible(true);
      npc.moveTo(role.anchor.getX()+.5,role.anchor.getY()+staffLift(role),role.anchor.getZ()+.5,role==Role.FORTUNE?90:0,0);
      npc.setInvulnerable(true);npc.setNoGravity(true);npc.getPersistentData().putBoolean(RESIDENT,true);
      styleStaff(npc);
      level.addFreshEntity(npc);
    }
    BlockPos market=MARKET_ANCHOR;level.getChunkAt(market);
    // A saved legacy resident may be in a neighboring service chunk. Wait for
    // the whole ticketed campus to be published and ticking before deciding
    // it is absent; LOADED alone can still mean hidden during chunk unloading.
    for(int[] chunk:RESIDENT_CHUNKS)if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(chunk[0],chunk[1]))
        ||!level.isPositionEntityTicking(new BlockPos(chunk[0]*16+8,80,chunk[1]*16+8)))return;
    // An older resident may have drifted/fallen outside the former five-block query.
    // Recover that same loaded actor and its purchased stock before creating another.
    KindergartenPupils0257.ensure(level);
    var merchants=new ArrayList<BlackMarketTrader>();
    for(var entity:level.getAllEntities())if(entity instanceof BlackMarketTrader trader&&trader.isAlive()&&trader.resident())merchants.add(trader);
    if(!merchants.isEmpty()){
      var expected=residentId("market");
      // Development/pre-release saves may contain duplicate fixed residents. Preserve an active
      // customer first, then demonstrably purchased stock, before preferring the stable new UUID.
      merchants.sort(Comparator.comparing((BlackMarketTrader t)->!t.isTrading()).thenComparing(Comparator.comparingLong(KindergartenContent::purchases).reversed())
          .thenComparing(t->!t.getUUID().equals(expected)).thenComparing(t->t.getUUID().toString()));
      var retained=merchants.getFirst();placeMarketResident(retained);
      for(int i=1;i<merchants.size();i++){var duplicate=merchants.get(i);if(duplicate.getTradingPlayer() instanceof ServerPlayer customer)customer.closeContainer();duplicate.setTradingPlayer(null);duplicate.discard();}
    }else{
      var trader=MarketContent.TRADER.get().create(level);if(trader!=null){
        trader.setUUID(residentId("market"));
        placeMarketResident(trader);level.addFreshEntity(trader);
      }
    }
  }
  private static void placeMarketResident(BlackMarketTrader trader){
    trader.setResident(true);trader.setNoAi(true);trader.setNoGravity(true);trader.setInvulnerable(true);trader.setPersistenceRequired();
    trader.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);trader.setInvisible(false);
    trader.setDeltaMovement(Vec3.ZERO);trader.moveTo(MARKET_ANCHOR.getX()+.5,MARKET_ANCHOR.getY(),MARKET_ANCHOR.getZ()+.5,-90,0);
    trader.yBodyRot=-90;trader.setYHeadRot(-90);
    trader.setCustomName(Component.literal("Black Market"));trader.setCustomNameVisible(false);
  }
  private static double staffLift(Role role){return role==Role.PRINCIPAL?.5:role==Role.DOCTOR||role==Role.FORTUNE?.4:0;}
  private static void styleStaff(KindergartenNpc npc){
    if(npc.getPersistentData().getInt("KindergartenAppearanceVersion")>=25)return;
    var state=ChaoState.egg(0,false,false,0,new Random(250+ npc.role().ordinal()));state.adult=true;state.happiness=80;npc.setState(state);
    for(var slot:EquipmentSlot.values())if(!npc.getItemBySlot(slot).isEmpty())npc.setItemSlot(slot,ItemStack.EMPTY);
    npc.getPersistentData().putInt("KindergartenAppearanceVersion",25);
  }
  private static UUID residentId(String role){return UUID.nameUUIDFromBytes((ChaoGarden.MODID+":kindergarten:"+role).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
  private static long purchases(BlackMarketTrader trader){
    var saved=new CompoundTag();trader.saveWithoutId(saved);long used=0;var offers=saved.getCompound("Offers").getList("Recipes",10);
    for(int i=0;i<offers.size();i++)used+=Math.max(0,offers.getCompound(i).getInt("uses"));return used;
  }
  /** Mob.interact handles name tags/leads before mobInteract, so protect residents and students at the generic boundary. */
  @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void protectedInteraction(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event){
    if(!(event.getTarget() instanceof ChaoEntity chao)||!isResident(chao)&&!isBoarded(chao))return;
    if(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&event.getEntity() instanceof ServerPlayer keeper){
      if(chao instanceof KindergartenNpc npc)open(keeper,npc,"");
      else keeper.displayClientMessage(Component.literal(chao instanceof KindergartenPupil0257?"These pupils belong to the school.":"Speak to the teacher to collect this student after class."),true);
    }
    event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
  }
  /** The decorated vanilla notice panel is the board's physical interaction surface. */
  @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOW,receiveCanceled=true)
  public static void board(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
    if(event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
        || !inKindergarten(player.level()))return;
    var pos=event.getPos();if(!pos.equals(KindergartenRooms025.BOARD_PANEL)
        ||!player.level().getBlockState(pos).is(KindergartenDecor025.block("school_noticeboard")))return;
    var residents=player.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(Role.BOARD.anchor).inflate(4),n->n.role()==Role.BOARD);
    if(!residents.isEmpty()){open(player,residents.getFirst(),"");event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}
  }
  private static boolean authorized(ServerPlayer player,KindergartenNpc npc){
    return player.isAlive()&&!player.isSpectator()&&inKindergarten(player.level())&&npc.level()==player.level()&&npc.isAlive()
        &&player.distanceToSqr(npc)<=64&&player.containerMenu==player.inventoryMenu&&player.inventoryMenu.getCarried().isEmpty();
  }
  public static void open(ServerPlayer player,KindergartenNpc npc,String selected){
    if(!authorized(player,npc))return;
    npc.triggerServiceGesture(switch(npc.role()){case TEACHER->1;case DOCTOR->2;case FORTUNE->3;case PRINCIPAL->4;default->0;});
    Session session=new Session(npc.getId(),player.serverLevel().getGameTime());session.selected=selected;SESSIONS.put(player.getUUID(),session);send(player,npc,session,"");
  }
  private static List<Choice> choices(ServerPlayer player,Role role){
    var result=new ArrayList<Choice>();var seen=new HashSet<UUID>();
    for(int i=0;i<36;i++){
      var stack=player.getInventory().getItem(i);if(!usableCarrier(player,stack))continue;
      UUID id=ChaoCarrierItem.storedUUID(stack);if(id==null||!seen.add(id))continue;
      result.add(new Choice("i:"+i+":"+id,label(stack.getHoverName().getString()),ChaoCarrierItem.state(stack),stack,null,null));
    }
    for(var chao:player.serverLevel().getEntitiesOfClass(ChaoEntity.class,player.getBoundingBox().inflate(8),c->!(c instanceof KindergartenNpc)
        &&player.getUUID().equals(c.owner)&&c.isAlive()&&!CompanionContent.isManaged(c)&&!isBoarded(c)))
      if(result.size()<44&&seen.add(chao.getUUID()))result.add(new Choice("w:"+chao.getUUID(),label(chao.getName().getString()),chao.state(),ItemStack.EMPTY,chao,null));
    if(role==Role.TEACHER)for(var student:KindergartenRoster.get(player.server).students.values())if(student.keeper.equals(player.getUUID())){
      var body=studentBody(player.server,student);var state=body==null?ChaoState.decode(student.entity.getString("Chao")):body.state();
      String name=body==null?"Student "+(student.seat+1):label(body.getName().getString());
      result.add(new Choice("c:"+student.id,name,state,ItemStack.EMPTY,body,student));
    }
    return result;
  }
  private static String label(String value){return value.codePoints().limit(48).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().replace('|','/').replace('\n',' ');}
  private static boolean usableCarrier(ServerPlayer player,ItemStack stack){
    var id=ChaoCarrierItem.storedUUID(stack);
    return ownedCarrier(player,stack)&&id!=null&&!KindergartenRoster.get(player.server).students.containsKey(id)
        &&CompanionContent.findEntity(player.server,id)==null;
  }
  private static boolean ownedCarrier(ServerPlayer player,ItemStack stack){
    if(stack.getCount()!=1||!ChaoCarrierItem.filled(stack)||!ChaoCarrierItem.canRelease(stack,player)
        ||CompanionContent.itemIsSummoned(stack)||CompanionContent.remaining(stack,player.server)>0)return false;
    var entity=ChaoCarrierItem.entityData(stack);return !entity.hasUUID("Keeper")||entity.getUUID("Keeper").equals(player.getUUID());
  }
  private static boolean nearRole(ServerPlayer player,Role role){return player.isAlive()&&!player.isSpectator()&&inKindergarten(player.level())&&player.distanceToSqr(role.anchor.getCenter())<=64&&player.containerMenu==player.inventoryMenu&&player.inventoryMenu.getCarried().isEmpty();}
  private static Choice selected(ServerPlayer player,Role role,String key){return choices(player,role).stream().filter(c->c.key.equals(key)).findFirst().orElse(null);}
  private static void send(ServerPlayer player,KindergartenNpc npc,Session session,String result){KindergartenNetwork.send(player,snapshot(player,npc,session,result));}
  /** Build the same authorized, bounded display snapshot used by the live resident menu. */
  public static KindergartenNetwork.Open describe(ServerPlayer player,KindergartenNpc npc){
    return authorized(player,npc)?snapshot(player,npc,new Session(npc.getId(),player.level().getGameTime()),""):null;
  }
  private static KindergartenNetwork.Open snapshot(ServerPlayer player,KindergartenNpc npc,Session session,String result){
    var list=choices(player,npc.role());var choice=list.stream().filter(c->c.key.equals(session.selected)).findFirst().orElse(list.isEmpty()?null:list.getFirst());
    session.selected=choice==null?"":choice.key;
    if(session.proposal.isEmpty())session.proposal=NAMES[player.getRandom().nextInt(NAMES.length)];
    String options=String.join("\n",list.stream().map(c->c.key+"|"+c.name.replace('|','/').replace('\n',' ')+(c.student==null?"":" · "+(c.state.traitInt("lesson.remaining",0)>0?seconds(c.state.traitInt("lesson.remaining",0)):"Ready"))).toList());
    String lesson=ChaoCareServices.currentLesson(player.level().getGameTime());var info=ChaoCareServices.lesson(lesson);
    String message=switch(npc.role()){
      case TEACHER->"Current class: "+info.name()+"\nNext offer in "+seconds(ChaoCareServices.lessonOfferRemaining(player.level().getGameTime()))
          +"\nClassroom: "+KindergartenRoster.get(player.server).students.size()+" / 4 students\nLeave a healthy, fed Chao. Return after the lesson finishes.\nLessons teach actions, not intelligence.";
      case DOCTOR->"The doctor can examine your Chao and provide treatment for its six ailments. Medical charts show stats, personality and contest records.";
      case PRINCIPAL->session.topic<0?"Welcome to the Kindergarten. I am the principal. What would you like to learn?":PrincipalTopics025.TOPICS.get(session.topic).pages().get(session.tipPage);
      case FORTUNE->"A lucky name for your Chao: "+session.proposal+".\nAccept it, ask for another name, or choose your own.";
      case BOARD->"Welcome to Chao Kindergarten!\nCurrent class: "+info.name()+"\nNew lesson in "+seconds(ChaoCareServices.lessonOfferRemaining(player.level().getGameTime()))
          +"\nClassroom occupancy: "+KindergartenRoster.get(player.server).students.size()+" / 4\nVisit the Principal for care advice, the Doctor for health, and the Fortune-Teller for names.";
    };
    if(!result.isEmpty())message=result+"\n\n"+message;
    if(choice!=null&&choice.student!=null)message+="\nSelected student: "+(choice.state.traitInt("lesson.remaining",0)>0?seconds(choice.state.traitInt("lesson.remaining",0))+" remaining":"Ready to take home");
    return new KindergartenNetwork.Open(npc.getId(),session.nonce.toString(),npc.role().name(),session.selected,
        choice==null?"No Chao selected":choice.name,choice==null?"":choice.state.encode(),options,message,npc.role()==Role.PRINCIPAL?(session.topic<0?"principal|home":"principal|"+PrincipalTopics025.TOPICS.get(session.topic).id()+"|"+session.tipPage+"|"+PrincipalTopics025.TOPICS.get(session.topic).pages().size()):session.proposal);
  }
  private static String seconds(int ticks){int seconds=(Math.max(0,ticks)+19)/20;return (seconds/60)+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60);}
  public static void handle(ServerPlayer player,KindergartenNetwork.Action request){
    if(player.serverLevel().getEntity(request.npc()) instanceof BlackMarketTrader){KindergartenMarket025.handle(player,request);return;}
    Session session=SESSIONS.get(player.getUUID());long now=player.level().getGameTime();
    if(session==null||session.npc!=request.npc()||!session.nonce.toString().equals(request.nonce())||now-session.opened>12000
        ||!(player.serverLevel().getEntity(request.npc()) instanceof KindergartenNpc npc)||!authorized(player,npc))return;
    if(request.action().equals("exit")){SESSIONS.remove(player.getUUID());return;}
    // Read-only classroom heartbeats never consume the cooldown of an actual care transaction.
    if(request.action().equals("refresh")||request.action().equals("select")){
      boolean selecting=request.action().equals("select");long last=selecting?session.lastSelect:session.lastRead;
      if(last!=Long.MIN_VALUE&&now-last<4)return;if(selecting)session.lastSelect=now;else session.lastRead=now;session.opened=now;
      if(selecting){session.selected=request.selection();session.proposal="";}send(player,npc,session,"");return;
    }
    if(session.lastAction!=Long.MIN_VALUE&&now-session.lastAction<4)return;session.lastAction=now;session.opened=now;
    if(!TRANSACTIONS.add(player.getUUID()))return;
    String message="That service is not available for this Chao right now.";
    try{
      Choice choice=selected(player,npc.role(),request.selection());session.selected=request.selection();
      switch(request.action()){
        case "leave"->{if(npc.role()==Role.TEACHER&&choice!=null&&enroll(player,choice)){message="Your Chao is safely enrolled. Come back when its lesson is finished.";npc.triggerServiceGesture(1);}}
        case "take"->{if(choice!=null&&(npc.role()==Role.TEACHER&&choice.student!=null?retrieve(player,choice.student):captureNearby(player,choice)))message="Your Chao is ready to carry home.";}
        case "cure"->{if(npc.role()==Role.DOCTOR&&choice!=null){if(treat(player,choice)){message="The doctor treated your Chao. All six illness meters are healthy.";npc.triggerServiceGesture(2);}else if(!choice.state.isIll())message="Your Chao is very healthy. Please keep taking good care of it.";}}
        case "grade"->{if(npc.role()==Role.DOCTOR&&choice!=null){try{if(upgrade(player,choice,Integer.parseInt(request.value())))message="Grade care is complete. This ability starts training again at its new grade.";}catch(NumberFormatException invalid){}}}
        case "reroll"->{if(npc.role()==Role.FORTUNE){session.proposal=NAMES[player.getRandom().nextInt(NAMES.length)];npc.triggerServiceGesture(3);message="How about this lucky name?";}}
        case "accept"->{if(npc.role()==Role.FORTUNE&&choice!=null&&rename(player,choice,session.proposal))message="Your Chao's new name is "+session.proposal+".";}
        case "name"->{if(npc.role()==Role.FORTUNE&&choice!=null&&rename(player,choice,request.value()))message="Your Chao's name was updated.";}
        case "topic"->{if(npc.role()==Role.PRINCIPAL){int topic=PrincipalTopics025.index(request.value());if(topic>=0){session.topic=topic;session.tipPage=0;message="";}}}
        case "tip_next"->{if(npc.role()==Role.PRINCIPAL&&session.topic>=0){session.tipPage=Math.min(PrincipalTopics025.TOPICS.get(session.topic).pages().size()-1,session.tipPage+1);message="";}}
        case "tip_prev"->{if(npc.role()==Role.PRINCIPAL&&session.topic>=0){session.tipPage=Math.max(0,session.tipPage-1);message="";}}
        case "tip_home"->{if(npc.role()==Role.PRINCIPAL){session.topic=-1;session.tipPage=0;message="";}}
        case "refresh"->{message="";}
        default->{return;}
      }
    }finally{TRANSACTIONS.remove(player.getUUID());}
    player.getInventory().setChanged();player.containerMenu.broadcastChanges();send(player,npc,session,message);
  }
  private static ChaoEntity materialize(ServerPlayer player,Choice choice){
    if(choice.body!=null)return choice.body;
    if(choice.stack.isEmpty()||!usableCarrier(player,choice.stack))return null;
    var chao=ChaoGarden.CHAO.get().create(player.serverLevel());if(chao==null)return null;
    chao.load(ChaoCarrierItem.entityData(choice.stack));if(chao.owner==null)chao.owner=player.getUUID();return chao;
  }
  private static void storeChoice(ServerPlayer player,Choice choice,ChaoEntity chao){
    chao.sync();if(!choice.stack.isEmpty()){
      ChaoCarrierItem.snapshot(choice.stack,chao);choice.stack.set(DataComponents.CUSTOM_NAME,chao.getName().copy());
    }
    player.getInventory().setChanged();
  }
  public static boolean treat(ServerPlayer player,String key){if(!nearRole(player,Role.DOCTOR))return false;var choice=selected(player,Role.DOCTOR,key);return choice!=null&&treat(player,choice);}
  private static boolean treat(ServerPlayer player,Choice choice){
    var chao=materialize(player,choice);if(chao==null||chao.isCocooned()||chao.state().dead||!chao.state().isIll())return false;
    chao.state().cureAllIllnesses();storeChoice(player,choice,chao);player.playSound(SoundEvents.BREWING_STAND_BREW,.4f,1.4f);return true;
  }
  private static boolean upgrade(ServerPlayer player,Choice choice,int stat){
    var chao=materialize(player,choice);if(chao==null)return false;
    if(!choice.stack.isEmpty())chao.setPos(Role.DOCTOR.anchor.getCenter());
    if(!ChaoCareServices.upgrade(player,chao,stat))return false;storeChoice(player,choice,chao);return true;
  }
  public static boolean rename(ServerPlayer player,String key,String value){if(!nearRole(player,Role.FORTUNE))return false;var choice=selected(player,Role.FORTUNE,key);return choice!=null&&rename(player,choice,value);}
  private static boolean rename(ServerPlayer player,Choice choice,String value){
    StringBuilder b=new StringBuilder();value.strip().codePoints().filter(c->!Character.isISOControl(c)&&c!=0xa7).limit(24).forEach(b::appendCodePoint);
    if(b.toString().isBlank())return false;var chao=materialize(player,choice);if(chao==null||chao.isCocooned()||chao.state().dead)return false;
    chao.setCustomName(Component.literal(b.toString()));storeChoice(player,choice,chao);return true;
  }
  public static boolean enroll(ServerPlayer player,String key){if(!nearRole(player,Role.TEACHER))return false;var choice=selected(player,Role.TEACHER,key);return choice!=null&&enroll(player,choice);}
  private static boolean enroll(ServerPlayer player,Choice choice){
    var roster=KindergartenRoster.get(player.server);int seat=roster.freeSeat();if(seat<0||choice.student!=null)return false;
    ChaoEntity chao=materialize(player,choice);if(chao==null||!player.getUUID().equals(chao.owner)||chao.isCocooned()
        ||chao.isHandlingItem()||ChaoCareServices.hasLesson(chao)||ActivityManager.isBusy(chao)||CompanionContent.isManaged(chao))return false;
    UUID id=chao.getUUID();if(roster.students.containsKey(id))return false;
    if(!choice.stack.isEmpty()&&CompanionContent.findEntity(player.server,id)!=null)return false;
    CompoundTag original=new CompoundTag();chao.saveWithoutId(original);ItemStack source=choice.stack.copy();
    var student=new KindergartenRoster.Student(id,player.getUUID(),seat,original);
    boolean committed=false;
    // Reserve the real roster seat before beginning class. The care service accepts
    // lessons only for registered daycare students; every failed drop-off rolls back.
    roster.students.put(id,student);chao.getPersistentData().putUUID(STUDENT,player.getUUID());
    try {
      chao.moveTo(seat(seat).x,80,seat(seat).z,180,0);
      if(!player.serverLevel().noCollision(chao)||!ChaoCareServices.beginLesson(player,chao,ChaoCareServices.currentLesson(player.level().getGameTime())))return false;
      if(!choice.stack.isEmpty()&&!player.serverLevel().addFreshEntity(chao))return false;
      if(!chao.isAlive()||(!source.isEmpty()&&(!ownedCarrier(player,choice.stack)
          ||!ItemStack.isSameItemSameComponents(source,choice.stack)||!id.equals(ChaoCarrierItem.storedUUID(choice.stack))
          ||!inventoryContainsReference(player,choice.stack))))return false;
      chao.setNoAi(true);chao.setNoGravity(true);chao.setStaying(true);
      syncStudent(roster,student,chao);if(!choice.stack.isEmpty())choice.stack.setCount(0);
      committed=true;roster.setDirty();updateForced(player.serverLevel(),roster);return true;
    } finally {
      if(!committed){
        roster.students.remove(id,student);roster.setDirty();
        ChaoCareServices.cancelLesson(chao);chao.getPersistentData().remove(STUDENT);
        if(choice.body==null)chao.discard();else {chao.load(original);chao.sync();}
      }
    }
  }
  private static boolean inventoryContainsReference(ServerPlayer player,ItemStack stack){for(int i=0;i<36;i++)if(player.getInventory().getItem(i)==stack)return true;return false;}
  private static boolean captureNearby(ServerPlayer player,Choice choice){
    if(choice.body==null||choice.student!=null||isBoarded(choice.body)||ChaoCareServices.hasLesson(choice.body))return false;
    int free=player.getInventory().getFreeSlot();if(free<0)return false;
    var item=new ItemStack(ChaoGarden.item("chao_carrier"));if(!ChaoCarrierItem.capture(item,choice.body,player))return false;
    player.getInventory().setItem(free,item);return true;
  }
  public static boolean retrieve(ServerPlayer player,UUID id){if(!nearRole(player,Role.TEACHER))return false;var student=KindergartenRoster.get(player.server).students.get(id);return student!=null&&retrieve(player,student);}
  private static boolean retrieve(ServerPlayer player,KindergartenRoster.Student student){
    var roster=KindergartenRoster.get(player.server);if(!student.keeper.equals(player.getUUID()))return false;
    var body=studentBody(player.server,student);if(body==null||ChaoCareServices.hasLesson(body))return false;
    int free=player.getInventory().getFreeSlot();if(free<0)return false;
    // Settle first-instrument markers before any external inventory/drop callback, then commit the
    // complete same-UUID carrier and remove its projection before delivering independent rewards.
    var rewards=ChaoCareServices.settleLessonRewards(body.state());syncStudent(roster,student,body);
    var item=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.storeEntity(item,student.entity.copy());
    item.set(DataComponents.CUSTOM_NAME,body.getName().copy());CompanionContent.token(item,true);
    player.getInventory().setItem(free,item);roster.students.remove(student.id);roster.setDirty();body.discard();updateForced(player.serverLevel(),roster);
    for(var reward:rewards)if(!player.addItem(reward)){var drop=player.drop(reward,false);if(drop!=null)drop.getPersistentData().putLong("ChaoRejectedUntil",player.level().getGameTime()+200);}
    return true;
  }
  private static ChaoEntity studentBody(MinecraftServer server,KindergartenRoster.Student student){
    var e=CompanionContent.findEntity(server,student.id);return e instanceof ChaoEntity c&&isBoarded(c)?c:null;
  }
  private static void syncStudent(KindergartenRoster roster,KindergartenRoster.Student student,ChaoEntity body){
    var original=student.entity;var next=new CompoundTag();body.saveWithoutId(next);
    next.putBoolean("NoAI",original.getBoolean("NoAI"));next.putBoolean("NoGravity",original.getBoolean("NoGravity"));next.putBoolean("Stay",original.getBoolean("Stay"));next.putFloat("FallDistance",0);
    var motion=new ListTag();for(int i=0;i<3;i++)motion.add(DoubleTag.valueOf(0));next.put("Motion",motion);
    for(String key:new String[]{"NeoForgeData","ForgeData"}){var tag=next.getCompound(key);tag.remove(STUDENT);if(tag.isEmpty())next.remove(key);else next.put(key,tag);}
    student.entity=next;roster.setDirty();
  }
  private static void updateResidentTickets(ServerLevel level,boolean occupied){
    for(int[] chunk:RESIDENT_CHUNKS)RESIDENT_TICKETS.forceChunk(level,RESIDENT_TICKET_OWNER,chunk[0],chunk[1],occupied,true);
  }
  private static void updateForced(ServerLevel level,KindergartenRoster roster){
    if(!inKindergarten(level))return;
    boolean needed=!roster.students.isEmpty();
    // Namespaced owner tickets never remove another mod's or an administrator's force-load.
    CLASSROOM_TICKETS.forceChunk(level,Role.TEACHER.anchor,-2,-1,needed,true);
    CLASSROOM_TICKETS.forceChunk(level,Role.TEACHER.anchor,-2,-2,needed,true);
  }
  @SubscribeEvent public static void serverTick(ServerTickEvent.Post event){
    var server=event.getServer();var level=server.getLevel(DIMENSION);if(level==null)return;
    SESSIONS.entrySet().removeIf(e->{var p=server.getPlayerList().getPlayer(e.getKey());return p==null||!inKindergarten(p.level())||level.getGameTime()-e.getValue().opened>12000;});
    var roster=KindergartenRoster.get(server);
    if(server.overworld().getGameTime()%20!=0)return;updateResidentTickets(level,!level.players().isEmpty());if(!level.players().isEmpty())ensureServices(level);updateForced(level,roster);
    for(var student:roster.students.values()){
      var body=studentBody(server,student);
      if(body==null){
        if(CompanionContent.findEntity(server,student.id)!=null)continue;
        body=ChaoGarden.CHAO.get().create(level);if(body==null)continue;body.load(student.entity.copy());body.owner=student.keeper;
        body.getPersistentData().putUUID(STUDENT,student.keeper);body.moveTo(seat(student.seat).x,80,seat(student.seat).z,180,0);
        body.setNoAi(true);body.setNoGravity(true);body.setStaying(true);if(!level.addFreshEntity(body))continue;
      }
      syncStudent(roster,student,body);
    }
  }
  @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){SESSIONS.remove(event.getEntity().getUUID());}
  @SubscribeEvent public static void stop(ServerStoppingEvent event){
    var level=event.getServer().getLevel(DIMENSION);if(level!=null){updateResidentTickets(level,false);var roster=KindergartenRoster.get(event.getServer());for(var s:roster.students.values()){var c=studentBody(event.getServer(),s);if(c!=null)syncStudent(roster,s,c);}CLASSROOM_TICKETS.forceChunk(level,Role.TEACHER.anchor,-2,-1,false,true);CLASSROOM_TICKETS.forceChunk(level,Role.TEACHER.anchor,-2,-2,false,true);}
    SESSIONS.clear();TRANSACTIONS.clear();
  }
}
