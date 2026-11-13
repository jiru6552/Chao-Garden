# Hero Swim 0.25.64 construction audit

The model starts from the original baby asset. Its 7 × 6 × 6 head box, face/eye meshes, expressions, original joints, native attachment pivots and small tail remain exact. Nine previously completed gameplay models are byte-identical to 0.25.63.

There is no imported official Hero Swim asset in this project. The actual construction references inspected are `chao_hero.geo.json`, `chao_hero_fly.geo.json`, `chao_hero_power.geo.json` and `chao_swim.geo.json`. Their plain cuboid vocabulary and four-part halo inform construction; their head geometry is not substituted.

The fresh independently operated viewer shows paired low draping flaps immediately after Swim first evolution. Four closed cuboids implement the two white/inherited lower roots and two colored upper sections. Long Run flaps turn down/forward, Swim flaps down/back; Power broadens upright/back paddles and opposite Fly raises the outside tips without changing the first Swim identity. Each upper section carries two real pigment regions on its existing faces. Tiny terminal upturns, curved taper and webbed feet remain approximations.

The shared rig adds only three mesh-only halo children. Seventeen morph boxes comprise four crown sections, five compact torso/hand/foot shapes, four small wing lobes and four halo bars. Constant wing directions preserve the exact proximal attachment edge; body extension supports the existing scoped walking guard. The original tail geometry and UVs are unchanged.

Source validation checked 9,773 dense/random maturity/axis blends and 3,518,280 arbitrary-rotation face pairs. All head features remain connected; no positive-area cofacial overlap, eye intersection or actual tilted-halo intersection was found. All cuboids retain positive volume and closed topology. The four longitudinal faces of both caps and all four wing lobes carry their expected proximal/distal pigment halves. These diagnostics are not Minecraft visual acceptance.

A private authoring projection exposed initially buried side caps. The new model was revised so the long Run/Swim cap bodies stay outside the fixed skull and then passed the complete source validator again. No previous model changed. Actual Minecraft visual and motion acceptance belongs to the separate current native cohort and its later review.
