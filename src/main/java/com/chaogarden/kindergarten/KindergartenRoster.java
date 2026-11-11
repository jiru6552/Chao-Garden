package com.chaogarden.kindergarten;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** The classroom owns one durable snapshot per student. Its visible bodies are unsaved projections. */
public final class KindergartenRoster extends SavedData {
  public static final int CAPACITY=4;
  public static final class Student {
    public final UUID id,keeper;
    public final int seat;
    public CompoundTag entity;
    Student(UUID id,UUID keeper,int seat,CompoundTag entity){this.id=id;this.keeper=keeper;this.seat=seat;this.entity=entity.copy();}
  }
  public final Map<UUID,Student> students=new LinkedHashMap<>();
  public static KindergartenRoster get(MinecraftServer server){
    return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(KindergartenRoster::new,KindergartenRoster::load),"chao_kindergarten_roster");
  }
  public int freeSeat(){for(int seat=0;seat<CAPACITY;seat++){final int at=seat;if(students.values().stream().noneMatch(s->s.seat==at))return seat;}return -1;}
  public static KindergartenRoster load(CompoundTag root,HolderLookup.Provider lookup){
    var data=new KindergartenRoster();var list=root.getList("Students",10);
    for(int i=0;i<list.size()&&data.students.size()<CAPACITY;i++){
      var row=list.getCompound(i);if(!row.hasUUID("Id")||!row.hasUUID("Keeper")||!row.contains("Entity"))continue;
      int seat=row.getInt("Seat");if(seat<0||seat>=CAPACITY||data.students.values().stream().anyMatch(s->s.seat==seat))continue;
      var entity=row.getCompound("Entity");UUID id=row.getUUID("Id");
      if(!entity.hasUUID("UUID")||!entity.getUUID("UUID").equals(id))continue;
      data.students.putIfAbsent(id,new Student(id,row.getUUID("Keeper"),seat,entity));
    }
    return data;
  }
  @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
    var list=new ListTag();for(var s:students.values()){
      var row=new CompoundTag();row.putUUID("Id",s.id);row.putUUID("Keeper",s.keeper);row.putInt("Seat",s.seat);row.put("Entity",s.entity.copy());list.add(row);
    }root.put("Students",list);return root;
  }
}
