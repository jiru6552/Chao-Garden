#!/usr/bin/env python3
"""Validate editable Blockbench cuboids/meshes and their native Minecraft geometry.

This checks the saved-file contract and actual mesh data. It is deliberately
not described as an automated Blockbench application/import test.
"""

from __future__ import annotations

import argparse
import base64
import copy
import importlib.util
import io
import json
import math
import subprocess
import sys
import tempfile
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"art"))
from runtime_resource_contract_022 import prototype_path
from typing import Any

from PIL import Image
from validate_head_morphs import profiles_for, validate_head_parts

ROOT = Path(__file__).resolve().parents[1]
EPSILON = 1e-7


class ModelValidationError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ModelValidationError(message)


def vector(value: Any, length: int, label: str) -> list[float]:
    require(isinstance(value, (list, tuple)) and len(value) == length,
            f"{label}: expected {length} coordinates")
    require(all(isinstance(v, (int, float)) and not isinstance(v, bool)
                and math.isfinite(v) for v in value), f"{label}: non-finite coordinate")
    return list(value)


def subtract(a: list[float], b: list[float]) -> list[float]:
    return [a[i] - b[i] for i in range(3)]


def cross(a: list[float], b: list[float]) -> list[float]:
    return [a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]]


def dot(a: list[float], b: list[float]) -> float:
    return sum(a[i] * b[i] for i in range(3))


def face_geometry(points: list[list[float]], label: str) -> float:
    """Reject repeated/zero-area triangles and return signed fan volume."""
    require(len(points) >= 3, f"{label}: fewer than three face vertices")
    volume = 0.0
    area_squared = 0.0
    for i in range(1, len(points) - 1):
        a, b, c = points[0], points[i], points[i + 1]
        normal = cross(subtract(b, a), subtract(c, a))
        area_squared += dot(normal, normal)
        volume += dot(a, cross(b, c)) / 6.0
    require(area_squared > EPSILON * EPSILON, f"{label}: zero-area face")
    return volume


@dataclass
class MeshSummary:
    file: str
    elements: int = 0
    meshes: int = 0
    cubes: int = 0
    integer_cubes: int = 0
    half_unit_cubes: int = 0
    unit_density_faces: int = 0
    cube_faces: int = 0
    vertices: int = 0
    faces: int = 0
    bones: int = 0
    animations: int = 0
    embedded_textures: int = 0
    closed_meshes: int = 0


def topology(faces: list[list[str]], positions: dict[str, list[float]],
             label: str) -> bool:
    """Check local winding; every closed connected surface must point outward."""
    edges: dict[tuple[str, str], list[tuple[int, str, str]]] = defaultdict(list)
    face_edges: list[list[tuple[str, str]]] = []
    volumes = []
    for index, keys in enumerate(faces):
        require(len(set(keys)) == len(keys), f"{label}/face {index}: repeated vertex")
        volumes.append(face_geometry([positions[k] for k in keys],
                                     f"{label}/face {index}"))
        current_edges = []
        for i, key in enumerate(keys):
            other = keys[(i + 1) % len(keys)]
            edge = tuple(sorted((key, other)))
            edges[edge].append((index, key, other))
            current_edges.append(edge)
        face_edges.append(current_edges)
    adjacent: dict[int, set[int]] = defaultdict(set)
    for key, uses in edges.items():
        require(len(uses) <= 2, f"{label}: non-manifold edge {key}")
        if len(uses) == 2:
            require(uses[0][1:] == tuple(reversed(uses[1][1:])),
                    f"{label}: inconsistent winding along edge {key}")
            adjacent[uses[0][0]].add(uses[1][0])
            adjacent[uses[1][0]].add(uses[0][0])
    visited: set[int] = set()
    all_closed = bool(edges)
    for start in range(len(faces)):
        if start in visited:
            continue
        component = set()
        pending = [start]
        while pending:
            current = pending.pop()
            if current in component:
                continue
            component.add(current)
            pending.extend(adjacent[current] - component)
        visited.update(component)
        closed = all(len(edges[e]) == 2 for index in component for e in face_edges[index])
        all_closed &= closed
        if closed:
            volume = sum(volumes[index] for index in component)
            require(abs(volume) > EPSILON, f"{label}: closed surface has zero volume")
            require(volume > 0, f"{label}: closed surface is inside-out")
    return all_closed



CUBE_DIRECTIONS = {"north", "south", "east", "west", "up", "down"}


