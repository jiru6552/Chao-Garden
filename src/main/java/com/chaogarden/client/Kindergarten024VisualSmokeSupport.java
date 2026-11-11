package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenNpc;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Real packet/menu study fixtures; excluded from installable jars. */
public final class Kindergarten024VisualSmokeSupport {
  private Kindergarten024VisualSmokeSupport(){}
  private static final java.util.Map<java.util.UUID,String> PENDING=new java.util.HashMap<>();
  private static volatile java.util.UUID expectedChao;
  private static final String FIXTURE="ChaoKindergartenVisualStudy024";
  public static void cleanup(net.minecraft.server.MinecraftServer server){
    var roster=com.chaogarden.kindergarten.KindergartenRoster.get(server);
    for(var student:java.util.List.copyOf(roster.students.values())){
      boolean tagged=student.entity.getCompound("NeoForgeData").getBoolean(FIXTURE)||student.entity.getCompound("ForgeData").getBoolean(FIXTURE);
      // Proven interrupted-study fixture: exact ID+keeper from the current roster; absent in the pre-study backup.
      boolean legacy=student.id.equals(java.util.UUID.fromString("3c08f507-324a-4c50-aeed-0e4b45819e2f"))
          &&student.keeper.equals(java.util.UUID.fromString("380df991-f603-344c-a090-369bad2a924a"));
      if(!tagged&&!legacy)continue;
      var body=com.chaogarden.companion.CompanionContent.findEntity(server,student.id);if(body!=null)body.discard();roster.students.remove(student.id);roster.setDirty();
    }
    var fixtures=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
    for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof ChaoEntity&&entity.getPersistentData().getBoolean(FIXTURE))fixtures.add(entity);
    fixtures.forEach(net.minecraft.world.entity.Entity::discard);
    PENDING.clear();expectedChao=null;
  }
  private static KindergartenContent.Role role(String key){return switch(key){
    case "kindergarten_teacher"->KindergartenContent.Role.TEACHER;
    case "kindergarten_doctor"->KindergartenContent.Role.DOCTOR;
    case "kindergarten_fortune"->KindergartenContent.Role.FORTUNE;
    case "kindergarten_principal"->KindergartenContent.Role.PRINCIPAL;
    default->throw new IllegalArgumentException(key);
  };}
  public static void prepare(ServerPlayer player,String key){
    var level=player.server.getLevel(KindergartenContent.DIMENSION);HubContent.ensure(level);
    var role=role(key);var at=role.anchor;
    player.teleportTo(level,at.getX()+.5,80,at.getZ()+3.5,java.util.Set.of(),180,0);player.setNoGravity(true);player.getInventory().clearContent();
    var state=ChaoState.egg(4,false,false,0,new Random(24));state.adult=true;state.happiness=87;state.hunger=81;state.energy=94;
    state.reincarnations=2;state.ageTicks=38000;state.adultAgeTicks=18000;
    for(int i=0;i<5;i++){state.grades[i]=i+1;state.levels[i]=12+i*9;state.points[i]=400+i*173;}
    if(role==KindergartenContent.Role.DOCTOR){state.setIllness(2,-100);state.setTraitInt("medal.karate_gold",1);state.setTraitInt("contest.karate.wins",3);}
    var chao=ChaoGarden.CHAO.get().create(level);chao.owner=player.getUUID();chao.setState(state);chao.setCustomName(Component.literal("Melody"));chao.getPersistentData().putBoolean(FIXTURE,true);
    var held=new ItemStack(ChaoGarden.item("chao_carrier"));if(!ChaoCarrierItem.capture(held,chao,player))throw new IllegalStateException("Study carrier rejected");
    player.getInventory().setItem(0,held);player.getInventory().selected=0;player.inventoryMenu.broadcastChanges();
    expectedChao=ChaoCarrierItem.storedUUID(held);PENDING.put(player.getUUID(),key);maintain(player,key);
  }
  public static void maintain(ServerPlayer player,String key){
    if(!key.equals(PENDING.get(player.getUUID())))return;
    var role=role(key);KindergartenContent.ensureServices(player.serverLevel());
    var npc=player.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(4),n->n.role()==role).stream().findFirst().orElse(null);
    if(npc!=null&&player.tickCount%20==0)npc.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
  }
  public static boolean clientReady(String key){
    var mc=Minecraft.getInstance();boolean ready=mc.screen instanceof KindergartenScreen screen&&screen.serviceRole().equals(role(key).name())
        &&expectedChao!=null&&screen.selectedKey().endsWith(expectedChao.toString());
    if(ready&&mc.player!=null&&mc.getSingleplayerServer()!=null){var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->PENDING.remove(id,key));}
    return ready;
  }
}
