# A finite mechanical Chao life - 0.21

Omochao remains a persistent `ChaoEntity` character with the mechanical rig and the existing owner, inventory, garden and companion systems. It never starts as a baby. A completed first build starts at zero adult age, fresh skill levels and progress, and coherent C/C genetic grade pairs for all five visible skills. It is not immortal. The existing loaded garden clock supplies its adult lifespan; carried and equipped companion time continues to pause the long-life clock just as it does for ordinary Chao.

The user requested this robot lifecycle as a Minecraft expansion. It is not claimed to be a mechanic from Sonic Adventure 2 or Chao World Extended.

## Four physical parts and one actor

A wrench-trained builder consumes four parts through the normal consideration and gift-animation transaction. Parts Boxes and recovered Robot Components each contribute one stage. A calibrated Robo Heart can replace one of those four parts, at any stage of a new construction. A second heart is refused and returned, rather than consumed or stacked for extra ranks. The same builder token, prototype UUID and owner persist through body, head, arms/key and completed robot stages. Missing or unloaded prototypes are never respawned.

The first and final stages snapshot the builder's actual appearance. That includes the independent gradual-evolution sliders and age strength, so an ordinary natural builder's current palette can be remapped onto the mechanical skin UVs. Fixed lenses and metal hardware retain their materials. New skill progress, learned lessons and animal parts are not cloned.

## End of life and reconstruction

At the end of a loaded adult garden life, happiness greater than 50 produces one Robo Heart; happiness 50 or lower produces one Robot Component. A real garden combat death yields a component. Managed companion defeat is handled by the existing temporary disable mechanic and yields neither item.

A heart advances uniform skill grades C → B → A → S, capped at S. All-S eligibility also checks all five actual grades. Robot grades cannot be raised through the Doctor or Hyper Fruit; drives, fruit, practice and activities can still train levels. Animal genes provide neither stats nor parts, including when the optional rare-animal rule is enabled. Direct illness setters, infection risk and old saved illness meters are suppressed.

One recovered part is inserted into the world before the old actor is discarded. If insertion is refused, the old body, equipment and garden slot remain, with a codec-backed pending outcome for retry after reload. Completion returns held and worn stacks once, removes the terminal robot's old garden ledger entry, and releases the loaded builder's construction claim. The normal Chao capture/rebirth UUID rules are unchanged. A recovered item's bounded origin token can also release the original builder's stale saved claim after that builder's chunk is loaded later, while a living actor with the claimed UUID blocks this route.

## Mechanical family records

The current Chao builder is the sole parent, recorded by stable lineage ID, compact DNA and its actual Unicode display name. On a first build the creator's own recorded parents can populate grandparents. When a heart is used, its previous creator becomes the robot's recorded grandparent; a changed or unchanged current creator is still its parent. The heart contains only the bounded next rank and previous-creator ledger, not an entire nested Chao save. A successor has a fresh actor and lineage identity. Older robots without a recoverable creator record keep the explicit unknown-name presentation; names are not invented.

## Compatibility and validation

The version-6 state codec stores robot migration initialization, pending outcome and committed-output flag independently of the bounded feature map, avoiding infinite age resets or lost pending drops when a legacy map is full. An old immortal robot receives one safe finite C-rank adult life while retaining trained levels. Later normalization never resets its earned grade, age or progress.

`OmochaoRulesTest` covers 191 independent assertions, including the actual state codec and full feature-map cases. `OmochaoLifecycleGameTests` exercises real physical output, rank generations, creator ancestry, ordinary gift conservation, denied item insertion/reload, equipment preservation, garden cleanup and original-builder restart. The material client probe checks actual static/dynamic inherited pixels, fixed hardware and aging builder palettes. Both completed server profiles passed 117/117 required tests. The final client completed 340 captures, including 14 actual Omochao material samples and the physical C→B→A→S assembly/recovery chain. Full-size robot PDF pages passed individual visual review; completed evidence is recorded in `TESTING.md`.