def validate_cube(element: dict[str, Any], texture_sizes: list[tuple[float, float]],
                  label: str, summary: MeshSummary, default_box_uv: bool = False) -> None:
    """Validate actual Blockbench Cube primitives, including zero-depth panes.

    UV mirroring is legal: rectangle endpoints need not be increasing. A pane
    has at least two nonzero dimensions; disabled or texture:null faces are not
    renderable. Geometric topology is checked after conversion by the exporter.
    """
    start = vector(element.get("from"), 3, label + "/from")
    end = vector(element.get("to"), 3, label + "/to")
    size = [end[i] - start[i] for i in range(3)]
    require(all(v >= -EPSILON for v in size), label + ": reversed cube bounds")
    inflate = element.get("inflate", 0)
    require(type(inflate) in (int, float) and math.isfinite(inflate),
            label + ": invalid cube inflate")
    stretch = vector(element.get("stretch", [1, 1, 1]), 3, label + "/stretch")
    require(all(v > 0 for v in stretch), label + ": nonpositive stretch")
    physical = [(v + 2 * inflate) * stretch[i] for i, v in enumerate(size)]
    require(all(v >= -EPSILON for v in physical)
            and sum(v > EPSILON for v in physical) >= 2,
            label + ": cube is collapsed to a line or point")
    summary.cubes += 1
    summary.integer_cubes += int(all(abs(v - round(v)) <= EPSILON for v in size))
    summary.half_unit_cubes += int(all(abs(2 * v - round(2 * v)) <= EPSILON for v in size))
    faces = element.get("faces", {})
    require(isinstance(faces, dict), label + ": missing cube faces")
    require(set(faces) <= CUBE_DIRECTIONS, label + ": invalid cube face direction")
    box_uv = element.get("box_uv", default_box_uv)
    require(isinstance(box_uv, bool), label + ": invalid box_uv flag")
    offset = vector(element.get("uv_offset", [0, 0]), 2, label + "/uv_offset")
    if box_uv:
        faces = {direction: faces.get(direction, {"texture": 0}) for direction in CUBE_DIRECTIONS}
    active = 0
    for direction, face in faces.items():
        context = label + "/face " + direction
        require(isinstance(face, dict), context + ": invalid cube face")
        if face.get("enabled") is False or face.get("texture") is None:
            continue
        texture = face.get("texture")
        require(type(texture) is int and 0 <= texture < len(texture_sizes),
                context + ": invalid texture array index")
        rotation = face.get("rotation", 0)
        require(type(rotation) in (int, float) and math.isfinite(rotation)
                and abs(rotation / 90 - round(rotation / 90)) <= EPSILON,
                context + ": UV rotation must be a multiple of 90 degrees")
        if box_uv:
            # A standard Box UV unwrap occupies 2*(width+depth) by height+depth.
            w, h, d = size
            tw, th = texture_sizes[texture]
            require(offset[0] >= -EPSILON and offset[1] >= -EPSILON
                    and offset[0] + 2 * (w + d) <= tw + EPSILON
                    and offset[1] + h + d <= th + EPSILON,
                    context + ": Box UV layout outside texture")
        else:
            uv = vector(face.get("uv"), 4, context + "/UV")
            tw, th = texture_sizes[texture]
            require(all(-EPSILON <= uv[i] <= (tw if i % 2 == 0 else th) + EPSILON
                        for i in range(4)), context + ": UV outside texture")
            u, v = abs(uv[2] - uv[0]), abs(uv[3] - uv[1])
            if int(round(rotation / 90)) % 2:
                u, v = v, u
            a, b = (size[0], size[1]) if direction in ("north", "south") else \
                   (size[2], size[1]) if direction in ("east", "west") else \
                   (size[0], size[2])
            if a > EPSILON and b > EPSILON:
                require(u > EPSILON and v > EPSILON, context + ": empty UV rectangle")
                summary.unit_density_faces += int(abs(u - a) <= EPSILON and abs(v - b) <= EPSILON)
        # Edge faces of zero-depth panes are intentionally omitted by the bridge.
        a, b = (physical[0], physical[1]) if direction in ("north", "south") else \
               (physical[2], physical[1]) if direction in ("east", "west") else \
               (physical[0], physical[2])
        if a > EPSILON and b > EPSILON:
            active += 1
            summary.cube_faces += 1
            if box_uv:
                summary.unit_density_faces += 1
    require(active > 0, label + ": no active nondegenerate cube faces")

def validate_bbmodel(model: dict[str, Any], path: str = "<memory>") -> MeshSummary:
    summary = MeshSummary(path)
    require(isinstance(model, dict), f"{path}: expected a JSON object")
    meta = model.get("meta", {})
    require(meta.get("model_format") == "free", f"{path}: expected Generic/free format")
    version = str(meta.get("format_version", ""))
    require(version.split(".")[0] in ("4", "5"),
            f"{path}: expected supported Blockbench 4.x/5.x saved-file format")
    resolution = model.get("resolution", {})
    width, height = resolution.get("width"), resolution.get("height")
    require(isinstance(width, int) and isinstance(height, int) and width > 0 and height > 0,
            f"{path}: invalid project texture resolution")

    textures = model.get("textures", [])
    require(isinstance(textures, list) and textures, f"{path}: no embedded textures")
    texture_sizes = []
    for i, texture in enumerate(textures):
        source = texture.get("source", "")
        require(isinstance(source, str) and source.startswith("data:image/png;base64,"),
                f"{path}/texture {i}: expected self-contained embedded PNG")
        try:
            png = base64.b64decode(source.split(",", 1)[1], validate=True)
            with Image.open(io.BytesIO(png)) as image:
                image.verify()
            with Image.open(io.BytesIO(png)) as image:
                physical_size = image.size
        except Exception as error:
            raise ModelValidationError(f"{path}/texture {i}: invalid PNG: {error}") from error
        uv_width = texture.get("uv_width", width)
        uv_height = texture.get("uv_height", height)
        require(isinstance(uv_width, (int, float)) and uv_width > 0
                and isinstance(uv_height, (int, float)) and uv_height > 0,
                f"{path}/texture {i}: invalid UV resolution")
        if "width" in texture:
            require(texture["width"] == physical_size[0],
                    f"{path}/texture {i}: physical width does not match PNG")
        if "height" in texture:
            require(texture["height"] == physical_size[1],
                    f"{path}/texture {i}: physical height does not match PNG")
        texture_sizes.append((uv_width, uv_height))
    summary.embedded_textures = len(textures)

    elements = model.get("elements", [])
    require(isinstance(elements, list) and elements, f"{path}: no editable elements")
    uuids: set[str] = set()
    element_ids: set[str] = set()
    for element in elements:
        uid = element.get("uuid")
        name = element.get("name", uid)
        label = f"{path}/{name}"
        require(isinstance(uid, str) and uid and uid not in uuids,
                f"{label}: missing or duplicate element UUID")
        uuids.add(uid)
        element_ids.add(uid)
        summary.elements += 1
        vector(element.get("origin", [0, 0, 0]), 3, label + "/origin")
        vector(element.get("rotation", [0, 0, 0]), 3, label + "/rotation")
        if element.get("type", "cube") == "cube":
            validate_cube(element, texture_sizes, label, summary, meta.get("box_uv", False))
            continue
        require(element.get("type") == "mesh", f"{label}: expected an editable cube or mesh")
        require(element.get("shading", "flat") in ("flat", "smooth"),
                f"{label}: invalid shading")
        vertices = element.get("vertices")
        require(isinstance(vertices, dict) and len(vertices) >= 3, f"{label}: missing vertices")
        for key, position in vertices.items():
            require(isinstance(key, str) and key, f"{label}: invalid vertex key")
            vector(position, 3, f"{label}/vertex {key}")
        faces = element.get("faces")
        require(isinstance(faces, dict) and faces, f"{label}: missing faces")
        ordered_faces = []
        for key, face in faces.items():
            face_label = f"{label}/face {key}"
            keys = face.get("vertices")
            require(isinstance(keys, list) and len(keys) in (3, 4),
                    f"{face_label}: only triangles/quads are supported")
            require(all(k in vertices for k in keys), f"{face_label}: dangling vertex reference")
            texture = face.get("texture")
            require(type(texture) is int and 0 <= texture < len(textures),
                    f"{face_label}: invalid texture array index")
            size = texture_sizes[texture]
            uv = face.get("uv")
            require(isinstance(uv, dict), f"{face_label}: missing per-vertex UV map")
            for vertex_id in keys:
                coords = vector(uv.get(vertex_id), 2, f"{face_label}/UV {vertex_id}")
                require(-EPSILON <= coords[0] <= size[0] + EPSILON
                        and -EPSILON <= coords[1] <= size[1] + EPSILON,
                        f"{face_label}/UV {vertex_id}: outside texture")
            ordered_faces.append(keys)
        summary.closed_meshes += int(topology(ordered_faces, vertices, label))
        summary.meshes += 1
        summary.vertices += len(vertices)
        summary.faces += len(faces)

    groups: dict[str, dict[str, Any]] = {}
    for group in model.get("groups", []):
        uid = group.get("uuid")
        require(isinstance(uid, str) and uid and uid not in uuids,
                f"{path}: missing or duplicate group UUID")
        uuids.add(uid)
        groups[uid] = group
    hierarchy_seen: set[str] = set()
    def visit(node: Any, ancestors: tuple[str, ...] = ()) -> None:
        if isinstance(node, str):
            require(node in element_ids, f"{path}: dangling element reference {node}")
            require(node not in hierarchy_seen, f"{path}: element referenced more than once {node}")
            hierarchy_seen.add(node)
            return
        require(isinstance(node, dict), f"{path}: invalid outliner entry")
        uid = node.get("uuid")
        require(isinstance(uid, str) and uid and uid not in element_ids,
                f"{path}: invalid outliner group UUID")
        require(uid not in ancestors and uid not in hierarchy_seen,
                f"{path}: cyclic or duplicated group {uid}")
        if uid not in groups:
            require(version.startswith("4."), f"{path}: missing flat group definition {uid}")
            require(uid not in uuids, f"{path}: duplicate group {uid}")
            uuids.add(uid)
            groups[uid] = node
        group = groups[uid]
        require(isinstance(group.get("name"), str) and group["name"],
                f"{path}: unnamed group {uid}")
        vector(group.get("origin", [0, 0, 0]), 3, f"{path}/{group['name']}/origin")
        vector(group.get("rotation", [0, 0, 0]), 3, f"{path}/{group['name']}/rotation")
        hierarchy_seen.add(uid)
        require(isinstance(node.get("children", []), list), f"{path}: invalid child list")
        for child in node.get("children", []):
            visit(child, ancestors + (uid,))
    for node in model.get("outliner", []):
        visit(node)
    require(element_ids <= hierarchy_seen, f"{path}: orphan elements")
    require(set(groups) <= hierarchy_seen, f"{path}: orphan groups")
    summary.bones = len(groups)
    require(summary.bones > 0, f"{path}: missing animation rig")

    animation_ids: set[str] = set()
    for animation in model.get("animations", []):
        aid = animation.get("uuid")
        require(isinstance(aid, str) and aid and aid not in animation_ids,
                f"{path}: missing or duplicate animation UUID")
        animation_ids.add(aid)
        length = animation.get("length")
        require(isinstance(length, (int, float)) and math.isfinite(length) and length > 0,
                f"{path}: invalid animation duration")
        for target, animator in animation.get("animators", {}).items():
            require(target == "effects" or target in groups,
                    f"{path}: animation targets missing bone {target}")
            for keyframe in animator.get("keyframes", []):
                time = keyframe.get("time")
                require(isinstance(time, (int, float)) and math.isfinite(time)
                        and -EPSILON <= time <= length + EPSILON,
                        f"{path}: keyframe outside animation duration")
                require(isinstance(keyframe.get("data_points"), list)
                        and keyframe["data_points"], f"{path}: empty animation keyframe")
        summary.animations += 1
    return summary


