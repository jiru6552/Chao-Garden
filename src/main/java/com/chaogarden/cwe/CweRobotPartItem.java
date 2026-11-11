package com.chaogarden.cwe;

import com.chaogarden.core.ChaoState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** Recovered parts are real, considered gifts; hearts carry one bounded previous-creator snapshot. */
public final class CweRobotPartItem extends CweGiftItem {
  public static final String HEART_DATA="OmochaoHeart";
  private final boolean heart;
  public CweRobotPartItem(boolean heart){
    super(new CweCatalog.Gift(heart?"robo_heart":"robot_component",CweCatalog.Kind.PARTS,heart?2:1,0,null,false),
        new Properties().stacksTo(heart?1:16));this.heart=heart;
  }
  public boolean isHeart(){return heart;}
  public static CompoundTag heartData(ItemStack stack){
    if(!(stack.getItem() instanceof CweRobotPartItem part)||!part.heart)return new CompoundTag();
    var data=stack.get(DataComponents.CUSTOM_DATA);
    return data==null?new CompoundTag():data.copyTag().getCompound(HEART_DATA);
  }
  public static boolean validHeart(ItemStack stack){
    var data=heartData(stack);int rank=data.getInt("Rank");
    if(data.getInt("Version")!=1||rank<3||rank>5)return false;
    for(String key:new String[]{"CreatorId","CreatorDna","CreatorName"}){
      String value=data.getString(key);if(value.length()>128||value.chars().anyMatch(c->c<32||c>126))return false;
    }
    String name=data.getString("CreatorName");if(!name.isEmpty()&&ChaoState.decodeFamilyName(name).isEmpty())return false;
    String id=data.getString("CreatorId");return id.isEmpty()||id.matches("[0-9a-f]{16}");
  }
  public static ItemStack heart(ChaoState previous){
    var stack=new ItemStack(com.chaogarden.ChaoGarden.item("robo_heart"));var data=new CompoundTag();
    data.putInt("Version",1);data.putInt("Rank",com.chaogarden.core.OmochaoRules.nextHeartRank(previous));
    data.putString("CreatorId",previous.trait("family.parent.0.id"));
    data.putString("CreatorDna",previous.trait("family.parent.0.dna"));
    data.putString("CreatorName",previous.trait("family.parent.0.name"));
    var tag=new CompoundTag();tag.put(HEART_DATA,data);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
  }
  public static ItemStack recovered(ChaoState previous,java.util.UUID actor,boolean heart){
    ItemStack stack=heart?heart(previous):new ItemStack(com.chaogarden.ChaoGarden.item("omochao_parts_box"));
    var old=stack.get(DataComponents.CUSTOM_DATA);var tag=old==null?new CompoundTag():old.copyTag();
    var origin=new CompoundTag();origin.putUUID("Robot",actor);origin.putString("BuilderToken",previous.trait("content.builder_token"));
    tag.put("OmochaoOrigin",origin);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
  }
  public static CompoundTag origin(ItemStack stack){
    if(!(stack.getItem() instanceof CweRobotPartItem)&&!stack.is(com.chaogarden.ChaoGarden.item("omochao_parts_box")))return new CompoundTag();
    var data=stack.get(DataComponents.CUSTOM_DATA);return data==null?new CompoundTag():data.copyTag().getCompound("OmochaoOrigin");
  }
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tips,TooltipFlag flag){
    if(!heart)return;
    tips.add(Component.literal("Rebuilds a cared-for Omochao with higher skill grades: C → B → A → S.").withStyle(ChatFormatting.AQUA));
    tips.add(Component.literal("Give to a wrench-trained Chao with three Parts Boxes; one heart per robot.").withStyle(ChatFormatting.AQUA));
    if(validHeart(stack))tips.add(Component.literal("This heart gives all "+ChaoState.gradeName(heartData(stack).getInt("Rank"))+" grades and preserves the previous creator as grandparent.").withStyle(ChatFormatting.AQUA));
    else tips.add(Component.literal("Uncalibrated: obtain a heart when a well-cared-for Omochao reaches the end of its life.").withStyle(ChatFormatting.AQUA));
  }
}
