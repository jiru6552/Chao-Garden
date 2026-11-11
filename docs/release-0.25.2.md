# Chao Garden 0.25.2

This refinement follows the thirteen new feedback screenshots and the supplied game references. It closes the entrance above the stepped door arch, aligns the selectable door surfaces with their visible panels, corrects reverse-face door artwork, repairs the Doctor's moustache lighting, and gives each destination hallway its own materials and lighting.

The Fortune-Teller approach uses deep purple walls and fabric, dark carpet, astral gold trim, a star ceiling and subdued lanterns. The Clinic adds a mint-and-white waiting and registration area. The Classroom approach displays student work and class pictures; the Principal's route and office display awards and certificates. An unmarked cabinet hides the Black Market passage, whose dark inset shelves display models of actual possible shop goods. These displays cannot be collected and do not change shop inventory. The public entrance stays welcoming and bright.

The fixed merchant is recovered from any loaded displaced position and returned to his counter with the same identity and purchased stock. Old falling/invisible states are cleared. The client accepts the resident before his synchronized resident flag arrives. His nameplate stays hidden from the public hall; normal interaction opens the shop inside. The back-wall bottles and books sit on slim supported shelves.

The first Neutral Garden has a 49-by-41-block bounding box, compared with the previous 101-by-91-block box. Its shaped terrain uses 1,745 columns instead of 8,279, a reduction of about 79%. The waterfall, pond, cave exit, low transporter ridge and palms follow their relative positions in the supplied SA2/Battle views. Scale is inferred from the character, cave and palm relationships rather than measured original game geometry. Migration checks space before changing the terrain, preserves supported care assemblies, registered providers and route metadata, and places bodies on clear dry ground. Water toys remain in water. If an assembly cannot fit safely, migration waits. An unfinished marker draft whose physical start has moved is cleared on use with a message to select the starting line again.

The central lobby's spiral discs write depth, and its glazing and energy ribbons share a sorted translucent pass after the portal surfaces. Foreground tubes stay visible over distant portal apertures. The garden gate's statues have floor-connected plinths and pointed heads. Its open vases contain raised foliage and visible flower stalks, with clearance around the rim.

The Doctor's darker moustache lobe came from seven inward-facing surface normals after its geometry was reflected. Reversing those face windings corrects the lighting without changing the silhouette, material, eyes, idle behavior or service gestures. The Fortune-Teller's eyes stay closed.

Current validation and native evidence are documented in [TESTING.md](../TESTING.md). The new [reference brief](REFERENCE_REFINEMENTS_0252.md) records the user angles that the earlier broad capture did not adequately cover. Prior release checks are historical evidence and do not substitute for the current real-ray door checks or new close views.

Existing Chao data, two-sided framed portals, Adventure-mode care, wildlife restrictions and safe return behavior continue from 0.25.1. The scenery revision is 27, which updates the rooms when an existing save is next prepared.

## Source and preview retention

The complete download contains editable code and assets, all thirteen new reference screenshots, current native captures and validation, and the earlier source reference evidence. Its local gallery uses media within the extracted archive. The separate standalone gallery embeds its media for viewing without sidecars or a network connection.

Generated native screenshots, clips and visual-review sheets from 0.25.1, and the older broad world/room screenshots, remain in the previously saved 0.25.1 complete release. They are omitted from this source export to avoid duplicating prior studies. Their textual records retain their historical labels. The nine separately identified 0.25.0 baby clips, timed native frames and provenance remain historical; the earlier full native recording, derived GIFs and contact-sheet duplicates remain in the prior complete release. The large original recordings are represented by their provenance hashes and selected source frames rather than duplicated in each export.
