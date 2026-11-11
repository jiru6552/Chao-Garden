package com.chaogarden.item;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class CombinedDriveItem extends CareItem {
  public final DriveRules.Tier tier;
  public CombinedDriveItem(DriveRules.Tier tier,int stat){super(Kind.DRIVE,tier.name(),stat);this.tier=tier;}
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tips,TooltipFlag flag){
    if(tier==DriveRules.Tier.UNSTABLE_CHAOTIC){
      tips.add(Component.literal("ARE YOU SURE YOU WANT TO USE THIS???").withStyle(ChatFormatting.ITALIC));return;
    }
    String skill=DriveRules.typed(tier)?ChaoState.STATS[index]:"";
    String text=switch(tier){
      case SOLID -> skill+": +1 full level; keeps current progress.";
      case SUPER_SOLID -> skill+": +10 full levels; keeps current progress.";
      case CHAOTIC -> "Swim, Fly, Run and Power: +24% training progress each.";
      case SUPER_CHAOTIC -> "Randomly assigns +24% progress, +1 level, +5 levels and -2 levels to four different skills.";
      case UNSTABLE_SOLID -> skill+": 5% chance of level 99; 95% chance of level 0.";
      case PERFECT -> "Raises one random non-maxed skill to level 99, including Stamina.";
      default -> "";
    };
    tips.add(Component.literal(text).withStyle(ChatFormatting.AQUA));
    if(tier==DriveRules.Tier.PERFECT){
      tips.add(Component.literal("Rank is unchanged. Returned if all five skills are maxed.").withStyle(ChatFormatting.DARK_AQUA));return;
    }
    if(tier==DriveRules.Tier.UNSTABLE_SOLID)
      tips.add(Component.literal("Independent rolls: happiness +1/-1, luck +40%/-40%, intelligence +20%/-20% training.").withStyle(ChatFormatting.DARK_AQUA));
    else tips.add(Component.literal("+1 happiness, +40% luck and +20% intelligence training.").withStyle(ChatFormatting.DARK_AQUA));
    tips.add(Component.literal("Levels stop at 0/99; level 99 has no progress. Rank is unchanged.").withStyle(ChatFormatting.GRAY));
  }
}
