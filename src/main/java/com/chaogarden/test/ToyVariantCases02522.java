package com.chaogarden.test;

import com.chaogarden.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Shared descriptions for native sessions and the graphical variant contact sheets. */
public final class ToyVariantCases02522 {
  public record Case(String name,Consumer<ChaoState> configure){
    public ChaoState state(){var s=new ChaoState();s.adult=true;s.happiness=20;s.hunger=90;s.energy=100;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;configure.accept(s);if(s.adult)s.points[ChaoState.RUN]=667;return s;}
  }
  private static void add(List<Case> c,String name,Consumer<ChaoState> change){c.add(new Case(name,change));}
  public static List<Case> all(){
    var c=new ArrayList<Case>();
    for(int alignment=-1;alignment<=1;alignment++){
      final int a=alignment;String prefix=a<0?"dark":a>0?"hero":"neutral";
      add(c,"baby_"+prefix,s->{s.adult=false;s.align=a;});
      for(int type=0;type<5;type++){final int t=type;add(c,"adult_"+prefix+"_type"+t,s->{s.align=a;s.primaryType=t;s.setEvolutionStrength(1);});}
    }
    for(int[] pair:new int[][]{{1,3},{3,1},{3,2},{2,3},{2,4},{4,2},{4,1},{1,4}}){
      int first=pair[0],second=pair[1];add(c,"mixed_"+first+"_"+second,s->{s.primaryType=first;s.secondaryType=second;s.swimFly=second==1?-1:second==2?1:0;s.runPower=second==3?-1:second==4?1:0;s.setEvolutionStrength(1);});
    }
    for(int alignment=-1;alignment<=1;alignment++){int a=alignment;add(c,"chaos_"+(a+1),s->{s.immortal=true;s.align=a;});}
    for(String character:List.of("tails","knuckles","amy"))for(boolean adult:new boolean[]{false,true})
      add(c,character+"_"+(adult?"adult":"baby"),s->{s.character=character;s.adult=adult;});
    add(c,"omochao_adult",s->{s.character="omochao";OmochaoRules.normalize(s);});
    for(int animal=0;animal<ChaoState.ANIMALS.length;animal++)if((ChaoState.animalSlots(animal)&1)!=0){
      final int id=animal;
      for(boolean adult:new boolean[]{false,true})add(c,"animal_"+ChaoState.ANIMALS[id]+"_"+(adult?"adult":"baby"),s->{
        s.adult=adult;for(int slot=0;slot<s.animalParts.length;slot++)if((ChaoState.animalSlots(id)&(1<<slot))!=0)s.animalParts[slot]=id;
      });
    }
    for(String variant:List.of("nether","end"))for(boolean adult:new boolean[]{false,true})
      add(c,variant+"_"+(adult?"adult":"baby"),s->{s.variant=variant;s.adult=adult;});
    add(c,"pink_mono_baby",s->{s.adult=false;s.expressedColor=6;s.twoTone=false;});
    add(c,"blue_two_adult",s->{s.expressedColor=4;s.twoTone=true;});
    add(c,"shiny_yellow",s->{s.expressedColor=3;s.shiny=true;s.twoTone=false;});
    add(c,"jewel_ruby",s->{s.expressedJewel=3;s.twoTone=false;});
    add(c,"shiny_moon",s->{s.expressedJewel=13;s.shiny=true;});
    add(c,"negative_bright",s->{s.expressedColor=1;s.negativePalette=true;s.brightCoat=true;});
    add(c,"ghost_flame",s->{s.ghostBody=true;s.flameOrb=true;});
    add(c,"hero_brush",s->{s.align=1;s.primaryType=3;s.heroBrushType=2;s.cosmeticWings=true;});
    return List.copyOf(c);
  }
}
