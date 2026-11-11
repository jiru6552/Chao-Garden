# Clinic refinement 0.25.4

The Family portrait now occupies the continuous west wall at `(14,82,-16)`.
Its full 1.5-block frame is backed by the wall, clear of the corridor elbow.
The Chao portrait keeps its previous supported mount.

Both examination beds retain the same two-block floor footprint, `x22..24`,
but their anchors move to `x23` and face west. Their pillows now sit at the east
wall end. The native bed mesh is built from nonoverlapping posts, rails, pillow
and quilt, with internal touching faces removed. This removes the coplanar green
and white corner skins visible in the supplied photos. The quilt begins where
the pillow ends, and its texture has the matching 14:23 physical aspect.

The diagnostic machine explicitly faces south toward the doctor's chair and
patient side, using its existing directional blockstate.

The right Clinic curtain moves to `z-11`; its native three-block width ends at
the inside face of the south wall, `z-9`. The Clinic now uses physical tie segments
at `y81`. Right Clinic and Principal ties select the east variant; left ties keep
their original west variant. The finite east tie model reflects its depth and
front/back face normals before its blockstate turn. Its dangling gold end is
therefore on the left and remains in front of the cloth. There are no new block
IDs or registry changes.

Run `scripts/generate_clinic_refinement_0254.py` after the base Kindergarten
asset generator. It writes only the bed model, bed quilt texture, green tie east
model and green tie blockstate. `scripts/validate_clinic_refinement_0254.py`
independently checks coplanar overlap, mesh extents, quilt UV/aspect, transformed
tassel position and deterministic regeneration. A duplicated bed skin mutation
must fail validation.

`ClinicRefinementGameTests0254` provides four native tests in batch `clinic0254`.
They build the actual school and inspect the packaged model resources, complete
portrait backing, bed collision/mesh footprint and pillow orientation, machine
screen direction, curtain-to-wall contact and both right/left tie selectors.
