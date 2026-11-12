package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoGaitClock;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shared runtime for the imported Bedrock cuboids and their authored clips. */
public final class OfficialChaoRig {
  public static final List<String> REFERENCE_MODELS = List.of("baby","swim","fly","run","power",
      "hero","hero_fly","hero_power","dark","dark_run","dark_swim","golden","baghead",
      "dad","dragon","ender","tails","knuckles","emerald","creeper");
  private record Frame(ChaoMeshAsset mesh, Map<String, ChaoMeshAsset.Pose> poses) {}
  private final Map<String, Frame> frames = new HashMap<>();
  private final JsonObject clips;
  private final Map<JsonElement,NavigableMap<Float,JsonElement>> channelPoints = new IdentityHashMap<>();
  private Frame current;
  private String key = "baby";
  private int sampledFrames;
  private boolean referenceRendering;
  private boolean adultPose, broadWalkHead;
  private float powerSwimWeight;
  private boolean prototypeSelection;
  private boolean neutralSelection056,heroNormalSelection057,neutralSwimSelection059;
  private String prototypeOwnerKey;
  public enum Motion { CRAWL, WALK, RUN, DASH, FLAIL, PADDLE, SWIM, GLIDE, GRIP, FALL, HELD, SIT, REST,
    PONDER, INSPECT, EAT, TRAIN, REJECT, DELIGHT, PLAY, PET }
  private Motion motion = Motion.WALK;
  private float idleGestureWeight;
  // Entity UUIDs stay strongly held while that entity is loaded. Weak keys avoid
  // retaining departed creatures, without evicting every stride in a busy garden.
  private final Map<UUID, ChaoGaitClock> gaitClocks = new WeakHashMap<>();
  private final LinkedHashMap<UUID, Transition> transitions = new LinkedHashMap<>(128, .75f, true);
  private static final class Transition {
    Motion mode;
    int emotion;
    String geometry;
    float age, started;
    Map<String, float[]> last, from;
    boolean lastProne,fromProne;
  }