def validate_runtime(model: dict[str, Any], path: str = "<runtime>") -> dict[str, Any]:
    width, height = model.get("texture_width"), model.get("texture_height")
    require(isinstance(width, int) and width > 0 and isinstance(height, int) and height > 0,
            f"{path}: invalid runtime texture dimensions")
    bones = model.get("bones")
    require(isinstance(bones, list) and bones, f"{path}: missing runtime bones")
    by_name = {}
    for bone in bones:
        name = bone.get("name")
        require(isinstance(name, str) and name and name not in by_name,
                f"{path}: missing or duplicate runtime bone")
        by_name[name] = bone
        vector(bone.get("pivot"), 3, f"{path}/{name}/pivot")
        vector(bone.get("rotation", [0, 0, 0]), 3, f"{path}/{name}/rotation")
        for mesh in bone.get("meshes", []):
            label = f"{path}/{name}/{mesh.get('name')}"
            require(mesh.get("shading", "flat") in ("flat", "smooth"),
                    f"{label}: invalid runtime shading")
            vertices = mesh.get("vertices")
            require(isinstance(vertices, list) and len(vertices) >= 3, f"{label}: missing vertices")
            for index, v in enumerate(vertices):
                vector(v, 3, f"{label}/vertex {index}")
            faces = mesh.get("faces")
            require(isinstance(faces, list) and faces, f"{label}: missing faces")
            for index, face in enumerate(faces):
                indices = face.get("v")
                require(isinstance(indices, list) and len(indices) in (3, 4)
                        and all(type(v) is int and 0 <= v < len(vertices) for v in indices),
                        f"{label}/face {index}: invalid vertex index")
                require(len(set(indices)) == len(indices), f"{label}/face {index}: repeated vertex")
                face_geometry([vertices[v] for v in indices], f"{label}/face {index}")
                uv = face.get("uv")
                require(isinstance(uv, list) and len(uv) == len(indices),
                        f"{label}/face {index}: UV count mismatch")
                for point in uv:
                    u, v = vector(point, 2, f"{label}/face {index}/UV")
                    require(-EPSILON <= u <= width + EPSILON
                            and -EPSILON <= v <= height + EPSILON,
                            f"{label}/face {index}: UV outside texture")
            if mesh.get("primitive") == "cube":
                positions = {str(i): v for i, v in enumerate(vertices)}
                face_ids = [[str(i) for i in face["v"]] for face in faces]
                geometric_faces = [tuple(sorted(tuple(round(v, 5) for v in vertices[i])
                                                for i in face["v"])) for face in faces]
                require(len(set(geometric_faces)) == len(geometric_faces),
                        label + ": duplicate coplanar cube faces would cause z-fighting")
                topology(face_ids, positions, label)

    for name, bone in by_name.items():
        parent = bone.get("parent")
        require(parent is None or parent in by_name, f"{path}/{name}: missing parent {parent}")
        ancestors = {name}
        while parent:
            require(parent not in ancestors, f"{path}/{name}: cyclic runtime hierarchy")
            ancestors.add(parent)
            parent = by_name[parent].get("parent")
    return by_name


