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
public final class Fruit02512VisualSmoke {
  private static int page=-1;private static boolean checked;
  private static List<String> ids(){var out=new ArrayList<>(FruitCatalog.registeredFoods());
    FruitCatalog.TREES.forEach(f->out.add(f+"_seed"));out.add("chao_watering_can");out.add("chao_shovel");return out;}
  public static void select(String key) {
    page=key.startsWith("fruit-native-")?Integer.parseInt(key.substring("fruit-native-".length())):-1;
    if(page<0||checked)return;var mc=Minecraft.getInstance();int items=0,blocks=0,faces=0;
    for(String id:ids()) {
      var stack=new ItemStack(ChaoGarden.item(id));var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
      var quads=model.getQuads(null,null,RandomSource.create(12));
      if(quads.size()<18||quads.stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))
        throw new IllegalStateException("02512 bad native item bake "+id+" quads="+quads.size());
      items++;faces+=quads.size();
    }
    for(String fruit:FruitCatalog.TREES)for(int age=0;age<4;age++) {
      var state=FruitContent.podForFruit(fruit).defaultBlockState().setValue(com.chaogarden.fruit.HangingFruitBlock.AGE,age);
      var quads=mc.getBlockRenderer().getBlockModel(state).getQuads(state,null,RandomSource.create(12));
      if(quads.size()<18||quads.stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))
        throw new IllegalStateException("02512 bad native pod bake "+fruit+"/"+age+" quads="+quads.size());
      blocks++;faces+=quads.size();
    }
    checked=true;LogUtils.getLogger().info("CHAO_FRUIT02512_NATIVE items={} pods={} bakedQuads={} missingMaterials=0 actualModelManager=true",items,blocks,faces);
  }
  @SubscribeEvent public static void draw(RenderGuiEvent.Post event) {
    if(page<0)return;var mc=Minecraft.getInstance();if(mc.level==null)return;
    var g=event.getGuiGraphics();var ids=ids();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
    g.fill(0,0,w,h,0xff1e2934);g.drawString(mc.font,"0.25.14 | Inventory icon + native held mesh | "+(page+1)+" / 3",8,7,0xffe3e8eb,false);
    for(int n=0;n<25;n++) {
      int index=page*25+n;if(index>=ids.size())break;String id=ids.get(index);var stack=new ItemStack(ChaoGarden.item(id));
      int x=8+(n%5)*125,y=25+(n/5)*65;
      g.fill(x,y,x+120,y+61,0xff2c3a47);g.renderItem(stack,x+9,y+15);
      g.flush();g.pose().pushPose();g.pose().translate(x+74,y+29,150);g.pose().scale(37,-37,37);
      g.pose().mulPose(Axis.XP.rotationDegrees(12));g.pose().mulPose(Axis.YP.rotationDegrees(25));
      mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,
          g.pose(),mc.renderBuffers().bufferSource(),mc.level,0);
      g.flush();g.pose().popPose();
      String label=id.replace("_fruit","").replace("chao_","").replace('_',' ');
      g.drawString(mc.font,label,x+4,y+49,0xffdae4e9,false);
    }
    g.flush();
  }
}
