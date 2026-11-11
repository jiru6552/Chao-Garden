package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Original additions fitted to the imported rig and following its sampled poses. */
public final class ChaoOverlayLayer extends RenderLayer<ChaoEntity, ChaoModel> {
  private record Animal(int slot,int id) {}
  record Rig(ChaoMeshAsset geometry, Map<String, ChaoMeshAsset.Pose> poses,Map<String,Animal> animals) {}
  private final Map<String, Rig> rigs = new HashMap<>();
  private final Map<String, Rig> cweRigs = new HashMap<>();

  public ChaoOverlayLayer(RenderLayerParent<ChaoEntity, ChaoModel> parent) {
    super(parent);
  }

  private Rig rig(String name) {
    String key = name.startsWith("chao_") ? name : "chao_" + name;
    return rigs.computeIfAbsent(key, id -> {
      var geometry = ChaoMeshAsset.load("overlays/" + id);
      return fittedRig(geometry);
    });
  }
  static Rig fittedRig(ChaoMeshAsset geometry) {
    var poses=geometry.createPoses();var animals=new HashMap<String,Animal>();
    for(String name:poses.keySet())if(name.startsWith("animal_")) {
      String[] values=name.split("_");animals.put(name,new Animal(Integer.parseInt(values[1]),Integer.parseInt(values[2])));
    }
    return new Rig(geometry,poses,Map.copyOf(animals));
  }

  private static boolean hasGene(ChaoState state, int slot) {
    int animal = state.animalParts[slot];
    return state.allowsAnimalParts() && animal >= 0
        && (ChaoState.animalSlots(animal) & (1 << slot)) != 0;
  }

  static void copy(ChaoMeshAsset.Pose from, ChaoMeshAsset.Pose to) {
    to.x = from.x; to.y = from.y; to.z = from.z;
    to.rx = from.rx; to.ry = from.ry; to.rz = from.rz;
    to.sx = from.sx; to.sy = from.sy; to.sz = from.sz; to.reach = from.reach;
    // Imported base-mesh hiding must not hide an attached replacement limb.
    to.visible = true;
  }

  private static boolean headSlot(int slot) {
    // Face/beak panels keep their eye-relative placement; the morph preserves
    // the face plane while extending the crown and rear of the skull.
    return slot==ChaoState.EARS||slot==ChaoState.FOREHEAD||slot==ChaoState.HORNS;
  }

  /** Closed vanilla masks cover added head surfaces without changing saved DNA.
   * Existing open/SA2 hats keep their established animal-part presentation. */
  static boolean animalPartHiddenByHat(String hat,int slot) {
    return VanillaHatRenderer050.supports(hat)&&(headSlot(slot)||slot==ChaoState.FACE);
  }

  /** Fit only the added root in skull space; the semantic skull keeps its sampled pose. */
  static void fitHead(Rig rig,String name,ChaoMeshAsset.Pose pose,ChaoAccessoryFit.Fit fit) {
    float px=rig.geometry.pivot(name,0),py=rig.geometry.pivot(name,1),pz=rig.geometry.pivot(name,2);
    pose.x=fit.x()+fit.sx()*(px+pose.x)-px;
    pose.y=fit.y()+fit.sy()*(py+pose.y)-py;
    pose.z=fit.z()+fit.sz()*(pz+pose.z)-pz;
    pose.sx*=fit.sx();pose.sy*=fit.sy();pose.sz*=fit.sz();
  }

  private static void shrinkHeadRoot(ChaoMeshAsset.Pose pose,float top,float scale) {
    pose.y=top+(pose.y-top)*scale;
    pose.sy*=scale;
  }

