# Removed character forms — 0.20

The user's removal request applies to the Spartoi and full-Egg character Chao, not ordinary Chao eggs or egg handling.

- Removed the one-percent Egg character birth roll and the Spartoi lens/evolution acquisition route.
- Active character generation and rendering now retain Amy, Tails, Knuckles and Omochao. Authored Spartoi/Egg gameplay meshes and skins are pruned. The exact twenty supplied comparison rigs remain untouched.
- A saved Egg character becomes an ordinary Chao with the same genes and progress; an independently saved Chaos immortality flag remains valid.
- A saved Spartoi becomes the ordinary form corresponding to its existing alignment/type/coat. Its character-only immortality is cleared. If that flag was active, only the current adult lifespan counter restarts at zero to avoid an immediate old-age death. Overall age, reincarnations, grades, trained points, personality, emotions, illness, acquired parts, learning, family, rewards, names, identity, owner and real equipment remain intact.
- Retired iris index 12 becomes ordinary clear eyes. The other twelve iris indices are preserved. No schema change is required.
- The old `spartoi_lens` inventory ID remains loadable as an inert hidden compatibility item with a clear-lens icon. It is absent from active catalogs, creative browsing and shop selection. Giving one invokes ordinary unknown-object inspection and returns the actual item; it cannot change a Chao.
- Market stock version 20 replaces saved 0.19 offers on load, preventing an old trader from continuing to sell the retired lens.

Validation prepared: `RetiredCharacterState020Test` passes 1,300 standalone assertions over literal old saves, all remaining iris choices, lineage, genes, progress, trait retention, idempotence and forced would-be Egg birth rolls. New real-server tests cover retired lens inspection/component conservation, complete entity migration including all five paid equipment slots, and saved-trader stock migration. Full NeoForge execution is coordinated with the complete release.

`art/retire_characters_020.py` must run after older writers, followed by `art/validate_retired_characters_020.py`. The resource validator checks that ordinary egg meshes/items remain while only the authored retired character geometry and active lens artwork are absent.
