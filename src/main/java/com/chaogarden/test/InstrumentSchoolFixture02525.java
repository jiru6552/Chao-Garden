package com.chaogarden.test;
import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
/** Real enrollment fixture; only the lesson selection/duration is shortened after enrollment. Never packaged. */
public final class InstrumentSchoolFixture02525 {
  public static ChaoEntity enroll(ServerPlayer keeper,int slot,String lesson,boolean adult,int duration){
    var level=keeper.serverLevel();var c=ChaoGarden.CHAO.get().create(level);var s=new ChaoState();s.adult=adult;s.hunger=83;s.energy=100;s.happiness=84;c.setState(s);c.owner=keeper.getUUID();
    var item=new ItemStack(ChaoGarden.item("chao_carrier"));if(!ChaoCarrierItem.capture(item,c,keeper))throw new IllegalStateException("Cannot capture classroom fixture");
    keeper.getInventory().setItem(slot,item);var id=ChaoCarrierItem.storedUUID(item);
    if(!KindergartenContent.enroll(keeper,"i:"+slot+":"+id))throw new IllegalStateException("Actual teacher enrollment failed");
    var body=(ChaoEntity)level.getEntity(id);body.state().setTrait("lesson.active",lesson);body.state().setTraitInt("lesson.remaining",duration);body.state().setTraitInt("lesson.paused",0);body.startBehavior(ChaoEntity.Behavior.LEARN,duration);body.sync();
    var roster=KindergartenRoster.get(keeper.server);var tag=new net.minecraft.nbt.CompoundTag();body.saveWithoutId(tag);roster.students.get(id).entity=tag;roster.setDirty();return body;
  }
  public static void visit(ServerPlayer p){var l=p.server.getLevel(KindergartenContent.DIMENSION);var at=KindergartenContent.Role.TEACHER.anchor;p.teleportTo(l,at.getX()+.5,80,at.getZ()+2.5,0,0);p.hasChangedDimension();}
}
