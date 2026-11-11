# Actual client world and service study

The excluded development harness `World025VisualSmoke` runs only with `-PchaoWorld025Study=true`, which sets `chao_garden.world025Study`. The earlier `hub024Study` and separate `animation025Study` must remain false. Run it in the disposable `ChaoVisual` world at an actual 1280×720 framebuffer.

The ordered 42-scene contract and honest captions are in `release-scenes-0.25.0.json`. It includes five lobby angles, the neutral garden and real care admission, the school exterior and all rooms, front/oblique adult staff views, a zero-Run baby comparison, and the resident services.

## Capture admission

Each view requires every chunk intersecting its authored realm bounds to be received by the client and `LevelRenderer.hasRenderedAllSections()` to report an empty section compilation/upload queue. These conditions must hold for 40 consecutive render frames. A failed condition resets the qualification. UI views additionally check the actual selected individual, service role, view mode, chart tab and, after classroom admission, the synchronized student entity with a live lesson timer. An overall scene deadline fails the run rather than producing a timed placeholder.

The physical television changes its block mesh every four server ticks. Its scene therefore qualifies the unchanged garden for 40 stable frames **before** actual placement and voluntary play, then requires its current render queue to be empty at capture. It does not pause, force or forge the production play session.

Every `CHAO_WORLD025_CAPTURE` event names the exact file, stage, dimension, game mode and readiness facts. The successful screenshot callback checks the PNG dimensions and logs `CHAO_WORLD025_SAVED` with its SHA256. Completion is logged only after all 42 callbacks and tagged fixture cleanup.

## Production paths and fixture scope

- `HubContent.enterHub`, `visitGarden`, `returnToHub` and `visitKindergarten` perform actual realm admission. Hub/school use Adventure; the garden selects protected Survival, retaining the original home-mode journal.
- `ServerPlayerGameMode.useItemOn` places a normal egg, releases the exact captured Chao UUID from its held item, and places one physical TV using the ordinary item and block admission logic.
- The released Chao uses the production `ToyContent.PlayGoal` with valid needs and a deterministic study seed. Placement must leave the toy inactive; the study waits for the Chao's actual voluntary choice.
- Staff UI is opened through the resident's normal interaction. The market uses `KindergartenMarket025.open`, the same method called by its production interaction event. Screen buttons submit normal authorized actions.
- Disposable actors and any boarded ledger rows carry `ChaoWorld025VisualStudy`. Cleanup removes only that tag. It does not sweep ordinary creatures, fixed residents, or untagged classroom entries. The one study-placed TV restores its recorded prior block state.

The cameras, held samples, test grades and temporary actors are fixtures. They are not screenshots of a naturally progressed survival save. The gallery labels them accordingly.

## Gallery and review

Preserve the world and baby client logs separately before another run replaces `latest.log`. Then run:

```sh
python scripts/build_preview_gallery_025.py --source run/screenshots --log PATH_TO_WORLD_LOG --baby-log PATH_TO_BABY_LOG --allow-unreviewed
```

This makes a review-only offline gallery, never a visual PASS. Review each exact final image and record its key, SHA256, result and observation in `validation-0.25/world-visual-review.json`. Rebuild without `--allow-unreviewed` only after every still has a matching PASS. The builder refuses stale screenshot hashes, incomplete/coalesced capture events, changed dimensions and mismatched embedded image bytes. It also builds labelled actual baby GIFs and contact strips from the separate 108-frame animation study, retaining all native input PNGs and their timed events.

Source references are separately labelled user-video images with exact requested timestamps, source filenames and native PNG hashes. They are never counted as mod captures. See `references-0.25/README.md` for the curated source-video research.

After unpacking the complete source download, the already captured gallery can be rebuilt from its preserved media and receipts without running Minecraft:

```sh
python scripts/build_preview_gallery_025.py --source docs/screenshots-0.25 --baby-source docs/motion-0.25/frames --log docs/validation-0.25/world-capture.txt --baby-log docs/validation-0.25/baby-capture.txt
```
