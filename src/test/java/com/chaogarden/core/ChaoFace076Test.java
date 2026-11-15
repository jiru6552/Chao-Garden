package com.chaogarden.core;

import com.chaogarden.core.ChaoFaceRules076.Eyes;
import com.chaogarden.core.ChaoFaceRules076.Mouth;
import com.chaogarden.core.ChaoFaceRules076.Resolved;
import com.chaogarden.core.ChaoFaceRules076.Signals;
import com.chaogarden.core.ChaoFaceRules076.Source;
import com.chaogarden.core.ChaoFaceRules076.Style;
import com.chaogarden.core.ChaoFaceRules076.Symptom;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Literal pre-update RNG fixtures and observable fixed-face contracts. */
public final class ChaoFace076Test {
  private static int checks;
  private record Frozen(String name,long nextRandom,String encoded) {}
  private static void check(boolean value,String message) {
    checks++;
    if(!value)throw new AssertionError(message);
  }

  // Deliberately literal v2/v4 saves: the current encoder cannot update their expectations.
  private static final String V2=
      "2;2,14;1,0;0,1;0,12;0,1,2,3,4;5,4,3,2,1;5,1,3,2,4;99,3,5,6,7;9,8,7,6,5;"
      +"9999,321,456,222,987;14,12,1,1,2,320,411,6178,2222,3,1,0,0,2,3,88,66,77,55,1,"
      +"294912,32768,1,0,1,11,12,13,44;15,-1,15,-1,-1,-1,-1,-1;"
      +"0x1.8p-1,-0x1.0p-2,0x1.0p-1;-123456789012345678";
  private static final String V4=
      "4;2,14;1,0;0,1;0,12;0,1,2,3,4;5,4,3,2,1;5,1,3,2,4;99,3,5,6,7;9,8,7,6,5;9999,321,456,222,987;"
      +"14,12,1,1,2,320,411,6178,2222,3,1,0,0,2,3,88,66,77,55,1,294912,32768,1,0,1,11,12,13,44,"
      +"1,72,31,-19,-77,45,2,0;15,-1,15,-1,-1,-1,-1,-1;"
      +"0x1.8p-1,-0x1.0p-2,0x1.0p-1;-123456789012345678;create";

  public static int run() {
    checks=0;
    List<Frozen> frozen=frozen();
    frozenGenetics(frozen);
    legacyMigration(frozen.getFirst().encoded());
    storageAndOverrides(frozen.getFirst().encoded());
    birthVariety();
    personalityInheritance();
    fixedFaceAndHatching();
    baselineAndAlignment();
    expressionPrecedence();
    moodBoundariesAndRecovery();
    previewProjection();
    readOnlyProjection();
    return checks;
  }

