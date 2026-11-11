#!/usr/bin/env python3
"""Exercise the current breeding publication methods with API stubs, not Minecraft."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
text = (root / 'src/main/java/com/chaogarden/entity/ChaoEntity.java').read_text()

def method(start):
    begin = text.index(start)
    end = text.index('{', begin) + 1
    depth = 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[begin:end].replace('com.chaogarden.ChaoVariants', 'ChaoVariants')

can_mate = method('  public boolean canMate(Animal other)')
spawn = method('  public void spawnChildFromBreeding(ServerLevel world, Animal mate)')
source = '''import java.util.*;
class BreedingCheck {
 static class Animal {}
 static class ChaoState {static ChaoState breed(Object... args){return new ChaoState();}void recordParentNames(String a,String b){}}
 static class ChaoVariants {static void normalizeNewCreature(ChaoState c){}}
 static class GardenConfig {static final Option EXPANDED_RARE_ACTIONS=new Option(),NEWBORN_EYE_CHANCE=new Option();static class Option {boolean get(){return false;}}}
 static class ParticleTypes {static final Object HEART=new Object();}
 static class ExperienceOrb {ExperienceOrb(ServerLevel l,double x,double y,double z,int count){}}
 static class ChaoEggEntity {UUID owner;void setState(ChaoState s){}void moveTo(double x,double y,double z,int yaw,int pitch){}}
 static class ChaoGarden {static final Holder CHAO_EGG=new Holder();static class Holder {Factory get(){return new Factory();}}static class Factory {ChaoEggEntity create(ServerLevel l){if(l.creationCallback!=null)l.creationCallback.run();return l.nullEgg?null:new ChaoEggEntity();}}}
 static class Name {String getString(){return "Parent";}}
 static class ServerLevel {
  boolean nullEgg,rejectEgg,rejectXp,throwEgg;int eggAttempts,xpAttempts,publishedEggs,publishedXp;
  Runnable creationCallback,eggCallback,xpCallback;
  boolean addFreshEntity(Object e){
   if(e instanceof ChaoEggEntity){eggAttempts++;if(eggCallback!=null)eggCallback.run();if(throwEgg)throw new IllegalStateException("injected publication error");if(rejectEgg)return false;publishedEggs++;}
   else {xpAttempts++;if(xpCallback!=null)xpCallback.run();if(rejectXp)return false;publishedXp++;}
   return true;
  }
 }
 static class ChaoEntity extends Animal {
  boolean publishingBredEgg,love=true,eligible=true;int breedingCooldown,hearts,resets;ChaoState state=new ChaoState();Random simulationRandom=new Random(1),random=new Random(2);UUID owner=UUID.randomUUID();
  boolean mayBreed(){return eligible;}boolean isInLove(){return love;}void resetLove(){love=false;resets++;}Name getName(){return new Name();}double getX(){return 0;}double getY(){return 0;}double getZ(){return 0;}void particles(Object type,int count){hearts+=count;}
''' + can_mate + '\n' + spawn + '''
 }
 static int checks;
 static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void unchanged(ChaoEntity a,ChaoEntity b,ServerLevel l){
  check(a.canMate(b)&&b.canMate(a),"failed attempt lost eligibility or reservation");
  check(a.love&&b.love&&a.breedingCooldown==0&&b.breedingCooldown==0,"premature love/cooldown commit");
  check(a.resets==0&&b.resets==0&&a.hearts==0&&b.hearts==0&&l.xpAttempts==0,"premature rewards");
  check(l.publishedEggs==0,"failed attempt published an egg");
 }
 static void committed(ChaoEntity a,ChaoEntity b,ServerLevel l){
  check(!a.canMate(b)&&!b.canMate(a),"success left parents available");
  check(!a.love&&!b.love&&a.breedingCooldown==6000&&b.breedingCooldown==6000,"missing parent commit");
  check(a.resets==1&&b.resets==1&&a.hearts==12&&b.hearts==0&&l.xpAttempts==1&&l.publishedEggs==1,"duplicate/missing reward or offspring");
  check(!a.publishingBredEgg&&!b.publishingBredEgg,"reservation leaked after success");
 }
 public static void main(String[] args){
  for(int failure=0;failure<3;failure++){
   var level=new ServerLevel();var a=new ChaoEntity();var b=new ChaoEntity();var third=new ChaoEntity();
   Runnable reenter=()->{
    check(!a.canMate(b)&&!b.canMate(a)&&!a.canMate(third)&&!third.canMate(b),"pair not reserved for callback");
    a.spawnChildFromBreeding(level,b);b.spawnChildFromBreeding(level,a);a.spawnChildFromBreeding(level,third);third.spawnChildFromBreeding(level,b);
   };
   level.creationCallback=reenter;level.eggCallback=reenter;level.xpCallback=reenter;
   level.nullEgg=failure==0;level.rejectEgg=failure==1;level.throwEgg=failure==2;
   boolean thrown=false;try{a.spawnChildFromBreeding(level,b);}catch(IllegalStateException expected){thrown=true;}
   check(thrown==(failure==2),"publication exception handling changed");unchanged(a,b,level);
   level.nullEgg=level.rejectEgg=level.throwEgg=false;a.spawnChildFromBreeding(level,b);committed(a,b,level);
   int attempts=level.eggAttempts;
   a.spawnChildFromBreeding(level,b);b.spawnChildFromBreeding(level,a);third.spawnChildFromBreeding(level,b);committed(a,b,level);
   check(level.eggAttempts==attempts&&third.love&&third.breedingCooldown==0,"late callbacks consumed third parent or duplicated egg");
  }
  var level=new ServerLevel();var a=new ChaoEntity();var b=new ChaoEntity();level.rejectXp=true;
  a.spawnChildFromBreeding(level,b);committed(a,b,level);a.spawnChildFromBreeding(level,b);check(level.publishedEggs==1&&level.xpAttempts==1&&level.publishedXp==0,"rejected XP caused breeding replay");
  a=new ChaoEntity();b=new ChaoEntity();a.eligible=false;a.spawnChildFromBreeding(new ServerLevel(),b);check(a.resets==0&&b.resets==0,"ineligible parent committed");
  a=new ChaoEntity();a.spawnChildFromBreeding(new ServerLevel(),a);check(a.resets==0,"self-breeding committed");
  System.out.println(checks+" isolated breeding checks passed using extracted production methods; Minecraft APIs are stubs, not a native test.");
 }
}
'''

if __name__ == '__main__':
    from isolated_java_support import run_java_source
    run_java_source('BreedingCheck', source)
