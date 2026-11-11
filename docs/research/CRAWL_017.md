# Crawl reference and implementation, 0.17.0

The earlier controller had a pitched crawl pose, but its hands travelled only 0.35 model pixels and its knees 0.25 pixels at full movement weight. Ordinary newborn walking-animation amounts are much smaller than the upright test inputs, so those movements became almost invisible. Its distance-based cycle also spanned roughly 1.4 blocks, far longer than the body.

The replacement retains the point-based crawl requirement and uses a shorter, distance-driven stride. Each hand has a 60% contact phase and a 40% lifted recovery; opposing hands alternate. Hand travel spans 2.1 model pixels and recovery rises 0.85 pixels. Opposing rear feet push and tuck, the shoulders rock and bob, and the low head remains upright with a small nod. Movement weighting is calibrated to the approximately 0.022 limb amount observed for an actual newborn, rather than upright movement speed. Stopping settles the hands and knees into support; game time does not keep advancing the gait.

Reference footage was visually inspected from the gameplay GIF at [Tenor](https://tenor.com/view/oomfie-scrungo-chao-omw-gif-22210069), [direct animated GIF](https://media1.tenor.com/m/T9Y8Qv4zok4AAAAC/oomfie-scrungo.gif). It shows a gold Chao with its head low and upright, alternating forelimb contact and recovery, a visible shoulder/head bob, and bent rear feet that lift and tuck. The GIF is a secondary presentation of game footage and may alter playback speed; its exact timer is not treated as canonical. Reference frames are review inputs, not game assets or redistributed production textures.

At the 0.16 capture's approximately 0.0055 blocks/tick, the new cycle takes about 30 ticks (1.5 seconds) for both sides. The cycle remains tied to travelled distance. The rate increases only when the entity actually travels faster.

Validation:

- The existing development client probe now also exercises 20 rigs at low movement input, checking a visible hand reach/lift, an opposite supporting hand, finite poses and a stopped crawl that settles.
- The bounded cuboid diagnostic samples 49 phases across 11 ordinary native rigs and 18 authored arm sets (14 equip-compatible). It measures 2,156 native and 44,198 fitted arm/head combinations, with zero contacts deeper than 0.05 model pixels.
- The head sits 0.7 model pixels higher than the old static pose to clear the newly lifted forelimbs. It remains low and forward of the pitched torso.

The cuboid check holds breathing at its midpoint and excludes look, reactions, transition blends, noncuboid features and arbitrary animal combinations. It is a targeted regression diagnostic; actual-client review remains necessary.
