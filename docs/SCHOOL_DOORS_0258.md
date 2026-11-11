# School doors 0.25.8

The six ornate door families open in the opposite direction while retaining their 0.25.7 edge placement. Closed panels remain at local depth 14..16 pixels. Their front edge at depth 16 is the physical pivot, keeping opened thickness inside the jamb rather than cutting through its colored trim.

The outer leaf edges meet the inner jambs at x=-14.5 and x=30.5 pixels; the previous quarter-pixel vertical slit is removed. Crown clearance is reduced to 0.01 pixels. Front/back art follows the rigid outward swing with the native face vertex order, without mirroring the whole texture.

A 22.5-pixel-wide panel reaches depth 38.5 pixels when opened. Each ornate pair therefore owns 21 bounded native cells: six leaf roots, three center members, six first forward tips and six far forward tips. Empty cells reserve the actual swing footprint. Selection, collision and render geometry remain within each owning voxel.

The tool cabinet retains its existing 15 cells, moving leaves, rectangular metal case and textures. Its shared enum's unused FAR_TIP states map to existing empty models. No FAR_TIP cells are placed for cabinets.

Saved 0.25.7 pairs with rear tips and older pairs with forward tips migrate on first use. Migration requires every saved part to match and every newly needed cell to be loaded and empty before any write. Clicking an old rear tip resolves the original center. Both historical and current complete pairs tear down as one item, preserving unrelated neighboring blocks. Existing six-root pairs retain conservative expansion.

Validation covers all four facings, both hinges, both open states, all four sections and three heights; 48 compiled geometry states, 288 state meshes, 288 native selection/click probes, 336 independent crown/jamb probes and 408 actual current/historical destruction fixtures. Native client validation additionally checks ordinary Adventure prediction and the server acknowledgement for all 21 ornate cells. Final release reports record actual results.
