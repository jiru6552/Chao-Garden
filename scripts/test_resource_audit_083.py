#!/usr/bin/env python3
"""Small corrupt/missing-resource fixtures for the read-only maintenance guard."""
from pathlib import Path
import json
import struct
import tempfile
import unittest
from unittest.mock import patch
import zlib

import audit_resource_links_083 as auditor


def png():
    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff))
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"\0\xff\xff\xff\xff"))
            + chunk(b"IEND", b""))


class ResourceAuditTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.assets = self.root / "src/main/resources/assets/chao_garden"
        self.model = self.assets / "models/item/example.json"
        self.texture = self.assets / "textures/item/example.png"
        self.model.parent.mkdir(parents=True)
        self.texture.parent.mkdir(parents=True)
        self.texture.write_bytes(png())
        self.model.write_text(json.dumps({"parent": "minecraft:item/generated",
            "textures": {"layer0": "chao_garden:item/example"}}))

    def test_existing_link_and_external_parent(self):
        result = auditor.audit(self.root)
        self.assertEqual([], result["errors"])
        self.assertEqual(1, result["summary"]["internal_reference_edges"])

    def test_no_required_pillow_dependency(self):
        with patch.object(auditor, "Image", None):
            result = auditor.audit(self.root)
        self.assertEqual([], result["errors"])
        self.assertIn("CRCs only", result["summary"]["png_validation"])

    def test_missing_internal_model_and_texture(self):
        self.model.write_text(json.dumps({"parent": "chao_garden:item/missing",
            "textures": {"layer0": "chao_garden:item/absent"}}))
        errors = auditor.audit(self.root)["errors"]
        self.assertEqual(2, len(errors))
        self.assertTrue(all(error["kind"] == "missing_reference" for error in errors))

    def test_duplicate_json_key(self):
        self.model.write_text('{"textures":{},"textures":{}}')
        self.assertEqual("duplicate_json_key", auditor.audit(self.root)["errors"][0]["kind"])

    def test_invalid_json(self):
        self.model.write_text("{broken")
        self.assertEqual("invalid_file", auditor.audit(self.root)["errors"][0]["kind"])

    def test_corrupt_png_crc_without_pillow(self):
        raw = bytearray(png())
        raw[-1] ^= 1
        self.texture.write_bytes(raw)
        with patch.object(auditor, "Image", None):
            errors = auditor.audit(self.root)["errors"]
        self.assertTrue(any("CRC mismatch" in error.get("detail", "") for error in errors))

    def test_imported_uv_and_collapsed_morph_are_warnings(self):
        path = self.assets / "models/entity/example.json"
        path.parent.mkdir()
        path.write_text(json.dumps({"texture_width": 64, "texture_height": 32,
            "bones": [{"name": "skull", "meshes": [{"vertices": [[0, 0, 0]] * 3,
                "faces": [{"v": [0, 1, 2], "uv": [[66, 0]] * 3}]}]}]}))
        result = auditor.audit(self.root)
        self.assertEqual([], result["errors"])
        self.assertEqual({"uv_outside_declared_atlas", "collapsed_first_triangle"},
                         {warning["kind"] for warning in result["warnings"]})

    def test_invalid_mesh_index_is_error(self):
        path = self.assets / "models/entity/example.json"
        path.parent.mkdir()
        path.write_text(json.dumps({"bones": [{"name": "root", "meshes": [
            {"vertices": [], "faces": [{"v": [0, 1, 2], "uv": [[0, 0]] * 3}]}]}]}))
        errors = auditor.audit(self.root)["errors"]
        self.assertEqual("invalid_mesh_index", errors[0]["kind"])


if __name__ == "__main__":
    unittest.main()