def close_vector(actual: Any, expected: Any, label: str) -> None:
    require(len(actual) == len(expected)
            and all(abs(a - b) <= 0.00002 for a, b in zip(actual, expected)),
            f"{label}: {actual!r} differs from {expected!r}")


def same_render_geometry(actual: dict[str, Any], expected: dict[str, Any],
                         label: str, *, exact_materials: bool = True) -> None:
    """Compare the geometry consumed by the Java renderer, not JSON whitespace."""
    actual_bones = validate_runtime(actual, label + "/actual")
    expected_bones = validate_runtime(expected, label + "/expected")
    require(actual["texture_width"] == expected["texture_width"]
            and actual["texture_height"] == expected["texture_height"],
            label + ": texture resolution changed")
    require(actual_bones.keys() == expected_bones.keys(), label + ": bone names changed")
    luminous = {"orb", "halo", "dark_orb", "heart", "flame"}
    for name, bone in expected_bones.items():
        other = actual_bones[name]
        require(other.get("parent") == bone.get("parent"), f"{label}/{name}: parent changed")
        close_vector(other["pivot"], bone["pivot"], f"{label}/{name}/pivot")
        close_vector(other.get("rotation", [0, 0, 0]), bone.get("rotation", [0, 0, 0]),
                     f"{label}/{name}/rest rotation")
        actual_meshes, expected_meshes = other.get("meshes", []), bone.get("meshes", [])
        require(len(actual_meshes) == len(expected_meshes), f"{label}/{name}: mesh count changed")
        for index, (a, b) in enumerate(zip(actual_meshes, expected_meshes)):
            context = f"{label}/{name}/mesh {index}"
            require(a.get("name") == b.get("name"), context + ": mesh name/order changed")
            require(a.get("shading", "flat") == b.get("shading", "flat"),
                    context + ": flat/smooth shading changed")
            require(a.get("primitive") == b.get("primitive"),
                    context + ": source primitive marker changed")
            if exact_materials:
                require(a.get("material") == b.get("material"), context + ": material changed")
            else:
                # Eye/pupil are aliases for the same non-luminous UV tile. Generic
                # Blockbench saves drop custom material labels; rendering still must match.
                require((a.get("material") in luminous) == (b.get("material") in luminous),
                        context + ": emissive behavior changed after metadata removal")
            require(len(a["vertices"]) == len(b["vertices"]), context + ": vertex count changed")
            for vi, (v1, v2) in enumerate(zip(a["vertices"], b["vertices"])):
                close_vector(v1, v2, f"{context}/vertex {vi}")
            require(len(a["faces"]) == len(b["faces"]), context + ": face count changed")
            for fi, (f1, f2) in enumerate(zip(a["faces"], b["faces"])):
                require(f1["v"] == f2["v"], f"{context}/face {fi}: winding/indices changed")
                require(len(f1["uv"]) == len(f2["uv"]), f"{context}/face {fi}: UV count changed")
                for ui, (uv1, uv2) in enumerate(zip(f1["uv"], f2["uv"])):
                    close_vector(uv1, uv2, f"{context}/face {fi}/UV {ui}")


def group_nodes(project: dict[str, Any]) -> list[dict[str, Any]]:
    """Return actual editable group definitions in either saved-file layout."""
    if project.get("groups") is not None:
        return project["groups"]
    result = []
    def visit(node: Any) -> None:
        if isinstance(node, dict):
            result.append(node)
            for child in node.get("children", []):
                visit(child)
    for node in project.get("outliner", []):
        visit(node)
    return result


def parent_of_element(project: dict[str, Any], element_uuid: str) -> tuple[dict[str, Any], list[Any]]:
    flat = {g["uuid"]: g for g in project.get("groups", [])}
    def visit(node: Any) -> Any:
        if not isinstance(node, dict):
            return None
        children = node.get("children", [])
        if element_uuid in children:
            return flat.get(node["uuid"], node), children
        for child in children:
            found = visit(child)
            if found is not None:
                return found
        return None
    for node in project.get("outliner", []):
        found = visit(node)
        if found is not None:
            return found
    raise ModelValidationError(f"No parent rig group for mesh {element_uuid}")


def to_v5_layout(project: dict[str, Any]) -> dict[str, Any]:
    """Exercise the official v5 flat-groups/outliner-reference representation."""
    result = copy.deepcopy(project)
    if str(result.get("meta", {}).get("format_version", "")).startswith("5."):
        return result
    flat_groups = []
    def flatten(node: Any) -> Any:
        if isinstance(node, str):
            return node
        children = node.get("children", [])
        flat_groups.append({k: copy.deepcopy(v) for k, v in node.items() if k != "children"})
        return {"uuid": node["uuid"], "isOpen": node.get("isOpen", False),
                "children": [flatten(child) for child in children]}
    result["outliner"] = [flatten(node) for node in result["outliner"]]
    result["groups"] = flat_groups
    result["meta"]["format_version"] = "5.0"
    return result


def rejection_checks(project: dict[str, Any]) -> int:
    """Reject broken artist data rather than silently producing corrupt geometry."""
    corruptions = []
    mesh = next((e for e in project["elements"] if e.get("type") == "mesh"), None)
    if mesh is not None:
        index = project["elements"].index(mesh)
        broken = copy.deepcopy(project)
        face = next(iter(broken["elements"][index]["faces"].values()))
        face["vertices"][0] = "missing_vertex"
        corruptions.append(("dangling mesh vertex", broken))
        broken = copy.deepcopy(project)
        face = next(iter(broken["elements"][index]["faces"].values()))
        face["uv"][face["vertices"][0]][0] = float("nan")
        corruptions.append(("non-finite mesh UV", broken))
    cube = next((e for e in project["elements"] if e.get("type") == "cube"), None)
    if cube is not None:
        index = project["elements"].index(cube)
        for reason, mutate in (
            ("non-finite cube bounds", lambda e: e["from"].__setitem__(0, float("nan"))),
            ("collapsed cube line", lambda e: e.update({"from": [0, 0, 0], "to": [0, 0, 3], "inflate": 0})),
            ("non-quarter-turn cube UV", lambda e: next(iter(e["faces"].values())).__setitem__("rotation", 13)),
            ("cube with no active faces", lambda e: [f.__setitem__("texture", None) for f in e["faces"].values()]),
            ("Box UV outside texture", lambda e: e.update({"box_uv": True, "uv_offset": [100000, 100000]})),
        ):
            broken = copy.deepcopy(project)
            mutate(broken["elements"][index])
            corruptions.append((reason, broken))
    broken = copy.deepcopy(project)
    broken["outliner"].append(broken["outliner"][0])
    corruptions.append(("duplicate hierarchy", broken))
    broken = copy.deepcopy(project)
    broken["textures"][0]["source"] = "data:image/png;base64,bm90IGEgcG5n"
    corruptions.append(("invalid embedded PNG", broken))
    for reason, broken in corruptions:
        try:
            validate_bbmodel(broken, "<intentionally invalid: " + reason + ">")
        except ModelValidationError:
            continue
        raise ModelValidationError("Validator accepted intentionally invalid " + reason)
    return len(corruptions)


