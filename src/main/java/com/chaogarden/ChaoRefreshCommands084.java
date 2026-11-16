package com.chaogarden;

import com.chaogarden.companion.CompanionContent;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Explicit operator maintenance. Saved creature identity and biological data
 * are never reconstructed from training, appearance or the current calendar. */
public final class ChaoRefreshCommands084 {
  private ChaoRefreshCommands084() {}
  private static final String USAGE="Usage: /chao refresh <name or ~> or /chao refresh held. Quote names containing spaces; use \"held\" for a Chao named held.";
  private static boolean authorized(CommandSourceStack source){return source.hasPermission(2);}
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
    dispatcher.register(Commands.literal("chao").then(Commands.literal("refresh")
        .requires(ChaoRefreshCommands084::authorized).executes(c->usage(c.getSource()))
        .then(Commands.literal("~").executes(c->refresh(c.getSource(),"~")))
        .then(Commands.literal("held").executes(c->held(c.getSource())))
        .then(Commands.argument("chaoName",StringArgumentType.string())
            .suggests((c,b)->SharedSuggestionProvider.suggest(EditingCommands056.loadedNames(c.getSource()),b))
            .executes(c->refresh(c.getSource(),StringArgumentType.getString(c,"chaoName"))))));
  }
  private static int usage(CommandSourceStack source){source.sendFailure(Component.literal(USAGE));return 0;}
  private static boolean permission(CommandSourceStack source){
    if(authorized(source))return true;
    source.sendFailure(Component.literal("Chao refresh requires operator permission level 2."));return false;
  }
  private static boolean available(ChaoEntity chao){
    return !chao.isCocooned()&&chao.state().intTrait("content.construction")==0
        &&!com.chaogarden.activities.ActivityManager.isBusy(chao)
        &&!com.chaogarden.toys.ToyPreview.active(chao)
        &&!com.chaogarden.companion.CarryPreview.active(chao);
  }
  public static int refresh(CommandSourceStack source,String target){
    if(!permission(source))return 0;
    var chao=EditingCommands056.select(source,target);if(chao==null)return 0;
    if(!available(chao)){
      source.sendFailure(Component.literal("That Chao is in a cocoon, construction, activity or operator preview. Retry when it is available; no data changed."));return 0;
    }
    chao.refreshGardenAppearance084();CompanionContent.itemStateChanged(chao);
    source.sendSuccess(()->Component.literal(chao.getName().getString()+": current garden appearance refreshed. Official/reference overrides cleared and clients synchronized; saved age, evolution identity, training, genes and equipment preserved."),true);
    return 1;
  }
  /** The main-hand stack is explicit. No inventory, offline-player or world scan
   * can accidentally select a different saved creature. Raw state is retained. */
  public static int held(CommandSourceStack source){
    if(!permission(source))return 0;
    if(!(source.getEntity() instanceof ServerPlayer player)){
      source.sendFailure(Component.literal("Hold the filled Chao in your main hand as an online player, then /chao refresh held."));return 0;
    }
    var stack=player.getMainHandItem();
    if(!(stack.getItem() instanceof ChaoCarrierItem)||!ChaoCarrierItem.filled(stack)){
      source.sendFailure(Component.literal("Hold the filled Chao item in your main hand. Equipped Chao use /chao refresh <name or ~>."));return 0;
    }
    var id=ChaoCarrierItem.storedUUID(stack);var saved=ChaoCarrierItem.entityData(stack);
    if(id==null||saved.getString("Chao").isEmpty()){
      source.sendFailure(Component.literal("The held item lacks a saved Chao identity/state. It was left unchanged."));return 0;
    }
    if(CompanionContent.itemIsSummoned(stack)||CompanionContent.hasManagedIdentity084(id)
        ||CompanionContent.findEntity(player.server,id)!=null){
      source.sendFailure(Component.literal("That Chao is already equipped, managed or present in a world. Refresh its actual loaded body instead; the held copy was left unchanged."));return 0;
    }
    if(!ChaoCarrierItem.canRelease(stack,player)){
      source.sendFailure(Component.literal("The held Chao is owned by another keeper or its recovery is not available. It was left unchanged."));return 0;
    }
    // Normal release/load already owns supported legacy decoding. Re-encoding
    // here could guess new data for a damaged/old string, so retain it verbatim.
    saved.putBoolean("OfficialComparison",false);saved.remove("SourceReferenceRig");
    ChaoCarrierItem.storeEntity(stack,saved);player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    source.sendSuccess(()->Component.literal("Held Chao "+id+": current garden appearance selected. Only official/reference overrides changed; saved creature data and item components retained. Normal decoding/migration still occurs on release."),true);
    return 1;
  }
}
