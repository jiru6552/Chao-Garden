package com.chaogarden.kindergarten;

import com.chaogarden.ChaoGarden;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native, full masonry/fabric surfaces for the school's destination-themed passages. */
public final class KindergartenHallThemes0252 {
  private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,ChaoGarden.MODID);
  private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ChaoGarden.MODID);
  private static final Map<String,DeferredHolder<Block,Block>> blocks=new LinkedHashMap<>();
  private static final Map<String,DeferredHolder<Item,BlockItem>> items=new LinkedHashMap<>();
  static {
    add(0,"school_hall_fortune_carpet","school_hall_fortune_dado","school_hall_fortune_wall","school_hall_fortune_frieze","school_hall_fortune_ceiling",
        "school_hall_clinic_wall","school_hall_clinic_frieze","school_hall_clinic_ceiling",
        "school_hall_classroom_dado","school_hall_classroom_letters",
        "school_hall_principal_parquet","school_hall_principal_panel","school_hall_principal_wall","school_hall_principal_frieze","school_hall_principal_ceiling",
        "school_hall_market_flagstone","school_hall_market_wall","school_hall_market_frieze","school_hall_market_ceiling",
        "school_hall_foyer_frieze","school_hall_principal_certificate","school_hall_principal_award",
        "school_hall_classroom_student_work","school_hall_classroom_class_photo","school_hall_clinic_registration","school_hall_clinic_wellness","school_hall_market_shelf","school_hall_market_wall_shelf");
    add(13,"school_hall_principal_light");add(15,"school_hall_clinic_light");
  }
  private KindergartenHallThemes0252(){}
  private static void add(int light,String... names){for(String id:names){
    var block=BLOCKS.register(id,()->{
      var properties=BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOD).lightLevel(s->light);
      if(id.equals("school_hall_market_shelf")||id.equals("school_hall_market_wall_shelf"))properties.noOcclusion();
      return id.equals("school_hall_market_wall_shelf")?new WallShelfBlock(properties):new Block(properties);
    });
    blocks.put(id,block);items.put(id,ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
  }}
  public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
  public static boolean contains(String id){return blocks.containsKey(id);}
  public static Block block(String id){var b=blocks.get(id);if(b==null)throw new IllegalArgumentException(id);return b.get();}
  public static Item item(String id){return items.get(id).get();}
  public static List<String> ids(){return List.copyOf(blocks.keySet());}
  /** A real ten-pixel tabletop meets the furniture props' existing minus-six-pixel offset. */
  private static final class WallShelfBlock extends Block {
    private static final VoxelShape SHAPE=Shapes.or(Block.box(0,8,1,14,10,15),Block.box(0,2,3,2,8,5),Block.box(0,2,11,2,8,13));
    WallShelfBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
  }
}
