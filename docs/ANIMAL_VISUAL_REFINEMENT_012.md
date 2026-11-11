# Animal part visual refinement

This revision keeps the existing eight independent slots, one compatible new
part per ordinary sample, mix-and-match behavior, save data and Chaos rules.
It changes the authored cuboid attachments rather than their gameplay effects.

- Rabbit ears now have a substantial root, long outer ear, narrower stepped cap
  and separate pink inset face. They extend above the head instead of reading
  as short solid posts. Rabbit paws and the small squared pompom tail match.
- Feline ears have a broad root, a smaller top step and contrasting inset;
  round and floppy mammal ears have species-appropriate wider silhouettes.
- Bird crests have separate tipped feathers, wings have three stepped feathers,
  and the tail fans have five individually visible, accented feathers.
- Long mammal tails, the skunk's striped bush, otter paddle, hoof splits, paw
  digits, larger feet, the unicorn spire and curled sheep/dragon horns make
  individual supplied slots easier to identify at game distance.
- Hands gain volume below the previous shoulder edge, preserving the hanging
  neutral pose. The head and expression planes stay unchanged. Children retain
  the existing smaller part transforms; adults use full authored dimensions.
- Child muzzles shorten vertically while retaining their side placement.
  Horizontal scaling around the head center pulled Otter cheek whiskers inward
  across the eye columns. The actual animation pose validator now checks both
  adult and child muzzle/whisker bounds against the shipped eye and mouth
  geometry, and rejects the previous horizontal-scaling behavior.

All additions remain editable native Blockbench cuboids. Insets use opaque
animal-specific swatches at one texel per model unit, with a real positive
surface offset, preserving visible color without coplanar surface flicker.
The generator's `--entity-only` option rebuilds these rigs and entity atlases
without touching separately authored inventory sprites.
