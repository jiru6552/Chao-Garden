package com.chaogarden.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Opt-in static style approval sheet. It never creates an entity/world,
 * samples the production morphology selector, or writes a Chao state. */
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class BlockyBabyStyleVisualSmoke {
  private static String[] VIEWS={"front","side","back"};
  private static float[] YAW={180,90,0};
  private static final AtomicInteger SAVED=new AtomicInteger();
  private static int[] FRAMES=new int[3];
  private static boolean started,finished;
  private static int phase,ticks;
  private static Path directory;
  private static JsonObject configuration=new JsonObject();
  private static String outputPrefix="blocky-baby-style";
  private static PreviewScreen screen;
  private BlockyBabyStyleVisualSmoke() {}

  private static boolean enabled(){return "true".equalsIgnoreCase(System.getenv("CHAO_BLOCKY_BABY_PREVIEW"));}
  private static String sha(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
  private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("BLOCKY_BABY_STYLE "+message);}
  private static String setting(String key,String fallback){return configuration.has(key)?configuration.get(key).getAsString():fallback;}
  private static int integerSetting(String key,int fallback){return configuration.has(key)?configuration.get(key).getAsInt():fallback;}
  private static void configureViews() {
    if(configuration.has("views")) {
      var requested=configuration.getAsJsonArray("views");
      require(requested.size()>=1&&requested.size()<=8,"static sheet needs one to eight views");
      VIEWS=new String[requested.size()];YAW=new float[requested.size()];var ids=new java.util.HashSet<String>();
      for(int i=0;i<requested.size();i++) {
        var view=requested.get(i).getAsJsonObject();String id=view.get("id").getAsString();float yaw=view.get("yaw").getAsFloat();
        require(id.matches("[a-z0-9][a-z0-9._-]*")&&ids.add(id),"invalid or duplicate view id");
        require(Float.isFinite(yaw),"non-finite static view rotation");VIEWS[i]=id;YAW[i]=yaw;
      }
    }
    FRAMES=new int[VIEWS.length];
  }
  private static Path input(String name)throws IOException {
    Path base=directory.toRealPath(),file=directory.resolve(name).normalize();
    require(!Path.of(name).isAbsolute()&&file.startsWith(directory.normalize()),"input must be inside the private preview directory");
    require(Files.isRegularFile(file)&&file.toRealPath().startsWith(base),"missing or external preview input "+name);
    return file;
  }

  /** Actual existing accessory resources, with independent poses per column. */
  private static final class StaticPart {
    final ChaoMeshAsset geometry;
    final Map<String,ChaoMeshAsset.Pose> poses;
    final ResourceLocation texture;
    final String geometryPath,texturePath,geometryHash,textureHash;
    StaticPart(String geometryPath,String texturePath)throws Exception {
      this.geometryPath=geometryPath;this.texturePath=texturePath;
      var resources=Minecraft.getInstance().getResourceManager();byte[] geometryBytes,textureBytes;
      try(var stream=resources.open(ResourceLocation.fromNamespaceAndPath("chao_garden",geometryPath))){geometryBytes=stream.readAllBytes();}
      try(var stream=resources.open(ResourceLocation.fromNamespaceAndPath("chao_garden",texturePath))){textureBytes=stream.readAllBytes();}
      geometryHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(geometryBytes));
      textureHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(textureBytes));
      geometry=ChaoMeshAsset.parse(JsonParser.parseString(new String(geometryBytes,StandardCharsets.UTF_8)).getAsJsonObject());
      poses=geometry.createPoses();texture=ResourceLocation.fromNamespaceAndPath("chao_garden",texturePath);
    }
    JsonObject provenance() {
      var value=new JsonObject();value.addProperty("model",geometryPath);value.addProperty("texture",texturePath);
      value.addProperty("model_sha256",geometryHash);value.addProperty("texture_sha256",textureHash);return value;
    }
  }

  private static final class Spec {
    final String id,label,modelFile,textureFile;
    final String hat;
    final boolean bow;
    final OfficialChaoRig rig;
    final ResourceLocation texture;
    final String modelHash,textureHash;
    final StaticPart hatPart,bowPart;
    Spec(String id,String label,Path model,Path textureFile,int index,JsonObject clips,String hat,boolean bow)throws Exception {
      this.id=id;this.label=label;this.modelFile=directory.relativize(model).toString();this.textureFile=directory.relativize(textureFile).toString();
      require(hat.isBlank()||hat.equals("red_wool_beanie"),"unsupported static hat "+hat);this.hat=hat;this.bow=bow;
      modelHash=sha(model);textureHash=sha(textureFile);
      var mesh=ChaoMeshAsset.parse(JsonParser.parseString(Files.readString(model)).getAsJsonObject());
      // The proposal has only static vertices. Also disable the old axis
      // lattice in the historical model so its original cubes stay exact.
      mesh.development(0,0,0);mesh.heroRunDevelopment(null,0,0);
      rig=new OfficialChaoRig(clips,key->mesh);
      // Deliberately do not call prepareDevelopment/development(state): that
      // would sample the paused prototype. This is one unchanged idle frame.
      rig.animateGameplay("baby",new UUID(255100L,index),OfficialChaoRig.Motion.WALK,
          0,0,0,0,0,0,0,false);
      // Production ChaoModel hides its declared head decorations when wearing
      // a hat. Keep that same behavior in this static attachment inspection.
      if(!hat.isBlank())rig.hideHeadDecorations();
      require(rig.finite(),"non-finite static pose "+label);
      hatPart=hat.isBlank()?null:new StaticPart("models/entity/hats/chao_baby.json","textures/entity/chao/market_accessories.png");
      bowPart=!bow?null:new StaticPart("models/entity/cwe_accessories/bowtie_accessory.json","textures/entity/chao/cwe_source02516.png");
      texture=ResourceLocation.fromNamespaceAndPath("chao_garden","blocky_style_preview/"+index);
      try(var input=Files.newInputStream(textureFile)) {
        var dynamic=new DynamicTexture(NativeImage.read(input));dynamic.setFilter(false,false);
        Minecraft.getInstance().getTextureManager().register(texture,dynamic);
      }
    }

    void renderAccessories(com.mojang.blaze3d.vertex.PoseStack stack,net.minecraft.client.renderer.MultiBufferSource buffers) {
      if(hatPart!=null) {
        // Same copied hierarchy, selected-root visibility and legacyHead fit
        // as ChaoHeadwear.render; only its ChaoModel wrapper is unnecessary.
        hatPart.poses.values().forEach(ChaoMeshAsset.Pose::reset);
        for(var entry:hatPart.poses.entrySet()) {
          if(rig.poses().containsKey(entry.getKey()))rig.copyAttachmentPoseTo(entry.getKey(),hatPart.geometry,entry.getValue());
          else entry.getValue().visible=entry.getKey().equals(hat);
        }
        var selected=hatPart.poses.get(hat);require(selected!=null,"actual hat root missing");
        ChaoAccessoryFit.legacyHead(rig.attachmentBounds("skull"),"baby").apply(selected);
        hatPart.geometry.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(hatPart.texture)),
            LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0xffffffff,hatPart.poses);
      }
      if(bowPart!=null) {
        // Same body transform, fit, parts reset and source texture as the
        // production CweAccessoryLayer draw path for bowtie_accessory.
        stack.pushPose();rig.transformBone("body",stack);
        var fit=ChaoAccessoryFit.accessory("bowtie_accessory",rig.attachmentBounds("skull"),rig.attachmentEyes(),
            rig.attachmentBounds("body"),rig.attachmentBounds("body"));
        ChaoAccessoryFit.parts("bowtie_accessory",rig.attachmentBounds("skull"),fit,bowPart.poses);fit.apply(stack);
        bowPart.geometry.render(stack,buffers.getBuffer(RenderType.entityCutoutNoCull(bowPart.texture)),
            LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0xffffffff,bowPart.poses);stack.popPose();
      }
    }
  }

  private static final class PreviewScreen extends Screen {
    private final Spec[] specs;
    PreviewScreen()throws Exception {
      super(Component.literal(setting("title","Blocky baby style proposal")));
      var mc=Minecraft.getInstance();
      var id=ResourceLocation.fromNamespaceAndPath("chao_garden","animations/official/chao.animation.json");
      JsonObject clips;
      try(var reader=mc.getResourceManager().openAsReader(id)){clips=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");}
      if(configuration.has("variants")) {
        var variants=configuration.getAsJsonArray("variants");
        require(variants.size()>=1&&variants.size()<=4,"static sheet needs one to four baby variants");
        specs=new Spec[variants.size()];
        for(int i=0;i<specs.length;i++) {
          var variant=variants.get(i).getAsJsonObject();String variantId=variant.has("id")?variant.get("id").getAsString():"variant-"+i;
          require(variantId.matches("[a-z0-9_-]+"),"invalid static variant id");
          String hat=variant.has("hat")?variant.get("hat").getAsString():"";boolean bow=variant.has("bow")&&variant.get("bow").getAsBoolean();
          specs[i]=new Spec(variantId,variant.get("label").getAsString(),input(variant.get("model").getAsString()),input(variant.get("texture").getAsString()),i,clips,hat,bow);
        }
      } else specs=new Spec[]{
          new Spec("original-neutral","Original 0.25.51",input("original.json"),input("original-neutral.png"),0,clips,"",false),
          new Spec("normal-hero","Normal Hero (same cubes)",input("original.json"),input("normal-hero.png"),1,clips,"",false),
          new Spec("proposed-hero-run","Proposed max Run Hero",input("proposal.json"),input("proposal-hero-run.png"),2,clips,"",false)};
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
      g.fill(0,0,width,height,0xffd8e3ec);
      centered(g,title.getString(),width/2,23,0xff173449);
      centered(g,setting("subtitle","Static models rendered in Minecraft - original construction and existing pixel face"),width/2,43,0xff35546b);
      centered(g,"View: "+VIEWS[Math.min(phase,VIEWS.length-1)],width/2,65,0xff173449);
      int margin=20,gap=18,cell=(width-margin*2-gap*(specs.length-1))/specs.length;
      float baseline=height-105;
      float scale=Math.min(cell*1.02f,(height-195)/1.2f);
      for(int i=0;i<specs.length;i++) {
        int left=margin+i*(cell+gap),right=left+cell;
        g.fill(left,93,right,height-64,0xff65798a);
        g.fill(left+12,(int)baseline,right-12,(int)baseline+1,0xffa7bac9);
        centered(g,specs[i].label,(left+right)/2,108,0xffeff7ff);
        g.flush();
        RenderSystem.enableDepthTest();Lighting.setupForEntityInInventory();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        g.pose().pushPose();
        g.pose().translate((left+right)/2f,baseline,250);
        g.pose().scale(scale,scale,scale);
        g.pose().translate(0,-1.5f,0);
        g.pose().mulPose(Axis.YP.rotationDegrees(YAW[Math.min(phase,YAW.length-1)]));
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        specs[i].rig.render(g.pose(),buffers.getBuffer(RenderType.entityCutoutNoCull(specs[i].texture)),
            LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,0xffffffff);
        specs[i].renderAccessories(g.pose(),buffers);
        buffers.endBatch();g.pose().popPose();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        Lighting.setupFor3DItems();
        g.flush();
      }
      centered(g,setting("footer","Static late-baby key endpoints only: look draft, not animation, gameplay or save verification."),width/2,height-42,0xff35546b);
      centered(g,setting("scope_note","Alignment and Run are separate inputs. No intermediate stages or adults are authored in this review."),width/2,height-24,0xff35546b);
    }
    private void centered(GuiGraphics g,String text,int x,int y,int color){g.drawString(Minecraft.getInstance().font,text,x-Minecraft.getInstance().font.width(text)/2,y,color,false);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean shouldCloseOnEsc(){return false;}
  }

  @SubscribeEvent public static void renderComplete(ScreenEvent.Render.Post event) {
    if(enabled()&&started&&phase<VIEWS.length&&event.getScreen()==screen)FRAMES[phase]++;
  }

  @SubscribeEvent public static void client(ClientTickEvent.Post event)throws Exception {
    if(!enabled()||finished)return;
    var mc=Minecraft.getInstance();require(++ticks<1200,"static approval capture timed out");
    if(mc.screen instanceof AccessibilityOnboardingScreen)mc.setScreen(new TitleScreen());
    if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
      directory=Path.of(System.getenv().getOrDefault("CHAO_BLOCKY_PREVIEW_DIR","/workspace/.setup/blocky-baby-style")).toAbsolutePath().normalize();
      Files.createDirectories(directory);Path manifest=directory.resolve("preview.json");
      if(Files.isRegularFile(manifest))configuration=JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
      configureViews();
      outputPrefix=setting("output_prefix","blocky-baby-style");require(outputPrefix.matches("[a-z0-9][a-z0-9._-]*"),"invalid screenshot prefix");
      int windowWidth=integerSetting("window_width",1440),windowHeight=integerSetting("window_height",900);
      require(windowWidth>=960&&windowWidth<=2560&&windowHeight>=600&&windowHeight<=1440,"invalid static preview dimensions");
      mc.options.guiScale().set(1);mc.options.framerateLimit().set(30);
      mc.options.pauseOnLostFocus=false;mc.getWindow().setWindowed(windowWidth,windowHeight);mc.resizeDisplay();
      screen=new PreviewScreen();mc.setScreen(screen);started=true;
      JsonObject proof=new JsonObject();proof.addProperty("kind","Actual Minecraft GUI static model rendering; not a live entity/animation pass");
      proof.addProperty("production_morphology_selected",false);proof.addProperty("world_created",false);
      proof.addProperty("attachment_scope","Optional actual accessory meshes with shared original-rig transforms and production fitting recipe; static placement only, not live equipment or save verification");
      proof.addProperty("columns",screen.specs.length);proof.addProperty("output_prefix",outputPrefix);
      var views=new com.google.gson.JsonArray();
      for(int i=0;i<VIEWS.length;i++){var view=new JsonObject();view.addProperty("id",VIEWS[i]);view.addProperty("yaw",YAW[i]);views.add(view);}proof.add("views",views);
      if(Files.isRegularFile(manifest))proof.addProperty("manifest_sha256",sha(manifest));proof.add("configuration",configuration.deepCopy());
      var models=new com.google.gson.JsonArray();
      for(var s:screen.specs){var e=new JsonObject();e.addProperty("id",s.id);e.addProperty("label",s.label);e.addProperty("model",s.modelFile);e.addProperty("texture",s.textureFile);e.addProperty("model_sha256",s.modelHash);e.addProperty("texture_sha256",s.textureHash);
        e.addProperty("hat",s.hat);e.addProperty("bow",s.bow);if(s.hatPart!=null)e.add("hat_resources",s.hatPart.provenance());if(s.bowPart!=null)e.add("bow_resources",s.bowPart.provenance());models.add(e);}
      proof.add("models",models);Files.writeString(directory.resolve("minecraft-preview-inputs.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(proof)+"\n");
      LogUtils.getLogger().info("BLOCKY_BABY_STYLE_STARTED width={} height={} columns={} views={} world=false productionMorphology=false",mc.getWindow().getWidth(),mc.getWindow().getHeight(),screen.specs.length,VIEWS.length);
    }
    if(!started||mc.getOverlay()!=null)return;
    if(phase<VIEWS.length&&FRAMES[phase]>=24) {
      int index=phase;String name=outputPrefix+"-"+VIEWS[index]+".png";
      Screenshot.grab(directory.toFile(),name,mc.getMainRenderTarget(),message->{
        var path=directory.resolve("screenshots").resolve(name);require(Files.isRegularFile(path),"screenshot missing");
        try{LogUtils.getLogger().info("BLOCKY_BABY_STYLE_CAPTURE view={} path={} sha256={} frames={} yaw={}",VIEWS[index],path,sha(path),FRAMES[index],YAW[index]);}
        catch(Exception e){throw new IllegalStateException(e);}SAVED.incrementAndGet();});phase++;
    }
    if(phase==VIEWS.length&&SAVED.get()==VIEWS.length){finished=true;LogUtils.getLogger().info("BLOCKY_BABY_STYLE_COMPLETE views={} saved={} staticOnly=true world=false",VIEWS.length,SAVED.get());mc.stop();}
  }
}
