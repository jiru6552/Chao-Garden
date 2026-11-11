package com.chaogarden.client;
import java.util.Map;
/** Follow the instrument at chest height instead of staring at its now-hidden floor block. */
final class ChaoInstrumentFocus {
  private ChaoInstrumentFocus(){}
  static void apply(Map<String,ChaoMeshAsset.Pose> poses,float weight){
    var head=poses.get("skull");if(head==null)return;
    float w=Math.max(0,Math.min(1,weight));w=1-(1-w)*(1-w)*(1-w);
    head.rx=head.rx*(1-w)+.025f*w;head.ry*=1-w;
  }
}
