package com.chaogarden.kindergarten;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ToyContent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Four durable classroom-owned babies: two free-play pupils and two current-class participants. */
public final class KindergartenPupils0257 {
  public static final int COUNT=4;
  private static final Vec3[] HOMES={new Vec3(-22.5,80,-9.5),new Vec3(-18.5,80,-9.5),
      new Vec3(-22.5,80,-14.5),new Vec3(-22.5,80,-12.5)};
  private static final BlockPos[] TOYS={new BlockPos(-23,80,-11),new BlockPos(-19,80,-11)};
  private static final String[] TOY_KINDS={"chao_ball","chao_rattle"};
  // The two former foyer chunks must publish before absence detection too:
  // a0.25.7 saved body migrates into class with its original UUID and payload.
  private static final int[][] CHUNKS={{-2,-1},{-1,0},{0,0}};
  private KindergartenPupils0257(){}
  public static UUID identity(int slot){return UUID.nameUUIDFromBytes((ChaoGarden.MODID+":school_pupil:"+slot).getBytes(StandardCharsets.UTF_8));}
  public static Vec3 home(int slot){return HOMES[Math.max(0,Math.min(COUNT-1,slot))];}
  public static BlockPos toy(int slot){return TOYS[Math.floorMod(slot,2)];}
  public static void authorToys(Map<BlockPos,BlockState> blocks){for(int i=0;i<2;i++)blocks.put(TOYS[i],ToyContent.block(TOY_KINDS[i]).defaultBlockState());}
  public static boolean schoolToy(BlockPos pos){return pos.equals(TOYS[0])||pos.equals(TOYS[1]);}
  public static boolean withinHome(KindergartenPupil0257 pupil){
    return withinPocket(pupil.schoolSlot(),pupil.position());
  }
  /** The whole baby body stays inside the classroom walls, away from its corridor. */
  public static boolean withinClassroom(KindergartenPupil0257 pupil){
    var b=pupil.getBoundingBox();
    return b.minX>=-24.8&&b.maxX<=-13.2&&b.minZ>=-21.8&&b.maxZ<=-8.2
        &&b.minY>=79.5&&b.maxY<=83;
  }
  private static boolean withinPocket(int slot,Vec3 at){
    double x=at.x,z=at.z;
    return at.y>=79.5&&at.y<=82&& (slot<2
        ?x>=(slot==0?-24:-20)&&x<=(slot==0?-21:-17)&&z>=-11.5&&z<=-9.1
        :x>=-24&&x<=-21&&z>=-15.9&&z<=-11.8);
  }
  private static boolean clear(ServerLevel level,ChaoEntity pupil,Vec3 at){
    var base=BlockPos.containing(at);
    return level.hasChunkAt(base)&&level.getBlockState(base.below()).isFaceSturdy(level,base.below(),Direction.UP)
        &&level.noCollision(pupil,pupil.getBoundingBox().move(at.subtract(pupil.position())).deflate(.001));
  }
  /** Full publication precedes absence detection, so reloads never create a second saved body. */
  public static void ensure(ServerLevel level){
    if(!KindergartenContent.inKindergarten(level))return;
    for(int[] c:CHUNKS){level.getChunk(c[0],c[1]);if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(c[0],c[1]))
        ||!level.isPositionEntityTicking(new BlockPos(c[0]*16+8,80,c[1]*16+8)))return;}
    var pupils=new ArrayList<KindergartenPupil0257>();
    for(var actor:level.getAllEntities())if(actor instanceof KindergartenPupil0257 pupil&&pupil.isAlive())pupils.add(pupil);
    for(int slot=0;slot<COUNT;slot++){
      final int index=slot;var matches=pupils.stream().filter(p->p.schoolSlot()==index).sorted(
          Comparator.comparing((KindergartenPupil0257 p)->!p.getUUID().equals(identity(index))).thenComparing(p->p.getUUID().toString())).toList();
      // Canonical UUIDs are reserved for this subtype. Never adopt an arbitrary
      // entity which happens to have the same UUID, and never mint a duplicate.
      var known=com.chaogarden.companion.CompanionContent.findEntity(level.getServer(),identity(slot));
      if(known!=null&&!(known instanceof KindergartenPupil0257))continue;
      KindergartenPupil0257 pupil=known instanceof KindergartenPupil0257 canonical?canonical:null;
      if(pupil!=null&&pupil.level()!=level){
        Vec3 at=safeHome(level,pupil);if(at==null)continue;
        var moved=pupil.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(level,at,Vec3.ZERO,0,0,
            net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        if(!(moved instanceof KindergartenPupil0257 transferred))continue;pupil=transferred;
      }
      if(pupil==null&&!matches.isEmpty())pupil=matches.getFirst();
      if(pupil==null){
        pupil=KindergartenContent.PUPIL.get().create(level);if(pupil==null)continue;
        pupil.schoolSlot(slot);pupil.setUUID(identity(slot));
        var state=ChaoState.egg(new int[]{0,4,5,6}[slot],false,false,0,new java.util.Random(2570+slot));
        state.adult=false;state.happiness=80;state.hunger=80;state.energy=80;pupil.setState(state);
        Vec3 at=safeHome(level,pupil);if(at==null)continue;
        pupil.moveTo(at.x,at.y,at.z,0,0);if(!level.addFreshEntity(pupil))continue;
      }
      for(var duplicate:matches)if(duplicate!=pupil)duplicate.discard();
      pupil.owner=null;pupil.setNoAi(false);pupil.setNoGravity(false);pupil.setInvulnerable(true);pupil.setPersistenceRequired();
      pupil.setCustomNameVisible(false);
      if(!withinHome(pupil)||!level.noCollision(pupil))rescue(level,pupil);
    }
  }
  private static Vec3 safeHome(ServerLevel level,KindergartenPupil0257 pupil){
    var origin=home(pupil.schoolSlot());
    for(double[] d:new double[][]{{0,0},{0,-1},{0,1},{1,0},{-1,0}}){var at=origin.add(d[0],0,d[1]);if(withinPocket(pupil.schoolSlot(),at)&&clear(level,pupil,at))return at;}
    return null;
  }
  private static void rescue(ServerLevel level,KindergartenPupil0257 pupil){
    var at=safeHome(level,pupil);if(at!=null){
      if(pupil.toyPlayingTicks()>0||pupil.getPersistentData().contains("ChaoToyTarget"))ToyContent.endPlaying(pupil,false);
      pupil.getNavigation().stop();pupil.setDeltaMovement(Vec3.ZERO);pupil.moveTo(at.x,at.y,at.z,0,0);pupil.fallDistance=0;pupil.destination=null;pupil.nextAction=0;
    }
  }
  public static ChaoEntity.Behavior lessonBehavior(String lesson){
    if(lesson.equals("song"))return ChaoEntity.Behavior.SING;
    if(lesson.equals("drawing"))return ChaoEntity.Behavior.DRAW;
    if(lesson.endsWith("dance")||lesson.equals("exercise"))return ChaoEntity.Behavior.DANCE;
    return ChaoCareServices.lesson(lesson)==null?ChaoEntity.Behavior.LEARN:ChaoEntity.Behavior.INSTRUMENT;
  }
  public static void rehearse(KindergartenPupil0257 pupil,String lesson){
    pupil.setToyPlaying("",0);pupil.startBehavior(lessonBehavior(lesson),160);pupil.setBehaviorDetail(lesson);
  }
  public static void tick(KindergartenPupil0257 pupil){
    if(!(pupil.level() instanceof ServerLevel level)||!KindergartenContent.inKindergarten(level)){pupil.getNavigation().stop();return;}
    long now=level.getGameTime();
    // Catch a loaded legacy foyer body or an externally displaced child on its
    // first AI tick; normal pocket/collision maintenance remains inexpensive.
    if(!withinClassroom(pupil)||pupil.tickCount%20==0&&(!withinHome(pupil)||!level.noCollision(pupil)))rescue(level,pupil);
    if(pupil.schoolSlot()>=2){
      var at=home(pupil.schoolSlot());
      if(pupil.position().distanceToSqr(at)>.16){pupil.clearBehavior();pupil.getNavigation().moveTo(at.x,80,at.z,1);return;}
      pupil.getNavigation().stop();pupil.getLookControl().setLookAt(-17.5,80.8,-16.5);
      String lesson=ChaoCareServices.currentLesson(now);
      boolean timed=com.chaogarden.toys.InstrumentPerformance.supports(lesson);
      if(!lesson.equals(pupil.behaviorDetail())||pupil.getBehaviorTicks()<(timed?1:25))rehearse(pupil,lesson);
      if(timed)com.chaogarden.toys.InstrumentPerformance.tick(pupil);
      SchoolSinging0258.tick(pupil,lesson);
      if(!timed&&Math.floorMod(now+pupil.schoolSlot()*11,80)==0&&lessonBehavior(lesson)==ChaoEntity.Behavior.INSTRUMENT)
        ToyContent.playPulse(level,pupil.blockPosition(),"chao_"+lesson);
      return;
    }
    if(pupil.toyPlayingTicks()>0){pupil.getNavigation().stop();pupil.getLookControl().setLookAt(Vec3.atCenterOf(toy(pupil.schoolSlot())));return;}
    if(pupil.getPersistentData().contains("ChaoToyTarget"))ToyContent.endPlaying(pupil,false);
    if(now<pupil.nextAction)return;
    if(pupil.destination!=null){
      if(pupil.position().distanceToSqr(pupil.destination)<.25){
        pupil.getNavigation().stop();
        var stand=pupil.destination;pupil.destination=null;
        if(stand.distanceToSqr(Vec3.atBottomCenterOf(toy(pupil.schoolSlot())))<2.5
            &&ToyContent.beginSchoolPlay(pupil,toy(pupil.schoolSlot()),BlockPos.containing(stand)))pupil.nextAction=now+180;
        else pupil.nextAction=now+40;
      }else if(pupil.getNavigation().isDone()){pupil.destination=null;pupil.nextAction=now+20;}
      return;
    }
    var stand=home(pupil.schoolSlot());
    if(pupil.nextAction!=0&&Math.floorMod(now/160+pupil.schoolSlot(),3)!=0)
      stand=new Vec3((pupil.schoolSlot()==0?-23.5:-19.5)+pupil.getRandom().nextInt(3),80,-10.5+pupil.getRandom().nextInt(2));
    if(withinPocket(pupil.schoolSlot(),stand)&&clear(level,pupil,stand)){pupil.destination=stand;pupil.getNavigation().moveTo(stand.x,stand.y,stand.z,1);pupil.nextAction=now+10;}
    else pupil.nextAction=now+30;
  }
}
