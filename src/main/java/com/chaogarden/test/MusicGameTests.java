package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.music.GardenMusicContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class MusicGameTests {
  @GameTest(template = "empty", timeoutTicks = 60)
  public static void gardenDiscPlaysFinishesAndEjectsThroughVanillaJukebox(GameTestHelper helper) {
    var world = helper.getLevel(); var keeper = helper.makeMockPlayer(GameType.SURVIVAL);
    for(var area:com.chaogarden.core.AreaMusicRules0257.Track.values())if(area!=com.chaogarden.core.AreaMusicRules0257.Track.NONE){
      var id=net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(GardenMusicContent.area(area));
      helper.assertTrue(id!=null&&id.getNamespace().equals(ChaoGarden.MODID)&&id.getPath().equals(area.event()),
          "Area soundtrack event is missing from the actual registry: "+area);
    }
    var local = new BlockPos(2, 2, 2); helper.setBlock(local, Blocks.JUKEBOX);
    var position = helper.absolutePos(local);
    ItemStack disc = new ItemStack(ChaoGarden.item("chao_garden_music_disc"));
    helper.assertTrue(disc.has(DataComponents.JUKEBOX_PLAYABLE), "Garden CD lacks its vanilla jukebox component");
    var songHolder = JukeboxSong.fromStack(world.registryAccess(), disc).orElseThrow();
    var song = songHolder.value();
    helper.assertTrue(song.soundEvent().value() == GardenMusicContent.MUSIC.get()
        && Math.abs(song.lengthInSeconds() - 115.809592f) < .001f && song.comparatorOutput() == 7,
        "Garden CD resolved the wrong recording, duration or comparator strength");

    JukeboxPlayable.tryInsertIntoJukebox(world, position, disc, keeper);
    var jukebox = (JukeboxBlockEntity) world.getBlockEntity(position);
    helper.assertTrue(disc.isEmpty() && jukebox != null && jukebox.getSongPlayer().isPlaying()
        && jukebox.getTheItem().is(ChaoGarden.item("chao_garden_music_disc"))
        && world.getBlockState(position).getValue(JukeboxBlock.HAS_RECORD)
        && jukebox.getComparatorOutput() == 7,
        "Normal jukebox insertion failed to consume, store or start the Garden CD");

    // Exercise vanilla completion at the actual track boundary without waiting
    // two real minutes for every server test run.
    jukebox.getSongPlayer().setSongWithoutPlaying(songHolder, song.lengthInTicks() + 19L);
    JukeboxBlockEntity.tick(world, position, world.getBlockState(position), jukebox);
    JukeboxBlockEntity.tick(world, position, world.getBlockState(position), jukebox);
    helper.assertTrue(!jukebox.getSongPlayer().isPlaying()
        && !jukebox.getTheItem().isEmpty(), "Track completion consumed the reusable CD or failed to stop playback");
    jukebox.popOutTheItem();
    var dropped = world.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2),
        entity -> entity.getItem().is(ChaoGarden.item("chao_garden_music_disc")));
    helper.assertTrue(dropped.size() == 1 && dropped.getFirst().getItem().getCount() == 1
        && jukebox.getTheItem().isEmpty() && !world.getBlockState(position).getValue(JukeboxBlock.HAS_RECORD),
        "Jukebox ejection did not return exactly one Garden CD");
    dropped.forEach(ItemEntity::discard); keeper.getInventory().clearContent(); helper.succeed();
  }
}
