# 0.25.3 editable source and proof inventory

`scripts/source_export0253.py` exports the union of all editable `src` files,
native `art` masters and imported inputs, build/wrapper files, scripts, current
verification tooling, and primary reference trees from 0.25, 0.25.1, 0.25.2 and
0.25.3. The existing exact 41-file imported Chao input inventory is checked.
Reference originals, provenance records, selected native reference frames and
old source baselines are retained. Historical generated screenshots, motion,
galleries and validation chains stay in their corresponding saved releases.

All current `docs/screenshots-0.25.3`, `docs/motion-0.25.3` and
`docs/validation-0.25.3` files are included. Current `.log` files are explicitly
retained, including `chao0253-native-raw.ffmpeg.log`; the previous generic helper
omitted that suffix. Final verification also requires the closed production,
two server-profile, native-client and asset-rebuild `.txt` logs. Caches, game
worlds, third-party JARs, build products and existing archives are excluded.

| Command | Purpose |
| --- | --- |
| `python scripts/source_export0253.py` | Write the current SHA-256, size and path inventory to `docs/validation-0.25.3/source-inventory.json`. This is an inventory, not a test result. |
| `python scripts/freeze_source_cohort0253.py` | Freeze stable code, assets, references, build inputs, tooling and visual obligations before native evidence capture. |
| `python scripts/freeze_source_cohort0253.py --verify` | Reject any changed, added or removed frozen input. |
| `python scripts/source_export0253.py --verify-evidence` | Require the frozen cohort and current known PASS reports, verify each direct SHA-bound input, then update the final inventory. |

The known verification reports are `core`, `server-default`,
`server-accessories`, `runtime-client`, `visual-review`, `production-jar` and
`asset-rebuild`. Each must identify release 0.25.3, report PASS and provide actual
SHA-bound inputs. Those inputs must be exported, apart from the production JAR
that is delivered separately. The visual review must explicitly reference the
current source-cohort file and its exact SHA. This validates current evidence without
recursively carrying earlier releases' review state.

The source cohort excludes generated screenshots, motion, logs, reports,
inventories, galleries, release prose and archives so they can be produced
without a hash cycle. The inventory excludes its own row for the same reason;
the packager may include the completed inventory as an ordinary source file.
Files are yielded once in deterministic path order. Unsafe paths, symlinks,
omitted primary inputs and individual files of 500 MB or more are rejected.
The final packager must separately enforce an archive size below 500 MB and
compare every archived source payload with this live file list. These scripts
do not build the mod, create final archives or publish files.
