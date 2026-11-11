package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.cwe.CweContent;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.genes.GeneContent;
import com.chaogarden.genes.GeneItem;
import com.chaogarden.item.GardenGuideItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server-host evolution, registered content and old-save compatibility for the full DNA rule. */
@GameTestHolder("chao_dna50") @PrefixGameTestTemplate(false)
public final class DnaEligibilityGameTests050 {
  private static final int FULL = 0x7fffffff, ORIGINAL = 0x1fffff;
  @GameTestGenerator public static Collection<TestFunction> missingAndCompleteCases() {
    var cases = new ArrayList<TestFunction>();
    for (int id = 0; id < ChaoGarden.ANIMALS.length; id++) {
      int missing = id;
      cases.add(new TestFunction("dna050_evolution", "missing_" + ChaoGarden.ANIMALS[id],
          "chao_dna50:empty", 40, 0, true, h -> evolve(h, FULL & ~(1 << missing), false, 0)));
    }
    for (int alignment = -1; alignment <= 1; alignment++) {
      int align = alignment;
      cases.add(new TestFunction("dna050_evolution", "complete_alignment_" + (alignment + 1),
          "chao_dna50:empty", 40, 0, true, h -> evolve(h, FULL, true, align)));
    }
    return cases;
  }
  private static ChaoState ready(int mask) {
    var s = new ChaoState();
    s.happiness = 90; s.reincarnations = 2; s.animalMask = mask;
    s.points[ChaoState.STAMINA] = 300; s.ageTicks = GardenConfig.CHILD_TICKS.get() - 1;
    return s;
  }
  private static ChaoEntity actor(GameTestHelper h, ChaoState state) {
    var c = ChaoGarden.CHAO.get().create(h.getLevel());
    h.assertTrue(c != null, "Actual Chao entity creates");
    c.setState(state); c.setNoAi(true); c.setNoGravity(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 2, 1))));
    h.getLevel().addFreshEntity(c);
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo i) {}
      public void testPassed(GameTestInfo i, GameTestRunner r) {c.discard();}
      public void testFailed(GameTestInfo i, GameTestRunner r) {c.discard();}
      public void testAddedForRerun(GameTestInfo a, GameTestInfo b, GameTestRunner r) {c.discard();}
    });
    return c;
  }
  private static void evolve(GameTestHelper h, int mask, boolean chaos, int alignment) {
    var s = ready(mask); s.align = alignment;
    var c = actor(h, ChaoState.decode(s.encode()));
    h.runAfterDelay(4, () -> {
      h.assertTrue(c.state().adult && c.state().immortal == chaos,
          "Actual entity-hosted child evolution must require every DNA species");
      h.assertTrue(c.state().animalMask == mask && c.state().primaryType == ChaoState.NORMAL,
          "Evolution must retain received DNA and Normal first-type selection");
      h.assertTrue(c.state().character.equals("normal"), "Biological Chaos check must not select a separate character form");
      if (chaos) h.assertTrue(c.state().typeName().equals(alignment < 0 ? "Devil" : alignment > 0 ? "Angel" : "Light"),
          "Alignment phenotype remains intact");
      h.succeed();
    });
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void allRegisteredSamplesAndTooltipsUseTheSameChecklist(GameTestHelper h) {
    h.assertTrue(ChaoGarden.ANIMALS.length == 31 && ChaoState.ANIMALS.length == 31
        && ChaoState.ALL_ANIMALS == FULL, "The registry and persistence catalog contain the same 31 DNA indices");
    for (int id = 0; id < 31; id++) {
      h.assertTrue(GeneContent.gene(id) instanceof GeneItem item && item.index == id,
          "A registered sample must keep its save-bit index");
      String stateName = id == 8 ? "warthog" : ChaoState.ANIMALS[id];
      h.assertTrue(ChaoGarden.ANIMALS[id].equals(stateName), "Registry and state order mismatch");
      var tips = new ArrayList<Component>(); GeneContent.appendBenefits(id, tips);
      h.assertTrue(tips.stream().anyMatch(t -> t.getString().contains("Chaos checklist"))
          && tips.stream().noneMatch(t -> t.getString().contains("bonus animal parts")),
          "Every registered DNA tooltip must count toward Chaos");
    }
    h.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void legacyPartialEntityAndExistingChaosSurviveReload(GameTestHelper h) {
    int mask = ORIGINAL | 1 << 24 | 1 << 30;
    var s = ready(mask); s.animalParts[0] = 24; s.animalParts[7] = 30;
    s.animalPartsMask = 1 << 24 | 1 << 30; s.setTrait("foreign.keep", "yes");
    var c = ChaoGarden.CHAO.get().create(h.getLevel()); c.setState(s);
    var opaque = new CompoundTag(); opaque.putString("Unrelated", "not rewritten");
    c.getPersistentData().put("ForeignMod", opaque);
    var saved = new CompoundTag(); c.saveWithoutId(saved);
    var copy = ChaoGarden.CHAO.get().create(h.getLevel()); copy.load(saved);
    h.assertTrue(copy.state().animalMask == mask && copy.state().animalParts[0] == 24
        && copy.state().animalParts[7] == 30 && copy.state().trait("foreign.keep").equals("yes")
        && copy.getPersistentData().getCompound("ForeignMod").equals(opaque),
        "Existing entity NBT must preserve acquired extra DNA, parts and unrelated data");
    var chaos = ready(ORIGINAL); chaos.adult = chaos.immortal = true;
    chaos.adultAgeTicks = Integer.MAX_VALUE - 3;
    var live = actor(h, chaos);
    var oldSave = new CompoundTag(); live.saveWithoutId(oldSave); live.load(oldSave);
    h.runAfterDelay(4, () -> {
      h.assertTrue(live.isAlive() && live.state().immortal && !live.state().dead
          && live.state().animalMask == ORIGINAL, "Already-evolved Chaos remains immortal without retroactive DNA requirements");
      c.discard(); copy.discard(); h.succeed();
    });
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void legacyTwentyOneNoLongerCompletesNewChaos(GameTestHelper h) {evolve(h, ORIGINAL, false, 0);}

  @GameTest(template="empty", timeoutTicks=40)
  public static void separateCharacterGateRetainsItsOriginalSpecies(GameTestHelper h) {
    for (int type : new int[] {ChaoState.FLY_TYPE, ChaoState.RUN_TYPE, ChaoState.POWER_TYPE}) {
      var c = ChaoGarden.CHAO.get().create(h.getLevel()); var s = ready(ORIGINAL);
      s.adult = true; s.primaryType = type; s.happiness = 80; c.setState(s);
      CweContent.onFirstEvolution(c);
      String expected = type == ChaoState.FLY_TYPE ? "tails" : type == ChaoState.RUN_TYPE ? "amy" : "knuckles";
      h.assertTrue(c.state().character.equals(expected) && c.state().immortal,
          "Unrelated character evolution keeps its original 21-species and care gate");
      c.discard();
    }
    h.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void olderJournalRefreshesDnaGuidanceOnUse(GameTestHelper h) {
    var p = TestConnections.makeSurvivalServerPlayer(h);
    var guide = new ItemStack(ChaoGarden.item("garden_guide"));
    h.assertTrue(guide.getItem() instanceof GardenGuideItem, "Actual journal item is registered");
    var legacy = new WrittenBookContent(Filterable.passThrough("A Keeper's Journal"), "Chao Garden", 0,
        List.of(Filterable.passThrough(Component.literal("Animal Genes\n\nOffer one of the 21 SA2 DNA tubes"))), true);
    guide.set(DataComponents.WRITTEN_BOOK_CONTENT, legacy);
    guide.set(DataComponents.CUSTOM_NAME, Component.literal("My old journal"));
    var data = new CompoundTag(); data.putString("ForeignKeep", "yes");
    guide.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    p.setItemInHand(InteractionHand.MAIN_HAND, guide);
    guide.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
    String text = guide.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().stream()
        .map(page -> page.get(false).getString()).collect(java.util.stream.Collectors.joining("\n"));
    h.assertTrue(text.contains("31 animal DNA tubes") && text.contains("Finish all 31 animal DNA species")
        && !text.contains("all 21") && !text.contains("21 SA2 DNA"), "Opening an older journal refreshes all live Chaos guidance");
    h.assertTrue(guide.get(DataComponents.CUSTOM_NAME).getString().equals("My old journal")
        && guide.get(DataComponents.CUSTOM_DATA).copyTag().equals(data), "Journal refresh preserves unrelated stack components");
    p.discard(); h.succeed();
  }
}