def roundtrip_checks(project: dict[str, Any], expected: dict[str, Any],
                     path: Path) -> int:
    """Export real editable projects and verify edits reach the runtime format."""
    bridge_path = ROOT / "art" / "blockbench_to_runtime.py"
    require(bridge_path.is_file(), "Missing editable-model export bridge")
    spec = importlib.util.spec_from_file_location("chao_blockbench_bridge", bridge_path)
    require(spec is not None and spec.loader is not None, "Cannot load export bridge")
    bridge = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(bridge)
    exported = bridge.export_model(copy.deepcopy(project))
    same_render_geometry(exported, expected, path.name + "/unmodified export")
    checks = 1

    v5 = to_v5_layout(project)
    validate_bbmodel(v5, path.name + "/v5 layout")
    same_render_geometry(bridge.export_model(v5), expected, path.name + "/v5 export")
    checks += 1

    # The official codec serializes registered properties only. A user saving in
    # Blockbench may lose custom labels, so the public bridge must infer these safely.
    no_metadata = copy.deepcopy(v5)
    no_metadata.pop("chao_runtime", None)
    for element in no_metadata["elements"]:
        element.pop("chao_material", None)
    same_render_geometry(bridge.export_model(no_metadata), expected,
                         path.name + "/ordinary editor save", exact_materials=False)
    checks += 1

    first_element = project["elements"][0]
    element_uuid = first_element["uuid"]
    group, children = parent_of_element(project, element_uuid)
    target_name = group["name"]
    target_index = [child for child in children if isinstance(child, str)
                    and any(e["uuid"] == child for e in project["elements"])].index(element_uuid)
    baseline_mesh = next(b for b in exported["bones"] if b["name"] == target_name)["meshes"][target_index]
    vertex_index = 0

    changed = copy.deepcopy(project)
    if first_element.get("type") == "cube":
        changed["elements"][0]["to"][0] += 0.125
    else:
        key = next(iter(first_element["vertices"]))
        vertex_index = list(first_element["vertices"]).index(key)
        changed["elements"][0]["vertices"][key][0] += 0.125
    edited = bridge.export_model(changed)
    edited_mesh = next(b for b in edited["bones"] if b["name"] == target_name)["meshes"][target_index]
    require(edited_mesh["vertices"] != baseline_mesh["vertices"],
            path.name + ": an actual geometry edit did not reach the runtime mesh")
    checks += 1

    changed = copy.deepcopy(project)
    first_face = next(f for f in changed["elements"][0]["faces"].values()
                      if f.get("texture") is not None and f.get("enabled", True))
    if first_element.get("type") == "cube":
        old_u = first_face["uv"][0]
        delta = 0.25 if old_u + 0.25 <= project["resolution"]["width"] else -0.25
        first_face["uv"][0] += delta
    else:
        uv_key = first_face["vertices"][0]
        old_u = first_face["uv"][uv_key][0]
        delta = 0.25 if old_u + 0.25 <= project["resolution"]["width"] else -0.25
        first_face["uv"][uv_key][0] += delta
    edited = bridge.export_model(changed)
    edited_mesh = next(b for b in edited["bones"] if b["name"] == target_name)["meshes"][target_index]
    require([f["uv"] for f in edited_mesh["faces"]] != [f["uv"] for f in baseline_mesh["faces"]],
            path.name + ": an actual UV edit did not reach the runtime mesh")
    checks += 1

    changed = copy.deepcopy(project)
    edited_group = next(g for g in group_nodes(changed) if g["uuid"] == group["uuid"])
    edited_group["rotation"] = [0, 0, 7]
    edited = bridge.export_model(changed)
    rest = next(b for b in edited["bones"] if b["name"] == target_name)
    close_vector(rest["rotation"], [0, 0, -7],
                 path.name + "/edited bone rotation uses the reflected coordinate basis")
    checks += 1

    changed = copy.deepcopy(project)
    edited_group = next(g for g in group_nodes(changed) if g["uuid"] == group["uuid"])
    edited_group["origin"][0] += 0.25
    edited = bridge.export_model(changed)
    shifted = next(b for b in edited["bones"] if b["name"] == target_name)
    base_bone = next(b for b in exported["bones"] if b["name"] == target_name)
    expected_pivot = base_bone["pivot"].copy()
    expected_pivot[0] += 0.25
    close_vector(shifted["pivot"], expected_pivot,
                 path.name + "/edited pivot reaches runtime rig")
    expected_local = baseline_mesh["vertices"][vertex_index].copy()
    expected_local[0] -= 0.25
    close_vector(shifted["meshes"][target_index]["vertices"][vertex_index], expected_local,
                 path.name + "/pivot edit preserves mesh rest position")
    checks += 1

    with tempfile.TemporaryDirectory(prefix="chao-model-roundtrip-") as temporary:
        directory = Path(temporary)
        source, destination, texture = directory / "edited.bbmodel", directory / "model.json", directory / "atlas.png"
        source.write_text(json.dumps(project))
        subprocess.run([sys.executable, str(bridge_path), str(source), str(destination),
                        "--texture-output", str(texture)], check=True, capture_output=True, text=True)
        same_render_geometry(json.loads(destination.read_text()), expected,
                             path.name + "/public command-line export")
        embedded = base64.b64decode(project["textures"][0]["source"].split(",", 1)[1], validate=True)
        require(texture.read_bytes() == embedded, path.name + ": command-line export changed PNG bytes")
    return checks + 1



