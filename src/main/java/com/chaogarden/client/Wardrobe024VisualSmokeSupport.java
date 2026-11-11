package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

/** Excluded visual fixtures. Choice results are deliberately latched through saved data;
 * all actual inventory/hold/wear/throw transactions use the production state machine.
 * WardrobeChoiceTest independently checks the mood percentage and every possible roll. */
public final class Wardrobe024VisualSmokeSupport {
  private Wardrobe024VisualSmokeSupport() {}
  public static ChaoEntity prepare(ServerPlayer keeper,String key,Vec3 at) {
    var c=ChaoGarden.CHAO.get().create(keeper.serverLevel());
    var s=ChaoState.egg(0,false,true,0,new Random(240));s.adult=true;s.happiness=95;s.energy=100;s.hunger=80;
    s.swimFly=s.runPower=0;s.primaryType=0;s.setEvolutionStrength(.6f);
    c.setState(s);c.owner=keeper.getUUID();c.setNoAi(true);c.setNoGravity(true);c.setStaying(true);c.moveTo(at,0,0);c.setYHeadRot(0);c.yBodyRot=0;
    keeper.serverLevel().addFreshEntity(c);
    boolean reject=key.equals("wardrobe-reject");
    ItemStack gift;
    if(key.startsWith("shell-")) {
      var coat=ChaoState.egg(key.equals("shell-normal")?2:4,key.equals("shell-shiny"),true,key.equals("shell-jewel")?3:key.equals("shell-moon")?13:0,new Random(24));
      if(key.equals("shell-nether"))coat.variant="nether";
      if(key.equals("shell-end"))coat.variant="end";
      c.setOriginEggCoat(coat);
      gift=new ItemStack(ChaoGarden.item("eggshell_hat"));var tag=new CompoundTag();tag.putString("EggShellCoat",coat.encode());gift.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    } else {
      c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("pan_hat")));
      gift=new ItemStack(ChaoGarden.item(reject?"paper_bag_hat":"blue_hoodie_accessory"));
      if(reject)c.state().happiness=-75;
    }
    keeper.getInventory().setItem(8,gift);
    if(!c.offerWardrobeItem(keeper,keeper.getInventory().getItem(8)))throw new IllegalStateException("Visual wardrobe offer rejected: "+key);
    var saved=new CompoundTag();c.saveWithoutId(saved);saved.putBoolean("WardrobeChoiceMade",true);saved.putBoolean("WardrobeChoiceAccepted",!reject);c.load(saved);
    c.getPersistentData().putString("Wardrobe024Scene",key);return c;
  }
  @SuppressWarnings("unchecked") private static void clock(ChaoEntity c,int action,int duration,long start) {
    try {
      for(var entry:java.util.Map.of("ITEM_ACTION",action,"ITEM_DURATION",duration).entrySet()) {
        var f=ChaoEntity.class.getDeclaredField(entry.getKey());f.setAccessible(true);c.getEntityData().set((EntityDataAccessor<Integer>)f.get(null),entry.getValue());
      }
      var f=ChaoEntity.class.getDeclaredField("ITEM_STARTED");f.setAccessible(true);c.getEntityData().set((EntityDataAccessor<Long>)f.get(null),start);
    }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  }
  /** Keep the real outcome visible after it commits; fixtures never create a second throw. */
  public static void maintain(ChaoEntity c,String key) {
    if(ready(c,key)) {
      c.setNoAi(true);c.setNoGravity(true);c.setStaying(true);c.setDeltaMovement(Vec3.ZERO);
      var action=key.equals("wardrobe-reject")?ChaoEntity.ItemAction.REJECT:ChaoEntity.ItemAction.DELIGHT;
      clock(c,action.ordinal(),100000,c.level().getGameTime()-65000);
      for(var item:c.level().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(4),e->e.getPersistentData().contains("ChaoRejectedUntil"))) {
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);
        if(key.equals("wardrobe-reject"))item.setPos(c.getX()-.55,c.getY()+.7,c.getZ()+.12);
      }
    }
  }
  public static boolean ready(ChaoEntity c,String key) {
    if(!c.getMainHandItem().isEmpty())return false;
    if(key.equals("wardrobe-reject"))return !c.level().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(4),e->e.getItem().is(ChaoGarden.item("paper_bag_hat"))).isEmpty();
    return key.startsWith("shell-")?c.getItemBySlot(EquipmentSlot.HEAD).is(ChaoGarden.item("eggshell_hat")):c.getItemBySlot(EquipmentSlot.CHEST).is(ChaoGarden.item("blue_hoodie_accessory"));
  }
  /** Real runtime rigs, original iris landmarks and actual mask triangles. */
  public static int inspectClient() {
    int cases=0;var rig=new OfficialChaoRig();var shell=new EggshellMask();
    var themes=new java.util.ArrayList<String>();themes.add(ChaoVariants.NORMAL);for(var definition:ChaoVariants.all())themes.add(definition.id());
    for(String theme:themes)for(int family=0;family<15;family++)for(float age:new float[]{0,.6f,1.2f})for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++) {
      var s=new ChaoState();s.variant=theme;s.adult=true;s.align=family<5?0:family<10?1:-1;s.primaryType=family%5;s.swimFly=sf;s.runPower=rp;s.setEvolutionStrength(age);
      String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(24,++cases),OfficialChaoRig.Motion.WALK,120,0,0,0,0,0,0,false);rig.development(s);
      inspect(shell,rig,key+"/"+theme);
    }
    for(String key:new String[]{"baby","chaos_light","chaos_angel","chaos_devil","amy","omochao","tails","knuckles"}) {
      var s=new ChaoState();s.adult=!key.equals("baby");s.character=key;rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(24,++cases),OfficialChaoRig.Motion.WALK,120,0,0,0,0,0,0,false);rig.development(s);inspect(shell,rig,key);
    }
    return cases;
  }
  private static void inspect(EggshellMask mask,OfficialChaoRig rig,String key) {
    var h=rig.attachmentBounds("skull");var e=rig.attachmentEyes();require(h!=null&&e!=null,key+" missing actual skull or eye landmarks");
    var v=mask.inspectGeometry(h,e);require(v.length>500,key+" empty shell");float[] lo={999,999,999},hi={-999,-999,-999};
    for(int i=0;i<v.length;i+=8)for(int a=0;a<8;a++) {require(Float.isFinite(v[i+a]),key+" non-finite mask");if(a<3){lo[a]=Math.min(lo[a],v[i+a]);hi[a]=Math.max(hi[a],v[i+a]);}if(a==3||a==4)require(v[i+a]>=0&&v[i+a]<=1,key+" unwrapped shell UV");}
    for(int a=0;a<3;a++)require(lo[a]<h[a]&&hi[a]>h[a+3],key+" shell failed to surround full skull axis "+a);
    // Fire frontward rays at each original pupil and around its visible central area.
    for(int side:new int[]{-1,1})for(float dx:new float[]{-.45f,0,.45f})for(float dy:new float[]{-.45f,0,.45f}) {
      float x=e[0]+side*e[3]+dx,y=e[1]+dy;
      for(int i=0;i<v.length;i+=32) {
        require(!frontTriangle(v,i,i+8,i+16,x,y,e[2])&&!frontTriangle(v,i,i+16,i+24,x,y,e[2]),key+" native eye occluded by shell");
      }
    }
  }
  private static boolean frontTriangle(float[] v,int a,int b,int c,float x,float y,float eyeZ) {
    float den=(v[b+1]-v[c+1])*(v[a]-v[c])+(v[c]-v[b])*(v[a+1]-v[c+1]);if(Math.abs(den)<.00001f)return false;
    float u=((v[b+1]-v[c+1])*(x-v[c])+(v[c]-v[b])*(y-v[c+1]))/den;
    float w=((v[c+1]-v[a+1])*(x-v[c])+(v[a]-v[c])*(y-v[c+1]))/den;float t=1-u-w;
    return u>.0001f&&w>.0001f&&t>.0001f&&u*v[a+2]+w*v[b+2]+t*v[c+2]<eyeZ-.001f;
  }
  private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Wardrobe shell audit: "+why);}
}
