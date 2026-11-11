package com.chaogarden.client;
import com.chaogarden.toys.RockingHorsePlayRules;
import org.joml.Vector3f;
import org.joml.Matrix4f;
public final class RockingHorseTimingTest {
  public static void main(String[] args){
    var clip=new LionfishRockingHorseClip();int samples=0,cues=0;float error=0,maxStep=0;
    for(int n=0;n<=640;n++){
      float tick=n/4f,angle=clip.angle(tick),server=RockingHorsePlayRules.angle(tick);
      error=Math.max(error,Math.abs(angle-server));
      if(Math.abs(angle)>Math.toRadians(6.001)||Math.abs(angle-server)>.00001)throw new AssertionError("horse shared clock "+tick+" "+angle+" vs "+server);
      if(n>0)maxStep=Math.max(maxStep,Math.abs(angle-clip.angle(tick-.25f)));
      if(n%4==0&&RockingHorsePlayRules.soundDue((int)tick)){
        if(Math.abs(angle)<.09||RockingHorsePlayRules.occupancy(tick)!=1)throw new AssertionError("sound without loaded rocker "+tick);cues++;
      }
      double y=RockingHorsePlayRules.riderY(tick),z=RockingHorsePlayRules.riderZ(tick);
      if(!Double.isFinite(y+z)||y<.33||y>.38||z<-.14||z>-.06)throw new AssertionError("bad seat anchor "+tick);
      for(float scale:new float[]{.64f,.8f,.86f})for(float actorX:new float[]{-.7f,0,.7f})for(float actorY:new float[]{0,.35f,.5f}){
        float actorZ=-.3f;var matrix=ChaoRockingHorseAnimation.horseMatrix(scale,actorX,actorY,actorZ,server);
        for(var point:new Vector3f[]{new Vector3f(0,.005f,0),new Vector3f(0,RockingHorsePlayRules.SEAT_Y,-.1f),new Vector3f(.13f,.6377f,.14625f)}){
          var actual=matrix.transformPosition(new Vector3f(point.x,point.y-.5f,point.z));
          var expected=ChaoRockingHorseAnimation.modelPoint(point.x,point.y,point.z,tick,scale,actorX,actorY,actorZ);
          if(actual.distance(expected)>.000001f)throw new AssertionError("horse moves with rendered entity instead of remaining at block "+tick+" "+actual+" vs "+expected);
          if(n%64==0)for(float yaw:new float[]{-170,-90,0,90,170})for(float facing:new float[]{0,90,180,270}){
            float body=(float)Math.toRadians(yaw),source=(float)Math.toRadians(facing),correction=source-body;
            var produced=new Matrix4f().rotationY(-body).rotateX((float)Math.PI).scale(scale).translate(0,-1.501f,0)
                .rotateY(correction).mul(matrix).transformPosition(new Vector3f(point.x,point.y-.5f,point.z));
            var desired=new Vector3f(point.x-actorX,RockingHorsePlayRules.pointY(point.y,point.z,tick)-actorY,RockingHorsePlayRules.pointZ(point.y,point.z,tick)-actorZ).rotateY(-source);
            if(produced.distance(desired)>.000002f)throw new AssertionError("horse rotates with incoming body yaw "+yaw+" expected block "+facing+" "+produced+" vs "+desired);
          }
        }
      }
      samples++;
    }
    if(cues!=8||maxStep>.008||clip.angle(Float.NaN)!=0||clip.angle(0)!=0||clip.angle(160)!=0||RockingHorsePlayRules.occupancy(0)!=0||RockingHorsePlayRules.occupancy(160)!=0)throw new AssertionError("horse endpoints/cadence");
    System.out.println("Rocking horse timing: "+samples+" real Lionfish quarter-tick samples; eight loaded-rock cues; shared server/client angle max error="+error+" radians; max quarter-tick step="+maxStep);
  }
}
