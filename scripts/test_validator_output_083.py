"""Cheap output/CLI regressions; never run the expensive geometry sweeps."""
from __future__ import annotations

import contextlib
import io
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

if __package__:
    from .validator_output import parse_output, write_result
else:
    from validator_output import parse_output, write_result


class ValidatorOutputTest(unittest.TestCase):
    def test_default_does_not_write_or_serialize(self):
        self.assertIsNone(parse_output([]))
        # A value which cannot be serialized is harmless without --output.
        write_result(None, {"unused": object()})

    def test_explicit_new_path_round_trips_json_and_creates_parent(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "nested" / "report.json"
            self.assertEqual(parse_output(["--output", str(output)]), output)
            self.assertFalse(output.exists())
            result = {"checks": 42, "scope": "regression fixture"}
            write_result(output, result)
            self.assertEqual(json.loads(output.read_text()), result)

    def test_existing_destinations_are_rejected_without_changes(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            old = root / "old.json"
            old.write_bytes(b"preserved evidence\n")
            directory = root / "directory"
            directory.mkdir()
            dangling = root / "dangling.json"
            dangling.symlink_to(root / "absent")
            for output in (old, directory, dangling):
                with self.subTest(output=output), contextlib.redirect_stderr(io.StringIO()):
                    with self.assertRaises(SystemExit) as error:
                        parse_output(["--output", str(output)])
                    self.assertEqual(error.exception.code, 2)
            self.assertEqual(old.read_bytes(), b"preserved evidence\n")
            self.assertTrue(dangling.is_symlink())
            self.assertEqual(list(directory.iterdir()), [])

    def test_path_created_after_parse_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "report.json"
            parsed = parse_output(["--output", str(output)])
            output.write_bytes(b"concurrent evidence\n")
            with self.assertRaises(FileExistsError):
                write_result(parsed, {"replace": True})
            self.assertEqual(output.read_bytes(), b"concurrent evidence\n")

    def test_each_real_entrypoint_rejects_before_reading_geometry(self):
        scripts = [
            "validate_hero_swim_morph_02564.py",
            "validate_hero_fly_morph_02565.py",
            "validate_hero_run_morph_02566.py",
            "validate_hero_power_morph_02567.py",
        ]
        # A temporary project containing scripts alone has no models to read.
        # The actual CLI must reject the old report before trying geometry.
        source = Path(__file__).resolve().parent
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            copied = root / "scripts"
            copied.mkdir()
            for path in source.glob("*.py"):
                (copied / path.name).write_bytes(path.read_bytes())
            output = root / "preserve.json"
            output.write_bytes(b"existing native evidence\n")
            for name in scripts:
                commands = [
                    [sys.executable, "-B", str(copied / name)],
                    [sys.executable, "-B", "-m", "scripts." + Path(name).stem],
                ]
                for command in commands:
                    with self.subTest(script=name, mode=command[-1]):
                        self.check_entrypoint(command, root, output)

    def check_entrypoint(self, command, root, output):
        result = subprocess.run(
            command + ["--output", str(output)],
            cwd=root, text=True, capture_output=True, timeout=10)
        self.assertEqual(result.returncode, 2, result.stderr)
        self.assertIn("Refusing to overwrite existing output", result.stderr)
        self.assertNotIn("Traceback", result.stderr)
        self.assertEqual(output.read_bytes(), b"existing native evidence\n")
        self.assertFalse((root / "build").exists())


if __name__ == "__main__":
    unittest.main()