  private static List<Frozen> frozen() {
    var stream=ChaoFace076Test.class.getResourceAsStream("face076-frozen075.txt");
    if(stream==null)throw new AssertionError("Missing literal accepted-.75 genetic fixtures");
    var rows=new ArrayList<Frozen>();
    try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
      for(String line; (line=reader.readLine())!=null;) {
        if(line.isBlank()||line.startsWith("#"))continue;
        String[] values=line.split("\t",3);
        if(values.length!=3)throw new AssertionError("Malformed frozen fixture row");
        rows.add(new Frozen(values[0],Long.parseLong(values[1]),values[2]));
      }
    }catch(java.io.IOException ex) {throw new AssertionError("Cannot read frozen fixtures",ex);}
    check(rows.size()==16,"Retain all sixteen literal accepted-.75 birth/RNG fixtures");
    return rows;
  }

  /** Project the three face fields and the two .81 rounding fields away;
   * every .75 byte still compares against the same frozen birth/RNG fixtures. */
  private static String withoutFace(String encoded) {
    String[] fields=encoded.split(";",-1);
    check(fields.length==25&&fields[0].equals("6"),"Face storage keeps schema6 and all25 sections");
    String[] extras=fields[21].split(",",-1);
    check(extras.length==43,"Expanded storage retains three face fields then appends two garden rounding fields");
    check(extras[41].equals("0")&&extras[42].equals("0"),"Frozen historical births and migrations receive zero garden rounding residue");
    fields[21]=String.join(",",Arrays.copyOf(extras,38));
    return String.join(";",fields);
  }

  private static void frozenGenetics(List<Frozen> frozen) {
    for(var fixture:frozen) {
      int separator=fixture.name().lastIndexOf('-');
      String kind=fixture.name().substring(0,separator);
      long seed=Long.parseLong(fixture.name().substring(separator+1));
      var random=new Random(seed);
      ChaoState actual=switch(kind) {
        case "random" -> ChaoState.random(random,0);
        case "random-coloreyes" -> ChaoState.random(random,100);
        case "egg" -> ChaoState.egg(3,true,true,0,random,65);
        case "breed" -> {
          var first=ChaoState.egg(4,true,true,0,new Random(111),35);
          var second=ChaoState.egg(7,false,false,0,new Random(222),35);
          first.adult=second.adult=true;
          first.primaryType=ChaoState.RUN_TYPE;second.primaryType=ChaoState.SWIM_TYPE;
          first.align=1;second.align=-1;
          yield ChaoState.breed(first,second,random,true,65);
        }
        default -> throw new AssertionError("Unknown literal fixture: "+fixture.name());
      };
      check(random.nextLong()==fixture.nextRandom(),fixture.name()+": exact next caller RNG value remains .75");
      check(withoutFace(actual.encode()).equals(fixture.encoded()),fixture.name()+": every retained encoded field remains exact .75");
      check(actual.faceBaselineInitialized(),fixture.name()+": completed birth has a saved face");
      var migrated=ChaoState.decode(fixture.encoded());
      check(withoutFace(migrated.encode()).equals(fixture.encoded()),fixture.name()+": .75 migration changes only new face storage");
      stable(migrated,fixture.name()+" migration");
    }
  }

  private static void stable(ChaoState state,String label) {
    int eyes=state.baselineEyes(),mouth=state.baselineMouth();
    long entropy=state.entropy;String identity=state.lineageId(),canonical=state.encode();
    for(int repeat=0;repeat<3;repeat++) {
      state.personalityProfile();state.doctorPersonality(repeat);state.baselineMouth();state.baselineEyes();
      check(state.encode().equals(canonical),label+": read order never rerolls or mutates saved state");
      state=ChaoState.decode(canonical);
      check(state.faceBaselineInitialized()&&state.baselineEyes()==eyes&&state.baselineMouth()==mouth,
          label+": fixed face survives repeated decode");
      check(state.entropy==entropy&&state.lineageId().equals(identity),label+": no entropy or identity consumption");
      check(state.encode().equals(canonical),label+": exact canonical roundtrip");
    }
  }

  private static void legacyMigration(String old075) {
    for(String literal:new String[]{V2,"3"+V2.substring(1)+";normal",V4}) {
      var state=ChaoState.decode(literal);
      check(state.faceBaselineInitialized(),"v2/v3/v4 migration initializes one persisted face");
      check(state.entropy==-123456789012345678L&&state.ageTicks==6178&&state.adultAgeTicks==2222,
          "Legacy face migration retains entropy and both lifetime clocks");
      check(Arrays.equals(state.colorGenes,new int[]{2,14})&&Arrays.equals(state.jewelGenes,new int[]{0,12})
          &&Arrays.equals(state.gradeA,new int[]{0,1,2,3,4})&&Arrays.equals(state.gradeB,new int[]{5,4,3,2,1}),
          "Legacy face migration retains both parental appearance/stat alleles");
      check(state.happiness==88&&state.bond==66&&state.hunger==77&&state.energy==55&&state.sleeping,
          "Legacy face migration retains real care and sleep");
      if(literal.startsWith("4;")) {
        check(state.variant.equals("create")&&state.legacyProfileLabel==1&&state.personalityProfile()[0]==1,
            "Legacy Doctor overlay and optional theme survive face migration");
        check(state.curiosityTendency==31&&state.energeticTendency==-19&&state.naiveTendency==-77&&state.carefreeTendency==45,
            "Face migration never quantizes trained legacy tendencies");
      }
      stable(state,"v"+literal.charAt(0));
    }
    String[] original=old075.split(";",-1);String[] extras=original[21].split(",",-1);
    for(String version:new String[]{"5","6"})for(int count:new int[]{17,19,21,36,37,38}) {
      String[] early=original.clone();early[0]=version;
      early[21]=String.join(",",Arrays.copyOf(extras,count));
      var state=ChaoState.decode(String.join(";",early));
      check(state.faceBaselineInitialized(),"Every supported early expanded length gets a saved face");
      String[] after=state.encode().split(";",-1);
      for(int field=1;field<=20;field++)check(after[field].equals(original[field]),
          "Schema"+version+" extra"+count+" retains old field"+field);
      stable(state,"schema"+version+" extra"+count);
    }
  }

  private static String appendFace(String old075,String face) {
    String[] fields=old075.split(";",-1);fields[21]+=","+face;return String.join(";",fields);
  }

  private static void storageAndOverrides(String old075) {
    var literal=ChaoState.decode(appendFace(old075,"3,4,1"));
    check(literal.baselineEyes()==3&&literal.baselineMouth()==4,"Literal initialized .76 IDs retain Mean/Wavy");
    check(withoutFace(literal.encode()).equals(old075),"Reading new face IDs preserves every retained .75 field");
    var bounded=ChaoState.decode(appendFace(old075,"-2147483648,2147483647,1"));
    check(bounded.baselineEyes()==0&&bounded.baselineMouth()==4,"Out-of-range integer face IDs clamp independently");
    check(withoutFace(bounded.encode()).equals(old075),"Malformed bounded face IDs never discard unrelated save data");
    var absent=ChaoState.decode(old075);var uninitialized=ChaoState.decode(appendFace(old075,"4,4,0"));
    check(absent.baselineEyes()==uninitialized.baselineEyes()&&absent.baselineMouth()==uninitialized.baselineMouth(),
        "False initialized flag uses deterministic migration, not arbitrary payload IDs");
    for(int eyes=0;eyes<5;eyes++)for(int mouth=0;mouth<5;mouth++) {
      var state=ChaoState.decode(old075);state.setBaselineFace076(eyes,mouth);
      check(state.baselineEyes()==eyes&&state.baselineMouth()==mouth,"Every supported eye/mouth ID pair is settable");
      check(withoutFace(state.encode()).equals(old075),"Explicit face override changes no old state field");
      stable(state,"explicit face "+eyes+"/"+mouth);
    }
    literal.setBaselineFace076(Integer.MAX_VALUE,Integer.MIN_VALUE);
    check(literal.baselineEyes()==4&&literal.baselineMouth()==0,"Direct invalid overrides have bounded Chaos/None endpoints");
    literal.setBaselineFace076(3,4);
    for(int i=0;i<256;i++)check(literal.setTraitInt("record.face"+i,i),"Full feature-map fixture accepts all original slots");
    check(!literal.setTraitInt("record.overflow",1),"Feature-map fixture really is saturated");
    var restored=ChaoState.decode(literal.encode());
    check(restored.traitEntries().size()==256&&restored.intTrait("record.face255")==255,
        "Dedicated face storage does not consume or evict a feature-map slot");
    stable(restored,"saturated feature map");
  }

  private static void birthVariety() {
    var allEyes=new HashSet<Integer>();var allMouths=new HashSet<Integer>();
    for(int label=0;label<15;label++) {
      var faces=new HashSet<String>();
      for(int birth=0;birth<96;birth++) {
        String[] fields=V4.split(";",-1);String[] care=fields[11].split(",",-1);
        care[29]=Integer.toString(label);fields[11]=String.join(",",care);fields[14]=Long.toString(birth);
        var state=ChaoState.decode(String.join(";",fields));
        check(state.personalityProfile()[0]==label,"Every old Doctor label remains visible while assigning a face");
        check(state.baselineEyes()>=0&&state.baselineEyes()<5&&state.baselineMouth()>=0&&state.baselineMouth()<5,
            "Deterministic identity assignment stays inside authored shape ranges");
        faces.add(state.baselineEyes()+"/"+state.baselineMouth());
        allEyes.add(state.baselineEyes());allMouths.add(state.baselineMouth());
      }
      check(faces.size()>1,"A shared Doctor label allows distinct intrinsic faces");
    }
    check(allEyes.size()==5&&allMouths.size()==5,"All five eye and mouth identities remain obtainable");
  }

  private static void personalityInheritance() {
    var first=new ChaoState();var second=new ChaoState();first.adult=second.adult=true;
    for(int i=0;i<13;i++) {
      first.personalityGenesA[i]=0;first.personalityGenesB[i]=4;
      second.personalityGenesA[i]=1;second.personalityGenesB[i]=3;
      first.setPersonalitySlider(i,17);second.setPersonalitySlider(i,-29);
    }
    first.setEmotion(ChaoEmotion.ANGER,200);second.setIllness(ChaoIllness.COUGH,-100);
    first.setBaselineFace076(3,3);second.setBaselineFace076(2,2);
    var observed=new boolean[13][4];
    for(int seed=0;seed<96;seed++) {
      var child=ChaoState.breed(first,second,new Random(seed));
      for(int i=0;i<13;i++) {
        int a=child.personalityGenesA[i],b=child.personalityGenesB[i];
        check((a==0||a==4)&&(b==1||b==3),"Each of thirteen traits retains one actual allele from each parent");
        int slider=child.personalitySlider(i);
        check(slider==-80||slider==-40||slider==40||slider==80,"Expressed child slider is an inherited allele phenotype, never an averaged/trained parent value");
        check(slider==(a-2)*40||slider==(b-2)*40,"Expressed child slider belongs to its own inherited pair");
        observed[i][a==0?0:1]=true;observed[i][b==1?2:3]=true;
      }
      check(child.emotion(ChaoEmotion.ANGER)==0&&!child.isIll()&&!child.adult&&child.ageTicks==0,
          "Birth does not inherit acquired mood, illness or adult lifecycle");
      check(child.faceBaselineInitialized(),"Final inherited child profile receives one fixed face");
      stable(child,"inherited child"+seed);
    }
    for(boolean[] alleles:observed)for(boolean seen:alleles)check(seen,"Both parental alleles remain transmissible for all thirteen traits");
    check(Arrays.equals(first.personalityGenesA,new int[13])&&second.personalityGenesA[0]==1,
        "Breeding and face assignment do not mutate parental genotype");
  }

  private static void fixedFaceAndHatching() {
    var state=ChaoState.random(new Random(9));state.setBaselineFace076(3,4);
    state.kindnessGenes=new int[]{0,4};state.aggressiveGenes=new int[]{0,4};
    int[] sliders=ChaoPersonality.sliders(state);int happiness=state.happiness;
    state.applyHatchingMethod(true);
    check(Arrays.equals(state.kindnessGenes,new int[]{1,4})&&Arrays.equals(state.aggressiveGenes,new int[]{0,3}),
        "Gentle hatch retains the existing bounded genotype changes");
    check(Arrays.equals(sliders,ChaoPersonality.sliders(state))&&state.happiness==happiness,
        "Hatching preserves fixed expressed personality and happiness");
    check(state.baselineEyes()==3&&state.baselineMouth()==4,"Gentle hatching cannot reroll the original face");
    state.applyHatchingMethod(false);
    check(Arrays.equals(state.kindnessGenes,new int[]{0,3})&&Arrays.equals(state.aggressiveGenes,new int[]{1,4}),
        "Rough hatch retains existing opposite genotype changes");
    check(state.baselineEyes()==3&&state.baselineMouth()==4,"Rough hatching cannot reroll the original face");
    for(int i=0;i<13;i++)state.setPersonalitySlider(i,i%2==0?100:-100);
    state.adult=true;state.ageTicks=5432;state.adultAgeTicks=6789;state.align=-1;state.eyeColor=11;
    state.setEmotion(ChaoEmotion.JOY,200);state.setEmotion(ChaoEmotion.ANGER,200);state.setIllness(ChaoIllness.COUGH,-100);
    check(state.baselineEyes()==3&&state.baselineMouth()==4,"Later personality, alignment, iris, age, mood and illness never rewrite intrinsic shape");
    stable(state,"cared-for fixed face");
    state.reincarnate();
    check(state.baselineEyes()==3&&state.baselineMouth()==4,"Reincarnation retains the same creature's face despite personality reset");
    stable(state,"reborn fixed face");
  }

  private static ChaoState face(int eyes,int mouth) {
    var state=new ChaoState();state.setBaselineFace076(eyes,mouth);return state;
  }

  private static void resolved(ChaoState state,Signals signal,Eyes eyes,Mouth mouth,
      Source eyeSource,Source mouthSource,String label) {
    String before=state.encode();
    var result=ChaoFaceRules076.resolve(state,signal);
    check(result.eyes()==eyes&&result.mouth()==mouth,label+": exact eye and mouth shapes");
    check(result.eyeSource()==eyeSource&&result.mouthSource()==mouthSource,label+": each channel has the expected priority source");
    check(result.baselineEyes()==state.baselineEyes()&&result.baselineMouth()==state.baselineMouth(),
        label+": result retains the original intrinsic identity");
    check(state.encode().equals(before),label+": presentation never changes saved state");
  }

  private static void baselineAndAlignment() {
    Eyes[] expectedEyes={Eyes.NORMAL,Eyes.HALF_CLOSED,Eyes.HAPPY_CLOSED,Eyes.MEAN,Eyes.CHAOS};
    Mouth[] expectedMouths={Mouth.NONE,Mouth.SMILE,Mouth.FROWN,Mouth.GRIN,Mouth.WAVY};
    for(int eyes=0;eyes<5;eyes++)for(int mouth=0;mouth<5;mouth++)
      resolved(face(eyes,mouth),Signals.quiet(),expectedEyes[eyes],expectedMouths[mouth],Source.BASELINE,Source.BASELINE,
          "literal baseline "+eyes+"/"+mouth);
    record Alignment(boolean adult,float alignment,int iris,Style style) {}
    Alignment[] cases={
        new Alignment(false,1,0,Style.NEUTRAL),new Alignment(false,-1,0,Style.NEUTRAL),
        new Alignment(true,0,0,Style.NEUTRAL),new Alignment(true,.4999f,0,Style.NEUTRAL),
        new Alignment(true,-.4999f,0,Style.NEUTRAL),new Alignment(true,.5f,0,Style.HERO),
        new Alignment(true,-.5f,0,Style.DARK),new Alignment(true,1,0,Style.HERO),
        new Alignment(true,-1,0,Style.DARK),new Alignment(true,-1,1,Style.DARK),
        new Alignment(true,1,8,Style.HERO),new Alignment(true,-1,9,Style.HERO_LENS),
        new Alignment(true,1,10,Style.DARK_LENS),new Alignment(true,-1,11,Style.CHAOS_LENS),
        new Alignment(false,0,9,Style.HERO_LENS),new Alignment(false,0,10,Style.DARK_LENS),
        new Alignment(false,0,11,Style.CHAOS_LENS)};
    for(var entry:cases)for(int eyes=0;eyes<5;eyes++) {
      var state=face(eyes,0);state.adult=entry.adult();state.align=entry.alignment();state.eyeColor=entry.iris();
      state.variant="nether";
      var result=ChaoFaceRules076.resolve(state,Signals.quiet());
      check(result.style()==entry.style()&&result.irisColor()==entry.iris(),"Alignment and explicit lens select exact artwork while iris identity persists");
      check(result.eyes()==expectedEyes[eyes]&&result.mouth()==Mouth.NONE,
          "Alignment changes artwork without rewriting baseline semantics or inventing a mouth");
      check(state.baselineEyes()==eyes&&state.baselineMouth()==0,"Every alignment projection leaves saved baseline untouched");
    }
    var lockedHero=face(3,0);lockedHero.adult=true;lockedHero.align=1;lockedHero.encode();lockedHero.align=-1;
    check(ChaoFaceRules076.resolve(lockedHero,Signals.quiet()).style()==Style.HERO&&lockedHero.align==-1,
        "First-evolution Hero artwork survives external raw alignment edits without a render-time state write");
    for(String character:new String[]{"normal","omochao"}) {
      var state=face(4,3);state.character=character;state.immortal=character.equals("normal");
      var result=ChaoFaceRules076.resolve(state,Signals.quiet());
      check(result.nativeExcluded()&&result.style()==Style.NATIVE&&result.eyeSource()==Source.NATIVE&&result.mouthSource()==Source.NATIVE,
          "Actual Chaos/robot state retains native anatomy even with quiet external signals");
    }
    for(int eyes:new int[]{2,4})
      resolved(face(eyes,0),signals("blink"),expectedEyes[eyes],Mouth.NONE,Source.BASELINE,Source.BASELINE,
          "Closed/Chaos intrinsic face ignores ordinary blink and preserves no-mouth baseline");
  }

  /** Test fixture flags only build immutable inputs; expected results below are literal. */
  private static Signals signals(String flags) {return signals(flags,Symptom.NONE,null,-1);}
  private static Signals signals(String flags,Symptom symptom,Mouth action,int behavior) {
    List<String> words=List.of(flags.split(" "));
    return new Signals(words.contains("native"),words.contains("hurt"),words.contains("danger"),
        words.contains("sleep"),words.contains("groggy"),symptom,words.contains("illness"),
        words.contains("happy"),words.contains("pet"),action,behavior,words.contains("blink"));
  }

  private static void expressionPrecedence() {
    record Expression(String name,Signals signal,Eyes eyes,Mouth mouth,Source eyesFrom,Source mouthFrom) {}
    Expression[] cases={
        new Expression("intrinsic idle",signals(""),Eyes.MEAN,Mouth.GRIN,Source.BASELINE,Source.BASELINE),
        new Expression("ordinary blink",signals("blink"),Eyes.BLINK,Mouth.GRIN,Source.BLINK,Source.BASELINE),
        new Expression("happy toy",signals("happy blink"),Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.ACTIVITY,Source.ACTIVITY),
        new Expression("pet affection",signals("pet blink"),Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.CARE,Source.CARE),
        new Expression("groggy",signals("groggy"),Eyes.HALF_CLOSED,Mouth.WAVY,Source.MOOD,Source.MOOD),
        new Expression("independent breath",signals("",Symptom.NONE,Mouth.OPEN,-1),Eyes.MEAN,Mouth.OPEN,Source.BASELINE,Source.ACTIVITY),
        new Expression("no-mouth action",signals("happy",Symptom.NONE,Mouth.NONE,-1),Eyes.HAPPY_CLOSED,Mouth.NONE,Source.ACTIVITY,Source.ACTIVITY),
        new Expression("authored crying behavior",signals("",Symptom.NONE,null,2),Eyes.DISTRESS,Mouth.FROWN,Source.BEHAVIOR,Source.BEHAVIOR),
        new Expression("toy owns behavior mouth",signals("happy",Symptom.NONE,null,2),Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.ACTIVITY,Source.ACTIVITY),
        new Expression("care owns behavior mouth",signals("pet",Symptom.NONE,null,2),Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.CARE,Source.CARE),
        new Expression("action owns behavior mouth",signals("",Symptom.NONE,Mouth.OPEN,2),Eyes.DISTRESS,Mouth.OPEN,Source.BEHAVIOR,Source.ACTIVITY),
        new Expression("illness owns toy/care/action",signals("illness happy pet blink",Symptom.NONE,Mouth.OPEN,6),Eyes.HALF_CLOSED,Mouth.WAVY,Source.ILLNESS,Source.ILLNESS),
        new Expression("symptom owns illness/toy/care/action",signals("illness happy pet blink",Symptom.COUGH,Mouth.SMILE,6),Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM,Source.SYMPTOM),
        new Expression("sleep owns symptom",signals("sleep illness happy pet blink",Symptom.COUGH,Mouth.OPEN,6),Eyes.SLEEP,Mouth.NONE,Source.SLEEP,Source.SLEEP),
        new Expression("danger owns sleep",signals("danger sleep illness happy pet blink",Symptom.COUGH,Mouth.SMILE,6),Eyes.DISTRESS,Mouth.OPEN,Source.DANGER,Source.DANGER),
        new Expression("damage owns all later overrides",signals("hurt danger sleep groggy illness happy pet blink",Symptom.COUGH,Mouth.OPEN,6),Eyes.DISTRESS,Mouth.FROWN,Source.DAMAGE,Source.DAMAGE)};
    for(var entry:cases)resolved(face(3,3),entry.signal(),entry.eyes(),entry.mouth(),entry.eyesFrom(),entry.mouthFrom(),entry.name());
    for(Symptom symptom:new Symptom[]{Symptom.COUGH,Symptom.SNEEZE,Symptom.HICCUP})
      resolved(face(3,3),signals("blink",symptom,null,-1),Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM,Source.SYMPTOM,"acute "+symptom);
    for(Symptom symptom:new Symptom[]{Symptom.SCRATCH,Symptom.STOMACH_ACHE,Symptom.RUNNY_NOSE})
      resolved(face(3,3),signals("blink",symptom,null,-1),Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM,Source.SYMPTOM,"uncomfortable "+symptom);
    Mouth[] authored={Mouth.GRIN,Mouth.SMILE,Mouth.FROWN,Mouth.FLAT,Mouth.OPEN,Mouth.WAVY,Mouth.OPEN};
    Eyes[] authoredEyes={Eyes.MEAN,Eyes.MEAN,Eyes.DISTRESS,Eyes.MEAN,Eyes.MEAN,Eyes.MEAN,Eyes.MEAN};
    Source[] eyeSources={Source.BASELINE,Source.BASELINE,Source.BEHAVIOR,Source.BASELINE,Source.BASELINE,Source.BASELINE,Source.BASELINE};
    Source[] sources={Source.BASELINE,Source.BEHAVIOR,Source.BEHAVIOR,Source.BEHAVIOR,Source.BEHAVIOR,Source.BEHAVIOR,Source.BEHAVIOR};
    for(int id=0;id<=6;id++)resolved(face(3,3),signals("",Symptom.NONE,null,id),authoredEyes[id],authored[id],eyeSources[id],sources[id],"authored native mouth"+id);
    var excluded=ChaoFaceRules076.resolve(face(3,3),signals("native hurt danger sleep illness happy pet blink",Symptom.COUGH,Mouth.OPEN,6));
    check(excluded.eyes()==Eyes.MEAN&&excluded.mouth()==Mouth.GRIN&&excluded.style()==Style.NATIVE
        &&excluded.eyeSource()==Source.NATIVE&&excluded.mouthSource()==Source.NATIVE,"Native anatomy gate bypasses every custom override");
    var sleeping=face(3,3);sleeping.sleeping=true;
    resolved(sleeping,Signals.quiet(),Eyes.SLEEP,Mouth.NONE,Source.SLEEP,Source.SLEEP,"Actual saved sleep is respected without an invented behavior");
  }

  private static void moodBoundariesAndRecovery() {
    record Boundary(int emotion,int below,int threshold,Eyes eyes,Mouth mouth) {}
    Boundary[] cases={
        new Boundary(ChaoEmotion.FEAR,114,115,Eyes.SURPRISED,Mouth.OPEN),
        new Boundary(ChaoEmotion.CRY,99,100,Eyes.DISTRESS,Mouth.FROWN),
        new Boundary(ChaoEmotion.ANGER,104,105,Eyes.MEAN,Mouth.FROWN),
        new Boundary(ChaoEmotion.SLEEPINESS,7999,8000,Eyes.HALF_CLOSED,Mouth.WAVY),
        new Boundary(ChaoEmotion.TIREDNESS,5999,6000,Eyes.HALF_CLOSED,Mouth.WAVY),
        new Boundary(ChaoEmotion.TIRE,7999,8000,Eyes.HALF_CLOSED,Mouth.WAVY),
        new Boundary(ChaoEmotion.JOY,114,115,Eyes.HAPPY_CLOSED,Mouth.SMILE),
        new Boundary(ChaoEmotion.RELAX,69,70,Eyes.HALF_CLOSED,Mouth.SMILE),
        new Boundary(ChaoEmotion.LONELY,3999,4000,Eyes.HALF_CLOSED,Mouth.FROWN)};
    for(var entry:cases) {
      var state=face(0,0);state.setEmotion(entry.emotion(),entry.below());
      resolved(state,Signals.quiet(),Eyes.NORMAL,Mouth.NONE,Source.BASELINE,Source.BASELINE,"Mood below threshold "+entry.emotion());
      state.setEmotion(entry.emotion(),entry.threshold());
      resolved(state,Signals.quiet(),entry.eyes(),entry.mouth(),Source.MOOD,Source.MOOD,"Mood at threshold "+entry.emotion());
      state.setEmotion(entry.emotion(),0);
      resolved(state,Signals.quiet(),Eyes.NORMAL,Mouth.NONE,Source.BASELINE,Source.BASELINE,"Recovered mood "+entry.emotion());
    }
    var tired=face(0,0);tired.energy=16;
    resolved(tired,Signals.quiet(),Eyes.NORMAL,Mouth.NONE,Source.BASELINE,Source.BASELINE,"Energy16 stays above tired face");
    tired.energy=15;
    resolved(tired,Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.MOOD,Source.MOOD,"Energy15 selects tired face");
    var mixed=face(0,0);
    mixed.setEmotion(ChaoEmotion.FEAR,115);mixed.setEmotion(ChaoEmotion.CRY,100);
    mixed.setEmotion(ChaoEmotion.ANGER,105);mixed.setEmotion(ChaoEmotion.JOY,115);mixed.setEmotion(ChaoEmotion.RELAX,70);
    resolved(mixed,Signals.quiet(),Eyes.SURPRISED,Mouth.OPEN,Source.MOOD,Source.MOOD,"Fear owns mixed distress/joy");
    resolved(mixed,signals("happy"),Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.ACTIVITY,Source.ACTIVITY,"Toy activity owns ordinary mood");
    resolved(mixed,signals("pet"),Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.CARE,Source.CARE,"Affection owns ordinary mood");
    resolved(mixed,signals("",Symptom.NONE,Mouth.FLAT,-1),Eyes.SURPRISED,Mouth.FLAT,Source.MOOD,Source.ACTIVITY,"Mood eyes coexist with independent action mouth");
    mixed.setEmotion(ChaoEmotion.FEAR,0);
    resolved(mixed,Signals.quiet(),Eyes.DISTRESS,Mouth.FROWN,Source.MOOD,Source.MOOD,"Crying owns anger and joy");
    mixed.setEmotion(ChaoEmotion.CRY,0);
    resolved(mixed,Signals.quiet(),Eyes.MEAN,Mouth.FROWN,Source.MOOD,Source.MOOD,"Anger owns joy");
    resolved(mixed,signals("blink"),Eyes.BLINK,Mouth.FROWN,Source.BLINK,Source.MOOD,"Blink replaces mood eyes while retaining the independent mood mouth");
    var natural=face(1,3);natural.setEmotion(ChaoEmotion.FEAR,115);natural.ageTicks=19;
    natural.tick(1000000,1000000,new Random(1));
    resolved(natural,Signals.quiet(),Eyes.HALF_CLOSED,Mouth.GRIN,Source.BASELINE,Source.BASELINE,"Actual mood decay restores the same intrinsic face");
    var ill=face(4,0);ill.setIllness(ChaoIllness.COUGH,-100);
    resolved(ill,signals("illness",Symptom.COUGH,null,-1),Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM,Source.SYMPTOM,"Visible cough is only a symptom");
    check(ill.illness(ChaoIllness.COUGH)==-100&&ill.isIll(),"Resolving a symptom never cures the illness");
    resolved(ill,Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.ILLNESS,Source.ILLNESS,"Actual illness is visible without external illness signals");
    check(ill.cureIllness(ChaoIllness.COUGH)&&ill.illnessImmunityTicks(ChaoIllness.COUGH)==24000,"One actual cure starts the existing immunity clock");
    resolved(ill,Signals.quiet(),Eyes.CHAOS,Mouth.NONE,Source.BASELINE,Source.BASELINE,"Cure restores the original Chaos-style/None baseline");
    stable(ill,"treated illness face");
    ill.illnessCooldown[ChaoIllness.COUGH]=1;ill.ageTicks=19;ill.tick(1000000,1000000,new Random(1));
    check(ill.illness(ChaoIllness.COUGH)==0&&ill.illnessImmunityTicks(ChaoIllness.COUGH)==0,
        "Immunity expiry returns healthy susceptibility without a stale illness face");
    resolved(ill,Signals.quiet(),Eyes.CHAOS,Mouth.NONE,Source.BASELINE,Source.BASELINE,"Post-immunity face remains intrinsic");
  }

  private static void previewProjection() {
    check(ChaoFaceRules076.FACE_PREVIEW_IDS.equals(List.of("happy","distress","tired","sleep","cough","cold","rash","runny_nose","hiccups","stomach_ache")),
        "Operator preview catalogue retains all ten production face inputs");
    record Preview(String id,Eyes eyes,Mouth mouth,Source source) {}
    Preview[] cases={new Preview("happy",Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.ACTIVITY),
        new Preview("distress",Eyes.DISTRESS,Mouth.OPEN,Source.DANGER),new Preview("tired",Eyes.HALF_CLOSED,Mouth.WAVY,Source.MOOD),
        new Preview("sleep",Eyes.SLEEP,Mouth.NONE,Source.SLEEP),new Preview("cough",Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM),
        new Preview("cold",Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM),new Preview("rash",Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM),
        new Preview("runny_nose",Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM),new Preview("hiccups",Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM),
        new Preview("stomach_ache",Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM)};
    var natural=Signals.quiet();
    for(var entry:cases) {
      String kind="preview_face_"+entry.id();
      check(ChaoFaceRules076.isFacePreviewKind(kind),"Listed operator face token is recognized");
      for(float elapsed:new float[]{20,80,139.999f})resolved(face(3,3),ChaoFaceRules076.previewSignals(natural,kind,elapsed),
          entry.eyes(),entry.mouth(),entry.source(),entry.source(),entry.id()+" production preview at"+elapsed);
      for(float elapsed:new float[]{-1,0,19.999f,140,160,Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY})
        check(ChaoFaceRules076.previewSignals(natural,kind,elapsed)==natural,"Outside active preview window keeps the exact natural input");
      var damaged=ChaoFaceRules076.previewSignals(signals("hurt"),kind,80);
      resolved(face(3,3),damaged,Eyes.DISTRESS,Mouth.FROWN,Source.DAMAGE,Source.DAMAGE,"Damage owns "+entry.id()+" preview");
    }
    for(String invalid:new String[]{"","preview_face_","preview_face_unknown","chao_radio","happy"}) {
      check(!ChaoFaceRules076.isFacePreviewKind(invalid),"Invalid face preview token cannot reserve presentation");
      check(ChaoFaceRules076.previewSignals(natural,invalid,80)==natural,"Invalid preview token returns natural signals");
    }
    check(!ChaoFaceRules076.isFacePreviewKind(null),"Null face token is inactive");
  }

  private static void readOnlyProjection() {
    var state=face(1,4);state.encode();
    // Deliberately unsynchronized legacy meters catch accidental calls to mutating get()/sync().
    state.hunger=7;state.energy=82;state.boredom=93;state.emotions[ChaoEmotion.HUNGRY]=5101;
    int[] before=state.emotions.clone(),genesA=state.personalityGenesA.clone(),genesB=state.personalityGenesB.clone();
    long entropy=state.entropy;String identity=state.lineageId();
    Resolved first=ChaoFaceRules076.resolve(state,Signals.quiet());
    for(int i=0;i<10;i++)check(ChaoFaceRules076.resolve(state,Signals.quiet()).equals(first),"Repeated client/observer projection is deterministic");
    check(Arrays.equals(before,state.emotions)&&state.hunger==7&&state.energy==82&&state.boredom==93,
        "Presentation does not synchronize or round fractional authoritative emotion storage");
    check(Arrays.equals(genesA,state.personalityGenesA)&&Arrays.equals(genesB,state.personalityGenesB)
        &&state.entropy==entropy&&state.lineageId().equals(identity),"Presentation never writes genetics, entropy or ancestry identity");
  }

  public static void main(String[] args) {System.out.println("Chao face076: "+run()+" checks passed");}
}
