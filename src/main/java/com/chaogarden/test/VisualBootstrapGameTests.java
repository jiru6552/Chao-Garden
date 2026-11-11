package com.chaogarden.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated, actor-free world generation for the opt-in actual-client study. Never shipped. */
@GameTestHolder("chao_visual_bootstrap")
@PrefixGameTestTemplate(false)
public final class VisualBootstrapGameTests {
  @GameTest(template="empty",timeoutTicks=20)
  public static void createCleanVisualWorld(GameTestHelper helper) { helper.succeed(); }
}
