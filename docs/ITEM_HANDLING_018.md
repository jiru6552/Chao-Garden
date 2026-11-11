# Chao gifts and curiosity - 0.18

Offering an item transfers one real stack unit into the Chao's main hand. It does not grant food or training benefits immediately. The same visible stack, including its name, durability and custom components, is saved with the creature. A Chao holds one gift at a time.

| Gift | Decision and result |
| --- | --- |
| Chao fruit and mushrooms | Think, check fullness and preference, then eat or return the food. Benefits apply once when the eating animation finishes. |
| Animal genes and Chaos Drives | Think, check whether the Chao is rested, then use the sample or return it. Level-99 drives can still steer evolution or improve uncapped luck/happiness; a drive is returned once all its benefits are capped. Chaos Chao still receive gene training benefits without acquiring animal parts. |
| Chao hats | Think, then put on a different hat if rested and in a nonnegative mood. The existing hat stays equipped until the swap commits and is returned once. Refused hats keep all their components. |
| Other items | Inspect with a confused expression, then throw the unchanged item back into the world. Ordinary vanilla or other-mod foods are not silently treated as Chao fruit. |
| Placed toys | Use on the ground to place the toy. Clicking a Chao gives placement guidance and does not force immediate play. |
| Analyzer, Care Guide, shears and carrying | Keep their explicit tool actions. Shears remove headwear while the Chao is idle. |

Chao may approach available dropped items within four blocks when idle and on the ground. Each pickup takes one unit, respects the item's pickup delay and a specific target owner, and preserves the remaining dropped stack. Refused items have a ten-second Chao pickup grace period so they are not caught and rejected in a tight loop. Players can collect the returned item after its ordinary one-second pickup delay.

The mod uses an existing fullness meter: zero means hungry and 100 means full. The default food threshold is 85; a favorite shaped fruit has a threshold of 95. Big eaters retain the documented exception of accepting food even while full, while Quiet and Cry Baby personalities consider ordinary food full sooner. Adult Hero and Dark forms refuse fruit intended for the opposite alignment; aligned children can eat those fruits while developing their alignment. Fullness falls by one point per fifteen loaded seconds, giving an ordinary full Chao roughly four minutes before another ordinary meal. Numeric thresholds, this clock and the following durations are Minecraft adaptations of hunger/personality behavior, not claims about original SA2 internal values.

| Phase | Duration |
| --- | ---: |
| Think about a familiar gift | 18-70 game ticks, depending on personality and curiosity |
| Inspect an unfamiliar item | 60 game ticks |
| Eat an accepted fruit | 60 game ticks |
| Use a drive/gene or equip a hat | 40 game ticks |
| Wind up and reject an item | 18 game ticks; the item leaves the hands during the gesture |
| React happily after success | 24 game ticks |

The source behavior references are the [feeding guide](https://chao-island.com/info-center/basics/feeding.html) and [personality/emotion reference](https://chao-island.com/wiki/Personality_%26_Emotion). SA2 and SA2 Battle take precedence for the surrounding care simulation. Accepted whole-fruit totals remain in the fruit catalog; the new interaction delays their application rather than granting extra bites or duplicate rewards.

Pending gifts, their giver, phase and remaining progress survive world saves and carrying. A recalled or disabled Accessories companion keeps the same pending stack in its canonical creature item; a real companion finishes considering a pending gift in place. Transfers and completed effects immediately refresh that snapshot. Death, lifecycle removal, activity preparation or an interruption returns an unconsumed real item exactly once. Picking up a Chao preserves its pending gift inside the saved creature instead of dropping a second copy.

The separate official comparison switch and optional source rig key are entity appearance settings. They survive saving and carrying without changing genes, ownership or care stats. Pure official rendering omits custom held-item additions; garden rendering displays the new item interactions.
