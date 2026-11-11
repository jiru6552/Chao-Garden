package com.chaogarden.market.client;

import com.chaogarden.fruit.FruitCatalog;
import com.chaogarden.market.MarketStock;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/** Stock tabs filter the real vanilla offers; server trade indices never change. */
public final class CweMarketScreen extends AbstractContainerScreen<MerchantMenu> {
  private static final String[] CATEGORIES={"All","Eggs","Fruit","Seeds","Hats","Gear","Toys","Train"};
  private static final ResourceLocation PANEL=ResourceLocation.withDefaultNamespace("textures/gui/container/villager.png");
  private final long refreshAt;
  private final List<Button> offers=new ArrayList<>();
  private int category,page,selected=-1,lastCount=-1;
  public CweMarketScreen(MerchantMenu menu,Inventory inventory,Component title,long refreshAt){
    super(menu,inventory,title);imageWidth=276;inventoryLabelX=107;this.refreshAt=refreshAt;
  }
  public int selectedCategory(){return category;}
  public void selectCategory(int value){category=Math.max(0,Math.min(7,value));page=0;rebuildWidgets();}
  private static int category(ItemStack stack){
    String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    if(id.endsWith("_egg")||id.endsWith("_chao_egg"))return 1;
    if(id.endsWith("_seed"))return 3;
    if(FruitCatalog.ACTIVE.contains(id))return 2;
    if(MarketStock.HATS.contains(id))return 4;
    if(MarketStock.TOYS.contains(id)||MarketStock.LEGACY_TOYS.contains(id))return 6;
    if(MarketStock.DRIVES.contains(id)||MarketStock.ORDINARY_GENES.contains(id)||MarketStock.MYTHICAL_GENES.contains(id))return 7;
    if(id.endsWith("_lens"))return 5;
    return 5;
  }
  private List<Integer> visible(){
    var list=new ArrayList<Integer>();for(int i=0;i<menu.getOffers().size();i++)
      if(category==0||category(menu.getOffers().get(i).getResult())==category)list.add(i);
    return list;
  }
  @Override protected void init(){
    super.init();
    // Centre the complete panel, including its tabs and footer, at GUI scale3.
    topPos=(height-233)/2+42;
    offers.clear();lastCount=menu.getOffers().size();
    for(int i=0;i<CATEGORIES.length;i++){
      final int tab=i;
      var button=Button.builder(Component.literal(CATEGORIES[i]),b->selectCategory(tab))
          .bounds(leftPos+i*34,topPos-22,33,19).build();button.active=category!=i;addRenderableWidget(button);
    }
    var filtered=visible();page=Math.max(0,Math.min(page,Math.max(0,(filtered.size()-1)/7)));
    for(int row=0;row<7;row++){
      int at=page*7+row;if(at>=filtered.size())break;final int index=filtered.get(at);
      var button=Button.builder(Component.empty(),b->selectOffer(index))
          .bounds(leftPos+5,topPos+18+row*20,88,20).build();offers.add(button);addRenderableWidget(button);
    }
    var previous=Button.builder(Component.literal("<"),b->{page--;rebuildWidgets();})
        .bounds(leftPos+5,topPos+169,22,18).build();previous.active=page>0;addRenderableWidget(previous);
    var next=Button.builder(Component.literal(">"),b->{page++;rebuildWidgets();})
        .bounds(leftPos+71,topPos+169,22,18).build();next.active=(page+1)*7<filtered.size();addRenderableWidget(next);
    addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose())
        .bounds(leftPos+224,topPos+169,48,18).build());
  }
  private void selectOffer(int index){
    if(index<0||index>=menu.getOffers().size()||minecraft==null||minecraft.getConnection()==null)return;
    selected=index;menu.setSelectionHint(index);menu.tryMoveItems(index);
    minecraft.getConnection().send(new ServerboundSelectTradePacket(index));
  }
  @Override protected void containerTick(){
    super.containerTick();if(menu.getOffers().size()!=lastCount)rebuildWidgets();
    if(minecraft!=null&&minecraft.level!=null&&minecraft.level.getGameTime()>=refreshAt){
      onClose();if(minecraft.player!=null)minecraft.player.displayClientMessage(Component.literal("New Black Market stock is ready. Speak to the trader again."),true);
    }
  }
  @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
    g.fill(leftPos-3,topPos-42,leftPos+imageWidth+3,topPos+191,0xff154b68);
    g.fill(leftPos-1,topPos-40,leftPos+imageWidth+1,topPos+189,0xffe6f4f3);
    g.blit(PANEL,leftPos,topPos,0,0,0,imageWidth,imageHeight,512,256);
    g.fill(leftPos+3,topPos+15,leftPos+97,topPos+161,0xffd2e9e7);
    int secs=minecraft==null||minecraft.level==null?0:(int)Math.max(0,(refreshAt-minecraft.level.getGameTime()+19)/20);
    g.drawString(font,"BLACK MARKET",leftPos+5,topPos-36,0xff16485c,false);
    g.drawString(font,String.format(Locale.ROOT,"Stock %d:%02d",secs/60,secs%60),leftPos+191,topPos-36,0xff416477,false);
    g.drawCenteredString(font,(page+1)+" / "+Math.max(1,(visible().size()+6)/7),leftPos+49,topPos+174,0xff154b68);
  }
  @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
    g.drawString(font,"Trade",140,6,0xff16485c,false);
    g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xff16485c,false);
    if(selected>=0&&selected<menu.getOffers().size()){
      String text=font.plainSubstrByWidth(menu.getOffers().get(selected).getResult().getHoverName().getString(),164);
      g.drawString(font,text,105,61,0xff416477,false);
    }
  }
  @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
    super.render(g,mouseX,mouseY,partial);var visible=visible();ItemStack hovered=ItemStack.EMPTY;
    for(int row=0;row<7;row++){
      int at=page*7+row;if(at>=visible.size())break;MerchantOffer offer=menu.getOffers().get(visible.get(at));
      int x=leftPos+8,y=topPos+20+row*20;
      ItemStack[] stacks={offer.getCostA(),offer.getCostB(),offer.getResult()};int[] offsets={0,22,62};
      for(int i=0;i<3;i++)if(!stacks[i].isEmpty()){
        g.renderItem(stacks[i],x+offsets[i],y);g.renderItemDecorations(font,stacks[i],x+offsets[i],y);
        if(mouseX>=x+offsets[i]&&mouseX<x+offsets[i]+16&&mouseY>=y&&mouseY<y+16)hovered=stacks[i];
      }
      g.drawString(font,offer.isOutOfStock()?"x":">",x+46,y+4,offer.isOutOfStock()?0xffbd5151:0xff416477,false);
      if(visible.get(at)==selected)g.renderOutline(leftPos+5,topPos+18+row*20,88,20,0xff14a3bc);
    }
    renderTooltip(g,mouseX,mouseY);if(!hovered.isEmpty())g.renderTooltip(font,hovered,mouseX,mouseY);
  }
}
