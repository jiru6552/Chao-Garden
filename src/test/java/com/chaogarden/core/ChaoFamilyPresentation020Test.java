package com.chaogarden.core;

import java.util.*;

/** Names and readable DNA must survive a real two-generation save sequence. */
public final class ChaoFamilyPresentation020Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static void main(String[] args){System.out.println("Family presentation: "+run()+" checks passed");}
  public static int run(){
    checks=0;
    var mother=ChaoState.random(new Random(21));var father=ChaoState.random(new Random(22));
    mother.adult=father.adult=true;
    var child=ChaoState.breed(mother,father,new Random(23));
    String motherId=child.trait("family.parent.0.id"),motherDna=child.trait("family.parent.0.dna");
    check(child.familyName("family.parent.0").isEmpty(),"An old record must not invent a parent's name from its ID");
    child.recordParentNames("  Maple  ","カオス 🌟");
    check(child.familyName("family.parent.0").equals("Maple"),"A real parent name is saved separately from lineage");
    check(child.familyName("family.parent.1").equals("カオス 🌟"),"Unicode and supplementary characters survive the ASCII trait codec");
    check(child.trait("family.parent.0.id").equals(motherId)&&child.trait("family.parent.0.dna").equals(motherDna),"Adding names must not rewrite ancestry or genes");
    child=ChaoState.decode(child.encode());
    check(child.familyName("family.parent.1").equals("カオス 🌟"),"Reload preserves the actual parent name");
    var partner=ChaoState.random(new Random(24));child.adult=partner.adult=true;
    var grandchild=ChaoState.breed(child,partner,new Random(25));grandchild.recordParentNames("Willow","Fern");
    grandchild=ChaoState.decode(grandchild.encode());
    check(grandchild.familyName("family.parent.0").equals("Willow")&&grandchild.familyName("family.parent.1").equals("Fern"),"Each generation records its own real parents");
    check(grandchild.familyName("family.grandparent.0").equals("Maple")&&grandchild.familyName("family.grandparent.1").equals("カオス 🌟"),"Recorded grandparent names follow their exact saved lineage positions");
    check(grandchild.familyName("family.grandparent.2").isEmpty()&&grandchild.familyName("family.grandparent.3").isEmpty(),"Missing grandparents remain unknown");
    check(grandchild.trait("family.grandparent.0.id").equals(mother.lineageId())&&grandchild.trait("family.grandparent.0.dna").equals(motherDna),"Names never replace the stable ancestor identity or compact genotype");
    for(String name:List.of("Cedar","Étoile","芽衣","Chao 🧊","\nMaple\t","葉".repeat(100),"🌟".repeat(100))){
      String encoded=ChaoState.encodeFamilyName(name),decoded=ChaoState.decodeFamilyName(encoded);
      check(encoded.length()<=128&&encoded.chars().allMatch(c->c>=32&&c<=126),"Family name fits the existing bounded ASCII save field");
      check(!decoded.contains("�")&&decoded.codePoints().noneMatch(Character::isISOControl),"Truncation never splits UTF-8 or retains control characters");
      check(ChaoState.encodeFamilyName(decoded).equals(encoded),"The name codec has an exact stable roundtrip");
    }
    for(String bad:List.of("%broken","_w","YQ=", "YQ==".repeat(40)))
      check(ChaoState.decodeFamilyName(bad).isEmpty(),"Corrupt/oversized family text must not leak raw encoded bytes into the chart");
    var genome=new ChaoState();genome.colorGenes=new int[]{2,4};genome.jewelGenes=new int[]{3,13};
    genome.gradeA=new int[]{5,4,3,2,1};genome.gradeB=new int[]{1,2,3,4,5};
    var text=ChaoState.describeDnaSummary(genome.dnaSummary());
    check(text.size()==9&&text.getFirst().equals("Colour alleles: Red / Blue"),"Family DNA presents actual colour names");
    check(text.get(3).equals("Jewel alleles: Ruby / Moon"),"Jewel indices are shown as named coats");
    check(text.get(4).contains("Swim S/D")&&text.get(4).contains("Stamina D/S")&&!text.get(4).contains("54321"),"Ability DNA is separated into named skills and grade letters");
    check(text.get(8).contains(ChaoPersonality.NAMES[0]+" 2/2"),"Personality pairs retain real values and identify their axes");
    check(ChaoState.describeDnaSummary("C:999/0 G:abc").stream().allMatch(x->x.endsWith("not recorded")),"Malformed stored DNA is safely described instead of indexed blindly");
    // Immortality is shared by rare characters; it must not change their
    // identity into an alignment-dependent Chaos phenotype after reload.
    String[] characters={"amy","omochao","tails","knuckles"},labels={"Amy Chao","Omochao","Tails Chao","Knuckles Chao"};
    for(int i=0;i<characters.length;i++) {
      var rare=new ChaoState();rare.character=characters[i];rare.adult=true;rare.immortal=true;
      for(float alignment:new float[]{-1,0,1}) {
        rare.align=alignment;var loaded=ChaoState.decode(rare.encode());
        check(loaded.stageName().equals("Adult")&&loaded.typeName().equals(labels[i]),"Reloaded immortal characters keep their identity across all alignment extremes");
      }
    }
    for(float alignment:new float[]{-1,0,1}) {
      var chaos=new ChaoState();chaos.adult=true;chaos.immortal=true;chaos.align=alignment;chaos=ChaoState.decode(chaos.encode());
      check(chaos.stageName().equals("Chaos Chao")&&chaos.typeName().equals(alignment<0?"Devil":alignment>0?"Angel":"Light"),"Actual genetic Chaos retain their proper alignment phenotype after reload");
    }
    return checks;
  }
}
