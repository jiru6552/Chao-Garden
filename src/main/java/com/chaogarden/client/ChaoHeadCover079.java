package com.chaogarden.client;

/** Shared full-cover landmarks in skull-local pixels; the skin is never resized. */
final class ChaoHeadCover079 {
  record Ports(float left0,float left1,float right0,float right1,float top,float bottom) {}
  private ChaoHeadCover079() {}
  static Ports eyePorts(float[] head,float[] eyes) {
    float width=head[3]-head[0],height=head[4]-head[1];
    float center=eyes==null?(head[0]+head[3])*.5f:eyes[0];
    float y=eyes==null?head[1]+height*2/3:eyes[1];
    float spacing=eyes==null?width*2/7:eyes[3];
    float halfWidth=width*1.2f/7,halfHeight=height*1.25f/6;
    return new Ports(center-spacing-halfWidth,center-spacing+halfWidth,
        center+spacing-halfWidth,center+spacing+halfWidth,y-halfHeight,y+halfHeight);
  }
}