  @Override
  public void render(PoseStack stack, MultiBufferSource buffers, int light,
                     ChaoEntity entity, float swing, float amount, float partialTick,
                     float age, float yaw, float pitch) {
    ChaoModel model = getParentModel();
    if (entity.officialComparison() || !model.officialActive()
        || entity.isCocooned() || entity.isInvisible()) return;
    ChaoState state = entity.state();
    String variant = ChaoVariants.activeId(state);
    boolean themed = !state.immortal && state.character.equals("normal") && !ChaoVariants.NORMAL.equals(variant);
    boolean reaction = model.officialEmotion() != 0;
    boolean headGenes = hasGene(state, ChaoState.EARS)
        || hasGene(state, ChaoState.FOREHEAD) || hasGene(state, ChaoState.HORNS);
    boolean authoredChaos = state.immortal && state.character.equals("normal")
        && model.gameplayFormKey().startsWith("chaos_");
    String chaos = state.align >= .5f ? "angel" : state.align <= -.5f ? "devil" : "light";
    float[] headBounds=model.accessoryBounds("skull");
    var headFit=ChaoAccessoryFit.legacyHead(headBounds,model.officialGeometryKey());
    var bodyFit=ChaoAccessoryFit.legacyBody(model.accessoryBounds("body"),model.officialGeometryKey());
    var markerFit=ChaoAccessoryFit.legacyMarker(headBounds,model.officialGeometryKey());
    boolean covered=ChaoAccessoryFit.headCovered(entity);
    String hat=entity.getChaoHat();
    float headTop=headBounds==null?-6:headBounds[1];
    Rig rig = rig(model.officialGeometryKey());
    rig.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    rig.geometry.clearUvOverrides();
    for (var anchor : model.officialPoses().entrySet()) {
      var target = rig.poses.get(anchor.getKey());
      if (target != null) model.copyAccessoryPoseTo(anchor.getKey(),rig.geometry,target);
      // Legacy overlays use the logical right-foot name on donors whose
      // supplied joint is named feet_bone_right. Keep those attachments moving.
      if(anchor.getKey().equals("feet_bone_right")) {
        var alias=rig.poses.get("foot_right");if(alias!=null)copy(anchor.getValue(),alias);
      }
    }
    for (var entry : rig.poses.entrySet()) {
      String name = entry.getKey();
      var pose = entry.getValue();
      var tag=rig.animals.get(name);
      if (tag!=null) {
        int slot = tag.slot, animal = tag.id;
        pose.visible = hasGene(state, slot) && state.animalParts[slot] == animal
            && !animalPartHiddenByHat(hat,slot);
        if(pose.visible&&headSlot(slot)) {
          fitHead(rig,name,pose,headFit);
          if(!state.adult&&(slot==ChaoState.EARS||slot==ChaoState.FOREHEAD||slot==ChaoState.HORNS))
            shrinkHeadRoot(pose,headTop,slot==ChaoState.EARS?.82f:.8f);
        } else if(pose.visible&&!state.adult) {
          if(slot==ChaoState.ARMS||slot==ChaoState.FEET)pose.sx=pose.sy=pose.sz=.92f;
          else if(slot==ChaoState.WINGS||slot==ChaoState.TAIL)pose.sx=pose.sy=pose.sz=.85f;
        }
      } else if (name.startsWith("variant_")) {
        pose.visible = themed && name.startsWith("variant_" + variant + "_");
        if (name.endsWith("_head") && (headGenes||covered)) pose.visible = false;
        if(pose.visible&&name.endsWith("_head"))fitHead(rig,name,pose,headFit);
        if(pose.visible&&name.endsWith("_marker"))fitHead(rig,name,pose,markerFit);
        if(pose.visible&&name.endsWith("_body"))fitHead(rig,name,pose,bodyFit);
        if (name.endsWith("_marker") && (reaction || state.flameOrb)) pose.visible = false;
      } else if (name.startsWith("chaos_")) {
        pose.visible = state.immortal && state.character.equals("normal") && !authoredChaos
            && name.startsWith("chaos_" + chaos + "_");
        if (name.endsWith("_marker") && reaction) pose.visible = false;
        if(pose.visible&&(name.endsWith("_crest")||name.endsWith("_marker")))fitHead(rig,name,pose,headFit);
        if(name.contains("_wing_") && hasGene(state,ChaoState.WINGS))pose.visible=false;
        if(name.endsWith("_tail") && hasGene(state,ChaoState.TAIL))pose.visible=false;
        if(state.cosmeticWings && name.contains("_wing_"))rig.geometry.overrideBoneUv(name,16.5f,16.5f);
      } else if (name.equals("flame")) {
        pose.visible = !state.immortal && state.flameOrb && !reaction;
        if(pose.visible)fitHead(rig,name,pose,headFit);
      }
      if (pose.visible && (name.endsWith("_marker") || name.equals("flame"))) {
        pose.y += Mth.sin(age * .075f) * .18f;
        pose.ry += age * .012f;
      }
    }
    var raw=HeroRunAppearance052.sample(state).enabled()?HeroRunCoatMaterial052.textureOverlay(state)
        :ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/"+ChaoRenderer.textureKey(state)+".png");
    var texture = ChaoPaletteMaterial.texture(raw,state,"overlay");
    rig.geometry.render(stack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
        light, OverlayTexture.NO_OVERLAY, -1, rig.poses);
    if (ChaoShine.visible(state))
      rig.geometry.render(stack, buffers.getBuffer(ChaoShine.surface(texture)),
          light, OverlayTexture.NO_OVERLAY, -1, rig.poses);
    if (state.allowsAnimalParts()) {
      Rig parts=cweRigs.computeIfAbsent(model.officialGeometryKey(),key->{
        var geometry=ChaoMeshAsset.load("cwe_parts/chao_"+key);
        return fittedRig(geometry);
      });
      parts.poses.values().forEach(ChaoMeshAsset.Pose::reset);
      for(var anchor:model.officialPoses().entrySet()) {
        var pose=parts.poses.get(anchor.getKey());if(pose!=null)model.copyAccessoryPoseTo(anchor.getKey(),parts.geometry,pose);
      }
      for(var entry:parts.animals.entrySet()) {
        int slot=entry.getValue().slot,animal=entry.getValue().id;
        var pose=parts.poses.get(entry.getKey());pose.visible=hasGene(state,slot)&&state.animalParts[slot]==animal
            &&!animalPartHiddenByHat(hat,slot);
        if(pose.visible&&headSlot(slot)) {
          fitHead(parts,entry.getKey(),pose,headFit);
          if(!state.adult&&(slot==ChaoState.EARS||slot==ChaoState.HORNS||slot==ChaoState.FOREHEAD))
            shrinkHeadRoot(pose,headTop,.88f);
        }
      }
      var partsTexture=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/cwe_parts.png");
      parts.geometry.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(partsTexture)),
          light,OverlayTexture.NO_OVERLAY,-1,parts.poses);
    }
  }
}
