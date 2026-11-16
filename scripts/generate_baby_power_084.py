#!/usr/bin/env python3
"""Author separate blocky Power baby features; keep the approved .55 model exact.

The live viewer establishes the rear lip/nubs, triple crown and bent stem.
Dimensions below are authored Minecraft adaptations, not extracted geometry.
"""
from pathlib import Path
import copy
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/chao_garden"
SOURCE = ASSETS / "models/entity/gameplay/chao_hero_run_proto055.json"


def main():
    source = json.loads(SOURCE.read_text())
    feature = next(b for b in source["bones"] if b["name"] == "gradual_head_021")
    meshes = {m["name"]: m for m in feature["meshes"]}
    targets = {}

    def add(name, neutral=None, hero=None, dark=None):
        old = meshes[name]["blocky_box_055"]
        hidden = tuple(copy.deepcopy(old[k][0]) for k in ("centers", "sizes", "rotations"))
        poses = [neutral or hidden, hero or hidden, dark or hidden]
        targets[name] = {
            key: [pose[index] for pose in poses]
            for index, key in enumerate(("centers", "sizes", "rotations"))
        }

    # Neutral: a broad short rear lip and its separate raised front crest.
    # Dark: the same two closed boxes form a short bent upright stem.
    add("neutral_run_crest_root",
        ([0, -5.45, 3.20], [5.6, 1.25, 2.5], [20, 0, 0]),
        dark=([-0.15, -6.65, 1.00], [.75, 1.80, .80], [18, 0, -16]))
    add("neutral_run_crest_tip",
        ([0, -6.30, .35], [1.45, 1.35, 1.5], [15, 0, 0]),
        dark=([.15, -7.65, 1.25], [.65, .85, .75], [-20, 0, 22]))
    for sign, side in ((-1, "left"), (1, "right")):
        add("neutral_run_front_nub_" + side,
            ([sign * 3.35, -4.85, -1.25], [1.30, .80, 1.10], [0, 0, sign * -18]))
        # Hero: two flaring blunt top caps plus a smaller center/rear lobe.
        add("hero_" + side + "_bud",
            hero=([sign * 1.0, -6.65, 1.05], [1.0, 1.55, 1.05], [8, 0, sign * 23]))
    add("hero_run_stem_root", hero=([0, -6.15, 1.5], [.70, .70, .80], [-10, 0, 0]))
    add("hero_run_stem_tip", hero=([0, -6.65, 1.6], [.55, .60, .65], [-12, 0, 0]))
    result = {
        "version": "0.25.84",
        "scope": "Ordinary babies only; age x positive current Power-minus-Run level bars.",
        "source_model": str(SOURCE.relative_to(ASSETS)),
        "source_model_sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "original_head_box": [7, 6, 6],
        "legacy_tuft_retraction_y": 2.5,
        "targets": ["neutral_power_baby", "hero_power_baby", "dark_power_baby"],
        "source_geometry_copied": False,
        "reference": "Independently rendered Chao Island Child with Power, alignment and age controls; front/side/back inspected for all three alignments.",
        "approximation": "Eight existing closed cuboids supply a broad Neutral rear lip/crest and short side nubs, Hero triple top crown, and Dark bent stem. The base skull, eyes, shared joints, body and limb boxes are unchanged. Rounded tips, fine gradients and exact source timing are simplified.",
        "features": targets,
    }
    (ASSETS / "baby_power084.json").write_text(json.dumps(result, indent=2) + "\n")


if __name__ == "__main__":
    main()
