package com.chaogarden.test;

import com.chaogarden.core.ChaoState;
import java.util.Arrays;
import net.minecraft.gametest.framework.GameTestHelper;

/** Exact historical comparison: three fixed-face values then two zero garden-rounding values. */
final class FaceLegacyAssertions076 {
  private FaceLegacyAssertions076() {}

  static void assertAppendOnly(GameTestHelper helper,String legacy,ChaoState state,String reason) {
    String upgraded=state.encode();
    String[] oldFields=legacy.split(";",-1),newFields=upgraded.split(";",-1);
    helper.assertTrue(oldFields.length==25&&newFields.length==25,reason+"; preserve all25 sections");
    String[] oldExtras=oldFields[21].split(",",-1),newExtras=newFields[21].split(",",-1);
    helper.assertTrue(oldExtras.length==38&&newExtras.length==43,reason+"; retain face extras38..40 then garden extras41..42");
    helper.assertTrue(newExtras[41].equals("0")&&newExtras[42].equals("0"),reason+"; old saves receive zero garden-alignment rounding residue");
    int eyes=Integer.parseInt(newExtras[38]),mouth=Integer.parseInt(newExtras[39]);
    helper.assertTrue(eyes>=0&&eyes<5&&mouth>=0&&mouth<5&&newExtras[40].equals("1")
        &&state.faceBaselineInitialized()&&state.baselineEyes()==eyes&&state.baselineMouth()==mouth,
        reason+"; saved face is bounded and initialized");
    newFields[21]=String.join(",",Arrays.copyOf(newExtras,38));
    helper.assertTrue(String.join(";",newFields).equals(legacy),reason+"; every old byte stays exact after projecting only extras38..42");
    String repeated=upgraded;
    for(int reload=0;reload<3;reload++) {
      var copy=ChaoState.decode(repeated);
      helper.assertTrue(copy.encode().equals(upgraded)&&copy.baselineEyes()==eyes&&copy.baselineMouth()==mouth,
          reason+"; upgraded save and fixed face remain exact through repeated reloads");
      repeated=copy.encode();
    }
  }
}
