# Kindergarten adult staff, 0.25

The supplied Kindergarten recording is the primary appearance reference. The native-resolution frames at 69 seconds (Fortune-Teller), 134 seconds (Doctor) and 167 seconds (Principal) were inspected, alongside the full room timelines. These are cyan adult Chao with professional details; the Doctor's bright pink belongs to the chair, rather than its skin or a pink robe.

| Staff | Reference-derived appearance |
| --- | --- |
| Doctor | Cyan body, gray swept side hair, relaxed half-lidded eyes and toothy grin, curved stethoscope headset with a blue medical-cross receiver, and a hanging chestpiece |
| Principal | Cyan body, round pale spectacles with connected bridge/temples, white eyebrows, a broad white moustache and stepped beard |
| Fortune-Teller | Cyan body, closed violet eyelids and white side curls, a gold forehead circlet and purple jewel, with a purple/gold stole and brooch |
| Teacher | Original design, because the source classroom has no teacher: green academic cap with gold tassel, cream waistcoat/red tie, and an open lesson book |

The profession models retain the original neutral skeleton and body shapes. In the supplied addon, the neutral base geometry is called `baby` even when it supplies ordinary neutral adults. Staff use the adult scale and explicitly sample a quiet adult stance: the teacher stands, while the Doctor, Principal and Fortune-Teller sit with their feet forward and their torsos upright. They never select newborn crawling from zero Run points. No ability points are invented to change that presentation.

Each profession has exactly one authored eye pair. The old raised eye meshes are removed in the generated staff copies before the new eyes/spectacles are added. Eyelids blink together, and small breathing/head motions run while a staff member remains at its counter. An authorized service interaction triggers one finite, softly blended acknowledgement: a teacher welcome, a Doctor's attentive tilt, the Fortune-Teller's hand motion or the Principal's nod. Their feet remain planted. Profession details attach to the native skull and torso joints; the arm poses frame the book and counters.

`art/build_staff_025.py` creates four complete role meshes and one shared atlas. The original imported source models, textures and animation curves are not edited. The new atlas preserves the original neutral PNG's pixels exactly in its native region, and adds profession material colors in a separate region. `art/validate_staff_025.py` checks source preservation, skeleton topology, finite vertices, valid atlas coordinates, required profession details and the single eye pair (12,870 assertions).

Registration is isolated in `StaffChaoClient025.registerLayers` and `registerRenderer`; live Kindergarten staff no longer reuse the ordinary garden Chao renderer or the generic clothing layers. Room staff identity, protection, ownership and service behavior remain with `KindergartenContent`/`KindergartenNpc`. The existing Black Market merchant retains its own renderer.

The final World42 actual client review opened front and oblique views of all four professions and an adult/baby comparison. It confirms one aligned eye pair per role, a standing adult teacher, three upright seated adults, and readable head/hand/foot details around the compact chairs. The Fortune-Teller faces its counter correctly, with its violet eyelids and forehead jewel visible past the small crystal. The Principal's oblique retains a natural foreground globe overlap below the eyes; its clear front verifies the moustache, beard and torso. No reviewed camera remains inside a wall. Full image SHA-256 values and per-view observations are recorded in `docs/validation-0.25/staff-visual-review.json`.
