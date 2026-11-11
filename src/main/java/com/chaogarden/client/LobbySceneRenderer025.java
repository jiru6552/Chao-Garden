package com.chaogarden.client;

import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.PortalScenes0251;
import net.minecraft.core.BlockPos;
import com.chaogarden.core.GardenGateGeometry0256;
import com.chaogarden.core.PortalGeometry0251;
import com.chaogarden.core.LobbyGeometry0252;
import com.chaogarden.core.LobbyApproachGeometry0255;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** The actual transparent lobby surface, underside and authored architectural meshes. */
@EventBusSubscriber(modid="chao_garden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class LobbySceneRenderer025 implements BlockEntityRenderer<LobbyDecor025.SceneEntity> {
  private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath("chao_garden","textures/scene/"+name+".png");}
  private static final ResourceLocation WHITE=texture("white"),SKY_TEX=texture("lobby_celestial_sky"),STAR_TEX=texture("lobby_star_panel"),WOOD_TEX=texture("lobby_wood"),LEAF_TEX=texture("garden_palm_frond"),SIGN_TEX=texture("kindergarten_portal_sign"),PAD_TEX=texture("lobby_exit_pad"),EXIT_TEX=texture("lobby_exit_sign");
  private static final ResourceLocation QUARTZ=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/quartz_block_side.png"),STONE=ResourceLocation.fromNamespaceAndPath("minecraft","textures/block/stone_bricks.png");
  private static ShaderInstance shader;
  private static final Mesh SKY=sky(),STARS=panels(false),REFLECTED_STARS=panels(true),WOOD=wood(false),REFLECTED_WOOD=wood(true),FLOOR=floor(),GRID=grid(),ARCHITECTURE=architecture(),GARDEN_STONES=gardenStones(),GARDEN_QUARTZ=gardenQuartz(),GARDEN_LEAVES=gardenLeaves(),SIGN=sign(),PALM=palm(),GLAZING=glazing(),PAD=pad(.04,2.68,96),UPPER_PAD=pad(5.30,3.34,32),EXIT=exitSign(),APPROACH_GLASS=approachGlass(),APPROACH_STARS=approachStars(),APPROACH_TRIM=approachTrim(),APPROACH_GATES=approachGates(false),APPROACH_GATE_MASONRY=approachGates(true);
  private static final List<Vector3f> PALM_NORMALS = palmNormals();
  private static final Mesh[] PORTAL_FRAMES={portalFrame(0),portalFrame(1),portalFrame(2),portalFrame(3),portalFrame(4),portalFrame(5)};
  private static final List<List<PortalGeometry0251.Point>> PORTAL_OPENINGS=List.of(PortalGeometry0251.aperture(0),PortalGeometry0251.aperture(1),PortalGeometry0251.aperture(2),PortalGeometry0251.aperture(3),PortalGeometry0251.aperture(4),PortalGeometry0251.aperture(5));
  // lobby_glass.png is a uniform texel (21,37,68,59). Vertex tint lets all
  // clear scene surfaces share a single back-to-front sorted material.
  private static final int GLASS_COLOR=0x3b152544,GOLD=0xffffce49,CREAM=0xfff3e9ca,CYAN=0xff68e1df;
  private record AlphaScene(Matrix4f pose,Matrix3f normal,float phase){
    PoseStack stack(){
      var stack=new PoseStack();
      stack.last().pose().set(pose);stack.last().normal().set(normal);
      return stack;
    }
  }
  private static final List<AlphaScene> ALPHA_SCENES=new ArrayList<>();
  private static final class SceneBuffers {
    // Iris intentionally ignores explicit flushes on its shared entity source.
    // Own this immediate source so scene order, late submissions and temporary
    // background fog state take effect at the actual draw, including fallbacks.
    private static final MultiBufferSource.BufferSource SOURCE=MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(262144));
  }
  private static boolean mainSceneSubmitted, shaderPack;
  private static final java.util.Set<BlockPos> renderedPortals = new java.util.HashSet<>();
  private static final List<BlockPos> HUB_ANCHORS=List.of(new BlockPos(0,78,0),PortalScenes0251.HUB_SCHOOL,
      PortalScenes0251.HUB_GARDEN,HubRooms.HERO_DOOR,HubRooms.DARK_DOOR);
  public LobbySceneRenderer025(BlockEntityRendererProvider.Context context){}
  @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(LobbyDecor025.SCENE.get(),LobbySceneRenderer025::new);}
  @SubscribeEvent public static void registerShader(RegisterShadersEvent event)throws IOException{
    Material.clear();ALPHA_SCENES.clear();event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.fromNamespaceAndPath("chao_garden","scene_unlit"),DefaultVertexFormat.POSITION_TEX_COLOR),loaded->shader=loaded);
  }
  private static List<Vector3f> palmNormals() {
    var normals = new ArrayList<Vector3f>();
    for (int i = 0; i < PALM.vertices.size(); i += 4) {
      var a = PALM.vertices.get(i);
      var b = PALM.vertices.get(i + 1);
      var d = PALM.vertices.get(i + 3);
      normals.add(new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z)
          .cross(d.x - a.x, d.y - a.y, d.z - a.z).normalize());
    }
    return List.copyOf(normals);
  }

  public static void renderPalm(PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
    poses.translate(.5, .05, .5);
    var pose = poses.last();
    var out = buffers.getBuffer(RenderType.entityCutoutNoCull(LEAF_TEX));
    for (int face = 0; face < PALM_NORMALS.size(); face++) {
      var normal = PALM_NORMALS.get(face);
      for (int corner = 0; corner < 4; corner++) {
        var v = PALM.vertices.get(face * 4 + corner);
        out.addVertex(pose, v.x, v.y, v.z).setColor(v.color).setUv(v.u, v.v)
            .setOverlay(overlay).setLight(light).setNormal(pose, normal.x, normal.y, normal.z);
      }
    }
  }
  @Override public boolean shouldRenderOffScreen(LobbyDecor025.SceneEntity entity){return true;}
  @Override public int getViewDistance(){return 192;}
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(LobbyDecor025.SceneEntity entity){return entity.sceneBounds();}
  @Override public void render(LobbyDecor025.SceneEntity entity,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
    poses.pushPose();
    boolean ownsBuffers=false;
    try{
      if(entity.getBlockState().is(LobbyDecor025.PALM_CROWN.get())){
        renderPalm(poses,buffers,light,overlay);return;
      }
      // The lobby sky, glass, projected reflections and portal energy are visual scenery.
      // A shadow-map submission must never consume the visible pass or its queued transforms.
      if(shaderPack && SceneShaderPack.shadows())return;
      buffers=SceneBuffers.SOURCE;ownsBuffers=true;
      if(entity.getBlockState().getValue(LobbyDecor025.SceneAnchor.PORTAL)){
        if(renderedPortals.add(entity.getBlockPos()))portal(entity,poses,buffers);return;
      }
      // Native global BER registration happens only after its owning section
      // compiles. The late fallback below shares this guard, so either route
      // submits the complete scene exactly once per world frame.
      if(mainSceneSubmitted)return;
      mainSceneSubmitted=true;
      poses.translate(.5,2.002,.5);
      APPROACH_TRIM.draw(poses,buffers,WHITE,1);APPROACH_GATES.draw(poses,buffers,WHITE,1);
      APPROACH_GATE_MASONRY.draw(poses,buffers,STONE,1);
      GRID.draw(poses,buffers,WHITE,1);STARS.draw(poses,buffers,STAR_TEX,1);WOOD.draw(poses,buffers,WOOD_TEX,1);ARCHITECTURE.draw(poses,buffers,WHITE,1);
      GARDEN_STONES.draw(poses,buffers,STONE,1);GARDEN_QUARTZ.draw(poses,buffers,QUARTZ,1);GARDEN_LEAVES.draw(poses,buffers,WHITE,1);
      // Both physical energy faces establish depth before transparent tubes. A
      // ribbon behind a disc must fail the depth test at every camera angle.
      PAD.draw(poses,buffers,PAD_TEX,1);UPPER_PAD.draw(poses,buffers,PAD_TEX,1);
      flush(buffers,Material.get(WHITE,1));flush(buffers,Material.get(STAR_TEX,1));flush(buffers,Material.get(WOOD_TEX,1));
      flush(buffers,Material.get(STONE,1));flush(buffers,Material.get(QUARTZ,1));flush(buffers,Material.get(PAD_TEX,1));
      darkFire(poses,buffers,entity.getLevel()==null?partial:entity.getLevel().getGameTime()+partial);
      SIGN.draw(poses,buffers,SIGN_TEX,0);flush(buffers,Material.get(SIGN_TEX,0));EXIT.draw(poses,buffers,EXIT_TEX,2);flush(buffers,Material.get(EXIT_TEX,2));
      // End-portal block entities may render after this anchor. Their opaque
      // shader would replace these color-only tubes even when farther away.
      // Preserve this frame's camera-relative transform for the final alpha pass.
      long time=entity.getLevel()==null?0:entity.getLevel().getGameTime();
      ALPHA_SCENES.add(new AlphaScene(new Matrix4f(poses.last().pose()),new Matrix3f(poses.last().normal()),(time+partial)*.09f));
    }finally{try{if(ownsBuffers)SceneBuffers.SOURCE.endBatch();}finally{poses.popPose();}}
  }
  @EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
  public static final class TranslucentPass {
    @SubscribeEvent public static void beginFrame(RenderFrameEvent.Pre event){
      ALPHA_SCENES.clear();renderedPortals.clear();mainSceneSubmitted=false;shaderPack=SceneShaderPack.active();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
      if(shaderPack && SceneShaderPack.shadows())return;
      if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES){submitUnregisteredLobby(event);return;}
      if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||ALPHA_SCENES.isEmpty())return;
      var buffers=SceneBuffers.SOURCE;
      try{
        // Shaderpacks may replace pixels with sky during their deferred pass
        // when depth is empty. Submit the authored sky/reflections afterwards,
        // still without depth writes, before the actual transparent surfaces.
        float fogStart=RenderSystem.getShaderFogStart(),fogEnd=RenderSystem.getShaderFogEnd();
        try{
          // The authored sky is scenery beyond the room, not fogged geometry.
          // Solas's late emissive program leaves it unlit; disable the stock
          // shader's distance fade too, and restore fog before all foregrounds.
          if(shaderPack){RenderSystem.setShaderFogStart(10000);RenderSystem.setShaderFogEnd(10001);}
          for(var scene:ALPHA_SCENES)SKY.draw(scene.stack(),buffers,SKY_TEX,3);
          flush(buffers,Material.get(SKY_TEX,3));
          for(var scene:ALPHA_SCENES)REFLECTED_STARS.draw(scene.stack(),buffers,STAR_TEX,3);
          flush(buffers,Material.get(STAR_TEX,3));
          for(var scene:ALPHA_SCENES)REFLECTED_WOOD.draw(scene.stack(),buffers,WOOD_TEX,3);
          flush(buffers,Material.get(WOOD_TEX,3));
        }finally{
          if(shaderPack){RenderSystem.setShaderFogStart(fogStart);RenderSystem.setShaderFogEnd(fogEnd);}
        }
        // Opaque geometry and portals have established depth. The floor, stair
        // faces, guards, crown, tube walls and ribbons now sort together. Using
        // the emissive/eyes shader here makes shaderpacks treat glass as glow
        // with incompatible depth/composite rules.
        for(var scene:ALPHA_SCENES)APPROACH_STARS.draw(scene.stack(),buffers,STAR_TEX,0);
        flush(buffers,Material.get(STAR_TEX,0));
        for(var scene:ALPHA_SCENES){
          var poses=scene.stack();
          FLOOR.draw(poses,buffers,WHITE,0);APPROACH_GLASS.draw(poses,buffers,WHITE,0);
          GLAZING.draw(poses,buffers,WHITE,0);energy(poses,buffers,scene.phase());
        }
        flush(buffers,Material.get(WHITE,0));
      }finally{try{buffers.endBatch();}finally{ALPHA_SCENES.clear();}}
    }
  }
  private static void submitUnregisteredLobby(RenderLevelStageEvent event){
    var mc=Minecraft.getInstance();if(mc.level==null)return;
    var dim=mc.level.dimension();
    if(dim.equals(HubContent.HUB))for(var pos:HUB_ANCHORS)submitAnchor(event,pos);
    else if(dim.equals(HubContent.KINDERGARTEN))submitAnchor(event,PortalScenes0251.SCHOOL_RETURN);
    else if(dim.equals(HubContent.GARDEN))submitAnchor(event,com.chaogarden.hub.GardenRooms025.EXIT_PORTAL);
    else if(dim.equals(HubContent.HERO_GARDEN))submitAnchor(event,com.chaogarden.hub.HeroGardenRooms0253.EXIT_PORTAL);
    else if(dim.equals(HubContent.DARK_GARDEN))submitAnchor(event,com.chaogarden.hub.DarkGardenRooms0255.EXIT_PORTAL);
  }

  /** Some entirely invisible chunk sections do not enqueue their offscreen block entity. */
  private static void submitAnchor(RenderLevelStageEvent event,BlockPos pos){
    var mc=Minecraft.getInstance();
    if(renderedPortals.contains(pos) || !mc.level.hasChunkAt(pos)
        || !(mc.level.getBlockEntity(pos) instanceof LobbyDecor025.SceneEntity anchor)
        || !anchor.getBlockState().is(LobbyDecor025.SCENE_ANCHOR.get()))return;
    if(!anchor.getBlockState().getValue(LobbyDecor025.SceneAnchor.PORTAL) && mainSceneSubmitted)return;
    var camera=event.getCamera().getPosition();
    if(!event.getFrustum().isVisible(anchor.sceneBounds()) || pos.distToCenterSqr(camera)>192*192)return;
    var poses=event.getPoseStack();poses.pushPose();
    try{
      poses.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
      mc.getBlockEntityRenderDispatcher().render(anchor,event.getPartialTick().getGameTimeDeltaPartialTick(false),poses,mc.renderBuffers().bufferSource());
    }finally{poses.popPose();}
  }
  private static Mesh portalFrame(int style){var mesh=new Mesh();if(!PortalGeometry0251.hasFrame(style))return mesh;for(var q:PortalGeometry0251.frame(style))mesh.quad(p(q.a().x(),q.a().y(),q.a().z()),p(q.b().x(),q.b().y(),q.b().z()),p(q.c().x(),q.c().y(),q.c().z()),p(q.d().x(),q.d().y(),q.d().z()),q.color());double half=PortalGeometry0251.width(style)/2;mesh.box(-half-.28,.002,-.27,half+.28,.08,.27,0xfffaf4e9);return mesh;}
  private static void portal(LobbyDecor025.SceneEntity entity,PoseStack poses,MultiBufferSource buffers){
    var state=entity.getBlockState();int style=state.getValue(LobbyDecor025.SceneAnchor.STYLE);
    poses.translate(.5,0,.5);poses.mulPose(Axis.YP.rotationDegrees(switch(state.getValue(LobbyDecor025.SceneAnchor.FACING)){case NORTH->180;case EAST->90;case WEST->-90;default->0;}));
    PORTAL_FRAMES[style].draw(poses,buffers,QUARTZ,1);flush(buffers,Material.get(QUARTZ,1));
    if(shaderPack){
      var type=Material.energy(net.minecraft.client.renderer.blockentity.TheEndPortalRenderer.END_PORTAL_LOCATION);
      var out=buffers.getBuffer(type);var outline=PORTAL_OPENINGS.get(style);
      float centre=(float)PortalGeometry0251.height(style)/2;
      double time=entity.getLevel().getGameTime()+Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      float phase=(float)((time%2000)/2000);
      for(int i=0;i<outline.size();i++){
        var a=outline.get(i);var b=outline.get((i+1)%outline.size());
        packedPortalVertex(out,poses.last(),0,centre,phase);
        packedPortalVertex(out,poses.last(),(float)a.x(),(float)a.y(),phase);
        packedPortalVertex(out,poses.last(),(float)b.x(),(float)b.y(),phase);
        packedPortalVertex(out,poses.last(),0,centre,phase);
      }
      flush(buffers,type);return;
    }
    // Vanilla's End portal shader projects moving star layers in screen space.
    // Position-only vertices and opposite windings render both sides of a vertical
    // aperture; no black concrete or textured square sits behind this surface.
    var v=buffers.getBuffer(RenderType.endPortal());var matrix=poses.last().pose();var outline=PORTAL_OPENINGS.get(style);float cy=(float)(PortalGeometry0251.height(style)/2);
    for(int i=0;i<outline.size();i++){var a=outline.get(i);var b=outline.get((i+1)%outline.size());
      v.addVertex(matrix,0,cy,.001f);v.addVertex(matrix,(float)a.x(),(float)a.y(),.001f);v.addVertex(matrix,(float)b.x(),(float)b.y(),.001f);v.addVertex(matrix,0,cy,.001f);
      v.addVertex(matrix,0,cy,-.001f);v.addVertex(matrix,(float)b.x(),(float)b.y(),-.001f);v.addVertex(matrix,(float)a.x(),(float)a.y(),-.001f);v.addVertex(matrix,0,cy,-.001f);
    }
    flush(buffers,RenderType.endPortal());
  }
  private static void packedPortalVertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float phase){
    out.addVertex(pose,x,y,0).setColor(0xff80c4ed).setUv(x*.23f+phase,y*.23f-phase)
        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose,0,0,1);
  }
  private static void flush(MultiBufferSource buffers,RenderType type){if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch(type);}
  private static final class Material extends RenderType {
    private Material(String n,VertexFormat f,VertexFormat.Mode m,int s,boolean c,boolean sort,Runnable a,Runnable b){super(n,f,m,s,c,sort,a,b);}
    private static final Map<String,RenderType> CACHE=new java.util.HashMap<>();
    static void clear(){CACHE.clear();}
    static RenderType energy(ResourceLocation texture){
      return CACHE.computeIfAbsent(texture+":energy",key->create("chao_scene_energy",
          DefaultVertexFormat.BLOCK,VertexFormat.Mode.QUADS,4096,false,false,
          CompositeState.builder().setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
              .setTextureState(new TextureStateShard(texture,false,false)).setCullState(NO_CULL)
              .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false)));
    }
    static RenderType get(ResourceLocation texture,int mode){
      if(shaderPack && texture.equals(PAD_TEX))return energy(texture);
      // Emissive/color-only is appropriate for the distant authored background,
      // never for glass that must supply foreground depth to the shaderpack.
      if(shaderPack && mode==3)return RenderType.entityTranslucentEmissive(texture,false);
      if(shaderPack && mode==0)return CACHE.computeIfAbsent(texture+":translucent",key->create("chao_scene_translucent",
          DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,262144,false,true,
          CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
              .setTextureState(new TextureStateShard(texture,false,false))
              .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
              .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
              .setDepthTestState(LEQUAL_DEPTH_TEST)
              // Standard shader translucency needs foreground depth for fog,
              // reflections and compositing. Background sky remains color-only.
              .setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false)));
      if(shaderPack)return switch(mode){
        case 2 -> RenderType.entityCutout(texture);
        default -> RenderType.entityCutoutNoCull(texture);
      };
      return CACHE.computeIfAbsent(texture+":"+mode,key->create("chao_scene_"+mode,DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,262144,false,mode!=1,
        CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTextureState(new TextureStateShard(texture,false,false))
        .setTransparencyState(mode!=1?TRANSLUCENT_TRANSPARENCY:NO_TRANSPARENCY).setCullState(mode==2?CULL:NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
        .setWriteMaskState(mode==0||mode==3?COLOR_WRITE:COLOR_DEPTH_WRITE).createCompositeState(false)));}
  }
  private record V(float x,float y,float z,float u,float v,int color){}
  private static final class Mesh {
    final List<V> vertices=new ArrayList<>();
    void vertex(double x,double y,double z,float u,float v,int color){vertices.add(new V((float)x,(float)y,(float)z,u,v,color));}
    void quad(double[] a,double[] b,double[] c,double[] d,int color){quad(a,b,c,d,color,0,0,1,1);}
    void quad(double[] a,double[] b,double[] c,double[] d,int color,float u0,float v0,float u1,float v1){vertex(a[0],a[1],a[2],u0,v0,color);vertex(b[0],b[1],b[2],u1,v0,color);vertex(c[0],c[1],c[2],u1,v1,color);vertex(d[0],d[1],d[2],u0,v1,color);}
    void floorQuad(double[] a,double[] b,double[] c,double[] d,int color){
      var polygon=List.of(new LobbyApproachGeometry0255.Point(a[0],a[1],a[2]),new LobbyApproachGeometry0255.Point(b[0],b[1],b[2]),
          new LobbyApproachGeometry0255.Point(c[0],c[1],c[2]),new LobbyApproachGeometry0255.Point(d[0],d[1],d[2]));
      for(var piece:LobbyApproachGeometry0255.floorPieces(polygon))for(int i=1;i+1<piece.size();i++){
        var x=piece.getFirst();var y=piece.get(i);var z=piece.get(i+1);
        quad(p(x.x(),x.y(),x.z()),p(y.x(),y.y(),y.z()),p(z.x(),z.y(),z.z()),p(x.x(),x.y(),x.z()),color);
      }
    }
    void floorRing(double radius,double width,double y,int color){for(int i=0;i<192;i++){
      double a=i*Math.PI/96,b=(i+1)*Math.PI/96;
      floorQuad(p(Math.cos(a)*(radius-width/2),y,Math.sin(a)*(radius-width/2)),p(Math.cos(b)*(radius-width/2),y,Math.sin(b)*(radius-width/2)),
          p(Math.cos(b)*(radius+width/2),y,Math.sin(b)*(radius+width/2)),p(Math.cos(a)*(radius+width/2),y,Math.sin(a)*(radius+width/2)),color);
    }}
    private Vector3f[] normals;
    void draw(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,int mode){
      var consumer=buffers.getBuffer(Material.get(texture,mode));var transform=pose.last();
      if(!shaderPack){
        for(var v:vertices)emit(consumer,transform.pose(),v.x,v.y,v.z,v.u,v.v,v.color);
        return;
      }
      if(normals==null){
        normals=new Vector3f[vertices.size()/4];
        for(int i=0;i<normals.length;i++){
          var a=vertices.get(i*4);var b=vertices.get(i*4+1);var c=vertices.get(i*4+2);var d=vertices.get(i*4+3);
          // Newell's normal also handles the repeated corner of triangle fans.
          var n=new Vector3f(b.x-d.x,b.y-d.y,b.z-d.z).cross(c.x-a.x,c.y-a.y,c.z-a.z);
          normals[i]=n.lengthSquared()>1e-16f?n.normalize().negate():n.set(0,1,0);
        }
      }
      for(int i=0;i<vertices.size();i++){
        var v=vertices.get(i);var n=normals[i/4];
        consumer.addVertex(transform,v.x,v.y,v.z).setColor(v.color).setUv(v.u,v.v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(transform,n.x,n.y,n.z);
      }
    }
    void box(double x0,double y0,double z0,double x1,double y1,double z1,int color){
      quad(p(x0,y1,z0),p(x1,y1,z0),p(x1,y1,z1),p(x0,y1,z1),color);
      quad(p(x0,y0,z1),p(x1,y0,z1),p(x1,y0,z0),p(x0,y0,z0),shade(color,.55));
      quad(p(x0,y0,z0),p(x1,y0,z0),p(x1,y1,z0),p(x0,y1,z0),shade(color,.8));
      quad(p(x1,y0,z1),p(x0,y0,z1),p(x0,y1,z1),p(x1,y1,z1),shade(color,.88));
      quad(p(x0,y0,z1),p(x0,y0,z0),p(x0,y1,z0),p(x0,y1,z1),shade(color,.69));
      quad(p(x1,y0,z0),p(x1,y0,z1),p(x1,y1,z1),p(x1,y1,z0),shade(color,.94));
    }
    /** Closed prism, including both caps and every underside/reveal. */
    void prism(double[][] outline,double z0,double z1,int color){
      var first=outline[0];for(int i=1;i+1<outline.length;i++){
        var b=outline[i];var c=outline[i+1];quad(p(first[0],first[1],z0),p(b[0],b[1],z0),p(c[0],c[1],z0),p(first[0],first[1],z0),color);
        quad(p(first[0],first[1],z1),p(c[0],c[1],z1),p(b[0],b[1],z1),p(first[0],first[1],z1),shade(color,.9));
      }
      for(int i=0;i<outline.length;i++){var a=outline[i];var b=outline[(i+1)%outline.length];quad(p(a[0],a[1],z0),p(a[0],a[1],z1),p(b[0],b[1],z1),p(b[0],b[1],z0),shade(color,b[0]>a[0]?.7:.88));}
    }
    void geometry(List<LobbyGeometry0252.Face> faces,int color){for(var q:faces)quad(p(q.a().x(),q.a().y(),q.a().z()),p(q.b().x(),q.b().y(),q.b().z()),p(q.c().x(),q.c().y(),q.c().z()),p(q.d().x(),q.d().y(),q.d().z()),shade(color,q.shade()));}
    void stem(LobbyGeometry0252.Stem stem,double radius,int color){
      var a=stem.root();var b=stem.bloom();double dx=b.x()-a.x(),dy=b.y()-a.y(),dz=b.z()-a.z(),length=Math.sqrt(dx*dx+dy*dy+dz*dz);
      double ux=1,uy=-dx/dy,uz=0,ul=Math.sqrt(ux*ux+uy*uy);ux/=ul;uy/=ul;
      double vx=-dz*uy/length,vy=dz*ux/length,vz=(dx*uy-dy*ux)/length;
      for(int i=0;i<6;i++){double u=i*Math.PI/3,v=(i+1)*Math.PI/3;
        double ox=(ux*Math.cos(u)+vx*Math.sin(u))*radius,oy=(uy*Math.cos(u)+vy*Math.sin(u))*radius,oz=(uz*Math.cos(u)+vz*Math.sin(u))*radius;
        double px=(ux*Math.cos(v)+vx*Math.sin(v))*radius,py=(uy*Math.cos(v)+vy*Math.sin(v))*radius,pz=(uz*Math.cos(v)+vz*Math.sin(v))*radius;
        quad(p(a.x()+ox,a.y()+oy,a.z()+oz),p(a.x()+px,a.y()+py,a.z()+pz),p(b.x()+px,b.y()+py,b.z()+pz),p(b.x()+ox,b.y()+oy,b.z()+oz),shade(color,.82+.13*Math.cos(u)));
      }
    }
    void ellipsoid(double x,double y,double z,double rx,double ry,double rz,int color){
      for(int j=0;j<8;j++)for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8,c=j*Math.PI/8,d=(j+1)*Math.PI/8;
        quad(p(x+rx*Math.sin(c)*Math.cos(a),y+ry*Math.cos(c),z+rz*Math.sin(c)*Math.sin(a)),p(x+rx*Math.sin(c)*Math.cos(b),y+ry*Math.cos(c),z+rz*Math.sin(c)*Math.sin(b)),p(x+rx*Math.sin(d)*Math.cos(b),y+ry*Math.cos(d),z+rz*Math.sin(d)*Math.sin(b)),p(x+rx*Math.sin(d)*Math.cos(a),y+ry*Math.cos(d),z+rz*Math.sin(d)*Math.sin(a)),shade(color,.8+.17*Math.cos(c)));
      }
    }
    void cylinder(double x,double y,double z,double radius,double height,int sides,int color){cylinder(x,y,z,radius,height,sides,color,true);}
    void cylinder(double x,double y,double z,double radius,double height,int sides,int color,boolean bottom){
      for(int i=0;i<sides;i++){double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;double x0=x+Math.cos(a)*radius,z0=z+Math.sin(a)*radius,x1=x+Math.cos(b)*radius,z1=z+Math.sin(b)*radius;
        quad(p(x0,y,z0),p(x1,y,z1),p(x1,y+height,z1),p(x0,y+height,z0),shade(color,.72+.18*Math.cos(a-.7)));
        quad(p(x,y+height,z),p(x0,y+height,z0),p(x1,y+height,z1),p(x,y+height,z),color);
        if(bottom)quad(p(x,y,z),p(x1,y,z1),p(x0,y,z0),p(x,y,z),shade(color,.65));}
    }
    /** Short glass quads keep center-distance sorting accurate at steep view angles. */
    void tubeWalls(double x,double y,double z,double radius,double height,int sides,int strips,int color){
      for(int row=0;row<strips;row++)for(int i=0;i<sides;i++){
        double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides,y0=y+height*row/strips,y1=y+height*(row+1)/strips;
        quad(p(x+Math.cos(a)*radius,y0,z+Math.sin(a)*radius),p(x+Math.cos(b)*radius,y0,z+Math.sin(b)*radius),
            p(x+Math.cos(b)*radius,y1,z+Math.sin(b)*radius),p(x+Math.cos(a)*radius,y1,z+Math.sin(a)*radius),shade(color,.72+.18*Math.cos(a-.7)));
      }
    }
    void band(double x,double y,double z,double outer,double inner,double height,int sides,int color){
      for(int i=0;i<sides;i++){double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;
        double[] lo=p(x+Math.cos(a)*outer,y,z+Math.sin(a)*outer),ro=p(x+Math.cos(b)*outer,y,z+Math.sin(b)*outer),li=p(x+Math.cos(a)*inner,y,z+Math.sin(a)*inner),ri=p(x+Math.cos(b)*inner,y,z+Math.sin(b)*inner);
        double[] lt=p(lo[0],y+height,lo[2]),rt=p(ro[0],y+height,ro[2]),lit=p(li[0],y+height,li[2]),rit=p(ri[0],y+height,ri[2]);
        quad(lo,ro,rt,lt,shade(color,.76));quad(ri,li,lit,rit,shade(color,.62));quad(lt,rt,rit,lit,color);quad(li,ri,ro,lo,shade(color,.6));
      }
    }
    void ring(double radius,double width,double y,int color){for(int i=0;i<192;i++){double a=i*Math.PI/96,b=(i+1)*Math.PI/96;quad(p(Math.cos(a)*(radius-width/2),y,Math.sin(a)*(radius-width/2)),p(Math.cos(b)*(radius-width/2),y,Math.sin(b)*(radius-width/2)),p(Math.cos(b)*(radius+width/2),y,Math.sin(b)*(radius+width/2)),p(Math.cos(a)*(radius+width/2),y,Math.sin(a)*(radius+width/2)),color);}}
  }
  private static double[] p(double x,double y,double z){return new double[]{x,y,z};}
  private static int shade(int color,double f){return (color&0xff000000)|((int)(((color>>16)&255)*f)<<16)|((int)(((color>>8)&255)*f)<<8)|(int)((color&255)*f);}
  private static void emit(VertexConsumer v,Matrix4f m,float x,float y,float z,float u,float w,int color){v.addVertex(m,x,y,z).setUv(u,w).setColor((color>>16)&255,(color>>8)&255,color&255,(color>>>24)&255);}
  private static Mesh sky(){var m=new Mesh();int longitude=96,latitude=48;double radius=120;
    for(int j=0;j<latitude;j++)for(int i=0;i<longitude;i++){
      double a=i*Math.PI*2/longitude,b=(i+1)*Math.PI*2/longitude,c=j*Math.PI/latitude,d=(j+1)*Math.PI/latitude;
      m.quad(p(Math.sin(c)*Math.cos(a)*radius,Math.cos(c)*radius,Math.sin(c)*Math.sin(a)*radius),p(Math.sin(c)*Math.cos(b)*radius,Math.cos(c)*radius,Math.sin(c)*Math.sin(b)*radius),p(Math.sin(d)*Math.cos(b)*radius,Math.cos(d)*radius,Math.sin(d)*Math.sin(b)*radius),p(Math.sin(d)*Math.cos(a)*radius,Math.cos(d)*radius,Math.sin(d)*Math.sin(a)*radius),0xffffffff,(float)i/longitude,(float)j/latitude,(float)(i+1)/longitude,(float)(j+1)/latitude);
    }return m;
  }
  private static double[] padEdge(int segment){
    // Intersect each glass spoke with the straight edge of the 16-sided gold platform.
    double a=segment*Math.PI/96, local=((segment%12)-6)*Math.PI/96;
    double r=3.8*Math.cos(Math.PI/16)/Math.cos(local);
    return p(r*Math.cos(a),0,r*Math.sin(a));
  }
  private static Mesh floor(){var m=new Mesh();for(int i=0;i<192;i++){double a=i*Math.PI/96,b=(i+1)*Math.PI/96;
    // Short radial spans keep translucent sorting accurate beside the tubes
    // and curved stairs, including views up through the floor.
    m.floorQuad(padEdge(i),p(Math.cos(a)*4,0,Math.sin(a)*4),p(Math.cos(b)*4,0,Math.sin(b)*4),padEdge(i+1),GLASS_COLOR);
    for(int r=4;r<25;r++){
      double outer=Math.min(r+1,24.8);
      m.floorQuad(p(Math.cos(a)*r,0,Math.sin(a)*r),p(Math.cos(a)*outer,0,Math.sin(a)*outer),
          p(Math.cos(b)*outer,0,Math.sin(b)*outer),p(Math.cos(b)*r,0,Math.sin(b)*r),GLASS_COLOR);
    }
  }return m;}
  private static Mesh grid(){var m=new Mesh();for(int r=4;r<=24;r+=4)m.floorRing(r,.026,.007,0xffa2bfdc);
    for(int i=0;i<24;i++){double a=i*Math.PI/12,dx=Math.cos(a),dz=Math.sin(a),nx=-dz*.015,nz=dx*.015;m.floorQuad(p(dx*3.5+nx,.008,dz*3.5+nz),p(dx*24.7+nx,.008,dz*24.7+nz),p(dx*24.7-nx,.008,dz*24.7-nz),p(dx*3.5-nx,.008,dz*3.5-nz),0xff9db7d4);}
    m.floorRing(7,.24,.013,GOLD);m.floorRing(19,.3,.013,GOLD);m.floorRing(24.2,.25,.013,GOLD);return m;}
  private static Mesh approachGlass(){var m=new Mesh();
    for(boolean hero:new boolean[]{true,false}){
      for(var face:LobbyApproachGeometry0255.stairFaces(hero))approachFace(m,face,0,GLASS_COLOR);
      for(var rail:LobbyApproachGeometry0255.rails(hero,false))approachRail(m,rail,false);
    }
    for(var rail:LobbyApproachGeometry0255.rails(false,true))approachRail(m,rail,false);
    return m;
  }
  private static void approachFace(Mesh m,LobbyApproachGeometry0255.Face f,double dy,int color){
    // The shared stair plan has outward CCW winding; scene meshes use the
    // opposite convention. Keep shader normals outward on tops AND undersides.
    m.quad(p(f.a().x(),f.a().y()+dy,f.a().z()),p(f.d().x(),f.d().y()+dy,f.d().z()),p(f.c().x(),f.c().y()+dy,f.c().z()),p(f.b().x(),f.b().y()+dy,f.b().z()),color);
  }
  private static Mesh approachStars(){var m=new Mesh();
    for(boolean hero:new boolean[]{true,false})for(var f:LobbyApproachGeometry0255.stairFaces(hero))
      if(f.a().y()==f.b().y()&&f.a().y()==f.c().y()&&f.a().y()==f.d().y()&&Math.abs(f.a().y()*2-Math.rint(f.a().y()*2))<1e-8){
        // Every thin native tread shares the lobby's star-and-glass material.
        double y=f.a().y()-.018;
        m.quad(p(f.a().x(),y,f.a().z()),p(f.d().x(),y,f.d().z()),p(f.c().x(),y,f.c().z()),p(f.b().x(),y,f.b().z()),0x8f96aed3,
            (float)f.a().x()/2,(float)f.a().z()/2,(float)f.c().x()/2,(float)f.c().z()/2);
      }
    return m;
  }
  private static void approachRail(Mesh m,LobbyApproachGeometry0255.Rail rail,boolean cap){
    var a=rail.edge().a();var b=rail.edge().b();double dx=b.x()-a.x(),dz=b.z()-a.z(),length=Math.hypot(dx,dz);
    if(length<1e-8)return;double nx=-dz/length*(cap?.075:.06),nz=dx/length*(cap?.075:.06);
    double y0=rail.y0()-80+(cap?1.23:-rail.skirt()),y1=rail.y1()-80+(cap?1.23:-rail.skirt()),height=cap?.06:LobbyApproachGeometry0255.RAIL_HEIGHT+rail.skirt();int color=cap?GOLD:GLASS_COLOR;
    var a0=p(a.x()+nx,y0,a.z()+nz);var a1=p(a.x()-nx,y0,a.z()-nz);var b0=p(b.x()+nx,y1,b.z()+nz);var b1=p(b.x()-nx,y1,b.z()-nz);
    var at0=p(a.x()+nx,y0+height,a.z()+nz);var at1=p(a.x()-nx,y0+height,a.z()-nz);var bt0=p(b.x()+nx,y1+height,b.z()+nz);var bt1=p(b.x()-nx,y1+height,b.z()-nz);
    m.quad(a0,at0,bt0,b0,color);m.quad(b1,bt1,at1,a1,color);m.quad(at0,at1,bt1,bt0,color);m.quad(a1,a0,b0,b1,color);m.quad(a1,at1,at0,a0,color);m.quad(b0,bt0,bt1,b1,color);
    // Rounded shared endpoint caps overlap both strips at every rail turn.
    if(cap){m.cylinder(a.x(),y0,a.z(),.081,height,8,color);m.cylinder(b.x(),y1,b.z(),.081,height,8,color);}
    else {m.cylinder(a.x(),y0,a.z(),.07,height,8,color);m.cylinder(b.x(),y1,b.z(),.07,height,8,color);}
  }
  private static Mesh approachTrim(){var m=new Mesh();
    for(boolean hero:new boolean[]{true,false}){
      for(var rail:LobbyApproachGeometry0255.rails(hero,false))approachRail(m,rail,true);
      for(int i=0;i<LobbyApproachGeometry0255.STEPS;i++){
        double a=LobbyApproachGeometry0255.angle(i),y=LobbyApproachGeometry0255.treadTop(hero,i)-80+.006,sign=hero?1:-1;
        double x0=sign*LobbyApproachGeometry0255.INNER_RADIUS*Math.cos(a),z0=LobbyApproachGeometry0255.sceneZ(hero,LobbyApproachGeometry0255.INNER_RADIUS*Math.sin(a)),x1=sign*LobbyApproachGeometry0255.OUTER_RADIUS*Math.cos(a),z1=LobbyApproachGeometry0255.sceneZ(hero,LobbyApproachGeometry0255.OUTER_RADIUS*Math.sin(a));
        double nx=-Math.sin(a)*.025,nz=LobbyApproachGeometry0255.sceneZ(hero,Math.cos(a)*.025);
        m.quad(p(x0+nx,y,z0+nz),p(x1+nx,y,z1+nz),p(x1-nx,y,z1-nz),p(x0-nx,y,z0-nz),GOLD);
      }
    }
    for(var rail:LobbyApproachGeometry0255.rails(false,true))approachRail(m,rail,true);
    return m;
  }
  /** Minecraft's atlas updates the soul-fire frames; the crossed sheets are transparent fire, not rigid tongues. */
  private static void darkFire(PoseStack poses,MultiBufferSource buffers,double time){
    var atlas=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
    for(int index=0;index<GardenGateGeometry0256.darkFlames().size();index++){
      var centre=GardenGateGeometry0256.darkFlames().get(index);
      double width=.44+.025*Math.sin(time*.31+index),height=.66+.035*Math.sin(time*.23+index*2);
      for(int cross=0;cross<2;cross++){
        var sprite=atlas.apply(ResourceLocation.withDefaultNamespace("block/soul_fire_"+cross));
        double dx=cross==0?width/2:0,dz=cross==0?0:width/2;
        var fire=new Mesh();
        var a=new GardenGateGeometry0256.Point(centre.x()-dx,centre.y(),centre.z()-dz);
        var b=new GardenGateGeometry0256.Point(centre.x()+dx,centre.y(),centre.z()+dz);
        var c=new GardenGateGeometry0256.Point(centre.x()+dx,centre.y()+height,centre.z()+dz);
        var d=new GardenGateGeometry0256.Point(centre.x()-dx,centre.y()+height,centre.z()-dz);
        fire.quad(gatePoint(a,HubRooms.DARK_DOOR,HubRooms.DARK_FACING),gatePoint(b,HubRooms.DARK_DOOR,HubRooms.DARK_FACING),
            gatePoint(c,HubRooms.DARK_DOOR,HubRooms.DARK_FACING),gatePoint(d,HubRooms.DARK_DOOR,HubRooms.DARK_FACING),0xffffffff,
            sprite.getU0(),sprite.getV1(),sprite.getU1(),sprite.getV0());
        fire.draw(poses,buffers,TextureAtlas.LOCATION_BLOCKS,0);
      }
    }
    flush(buffers,Material.get(TextureAtlas.LOCATION_BLOCKS,0));
  }
  private static Mesh approachGates(boolean masonry){var result=new Mesh();
    for(boolean hero:new boolean[]{true,false}){
      var anchor=hero?HubRooms.HERO_DOOR:HubRooms.DARK_DOOR;
      var facing=hero?HubRooms.HERO_FACING:HubRooms.DARK_FACING;
      for(var face:GardenGateGeometry0256.gate(hero)){
        if((face.material()==GardenGateGeometry0256.Material.DARK_BRICK)!=masonry)continue;
        if(masonry){
          double ux=face.b().x()-face.a().x(),uy=face.b().y()-face.a().y(),uz=face.b().z()-face.a().z();
          double vx=face.c().x()-face.a().x(),vy=face.c().y()-face.a().y(),vz=face.c().z()-face.a().z();
          double nx=Math.abs(uy*vz-uz*vy),ny=Math.abs(uz*vx-ux*vz),nz=Math.abs(ux*vy-uy*vx);
          int axis=ny>nx&&ny>nz?1:nx>nz?0:2;
          // World-unit UVs keep every brick course the same scale across the
          // curved crown; one stretched whole tile per narrow slice would stripe it.
          for(var v:List.of(face.a(),face.b(),face.c(),face.d())){
            var point=gatePoint(v,anchor,facing);
            result.vertex(point[0],point[1],point[2],(float)(axis==0?v.z():v.x()),
                (float)(axis==1?v.z():-v.y()),face.color());
          }
        }else result.quad(gatePoint(face.a(),anchor,facing),gatePoint(face.b(),anchor,facing),
            gatePoint(face.c(),anchor,facing),gatePoint(face.d(),anchor,facing),face.color());
      }
    }
    return result;
  }
  private static double[] gatePoint(GardenGateGeometry0256.Point v,net.minecraft.core.BlockPos anchor,net.minecraft.core.Direction facing){
    var world=HubRooms.gateWorldPoint(anchor,facing,v);
    return p(world.x-.5,world.y-80,world.z-.5);
  }
  private static Mesh panels(boolean reflection){var m=new Mesh();for(int i=0;i<96;i++){double a=i*Math.PI/48,b=(i+1)*Math.PI/48;double y0=reflection?-.15:.15,y1=reflection?-4.45:4.45;
    m.quad(p(Math.cos(a)*24.7,y0,Math.sin(a)*24.7),p(Math.cos(b)*24.7,y0,Math.sin(b)*24.7),p(Math.cos(b)*24.7,y1,Math.sin(b)*24.7),p(Math.cos(a)*24.7,y1,Math.sin(a)*24.7),reflection?0x777b8bad:0xffffffff,i/12f,1,(i+1)/12f,0);
    }return m;}
  private static Mesh wood(boolean reflection){var m=new Mesh();double sign=reflection?-1:1;int color=reflection?0x887d6b62:0xffffffff;
    for(int i=0;i<24;i++){double a=i*Math.PI/12,x=Math.rint(Math.cos(a)*24.5),z=Math.rint(Math.sin(a)*24.5);m.box(x-.22,Math.min(sign*.1,sign*4.8),z-.22,x+.22,Math.max(sign*.1,sign*4.8),z+.22,color);}
    for(int i=0;i<192;i++){double a=i*Math.PI/96,b=(i+1)*Math.PI/96;for(double h:new double[]{.15,4.55}){double y=sign*h;m.quad(p(Math.cos(a)*24.35,y,Math.sin(a)*24.35),p(Math.cos(b)*24.35,y,Math.sin(b)*24.35),p(Math.cos(b)*24.9,y,Math.sin(b)*24.9),p(Math.cos(a)*24.9,y,Math.sin(a)*24.9),color);}}
    // Continuous vertical moulding is visible at eye level, unlike a zero-height ring.
    for(int i=0;i<192;i++){double a=i*Math.PI/96,b=(i+1)*Math.PI/96;for(double h:new double[]{.15,4.55}){double y=sign*h;for(double radius:new double[]{24.35,24.9})m.quad(p(Math.cos(a)*radius,y-.18,Math.sin(a)*radius),p(Math.cos(b)*radius,y-.18,Math.sin(b)*radius),p(Math.cos(b)*radius,y+.18,Math.sin(b)*radius),p(Math.cos(a)*radius,y+.18,Math.sin(a)*radius),color);}}
    for(int i=0;i<24;i++){double a=i*Math.PI/12,x=Math.rint(Math.cos(a)*24.5),z=Math.rint(Math.sin(a)*24.5);double c=sign*4.55;m.box(x-.43,c-.4,z-.43,x+.43,c+.4,z+.43,color);}
    return m;}
  private static Mesh architecture(){var m=new Mesh();
    m.cylinder(0,-.22,0,3.8,.22,16,0xfff8c944);m.cylinder(0,-.35,0,3.4,.15,16,CYAN);m.ring(3.1,.12,.025,0xff746454);m.ring(2.7,.10,.026,0xffeee7e6);
    // Four slender gold caps enclose transparent aqua columns and energy ribbons.
    for(int x:new int[]{-3,3})for(int z:new int[]{-3,3}){m.cylinder(x,.08,z,.42,.26,12,GOLD);m.cylinder(x,5.15,z,.4,.3,12,GOLD);m.cylinder(x,5.43,z,.52,.16,12,0xffda951e);}
    m.band(0,5.3,0,4,3.35,.32,24,0xffe5ad2f);m.band(0,5.62,0,3.55,3.35,.1,24,GOLD);
    // The energy face supplies the bottom cap itself. A second coincident or
    // nearly coincident underside would compete for depth at shallow angles.
    m.cylinder(0,5.30,0,3.34,.08,32,0xffe6b733,false);
    // Sloped octagonal glass crown with a broad gold fan crest.
    for(int i=0;i<16;i++){double a=i*Math.PI/8,w=.025;m.quad(p(Math.cos(a-w)*3.5,5.76,Math.sin(a-w)*3.5),p(Math.cos(a+w)*3.5,5.76,Math.sin(a+w)*3.5),p(Math.cos(a+w)*.12,8.05,Math.sin(a+w)*.12),p(Math.cos(a-w)*.12,8.05,Math.sin(a-w)*.12),GOLD);}
    m.cylinder(0,8.02,0,.19,.55,10,GOLD);m.cylinder(0,8.5,0,.48,.65,12,CYAN);
    crest(m);
    // Blue-columned cream Kindergarten arch and red double-slope roof.
    for(int x:new int[]{-4,4}){m.box(x-.8,0,-23.7,x+.8,.8,-21.5,0xffb6734f);m.box(x-.95,.8,-23.8,x+.95,1.02,-21.4,CREAM);m.cylinder(x,1.02,-22.55,.54,3.5,16,0xff1856c8);m.cylinder(x,4.35,-22.55,.76,.32,16,0xff3168db);m.box(x-.9,4.67,-23.65,x+.9,5.04,-21.45,CREAM);}
    // Solid segmented arched infill. The underside, depth reveals and back are
    // actual caps, so looking up into the gate never exposes its hollow geometry.
    for(int i=0;i<32;i++){double x0=-4.95+i*9.9/32,x1=-4.95+(i+1)*9.9/32;
      double low0=schoolCrown(x0),low1=schoolCrown(x1),high0=8.23-Math.abs(x0)*.54,high1=8.23-Math.abs(x1)*.54;
      if(Math.min(high0-low0,high1-low1)>.001)m.prism(new double[][]{{x0,low0},{x1,low1},{x1,high1},{x0,high0}},-23.75,-21.43,CREAM);
    }
    m.prism(new double[][]{{-5.4,5.35},{0,8.5},{0,8.25},{-5.4,5.1}},-23.9,-21.32,0xffcf493f);
    m.prism(new double[][]{{0,8.5},{5.4,5.35},{5.4,5.1},{0,8.25}},-23.9,-21.32,0xffbc3b35);
    return m;
  }
  private static double schoolCrown(double x){return Math.abs(x)>=3.12?4.9:4.35+1.8*Math.sqrt(Math.max(0,1-x*x/(3.12*3.12)));}
  private static Mesh gardenStones(){var m=new Mesh();
    // The supplied SA2 arch uses irregular radial masonry, not a flat moss lintel.
    for(int side:new int[]{-1,1}){double x=side*3.55;m.cylinder(x,0,22.4,1.08,.48,7,0xffa4aba8);
      // Grounded front masonry joins the statue platform, urn and rear pier.
      m.cylinder(side*3.3,LobbyGeometry0252.PLINTH_BOTTOM,21.1,1.12,LobbyGeometry0252.PLINTH_TOP,7,0xffa4aba8);
      for(int i=0;i<5;i++){double wobble=(i%2==0?.10:-.06)*side;m.box(x-.58+wobble,.48+i*.87,22.3,x+.58+wobble,1.31+i*.87,23.65,i%2==0?0xffbabbb3:0xffaeb4b1);}
    }
    for(int i=0;i<9;i++){double a=i*Math.PI/9,b=(i+1)*Math.PI/9;double inner=2.78,outer=3.62+(i%2)*.17;
      m.prism(new double[][]{{Math.cos(a)*inner,2.64+Math.sin(a)*2.75},{Math.cos(b)*inner,2.64+Math.sin(b)*2.75},{Math.cos(b)*outer,2.64+Math.sin(b)*3.52},{Math.cos(a)*outer,2.64+Math.sin(a)*3.52}},21.94+(i%2)*.08,23.85, i%2==0?0xffb0b5af:0xffbfc1b7);
    }
    return m;
  }
  private static Mesh gardenQuartz(){var m=new Mesh();
    for(int side:new int[]{-1,1}){double x=side*3.42;
      m.cylinder(x,.48,21.98,.55,.19,12,0xfff3ede8);m.cylinder(x,.67,21.98,.33,3.5,14,0xffede9e5);m.cylinder(x,4.17,21.98,.53,.2,12,0xfff8f3ed);
      // Raised spiral carving winds around each slender white column.
      for(int i=0;i<48;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8,y=.78+i*.067;
        m.quad(p(x+Math.cos(a)*.337,y,21.98+Math.sin(a)*.337),p(x+Math.cos(b)*.337,y+.067,21.98+Math.sin(b)*.337),p(x+Math.cos(b)*.337,y+.14,21.98+Math.sin(b)*.337),p(x+Math.cos(a)*.337,y+.073,21.98+Math.sin(a)*.337),0xffbfc2bb);
      }
      m.geometry(LobbyGeometry0252.urn(x,4.35,21.98,1),0xfff1edf0);
      double sx=side*3.7,sz=21.1;m.cylinder(sx,LobbyGeometry0252.PLINTH_TOP,sz,.73,LobbyGeometry0252.STATUE_PLATFORM_TOP-LobbyGeometry0252.PLINTH_TOP,12,0xffd7d9d3);
      m.ellipsoid(sx,.99,sz,.36,.43,.31,0xfff6f2f4);
      m.geometry(LobbyGeometry0252.statueHead(sx,1.66,sz-.03),0xfff6f2f4);
      m.ellipsoid(sx,2.6,sz,.12,.12,.12,0xfffaf6f8);
      for(int limb:new int[]{-1,1}){m.ellipsoid(sx+limb*.23,.69,sz-.25,.24,.14,.28,0xffefedf1);m.ellipsoid(sx+limb*.38,1.04,sz-.12,.14,.29,.15,0xfff0eef3);m.ellipsoid(sx+limb*.18,1.69,sz-.424,.075,.105,.025,0xffb8babe);}
      // Each foreground urn rests directly on the same solid rock plinth.
      m.geometry(LobbyGeometry0252.urn(side*2.9,LobbyGeometry0252.PLINTH_TOP,21.11,.7),0xfff1edf2);
    }
    return m;
  }
  private static Mesh gardenLeaves(){var m=new Mesh();
    for(int side:new int[]{-1,1}){
      gatePlant(m,side*3.42,4.35,21.98,1,false);
      gatePlant(m,side*2.9,LobbyGeometry0252.PLINTH_TOP,21.11,.7,true);
    }return m;
  }
  private static void gatePlant(Mesh m,double x,double base,double z,double size,boolean flowers){
    m.cylinder(x,base+LobbyGeometry0252.SOIL_Y*size,z,.27*size,.012*size,16,0xff4b4431);
    m.geometry(LobbyGeometry0252.leaves(x,base,z,size),0xff4d9e39);
    if(flowers)for(var stem:LobbyGeometry0252.flowers(x,base,z,size)){
      m.stem(stem,.018*size,0xff437f2c);var flower=stem.bloom();
      for(int i=0;i<5;i++){double angle=i*Math.PI*2/5;
        m.ellipsoid(flower.x()+Math.cos(angle)*.072*size,flower.y(),flower.z()+Math.sin(angle)*.072*size,.067*size,.035*size,.06*size,i%2==0?0xffffb52b:0xffed7135);
      }
      m.ellipsoid(flower.x(),flower.y()+.018*size,flower.z(),.045*size,.027*size,.045*size,0xffbd7130);
    }
  }

  private static Mesh glazing(){var m=new Mesh();for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8;m.quad(p(Math.cos(a)*3.5,5.76,Math.sin(a)*3.5),p(Math.cos(b)*3.5,5.76,Math.sin(b)*3.5),p(Math.cos(b)*.12,8.05,Math.sin(b)*.12),p(Math.cos(a)*.12,8.05,Math.sin(a)*.12),0x3879c4ca);}
    for(int x:new int[]{-3,3})for(int z:new int[]{-3,3})m.tubeWalls(x,.35,z,.29,4.8,24,15,0x5968e8ed);return m;}
  private static Mesh pad(double y,double radius,int sides){var m=new Mesh();for(int i=0;i<sides;i++){double a=i*2*Math.PI/sides,b=(i+1)*2*Math.PI/sides;m.vertex(0,y,0,.5f,.5f,0xffffffff);m.vertex(Math.cos(a)*radius,y,Math.sin(a)*radius,(float)(.5+Math.cos(a)*.5),(float)(.5+Math.sin(a)*.5),0xffffffff);m.vertex(Math.cos(b)*radius,y,Math.sin(b)*radius,(float)(.5+Math.cos(b)*.5),(float)(.5+Math.sin(b)*.5),0xffffffff);m.vertex(0,y,0,.5f,.5f,0xffffffff);}return m;}
  private static double[] fanPoint(double angle,double radius,double z){return p(Math.cos(angle)*radius,4.8+Math.sin(angle)*radius,z);}
  private static void crest(Mesh m){
    // Seven pierced hexagonal windows form one broad fan, rather than separate
    // rods. Front/back rings and window reveals give the plate real thickness.
    double start=Math.PI/12,span=Math.PI*5/6,half=span/14;
    for(int i=0;i<7;i++){double a=start+(i+.5)*span/7;
      double[] angles={a-half,a+half,a+half,a+half,a-half,a-half};double[] radii={2.3,2.3,3.375,4.1,4.1,3.375};
      double small=Math.PI/150,wide=Math.PI/40;
      double[] holeAngles={a-small,a+small,a+wide,a+small,a-small,a-wide};double[] holeRadii={3.0,3.0,3.375,3.75,3.75,3.375};
      for(int j=0;j<6;j++){int k=(j+1)%6;var o0=fanPoint(angles[j],radii[j],-3.72);var o1=fanPoint(angles[k],radii[k],-3.72);var h0=fanPoint(holeAngles[j],holeRadii[j],-3.72);var h1=fanPoint(holeAngles[k],holeRadii[k],-3.72);
        m.quad(o0,o1,h1,h0,GOLD);var ob0=p(o0[0],o0[1],-3.44);var ob1=p(o1[0],o1[1],-3.44);var hb0=p(h0[0],h0[1],-3.44);var hb1=p(h1[0],h1[1],-3.44);
        m.quad(hb0,hb1,ob1,ob0,shade(GOLD,.86));m.quad(h0,h1,hb1,hb0,shade(GOLD,.57));m.quad(ob0,ob1,o1,o0,shade(GOLD,.72));
      }
      var left=fanPoint(a-half,2.3,-3.72);var right=fanPoint(a+half,2.3,-3.72);m.quad(p(left[0],5.32,-3.72),p(right[0],5.32,-3.72),right,left,GOLD);
      var leftBack=p(left[0],left[1],-3.44);var rightBack=p(right[0],right[1],-3.44);m.quad(leftBack,rightBack,p(right[0],5.32,-3.44),p(left[0],5.32,-3.44),shade(GOLD,.86));
    }
  }
  private static Mesh exitSign(){var m=new Mesh();
    // Each label belongs to its own plate face and points outwards. The old
    // opposite-side label at z+3.72 floated in empty space below the portal.
    m.quad(p(1.8,5.68,-3.77),p(-1.8,5.68,-3.77),p(-1.8,6.58,-3.77),p(1.8,6.58,-3.77),0xffffffff,0,1,1,0);
    m.quad(p(-1.8,5.68,-3.39),p(1.8,5.68,-3.39),p(1.8,6.58,-3.39),p(-1.8,6.58,-3.39),0xffffffff,0,1,1,0);
    return m;
  }
  private static Mesh sign(){var m=new Mesh();m.quad(p(-3.45,5.6,-21.25),p(3.45,5.6,-21.25),p(3.45,7.32,-21.25),p(-3.45,7.32,-21.25),0xffffffff,0,1,1,0);return m;}
  private static Mesh palm(){var m=new Mesh();for(int i=0;i<8;i++){double a=i*Math.PI/4,dx=Math.cos(a),dz=Math.sin(a),nx=-dz,nz=dx;for(int j=0;j<8;j++){double t=j/8.0,u=(j+1)/8.0;double r0=t*4.6,r1=u*4.6,y0=.8*Math.sin(t*Math.PI)-t*.85,y1=.8*Math.sin(u*Math.PI)-u*.85,w0=.54*Math.sin((t+.03)*Math.PI),w1=.54*Math.sin((u+.03)*Math.PI);m.quad(p(dx*r0+nx*w0,y0,dz*r0+nz*w0),p(dx*r1+nx*w1,y1,dz*r1+nz*w1),p(dx*r1-nx*w1,y1,dz*r1-nz*w1),p(dx*r0-nx*w0,y0,dz*r0-nz*w0),0xffffffff,(float)t,0,(float)u,1);}}return m;}
  private static void energyVertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float u,float v,int color,float nx,float ny,float nz){
    if(!shaderPack){emit(out,pose.pose(),x,y,z,u,v,color);return;}
    out.addVertex(pose,x,y,z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(LightTexture.FULL_BRIGHT).setNormal(pose,nx,ny,nz);
  }
  private static void energy(PoseStack poses,MultiBufferSource buffers,float t){var b=buffers.getBuffer(Material.get(WHITE,0));var matrix=poses.last();
    for(int x:new int[]{-3,3})for(int z:new int[]{-3,3}){
      for(int i=0;i<14;i++){float y0=.35f+i*.34f,y1=y0+.34f;float ox0=(float)Math.sin(t+i*.7f)*.09f,ox1=(float)Math.sin(t+(i+1)*.7f)*.09f;
        int color=0xaacb75ff;energyVertex(b,matrix,x+ox0-.035f,y0,z,0,0,color,0,0,1);energyVertex(b,matrix,x+ox1-.035f,y1,z,1,0,color,0,0,1);energyVertex(b,matrix,x+ox1+.035f,y1,z,1,1,color,0,0,1);energyVertex(b,matrix,x+ox0+.035f,y0,z,0,1,color,0,0,1);
        energyVertex(b,matrix,x-.16f,y0,z-.16f,0,0,0x3976f3f1,.70710677f,0,-.70710677f);energyVertex(b,matrix,x+.16f,y0,z+.16f,1,0,0x3976f3f1,.70710677f,0,-.70710677f);energyVertex(b,matrix,x+.16f,y1,z+.16f,1,1,0x3976f3f1,.70710677f,0,-.70710677f);energyVertex(b,matrix,x-.16f,y1,z-.16f,0,1,0x3976f3f1,.70710677f,0,-.70710677f);
      }
    }
    float[] corners={-2.15f,2.15f};for(float x:corners)for(float z:corners){energyVertex(b,matrix,x-.15f,.035f,z-.15f,0,0,0x6676e5ff,0,1,0);energyVertex(b,matrix,x+.15f,.035f,z-.15f,1,0,0x6676e5ff,0,1,0);energyVertex(b,matrix,x+.15f,.035f,z+.15f,1,1,0x6676e5ff,0,1,0);energyVertex(b,matrix,x-.15f,.035f,z+.15f,0,1,0x6676e5ff,0,1,0);}
  }
}
