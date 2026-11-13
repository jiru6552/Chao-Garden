# Dark 0.25.60 final comparison generator

`generate.py` is prepared only. It has not generated or reviewed a future client cohort.

Run after the coordinator completes the canonical 80-case client run, confirms its actual process exit, and binds the frozen development runtime to the final JAR:

```sh
python3 /workspace/.setup/dark060-final-reference-compare/generate.py
```

Defaults require the current `dark060-client-final` catalog/screenshots, canonical `dark060-client-final.log`, independent live `dark060-viewer` references, `run060-runtime-proof.json` and actual `chao_garden-0.25.60.jar`. CLI arguments allow explicit alternative input paths for review. The generator requires the full catalog and one complete 80/80, zero-skip log; each native/render completion must occur exactly once. It verifies all selected source PNG hashes against their actual capture logs/reference manifest and obtains the JAR hash from the actual runtime proof. It never predicts a future JAR hash.

Outputs are three matched stage sheets (nine rows, paired Minecraft/viewer front, right/side and back/square-back views), one Minecraft opposite-side audit, and a provenance manifest. Crops are fixed per source, padded and scaled uniformly; there are no flips, recolors, per-frame warps or generated geometry. Original screenshots remain unchanged. The manifest starts with visual review and coordinator acceptance false; generation does not establish those facts, an actual process exit, or archive-loaded runtime behavior.

The user-facing family is Dark / Neutral-first; the internal first-type constant and the viewer checkbox are called Normal. Viewer age/stat percentages and orbit deltas are approximate controls. Compatible pair interiors/rates remain an explicit approximation: the viewer reaches its paired endpoints around half of each axis. Read the live viewer findings and coordinator report for limits.
