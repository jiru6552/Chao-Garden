# Native rig, palettes and motion — 0.18

The comparison mode selects one of the twenty original supplied Chao rigs and
its original PNG. `/chao appearance official [source]` changes the nearest Chao
to this view; `/chao appearance garden` restores the customized view. An omitted
source chooses the rig associated with that Chao's first evolution. This only
changes presentation: care, genes, inventory, ownership and learned abilities
are retained. The source PNG files and eight animation definitions remain
byte-for-byte copies of the supplied asset-only authoring inputs.

The native renderer had two conversion defects. The original box UVs were
mirrored on their wall faces and reversed at their poles, putting the yellow
end of a foot at its heel and giving the crest an inconsistent terminal color.
The Y rotation sign was also wrong. On the child source rig that made a wing
rooted at the back turn its tip forward over the torso. Neither defect required
changing the original model's pivots, cuboid dimensions or source image.

The correction follows the independently inspected primary Blockbench code:

- [Bedrock codec](https://github.com/JannisX11/blockbench/blob/master/js/formats/bedrock/bedrock.js)
  imports the source's X reflection and signed cube/group rotations.
- [Cube preview controller](https://github.com/JannisX11/blockbench/blob/master/js/outliner/types/cube.js)
  supplies the six box-UV rectangles, face corners and mirror behavior.

Combining that import with the Java model's downward Y axis gives model
positions `(-x,24-y,z)` and rotations `(x,-y,-z)`. The model renderer applies
the same complete ancestor chain to attachments as to the body itself.
`art/validate_native_rig_018.py` checks 6,472 transformed source corners across
all twenty rigs against an independent Blockbench-space calculation; the
maximum difference is below 0.000000000000004 model pixels. Three adjoining toe
faces and inherited-color/eye/alpha fixtures provide another fourteen checks.
The existing 19,746 source-parity checks also pass with the corrected convention.

Inherited two-tone colors formerly skipped the arms and feet entirely. The
native coat painter now changes the same skin hue on the torso, head and limbs
while retaining the source's contrasting tips and other accents. Monotone and
jewel coats cover all skin parts. Dark two-tone coats retain their charcoal
background and change the contrasting accent regions. Eye pixels and wing,
emotion and held-block texture regions are protected. Shiny skin uses the same
pigment as its ordinary counterpart; the separate material supplies reflection.
Moon and genetics-aware Chaos coats are finalized by `art/build_heritage_018.py`.

Gameplay walking now advances at a short distance-based stride rather than
stretching the one-second clip over many seconds of small vanilla limb deltas.
Petting samples use the render's fractional tick; hurt and attack reactions use
local reaction progress instead of the creature's lifetime clock. Full-body reaction clips
replace the idle/walk layer while they play rather than adding a second resting
arm pose. Smooth transitions include emotion changes and remember the actual
outgoing pose after held-item, attack and activity additions. The exact source
clip curves and timing remain available in comparison mode.

The item-handling poses distinguish thought, inspection, bites, training,
rejection and delight. The actual retained item is attached between the hands.
Confusion uses the original question-symbol geometry with a safe gameplay-only
UV sample from each rig's solid emotion-color patch. This avoids the source-unused
question cuboid's U=60..66 face wrapping into eye pixels; the original comparison
UVs are retained. All twenty patch samples are opaque. Training can show the
exclamation symbol, and delight uses the supplied happy clip. Eight placed toys
have small activity poses, including watching, tapping, blowing and reacting.
Saved personality and hunger can select short curious, quiet, energetic,
chatty, lonely or food-seeking gestures during otherwise quiet idle time.

Both secondary training axes change adult proportions continuously without
discarding the first evolution. Existing source crest cuboids, body and feet
change proportions, allowing the nine single/combined secondary outcomes to
coexist with genetic coats. These moderate, reversible shape changes are an
authored Minecraft adaptation of the gallery's gradual development; they are
not additional original Marketplace geometries or exact reproductions of all
Sonic Adventure 2 meshes. Comparison mode bypasses this deformation and all
animal, themed, headwear, extra-material and personality additions.

The integrated actual-client study completed 665 Minecraft views. Its runtime
probe passed 16,795 gameplay samples and 2,000 raw source samples. All twenty
source-rig captures were visually inspected, along with front/side/rear comparison,
coat palettes, front/rear wing and foot orientation, heritage, held-item and
pet/walk samples. No Chao pose or render exception occurred in the completed run.
The main release validation document records the full capture study; these are
bounded fixtures rather than a claim to have sampled every possible permutation.
