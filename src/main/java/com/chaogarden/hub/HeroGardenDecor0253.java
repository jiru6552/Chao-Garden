package com.chaogarden.hub;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Included native masonry for the Hero Garden's first authored pass. */
public final class HeroGardenDecor0253 {
  private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,ChaoGarden.MODID);
  private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ChaoGarden.MODID);
  private static final Map<String,DeferredHolder<Block,Block>> blocks=new LinkedHashMap<>();
  private static final Map<String,DeferredHolder<Item,BlockItem>> items=new LinkedHashMap<>();
  static {
    for(String id:List.of("hero_marble","hero_marble_trim","hero_fountain_emblem","hero_column")){
      var block=BLOCKS.register(id,()->id.equals("hero_marble_trim")
          ?new HeroCorniceBlock0254(BlockBehaviour.Properties.of().strength(2).sound(SoundType.STONE))
          :id.equals("hero_column")?new HeroColumn0259(BlockBehaviour.Properties.of().strength(2).sound(SoundType.STONE).noOcclusion())
          :new Block(BlockBehaviour.Properties.of().strength(2).sound(SoundType.STONE)));
      blocks.put(id,block);items.put(id,ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
    }
  }
  private HeroGardenDecor0253(){}
  public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
  public static Block block(String id){var block=blocks.get(id);if(block==null)throw new IllegalArgumentException(id);return block.get();}
  public static Item item(String id){return items.get(id).get();}
  public static List<String> ids(){return List.copyOf(blocks.keySet());}
}