def validate_chao_cube_style(project: dict[str, Any], summary: MeshSummary,
                            path: str) -> int:
    """Release art contract: broad, shallow Chao proportions and readable pixel eyes.

    This is a test of this mod's authored Chao rig, not a claim that every
    possible Blockbench project or every Bedrock character has these shapes.
    The command-line escape is useful while artists intentionally experiment.
    """
    groups = {group["name"]: group for group in group_nodes(project)}
    if not {"body", "head", "eyes"} <= groups.keys():
        return 0  # Static egg and independent custom exporter fixtures.
    elements = {element["uuid"]: element for element in project["elements"]}

    def direct(name: str) -> list[dict[str, Any]]:
        return [elements[child] for child in groups[name].get("children", [])
                if isinstance(child, str) and child in elements]

    require(summary.cubes > 0 and summary.half_unit_cubes == summary.cubes,
            f"{path}: release Chao cube Size dimensions must use half-unit increments")
    require(summary.cube_faces > 0 and summary.unit_density_faces == summary.cube_faces,
            f"{path}: release cube faces must use one texture texel per model pixel")
    head = direct("head")
    require(len(head) == 1 and head[0].get("type") == "cube",
            f"{path}: the primary head must be one unbroken cube plane, not an ellipsoid/stack")
    body = direct("body")
    require(len(body) == 1 and body[0].get("type") == "cube",
            f"{path}: the primary torso must be one compact cuboid")
    head_size = subtract(head[0]["to"], head[0]["from"])
    body_size = subtract(body[0]["to"], body[0]["from"])
    close_vector(head_size, [8, 6.5, 6.5], f"{path}: release head dimensions")
    require(min(head_size) > EPSILON
            and 1.10 <= head_size[0] / head_size[1] <= 1.50
            and .60 <= head_size[2] / head_size[0] <= .95,
            f"{path}: primary head must remain broader than tall with a shallower depth")
    require(body_size[0] > EPSILON and 1.40 <= head_size[0] / body_size[0] <= 1.90,
            f"{path}: head must read broad against the small torso")
    for name in ("left_arm", "right_arm", "left_foot", "right_foot"):
        require(name in groups and direct(name)
                and all(e.get("type") == "cube" for e in direct(name)),
                f"{path}: {name} must use cuboid anatomy")
    for name in ("left_arm", "right_arm"):
        require(head[0]["from"][1] - max(e["to"][1] for e in direct(name)) >= .75,
                f"{path}: {name} must rest clearly below the head instead of at face height")

    def expression_density(uv_extents: list[float], spatial: list[float], label: str,
                           name: str) -> None:
        if name == "eyes":
            # The primary white/pupil/white design intentionally samples four
            # columns across 2.5 units, preserving its 1:2:1 painted widths.
            # These broad limits catch accidental stretching without requiring
            # the primary face design to share the expression atlas's density.
            density = [uv_extents[i] / spatial[i] for i in range(2)]
            require(1.40 <= density[0] <= 2.10 and 1.85 <= density[1] <= 2.15,
                    label + ": primary eye UV density is outside the readable face range")
        else:
            close_vector(uv_extents, [2 * v for v in spatial],
                         label + ": expression pixels must keep two texels per model pixel")

    for name in ("eyes", "lids", "eyes_happy", "mouth", "mouth_open"):
        require(name in groups and direct(name), f"{path}: missing pixel expression {name}")
        for plane in direct(name):
            label = f"{path}/{name}"
            if plane.get("type") == "cube":
                sizes = [abs(b - a) for a, b in zip(plane["from"], plane["to"])]
                require(sum(v <= EPSILON for v in sizes) == 1,
                        label + ": facial cubes must be zero-thickness pixel planes")
                spatial = sorted(v for v in sizes if v > EPSILON)
                active_faces = [face for face in plane["faces"].values()
                                if face.get("enabled") is not False and face.get("texture") is not None]
                require(bool(active_faces), label + ": pixel expression has no painted face")
                for face in active_faces:
                    uv = face["uv"]
                    uv_extents = sorted([abs(uv[2] - uv[0]), abs(uv[3] - uv[1])])
                    expression_density(uv_extents, spatial, label, name)
            else:
                positions = list(plane["vertices"].values())
                extents = [max(v[i] for v in positions) - min(v[i] for v in positions)
                           for i in range(3)]
                require(sum(v <= EPSILON for v in extents) == 1,
                        label + ": expression must be planar pixel art")
                uv = [point for face in plane["faces"].values() for point in face["uv"].values()]
                uv_extents = sorted(max(p[i] for p in uv) - min(p[i] for p in uv) for i in range(2))
                spatial = sorted(v for v in extents if v > EPSILON)
                expression_density(uv_extents, spatial, label, name)

    require("orb" in groups and direct("orb")
            and all(e.get("type") == "cube" for e in direct("orb")),
            f"{path}: the neutral emotion marker must use a cube silhouette")
    require("tuft" in groups and direct("tuft")
            and all(e.get("type") == "cube" for e in direct("tuft")),
            f"{path}: the low swept crest must remain editable cube anatomy")
    # Use the exporter's rotated cube corners so a steeply tilted crest cannot
    # pass only because its unrotated Size dimensions happen to be short.
    bridge_path = ROOT / "art" / "blockbench_to_runtime.py"
    spec = importlib.util.spec_from_file_location("chao_style_bridge", bridge_path)
    require(spec is not None and spec.loader is not None, "Cannot load cube style bridge")
    bridge = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(bridge)
    materials = project.get("chao_runtime", {}).get("materials", bridge.DEFAULT_MATERIALS)
    crest_points = [point for element in direct("tuft")
                    for point in bridge.cube_to_runtime(element, [0, 0, 0], materials)["vertices"]]
    crest_extents = [max(p[i] for p in crest_points) - min(p[i] for p in crest_points)
                     for i in range(3)]
    crown_height = head[0]["to"][1]
    crest_rise = max(-p[1] for p in crest_points) - crown_height
    require(0 < crest_rise <= head_size[1] * .45
            and max(crest_extents[0], crest_extents[2]) >= head_size[0] * .35,
            f"{path}: crest must stay low and broad above the compact head")
    animal_groups = [name for name in groups if name.startswith("animal_")]
    if animal_groups:
        require(all(all(element.get("type") == "cube" for element in direct(name))
                    for name in animal_groups),
                f"{path}: animal gene anatomy must use editable cuboids")
        require(all(len(name.split("_")) >= 3 and 0 <= int(name.split("_")[1]) < 8
                    and 0 <= int(name.split("_")[2]) < 18 for name in animal_groups),
                f"{path}: ghost-gene or invalid-slot geometry must not be fabricated")
    checks = 22
    themed = [name for name in groups if name.startswith("variant_")]
    if themed:
        catalog_path = ROOT / "src/main/resources/assets/chao_garden/chao_variants.json"
        require(catalog_path.is_file(), f"{path}: themed rig is missing its catalog")
        definitions = json.loads(catalog_path.read_text())["variants"]
        active_ids = {definition["id"] for definition in definitions}
        require(all(any(name == "variant_" + ident + suffix
                        for ident in active_ids for suffix in ("_head", "_body", "_marker"))
                    for name in themed), f"{path}: retired or unknown theme groups remain in the rig")
        refined = profiles_for(ROOT)
        runtime_bones = {bone["name"]: bone for bone in bridge.export_model(project)["bones"]}
        for definition in definitions:
            prefix = "variant_" + definition["id"]
            require(all(prefix + suffix in groups and direct(prefix + suffix)
                        for suffix in ("_head", "_body", "_marker")),
                    f"{path}: {definition['id']} needs authored head, body and marker groups")
            head_parts = direct(prefix + "_head")
            head_meshes = runtime_bones[prefix + "_head"]["meshes"]
            if definition["id"] in refined:
                require(all(e.get("type") == "cube" for e in head_parts),
                        f"{path}: {definition['id']} crown must use editable cuboids")
                validate_head_parts(head_meshes, runtime_bones["head"]["meshes"][0],
                                    f"{path}/{definition['id']}")
            else:
                require(all(e.get("type") == "cube" for e in head_parts),
                        f"{path}: {definition['id']} legacy head accessories must remain editable cuboids")
                require(max(v[1] for mesh in head_meshes for v in mesh["vertices"]) <= 1 + EPSILON,
                        f"{path}: {definition['id']} head accessories descend into shoulder space")
            checks += 3
    return checks

