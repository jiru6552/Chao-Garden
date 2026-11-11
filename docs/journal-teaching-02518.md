# Keeper's Journal teaching pass

This is a focused source update on top of the supplied 0.25.18 source. It does not establish that the old packaged JAR or screenshots contain these changes.

## Scope

- Rewrote 46 sections around actions, prerequisites, observable results and common reasons an action does not work. Added a paragraph break to Expanded garden gifts.
- Preserved all 58 section titles and their original order. Chapter classification, search, previous/next navigation and book-refresh behaviour are unchanged.
- Kept existing acquisition details and specialist reference pages where no correction was needed.
- Changed no Analyzer, toy, rendering, gameplay or configuration implementation.

## Important corrections

- Omochao construction is available; only new mod-addon heritage is paused. The journal now teaches the five-race-win wrench prerequisite.
- Whole fruit speeds childhood growth, so three loaded hours is the default upper duration rather than a fixed wait.
- Ordinary adult breeding has exceptions. Omochao cannot breed, including with optional rare breeding enabled.
- First evolution uses ability points. Stamina selects Normal, and the latest trained tied ability breaks ties. Companion thresholds are explicitly labelled levels.
- Doctor treatment is reached from its main menu; grade upgrades use MEDICAL CHART / STATS. Omochao improves grades through rebuilding instead.
- Companion defeat also removes points and resets the selected ability's progress; the journal no longer promises that all progress survives.
- Animal DNA completion is distinct from collecting every visible body part.
- The Analyzer overview names its actual tabs and directs beginners to needs and Favorite Fruit before detailed genetics.

## Verification for this pass

Run: `bash scripts/test_journal_02518.sh`.

Passed:
- Nine source-content checks: stable section identity/order, short paragraphs, search/book-refresh wording, corrected mechanics, actionable guidance and existing Analyzer tab names and Doctor home/chart navigation.
- Nineteen executable assertions against the real Minecraft-independent core classes: point-based locomotion, first-form selection and tie-breaking, fruit-accelerated adulthood, breeding eligibility and robot heart/parts happiness boundary.

The standalone runner compiles real core classes with FruitCatalog. It does not compile GardenGuideItem against Minecraft/NeoForge, launch Minecraft, inspect rendered pages or run native GameTests. Those stages remain unrun for this pass because the full native build is currently blocked in this environment. The existing TESTING.md describes the supplied earlier package, not validation of this edit.

## Follow-up native review

When native builds are available, open a newly created and an existing journal, check representative long sections at ordinary and small GUI scales, and verify section navigation and the updated Analyzer references. This pass intentionally does not redesign search or page navigation.
