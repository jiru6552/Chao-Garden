"""Optional, portable and non-overwriting reports for source validators."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Sequence


def parse_output(argv: Sequence[str] | None = None) -> Path | None:
    """Reject existing destinations before a validator starts expensive checks."""
    parser = argparse.ArgumentParser(
        description="Validate source geometry; report to stdout by default.")
    parser.add_argument(
        "--output", type=Path,
        help="Write the JSON result to a new file; never overwrite an existing path.")
    output = parser.parse_args(argv).output
    # is_symlink also catches dangling links, which Path.exists alone misses.
    if output is not None and (output.exists() or output.is_symlink()):
        parser.error(f"Refusing to overwrite existing output: {output}")
    return output


def write_result(output: Path | None, result: dict) -> None:
    """Leave stdout-only runs untouched; exclusive creation also closes races."""
    if output is None:
        return
    payload = json.dumps(result, indent=2) + "\n"
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("x", encoding="utf-8") as stream:
        stream.write(payload)
