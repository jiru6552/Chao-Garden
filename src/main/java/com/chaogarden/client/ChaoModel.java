package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoPersonality;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoAnimationRules;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Imported Chao anatomy and authored motion, with our care-system extensions. */
public final class ChaoModel extends HierarchicalModel<ChaoEntity> {
  public static final ModelLayerLocation LAYER = new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("chao_garden", "chao"), "main");
  private final ModelPart root;
  private final OfficialChaoRig official = new OfficialChaoRig();
  private final ChaoMeshAsset cocoon = ChaoMeshAsset.load("chao_cocoon");
  private final Map<String, ChaoMeshAsset.Pose> cocoonPoses = cocoon.createPoses();
  private boolean cocooned;
  private float monsterAlpha=1;
  private int currentEmotion;
  private int eyeExpression;
  private final LionfishRattleClip rattleClip = new LionfishRattleClip();
  private final LionfishBubbleClip bubbleClip = new LionfishBubbleClip();
  private final LionfishMaracasClip maracasClip = new LionfishMaracasClip();
  private final LionfishTambourineClip tambourineClip = new LionfishTambourineClip();
  private final LionfishFluteClip fluteClip = new LionfishFluteClip();
  private final LionfishPianoClip pianoClip = new LionfishPianoClip();
  private final LionfishOrganClip organClip = new LionfishOrganClip();
  private final LionfishGuitarClip guitarClip = new LionfishGuitarClip();
  private final LionfishAccordionClip accordionClip = new LionfishAccordionClip();
  private final LionfishRockingHorseClip rockingHorseClip = new LionfishRockingHorseClip();
  private final LionfishActiveToyClip049 activeToyClip049 = new LionfishActiveToyClip049();
  private final LionfishQuietToyClip049 quietToyClip049 = new LionfishQuietToyClip049();
  float accordionBellows(float phase){return accordionClip.bellows(phase);}
  private final LionfishTriangleClip triangleClip = new LionfishTriangleClip();
  float triangleTap(float phase){return triangleClip.tap(phase);}
  private final LionfishTrumpetClip trumpetClip = new LionfishTrumpetClip();
  float trumpetLean(float phase){return trumpetClip.lean(phase);}
  float fluteLean(float phase){return fluteClip.lean(phase);}
  private final LionfishDrumClip drumClip = new LionfishDrumClip();
  float drumTap(float phase){return drumClip.tap(phase);}
  private final LionfishCymbalsClip cymbalsClip = new LionfishCymbalsClip();
  private final LionfishCastanetsClip castanetsClip = new LionfishCastanetsClip();
  float castanetsTap(float tick){return castanetsClip.tap(tick);}
  private final LionfishBellClip bellClip = new LionfishBellClip();
  private final ChaoToyGrip toyGrip = new ChaoToyGrip();
  private float[] toyPalm,leftToyPalm;
  float[] baseToyPalm(boolean left){return left?leftToyPalm:toyPalm;}
  /** Current actual visible hand volume in arm-local pixels, including reach. */
  float[] toyHandBounds(ChaoState state,boolean left){
    var bounds=toyGrip.boundsVisible(state,official,left);if(bounds==null)return null;
    var arm=official.poses().get(left?"arm_left":"arm_right");
    float reach=arm==null?0:arm.reach*(left?1:-1);
    bounds[0]+=reach;bounds[3]+=reach;return bounds;
  }
  float[] toyPalm(boolean left){
    float[] palm=baseToyPalm(left);if(palm==null)return null;
    var arm=official.poses().get(left?"arm_left":"arm_right");
    return arm==null||arm.reach<=0?palm:ChaoTrumpetAnimation.reachedPalm(palm,arm.reach,left);
  }
  float[] rattlePalm() { return toyPalm(false); }
  private float heldY=-2.8f,heldZ=-3.2f;

  public ChaoModel(ModelPart root) { this.root = root; }
  @Override public ModelPart root() { return root; }
  public static LayerDefinition createBodyLayer() { return LayerDefinition.create(new MeshDefinition(),64,32); }
  public boolean officialActive() { return !cocooned; }
  public int officialEmotion() { return currentEmotion; }
  /** 0 native open eyes, 1 content, 2 water distress, 3 sleep, 4 blink. */
  public int eyeExpression() { return eyeExpression; }
  public OfficialChaoRig.Motion officialMotion() { return official.motion(); }
  public float officialIdleGestureWeight() { return official.idleGestureWeight(); }
  public String officialGeometryKey() { return official.sourceGeometryKey(); }
  public String gameplayFormKey() { return official.geometryKey(); }
  public void transformAccessoryAnchor(String semanticBone,PoseStack stack) { official.transformBone(semanticBone,stack); }
  void copyAccessoryPoseTo(String semanticBone,ChaoMeshAsset target,ChaoMeshAsset.Pose to) {
    official.copyAttachmentPoseTo(semanticBone,target,to);
  }
  public float[] accessoryBounds(String semanticBone) { return official.attachmentBounds(semanticBone); }
  float[] toyMouth() { return official.attachmentMouth(); }
  public float[] accessoryEyes() { return official.attachmentEyes(); }
  public Map<String, ChaoMeshAsset.Pose> officialPoses() { return official.poses(); }
  public boolean officialPosesFinite() { return official.finite(); }
  public int officialSampledFrames() { return official.sampledFrames(); }

  @Override public void renderToBuffer(PoseStack stack, VertexConsumer vertices,
                                      int light, int overlay, int color) {
    color=(color&0x00ffffff)|(Math.round(((color>>>24)&255)*monsterAlpha)<<24);
    if (cocooned) cocoon.render(stack,vertices,light,overlay,color,cocoonPoses);
    else official.render(stack,vertices,light,overlay,color);
  }

  public void transformHeldItem(PoseStack stack) {
    official.transformBone("body",stack);
    stack.translate(0,heldY/16f,heldZ/16f);
  }

  private static boolean hasGene(ChaoState state,int slot) {
    int animal=state.animalParts[slot];
    return state.allowsAnimalParts() && animal>=0 && (ChaoState.animalSlots(animal)&(1<<slot))!=0;
  }
  private void addRotation(String name,float x,float y,float z) {
    var p=official.pose(name);if(p==null&&name.equals("foot_right"))p=official.pose("feet_bone_right");if(p!=null){p.rx+=x;p.ry+=y;p.rz+=z;}
  }

  @Override public void setupAnim(ChaoEntity entity,float swing,float amount,float age,float yaw,float pitch) {
    toyPalm=null;leftToyPalm=null;
    eyeExpression=0;
    ChaoState state=entity.state();cocooned=entity.isCocooned();
    monsterAlpha=cocooned||entity.officialComparison()?1:state.monsterAlpha();
    if(cocooned) {
      currentEmotion=0;
      cocoonPoses.forEach((name,p)->{p.reset();p.visible=name.equals("cocoon");});
      var p=cocoonPoses.get("cocoon");p.ry=age*.014f;p.sx=p.sz=1+Mth.sin(age*.12f)*.018f;
      return;
    }
    boolean gripping=entity.isCompanionAttached();
    boolean rest=!gripping&&entity.isResting(),eat=!gripping&&entity.isEating(),pet=!gripping&&entity.isBeingPetted();
    float partial=Mth.clamp(age-entity.tickCount,0,1);
    var action=entity.getItemAction();
    var behavior=entity.getBehavior();
    boolean gesture=!gripping&&behavior!=ChaoEntity.Behavior.NONE;
    boolean delighted=action==ChaoEntity.ItemAction.DELIGHT;
    float attackProgress=Mth.clamp(entity.getAttackAnim(partial),0,1);
    int emotion=entity.hurtTime>0 ? 1 : (pet||delighted)&&!rest ? 3 : attackProgress>0 ? 2 : 0;
    currentEmotion=emotion;
    float emotionalTime=entity.hurtTime>0 ? Math.max(0,entity.hurtDuration-entity.hurtTime+partial)/20f : pet ? Math.max(0,ChaoAnimationRules.PET_TICKS-entity.getAffectionTicks()+partial)/20f
        : delighted ? entity.getItemActionProgress(partial)*1.58333f
        : entity.hurtTime>0 ? Math.max(0,entity.hurtDuration-entity.hurtTime+partial)/20f
        : attackProgress>0 ? attackProgress*.75f : age/20f;
    String model=OfficialChaoRig.geometryKey(state);
    heldY=-2.8f;heldZ=-3.2f;
    if(entity.officialComparison()) {
      official.animate(OfficialChaoRig.referenceGeometry(state,entity.sourceReferenceKey()),age,swing,amount,
          emotion,emotionalTime,yaw,pitch);
      official.applySpecial(false,0,age);
      if(gripping)official.applyCompanionCarry(entity.companionBackBlend(partial));
      return;
    }
    // The carried item renders an unregistered client-side copy. Keep it
    // comfortably upright instead of treating it as a newborn on the ground.
    boolean held = entity.level().getEntity(entity.getId()) != entity;
    String toyKind=entity.toyPlayingKind();
    boolean rattle=ChaoRattleAnimation.active(entity),rattleHappy=ChaoRattleAnimation.happy(entity,partial);
    boolean bubble=ChaoBubbleAnimation.active(entity),bubbleHappy=ChaoBubbleAnimation.happy(entity,partial),maracas=ChaoMaracasAnimation.active(entity);
    boolean horse049=ChaoRockingHorseAnimation.active(entity),active049=ChaoActiveToyAnimation049.active(entity),quiet049=ChaoQuietToyAnimation049.active(entity);
    rattleHappy|=ChaoRockingHorseAnimation.happy(entity,partial)||ChaoActiveToyAnimation049.happy(entity,partial)||ChaoQuietToyAnimation049.happy(entity,partial);
    rattleHappy|=bubbleHappy||ChaoPianoAnimation.happy(entity,partial)||ChaoOrganAnimation.happy(entity,partial)||ChaoGuitarAnimation.happy(entity,partial)||ChaoAccordionAnimation.happy(entity,partial);
    boolean bell=ChaoBellAnimation.active(entity),tambourine=ChaoTambourineAnimation.active(entity),castanets=ChaoCastanetsAnimation.active(entity),cymbals=ChaoCymbalsAnimation.active(entity),drum=ChaoDrumAnimation.active(entity),flute=ChaoFluteAnimation.active(entity),trumpet=ChaoTrumpetAnimation.active(entity),triangle=ChaoTriangleAnimation.active(entity),piano=ChaoPianoAnimation.active(entity),organ=ChaoOrganAnimation.active(entity),guitar=ChaoGuitarAnimation.active(entity),accordion=ChaoAccordionAnimation.active(entity);
    rattleHappy|=ChaoMaracasAnimation.happy(entity,partial)||ChaoBellAnimation.happy(entity,partial)||ChaoTambourineAnimation.happy(entity,partial)||ChaoCastanetsAnimation.happy(entity,partial)||ChaoCymbalsAnimation.happy(entity,partial)||ChaoDrumAnimation.happy(entity,partial)||ChaoFluteAnimation.happy(entity,partial)||ChaoTrumpetAnimation.happy(entity,partial)||ChaoTriangleAnimation.happy(entity,partial);
    boolean ballChasing=com.chaogarden.toys.ToyInteractionRules.movingBallPlayer(toyKind,entity.toyKickElapsed());
    boolean floating=com.chaogarden.toys.ToyInteractionRules.supportedFloat(toyKind);
    OfficialChaoRig.Motion motion;
    if (gripping) motion = OfficialChaoRig.Motion.SIT;
    else if (rest) motion = OfficialChaoRig.Motion.REST;
    else if (held) motion = OfficialChaoRig.Motion.HELD;
    else if (floating) motion = OfficialChaoRig.Motion.PLAY;
    else if (entity.isInWater()) motion = !LocomotionRules.canSwim(state)
        ? OfficialChaoRig.Motion.FLAIL : LocomotionRules.advancedSwim(state)
        ? OfficialChaoRig.Motion.SWIM : OfficialChaoRig.Motion.PADDLE;
    else if (entity.isChaoGliding()) motion = OfficialChaoRig.Motion.GLIDE;
    else if(entity.isHandlingItem()) motion=switch(action) {
      case GARDEN -> OfficialChaoRig.Motion.WALK;
      case PLANT -> OfficialChaoRig.Motion.SIT;
      case THINK -> OfficialChaoRig.Motion.PONDER;
      case INSPECT -> OfficialChaoRig.Motion.INSPECT;
      case EAT -> OfficialChaoRig.Motion.EAT;
      case TRAIN -> OfficialChaoRig.Motion.TRAIN;
      case REJECT -> OfficialChaoRig.Motion.REJECT;
      case DELIGHT -> OfficialChaoRig.Motion.DELIGHT;
      default -> OfficialChaoRig.Motion.SIT;
    };
    else if(toyKind.equals("chao_ball")&&entity.toyKickElapsed()>=0)motion=OfficialChaoRig.Motion.WALK;
    else if(toyKind.equals(com.chaogarden.toys.ToyPreview.SITTING_KIND)) {
      float phase=160-entity.toyPlayingTicks();motion=phase<20||phase>=140?OfficialChaoRig.Motion.WALK:OfficialChaoRig.Motion.SIT;
    }
    else if(entity.toyPlayingKind().equals("chao_pogo_stick")||entity.toyPlayingKind().equals("chao_broom")
        ||entity.toyPlayingKind().equals("chao_toy_car")||entity.toyPlayingKind().equals("chao_crayons"))motion=OfficialChaoRig.Motion.WALK;
    else if(entity.toyPlayingTicks()>0&&!ballChasing)motion=rattle||bubble
        ?OfficialChaoRig.Motion.WALK:OfficialChaoRig.Motion.PLAY;
    else if (pet) motion = OfficialChaoRig.Motion.PET;
    else if (eat || ChaoBehaviorPoses.seated(behavior)) motion = OfficialChaoRig.Motion.SIT;
    else if (!entity.onGround() && entity.getDeltaMovement().y < -.08)
      motion = OfficialChaoRig.Motion.FALL;
    else motion = switch (LocomotionRules.groundGait(state)) {
      case CRAWL -> ChaoAnimationRules.crawling(amount)
          ? OfficialChaoRig.Motion.CRAWL : OfficialChaoRig.Motion.SIT;
      case WALK -> OfficialChaoRig.Motion.WALK;
      case RUN -> OfficialChaoRig.Motion.RUN;
      case DASH -> OfficialChaoRig.Motion.DASH;
    };
    // LivingEntityRenderer triples every vanilla baby's stride phase. These
    // authored Chao gaits instead track distance, with only the 0.64/0.80 body
    // scale difference shortening a child's stride.
    float gaitSwing = state.adult ? swing : swing * (1.25f / 3f);
    HeroRunAppearance052.prepare(entity.getUUID(),state,age);
    NeutralAppearance056.prepare(entity.getUUID(),state,age);
    HeroNormalAppearance057.prepare(entity.getUUID(),state,age);
    NeutralSwimAppearance059.prepare(entity.getUUID(),state,age);
    official.prepareDevelopment(model,state);
    official.animateGameplay(model,entity.getUUID(),motion,age,gaitSwing,
        rest||eat||pet||held||entity.isHandlingItem()&&action!=ChaoEntity.ItemAction.GARDEN||entity.toyPlayingTicks()>0&&!ballChasing||gesture&&behavior!=ChaoEntity.Behavior.FLEE&&behavior!=ChaoEntity.Behavior.HOLD_HANDS?0:amount,emotion,emotionalTime,
        state.immortal?Mth.clamp(yaw,-12,12):yaw,pitch,!gripping&&!rest&&!eat&&!pet&&!entity.isHandlingItem()&&entity.toyPlayingTicks()==0&&!gesture,false);
    official.applySpecial(false,0,age);
    official.development(state);
    int construction=state.intTrait("content.construction");
    if(state.character.equals("omochao")&&construction>0) {
      for(String foot:java.util.List.of("foot_left","foot_right","feet_bone_right","foot_left2","foot_right2"))official.hidePart(foot);
      if(construction<3){official.hidePart("arm_left");official.hidePart("arm_right");official.hidePart("character_propeller");official.hidePart("character_windup_key");official.hidePart("character_robot_key_shaft");}
      if(construction<2)official.hidePart("skull");
    }
    official.expression(entity.hurtTime>0?2:pet||delighted||rattleHappy?1:attackProgress>0?3:0);
    String variant=ChaoVariants.activeId(state);
    boolean actualChaos=state.immortal&&"normal".equals(state.character);
    boolean themed=!state.immortal&&"normal".equals(state.character)&&!ChaoVariants.NORMAL.equals(variant);
    boolean headGenes=hasGene(state,ChaoState.EARS)||hasGene(state,ChaoState.FOREHEAD)||hasGene(state,ChaoState.HORNS);
    boolean wearingHat = ChaoAccessoryFit.headCovered(entity);
    if (headGenes || wearingHat || themed&&(!state.adult||state.primaryType==ChaoState.NORMAL)) official.hideHeadDecorations();
    if(themed || actualChaos || headGenes || wearingHat) {
      // Only Baby's crest is replaced. Its other skull cubes are the original
      // head and raised face panels, and remain present under custom additions.
      if(model.equals("baby"))official.hideCube("skull",1);
    }
    if(hasGene(state,ChaoState.ARMS)){official.hidePart("arm_left");official.hidePart("arm_right");}
    if(hasGene(state,ChaoState.FEET)||state.ghostBody){official.hidePart("foot_left");official.hidePart("foot_right");official.hidePart("feet_bone_right");official.hidePart("foot_left2");official.hidePart("foot_right2");}
    if(hasGene(state,ChaoState.WINGS)||state.ghostBody||actualChaos){official.hidePart("wing_left");official.hidePart("wing_right");}
    if(hasGene(state,ChaoState.TAIL)||state.ghostBody||actualChaos)official.hidePart("tail");
    if(emotion==0&&(themed||state.flameOrb||actualChaos||state.character.equals("omochao")))official.hidePart("emotion_dot");
    if(actualChaos){var marker=official.pose("chaos_marker");if(marker!=null){marker.visible=emotion==0;marker.y+=Mth.sin(age*.075f)*.18f;marker.ry+=age*.012f;}}
    if(!gripping&&entity.isHandlingItem()) itemAction(entity,action,partial,age);
    else if(!gripping&&entity.toyPlayingTicks()>0) {
      if(!rattle&&!bubble&&!maracas&&!bell&&!tambourine&&!castanets&&!cymbals&&!flute&&!trumpet&&!triangle&&!piano&&!organ&&!guitar&&!accordion&&!horse049&&!active049&&!quiet049)toyAction(entity.toyPlayingKind(),age,entity.toyPlayingTicks(),partial,entity.toyKickElapsed());
    }
    else if(eat) {
      holdHands(.15f);addRotation("skull",.10f+Mth.sin(age*.55f)*.025f,0,0);
    } else if(!gripping&&!rest&&!pet&&!held&&gesture&&!maracas&&!bell&&!tambourine&&!castanets&&!cymbals&&!flute&&!trumpet&&!triangle&&!guitar&&!accordion&&!active049&&!quiet049) {
      // Generic behavior saves retain the remaining timer. A newborn's one
      // settling sequence must not restart its head turn after such a reload.
      float behaviorProgress=behavior==ChaoEntity.Behavior.HATCH
          ? Mth.clamp(1-(entity.getBehaviorTicks()-partial)/(float)ChaoAnimationRules.HATCH_TICKS,0,1)
          : entity.getBehaviorProgress(partial);
      int expression=ChaoBehaviorPoses.apply(official,behavior,entity.behaviorDetail(),behaviorProgress,age*ChaoPreferences.animationSpeed(state));
      official.expression(expression);
      if(behavior==ChaoEntity.Behavior.WATER) {
        float pour=ChaoAnimationRules.wateringPour(behaviorProgress);
        holdHands(.12f+pour*.3f);heldY=-2.5f;heldZ=-3.7f-pour*.3f;
      }
      if(behavior==ChaoEntity.Behavior.INSTRUMENT){heldY=entity.behaviorDetail().equals("trumpet")||entity.behaviorDetail().equals("flute")?-3.6f:-2.7f;heldZ=-3.2f;}
    } else if(!gripping&&!rest&&!pet&&amount<.01f&&!held)personalityIdle(entity,age);

    // Native face panels are replaced only for a real expression, so there is
    // never a second set of open eyes underneath closed eyelids. Robotic and
    // Chaos faces retain their own anatomy/materials.
    if(!gripping&&"normal".equals(state.character)&&!state.immortal
        &&official.attachmentEyes()!=null) {
      long seed=entity.getUUID().getLeastSignificantBits()^entity.getUUID().getMostSignificantBits();
      eyeExpression=rest||behavior==ChaoEntity.Behavior.GROGGY?3
          :motion==OfficialChaoRig.Motion.FLAIL||behavior==ChaoEntity.Behavior.CRY?2
          :action==ChaoEntity.ItemAction.EAT||rattleHappy?1
          :pet&&Mth.sin(age*.15f)>.72f?1
          :held&&Mth.sin(age*.11f)>.5f?1
          :ChaoAnimationRules.blinking(age,seed)?4:0;
      if(eyeExpression!=0)official.eyesVisible(false);
    }
    if(motion==OfficialChaoRig.Motion.FLAIL)official.expression(6);
    if(pet&&entity.hurtTime==0) {
      official.symbol("emotion_heart");
      var heart=official.pose("emotion_heart");
      if(heart!=null)heart.sx=heart.sy=heart.sz=1.10f+Mth.sin(age*.15f)*.08f;
    }
    if(rest) {
      official.expression(0);
      if(eyeExpression==3)official.hidePart("emotion_dot");
      // The slow head settling is driven by actual saved sleep, not daytime.
      addRotation("skull",Mth.sin(age*.045f)*.025f,0,Mth.sin(age*.035f)*.025f);
    }

    float attack=attackProgress;
    if(!gripping&&attack>0&&!rest&&!eat&&!entity.isInWater()) {
      float strike=Mth.sin((float)Math.sqrt(attack)*Mth.PI);
      addRotation("arm_right",-.3f-strike*.75f,0,-.15f);
      addRotation("arm_left",-.3f,0,.15f);addRotation("body",0,strike*.055f,0);
    }
    if(state.character.equals("omochao")){
      var propeller=official.pose("character_propeller");if(propeller!=null)propeller.ry=(age*.08f)%(Mth.PI*2);
      var key=official.pose("character_windup_key");if(key!=null)key.rz=(age*.026f)%(Mth.PI*2);
    }
    if(bubbleHappy)official.expression(6); // Happy eyes with the existing open-mouth mesh.
    if(flute&&com.chaogarden.toys.FlutePlayRules.blowing(com.chaogarden.toys.InstrumentPerformance.phase(entity,partial)))official.expression(6);
    if(trumpet&&com.chaogarden.toys.TrumpetPlayRules.blowing(com.chaogarden.toys.InstrumentPerformance.phase(entity,partial)))official.expression(6);
    // Damage wins over every later face override, including simultaneous pet,
    // delight, blink and toy metadata arriving in a different packet order.
    if(entity.hurtTime>0) {
      currentEmotion=1;official.expression(2);official.symbol("emotion_exclamation");
      if("normal".equals(state.character)&&!state.immortal&&official.attachmentEyes()!=null) {
        eyeExpression=2;official.eyesVisible(false);
      } else eyeExpression=0;
    }
    if(gripping)official.applyCompanionCarry(entity.companionBackBlend(partial));
    if(rattle||bubble||maracas||bell||tambourine||castanets||cymbals||drum||flute||trumpet||triangle)toyPalm=toyGrip.locate(state,official);
    if(maracas||tambourine||castanets||cymbals||drum||flute||trumpet||triangle)leftToyPalm=toyGrip.locate(state,official,true);
    if(trumpet||triangle||piano||organ||guitar||accordion||horse049||active049||quiet049){toyPalm=toyGrip.locateVisible(state,official,false);leftToyPalm=toyGrip.locateVisible(state,official,true);}
    ChaoRattleAnimation.pose(entity,this,rattleClip,partial);
    ChaoBubbleAnimation.pose(entity,this,bubbleClip,partial);
    ChaoMaracasAnimation.pose(entity,this,maracasClip,partial);
    ChaoBellAnimation.pose(entity,this,bellClip,partial);
    ChaoTambourineAnimation.pose(entity,this,tambourineClip,partial);
    ChaoCastanetsAnimation.pose(entity,this,castanetsClip,partial);
    ChaoCymbalsAnimation.pose(entity,this,cymbalsClip,partial);
    ChaoDrumAnimation.pose(entity,this,drumClip,partial);
    ChaoFluteAnimation.pose(entity,this,fluteClip,partial);
    // Record the complete Lionfish pose, so cancellation blends the visible
    // outgoing arm back to the next activity instead of snapping to idle.
    int toyTransition=rattle?1:bubble?2:maracas?3:bell?4:tambourine?5:castanets?6:cymbals?7:drum?8:flute?9:trumpet?10:triangle?11:piano?12:organ?13:guitar?14:accordion?15:horse049?16:active049?17+com.chaogarden.toys.GardenToyPerformance049.KINDS.indexOf(entity.toyPlayingKind()):quiet049?25+com.chaogarden.toys.QuietToyPlayRules049.index(entity.toyPlayingKind()):0;
    official.finishGameplayFrame(entity.getUUID(),emotion+16*behavior.ordinal()+1024*toyTransition,age);
    if(official.recoveringProne(entity.getUUID())&&ChaoProneToyPose051.clearExitFloor(entity,this))
      official.rememberGameplayFrame(entity.getUUID());
    // Fit the mouth-held trumpet to the actual blended incoming skeleton.
    // Blending its already-solved arms independently from the head can sweep
    // the hands through the face and detach their supports on a late observer.
    if(trumpet){ChaoTrumpetAnimation.pose(entity,this,trumpetClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(triangle){ChaoTriangleAnimation.pose(entity,this,triangleClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(piano){ChaoPianoAnimation.pose(entity,this,pianoClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(organ){ChaoOrganAnimation.pose(entity,this,organClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(guitar){ChaoGuitarAnimation.pose(entity,this,guitarClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(accordion){ChaoAccordionAnimation.pose(entity,this,accordionClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(horse049){ChaoRockingHorseAnimation.pose(entity,this,rockingHorseClip,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(active049){ChaoActiveToyAnimation049.pose(entity,this,activeToyClip049,partial);official.rememberGameplayFrame(entity.getUUID());}
    if(quiet049){ChaoQuietToyAnimation049.pose(entity,this,quietToyClip049,partial);official.rememberGameplayFrame(entity.getUUID());}
    official.rememberProneFrame(entity.getUUID(),ChaoProneToyPose051.supports(toyKind)
        &&(active049||quiet049)&&com.chaogarden.toys.GardenToyPerformance049.presence(entity,partial)>0);
    if(!official.finite())throw new IllegalStateException("Non-finite imported Chao pose");
  }
  private void holdHands(float lift) {
    for(String name:java.util.List.of("arm_left","arm_right")) {
      var p=official.pose(name);if(p==null)continue;
      float side=name.endsWith("left")?1:-1;
      p.x=-side*.65f;p.y=-.25f-lift;p.z=-.3f;
      p.rx=-.12f;p.ry=side*1.10f;p.rz=side*.75f;
    }
  }

  private void itemAction(ChaoEntity entity,ChaoEntity.ItemAction action,float partial,float age) {
    float progress=entity.getItemActionProgress(partial);
    float pulse=Mth.sin(age*.22f);
    if(action==ChaoEntity.ItemAction.DELIGHT)return;
    holdHands(0);
    switch(action) {
      case GARDEN -> {holdHands(.2f);}
      case PLANT -> {holdHands(.15f+Math.max(0,pulse)*.3f);heldY=-1.6f;heldZ=-3.8f;addRotation("skull",.25f,0,0);}
      case THINK -> {
        float consider=Mth.sin(progress*Mth.PI);
        addRotation("skull",-.04f,consider*.09f,consider*.12f);
        if(progress>.35f){official.symbol("emotion_question");currentEmotion=4;}
      }
      case INSPECT -> {
        addRotation("skull",.08f,Mth.sin(progress*Mth.PI*2)*.17f,Mth.sin(progress*Mth.PI)*.08f);
        heldY=-2.8f-pulse*.10f;heldZ=-3.3f;
        official.symbol("emotion_question");
        currentEmotion=4;
      }
      case EAT -> {
        float bite=ChaoAnimationRules.nibbleLift(progress),chew=ChaoAnimationRules.chew(progress);
        holdHands(.08f+bite*.55f);heldY=-2.8f-bite*.65f;heldZ=-3.3f;
        addRotation("skull",.07f+bite*.05f+chew*.018f,0,0);
        official.expression(Math.abs(chew)>.35f?6:1);
      }
      case TRAIN -> {
        holdHands(.4f+.12f*pulse);heldY=-3.2f-.12f*pulse;
        addRotation("skull",-.06f,0,pulse*.07f);
        if(progress>.6f){official.symbol("emotion_exclamation");currentEmotion=2;}
      }
      case REJECT -> {
        float toss=Mth.sin(Mth.clamp(progress/.65f,0,1)*Mth.PI);
        holdHands(toss*.75f);heldY=-2.8f-toss*.75f;heldZ=-3.2f-toss*1.1f;
        addRotation("arm_right",-.55f*toss,0,-.35f*toss);
        addRotation("skull",0,Mth.sin(progress*Mth.PI*4)*.18f,0);
        official.symbol("emotion_question");
        currentEmotion=4;
      }
      case DELIGHT -> { /* The original happy clip supplies the small celebration. */ }
      default -> { }
    }
  }

  private void toyAction(String kind,float age,int remaining,float partial,int kickElapsed) {
    int elapsed=com.chaogarden.toys.ToyInteractionRules.SESSION_TICKS-remaining;
    int frame=com.chaogarden.toys.ToyInteractionRules.mountedFrame(elapsed);
    // Observers joining at different times use the same session phase.
    float tap=Mth.sin((elapsed+partial)*Mth.PI/8);
    switch(kind) {
      case "chao_tv","chao_radio" -> {
        addRotation("skull",.08f,0,tap*.025f);
        if(kind.equals("chao_radio")){addRotation("body",0,0,tap*.035f);holdHands(.1f);}
      }
      case "chao_piano" -> {
        // Low source keyboards: seated, hands forward with small independent key presses.
        holdHands(.35f);addRotation("skull",.16f,0,0);
        addRotation("arm_left",tap*.06f,0,.04f);
        addRotation("arm_right",-tap*.06f,0,-.04f);
      }
      case "chao_lifesaver" -> {addRotation("arm_left",-.15f,0,.75f);addRotation("arm_right",-.15f,0,-.75f);addRotation("foot_left",tap*.10f,0,0);addRotation("foot_right",-tap*.10f,0,0);}
      case "chao_trumpet" -> {holdHands(.6f);addRotation("skull",-.10f,0,tap*.04f);}
      case "chao_jack_in_box" -> {
        float reaction=(float)com.chaogarden.toys.ToyInteractionRules.jackReaction(elapsed+partial);
        float anticipation=elapsed<com.chaogarden.toys.ToyInteractionRules.JACK_POP_TICK
            ?Mth.sin((elapsed+partial)*.22f):0;
        holdHands(.15f+.3f*reaction);addRotation("skull",-.08f-.06f*reaction,0,-anticipation*.025f);
        if(reaction>0){official.symbol("emotion_exclamation");currentEmotion=2;}
      }
      case "chao_ball" -> {
        if(kickElapsed>=0)addRotation("foot_right",-.55f*(float)com.chaogarden.toys.ToyInteractionRules.ballKickSwing(kickElapsed+partial),0,0);
        addRotation("skull",.10f,0,0);
      }
      default -> {
        holdHands(.2f);addRotation("arm_left",tap*.24f,0,tap*.10f);
        addRotation("arm_right",-tap*.24f,0,tap*.10f);addRotation("body",0,0,tap*.025f);
      }
    }
  }

  private void personalityIdle(ChaoEntity entity,float age) {
    ChaoState s=entity.state();long seed=entity.getUUID().getLeastSignificantBits();
    float cycle=(age+(seed&255))%240;
    if(cycle<0)cycle+=240;
    if(cycle>45)return;
    float weight=Mth.sin(cycle/45f*Mth.PI);
    float curiosity=s.personalitySlider(ChaoPersonality.CURIOUS)/100f;
    float energetic=s.personalitySlider(ChaoPersonality.ENERGETIC)/100f;
    float carefree=s.personalitySlider(ChaoPersonality.CAREFREE)/100f;
    float chatty=s.personalitySlider(ChaoPersonality.CHATTY)/100f;
    float solitude=s.personalitySlider(ChaoPersonality.SOLITUDE)/100f;
    if(curiosity>0){addRotation("skull",-.05f*weight,.15f*weight*curiosity,.08f*weight*curiosity);if(cycle>12&&cycle<30&&curiosity>.35f){official.symbol("emotion_question");currentEmotion=4;}}
    addRotation("body",0,0,Mth.sin(cycle*.2f)*.045f*weight*Math.max(0,energetic));
    addRotation("skull",0,Mth.sin(cycle*.09f)*.13f*weight*Math.max(0,carefree),0);
    if(chatty>.2f)addRotation("arm_right",0,0,-.15f*weight*chatty);
    if(solitude<-.3f&&s.emotion(ChaoEmotion.LONELY)>5000){addRotation("skull",.1f*weight,0,-.08f*weight);official.expression(2);}
    if(s.personalitySlider(ChaoPersonality.BIG_EATER)>30&&s.hunger<40)addRotation("skull",.12f*weight,0,0);

  }
}
