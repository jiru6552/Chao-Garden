# Staff face refinement, 0.25.1

The direct user uploads `18.17.38`, `18.17.56` and `18.20.07` were inspected. The Doctor's old geometry contained actual tooth slabs and tooth gaps. These have been removed and replaced by two swept gray moustache lobes with subtle hair highlights. The Doctor has no beard, preserving the Principal's distinct large white moustache and goatee.

The Fortune-Teller's blinking came from the dedicated staff model's universal eyelid scaling, rather than the ordinary Chao expression layer. Its authored violet closed lids now remain fully visible at scale one. This profession rule runs after service gestures, so idle, looking and naming gestures preserve the closed-eye illusion. Other staff retain their existing occasional blink timing.

The Teacher, Principal and Fortune-Teller mesh files and the shared staff atlas remain byte-identical to 0.25. All adult proportions, colors and adult seated/standing poses are preserved. The original imported assets remain unchanged.

The isolated face timing test passes 177,195 assertions across sixteen former blink periods and multiple UUID phases. Staff mesh validation passes 12,848 assertions, including the absence of Doctor teeth and the Fortune-Teller's single closed-lid pair. Five generated staff payloads are byte-idempotent. Details are recorded in `docs/validation-0.25.1/staff-facial-rules.json`.

Actual client signoff passes in the fresh v6 capture. Doctor front/oblique originals show the new moustache and one aligned eye pair. All 731 decoded Fortune face frames across 24.363100 seconds preserve the closed violet lids, including before, during and after its service gesture. Five full frames were also inspected for face, hand and altar alignment; this review does not claim real-time playback. The actor-bound submitted-mesh probe checked 504 rendered frames, including 44 genuine service-gesture frames, with both eyelid scales fixed at one and the native Fortune UI verified. Final exact hashes, actual timestamps and review limits are recorded in `docs/validation-0.25.1/staff-visual-review.json`.
