# Egg handling and hatching — 0.19

Reviewed on 30 October 2026. The modern SA2/Battle behavior is the target. The
placed shell remains this project's authored, unwrapped shell: the supplied
official Chao assets do not contain an egg model or egg animation.

The primary references are the [Egg Wiki](https://chao-island.com/wiki/Egg),
[hatching guide](https://chao-island.com/info-center/life-cycle/hatching.html),
and the current indexed [Genetics Wiki](https://chao-island.com/wiki/Genetics).
The live Wiki returned HTTP 403 during this review; the search service provided
the current primary-page text. The older April 2025 Genetics archive predates
the indexed personality-gene research and does not substantiate those newer
details by itself. The Wiki still labels its genetics discussion as a draft.

The modern source distinguishes waiting, gentle rocking followed by setting
down, and throwing against a wall. It explicitly says a floor impact or fall
does not count as throwing-to-hatch. Rough hatching starts the baby with about
ten less character bond and a brief dizzy response, without a happiness cost.
Personality and the initial face were already chosen before hatching. Current
genetics documentation describes hatching changes to the inherited Kindness
and Energy genes, rather than rerolling the newborn's expressed personality.

The implemented controls are:

- Use an egg on a free block face to place it for natural incubation.
- Right-click a placed egg to recover it. Sneaking is supported but not
  required, retaining the earlier recovery control.
- Hold use in the air without sneak to rock it. Eight accumulated seconds
  prepare it for hatching; release and place it. The placed egg wobbles and
  hatches after thirty more loaded ticks (or the configured hatch timer if
  shorter). Eight seconds and the thirty-tick
  settling animation are Minecraft timing choices within the source's roughly
  five-to-ten-second rocking description.
- Begin use while sneaking in the air, then release to throw. The mode is
  latched when use begins, preventing a mid-rock sneak press from accidentally
  throwing an individual egg. Charge lasts four to twenty ticks and changes
  speed. Punching an owned placed egg also launches it.
- A moving horizontal block collision hatches a thrown egg. A floor landing
  leaves it intact; after settling, ordinary incubation resumes. A stationary
  egg bumped into a wall does not acquire rough-hatch effects.

The actual egg entity performs the flight, gravity and collision checks. The
client has an interpolated hand-rocking transform and a spinning thrown shell;
the final incubation wobble gains a small squash-and-stretch motion with a
fading vanilla crack overlay. Rocked eggs begin this short settling animation
when set down. The End
portal surface and shiny shell rendering continue to use the existing material
paths. No white bottom-shell pedestal or empty carrier is introduced.

Hatching applies the selected gene change to a copied state exactly once, when
the creature's world spawn commits. Gentle hatching increments both Kindness
alleles and decrements both Energy alleles; a rough hatch does the inverse.
Alleles are clamped to their source range. Core rules preserve the previously
expressed sliders/face. Natural hatching changes neither pair. The rough bond
cost is applied once and happiness is left unchanged. The behavior controller
supplies a timed dizzy response after rough hatching.

Owner UUID, egg UUID, name, incubation, treatment, throw velocity, full source
item components and foreign custom data survive pickup, replacement and saves.
Handled/recovered individual eggs are consumed even in creative; infinite
creative catalogue eggs instead receive a new individual identity per normal
placement. Loaded duplicate UUID placement is rejected. This is not a global
anti-cheat system for edited or command-cloned NBT across unloaded chunks.

Themed crafting accepts every native plain, colored, shiny and jewel egg.
It validates the remaining recipe ingredients against a copied normal-shell
placeholder, then changes the original egg's registered shell and theme while
preserving its UUID, encoded genes, owner, history and all item components.
Live held Chao, spawn eggs, already themed eggs and two-egg ambiguous inputs
cannot replace the native egg ingredient. Missing-mod checks still apply to
matching and direct assembly.

A fresh catalogue coat has no hidden genes yet. Its color, shiny and jewel
properties pass through a temporary `NativeEggCoat` record, so recipe previews
remain stable and do not generate replacement creatures. First handling
freezes the original native coat and new hidden genes together, applies the
optional newborn-eye configuration once, and removes that temporary record.
Inherited and reincarnated eggs retain their existing eye genes.

Reincarnation alone records the original Chao body UUID in `RebornChaoId`.
That identity survives egg entity saves, item pickup and item NBT reload, then
is assigned before spawning the next-life body. Its existing garden row and
one reserved slot therefore follow it through the egg and into its next life.
Hatching first checks every loaded server dimension for an actor with that UUID;
if one exists, the egg stays intact without applying another gene or bond
change. Ordinary newborns still receive fresh body UUIDs. Actual natural death
continues to remove garden entries.

Reincarnation equipment in HEAD/OFFHAND/CHEST/LEGS/FEET and bounded relationship
records pass through egg entity saves and item recovery, then restore once to
the hatchling. Its old rebirth cocoon timer is cleared: a newly hatched creature
does not enter a second cocoon. One useful eggshell hat is left after a
successful hatch, dyed to the source coat and marked with its original coat
data. It is reserved to the keeper until picked up; it is not an empty carrier.

`EggInteractionGameTests` contains seven server cases: gentle readiness and
single gene commit; creative charge/real wall hatching; a floor landing that
does not hatch; saved flight with all five equipment slots, shell materials and
relationship restoration; owned egg recovery into full survival and creative
inventories without duplicate items; and duplicate loaded egg UUID rejection
followed by one natural hatch with unchanged genes; and a garden-bound actual
rebirth with tick40 cocoon removal, egg item/entity reload, a conflicting actor
in another dimension, and one next-life body retaining its sole garden row and
friendship identity. These cases still
require the coordinated full server run; writing them is not a claim that the
shared build has already passed.

Three additional `VariantEggRecipeGameTests` exercise actual world-recovered
blue, shiny red and Moon-jewel eggs with component/UUID conservation; fresh
native coat previews followed by real placement/state freezing; and rejection
of carried Chao, spawn eggs, themed wrappers, ambiguous eggs, wrong materials
and missing mod integrations. These also require the coordinated server run.
