package com.chaogarden.client;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
public final class AccessoryFitHeadless {
 static float[] array(JsonObject data,String id){JsonArray a=data.getAsJsonArray(id);float[] out=new float[a.size()];for(int i=0;i<out.length;i++)out[i]=a.get(i).getAsFloat();return out;}
 public static void main(String[] args)throws Exception{
  JsonArray cases=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonArray();JsonArray result=new JsonArray();
  String[] items={"baseball_cap_accessory","press_hat_accessory","headphones_accessory","round_glasses_accessory","aviators_accessory","pacifier_accessory","bowtie_accessory","cheese_bowtie_accessory","blue_hoodie_accessory","pink_hoodie_accessory","hoodie_sleeve_left","hoodie_sleeve_right","hoodie_hood","medal_race","medal_karate","eggshell_hat"};
  Gson gson=new Gson();
  for(var element:cases){JsonObject data=element.getAsJsonObject(),out=new JsonObject();out.add("name",data.get("name"));
   float[] head=array(data,"h"),eyes=array(data,"e"),body=array(data,"b");
   for(String item:items){var fit=ChaoAccessoryFit.accessory(item,head,eyes,body,item.endsWith("left")?array(data,"al"):array(data,"ar"));
    var value=new JsonObject();value.add("fit",gson.toJsonTree(new float[]{fit.x(),fit.y(),fit.z(),fit.sx(),fit.sy(),fit.sz()}));
    Map<String,ChaoMeshAsset.Pose> parts=new LinkedHashMap<>();for(String name:List.of("accessory","temple_left","temple_right","hinge_left","hinge_right"))parts.put(name,new ChaoMeshAsset.Pose());
    ChaoAccessoryFit.parts(item,head,fit,parts);value.add("parts",gson.toJsonTree(parts));out.add(item,value);
   }result.add(out);
  }Files.writeString(Path.of(args[1]),gson.toJson(result));System.out.println("Runtime accessory formula cases: "+result.size());
 }
}
