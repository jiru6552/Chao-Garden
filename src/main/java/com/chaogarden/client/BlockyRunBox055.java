package com.chaogarden.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Authored cuboid poses. Blend transforms, so rotations never shear a box. */
final class BlockyRunBox055 {
  private final float[][] centers,sizes,rotations,corners;

  private BlockyRunBox055(float[][] centers,float[][] sizes,float[][] rotations,float[][] corners) {
    this.centers=centers;this.sizes=sizes;this.rotations=rotations;this.corners=corners;
  }

  static BlockyRunBox055 parse(JsonObject data,float[][] reference) {
    float[][] centers=targets(data.getAsJsonArray("centers"));
    float[][] sizes=targets(data.getAsJsonArray("sizes"));
    float[][] rotations=targets(data.getAsJsonArray("rotations"));
    for(var size:sizes)for(float value:size)if(value<.01f)
      throw new IllegalStateException("Blocky morphology requires positive box sizes");
    if(reference.length!=8)throw new IllegalStateException("Blocky morphology requires eight box vertices");
    float[][] corners=new float[8][3];
    for(int i=0;i<8;i++) {
      float[] v={reference[i][0]-centers[0][0],reference[i][1]-centers[0][1],reference[i][2]-centers[0][2]};
      rotate(v,2,-rotations[0][2]);rotate(v,1,-rotations[0][1]);rotate(v,0,-rotations[0][0]);
      for(int a=0;a<3;a++) {
        float value=v[a]/sizes[0][a];
        if(Math.abs(Math.abs(value)-.5f)>.003f)throw new IllegalStateException("Reference box/transform correspondence differs");
        corners[i][a]=Math.copySign(.5f,value);
      }
    }
    return new BlockyRunBox055(centers,sizes,rotations,corners);
  }

  private static float[][] targets(JsonArray array) {
    if(array==null||array.size()!=7)throw new IllegalStateException("Expected seven blocky morphology poses");
    float[][] values=new float[7][3];
    for(int t=0;t<7;t++) {
      var vector=array.get(t).getAsJsonArray();
      if(vector.size()!=3)throw new IllegalStateException("Expected a three-component box transform");
      for(int a=0;a<3;a++) {
        values[t][a]=vector.get(a).getAsFloat();
        if(!Float.isFinite(values[t][a]))throw new IllegalStateException("Non-finite blocky morphology transform");
      }
    }
    return values;
  }

  void evaluate(float[] weights,float[][] output) {
    float[] c=new float[3],s=new float[3],r=new float[3];
    for(int t=0;t<7;t++)for(int a=0;a<3;a++) {
      c[a]+=centers[t][a]*weights[t];s[a]+=sizes[t][a]*weights[t];r[a]+=rotations[t][a]*weights[t];
    }
    for(int i=0;i<8;i++) {
      float[] v=output[i];for(int a=0;a<3;a++)v[a]=corners[i][a]*s[a];
      rotate(v,0,r[0]);rotate(v,1,r[1]);rotate(v,2,r[2]);
      for(int a=0;a<3;a++)v[a]+=c[a];
    }
  }

  private static void rotate(float[] v,int axis,float degrees) {
    float c=(float)Math.cos(Math.toRadians(degrees)),s=(float)Math.sin(Math.toRadians(degrees));
    float x=v[0],y=v[1],z=v[2];
    if(axis==0){v[1]=c*y-s*z;v[2]=s*y+c*z;}
    else if(axis==1){v[0]=c*x+s*z;v[2]=-s*x+c*z;}
    else {v[0]=c*x-s*y;v[1]=s*x+c*y;}
  }
}
