package com.chaogarden.kindergarten;

import com.chaogarden.ChaoGarden;
import com.chaogarden.market.MarketStock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/** Noncollectible native item models show possible goods behind the unmarked storage cupboard. */
public final class KindergartenHallDisplays0252 {
  public record Goods(UUID uuid,String itemId,double x,double y,double z,float yaw){}
  private static final List<Goods> GOODS=goods();
  private KindergartenHallDisplays0252(){}
  private static List<Goods> goods(){
    String[] items={"normal_egg","hero_garden_fruit","swim_drive","round_fruit","apple_hat","fly_drive",
        "blue_wool_beanie","dark_garden_fruit","power_drive","triangle_fruit","red_egg","run_drive"};
    var result=new ArrayList<Goods>();int index=0;
    for(int x:new int[]{-10,-13,-16})for(int z:new int[]{3,7})for(int tier=0;tier<2;tier++){
      String id=items[index];UUID uuid=UUID.nameUUIDFromBytes(("chao_garden:school0252:stock-display:"+index).getBytes(StandardCharsets.UTF_8));
      result.add(new Goods(uuid,id,x+.5,80+(tier==0?.46:.835),z+(z==3?.83:.17),z==3?0:180));index++;
    }
    return List.copyOf(result);
  }
  public static List<Goods> showcases(){return GOODS;}
  private static ListTag floats(float... values){var result=new ListTag();for(float value:values)result.add(FloatTag.valueOf(value));return result;}
  public static void ensure(ServerLevel level){
    if(!KindergartenContent.inKindergarten(level))throw new IllegalArgumentException("School shelf displays are dimension scoped");
    // LOADED can still include a hidden/unloading UUID that getEntity cannot see.
    // A real occupied ticking chunk guarantees visible publication before absence
    // is used to create a durable fixture, just like the real resident guard.
    if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(-1,0))
        ||!level.isPositionEntityTicking(new net.minecraft.core.BlockPos(-10,80,5)))return;
    for(var goods:GOODS){
      if(!MarketStock.catalog().containsKey(goods.itemId()))throw new IllegalStateException("Shelf prop is not possible shop stock: "+goods.itemId());
      var existing=level.getEntity(goods.uuid());
      if(existing!=null&&!(existing instanceof Display.ItemDisplay))throw new IllegalStateException("School showcase identity belongs to another entity");
      Display.ItemDisplay display=existing instanceof Display.ItemDisplay item?item:EntityType.ITEM_DISPLAY.create(level);
      if(display==null)throw new IllegalStateException("Cannot create native school shelf item display");
      if(existing!=null&&display.getPersistentData().getString("ChaoSchoolStockItem0252").equals(goods.itemId())
          &&display.position().distanceToSqr(goods.x(),goods.y(),goods.z())<.000001&&display.isNoGravity())continue;
      // Use the ordinary persisted display schema, including the real ItemStack's model/components.
      // These entities are visual fixtures; they never insert goods, expose containers or drop items.
      CompoundTag tag=new CompoundTag();
      tag.put("item",new ItemStack(ChaoGarden.item(goods.itemId())).save(level.registryAccess()));
      tag.putString("item_display","fixed");tag.putString("billboard","fixed");tag.putBoolean("Invulnerable",true);
      tag.putFloat("view_range",1);tag.putFloat("shadow_radius",0);tag.putFloat("shadow_strength",0);tag.putFloat("width",.75f);tag.putFloat("height",.75f);
      CompoundTag transform=new CompoundTag();transform.put("translation",floats(0,0,0));transform.put("scale",floats(.42f,.42f,.42f));transform.put("left_rotation",floats(0,0,0,1));transform.put("right_rotation",floats(0,0,0,1));tag.put("transformation",transform);
      display.load(tag);display.setUUID(goods.uuid());display.setNoGravity(true);display.setPos(goods.x(),goods.y(),goods.z());display.setYRot(goods.yaw());display.setXRot(0);
      display.addTag("chao_school_stock_showcase");
      display.getPersistentData().putString("ChaoSchoolStockItem0252",goods.itemId());
      if(existing==null&&!level.addFreshEntity(display))throw new IllegalStateException("Could not publish school shelf display "+goods.uuid());
    }
  }
}