def synthetic_cube_project() -> dict[str, Any]:
    """Independent small artist fixture; not a copy of the generated Chao model."""
    image = Image.new("RGBA", (256, 256), (82, 174, 202, 255))
    stream = io.BytesIO()
    image.save(stream, format="PNG")
    rectangles = {"north": [0, 0, 2, 3], "south": [2, 0, 4, 3],
                  "east": [4, 0, 8, 3], "west": [8, 0, 12, 3],
                  "up": [12, 0, 14, 4], "down": [14, 0, 16, 4]}
    return {
        "meta": {"format_version": "4.0", "model_format": "free", "box_uv": False},
        "resolution": {"width": 256, "height": 256},
        "textures": [{"name": "fixture.png", "uuid": "fixture-texture", "width": 256,
                      "height": 256, "source": "data:image/png;base64," +
                      base64.b64encode(stream.getvalue()).decode()}],
        "elements": [{"type": "cube", "uuid": "fixture-cube", "name": "fixture_cube",
                      "from": [1, 2, 3], "to": [3, 5, 7], "origin": [1, 2, 3],
                      "rotation": [0, 0, 0], "box_uv": False, "inflate": 0,
                      "faces": {direction: {"uv": uv, "rotation": 0, "texture": 0}
                                for direction, uv in rectangles.items()}}],
        "outliner": [{"uuid": "fixture-root", "name": "root", "origin": [0, 0, 0],
                      "rotation": [0, 0, 0], "children": ["fixture-cube"]}],
        "animations": [],
    }


