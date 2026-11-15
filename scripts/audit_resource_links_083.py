#!/usr/bin/env python3
"""Read-only resource audit; a missing static reference is not proof of dead content.

Only Chao Garden namespace links are resolved. Runtime registries, dynamic model
selection, external packs, visual appearance and audio playback need game tests.
Pillow, when already installed, adds image-format verification; otherwise PNG
signatures and all chunk CRCs are still checked using the standard library.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import hashlib
import json
import math
from pathlib import Path
import re
import struct
import zlib

try:
    from PIL import Image
except ImportError:
    Image = None


def objects(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from objects(child)
    elif isinstance(value, list):
        for child in value:
            yield from objects(child)


def finite(value):
    if isinstance(value, float):
        return math.isfinite(value)
    if isinstance(value, (list, tuple)):
        return all(map(finite, value))
    if isinstance(value, dict):
        return all(map(finite, value.values()))
    return True


def png_info(raw):
    """Verify container CRCs without introducing a required imaging dependency."""
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("invalid PNG signature")
    offset, size, ended = 8, None, False
    while offset < len(raw):
        if offset + 12 > len(raw):
            raise ValueError("truncated PNG chunk")
        length, = struct.unpack_from(">I", raw, offset)
        kind = raw[offset + 4:offset + 8]
        end = offset + 8 + length
        if end + 4 > len(raw):
            raise ValueError("truncated PNG payload")
        expected, = struct.unpack_from(">I", raw, end)
        if zlib.crc32(raw[offset + 4:end]) & 0xffffffff != expected:
            raise ValueError("PNG chunk CRC mismatch")
        if kind == b"IHDR":
            if size is not None or offset != 8 or length != 13:
                raise ValueError("invalid PNG header")
            size = list(struct.unpack_from(">II", raw, offset + 8))
            if min(size) <= 0:
                raise ValueError("empty PNG image")
        offset = end + 4
        if kind == b"IEND":
            ended = True
            break
    if size is None or not ended or offset != len(raw):
        raise ValueError("incomplete PNG container")
    return {"size": size}


def audit(root):
    resource_root = root / "src/main/resources"
    paths = sorted(p for p in resource_root.rglob("*") if p.is_file())
    if not paths:
        raise ValueError("no src/main/resources files beneath selected root")
    available = {p.relative_to(resource_root).as_posix() for p in paths}
    errors, warnings, edges, inventory = [], [], [], []
    documents, duplicates = {}, defaultdict(list)

    def note(collection, name, kind, **details):
        collection.append({"file": name, "kind": kind, **details})

    def link(name, value, kind):
        if not isinstance(value, str) or not value.startswith("chao_garden:"):
            return
        relative = value.split(":", 1)[1]
        target = {
            "model": f"assets/chao_garden/models/{relative}.json",
            "texture": f"assets/chao_garden/textures/{relative}.png",
            "particle": f"assets/chao_garden/textures/particle/{relative}.png",
            "sound": f"assets/chao_garden/sounds/{relative}.ogg",
            "file": f"assets/chao_garden/{relative}",
        }[kind]
        edges.append({"file": name, "kind": kind, "value": value,
                      "target": target, "exists": target in available})
        if target not in available:
            note(errors, name, "missing_reference", target=target, value=value)

    for path in paths:
        name = path.relative_to(resource_root).as_posix()
        raw = path.read_bytes()
        digest = hashlib.sha256(raw).hexdigest()
        duplicates[(digest, len(raw))].append(name)
        row = {"path": name, "bytes": len(raw), "sha256": digest}
        inventory.append(row)
        try:
            if path.suffix in {".json", ".mcmeta"}:
                def unique(pairs):
                    result = {}
                    for key, value in pairs:
                        if key in result:
                            note(errors, name, "duplicate_json_key", key=key)
                        result[key] = value
                    return result
                data = json.loads(raw, object_pairs_hook=unique)
                if not finite(data):
                    note(errors, name, "nonfinite_json")
                documents[name] = data
            elif path.suffix == ".png":
                row["image"] = png_info(raw)
                if Image is not None:
                    with Image.open(path) as image:
                        row["image"]["mode"] = image.mode
                        image.verify()
            elif path.suffix == ".ogg" and raw[:4] != b"OggS":
                note(errors, name, "invalid_ogg_signature")
            elif path.suffix == ".obj":
                counts = Counter()
                for line_number, line in enumerate(raw.decode().splitlines(), 1):
                    fields = line.split()
                    if not fields:
                        continue
                    if fields[0] in {"v", "vt", "vn"}:
                        counts[fields[0]] += 1
                        if not all(math.isfinite(float(v)) for v in fields[1:]):
                            note(errors, name, "nonfinite_obj", line=line_number)
                    elif fields[0] == "f":
                        for vertex in fields[1:]:
                            for field, kind in zip(vertex.split("/"), ("v", "vt", "vn")):
                                if field and (int(field) == 0 or abs(int(field)) > counts[kind]):
                                    note(errors, name, "invalid_obj_index", line=line_number)
        except (ValueError, OSError, struct.error) as error:
            note(errors, name, "invalid_file", detail=str(error))

    entity_models = 0
    for name, data in documents.items():
        if "/models/" in name:
            for obj in objects(data):
                parent = obj.get("parent")
                if isinstance(parent, str) and ":" in parent:
                    link(name, parent, "model")
                model = obj.get("model")
                if isinstance(model, str):
                    link(name, model, "file" if model.endswith((".obj", ".json")) else "model")
                if isinstance(obj.get("textures"), dict):
                    for texture in obj["textures"].values():
                        link(name, texture, "texture")
        elif "/blockstates/" in name:
            for obj in objects(data):
                link(name, obj.get("model"), "model")
        elif "/particles/" in name and isinstance(data, dict):
            for texture in data.get("textures", []):
                link(name, texture, "particle")
        if name.endswith("/sounds.json") and isinstance(data, dict):
            for event in data.values():
                for sound in event.get("sounds", []):
                    if isinstance(sound, str):
                        link(name, sound, "sound")
                    elif sound.get("type", "file") == "file":
                        link(name, sound.get("name"), "sound")
        if "/models/entity/" not in name or not isinstance(data, dict) or "bones" not in data:
            continue
        entity_models += 1
        bones = data["bones"]
        names = [bone["name"] for bone in bones]
        if len(names) != len(set(names)):
            note(errors, name, "duplicate_bone")
        outside, collapsed = 0, 0
        for bone in bones:
            if bone.get("parent") is not None and bone["parent"] not in names:
                note(errors, name, "missing_bone_parent", bone=bone["name"])
            for mesh in bone.get("meshes", []):
                vertices = mesh.get("vertices", [])
                for face in mesh.get("faces", []):
                    indices, uv = face.get("v", []), face.get("uv", [])
                    if any(type(i) is not int or not 0 <= i < len(vertices) for i in indices):
                        note(errors, name, "invalid_mesh_index", bone=bone["name"])
                        continue
                    if len(indices) != len(uv):
                        note(errors, name, "mesh_uv_arity", bone=bone["name"])
                    width, height = data.get("texture_width"), data.get("texture_height")
                    outside += bool(width and height and any(
                        not 0 <= point[0] <= width or not 0 <= point[1] <= height for point in uv))
                    if len(indices) >= 3:
                        a, b, c = (vertices[i] for i in indices[:3])
                        ab, ac = ([b[i] - a[i] for i in range(3)], [c[i] - a[i] for i in range(3)])
                        normal = [ab[1] * ac[2] - ab[2] * ac[1], ab[2] * ac[0] - ab[0] * ac[2], ab[0] * ac[1] - ab[1] * ac[0]]
                        collapsed += sum(v * v for v in normal) < 1e-14
        if outside:
            note(warnings, name, "uv_outside_declared_atlas", faces=outside,
                 context="Imported question/dizzy symbols are remapped by ChaoMeshAsset; inspect other cases.")
        if collapsed:
            note(warnings, name, "collapsed_first_triangle", faces=collapsed,
                 context="Emerging morph targets may intentionally start collapsed; not proof of broken geometry.")

    # Literal calls and data paths are an inventory, never an unused-ID oracle.
    declarations = []
    for path in sorted((root / "src/main/java").rglob("*.java")):
        if "/test/" in path.as_posix():
            continue
        source = path.read_text()
        for match in re.finditer(r'\b(?:register|registerItem|simple|add)\s*\(\s*"([a-z][a-z0-9_./-]*)"', source):
            declarations.append({"file": path.relative_to(root).as_posix(),
                                 "line": source.count("\n", 0, match.start()) + 1,
                                 "literal": match.group(1)})
    duplicate_groups = [{"sha256": digest, "bytes_each": size, "paths": names}
                        for (digest, size), names in duplicates.items() if len(names) > 1]
    return {
        "scope": "Static own-namespace direct references and file/mesh structure; not runtime reachability, visual, shader or audio verification.",
        "summary": {"files": len(paths), "bytes": sum(row["bytes"] for row in inventory),
                    "json_documents": len(documents), "internal_reference_edges": len(edges),
                    "entity_models": entity_models, "errors": len(errors), "warnings": len(warnings),
                    "duplicate_groups": len(duplicate_groups), "literal_registration_calls": len(declarations),
                    "png_validation": "container CRCs + Pillow verify" if Image is not None else "container CRCs only; Pillow not installed"},
        "errors": errors, "warnings": warnings, "files": inventory,
        "reference_edges": edges, "duplicate_groups": duplicate_groups,
        "registry_inventory": {"literal_calls": declarations,
            "catalog_paths": sorted(n for n in documents if n.startswith("assets/chao_garden/") and n.count("/") == 2),
            "limits": "Dynamic catalog loops, aliases, renderer-selected paths, generators and tests are preserved. Native registry enumeration is separate; no removals inferred."},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--json", type=Path, help="Optional full inventory output; source assets are never changed.")
    args = parser.parse_args()
    result = audit(args.root.resolve())
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    print(json.dumps(result["summary"], sort_keys=True))
    for error in result["errors"][:20]:
        print("ERROR " + json.dumps(error, sort_keys=True))
    raise SystemExit(bool(result["errors"]))


if __name__ == "__main__":
    main()
