package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.item.ChaoEggItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** A small continuous rocking pose, rather than an eating/bow animation for eggs. */
@EventBusSubscriber(modid=ChaoGarden.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ChaoEggClient {
  private ChaoEggClient() {}
  @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
    var eggs=ChaoGarden.CONTENT.values().stream().map(x->x.get()).filter(x->x instanceof ChaoEggItem)
        .toArray(net.minecraft.world.item.Item[]::new);
    event.registerItem(new IClientItemExtensions() {
      @Override public boolean applyForgeHandTransform(PoseStack pose,LocalPlayer player,HumanoidArm arm,
          ItemStack stack,float partial,float equipped,float swing) {
        if(!player.isUsingItem()||player.getUseItem()!=stack)return false;
        float elapsed=player.getTicksUsingItem()+partial;
        var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(tag.getBoolean("ThrowCharging")) {
          float charge=Math.min(1,elapsed/20f);
          pose.translate(0,charge*.12,charge*.1);
          pose.mulPose(Axis.XP.rotationDegrees(-charge*18));
        } else {
          float rock=(float)Math.sin(elapsed*.19f);
          pose.translate(0,.06+Math.sin(elapsed*.38f)*.008,0);
          pose.mulPose(Axis.ZP.rotationDegrees(rock*9));
          pose.mulPose(Axis.XP.rotationDegrees((float)Math.cos(elapsed*.19f)*3));
        }
        return false;
      }
    },eggs);
  }
}
