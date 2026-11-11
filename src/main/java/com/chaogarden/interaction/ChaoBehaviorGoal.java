package com.chaogarden.interaction;

import com.chaogarden.GardenConfig;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPersonality;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Real need-driven garden actions, including two-body social sessions.
 * Source thresholds use the saved emotion scales. Durations and path radii are
 * Minecraft adaptations; every action can be interrupted without a reward.
 */
public final class ChaoBehaviorGoal extends Goal {
  private final ChaoEntity chao;
  private Behavior selected=Behavior.NONE;
  private Entity target;
  private Vec3 walkTarget;
  private long nextCheck,approachUntil;
  private int duration;
  private boolean approaching,effectApplied;
  private String detail="";

  public ChaoBehaviorGoal(ChaoEntity chao) {
    this.chao=chao;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
  }
  private boolean eligible() {
    return chao.isAlive()&&!chao.isNoAi()&&!chao.isCocooned()&&!chao.isResting()
        &&!chao.isHandlingItem()&&!chao.isEating()&&!chao.isStaying()&&!chao.isInWater()
        &&(chao.onGround()||chao.isBehaving())&&chao.getAffectionTicks()==0&&chao.toyPlayingTicks()==0
        &&!ActivityManager.isBusy(chao)&&!CompanionContent.isManaged(chao);
  }
  @Override public boolean canUse() {
    if(!eligible())return false;
    if(chao.getBehavior()==Behavior.SHAKE_TREE||chao.getBehavior()==Behavior.LEARN)return false;
    if(chao.isBehaving()) {selected=chao.getBehavior();return true;}
    long now=chao.level().getGameTime();
    if(now<nextCheck||now<chao.nextBehaviorTime())return false;
    nextCheck=now+20+chao.getRandom().nextInt(41);
    selected=Behavior.NONE;target=null;detail="";walkTarget=null;
    var s=chao.state();
    if(s.isIll()&&s.emotion(ChaoEmotion.CONDITION)<5000) {
      for(int i=0;i<6;i++)if(s.illness(i)<0) {
        selected=switch(i){case ChaoIllness.COUGH->Behavior.COUGH;case ChaoIllness.COLD->Behavior.SNEEZE;
          case ChaoIllness.RASH->Behavior.SCRATCH;case ChaoIllness.RUNNY_NOSE->Behavior.RUNNY_NOSE;
          case ChaoIllness.HICCUPS->Behavior.HICCUP;default->Behavior.STOMACH_ACHE;};
        duration=36;return true;
      }
    }
    if(s.emotion(ChaoEmotion.FEAR)>=115) {
      selected=s.emotion(ChaoEmotion.FEAR)>=160?Behavior.FLEE:Behavior.COWER;
      target=chao.getLastHurtByMob();duration=80;return true;
    }
    if(s.emotion(ChaoEmotion.CRY)>=100) {
      selected=Behavior.CRY;duration=ChaoPreferences.hasPersonality(s,ChaoPreferences.CRY_BABY)?160:90;return true;
    }
    if(s.emotion(ChaoEmotion.ANGER)>=105) {
      Player disliked=chao.owner==null?null:chao.level().getPlayerByUUID(chao.owner);
      if(s.align<=-.5f&&s.bond<0&&s.emotion(ChaoEmotion.ANGER)>=150
          &&disliked!=null&&disliked.isAlive()&&chao.distanceToSqr(disliked)<64) {
        target=disliked;selected=Behavior.BULLY;duration=60;return true;
      }
      if(GardenConfig.BULLY_CHAO.get()&&s.personalitySlider(ChaoPersonality.KINDNESS)<-54
          &&s.emotion(ChaoEmotion.ANGER)>=150) {
        target=nearbyChao().stream().filter(c->c!=chao&&c.canBeginSocial()&&c.getHealth()>1)
            .min(java.util.Comparator.comparingDouble(chao::distanceToSqr)).orElse(null);
        if(target!=null){selected=Behavior.BULLY;duration=60;return true;}
      }
      selected=Behavior.TANTRUM;duration=90;return true;
    }
    if(s.personalitySlider(ChaoPersonality.KINDNESS)>54) {
      target=nearbyChao().stream().filter(c->c.getBehavior()==Behavior.BULLY).findFirst().orElse(null);
      if(target!=null){selected=Behavior.INTERVENE;duration=60;return true;}
    }
    if(s.emotion(ChaoEmotion.SURPRISE)>120) {
      selected=Behavior.DIZZY;duration=ChaoPreferences.recoveryTicks(s,80);return true;
    }
    if(s.emotion(ChaoEmotion.TIREDNESS)>6000) {
      selected=Behavior.GROGGY;duration=ChaoPreferences.recoveryTicks(s,80);return true;
    }
    if(s.emotion(ChaoEmotion.JOY)>115) {selected=Behavior.CHEER;duration=80;return true;}
    if(chao.canStartBirthday()){selected=Behavior.BIRTHDAY;duration=500;return true;}
    if(chao.canBeginSocial()) {
      target=nearbyChao().stream().filter(c->c!=chao&&c.getBehavior()==Behavior.BIRTHDAY&&c.getBehaviorTicks()>80
          &&!c.birthdayPartyKey().equals(s.trait("birthday.party")))
          .min(java.util.Comparator.comparingDouble(chao::distanceToSqr)).orElse(null);
      if(target!=null){selected=Behavior.PARTY;duration=160;return true;}
    }
    if(s.emotion(ChaoEmotion.RELAX)>=70&&s.hunger>=30&&s.energy>=20) {
      selected=Behavior.RELAX;duration=Math.max(40,120-s.personalitySlider(ChaoPersonality.FICKLE)/2);return true;
    }
    if(s.emotion(ChaoEmotion.LONELY)>=4000&&chao.level().getGameTime()>=chao.nextSocialTime()
        &&s.hunger>=25&&s.energy>=25) {
      Player keeper=chao.owner==null?null:chao.level().getPlayerByUUID(chao.owner);
      if(keeper!=null&&keeper.isAlive()&&chao.distanceToSqr(keeper)<64&&s.bond>=20
          &&s.personalitySlider(ChaoPersonality.SOLITUDE)<55&&chao.getRandom().nextBoolean()) {
        target=keeper;selected=s.bond>=60?Behavior.NUZZLE:Behavior.GREET;duration=80;return true;
      }
      int chance=ChaoPreferences.socialChanceDenominator(s);
      if(chance>0&&chao.getRandom().nextInt(chance)==0) {
        target=nearbyChao().stream().filter(c->c!=chao&&!c.isNoAi()&&c.canBeginSocial())
            .min(java.util.Comparator.comparingDouble(chao::distanceToSqr)).orElse(null);
        if(target instanceof ChaoEntity other) {
          int talks=chao.completedConversations(other.getUUID());
          selected=talks>=10&&chao.getRandom().nextInt(3)==0
              ?chao.getRandom().nextBoolean()?Behavior.HUG:Behavior.HOLD_HANDS
              :talks>=5&&chao.getRandom().nextInt(3)==0?Behavior.GOSSIP:Behavior.TALK;
          duration=selected==Behavior.HOLD_HANDS?180:selected==Behavior.HUG?100+chao.getRandom().nextInt(81):120;
          return true;
        }
      }
    }
    if(s.emotion(ChaoEmotion.BOREDOM)>=4000&&s.hunger>=25&&s.energy>=25&&chao.getRandom().nextInt(3)==0) {
      List<Behavior> known=new ArrayList<>();
      if(ChaoCareServices.knowsLesson(chao,"song"))known.add(Behavior.SING);
      if(ChaoCareServices.knowsLesson(chao,"drawing"))known.add(Behavior.DRAW);
      var dances=List.of("shake_dance","spin_dance","step_dance","gogo_dance","exercise").stream()
          .filter(id->ChaoCareServices.knowsLesson(chao,id)).toList();
      if(!dances.isEmpty())known.add(Behavior.DANCE);
      if(ChaoCareServices.allDancesKnown(s))known.add(Behavior.BREAK_DANCE);
      var instruments=List.of("bell","castanets","cymbals","drum","flute","maracas","tambourine",
          "trumpet","triangle","guitar","accordion").stream().filter(id->ChaoCareServices.knowsLesson(chao,id)).toList();
      if(!instruments.isEmpty())known.add(Behavior.INSTRUMENT);
      if(!known.isEmpty()) {
        selected=known.get(chao.getRandom().nextInt(known.size()));duration=160;
        if(selected==Behavior.DANCE)detail=dances.isEmpty()?"exercise":dances.get(chao.getRandom().nextInt(dances.size()));
        if(selected==Behavior.INSTRUMENT) {
          detail=instruments.get(chao.getRandom().nextInt(instruments.size()));
          target=nearbyChao().stream().filter(c->c!=chao&&c.getBehavior()==Behavior.INSTRUMENT&&c.getBehaviorTicks()>60)
              .min(java.util.Comparator.comparingDouble(chao::distanceToSqr)).orElse(null);
          if(target instanceof ChaoEntity leader)duration=leader.getBehaviorTicks();
        }
        return true;
      }
    }
    return false;
  }
  private List<ChaoEntity> nearbyChao() {
    return chao.level().getEntitiesOfClass(ChaoEntity.class,chao.getBoundingBox().inflate(7),c->c.isAlive()&&!c.isRemoved());
  }
  @Override public void start() {
    effectApplied=false;approaching=false;
    if(chao.isBehaving()) {
      selected=chao.getBehavior();effectApplied=chao.getBehaviorProgress(0)>.5f;
      var partner=chao.behaviorPartner();target=partner==null?null:((ServerLevel)chao.level()).getEntity(partner);
      return;
    }
    approaching=target!=null&&selected!=Behavior.FLEE;approachUntil=chao.level().getGameTime()+240;
    if(approaching)navigate();else beginSingle();
  }
  private void beginSingle() {
    if(selected==Behavior.BIRTHDAY) {
      if(!chao.startBirthdayParty())chao.deferBehavior(100);
      approaching=false;return;
    }
    if(selected==Behavior.PARTY&&target instanceof ChaoEntity host) {
      if(!chao.joinBirthdayParty(host))chao.deferBehavior(100);
      approaching=false;return;
    }
    if(selected==Behavior.INSTRUMENT&&target instanceof ChaoEntity leader) {
      if(leader.getBehavior()!=Behavior.INSTRUMENT||leader.getBehaviorTicks()<40) {
        target=null;chao.deferBehavior(80);approaching=false;return;
      }
      duration=leader.getBehaviorTicks();
    }
    chao.startBehavior(selected,duration);chao.setBehaviorDetail(detail);
    if(target!=null)chao.setBehaviorPartner(target.getUUID());
    approaching=false;
  }
  private void navigate() {
    if(target!=null)chao.getNavigation().moveTo(target,.8);
  }
  @Override public boolean canContinueToUse() {
    if(!eligible())return false;
    if(approaching)return target!=null&&target.isAlive()&&chao.distanceToSqr(target)<100
        &&chao.level().getGameTime()<approachUntil
        &&(!(target instanceof ChaoEntity other)||!ChaoEntity.isSocialBehavior(selected)||other.canBeginSocial());
    return chao.isBehaving();
  }
  @Override public void tick() {
    if(approaching) {
      if(target==null)return;
      chao.getLookControl().setLookAt(target,30,20);
      if(chao.distanceToSqr(target)<=4) {
        if(ChaoEntity.isSocialBehavior(selected)&&target instanceof ChaoEntity other) {
          if(chao.startSocialSession(other,selected,duration))approaching=false;
        } else beginSingle();
      } else if(chao.getNavigation().isDone())navigate();
      return;
    }
    Behavior action=chao.getBehavior();if(action==Behavior.NONE)return;
    if(action==Behavior.INSTRUMENT&&com.chaogarden.toys.InstrumentPerformance.supports(chao.behaviorDetail())) {
      if(target instanceof ChaoEntity leader&&leader.getBehavior()!=Behavior.INSTRUMENT){chao.clearBehavior();return;}
      com.chaogarden.toys.InstrumentPerformance.tick(chao);
    }
    if(action==Behavior.PARTY&&(!(target instanceof ChaoEntity host)||!host.isAlive()||host.getBehavior()!=Behavior.BIRTHDAY)) {
      chao.clearBehavior();return;
    }
    if(ChaoEntity.isSocialBehavior(action)) {
      if(!(target instanceof ChaoEntity other)||!other.isAlive()||!chao.getUUID().equals(other.behaviorPartner())
          ||other.getBehavior()!=action||chao.distanceToSqr(other)>25){chao.clearBehavior();return;}
      chao.getLookControl().setLookAt(other,30,20);
      if(action==Behavior.HOLD_HANDS) {
        if(chao.getUUID().compareTo(other.getUUID())<0) {
          if(walkTarget==null)walkTarget=chao.position().add(2+chao.getRandom().nextDouble()*2,0,2-chao.getRandom().nextDouble()*4);
          if(chao.getNavigation().isDone())chao.getNavigation().moveTo(walkTarget.x,walkTarget.y,walkTarget.z,.55);
        } else chao.getNavigation().moveTo(other.getX()+.65,other.getY(),other.getZ(),.65);
      } else if(action==Behavior.HUG&&chao.distanceToSqr(other)>1.5)chao.getNavigation().moveTo(other,.45);
      else chao.getNavigation().stop();
      return;
    }
    if(action==Behavior.FLEE) {
      if(chao.getNavigation().isDone()) {
        Entity threat=target!=null?target:chao.level().getNearestPlayer(chao,7);
        Vec3 away=threat==null?new Vec3(chao.getRandom().nextDouble()-.5,0,chao.getRandom().nextDouble()-.5)
            :chao.position().subtract(threat.position()).multiply(1,0,1);
        away=away.lengthSqr()<.01?new Vec3(1,0,0):away.normalize();
        Vec3 to=chao.position().add(away.scale(4));chao.getNavigation().moveTo(to.x,to.y,to.z,1.1);
      }
    } else if(action==Behavior.NUZZLE&&target instanceof Player player) {
      if(chao.distanceToSqr(player)>2)chao.getNavigation().moveTo(player,.6);else chao.getNavigation().stop();
      chao.getLookControl().setLookAt(player,30,20);
    } else {
      chao.getNavigation().stop();
      if(target!=null)chao.getLookControl().setLookAt(target,30,20);
    }
    if((action==Behavior.CHEER||action==Behavior.PARTY)&&chao.tickCount%20==0&&chao.onGround())chao.getJumpControl().jump();
    if(!effectApplied&&chao.getBehaviorProgress(0)>=.5f) {
      effectApplied=true;
      if(action==Behavior.BULLY&&target instanceof LivingEntity other&&other.isAlive()
          &&chao.distanceToSqr(other)<4&&other.getHealth()>1) {
        if(other.hurt(chao.damageSources().mobAttack(chao),.5f))
          chao.state().addEmotion(ChaoEmotion.ANGER,other instanceof Player?-10:-35);
        chao.sync();
      }
      if(action==Behavior.INTERVENE&&target instanceof ChaoEntity bully&&bully.getBehavior()==Behavior.BULLY) {
        var victimId=bully.behaviorPartner();var world=(ServerLevel)chao.level();
        if(victimId!=null&&world.getEntity(victimId) instanceof ChaoEntity victim) {
          victim.state().addEmotion(ChaoEmotion.FEAR,-75);victim.state().onSocialInteraction(true);victim.sync();
        }
        bully.clearBehavior();bully.state().addEmotion(ChaoEmotion.ANGER,-100);bully.deferBehavior(200);bully.sync();
        chao.state().onSocialInteraction(true);chao.sync();
      }
    }
    if(chao.tickCount%20==0&&chao.level() instanceof ServerLevel world) {
      if(action==Behavior.INSTRUMENT&&!com.chaogarden.toys.InstrumentPerformance.supports(chao.behaviorDetail())) {
        if(target instanceof ChaoEntity leader&&leader.getBehavior()!=Behavior.INSTRUMENT) {chao.clearBehavior();return;}
        world.sendParticles(ParticleTypes.NOTE,chao.getX(),chao.getY()+.8,chao.getZ(),1,.1,.1,.1,0);
        chao.playSound(switch(chao.behaviorDetail()) {
          case "drum","cymbals","tambourine","castanets"->SoundEvents.NOTE_BLOCK_BASEDRUM.value();
          case "flute","accordion"->SoundEvents.NOTE_BLOCK_FLUTE.value();
          case "bell","triangle"->SoundEvents.NOTE_BLOCK_BELL.value();
          case "guitar"->SoundEvents.NOTE_BLOCK_GUITAR.value();
          default->SoundEvents.NOTE_BLOCK_DIDGERIDOO.value();
        },.3f,1+(chao.tickCount/20%4)*.12f);
      } else if(action==Behavior.SING) {
        world.sendParticles(ParticleTypes.NOTE,chao.getX(),chao.getY()+.8,chao.getZ(),1,.1,.1,.1,0);
        chao.playSound(SoundEvents.NOTE_BLOCK_HARP.value(),.28f,1+(chao.tickCount/20%4)*.12f);
        if(ChaoCareServices.lessonLevel(chao.state(),"song")<=1)for(var other:nearbyChao())if(other!=chao&&chao.distanceToSqr(other)<9)
          other.state().addEmotion(ChaoEmotion.ANGER,5);
      } else if(action==Behavior.CRY)world.sendParticles(ParticleTypes.SPLASH,chao.getX(),chao.getY()+.6,chao.getZ(),3,.14,.02,.14,0);
      else if(action==Behavior.COUGH||action==Behavior.SNEEZE||action==Behavior.HICCUP)
        world.sendParticles(ParticleTypes.CLOUD,chao.getX(),chao.getY()+.6,chao.getZ(),1,.05,.03,.05,.01);
    }
  }
  @Override public void stop() {
    chao.getNavigation().stop();
    if(approaching){chao.deferBehavior(80);approaching=false;}
    // Timed completion is committed by the entity after AI processing, so the
    // order in which two partners tick cannot drop or double a social reward.
    if(!eligible())chao.clearBehavior();
    target=null;walkTarget=null;
  }
  @Override public boolean requiresUpdateEveryTick(){return true;}
}
