package com.chaogarden.core;

import java.util.*;

/** Literal old character records must migrate without losing unrelated saved state. */
public final class RetiredCharacterState020Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static String oldRecord(ChaoState original,String character,boolean immortal,int adultAge){
    String[] fields=original.encode().split(";",25);
    String[] values=fields[11].split(",");values[8]=Integer.toString(adultAge);values[11]=immortal?"1":"0";
    fields[11]=String.join(",",values);
    String[] appearance=fields[21].split(",");appearance[11]="12";fields[21]=String.join(",",appearance);
    fields[23]=character;return String.join(";",fields);
  }
  public static int run(){
    checks=0;
    for(int seed=0;seed<64;seed++){
      var source=ChaoState.random(new Random(seed));source.adult=true;source.ageTicks=987654;
      source.adultAgeTicks=321;source.reincarnations=3;source.primaryType=ChaoState.POWER_TYPE;
      source.align=-1;source.happiness=83;source.bond=72;source.hunger=46;source.energy=57;
      source.eyeColor=11;source.character="normal";source.hatsUnlocked=true;source.flameOrb=true;
      source.setTrait("rare.parts",1);source.animalMask=ChaoState.ALL_ANIMALS;
      source.animalParts[ChaoState.EARS]=6;source.animalParts[ChaoState.WINGS]=17;
      source.setTrait("learned.bell",1);source.setTrait("contest.race.wins",12);
      source.setTrait("family.parent.0.name","QW5jZXN0b3I");source.setTrait("family.parent.0.id","0123456789abcdef");
      source.cosmeticWings=true;source.negativePalette=true;source.brightCoat=true;
      for(int stat=0;stat<5;stat++){source.points[stat]=seed*100+stat;source.progress[stat]=stat+17;source.levels[stat]=stat+40;}
      source.setPersonalitySlider(ChaoPersonality.KINDNESS,77);source.setIllness(ChaoIllness.COLD,-88);
      var canonical=ChaoState.decode(source.encode());
      for(String character:List.of("spartoi","egg"))for(boolean wasImmortal:List.of(false,true)){
        var expected=ChaoState.decode(canonical.encode());expected.character="normal";expected.eyeColor=0;
        expected.immortal=character.equals("spartoi")?false:wasImmortal;
        expected.adultAgeTicks=character.equals("spartoi")&&wasImmortal?0:9999999;
        var migrated=ChaoState.decode(oldRecord(canonical,character,wasImmortal,9999999));
        check(migrated.encode().equals(expected.encode()),"Retired "+character+" migration changed unrelated saved state");
        check(migrated.lineageId().equals(canonical.lineageId()),"Retirement changed stable lineage identity");
        check(migrated.traitEntries().equals(canonical.traitEntries()),"Retirement changed learning, family or earned rewards");
        migrated.adultAgeTicks=74;String frozen=migrated.encode();
        check(migrated.adultAgeTicks==74&&frozen.equals(ChaoState.decode(frozen).encode()),"Retired migration replayed after conversion");
      }
    }
    var a=new ChaoState();var b=new ChaoState();a.adult=b.adult=true;
    for(int seed=0;seed<128;seed++){
      var forced=new Random(seed){@Override public int nextInt(int bound){return bound==100?0:super.nextInt(bound);}};
      var child=ChaoState.breed(a,b,forced);
      check(child.character.equals("normal"),"Removed one-percent Egg character was generated");
      check(ChaoState.decode(child.encode()).character.equals("normal"),"Ordinary birth changed into a retired character after reload");
    }
    check(ChaoState.normalizeCharacter("spartoi").equals("normal")&&ChaoState.normalizeCharacter("egg").equals("normal"),"Public character normalization exposes retired forms");
    for(String active:List.of("normal","amy","tails","knuckles","omochao"))check(ChaoState.normalizeCharacter(active).equals(active),"Retirement removed an active character");
    var direct=new ChaoState();direct.adult=true;direct.immortal=true;direct.character="spartoi";direct.eyeColor=12;direct.adultAgeTicks=Integer.MAX_VALUE;
    direct.encode();check(direct.character.equals("normal")&&!direct.immortal&&direct.adultAgeTicks==0&&direct.eyeColor==0,"Direct legacy state was not safely migrated before saving");
    check(direct.canBreed(),"Retired Spartoi remained an unbreedable removed character");
    for(int lens=0;lens<12;lens++){var s=new ChaoState();s.eyeColor=lens;check(ChaoState.decode(s.encode()).eyeColor==lens,"Active iris changed during retirement");}
    return checks;
  }
  public static void main(String[] args){System.out.println("Retired character state020: "+run()+" checks passed");}
}
