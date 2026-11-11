# Chao Garden 0.25.14

The supplied Sonic Adventure 2 `gd_PC` files now drive the appearance of 22 foods, seven Battle seeds, the shovel and watering can. Their original mesh silhouettes, texture assignments and wrapping are adapted for Minecraft, with matching inventory icons, held/dropped forms, growing pods and seedlings. Fine seed connectors are thickened slightly to remain visible at inventory resolution.

The shovel has an orange/red toy appearance and matching placed poses. The cyan watering can uses the original face, handle and spout geometry. The Black Market still sells both tools, and the planting/watering rules are unchanged.

Eighteen garden materials now use the supplied game references: palm bark and cut ends; Neutral Garden cliff, grass, waterfall rock and broadleaf plants; Hero Garden marble, frieze, emblem and columns; and Dark Garden gravestones, deadwood, fencing, rock and pale soil. The corner frieze turns continuously and the palm cut ends use separate end grain. Existing block identities, garden layouts, collision shapes and saved worlds are retained.

Fifteen additional foods and twenty-nine extension/compatibility seeds retain their established authored designs because these SA2 files do not provide a confirmed matching model. No approximate texture assignment has been presented as an original model. Existing music and singing recordings are unchanged.

## Source and rebuilding

`art/sa2_02514/` contains the selected supplied PRS members, extracted reference geometry, named textures and input hashes. `art/extract_sa2_02514.py` re-extracts those inputs offline. `scripts/generate_sa2_assets_02514.py` adapts them after the existing fruit generator, including baking repeating/mirrored textures into atlas-safe materials. `scripts/generate_all_assets.py` includes this final pass.

The conversion follows the documented SA Tools chunk-model layout. Exact format references and source-file hashes are recorded in `art/sa2_02514/provenance.json`. The original game archives are source-art inputs only; the runtime JAR contains the converted models and textures.

## Other useful supplied assets reviewed

The folder also contains more toy/Kindergarten object textures and Chao motion data. Those are useful references for later toy, decoration and animation chunks. This release limits imported geometry to confidently identified fruit, seed and gardening-tool models, and keeps unrelated Chao rigs and behavior intact.

Install the new JAR on both client and server after removing the previous version. No extra resource pack or game-file installation is needed. See `TESTING.md` for completed checks and practical limits.
