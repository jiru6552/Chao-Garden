# Broom, book and crayons — 0.25.51

These changes refine the existing 0.25.49 toy presentations. The item/block IDs, source models and UVs, recipes, rewards, reservations, cooldowns and preview clocks remain in place. The source models are the previously extracted SA2 Battle broom, book covers/page, crayon box and crayon. No duplicate toy or replacement asset is introduced.

## Broom

Both visible palms now grip the actual wooden shaft at staggered heights. The left arm no longer uses the old free-arm balancing pose. The pole is nearer the performer, the upper arms move slightly forward, and the extension cap is **1.2 model pixels**, reduced from 3.2. The same targets drive the fitted hands and the rendered broom throughout the sweep.

The shaft stays upright in front of the face. Its floor height is measured from its rotated source bounds; the bristles retain the existing small floor margin. A slightly rearward, outward-toed stance keeps long Run feet clear of the brush at the sweep extremes. The existing eight sweep cues and production sweep curve remain unchanged.

## Picture book

The book and both hands are raised and brought forward together. Their placement also measures the highest posed foot surface, including genetic and glass feet. The book's underside and palms stay above that surface instead of intersecting long toes or large paws. Its original cover hinges, page mesh and two page passages remain.

The bounds-aware `bookMatrix` overload is used by production rendering, palm fitting and the new client study. Merely using the old bounds-free helper would not account for replacement feet. Actual source-volume tests cover the book, turning page and emitted palms against both feet, head and torso.

The interaction is silent. Page timing is still tested independently of sound dispatch.

## Crayons

The Chao uses the shared belly-supported prone pose, with its feet behind and chin raised enough to clear the drawing hands. The right hand draws; the left steadies the paper from above. The sheet lies flat on the floor ahead of the shoulders, with the real box on the floor beside it. Existing staged marks remain temporary presentation geometry and disappear with the interaction; no terrain or persistent drawing data is created.

The existing crayon is held slightly farther back along its real barrel and fitted to the thick visible palms. Its actual source tip meets the floor sheet at each stroke. The writing-arm extension cap is **1.8 model pixels**; the support arm needs less than one pixel in the tested source set. Both visible hand volumes are checked against the floor, rather than checking only abstract palm centers.

The interaction is silent. All eight stroke starts, completed-ink ordering and pen lifts remain on their existing production clock and are checked independently of sound dispatch.

## Verification and previews

`QuietToyRigTest049` now checks both broom palms, all three toys' actual emitted hands and props against visible feet, source head/torso clearance, floor grounding and the crayon's real tip. It includes source forms, both ages, morph combinations, replacement hands and genetic/glass feet. The former left-broom contact exemption is removed. The final combined Gradle run passed 59,346 poses and 96,084 emitted hands with zero failures. Both broom palm errors remain below 0.00000026 model blocks; the real crayon-tip error remains below 0.0000004. Visible palms retain positive head/body/foot separation, and the highest drawing-hand vertex is 1.498763 against floor Y=1.501. The main 0.25.51 report records the native server, actual-client and full-build outcomes, including inherited failures outside these toys.

`QuietToyTimingTest049` retains 641 quarter-tick samples and endpoint/curve checks, while checking silent page and drawing phases outside the sound branch. Asset tests preserve the original source surfaces and check independent floor grounding. Automated source checks are not a visual or listening acceptance claim.

Use the existing operator previews, with a nearby Chao:

```text
/chao testtoy broom once
/chao testtoy broom loop
/chao testtoy picture_book once
/chao testtoy crayons loop
/chao testtoy stop
```

Every toy retains `once`, `loop` and `stop`; these use the actual interaction, grant no rewards and preserve saved state. Native interruption and real-client checks are coordinated with the other 0.25.51 changes. Preferred resource packs, custom headwear, remote-player views and full modpacks still need their own in-game inspection. No sound was listened to in this environment; broom listening remains a user check, while book/crayon cue absence is tested directly.
