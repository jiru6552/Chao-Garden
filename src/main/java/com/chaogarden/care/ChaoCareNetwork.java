package com.chaogarden.care;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.AlignmentEditRules053;
import com.chaogarden.core.MaturityEditRules056;
import com.chaogarden.entity.ChaoEntity;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded requests; no client-supplied stats, prices, items or colours are trusted. */
public final class ChaoCareNetwork {
  private ChaoCareNetwork() {}
  private static ResourceLocation id(String name) {return ResourceLocation.fromNamespaceAndPath("chao_garden",name);}
  public record Action(String chao,String action,String value) implements CustomPacketPayload {
    public static final Type<Action> TYPE=new Type<>(id("care_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(36),Action::chao,ByteBufCodecs.stringUtf8(16),Action::action,
        ByteBufCodecs.stringUtf8(64),Action::value,Action::new);
    @Override public Type<Action> type() {return TYPE;}
  }
  public record StatEdit(String chao,int stat,int action,int value) implements CustomPacketPayload {
    public static final Type<StatEdit> TYPE=new Type<>(id("analyzer_stat_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,StatEdit> CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(36),StatEdit::chao,ByteBufCodecs.VAR_INT,StatEdit::stat,
        ByteBufCodecs.VAR_INT,StatEdit::action,ByteBufCodecs.VAR_INT,StatEdit::value,StatEdit::new);
    @Override public Type<StatEdit> type(){return TYPE;}
  }
  /** Fixed-point [-1000,1000] values represent the Dark-to-Hero range [-1,1]. */
  public record AlignmentEdit(String chao,int value) implements CustomPacketPayload {
    public static final Type<AlignmentEdit> TYPE=new Type<>(id("analyzer_alignment_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AlignmentEdit> CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(36),AlignmentEdit::chao,ByteBufCodecs.VAR_INT,AlignmentEdit::value,AlignmentEdit::new);
    @Override public Type<AlignmentEdit> type(){return TYPE;}
  }
  /** Current-stage fraction, 0..990 for a child and 0..1000 for an adult. */
  public record MaturityEdit(String chao,int value) implements CustomPacketPayload {
    public static final Type<MaturityEdit> TYPE=new Type<>(id("analyzer_maturity_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MaturityEdit> CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(36),MaturityEdit::chao,ByteBufCodecs.VAR_INT,MaturityEdit::value,MaturityEdit::new);
    @Override public Type<MaturityEdit> type(){return TYPE;}
  }
  /** Shared by the network handler and native authorization tests; always runs on the server thread. */
  public static boolean editStats(ServerPlayer player,StatEdit message){
    if(!player.isCreative()||player.isSpectator())return false;
    UUID id;try{id=UUID.fromString(message.chao());}catch(IllegalArgumentException e){return false;}
    if(!(player.serverLevel().getEntity(id) instanceof ChaoEntity chao)
        ||!ChaoCareServices.canManage(player,chao)||chao.isCocooned()
        ||com.chaogarden.activities.ActivityManager.isBusy(chao))return false;
    if(!com.chaogarden.core.AnalyzerStatRules.apply(chao.state(),message.stat(),message.action(),message.value()))return false;
    chao.sync();com.chaogarden.companion.CompanionContent.itemStateChanged(chao);return true;
  }
  public static void requestStat(ChaoEntity chao,com.chaogarden.core.AnalyzerStatRules.Edit edit){
    PacketDistributor.sendToServer(new StatEdit(chao.getUUID().toString(),edit.stat(),edit.action(),edit.value()));
  }
  /** Every drag sample is authorized from current server state, never client screen flags. */
  public static boolean editAlignment(ServerPlayer player,AlignmentEdit message) {
    if(player==null||message==null||!player.isCreative()||player.isSpectator()
        ||!AlignmentEditRules053.validMilli(message.value())||!hasAnalyzer(player))return false;
    UUID target;
    try{target=UUID.fromString(message.chao());}
    catch(IllegalArgumentException|NullPointerException invalid){return false;}
    // Preserve the same Creative ownership override as other Analyzer/care edits.
    if(!(player.serverLevel().getEntity(target) instanceof ChaoEntity chao)
        ||!ChaoCareServices.canManage(player,chao)||chao.isCocooned()
        ||com.chaogarden.activities.ActivityManager.isBusy(chao)
        ||!AlignmentEditRules053.canEdit(chao.state(),true,false))return false;
    if(!AlignmentEditRules053.applyMilli(chao.state(),message.value()))return false;
    chao.sync();com.chaogarden.companion.CompanionContent.itemStateChanged(chao);return true;
  }
  private static boolean hasAnalyzer(ServerPlayer player) {
    var analyzer=ChaoGarden.item("chao_analyzer");
    return player.getMainHandItem().is(analyzer)||player.getOffhandItem().is(analyzer);
  }
  public static void requestAlignment(ChaoEntity chao,int value) {
    if(chao!=null)PacketDistributor.sendToServer(new AlignmentEdit(chao.getUUID().toString(),value));
  }
  public static boolean editMaturity(ServerPlayer player,MaturityEdit message) {
    if(player==null||message==null||!player.isCreative()||player.isSpectator()||!hasAnalyzer(player))return false;
    UUID target;
    try{target=UUID.fromString(message.chao());}
    catch(IllegalArgumentException|NullPointerException invalid){return false;}
    if(!(player.serverLevel().getEntity(target) instanceof ChaoEntity chao)
        ||!ChaoCareServices.canManage(player,chao)||chao.isCocooned()
        ||com.chaogarden.activities.ActivityManager.isBusy(chao)
        ||!MaturityEditRules056.applyMilli(chao.state(),message.value()))return false;
    chao.sync();com.chaogarden.companion.CompanionContent.itemStateChanged(chao);return true;
  }
  public static void requestMaturity(ChaoEntity chao,int value) {
    if(chao!=null)PacketDistributor.sendToServer(new MaturityEdit(chao.getUUID().toString(),value));
  }
  public record Open(int entity,int page,int section) implements CustomPacketPayload {
    public static final Type<Open> TYPE=new Type<>(id("care_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,Open::entity,ByteBufCodecs.VAR_INT,Open::page,
        ByteBufCodecs.VAR_INT,Open::section,Open::new);
    @Override public Type<Open> type() {return TYPE;}
  }
  public static void register(IEventBus bus) {bus.addListener(ChaoCareNetwork::payloads);}
  private static void payloads(RegisterPayloadHandlersEvent event) {
    var r=event.registrar("care_1");
    r.playToServer(StatEdit.TYPE,StatEdit.CODEC,(message,context)->{
      if(context.player() instanceof ServerPlayer player)editStats(player,message);
    });
    r.playToServer(AlignmentEdit.TYPE,AlignmentEdit.CODEC,(message,context)->{
      if(context.player() instanceof ServerPlayer player)editAlignment(player,message);
    });
    r.playToServer(MaturityEdit.TYPE,MaturityEdit.CODEC,(message,context)->{
      if(context.player() instanceof ServerPlayer player)editMaturity(player,message);
    });
    r.playToServer(Action.TYPE,Action.CODEC,(message,context)->{
      if(!(context.player() instanceof ServerPlayer player))return;
      UUID id;try{id=UUID.fromString(message.chao());}catch(IllegalArgumentException e){return;}
      if(player.serverLevel().getEntity(id) instanceof ChaoEntity chao)
        ChaoCareServices.handle(player,chao,message.action(),message.value());
    });
    r.playToClient(Open.TYPE,Open.CODEC,(message,context)->
        com.chaogarden.client.ChaoCareClient.receive(message));
  }

  public static void open(ServerPlayer keeper,ChaoEntity chao,int page) {
    open(keeper,chao,page,0);
  }
  public static void open(ServerPlayer keeper,ChaoEntity chao,int page,int section) {
    if(ChaoCareServices.canManage(keeper,chao))PacketDistributor.sendToPlayer(keeper,new Open(chao.getId(),page,section));
  }
  public static void request(ChaoEntity chao,String action,String value) {
    PacketDistributor.sendToServer(new Action(chao.getUUID().toString(),action,value));
  }
}
