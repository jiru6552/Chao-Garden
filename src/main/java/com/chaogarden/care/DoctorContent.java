package com.chaogarden.care;

import com.chaogarden.ChaoGarden;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Physical kindergarten facilities; no external Accessories dependency is required. */
public final class DoctorContent {
  public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,ChaoGarden.MODID);
  public static final DeferredHolder<Block,ChaoSchoolBlock> LESSON_DESK=BLOCKS.register("lesson_desk",
      ()->new ChaoSchoolBlock(ChaoSchoolBlock.Kind.CLASSROOM,
          BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD)));
  public static final DeferredHolder<Block,ChaoSchoolBlock> DOCTOR_STATION=BLOCKS.register("doctor_station",
      ()->new ChaoSchoolBlock(ChaoSchoolBlock.Kind.DOCTOR,
          BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.METAL)));
  private DoctorContent() {}
  public static void bootstrap() {
    ChaoGarden.add("chao_medicine",ChaoMedicineItem::new);
    ChaoGarden.add("lesson_desk",()->new BlockItem(LESSON_DESK.get(),new Item.Properties()));
    ChaoGarden.add("doctor_station",()->new BlockItem(DOCTOR_STATION.get(),new Item.Properties()));
  }
  public static void register(IEventBus bus) { BLOCKS.register(bus);ChaoCareNetwork.register(bus); }
}
