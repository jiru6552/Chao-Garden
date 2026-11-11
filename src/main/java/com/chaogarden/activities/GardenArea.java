package com.chaogarden.activities;

import com.chaogarden.hub.HubContent;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** New gardens cover nine chunks; legacy enclosure footprints and authored garden realms retain their saved bounds. */
public final class GardenArea {
  public static final List<String> FOOD = List.of("garden_fruit", "hero_garden_fruit", "dark_garden_fruit");
  public final boolean realm;
  public final Set<Long> floor;
  public final BlockPos home;
  public final boolean chunkSquare;
  public final int centerChunkX,centerChunkZ;
  private GardenArea(int cx,int cz,BlockPos home){
    realm=false;floor=Set.of();this.home=home.immutable();chunkSquare=true;centerChunkX=cx;centerChunkZ=cz;
  }
  public GardenArea(boolean realm, Collection<Long> floor, BlockPos home) {
    this.realm=realm; this.floor=Set.copyOf(floor); this.home=home.immutable();chunkSquare=false;centerChunkX=centerChunkZ=0;
  }
  public boolean contains(ServerLevel level, Vec3 pos) {
    if(realm) return HubContent.isGardenDimension(level) && HubContent.insideGarden(level.dimension(), (int)Math.floor(pos.x), (int)Math.floor(pos.z))
        && HubContent.gardenBounds(level.dimension()).contains(pos);
    if(chunkSquare){
      int cx=net.minecraft.util.Mth.floor(pos.x)>>4,cz=net.minecraft.util.Mth.floor(pos.z)>>4;
      return cx>=centerChunkX-1&&cx<=centerChunkX+1&&cz>=centerChunkZ-1&&cz<=centerChunkZ+1
          &&pos.y>=level.getMinBuildHeight()&&pos.y<level.getMaxBuildHeight();
    }
    // Vertical headroom lets residents jump or glide inside their fenced footprint.
    BlockPos p=BlockPos.containing(pos);
    for(int dy=-12;dy<=3;dy++) if(floor.contains(p.offset(0,dy,0).asLong()))return true;
    return false;
  }
  public static GardenArea inspect(ServerLevel level, BlockPos device) {
    if(HubContent.isGardenDimension(level))return new GardenArea(true,List.of(),device.offset(2,0,0));
    return new GardenArea(device.getX()>>4,device.getZ()>>4,device.offset(2,0,0));
  }
  public CompoundTag save() {
    var n=new CompoundTag();n.putBoolean("Realm",realm);n.putLong("Home",home.asLong());
    if(chunkSquare){n.putBoolean("ChunkSquare",true);n.putInt("CenterChunkX",centerChunkX);n.putInt("CenterChunkZ",centerChunkZ);}
    n.putLongArray("Floor",floor.stream().mapToLong(Long::longValue).sorted().toArray());return n;
  }
  public static GardenArea load(CompoundTag n) {
    if(n.getBoolean("ChunkSquare")&&!n.getBoolean("Realm"))return new GardenArea(n.getInt("CenterChunkX"),n.getInt("CenterChunkZ"),BlockPos.of(n.getLong("Home")));
    long[] values=n.getLongArray("Floor");if(values.length>48*48*4)return null;
    return new GardenArea(n.getBoolean("Realm"),Arrays.stream(values).boxed().toList(),BlockPos.of(n.getLong("Home")));
  }
}
