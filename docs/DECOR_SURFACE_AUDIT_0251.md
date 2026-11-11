# Native school surface audit — 0.25.1

This audit covers every face in the finite registered COUNTER, DESK, SMALL, DRAPE, CURTAIN, WINDOW, PANEL, COLUMN, CABINET and BED model sets. Patterned image samples are compared with the physical face width and height after their actual UV crop. Uniform solid-color materials retain their intended use on narrow edges and cross-sections. Atlas scenery and the legacy static door-leaf compatibility pieces are separate from these furnishing surfaces; the new working doors have their own exact source-aspect/sliced-UV/hinge contract.

| Surface | Physical dimensions, model pixels | Correct source canvas / mapping |
| --- | --- | --- |
| Counter visible apron front/back and sides | 8 × 6 | 64 × 48, partitioned between full wooden posts |
| Principal desk visible apron front/back | 16 × 6 | 128 × 48 |
| Principal desk visible apron side | 8 × 6 | 64 × 48 |
| Counter / desk top | 16 × 16 / 24 × 16 | 64 × 64 / 96 × 64 |
| Counter / desk front rim | 16 × 2 / 24 × 2 | 128 × 16 / 192 × 16 |
| Medicine cabinet front | 24 × 32 | 192 × 256; side/back are solid blue |
| Exam-bed quilt top | 14 × 25 | 224 × 400; edges are solid pink |
| Main room curtain | 48 × 16 per segment | 192 × 64, cropped by actual fold coordinates |
| Legacy curtain | 26 × 32 | 208 × 256, cropped by actual fold coordinates |
| Artwork and labels | Actual inset dimensions | Insets preserve the source picture/text aspect |
| Three-piece entrance title | Each 32 × 18 | Three equal 192 × 108 image pieces |
| Globe continents | Actual stepped sphere faces | One spatially cropped map over the sphere, rather than a complete map repeated on each thin band |

Every solid furnishing element has all six closed faces. The only intentional one-sided elements in this set are wall image/seal decals, with complete wooden frame/back plates where applicable. Books have separate covers, page blocks and colored spines.

The apron is partitioned around the wooden posts; different external materials no longer share positive-area coplanar corner faces. Curtain cloth is likewise partitioned below and above the solid gold band, and no longer contains a painted gold stripe at a different height from the real tie. The tassel connects to the front gold band, in front of the cloth. Principal-paper ink and the Fortune card mark now have at least 0.05 model pixels of depth separation from their backing planes.

The finite native-resource validator checks every patterned face ratio, required material bindings, six-face closure, globe-map continuity across its stepped bands, front tassel connection, and paper/card separation. Malformed fixtures reject the original stretched counter mapping, missing furniture back, full-image repeated curtain fold, repeated globe band and rear-facing tassel.
