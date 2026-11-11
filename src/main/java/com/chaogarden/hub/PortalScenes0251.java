package com.chaogarden.hub;

import com.chaogarden.core.PortalGeometry0251;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Anchor X/Z use the block center; Y is the aperture's standing-floor height. */
public final class PortalScenes0251 {
  public static final int RETURN=PortalGeometry0251.RETURN,SCHOOL=PortalGeometry0251.SCHOOL,GARDEN=PortalGeometry0251.GARDEN,TUNNEL=PortalGeometry0251.TUNNEL,HERO=PortalGeometry0251.HERO,DARK=PortalGeometry0251.DARK;
  public static final BlockPos HUB_SCHOOL=new BlockPos(0,80,-23),HUB_GARDEN=new BlockPos(0,80,23),SCHOOL_RETURN=new BlockPos(0,80,49);
  private PortalScenes0251(){}
  public static BlockState state(Direction facing,int style){
    if(facing.getAxis().isVertical()||style<RETURN||style>DARK)throw new IllegalArgumentException("Invalid Chao portal anchor");
    return LobbyDecor025.SCENE_ANCHOR.get().defaultBlockState().setValue(LobbyDecor025.SceneAnchor.PORTAL,true)
        .setValue(LobbyDecor025.SceneAnchor.FACING,facing).setValue(LobbyDecor025.SceneAnchor.STYLE,style);
  }
  public static void place(ScenePlan025 plan,BlockPos anchor,Direction facing,int style){plan.put(anchor.getX(),anchor.getY(),anchor.getZ(),state(facing,style));}
}
