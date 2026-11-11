package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.cwe.CweContent;
import com.chaogarden.entity.ChaoEntity;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual persistent assembly, not just a test of the coat-copy helper. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class RobotAppearanceGameTests {
  private RobotAppearanceGameTests(){}
  @GameTest(template="empty",timeoutTicks=40)
  public static void robotRefreshesBuilderAppearanceOnCompletionWithoutCopyingItsProgress(GameTestHelper h){
    for(int sample=0;sample<5;sample++){
      var at=h.absolutePos(new BlockPos(3,2,3));var builder=ChaoGarden.CHAO.get().create(h.getLevel());
      var source=ChaoState.egg(2,false,false,0,new Random(200+sample));
      source.adult=true;source.align=.8f;source.primaryType=ChaoState.POWER_TYPE;
      source.heroBrushType=2;source.negativePalette=true;source.brightCoat=true;
      source.swimFly=.75f;source.runPower=-.5f;source.setEvolutionStrength(.9f);
      source.variant=sample==1?"nether":sample==2?"end":sample==4?"create":"normal";
      source.setTrait("reward.wrench",1);source.animalParts[ChaoState.EARS]=6;
      source.points[ChaoState.RUN]=12345;source.levels[ChaoState.RUN]=73;
      source.setTrait("learned.bell",1);source.reincarnations=4;
      builder.setState(source);builder.owner=UUID.randomUUID();builder.setCustomName(Component.literal("Actual builder "+sample));builder.moveTo(at.getX(),at.getY(),at.getZ());
      h.getLevel().addFreshEntity(builder);
      String initialDna=source.dnaSummary();
      h.assertTrue(CweContent.assembleOmochao(builder),"First paid construction stage failed");
      var robot=(ChaoEntity)h.getLevel().getEntity(UUID.fromString(source.trait("content.omochao_uuid")));
      h.assertTrue(robot!=null&&robot.state().baseColor()==2&&!robot.state().twoTone,"Prototype did not snapshot starting coat");
      UUID actor=robot.getUUID();
      // The builder changes during construction. Only the completion snapshot
      // should pick up the new expressed coat and its coherent genetic alleles.
      source.expressedColor=4;source.colorGenes=new int[]{4,4};source.shiny=true;source.shinyGenes=new int[]{1,1};
      source.twoTone=sample%2==0;source.toneGenes=new int[]{source.twoTone?1:0,source.twoTone?1:0};
      source.expressedJewel=sample==3?4:0;source.jewelGenes=new int[]{source.expressedJewel,source.expressedJewel};
      if(sample==3)source.character="amy";
      String completedDna=source.dnaSummary();
      for(int box=1;box<4;box++)h.assertTrue(CweContent.assembleOmochao(builder),"Construction failed at box "+box);
      var result=robot.state();
      h.assertTrue(robot.getUUID().equals(actor)&&builder.owner.equals(robot.owner),"Completion changed the persistent actor or owner");
      h.assertTrue(result.baseColor()==4&&result.colorGenes[0]==4&&result.colorGenes[1]==4
          &&result.shiny&&result.shinyGenes[0]==1&&result.twoTone==source.twoTone
          &&result.toneGenes[0]==source.toneGenes[0]&&result.jewel()==source.jewel()
          &&result.jewelGenes[0]==source.jewel(),"Expressed robot coat and saved coat alleles disagree");
      h.assertTrue(result.variant.equals(com.chaogarden.ChaoVariants.obtainable(source.variant)?source.variant:"normal")&&result.align==source.align&&result.primaryType==source.primaryType
          &&result.heroBrushType==source.heroBrushType&&result.negativePalette&&result.brightCoat,"Visible builder appearance was dropped");
      h.assertTrue(result.trait("robot.source_character").equals(source.character)&&result.intTrait("robot.source_adult")==1,"Character coat context missing");
      h.assertTrue(result.trait("robot.source_sf").equals(Float.toHexString(source.swimFly))
          &&result.trait("robot.source_rp").equals(Float.toHexString(source.runPower))
          &&result.trait("robot.source_strength").equals(Float.toHexString(source.evolutionStrength())),"Actual gradual coat context was not snapshotted");
      h.assertTrue(result.adult&&!result.immortal&&result.adultAgeTicks==0&&Arrays.stream(result.grades).allMatch(g->g==2)
          &&Arrays.equals(result.grades,result.gradeA)&&Arrays.equals(result.gradeA,result.gradeB),"New Omochao is not a finite young-adult with coherent all-C ranks");
      h.assertTrue(result.trait("family.parent.0.id").equals(source.lineageId())
          &&result.familyName("family.parent.0").equals(builder.getName().getString()),"Assembly omitted its actual creator parent");
      h.assertTrue(Arrays.stream(result.points).sum()==0&&Arrays.stream(result.levels).sum()==0
          &&result.reincarnations==0&&result.intTrait("learned.bell")==0&&Arrays.stream(result.animalParts).allMatch(x->x<0),"Construction cloned earned progress or animal geometry");
      h.assertTrue(source.dnaSummary().equals(completedDna)&&!initialDna.equals(completedDna)
          &&source.points[ChaoState.RUN]==12345&&source.animalParts[ChaoState.EARS]==6,"Reading appearance mutated the builder");
      h.assertTrue(ChaoState.decode(result.encode()).trait("robot.source_character").equals(source.character),"Appearance snapshot did not survive a save");
      h.assertTrue(!CweContent.assembleOmochao(builder),"A fifth box duplicated the completed robot");
      robot.discard();builder.discard();
    }
    h.succeed();
  }
}