def cube_regression_checks() -> tuple[int, int]:
    """Golden-coordinate cases guard real cube and plane exporter semantics."""
    bridge_path = ROOT / "art" / "blockbench_to_runtime.py"
    spec = importlib.util.spec_from_file_location("chao_cube_regression_bridge", bridge_path)
    require(spec is not None and spec.loader is not None, "Cannot load cube export bridge")
    bridge = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(bridge)
    project = synthetic_cube_project()
    checks = 0

    def export(edited: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
        validate_bbmodel(edited, "<cube semantic fixture>")
        runtime = bridge.export_model(copy.deepcopy(edited))
        validate_runtime(runtime, "<cube semantic fixture export>")
        meshes = runtime["bones"][0]["meshes"]
        require(len(meshes) == 1 and meshes[0].get("primitive") == "cube",
                "A cube fixture did not export exactly one native cube mesh")
        return runtime, meshes[0]

    def bounds(mesh: dict[str, Any], low: list[float], high: list[float], label: str) -> None:
        close_vector([min(v[i] for v in mesh["vertices"]) for i in range(3)], low, label + "/minimum")
        close_vector([max(v[i] for v in mesh["vertices"]) for i in range(3)], high, label + "/maximum")

    def changed(**properties: Any) -> dict[str, Any]:
        result = copy.deepcopy(project)
        result["elements"][0].update(properties)
        return result

    baseline, mesh = export(project)
    bounds(mesh, [1, -5, 3], [3, -2, 7], "Y-up cube bounds become Y-down local coordinates")
    require(len(mesh["vertices"]) == 8 and len(mesh["faces"]) == 6,
            "A closed cube must retain eight unique corners and six quads")
    checks += 1

    _, mesh = export(changed(to=[4, 5, 7]))
    bounds(mesh, [1, -5, 3], [4, -2, 7], "Edited Cube.to")
    checks += 1
    _, mesh = export(changed(**{"from": [0, 2, 3]}))
    bounds(mesh, [0, -5, 3], [3, -2, 7], "Edited Cube.from")
    checks += 1
    _, mesh = export(changed(inflate=0.5))
    bounds(mesh, [.5, -5.5, 2.5], [3.5, -1.5, 7.5], "Inflation expands every face")
    checks += 1
    _, mesh = export(changed(rotation=[0, 0, 90]))
    bounds(mesh, [-2, -4, 3], [1, -2, 7], "Cube rotation around its object origin")
    checks += 1
    _, mesh = export(changed(rotation=[0, 0, 90], origin=[0, 0, 0]))
    bounds(mesh, [-5, -3, 3], [-2, -1, 7], "Edited cube origin changes rotation pivot")
    checks += 1
    _, mesh = export(changed(rotation=[90, 0, 0]))
    bounds(mesh, [1, -2, 3], [3, 2, 6], "X-axis cube rotation around its object origin")
    checks += 1
    _, mesh = export(changed(rotation=[0, 90, 0]))
    bounds(mesh, [1, -5, 1], [5, -2, 3], "Y-axis cube rotation around its object origin")
    checks += 1
    _, mesh = export(changed(rotation=[0, 0, 45], rescale=True))
    bounds(mesh, [-2, -7, 3], [3, -2, 7], "Blockbench rescale preserves the rotated cube projection")
    checks += 1
    _, mesh = export(changed(stretch=[2, 1, .5]))
    bounds(mesh, [0, -5, 4], [4, -2, 6], "Cube stretch around its center")
    checks += 1

    box = changed(box_uv=True, uv_offset=[10, 12])
    boxed, mesh = export(box)
    uvs = [p for face in mesh["faces"] for p in face["uv"]]
    close_vector([min(p[i] for p in uvs) for i in range(2)], [10, 12], "Box UV minimum")
    close_vector([max(p[i] for p in uvs) for i in range(2)], [22, 19], "Box UV maximum")
    checks += 1
    inherited = copy.deepcopy(box)
    inherited["elements"][0].pop("box_uv")
    inherited["meta"]["box_uv"] = True
    inherited_model, _ = export(inherited)
    same_render_geometry(inherited_model, boxed, "Box UV inherited from project metadata")
    checks += 1
    mirrored = copy.deepcopy(box)
    mirrored["elements"][0]["mirror_uv"] = True
    _, mirror_mesh = export(mirrored)
    bounds(mirror_mesh, [1, -5, 3], [3, -2, 7], "Mirror UV does not mirror geometry")
    require([f["uv"] for f in mirror_mesh["faces"]] != [f["uv"] for f in mesh["faces"]],
            "Mirror UV did not change texture orientation")
    checks += 1

    rotated_uv = copy.deepcopy(project)
    rotated_uv["elements"][0]["faces"]["north"]["rotation"] = 90
    _, mesh = export(rotated_uv)
    first_face = mesh["faces"][0]
    pair = {tuple(mesh["vertices"][i]): uv for i, uv in zip(first_face["v"], first_face["uv"])}
    close_vector(pair[(1.0, -5.0, 3.0)], [0, 0], "Face UV rotation preserves corner association")
    checks += 1
    reversed_uv = copy.deepcopy(project)
    reversed_uv["elements"][0]["faces"]["north"]["uv"] = [2, 3, 0, 0]
    _, mesh = export(reversed_uv)
    first_face = mesh["faces"][0]
    pair = {tuple(mesh["vertices"][i]): uv for i, uv in zip(first_face["v"], first_face["uv"])}
    close_vector(pair[(3.0, -5.0, 3.0)], [2, 3], "Reversed UV rectangle remains editable")
    checks += 1

    pane = changed(to=[3, 5, 3])
    _, mesh = export(pane)
    bounds(mesh, [1, -5, 3], [3, -2, 3], "Zero-depth face plane bounds")
    require(len(mesh["vertices"]) == 4 and len(mesh["faces"]) == 1,
            "No-cull face plane must not duplicate coincident front/back or zero-area edges")
    checks += 1
    single = copy.deepcopy(pane)
    for direction, face in single["elements"][0]["faces"].items():
        if direction != "south":
            face["texture"] = None
    _, mesh = export(single)
    require(len(mesh["faces"]) == 1, "A single active pane face was lost")
    checks += 1
    inflated_plane = copy.deepcopy(pane)
    inflated_plane["elements"][0]["inflate"] = .25
    _, mesh = export(inflated_plane)
    bounds(mesh, [.75, -5.25, 2.75], [3.25, -1.75, 3.25], "Inflating a face plane creates real depth")
    require(len(mesh["faces"]) == 6, "Inflated plane did not form a closed cuboid")
    checks += 1
    disabled = copy.deepcopy(project)
    disabled["elements"][0]["faces"]["north"]["enabled"] = False
    _, mesh = export(disabled)
    require(len(mesh["faces"]) == 5, "Disabled cube face was exported")
    checks += 1

    malformed = copy.deepcopy(baseline)
    malformed["bones"][0]["meshes"][0]["faces"].append(copy.deepcopy(malformed["bones"][0]["meshes"][0]["faces"][0]))
    try:
        validate_runtime(malformed, "<intentionally duplicate coplanar native face>")
    except ModelValidationError:
        checks += 1
    else:
        raise ModelValidationError("Native cube validation accepted duplicate coplanar faces")

    checks += roundtrip_checks(project, baseline, Path("synthetic_cube.bbmodel"))
    return checks, rejection_checks(project)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("models", nargs="*", type=Path)
    parser.add_argument("--no-roundtrip", action="store_true",
                        help="Only check saved-file structure; skip the public exporter checks")
    parser.add_argument("--no-style-checks", action="store_true",
                        help="Skip this release's Chao proportions/cube/pixel-density art contract")
    args = parser.parse_args()
    files = args.models or sorted((ROOT / "art" / "blockbench").glob("*.bbmodel"))
    require(bool(files), "No editable .bbmodel files found")
    total_roundtrips = 0
    style_checks = 0
    for path in files:
        project = json.loads(path.read_text())
        summary = validate_bbmodel(project, str(path))
        print(f"{path.name}: {summary.bones} bones, {summary.cubes} cubes "
              f"({summary.integer_cubes} integer-sized; {summary.half_unit_cubes} half-unit-sized), "
              f"{summary.meshes} meshes, "
              f"{summary.vertices} mesh vertices, {summary.faces} mesh faces, "
              f"{summary.unit_density_faces}/{summary.cube_faces} unit-density cube faces, "
              f"{summary.animations} animation clips, "
              f"{summary.embedded_textures} embedded PNG textures")
        if not args.no_style_checks:
            style_checks += validate_chao_cube_style(project, summary, path.name)
        runtime_name = "egg_mesh.json" if "egg" in path.stem else "chao_mesh.json"
        runtime_path = ROOT / "src/main/resources/assets/chao_garden/models/entity" / runtime_name if runtime_name == "egg_mesh.json" else prototype_path(ROOT)
        if not args.no_roundtrip:
            require(runtime_path.is_file(), f"Missing runtime counterpart {runtime_path}")
            total_roundtrips += roundtrip_checks(project, json.loads(runtime_path.read_text()), path)
    rejected = rejection_checks(json.loads(files[0].read_text()))
    cube_checks, cube_rejected = (0, 0) if args.no_roundtrip else cube_regression_checks()
    print(f"PASS: {len(files)} editable projects, {total_roundtrips} exporter roundtrip/edit checks, "
          f"{cube_checks} cube/plane semantic and roundtrip checks, "
          f"{style_checks} release art-contract checks, "
          f"{rejected + cube_rejected} deliberately malformed projects rejected.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ModelValidationError, ValueError, OSError) as error:
        raise SystemExit(f"MODEL VALIDATION FAILED: {error}")
