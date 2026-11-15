package com.chaogarden.companion;

import com.chaogarden.ChaoGarden;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;

/** Alignment classification is deliberately narrower than the existing drive
 * reward rule. A neutral creature's defensive aggression is not a moral type.
 * This helper grants nothing and never changes kill credit, drops or behavior. */
public final class BabyKillAlignment082 {
  public enum Kind { HOSTILE, PEACEFUL, UNCLASSIFIED }

  public static final TagKey<EntityType<?>> HOSTILE=tag("baby_alignment_hostile");
  public static final TagKey<EntityType<?>> PEACEFUL=tag("baby_alignment_peaceful");
  public static final TagKey<EntityType<?>> IGNORED=tag("baby_alignment_ignored");

  private BabyKillAlignment082() {}

  private static TagKey<EntityType<?>> tag(String path) {
    return TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,path));
  }

  /** Read at the qualifying death, including current server datapack tags.
   * Players/villagers and the explicitly hostile killer-bunny variant have
   * defined meanings. Players/villagers cannot be reclassified by tags. An
   * ignored ordinary type or conflicting opt-in tags opts out.
   * Unknown modded animals need a peaceful tag; CREATURE alone proves nothing. */
  public static Kind classify(LivingEntity victim) {
    if(victim==null)return Kind.UNCLASSIFIED;
    if(victim instanceof Player||victim instanceof AbstractVillager)return Kind.PEACEFUL;
    if(victim.getType().is(IGNORED))return Kind.UNCLASSIFIED;
    if(victim instanceof Rabbit rabbit&&rabbit.getVariant()==Rabbit.Variant.EVIL)return Kind.HOSTILE;

    boolean hostile=victim.getType().is(HOSTILE),peaceful=victim.getType().is(PEACEFUL);
    if(hostile&&peaceful)return Kind.UNCLASSIFIED;
    if(hostile)return Kind.HOSTILE;
    if(peaceful)return Kind.PEACEFUL;

    // Piglins and light-dependent spiders do not implement NeutralMob. Do not
    // infer a side for an unknown tamable creature or quest NPC either. A
    // modpack can explicitly classify a known variation with the tags above.
    if(victim instanceof NeutralMob||victim instanceof Piglin||victim instanceof Spider
        ||victim instanceof TamableAnimal||victim instanceof Npc)return Kind.UNCLASSIFIED;
    if(victim instanceof Enemy||victim.getType().getCategory()==MobCategory.MONSTER)return Kind.HOSTILE;
    return Kind.UNCLASSIFIED;
  }
}
