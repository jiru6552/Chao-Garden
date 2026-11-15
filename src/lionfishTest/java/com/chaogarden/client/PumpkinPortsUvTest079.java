package com.chaogarden.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Rasterize the production front-panel UVs against Minecraft's native texture.
 * Reads the configured resource classpath or its generated resource jar; no
 * Minecraft image is copied into this mod's resources or started as a client. */
public final class PumpkinPortsUvTest079 {
  private static final String ENTRY="assets/minecraft/textures/block/carved_pumpkin.png";
  private static long checks,samples;
  private static String textureSource;
  private record Counts(long eyes,long mouth,Set<Integer> mouthTexels) {}
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static byte[] texture(String[] args)throws Exception {
    try(var in=PumpkinPortsUvTest079.class.getClassLoader().getResourceAsStream(ENTRY)) {
      if(in!=null){textureSource="runtime-classpath:"+ENTRY;return in.readAllBytes();}
    }
    Path jar=args.length>0?Path.of(args[0]):null;
    if(jar==null)try(var files=Files.list(Path.of("build/moddev/artifacts"))) {
      jar=files.filter(p->p.getFileName().toString().endsWith("-client-extra-aka-minecraft-resources.jar")).findFirst().orElseThrow();
    }
    try(var zip=new ZipFile(jar.toFile());var in=zip.getInputStream(zip.getEntry(ENTRY))) {
      textureSource=jar+"!"+ENTRY;return in.readAllBytes();
    }
  }
  private static boolean dark(int rgba){return ((rgba>>>16)&255)<85&&((rgba>>>8)&255)<45;}
  private static float[] panel(float x,float y,float[] b,ChaoHeadCover079.Ports p) {
    if(y<p.top())return new float[]{b[0],b[3],b[1],p.top()};
    if(y>p.bottom())return new float[]{b[0],b[3],p.bottom(),b[4]};
    if(x<p.left0())return new float[]{b[0],p.left0(),p.top(),p.bottom()};
    if(x>p.left1()&&x<p.right0())return new float[]{p.left1(),p.right0(),p.top(),p.bottom()};
    if(x>p.right1())return new float[]{p.right1(),b[3],p.top(),p.bottom()};
    return null; // A real opening emits no front geometry or texture sample.
  }
  private static Counts raster(BufferedImage image,float[] head,float[] eyes,boolean aligned) {
    float[] b=VanillaHatRenderer050.fittedBounds(head);var ports=ChaoHeadCover079.eyePorts(head,eyes);
    long painted=0,mouth=0;var texels=new HashSet<Integer>();
    // Inward pixel centers avoid assigning a shared edge to two panels.
    for(int row=0;row<416;row++)for(int col=0;col<480;col++) {
      float x=b[0]+(col+.5f)/480*(b[3]-b[0]),y=b[1]+(row+.5f)/416*(b[4]-b[1]);
      float[] panel=panel(x,y,b,ports);if(panel==null)continue;
      float[] uv=VanillaHatRenderer050.frontUv(b,ports,aligned,panel[0],panel[1],panel[2],panel[3]);
      float u=uv[0]+(x-panel[0])/(panel[1]-panel[0])*(uv[2]-uv[0]);
      float v=uv[1]+(y-panel[2])/(panel[3]-panel[2])*(uv[3]-uv[1]);
      check(Float.isFinite(u)&&Float.isFinite(v)&&u>=0&&u<=1&&v>=0&&v<=1,"Finite sprite-local front UVs");
      int tx=Math.min(15,(int)(u*16)),ty=Math.min(15,(int)(v*16));samples++;
      if(dark(image.getRGB(tx,ty))) {
        if(ty<8)painted++;
        else {mouth++;texels.add(ty*16+tx);}
      }
      // The skull branch retains exactly its old front texture coordinates.
      float[] skull=VanillaHatRenderer050.frontUv(b,ports,false,panel[0],panel[1],panel[2],panel[3]);
      check(skull[0]==(panel[0]-b[0])/(b[3]-b[0])&&skull[1]==(panel[2]-b[1])/(b[4]-b[1])
          &&skull[2]==(panel[1]-b[0])/(b[3]-b[0])&&skull[3]==(panel[3]-b[1])/(b[4]-b[1]),"Skull UV branch changed");
    }
    return new Counts(painted,mouth,texels);
  }
  public static void main(String[] args)throws Exception {
    byte[] bytes=texture(args);var image=ImageIO.read(new ByteArrayInputStream(bytes));check(image.getWidth()==16&&image.getHeight()==16,"Native carved pumpkin texture dimensions");
    var mouth=new HashSet<Integer>();int nativeEyes=0;
    for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(dark(image.getRGB(x,y))) {
      if(y<8)nativeEyes++;else mouth.add(y*16+x);
    }
    check(nativeEyes>0&&!mouth.isEmpty(),"Native texture must contain both painted eyes and carved mouth");
    float[][] heads={{-3.5f,-6,-3,3.5f,0,3},{-3.12f,-5.9f,-2.95f,3.12f,-.18f,2.85f},
        {-3.45f,-6.08f,-2.98f,3.45f,-.04f,2.98f},{-3.5f,-6.2f,-3,3.5f,0,2.65f}};
    float[][] eyes={{0,-2,-3.135f,2},{0,-2,-3.1f,2},{0,-2.35f,-3.23f,1.9f},{0,-2.25f,-3.045f,1.75f}};
    for(int n=0;n<heads.length;n++) {
      var fixed=raster(image,heads[n],eyes[n],true);check(fixed.eyes()==0,"Painted pumpkin eye survives outside a real iris port, case "+n);
      check(fixed.mouth()>0&&fixed.mouthTexels().containsAll(mouth),"Native carved mouth texels were lost, case "+n);
    }
    var old=raster(image,heads[0],eyes[0],false);check(old.eyes()>0,"Control failed to expose the original four-eye artifact");
    System.out.println("{\"result\":\"PASS\",\"checks\":"+checks+",\"raster_samples\":"+samples
        +",\"head_profiles\":4,\"painted_eye_samples_outside_ports\":0,\"native_mouth_dark_texels_preserved\":"+mouth.size()
        +",\"old_mapping_painted_eye_samples\":"+old.eyes()+",\"native_texture_sha256\":\""
        +HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))+"\",\"native_texture_source\":\""+textureSource+"\"}");
  }
}
