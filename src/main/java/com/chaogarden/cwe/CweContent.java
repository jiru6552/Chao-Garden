package com.chaogarden.cwe;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.genes.GeneContent;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Working CWE gifts and fruit-to-DNA attraction; no additional animal mobs. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class CweContent {
  private static boolean bootstrapped;
  public static final int ATTRACTION_TICKS=600;
  private static final Map<String,int[]> POOLS=Map.of(
      "orange_fruit",new int[]{21,40,22,40,23,20},
      "gba_orange_fruit",new int[]{2,35,1,15,28,25,11,25},
      "gba_blue_fruit",new int[]{30,40,3,20,4,20,0,10,27,10},
      "gba_pink_fruit",new int[]{2,15,1,35,26,40,29,10},
      "gba_green_fruit",new int[]{25,40,24,30,26,20,29,10},
      "gba_purple_fruit",new int[]{6,30,3,20,27,10,29,10,12,30},
      "gba_yellow_fruit",new int[]{30,30,4,40,11,10,27,10,12,10},
      "gba_red_fruit",new int[]{0,20,28,30,24,20,27,10,29,10,12,10});
  private CweContent(){}
  public static void bootstrap(){
    if(bootstrapped)return;bootstrapped=true;
    for(var gift:CweCatalog.gifts().values())ChaoGarden.add(gift.id(),()->new CweGiftItem(gift));
    ChaoGarden.add("spartoi_lens",RetiredCweGiftItem::new);
    ChaoGarden.ITEMS.addAlias(net.minecraft.resources.ResourceLocation.parse("chao_garden:robot_component"),net.minecraft.resources.ResourceLocation.parse("chao_garden:omochao_parts_box"));
    ChaoGarden.add("robo_heart",()->new CweRobotPartItem(true));
  }
  /** Subscriber registration is automatic, consistent with MarketContent. */
  public static void register(){}
  public static Set<String> attractingFruits(){return POOLS.keySet();}
  public static int attractedAnimal(String fruit,int roll){
    int[] pool=POOLS.get(fruit);if(pool==null)return -1;int pick=Math.floorMod(roll,100);
    for(int i=0;i<pool.length;i+=2){pick-=pool[i+1];if(pick<0)return pool[i];}throw new IllegalStateException("Attraction weights");
  }
  /** Only unowned, stationary, collectible fruit is eligible. One consumed unit yields one sample. */
  public static boolean attract(ItemEntity fruit){
    if(!(fruit.level() instanceof ServerLevel level)||!fruit.isAlive()||fruit.hasPickUpDelay()||fruit.getTarget()!=null||!fruit.onGround())return false;
    var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(fruit.getItem().getItem());
    if(!id.getNamespace().equals(ChaoGarden.MODID)||!POOLS.containsKey(id.getPath()))return false;
    long now=level.getGameTime();var tag=fruit.getPersistentData();
    if(!tag.contains("CweAttractAt")){tag.putLong("CweAttractAt",now+ATTRACTION_TICKS);return false;}
    if(now<tag.getLong("CweAttractAt"))return false;
    int animal=attractedAnimal(id.getPath(),level.random.nextInt(100));
    var sample=new ItemEntity(level,fruit.getX(),fruit.getY()+.15,fruit.getZ(),new ItemStack(GeneContent.gene(animal)));
    sample.setDefaultPickUpDelay();
    if(!level.addFreshEntity(sample))return false;
    fruit.getItem().shrink(1);tag.putLong("CweAttractAt",now+ATTRACTION_TICKS);
    if(fruit.getItem().isEmpty())fruit.discard();
    level.sendParticles(ParticleTypes.HAPPY_VILLAGER,sample.getX(),sample.getY(),sample.getZ(),5,.15,.15,.15,0);return true;
  }
  @SubscribeEvent public static void tick(LevelTickEvent.Post event){
    if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%100!=0)return;
    Set<UUID> seen=new HashSet<>();
    for(var player:level.players())for(var fruit:level.getEntitiesOfClass(ItemEntity.class,new AABB(player.blockPosition()).inflate(32)))
      if(seen.add(fruit.getUUID()))attract(fruit);
  }
  /** Called at the real first-evolution transition, never inferred again during load. */
  public static void onFirstEvolution(ChaoEntity chao){
    var s=chao.state();s.retireRemovedCharacters();if(!"normal".equals(s.character))return;
    if(s.reincarnations>=2 && (s.animalMask&ChaoState.ALL_ANIMALS)==ChaoState.ALL_ANIMALS && s.happiness>=80){
      s.character=switch(s.primaryType){case ChaoState.FLY_TYPE->"tails";case ChaoState.RUN_TYPE->"amy";case ChaoState.POWER_TYPE->"knuckles";default->"normal";};
      if(!s.character.equals("normal")){s.immortal=true;s.clearParts();}
    }
    chao.sync();
  }
  private static ChaoEntity prototype(ChaoEntity builder){
    if(!(builder.level() instanceof ServerLevel level))return null;
    var state=builder.state();
    try{
      var entity=level.getEntity(UUID.fromString(state.trait("content.omochao_uuid")));
      if(entity instanceof ChaoEntity chao && chao.isAlive() && builder.distanceToSqr(chao)<=64
          && chao.state().character.equals("omochao")
          && chao.state().trait("content.builder_token").equals(state.trait("content.builder_token"))
          && chao.state().intTrait("content.construction")==state.intTrait("content.omochao_parts"))return chao;
    }catch(IllegalArgumentException ignored){}
    return null;
  }
  /** Never replace an unloaded construction: its saved UUID remains the only prototype. */
  public static boolean canAssembleOmochao(ChaoEntity builder){
    return canAssembleOmochao(builder,ItemStack.EMPTY);
  }
  public static boolean canAssembleOmochao(ChaoEntity builder,ItemStack part){
    if(!builder.isAlive()||builder.state().dead||builder.state().intTrait("content.construction")>0)return false;
    if(!part.isEmpty()&&!(part.getItem() instanceof CweRobotPartItem)&&!part.is(ChaoGarden.item("omochao_parts_box")))return false;
    boolean heart=part.getItem() instanceof CweRobotPartItem p&&p.isHeart();
    if(heart&&!CweRobotPartItem.validHeart(part))return false;
    var source=builder.state();int parts=canRestart(builder,part)?0:source.intTrait("content.omochao_parts");
    var old=parts>0?prototype(builder):null;
    if(heart&&old!=null&&old.state().intTrait("robot.heart_used")>0)return false;
    return source.intTrait("reward.wrench")>0 && parts>=0 && parts<4
        && (parts==0 && (source.trait("content.omochao_uuid").isEmpty()||canRestart(builder,part)) || parts>0 && old!=null);
  }
  private static boolean canRestart(ChaoEntity builder,ItemStack part){
    var source=builder.state();if(source.intTrait("content.omochao_parts")!=4||!(builder.level() instanceof ServerLevel level))return false;
    var origin=CweRobotPartItem.origin(part);if(!origin.hasUUID("Robot")||!origin.getUUID("Robot").toString().equals(source.trait("content.omochao_uuid"))
        ||!origin.getString("BuilderToken").equals(source.trait("content.builder_token")))return false;
    for(var world:level.getServer().getAllLevels())if(world.getEntity(origin.getUUID("Robot"))!=null)return false;
    return true;
  }
  private static ChaoState robotCoat(ChaoEntity builder,long seed,int rank){
    var source=builder.state();
    var state=ChaoState.egg(source.expressedColor,source.shiny,source.twoTone,source.expressedJewel,new Random(seed));
    OmochaoRules.initialize(state,source,builder.getName().getString(),rank);
    // A visual snapshot, not a clone of the builder's progress, equipment or
    // acquired animal parts. Refresh it once more when the fourth box completes.
    state.variant=com.chaogarden.ChaoVariants.obtainable(source.variant)?ChaoState.normalizeVariant(source.variant):"normal";state.align=source.align;
    state.primaryType=source.primaryType;state.heroBrushType=source.heroBrushType;
    state.negativePalette=source.negativePalette;state.brightCoat=source.brightCoat;
    boolean robot="omochao".equals(source.character);
    state.setTrait("robot.source_character",robot?source.trait("robot.source_character"):source.character);
    state.setTrait("robot.source_chaos",robot?source.intTrait("robot.source_chaos"):source.immortal&&"normal".equals(source.character)?1:0);
    state.setTrait("robot.source_adult",robot?source.intTrait("robot.source_adult"):source.adult?1:0);
    state.setTrait("robot.source_sf",robot?source.trait("robot.source_sf"):Float.toHexString(source.swimFly));
    state.setTrait("robot.source_rp",robot?source.trait("robot.source_rp"):Float.toHexString(source.runPower));
    state.setTrait("robot.source_strength",robot?source.trait("robot.source_strength"):Float.toHexString(source.evolutionStrength()));
    return state;
  }
  /** Four consumed boxes assemble one persistent owned robot, visibly piece by piece. */
  public static boolean assembleOmochao(ChaoEntity builder){
    return assembleOmochao(builder,ItemStack.EMPTY);
  }
  public static boolean assembleOmochao(ChaoEntity builder,ItemStack offered){
    if(!(builder.level() instanceof ServerLevel level)||!canAssembleOmochao(builder,offered))return false;
    var source=builder.state();int parts=canRestart(builder,offered)?0:source.intTrait("content.omochao_parts");
    boolean heart=offered.getItem() instanceof CweRobotPartItem item&&item.isHeart();
    if(parts==0){
      ChaoEntity built=ChaoGarden.CHAO.get().create(level);if(built==null)return false;
      String token=UUID.randomUUID().toString();
      var state=robotCoat(builder,level.random.nextLong(),OmochaoRules.C_RANK);if(heart)applyHeart(state,offered);
      if(!state.setTrait("content.builder_token",token)||!state.setTrait("content.construction",1))return false;
      built.setState(state);built.owner=builder.owner;built.setNoAi(true);built.setInvulnerable(true);built.setPersistenceRequired();
      built.moveTo(builder.getX()+.8,builder.getY(),builder.getZ(),builder.getYRot(),0);
      if(!level.noCollision(built))return false;
      String previousToken=source.trait("content.builder_token"),previousId=source.trait("content.omochao_uuid"),previousParts=source.trait("content.omochao_parts");
      if(!source.setTrait("content.builder_token",token)||!source.setTrait("content.omochao_uuid",built.getUUID().toString())||!source.setTrait("content.omochao_parts",1)){
        source.setTrait("content.builder_token",previousToken);source.setTrait("content.omochao_uuid",previousId);source.setTrait("content.omochao_parts",previousParts);return false;
      }
      if(!level.addFreshEntity(built)){source.setTrait("content.builder_token",previousToken);source.setTrait("content.omochao_uuid",previousId);source.setTrait("content.omochao_parts",previousParts);return false;}
    }else{
      ChaoEntity built=prototype(builder);if(built==null)return false;
      if(!source.setTrait("content.omochao_parts",parts+1))return false;
      if(heart)applyHeart(built.state(),offered);
      if(parts<3){built.state().setTrait("content.construction",parts+1);built.sync();}
      else{
        var state=robotCoat(builder,level.random.nextLong(),OmochaoRules.rank(built.state()));state.setTrait("content.builder_token",source.trait("content.builder_token"));
        if(built.state().intTrait("robot.heart_used")>0){
          state.setTrait("robot.heart_used",1);
          for(int i=0;i<4;i++)for(String field:new String[]{"id","dna","name"})state.setTrait("family.grandparent."+i+"."+field,"");
          for(String field:new String[]{"id","dna","name"})state.setTrait("family.grandparent.0."+field,built.state().trait("robot.previous_creator."+field));
        }
        built.setState(state);built.owner=builder.owner;built.setNoAi(false);built.setInvulnerable(false);
        level.sendParticles(ParticleTypes.ENCHANT,built.getX(),built.getY()+.5,built.getZ(),20,.3,.4,.3,.1);
      }
    }
    builder.sync();return true;
  }
  private static void applyHeart(ChaoState state,ItemStack heart){
    var data=CweRobotPartItem.heartData(heart);OmochaoRules.setRanks(state,data.getInt("Rank"));state.setTrait("robot.heart_used",1);
    for(int i=0;i<4;i++)for(String field:new String[]{"id","dna","name"})state.setTrait("family.grandparent."+i+"."+field,"");
    String[] fields={"id","dna","name"},keys={"CreatorId","CreatorDna","CreatorName"};
    for(int i=0;i<fields.length;i++){
      state.setTrait("robot.previous_creator."+fields[i],data.getString(keys[i]));
      state.setTrait("family.grandparent.0."+fields[i],data.getString(keys[i]));
    }
  }
}
