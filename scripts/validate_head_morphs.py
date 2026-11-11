#!/usr/bin/env python3
"""Validate block-built themed Chao crowns and fitted forehead goggles.

The primary head remains one full cube. Type silhouettes must come from a
small, attached set of actual editable cuboids with square ends; expression
rays independently guard the unchanged eyes and mouth against occlusion.
"""
from __future__ import annotations

import copy
import hashlib
import importlib
import json
import math
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"art"))
from runtime_resource_contract_022 import prototype_path, ATTACHMENT_LAYOUTS
import sys

ROOT = Path(__file__).resolve().parents[1]


def profiles_for(root=ROOT):
    art = str(root / "art")
    if art not in sys.path:
        sys.path.insert(0, art)
    profiles = importlib.import_module("chao_head_morphs").HEAD_PROFILES
    catalog = json.loads((root / "src/main/resources/assets/chao_garden/chao_variants.json").read_text())
    active = {entry["id"] for entry in catalog["variants"]}
    return {ident: profile for ident, profile in profiles.items() if ident in active}


def require(value, message):
    if not value:
        raise ValueError(message)


def sub(a, b):
    return [a[i]-b[i] for i in range(3)]


def cross(a, b):
    return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]


def dot(a, b):
    return sum(x*y for x, y in zip(a, b))


def length(v):
    return math.sqrt(dot(v, v))