  private final java.util.function.Function<String,ChaoMeshAsset> assets;
  public OfficialChaoRig() {this(loadClips(),ChaoMeshAsset::load);}
  OfficialChaoRig(JsonObject clips,java.util.function.Function<String,ChaoMeshAsset> assets){
    this.clips=clips;this.assets=assets;if(clips.size()!=8)throw new IllegalStateException("Expected eight imported Chao clips");select("baby");
  }
  private static JsonObject loadClips(){
    var id=ResourceLocation.fromNamespaceAndPath("chao_garden","animations/official/chao.animation.json");
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id)){
      return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
    }catch(IOException|RuntimeException error){throw new IllegalStateException("Cannot load imported Chao animations",error);}
  }

  private void select(String model) { select(model,false); }

  private void select(String model,boolean gameplay) {
    key = model;
    boolean prototype=gameplay&&prototypeSelection&&model.equals(prototypeOwnerKey);
    String prototypeModel=neutralSwimSelection059?"neutral_swim_proto059":heroNormalSelection057?"hero_normal_proto057":neutralSelection056?"neutral_proto056":"hero_run_proto055";
    String cache=prototype?"gameplay:"+prototypeModel:(gameplay?"gameplay:":"source:")+model;
    current = frames.computeIfAbsent(cache, ignored -> {
      var mesh = assets.apply(prototype?"gameplay/chao_"+prototypeModel:(gameplay?"gameplay/":"official/")+"chao_"+model);
      return new Frame(mesh, mesh.createPoses());
    });
  }

  /** Selection preserves our genetic/care state; source variants are only visual rigs. */
  public static String geometryKey(ChaoState state) {
    if(state.character!=null && Set.of("tails","amy","knuckles","omochao").contains(state.character))return state.character;
    if(state.immortal&&"normal".equals(state.character))return state.align>=.5f?"chaos_angel":state.align<=-.5f?"chaos_devil":"chaos_light";
    if (!state.adult) return "baby";
    int type = state.primaryType;
    boolean hero = state.align >= .5f, dark = state.align <= -.5f;
    if (hero) return switch (type) { case 1 -> "hero_swim"; case 2 -> "hero_fly"; case 3 -> "hero_run"; case 4 -> "hero_power"; default -> "hero"; };
    if (dark) return switch (type) { case 1 -> "dark_swim"; case 2 -> "dark_fly"; case 3 -> "dark_run"; case 4 -> "dark_power"; default -> "dark"; };
    return switch (type) { case 1 -> "swim"; case 2 -> "fly"; case 3 -> "run"; case 4 -> "power"; default -> "baby"; };
  }

  public static ResourceLocation texture(ChaoState state) {
    if(NeutralSwimAppearance059.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralSwimCoatMaterial059.texture(state),state,"baby");
    if(HeroNormalAppearance057.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroNormalCoatMaterial057.texture(state),state,"baby");
    if(NeutralAppearance056.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralCoatMaterial056.texture(state),state,"baby");
    if(HeroRunAppearance052.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroRunCoatMaterial055.texture(state),state,"baby");
    String selected=geometryKey(state),geometry=sourceGeometryKey(selected),old=ChaoRenderer.textureKey(state),path;
    if(selected.equals("omochao"))return OmochaoMaterial.texture(state);
    if(selected.startsWith("chaos_"))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/gameplay/chaos/"+old+".png"),state,"chaos");
    if(selected.equals("amy"))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/gameplay/character_"+selected+".png"),state,geometry);
    if(selected.equals("tails")||selected.equals("knuckles"))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/official/"+selected+".png"),state,geometry);
    if(!ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state)))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/gameplay/variants/"+ChaoVariants.activeId(state)+"/"+geometry+".png"),state,geometry);
    int effectiveType = state.primaryType;
    if (state.immortal || !ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state))) path = old;
    else if (state.baseColor() == 0 && state.jewel() == 0 && !state.isShiny()
        && !(state.adult && (state.align>=.5f && (effectiveType==1 || effectiveType==3)
          || state.align<=-.5f && (effectiveType==2 || effectiveType==4)))) path = geometry;
    else path = "coats/" + geometry + "/" + old;
    var raw=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/official/"+path+".png");
    return ChaoPaletteMaterial.texture(GradualCoatMaterial.texture(raw,state,geometry),state,geometry);
  }

  public static String referenceGeometry(ChaoState state, String requested) {
    return REFERENCE_MODELS.contains(requested) ? requested : sourceGeometryKey(geometryKey(state));
  }

  /** Exact original PNG; no palette, extra symbols, animal parts or theme overlay. */
  public static ResourceLocation referenceTexture(ChaoState state, String requested) {
    return ResourceLocation.fromNamespaceAndPath("chao_garden",
        "textures/entity/chao/official/"+referenceGeometry(state,requested)+".png");
  }

  /** UV/attachment skeleton alias; gameplay counterparts retain these semantic anchors. */
  public static String sourceGeometryKey(String selected) {
    return switch(selected) {
      case "hero_swim" -> "swim";case "hero_run" -> "run";
      case "dark_fly" -> "fly";case "dark_power" -> "power";
      case "amy","omochao","chaos_light","chaos_angel","chaos_devil" -> "baby";
      default -> selected;
    };
  }
  public String sourceGeometryKey() {
    // These scoped gameplay paths retain the original baby UV and attachment
    // family while their saved/logical alignment and first type remain locked.
    return current.mesh.heroRunPrototype()||current.mesh.neutralPrototype056()||current.mesh.heroNormalPrototype057()||current.mesh.neutralSwimPrototype059()
        ?"baby":sourceGeometryKey(key);
  }
  public String geometryKey() { return key; }
  long shapeSignature(){return current.mesh.shapeSignature();}
  boolean naturalHeroCoreActive(){return current.mesh.naturalHeroCoreActive();}
  void expression(int value){current.mesh.expression(value);}
  void eyesVisible(boolean visible){current.mesh.eyesVisible(current.poses,visible);}
  Map<String, ChaoMeshAsset.Pose> poses() { return current.poses; }
  /** Actual sampled surface; a logical family can select a shared morph asset. */
  ChaoMeshAsset sampledMesh() { return current.mesh; }
  /** Visible terminal volume in arm-local pixels, before procedural reach. */
  float[] baseToyHandBounds(boolean left) {
    var arm=pose(left?"arm_left":"arm_right");
    if(arm==null)return null;
    float reach=arm.reach;arm.reach=0;
    try{return current.mesh.visibleHandBounds(current.poses,left);}
    finally{arm.reach=reach;}
  }
  ChaoMeshAsset.Pose pose(String name) { return current.poses.get(name); }
  public int sampledFrames() { return sampledFrames; }

  public void animate(String model, float ageTicks, float limbSwing, float movement,
                      int emotion, float emotionSeconds, float yawDegrees, float pitchDegrees) {
    select(model);
    referenceRendering=true;
    current.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    float time = ageTicks / 20f;
    float walking = Mth.clamp(movement * 4f, 0, 1);
    if(emotion==0) {
      apply("animation.chao.idle", time, 1 - walking, 0, 0);
      apply("animation.chao.walk", time, walking, 0, 0);
    }
    String clip = switch (emotion) { case 1 -> "sad"; case 2 -> "alert"; case 3 -> "happy"; default -> "no_emotion"; };
    apply("animation.chao." + clip, emotion == 0 ? time : Math.max(0, emotionSeconds), 1, 0, 0);
    // Mutually exclusive modelled symbols: unused authoring glyphs are never
    // rendered together when a clip omits their explicit scale channel.
    String marker = emotion == 3 ? "emotion_heart" : emotion == 2 ? "emotion_exclamation" : emotion == 0 ? "emotion_dot" : "";
    for (String symbol : List.of("emotion_dot", "emotion_heart", "emotion_exclamation", "emotion_question", "emotion_dizzy"))
      if (!symbol.equals(marker)) hidePart(symbol);
    var skull = pose("skull");
    if (skull != null) {
      skull.ry += Mth.clamp(yawDegrees * Mth.DEG_TO_RAD, -.65f, .65f);
      skull.rx += Mth.clamp(pitchDegrees * Mth.DEG_TO_RAD, -.25f, .25f);
    }
    sampledFrames++;
  }

  public Motion motion() { return motion; }
  public float idleGestureWeight() { return idleGestureWeight; }

  /** A single imported sway gesture every 13-20 seconds, with a per-Chao phase. */
  public static float gestureWeight(float ageTicks, long seed) {
    int period = 260 + (int) Math.floorMod(seed, 140L);
    float phase = (ageTicks + Math.floorMod(seed >>> 17, (long) period)) % period;
    if (phase < 0) phase += period;
    if (phase >= 27.5f) return 0;
    float fade = Math.min(phase / 5f, (27.5f - phase) / 5f);
    float value = Mth.clamp(fade, 0, 1);
    return value * value * (3 - 2 * value);
  }

  private static float gestureTime(float ageTicks, long seed) {
    int period = 260 + (int) Math.floorMod(seed, 140L);
    float phase = (ageTicks + Math.floorMod(seed >>> 17, (long) period)) % period;
    if (phase < 0) phase += period;
    return phase / 20f;
  }

  /**
   * Gameplay controller. The raw animate method above remains the exact source-clip
   * sampler used by the imported-animation inspection; ordinary creatures use this
   * quieter controller and stat-dependent locomotion instead.
   */
  public void animateGameplay(String model, UUID id, Motion requested, float ageTicks,
      float limbSwing, float movement, int emotion, float emotionSeconds,
      float yawDegrees, float pitchDegrees, boolean allowGesture) {
    animateGameplay(model,id,requested,ageTicks,limbSwing,movement,emotion,emotionSeconds,
        yawDegrees,pitchDegrees,allowGesture,true);
  }

  /** Defer the blend when the model will add held-item, gene or activity poses. */
  void animateGameplay(String model, UUID id, Motion requested, float ageTicks,
      float limbSwing, float movement, int emotion, float emotionSeconds,
      float yawDegrees, float pitchDegrees, boolean allowGesture, boolean finishTransition) {
    select(model,true);
    referenceRendering=false;
    motion = requested;
    current.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    var monsterCrown=pose("monster_crown");if(monsterCrown!=null)monsterCrown.visible=false;
    long seed = id.getLeastSignificantBits() ^ id.getMostSignificantBits();
    float activity = Mth.clamp(movement * 22f, 0, 1);
    boolean uprightGait = requested == Motion.WALK || requested == Motion.RUN || requested == Motion.DASH;
    float walk = uprightGait ? activity : 0;
    boolean gesture = allowGesture && emotion == 0 && activity < .04f
        && (uprightGait || requested == Motion.HELD);
    idleGestureWeight = gesture ? gestureWeight(ageTicks, seed) : 0;
    idleGestureWeight *= 1 - walk;
    if (requested == Motion.HELD) idleGestureWeight *= .35f;
    if(emotion==0) {
      apply("animation.chao.idle", 0, 1 - walk - idleGestureWeight, 0, 0);
      apply("animation.chao.idle", gestureTime(ageTicks, seed), idleGestureWeight, 0, 0);
    }
    // Vanilla limbSwing integrates distance, not clip seconds. Feeding it directly
    // to the imported clip made a high-Run Chao flicker through many full strides.
    var gaitClock = gaitClocks.computeIfAbsent(id, ignored -> new ChaoGaitClock());
    float gaitTime = gaitClock.sample(ageTicks, limbSwing, movement, uprightGait && emotion == 0);
    if(emotion==0)apply("animation.chao.walk", gaitTime, walk, 0, 0);
    float breath = Mth.sin(ageTicks * .055f + (seed & 1023) * .01f);
    var body = pose("body");
    var skull = pose("skull");
    if (requested == Motion.RUN || requested == Motion.DASH) {
      // Keep the authored feet and their ground contact, but steady the broad
      // walking shimmy as the faster gait leans into its direction of travel.
      float steady = 1 - walk * (requested == Motion.DASH ? .60f : .38f);
      if (body != null) {
        body.x *= steady; body.y *= steady; body.ry *= steady; body.rz *= steady;
      }
      if (skull != null) {
        skull.x *= steady; skull.y *= steady; skull.z *= steady; skull.rz *= steady;
      }
    }
    // Broad Dark Power, Chaos, Tails and developed Swim heads can meet walking palms;
    // that inherited overlap also carries into the first toy-entry frames.
    if((broadWalkHead||powerSwimWeight>0)&&uprightGait&&skull!=null){
      float anatomy=broadWalkHead?1:powerSwimWeight;
      float steady=1-.9f*walk*anatomy;
      skull.x*=steady;skull.rz*=steady;skull.y-=.35f*walk*anatomy;
      if(body!=null){body.x*=steady;body.y*=steady;body.rz*=steady;}
      if(powerSwimWeight>0)for(boolean left:new boolean[]{false,true}){var arm=pose(left?"arm_left":"arm_right");if(arm!=null)arm.x+=(left?.75f:-.75f)*walk*powerSwimWeight;}
    }
    if (body != null) { body.y += breath * .035f; body.sy += breath * .004f; }
    if (skull != null) { skull.y += breath * .02f; skull.rx += breath * .006f; }
    switch (requested) {
      // A newborn covers only a few hundredths of a block per tick. Upright
      // gait weighting and stride length made its previous tiny strokes almost
      // disappear. Keep the crawl short and readable at that actual speed.
      case CRAWL -> crawl(limbSwing * 7.5f, Mth.clamp(movement * 45f, 0, 1));
      case RUN, DASH -> {
        float lean = requested == Motion.DASH ? .24f : .12f;
        if (body != null) body.rx += lean * activity;
        if (skull != null) { skull.z -= lean * 2 * activity; skull.y += lean * activity; }
      }
      case FLAIL, PADDLE, SWIM -> swim(requested, ageTicks);
      case GLIDE -> glide(ageTicks);
      case GRIP -> grip();
      case FALL -> {
        rotate("arm_left", -.1f, .1f, -.2f);
        rotate("arm_right", -.1f, -.1f, .2f);
        rotate("skull", -.10f, 0, 0);
      }
      case SIT -> sit(false);
      case REST -> {
        sit(true);
        // The sleeping child at 06:27-06:29 curls low with its face down,
        // rather than remaining in the same upright sitting silhouette.
        if(key.equals("baby")) {
          if(body!=null){body.y-=1.10f;body.rx=.75f;body.rz=.16f;}
          if(skull!=null){skull.y+=1.25f;skull.z-=.8f;skull.rx+=.34f;skull.rz+=.10f;}
          rotate("arm_left",-.15f,.65f,.50f);rotate("arm_right",-.15f,-.65f,-.50f);
        }
      }
      case PET -> {
        sit(false);
        // SA2's long pet sequence keeps the feet planted and the arms low.
        // The imported celebration remains available to DELIGHT/comparison.
        float content=Mth.sin(ageTicks*.15f);
        rotate("skull",-.09f+content*.025f,0,content*.035f);
        for(String name:List.of("arm_left","arm_right")) {
          var arm=pose(name);if(arm==null)continue;
          float side=name.endsWith("left")?1:-1;
          arm.y=.5f;arm.rx=-.06f;arm.rz=side*.95f;
        }
      }
      case HELD -> held(ageTicks);
      case PONDER, INSPECT, EAT, TRAIN, REJECT, DELIGHT -> sit(false);
      case PLAY -> sit(false);
      default -> { }
    }
    String reaction = requested==Motion.PET ? "no_emotion" : switch (emotion) { case 1 -> "sad"; case 2 -> "alert"; case 3 -> "happy"; default -> "no_emotion"; };
    apply("animation.chao." + reaction, emotion == 0 ? ageTicks / 20f : Math.max(0, emotionSeconds), 1, 0, 0);
    String marker = emotion == 3 ? "emotion_heart" : emotion == 2 ? "emotion_exclamation" : emotion == 0 ? "emotion_dot" : "";
    for (String symbol : List.of("emotion_dot", "emotion_heart", "emotion_exclamation", "emotion_question", "emotion_dizzy"))
      if (!symbol.equals(marker)) hidePart(symbol);
    if (skull != null) {
      skull.ry += Mth.clamp(yawDegrees * Mth.DEG_TO_RAD, -.65f, .65f);
      skull.rx += Mth.clamp(pitchDegrees * Mth.DEG_TO_RAD, -.25f, .25f);
    }
    if(switch(requested){case SIT,PET,PONDER,INSPECT,EAT,TRAIN,REJECT,DELIGHT,PLAY->true;default->false;})
      ChaoSittingPose.apply(current.mesh,current.poses,adultPose);
    if(finishTransition)transition(id,requested,emotion,ageTicks);
    sampledFrames++;
  }

  private void rotate(String name, float x, float y, float z) {
    var p = pose(name); if (p != null) { p.rx += x; p.ry += y; p.rz += z; }
  }

  /** Arms reach around the keeper's sides; inherited head anatomy stays intact. */
  private void grip() {
    var body = pose("body"); if (body != null) body.rx = 0;
    var head = pose("skull");
    if (head != null) { head.rx = -.04f; head.ry = 0; head.rz = 0; }
    for (String name : List.of("arm_left", "arm_right")) {
      var arm = pose(name); if (arm == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      arm.x = side * .45f; arm.y = .15f; arm.z = -1.8f;
      arm.rx = 0; arm.ry = side * .85f; arm.rz = side * .06f;
    }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var foot = pose(name); if (foot == null) continue;
      foot.y = -.25f; foot.z = -.7f; foot.rx = -.25f;
    }
    rotate("wing_left", 0, -.55f, 0); rotate("wing_right", 0, .55f, 0);
  }

  /** The comparison renderer keeps its exact mesh, UVs and source clips. */
  void applyCompanionGrip() { motion = Motion.GRIP; grip(); }

  /** Blend the existing seated pose into the existing grip, including replacement hands. */
  void applyCompanionCarry(float back) {
    if(back<=0)return;
    var before=new HashMap<String,float[]>();snapshot(current.poses,before);
    grip();
    current.poses.forEach((name,p)->{
      var a=before.get(name);if(a==null)return;
      p.x=Mth.lerp(back,a[0],p.x);p.y=Mth.lerp(back,a[1],p.y);p.z=Mth.lerp(back,a[2],p.z);
      p.rx=Mth.lerp(back,a[3],p.rx);p.ry=Mth.lerp(back,a[4],p.ry);p.rz=Mth.lerp(back,a[5],p.rz);
    });
  }


  private record CrawlStep(float reach, float lift) {}

  /** Sixty percent planted push, then a lifted recovery to the next contact. */
  private static CrawlStep crawlStep(float cycle) {
    float phase = cycle / Mth.TWO_PI;
    phase -= Mth.floor(phase);
    if (phase < .6f) return new CrawlStep(Mth.lerp(phase / .6f, -1.05f, 1.05f), 0);
    float recovery = (phase - .6f) / .4f;
    float ease = recovery * recovery * (3 - 2 * recovery);
    return new CrawlStep(Mth.lerp(ease, 1.05f, -1.05f),
        Mth.sin(recovery * Mth.PI) * .85f);
  }

  private void crawl(float cycle, float movement) {
    var body = pose("body");
    var head = pose("skull");
    final float pitch = 1.10f;
    float rock = Mth.sin(cycle) * movement;
    float bob = (1 - Mth.cos(cycle * 2)) * .045f * movement;
    if (body != null) {
      body.y += .5f + bob; body.rx = pitch;
      body.x += rock * .12f;
      body.ry += rock * .025f;
      body.rz += rock * .045f;
    }
    if (head != null) {
      // The source skull is the torso's sibling. Its low, upright face leads
      // the hands while the shoulders and tucked rear feet do the crawling.
      // Keep 0.7 pixel more shoulder clearance than the old static crawl: the
      // recovery hand now actually lifts toward the underside of the head.
      float lower = key.equals("hero") ? .3f : .7f;
      head.y += lower - bob * .35f;
      head.x += rock * .025f;
      head.z -= 3.4f; head.rx += .025f;
    }
    float sinPitch = Mth.sin(pitch), cosPitch = Mth.cos(pitch);
    for (String name : List.of("arm_left", "arm_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      CrawlStep step = crawlStep(cycle + (side < 0 ? Mth.PI : 0));
      float reach = step.reach * movement;
      float lift = step.lift * movement;
      // Express the visible hand arc in ground coordinates, then undo the
      // torso's pitch. A planted hand sweeps backward as the body moves ahead;
      // the other hand lifts and reaches instead of both sliding together.
      float vertical = -lift - bob;
      p.x = side * .9f;
      p.y = cosPitch * vertical + sinPitch * reach;
      p.z = .7f - sinPitch * vertical + cosPitch * reach;
      p.rx = 0; p.ry = side * pitch; p.rz = side * (1.4f - lift * .09f);
    }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.contains("left") ? 1 : -1;
      // Each rear knee pushes with the opposite forelimb, then tucks forward.
      CrawlStep step = crawlStep(cycle + (side > 0 ? Mth.PI : 0));
      float reach = step.reach * .7f * movement;
      float lift = step.lift * .6f * movement;
      float depth = current.mesh.forwardReach(name);
      float bodyY = current.mesh.pivot("root", 1) + current.mesh.pivot("body", 1) + .5f;
      float ankle = current.mesh.pivot(name, 1);
      float rear = (bodyY + ankle * cosPitch + depth - 24f) / sinPitch;
      float vertical = -lift - bob;
      p.y = cosPitch * vertical + sinPitch * reach;
      p.z = rear - current.mesh.pivot(name, 2) - sinPitch * vertical + cosPitch * reach;
      p.rx = Mth.HALF_PI - pitch - lift * .22f; p.ry = 0; p.rz = 0;
    }
  }

  private void sit(boolean sleeping) {
    var body = pose("body"); var skull = pose("skull");
    float lower = sleeping ? 1.2f : .8f;
    if(powerSwimWeight>0)for(boolean left:new boolean[]{false,true}){var arm=pose(left?"arm_left":"arm_right");if(arm!=null)arm.rz+=(left?.22f:-.22f)*powerSwimWeight;}
    if (body != null) { body.y += lower; body.rx -= .08f; }
    if (skull != null) { skull.y += lower; skull.rx += sleeping ? .15f : .025f; if(sleeping)skull.rz += .18f; }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var p = pose(name); if (p != null && sleeping) { p.y -= .8f; p.z -= .5f; p.rx -= .65f; }
    }
  }

  /** Suspended, slightly tucked feet and low outstretched hands, as at 05:27. */
  private void held(float age) {
    float swing=Mth.sin(age*.11f),kick=Mth.sin(age*.17f);
    rotate("body",.035f,0,swing*.025f);
    rotate("skull",.055f,0,-swing*.018f);
    for(String name:List.of("arm_left","arm_right")) {
      var p=pose(name);if(p==null)continue;
      float side=name.endsWith("left")?1:-1;
      p.y=.45f;p.z-=.15f;p.rx=-.12f+side*swing*.025f;
      p.ry=side*.25f;p.rz=side*.80f;
    }
    for(String name:List.of("foot_left","foot_right","foot_left2","foot_right2","feet_bone_right")) {
      var p=pose(name);if(p==null)continue;
      float side=name.contains("left")?1:-1;
      p.y-=.25f;p.z-=.55f;p.rx=-.58f+side*kick*.06f;
    }
  }

  private void swim(Motion mode, float age) {
    float phase = age * (mode == Motion.FLAIL ? .48f : mode == Motion.SWIM ? .21f : .26f);
    var body = pose("body"); var head = pose("skull");
    float lean = mode == Motion.FLAIL ? -.09f : mode == Motion.SWIM ? .75f : .45f;
    if (body != null) {body.rx += lean;if(mode==Motion.FLAIL)body.rz+=Mth.sin(phase)*.035f;}
    if (head != null) { head.y += Math.max(0, lean); head.z -= Math.max(0, lean) * 1.8f; head.rx += mode == Motion.FLAIL ? -.08f : .05f; }
    for (String name : List.of("arm_left", "arm_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      if (mode == Motion.FLAIL) {
        // Small frantic alternating scoops while the body remains upright;
        // this is deliberately distinct from the trained breaststroke.
        p.rx = -.20f+Mth.sin(phase + (side < 0 ? Mth.PI : 0)) * .48f;
        p.ry = side*.25f;
        p.y=.35f;p.rz = side*(.55f+Mth.cos(phase)*.13f);
      } else if (mode == Motion.PADDLE) {
        p.rx += Mth.sin(phase + (side < 0 ? Mth.PI : 0)) * .42f;
        p.ry += side * .2f;
      } else {
        p.ry += side * (.45f + Mth.sin(phase) * .5f);
        p.rx += .2f + Mth.cos(phase) * .25f;
        p.rz -= side * .15f;
      }
    }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var p = pose(name); if (p != null) {
        p.rx += Mth.cos(phase + (name.contains("left") ? 0 : Mth.PI)) * (mode == Motion.FLAIL ? .62f : .35f);
        if(mode==Motion.FLAIL){p.rx-=.25f;p.z-=.32f;}
      }
    }
  }

  private void glide(float age) {
    var body = pose("body"); var head = pose("skull");
    if (body != null) body.rx += .65f;
    if (head != null) { head.y += .6f; head.z -= 1.2f; }
    for (String name : List.of("arm_left", "arm_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      p.rz = side * .28f; p.ry = side * .2f; p.rx = -.08f;
    }
    rotate("wing_left", 0, -.7f + Mth.sin(age * .12f) * .06f, 0);
    rotate("wing_right", 0, .7f - Mth.sin(age * .12f) * .06f, 0);
    rotate("foot_left", -.3f, 0, 0); rotate("foot_right", -.3f, 0, 0);
  }

  private static void snapshot(Map<String, ChaoMeshAsset.Pose> poses, Map<String, float[]> target) {
    poses.forEach((name,p) -> {
      if (name.contains("__cube_")) return;
      float[] values = target.computeIfAbsent(name, key -> new float[10]);
      values[0]=p.x;values[1]=p.y;values[2]=p.z;values[3]=p.rx;values[4]=p.ry;
      values[5]=p.rz;values[6]=p.sx;values[7]=p.sy;values[8]=p.sz;values[9]=p.reach;
    });
  }

  private void transition(UUID id, Motion mode, int emotion, float age) {
    Transition state = transitions.get(id);
    // A culled Chao can change activities while it is offscreen. Replaying
    // an old pose on re-entry would attach a current prop to stale, lowered arms.
    if (state == null || age < state.age - 1 || age - state.age > 8) {
      state = new Transition(); state.mode = mode; state.emotion=emotion; state.geometry = key;
      transitions.put(id, state);
    } else if (state.mode != mode || state.emotion != emotion || !key.equals(state.geometry)) {
      state.fromProne=state.lastProne||(state.fromProne&&state.from!=null);
      state.lastProne=false;
      state.from = state.last; state.last = null; state.started = age; state.mode = mode; state.emotion=emotion; state.geometry = key;
    }
    if (state.from != null) {
      float blend = Mth.clamp((age - state.started) / 8f, 0, 1);
      blend = blend * blend * (3 - 2 * blend);
      final float weight = blend;
      state.from.forEach((name, prior) -> {
        var p = pose(name); if (p == null) return;
        p.x=Mth.lerp(weight,prior[0],p.x);p.y=Mth.lerp(weight,prior[1],p.y);p.z=Mth.lerp(weight,prior[2],p.z);
        p.rx=Mth.lerp(weight,prior[3],p.rx);p.ry=Mth.lerp(weight,prior[4],p.ry);p.rz=Mth.lerp(weight,prior[5],p.rz);
        p.sx=Mth.lerp(weight,prior[6],p.sx);p.sy=Mth.lerp(weight,prior[7],p.sy);p.sz=Mth.lerp(weight,prior[8],p.sz);p.reach=Mth.lerp(weight,prior[9],p.reach);
      });
      if (blend >= 1){state.from = null;state.fromProne=false;}
    }
    state.age = age;
    if (state.last == null) state.last = new HashMap<>();
    snapshot(current.poses, state.last);
    if (transitions.size() > 96) transitions.remove(transitions.keySet().iterator().next());
  }

  /** Blend the complete outgoing/target poses, including interaction additions. */
  void finishGameplayFrame(UUID id, int emotion, float ageTicks) {
    if(switch(motion){case SIT,PET,PONDER,INSPECT,EAT,TRAIN,REJECT,DELIGHT,PLAY->true;default->false;})
      for(String foot:ChaoSittingPose.FEET)ChaoSittingPose.ground(current.mesh,current.poses,foot);
    transition(id,motion,emotion,ageTicks);
  }

  /** Keep the final constraint-fitted pose for interruption recovery. */
  void rememberGameplayFrame(UUID id) {
    var state=transitions.get(id);if(state==null)return;
    if(state.last==null)state.last=new HashMap<>();
    snapshot(current.poses,state.last);
  }
  /** Only the actual completed car/crayon pose marks an outgoing floor-play blend. */
  void rememberProneFrame(UUID id,boolean prone){var state=transitions.get(id);if(state!=null)state.lastProne=prone;}
  boolean recoveringProne(UUID id){var state=transitions.get(id);return state!=null&&state.from!=null&&state.fromProne;}

  void transformBone(String name, PoseStack stack) { current.mesh.transformBone(name,stack,current.poses); }
  void copyAttachmentPoseTo(String name,ChaoMeshAsset target,ChaoMeshAsset.Pose to) {
    var from=current.poses.get(name);if(from!=null)current.mesh.copyJointPoseTo(name,target,from,to);
  }

  float[] rattlePalm() { return toyPalm(false); }
  float[] toyPalm(boolean left) { return current.mesh.toyPalm(current.poses,left); }
  float[] attachmentBounds(String name) { return current.mesh.attachmentBounds(name,current.poses); }
  float[] attachmentMouth() { return current.mesh.attachmentMouth(current.poses); }
  float[] attachmentEyes() { return current.mesh.attachmentEyes(current.poses); }

  void symbol(String selected) {
    for(String name:List.of("emotion_dot","emotion_heart","emotion_exclamation","emotion_question","emotion_dizzy")) {
      var p=pose(name);if(p==null)continue;
      current.mesh.setSubtreeVisible(name,current.poses,name.equals(selected));
      if(name.equals(selected))p.sx=p.sy=p.sz=1;
    }
  }

  /** Set this entity's geometry before gait fitting samples its current toe reach. */
  void prepareDevelopment(String model,ChaoState state) {
    neutralSelection056=NeutralAppearance056.sample(state).enabled();
    heroNormalSelection057=HeroNormalAppearance057.sample(state).enabled();
    neutralSwimSelection059=NeutralSwimAppearance059.sample(state).enabled();
    prototypeSelection=neutralSwimSelection059||heroNormalSelection057||neutralSelection056||HeroRunAppearance052.sample(state).enabled();prototypeOwnerKey=model;
    select(model,true);meshDevelopment(state);
  }

  private void meshDevelopment(ChaoState state) {
    adultPose=state.adult;
    var neutralSwim=NeutralSwimAppearance059.sample(state);
    // The inherited wide-palmed Swim gait follows this profile's actual
    // current-bar anatomy, including reductions; legacy paths keep their input.
    powerSwimWeight=neutralSwim.enabled()?Math.max(0,neutralSwim.runPower())*neutralSwim.maturity()
        :state.adult&&"normal".equals(state.character)&&state.primaryType==ChaoState.SWIM_TYPE?Mth.clamp(state.runPower,0,1)*state.gradualVisualStrength():0;
    // This Swim profile retains the full original head even at zero Power.
    // Keep its walking palms clear independently of reversible specialization.
    broadWalkHead=neutralSwim.enabled()||state.immortal||"tails".equals(state.character)||state.adult&&"normal".equals(state.character)&&state.align<=-.5f&&state.primaryType==ChaoState.POWER_TYPE;
    boolean develop=state.adult && !state.immortal
        && (state.character==null||state.character.equals("normal"));
    boolean child=!state.adult && !state.immortal && (state.character==null||state.character.equals("normal"));
    current.mesh.naturalHeroCore(develop&&ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state))&&state.align>=.5f&&state.primaryType==ChaoState.SWIM_TYPE&&state.baseColor()==0&&state.jewel()==0);
    float growth=state.gradualVisualStrength();
    current.mesh.development(develop||child?state.swimFly:0,develop||child?state.runPower:0,
        develop?growth:child?growth*.14f:0);
    current.mesh.heroRunDevelopment(current.mesh.heroRunPrototype()?HeroRunAppearance052.sample(state):null,state.swimFly,state.runPower);
    current.mesh.heroRunTwoTone(state.baseColor()==0&&state.jewel()==0&&state.isTwoTone());
    if(current.mesh.neutralPrototype056())current.mesh.neutralDevelopment(NeutralAppearance056.sample(state));
    if(current.mesh.heroNormalPrototype057())current.mesh.heroNormalDevelopment(HeroNormalAppearance057.sample(state));
    if(current.mesh.neutralSwimPrototype059())current.mesh.neutralSwimDevelopment(neutralSwim);
  }

  /** Reversible secondary anatomy is blended independently from biological age. */
  void development(ChaoState state) {
    meshDevelopment(state);
    var marker=pose("emotion_dots");
    if(marker!=null) {
      float offset=current.mesh.proportionMarkerOffset();
      marker.y+=offset-marker.proportionY;
      marker.proportionY=offset;
    }
    int[] colors={0,0xff4a9df1,0xff48bd67,0xffdf505c,0xffffd852,0xff9b68d9,
        0xffee8ab5,0xffef9b47,0xff48d5d2,0xff8edfff,0xffd75d55,0xff61e58b,0xffffd852};
    current.mesh.lens(colors[Math.max(0,Math.min(colors.length-1,state.eyeColor))]);
    int stage=state.monsterStage();float boost=stage==0?0:Mth.clamp(Math.max(stage*.35f,state.monsterInfluence()-1),0,2);
    var crown=pose("monster_crown");if(crown!=null)crown.visible=stage>0&&state.monsterType()==ChaoState.NORMAL;
    if(boost>0) {
      // Authored amplification beyond ordinary development. Semantic anchors
      // scale with their attachments; comparison sampling never enters here.
      var skull=pose("skull");if(skull!=null&&!current.mesh.fixedHeadBox055()){skull.sx*=1+boost*.05f;skull.sy*=1+boost*.03f;skull.sz*=1+boost*.07f;}
      var body=pose("body");if(body!=null){body.sx*=1+boost*.08f;body.sy*=1+boost*.05f;body.y-=boost*.125f;}
      for(var entry:current.poses.entrySet())if(entry.getKey().startsWith("evolution_ridge_")||entry.getKey().startsWith("evolution020_")) {
        var ridge=entry.getValue();ridge.sx*=1+boost*.12f;ridge.sy*=1+boost*.15f;ridge.sz*=1+boost*.25f;
      }
      if(crown!=null){crown.sy=1+boost*.2f;crown.sz=1+boost*.2f;}
    }
  }

  /** Public special clips retained for matching source rigs and development inspection. */
  public void applySpecial(boolean carrying, float swelling, float ageTicks) {
    if (key.equals("ender") && carrying) {
      apply("animation.chao_ender.carry", ageTicks / 20f, 1, 1, swelling);
      // Idle/walk use variable.carry to hide this mesh. Carry is supplied here
      // after locomotion sampling, so restore its authored unit scale as well.
      var block = pose("held_block");
      if (block != null) block.sx = block.sy = block.sz = 1;
    }
    if (key.equals("creeper")) apply("animation.chao_creeper.swell", ageTicks / 20f, 1, 0, swelling);
  }

  public void hideCube(String bone, int index) {
    var p = pose(bone + "__cube_" + index); if (p != null) p.visible = false;
  }
  void hidePart(String name) { current.mesh.setSubtreeVisible(name, current.poses, false); }
  void hideHeadDecorations() { current.mesh.hideHeadDecorations(current.poses); }

  public void render(PoseStack stack, VertexConsumer vertices, int light, int overlay, int color) {
    current.mesh.render(stack, vertices, light, overlay, color, current.poses,!referenceRendering);
  }

  public boolean finite() {
    for (var p : current.poses.values())
      for (float v : new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz,p.reach})
        if (!Float.isFinite(v)) return false;
    return true;
  }

  private void apply(String name, float seconds, float weight, float carry, float swelling) {
    if (weight <= 0) return;
    JsonObject clip = clips.getAsJsonObject(name);
    float time = seconds;
    if (clip.has("animation_length")) {
      float length = clip.get("animation_length").getAsFloat();
      boolean loop = clip.has("loop") && clip.get("loop").isJsonPrimitive()
          && clip.get("loop").getAsJsonPrimitive().isBoolean() && clip.get("loop").getAsBoolean();
      time = loop ? seconds % length : Math.min(seconds, length);
    }
    for (var entry : clip.getAsJsonObject("bones").entrySet()) {
      var p = pose(entry.getKey()); if (p == null) continue;
      JsonObject channels = entry.getValue().getAsJsonObject();
      for (var channel : channels.entrySet()) {
        String property = channel.getKey();
        for (int i=0; i<3; i++) {
          float sign = property.equals("rotation") ? (i == 0 ? 1 : -1) : (i == 2 ? 1 : -1);
          float currentValue = switch (property) {
            case "rotation" -> (i==0?p.rx:i==1?p.ry:p.rz) / Mth.DEG_TO_RAD * sign;
            case "position" -> (i==0?p.x:i==1?p.y:p.z) * sign;
            case "scale" -> i==0?p.sx:i==1?p.sy:p.sz;
            default -> 0;
          };
          float v = sample(channel.getValue(), time, i, currentValue, carry, swelling);
          if (property.equals("rotation")) {
            v *= sign * Mth.DEG_TO_RAD * weight;
            if(i==0)p.rx+=v;else if(i==1)p.ry+=v;else p.rz+=v;
          } else if (property.equals("position")) {
            v *= sign * weight;
            if(i==0)p.x+=v;else if(i==1)p.y+=v;else p.z+=v;
          } else if (property.equals("scale")) {
            v=(v-1)*weight;
            if(i==0)p.sx+=v;else if(i==1)p.sy+=v;else p.sz+=v;
          }
        }
      }
    }
  }

  private float sample(JsonElement data, float time, int component,
                              float prior, float carry, float swelling) {
    if (!data.isJsonObject()) return value(data, component, time, prior, carry, swelling);
    var points = channelPoints.computeIfAbsent(data, source -> {
      var sorted = new TreeMap<Float,JsonElement>();
      for (var p : source.getAsJsonObject().entrySet()) sorted.put(Float.parseFloat(p.getKey()),p.getValue());
      return sorted;
    });
    var a = points.floorEntry(time); if(a==null)a=points.firstEntry();
    var b = points.ceilingEntry(time); if(b==null)b=points.lastEntry();
    float av=value(a.getValue(),component,time,prior,carry,swelling);
    float bv=value(b.getValue(),component,time,prior,carry,swelling);
    return a.getKey().equals(b.getKey()) ? av : Mth.lerp((time-a.getKey())/(b.getKey()-a.getKey()),av,bv);
  }

  private static float value(JsonElement raw, int component, float time,
                             float prior, float carry, float swelling) {
    if(raw.isJsonArray())raw=raw.getAsJsonArray().get(component);
    if(raw.getAsJsonPrimitive().isNumber())return raw.getAsFloat();
    return new Expression(raw.getAsString(),time,prior,carry,swelling).parse();
  }

  /** Small deterministic Molang subset used by the eight supplied source clips. */
  private static final class Expression {
    final String source; final float time, prior, carry, swelling; int at;
    Expression(String s,float t,float p,float c,float swell){source=s.toLowerCase(Locale.ROOT).replace(" ","");time=t;prior=p;carry=c;swelling=swell;}
    float parse(){float n=sum();if(at!=source.length())throw new IllegalArgumentException("Unsupported Chao animation expression");return n;}
    boolean eat(char c){if(at<source.length()&&source.charAt(at)==c){at++;return true;}return false;}
    float sum(){float n=product();while(at<source.length()){if(eat('+'))n+=product();else if(eat('-'))n-=product();else break;}return n;}
    float product(){float n=atom();while(at<source.length()){if(eat('*'))n*=atom();else if(eat('/'))n/=atom();else break;}return n;}
    float atom(){if(eat('-'))return -atom();if(eat('+'))return atom();if(eat('(')){float n=sum();if(!eat(')'))throw new IllegalArgumentException("Animation parenthesis");return n;}
      int start=at;
      if(at<source.length()&&(Character.isDigit(source.charAt(at))||source.charAt(at)=='.')){while(at<source.length()&&(Character.isDigit(source.charAt(at))||source.charAt(at)=='.'))at++;return Float.parseFloat(source.substring(start,at));}
      while(at<source.length()&&(Character.isLetterOrDigit(source.charAt(at))||source.charAt(at)=='.'||source.charAt(at)=='_'))at++;
      if(start==at)throw new IllegalArgumentException("Animation expression token");
      String id=source.substring(start,at);
      if(eat('(')){float n=sum();if(!eat(')'))throw new IllegalArgumentException("Animation function");return switch(id){case "math.sin"->Mth.sin(n*Mth.DEG_TO_RAD);case "math.cos"->Mth.cos(n*Mth.DEG_TO_RAD);default->throw new IllegalArgumentException("Unsupported Chao animation function");};}
      return switch(id){case "query.anim_time"->time;case "this"->prior;case "variable.carry"->carry;case "variable.swelling_scale1"->(float)((Math.pow(Mth.clamp(swelling,0,1),4)*.4+1)*(Math.sin(swelling*5730*Mth.DEG_TO_RAD)*swelling*.01+1));case "variable.swelling_scale2"->(float)((Math.pow(Mth.clamp(swelling,0,1),4)*.1+1)/(Math.sin(swelling*5730*Mth.DEG_TO_RAD)*swelling*.01+1));default->throw new IllegalArgumentException("Unsupported Chao animation variable");};
    }
  }
}
