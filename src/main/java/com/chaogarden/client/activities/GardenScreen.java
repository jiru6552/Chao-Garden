package com.chaogarden.client.activities;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityNetwork;
import com.chaogarden.item.ChaoCarrierItem;
import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Read-only inventory drag previews. The server registers the UUID without moving the item. */
public final class GardenScreen extends Screen {
  private final BlockPos position;
  private JsonObject data;
  private final ItemStack[] icons=new ItemStack[6];
  private final UUID[] ids=new UUID[6];
  private int left,top,drag=-1,age;
  private UUID draggingId;
  private boolean awaiting;
  private float scale;
  private GardenScreen(BlockPos pos,JsonObject data){super(Component.literal("Chao Garden"));position=pos.immutable();accept(data);}
  public static void receive(BlockPos pos,JsonObject data){
    var mc=Minecraft.getInstance();
    if(mc.screen instanceof GardenScreen screen&&screen.position.equals(pos)){screen.accept(data);screen.rebuildWidgets();}
    else if(mc.screen==null&&data.has("open")&&data.get("open").getAsBoolean())mc.setScreen(new GardenScreen(pos,data));
  }
  private void accept(JsonObject value){
    data=value;awaiting=false;java.util.Arrays.fill(icons,ItemStack.EMPTY);java.util.Arrays.fill(ids,null);
    for(var item:data.getAsJsonArray("entries")){
      var row=item.getAsJsonObject();if(!row.get("selected").getAsBoolean())continue;
      int slot=row.get("slot").getAsInt();if(slot<0||slot>=6)continue;
      try {
        ids[slot]=UUID.fromString(row.get("id").getAsString());
        var stack=new ItemStack(ChaoGarden.item("chao_carrier"));var tag=new CompoundTag();
        tag.putUUID("UUID",ids[slot]);tag.putString("Chao",row.get("appearance").getAsString());ChaoCarrierItem.storeEntity(stack,tag);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal(row.get("name").getAsString()));icons[slot]=stack;
      }catch(RuntimeException ignored){ids[slot]=null;}
    }
  }
  private boolean active(){return data.has("active")&&data.get("active").getAsBoolean();}
  @Override protected void init(){
    scale=Math.min(1f,Math.min((width-12f)/320,(height-12f)/302));left=Math.round((width-320*scale)/2);top=Math.round((height-302*scale)/2);
    var button=addRenderableWidget(Button.builder(Component.literal(active()?"Recheck garden":"Activate garden"),b->send("activate",null)).bounds(12,33,144,20).build());button.active=!awaiting;
    addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(254,275,54,19).build());
  }
  private void send(String action,UUID id){awaiting=true;ActivityNetwork.request(position,action,id);rebuildWidgets();}
  private int x(double x){return (int)((x-left)/scale);}private int y(double y){return (int)((y-top)/scale);}
  private int inventoryAt(int x,int y){
    if(x<79||x>=241)return -1;
    int col=(x-79)/18;
    if(y>=183&&y<237)return 9+(y-183)/18*9+col;
    if(y>=241&&y<259)return col;
    return -1;
  }
  private int gardenAt(int x,int y){if(y<76||y>=108)return -1;for(int i=0;i<6;i++)if(x>=13+i*50&&x<45+i*50)return i;return -1;}
  @Override public boolean mouseClicked(double mx,double my,int button){
    int x=x(mx),y=y(my);if(button==0&&!awaiting){
      int slot=gardenAt(x,y);if(slot>=0&&ids[slot]!=null){send("toggle",ids[slot]);return true;}
      int inv=inventoryAt(x,y);if(inv>=0&&minecraft.player!=null){var stack=minecraft.player.getInventory().getItem(inv);
        if(stack.getItem() instanceof ChaoCarrierItem&&ChaoCarrierItem.filled(stack)){drag=inv;draggingId=ChaoCarrierItem.storedUUID(stack);return true;}
      }
    }
    return super.mouseClicked(x,y,button);
  }
  @Override public boolean mouseReleased(double mx,double my,int button){
    if(button==0&&drag>=0){int slot=gardenAt(x(mx),y(my));if(slot>=0&&ids[slot]==null&&active()&&draggingId!=null&&!awaiting){
        awaiting=true;ActivityNetwork.registerCarried(position,drag,slot,draggingId);rebuildWidgets();
      }drag=-1;draggingId=null;return true;}
    return super.mouseReleased(x(mx),y(my),button);
  }
  @Override public void tick(){
    if(minecraft.player==null||minecraft.player.distanceToSqr(position.getCenter())>144){onClose();return;}
    // Polling is read-only and must never swallow the start of a user's drag.
    if(++age%40==0&&drag<0&&!awaiting)ActivityNetwork.request(position,"refresh",null);
  }
  @Override public void render(GuiGraphics g,int mx,int my,float partial){
    renderBackground(g,mx,my,partial);g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);
    g.fill(-2,-2,322,304,0xff2479b3);g.fill(0,0,320,302,0xffeffafd);
    g.drawString(font,"CHAO GARDEN",12,12,0xff204777,false);
    g.drawString(font,active()?"Active":"Inactive",244,13,active()?0xff26804a:0xff98674c,false);
    g.drawString(font,"Registered residents",12,61,0xff204777,false);
    for(int i=0;i<6;i++){
      int px=13+i*50;g.fill(px-1,75,px+33,109,0xff64849f);g.fill(px,76,px+32,108,0xffd7f0f4);
      if(!icons[i].isEmpty()) {g.pose().pushPose();g.pose().translate(px-8,60,0);g.pose().scale(3f,3f,1);g.renderItem(icons[i],0,0);g.pose().popPose();
        g.drawString(font,font.plainSubstrByWidth(icons[i].getHoverName().getString(),46),px-4,113,0xff204777,false);
      }else g.drawCenteredString(font,"+",px+16,87,0xff64849f);
    }
    String status=data.get("status").getAsString();int yy=133;
    for(var line:font.split(Component.literal(status),296)){if(yy>168)break;g.drawString(font,line,12,yy,0xff204777,false);yy+=10;}
    g.drawString(font,"Inventory",79,171,0xff64849f,false);
    if(minecraft.player!=null)for(int i=0;i<36;i++){
      int px=79+(i%9)*18,py=i<9?241:183+(i/9-1)*18;g.fill(px,py,px+18,py+18,0xff64849f);g.fill(px+1,py+1,px+17,py+17,0xffe5f2f6);
      var stack=minecraft.player.getInventory().getItem(i);g.renderItem(stack,px+1,py+1);g.renderItemDecorations(font,stack,px+1,py+1);
    }
    g.drawString(font,"Chao stay in your inventory.",12,279,0xff64849f,false);
    for(var widget:renderables)widget.render(g,x(mx),y(my),partial);
    if(drag>=0&&minecraft.player!=null)g.renderItem(minecraft.player.getInventory().getItem(drag),x(mx)-8,y(my)-8);
    int hover=gardenAt(x(mx),y(my));if(drag<0&&hover>=0&&ids[hover]!=null)g.renderTooltip(font,Component.literal("Left-click to unregister "+icons[hover].getHoverName().getString()),x(mx),y(my));
    g.pose().popPose();
  }
  @Override public boolean isPauseScreen(){return false;}
}