def ray_depths(mesh, x, y):
    """Intersections with a ray through the real X/Y expression bounds."""
    hits = []
    vertices = mesh["vertices"]
    for face in mesh["faces"]:
        ids = face["v"]
        for index in range(1, len(ids)-1):
            a, b, c = [vertices[i] for i in (ids[0], ids[index], ids[index+1])]
            divisor = (b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(divisor) < 1e-9:
                continue
            wa = ((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/divisor
            wb = ((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/divisor
            wc = 1-wa-wb
            if min(wa, wb, wc) >= -1e-7:
                hits.append(wa*a[2]+wb*b[2]+wc*c[2])
    return hits


def cuboid_edges(mesh):
    vertices = mesh["vertices"]
    edges = {tuple(sorted((a, b))) for face in mesh["faces"]
             for a, b in zip(face["v"], face["v"][1:]+face["v"][:1])}
    neighbors = sorted(b if a == 0 else a for a, b in edges if a == 0 or b == 0)
    require(len(neighbors) == 3, mesh.get("name", "cube")+": not a cuboid corner")
    return [sub(vertices[i], vertices[0]) for i in neighbors]


def validate_cuboid(mesh, label):
    require(mesh.get("primitive") == "cube", label+": a free mesh slope replaced an editable cuboid")
    vertices, faces = mesh["vertices"], mesh["faces"]
    require(len(vertices) == 8 and len({tuple(p) for p in vertices}) == 8,
            label+": solid must have eight distinct box corners")
    require(len(faces) == 6 and all(len(f["v"]) == 4 for f in faces),
            label+": solid must have six square-ended rectangular faces")
    edges = cuboid_edges(mesh)
    sizes = [length(e) for e in edges]
    require(all(size >= .4999 and abs(size*2-round(size*2)) < 1e-4 for size in sizes),
            label+": cuboid Size dimensions must use half-unit increments")
    require(all(abs(dot(edges[i], edges[j])) < 1e-4 for i, j in ((0, 1), (0, 2), (1, 2))),
            label+": block was sheared into a wedge")
    origin = vertices[0]
    possible = [[origin[k]+sum(edges[i][k] for i in range(3) if mask & (1 << i))
                 for k in range(3)] for mask in range(8)]
    require(all(any(length(sub(point, expected)) < 1e-4 for expected in possible) for point in vertices),
            label+": vertex moved off the true rectangular box")
    uses, volume = {}, 0.0
    for face in faces:
        ids = face["v"]
        for a, b in zip(ids, ids[1:]+ids[:1]):
            uses.setdefault(tuple(sorted((a, b))), []).append((a, b))
        points = [vertices[i] for i in ids]
        for i in (1, 2):
            volume += dot(points[0], cross(points[i], points[i+1]))/6
        require(abs(dot(sub(points[3], points[0]), cross(sub(points[1], points[0]),
                                                                     sub(points[2], points[0])))) < 1e-4,
                label+": box face is not planar")
        uv = face["uv"]
        require(len(uv) == 4, label+": incomplete cube UV face")
        for i in range(4):
            spatial = length(sub(points[(i+1) % 4], points[i]))
            texture = math.dist(uv[(i+1) % 4], uv[i])
            require(abs(spatial-texture) < 1e-4, label+": cuboid texture density is stretched")
    require(all(len(entries) == 2 and entries[0] == entries[1][::-1] for entries in uses.values()),
            label+": cube surface is open or inconsistently wound")
    require(abs(volume-math.prod(sizes)) < .001,
            label+": cube surface is inward or does not enclose its rectangular volume")
    return sizes


def cuboid_gap(a, b):
    """Exact SAT separating distance along box axes, including rotated boxes."""
    ae, be = cuboid_edges(a), cuboid_edges(b)
    axes = [cross(es[i], es[j]) for es in (ae, be) for i, j in ((0, 1), (0, 2), (1, 2))]
    axes += [cross(x, y) for x in ae for y in be]
    gaps = []
    for axis in axes:
        norm = length(axis)
        if norm < 1e-9:
            continue
        axis = [x/norm for x in axis]
        aa, bb = [[dot(p, axis) for p in m["vertices"]] for m in (a, b)]
        gaps.append(max(min(aa)-max(bb), min(bb)-max(aa)))
    return max(gaps)


def attached(meshes, base, label):
    all_meshes = [base]+meshes
    linked, pending = {0}, [0]
    while pending:
        index = pending.pop()
        for other in range(1, len(all_meshes)):
            if other not in linked and cuboid_gap(all_meshes[index], all_meshes[other]) <= .025:
                linked.add(other); pending.append(other)
    require(len(linked) == len(all_meshes), label+": detached blocks do not join the main head")


def is_goggle(mesh):
    return "goggle" in mesh.get("name", "").lower()


def expression_samples():
    points = [(x, y) for x in (-3.49, -2.25, -1.01, 1.01, 2.25, 3.49)
              for y in (-.99, .5, 1.99)]
    return points+[(x, y) for x in (-.99, 0, .99) for y in (1.66, 2.1, 2.64)]


def validate_face_clearance(meshes, base, label):
    points = expression_samples()
    for x, y in points:
        hits = ray_depths(base, x, y)
        require(hits and abs(min(hits)+3.25) < 1e-4, label+": base head does not back the face")
        for mesh in meshes:
            hits = ray_depths(mesh, x, y)
            require(not hits or min(hits) >= -3.255,
                    label+f": {mesh['name']} obscures an expression at {(x, y)}")
    return len(points)


def validate_head_parts(meshes, base, label):
    require(bool(meshes), label+": crown has no authored blocks")
    for mesh in meshes:
        validate_cuboid(mesh, label+"/"+mesh["name"])
    crown = [mesh for mesh in meshes if not is_goggle(mesh)]
    require(2 <= len(crown) <= 14, label+": crown needs a restrained set of chunky blocks and terminal caps")
    points = [v for mesh in crown for v in mesh["vertices"]]
    require(-7.5 <= min(v[1] for v in points) < -3.8,
            label+": crown does not rise visibly above the full head cube")
    require(max(v[1] for v in points) <= -2.3,
            label+": crown blocks descend into the forehead/face")
    require(max(abs(v[0]) for v in points) <= 5.5,
            label+": crown is excessively wide")
    require(max(v[0] for v in points)-min(v[0] for v in points) >= 2,
            label+": the main crown became a narrow stick")
    attached(crown, base, label)
    return validate_face_clearance(meshes, base, label)


def validate_goggles(meshes, base, label):
    parts = [m for m in meshes if is_goggle(m)]
    require(parts, label+": missing forehead goggles")
    for mesh in parts:
        validate_cuboid(mesh, label+"/"+mesh["name"])
    lens_parts = [m for m in parts if "lens" in m["name"].lower()]
    require(len(lens_parts) >= 2, label+": goggles must retain paired lenses")
    centres = [[sum(p[k] for p in m["vertices"])/8 for k in range(3)] for m in lens_parts]
    require(any(p[0] < -.5 for p in centres) and any(p[0] > .5 for p in centres),
            label+": goggles do not straddle the upper forehead")
    require(all(-3.75 <= p[1] <= -1.25 and p[2] < -3.25 for p in centres),
            label+": lenses sit on the crown or intersect the head instead of the forehead")
    # All frame and strap pieces must remain above every ordinary expression.
    require(max(v[1] for m in parts for v in m["vertices"]) < -1.05,
            label+": goggles overlap the visible eyes")
    require(min(v[1] for m in lens_parts for v in m["vertices"]) >= -3.75001,
            label+": goggles are perched on top of the skull")
    attached(parts, base, label+" goggles")
    return validate_face_clearance(parts, base, label)


def validate_default_preview(root, runtime_bones):
    project = json.loads((root/"art/blockbench/chao_mod_variants.bbmodel").read_text())
    elements = {e["uuid"]: e for e in project["elements"]}
    group_defs = {g["uuid"]: g for g in project.get("groups", [])}
    visible_groups, direct = {}, {}
    def visit(node, parent_visible=True):
        if isinstance(node, str):node = group_defs[node]
        else:node = {**group_defs.get(node.get("uuid"), {}), **node}
        active = parent_visible and node.get("visibility") is not False
        visible_groups[node["name"]] = active
        direct[node["name"]] = []
        for child in node.get("children", []):
            if isinstance(child, str) and child in elements:direct[node["name"]].append(elements[child])
            else:visit(child, active)
    for node in project["outliner"]:visit(node)
    for name in ("head", "eyes", "variant_create_head"):
        require(visible_groups.get(name) and direct[name]
                and all(e.get("visibility") is not False for e in direct[name]),
                "Default Blockbench preview hides "+name)
    require(not any(visible for name, visible in visible_groups.items()
                    if name.startswith("variant_") and name.endswith("_head") and name != "variant_create_head"),
            "Default preview overlays another crown")
    require(len(direct["head"]) == 1 and direct["head"][0].get("type") == "cube",
            "Default preview no longer has one full main head cube")
    bridge = importlib.import_module("blockbench_to_runtime")
    exported = {b["name"]: b for b in bridge.export_model(project)["bones"]}
    require(exported["head"]["meshes"] == runtime_bones["head"]["meshes"],
            "Default preview changed the main head during export")
    return 6


def corruptions(base, valid):
    free = copy.deepcopy(valid); free[0].pop("primitive")
    detached = copy.deepcopy(valid)
    for v in detached[0]["vertices"]:v[1] -= 20
    sheared = copy.deepcopy(valid);sheared[0]["vertices"][0][0] += .25
    stretched = copy.deepcopy(valid)
    stretched[0]["faces"][0]["uv"][0][0] += .5
    for broken, label in ((free, "free slope mesh"), (detached, "floating block"),
                          (sheared, "wedge"), (stretched, "stretched texture")):
        try:validate_head_parts(broken, base, label)
        except ValueError:continue
        raise ValueError("Head validator accepted "+label)
    return 4


def validate(root=ROOT):
    profiles = profiles_for(root)
    assets = root/"src/main/resources/assets/chao_garden"
    catalog = json.loads((assets/"chao_variants.json").read_text())["variants"]
    require({e["id"] for e in catalog if e.get("head_morph")} == set(profiles),
            "Saved head-profile flags differ from authored crowns")
    model = json.loads((prototype_path(root)).read_text())
    bones = {b["name"]: b for b in model["bones"]}
    base = bones["head"]["meshes"][0]
    sizes = validate_cuboid(base, "main head")
    require(sorted(round(x, 4) for x in sizes) == [6.5, 6.5, 8], "The main head cube dimensions changed")
    preview_checks = validate_default_preview(root, bones)
    signatures, rays, blocks = {}, 0, 0
    for ident in profiles:
        b = bones["variant_"+ident+"_head"]
        require(b["parent"] == "head" and b["pivot"] == [0, 0, 0], ident+": crown left the shared head transform")
        meshes = b["meshes"]
        rays += validate_head_parts(meshes, base, ident)
        blocks += sum(not is_goggle(m) for m in meshes)
        signature = hashlib.sha256(json.dumps([{"v": m["vertices"], "f": [f["v"] for f in m["faces"]]}
                      for m in meshes if not is_goggle(m)], sort_keys=True).encode()).hexdigest()
        require(signature not in signatures, ident+": crown geometry duplicates "+signatures.get(signature, "another theme"))
        signatures[signature] = ident
    goggle_signatures = []
    for ident in ("create", "thaumcraft"):
        meshes = bones["variant_"+ident+"_head"]["meshes"]
        rays += validate_goggles(meshes, base, ident)
        goggle_signatures.append(json.dumps([m["vertices"] for m in meshes if is_goggle(m)]))
    require(goggle_signatures[0] != goggle_signatures[1], "Create and Thaumcraft goggles share the same geometry")
    rejected = corruptions(base, bones["variant_create_head"]["meshes"])
    for offset, label in ((2.0, "goggles over the eyes"), (-3.0, "goggles on top of the crown")):
        broken = copy.deepcopy(bones["variant_create_head"]["meshes"])
        for mesh in broken:
            if is_goggle(mesh):
                for vertex in mesh["vertices"]:vertex[1] += offset
        try:validate_goggles(broken, base, label)
        except ValueError:rejected += 1;continue
        raise ValueError("Head validator accepted "+label)
    print(f"Block-built heads passed: {len(profiles)} distinct crowns, {blocks} editable crown blocks; "
          f"{rays:,} expression rays; 2 fitted goggle designs; {preview_checks} Blockbench checks; {rejected} rejected corruptions.")
    return len(profiles), rays


if __name__ == "__main__":
    validate()
