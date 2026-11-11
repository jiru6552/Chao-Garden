package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.fruit.FruitCatalog;
import com.chaogarden.fruit.FruitContent;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Disposable native model audit: GUI sprites alongside actual baked 3D items. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class Cwe02516VisualSmoke {
  private static int page=-1;private static boolean checked;
  private static List<String> ids(){return List.of("baseball_cap_accessory","bowtie_accessory","round_glasses_accessory","aviators_accessory","headphones_accessory","pacifier_accessory","press_hat_accessory","blue_hoodie_accessory","cheese_bowtie_accessory","pink_hoodie_accessory","chao_guitar","chao_accordion","shiny_fruit","hyper_power_fruit","hyper_run_fruit","hyper_fly_fruit","hyper_swim_fruit","suspicious_potion","reverse_mirror","omochao_parts_box","cosmetic_wings","normal_hero_brush","swim_hero_brush","fly_hero_brush","run_hero_brush","power_hero_brush");}
  public static void select(String key) {
    page=key.startsWith("cwe-native-")?Integer.parseInt(key.substring("cwe-native-".length())):-1;
    if(page<0||checked)return;var mc=Minecraft.getInstance();int items=0,blocks=0,faces=0;
    for(String id:ids()) {
      var stack=new ItemStack(ChaoGarden.item(id));var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
      var quads=model.getQuads(null,null,RandomSource.create(12));
      if(quads.size()<10||quads.stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))
        throw new IllegalStateException("02512 bad native item bake "+id+" quads="+quads.size());
      items++;faces+=quads.size();
    }
    for(String toy:List.of("chao_guitar","chao_accordion"))for(var state:com.chaogarden.toys.ToyContent.block(toy).getStateDefinition().getPossibleStates()){
      var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(15));
      if(quads.isEmpty()||quads.stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))throw new IllegalStateException("02515 bad toy bake "+state);
      blocks++;faces+=quads.size();
    }
    int contexts=0;
    for(String id:ids())for(var context:ItemDisplayContext.values()){
      var stack=new ItemStack(ChaoGarden.item(id));var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);var pose=new com.mojang.blaze3d.vertex.PoseStack();var chosen=model.applyTransform(context,pose,context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
      if(!pose.last().pose().isFinite()||pose.last().pose().determinant()<=0||chosen.getQuads(null,null,RandomSource.create(15)).stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))throw new IllegalStateException("02515 bad context "+id+"/"+context);contexts++;
    }
    LogUtils.getLogger().info("CHAO_CWE02516_CONTEXTS items={} contexts={} toyStates={} missingMaterials=0",items,contexts,blocks);
    checked=true;LogUtils.getLogger().info("CHAO_CWE02516_NATIVE items={} pods={} bakedQuads={} missingMaterials=0 actualModelManager=true",items,blocks,faces);
  }
  private static net.minecraft.server.level.ServerLevel fixtureLevel;
  private static final java.util.Map<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState> original=new java.util.LinkedHashMap<>();
  private static final java.util.List<java.util.UUID> actors=new ArrayList<>();
  public static void prepare(net.minecraft.server.level.ServerPlayer player,String key){
    if(fixtureLevel!=null){for(var id:actors){var e=fixtureLevel.getEntity(id);if(e!=null)e.discard();}original.forEach((p,v)->fixtureLevel.setBlock(p,v,3));actors.clear();original.clear();fixtureLevel=null;}
    if(!key.startsWith("cwe-wearables-"))return;
    int group=Integer.parseInt(key.substring("cwe-wearables-".length()));
    fixtureLevel=player.serverLevel();
    for(var p:net.minecraft.core.BlockPos.betweenClosed(-10,109,-3,10,115,8)){var at=p.immutable();original.put(at,fixtureLevel.getBlockState(at));fixtureLevel.setBlock(at,p.getY()==109?net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}
    int i=0;for(String hat:List.of("baseball_cap_accessory","bowtie_accessory","round_glasses_accessory","aviators_accessory","headphones_accessory","pacifier_accessory","press_hat_accessory","blue_hoodie_accessory","cheese_bowtie_accessory","pink_hoodie_accessory")){
      if(i/3!=group){i++;continue;}
      var c=ChaoGarden.CHAO.get().create(fixtureLevel);var state=new com.chaogarden.core.ChaoState();state.energy=90;state.hunger=90;c.setState(state);c.owner=player.getUUID();c.setNoAi(true);c.setNoGravity(true);c.moveTo(-1.6+(i%3)*1.6,110,.5,0,0);
      var wearable=(com.chaogarden.cwe.CweGiftItem)ChaoGarden.item(hat);c.setItemSlot(wearable.slot(),new ItemStack(wearable));
      if(wearable.slot()!=net.minecraft.world.entity.EquipmentSlot.CHEST)c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(ChaoGarden.item(i%2==0?"blue_hoodie_accessory":"pink_hoodie_accessory")));
      fixtureLevel.addFreshEntity(c);actors.add(c.getUUID());i++;
    }
    LogUtils.getLogger().info("CHAO_CWE02516_WEARABLES actors={} sourceWearables=true hoodieColors=2",actors.size());
  }
  @SubscribeEvent public static void draw(RenderGuiEvent.Post event) {
    if(page<0)return;var mc=Minecraft.getInstance();if(mc.level==null)return;
    var g=event.getGuiGraphics();var ids=ids();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
    g.fill(0,0,w,h,0xff1e2934);g.drawString(mc.font,"0.25.16 | Inventory icon + native held mesh | "+(page+1)+" / 2",8,7,0xffe3e8eb,false);
    for(int n=0;n<25;n++) {
      int index=page*25+n;if(index>=ids.size())break;String id=ids.get(index);var stack=new ItemStack(ChaoGarden.item(id));
      int x=8+(n%5)*125,y=25+(n/5)*65;
      g.fill(x,y,x+120,y+61,0xff2c3a47);g.renderItem(stack,x+9,y+15);
      g.flush();g.pose().pushPose();g.pose().translate(x+74,y+29,150);g.pose().scale(37,-37,37);
      g.pose().mulPose(Axis.XP.rotationDegrees(12));g.pose().mulPose(Axis.YP.rotationDegrees(25));
      mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,
          g.pose(),mc.renderBuffers().bufferSource(),mc.level,0);
      g.flush();g.pose().popPose();
      String label=id.replace("_accessory","").replace("_fruit","").replace("chao_","").replace('_',' ');
      g.drawString(mc.font,label,x+4,y+49,0xffdae4e9,false);
    }
    g.flush();
  }
}
