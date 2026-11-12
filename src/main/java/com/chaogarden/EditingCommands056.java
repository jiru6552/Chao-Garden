package com.chaogarden;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.CreativeChaoEditRules056;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Server-authoritative, explicit-target Creative/admin testing commands. */
public final class EditingCommands056 {
  public static final int NEAREST_RADIUS=12;
  private static final String USAGE="Usage: /chao <name or ~> <-100..100 alignment> <Power|Fly|Run|Swim|Intelligence|Luck> <0..99 level>. Quote names containing spaces.";
  private EditingCommands056() {}

  public static boolean authorized(CommandSourceStack source) {
    return source.hasPermission(2)||(source.getEntity() instanceof ServerPlayer player&&player.isCreative()&&!player.isSpectator());
  }
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // Brigadier's standard unquoted-string parser does not admit '~'. A literal
    // keeps the requested syntax and uses vanilla command-tree serialization.
    dispatcher.register(Commands.literal("chao").executes(context->usage(context.getSource()))
        .then(Commands.literal("~").requires(EditingCommands056::authorized)
            .executes(context->usage(context.getSource())).then(editArguments(true)))
        .then(Commands.argument("chaoName",StringArgumentType.string())
            .requires(EditingCommands056::authorized)
            .suggests((context,builder)->SharedSuggestionProvider.suggest(loadedNames(context.getSource()),builder))
            .executes(context->usage(context.getSource())).then(editArguments(false))));
  }
  private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,Integer> editArguments(boolean nearest) {
    return Commands.argument("alignment",IntegerArgumentType.integer(-100,100))
        .executes(context->usage(context.getSource()))
        .then(Commands.argument("stat",StringArgumentType.word())
            .suggests((context,builder)->SharedSuggestionProvider.suggest(CreativeChaoEditRules056.STAT_NAMES,builder))
            .executes(context->usage(context.getSource()))
            .then(Commands.argument("level",IntegerArgumentType.integer(0,99))
                .executes(context->edit(context.getSource(),nearest?"~":StringArgumentType.getString(context,"chaoName"),
                    IntegerArgumentType.getInteger(context,"alignment"),StringArgumentType.getString(context,"stat"),
                    IntegerArgumentType.getInteger(context,"level")))));
  }
  private static int usage(CommandSourceStack source) {source.sendFailure(Component.literal(USAGE));return 0;}
  private static boolean actual(ChaoEntity chao) {
    return chao.isAlive()&&!chao.state().dead&&!(chao instanceof com.chaogarden.kindergarten.KindergartenPupil0257);
  }
  private static List<String> loadedNames(CommandSourceStack source) {
    var names=new java.util.TreeSet<String>();names.add("~");
    for(var entity:source.getLevel().getAllEntities())if(entity instanceof ChaoEntity chao&&actual(chao)) {
      String name=chao.getName().getString();
      // Quote even single words: an existing literal such as "rebirth" must not win over the name.
      names.add("\""+name.replace("\\","\\\\").replace("\"","\\\"")+"\"");
    }
    return new ArrayList<>(names);
  }
  /** Searches loaded real Chao in the source dimension; never picks the first matching duplicate. */
  public static ChaoEntity select(CommandSourceStack source,String name) {
    if(name==null||name.isBlank()) {source.sendFailure(Component.literal("A Chao name or ~ is required. "+USAGE));return null;}
    if(name.equals("~")) {
      var candidates=source.getLevel().getEntitiesOfClass(ChaoEntity.class,
          new AABB(source.getPosition(),source.getPosition()).inflate(NEAREST_RADIUS),
          chao->actual(chao)&&chao.position().distanceToSqr(source.getPosition())<=NEAREST_RADIUS*NEAREST_RADIUS);
      candidates.sort(Comparator.comparingDouble(chao->chao.position().distanceToSqr(source.getPosition())));
      if(candidates.isEmpty()) {source.sendFailure(Component.literal("No Chao within 12 blocks in this dimension."));return null;}
      if(candidates.size()>1&&Math.abs(candidates.get(0).position().distanceToSqr(source.getPosition())
          -candidates.get(1).position().distanceToSqr(source.getPosition()))<.000001) {
        source.sendFailure(Component.literal("Two Chao are equally near. Move closer to one or use a unique name."));return null;
      }
      return candidates.get(0);
    }
    var matching=new ArrayList<ChaoEntity>();
    for(var entity:source.getLevel().getAllEntities())if(entity instanceof ChaoEntity chao&&actual(chao)
        &&chao.getName().getString().equalsIgnoreCase(name))matching.add(chao);
    if(matching.isEmpty()) {source.sendFailure(Component.literal("No loaded Chao named \""+name+"\" in this dimension. Check its name and load its area."));return null;}
    if(matching.size()>1) {source.sendFailure(Component.literal("Ambiguous Chao name \""+name+"\": "+matching.size()+" loaded matches. Rename one, or stand beside the intended Chao and use ~."));return null;}
    return matching.get(0);
  }
  public static int edit(CommandSourceStack source,String name,int alignment,String statName,int level) {
    if(!authorized(source)) {source.sendFailure(Component.literal("Chao editing requires Creative mode or operator permission level 2."));return 0;}
    int stat=CreativeChaoEditRules056.stat(statName);
    if(stat<0) {source.sendFailure(Component.literal("Unknown stat \""+statName+"\". Choose Power, Fly, Run, Swim, Intelligence or Luck. All use levels 0..99; Intelligence/Luck earned points are separate 0..4000 values."));return 0;}
    if(alignment< -100||alignment>100||level<0||level>99)return usage(source);
    var chao=select(source,name);if(chao==null)return 0;
    if(chao.isCocooned()||com.chaogarden.activities.ActivityManager.isBusy(chao)||chao.state().intTrait("content.construction")>0) {
      source.sendFailure(Component.literal("That Chao is in a cocoon, activity or construction state. Wait until it is available."));return 0;
    }
    var state=chao.state();
    if(!CreativeChaoEditRules056.alignmentAllowed(state,alignment)) {
      source.sendFailure(Component.literal(state.adult?"Adult evolution alignment is locked at "+state.alignmentName()
          +" ("+signed(CreativeChaoEditRules056.alignmentCenti(state))+"). No values changed. Repeat with that alignment number to edit the stat."
          :"This special Chao's alignment is read-only. No values changed."));return 0;
    }
    if(!CreativeChaoEditRules056.apply(state,alignment,stat,level))return 0;
    chao.sync();com.chaogarden.companion.CompanionContent.itemStateChanged(chao);
    int points=stat==CreativeChaoEditRules056.LUCK?state.luck:stat==CreativeChaoEditRules056.INTELLIGENCE?state.intelligence:state.points[stat];
    String hidden=stat>=CreativeChaoEditRules056.INTELLIGENCE?"; hidden earned points "+points+"/4000":"; earned points "+points+"/9999";
    source.sendSuccess(()->Component.literal(chao.getName().getString()+": "+CreativeChaoEditRules056.statName(stat)
        +" level "+level+"/99, progress reset to 0"+hidden+"; alignment "+signed(CreativeChaoEditRules056.alignmentCenti(state))
        +(state.adult?" (adult identity unchanged and locked).":". First type will be selected at natural evolution.")),true);
    return 1;
  }
  private static String signed(int value) {return value>0?"+"+value:Integer.toString(value);}
}
