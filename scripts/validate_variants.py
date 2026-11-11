#!/usr/bin/env python3
"""Inspect retained themed artwork and verify that every conversion recipe is absent.

Actual item-use and loaded-mod acquisition boundaries run in the server tests.
This resource check does not claim full optional-mod compatibility.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"art"))
from runtime_resource_contract_022 import prototype_path, ATTACHMENT_LAYOUTS
import re

from PIL import Image
from validate_gene_assets import validate_geometry
from validate_head_morphs import profiles_for, validate_head_parts

ROOT = Path(__file__).resolve().parents[1]


def require(value, message):
    if not value:
        raise ValueError(message)


def fingerprint(groups):
    """Ignore labels/colors: a palette-only copy does not count as a new shape."""
    shapes = []
    for bone in groups:
        pieces = []
        for mesh in bone["meshes"]:
            pieces.append({"vertices": mesh["vertices"],
                           "faces": [face["v"] for face in mesh["faces"]]})
        shapes.append({"pivot": bone["pivot"], "meshes": pieces})
    return hashlib.sha256(json.dumps(shapes, sort_keys=True).encode()).hexdigest()


def decoded_png(path, size):
    require(path.is_file(), "Missing texture " + str(path))
    with Image.open(path) as image:
        require(image.size == size, f"{path}: expected {size}, found {image.size}")
        rgba = image.convert("RGBA")
        rgba.load()
        require(rgba.getbbox() is not None, str(path) + ": texture is completely empty")
        return rgba


def validate(root):
    assets = root / "src/main/resources/assets/chao_garden"
    source = json.loads((assets / "chao_variants.json").read_text())
    definitions = source["variants"]
    require(len(definitions) == 19, "Release catalog must contain exactly the 19 retained themes")
    retired = json.loads((assets / "retired_chao_variants.json").read_text())
    require(len(retired) == len(set(retired)) == 29, "Exactly 29 themes are retired")
    ids = [value["id"] for value in definitions]
    require(not (set(ids) & set(retired)), "A retired theme remains active")
    neo = next(value for value in definitions if value["id"] == "blood_magic")
    require(neo["name"] == "Neo Vitae" and neo["mods"] == ["neovitae"],
            "Neo Vitae must use its current public name and actual mod ID")
    require(len(set(ids)) == len(ids), "Duplicate catalog IDs")
    require(all(re.fullmatch(r"[a-z][a-z0-9_]{0,47}", name) and name != "normal" for name in ids),
            "Invalid catalog theme ID")
    require({v["id"] for v in definitions if not v["mods"]} == {"nether", "end"},
            "Only the Nether and End themes may be available without another mod")
    all_mods = {mod for definition in definitions for mod in definition["mods"]}
    require(all(re.fullmatch(r"[a-z][a-z0-9_]{1,63}", name) for name in all_mods),
            "Invalid integration mod ID")
    model = json.loads((prototype_path(root)).read_text())
    bones = {bone["name"]: bone for bone in model["bones"]}
    # Animal genes now coexist with inherited themed coats. Their exact supplied
    # slots and facial clearance are checked independently from theme shapes.
    validate_geometry(root)
    require(model["materials"].get("variant_detail") == [32, 128, 32, 32],
            "Themed glyph atlas region moved without updating the authoring contract")
    for ident in retired:
        prefix = "variant_" + ident
        require(not any(name.startswith(prefix + "_") for name in bones),
                ident + ": retired themed geometry remains in the rig")
        item = ident + "_chao_egg"
        require(not (root / "src/main/resources/data/chao_garden/recipe" / (item + ".json")).exists(),
                ident + ": retired recipe remains obtainable")
        require(not (root / "src/main/resources/data/chao_garden/advancement/recipes/chao_variants" / (item + ".json")).exists(),
                ident + ": retired recipe advancement remains")
        require(not (assets / "textures/item" / (item + ".png")).exists(),
                ident + ": retired egg artwork remains")
        require(not (assets / "textures/entity/chao" / (prefix + ".png")).exists()
                and not (assets / "textures/entity/eggs" / (prefix + ".png")).exists(),
                ident + ": retired Chao/egg atlas remains")
        compatibility = json.loads((assets / "models/item" / (item + ".json")).read_text())
        require(compatibility == {"parent": "chao_garden:item/normal_egg"},
                ident + ": legacy shell must render as an ordinary egg")
    language = json.loads((assets / "lang/en_us.json").read_text())
    geometries, body_images, egg_images, item_images = {}, {}, {}, {}
    refined = profiles_for(root)
    checks = 0
    for value in definitions:
        ident = value["id"]
        prefix = "variant_" + ident
        groups = [bones.get(prefix + suffix) for suffix in ("_head", "_body", "_marker")]
        require(all(bone and bone.get("meshes") for bone in groups), ident + ": missing authored head/body/marker")
        require([bone["parent"] for bone in groups] == ["head", "body", "head"],
                ident + ": accessories are attached to the wrong bones")
        if ident in refined:
            validate_head_parts(groups[0]["meshes"], bones["head"]["meshes"][0], ident)
        else:
            require(all(mesh.get("primitive") == "cube" for mesh in groups[0]["meshes"]),
                    ident + ": legacy head solid must remain an editable cuboid")
            require(max(vertex[1] for mesh in groups[0]["meshes"] for vertex in mesh["vertices"]) <= 1.00001,
                    ident + ": head accessory reaches shoulder space at rest")
        signature = fingerprint(groups)
        require(signature not in geometries,
                ident + ": geometry duplicates " + geometries.get(signature, "another theme"))
        geometries[signature] = ident
        palette = value["palette"]
        require(all(isinstance(color, list) and len(color) == 3
                    and all(type(channel) is int and 0 <= channel <= 255 for channel in color)
                    for color in palette.values()), ident + ": malformed palette")
        body = decoded_png(assets / "textures/entity/chao" / (prefix + ".png"), (256, 256))
        glyph = body.crop((32, 128, 48, 144)).getchannel("A")
        require(glyph.getextrema() == (0, 255), ident + ": chest glyph needs painted texels and a transparent surround")
        egg = decoded_png(assets / "textures/entity/eggs" / (prefix + ".png"), (64, 64))
        item_path = assets / "textures/item" / (ident + "_chao_egg.png")
        require(item_path.is_file(), "Missing egg item texture " + str(item_path))
        with Image.open(item_path) as im:
            require(im.size in ((16, 16), (32, 32)), ident + ": egg icon must use Minecraft pixel dimensions")
            icon = im.convert("RGBA")
            require(icon.getchannel("A").getextrema() == (0, 255), ident + ": icon needs a transparent background")
        for image, seen, label in ((body, body_images, "body"), (egg, egg_images, "egg"), (icon, item_images, "item icon")):
            signature = hashlib.sha256(image.tobytes()).hexdigest()
            require(signature not in seen, f"{ident}: {label} duplicates {seen.get(signature, 'another theme')}")
            seen[signature] = ident
        item_model = json.loads((assets / "models/item" / (ident + "_chao_egg.json")).read_text())
        require(item_model.get("textures", {}).get("layer0") == "chao_garden:item/"+ident+"_chao_egg",
                ident + ": egg item model selects the wrong texture")
        require("item.chao_garden."+ident+"_chao_egg" in language, ident + ": missing translated item name")
        for relative in ("recipe/"+ident+"_chao_egg.json", "advancement/recipes/chao_variants/"+ident+"_chao_egg.json"):
            require(not (root/"src/main/resources/data/chao_garden"/relative).exists(), ident+": paused acquisition returned")
            checks += 1
    print(f"Variant assets passed: {len(definitions)} distinct geometry themes, "
          f"{len(body_images)} body atlases, {len(egg_images)} egg atlases, {len(item_images)} item icons.")
    print(f"Acquisition passed: all19 themed recipes and recipe advancements disabled; {checks} absence checks. Appearance assets remain intact.")
    return len(definitions), checks


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args()
    try:
        validate(args.root.resolve())
    except (OSError, ValueError, KeyError) as error:
        raise SystemExit("VARIANT VALIDATION FAILED: " + str(error))
