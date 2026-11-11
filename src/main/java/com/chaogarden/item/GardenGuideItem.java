package com.chaogarden.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public final class GardenGuideItem extends WrittenBookItem {
  public GardenGuideItem() {
    super(
        new Item.Properties()
            .stacksTo(1)
            .component(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                    Filterable.passThrough("A Keeper's Journal"),
                    "Chao Garden",
                    0,
                    List.of(
                        page(
                            "A Keeper's Journal\n\nStart with one egg and a little fruit. Buy a base-colour egg from a "
                                + "Black Market Trader, or find one very rarely in chest loot. Eggs cannot be crafted.\n\nPlace "
                                + "the egg and stay nearby: it normally hatches after five loaded minutes. Then open First "
                                + "care. Use the chapter list to learn one task at a time; search finds chapter and section "
                                + "names."),
                        page(
                            "First care\n\n1. Right-click your Chao with an empty hand to pet and claim it.\n2. Offer "
                                + "fruit with right-click, then wait while it decides and eats.\n3. Let it rest when "
                                + "tired.\n\nKeep caring for it to build a bond; bonded Chao follow their keeper. "
                                + "Sneak-right-click with an empty hand to carry it. Name tags work normally."),
                        page(
                            "Care supplies\n\nFor a first meal, offer fruit. It restores fullness and trains Stamina; "
                                + "Chao Fruit trains all five stats. Other fruits have different care effects.\n\nTo train a "
                                + "chosen ability, use a drive: enemies have a 5% chance to drop one base drive, guaranteed for kills credited to a keeper with an equipped Chao. Chest loot can contain "
                                + "Animal Gene tubes; mythical samples are a little less common. See Training for drive "
                                + "colours."),
                        page(
                            "The Analyzer\n\nRight-click a Chao with an Analyzer when you want to understand what "
                                + "changed.\n\nStart with STATS for needs, ability points and Favorite Fruit. GENES explains "
                                + "inherited traits and growth. PARTS separates visible body parts from the animal DNA "
                                + "checklist. MIND shows personality, feelings, health and family. CARE holds care and "
                                + "wardrobe tools. Hidden genes stay saved even when they are not visible."),
                        page(
                            "Training\n\nChoose an ability, then offer its drive:\nGreen: Run | Yellow: Swim\nPurple: Fly | "
                                + "Red: Power\n\nEach 100 progress earns a level, up to 99. Grades E to S determine how many "
                                + "points a level earns. Points unlock garden movement and choose the first form; levels "
                                + "govern companion perks. Animals train several abilities. Check the Analyzer after the gift "
                                + "finishes."),
                        page(
                            "Learning to move\n\nWatch Run POINTS in the Analyzer:\n50: walk\n667: run\n1334: dash\n\nA "
                                + "crawling newborn is normal. Offer green drives or Run animals and let it finish using "
                                + "them. Reborn Chao may keep enough points to walk immediately. A high level alone does not "
                                + "guarantee a movement threshold."),
                        page(
                            "Learning to swim\n\nTrain with yellow drives or Swim animals, then check Swim POINTS:\n100: "
                                + "paddle\n667: stronger stroke\n\nBelow 100, Chao flail and stay afloat. These thresholds "
                                + "describe the Chao's garden movement, not your equipped companion's water perks. For those, "
                                + "see Companion flight and water."),
                        page(
                            "Learning to fly\n\nFor garden glides, train Fly to 135 points with purple drives or Fly "
                                + "animals. Chao glide briefly from ledges; they do not hover or take off from flat "
                                + "ground.\n\nEquipped adults work differently: double jump while airborne to start elytra "
                                + "flight. Fly LEVEL extends its timer; level 99 removes the timer. See Companion flight and "
                                + "water."),
                        page(
                            "Growth and alignment\n\nChildhood takes up to three loaded hours by default. Eating whole "
                                + "fruit speeds growth, so adulthood can arrive sooner. Adult life normally lasts twelve more "
                                + "loaded hours. Time stops when unloaded or carried.\n\nBefore adulthood, use Hero Garden or "
                                + "Dark Garden Fruit to influence alignment. Check GENES / GROWTH as you raise your Chao: "
                                + "alignment locks at first evolution."),
                        page(
                            "Evolution\n\nTo aim for a first form, compare ability POINTS in the Analyzer. The highest "
                                + "wins; the most recently increased tied ability breaks a tie. Stamina gives Normal.\n\nFor "
                                + "example, make Run points highest for a Run first form. Animals and drives also move "
                                + "separate shape influences: Swim versus Fly and Run versus Power. Those influences are not "
                                + "the same as levels or points."),
                        page(
                            "Adult forms\n\nOnce your Chao becomes an adult, its first form stays fixed. Later training "
                                + "changes its secondary shape gradually as it matures.\n\nYou can combine one Swim/Fly "
                                + "direction with one Run/Power direction. Opposites cannot be secondary directions together. "
                                + "A first Swim form can still develop a Fly secondary. Check GENES / GROWTH to follow the "
                                + "change."),
                        page(
                            "Breeding\n\nGive Heart Fruit to two eligible adults and keep them close. After they lay an "
                                + "egg, wait five minutes before trying again.\n\nChildren and Omochao cannot breed. By "
                                + "default, immortal Chaos Chao and special character forms cannot breed either. The server's "
                                + "optional rare-breeding setting changes rare-form eligibility, but never allows Omochao "
                                + "breeding."),
                        page(
                            "Inheritance\n\nA baby's coat and stat genes come from its parents. Nether and End heritage "
                                + "can pass on too. Training, alignment and evolved forms do not pass on.\n\nNew mod-addon "
                                + "heritage is paused; existing living Chao keep their saved heritage. Omochao construction "
                                + "is available separately: see Robot legacy. Use the Analyzer to compare visible traits with "
                                + "hidden genes."),
                        page(
                            "Special eggs\n\nAll egg crafting is disabled. Nether eggs rarely appear in Nether chest "
                                + "loot, and End eggs in End chest loot.\n\nNew mod-addon Chao cannot be obtained yet. Existing "
                                + "addon eggs keep their data and can be recovered, but do not hatch during this pause."),
                        page(
                            "Animal Genes\n\nOffer one of the 21 SA2 DNA tubes, wait for it to finish, then check the "
                                + "Analyzer's animal DNA list. Each species counts once for this life. Chaos evolution needs "
                                + "all 21, not every visible part.\n\nAn ordinary tube adds one new compatible part. Repeat it "
                                + "for a fuller set or mix species. Ears and horns from different animals conflict; Skeleton "
                                + "Dog clears parts."),
                        page(
                            "Reincarnation\n\nAt the end of ordinary adulthood, happiness above 50 lets a Chao return as "
                                + "an egg. Unhappy Chao may pass away. Omochao instead follows A robot's next life.\n\nThe new "
                                + "life keeps genes, grades, name and 10% of points. Levels reset to 1. Hero/Dark class, "
                                + "learned skills, equipment and friends survive; some parts remain. Personality and this "
                                + "life's animal checklist reset."),
                        page(
                            "Chaos Chao\n\nPrepare before first evolution in the third life or later:\n1. Finish all 21 "
                                + "SA2 animal samples in that life.\n2. Keep happiness above 80.\n3. Aim for Normal by making "
                                + "Stamina points highest.\n\nCheck the Analyzer's DNA checklist for missing species. Neutral "
                                + "becomes Light, Hero becomes Angel, and Dark becomes Devil. Chaos Chao are immortal and "
                                + "cannot breed under default rules."),
                        page(
                            "Safe transport\n\nSneak-right-click your Chao with an empty hand to carry it. Use the "
                                + "carried Chao on a block to release it. Its name and saved data travel with it, and its "
                                + "life clock pauses while carried.\n\nFor an egg, right-click the placed egg to recover it. "
                                + "Its genes and incubation progress stay intact. See Handling eggs before rocking or "
                                + "throwing one."),
                        page(
                            "Minecraft adaptations\n\nTraining changes what your Chao can do here: Run points improve "
                                + "speed, Stamina improves health, and Power improves armor. Swim helps water movement; Fly "
                                + "unlocks ledge glides.\n\nNeeds matter too. Hungry Chao seek nearby fruit; tired Chao rest. "
                                + "If play or gifts are not working, check needs before adding more training. See Choosing a "
                                + "gift."),
                        page(
                            "Pack makers\n\nServer config sets hatching, life stages and natural death.\n\nLoot uses "
                                + "additive modifiers.\n\nCommands:\n/chao inspect\n/chao testkit (OP)\n/chao mature (OP)"),
                        page(
                            "Fruit orchards\n\nLook for Garden Fruit in the Overworld and Dark Garden Fruit in the Nether. "
                                + "With The Aether installed, Hero Garden Fruit grows on its islands. Right-click ripe hanging pods to harvest; they regrow.\n\nTo grow your "
                                + "own, craft non-mushroom fruit into a seed. Leave seven blocks of height and a clear 5 by 5 "
                                + "canopy. Bone meal speeds growth and can upgrade an old small tree if there is room."),
                        page(
                            "Learning gardening\n\nTeach the tool skill first: buy a Shovel for 20 emeralds or a Watering "
                                + "Can for 25 at the Black Market, then offer it to your Chao.\n\nFor planting, the Chao needs "
                                + "the shovel skill and at least 50 Run points. Give it a seed and leave clear soil nearby. "
                                + "If it cannot plant, check its training and the tree space described in Fruit orchards."),
                        page(
                            "Watering trees\n\nUse a Watering Can on a seedling or tree. It helps seedlings grow, ripens "
                                + "fruit and adds a little life to planted trees.\n\nA Chao taught the watering skill may water "
                                + "nearby trees when idle. Give it time between other activities; learning the skill does not "
                                + "make every watering happen immediately."),
                        page(
                            "Planted-tree life\n\nPlan room for four planted trees per garden. Ordinary planted trees "
                                + "live 60 active minutes; Strong trees live 120, then wither. Watering adds a little "
                                + "life.\n\nAging stops without nearby players. Original trees and Garden, Hero Garden and Dark "
                                + "Garden fruit trees are permanent, so these are useful long-term garden anchors."),
                        page(
                            "Black Market\n\nA travelling Chao merchant visits near players. All 16 base-colour eggs are "
                                + "always in stock and never sell out. Normal costs 2 emeralds; other colours cost more.\n\nOne "
                                + "shiny colour rotates at a time. A gem offer appears on only one in 1,000 restocks."),
                        page(
                            "Rare egg discovery\n\nBase eggs can very rarely appear in any chest loot. Normal is slightly "
                                + "more likely than each individual colour.\n\nShiny eggs are very rare outside the Overworld, "
                                + "and in buried treasure, ancient cities, strongholds, trial chambers, desert pyramids and "
                                + "jungle temples."),
                        page(
                            "Gem eggs\n\nThe 13 gem coats, including Moon, are extremely rare in ancient city, desert "
                                + "pyramid, stronghold, bastion and End city treasure chests.\n\nWith a configured space mod "
                                + "installed, Moon eggs instead come only from chest loot on its configured surface Moon, "
                                + "never the market or orbit."),
                        page(
                            "Other market gifts\n\nFruit, seeds, hats, accessories, lenses and toys rotate with stock. "
                                + "Drives cost 1 emerald with unlimited stock. Three ordinary DNA offers cost 1 emerald (4 "
                                + "uses); one mythical offer costs 2 (2 uses).\n\nHats and clothes are considered gifts; mood "
                                + "affects acceptance. Shears remove hats. Place toys on the ground for Chao to discover."),
                        page(
                            "Your gardens\n\nPlace a Chao Garden device, open it and press Activate garden. This marks "
                                + "the device's chunk and its eight neighbouring chunks. No enclosure or tree is required. "
                                + "Owned Chao placed inside are detected automatically; Garden registration still manages residents."),
                        page(
                            "Garden registration\n\nCarry a Chao you own, then open your Chao Garden. Drag the carried "
                                + "Chao's inventory icon into one of six boxes. This assigns it without taking the real Chao "
                                + "from your inventory.\n\nNow release it inside the garden to settle. Left-click a registered "
                                + "icon to remove that assignment. Registration and release are separate steps."),
                        page(
                            "Garden boundaries\n\nNew gardens cover a 3 × 3 chunk square at every buildable height. "
                                + "Older saved enclosures keep their boundaries until you use Recheck garden. Breaking the device "
                                + "releases its assignments.\n\nThe Neutral, Hero and Dark garden dimensions still cover their whole garden."),
                        page(
                            "Building a race\n\nBuild a straight row of Start Line posts and place a Finish Line. Use a "
                                + "Race Marker on the start or last flag, then place the next flag.\n\nFollow the visible "
                                + "connection and keep each leg no longer than 32 blocks. End the route at the Finish Line. "
                                + "Once the course is valid, right-click its Start Line to open a lobby."),
                        page(
                            "Races and karate\n\nFor karate, place one Karate Mat at the centre of clear 7 by 7 ground; "
                                + "it lays the whole tatami carpet. Right-click the mat, or a valid race Start Line, to open "
                                + "a lobby.\n\nAdd Chao from your registered gardens to free slots. Other keepers can add their "
                                + "own Chao; only the host starts. Participants gain training and return home afterward."),
                        page(
                            "Chao companions\n\nWith Accessories installed, carry a healthy Chao and equip it in the Chao "
                                + "Companion slot. Babies ride on your head, or your back during flight and swimming, and grant no benefits.\n\nAn adult follows and "
                                + "defends you. Adults without Run ride on your head; without Swim they cling to your back in water. Movement unlocks "
                                + "use earned points; bonuses use levels. For controls and thresholds, open Companion flight and "
                                + "water."),
                        page(
                            "Companion flight and water\n\nWith an adult companion equipped, double jump while airborne "
                                + "to start elytra flight. Fly level 99 removes the timer; flight still needs energy.\n\nSwim "
                                + "level 70 adds underwater night vision; level 99 adds water breathing.\n\nAn all-S Omochao "
                                + "grants elytra and a boost key, default R, with a five-second cooldown. The assisting Chao "
                                + "holds onto your back."),
                        page(
                            "Nether companions\n\nAn adult Nether Chao in the Accessories slot grants fire and lava "
                                + "immunity. Its temporary body is protected too.\n\nThis trait works even when tired, but "
                                + "stops if the Chao is unequipped or disabled."),
                        page(
                            "End companions\n\nAn equipped adult End Chao blocks levitation and prevents Endermen "
                                + "becoming angry from eye contact. Attacking still allows retaliation.\n\nPress V to open your "
                                + "real Ender Chest. Rebind it in Controls. Access ends when the companion is unavailable."),
                        page(
                            "Robot legacy\n\nFirst earn five race wins with the builder Chao to teach its wrench ability. "
                                + "Then offer four Parts Boxes, keeping it near the unfinished robot. One calibrated Robo "
                                + "Heart can replace one box.\n\nA new Omochao starts as a young adult with all C grades. It "
                                + "cannot get sick or breed, and ignores Animal Genes. See A robot's next life for "
                                + "rebuilding."),
                        page(
                            "A robot's next life\n\nKeep an Omochao's happiness above 50 when its adult life ends to "
                                + "recover one Robo Heart. Otherwise it leaves one Parts Box.\n\nUse the heart with three Parts "
                                + "Boxes and a wrench-trained builder. Hearts raise the rebuilt robot's grades from C to B, "
                                + "then A, then S. This is a new robot: its builder is its parent and the old creator becomes "
                                + "its grandparent."),
                        page(
                            "Companion recovery\n\nA defeated equipped companion rests for one minute, then returns "
                                + "automatically.\n\nIt loses 10 happiness and one level in a random trained ability. That "
                                + "ability also loses points and its current progress resets. Genes and name stay saved. "
                                + "Unequipping safely returns the companion to carried form."),
                        page(
                            "Choosing a gift\n\nOffer one item, then wait. Your Chao takes the real item into its hands "
                                + "and thinks before using it. Benefits arrive only after the accepted action finishes. Full "
                                + "Chao may throw food away.\n\nNo response? Let it finish its held item, rest, play or "
                                + "activity first. Check ownership and needs in the Analyzer. Training supplies and clothing "
                                + "also take time."),
                        page(
                            "Curious hands\n\nA dropped item can catch a Chao's attention. If it does not recognise the "
                                + "item, it inspects it and throws it back. You can pick it up again.\n\nRejected items have a "
                                + "short pickup delay, so the Chao will not immediately grab them again. Gifts already in its "
                                + "hands survive saving and carrying; wait for the decision rather than offering duplicates."),
                        page(
                            "Personalities\n\nIf two Chao react differently to the same toy or gift, check MIND in the "
                                + "Analyzer. Personality, feelings and health help explain their choices. Several strong "
                                + "tendencies can cycle through the Doctor's labels.\n\nStart by meeting needs, then place toys "
                                + "within reach with clear space around them. Hungry or tired Chao may decline even when they "
                                + "know how to play."),
                        page(
                            "Portable adventure fruit\n\nThe Black Market rotates one Chao Adventure 2 food in addition "
                                + "to its ordinary stock. Ten additions include Energy, Peace, Smart, Wonder and Forbidden "
                                + "Fruit.\n\nCraft a purchased fruit into its seed to grow more."),
                        page(
                            "Kindergarten travel\n\nCraft a Chao Teleporter, place it with clear space above and step "
                                + "inside. It takes you to the hub in adventure mode.\n\nWalk through the blue Kindergarten "
                                + "arch. The central tube takes you back home and restores your previous game mode."),
                        page(
                            "Meet the staff\n\nCarry a Chao to the Teacher, Doctor or Fortune-Teller, then select it from "
                                + "the list. Nearby Chao you own also appear.\n\nChoose the Teacher for lessons, the Doctor for "
                                + "health, and the Fortune-Teller for names. The Principal gives care advice; the Bulletin "
                                + "Board lists classes. A permanent Black Market serves the west room."),
                        page(
                            "Doctor care\n\nFor illness treatment, carry your Chao to the Kindergarten Doctor, select it "
                                + "from the list and choose SEE THE DOCTOR on the main menu. Treatment is free.\n\nUse the chart's separate tabs "
                                + "for stats, personality and earned contest records. Medicine gifts and old Doctor Stations "
                                + "still work. Raising a grade is a different, paid service: see Grade care."),
                        page(
                            "Grade care\n\nAt the Kindergarten Doctor, select your Chao and open MEDICAL CHART, then "
                                + "STATS.\n\nChoose an ability at level 99 with a grade below A, then select UP beside it. You "
                                + "can raise a grade by one step up to three times per life.\n\nCheck the fee before "
                                + "committing: it rises with the old grade. The upgrade resets that ability's level, progress "
                                + "and points, so you must train it again. Hidden unexpressed alleles stay saved. Omochao "
                                + "grades improve through rebuilding instead."),
                        page(
                            "Classroom\n\nBring a carried, healthy, fed Chao to the Kindergarten Teacher. Choose a lesson "
                                + "and LEAVE CHAO to board it safely. Four students can share the room.\n\nBy default, offers "
                                + "rotate every 30 minutes and lessons take 10. Lessons continue while you leave or log out. "
                                + "Return for TAKE CHAO when finished. The Analyzer shows lesson information; enrollment "
                                + "happens at the Teacher."),
                        page(
                            "Learned skills\n\nLessons teach activities, not stat XP: dances, exercise, song, drawing and "
                                + "instruments. Song and drawing reach level five; learning all four dances unlocks "
                                + "breakdancing.\n\nAfter a first instrument lesson, collect your student to receive one "
                                + "placeable instrument. Leave an inventory slot for your Chao; overflow instruments drop "
                                + "beside you. Place the instrument where Chao can reach it."),
                        page(
                            "Wardrobe\n\nOpen CARE / WARDROBE and choose a real item from your inventory for a hat, head "
                                + "accessory, face, neck or body slot. Wait for the Chao's decision; happier Chao are more "
                                + "likely to accept.\n\nReplaced items return to you. Carry dye to recolour supported "
                                + "accessories. The medal button only displays awards this Chao has already earned."),
                        page(
                            "Handling eggs\n\nFor a gentle hatch, recover a placed egg with right-click. Hold use in the "
                                + "air for eight seconds to rock it, then place it again for a quick hatch.\n\nTo throw "
                                + "instead, sneak and hold use, then release. A wall impact hatches the egg roughly; a floor "
                                + "landing does not. Gentle care avoids the rough-hatch bond penalty."),
                        page(
                            "Garden friends\n\nGive familiar Chao time together. They can greet, talk, gossip, hug and "
                                + "hold hands; relationships survive saves.\n\nLessons open more shared play: drawing, singing, "
                                + "dancing and instrument bands. For sandcastles, teach the shovel skill and leave sand "
                                + "nearby. Needs and personality still affect whether a Chao chooses to join in."),
                        page(
                            "Contest awards\n\nKeep entering races and karate to earn medals and garden rewards. These "
                                + "are Minecraft contest tiers on player-built courses. Five race wins also teach the wrench "
                                + "ability used in Robot legacy.\n\nTo display an earned medal, use CARE / WARDROBE. Selecting "
                                + "a medal cannot unlock one your Chao has not won."),
                        page(
                            "Expanded garden gifts\n\nCWE-inspired fruits, SADX animal samples, lenses, accessories and "
                                + "placeable toys expand the garden. The Black Market rotates a small selection. Obtain a new "
                                + "fruit before crafting its seed.\n\n Special gifts still pass through the Chao's held "
                                + "decision animation."),
                        page(
                            "Birthday parties\n\nLet adult Chao spend loaded time with friends to see birthday "
                                + "celebrations. Friends nearby may join. If the host's headwear slots are clear, it wears a "
                                + "temporary party hat.\n\nA completed host party gives one marked cake serving for the Chao to "
                                + "consider and eat. A year is one loaded Minecraft day by default; the server can change or "
                                + "disable birthdays."),
                        page(
                            "Optional garden rules\n\nThis journal describes the default rules. If your server behaves "
                                + "differently, ask its owner which options are enabled.\n\nOptional settings include rare Chao "
                                + "breeding and parts, nonlethal bullying, stronger CWE fruit effects, monster development "
                                + "and newborn iris colours. These start disabled; ordinary care and rare-form restrictions "
                                + "remain the default."),
                        page(
                            "Official comparison (OP)\n\nCompare original addon visuals with the garden version on the "
                                + "nearest Chao:\n\n/chao appearance official\n/chao appearance garden\n\nAdd a suggested rig name "
                                + "after official to inspect another source model. Genetics stay saved.")),
                    true)));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (player instanceof ServerPlayer serverPlayer) {
      stack.set(DataComponents.WRITTEN_BOOK_CONTENT, getDefaultInstance().get(DataComponents.WRITTEN_BOOK_CONTENT));
      if (WrittenBookItem.resolveBookComponents(
          stack, serverPlayer.createCommandSourceStack(), serverPlayer))
        serverPlayer.containerMenu.broadcastChanges();
      // Vanilla ServerPlayer.openItemGui only accepts the vanilla written book.
      // The client reader itself supports any WRITTEN_BOOK_CONTENT component.
      serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
    }
    return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
  }

  private static Filterable<Component> page(String text) {
    return Filterable.passThrough(Component.literal(text));
  }
}
