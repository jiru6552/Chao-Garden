package com.chaogarden.test;

/** Serialized pre-removal Bell fixture for historical provider-migration tests only. */
@net.neoforged.fml.common.EventBusSubscriber(modid="chao_garden",bus=net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class LegacyGardenFixture {
  static net.minecraft.world.item.Item ITEM;
  @net.neoforged.bus.api.SubscribeEvent
  public static void register(net.neoforged.neoforge.registries.RegisterEvent event){
    if(Boolean.getBoolean("chao_garden.legacyFixtures")&&event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)){
      ITEM=new com.chaogarden.activities.GardenBellItem(new net.minecraft.world.item.Item.Properties().stacksTo(1));
      event.register(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chao_garden","legacy_bell_test_fixture"),()->ITEM);
    }
  }
}
