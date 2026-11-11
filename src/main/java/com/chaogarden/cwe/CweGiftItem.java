package com.chaogarden.cwe;

import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Physical gifts enter the existing think/use/commit animation before changing anything. */
public class CweGiftItem extends Item {
  public final CweCatalog.Gift gift;
  public CweGiftItem(CweCatalog.Gift gift){this(gift,new Properties().stacksTo(gift.kind()==CweCatalog.Kind.ACCESSORY?1:16));}
  protected CweGiftItem(CweCatalog.Gift gift,Properties properties){super(properties);this.gift=gift;}
  public EquipmentSlot slot(){return gift.slot();}
  @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){return target instanceof ChaoEntity chao?chao.offerItem(player,hand):InteractionResult.PASS;}
  public boolean canUse(ChaoEntity chao,ItemStack stack){
    var s=chao.state();
    if(s.dead||s.intTrait("content.construction")>0||chao.isCocooned()||s.sleeping)return false;
    return switch(gift.kind()){
      case ACCESSORY,WINGS,MIRROR -> true;
      case LENS -> gift.value()>=0 && gift.value()<12 && s.hatsUnlocked;
      case POTION -> s.adult && !s.immortal;
      case BRUSH -> s.adult && s.align>=.5f && !s.immortal && "normal".equals(s.character);
      case PARTS -> CweContent.canAssembleOmochao(chao,stack);
    };
  }
  public void apply(ChaoEntity chao,ItemStack stack,Player player){
    if(chao.level().isClientSide || !canUse(chao,stack))return;
    var s=chao.state();
    switch(gift.kind()){
      case LENS -> s.eyeColor=gift.value();
      case WINGS -> s.cosmeticWings=!s.cosmeticWings;
      case MIRROR -> s.negativePalette=!s.negativePalette;
      case POTION -> s.adultAgeTicks=Math.max(20,com.chaogarden.GardenConfig.ADULT_TICKS.get());
      case BRUSH -> s.heroBrushType=gift.value();
      case ACCESSORY -> {
        // This public compatibility entry point cannot force an outfit. Normal
        // accepted gifts commit inside ChaoEntity after its latched choice.
        if(player!=null)chao.offerWardrobeItem(player,stack);
      }
      case PARTS -> {if(!CweContent.assembleOmochao(chao,stack))giveBack(chao,stack.copyWithCount(1),player);}
    }
    chao.sync();
  }
  private static void giveBack(ChaoEntity chao,ItemStack stack,Player player){
    if(player!=null&&player.addItem(stack))return;
    var dropped=player==null?chao.spawnAtLocation(stack):player.drop(stack,false);
    if(dropped!=null){dropped.getPersistentData().putLong("ChaoRejectedUntil",chao.level().getGameTime()+200);dropped.setDefaultPickUpDelay();}
  }
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tips,TooltipFlag flag){
    String effect=switch(gift.kind()){
      case LENS -> "Changes the iris style; clear lens restores ordinary eyes. Requires Skeleton Dog genes.";
      case ACCESSORY -> "Wearable "+switch(slot()){case FEET->"head accessory";case OFFHAND->"face accessory";case LEGS->"neck accessory";default->"outfit";}+". Your Chao chooses whether to wear it.";
      case WINGS -> "Toggles matching the existing wings to the body coat.";
      case MIRROR -> "Toggles an inverted body palette.";
      case POTION -> "Ends an adult's current life cycle; happiness determines reincarnation or death.";
      case BRUSH -> "Changes an adult Hero Chao's accent colors while preserving its skill type.";
      case PARTS -> "A wrench-trained Chao builds Omochao with four parts. Stay near the unfinished robot.";
    };
    tips.add(Component.literal(effect).withStyle(ChatFormatting.AQUA));
  }
}
