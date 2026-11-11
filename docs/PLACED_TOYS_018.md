# Placed garden toys

Place a toy on the ground with its item. It faces the keeper placing it, stays
in the garden, and can be broken by hand to recover one item. All five current
Black Market toys and the three existing Ball, Rattle and Maracas IDs support
placement. Existing inventory stacks retain their IDs.

The five shop products retain their existing prices. Trumpet, Drum,
Jack-in-the-box, Radio and TV are physical objects with distinct models and
play frames. The box opens and its spring character pops up, drumsticks tap,
the radio shows a moving music display, and the television shows a short
animated Chao scene. Musical toys produce brief Minecraft instrument sounds;
these are playful garden interactions, rather than full recorded songs or a
general-purpose media player.

A Chao can choose a reachable toy within seven blocks horizontally and two
blocks vertically. Its personality and boredom affect its interest. Selection
occurs at a personal interval, so nearby Chao do not all begin on the same
tick. Hungry, exhausted, sleeping, eating, item-holding, recently petted,
cocooned, competing, summoned-companion and keeper-stay Chao attend to those
conditions before starting or continuing a session. There must be a clear
standing space beside the toy. Chao watch the TV from its screen side and
approach the jack-in-the-box from the side its spring character faces.

A completed session lasts eight seconds, raises happiness by four (seven for
a child's rattle), reduces boredom by 35 and costs two energy. Each Chao then
rests from toy play for roughly 30–40 seconds. Toy play does not award keeper
bond because it was not a direct keeper interaction. These timing and need
values are Minecraft adaptations, not claims about exact Sonic game timers.

Clicking a toy item directly at a Chao explains placement; it neither forces
play nor grants instant happiness. A toy is never copied into a collectible
item or temporary display entity while being used. Interrupted play grants no
completion reward. Breaking the block cancels its session and returns only
the actual block item. Loading a saved unfinished session cancels the
activity, retains the personal cooldown, and leaves the physical toy in place.

`art/build_toy_blocks_018.py` reproduces the native models, textures,
blockstates, item models, loot tables and block names. `ToyGameTests` covers
real item placement, one-item recovery, voluntary navigation and play, hunger
gating, interruption, animation frames and save/load ownership. The old
forced-toy test now checks that a click has no instant care effect.
